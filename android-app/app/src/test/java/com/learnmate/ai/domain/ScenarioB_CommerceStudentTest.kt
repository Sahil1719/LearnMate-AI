package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario B Acceptance Test:
 * Commerce student preparing for CA Foundation.
 * Expected output: Accounting, business law, mathematics, economics, and exam preparation.
 * No engineering-specific skills should appear.
 */
class ScenarioB_CommerceStudentTest {

    @Test
    fun testCommerceStudentCAFoundationPathway() {
        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Priya",
            learnerType = "Junior college student",
            educationLevel = "Higher Secondary / Junior College (11th-12th)",
            educationStream = "Commerce and Finance",
            targetDomain = "Commerce and Finance",
            targetGoalTitle = "Prepare for CA Foundation",
            availableHoursPerDay = 3
        )

        val suggestedSkills = GoalBasedLearningEngine.suggestCompetenciesForGoal(
            domain = profile.targetDomain,
            customGoal = profile.targetGoalTitle
        )

        // Must contain commerce & CA Foundation competencies
        assertTrue(suggestedSkills.any { it.contains("Accounting", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Business Law", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Mathematics", ignoreCase = true) || it.contains("Statistics", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Economics", ignoreCase = true) })

        // MUST NOT contain engineering skills!
        assertFalse(suggestedSkills.any { it.contains("C Programming", ignoreCase = true) })
        assertFalse(suggestedSkills.any { it.contains("Microcontroller", ignoreCase = true) })
        assertFalse(suggestedSkills.any { it.contains("RTOS", ignoreCase = true) })

        val assessments = mapOf(
            "Principles & Practice of Accounting" to 2,
            "Business Law & Commercial Knowledge" to 1,
            "Business Mathematics & Statistics" to 0,
            "Business Economics" to -1 // Skipped
        )

        val pathway = GoalBasedLearningEngine.generatePathway(profile, assessments)

        assertEquals(3, pathway.phases.size)
        assertTrue(pathway.phases[0].title.contains("Accounting", ignoreCase = true))
        assertTrue(pathway.phases[1].title.contains("Economics", ignoreCase = true) || pathway.phases[1].title.contains("Quantitative", ignoreCase = true))
        assertTrue(pathway.phases[2].title.contains("Revision", ignoreCase = true) || pathway.phases[2].title.contains("Examination", ignoreCase = true))

        // Ensure no engineering tasks were generated
        for (task in pathway.initialDailyTasks) {
            assertFalse(task.title.contains("C Pointer", ignoreCase = true))
            assertFalse(task.title.contains("Microcontroller", ignoreCase = true))
        }
    }
}
