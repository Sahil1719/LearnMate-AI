package com.learnmate.ai.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessment_records")
data class AssessmentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicName: String,
    val relatedSkill: String,
    val scorePercentage: Int, // 0..100
    val totalQuestions: Int,
    val correctQuestions: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val triggeredRemediation: Boolean = false,
    val profileId: String = "primary_learner"
)
