package com.learnmate.ai.domain

import com.learnmate.ai.domain.engine.DeviceMemorySnapshot
import com.learnmate.ai.domain.engine.ModelCandidate
import com.learnmate.ai.domain.engine.ModelSelectionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying ModelSelectionManager logic across device RAM tiers:
 * - 4GB RAM (e.g. Xiaomi Redmi 9 Power)
 * - 6GB RAM (healthy vs constrained available RAM)
 * - 8GB+ RAM (e.g. Realme X7 5G 8GB)
 */
class ModelSelectionManagerTest {

    private lateinit var manager: ModelSelectionManager

    @Before
    fun setUp() {
        manager = ModelSelectionManager()
    }

    @Test
    fun test4GbDevice_SelectsLlama32_1B() {
        // Redmi 9 Power: ~3.8 GB total RAM, ~1.34 GB available
        val snapshot = DeviceMemorySnapshot(
            totalRamBytes = 3900000000L,
            availableRamBytes = 1340000000L,
            memoryThresholdBytes = 400000000L,
            isLowMemory = false
        )

        val decision = manager.evaluateModelForDevice(snapshot)
        assertEquals(ModelCandidate.LLAMA_3_2_1B, decision.selectedCandidate)
        assertTrue(decision.isPermitted)
        assertFalse(decision.isMemoryPressureWarning)
    }

    @Test
    fun test4GbDevice_Rejects3BPreferenceWith1BFallback() {
        // Attempting to force 3B on 4GB device
        val snapshot = DeviceMemorySnapshot(
            totalRamBytes = 3900000000L,
            availableRamBytes = 1340000000L,
            memoryThresholdBytes = 400000000L,
            isLowMemory = false
        )

        val decision = manager.evaluateModelForDevice(snapshot, userPreference = ModelCandidate.LLAMA_3_2_3B)
        assertEquals(ModelCandidate.LLAMA_3_2_1B, decision.selectedCandidate)
        assertFalse("3B must not be permitted on 4GB RAM", decision.isPermitted)
        assertTrue("Must indicate memory pressure warning", decision.isMemoryPressureWarning)
        assertEquals(ModelCandidate.LLAMA_3_2_1B, decision.fallbackCandidate)
    }

    @Test
    fun test6GbDevice_HealthyAvailableRam_Allows3B() {
        // 6GB device with 3.2 GB available
        val snapshot = DeviceMemorySnapshot(
            totalRamBytes = 6400000000L,
            availableRamBytes = 3200000000L,
            memoryThresholdBytes = 600000000L,
            isLowMemory = false
        )

        val decision = manager.evaluateModelForDevice(snapshot)
        assertEquals(ModelCandidate.LLAMA_3_2_3B, decision.selectedCandidate)
        assertTrue(decision.isPermitted)
        assertFalse(decision.isMemoryPressureWarning)
    }

    @Test
    fun test6GbDevice_LowAvailableRam_FallsBackTo1B() {
        // 6GB device with only 1.8 GB available (< 2.5 GB minimum)
        val snapshot = DeviceMemorySnapshot(
            totalRamBytes = 6400000000L,
            availableRamBytes = 1800000000L,
            memoryThresholdBytes = 600000000L,
            isLowMemory = false
        )

        val decision = manager.evaluateModelForDevice(snapshot)
        assertEquals(ModelCandidate.LLAMA_3_2_1B, decision.selectedCandidate)
        assertTrue(decision.isPermitted)
        assertTrue("Should warn about memory pressure on 6GB device with low free RAM", decision.isMemoryPressureWarning)
    }

    @Test
    fun test8GbDevice_Allows3BDefault() {
        // 8GB device
        val snapshot = DeviceMemorySnapshot(
            totalRamBytes = 8500000000L,
            availableRamBytes = 4500000000L,
            memoryThresholdBytes = 800000000L,
            isLowMemory = false
        )

        val decision = manager.evaluateModelForDevice(snapshot)
        assertEquals(ModelCandidate.LLAMA_3_2_3B, decision.selectedCandidate)
        assertTrue(decision.isPermitted)
        assertFalse(decision.isMemoryPressureWarning)
    }
}
