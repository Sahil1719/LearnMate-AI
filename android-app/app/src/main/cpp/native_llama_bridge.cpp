/*
 * LearnMate AI - ExecuTorch Native Llama JNI Bridge
 * Target Architecture: ARM64 Android (Snapdragon 662, Android 12, API 26+)
 * Runtime: ExecuTorch v0.5.0 with XNNPACK CPU Delegate
 */

#include <jni.h>
#include <string>
#include <memory>
#include <atomic>
#include <mutex>
#include <unordered_map>
#include <vector>
#include <android/log.h>

#include <executorch/examples/models/llama/runner/runner.h>
#include <executorch/examples/models/llama/tokenizer/llama_tiktoken.h>
#include <executorch/extension/llm/runner/stats.h>
#include <executorch/runtime/platform/runtime.h>

#define LOG_TAG "LearnMate_NativeLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM* g_jvm = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    g_jvm = vm;
    LOGI("JNI_OnLoad: Initialized JavaVM pointer");
    executorch::runtime::runtime_init();
    return JNI_VERSION_1_6;
}

// Structure encapsulating the native runner state and TikToken tokenizer
struct NativeLlamaContext {
    std::unique_ptr<example::Runner> runner;
    std::unique_ptr<::executorch::extension::llm::Tiktoken> tokenizer;
    std::string model_path;
    std::string tokenizer_path;
    bool use_mmap{true};
    int32_t seq_len{128};
    int32_t num_threads{1};
    std::atomic<bool> is_initialized{false};
    std::atomic<bool> is_generating{false};
    std::atomic<bool> should_stop{false};
};

