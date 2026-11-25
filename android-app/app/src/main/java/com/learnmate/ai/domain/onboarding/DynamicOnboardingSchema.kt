package com.learnmate.ai.domain.onboarding

import com.learnmate.ai.domain.taxonomy.EducationalTaxonomy
import org.json.JSONArray
import org.json.JSONObject

/**
 * Structured Dynamic Onboarding Schema (Phase 3.2).
 * Defines the contract for dynamic, context-aware question generation.
 * Compatible with on-device ExecuTorch Llama 3 8B JSON generation and deterministic fallback.
 */

data class DynamicStepOption(
    val id: String,
    val label: String,
    val description: String,
    val categoryTag: String? = null
)

data class DynamicStepResponse(
    val stepId: String,
    val stepNumber: Int, // 1 to 6
    val title: String,
    val question: String,
    val description: String,
    val contextExplanation: String? = null,
    val options: List<DynamicStepOption>,
    val allowCustomInput: Boolean = true,
    val allowSkip: Boolean = false,
    val allowUndecided: Boolean = false,
    val nextStepType: String
)

data class OnboardingContext(
    val name: String = "",
    val learnerType: EducationalTaxonomy.LearnerType? = null,
    val educationStage: EducationalTaxonomy.EducationStage? = null,
    val stageDetail: String = "", // e.g. "10th Standard", "2nd Year B.Tech", "Intern"
    val institution: String = "",
    val selectedStreamId: String? = null,
    val selectedStreamLabel: String? = null,
    val customStream: String? = null,
    val selectedPathwayId: String? = null,
    val selectedPathwayLabel: String? = null,
    val customPathway: String? = null,
    val selectedGoalType: EducationalTaxonomy.GoalType? = null,
    val selectedGoalTitle: String? = null,
    val customGoalTitle: String? = null,
    val isHelpMeDecideActive: Boolean = false,
    val helpMeDecideInterest: String? = null,
    val assessedSkills: Map<String, Int> = emptyMap(),
    val availableHoursPerDay: Int = 2,
    val preferredStudyDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val preferredSessionMinutes: Int = 45,
    val preferredLanguage: String = "English",
    val targetTimeline: String = "3 Months",
    val learningPreferences: List<String> = emptyList()
) {
    /**
     * Resolves the effective stream name (custom or selected).
     */
    fun getEffectiveStream(): String {
        return when {
            !customStream.isNullOrBlank() -> customStream
            !selectedStreamLabel.isNullOrBlank() -> selectedStreamLabel
            else -> "General"
        }
    }

    /**
     * Resolves the effective pathway name (custom or selected).
     */
    fun getEffectivePathway(): String {
        return when {
            !customPathway.isNullOrBlank() -> customPathway
            !selectedPathwayLabel.isNullOrBlank() -> selectedPathwayLabel
            else -> "Foundations"
        }
    }

    /**
     * Resolves the effective learning goal title (custom or selected).
     */
    fun getEffectiveGoalTitle(): String {
        return when {
            !customGoalTitle.isNullOrBlank() -> customGoalTitle
            !selectedGoalTitle.isNullOrBlank() -> selectedGoalTitle
            else -> "Personalized Mastery"
        }
    }
}

/**
 * Interface for generating dynamic, context-aware steps.
 */
interface OnboardingStepGenerator {
    fun generateStep(stepNumber: Int, context: OnboardingContext): DynamicStepResponse
    fun generateAssessmentCompetencies(context: OnboardingContext): List<String>
    fun getAiStatus(): String
}

/**
 * Prepares system and user prompts for ExecuTorch Llama 3 8B.
 */
object LlamaPromptFormatter {

    const val SYSTEM_PROMPT = """You are the LearnMate AI Onboarding Reasoning Engine.
Your role is to analyze a learner's background, education stage, and interests, and generate the next personalized onboarding question.
You MUST output valid, strictly formed JSON adhering to this schema:
{
  "step_id": "<string>",
  "question": "<conversational question string>",
  "description": "<contextual description string>",
  "context_explanation": "<reasoning why these options match the learner>",
  "options": [
    {
      "id": "<string>",
      "label": "<concise title>",
      "description": "<explanatory text>",
      "category_tag": "<optional tag>"
    }
  ],
  "allow_custom_input": true,
  "allow_skip": false,
  "allow_undecided": false,
  "next_step_type": "<string>"
}
DO NOT include markdown ticks, preamble, or commentary outside the JSON."""

