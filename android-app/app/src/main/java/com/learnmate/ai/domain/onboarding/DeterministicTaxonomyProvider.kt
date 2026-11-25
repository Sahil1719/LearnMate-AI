package com.learnmate.ai.domain.onboarding

import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import com.learnmate.ai.domain.taxonomy.EducationalTaxonomy

/**
 * Deterministic Taxonomy Provider (Phase 3.2).
 * Implements OnboardingStepGenerator using the structured EducationalTaxonomy.
 * Provides rich, context-aware, progressive onboarding questions with zero cloud dependence.
 * Identifies transparently that the offline taxonomy engine is active.
 */
class DeterministicTaxonomyProvider : OnboardingStepGenerator {

    override fun getAiStatus(): String {
        return "Offline Knowledge Taxonomy active (Deterministic fallback; On-device Llama reasoning engine interface ready)"
    }

    override fun generateStep(stepNumber: Int, context: OnboardingContext): DynamicStepResponse {
        return when (stepNumber) {
            1 -> generateStep1Identity(context)
            2 -> generateStep2Stream(context)
            3 -> generateStep3Pathway(context)
            4 -> generateStep4Goal(context)
            5 -> generateStep5Assessment(context)
            6 -> generateStep6Preferences(context)
            else -> generateStep1Identity(context)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Step 1: Learner Identity & Education Stage
    // ---------------------------------------------------------------------------------------------
    private fun generateStep1Identity(context: OnboardingContext): DynamicStepResponse {
        val options = EducationalTaxonomy.LearnerType.values().map { type ->
            DynamicStepOption(
                id = type.name,
                label = type.displayName,
                description = type.description
            )
        }

        return DynamicStepResponse(
            stepId = "step_1_identity",
            stepNumber = 1,
            title = "Welcome! Let's get acquainted",
            question = if (context.name.isBlank()) "What is your name, and what describes your learning journey?" else "Welcome, ${context.name}! What describes your current stage?",
            description = "We tailor your entire curriculum to your academic level and schedule.",
            contextExplanation = null,
            options = options,
            allowCustomInput = false,
            allowSkip = false,
            allowUndecided = false,
            nextStepType = "stream"
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Step 2: Educational Stream & Background (Context-Aware based on Step 1)
    // ---------------------------------------------------------------------------------------------
    private fun generateStep2Stream(context: OnboardingContext): DynamicStepResponse {
        val stage = context.educationStage ?: EducationalTaxonomy.EducationStage.UNDERGRADUATE
        val taxonomyOptions = EducationalTaxonomy.getStreamsForStage(stage)

        val options = taxonomyOptions.map { opt ->
            DynamicStepOption(
                id = opt.id,
                label = opt.label,
                description = opt.description,
                categoryTag = opt.categoryTag
            )
        }

        val stageName = context.educationStage?.displayName ?: context.learnerType?.displayName ?: "Current Education"
        val question = when (context.learnerType) {
            EducationalTaxonomy.LearnerType.SCHOOL_STUDENT ->
                "What academic stream or subjects are you focusing on, ${context.name}?"
            EducationalTaxonomy.LearnerType.COLLEGE_STUDENT ->
                "What degree programme or academic stream are you enrolled in, ${context.name}?"
            EducationalTaxonomy.LearnerType.WORKING_PROFESSIONAL ->
                "Which industry or professional domain do you work in, ${context.name}?"
            EducationalTaxonomy.LearnerType.SELF_LEARNER ->
                "Which field or domain do you wish to explore, ${context.name}?"
            null -> "What is your educational background or field of interest?"
        }

        return DynamicStepResponse(
            stepId = "step_2_stream",
            stepNumber = 2,
            title = "Academic Stream & Background",
            question = question,
            description = "Choose the stream that best reflects your current study or professional background.",
            contextExplanation = "Showing streams tailored to $stageName.",
            options = options,
            allowCustomInput = true,
            allowSkip = false,
            allowUndecided = false,
            nextStepType = "pathway"
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Step 3: Explore Relevant Pathways (Context-Aware based on Step 1 & 2)
    // ---------------------------------------------------------------------------------------------
    private fun generateStep3Pathway(context: OnboardingContext): DynamicStepResponse {
        val streamId = context.selectedStreamId ?: "ug_engineering"
        val streamName = context.getEffectiveStream()
        val taxonomyOptions = EducationalTaxonomy.getPathwaysForStream(streamId)

        val options = taxonomyOptions.map { opt ->
            DynamicStepOption(
                id = opt.id,
                label = opt.label,
                description = opt.description,
                categoryTag = opt.categoryTag
            )
        }

        val question = "Which specific discipline or branch in $streamName interests you most, ${context.name}?"

        return DynamicStepResponse(
            stepId = "step_3_pathway",
            stepNumber = 3,
            title = "Explore Pathways",
            question = question,
            description = "Explore specialized branches, professional qualifications, or career tracks.",
            contextExplanation = "Curated specifically for your background in $streamName.",
            options = options,
            allowCustomInput = true,
            allowSkip = false,
            allowUndecided = true, // Learner can explore or say "Help me decide"
            nextStepType = "goal"
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Step 4: Define Actual Learning Objective (Context-Aware based on Pathway)
    // ---------------------------------------------------------------------------------------------
    private fun generateStep4Goal(context: OnboardingContext): DynamicStepResponse {
        val pathwayId = context.selectedPathwayId ?: "eng_entc"
        val pathwayName = context.getEffectivePathway()
        val streamId = context.selectedStreamId ?: "ug_engineering"

        val goalDefinitions = EducationalTaxonomy.getGoalsForPathway(pathwayId, streamId)

        val options = goalDefinitions.map { goal ->
            DynamicStepOption(
                id = goal.id,
                label = goal.title,
                description = goal.description,
                categoryTag = goal.goalType.displayName
            )
        }

        val question = "What is your primary learning objective in $pathwayName, ${context.name}?"

        return DynamicStepResponse(
            stepId = "step_4_goal",
            stepNumber = 4,
            title = "Target Objective",
            question = question,
            description = "Select a career goal, entrance exam, academic subject, or enter your own custom goal.",
            contextExplanation = "Relevant goals aligned with $pathwayName.",
            options = options,
            allowCustomInput = true,
            allowSkip = false,
            allowUndecided = true, // "Help me decide" guided discovery
            nextStepType = "assessment"
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Step 5: Context-Aware Skill Assessment
    // ---------------------------------------------------------------------------------------------
    private fun generateStep5Assessment(context: OnboardingContext): DynamicStepResponse {
        val goalTitle = context.getEffectiveGoalTitle()
        val skills = generateAssessmentCompetencies(context)

        val options = skills.mapIndexed { idx, skillName ->
            DynamicStepOption(
                id = "skill_$idx",
                label = skillName,
                description = "Self-assess from 0 (Novice) to 5 (Expert) or Skip"
            )
        }

        return DynamicStepResponse(
            stepId = "step_5_assessment",
            stepNumber = 5,
            title = "Diagnostic Self-Assessment",
            question = "Rate your current familiarity with key skills for $goalTitle",
            description = "Be completely honest! Skipping will safely mark a skill as unassessed rather than penalizing you.",
            contextExplanation = "Core competencies required for $goalTitle.",
            options = options,
            allowCustomInput = false,
            allowSkip = true,
            allowUndecided = false,
            nextStepType = "availability"
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Step 6: Learning Preferences & Availability
    // ---------------------------------------------------------------------------------------------
    private fun generateStep6Preferences(context: OnboardingContext): DynamicStepResponse {
        val options = listOf(
            DynamicStepOption("pref_projects", "Project-Driven", "Learn by building hands-on projects and coding"),
            DynamicStepOption("pref_theory", "Theory & Concepts First", "Deep fundamental principles before applications"),
            DynamicStepOption("pref_visual", "Visuals & Diagrams", "Rich schematics, mind maps, and illustrations"),
            DynamicStepOption("pref_exam", "Exam Practice & Problem Sets", "High volume of questions, quizzes, and mock tests")
        )

        return DynamicStepResponse(
            stepId = "step_6_preferences",
            stepNumber = 6,
            title = "Study Schedule & Style",
            question = "How would you like to structure your study routine, ${context.name}?",
            description = "We will strictly enforce your daily study time limit to prevent burnout.",
            contextExplanation = "Customized to your schedule: ${context.availableHoursPerDay} hrs/day (${context.availableHoursPerDay * 60} mins max).",
            options = options,
            allowCustomInput = false,
            allowSkip = false,
            allowUndecided = false,
            nextStepType = "generate"
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Competency Synthesis
    // ---------------------------------------------------------------------------------------------
    override fun generateAssessmentCompetencies(context: OnboardingContext): List<String> {
        val pathwayId = context.selectedPathwayId ?: ""
        val streamId = context.selectedStreamId ?: ""
        val goalTitle = context.getEffectiveGoalTitle()
        val domain = context.getEffectiveStream()

        // 1. Try exact matching from EducationalTaxonomy goal definitions
        val taxonomyGoals = EducationalTaxonomy.getGoalsForPathway(pathwayId, streamId)
        val matchedGoal = taxonomyGoals.firstOrNull {
            it.title.equals(goalTitle, ignoreCase = true) || it.id.equals(context.selectedGoalTitle, ignoreCase = true)
        }

        if (matchedGoal != null && matchedGoal.suggestedSkills.isNotEmpty()) {
            return matchedGoal.suggestedSkills
        }

        // 2. Query GoalBasedLearningEngine for broad domain / custom goal rules
        return GoalBasedLearningEngine.suggestCompetenciesForGoal(domain, goalTitle)
    }
}
