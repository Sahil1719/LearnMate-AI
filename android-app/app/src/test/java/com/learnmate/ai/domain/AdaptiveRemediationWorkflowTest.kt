package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.StudyTaskEntity
import com.learnmate.ai.domain.engine.SkillGapEngine
import com.learnmate.ai.domain.knowledge.CareerKnowledgeBase
import com.learnmate.ai.domain.model.GapPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveRemediationWorkflowTest {

    @Test
    fun testAdaptiveRemediationTaskCreation() {
        val strugglingTopic = "Pointer Arithmetic & Memory Leaks"
        val relatedSkill = "C Programming"

        val remediationTask = StudyTaskEntity(
            id = "remedy_test_1",
            title = "Remediation & Review: $strugglingTopic",
            relatedSkill = relatedSkill,
            durationMinutes = 45,
            scheduledDate = "Immediate Revision",
            isCompleted = false,
            isRevisionTask = true,
            orderIndex = 0
        )

        assertTrue(remediationTask.isRevisionTask)
        assertEquals("Immediate Revision", remediationTask.scheduledDate)
        assertEquals(0, remediationTask.orderIndex)
        assertFalse(remediationTask.isCompleted)
    }

    @Test
    fun testPassingScoreUpdatesCompetencyAndReducesGap() {
        // Initial state
        val initialSkills = mapOf("C Programming" to 2)
        val initialGaps = SkillGapEngine.calculateGaps("embedded_systems", initialSkills)
        val initialCGap = initialGaps.first { it.skillName == "C Programming" }
        assertEquals(2, initialCGap.gap) // Target is 4, 4 - 2 = 2

        // After successful assessment (e.g. score >= 80%), skill competency increases to 4
        val updatedSkills = mapOf("C Programming" to 4)
        val updatedGaps = SkillGapEngine.calculateGaps("embedded_systems", updatedSkills)
        val updatedCGap = updatedGaps.first { it.skillName == "C Programming" }

        assertEquals(0, updatedCGap.gap)
        assertEquals(GapPriority.NONE, updatedCGap.priority)
        assertTrue(updatedCGap.isMastered)
    }

    @Test
    fun testPrerequisiteSatisfactionUnlocksNextPhase() {
        // Microcontrollers requires C Programming and Digital Electronics
        val pathway = CareerKnowledgeBase.getPathway("embedded_systems")!!
        val prereqs = pathway.prerequisiteGraph["Microcontrollers"]!!

        assertEquals(listOf("C Programming", "Digital Electronics"), prereqs)

        // Learner has mastered both prerequisites
        val learnerSkills = mapOf(
            "C Programming" to 4,
            "Digital Electronics" to 3
        )

        val arePrereqsSatisfied = prereqs.all { prereqName ->
            val current = learnerSkills[prereqName] ?: 0
            val target = pathway.requiredSkills[prereqName] ?: 0
            current >= target
        }

        assertTrue("Prerequisites should be fully satisfied", arePrereqsSatisfied)
    }
}
