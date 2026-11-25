package com.learnmate.ai.domain

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.domain.engine.GoalBasedLearningEngine
import com.learnmate.ai.domain.onboarding.DeterministicTaxonomyProvider
import com.learnmate.ai.domain.onboarding.JsonSchemaValidator
import com.learnmate.ai.domain.onboarding.OnboardingFlowController
import com.learnmate.ai.domain.taxonomy.EducationalTaxonomy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3.2 Acceptance Test Suites:
 * Tests 10 complete branching scenarios, dependent state invalidation,
 * JSON schema validation, and daily time scheduling constraints.
 */
class ContextAwareOnboardingScenariosTest {

    private val provider = DeterministicTaxonomyProvider()

    // =============================================================================================
    // Scenario 1: School student choosing Science
    // =============================================================================================
    @Test
    fun testScenario1_SchoolStudentScience() {
        val controller = OnboardingFlowController(provider)

        // Step 1: Identity & Stage
        controller.updateStep1(
            name = "Aarav",
            learnerType = EducationalTaxonomy.LearnerType.SCHOOL_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.HIGH_SCHOOL_9_10,
            stageDetail = "10th Standard",
            institution = "Delhi Public School"
        )
        assertTrue(controller.canAdvance(1))
        controller.navigateToStep(2)

        // Step 2: Stream (Must show school subjects, NOT degrees!)
        val step2 = controller.getCurrentStepResponse()
        assertEquals(2, step2.stepNumber)
        assertTrue(step2.options.any { it.id == "school_science" })
        assertTrue(step2.options.any { it.id == "school_math" })
        assertFalse(step2.options.any { it.id == "ug_engineering" })
        assertFalse(step2.options.any { it.id == "ug_commerce" })

        controller.updateStep2("school_science", "Science (Physics, Chemistry, Biology)")
        assertTrue(controller.canAdvance(2))
        controller.navigateToStep(3)

        // Step 3: Pathway
        val step3 = controller.getCurrentStepResponse()
        assertEquals(3, step3.stepNumber)
        assertTrue(step3.options.any { it.id == "sch_phy_chem" })
        assertFalse(step3.options.any { it.id == "eng_entc" })
        assertFalse(step3.options.any { it.id == "comm_ca_cma_cs" })

        controller.updateStep3("sch_phy_chem", "Physics & Chemistry Core Foundations")
        assertTrue(controller.canAdvance(3))
        controller.navigateToStep(4)

        // Step 4: Goal
        val step4 = controller.getCurrentStepResponse()
        assertEquals(4, step4.stepNumber)
        assertTrue(step4.options.any { it.label.contains("Board Exam", ignoreCase = true) })

        controller.updateStep4(
            goalTitle = "Class Science Board Exam Preparation",
            goalType = EducationalTaxonomy.GoalType.ACADEMIC_SUBJECT
        )
        assertTrue(controller.canAdvance(4))
        controller.navigateToStep(5)

        // Step 5: Competencies
        val step5 = controller.getCurrentStepResponse()
        assertEquals(5, step5.stepNumber)
        val skills = controller.context.assessedSkills
        assertTrue("Expected science competencies, found empty", skills.isNotEmpty())
        assertTrue(skills.keys.any { it.contains("Newton", ignoreCase = true) || it.contains("Physics", ignoreCase = true) })
        assertFalse(skills.keys.any { it.contains("C Programming", ignoreCase = true) })
        assertFalse(skills.keys.any { it.contains("Taxation", ignoreCase = true) })

        // Step 6: Preferences & Strict 1-hour constraint
        controller.navigateToStep(6)
        controller.updateStep6(
            availableHours = 1,
            studyDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            sessionMinutes = 30,
            language = "English",
            timeline = "3 Months",
            preferences = listOf("Visuals & Diagrams")
        )
        assertTrue(controller.canAdvance(6))

        // Verify scheduled daily time <= 60 minutes
        val profile = LearnerProfileEntity(
            name = controller.context.name,
            availableHoursPerDay = controller.context.availableHoursPerDay,
            targetDomain = controller.context.getEffectiveStream(),
            targetGoalTitle = controller.context.getEffectiveGoalTitle(),
            preferredSessionDurationMinutes = controller.context.preferredSessionMinutes
        )
        val pathway = GoalBasedLearningEngine.generatePathway(profile, skills)
        pathway.initialDailyTasks.groupBy { it.scheduledDate }.forEach { (_, tasks) ->
            val totalMins = tasks.sumOf { it.durationMinutes }
            assertTrue("Day exceeded 60m: $totalMins", totalMins <= 60)
        }
    }

