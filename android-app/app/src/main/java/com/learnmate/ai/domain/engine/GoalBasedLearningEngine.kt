package com.learnmate.ai.domain.engine

import com.learnmate.ai.data.local.entities.LearnerProfileEntity
import com.learnmate.ai.data.local.entities.RoadmapPhaseEntity
import com.learnmate.ai.data.local.entities.SkillEntity
import com.learnmate.ai.data.local.entities.StudyTaskEntity
import com.learnmate.ai.domain.model.GapPriority
import com.learnmate.ai.domain.model.SkillGap

/**
 * Domain-Independent Learning Engine (Parts 3, 4, 5).
 * Generates tailored learning pathways for arbitrary goals across:
 * - Arts & Humanities, Commerce, Science, Engineering, Medical, Law, Languages,
 *   Competitive Exams (CA Foundation, NEET, etc.), Creative Skills, and Custom Goals.
 *
 * Enforces strict daily time constraints: Total scheduled daily tasks must NEVER
 * exceed (availableHoursPerDay * 60) minutes.
 */
object GoalBasedLearningEngine {

    data class GeneratedPathway(
        val learningObjective: String,
        val prerequisiteKnowledge: List<String>,
        val skills: List<SkillEntity>,
        val skillGaps: List<SkillGap>,
        val phases: List<RoadmapPhaseEntity>,
        val initialDailyTasks: List<StudyTaskEntity>,
        val estimatedWeeks: Int,
        val isCustomDomainFallback: Boolean = false
    )

