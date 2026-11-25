package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scenario C Acceptance Test:
 * Arts student wanting to learn digital illustration.
 * Expected output: Drawing fundamentals, composition, color theory, digital tools, practical exercises.
 */
class ScenarioC_ArtsStudentTest {

    @Test
    fun testArtsStudentDigitalIllustrationPathway() {
        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Ananya",
            learnerType = "Undergraduate student",
            educationLevel = "Undergraduate (Bachelor's)",
            educationStream = "Arts and Humanities",
            targetDomain = "Design and Creativity",
            targetGoalTitle = "Learn Digital Illustration",
            availableHoursPerDay = 2
        )

        val suggestedSkills = GoalBasedLearningEngine.suggestCompetenciesForGoal(
            domain = profile.targetDomain,
            customGoal = profile.targetGoalTitle
        )

        assertTrue(suggestedSkills.any { it.contains("Drawing", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Composition", ignoreCase = true) || it.contains("Perspective", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Color Theory", ignoreCase = true) })
        assertTrue(suggestedSkills.any { it.contains("Digital", ignoreCase = true) })

        assertFalse(suggestedSkills.any { it.contains("C Programming", ignoreCase = true) })

        val pathway = GoalBasedLearningEngine.generatePathway(profile, emptyMap())

        assertEquals(3, pathway.phases.size)
        assertTrue(pathway.phases[0].title.contains("Perspective", ignoreCase = true) || pathway.phases[0].title.contains("Form", ignoreCase = true))
        assertTrue(pathway.phases[1].title.contains("Lighting", ignoreCase = true) || pathway.phases[1].title.contains("Value", ignoreCase = true) || pathway.phases[1].title.contains("Color", ignoreCase = true))
        assertTrue(pathway.phases[2].title.contains("Character", ignoreCase = true) || pathway.phases[2].title.contains("Portfolio", ignoreCase = true))
    }
}
