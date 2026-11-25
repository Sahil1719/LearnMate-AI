package com.learnmate.ai.domain.model

/**
 * Standardized Competency Scale (Part 3):
 * -1 = Not Assessed (Skipped)
 * 0 = No prior knowledge
 * 1 = Beginner
 * 2 = Basic understanding
 * 3 = Intermediate
 * 4 = Advanced
 * 5 = Expert
 */
enum class CompetencyLevel(val score: Int, val label: String) {
    NOT_ASSESSED(-1, "Not Assessed"),
    NO_PRIOR_KNOWLEDGE(0, "No prior knowledge"),
    BEGINNER(1, "Beginner"),
    BASIC(2, "Basic understanding"),
    INTERMEDIATE(3, "Intermediate"),
    ADVANCED(4, "Advanced"),
    EXPERT(5, "Expert");

    companion object {
        fun fromScore(score: Int): CompetencyLevel {
            return entries.firstOrNull { it.score == score } ?: if (score < 0) NOT_ASSESSED else NO_PRIOR_KNOWLEDGE
        }
    }
}

data class Skill(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val prerequisites: List<String> = emptyList()
)