    /**
     * Suggests relevant skills for the learner to assess based on their selected goal and domain.
     */
    fun suggestCompetenciesForGoal(domain: String, customGoal: String): List<String> {
        val lowerGoal = customGoal.lowercase().trim()

        return when {
            // Commerce & Competitive Exams
            lowerGoal.contains("ca foundation") || lowerGoal.contains("chartered account") -> listOf(
                "Principles & Practice of Accounting",
                "Business Law & Commercial Knowledge",
                "Business Mathematics & Statistics",
                "Business Economics",
                "Logical Reasoning & Quantitative Aptitude"
            )
            lowerGoal.contains("accounting") || lowerGoal.contains("finance") || lowerGoal.contains("commerce") -> listOf(
                "Financial Accounting & Double Entry",
                "Cost Accounting & Budgeting",
                "Financial Statement Analysis",
                "Taxation & Regulatory Frameworks",
                "Spreadsheet Modeling (Excel)"
            )

            // Python (Custom goal, e.g., Commerce student learning Python)
            lowerGoal.contains("python") -> listOf(
                "Python Syntax & Data Structures",
                "Functions, Lambdas & Modular Code",
                "Data Analysis with Pandas & NumPy",
                "Object-Oriented Programming in Python",
                "Automation Scripts & API Requests"
            )

            // BCA / Software Development
            lowerGoal.contains("software dev") || lowerGoal.contains("full stack") || lowerGoal.contains("full-stack") -> listOf(
                "JavaScript & TypeScript",
                "React & Modern Frontend Architecture",
                "Node.js & Backend REST APIs",
                "Database Design & SQL (PostgreSQL)",
                "Version Control & Git Workflows"
            )

            // MCA / AI & Machine Learning
            lowerGoal.contains("ai & machine learning") || lowerGoal.contains("ai/ml") || (lowerGoal.contains("ai") && lowerGoal.contains("learning")) -> listOf(
                "Python for Machine Learning",
                "Linear Algebra & Multivariable Calculus",
                "Supervised & Unsupervised Learning (Scikit-Learn)",
                "Deep Neural Networks & PyTorch",
                "Model Evaluation, Validation & MLOps"
            )

            // Photography (Self-Learner)
            lowerGoal.contains("photography") || lowerGoal.contains("photo") -> listOf(
                "Manual Camera Exposure (Aperture, Shutter, ISO)",
                "Composition (Rule of Thirds, Leading Lines)",
                "Natural & Studio Lighting Techniques",
                "Portrait & Street Photography",
                "Adobe Lightroom & RAW Digital Workflow"
            )

            // Medical: NEET PG / Clinical Practice
            lowerGoal.contains("neet pg") || lowerGoal.contains("postgraduate medical") || lowerGoal.contains("clinical physician") -> listOf(
                "Internal Medicine & Clinical Vignettes",
                "General Surgery & Emergency Cases",
                "Obstetrics & Gynecology Clinical Protocols",
                "Clinical Pharmacology & Drug Interactions",
                "Systemic Pathology & Differential Diagnosis"
            )

            // Medical & NEET UG
            lowerGoal.contains("neet") || lowerGoal.contains("medical entrance") -> listOf(
                "Human Physiology & Anatomy",
                "Cell Biology & Genetics",
                "Plant Physiology & Diversity",
                "Organic & Inorganic Chemistry",
                "Physics: Mechanics & Electrodynamics"
            )

            // School Science
            lowerGoal.contains("school science") || lowerGoal.contains("board exam") || lowerGoal.contains("junior science") -> listOf(
                "Newton's Laws & Force Mechanics",
                "Atomic Structure & Chemical Bonding",
                "Human Organ Systems & Cellular Biology",
                "Mathematical Formula Derivations",
                "Experimental Observations & Practical Questions"
            )
            lowerGoal.contains("medical") || lowerGoal.contains("healthcare") || lowerGoal.contains("nursing") -> listOf(
                "Human Anatomy & Physiology",
                "Medical Terminology & Pathology",
                "Pharmacology Basics",
                "Clinical Assessment & Vital Signs",
                "Healthcare Ethics & Patient Care"
            )

            // Arts & Design
            lowerGoal.contains("illustration") || lowerGoal.contains("drawing") || lowerGoal.contains("digital art") -> listOf(
                "Drawing Fundamentals & Form",
                "Composition & Perspective",
                "Color Theory & Lighting",
                "Digital Painting Tools & Layers",
                "Character & Concept Design"
            )
            lowerGoal.contains("graphic design") || lowerGoal.contains("ui") || lowerGoal.contains("ux") -> listOf(
                "Typography & Hierarchy",
                "Color Systems & Branding",
                "Layout & Visual Composition",
                "Vector Graphics & Tooling",
                "User Research & Wireframing"
            )

            // Languages & Communication
            lowerGoal.contains("english") || lowerGoal.contains("communication") || lowerGoal.contains("speaking") -> listOf(
                "Grammar & Sentence Mechanics",
                "Active Vocabulary & Idioms",
                "Listening Comprehension & Pronunciation",
                "Spoken Fluency & Everyday Dialogue",
                "Professional Writing & Presentation"
            )

            // Law
            lowerGoal.contains("law") || lowerGoal.contains("legal") || lowerGoal.contains("constitution") -> listOf(
                "Constitutional Law & Governance",
                "Law of Contracts & Mercantile Law",
                "Criminal Law & Procedure",
                "Legal Drafting & Case Analysis",
                "Jurisprudence & Statutory Interpretation"
            )

            // Engineering / Technical
            lowerGoal.contains("embedded") -> listOf(
                "C Programming",
                "Microcontrollers",
                "RTOS",
                "Digital Electronics",
                "Communication Protocols"
            )
            lowerGoal.contains("ai") || lowerGoal.contains("machine learning") || lowerGoal.contains("data science") -> listOf(
                "Python Programming",
                "Linear Algebra & Calculus",
                "Machine Learning Algorithms",
                "Deep Learning & PyTorch",
                "Data Preprocessing & Feature Engineering"
            )

            // Domain-based fallbacks
            domain.contains("Commerce", ignoreCase = true) -> listOf(
                "Foundations of Commerce",
                "Business Operations & Management",
                "Financial Record Keeping",
                "Economic Principles",
                "Commercial Law Basics"
            )
            domain.contains("Arts", ignoreCase = true) || domain.contains("Creativity", ignoreCase = true) -> listOf(
                "Creative Foundations & Expression",
                "Technique & Form Mastery",
                "History & Cultural Context",
                "Practical Studio / Digital Practice",
                "Portfolio Development"
            )
            domain.contains("Science", ignoreCase = true) -> listOf(
                "Scientific Method & Inquiry",
                "Mathematical Problem Solving",
                "Core Physical & Chemical Principles",
                "Biological & Earth Systems",
                "Experimental Data Analysis"
            )
            domain.contains("Law", ignoreCase = true) -> listOf(
                "Legal Foundations & Institutions",
                "Statutory Reading & Analysis",
                "Case Briefing & Research",
                "Constitutional Principles",
                "Written Advocacy"
            )

            // Universal fallback for any custom goal
            else -> listOf(
                "Foundational Principles of $customGoal",
                "Core Methodologies & Techniques",
                "Critical Tools & Frameworks",
                "Practical Problem Solving in $customGoal",
                "Advanced Real-World Applications"
            )
        }
    }

