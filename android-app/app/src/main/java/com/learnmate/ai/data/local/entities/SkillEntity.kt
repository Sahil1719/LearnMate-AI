package com.learnmate.ai.data.local.entities

import androidx.room.Entity

@Entity(tableName = "learner_skills", primaryKeys = ["profileId", "skillName"])
data class SkillEntity(
    val skillName: String,
    val currentCompetency: Int, // -1 = NOT_ASSESSED, 0..5 = confirmed competency
    val targetCompetency: Int,  // 1..5
    val calculatedGap: Int,     // if unassessed: targetCompetency, else max(0, target - current)
    val isAssessed: Boolean = (currentCompetency >= 0),
    val priority: String = "CRITICAL",
    val category: String = "Core Competency",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val profileId: String = "primary_learner"
)
