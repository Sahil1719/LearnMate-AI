package com.learnmate.ai.domain.model

data class CareerPathway(
    val id: String,
    val title: String,
    val description: String,
    val requiredSkills: Map<String, Int>, // Skill Name -> Target Competency (1..5)
    val prerequisiteGraph: Map<String, List<String>>, // Skill Name -> List of Prerequisite Skills
    val phases: List<RoadmapPhaseTemplate>
)

data class RoadmapPhaseTemplate(
    val phaseNumber: Int,
    val title: String,
    val objective: String,
    val topics: List<String>,
    val practicalProject: String,
    val estimatedWeeks: Int
)
