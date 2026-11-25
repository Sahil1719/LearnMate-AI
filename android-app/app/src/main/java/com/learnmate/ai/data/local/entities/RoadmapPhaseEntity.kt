package com.learnmate.ai.data.local.entities

import androidx.room.Entity

@Entity(tableName = "roadmap_phases", primaryKeys = ["profileId", "id"])
data class RoadmapPhaseEntity(
    val id: String, // e.g., "phase_1"
    val phaseNumber: Int,
    val title: String,
    val objective: String,
    val topicsCsv: String, // Comma-separated list of topics
    val practicalProject: String,
    val learningOutcomesCsv: String = "",
    val estimatedWeeks: Int,
    val isCompleted: Boolean = false,
    val isCurrentActive: Boolean = false,
    val profileId: String = "primary_learner"
)
