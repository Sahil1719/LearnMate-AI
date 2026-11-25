package com.learnmate.ai.domain.engine

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.learnmate.ai.domain.model.InferenceProfile
import com.learnmate.ai.domain.model.ProfileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of device physical memory and operational thresholds.
 */
data class DeviceMemorySnapshot(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val memoryThresholdBytes: Long,
    val isLowMemory: Boolean,
    val totalRamMb: Long = totalRamBytes / (1024 * 1024),
    val availableRamMb: Long = availableRamBytes / (1024 * 1024),
    val thresholdMb: Long = memoryThresholdBytes / (1024 * 1024)
)

/**
 * Manages runtime inference profiles and dynamically adapts execution parameters
 * to device memory constraints. Listens to Android system low-memory events (ComponentCallbacks2)
 * to proactively avert low-memory-killer (LMK) termination.
 */
class InferenceProfileManager(
    private val context: Context? = null,
    totalRamOverrideBytes: Long? = null
) : ComponentCallbacks2 {

    companion object {
        private const val TAG = "InferenceProfileMgr"
        /**
         * 5.5 GB boundary to distinguish 6GB+ RAM devices (e.g. Realme X7 5G 8GB)
         * from 4GB devices (e.g. Xiaomi Redmi 9 Power 4GB).
         */
        const val HIGH_MEMORY_RAM_THRESHOLD_BYTES = 5800000000L // ~5.4 GB

        private fun logI(tag: String, msg: String) {
            try { Log.i(tag, msg) } catch (_: Throwable) { println("[$tag] $msg") }
        }

        private fun logW(tag: String, msg: String) {
            try { Log.w(tag, msg) } catch (_: Throwable) { System.err.println("[$tag] $msg") }
        }

        private fun logE(tag: String, msg: String) {
            try { Log.e(tag, msg) } catch (_: Throwable) { System.err.println("[$tag] $msg") }
        }
    }

    private val _currentSnapshot = MutableStateFlow(getMemorySnapshot(totalRamOverrideBytes))
    val currentSnapshot: StateFlow<DeviceMemorySnapshot> = _currentSnapshot.asStateFlow()

    private val _activeProfile = MutableStateFlow(determineProfile(_currentSnapshot.value.totalRamBytes))
    val activeProfile: StateFlow<InferenceProfile> = _activeProfile.asStateFlow()

    private var memoryPressureListener: ((Int) -> Unit)? = null

    init {
        context?.registerComponentCallbacks(this)
    }

    /**
     * Registers a callback invoked when OS memory pressure is signaled.
     * Receives ComponentCallbacks2 trim level constants.
     */
    fun setMemoryPressureListener(listener: (Int) -> Unit) {
        this.memoryPressureListener = listener
    }

    /**
     * Refreshes dynamic available memory and threshold statistics from the kernel.
     */
    fun refreshMemorySnapshot(): DeviceMemorySnapshot {
        val snapshot = getMemorySnapshot(null)
        _currentSnapshot.value = snapshot
        return snapshot
    }

    /**
     * Manually overrides the active inference profile for testing or evaluation.
     */
    fun forceProfile(profile: InferenceProfile) {
        logI(TAG, "Manually overriding active inference profile to: ${profile.name}")
        _activeProfile.value = profile
    }

    /**
     * Automatically determines the optimal profile based on physical RAM capacity.
     */
    fun determineProfile(totalRamBytes: Long): InferenceProfile {
        return if (totalRamBytes >= HIGH_MEMORY_RAM_THRESHOLD_BYTES) {
            logI(TAG, "Total physical RAM (${totalRamBytes / (1024 * 1024)} MB) >= 5.5 GB: Selecting STANDARD profile")
            InferenceProfile.STANDARD
        } else {
            logI(TAG, "Total physical RAM (${totalRamBytes / (1024 * 1024)} MB) < 5.5 GB: Selecting REDMI_LOW_MEMORY profile")
            InferenceProfile.REDMI_LOW_MEMORY
        }
    }

    private fun getMemorySnapshot(totalRamOverride: Long?): DeviceMemorySnapshot {
        if (totalRamOverride != null) {
            return DeviceMemorySnapshot(
                totalRamBytes = totalRamOverride,
                availableRamBytes = totalRamOverride / 3,
                memoryThresholdBytes = totalRamOverride / 8,
                isLowMemory = false
            )
        }

        if (context == null) {
            // Default baseline for headless execution and unit tests: assume 4GB device (Redmi 9 Power)
            val assumed4Gb = 3900000000L
            return DeviceMemorySnapshot(
                totalRamBytes = assumed4Gb,
                availableRamBytes = 1400000000L,
                memoryThresholdBytes = 400000000L,
                isLowMemory = false
            )
        }

        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            DeviceMemorySnapshot(
                totalRamBytes = memInfo.totalMem,
                availableRamBytes = memInfo.availMem,
                memoryThresholdBytes = memInfo.threshold,
                isLowMemory = memInfo.lowMemory
            )
        } catch (e: Throwable) {
            logW(TAG, "Failed to query ActivityManager.MemoryInfo: ${e.message}")
            DeviceMemorySnapshot(3900000000L, 1400000000L, 400000000L, false)
        }
    }

    override fun onTrimMemory(level: Int) {
        logW(TAG, "System onTrimMemory callback received with level: $level")
        refreshMemorySnapshot()
        memoryPressureListener?.invoke(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No-op
    }

    @Deprecated("Deprecated in Java", ReplaceWith("onTrimMemory"))
    override fun onLowMemory() {
        logE(TAG, "CRITICAL: System onLowMemory invoked by Android kernel!")
        refreshMemorySnapshot()
        memoryPressureListener?.invoke(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    /**
     * Unregisters system component callbacks to prevent memory leaks.
     */
    fun release() {
        try {
            context?.unregisterComponentCallbacks(this)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
