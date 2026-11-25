package com.learnmate.ai.domain.engine

import com.learnmate.ai.domain.knowledge.CareerKnowledgeBase
import com.learnmate.ai.domain.model.GapPriority
import com.learnmate.ai.domain.model.SkillGap

/**
 * Deterministic Skill Gap Engine (Section 10).
 * Implements strict mathematical formulation:
 *   Gap = max(0, TargetCompetency - CurrentCompetency)
 * Prerequisite dependencies are evaluated from the structured knowledge base.
 */
object SkillGapEngine {

    fun calculateGaps(
        targetPathwayId: String,
        currentSkills: Map<String, Int>
    ): List<SkillGap> {
        val pathway = CareerKnowledgeBase.getPathway(targetPathwayId)
            ?: return emptyList()

        val gaps = mutableListOf<SkillGap>()

        for ((skillName, targetScore) in pathway.requiredSkills) {
            val currentScore = currentSkills[skillName]?.coerceIn(0, 5) ?: 0
            val gap = maxOf(0, targetScore - currentScore)

            // Evaluate priority based on gap magnitude and prerequisite constraints
            val isPrerequisiteForOthers = pathway.prerequisiteGraph.values.any { it.contains(skillName) }
            val priority = when {
                gap == 0 -> GapPriority.NONE
                gap >= 3 || (gap >= 2 && isPrerequisiteForOthers) -> GapPriority.CRITICAL
                gap == 2 -> GapPriority.MODERATE
                else -> GapPriority.MINOR
            }

            val actions = generateDeterministicRecommendations(skillName, currentScore, targetScore, isPrerequisiteForOthers)

            gaps.add(
                SkillGap(
                    skillName = skillName,
                    currentLevel = currentScore,
                    targetLevel = targetScore,
                    gap = gap,
                    priority = priority,
                    recommendedActions = actions
                )
            )
        }

        // Return sorted by priority (CRITICAL first, then by gap descending)
        return gaps.sortedWith(
            compareBy<SkillGap> { it.priority.ordinal }
                .thenByDescending { it.gap }
        )
    }

    private fun generateDeterministicRecommendations(
        skill: String,
        current: Int,
        target: Int,
        isPrerequisite: Boolean
    ): List<String> {
        val list = mutableListOf<String>()
        if (current == 0) {
            list.add("Initiate foundational module for $skill.")
        } else if (current < target) {
            list.add("Target competency level $target with focused practice and assessments.")
        }
        if (isPrerequisite) {
            list.add("High priority: Required as a prerequisite for advanced topics.")
        }
        return list
    }
}
