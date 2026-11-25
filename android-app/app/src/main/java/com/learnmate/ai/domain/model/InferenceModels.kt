package com.learnmate.ai.domain.model

/**
 * On-Device LLM Inference Lifecycle State.
 * Transparently conveys model loading, execution, and memory status to the UI.
 */
sealed class InferenceState {
    /** Initial state: no model has been loaded yet. */
    object Uninitialized : InferenceState()

    /** Model artifact not found at expected on-device paths. */
    data class ModelMissing(
        val expectedModelPath: String,
        val searchedPaths: List<String>,
        val advice: String = "Stage exported .pte model to phone storage (e.g. /sdcard/Download/ or /data/local/tmp/llama/)"
    ) : InferenceState()

    /** Model is actively loading (memory-mapping weights, initializing KV-cache and tokenizer). */
    data class Loading(
        val modelPath: String,
        val progressPercent: Int = 0,
        val statusMessage: String = "Memory-mapping model weights..."
    ) : InferenceState()

    /** Model and tokenizer are successfully loaded into memory and ready for queries. */
    data class Ready(
        val modelName: String,
        val modelSizeBytes: Long,
        val contextLength: Int,
        val isMmap: Boolean = true,
        val delegateBackend: String = "XNNPACK (ARM NEON)",
        val profileName: String = "Redmi Low-Memory (4GB RAM)",
        val estimatedKvCacheMb: Int = 32
    ) : InferenceState()

    /** Active token generation in progress. */
    data class Generating(
        val partialResponse: String,
        val tokensGeneratedSoFar: Int,
        val instantaneousTokensPerSec: Float = 0.0f
    ) : InferenceState()

    /** Error state (e.g., OOM, invalid model file, unaligned tensor). */
    data class Error(
        val errorMessage: String,
        val isLowMemoryError: Boolean = false,
        val recoveryAction: String = "Free system memory or restart application."
    ) : InferenceState()
}

/**
 * Empirical Benchmark Statistics for On-Device Inference.
 * Strictly adheres to Rule 2 (Zero Fabricated Benchmarks).
 */
data class InferenceBenchmarkStats(
    val modelIdentifier: String,
    val modelSizeBytes: Long,
    val promptTokens: Int,
    val generatedTokens: Int,
    val modelLoadTimeMs: Long,
    val prefillLatencyMs: Long,
    val timeToFirstTokenMs: Long,
    val generationTimeMs: Long,
    val totalLatencyMs: Long,
    val tokensPerSecond: Float,
    val peakPssMb: Long = 0,
    val peakRssMb: Long = 0,
    val isColdStart: Boolean = true,
    val memoryMapped: Boolean = true,
    val xnnpackDelegated: Boolean = true,
    val activeProfileName: String = "Redmi Low-Memory (4GB RAM)"
)