    /**
     * Synthesizes a structured, domain-independent learning pathway.
     */
    fun generatePathway(
        profile: LearnerProfileEntity,
        assessedSkills: Map<String, Int>
    ): GeneratedPathway {
        val goal = profile.targetGoalTitle.ifBlank { "Personalized Learning Journey" }
        val domain = profile.targetDomain.ifBlank { "General Knowledge" }
        val lowerGoal = goal.lowercase().trim()

        val skillNames = if (assessedSkills.isNotEmpty()) {
            assessedSkills.keys.toList()
        } else {
            suggestCompetenciesForGoal(domain, goal)
        }

        val targetScore = 4
        val skills = mutableListOf<SkillEntity>()
        val skillGaps = mutableListOf<SkillGap>()

        for (skillName in skillNames) {
            val currentLevel = assessedSkills[skillName] ?: -1
            val isAssessed = (currentLevel >= 0)
            val gap = if (isAssessed) maxOf(0, targetScore - currentLevel) else targetScore

            val priority = when {
                !isAssessed -> GapPriority.CRITICAL // unassessed skills need diagnostic attention
                gap == 0 -> GapPriority.NONE
                gap >= 3 -> GapPriority.CRITICAL
                gap == 2 -> GapPriority.MODERATE
                else -> GapPriority.MINOR
            }

            val actions = mutableListOf<String>()
            if (!isAssessed) {
                actions.add("Complete baseline diagnostic quiz for $skillName.")
            } else if (gap > 0) {
                actions.add("Target competency level $targetScore with targeted study modules.")
            } else {
                actions.add("Competency verified. Maintain with periodic review.")
            }

            skills.add(
                SkillEntity(
                    skillName = skillName,
                    currentCompetency = currentLevel,
                    targetCompetency = targetScore,
                    calculatedGap = gap,
                    isAssessed = isAssessed,
                    priority = priority.name,
                    category = if (!isAssessed) "Unassessed Prerequisite" else "Core Competency"
                )
            )

            skillGaps.add(
                SkillGap(
                    skillName = skillName,
                    currentLevel = currentLevel,
                    targetLevel = targetScore,
                    gap = gap,
                    priority = priority,
                    isAssessed = isAssessed,
                    recommendedActions = actions
                )
            )
        }

        // Generate tailored phases
        val phases = generatePhasesForGoal(domain, goal)

        // Generate daily study tasks with STRICT daily time constraints
        val dailyTasks = generateConstrainedDailyTasks(
            phases = phases,
            availableHoursPerDay = profile.availableHoursPerDay,
            sessionDurationMinutes = profile.preferredSessionDurationMinutes,
            primarySkill = skillNames.firstOrNull() ?: "Core Principles"
        )

        return GeneratedPathway(
            learningObjective = "Mastery of $goal with verified competency in core prerequisites.",
            prerequisiteKnowledge = skillNames.take(2),
            skills = skills,
            skillGaps = skillGaps,
            phases = phases,
            initialDailyTasks = dailyTasks,
            estimatedWeeks = phases.sumOf { it.estimatedWeeks }
        )
    }

