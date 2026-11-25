package com.learnmate.ai.domain.engine

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Debug
import android.util.Log
import com.learnmate.ai.domain.knowledge.CareerKnowledgeBase
import com.learnmate.ai.domain.model.InferenceBenchmarkStats
import com.learnmate.ai.domain.model.InferenceProfile
import com.learnmate.ai.domain.model.InferenceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Concrete on-device inference implementation.
 * Integrates ExecuTorch C++ runtime via JNI with fallback to offline knowledge taxonomy.
 * Supports Meta Llama 3.2 3B Instruct fine-tuned model (learnmate_llama3_2_3b.pte).
 * Enforces exact TikToken context budgeting (P + G <= 128).
 */
@Suppress("DEPRECATION")
class ExecuTorchLlmEngine(
    private val context: Context? = null,
    val profileManager: InferenceProfileManager = InferenceProfileManager(context)
) : LlmInferenceEngine {

    companion object {
        private const val TAG = "ExecuTorchLlmEngine"

        /** Maximum hard sequence length supported by exported PTE model. */
        const val MAX_TOTAL_SEQ_LEN = 128

        /** Minimum generation token reservation when feasible. */
        const val MIN_GENERATION_TOKENS = 32

        /** Maximum prompt token limit when reserving 32 generation tokens (128 - 32). */
        const val SAFE_MAX_PROMPT_TOKENS = MAX_TOTAL_SEQ_LEN - MIN_GENERATION_TOKENS // 96 tokens

        // Candidate model search paths in order of priority (LearnMate fine-tuned Llama 3.2 3B is primary)
        val CANDIDATE_MODEL_PATHS = listOf(
            "/data/local/tmp/llama/learnmate_llama3_2_3b.pte",
            "/sdcard/Download/learnmate_llama3_2_3b.pte",
            "/data/local/tmp/llama/llama3_2_3b_8da4w.pte",
            "/sdcard/Download/llama3_2_3b_8da4w.pte",
            "/data/local/tmp/llama/llama3_2_1b_8da4w.pte",
            "/sdcard/Download/llama3_2_1b_8da4w.pte",
            "/data/local/tmp/llama/model.pte",
            "/sdcard/Download/llama3_8b_8da4w.pte",
            "/data/local/tmp/llama/llama3_8b_8da4w.pte",
            "/sdcard/Download/stories110m.pte",
            "/sdcard/Download/stories15m.pte",
            "/data/local/tmp/llama/stories110m.pte"
        )

        // Candidate tokenizer search paths
        val CANDIDATE_TOKENIZER_PATHS = listOf(
            "/data/local/tmp/llama/tokenizer.model",
            "/sdcard/Download/tokenizer.model",
            "/data/local/tmp/llama/tokenizer.bin",
            "/sdcard/Download/tokenizer.bin"
        )
    }

    /**
     * Complete structured prompt components for LearnMate AI.
     */
    data class CompletePrompt(
        val userQuestion: String,
        val goalTitle: String? = null,
        val systemInstruction: String? = null,
        val conversationContext: String? = null
    )

    /**
     * Calculated context budget parameters enforcing P + G <= 128.
     */
    data class BudgetResult(
        val safePrompt: String,
        val promptTokens: Int,
        val generationTokens: Int,
        val totalSeqLen: Int
    )

    private val _state = MutableStateFlow<InferenceState>(InferenceState.Uninitialized)
    override val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var nativeHandle: Long = 0L
    private var activeModelPath: String? = null
    private var activeTokenizerPath: String? = null
    private var latestBenchmarkStats: InferenceBenchmarkStats? = null
    private val isGenerating = AtomicBoolean(false)
    private val initMutex = Mutex()

    fun getNativeHandle(): Long = nativeHandle
    fun getActiveModelPath(): String? = activeModelPath
    fun getActiveTokenizerPath(): String? = activeTokenizerPath

    init {
        // Register memory pressure callback to protect system stability
        profileManager.setMemoryPressureListener { level ->
            if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                Log.w(TAG, "Background memory pressure (level=$level). Halting active generation.")
                stopGeneration()
            } else if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                val availMb = profileManager.refreshMemorySnapshot().availableRamMb
                if (availMb < 150) {
                    Log.w(TAG, "Emergency foreground memory pressure (level=$level, avail=${availMb}MB < 150MB). Halting active generation.")
                    stopGeneration()
                } else {
                    Log.d(TAG, "Foreground memory pressure (level=$level, avail=${availMb}MB). Sustaining active generation.")
                }
            }
        }
        // Automatically probe on-device storage upon instantiation
        probeAndInitialize()
    }

    private fun probeAndInitialize() {
        val foundModel = findModelFile()
        val foundTokenizer = findTokenizerFile()
        val profile = profileManager.activeProfile.value

        if (foundModel != null && foundTokenizer != null) {
            activeModelPath = foundModel.absolutePath
            activeTokenizerPath = foundTokenizer.absolutePath
            _state.value = InferenceState.Ready(
                modelName = foundModel.name,
                modelSizeBytes = foundModel.length(),
                contextLength = profile.maxContextLength,
                isMmap = profile.useMmap,
                delegateBackend = if (NativeLlamaBridge.isAvailable()) "XNNPACK (ARM NEON)" else "Offline Knowledge Fallback",
                profileName = profile.name,
                estimatedKvCacheMb = profile.estimatedKvCacheMb
            )
        } else {
            val expected = CANDIDATE_MODEL_PATHS.first()
            _state.value = InferenceState.ModelMissing(
                expectedModelPath = expected,
                searchedPaths = CANDIDATE_MODEL_PATHS
            )
        }
    }

    /**
     * Resolves the model weights file (.pte) from app-specific external storage first,
     * and falls back to candidate system/temporary paths.
     * Uses context.getExternalFilesDir("models") without requesting broad storage permissions.
     */
    fun findModelFile(candidatePaths: List<String> = CANDIDATE_MODEL_PATHS): File? {
        context?.let { ctx ->
            try {
                val internalModels = File(ctx.filesDir, "models")
                val internalExact = File(internalModels, "learnmate_llama3_2_3b.pte")
                if (internalExact.exists() && internalExact.isFile && internalExact.length() > 0) {
                    return internalExact
                }
                val appExternal = ctx.getExternalFilesDir("models")
                if (appExternal != null && appExternal.exists()) {
                    val files = appExternal.listFiles()
                    val exactMatch = files?.firstOrNull { it.name == "learnmate_llama3_2_3b.pte" && it.length() > 0 }
                    if (exactMatch != null) return exactMatch
                    val anyPte = files?.firstOrNull { it.name.endsWith(".pte") && it.length() > 0 }
                    if (anyPte != null) return anyPte
                }
            } catch (_: Exception) {
                // Silently ignore
            }
        }
        for (path in candidatePaths) {
            try {
                val f = File(path)
                if (f.exists() && f.canRead() && f.length() > 0 && f.name.endsWith(".pte")) {
                    return f
                }
            } catch (_: Exception) {
                // Ignore inaccessible filesystem sandboxes
            }
        }
        return null
    }

    /**
     * Resolves the tokenizer file (.model or .bin) independently of model file resolution,
     * checking app-private internal storage and app-specific external storage first.
     */
    fun findTokenizerFile(candidatePaths: List<String> = CANDIDATE_TOKENIZER_PATHS): File? {
        context?.let { ctx ->
            try {
                val internalModels = File(ctx.filesDir, "models")
                val internalExact = File(internalModels, "tokenizer.model")
                if (internalExact.exists() && internalExact.isFile && internalExact.length() > 0) {
                    return internalExact
                }
                val appExternal = ctx.getExternalFilesDir("models")
                if (appExternal != null && appExternal.exists()) {
                    val files = appExternal.listFiles()
                    val exactModel = files?.firstOrNull { it.name == "tokenizer.model" && it.length() > 0 }
                    if (exactModel != null) return exactModel
                    val exactBin = files?.firstOrNull { it.name == "tokenizer.bin" && it.length() > 0 }
                    if (exactBin != null) return exactBin
                    val anyTokenizer = files?.firstOrNull {
                        (it.name.endsWith(".model") || it.name.endsWith(".bin")) && it.length() > 0
                    }
                    if (anyTokenizer != null) return anyTokenizer
                }
            } catch (_: Exception) {
                // Silently ignore
            }
        }
        for (path in candidatePaths) {
            try {
                val f = File(path)
                if (f.exists() && f.canRead() && f.length() > 0 &&
                    (f.name.endsWith(".model") || f.name.endsWith(".bin"))
                ) {
                    return f
                }
            } catch (_: Exception) {
                // Ignore inaccessible filesystem sandboxes
            }
        }
        return null
    }

    /**
     * Retained for backward compatibility. Dispatches to findTokenizerFile or findModelFile.
     */
    fun findExistingFile(candidatePaths: List<String>): File? {
        val isTokenizer = candidatePaths.any { it.endsWith(".model") || it.endsWith(".bin") }
        return if (isTokenizer) findTokenizerFile(candidatePaths) else findModelFile(candidatePaths)
    }

    /**
     * Constructs a complete structured prompt incorporating Learner Goal, System Instructions,
     * Conversation Context, and User Question following the official Meta Llama 3.2 Instruct template.
     * Special tokens:
     *   <|begin_of_text|> (128000)
     *   <|start_header_id|> (128006)
     *   <|end_header_id|> (128007)
     *   <|eot_id|> (128009)
     * Ends with the assistant generation prefix without duplicating BOS or appending EOS.
     */
    fun buildCompletePrompt(
        userQuestion: String,
        goalTitle: String? = null,
        systemInstruction: String? = null,
        conversationContext: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append("<|begin_of_text|>")

        val hasSystem = !systemInstruction.isNullOrBlank() || !goalTitle.isNullOrBlank() || !conversationContext.isNullOrBlank()
        if (hasSystem) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n")
            val parts = mutableListOf<String>()
            if (!systemInstruction.isNullOrBlank()) {
                parts.add(systemInstruction.trim())
            } else {
                parts.add("You are LearnMate, an adaptive, offline on-device AI learning companion.")
            }
            if (!goalTitle.isNullOrBlank()) {
                parts.add("[Goal: ${goalTitle.trim()}]")
            }
            if (!conversationContext.isNullOrBlank()) {
                parts.add("[Context: ${conversationContext.trim()}]")
            }
            sb.append(parts.joinToString("\n"))
            sb.append("<|eot_id|>")
        }

        sb.append("<|start_header_id|>user<|end_header_id|>\n\n")
        sb.append(userQuestion.trim())
        sb.append("<|eot_id|>")

        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }

    /**
     * Enforces strict context safety (P + G <= 128) using exact TikToken tokenization.
     * Truncates long prompts on exact token boundaries while preserving user question content.
     */
    fun budgetCompletePrompt(
        prompt: String,
        requestedMaxTokens: Int = MAX_TOTAL_SEQ_LEN,
        minGenerationTokens: Int = MIN_GENERATION_TOKENS
    ): BudgetResult {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty or blank")
        }

        var budgetedText = prompt.trim()
        if (budgetedText.endsWith("<|start_header_id|>assistant<|end_header_id|>")) {
            budgetedText = "$budgetedText\n\n"
        }

        // 1. Calculate actual TikToken count of complete prompt including special tokens
        var actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)
        if (actualP < 1) {
            throw IllegalArgumentException("Prompt encoded to 0 tokens. Valid prompt required.")
        }

        val maxAllowedPromptTokens = (MAX_TOTAL_SEQ_LEN - minGenerationTokens).coerceAtMost(MAX_TOTAL_SEQ_LEN - 1)

        if (actualP > maxAllowedPromptTokens) {
            Log.w(
                TAG,
                "Prompt exceeds budget: $actualP tokens > max allowed $maxAllowedPromptTokens tokens. Truncating on token boundaries while preserving user question."
            )

            // 2. Handle structured Llama 3.2 Instruct prompts
            if (budgetedText.contains("<|start_header_id|>user<|end_header_id|>") &&
                budgetedText.contains("<|start_header_id|>assistant<|end_header_id|>")
            ) {
                val userHeaderIdx = budgetedText.indexOf("<|start_header_id|>user<|end_header_id|>")
                val userSection = budgetedText.substring(userHeaderIdx) // user turn + assistant trigger
                val userTokens = LlamaTikTokenTokenizer.countTokens(userSection, nativeHandle, context)
                val baseUserWithBos = userTokens + 1 // including <|begin_of_text|>

                if (baseUserWithBos >= maxAllowedPromptTokens) {
                    // Question alone exceeds budget: omit system message entirely and truncate question
                    val questionStart = userSection.indexOf("\n\n") + 2
                    val questionEnd = userSection.indexOf("<|eot_id|>")
                    val rawQuestion = if (questionStart >= 2 && questionEnd > questionStart) {
                        userSection.substring(questionStart, questionEnd)
                    } else {
                        userSection
                    }

                    // Overhead: <|begin_of_text|> (1) + <|start_header_id|>user<|end_header_id|>\n\n (4) + <|eot_id|> (1) + <|start_header_id|>assistant<|end_header_id|>\n\n (4) = 10 tokens
                    var maxQuestionTokens = (maxAllowedPromptTokens - 10).coerceAtLeast(1)
                    var truncatedQ = LlamaTikTokenTokenizer.truncateToTokens(rawQuestion, maxQuestionTokens, nativeHandle, context)
                    budgetedText = "<|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\n$truncatedQ<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
                    actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)

                    while (actualP > maxAllowedPromptTokens && maxQuestionTokens > 1) {
                        maxQuestionTokens--
                        truncatedQ = LlamaTikTokenTokenizer.truncateToTokens(rawQuestion, maxQuestionTokens, nativeHandle, context)
                        budgetedText = "<|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\n$truncatedQ<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
                        actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)
                    }
                } else {
                    // Question fits: preserve user turn and truncate system context into remaining budget
                    var remainingForSystemContent = maxAllowedPromptTokens - baseUserWithBos - 5 // overhead for system headers (5 tokens)
                    if (remainingForSystemContent >= 3 && budgetedText.contains("<|start_header_id|>system<|end_header_id|>")) {
                        val sysStart = budgetedText.indexOf("<|start_header_id|>system<|end_header_id|>\n\n") + "<|start_header_id|>system<|end_header_id|>\n\n".length
                        val sysEnd = budgetedText.indexOf("<|eot_id|>", sysStart)
                        val rawSys = if (sysEnd > sysStart) budgetedText.substring(sysStart, sysEnd) else ""
                        var truncatedSys = LlamaTikTokenTokenizer.truncateToTokens(rawSys, remainingForSystemContent, nativeHandle, context)
                        budgetedText = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$truncatedSys<|eot_id|>$userSection"
                        actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)

                        while (actualP > maxAllowedPromptTokens && remainingForSystemContent > 1) {
                            remainingForSystemContent--
                            truncatedSys = LlamaTikTokenTokenizer.truncateToTokens(rawSys, remainingForSystemContent, nativeHandle, context)
                            budgetedText = "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n$truncatedSys<|eot_id|>$userSection"
                            actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)
                        }

                        if (actualP > maxAllowedPromptTokens) {
                            // If still exceeding, drop system message entirely
                            budgetedText = "<|begin_of_text|>$userSection"
                            actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)
                        }
                    } else {
                        // Not enough room for meaningful system content; omit system header
                        budgetedText = "<|begin_of_text|>$userSection"
                        actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)
                    }
                }
            } else if (budgetedText.contains("Question:") &&
                (budgetedText.contains("Goal:") || budgetedText.contains("Context:") || budgetedText.contains("System:"))
            ) {
                // Backward compatibility for legacy structured prompts
                val questionIdx = budgetedText.indexOf("Question:")
                val prefix = budgetedText.substring(0, questionIdx).trim()
                val questionPart = budgetedText.substring(questionIdx).trim()

                val questionTokens = LlamaTikTokenTokenizer.countTokens(questionPart, nativeHandle, context)

                if (questionTokens >= maxAllowedPromptTokens) {
                    budgetedText = LlamaTikTokenTokenizer.truncateToTokens(questionPart, maxAllowedPromptTokens, nativeHandle, context)
                } else {
                    val remainingForPrefix = (maxAllowedPromptTokens - questionTokens - 1).coerceAtLeast(1)
                    val truncatedPrefix = LlamaTikTokenTokenizer.truncateToTokens(prefix, remainingForPrefix, nativeHandle, context)
                    budgetedText = "$truncatedPrefix\n$questionPart"
                }
            } else {
                budgetedText = LlamaTikTokenTokenizer.truncateToTokens(budgetedText, maxAllowedPromptTokens, nativeHandle, context)
            }

            actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)

            // Strictly enforce that actualP <= maxAllowedPromptTokens
            while (actualP > maxAllowedPromptTokens && actualP > 1) {
                val currentTokens = LlamaTikTokenTokenizer.encode(budgetedText, nativeHandle, context)
                if (currentTokens.size <= maxAllowedPromptTokens) break
                val sliceLen = maxAllowedPromptTokens.coerceAtMost(currentTokens.size - 1)
                budgetedText = LlamaTikTokenTokenizer.decode(currentTokens.subList(0, sliceLen), nativeHandle)
                actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)
            }
        }

        // Hard guarantee: actualP must be strictly < MAX_TOTAL_SEQ_LEN (128)
        if (actualP >= MAX_TOTAL_SEQ_LEN) {
            budgetedText = LlamaTikTokenTokenizer.truncateToTokens(budgetedText, MAX_TOTAL_SEQ_LEN - 1, nativeHandle, context)
            actualP = LlamaTikTokenTokenizer.countTokens(budgetedText, nativeHandle, context)
        }

        if (actualP < 1) {
            throw IllegalArgumentException("Unable to construct a valid prompt with at least 1 token.")
        }

        // Determine safe generation budget G such that P + G <= 128 and P < targetSeqLen
        val remainingCapacity = MAX_TOTAL_SEQ_LEN - actualP
        val safeG = requestedMaxTokens.coerceIn(1, remainingCapacity)
        val targetSeqLen = actualP + safeG // Guaranteed <= 128, and P < targetSeqLen

        return BudgetResult(
            safePrompt = budgetedText,
            promptTokens = actualP,
            generationTokens = safeG,
            totalSeqLen = targetSeqLen
        )
    }

    /**
     * Backward-compatible helper method for prompt budgeting.
     */
    fun budgetPrompt(prompt: String, maxPromptTokens: Int = SAFE_MAX_PROMPT_TOKENS): String {
        return try {
            val res = budgetCompletePrompt(prompt, requestedMaxTokens = MAX_TOTAL_SEQ_LEN, minGenerationTokens = MAX_TOTAL_SEQ_LEN - maxPromptTokens)
            res.safePrompt
        } catch (_: Exception) {
            prompt.trim()
        }
    }

    override suspend fun initialize(modelPath: String?, tokenizerPath: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            initMutex.withLock {
                try {
                    val resolvedModel = modelPath?.let { File(it) } ?: findModelFile()
                    val resolvedTokenizer = tokenizerPath?.let { File(it) } ?: findTokenizerFile()

                    if (resolvedModel == null || !resolvedModel.exists() || resolvedModel.length() == 0L) {
                        val missingPath = modelPath ?: CANDIDATE_MODEL_PATHS.first()
                        _state.value = InferenceState.ModelMissing(
                            expectedModelPath = missingPath,
                            searchedPaths = CANDIDATE_MODEL_PATHS
                        )
                        return@withLock Result.failure(
                            IllegalStateException("Model weights file not found on device: $missingPath")
                        )
                    }

                    if (resolvedTokenizer == null || !resolvedTokenizer.exists() || resolvedTokenizer.length() == 0L) {
                        val missingTokPath = tokenizerPath ?: CANDIDATE_TOKENIZER_PATHS.first()
                        Log.w(TAG, "Tokenizer file not found at: $missingTokPath. Inference may fail if native runtime requires it.")
                    }

                    // If already initialized with identical paths, avoid duplicate reload
                    if (nativeHandle != 0L &&
                        activeModelPath == resolvedModel.absolutePath &&
                        activeTokenizerPath == resolvedTokenizer?.absolutePath &&
                        _state.value is InferenceState.Ready
                    ) {
                        Log.i(TAG, "Runner already initialized with identical model: ${resolvedModel.absolutePath}")
                        return@withLock Result.success(Unit)
                    }

                    // If re-initializing with different path, release previous handle
                    if (nativeHandle != 0L) {
                        Log.i(TAG, "Releasing existing native runner handle ($nativeHandle) before re-initialization")
                        unload()
                    }

                    val activeProfile = profileManager.activeProfile.value

                    _state.value = InferenceState.Loading(
                        modelPath = resolvedModel.absolutePath,
                        progressPercent = 10,
                        statusMessage = "Opening memory map (${resolvedModel.length() / (1024 * 1024)} MB)..."
                    )

                    if (NativeLlamaBridge.isAvailable()) {
                        val startLoad = System.currentTimeMillis()
                        val handle = NativeLlamaBridge.nativeInit(
                            modelPath = resolvedModel.absolutePath,
                            tokenizerPath = resolvedTokenizer?.absolutePath ?: "",
                            useMmap = activeProfile.useMmap,
                            seqLen = activeProfile.maxContextLength.coerceAtMost(MAX_TOTAL_SEQ_LEN),
                            numThreads = activeProfile.numThreads
                        )
                        val loadDuration = System.currentTimeMillis() - startLoad

                        if (handle == 0L) {
                            _state.value = InferenceState.Error(
                                errorMessage = "Native runner initialization failed. Check device logs.",
                                isLowMemoryError = false
                            )
                            return@withLock Result.failure(RuntimeException("Native runner init failed"))
                        }

                        nativeHandle = handle
                        activeModelPath = resolvedModel.absolutePath
                        activeTokenizerPath = resolvedTokenizer?.absolutePath

                        _state.value = InferenceState.Ready(
                            modelName = resolvedModel.name,
                            modelSizeBytes = resolvedModel.length(),
                            contextLength = activeProfile.maxContextLength,
                            isMmap = activeProfile.useMmap,
                            delegateBackend = "XNNPACK (ARM NEON)",
                            profileName = activeProfile.name,
                            estimatedKvCacheMb = activeProfile.estimatedKvCacheMb
                        )
                        Log.i(TAG, "Native model loaded in ${loadDuration}ms via mmap (${activeProfile.name}, handle=$nativeHandle)")
                    } else {
                        // Native library not present on host JVM or uncompiled; enable deterministic fallback
                        activeModelPath = resolvedModel.absolutePath
                        activeTokenizerPath = resolvedTokenizer?.absolutePath

                        // Ensure standalone tokenizer is loaded for exact TikToken counting during host unit tests
                        resolvedTokenizer?.let { LlamaTikTokenTokenizer.loadFromFile(it) }

                        _state.value = InferenceState.Ready(
                            modelName = "${resolvedModel.name} (Deterministic Taxonomy Fallback)",
                            modelSizeBytes = resolvedModel.length(),
                            contextLength = activeProfile.maxContextLength,
                            isMmap = activeProfile.useMmap,
                            delegateBackend = "Offline Knowledge Fallback",
                            profileName = activeProfile.name,
                            estimatedKvCacheMb = activeProfile.estimatedKvCacheMb
                        )
                    }

                    Result.success(Unit)
                } catch (e: Exception) {
                    _state.value = InferenceState.Error(
                        errorMessage = e.message ?: "Unknown error initializing engine",
                        isLowMemoryError = e is OutOfMemoryError
                    )
                    Result.failure(e)
                }
            }
        }

    override fun generateStream(
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): Flow<String> = callbackFlow {
        if (prompt.isBlank()) {
            close(IllegalArgumentException("Prompt must contain at least 1 token"))
            return@callbackFlow
        }

        // Enforce sequential execution on low-memory profiles (Snapdragon 662)
        if (!isGenerating.compareAndSet(false, true)) {
            Log.w(TAG, "Concurrent generation rejected under sequential execution policy.")
            close(IllegalStateException("Active generation in progress. Sequential execution required."))
            return@callbackFlow
        }

        try {
            val budget = try {
                budgetCompletePrompt(prompt, requestedMaxTokens = maxTokens, minGenerationTokens = MIN_GENERATION_TOKENS)
            } catch (e: Exception) {
                _state.value = InferenceState.Error(
                    errorMessage = e.message ?: "Invalid prompt budget",
                    isLowMemoryError = false
                )
                close(e)
                return@callbackFlow
            }

            val startTime = System.currentTimeMillis()
            var firstTokenTime: Long = 0
            var tokensEmitted = 0

            // If native runner is not yet initialized but native library is available, lazily initialize
            if (nativeHandle == 0L && NativeLlamaBridge.isAvailable()) {
                val initRes = initialize(activeModelPath, activeTokenizerPath)
                if (initRes.isFailure) {
                    Log.w(TAG, "Lazy initialization of native runner failed: ${initRes.exceptionOrNull()?.message}")
                }
            }

            // If native runner is ready, execute streaming inference via JNI
            if (nativeHandle != 0L && NativeLlamaBridge.isAvailable()) {
                _state.value = InferenceState.Generating("", 0)

                val callback = NativeLlamaBridge.NativeTokenCallback { token ->
                    // Suppress and strip special token strings to prevent UI leakage
                    if (token == "<|eot_id|>" || token == "<|end_of_text|>" || token == "<|eom_id|>" || token == "<|begin_of_text|>") {
                        Log.d(TAG, "Suppressed special token from UI stream: $token")
                        return@NativeTokenCallback
                    }
                    val cleanedToken = token.replace("<|eot_id|>", "")
                        .replace("<|end_of_text|>", "")
                        .replace("<|eom_id|>", "")
                        .replace("<|begin_of_text|>", "")
                    if (cleanedToken.isEmpty()) {
                        return@NativeTokenCallback
                    }
                    if (firstTokenTime == 0L) {
                        firstTokenTime = System.currentTimeMillis()
                    }
                    tokensEmitted++
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0f
                    val tokPerSec = if (elapsedSec > 0) tokensEmitted / elapsedSec else 0.0f
                    _state.value = InferenceState.Generating(cleanedToken, tokensEmitted, tokPerSec)
                    Log.d(TAG, "NativeTokenCallback [$tokensEmitted]: \"$cleanedToken\"")
                    trySend(cleanedToken)
                }

                val resultCode = NativeLlamaBridge.nativeGenerate(
                    handle = nativeHandle,
                    prompt = budget.safePrompt,
                    maxTokens = budget.totalSeqLen,
                    temperature = temperature,
                    callback = callback
                )

                if (resultCode < 0) {
                    val errMessage = when (resultCode) {
                        -9 -> "Native generation rejected: prompt token count exceeds sequence capacity (P + G > 128)"
                        else -> "Native generation failed with error code $resultCode"
                    }
                    Log.e(TAG, errMessage)
                    _state.value = InferenceState.Error(
                        errorMessage = errMessage,
                        isLowMemoryError = (resultCode == -7 || resultCode == -8)
                    )
                    close(RuntimeException(errMessage))
                    return@callbackFlow
                }

                val totalDuration = System.currentTimeMillis() - startTime
                val ttft = if (firstTokenTime > 0) firstTokenTime - startTime else totalDuration
                val tokensPerSec = if (totalDuration > 0) (tokensEmitted * 1000.0f) / totalDuration else 0.0f
                Log.i(TAG, "Native inference completed successfully: promptTokens=${budget.promptTokens}, tokensEmitted=$tokensEmitted, totalMs=$totalDuration, ttftMs=$ttft, tokPerSec=$tokensPerSec, handle=$nativeHandle")

                latestBenchmarkStats = InferenceBenchmarkStats(
                    modelIdentifier = activeModelPath ?: "learnmate_llama3_2_3b.pte",
                    modelSizeBytes = activeModelPath?.let { File(it).length() } ?: 0L,
                    promptTokens = budget.promptTokens,
                    generatedTokens = tokensEmitted,
                    modelLoadTimeMs = 0L,
                    prefillLatencyMs = ttft,
                    timeToFirstTokenMs = ttft,
                    generationTimeMs = (totalDuration - ttft).coerceAtLeast(0L),
                    totalLatencyMs = totalDuration,
                    tokensPerSecond = tokensPerSec,
                    peakPssMb = try { Debug.getPss() / 1024 } catch (_: Throwable) { 0L },
                    memoryMapped = true,
                    xnnpackDelegated = true,
                    activeProfileName = profileManager.activeProfile.value.name
                )

                // Restore Ready state after generation completes
                val profile = profileManager.activeProfile.value
                _state.value = InferenceState.Ready(
                    modelName = activeModelPath?.let { File(it).name } ?: "learnmate_llama3_2_3b.pte",
                    modelSizeBytes = activeModelPath?.let { File(it).length() } ?: 0L,
                    contextLength = profile.maxContextLength,
                    isMmap = profile.useMmap,
                    delegateBackend = "XNNPACK (ARM NEON)",
                    profileName = profile.name,
                    estimatedKvCacheMb = profile.estimatedKvCacheMb
                )

                close()
            } else if (NativeLlamaBridge.isAvailable()) {
                val errMessage = "Local Llama 3.2 3B model failed to initialize. Verify extracted model files and retry."
                Log.e(TAG, errMessage)
                _state.value = InferenceState.Error(
                    errorMessage = errMessage,
                    isLowMemoryError = false
                )
                close(IllegalStateException(errMessage))
                return@callbackFlow
            } else {
                // Host JVM unit-test fallback when native JNI library is absent
                val fallbackResponse = synthesizeDeterministicResponse(budget.safePrompt)
                val words = fallbackResponse.split(" ")
                for (word in words) {
                    tokensEmitted++
                    trySend("$word ")
                    delay(25)
                }
                close()
            }
        } finally {
            isGenerating.set(false)
        }

        awaitClose {
            // Signal native runner to halt generation if flow is cancelled
            if (nativeHandle != 0L && NativeLlamaBridge.isAvailable()) {
                try {
                    NativeLlamaBridge.nativeStop(nativeHandle)
                } catch (e: Exception) {
                    Log.w(TAG, "Error invoking nativeStop on flow close: ${e.message}")
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Float
    ): String = withContext(Dispatchers.IO) {
        val builder = StringBuilder()
        generateStream(prompt, maxTokens, temperature).collect { token ->
            builder.append(token)
        }
        builder.toString()
    }

    private fun synthesizeDeterministicResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            Regex("\\b(hello|hi|hey)\\b").containsMatchIn(lower) ->
                "[Offline Knowledge Engine] Hello! I am your LearnMate AI Companion. " +
                "I operate offline using our structured curriculum taxonomy. Ask me about study topics, milestones, or key concepts!"

            Regex("\\b(roadmap|milestone|curriculum|plan)\\b").containsMatchIn(lower) ->
                "[Offline Knowledge Engine] Based on your diagnostic profile, your roadmap is divided into structured milestones. " +
                "Each phase focuses on bridging competencies where your target score exceeds your current self-assessment. " +
                "Dedicate your scheduled daily hours to high-yield practical exercises first."

            Regex("\\b(pointer|pointers|c programming|clang)\\b").containsMatchIn(lower) || (lower.contains("pointer") || Regex("\\bin c\\b").containsMatchIn(lower)) ->
                "[Offline Knowledge Engine] A pointer in C is a variable that stores the memory address of another variable. " +
                "Pointer arithmetic operates in units of the pointed-to type: if ptr points to a 4-byte integer at address 0x1000, " +
                "then (ptr + 1) points to address 0x1004. Always ensure pointers are initialized before dereferencing (*ptr)."

            Regex("\\b(accounting|ca|debit|credit|ledger|journal)\\b").containsMatchIn(lower) ->
                "[Offline Knowledge Engine] In double-entry bookkeeping, every financial transaction affects at least two accounts. " +
                "Golden rules: Debit what comes in, credit what goes out (Real Accounts); Debit the receiver, credit the giver (Personal Accounts); " +
                "Debit all expenses and losses, credit all incomes and gains (Nominal Accounts)."

            Regex("\\b(neet|mbbs|anatomy|medicine|medical|clinical)\\b").containsMatchIn(lower) ->
                "[Offline Knowledge Engine] For medical exam preparation, prioritize clinical correlations and high-yield physiological mechanisms. " +
                "Review organ system pathologies with anatomical cross-sections, and reinforce diagnostic criteria with daily question sets."

            else ->
                "[Offline Knowledge Engine] Pedagogical recommendation for '$prompt': " +
                "Deconstruct the objective into fundamental principles, apply deliberate practice for 45 minutes, " +
                "and verify retention through active recall."
        }
    }

    override fun stopGeneration() {
        if (nativeHandle != 0L && NativeLlamaBridge.isAvailable()) {
            try {
                NativeLlamaBridge.nativeStop(nativeHandle)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping generation: ${e.message}")
            }
        }
    }

    override fun unload() {
        if (nativeHandle != 0L && NativeLlamaBridge.isAvailable()) {
            try {
                NativeLlamaBridge.nativeRelease(nativeHandle)
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing native handle: ${e.message}")
            }
            nativeHandle = 0L
        }
        _state.value = InferenceState.Uninitialized
    }

    override fun getLatestBenchmark(): InferenceBenchmarkStats? = latestBenchmarkStats
}
