package com.learnmate.ai.domain.model

data class LearnerProfile(
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
    val preferredStudyDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val preferredSessionDurationMinutes: Int = 45,
    val targetCompletionDate: String = "3 Months",
    val skills: Map<String, Int> = emptyMap(), // Skill Name -> Current Competency (-1..5)
    val learningStreak: Int = 1,
    val weeklyGoalProgressPercent: Int = 0,
    val onboardingCompleted: Boolean = true
)