    fun buildPromptForStep(stepNumber: Int, context: OnboardingContext): String {
        val userContextJson = JSONObject().apply {
            put("learner_name", context.name)
            put("learner_type", context.learnerType?.displayName ?: "Unspecified")
            put("education_stage", context.educationStage?.displayName ?: "Unspecified")
            put("current_stage_detail", context.stageDetail)
            put("stream", context.getEffectiveStream())
            put("pathway", context.getEffectivePathway())
            put("target_goal", context.getEffectiveGoalTitle())
            put("step_requested", stepNumber)
        }

        return """Context:
${userContextJson.toString(2)}

Task:
Generate step $stepNumber question and relevant options for this learner. Ensure every option directly corresponds to their selected background and interests."""
    }
}

/**
 * JSON Schema Validator for LLM outputs.
 * Pure Kotlin parser independent of Android SDK stubs, making it 100% testable on the JVM.
 */
object JsonSchemaValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val stepResponse: DynamicStepResponse? = null,
        val errorMessage: String? = null
    )

    fun validateAndParse(jsonString: String): ValidationResult {
        return try {
            val cleanJson = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            fun extractString(key: String, source: String): String? {
                val pattern = Regex(""""$key"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
                return pattern.find(source)?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n")
            }

            fun extractInt(key: String, source: String, default: Int): Int {
                val pattern = Regex(""""$key"\s*:\s*(\d+)""")
                return pattern.find(source)?.groupValues?.get(1)?.toIntOrNull() ?: default
            }

            fun extractBoolean(key: String, source: String, default: Boolean): Boolean {
                val pattern = Regex(""""$key"\s*:\s*(true|false)""")
                return pattern.find(source)?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: default
            }

            val stepId = extractString("step_id", cleanJson)
            if (stepId.isNullOrBlank()) {
                return ValidationResult(false, null, "Missing or empty step_id")
            }

            val question = extractString("question", cleanJson)
            if (question.isNullOrBlank()) {
                return ValidationResult(false, null, "Missing or empty question")
            }

            val title = extractString("title", cleanJson) ?: "Explore Your Pathway"
            val description = extractString("description", cleanJson) ?: ""
            val contextExplanation = extractString("context_explanation", cleanJson)
            val nextStepType = extractString("next_step_type", cleanJson) ?: "pathway"
            val stepNumber = extractInt("step_number", cleanJson, 2)
            val allowCustom = extractBoolean("allow_custom_input", cleanJson, true)
            val allowSkip = extractBoolean("allow_skip", cleanJson, false)
            val allowUndecided = extractBoolean("allow_undecided", cleanJson, false)

            val optionsRegex = Regex(""""options"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
            val optionsMatch = optionsRegex.find(cleanJson)
            if (optionsMatch == null) {
                return ValidationResult(false, null, "Options list cannot be empty")
            }

            val optionsBlock = optionsMatch.groupValues[1]
            val objRegex = Regex("""\{([^}]*)\}""", RegexOption.DOT_MATCHES_ALL)
            val optionsList = mutableListOf<DynamicStepOption>()

            var optIndex = 0
            for (match in objRegex.findAll(optionsBlock)) {
                val objContent = match.groupValues[1]
                val id = extractString("id", objContent) ?: "opt_${optIndex++}"
                val label = extractString("label", objContent) ?: ""
                val optDesc = extractString("description", objContent) ?: ""
                val categoryTag = extractString("category_tag", objContent)

                if (label.isNotBlank()) {
                    optionsList.add(DynamicStepOption(id, label, optDesc, categoryTag))
                }
            }

            if (optionsList.isEmpty()) {
                return ValidationResult(false, null, "Options list cannot be empty")
            }

            val response = DynamicStepResponse(
                stepId = stepId,
                stepNumber = stepNumber,
                title = title,
                question = question,
                description = description,
                contextExplanation = contextExplanation,
                options = optionsList,
                allowCustomInput = allowCustom,
                allowSkip = allowSkip,
                allowUndecided = allowUndecided,
                nextStepType = nextStepType
            )

            ValidationResult(true, response, null)
        } catch (e: Exception) {
            ValidationResult(false, null, "JSON parse failure: ${e.message}")
        }
    }
}