    private fun generatePhasesForGoal(domain: String, goal: String): List<RoadmapPhaseEntity> {
        val lowerGoal = goal.lowercase().trim()

        return when {
            // CA Foundation
            lowerGoal.contains("ca foundation") || lowerGoal.contains("chartered account") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Mercantile Law & Accounting Foundations",
                    objective = "Master journal entries, ledger reconciliation, trial balance, and Indian Contract Act fundamentals.",
                    topicsCsv = "Double Entry Accounting, Trial Balance & Bank Reconciliation, Indian Contract Act 1872, Sale of Goods Act Basics",
                    practicalProject = "Comprehensive Financial Ledger Reconciliation Case Study",
                    learningOutcomesCsv = "Ability to reconcile accounts, understanding of essential contract validity clauses",
                    estimatedWeeks = 4,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Quantitative Aptitude & Business Economics",
                    objective = "Develop speed and accuracy in commercial mathematics, statistics, and microeconomic theory.",
                    topicsCsv = "Time Value of Money (SI/CI), Differential Calculus Basics, Measures of Dispersion, Demand & Supply Elasticity",
                    practicalProject = "Compound Interest & Investment Appraisal Problem Set",
                    learningOutcomesCsv = "Mastery of TVM formulas and demand curve analysis",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Examination Drills & Comprehensive Revision",
                    objective = "Solve past RTPs, MTPs, and full-length timed mock examinations.",
                    topicsCsv = "Past Exam Question Patterns, Time Management Drills, Negative Marking Strategy, Error Log Remediation",
                    practicalProject = "Full-Length 4-Paper Timed Mock Simulation",
                    learningOutcomesCsv = "Exam readiness and precision under timed conditions",
                    estimatedWeeks = 4
                )
            )

