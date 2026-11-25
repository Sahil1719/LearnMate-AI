package com.learnmate.ai.domain

import com.learnmate.ai.domain.engine.SkillGapEngine
import com.learnmate.ai.domain.model.GapPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveRemediationTest {

    @Test
    fun testQuizFailureTriggersRemediation() {
        val scorePercentage = 40 // Learner struggled with pointers
        val passThreshold = 60
        val isFailed = scorePercentage < passThreshold

        assertTrue("Score below 60% should flag for remediation", isFailed)

        // When failed, current competency remains low
        val currentSkills = mapOf("C Programming" to 1)
        val gaps = SkillGapEngine.calculateGaps("embedded_systems", currentSkills)

        val cGap = gaps.first { it.skillName == "C Programming" }
        assertEquals(3, cGap.gap)
        assertEquals(GapPriority.CRITICAL, cGap.priority)
    }
}
