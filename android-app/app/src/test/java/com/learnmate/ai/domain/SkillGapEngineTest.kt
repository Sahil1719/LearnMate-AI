package com.learnmate.ai.domain

import com.learnmate.ai.domain.engine.SkillGapEngine
import com.learnmate.ai.domain.model.GapPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillGapEngineTest {

    @Test
    fun testEmbeddedSystemsSkillGapCalculation() {
        // Section 10 example learner profile:
        // C: 3, Python: 2, Digital Electronics: 4, Microcontrollers: 1, RTOS: 0
        val currentSkills = mapOf(
            "C Programming" to 3,
            "Python" to 2,
            "Digital Electronics" to 4,
            "Microcontrollers" to 1,
            "RTOS" to 0
        )

        val gaps = SkillGapEngine.calculateGaps("embedded_systems", currentSkills)

        // Verify C Programming: Target 4, Current 3 -> Gap = 1
        val cGap = gaps.firstOrNull { it.skillName == "C Programming" }
        assertEquals(1, cGap?.gap)

        // Verify RTOS: Target 3, Current 0 -> Gap = 3 (Critical)
        val rtosGap = gaps.firstOrNull { it.skillName == "RTOS" }
        assertEquals(3, rtosGap?.gap)
        assertEquals(GapPriority.CRITICAL, rtosGap?.priority)

        // Verify Digital Electronics: Target 3, Current 4 -> Gap = max(0, 3 - 4) = 0
        val deGap = gaps.firstOrNull { it.skillName == "Digital Electronics" }
        assertEquals(0, deGap?.gap)
        assertTrue(deGap?.isMastered == true)

        // Verify Microcontrollers: Target 4, Current 1 -> Gap = 3 (Critical, prerequisite for RTOS)
        val mcGap = gaps.firstOrNull { it.skillName == "Microcontrollers" }
        assertEquals(3, mcGap?.gap)
        assertEquals(GapPriority.CRITICAL, mcGap?.priority)
    }
}