            // NEET
            lowerGoal.contains("neet") || lowerGoal.contains("medical entrance") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Human Physiology & Chemical Bonding Core",
                    objective = "Build in-depth conceptual clarity in high-weightage NCERT biology and inorganic chemistry.",
                    topicsCsv = "Digestive & Circulatory Physiology, Neural Control & Coordination, Periodic Table & Chemical Bonding, Kinematics & Laws of Motion",
                    practicalProject = "NCERT Line-by-Line Fact Mapping & Physiology Flashcard System",
                    learningOutcomesCsv = "Flawless recall of human organ systems and bonding geometries",
                    estimatedWeeks = 4,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Genetics, Organic Chemistry & Electrodynamics",
                    objective = "Master molecular genetics, reaction mechanisms, and circuit physics.",
                    topicsCsv = "Mendelian & Molecular Genetics, Reaction Intermediates & Isomerism, Current Electricity & Magnetism",
                    practicalProject = "Organic Reaction Synthesis Roadmap & Circuit Analysis Workbook",
                    learningOutcomesCsv = "Accuracy in multi-step organic synthesis and genetic cross predictions",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Speed Drills, Error Notebook & Full Syllabus Mocks",
                    objective = "Achieve 180-question accuracy within the 200-minute examination threshold.",
                    topicsCsv = "Formula Cheat-Sheet Rapid Revision, Biology Negative-Marking Avoidance, Timed Full-Length Mocks",
                    practicalProject = "Three Consecutive 720-Mark Timed Simulated Examinations",
                    learningOutcomesCsv = "High-speed question selection and optimal exam stamina",
                    estimatedWeeks = 4
                )
            )

            // Digital Illustration
            lowerGoal.contains("illustration") || lowerGoal.contains("drawing") || lowerGoal.contains("digital art") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Form, Perspective & Gesture Foundations",
                    objective = "Master line confidence, geometric simplification of objects, and vanishing point perspective.",
                    topicsCsv = "Line Weight & Contour Drawing, 1-Point & 2-Point Perspective, Basic 3D Volumes (Box/Sphere/Cylinder), Gesture Sketching",
                    practicalProject = "Daily 30-Minute Gesture & Perspective Workbook",
                    learningOutcomesCsv = "Confidence in sketching three-dimensional forms from imagination",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Lighting, Value & Digital Brushwork",
                    objective = "Understand light source physics, ambient occlusion, digital layer blending modes, and color palettes.",
                    topicsCsv = "5-Value Scale & Cast Shadows, Digital Brushes & Edge Control, Color Temperature (Warm vs Cool), Ambient Occlusion",
                    practicalProject = "Rendered Still Life Study with Dynamic Light Source",
                    learningOutcomesCsv = "Mastery of digital shading and atmospheric illumination",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Character Design & Portfolio Project",
                    objective = "Synthesize anatomy, costume, storytelling, and rendered background into a portfolio illustration.",
                    topicsCsv = "Character Silhouette & Thumbnails, Costume Detailing, Composition & Focal Points, Final Render Polish",
                    practicalProject = "Complete Storybook Character & Environmental Background",
                    learningOutcomesCsv = "Finished publication-ready digital artwork",
                    estimatedWeeks = 4
                )
            )

            // English Communication
            lowerGoal.contains("english") || lowerGoal.contains("communication") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Sentence Architecture & Active Vocabulary",
                    objective = "Eliminate common grammatical errors, understand verb tenses, and acquire 500 core conversational words.",
                    topicsCsv = "Essential Tenses & Auxiliary Verbs, Subject-Verb Agreement, Collocations & High-Frequency Idioms, Phonics & Pronunciation",
                    practicalProject = "Daily Audio Journal & Self-Correction Log",
                    learningOutcomesCsv = "Grammatically sound spontaneous sentence formation",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Listening Analysis & Spoken Fluency Drills",
                    objective = "Build auditory comprehension and achieve natural conversational rhythm without hesitation.",
                    topicsCsv = "Active Listening & Paraphrasing, Accent Neutralization & Connected Speech, Overcoming Hesitation & Fillers, Situational Role-Play",
                    practicalProject = "5-Minute Recorded Conversational Simulation",
                    learningOutcomesCsv = "Fluid conversational responses without translation delay",
                    estimatedWeeks = 3
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Professional Articulation & Public Speaking",
                    objective = "Present complex ideas clearly in formal, academic, and business environments.",
                    topicsCsv = "Structuring Clear Arguments (PREP Method), Formal Email & Report Writing, Impromptu Speaking, Interview Communication",
                    practicalProject = "Structured 7-Minute Persuasive Presentation",
                    learningOutcomesCsv = "Confident public articulation and formal correspondence",
                    estimatedWeeks = 3
                )
            )

            // Embedded Systems
            lowerGoal.contains("embedded") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Hardware Foundations & C Mastery",
                    objective = "Master pointer arithmetic, memory registers, bitwise masking, and basic microcontroller interfacing.",
                    topicsCsv = "Pointers & Dynamic Memory in C, Bitwise Register Manipulation, GPIO & Interrupt Handling Basics",
                    practicalProject = "Bare-Metal GPIO & Timer Driver on ARM Cortex-M",
                    learningOutcomesCsv = "Direct register access and low-level memory control",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Peripherals & Serial Communication Buses",
                    objective = "Implement industry standard UART, I2C, and SPI protocol drivers with ring buffers.",
                    topicsCsv = "UART Ring Buffers, I2C Protocol & Sensor Interfacing, SPI High-Speed Display Control",
                    practicalProject = "Multi-Sensor Environmental Telemetry Station",
                    learningOutcomesCsv = "Sensor integration and bus protocol debugging",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Real-Time Operating Systems (RTOS)",
                    objective = "Build deterministic multitasking firmware with task preemption and synchronization.",
                    topicsCsv = "FreeRTOS Task Scheduling, Mutexes & Priority Inversion, Message Queues & Event Flags",
                    practicalProject = "Multithreaded Data Acquisition & Control System",
                    learningOutcomesCsv = "Deterministic real-time firmware architecture",
                    estimatedWeeks = 4
                )
            )

            // Python Programming (e.g. Commerce student learning Python)
            lowerGoal.contains("python") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Python Syntax, Data Structures & Automation",
                    objective = "Master variables, loops, lists, dictionaries, file I/O, and writing automation scripts.",
                    topicsCsv = "Python Syntax & Types, Control Flow & Loops, Lists & Dictionaries, File Operations, Script Automation",
                    practicalProject = "Automated CSV Financial Report Parser & Formatter",
                    learningOutcomesCsv = "Ability to automate daily data processing tasks with clean Python scripts",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Data Wrangling & Analytics with Pandas",
                    objective = "Clean, filter, aggregate, and visualize real-world datasets with Pandas and Matplotlib.",
                    topicsCsv = "NumPy Arrays, Pandas DataFrames, Filtering & Grouping, Handling Missing Values, Matplotlib/Seaborn Visuals",
                    practicalProject = "Interactive Financial Statement Analysis Dashboard",
                    learningOutcomesCsv = "Confident exploratory data analysis and spreadsheet replacement",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Object-Oriented Architecture & API Integrations",
                    objective = "Design reusable OOP classes, consume REST APIs, and build a capstone project.",
                    topicsCsv = "Classes & Inheritance, Error Handling & Exceptions, Requests & REST APIs, Virtual Environments, Packaging",
                    practicalProject = "Real-Time Market Data Tracker with REST API & SQLite",
                    learningOutcomesCsv = "Modular, production-ready Python programming skills",
                    estimatedWeeks = 4
                )
            )

            // BCA / Software Development
            lowerGoal.contains("software dev") || lowerGoal.contains("full stack") || lowerGoal.contains("full-stack") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Frontend Architecture & Modern JavaScript/TypeScript",
                    objective = "Build responsive web interfaces using semantic HTML, modern CSS, and component-based TypeScript.",
                    topicsCsv = "TypeScript Basics, Component Lifecycle, State Management, DOM Manipulation, CSS Grid & Flexbox",
                    practicalProject = "Responsive Interactive Web Application with TypeScript",
                    learningOutcomesCsv = "Proficiency in building clean, responsive user interfaces",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Backend APIs, Database Design & Authentication",
                    objective = "Design RESTful APIs, relational databases in PostgreSQL, and secure JWT authentication.",
                    topicsCsv = "REST API Principles, Node.js/Express, SQL & Relational Schema, JWT Auth, Middleware",
                    practicalProject = "Secure CRUD API with Relational Database & JWT Auth",
                    learningOutcomesCsv = "Ability to engineer robust server-side APIs and schemas",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Full-Stack Integration, DevOps & Capstone",
                    objective = "Connect frontend to backend, containerize with Docker, and deploy with CI/CD.",
                    topicsCsv = "Full-Stack State Synchronization, Docker Containerization, CI/CD Workflows, Testing (Jest)",
                    practicalProject = "Production-Ready Full-Stack Web Platform",
                    learningOutcomesCsv = "Complete end-to-end full-stack software development competency",
                    estimatedWeeks = 4
                )
            )

            // MCA / AI & Machine Learning
            lowerGoal.contains("ai & machine learning") || lowerGoal.contains("ai/ml") || (lowerGoal.contains("ai") && lowerGoal.contains("learning")) -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Mathematics for ML & Classical Algorithms",
                    objective = "Master linear algebra, gradient descent, regressions, decision trees, and model evaluation metrics.",
                    topicsCsv = "Matrix Operations & Vectors, Cost Functions & Gradient Descent, Scikit-Learn Classifiers, Overfitting & Regularization",
                    practicalProject = "Predictive Housing Price & Classification Pipeline with Scikit-Learn",
                    learningOutcomesCsv = "Mathematical clarity and classical ML mastery",
                    estimatedWeeks = 4,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Deep Neural Networks & PyTorch",
                    objective = "Build, train, and optimize deep feedforward, convolutional, and recurrent neural networks in PyTorch.",
                    topicsCsv = "PyTorch Tensors & Autograd, Multi-Layer Perceptrons, CNNs for Image Classification, Loss Functions & Optimizers",
                    practicalProject = "Custom Image Classifier & Visual Heatmap with PyTorch",
                    learningOutcomesCsv = "Confidence in developing and training deep learning models",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Transformers, Modern NLP & MLOps",
                    objective = "Implement attention mechanisms, fine-tune Hugging Face models, and deploy inference pipelines.",
                    topicsCsv = "Self-Attention & Transformers, Hugging Face Pretrained Models, Quantization Basics, FastAPI Model Deployment",
                    practicalProject = "Fine-Tuned Domain QA Assistant with Local Inference",
                    learningOutcomesCsv = "Production-level AI/ML engineering capability",
                    estimatedWeeks = 4
                )
            )

            // Photography (Self-Learner)
            lowerGoal.contains("photography") || lowerGoal.contains("photo") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Camera Exposure & Manual Controls",
                    objective = "Master the exposure triangle (aperture, shutter speed, ISO), depth of field, and focal lengths.",
                    topicsCsv = "Aperture & Depth of Field, Shutter Speed & Motion Blur, ISO & Noise Management, Metering Modes, Focal Lengths",
                    practicalProject = "Manual Exposure Portfolio: High-Speed Motion & Bokeh Studies",
                    learningOutcomesCsv = "Intuitive manual control of camera settings in any lighting condition",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Composition, Light Direction & Street/Portraiture",
                    objective = "Understand natural light quality, leading lines, framing, and working with human subjects.",
                    topicsCsv = "Rule of Thirds & Golden Ratio, Natural Light Direction (Golden Hour/Backlight), Street Photography Ethics, Portrait Posing",
                    practicalProject = "Street & Portrait Photography Series (10 Final Images)",
                    learningOutcomesCsv = "Strong visual aesthetics and deliberate compositional framing",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Adobe Lightroom RAW Processing & Visual Style",
                    objective = "Develop an identifiable personal style, color grading, tone curve adjustments, and print preparation.",
                    topicsCsv = "RAW Import & Catalog Organization, Tone Curve & HSL Color Grading, Masking & Local Adjustments, Export Sharpening",
                    practicalProject = "Cohesive 20-Photo Printed & Digital Exhibition Portfolio",
                    learningOutcomesCsv = "Professional digital darkroom mastery and publication-quality output",
                    estimatedWeeks = 3
                )
            )

            // Medical: NEET PG / Clinical Medicine
            lowerGoal.contains("neet pg") || lowerGoal.contains("clinical physician") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "High-Yield Pre-Clinical & Para-Clinical Foundations",
                    objective = "Consolidate pathology, pharmacology, and microbiology mechanisms with clinical linkages.",
                    topicsCsv = "General Pathology & Neoplasia, Antimicrobial & Autonomic Pharmacology, Infectious Disease Microbiology, Forensic Jurisprudence",
                    practicalProject = "Integrated Mechanism Mapping for Top 50 Pharmacological Classes",
                    learningOutcomesCsv = "High-speed recall of drug mechanisms and pathognomonic histological features",
                    estimatedWeeks = 4,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Core Clinical Disciplines & Vignette Solving",
                    objective = "Master internal medicine, general surgery, obstetrics, and gynecology clinical management algorithms.",
                    topicsCsv = "Cardiology & Pulmonology Algorithms, Acute Abdomen Surgical Decisions, High-Risk Obstetrics, Pediatric Milestones",
                    practicalProject = "500-Question Timed Clinical Vignette Solving Workbook",
                    learningOutcomesCsv = "Accuracy in interpreting bedside presentations and multi-step management questions",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Short Subjects, Image-Based Questions & Mock Simulation",
                    objective = "Master ophthalmology, ENT, dermatology, radiology, and full 200-question timed mocks.",
                    topicsCsv = "Radiological Signs (CT/X-Ray/MRI), Dermatology Lesions, ECGs & Electrolytes, Grand Mock Strategy",
                    practicalProject = "Full-Length 200-Question NEET PG Simulated Examination",
                    learningOutcomesCsv = "Exam readiness and peak time management under pressure",
                    estimatedWeeks = 4
                )
            )

            // School Science
            lowerGoal.contains("school science") || lowerGoal.contains("board exam") || lowerGoal.contains("junior science") -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Physics Principles & Mathematical Motion",
                    objective = "Master kinematics, Newton's laws of motion, gravitation, and energy conservation.",
                    topicsCsv = "Speed, Velocity & Acceleration, Newton's 3 Laws, Universal Gravitation, Work, Energy & Power Derivations",
                    practicalProject = "Motion Graphing & Force Calculation Problem Set",
                    learningOutcomesCsv = "Ability to solve numerical motion problems without formula confusion",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Chemical Reactions, Acids & Life Processes",
                    objective = "Understand balanced chemical equations, periodic trends, and cellular organ systems.",
                    topicsCsv = "Balancing Chemical Reactions, Acids, Bases & pH, Cellular Respiration, Human Nutrition & Transport",
                    practicalProject = "Reaction Balancing & Organ System Diagram Notebook",
                    learningOutcomesCsv = "Flawless chemical equation balancing and biological diagram recall",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Electricity, Magnetism & Board Examination Drills",
                    objective = "Master Ohm's law, circuit resistance, electromagnetic induction, and past board papers.",
                    topicsCsv = "Ohm's Law & Circuit Analysis, Magnetic Effects of Current, Heredity Basics, Past 5-Year Board Papers",
                    practicalProject = "Complete Timed Board Pattern Science Mock Exam",
                    learningOutcomesCsv = "Exam readiness with structured, high-scoring answer presentation",
                    estimatedWeeks = 3
                )
            )

            // Generic / Custom Goals (Scenario F)
            else -> listOf(
                RoadmapPhaseEntity(
                    id = "phase_1",
                    phaseNumber = 1,
                    title = "Foundations & Core Principles of $goal",
                    objective = "Establish fundamental vocabulary, core concepts, and operational mental models in $goal.",
                    topicsCsv = "Basic Terminology & Concepts, Core Principles & History, Essential Tools & Working Environment",
                    practicalProject = "Introductory Guided Exercise & Knowledge Synthesis in $goal",
                    learningOutcomesCsv = "Solid comprehension of basic principles without ambiguity",
                    estimatedWeeks = 3,
                    isCurrentActive = true
                ),
                RoadmapPhaseEntity(
                    id = "phase_2",
                    phaseNumber = 2,
                    title = "Methodologies, Practice & Applied Techniques",
                    objective = "Apply foundational concepts to real-world tasks, iterative exercises, and structured problems.",
                    topicsCsv = "Standard Workflows & Methods, Problem Solving Techniques, Case Studies & Best Practices",
                    practicalProject = "Intermediate Hands-on Assignment applying $goal principles",
                    learningOutcomesCsv = "Ability to execute standard tasks independently",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseEntity(
                    id = "phase_3",
                    phaseNumber = 3,
                    title = "Integration, Mastery & Capstone Project",
                    objective = "Combine all competencies into an independent, real-world portfolio project in $goal.",
                    topicsCsv = "Advanced Topics & Complex Scenarios, Optimization & Troubleshooting, Independent Capstone Execution",
                    practicalProject = "Independent Capstone Project Demonstrating Full Mastery of $goal",
                    learningOutcomesCsv = "Verified proficiency and practical capability",
                    estimatedWeeks = 4
                )
            )
        }
    }

    /**
     * Enforces STRICT daily study time limits (Part 4).
     * For example, if availableHoursPerDay == 2, total scheduled minutes on any single day
     * will NEVER exceed 120 minutes.
     */
    fun generateConstrainedDailyTasks(
        phases: List<RoadmapPhaseEntity>,
        availableHoursPerDay: Int,
        sessionDurationMinutes: Int,
        primarySkill: String
    ): List<StudyTaskEntity> {
        val maxDailyMinutes = (availableHoursPerDay.coerceIn(1, 10)) * 60
        val sessionLength = sessionDurationMinutes.coerceIn(20, maxDailyMinutes)

        // Calculate how many sessions can fit in one day without exceeding maxDailyMinutes
        val sessionsPerDay = (maxDailyMinutes / sessionLength).coerceAtLeast(1)
        val actualDailyMinutes = sessionsPerDay * sessionLength

        // Safety invariant check
        check(actualDailyMinutes <= maxDailyMinutes) {
            "Scheduling invariant violated: Scheduled minutes ($actualDailyMinutes) exceeded max daily limit ($maxDailyMinutes)"
        }

        val phase1 = phases.firstOrNull() ?: return emptyList()
        val topics = phase1.topicsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }

        val tasks = mutableListOf<StudyTaskEntity>()
        var currentDayNumber = 1
        var taskOrder = 0

        for (topic in topics) {
            val isToday = (currentDayNumber == 1)
            val dateLabel = if (isToday) "Today" else "Day $currentDayNumber"

            // Session 1: Study & Core Concepts
            tasks.add(
                StudyTaskEntity(
                    id = "task_${phase1.id}_day${currentDayNumber}_s1",
                    title = "Study: $topic",
                    relatedSkill = primarySkill,
                    durationMinutes = sessionLength,
                    scheduledDate = dateLabel,
                    isCompleted = false,
                    isRevisionTask = false,
                    orderIndex = taskOrder++
                )
            )

            // If user has capacity for a second session on the same day, add practical drill
            if (sessionsPerDay >= 2) {
                tasks.add(
                    StudyTaskEntity(
                        id = "task_${phase1.id}_day${currentDayNumber}_s2",
                        title = "Practice & Notes: $topic",
                        relatedSkill = primarySkill,
                        durationMinutes = sessionLength,
                        scheduledDate = dateLabel,
                        isCompleted = false,
                        isRevisionTask = false,
                        orderIndex = taskOrder++
                    )
                )
            }

            currentDayNumber++
        }

        return tasks
    }
}
