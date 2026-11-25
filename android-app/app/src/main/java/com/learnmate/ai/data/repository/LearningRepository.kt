package com.learnmate.ai.data.repository

import android.content.Context
import com.learnmate.ai.data.local.LearnMateDatabase
import com.learnmate.ai.data.local.entities.AssessmentRecordEntity
import com.learnmate.ai.data.local.entities.ChatMessageEntity
import com.learnmate.ai.data.local.entities.ConversationEntity
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.data.local.entities.RoadmapPhaseEntity
import com.learnmate.ai.data.local.entities.SkillEntity
import com.learnmate.ai.data.local.entities.StudyTaskEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class LearningRepository(
    private val database: LearnMateDatabase,
    context: Context? = null
) {

    companion object {
        private const val PREFS_NAME = "learnmate_profile_prefs"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    }

    private val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val activeProfileIdState = MutableStateFlow(prefs?.getString(KEY_ACTIVE_PROFILE_ID, null))

    val allProfilesFlow: Flow<List<LearnerProfileEntity>> =
        database.learnerProfileDao().getAllProfilesFlow()

    val profileFlow: Flow<LearnerProfileEntity?> =
        combine(allProfilesFlow, activeProfileIdState) { profiles, selectedId ->
            when {
                profiles.isEmpty() -> null
                selectedId != null -> profiles.firstOrNull { it.id == selectedId } ?: profiles.first()
                else -> profiles.first()
            }
        }

    private val resolvedActiveProfileIdFlow: Flow<String?> =
        combine(allProfilesFlow, activeProfileIdState) { profiles, selectedId ->
            when {
                profiles.isEmpty() -> null
                selectedId != null && profiles.any { it.id == selectedId } -> selectedId
                else -> profiles.first().id
            }
        }

    val skillsFlow: Flow<List<SkillEntity>> = resolvedActiveProfileIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList()) else database.skillDao().getSkillsForProfileFlow(pid)
    }

    val phasesFlow: Flow<List<RoadmapPhaseEntity>> = resolvedActiveProfileIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList()) else database.roadmapDao().getPhasesForProfileFlow(pid)
    }

    val tasksFlow: Flow<List<StudyTaskEntity>> = resolvedActiveProfileIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList()) else database.studyTaskDao().getTasksForProfileFlow(pid)
    }

    val assessmentRecordsFlow: Flow<List<AssessmentRecordEntity>> = resolvedActiveProfileIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList()) else database.assessmentRecordDao().getRecordsForProfileFlow(pid)
    }

    val conversationsFlow: Flow<List<ConversationEntity>> = resolvedActiveProfileIdFlow.flatMapLatest { pid ->
        if (pid == null) flowOf(emptyList()) else database.conversationDao().getConversationsForProfileFlow(pid)
    }

    private fun setActiveProfileId(profileId: String?) {
        activeProfileIdState.value = profileId
        prefs?.edit()?.apply {
            if (profileId == null) remove(KEY_ACTIVE_PROFILE_ID) else putString(KEY_ACTIVE_PROFILE_ID, profileId)
            apply()
        }
    }

    suspend fun getActiveProfileId(): String? {
        val selected = activeProfileIdState.value
        val all = database.learnerProfileDao().getAllProfiles()
        if (all.isEmpty()) return null
        val match = all.firstOrNull { it.id == selected } ?: all.first()
        if (match.id != selected) {
            setActiveProfileId(match.id)
        }
        return match.id
    }

    suspend fun switchProfile(profileId: String) {
        val target = database.learnerProfileDao().getProfile(profileId)
        if (target != null) {
            setActiveProfileId(target.id)
        }
    }

    suspend fun saveFullProfile(
        name: String,
        learnerType: String,
        educationLevel: String,
        educationStream: String,
        educationInstitution: String,
        targetDomain: String,
        targetGoalTitle: String,
        preferredLanguage: String,
        availableHoursPerDay: Int,
        preferredStudyDays: List<String>,
        preferredSessionMinutes: Int,
        targetCompletionDate: String,
        assessedSkills: Map<String, Int>,
        existingProfileId: String? = null
    ): LearnerProfileEntity {
        val existing = existingProfileId?.let { database.learnerProfileDao().getProfile(it) }
        val targetId = existing?.id ?: UUID.randomUUID().toString()

        val profile = LearnerProfileEntity(
            id = targetId,
            name = name.trim(),
            learnerType = learnerType,
            educationLevel = educationLevel,
            educationStream = educationStream,
            educationInstitution = educationInstitution.trim(),
            targetDomain = targetDomain,
            targetGoalTitle = targetGoalTitle.trim(),
            preferredLanguage = preferredLanguage,
            availableHoursPerDay = availableHoursPerDay,
            preferredStudyDaysCsv = preferredStudyDays.joinToString(", "),
            preferredSessionDurationMinutes = preferredSessionMinutes,
            targetCompletionDate = targetCompletionDate,
            learningStreak = existing?.learningStreak ?: 1,
            weeklyGoalProgressPercent = existing?.weeklyGoalProgressPercent ?: 0,
            onboardingCompleted = true,
            createdAtTimestamp = existing?.createdAtTimestamp ?: System.currentTimeMillis()
        )
        database.learnerProfileDao().insertOrUpdate(profile)
        setActiveProfileId(targetId)

        // Generate tailored pathway using GoalBasedLearningEngine scoped strictly to targetId
        val generated = GoalBasedLearningEngine.generatePathway(
            profile = profile,
            assessedSkills = assessedSkills
        )

        database.skillDao().clearSkillsForProfile(targetId)
        database.skillDao().insertOrUpdateSkills(generated.skills.map { it.copy(profileId = targetId) })

        database.roadmapDao().clearPhasesForProfile(targetId)
        database.roadmapDao().insertPhases(generated.phases.map { it.copy(profileId = targetId) })

        database.studyTaskDao().clearTasksForProfile(targetId)
        database.studyTaskDao().insertTasks(generated.initialDailyTasks.map { it.copy(profileId = targetId) })

        return profile
    }

    // Overload for simpler calls & test compatibility
    suspend fun saveLearnerProfile(
        name: String,
        education: String,
        targetCareerGoalId: String,
        availableHoursPerDay: Int,
        initialSkillScores: Map<String, Int>
    ) {
        saveFullProfile(
            name = name,
            learnerType = "Student",
            educationLevel = education,
            educationStream = education,
            educationInstitution = "",
            targetDomain = "Technology",
            targetGoalTitle = targetCareerGoalId.replace('_', ' ').replaceFirstChar { it.uppercase() },
            preferredLanguage = "English",
            availableHoursPerDay = availableHoursPerDay,
            preferredStudyDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            preferredSessionMinutes = 45,
            targetCompletionDate = "3 Months",
            assessedSkills = initialSkillScores,
            existingProfileId = "primary_learner"
        )
    }

    suspend fun deleteProfile(profileId: String) {
        database.studyTaskDao().clearTasksForProfile(profileId)
        database.roadmapDao().clearPhasesForProfile(profileId)
        database.skillDao().clearSkillsForProfile(profileId)
        database.assessmentRecordDao().clearRecordsForProfile(profileId)
        database.conversationDao().deleteConversationsForProfile(profileId)
        database.learnerProfileDao().deleteProfile(profileId)

        val remaining = database.learnerProfileDao().getAllProfiles()
        setActiveProfileId(remaining.firstOrNull()?.id)
    }

    suspend fun resetProfile() {
        val activeId = getActiveProfileId()
        if (activeId != null) {
            deleteProfile(activeId)
        }
    }

    suspend fun updateTaskCompletion(taskId: String, completed: Boolean) {
        val activeId = getActiveProfileId() ?: return
        database.studyTaskDao().updateTaskCompletion(activeId, taskId, completed)
    }

    suspend fun updateSkillCompetency(skillName: String, newCompetency: Int) {
        val activeId = getActiveProfileId() ?: return
        database.skillDao().updateSkillCompetency(activeId, skillName, newCompetency)
    }

    suspend fun triggerAdaptiveRemediation(strugglingTopic: String, relatedSkill: String) {
        val activeId = getActiveProfileId() ?: return
        val revisionTask = StudyTaskEntity(
            id = "remedy_${System.currentTimeMillis()}",
            title = "Remediation & Review: $strugglingTopic",
            relatedSkill = relatedSkill,
            durationMinutes = 45,
            scheduledDate = "Today",
            isCompleted = false,
            isRevisionTask = true,
            orderIndex = 0,
            profileId = activeId
        )
        database.studyTaskDao().insertTasks(listOf(revisionTask))
    }

    suspend fun recordQuizResult(
        topicName: String,
        relatedSkill: String,
        totalQuestions: Int,
        correctQuestions: Int
    ): AssessmentRecordEntity {
        val activeId = getActiveProfileId() ?: "primary_learner"
        val scorePercent = if (totalQuestions > 0) (correctQuestions * 100) / totalQuestions else 0
        val needsRemediation = scorePercent < 60

        if (needsRemediation) {
            triggerAdaptiveRemediation(strugglingTopic = topicName, relatedSkill = relatedSkill)
        } else {
            val currentSkills = database.skillDao().getSkillsForProfile(activeId)
            val existing = currentSkills.firstOrNull { it.skillName.equals(relatedSkill, ignoreCase = true) }
            if (existing != null && scorePercent >= 80 && existing.currentCompetency < existing.targetCompetency) {
                val newScore = minOf(existing.targetCompetency, existing.currentCompetency + 1)
                database.skillDao().updateSkillCompetency(activeId, existing.skillName, newScore)
            }
        }

        val record = AssessmentRecordEntity(
            topicName = topicName,
            relatedSkill = relatedSkill,
            scorePercentage = scorePercent,
            totalQuestions = totalQuestions,
            correctQuestions = correctQuestions,
            timestamp = System.currentTimeMillis(),
            triggeredRemediation = needsRemediation,
            profileId = activeId
        )
        val insertedId = database.assessmentRecordDao().insertRecord(record)
        return record.copy(id = insertedId)
    }

    // ---- AI Tutor Conversation Persistence ----

    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessageEntity>> =
        database.conversationDao().getMessagesForConversationFlow(conversationId)

    suspend fun getMessages(conversationId: String): List<ChatMessageEntity> =
        database.conversationDao().getMessagesForConversation(conversationId)

    suspend fun createConversation(profileId: String, title: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.conversationDao().insertConversation(
            ConversationEntity(
                id = id,
                profileId = profileId,
                title = title.trim().ifBlank { "New Tutor Session" }.take(64),
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun appendChatMessage(
        conversationId: String,
        sender: String,
        text: String,
        isAi: Boolean,
        runtimeSource: String,
        orderIndex: Int
    ): Long {
        val now = System.currentTimeMillis()
        val msgId = database.conversationDao().insertMessage(
            ChatMessageEntity(
                conversationId = conversationId,
                sender = sender,
                text = text,
                isAi = isAi,
                runtimeSource = runtimeSource,
                timestamp = now,
                orderIndex = orderIndex
            )
        )
        database.conversationDao().updateConversationTimestamp(conversationId, now)
        return msgId
    }

    suspend fun updateChatMessage(
        conversationId: String,
        messageId: Long,
        text: String,
        runtimeSource: String
    ) {
        database.conversationDao().updateMessage(messageId, text, runtimeSource)
        database.conversationDao().updateConversationTimestamp(conversationId, System.currentTimeMillis())
    }

    suspend fun updateConversationTitle(conversationId: String, title: String) {
        database.conversationDao().updateConversationMeta(
            conversationId = conversationId,
            title = title.trim().ifBlank { "Tutor Session" }.take(64),
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteConversation(conversationId: String) {
        database.conversationDao().deleteConversation(conversationId)
    }
}
