package com.learnmate.ai.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learner_profile")
data class LearnerProfileEntity(
    @PrimaryKey
    val id: String = "primary_learner",
    val name: String,
    val learnerType: String = "Undergraduate student",
    val educationLevel: String = "",
    val educationStream: String = "",
    val educationInstitution: String = "",
    val targetDomain: String = "",
    val targetGoalTitle: String = "",
    val preferredLanguage: String = "English",
    val availableHoursPerDay: Int = 2,
    val preferredStudyDaysCsv: String = "Mon, Tue, Wed, Thu, Fri",
    val preferredSessionDurationMinutes: Int = 45,
    val targetCompletionDate: String = "3 Months",
    val learningStreak: Int = 1,
    val weeklyGoalProgressPercent: Int = 0,
    val onboardingCompleted: Boolean = true,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
