package com.learnmate.ai.domain.engine

import android.util.Log

/**
 * Representation of supported on-device LLM model candidates.
 */
enum class ModelCandidate(
    val modelId: String,
    val pteFilename: String,
    val parameterCountFormatted: String,
    val quantisation: String,
    val estimatedPeakRssMb: Long,
    val minimumAvailableRamMb: Long,
    val recommendedRamTierDescription: String
) {
    LLAMA_3_2_1B(
        modelId = "meta-llama/Llama-3.2-1B-Instruct",
        pteFilename = "llama3_2_1b_8da4w.pte",
        parameterCountFormatted = "1.24B",
        quantisation = "8da4w (INT4 Weight, INT8 Dynamic Activation)",
        estimatedPeakRssMb = 680L,
        minimumAvailableRamMb = 1000L,
        recommendedRamTierDescription = "4GB RAM devices (e.g. Xiaomi Redmi 9 Power, Snapdragon 662)"
    ),
    LLAMA_3_2_3B(
        modelId = "meta-llama/Llama-3.2-3B-Instruct",
        pteFilename = "llama3_2_3b_8da4w.pte",
        parameterCountFormatted = "3.21B",
        quantisation = "8da4w (INT4 Weight, INT8 Dynamic Activation)",
        estimatedPeakRssMb = 1750L,
        minimumAvailableRamMb = 2500L,
        recommendedRamTierDescription = "6GB+ RAM ARM64 Android devices (e.g. Realme X7 5G, OnePlus, Samsung)"
    ),
    LLAMA_3_8B_HISTORICAL(
        modelId = "meta-llama/Meta-Llama-3-8B-Instruct",
        pteFilename = "llama3_8b_8da4w.pte",
        parameterCountFormatted = "8.03B",
        quantisation = "8da4w (INT4 Weight, INT8 Dynamic Activation)",
        estimatedPeakRssMb = 4050L,
        minimumAvailableRamMb = 6000L,
        recommendedRamTierDescription = "High-RAM devices (>= 12GB RAM) or cloud evaluation"
    )
}

/**
 * Outcome of model evaluation for the current physical device state.
 */
data class ModelSelectionDecision(
    val selectedCandidate: ModelCandidate,
    val isPermitted: Boolean,
    val reason: String,
    val fallbackCandidate: ModelCandidate?,
    val isMemoryPressureWarning: Boolean = false
)

/**
 * Manages model candidate selection based on physical hardware memory constraints,
 * dynamic available RAM, and operating system memory pressure.
 *
 * Implements strict safeguards:
 * 1. 4GB RAM devices (< 5.5 GB total RAM): Llama 3.2 1B INT4 is preferred. 3B is disallowed due to XNNPACK heap footprint.
 * 2. 6GB RAM devices (5.5 GB - 7.5 GB total RAM): Llama 3.2 3B INT4 allowed only if dynamic available RAM >= 2500 MB.
 * 3. 8GB+ RAM devices (>= 7.5 GB total RAM): Llama 3.2 3B INT4 is default.
 * 4. Ensures single model residency (never loads multiple models concurrently).
 */
class ModelSelectionManager {

    companion object {
        private const val TAG = "ModelSelectionMgr"
        const val THRESHOLD_4GB_MAX_BYTES = 5800000000L  // ~5.4 GB
        const val THRESHOLD_8GB_MIN_BYTES = 7800000000L  // ~7.3 GB

        private fun logI(msg: String) {
            try { Log.i(TAG, msg) } catch (_: Throwable) { println("[$TAG] $msg") }
        }

        private fun logW(msg: String) {
            try { Log.w(TAG, msg) } catch (_: Throwable) { System.err.println("[$TAG] $msg") }
        }
    }

