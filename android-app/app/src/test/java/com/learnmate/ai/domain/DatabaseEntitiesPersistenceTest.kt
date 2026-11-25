package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.AssessmentRecordEntity
import com.learnmate.ai.data.local.entities.ChatMessageEntity
import com.learnmate.ai.data.local.entities.ConversationEntity
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.data.local.entities.RoadmapPhaseEntity
import com.learnmate.ai.data.local.entities.SkillEntity
import com.learnmate.ai.data.local.entities.StudyTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseEntitiesPersistenceTest {

    @Test
    fun testLearnerProfileEntityIntegrity() {
        val profile = LearnerProfileEntity(
            id = "primary_learner",
            name = "Maya",
            learnerType = "Undergraduate student",
            educationLevel = "Undergraduate (Bachelor's)",
            educationStream = "Commerce and Finance",
            educationInstitution = "Mumbai University",
            targetDomain = "Commerce and Finance",
            targetGoalTitle = "Prepare for CA Foundation",
            availableHoursPerDay = 3,
            preferredSessionDurationMinutes = 45
        )

        assertEquals("primary_learner", profile.id)
        assertEquals("Maya", profile.name)
        assertEquals("Undergraduate student", profile.learnerType)
        assertEquals("Undergraduate (Bachelor's)", profile.educationLevel)
        assertEquals("Commerce and Finance", profile.educationStream)
        assertEquals("Prepare for CA Foundation", profile.targetGoalTitle)
        assertEquals(3, profile.availableHoursPerDay)
        assertTrue(profile.onboardingCompleted)
    }

    @Test
    fun testRoadmapPhaseTransition() {
        val phase1 = RoadmapPhaseEntity(
            id = "phase_1",
            phaseNumber = 1,
            title = "Mercantile Law & Accounting Foundations",
            objective = "Master journal entries and contract law",
            topicsCsv = "Accounting, Trial Balance, Indian Contract Act",
            practicalProject = "Comprehensive Financial Ledger Reconciliation Case Study",
            estimatedWeeks = 4,
            isCompleted = false,
            isCurrentActive = true
        )

        assertTrue(phase1.isCurrentActive)
        assertFalse(phase1.isCompleted)

        // Simulate phase completion
        val completedPhase1 = phase1.copy(isCompleted = true, isCurrentActive = false)
        assertTrue(completedPhase1.isCompleted)
        assertFalse(completedPhase1.isCurrentActive)
    }

    @Test
    fun testTaskCompletionToggle() {
        val task = StudyTaskEntity(
            id = "task_1",
            title = "Study: Principles of Accounting",
            relatedSkill = "Principles of Accounting",
            durationMinutes = 45,
            scheduledDate = "Today",
            isCompleted = false,
            isRevisionTask = false,
            orderIndex = 0
        )

        assertFalse(task.isCompleted)
        val completedTask = task.copy(isCompleted = true)
        assertTrue(completedTask.isCompleted)
    }

    @Test
    fun testSkillEntityGapInvariant() {
        val skill = SkillEntity(
            skillName = "Principles of Accounting",
            currentCompetency = 2,
            targetCompetency = 4,
            calculatedGap = 2,
            isAssessed = true
        )

        assertEquals(skill.targetCompetency - skill.currentCompetency, skill.calculatedGap)
        assertTrue(skill.isAssessed)
    }

    @Test
    fun testUnassessedSkillEntity() {
        val unassessedSkill = SkillEntity(
            skillName = "Business Law",
            currentCompetency = -1,
            targetCompetency = 4,
            calculatedGap = 4,
            isAssessed = false
        )

        assertEquals(-1, unassessedSkill.currentCompetency)
        assertEquals(4, unassessedSkill.calculatedGap)
        assertFalse(unassessedSkill.isAssessed)
    }

    @Test
    fun testAssessmentRecordEntityScoringAndRemediationFlag() {
        val passedRecord = AssessmentRecordEntity(
            topicName = "Pointers & Memory Architecture",
            relatedSkill = "C Programming",
            scorePercentage = 80,
            totalQuestions = 5,
            correctQuestions = 4,
            triggeredRemediation = false
        )
        assertEquals(80, passedRecord.scorePercentage)
        assertEquals(4, passedRecord.correctQuestions)
        assertFalse(passedRecord.triggeredRemediation)

        val failedRecord = AssessmentRecordEntity(
            topicName = "Binary Search Trees",
            relatedSkill = "Data Structures",
            scorePercentage = 40,
            totalQuestions = 5,
            correctQuestions = 2,
            triggeredRemediation = true
        )
        assertEquals(40, failedRecord.scorePercentage)
        assertTrue(failedRecord.triggeredRemediation)
    }

    @Test
    fun testMultiProfileIsolationAndConversationEntities() {
        val convoA = ConversationEntity(
            id = "conv-a-1",
            profileId = "profile_a",
            title = "Pointers in C",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val convoB = ConversationEntity(
            id = "conv-b-1",
            profileId = "profile_b",
            title = "Double Entry Accounting",
            createdAt = 1500L,
            updatedAt = 2500L
        )
        assertEquals("profile_a", convoA.profileId)
        assertEquals("profile_b", convoB.profileId)

        val userMsg = ChatMessageEntity(
            conversationId = convoA.id,
            sender = "Aarav",
            text = "What is a pointer in C?",
            isAi = false,
            runtimeSource = "User",
            orderIndex = 0
        )
        val aiMsg = ChatMessageEntity(
            conversationId = convoA.id,
            sender = "LearnMate AI",
            text = "A pointer stores a memory address.",
            isAi = true,
            runtimeSource = "XNNPACK (ARM NEON)",
            orderIndex = 1
        )
        assertEquals(convoA.id, userMsg.conversationId)
        assertEquals(0, userMsg.orderIndex)
        assertEquals(1, aiMsg.orderIndex)
        assertTrue(aiMsg.isAi)

        val skillA = SkillEntity(skillName = "Embedded C", currentCompetency = 2, targetCompetency = 5, calculatedGap = 3, profileId = "profile_a")
        val skillB = SkillEntity(skillName = "Embedded C", currentCompetency = 4, targetCompetency = 5, calculatedGap = 1, profileId = "profile_b")
        assertEquals("profile_a", skillA.profileId)
        assertEquals("profile_b", skillB.profileId)
        assertEquals(3, skillA.calculatedGap)
        assertEquals(1, skillB.calculatedGap)
    }
}
