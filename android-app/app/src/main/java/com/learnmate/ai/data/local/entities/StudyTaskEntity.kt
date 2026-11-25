package com.learnmate.ai.data.local.entities

import androidx.room.Entity

@Entity(tableName = "study_tasks", primaryKeys = ["profileId", "id"])
data class StudyTaskEntity(
    val id: String,
    val title: String,
    val relatedSkill: String,
    val durationMinutes: Int,
    val scheduledDate: String, // YYYY-MM-DD
    val isCompleted: Boolean = false,
    val isRevisionTask: Boolean = false,
    val orderIndex: Int = 0,
    val profileId: String = "primary_learner"
)
