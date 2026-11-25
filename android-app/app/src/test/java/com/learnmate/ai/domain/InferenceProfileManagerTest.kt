package com.learnmate.ai.domain

import android.content.ComponentCallbacks2
import com.learnmate.ai.domain.engine.ExecuTorchLlmEngine
import com.learnmate.ai.domain.engine.InferenceProfileManager
import com.learnmate.ai.domain.model.InferenceProfile
import com.learnmate.ai.domain.model.InferenceState
import com.learnmate.ai.domain.model.ProfileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying inference profile selection, memory snapshot tracking,
 * and low-memory pressure handling.
 */
@Suppress("DEPRECATION")
class InferenceProfileManagerTest {

    @Test
    fun testAutomaticProfileSelection_SelectsRedmiLowMemoryFor4Gb() {
        // 4GB RAM device (e.g. 4 * 1024 * 1024 * 1024 = 4294967296 bytes)
        val manager = InferenceProfileManager(totalRamOverrideBytes = 4000000000L)
        val profile = manager.activeProfile.value

        assertEquals(ProfileType.REDMI_LOW_MEMORY, profile.type)
        assertEquals(128, profile.maxContextLength)
        assertEquals(32, profile.estimatedKvCacheMb)
        assertEquals(4, profile.numThreads)
        assertTrue("Redmi profile must enforce sequential execution", profile.sequentialExecution)
        assertFalse("Redmi profile must disallow concurrent requests", profile.allowConcurrentRequests)
        assertTrue("Redmi profile must use mmap", profile.useMmap)
    }

    @Test
    fun testAutomaticProfileSelection_SelectsStandardFor8Gb() {
        // 8GB RAM device (e.g. Realme X7 5G with 8GB RAM = ~8589934592 bytes)
        val manager = InferenceProfileManager(totalRamOverrideBytes = 8000000000L)
        val profile = manager.activeProfile.value

        assertEquals(ProfileType.STANDARD, profile.type)
        assertEquals(512, profile.maxContextLength)
        assertEquals(128, profile.estimatedKvCacheMb)
        assertEquals(4, profile.numThreads)
        assertFalse("Standard profile allows concurrent execution", profile.sequentialExecution)
        assertTrue("Standard profile allows concurrent requests", profile.allowConcurrentRequests)
        assertTrue("Standard profile must use mmap", profile.useMmap)
    }

    @Test
    fun testDefaultFallbackWithoutContext_SelectsRedmiProfileSafely() {
        val manager = InferenceProfileManager()
        val profile = manager.activeProfile.value
        assertEquals("Headless or test environments must default to low-memory profile", ProfileType.REDMI_LOW_MEMORY, profile.type)
    }

    @Test
    fun testForceProfile_OverridesAutomaticSelection() {
        val manager = InferenceProfileManager(totalRamOverrideBytes = 4000000000L)
        assertEquals(ProfileType.REDMI_LOW_MEMORY, manager.activeProfile.value.type)

        manager.forceProfile(InferenceProfile.STANDARD)
        assertEquals(ProfileType.STANDARD, manager.activeProfile.value.type)
        assertEquals(512, manager.activeProfile.value.maxContextLength)
    }

    @Test
    fun testMemoryPressureListener_InvokedOnTrimMemory() {
        val manager = InferenceProfileManager(totalRamOverrideBytes = 4000000000L)
        var capturedLevel = -1
        manager.setMemoryPressureListener { level ->
            capturedLevel = level
        }

        manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        assertEquals(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL, capturedLevel)
    }

    @Test
    fun testMemoryPressureListener_InvokedOnLowMemory() {
        val manager = InferenceProfileManager(totalRamOverrideBytes = 4000000000L)
        var capturedLevel = -1
        manager.setMemoryPressureListener { level ->
            capturedLevel = level
        }

        manager.onLowMemory()
        assertEquals(ComponentCallbacks2.TRIM_MEMORY_COMPLETE, capturedLevel)
    }

    @Test
    fun testEngineIntegration_ExposesProfileManagerAndRespectsProfile() {
        val customManager = InferenceProfileManager(totalRamOverrideBytes = 4000000000L)
        val engine = ExecuTorchLlmEngine(profileManager = customManager)

        assertNotNull(engine.profileManager)
        assertEquals(ProfileType.REDMI_LOW_MEMORY, engine.profileManager.activeProfile.value.type)
        assertEquals(128, engine.profileManager.activeProfile.value.maxContextLength)
    }
}
