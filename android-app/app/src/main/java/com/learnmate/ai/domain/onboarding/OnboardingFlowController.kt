package com.learnmate.ai.domain.onboarding

import com.learnmate.ai.domain.taxonomy.EducationalTaxonomy

/**
 * Application Flow Controller (Phase 3.2).
 * Controls onboarding state machine, enforces dependency invalidation when earlier answers change,
 * validates user inputs, and guarantees a safe and valid navigation state.
 */
class OnboardingFlowController(
    private val stepGenerator: OnboardingStepGenerator = DeterministicTaxonomyProvider()
) {

    var currentStepNumber: Int = 1
        private set

    var context: OnboardingContext = OnboardingContext()
        private set

    // ---------------------------------------------------------------------------------------------
    // Step 1: Learner Identity & Current Education
    // ---------------------------------------------------------------------------------------------
    fun updateStep1(
        name: String,
        learnerType: EducationalTaxonomy.LearnerType,
        educationStage: EducationalTaxonomy.EducationStage,
        stageDetail: String = "",
        institution: String = ""
    ) {
        val hasStageChanged = (context.educationStage != educationStage) || (context.learnerType != learnerType)

        context = context.copy(
            name = name.trim(),
            learnerType = learnerType,
            educationStage = educationStage,
            stageDetail = stageDetail.trim(),
            institution = institution.trim()
        )

        if (hasStageChanged) {
            invalidateDownstream(changedStep = 1)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Step 2: Educational Stream & Background
    // ---------------------------------------------------------------------------------------------
    fun updateStep2(
        streamId: String,
        streamLabel: String,
        customStream: String? = null
    ) {
        val hasStreamChanged = (context.selectedStreamId != streamId) || (context.customStream != customStream)

        context = context.copy(
            selectedStreamId = streamId,
            selectedStreamLabel = streamLabel,
            customStream = customStream?.trim()
        )

        if (hasStreamChanged) {
            invalidateDownstream(changedStep = 2)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Step 3: Explore Relevant Pathways
    // ---------------------------------------------------------------------------------------------
    fun updateStep3(
        pathwayId: String,
        pathwayLabel: String,
        customPathway: String? = null
    ) {
        val hasPathwayChanged = (context.selectedPathwayId != pathwayId) || (context.customPathway != customPathway)

        context = context.copy(
            selectedPathwayId = pathwayId,
            selectedPathwayLabel = pathwayLabel,
            customPathway = customPathway?.trim()
        )

        if (hasPathwayChanged) {
            invalidateDownstream(changedStep = 3)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Step 4: Define Learning Objective
    // ---------------------------------------------------------------------------------------------
    fun updateStep4(
        goalTitle: String,
        goalType: EducationalTaxonomy.GoalType,
        customGoalTitle: String? = null
    ) {
        val hasGoalChanged = (context.selectedGoalTitle != goalTitle) || (context.customGoalTitle != customGoalTitle)

        context = context.copy(
            selectedGoalTitle = goalTitle,
            selectedGoalType = goalType,
            customGoalTitle = customGoalTitle?.trim(),
            isHelpMeDecideActive = false
        )

        if (hasGoalChanged) {
            invalidateDownstream(changedStep = 4)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // "Help Me Decide" Guided Exploration
    // ---------------------------------------------------------------------------------------------
    fun activateHelpMeDecide() {
        context = context.copy(isHelpMeDecideActive = true)
    }

    fun applyRecommendation(recommendation: EducationalTaxonomy.Recommendation) {
        context = context.copy(
            selectedPathwayId = recommendation.pathwayId,
            selectedPathwayLabel = recommendation.pathwayTitle,
            selectedGoalTitle = recommendation.goalTitle,
            selectedGoalType = EducationalTaxonomy.GoalType.CAREER_GOAL,
            customGoalTitle = null,
            isHelpMeDecideActive = false
        )
        invalidateDownstream(changedStep = 4)
    }

    // ---------------------------------------------------------------------------------------------
    // Step 5: Context-Aware Skill Assessment
    // ---------------------------------------------------------------------------------------------
    fun updateSkillRating(skill: String, score: Int) {
        val currentSkills = context.assessedSkills.toMutableMap()
        currentSkills[skill] = score
        context = context.copy(assessedSkills = currentSkills)
    }

    fun skipSkill(skill: String) {
        updateSkillRating(skill, -1) // NOT_ASSESSED
    }

    fun initializeAssessmentSkills() {
        val suggested = stepGenerator.generateAssessmentCompetencies(context)
        val currentSkills = context.assessedSkills.toMutableMap()
        suggested.forEach { skill ->
            if (!currentSkills.containsKey(skill)) {
                currentSkills[skill] = -1 // Default to unassessed
            }
        }
        context = context.copy(assessedSkills = currentSkills)
    }

    // ---------------------------------------------------------------------------------------------
    // Step 6: Preferences & Availability
    // ---------------------------------------------------------------------------------------------
    fun updateStep6(
        availableHours: Int,
        studyDays: List<String>,
        sessionMinutes: Int,
        language: String,
        timeline: String,
        preferences: List<String>
    ) {
        require(availableHours in 1..8) { "Daily hours must be between 1 and 8" }
        require(studyDays.isNotEmpty()) { "At least one study day must be selected" }

        context = context.copy(
            availableHoursPerDay = availableHours,
            preferredStudyDays = studyDays,
            preferredSessionMinutes = sessionMinutes,
            preferredLanguage = language,
            targetTimeline = timeline,
            learningPreferences = preferences
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Dependent Answer Invalidation (Crucial Architecture Requirement)
    // ---------------------------------------------------------------------------------------------
    fun invalidateDownstream(changedStep: Int) {
        when (changedStep) {
            1 -> {
                // Changing Step 1 (Stage/Type) clears Stream, Pathway, Goal, Assessment
                context = context.copy(
                    selectedStreamId = null,
                    selectedStreamLabel = null,
                    customStream = null,
                    selectedPathwayId = null,
                    selectedPathwayLabel = null,
                    customPathway = null,
                    selectedGoalType = null,
                    selectedGoalTitle = null,
                    customGoalTitle = null,
                    assessedSkills = emptyMap(),
                    isHelpMeDecideActive = false
                )
            }
            2 -> {
                // Changing Step 2 (Stream) clears Pathway, Goal, Assessment
                context = context.copy(
                    selectedPathwayId = null,
                    selectedPathwayLabel = null,
                    customPathway = null,
                    selectedGoalType = null,
                    selectedGoalTitle = null,
                    customGoalTitle = null,
                    assessedSkills = emptyMap(),
                    isHelpMeDecideActive = false
                )
            }
            3 -> {
                // Changing Step 3 (Pathway) clears Goal, Assessment
                context = context.copy(
                    selectedGoalType = null,
                    selectedGoalTitle = null,
                    customGoalTitle = null,
                    assessedSkills = emptyMap(),
                    isHelpMeDecideActive = false
                )
            }
            4 -> {
                // Changing Step 4 (Goal) clears Assessment skills
                context = context.copy(
                    assessedSkills = emptyMap()
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Navigation Safety & Validation
    // ---------------------------------------------------------------------------------------------
    fun canAdvance(stepNumber: Int): Boolean {
        return when (stepNumber) {
            1 -> context.name.isNotBlank() && context.learnerType != null && context.educationStage != null
            2 -> context.selectedStreamId != null || !context.customStream.isNullOrBlank()
            3 -> context.selectedPathwayId != null || !context.customPathway.isNullOrBlank()
            4 -> context.selectedGoalTitle != null || !context.customGoalTitle.isNullOrBlank()
            5 -> true // Can always advance from assessment; unrated items are safely NOT_ASSESSED (-1)
            6 -> context.availableHoursPerDay in 1..8 && context.preferredStudyDays.isNotEmpty()
            else -> false
        }
    }

    fun navigateToStep(stepNumber: Int): Boolean {
        if (stepNumber in 1..6) {
            // If navigating forward, verify prerequisites
            if (stepNumber > currentStepNumber) {
                for (s in currentStepNumber until stepNumber) {
                    if (!canAdvance(s)) return false
                }
            }
            currentStepNumber = stepNumber

            if (stepNumber == 5 && context.assessedSkills.isEmpty()) {
                initializeAssessmentSkills()
            }
            return true
        }
        return false
    }

    fun nextStep(): Boolean {
        if (canAdvance(currentStepNumber) && currentStepNumber < 6) {
            return navigateToStep(currentStepNumber + 1)
        }
        return false
    }

    fun previousStep(): Boolean {
        if (currentStepNumber > 1) {
            currentStepNumber -= 1
            return true
        }
        return false
    }

    fun getCurrentStepResponse(): DynamicStepResponse {
        return stepGenerator.generateStep(currentStepNumber, context)
    }

    fun getAiStatus(): String {
        return stepGenerator.getAiStatus()
    }
}
