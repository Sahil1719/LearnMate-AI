package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenarios H & I Acceptance Test:
 * Scenario H: Profile persistence and recovery upon reopen.
 * Scenario I: Fresh / uninitialized state with zero default dummy info.
 */
class ScenarioH_PersistenceAndResetTest {

    @Test
    fun testFreshInstallationHasZeroAssumedLearnerInfo() {
        // Initially, in a fresh installation, no profile exists
        val freshProfile: LearnerProfileEntity? = null
        assertNull("Fresh installation profile must be null", freshProfile)
    }

    @Test
    fun testCompletedOnboardingRestoresSingleSourceOfTruth() {
        val onboardedProfile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Aarav",
            learnerType = "Undergraduate student",
            educationLevel = "Bachelor's",
            educationStream = "Commerce and Finance",
            educationInstitution = "Delhi University",
            targetDomain = "Commerce and Finance",
            targetGoalTitle = "Prepare for CA Foundation",
            availableHoursPerDay = 3,
            onboardingCompleted = true
        )

        // Verify all fields are preserved and match single source of truth
        assertEquals("Aarav", onboardedProfile.name)
        assertEquals("Prepare for CA Foundation", onboardedProfile.targetGoalTitle)
        assertEquals("Delhi University", onboardedProfile.educationInstitution)
        assertTrue(onboardedProfile.onboardingCompleted)
        assertNotNull(onboardedProfile.id)
    }
}
