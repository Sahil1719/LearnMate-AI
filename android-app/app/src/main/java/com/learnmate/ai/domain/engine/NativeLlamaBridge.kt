package com.learnmate.ai.domain.engine

import android.util.Log

/**
 * JNI Bridge interface to ExecuTorch native runtime.
 * Safely handles missing native libraries without crashing the JVM.
 */
object NativeLlamaBridge {
    private const val TAG = "NativeLlamaBridge"
    private var isNativeLibraryLoaded = false

    init {
        try {
            System.loadLibrary("executorch_llama_jni")
            isNativeLibraryLoaded = true
            Log.i(TAG, "Successfully loaded native library: libexecutorch_llama_jni.so")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLibraryLoaded = false
            Log.w(TAG, "Native library libexecutorch_llama_jni.so not present on device: ${e.message}")
        } catch (e: Exception) {
            isNativeLibraryLoaded = false
            Log.e(TAG, "Unexpected error loading native library: ${e.message}")
        }
    }

    fun isAvailable(): Boolean = isNativeLibraryLoaded

    /**
     * Token callback interface passed to native C++ runner for streaming.
     */
    fun interface NativeTokenCallback {
        fun onToken(tokenText: String)
    }

    /**
     * Initializes and memory-maps model weights and tokenizer in native C++.
     * Returns a 64-bit native handle pointer, or 0 on failure.
     */
    external fun nativeInit(
        modelPath: String,
        tokenizerPath: String,
        useMmap: Boolean,
        seqLen: Int,
        numThreads: Int = 1
    ): Long

    /**
     * Counts the exact number of TikToken tokens for the given text using the native tokenizer.
     * Returns token count >= 0, or negative error code on failure.
     */
    external fun nativeCountTokens(handle: Long, text: String): Int

    /**
     * Encodes text into an array of exact TikToken token IDs using the native tokenizer.
     * Returns LongArray of token IDs, or null on failure.
     */
    external fun nativeEncode(handle: Long, text: String): LongArray?

    /**
     * Decodes an array of token IDs back into string pieces using the native tokenizer.
     * Returns reconstructed string, or null on failure.
     */
    external fun nativeDecode(handle: Long, tokens: LongArray): String?

    /**
     * Executes generation loop on the native runner.
     * Emits tokens to callback.
     * Returns total tokens generated, or negative error code.
     */
    external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        callback: NativeTokenCallback
    ): Int

    /**
     * Stops active generation.
     */
    external fun nativeStop(handle: Long)

    /**
     * Frees native runner memory.
     */
    external fun nativeRelease(handle: Long)

    /**
     * Unloads model and releases runner resources (alias for nativeRelease).
     */
    external fun nativeUnload(handle: Long)
}
