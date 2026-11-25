package com.learnmate.ai.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.onboarding.DeterministicTaxonomyProvider
import com.learnmate.ai.domain.onboarding.DynamicStepOption
import com.learnmate.ai.domain.onboarding.DynamicStepResponse
import com.learnmate.ai.domain.onboarding.OnboardingFlowController
import com.learnmate.ai.domain.taxonomy.EducationalTaxonomy
import com.learnmate.ai.ui.theme.AmberWarning
import com.learnmate.ai.ui.theme.DeepBlueBorder
import com.learnmate.ai.ui.theme.DeepBlueCard
import com.learnmate.ai.ui.theme.ElectricBlue
import com.learnmate.ai.ui.theme.EmeraldGreen
import com.learnmate.ai.ui.theme.MidnightNavy
import com.learnmate.ai.ui.theme.SoftViolet
import com.learnmate.ai.ui.theme.TextPrimary
import com.learnmate.ai.ui.theme.TextSecondary
import com.learnmate.ai.ui.theme.Typography
import kotlinx.coroutines.delay

enum class OnboardingFlowStage {
    SPLASH,
    WELCOME,
    STEP_1_IDENTITY,
    STEP_2_STREAM,
    STEP_3_PATHWAY,
    STEP_4_GOAL,
    STEP_5_ASSESSMENT,
    STEP_6_PREFERENCES,
    PROFILE_REVIEW,
    GENERATING_PATHWAY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingFlow(
    hasExistingProfile: Boolean = false,
    existingProfileName: String? = null,
    editingProfile: LearnerProfileEntity? = null,
    isCreatingNewProfile: Boolean = false,
    initialStageName: String? = null,
    initialPreset: String? = null,
    commitPresetDirectly: Boolean = false,
    onCompleteOnboarding: (
        name: String,
        learnerType: String,
        educationLevel: String,
        educationStream: String,
        educationInstitution: String,
        targetDomain: String,
        targetGoalTitle: String,
        preferredLanguage: String,
        availableHoursPerDay: Int,
        preferredStudyDays: List<String>,
        preferredSessionMinutes: Int,
        targetCompletionDate: String,
        assessedSkills: Map<String, Int>
    ) -> Unit,
    onNavigateToExistingDashboard: () -> Unit = {}
) {
    val controller = remember { OnboardingFlowController(DeterministicTaxonomyProvider()) }

    val parsedInitialStage = remember(initialStageName) {
        if (!initialStageName.isNullOrBlank()) {
            try {
                OnboardingFlowStage.valueOf(initialStageName.uppercase())
            } catch (e: Exception) {
                null
            }
        } else null
    }

    var currentStage by remember {
        mutableStateOf(
            parsedInitialStage ?: when {
                editingProfile != null || isCreatingNewProfile -> OnboardingFlowStage.STEP_1_IDENTITY
                hasExistingProfile -> OnboardingFlowStage.WELCOME
                else -> OnboardingFlowStage.SPLASH
            }
        )
    }

    // Step 1 local fields
    var nameInput by remember { mutableStateOf(editingProfile?.name ?: "") }
    var selectedLearnerType by remember {
        mutableStateOf(
            editingProfile?.let { ep ->
                EducationalTaxonomy.LearnerType.entries.firstOrNull {
                    it.displayName.equals(ep.learnerType, ignoreCase = true)
                }
            }
        )
    }
    var selectedStage by remember {
        mutableStateOf(
            editingProfile?.let { ep ->
                EducationalTaxonomy.EducationStage.entries.firstOrNull {
                    it.displayName.equals(ep.educationLevel, ignoreCase = true)
                }
            }
        )
    }
    var stageDetailInput by remember { mutableStateOf("") }
    var institutionInput by remember { mutableStateOf(editingProfile?.educationInstitution ?: "") }

    // Step 2 local fields
    var selectedStreamId by remember { mutableStateOf<String?>(null) }
    var selectedStreamLabel by remember { mutableStateOf<String?>(editingProfile?.educationStream?.ifBlank { null }) }
    var customStreamInput by remember { mutableStateOf(editingProfile?.educationStream ?: "") }

    // Step 3 local fields
    var selectedPathwayId by remember { mutableStateOf<String?>(null) }
    var selectedPathwayLabel by remember { mutableStateOf<String?>(editingProfile?.targetDomain?.ifBlank { null }) }
    var customPathwayInput by remember { mutableStateOf(editingProfile?.targetDomain ?: "") }

    // Step 4 local fields
    var selectedGoalTitle by remember { mutableStateOf<String?>(editingProfile?.targetGoalTitle?.ifBlank { null }) }
    var selectedGoalType by remember { mutableStateOf(EducationalTaxonomy.GoalType.CAREER_GOAL) }
    var customGoalInput by remember { mutableStateOf(editingProfile?.targetGoalTitle ?: "") }
    var isHelpMeDecideOpen by remember { mutableStateOf(false) }

    // Step 5 local fields
    val localAssessedSkills = remember { mutableStateMapOf<String, Int>() }

    // Step 6 local fields
    var availableHours by remember { mutableIntStateOf(editingProfile?.availableHoursPerDay ?: 2) }
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            val initialDays = editingProfile?.preferredStudyDaysCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
            if (!initialDays.isNullOrEmpty()) addAll(initialDays) else addAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
        }
    }
    var sessionDurationMinutes by remember { mutableIntStateOf(editingProfile?.preferredSessionDurationMinutes ?: 45) }
    var preferredLanguage by remember { mutableStateOf(editingProfile?.preferredLanguage ?: "English") }
    var targetTimeline by remember { mutableStateOf(editingProfile?.targetCompletionDate ?: "3 Months") }
    val selectedLearningStyles = remember { mutableStateListOf("Practical Projects") }

    LaunchedEffect(initialPreset, initialStageName) {
        if (!initialPreset.isNullOrBlank()) {
            when (initialPreset.uppercase()) {
                "COMMERCE_CA" -> {
                    nameInput = "Priya"
                    selectedLearnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT
                    selectedStage = EducationalTaxonomy.EducationStage.JUNIOR_COLLEGE_11_12
                    stageDetailInput = "12th Standard Commerce"
                    institutionInput = "City Commerce College"
                    selectedStreamId = "jc_commerce"
                    selectedStreamLabel = "Commerce & Business Studies"
                    selectedPathwayId = "comm_ca_cma_cs"
                    selectedPathwayLabel = "Chartered Accountancy & Professional Certifications"
                    selectedGoalTitle = "CA Foundation Examination"
                    selectedGoalType = EducationalTaxonomy.GoalType.ENTRANCE_EXAM
                    controller.updateStep1(nameInput, selectedLearnerType!!, selectedStage!!, stageDetailInput, institutionInput)
                    controller.updateStep2(selectedStreamId!!, selectedStreamLabel!!)
                    controller.updateStep3(selectedPathwayId!!, selectedPathwayLabel!!)
                    controller.updateStep4(selectedGoalTitle!!, selectedGoalType)
                    controller.initializeAssessmentSkills()
                    localAssessedSkills.clear()
                    controller.context.assessedSkills.forEach { (k, v) -> localAssessedSkills[k] = v }
                }
                "MEDICAL_NEET_PG" -> {
                    nameInput = "Dr. Siddharth"
                    selectedLearnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT
                    selectedStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
                    stageDetailInput = "MBBS Intern"
                    institutionInput = "Government Medical College"
                    selectedStreamId = "ug_medicine"
                    selectedStreamLabel = "Medicine & Healthcare (MBBS)"
                    selectedPathwayId = "med_pg_prep"
                    selectedPathwayLabel = "Postgraduate Medical Exam Preparation (NEET PG)"
                    selectedGoalTitle = "NEET PG / INI-CET Medical Entrance"
                    selectedGoalType = EducationalTaxonomy.GoalType.ENTRANCE_EXAM
                    controller.updateStep1(nameInput, selectedLearnerType!!, selectedStage!!, stageDetailInput, institutionInput)
                    controller.updateStep2(selectedStreamId!!, selectedStreamLabel!!)
                    controller.updateStep3(selectedPathwayId!!, selectedPathwayLabel!!)
                    controller.updateStep4(selectedGoalTitle!!, selectedGoalType)
                    controller.initializeAssessmentSkills()
                    localAssessedSkills.clear()
                    controller.context.assessedSkills.forEach { (k, v) -> localAssessedSkills[k] = v }
                }
                "ENGINEERING_EMBEDDED" -> {
                    nameInput = "Rohan"
                    selectedLearnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT
                    selectedStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
                    stageDetailInput = "B.E. 3rd Year"
                    institutionInput = "College of Engineering"
                    selectedStreamId = "ug_engineering"
                    selectedStreamLabel = "Engineering & Technology (B.Tech / B.E)"
                    selectedPathwayId = "eng_entc"
                    selectedPathwayLabel = "Electronics & Telecommunication (ENTC / ECE)"
                    selectedGoalTitle = "Embedded Systems Engineer"
                    selectedGoalType = EducationalTaxonomy.GoalType.CAREER_GOAL
                    controller.updateStep1(nameInput, selectedLearnerType!!, selectedStage!!, stageDetailInput, institutionInput)
                    controller.updateStep2(selectedStreamId!!, selectedStreamLabel!!)
                    controller.updateStep3(selectedPathwayId!!, selectedPathwayLabel!!)
                    controller.updateStep4(selectedGoalTitle!!, selectedGoalType)
                    controller.initializeAssessmentSkills()
                    localAssessedSkills.clear()
                    controller.context.assessedSkills.forEach { (k, v) -> localAssessedSkills[k] = v }
                }
                "BCA_SOFTWARE" -> {
                    nameInput = "Vikram"
                    selectedLearnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT
                    selectedStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
                    stageDetailInput = "BCA 2nd Year"
                    institutionInput = "Institute of Technology"
                    selectedStreamId = "ug_bca"
                    selectedStreamLabel = "Computer Applications (BCA / B.Sc IT)"
                    selectedPathwayId = "bca_software_dev"
                    selectedPathwayLabel = "Software Development (Full-Stack & Apps)"
                    selectedGoalTitle = "Full-Stack Software Developer"
                    selectedGoalType = EducationalTaxonomy.GoalType.CAREER_GOAL
                    controller.updateStep1(nameInput, selectedLearnerType!!, selectedStage!!, stageDetailInput, institutionInput)
                    controller.updateStep2(selectedStreamId!!, selectedStreamLabel!!)
                    controller.updateStep3(selectedPathwayId!!, selectedPathwayLabel!!)
                    controller.updateStep4(selectedGoalTitle!!, selectedGoalType)
                    controller.initializeAssessmentSkills()
                    localAssessedSkills.clear()
                    controller.context.assessedSkills.forEach { (k, v) -> localAssessedSkills[k] = v }
                }
                "ARTS_ILLUSTRATION" -> {
                    nameInput = "Meera"
                    selectedLearnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT
                    selectedStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
                    stageDetailInput = "BFA 3rd Year"
                    institutionInput = "National College of Arts"
                    selectedStreamId = "ug_arts"
                    selectedStreamLabel = "Arts & Humanities (BFA)"
                    selectedPathwayId = "arts_illustration"
                    selectedPathwayLabel = "Digital Illustration & Concept Art"
                    selectedGoalTitle = "Professional Digital Illustrator"
                    selectedGoalType = EducationalTaxonomy.GoalType.CAREER_GOAL
                    controller.updateStep1(nameInput, selectedLearnerType!!, selectedStage!!, stageDetailInput, institutionInput)
                    controller.updateStep2(selectedStreamId!!, selectedStreamLabel!!)
                    controller.updateStep3(selectedPathwayId!!, selectedPathwayLabel!!)
                    controller.updateStep4(selectedGoalTitle!!, selectedGoalType)
                    controller.initializeAssessmentSkills()
                    localAssessedSkills.clear()
                    controller.context.assessedSkills.forEach { (k, v) -> localAssessedSkills[k] = v }
                }
                "PYTHON_FINANCE" -> {
                    nameInput = "Dev"
                    selectedLearnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT
                    selectedStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
                    stageDetailInput = "B.Com Final Year"
                    institutionInput = "City Commerce College"
                    selectedStreamId = "ug_commerce"
                    selectedStreamLabel = "Commerce & Management (B.Com)"
                    selectedPathwayId = "comm_accounting"
                    selectedPathwayLabel = "Accounting & Financial Reporting"
                    customGoalInput = "Python for Financial Data Analysis"
                    selectedGoalTitle = "Python for Financial Data Analysis"
                    selectedGoalType = EducationalTaxonomy.GoalType.CUSTOM_GOAL
                    controller.updateStep1(nameInput, selectedLearnerType!!, selectedStage!!, stageDetailInput, institutionInput)
                    controller.updateStep2(selectedStreamId!!, selectedStreamLabel!!)
                    controller.updateStep3(selectedPathwayId!!, selectedPathwayLabel!!)
                    controller.updateStep4(selectedGoalTitle!!, selectedGoalType, customGoalInput)
                    controller.initializeAssessmentSkills()
                    localAssessedSkills.clear()
                    controller.context.assessedSkills.forEach { (k, v) -> localAssessedSkills[k] = v }
                }
                "HELP_ME_DECIDE" -> {
                    nameInput = "Karan"
                    selectedLearnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT
                    selectedStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
                    selectedStreamId = "ug_bca"
                    selectedStreamLabel = "Computer Applications (BCA / B.Sc IT)"
                    selectedPathwayId = "bca_software_dev"
                    selectedPathwayLabel = "Software Development (Full-Stack & Apps)"
                    isHelpMeDecideOpen = true
                    controller.updateStep1(nameInput, selectedLearnerType!!, selectedStage!!)
                    controller.updateStep2(selectedStreamId!!, selectedStreamLabel!!)
                    controller.updateStep3(selectedPathwayId!!, selectedPathwayLabel!!)
                    controller.activateHelpMeDecide()
                }
            }

            if (commitPresetDirectly) {
                onCompleteOnboarding(
                    nameInput,
                    selectedLearnerType?.displayName ?: "Learner",
                    selectedStage?.displayName ?: "Undergraduate",
                    selectedStreamLabel ?: "General",
                    institutionInput,
                    selectedPathwayLabel ?: "Core Foundations",
                    selectedGoalTitle ?: "Personalized Goal",
                    preferredLanguage,
                    availableHours,
                    selectedDays.toList(),
                    sessionDurationMinutes,
                    targetTimeline,
                    localAssessedSkills.toMap()
                )
            }
        }

        if (!initialStageName.isNullOrBlank()) {
            try {
                currentStage = OnboardingFlowStage.valueOf(initialStageName.uppercase())
                val targetStepNum = when (currentStage) {
                    OnboardingFlowStage.STEP_1_IDENTITY -> 1
                    OnboardingFlowStage.STEP_2_STREAM -> 2
                    OnboardingFlowStage.STEP_3_PATHWAY -> 3
                    OnboardingFlowStage.STEP_4_GOAL -> 4
                    OnboardingFlowStage.STEP_5_ASSESSMENT -> 5
                    OnboardingFlowStage.STEP_6_PREFERENCES -> 6
                    else -> 1
                }
                controller.navigateToStep(targetStepNum)
            } catch (e: Exception) {
                // Keep default
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy)
    ) {
        AnimatedContent(
            targetState = currentStage,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "OnboardingTransition"
        ) { stage ->
            when (stage) {
                OnboardingFlowStage.SPLASH -> SplashScreen(
                    onTimeout = {
                        if (hasExistingProfile) {
                            onNavigateToExistingDashboard()
                        } else {
                            currentStage = OnboardingFlowStage.WELCOME
                        }
                    }
                )

                OnboardingFlowStage.WELCOME -> WelcomeScreen(
                    hasExistingProfile = hasExistingProfile,
                    existingProfileName = existingProfileName,
                    onGetStarted = { currentStage = OnboardingFlowStage.STEP_1_IDENTITY },
                    onGoToDashboard = onNavigateToExistingDashboard
                )

                // =================================================================================
                // STEP 1: IDENTITY & EDUCATION LEVEL
                // =================================================================================
                OnboardingFlowStage.STEP_1_IDENTITY -> {
                    Step1IdentityScreen(
                        name = nameInput,
                        onNameChange = { nameInput = it },
                        selectedType = selectedLearnerType,
                        onSelectType = { type ->
                            selectedLearnerType = type
                            selectedStage = null // reset stage when type changes
                        },
                        selectedStage = selectedStage,
                        onSelectStage = { selectedStage = it },
                        stageDetail = stageDetailInput,
                        onStageDetailChange = { stageDetailInput = it },
                        institution = institutionInput,
                        onInstitutionChange = { institutionInput = it },
                        onNext = {
                            if (nameInput.isNotBlank() && selectedLearnerType != null && selectedStage != null) {
                                controller.updateStep1(
                                    name = nameInput,
                                    learnerType = selectedLearnerType!!,
                                    educationStage = selectedStage!!,
                                    stageDetail = stageDetailInput,
                                    institution = institutionInput
                                )
                                // If downstream was invalidated, clear local downstream state
                                if (controller.context.selectedStreamId == null) {
                                    selectedStreamId = null
                                    selectedStreamLabel = null
                                    customStreamInput = ""
                                    selectedPathwayId = null
                                    selectedPathwayLabel = null
                                    customPathwayInput = ""
                                    selectedGoalTitle = null
                                    customGoalInput = ""
                                    localAssessedSkills.clear()
                                }
                                currentStage = OnboardingFlowStage.STEP_2_STREAM
                            }
                        },
                        onBack = { currentStage = OnboardingFlowStage.WELCOME }
                    )
                }

                // =================================================================================
                // STEP 2: STREAM & BACKGROUND (Context-Aware based on Step 1)
                // =================================================================================
                OnboardingFlowStage.STEP_2_STREAM -> {
                    val stepData = controller.getCurrentStepResponse().let {
                        if (controller.currentStepNumber != 2) {
                            controller.navigateToStep(2)
                            controller.getCurrentStepResponse()
                        } else it
                    }

                    Step2StreamScreen(
                        stepData = stepData,
                        contextExplanation = "Showing streams tailored to ${selectedStage?.displayName ?: "your education level"}",
                        selectedStreamId = selectedStreamId,
                        onSelectStream = { id, label ->
                            selectedStreamId = id
                            selectedStreamLabel = label
                            if (id != "custom") customStreamInput = ""
                        },
                        customStream = customStreamInput,
                        onCustomStreamChange = { customStreamInput = it },
                        onNext = {
                            val finalStreamId = selectedStreamId ?: "custom"
                            val finalLabel = if (finalStreamId == "custom" || selectedStreamId?.endsWith("custom") == true) {
                                customStreamInput.ifBlank { "Custom Stream" }
                            } else {
                                selectedStreamLabel ?: "General"
                            }

                            controller.updateStep2(finalStreamId, finalLabel, customStreamInput.ifBlank { null })

                            // Clear downstream if stream changed
                            if (controller.context.selectedPathwayId == null) {
                                selectedPathwayId = null
                                selectedPathwayLabel = null
                                customPathwayInput = ""
                                selectedGoalTitle = null
                                customGoalInput = ""
                                localAssessedSkills.clear()
                            }
                            controller.navigateToStep(3)
                            currentStage = OnboardingFlowStage.STEP_3_PATHWAY
                        },
                        onBack = { currentStage = OnboardingFlowStage.STEP_1_IDENTITY }
                    )
                }

                // =================================================================================
                // STEP 3: RELEVANT PATHWAYS (Context-Aware based on Stream)
                // =================================================================================
                OnboardingFlowStage.STEP_3_PATHWAY -> {
                    val stepData = controller.getCurrentStepResponse().let {
                        if (controller.currentStepNumber != 3) {
                            controller.navigateToStep(3)
                            controller.getCurrentStepResponse()
                        } else it
                    }

                    Step3PathwayScreen(
                        stepData = stepData,
                        streamName = controller.context.getEffectiveStream(),
                        selectedPathwayId = selectedPathwayId,
                        onSelectPathway = { id, label ->
                            selectedPathwayId = id
                            selectedPathwayLabel = label
                            if (id != "custom") customPathwayInput = ""
                        },
                        customPathway = customPathwayInput,
                        onCustomPathwayChange = { customPathwayInput = it },
                        onNext = {
                            val finalPathwayId = selectedPathwayId ?: "custom"
                            val finalLabel = if (finalPathwayId == "custom" || selectedPathwayId?.endsWith("custom") == true) {
                                customPathwayInput.ifBlank { "Custom Pathway" }
                            } else {
                                selectedPathwayLabel ?: "Core Foundations"
                            }

                            controller.updateStep3(finalPathwayId, finalLabel, customPathwayInput.ifBlank { null })

                            // Clear downstream if pathway changed
                            if (controller.context.selectedGoalTitle == null) {
                                selectedGoalTitle = null
                                customGoalInput = ""
                                localAssessedSkills.clear()
                            }
                            controller.navigateToStep(4)
                            currentStage = OnboardingFlowStage.STEP_4_GOAL
                        },
                        onBack = { currentStage = OnboardingFlowStage.STEP_2_STREAM }
                    )
                }

                // =================================================================================
                // STEP 4: LEARNING OBJECTIVE & "HELP ME DECIDE"
                // =================================================================================
                OnboardingFlowStage.STEP_4_GOAL -> {
                    val stepData = controller.getCurrentStepResponse().let {
                        if (controller.currentStepNumber != 4) {
                            controller.navigateToStep(4)
                            controller.getCurrentStepResponse()
                        } else it
                    }

                    Step4GoalScreen(
                        stepData = stepData,
                        pathwayName = controller.context.getEffectivePathway(),
                        educationStage = controller.context.educationStage ?: EducationalTaxonomy.EducationStage.UNDERGRADUATE,
                        selectedGoalTitle = selectedGoalTitle,
                        onSelectGoal = { title, type ->
                            selectedGoalTitle = title
                            selectedGoalType = type
                            customGoalInput = ""
                            isHelpMeDecideOpen = false
                        },
                        customGoal = customGoalInput,
                        onCustomGoalChange = {
                            customGoalInput = it
                            selectedGoalType = EducationalTaxonomy.GoalType.CUSTOM_GOAL
                        },
                        isHelpMeDecideOpen = isHelpMeDecideOpen,
                        onToggleHelpMeDecide = { isHelpMeDecideOpen = it },
                        onApplyRecommendation = { rec ->
                            controller.applyRecommendation(rec)
                            selectedPathwayId = rec.pathwayId
                            selectedPathwayLabel = rec.pathwayTitle
                            selectedGoalTitle = rec.goalTitle
                            selectedGoalType = EducationalTaxonomy.GoalType.CAREER_GOAL
                            customGoalInput = ""
                            isHelpMeDecideOpen = false
                        },
                        onNext = {
                            val effectiveGoal = if (customGoalInput.isNotBlank()) customGoalInput else selectedGoalTitle ?: "Goal"
                            controller.updateStep4(
                                goalTitle = effectiveGoal,
                                goalType = selectedGoalType,
                                customGoalTitle = customGoalInput.ifBlank { null }
                            )

                            // Initialize competencies for step 5
                            controller.initializeAssessmentSkills()
                            localAssessedSkills.clear()
                            controller.context.assessedSkills.forEach { (k, v) ->
                                localAssessedSkills[k] = v
                            }

                            controller.navigateToStep(5)
                            currentStage = OnboardingFlowStage.STEP_5_ASSESSMENT
                        },
                        onBack = { currentStage = OnboardingFlowStage.STEP_3_PATHWAY }
                    )
                }

                // =================================================================================
                // STEP 5: CONTEXT-AWARE SKILL ASSESSMENT
                // =================================================================================
                OnboardingFlowStage.STEP_5_ASSESSMENT -> {
                    Step5AssessmentScreen(
                        goalTitle = controller.context.getEffectiveGoalTitle(),
                        skills = localAssessedSkills,
                        onSkillScoreChange = { skill, score ->
                            localAssessedSkills[skill] = score
                            controller.updateSkillRating(skill, score)
                        },
                        onSkipSkill = { skill ->
                            localAssessedSkills[skill] = -1
                            controller.skipSkill(skill)
                        },
                        onNext = {
                            controller.navigateToStep(6)
                            currentStage = OnboardingFlowStage.STEP_6_PREFERENCES
                        },
                        onBack = { currentStage = OnboardingFlowStage.STEP_4_GOAL }
                    )
                }

                // =================================================================================
                // STEP 6: PREFERENCES & TIME AVAILABILITY
                // =================================================================================
                OnboardingFlowStage.STEP_6_PREFERENCES -> {
                    Step6PreferencesScreen(
                        availableHours = availableHours,
                        onHoursChange = { availableHours = it },
                        selectedDays = selectedDays,
                        onToggleDay = { day ->
                            if (selectedDays.contains(day)) {
                                if (selectedDays.size > 1) selectedDays.remove(day)
                            } else {
                                selectedDays.add(day)
                            }
                        },
                        sessionMinutes = sessionDurationMinutes,
                        onSessionMinutesChange = { sessionDurationMinutes = it },
                        language = preferredLanguage,
                        onLanguageChange = { preferredLanguage = it },
                        targetTimeline = targetTimeline,
                        onTimelineChange = { targetTimeline = it },
                        selectedStyles = selectedLearningStyles,
                        onToggleStyle = { style ->
                            if (selectedLearningStyles.contains(style)) {
                                if (selectedLearningStyles.size > 1) selectedLearningStyles.remove(style)
                            } else {
                                selectedLearningStyles.add(style)
                            }
                        },
                        onNext = {
                            controller.updateStep6(
                                availableHours = availableHours,
                                studyDays = selectedDays.toList(),
                                sessionMinutes = sessionDurationMinutes,
                                language = preferredLanguage,
                                timeline = targetTimeline,
                                preferences = selectedLearningStyles.toList()
                            )
                            currentStage = OnboardingFlowStage.PROFILE_REVIEW
                        },
                        onBack = { currentStage = OnboardingFlowStage.STEP_5_ASSESSMENT }
                    )
                }

                // =================================================================================
                // PROFILE REVIEW
                // =================================================================================
                OnboardingFlowStage.PROFILE_REVIEW -> {
                    StepProfileReview(
                        context = controller.context,
                        assessedCount = localAssessedSkills.count { it.value >= 0 },
                        unassessedCount = localAssessedSkills.count { it.value < 0 },
                        onEditStep = { stepNum ->
                            currentStage = when (stepNum) {
                                1 -> OnboardingFlowStage.STEP_1_IDENTITY
                                2 -> OnboardingFlowStage.STEP_2_STREAM
                                3 -> OnboardingFlowStage.STEP_3_PATHWAY
                                4 -> OnboardingFlowStage.STEP_4_GOAL
                                5 -> OnboardingFlowStage.STEP_5_ASSESSMENT
                                6 -> OnboardingFlowStage.STEP_6_PREFERENCES
                                else -> OnboardingFlowStage.STEP_1_IDENTITY
                            }
                        },
                        onGenerate = { currentStage = OnboardingFlowStage.GENERATING_PATHWAY },
                        onBack = { currentStage = OnboardingFlowStage.STEP_6_PREFERENCES }
                    )
                }

                // =================================================================================
                // GENERATING PATHWAY ANIMATION
                // =================================================================================
                OnboardingFlowStage.GENERATING_PATHWAY -> {
                    StepPathwayGeneration(
                        goal = controller.context.getEffectiveGoalTitle(),
                        onFinish = {
                            onCompleteOnboarding(
                                controller.context.name,
                                controller.context.learnerType?.displayName ?: "Learner",
                                controller.context.educationStage?.displayName ?: "Undergraduate",
                                controller.context.getEffectiveStream(),
                                controller.context.institution,
                                controller.context.getEffectivePathway(),
                                controller.context.getEffectiveGoalTitle(),
                                controller.context.preferredLanguage,
                                controller.context.availableHoursPerDay,
                                controller.context.preferredStudyDays,
                                controller.context.preferredSessionMinutes,
                                controller.context.targetTimeline,
                                localAssessedSkills.toMap()
                            )
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. Splash Screen
// -------------------------------------------------------------------------------------------------
@Composable
private fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1600)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(ElectricBlue, SoftViolet)))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MidnightNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "LearnMate AI Logo",
                        tint = ElectricBlue,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "LearnMate AI",
                style = Typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Learn smarter. Progress faster.",
                style = Typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = ElectricBlue,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. Welcome Screen
// -------------------------------------------------------------------------------------------------
@Composable
private fun WelcomeScreen(
    hasExistingProfile: Boolean,
    existingProfileName: String?,
    onGetStarted: () -> Unit,
    onGoToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DeepBlueCard)
                    .border(1.dp, DeepBlueBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Personalized On-Device AI Learning",
                style = Typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "LearnMate progressively learns about your background, career aspirations, and daily availability to build an adaptive, verifiable curriculum.",
                style = Typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SoftViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Context-Aware Onboarding",
                            style = Typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Every question adapts to your previous answers. No fixed questionnaires, no assumed engineering defaults, and zero cloud privacy leakage.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (hasExistingProfile && !existingProfileName.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onGoToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder)
                ) {
                    Text(
                        text = "Continue as $existingProfileName",
                        color = ElectricBlue,
                        style = Typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (hasExistingProfile) "Create New Personalized Profile" else "Get Started",
                    color = Color.White,
                    style = Typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step Progress Bar Component
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepHeader(
    currentStep: Int,
    totalSteps: Int = 6,
    title: String,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            }
            Text(
                text = "Step $currentStep of $totalSteps",
                style = Typography.labelMedium,
                color = ElectricBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ElectricBlue,
            trackColor = DeepBlueBorder,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = Typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Step 1: Learner Identity and Current Education
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1IdentityScreen(
    name: String,
    onNameChange: (String) -> Unit,
    selectedType: EducationalTaxonomy.LearnerType?,
    onSelectType: (EducationalTaxonomy.LearnerType) -> Unit,
    selectedStage: EducationalTaxonomy.EducationStage?,
    onSelectStage: (EducationalTaxonomy.EducationStage) -> Unit,
    stageDetail: String,
    onStageDetailChange: (String) -> Unit,
    institution: String,
    onInstitutionChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val isValid = name.isNotBlank() && selectedType != null && selectedStage != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                StepHeader(
                    currentStep = 1,
                    title = "Who is learning with us?",
                    onBack = onBack
                )

                Text(
                    text = "Let's personalize your experience. We do not assume any discipline or prior knowledge.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Name Input
                Text(
                    text = "Your Full Name *",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = { Text("Enter your name", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = DeepBlueBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Learner Type
                Text(
                    text = "What describes your current profile? *",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                EducationalTaxonomy.LearnerType.values().forEach { type ->
                    val isSelected = (selectedType == type)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) DeepBlueCard else MidnightNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) ElectricBlue else DeepBlueBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectType(type) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.displayName,
                                    style = Typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) ElectricBlue else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = type.description,
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Academic Stages for selected type
                if (selectedType != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Current Education Stage / Level *",
                        style = Typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val stages = EducationalTaxonomy.getStagesForLearnerType(selectedType)
                    stages.forEach { stage ->
                        val isSelected = (selectedStage == stage)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DeepBlueCard else MidnightNavy
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SoftViolet else DeepBlueBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectStage(stage) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stage.displayName,
                                    style = Typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SoftViolet else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SoftViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Optional details
                    Text(
                        text = "Current Grade, Year or Academic Stage (Optional)",
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = stageDetail,
                        onValueChange = onStageDetailChange,
                        placeholder = { Text("e.g. 10th Standard, 2nd Year, Final Semester", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = DeepBlueBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "School, College or Institution (Optional)",
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = institution,
                        onValueChange = onInstitutionChange,
                        placeholder = { Text("e.g. Mumbai University, CBSE, Freelance", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = DeepBlueBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        Button(
            onClick = onNext,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                disabledContainerColor = DeepBlueBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue to Educational Stream",
                color = if (isValid) Color.White else TextSecondary,
                style = Typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 2: Educational Stream and Background (Context-Aware)
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step2StreamScreen(
    stepData: DynamicStepResponse,
    contextExplanation: String,
    selectedStreamId: String?,
    onSelectStream: (id: String, label: String) -> Unit,
    customStream: String,
    onCustomStreamChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val isCustom = (selectedStreamId == "custom" || selectedStreamId?.endsWith("custom") == true)
    val isValid = (selectedStreamId != null && (!isCustom || customStream.isNotBlank()))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                StepHeader(
                    currentStep = 2,
                    title = "Educational Stream & Background",
                    onBack = onBack
                )

                // Context Reason Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = contextExplanation,
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stepData.question,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            items(stepData.options) { opt ->
                val isSelected = (selectedStreamId == opt.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DeepBlueCard else MidnightNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ElectricBlue else DeepBlueBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectStream(opt.id, opt.label) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.label,
                                style = Typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) ElectricBlue else TextPrimary
                            )
                            if (opt.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = opt.description,
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Custom stream input when selected
            if (isCustom) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Specify your stream or discipline *",
                        style = Typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customStream,
                        onValueChange = onCustomStreamChange,
                        placeholder = { Text("e.g. Mechatronics, Bio-Informatics, Culinary Arts", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = DeepBlueBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        Button(
            onClick = onNext,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                disabledContainerColor = DeepBlueBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue to Explore Pathways",
                color = if (isValid) Color.White else TextSecondary,
                style = Typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 3: Explore Relevant Pathways (Context-Aware based on Stream)
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step3PathwayScreen(
    stepData: DynamicStepResponse,
    streamName: String,
    selectedPathwayId: String?,
    onSelectPathway: (id: String, label: String) -> Unit,
    customPathway: String,
    onCustomPathwayChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val isCustom = (selectedPathwayId == "custom" || selectedPathwayId?.endsWith("custom") == true)
    val isValid = (selectedPathwayId != null && (!isCustom || customPathway.isNotBlank()))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                StepHeader(
                    currentStep = 3,
                    title = "Explore Relevant Pathways",
                    onBack = onBack
                )

                // Context Reason Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SoftViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Curated specifically for your background in $streamName.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stepData.question,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            items(stepData.options) { opt ->
                val isSelected = (selectedPathwayId == opt.id)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DeepBlueCard else MidnightNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) SoftViolet else DeepBlueBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectPathway(opt.id, opt.label) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = opt.label,
                                    style = Typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) SoftViolet else TextPrimary,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (!opt.categoryTag.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DeepBlueBorder)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = opt.categoryTag,
                                            style = Typography.labelSmall,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            if (opt.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = opt.description,
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SoftViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Custom pathway input when selected
            if (isCustom) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Specify your target pathway or branch *",
                        style = Typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customPathway,
                        onValueChange = onCustomPathwayChange,
                        placeholder = { Text("e.g. Biomedical Devices, Quantitative Trading", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftViolet,
                            unfocusedBorderColor = DeepBlueBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        Button(
            onClick = onNext,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftViolet,
                disabledContainerColor = DeepBlueBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Define Learning Objective",
                color = if (isValid) Color.White else TextSecondary,
                style = Typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 4: Define Learning Objective & "Help Me Decide"
// -------------------------------------------------------------------------------------------------
@Composable
private fun Step4GoalScreen(
    stepData: DynamicStepResponse,
    pathwayName: String,
    educationStage: EducationalTaxonomy.EducationStage,
    selectedGoalTitle: String?,
    onSelectGoal: (title: String, type: EducationalTaxonomy.GoalType) -> Unit,
    customGoal: String,
    onCustomGoalChange: (String) -> Unit,
    isHelpMeDecideOpen: Boolean,
    onToggleHelpMeDecide: (Boolean) -> Unit,
    onApplyRecommendation: (EducationalTaxonomy.Recommendation) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val isValid = (selectedGoalTitle != null && selectedGoalTitle.isNotBlank()) || customGoal.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                StepHeader(
                    currentStep = 4,
                    title = "Define Learning Objective",
                    onBack = onBack
                )

                // Context Reason Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Relevant goals aligned with $pathwayName.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stepData.question,
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // "Help Me Decide" Button
                    OutlinedButton(
                        onClick = { onToggleHelpMeDecide(!isHelpMeDecideOpen) },
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHelpMeDecideOpen) AmberWarning else DeepBlueBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isHelpMeDecideOpen) AmberWarning else TextSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHelpMeDecideOpen) "Close Guide" else "Help Me Decide",
                            style = Typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Guided Exploration ("Help Me Decide")
            if (isHelpMeDecideOpen) {
                item {
                    HelpMeDecidePanel(
                        educationStage = educationStage,
                        onSelectRecommendation = onApplyRecommendation
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            items(stepData.options) { opt ->
                val isSelected = (selectedGoalTitle == opt.label && customGoal.isBlank())
                val goalType = when (opt.categoryTag) {
                    "Entrance Examination" -> EducationalTaxonomy.GoalType.ENTRANCE_EXAM
                    "Academic Subject / Exam" -> EducationalTaxonomy.GoalType.ACADEMIC_SUBJECT
                    "Professional Certification" -> EducationalTaxonomy.GoalType.PROFESSIONAL_CERTIFICATION
                    "Skill Development" -> EducationalTaxonomy.GoalType.SKILL_DEVELOPMENT
                    "Personal Development" -> EducationalTaxonomy.GoalType.PERSONAL_DEVELOPMENT
                    else -> EducationalTaxonomy.GoalType.CAREER_GOAL
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DeepBlueCard else MidnightNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) EmeraldGreen else DeepBlueBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectGoal(opt.label, goalType) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = opt.label,
                                    style = Typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) EmeraldGreen else TextPrimary,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (!opt.categoryTag.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DeepBlueBorder)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = opt.categoryTag,
                                            style = Typography.labelSmall,
                                            color = EmeraldGreen,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            if (opt.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = opt.description,
                                    style = Typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Custom goal entry
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Or enter any custom goal in your own words:",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customGoal,
                    onValueChange = onCustomGoalChange,
                    placeholder = { Text("e.g. Learning Python for Finance, Organic Gardening", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = DeepBlueBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        Button(
            onClick = onNext,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                disabledContainerColor = DeepBlueBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue to Skill Assessment",
                color = if (isValid) Color.White else TextSecondary,
                style = Typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Guided Exploration Component ("Help Me Decide")
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HelpMeDecidePanel(
    educationStage: EducationalTaxonomy.EducationStage,
    onSelectRecommendation: (EducationalTaxonomy.Recommendation) -> Unit
) {
    var selectedInterestKey by remember { mutableStateOf("tech") }

    Card(
        colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guided Discovery (I'm not sure yet)",
                    style = Typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarning
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What type of challenge or activity excites you most?",
                style = Typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            val interests = listOf(
                "tech" to "Building software & tech",
                "finance" to "Business, finance & markets",
                "art" to "Creative arts & design",
                "medicine" to "Healthcare & human biology"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                interests.forEach { (key, label) ->
                    val isSel = (selectedInterestKey == key)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) AmberWarning else DeepBlueBorder)
                            .clickable { selectedInterestKey = key }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = Typography.labelSmall,
                            color = if (isSel) MidnightNavy else TextPrimary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val recommendations = EducationalTaxonomy.getGuidedRecommendations(educationStage, selectedInterestKey)
            recommendations.forEach { rec ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MidnightNavy),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectRecommendation(rec) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rec.goalTitle,
                                style = Typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rec.rationale,
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 5: Context-Aware Skill Assessment
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step5AssessmentScreen(
    goalTitle: String,
    skills: Map<String, Int>,
    onSkillScoreChange: (String, Int) -> Unit,
    onSkipSkill: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                StepHeader(
                    currentStep = 5,
                    title = "Diagnostic Self-Assessment",
                    onBack = onBack
                )

                // Context Reason Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Core competencies required for $goalTitle.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Rate your current familiarity with each skill (0 to 5), or click 'Skip' to leave it unassessed for baseline diagnostic quizzes.",
                    style = Typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            items(skills.keys.toList()) { skill ->
                val currentScore = skills[skill] ?: -1
                val isUnassessed = (currentScore < 0)

                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = skill,
                                style = Typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            // Skip action button
                            OutlinedButton(
                                onClick = { onSkipSkill(skill) },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isUnassessed) AmberWarning else DeepBlueBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isUnassessed) AmberWarning else TextSecondary
                                )
                            ) {
                                Text(
                                    text = if (isUnassessed) "Unassessed" else "Skip",
                                    style = Typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 0 to 5 score selector chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (0..5).forEach { score ->
                                val isSelected = (currentScore == score)
                                val label = when (score) {
                                    0 -> "0 Novice"
                                    1 -> "1 Beginner"
                                    2 -> "2 Elementary"
                                    3 -> "3 Intermediate"
                                    4 -> "4 Advanced"
                                    5 -> "5 Expert"
                                    else -> "$score"
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricBlue else MidnightNavy)
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricBlue else DeepBlueBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSkillScoreChange(skill, score) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = Typography.labelSmall,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue to Study Preferences",
                color = Color.White,
                style = Typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 6: Learning Preferences and Availability
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step6PreferencesScreen(
    availableHours: Int,
    onHoursChange: (Int) -> Unit,
    selectedDays: List<String>,
    onToggleDay: (String) -> Unit,
    sessionMinutes: Int,
    onSessionMinutesChange: (Int) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    targetTimeline: String,
    onTimelineChange: (String) -> Unit,
    selectedStyles: List<String>,
    onToggleStyle: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                StepHeader(
                    currentStep = 6,
                    title = "Study Schedule & Style",
                    onBack = onBack
                )

                // Time Invariant Notice Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Strict Daily Time Limit: ${availableHours} hrs/day (${availableHours * 60} mins max)",
                                style = Typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LearnMate guarantees that total scheduled tasks on any day will never exceed your limit.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Available hours slider
                Text(
                    text = "Daily Study Commitment: $availableHours hour(s) / day",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = availableHours.toFloat(),
                    onValueChange = { onHoursChange(it.toInt()) },
                    valueRange = 1f..6f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = DeepBlueBorder
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Preferred Study Days
                Text(
                    text = "Available Study Days (${selectedDays.size} selected)",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { day ->
                        val isSel = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSel) ElectricBlue else DeepBlueCard)
                                .border(
                                    1.dp,
                                    if (isSel) ElectricBlue else DeepBlueBorder,
                                    CircleShape
                                )
                                .clickable { onToggleDay(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.take(2),
                                style = Typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Session Length
                Text(
                    text = "Preferred Session Duration",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(30 to "30 mins", 45 to "45 mins", 60 to "60 mins").forEach { (mins, label) ->
                        val isSel = (sessionMinutes == mins)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) DeepBlueCard else MidnightNavy)
                                .border(
                                    1.dp,
                                    if (isSel) ElectricBlue else DeepBlueBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSessionMinutesChange(mins) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelSmall,
                                color = if (isSel) ElectricBlue else TextSecondary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Learning Style Preferences
                Text(
                    text = "Learning Style Preferences",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val styles = listOf(
                    "Practical Projects",
                    "Theory & Concepts First",
                    "Visuals & Diagrams",
                    "Practice Questions & Drills"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    styles.forEach { style ->
                        val isSel = selectedStyles.contains(style)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) DeepBlueCard else MidnightNavy)
                                .border(
                                    1.dp,
                                    if (isSel) SoftViolet else DeepBlueBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onToggleStyle(style) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = style,
                                style = Typography.labelSmall,
                                color = if (isSel) SoftViolet else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Target Timeline
                Text(
                    text = "Target Completion Timeline",
                    style = Typography.labelMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1 Month", "3 Months", "6 Months", "1 Year").forEach { time ->
                        val isSel = (targetTimeline == time)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) DeepBlueCard else MidnightNavy)
                                .border(
                                    1.dp,
                                    if (isSel) EmeraldGreen else DeepBlueBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onTimelineChange(time) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = time,
                                style = Typography.labelSmall,
                                color = if (isSel) EmeraldGreen else TextSecondary,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Review & Confirm Profile",
                color = Color.White,
                style = Typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 7: Profile Review Screen
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepProfileReview(
    context: com.learnmate.ai.domain.onboarding.OnboardingContext,
    assessedCount: Int,
    unassessedCount: Int,
    onEditStep: (Int) -> Unit,
    onGenerate: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "Verify Your Personalization",
                        style = Typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Honest AI Status Badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AmberWarning)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Knowledge Taxonomy Active",
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Deterministic curriculum synthesis active. On-device Llama reasoning engine interface ready for ExecuTorch deployment.",
                            style = Typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Review items with 1-tap edit shortcuts
                ReviewCardItem(
                    stepNumber = 1,
                    title = "Learner Identity",
                    content = "${context.name} • ${context.learnerType?.displayName ?: "Learner"} (${context.educationStage?.displayName ?: "Stage"})",
                    onEdit = { onEditStep(1) }
                )

                ReviewCardItem(
                    stepNumber = 2,
                    title = "Educational Stream",
                    content = context.getEffectiveStream(),
                    onEdit = { onEditStep(2) }
                )

                ReviewCardItem(
                    stepNumber = 3,
                    title = "Selected Pathway",
                    content = context.getEffectivePathway(),
                    onEdit = { onEditStep(3) }
                )

                ReviewCardItem(
                    stepNumber = 4,
                    title = "Target Objective",
                    content = context.getEffectiveGoalTitle(),
                    onEdit = { onEditStep(4) }
                )

                ReviewCardItem(
                    stepNumber = 5,
                    title = "Skill Diagnostics",
                    content = "$assessedCount assessed • $unassessedCount unassessed (scheduled for baseline quizzes)",
                    onEdit = { onEditStep(5) }
                )

                ReviewCardItem(
                    stepNumber = 6,
                    title = "Study Budget",
                    content = "${context.availableHoursPerDay} hrs/day (${context.availableHoursPerDay * 60} mins max) • ${context.preferredStudyDays.size} days/week • ${context.targetTimeline}",
                    onEdit = { onEditStep(6) }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate My Learning Pathway",
                    color = Color.White,
                    style = Typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReviewCardItem(
    stepNumber: Int,
    title: String,
    content: String,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepBlueCard),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step $stepNumber: $title",
                    style = Typography.labelSmall,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = content,
                    style = Typography.bodyMedium,
                    color = TextPrimary
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Step $stepNumber",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Step 8: Pathway Generation Animation
// -------------------------------------------------------------------------------------------------
@Composable
private fun StepPathwayGeneration(
    goal: String,
    onFinish: () -> Unit
) {
    var stepText by remember { mutableStateOf("Analyzing learner profile and prerequisites...") }
    var progress by remember { mutableFloatStateOf(0.15f) }

    LaunchedEffect(Unit) {
        delay(700)
        stepText = "Mapping domain competencies for $goal..."
        progress = 0.40f

        delay(700)
        stepText = "Sequencing 3-phase curriculum & practical projects..."
        progress = 0.70f

        delay(700)
        stepText = "Applying daily time budget constraints..."
        progress = 0.95f

        delay(600)
        progress = 1.0f
        delay(300)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DeepBlueCard)
                    .border(2.dp, ElectricBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Personalizing Your Pathway",
                style = Typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = goal,
                style = Typography.titleSmall,
                color = SoftViolet,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ElectricBlue,
                trackColor = DeepBlueBorder
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stepText,
                style = Typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
