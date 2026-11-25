package com.learnmate.ai.domain.model

/**
 * Enumeration of on-device LLM inference profiles.
 * Distinguishes hardware capability classes while maintaining the invariant
 * that the model architecture (Meta Llama 3 8B Instruct) remains identical.
 */
enum class ProfileType {
    STANDARD,
    REDMI_LOW_MEMORY
}

/**
 * Configuration profile for on-device inference runtime.
 * Explicitly parameters context length, KV-cache footprint, thread counts,
 * and memory management policies according to physical RAM constraints.
 */
data class InferenceProfile(
    val type: ProfileType,
    val name: String,
    val targetDeviceDescription: String,
    val modelTarget: String = "Meta Llama 3 8B Instruct (INT4 8da4w)",
    val maxContextLength: Int,
    val estimatedKvCacheMb: Int,
    val numThreads: Int,
    val useMmap: Boolean = true,
    val memoryPressureThresholdMb: Long,
    val sequentialExecution: Boolean,
    val allowConcurrentRequests: Boolean = false,
    val notes: String
) {
    companion object {
        /**
         * STANDARD Profile:
         * Tailored for modern Android devices with >= 6GB RAM (e.g., Realme X7 5G with 8GB RAM).
         * Utilizes 512 token context window (128 MB float32 KV cache) and 4 threads for higher throughput.
         */
        val STANDARD = InferenceProfile(
            type = ProfileType.STANDARD,
            name = "Standard Profile (>= 6GB RAM)",
            targetDeviceDescription = "Android devices with >= 6GB physical RAM (e.g. Realme X7 5G, Dimensity 800U, Snapdragon 8/7 Series)",
            modelTarget = "Meta Llama 3 8B Instruct (INT4 8da4w)",
            maxContextLength = 512,
            estimatedKvCacheMb = 128,
            numThreads = 4,
            useMmap = true,
            memoryPressureThresholdMb = 400L,
            sequentialExecution = false,
            allowConcurrentRequests = true,
            notes = "Standard allocation with 512-token context window (128 MB float32 KV-cache) and 4-thread XNNPACK NEON microkernel execution."
        )

        /**
         * REDMI LOW-MEMORY Profile:
         * Tailored specifically for Xiaomi Redmi 9 Power (Snapdragon 662, 4GB LPDDR4X RAM, Android 12).
         * Strictly retains the SAME Meta Llama 3 8B Instruct model.
         * Restrains context length to 128 tokens to cap KV-cache memory at 32 MB,
         * configures 4-thread XNNPACK execution matching Snapdragon 662 quad cores,
         * enforces sequential request execution to eliminate concurrent memory peaks,
         * and hooks into OS trim-memory callbacks to avert LMK kills.
         */
        val REDMI_LOW_MEMORY = InferenceProfile(
            type = ProfileType.REDMI_LOW_MEMORY,
            name = "Redmi Low-Memory Profile (4GB RAM)",
            targetDeviceDescription = "Xiaomi Redmi 9 Power (Snapdragon 662, 4GB RAM, Android 12, Adreno 610)",
            modelTarget = "Meta Llama 3 8B Instruct (INT4 8da4w)",
            maxContextLength = 128,
            estimatedKvCacheMb = 32,
            numThreads = 4,
            useMmap = true,
            memoryPressureThresholdMb = 600L,
            sequentialExecution = true,
            allowConcurrentRequests = false,
            notes = "Constrained allocation: 128-token context window (32 MB float32 KV-cache), 4-thread execution, sequential decoding, aggressive memory trim listeners to prevent OS termination."
        )
    }
}
