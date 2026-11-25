package com.learnmate.ai.domain.model

data class SkillGap(
    val skillName: String,
    val currentLevel: Int, // -1 = NOT_ASSESSED, 0..5
    val targetLevel: Int,
    val gap: Int, // Calculated: if unassessed gap = targetLevel, else max(0, targetLevel - currentLevel)
    val priority: GapPriority,
    val isAssessed: Boolean = (currentLevel >= 0),
    val recommendedActions: List<String> = emptyList()
) {
    val isMastered: Boolean get() = isAssessed && gap == 0
}

enum class GapPriority {
    CRITICAL,    // Gap >= 3 or unassessed prerequisite
    MODERATE,    // Gap == 2
    MINOR,       // Gap == 1
    NONE         // Gap == 0 (Mastered)
}
