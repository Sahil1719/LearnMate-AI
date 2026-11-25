package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario A Acceptance Test:
 * Engineering student aiming to become an Embedded Systems Engineer.
 * Expected output: C programming, electronics, microcontrollers, protocols and RTOS competencies.
 */
class ScenarioA_EngineeringStudentTest {

    @Test
    fun testEngineeringStudentEmbeddedSystemsPathway() {
        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Rohan",
            learnerType = "Undergraduate student",
            educationLevel = "Undergraduate (Bachelor's)",
            educationStream = "Engineering and Technology",
            targetDomain = "Engineering and Technology",
            targetGoalTitle = "Become an Embedded Systems Engineer",
            availableHoursPerDay = 3
        )

        // Suggest competencies
        val suggestedSkills = GoalBasedLearningEngine.suggestCompetenciesForGoal(
            domain = profile.targetDomain,
            customGoal = profile.targetGoalTitle
        )

        assertTrue(suggestedSkills.any { it.contains("C Programming", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Microcontrollers", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("RTOS", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Digital Electronics", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Protocols", ignoreCase = true) })

        // Generate pathway with self-assessment (C: 2, Digital Electronics: 3, RTOS: 0, Microcontrollers: -1 [Not assessed])
        val assessments = mapOf(
            "C Programming" to 2,
            "Digital Electronics" to 3,
            "RTOS" to 0,
            "Microcontrollers" to -1 // Skipped
        )

        val pathway = GoalBasedLearningEngine.generatePathway(profile, assessments)

        assertEquals(3, pathway.phases.size)
        assertTrue(pathway.phases[0].title.contains("Hardware Foundations", ignoreCase = true) || pathway.phases[0].title.contains("C Mastery", ignoreCase = true))
        assertTrue(pathway.phases[2].title.contains("RTOS", ignoreCase = true))

        // Verify unassessed Microcontrollers skill
        val mcSkill = pathway.skills.first { it.skillName == "Microcontrollers" }
        assertEquals(-1, mcSkill.currentCompetency)
        assertEquals(mcSkill.targetCompetency, mcSkill.calculatedGap)
        assertEquals(false, mcSkill.isAssessed)
    }
}
