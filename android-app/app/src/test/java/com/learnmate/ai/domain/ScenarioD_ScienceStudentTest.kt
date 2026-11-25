package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario D Acceptance Test:
 * Science student preparing for NEET.
 * Expected output: Biology, chemistry, physics, and exam preparation.
 */
class ScenarioD_ScienceStudentTest {

    @Test
    fun testScienceStudentNEETPathway() {
        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Kavya",
            learnerType = "Junior college student",
            educationLevel = "Higher Secondary / Junior College (11th-12th)",
            educationStream = "Science (PCM / PCB)",
            targetDomain = "Competitive Examinations",
            targetGoalTitle = "Prepare for NEET",
            availableHoursPerDay = 4
        )

        val suggestedSkills = GoalBasedLearningEngine.suggestCompetenciesForGoal(
            domain = profile.targetDomain,
            customGoal = profile.targetGoalTitle
        )

        assertTrue(suggestedSkills.any { it.contains("Physiology", ignoreCase = true) || it.contains("Biology", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Chemistry", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Physics", ignoreCase = true) })

        assertFalse(suggestedSkills.any { it.contains("C Programming", ignoreCase = true) })
        assertFalse(suggestedSkills.any { it.contains("Microcontroller", ignoreCase = true) })

        val pathway = GoalBasedLearningEngine.generatePathway(profile, emptyMap())

        assertEquals(3, pathway.phases.size)
        assertTrue(pathway.phases[0].title.contains("Physiology", ignoreCase = true) || pathway.phases[0].title.contains("Bonding", ignoreCase = true))
        assertTrue(pathway.phases[1].title.contains("Genetics", ignoreCase = true) || pathway.phases[1].title.contains("Organic", ignoreCase = true))
        assertTrue(pathway.phases[2].title.contains("Drills", ignoreCase = true) || pathway.phases[2].title.contains("Mocks", ignoreCase = true))
    }
}
