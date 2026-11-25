package com.learnmate.ai.domain.engine

import com.learnmate.ai.domain.model.InferenceBenchmarkStats
import com.learnmate.ai.domain.model.InferenceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean architectural abstraction for On-Device LLM Inference.
 * Ensures zero cloud dependency, asynchronous execution, and deterministic fallback.
 */
interface LlmInferenceEngine {
    /** Current lifecycle state observable by the UI. */
    val state: StateFlow<InferenceState>

    /**
     * Initializes the model and tokenizer from on-device paths.
     * Searches standard directories if paths are null or empty.
     */
    suspend fun initialize(modelPath: String? = null, tokenizerPath: String? = null): Result<Unit>

    /**
     * Streams generated tokens one by one as they are emitted by the runtime.
     * Guaranteed off-main-thread execution.
     */
    fun generateStream(
        prompt: String,
        maxTokens: Int = 128,
        temperature: Float = 0.7f
    ): Flow<String>

    /**
     * Complete non-streaming generation response.
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = 128,
        temperature: Float = 0.7f
    ): String

    /**
     * Cancels active generation if running.
     */
    fun stopGeneration()

    /**
     * Releases model buffers and native memory.
     */
    fun unload()

    /**
     * Returns the most recent empirical benchmark statistics, or null if unrun.
     */
    fun getLatestBenchmark(): InferenceBenchmarkStats?
}