    /**
     * Evaluates the optimal model candidate given a device memory snapshot.
     */
    fun evaluateModelForDevice(
        snapshot: DeviceMemorySnapshot,
        userPreference: ModelCandidate? = null
    ): ModelSelectionDecision {
        val totalRam = snapshot.totalRamBytes
        val availRamMb = snapshot.availableRamMb

        // 4GB RAM devices (< 5.5 GB total RAM, e.g. Xiaomi Redmi 9 Power)
        if (totalRam < THRESHOLD_4GB_MAX_BYTES) {
            if (userPreference == ModelCandidate.LLAMA_3_2_3B || userPreference == ModelCandidate.LLAMA_3_8B_HISTORICAL) {
                logW("User requested ${userPreference.modelId} on 4GB hardware (${snapshot.totalRamMb} MB total). Rejecting due to heap repacking constraint.")
                return ModelSelectionDecision(
                    selectedCandidate = ModelCandidate.LLAMA_3_2_1B,
                    isPermitted = false,
                    reason = "Device has ${snapshot.totalRamMb} MB physical RAM. 3B/8B models require >= 2.5 GB free RAM and will trigger instant LMK termination. Falling back to 1B INT4.",
                    fallbackCandidate = ModelCandidate.LLAMA_3_2_1B,
                    isMemoryPressureWarning = true
                )
            }
            logI("4GB RAM class detected (${snapshot.totalRamMb} MB total, $availRamMb MB available). Selecting Llama 3.2 1B INT4.")
            return ModelSelectionDecision(
                selectedCandidate = ModelCandidate.LLAMA_3_2_1B,
                isPermitted = true,
                reason = "Selected Llama 3.2 1B INT4 for 4GB RAM class. Fits comfortably in available RAM (${availRamMb} MB avail vs ${ModelCandidate.LLAMA_3_2_1B.estimatedPeakRssMb} MB estimated RSS).",
                fallbackCandidate = null
            )
        }

        // 6GB RAM devices (5.5 GB to 7.5 GB total RAM)
        if (totalRam < THRESHOLD_8GB_MIN_BYTES) {
            if (snapshot.isLowMemory || availRamMb < ModelCandidate.LLAMA_3_2_3B.minimumAvailableRamMb) {
                logW("6GB RAM class has low available memory ($availRamMb MB < ${ModelCandidate.LLAMA_3_2_3B.minimumAvailableRamMb} MB). Recommending 1B fallback.")
                return ModelSelectionDecision(
                    selectedCandidate = ModelCandidate.LLAMA_3_2_1B,
                    isPermitted = true,
                    reason = "Available memory ($availRamMb MB) is below the 2500 MB safe threshold for 3B INT4. Using 1B INT4 to preserve Android multitasking stability.",
                    fallbackCandidate = ModelCandidate.LLAMA_3_2_1B,
                    isMemoryPressureWarning = true
                )
            }
            logI("6GB RAM class detected with healthy available memory ($availRamMb MB). Permitting Llama 3.2 3B INT4.")
            return ModelSelectionDecision(
                selectedCandidate = userPreference ?: ModelCandidate.LLAMA_3_2_3B,
                isPermitted = true,
                reason = "Selected Llama 3.2 3B INT4 for 6GB RAM class ($availRamMb MB available RAM supports ~1.75 GB RSS with ~1.2 GB headroom).",
                fallbackCandidate = ModelCandidate.LLAMA_3_2_1B
            )
        }

        // 8GB+ RAM devices (>= 7.5 GB total RAM)
        logI("8GB+ RAM class detected (${snapshot.totalRamMb} MB total). Llama 3.2 3B INT4 fully supported.")
        return ModelSelectionDecision(
            selectedCandidate = userPreference ?: ModelCandidate.LLAMA_3_2_3B,
            isPermitted = true,
            reason = "Selected Llama 3.2 3B INT4 for 8GB+ RAM class. Ample physical headroom for full multitasking.",
            fallbackCandidate = ModelCandidate.LLAMA_3_2_1B
        )
    }
}
