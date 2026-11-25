package com.learnmate.ai.domain

import com.learnmate.ai.domain.engine.SkillGapEngine
import com.learnmate.ai.domain.knowledge.CareerKnowledgeBase
import com.learnmate.ai.domain.model.GapPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllCareerPathwaysSkillGapTest {

    @Test
    fun testAllSevenPathwaysExistAndHavePhases() {
        val expectedPathways = listOf(
            "embedded_systems",
            "ai_ml",
            "data_analyst",
            "full_stack",
            "cybersecurity",
            "vlsi_design",
            "cloud_engineer"
        )

        for (pathwayId in expectedPathways) {
            val pathway = CareerKnowledgeBase.getPathway(pathwayId)
            assertNotNull("Pathway $pathwayId should exist in knowledge base", pathway)
            assertTrue("Pathway $pathwayId must have required skills", pathway!!.requiredSkills.isNotEmpty())
            assertTrue("Pathway $pathwayId must have roadmap phases", pathway.phases.isNotEmpty())
        }
    }

    @Test
    fun testNoviceLearnerAllSkillsZeroGapsEqualTargets() {
        val pathway = CareerKnowledgeBase.getPathway("ai_ml")!!
        val zeroSkills = emptyMap<String, Int>()

        val gaps = SkillGapEngine.calculateGaps("ai_ml", zeroSkills)

        assertEquals(pathway.requiredSkills.size, gaps.size)
        for (gap in gaps) {
            val expectedTarget = pathway.requiredSkills[gap.skillName]!!
            assertEquals(expectedTarget, gap.gap)
            assertEquals(0, gap.currentLevel)
            assertEquals(expectedTarget, gap.targetLevel)
        }
    }

    @Test
    fun testExpertLearnerAllSkillsMaximizedZeroGaps() {
        val pathway = CareerKnowledgeBase.getPathway("embedded_systems")!!
        val maxSkills = pathway.requiredSkills.mapValues { 5 }

        val gaps = SkillGapEngine.calculateGaps("embedded_systems", maxSkills)

        for (gap in gaps) {
            assertEquals(0, gap.gap)
            assertEquals(GapPriority.NONE, gap.priority)
            assertTrue(gap.isMastered)
        }
    }

    @Test
    fun testUnknownPathwayReturnsEmptyListGracefully() {
        val gaps = SkillGapEngine.calculateGaps("quantum_computing_invalid", emptyMap())
        assertTrue("Unknown pathway should return empty list without crashing", gaps.isEmpty())
    }

    @Test
    fun testNegativeAndOutOfBoundsCompetencyCoercion() {
        // Values outside [0, 5] should be coerced cleanly
        val dirtySkills = mapOf(
            "C Programming" to -10,
            "Python" to 100
        )
        val gaps = SkillGapEngine.calculateGaps("embedded_systems", dirtySkills)

        val cGap = gaps.first { it.skillName == "C Programming" }
        assertEquals("Negative competency should be coerced to 0", 0, cGap.currentLevel)
        assertEquals(4, cGap.gap)

        val pyGap = gaps.first { it.skillName == "Python" }
        assertEquals("Super-high competency should be coerced to 5", 5, pyGap.currentLevel)
        assertEquals("Gap must be 0 when current >= target", 0, pyGap.gap)
    }
}