static std::mutex g_context_map_mutex;
static std::unordered_map<jlong, std::shared_ptr<NativeLlamaContext>> g_context_map;
static std::atomic<jlong> g_next_handle{1001};

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeInit(
    JNIEnv* env,
    jobject /* thiz */,
    jstring jModelPath,
    jstring jTokenizerPath,
    jboolean jUseMmap,
    jint jSeqLen,
    jint jNumThreads
) {
    if (!jModelPath || !jTokenizerPath) {
        LOGE("nativeInit: Null model or tokenizer path");
        return 0;
    }

    const char* model_path_cstr = env->GetStringUTFChars(jModelPath, nullptr);
    const char* tokenizer_path_cstr = env->GetStringUTFChars(jTokenizerPath, nullptr);

    std::string model_path = model_path_cstr ? model_path_cstr : "";
    std::string tokenizer_path = tokenizer_path_cstr ? tokenizer_path_cstr : "";

    env->ReleaseStringUTFChars(jModelPath, model_path_cstr);
    env->ReleaseStringUTFChars(jTokenizerPath, tokenizer_path_cstr);

    if (model_path.empty() || tokenizer_path.empty()) {
        LOGE("nativeInit: Empty path string received");
        return 0;
    }

    LOGI("nativeInit: model=%s, tokenizer=%s, mmap=%d, seq_len=%d, threads=%d",
         model_path.c_str(), tokenizer_path.c_str(), jUseMmap, jSeqLen, jNumThreads);

    auto ctx = std::make_shared<NativeLlamaContext>();
    ctx->model_path = model_path;
    ctx->tokenizer_path = tokenizer_path;
    ctx->use_mmap = (jUseMmap == JNI_TRUE);
    ctx->seq_len = (jSeqLen > 0) ? jSeqLen : 128;
    ctx->num_threads = (jNumThreads > 0) ? jNumThreads : 1;

    try {
        // Instantiate the verified ExecuTorch Llama runner
        ctx->runner = std::make_unique<example::Runner>(
            ctx->model_path,
            ctx->tokenizer_path,
            0.8f // default temperature
        );

        // Explicitly load model FlatBuffer and runner dependencies
        ::executorch::runtime::Error err = ctx->runner->load();
        if (err != ::executorch::runtime::Error::Ok) {
            LOGE("nativeInit: runner->load() failed with error 0x%x", static_cast<uint32_t>(err));
            return 0;
        }

        if (!ctx->runner->is_loaded()) {
            LOGE("nativeInit: runner->is_loaded() returned false after load()");
            return 0;
        }

        // Initialize dedicated TikToken instance for exact token counting and budgeting
        ctx->tokenizer = example::get_tiktoken_for_llama();
        ::executorch::runtime::Error tok_err = ctx->tokenizer->load(ctx->tokenizer_path);
        if (tok_err != ::executorch::runtime::Error::Ok) {
            LOGW("nativeInit: Standalone TikToken load returned error 0x%x for %s",
                 static_cast<uint32_t>(tok_err), ctx->tokenizer_path.c_str());
        } else {
            LOGI("nativeInit: Dedicated TikToken loaded successfully, vocab_size=%d",
                 ctx->tokenizer->vocab_size());
        }

        ctx->is_initialized.store(true);
        LOGI("nativeInit: Runner successfully loaded and verified");

        jlong handle = g_next_handle.fetch_add(1);
        {
            std::lock_guard<std::mutex> lock(g_context_map_mutex);
            g_context_map[handle] = ctx;
        }
        return handle;
    } catch (const std::exception& e) {
        LOGE("nativeInit: Exception during runner initialization: %s", e.what());
        return 0;
    } catch (...) {
        LOGE("nativeInit: Unknown native exception during runner initialization");
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeCountTokens(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jstring jText
) {
    if (!jText) return 0;

    std::shared_ptr<NativeLlamaContext> ctx;
    {
        std::lock_guard<std::mutex> lock(g_context_map_mutex);
        auto it = g_context_map.find(handle);
        if (it != g_context_map.end()) {
            ctx = it->second;
        }
    }

    if (!ctx || !ctx->tokenizer) {
        LOGW("nativeCountTokens: Context %lld or tokenizer is null", static_cast<long long>(handle));
        return -1;
    }

    const char* text_cstr = env->GetStringUTFChars(jText, nullptr);
    std::string text_str = text_cstr ? text_cstr : "";
    env->ReleaseStringUTFChars(jText, text_cstr);

    if (text_str.empty()) {
        return 0;
    }

    auto encode_res = ctx->tokenizer->encode(text_str, /* bos */ 0, /* eos */ 0);
    if (encode_res.error() != ::executorch::runtime::Error::Ok) {
        LOGW("nativeCountTokens: encode returned error 0x%x", static_cast<uint32_t>(encode_res.error()));
        return -2;
    }

    return static_cast<jint>(encode_res.get().size());
}

JNIEXPORT jlongArray JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeEncode(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jstring jText
) {
    if (!jText) return nullptr;

    std::shared_ptr<NativeLlamaContext> ctx;
    {
        std::lock_guard<std::mutex> lock(g_context_map_mutex);
        auto it = g_context_map.find(handle);
        if (it != g_context_map.end()) {
            ctx = it->second;
        }
    }

    if (!ctx || !ctx->tokenizer) return nullptr;

    const char* text_cstr = env->GetStringUTFChars(jText, nullptr);
    std::string text_str = text_cstr ? text_cstr : "";
    env->ReleaseStringUTFChars(jText, text_cstr);

    if (text_str.empty()) {
        return env->NewLongArray(0);
    }

    auto encode_res = ctx->tokenizer->encode(text_str, /* bos */ 0, /* eos */ 0);
    if (encode_res.error() != ::executorch::runtime::Error::Ok) {
        return nullptr;
    }

    const auto& tokens = encode_res.get();
    jlongArray result = env->NewLongArray(static_cast<jsize>(tokens.size()));
    if (!result) return nullptr;

    std::vector<jlong> jtokens(tokens.begin(), tokens.end());
    env->SetLongArrayRegion(result, 0, static_cast<jsize>(tokens.size()), jtokens.data());
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeDecode(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jlongArray jTokens
) {
    if (!jTokens) return nullptr;

    std::shared_ptr<NativeLlamaContext> ctx;
    {
        std::lock_guard<std::mutex> lock(g_context_map_mutex);
        auto it = g_context_map.find(handle);
        if (it != g_context_map.end()) {
            ctx = it->second;
        }
    }

    if (!ctx || !ctx->tokenizer) return nullptr;

    jsize len = env->GetArrayLength(jTokens);
    if (len == 0) return env->NewStringUTF("");

    std::vector<jlong> tokens(len);
    env->GetLongArrayRegion(jTokens, 0, len, tokens.data());

    std::string reconstructed;
    uint64_t prev = 0;
    for (jsize i = 0; i < len; ++i) {
        auto dec_res = ctx->tokenizer->decode(prev, static_cast<uint64_t>(tokens[i]));
        if (dec_res.error() == ::executorch::runtime::Error::Ok) {
            reconstructed += dec_res.get();
        }
        prev = static_cast<uint64_t>(tokens[i]);
    }

    return env->NewStringUTF(reconstructed.c_str());
}

JNIEXPORT jint JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeGenerate(
    JNIEnv* env,
    jobject /* thiz */,
    jlong handle,
    jstring jPrompt,
    jint maxTokens,
    jfloat temperature,
    jobject callback
) {
    std::shared_ptr<NativeLlamaContext> ctx;
    {
        std::lock_guard<std::mutex> lock(g_context_map_mutex);
        auto it = g_context_map.find(handle);
        if (it != g_context_map.end()) {
            ctx = it->second;
        }
    }

    if (!ctx || !ctx->is_initialized.load() || !ctx->runner) {
        LOGE("nativeGenerate: Invalid or uninitialized handle: %lld", static_cast<long long>(handle));
        return -1;
    }

    if (!jPrompt) {
        LOGE("nativeGenerate: Null prompt received");
        return -2;
    }

    if (!callback) {
        LOGE("nativeGenerate: Null callback received");
        return -3;
    }

    // Prevent concurrent generation on the same runner context
    bool expected_generating = false;
    if (!ctx->is_generating.compare_exchange_strong(expected_generating, true)) {
        LOGW("nativeGenerate: Concurrent generation rejected on context %lld", static_cast<long long>(handle));
        return -4;
    }

    ctx->should_stop.store(false);

    const char* prompt_cstr = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt_str = prompt_cstr ? prompt_cstr : "";
    env->ReleaseStringUTFChars(jPrompt, prompt_cstr);

    if (prompt_str.empty()) {
        ctx->is_generating.store(false);
        return 0;
    }

    // Resolve callback method: onToken(String)
    jclass callbackClass = env->GetObjectClass(callback);
    if (!callbackClass) {
        LOGE("nativeGenerate: Failed to get callback object class");
        ctx->is_generating.store(false);
        return -5;
    }

    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    if (!onTokenMethod) {
        LOGE("nativeGenerate: Failed to find onToken method on callback object");
        ctx->is_generating.store(false);
        return -6;
    }

    // Context sequence budget: clamp to model max_seq_len (128)
    int32_t target_seq_len = (maxTokens > 0) ? maxTokens : ctx->seq_len;
    if (target_seq_len > 128) {
        target_seq_len = 128;
    }

    // Enforce prompt length constraint using TikToken:
    // P must be >= 1, P < target_seq_len, and P < 128.
    if (ctx->tokenizer) {
        auto enc_res = ctx->tokenizer->encode(prompt_str, /* bos */ 0, /* eos */ 0);
        if (enc_res.error() == ::executorch::runtime::Error::Ok) {
            int actual_p = static_cast<int>(enc_res.get().size());
            if (actual_p < 1) {
                LOGE("nativeGenerate: Prompt encoded to 0 tokens");
                ctx->is_generating.store(false);
                return -2;
            }
            if (actual_p >= target_seq_len || actual_p >= 128) {
                LOGE("nativeGenerate: Prompt tokens (%d) >= target_seq_len (%d). Rejecting to prevent runner abort.",
                     actual_p, target_seq_len);
                ctx->is_generating.store(false);
                return -9; // Error code for context overflow
            }
            LOGI("nativeGenerate: Prompt validated via TikToken (%d prompt tokens, target_seq_len=%d)",
                 actual_p, target_seq_len);
        }
    }

    LOGI("nativeGenerate: Starting generation (max_seq_len=%d, temp=%.2f, prompt_len=%zu)",
         target_seq_len, temperature, prompt_str.length());

    std::atomic<int> tokens_emitted{0};

    // Construct streaming callback lambda
    auto token_callback = [&](const std::string& piece) {
        if (piece.empty() || ctx->should_stop.load()) {
            return;
        }

        // Ensure JNIEnv attachment
        JNIEnv* thread_env = env;
        if (g_jvm) {
            jint res = g_jvm->GetEnv(reinterpret_cast<void**>(&thread_env), JNI_VERSION_1_6);
            if (res == JNI_EDETACHED) {
                if (g_jvm->AttachCurrentThread(&thread_env, nullptr) != JNI_OK) {
                    LOGE("nativeGenerate callback: Failed to attach thread");
                    return;
                }
            }
        }

        jstring jPiece = thread_env->NewStringUTF(piece.c_str());
        if (jPiece) {
            thread_env->CallVoidMethod(callback, onTokenMethod, jPiece);
            thread_env->DeleteLocalRef(jPiece);
            tokens_emitted.fetch_add(1);

            if (thread_env->ExceptionCheck()) {
                LOGW("nativeGenerate callback: Exception thrown in Kotlin callback, halting");
                thread_env->ExceptionClear();
                ctx->should_stop.store(true);
                if (ctx->runner) {
                    ctx->runner->stop();
                }
            }
        }
    };

    try {
        // Execute generation with echo=false so only generated tokens are emitted
        ::executorch::runtime::Error gen_err = ctx->runner->generate(
            prompt_str,
            target_seq_len,
            token_callback,
            /* stats_callback */ nullptr,
            /* echo */ false,
            /* warmup */ false
        );

        ctx->is_generating.store(false);

        if (gen_err != ::executorch::runtime::Error::Ok) {
            LOGW("nativeGenerate: runner->generate returned error 0x%x (emitted %d tokens)",
                 static_cast<uint32_t>(gen_err), tokens_emitted.load());
        } else {
            LOGI("nativeGenerate: Generation completed successfully (emitted %d tokens)",
                 tokens_emitted.load());
        }

        return tokens_emitted.load();
    } catch (const std::exception& e) {
        LOGE("nativeGenerate: Exception during generation: %s", e.what());
        ctx->is_generating.store(false);
        return -7;
    } catch (...) {
        LOGE("nativeGenerate: Unknown native exception during generation");
        ctx->is_generating.store(false);
        return -8;
    }
}

JNIEXPORT void JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeStop(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle
) {
    std::shared_ptr<NativeLlamaContext> ctx;
    {
        std::lock_guard<std::mutex> lock(g_context_map_mutex);
        auto it = g_context_map.find(handle);
        if (it != g_context_map.end()) {
            ctx = it->second;
        }
    }

    if (ctx) {
        LOGI("nativeStop: Signaled stop for context %lld", static_cast<long long>(handle));
        ctx->should_stop.store(true);
        if (ctx->runner) {
            ctx->runner->stop();
        }
    }
}

JNIEXPORT void JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeRelease(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle
) {
    std::shared_ptr<NativeLlamaContext> ctx;
    {
        std::lock_guard<std::mutex> lock(g_context_map_mutex);
        auto it = g_context_map.find(handle);
        if (it != g_context_map.end()) {
            ctx = it->second;
            g_context_map.erase(it);
        }
    }

    if (ctx) {
        LOGI("nativeRelease: Releasing context %lld", static_cast<long long>(handle));
        ctx->should_stop.store(true);
        if (ctx->runner) {
            ctx->runner->stop();
        }
    }
}

JNIEXPORT void JNICALL
Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeUnload(
    JNIEnv* env,
    jobject thiz,
    jlong handle
) {
    Java_com_learnmate_ai_domain_engine_NativeLlamaBridge_nativeRelease(env, thiz, handle);
}

} // extern "C"
