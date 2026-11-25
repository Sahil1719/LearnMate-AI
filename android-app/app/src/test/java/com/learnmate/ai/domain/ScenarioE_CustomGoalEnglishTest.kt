package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario E Acceptance Test:
 * Custom goal: Improve English communication.
 * Expected output: Grammar, vocabulary, comprehension, speaking, communication competencies.
 */
class ScenarioE_CustomGoalEnglishTest {

    @Test
    fun testCustomGoalEnglishCommunicationPathway() {
        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Amit",
            learnerType = "Working professional",
            educationLevel = "Undergraduate (Bachelor's)",
            educationStream = "Engineering and Technology",
            targetDomain = "Languages and Communication",
            targetGoalTitle = "Improve English Communication",
            availableHoursPerDay = 1
        )

        val suggestedSkills = GoalBasedLearningEngine.suggestCompetenciesForGoal(
            domain = profile.targetDomain,
            customGoal = profile.targetGoalTitle
        )

        assertTrue(suggestedSkills.any { it.contains("Grammar", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Vocabulary", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Listening", ignoreCase = true) || it.contains("Pronunciation", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Speaking", ignoreCase = true) || it.contains("Fluency", ignoreCase = true) })

        assertFalse(suggestedSkills.any { it.contains("Microcontroller", ignoreCase = true) })

        val pathway = GoalBasedLearningEngine.generatePathway(profile, emptyMap())

        assertEquals(3, pathway.phases.size)
        assertTrue(pathway.phases[0].title.contains("Sentence", ignoreCase = true) || pathway.phases[0].title.contains("Vocabulary", ignoreCase = true))
        assertTrue(pathway.phases[1].title.contains("Listening", ignoreCase = true) || pathway.phases[1].title.contains("Fluency", ignoreCase = true))
        assertTrue(pathway.phases[2].title.contains("Public Speaking", ignoreCase = true) || pathway.phases[2].title.contains("Articulation", ignoreCase = true))
    }
}