    // =============================================================================================
    // Scenario 2: Junior college student choosing Commerce and then CA Foundation
    // =============================================================================================
    @Test
    fun testScenario2_JuniorCollegeCommerceCAFoundation() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Priya",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.JUNIOR_COLLEGE_11_12,
            stageDetail = "12th Standard Commerce"
        )
        controller.navigateToStep(2)

        val step2 = controller.getCurrentStepResponse()
        assertTrue(step2.options.any { it.id == "jc_commerce" })
        controller.updateStep2("jc_commerce", "Commerce & Business Studies")
        controller.navigateToStep(3)

        val step3 = controller.getCurrentStepResponse()
        assertTrue(step3.options.any { it.id == "comm_ca_cma_cs" })
        controller.updateStep3("comm_ca_cma_cs", "Chartered Accountancy & Professional Certifications")
        controller.navigateToStep(4)

        val step4 = controller.getCurrentStepResponse()
        assertTrue(step4.options.any { it.label.contains("CA Foundation", ignoreCase = true) })
        controller.updateStep4("CA Foundation Examination", EducationalTaxonomy.GoalType.ENTRANCE_EXAM)
        controller.navigateToStep(5)

        val skills = controller.context.assessedSkills
        assertTrue(skills.keys.any { it.contains("Accounting", ignoreCase = true) })
        assertTrue(skills.keys.any { it.contains("Law", ignoreCase = true) })
        assertFalse(skills.keys.any { it.contains("C Programming", ignoreCase = true) })
        assertFalse(skills.keys.any { it.contains("Circuit", ignoreCase = true) })

        controller.navigateToStep(6)
        controller.updateStep6(
            availableHours = 2,
            studyDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
            sessionMinutes = 45,
            language = "English",
            timeline = "6 Months",
            preferences = listOf("Practice Questions & Drills")
        )

        val profile = LearnerProfileEntity(
            name = controller.context.name,
            availableHoursPerDay = 2,
            targetDomain = "Commerce",
            targetGoalTitle = "CA Foundation Examination",
            preferredSessionDurationMinutes = 45
        )
        val pathway = GoalBasedLearningEngine.generatePathway(profile, skills)
        pathway.initialDailyTasks.groupBy { it.scheduledDate }.forEach { (_, tasks) ->
            val totalMins = tasks.sumOf { it.durationMinutes }
            assertTrue("Day exceeded 120m: $totalMins", totalMins <= 120)
        }
    }

    // =============================================================================================
    // Scenario 3: Engineering student choosing ENTC and then Embedded Systems
    // =============================================================================================
    @Test
    fun testScenario3_EngineeringStudentENTCEmbeddedSystems() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Rohan",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE,
            stageDetail = "B.E. 3rd Year"
        )
        controller.navigateToStep(2)

        controller.updateStep2("ug_engineering", "Engineering & Technology (B.Tech / B.E)")
        controller.navigateToStep(3)

        val step3 = controller.getCurrentStepResponse()
        assertTrue(step3.options.any { it.id == "eng_entc" })
        controller.updateStep3("eng_entc", "Electronics & Telecommunication (ENTC / ECE)")
        controller.navigateToStep(4)

        val step4 = controller.getCurrentStepResponse()
        assertTrue(step4.options.any { it.label.contains("Embedded Systems Engineer", ignoreCase = true) })
        controller.updateStep4("Embedded Systems Engineer", EducationalTaxonomy.GoalType.CAREER_GOAL)
        controller.navigateToStep(5)

        val skills = controller.context.assessedSkills
        assertTrue(skills.keys.any { it.contains("C Programming", ignoreCase = true) })
        assertTrue(skills.keys.any { it.contains("Microcontrollers", ignoreCase = true) })
        assertTrue(skills.keys.any { it.contains("RTOS", ignoreCase = true) })
    }

    // =============================================================================================
    // Scenario 4: BCA student choosing Software Development
    // =============================================================================================
    @Test
    fun testScenario4_BCAStudentSoftwareDevelopment() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Vikram",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE,
            stageDetail = "BCA 2nd Year"
        )
        controller.navigateToStep(2)

        val step2 = controller.getCurrentStepResponse()
        assertTrue(step2.options.any { it.id == "ug_bca" })
        controller.updateStep2("ug_bca", "Computer Applications (BCA / B.Sc IT)")
        controller.navigateToStep(3)

        val step3 = controller.getCurrentStepResponse()
        assertTrue(step3.options.any { it.id == "bca_software_dev" })
        controller.updateStep3("bca_software_dev", "Software Development (Full-Stack & Apps)")
        controller.navigateToStep(4)

        val step4 = controller.getCurrentStepResponse()
        assertTrue(step4.options.any { it.label.contains("Full-Stack", ignoreCase = true) })
        controller.updateStep4("Full-Stack Software Developer", EducationalTaxonomy.GoalType.CAREER_GOAL)
        controller.navigateToStep(5)

        val skills = controller.context.assessedSkills
        assertTrue(skills.keys.any { it.contains("TypeScript", ignoreCase = true) || it.contains("JavaScript", ignoreCase = true) })
        assertTrue(skills.keys.any { it.contains("React", ignoreCase = true) || it.contains("Frontend", ignoreCase = true) })
    }

    // =============================================================================================
    // Scenario 5: MCA student choosing AI and Machine Learning
    // =============================================================================================
    @Test
    fun testScenario5_MCAStudentAIML() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Ananya",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.POSTGRADUATE,
            stageDetail = "MCA Final Year"
        )
        controller.navigateToStep(2)

        val step2 = controller.getCurrentStepResponse()
        assertTrue(step2.options.any { it.id == "pg_mca" })
        controller.updateStep2("pg_mca", "Computer Applications & AI (MCA)")
        controller.navigateToStep(3)

        val step3 = controller.getCurrentStepResponse()
        assertTrue(step3.options.any { it.id == "mca_ai_ml" })
        controller.updateStep3("mca_ai_ml", "Artificial Intelligence & Machine Learning")
        controller.navigateToStep(4)

        val step4 = controller.getCurrentStepResponse()
        assertTrue(step4.options.any { it.label.contains("Machine Learning", ignoreCase = true) })
        controller.updateStep4("AI & Machine Learning Engineer", EducationalTaxonomy.GoalType.CAREER_GOAL)
        controller.navigateToStep(5)

        val skills = controller.context.assessedSkills
        assertTrue(skills.keys.any { it.contains("Python", ignoreCase = true) })
        assertTrue(skills.keys.any { it.contains("PyTorch", ignoreCase = true) || it.contains("Deep Learning", ignoreCase = true) })
    }

    // =============================================================================================
    // Scenario 6: Medical student choosing MBBS and postgraduate preparation
    // =============================================================================================
    @Test
    fun testScenario6_MedicalStudentMBBSPostgraduatePrep() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Dr. Siddharth",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE,
            stageDetail = "MBBS Intern"
        )
        controller.navigateToStep(2)

        val step2 = controller.getCurrentStepResponse()
        assertTrue(step2.options.any { it.id == "ug_medicine" })
        controller.updateStep2("ug_medicine", "Medicine & Healthcare (MBBS)")
        controller.navigateToStep(3)

        val step3 = controller.getCurrentStepResponse()
        assertTrue(step3.options.any { it.id == "med_pg_prep" })
        controller.updateStep3("med_pg_prep", "Postgraduate Medical Exam Preparation (NEET PG)")
        controller.navigateToStep(4)

        val step4 = controller.getCurrentStepResponse()
        assertTrue(step4.options.any { it.label.contains("NEET PG", ignoreCase = true) })
        controller.updateStep4("NEET PG / INI-CET Medical Entrance", EducationalTaxonomy.GoalType.ENTRANCE_EXAM)
        controller.navigateToStep(5)

        val skills = controller.context.assessedSkills
        assertTrue(skills.keys.any { it.contains("Medicine", ignoreCase = true) || it.contains("Surgery", ignoreCase = true) })
        assertFalse(skills.keys.any { it.contains("C Programming", ignoreCase = true) })
    }

    // =============================================================================================
    // Scenario 7: Arts student choosing Digital Illustration
    // =============================================================================================
    @Test
    fun testScenario7_ArtsStudentDigitalIllustration() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Meera",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE,
            stageDetail = "BFA 3rd Year"
        )
        controller.navigateToStep(2)

        val step2 = controller.getCurrentStepResponse()
        assertTrue(step2.options.any { it.id == "ug_arts" })
        controller.updateStep2("ug_arts", "Arts & Humanities (BFA)")
        controller.navigateToStep(3)

        val step3 = controller.getCurrentStepResponse()
        assertTrue(step3.options.any { it.id == "arts_illustration" })
        controller.updateStep3("arts_illustration", "Digital Illustration & Concept Art")
        controller.navigateToStep(4)

        val step4 = controller.getCurrentStepResponse()
        assertTrue(step4.options.any { it.label.contains("Illustrator", ignoreCase = true) })
        controller.updateStep4("Professional Digital Illustrator", EducationalTaxonomy.GoalType.CAREER_GOAL)
        controller.navigateToStep(5)

        val skills = controller.context.assessedSkills
        assertTrue(skills.keys.any { it.contains("Drawing", ignoreCase = true) || it.contains("Color Theory", ignoreCase = true) })
    }

    // =============================================================================================
    // Scenario 8: Commerce student choosing unrelated custom goal (learning Python)
    // =============================================================================================
    @Test
    fun testScenario8_CommerceStudentUnrelatedCustomGoalPython() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Dev",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
        )
        controller.navigateToStep(2)
        controller.updateStep2("ug_commerce", "Commerce & Management (B.Com)")
        controller.navigateToStep(3)
        controller.updateStep3("comm_accounting", "Accounting & Financial Reporting")
        controller.navigateToStep(4)

        // Learner enters an UNRELATED custom goal
        controller.updateStep4(
            goalTitle = "Learning Python Programming for Financial Data",
            goalType = EducationalTaxonomy.GoalType.CUSTOM_GOAL,
            customGoalTitle = "Learning Python Programming for Financial Data"
        )
        controller.navigateToStep(5)

        // Competencies MUST adapt to Python, NOT stay on balance sheets or accounting!
        val skills = controller.context.assessedSkills
        assertTrue("Expected Python skills, got: ${skills.keys}",
            skills.keys.any { it.contains("Python", ignoreCase = true) || it.contains("Pandas", ignoreCase = true) })
        assertFalse(skills.keys.any { it.contains("Trial Balance", ignoreCase = true) })
    }

    // =============================================================================================
    // Scenario 9: Self-learner choosing Professional Photography
    // =============================================================================================
    @Test
    fun testScenario9_SelfLearnerProfessionalPhotography() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Aditya",
            learnerType = EducationalTaxonomy.LearnerType.SELF_LEARNER,
            educationStage = EducationalTaxonomy.EducationStage.HOBBY_PASSION
        )
        controller.navigateToStep(2)

        val step2 = controller.getCurrentStepResponse()
        assertTrue(step2.options.any { it.id == "self_creative" })
        controller.updateStep2("self_creative", "Creative Arts, Photography & Media")
        controller.navigateToStep(3)

        val step3 = controller.getCurrentStepResponse()
        assertTrue(step3.options.any { it.id == "hobby_photography" })
        controller.updateStep3("hobby_photography", "Professional Photography & Lightroom")
        controller.navigateToStep(4)

        val step4 = controller.getCurrentStepResponse()
        assertTrue(step4.options.any { it.label.contains("Photographer", ignoreCase = true) })
        controller.updateStep4("Professional Photographer & Visual Storyteller", EducationalTaxonomy.GoalType.CAREER_GOAL)
        controller.navigateToStep(5)

        val skills = controller.context.assessedSkills
        assertTrue(skills.keys.any { it.contains("Exposure", ignoreCase = true) || it.contains("Lighting", ignoreCase = true) || it.contains("Lightroom", ignoreCase = true) })
    }

    // =============================================================================================
    // Scenario 10: Learner who does not know what they want ("Help me decide")
    // =============================================================================================
    @Test
    fun testScenario10_UndecidedLearnerHelpMeDecide() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1(
            name = "Karan",
            learnerType = EducationalTaxonomy.LearnerType.COLLEGE_STUDENT,
            educationStage = EducationalTaxonomy.EducationStage.UNDERGRADUATE
        )
        controller.navigateToStep(2)
        controller.updateStep2("ug_bca", "Computer Applications (BCA)")
        controller.navigateToStep(3)
        controller.updateStep3("bca_software_dev", "Software Development")
        controller.navigateToStep(4)

        // Learner activates "Help Me Decide"
        controller.activateHelpMeDecide()
        assertTrue(controller.context.isHelpMeDecideActive)

        // Get recommendations for interest "tech"
        val recommendations = EducationalTaxonomy.getGuidedRecommendations(
            stage = EducationalTaxonomy.EducationStage.UNDERGRADUATE,
            interestKey = "tech"
        )
        assertTrue(recommendations.isNotEmpty())
        val selectedRec = recommendations.first()

        // Apply recommendation
        controller.applyRecommendation(selectedRec)
        assertFalse(controller.context.isHelpMeDecideActive)
        assertEquals(selectedRec.goalTitle, controller.context.selectedGoalTitle)
        assertEquals(selectedRec.pathwayId, controller.context.selectedPathwayId)

        // Advance to step 5 and verify competencies were generated
        controller.navigateToStep(5)
        assertTrue(controller.context.assessedSkills.isNotEmpty())
    }

    // =============================================================================================
    // State Invalidation Test (Crucial Architecture Requirement)
    // =============================================================================================
    @Test
    fun testStateInvalidationWhenEarlierAnswersChange() {
        val controller = OnboardingFlowController(provider)

        controller.updateStep1("Rohan", EducationalTaxonomy.LearnerType.COLLEGE_STUDENT, EducationalTaxonomy.EducationStage.UNDERGRADUATE)
        controller.updateStep2("ug_engineering", "Engineering")
        controller.updateStep3("eng_entc", "ENTC")
        controller.updateStep4("Embedded Systems Engineer", EducationalTaxonomy.GoalType.CAREER_GOAL)
        controller.initializeAssessmentSkills()
        controller.updateSkillRating("C Programming", 3)

        assertEquals("ug_engineering", controller.context.selectedStreamId)
        assertEquals("eng_entc", controller.context.selectedPathwayId)
        assertEquals("Embedded Systems Engineer", controller.context.selectedGoalTitle)
        assertFalse(controller.context.assessedSkills.isEmpty())

        // Learner navigates back to Step 2 and changes stream to Commerce!
        controller.updateStep2("ug_commerce", "Commerce")

        // Downstream MUST be invalidated:
        assertEquals("ug_commerce", controller.context.selectedStreamId)
        assertNull("Pathway must be reset", controller.context.selectedPathwayId)
        assertNull("Goal must be reset", controller.context.selectedGoalTitle)
        assertTrue("Assessed skills must be cleared", controller.context.assessedSkills.isEmpty())

        // Learner navigates back to Step 1 and changes learner type to School!
        controller.updateStep1("Rohan", EducationalTaxonomy.LearnerType.SCHOOL_STUDENT, EducationalTaxonomy.EducationStage.HIGH_SCHOOL_9_10)

        // Stream, Pathway, Goal, Skills must all be cleared:
        assertNull("Stream must be reset", controller.context.selectedStreamId)
        assertNull("Pathway must be reset", controller.context.selectedPathwayId)
        assertNull("Goal must be reset", controller.context.selectedGoalTitle)
        assertTrue("Skills must be cleared", controller.context.assessedSkills.isEmpty())
    }

    // =============================================================================================
    // JSON Schema Validation Tests
    // =============================================================================================
    @Test
    fun testJsonSchemaValidation_ValidAndMalformed() {
        val validJson = """
        {
          "step_id": "commerce_pathway",
          "step_number": 3,
          "title": "Explore Commerce Pathways",
          "question": "Which area of Commerce interests you?",
          "description": "Let's explore pathways that match your interests.",
          "context_explanation": "Based on your Commerce background",
          "options": [
            {
              "id": "accounting",
              "label": "Accounting and Finance",
              "description": "Explore accounting, taxation and financial management",
              "category_tag": "Discipline"
            }
          ],
          "allow_custom_input": true,
          "allow_skip": false,
          "allow_undecided": true,
          "next_step_type": "goal"
        }
        """.trimIndent()

        val validResult = JsonSchemaValidator.validateAndParse(validJson)
        assertTrue(validResult.isValid)
        assertNotNull(validResult.stepResponse)
        assertEquals("commerce_pathway", validResult.stepResponse?.stepId)
        assertEquals(1, validResult.stepResponse?.options?.size)

        // Malformed JSON (missing options)
        val invalidJson = """
        {
          "step_id": "invalid_step",
          "question": "Where are options?"
        }
        """.trimIndent()

        val invalidResult = JsonSchemaValidator.validateAndParse(invalidJson)
        assertFalse(invalidResult.isValid)
        assertNotNull(invalidResult.errorMessage)
    }

    // =============================================================================================
    // Strict Daily Time Scheduling Invariant Test
    // =============================================================================================
    @Test
    fun testStrictDailyTimeConstraints() {
        val budgets = listOf(1, 2, 3, 4)

        budgets.forEach { hours ->
            val maxAllowedMins = hours * 60
            val profile = LearnerProfileEntity(
                name = "Test Learner",
                availableHoursPerDay = hours,
                targetDomain = "Computer Science",
                targetGoalTitle = "Full-Stack Software Developer",
                preferredSessionDurationMinutes = 45
            )

            val pathway = GoalBasedLearningEngine.generatePathway(profile, emptyMap())
            pathway.initialDailyTasks.groupBy { it.scheduledDate }.forEach { (date, tasks) ->
                val totalMins = tasks.sumOf { it.durationMinutes }
                assertTrue(
                    "Date $date exceeded limit of $maxAllowedMins mins: actual $totalMins mins",
                    totalMins <= maxAllowedMins
                )
            }
        }
    }
}
