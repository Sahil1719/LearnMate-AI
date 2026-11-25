package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario F Acceptance Test:
 * Unusual or unsupported goal not present in predefined categories (e.g. Permaculture Gardening).
 * Expected output: Accepts goal, builds pathway, communicates limitations clearly.
 * MUST NOT silently replace the goal with an engineering pathway!
 */
class ScenarioF_UnsupportedCustomGoalTest {

    @Test
    fun testUnusualGoalBuildsTailoredPathwayWithoutEngineeringSubstitution() {
        val customGoal = "Permaculture Gardening & Swale Design"
        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Sunita",
            learnerType = "Self-learner",
            educationLevel = "Self-Taught / Professional",
            educationStream = "Other",
            targetDomain = "Other",
            targetGoalTitle = customGoal,
            availableHoursPerDay = 2
        )

        val suggestedSkills = GoalBasedLearningEngine.suggestCompetenciesForGoal(
            domain = profile.targetDomain,
            customGoal = profile.targetGoalTitle
        )

        // Must derive skills from the custom goal itself
        assertTrue(suggestedSkills.isNotEmpty())
        assertTrue(suggestedSkills.any { it.contains(customGoal) || it.contains("Principles") || it.contains("Methodologies") })

        // MUST NOT substitute with C Programming or Embedded Systems!
        assertFalse(suggestedSkills.any { it.contains("C Programming", ignoreCase = true) })
        assertFalse(suggestedSkills.any { it.contains("Microcontrollers", ignoreCase = true) })
        assertFalse(suggestedSkills.any { it.contains("RTOS", ignoreCase = true) })

        val pathway = GoalBasedLearningEngine.generatePathway(profile, emptyMap())

        assertEquals(3, pathway.phases.size)
        // Check that phases are customized to the custom goal
        assertTrue(pathway.phases[0].title.contains(customGoal) || pathway.phases[0].title.contains("Foundations"))
        assertTrue(pathway.phases[2].title.contains(customGoal) || pathway.phases[2].title.contains("Capstone"))

        // Ensure tasks do not contain engineering references
        for (task in pathway.initialDailyTasks) {
            assertFalse(task.title.contains("Bare-Metal", ignoreCase = true))
            assertFalse(task.title.contains("C Pointer", ignoreCase = true))
        }
    }
}
