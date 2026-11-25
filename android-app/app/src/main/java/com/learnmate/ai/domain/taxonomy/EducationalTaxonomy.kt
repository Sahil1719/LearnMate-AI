package com.learnmate.ai.domain.taxonomy

/**
 * Structured Educational Knowledge Framework (Phase 3.2).
 * Distinguishes academic stages, streams, disciplines, branches,
 * professional qualifications, specialisations, and career destinations.
 */
object EducationalTaxonomy {

    enum class LearnerType(val displayName: String, val description: String) {
        SCHOOL_STUDENT("School Student", "Enrolled in primary or secondary school"),
        COLLEGE_STUDENT("College / University", "Pursuing Junior College, Degree, or Diploma"),
        WORKING_PROFESSIONAL("Working Professional", "Employed in industry or transitioning careers"),
        SELF_LEARNER("Self-Learner / Lifelong", "Independent learner studying out of curiosity or passion")
    }

    enum class EducationStage(val displayName: String, val learnerType: LearnerType) {
        // School stages
        MIDDLE_SCHOOL("Middle School (Grades 6–8)", LearnerType.SCHOOL_STUDENT),
        HIGH_SCHOOL_9_10("High School (Grades 9–10 / SSC / ICSE / CBSE)", LearnerType.SCHOOL_STUDENT),

        // College stages
        JUNIOR_COLLEGE_11_12("Junior College / 11th–12th / Pre-University", LearnerType.COLLEGE_STUDENT),
        UNDERGRADUATE("Undergraduate Degree (Bachelor's)", LearnerType.COLLEGE_STUDENT),
        POSTGRADUATE("Postgraduate Degree (Master's)", LearnerType.COLLEGE_STUDENT),
        DOCTORATE("Doctorate / Research (Ph.D.)", LearnerType.COLLEGE_STUDENT),

        // Professional stages
        EARLY_CAREER("Early Career (0–3 years experience)", LearnerType.WORKING_PROFESSIONAL),
        MID_SENIOR_CAREER("Mid to Senior Professional (3+ years experience)", LearnerType.WORKING_PROFESSIONAL),
        CAREER_TRANSITION("Career Switcher / Transitioning Domain", LearnerType.WORKING_PROFESSIONAL),

        // Self-learner stages
        HOBBY_PASSION("Hobby & Personal Passion", LearnerType.SELF_LEARNER),
        INDEPENDENT_SKILL_BUILDER("Independent Skill Builder", LearnerType.SELF_LEARNER)
    }

    enum class GoalType(val displayName: String) {
        CAREER_GOAL("Career Goal"),
        ENTRANCE_EXAM("Entrance Examination"),
        ACADEMIC_SUBJECT("Academic Subject / Exam"),
        PROFESSIONAL_CERTIFICATION("Professional Certification"),
        SKILL_DEVELOPMENT("Skill Development"),
        PERSONAL_DEVELOPMENT("Personal Development"),
        CUSTOM_GOAL("Custom Goal")
    }

    data class TaxonomyOption(
        val id: String,
        val label: String,
        val description: String,
        val categoryTag: String? = null
    )

    data class GoalDefinition(
        val id: String,
        val title: String,
        val description: String,
        val goalType: GoalType,
        val suggestedSkills: List<String>
    )

    // =============================================================================================
    // 1. STAGES FOR LEARNER TYPE
    // =============================================================================================
    fun getStagesForLearnerType(type: LearnerType): List<EducationStage> {
        return EducationStage.values().filter { it.learnerType == type }
    }

    // =============================================================================================
    // 2. STREAMS / DISCIPLINES FOR EDUCATION STAGE (Step 2)
    // =============================================================================================
    fun getStreamsForStage(stage: EducationStage): List<TaxonomyOption> {
        return when (stage) {
            EducationStage.MIDDLE_SCHOOL, EducationStage.HIGH_SCHOOL_9_10 -> listOf(
                TaxonomyOption("school_science", "Science (Physics, Chemistry, Biology)", "General and foundational scientific concepts"),
                TaxonomyOption("school_math", "Mathematics & Quantitative Problem Solving", "Algebra, Geometry, Arithmetic and reasoning"),
                TaxonomyOption("school_social", "Social Studies & Humanities", "History, Geography, Civics and Economics"),
                TaxonomyOption("school_cs_basic", "Computer Fundamentals & Coding Basics", "Introductory programming and digital literacy"),
                TaxonomyOption("school_custom", "Other School Stream / Custom", "Any specific school syllabus or board")
            )

            EducationStage.JUNIOR_COLLEGE_11_12 -> listOf(
                TaxonomyOption("jc_science_pcm", "Science (PCM - Physics, Chemistry, Math)", "Focus on engineering, physical sciences, and technical entrance prep"),
                TaxonomyOption("jc_science_pcb", "Science (PCB - Physics, Chemistry, Biology)", "Focus on medical entrance, life sciences, and healthcare"),
                TaxonomyOption("jc_commerce", "Commerce & Business Studies", "Accounting, Business Studies, Economics, and Commercial Mathematics"),
                TaxonomyOption("jc_arts", "Arts & Humanities", "Psychology, Sociology, Political Science, Literature, and History"),
                TaxonomyOption("jc_vocational", "Vocational & Technical Stream", "Applied skills, electronics, computer techniques, or commercial trades"),
                TaxonomyOption("jc_custom", "Other Junior College Stream", "Custom or regional high school curriculum")
            )

            EducationStage.UNDERGRADUATE -> listOf(
                TaxonomyOption("ug_engineering", "Engineering & Technology (B.Tech / B.E)", "Specialised engineering branches and technical disciplines"),
                TaxonomyOption("ug_bca", "Computer Applications (BCA / B.Sc IT / CS)", "Software development, programming, web, and computing systems"),
                TaxonomyOption("ug_commerce", "Commerce & Management (B.Com / BBA / BMS)", "Financial accounting, corporate finance, taxation, and business administration"),
                TaxonomyOption("ug_medicine", "Medicine & Healthcare (MBBS / BDS / B.Pharm / Nursing)", "Clinical medical practice, dental, pharmaceutical, and health sciences"),
                TaxonomyOption("ug_pure_sciences", "Pure & Applied Sciences (B.Sc)", "Physics, Chemistry, Mathematics, Biotechnology, and Life Sciences"),
                TaxonomyOption("ug_arts", "Arts & Humanities (B.A / BFA)", "Literature, Psychology, Fine Arts, History, Sociology, and Philosophy"),
                TaxonomyOption("ug_law", "Law & Legal Studies (BA LLB / BBA LLB / LLB)", "Constitutional, Corporate, Civil, and Criminal jurisprudence"),
                TaxonomyOption("ug_custom", "Other Degree / Interdisciplinary", "Any other undergraduate programme or double major")
            )

            EducationStage.POSTGRADUATE -> listOf(
                TaxonomyOption("pg_mca", "Computer Applications & AI (MCA / M.Sc CS)", "Advanced software engineering, AI, distributed systems, and machine learning"),
                TaxonomyOption("pg_engineering", "Engineering & Technology (M.Tech / M.E)", "Advanced engineering research, robotics, microelectronics, and signal processing"),
                TaxonomyOption("pg_management", "Business Administration & Management (MBA / PGDM)", "Strategic management, financial markets, marketing analytics, and operations"),
                TaxonomyOption("pg_commerce", "Commerce & Finance (M.Com / Master in Finance)", "Advanced financial reporting, auditing, international finance, and corporate taxation"),
                TaxonomyOption("pg_medicine", "Medical Post-Graduation (MD / MS / DNB)", "Clinical specialisations, surgical residency, pathology, and internal medicine"),
                TaxonomyOption("pg_sciences", "Pure & Natural Sciences (M.Sc)", "Advanced theoretical physics, organic chemistry, biotechnology research"),
                TaxonomyOption("pg_arts", "Humanities & Social Sciences (M.A)", "Clinical psychology, English literature, international relations, mass media"),
                TaxonomyOption("pg_law", "Master of Laws (LLM)", "Corporate law, intellectual property, constitutional jurisprudence"),
                TaxonomyOption("pg_custom", "Other Postgraduate Programme", "Any other postgraduate diploma or degree")
            )

            EducationStage.DOCTORATE -> listOf(
                TaxonomyOption("doc_stem", "STEM Research (Science & Engineering)", "Doctoral dissertation in physical, computer, or biological sciences"),
                TaxonomyOption("doc_humanities", "Humanities & Social Sciences Research", "Doctoral research in literature, sociology, economics, or history"),
                TaxonomyOption("doc_custom", "Other Doctoral Discipline", "Interdisciplinary or clinical doctoral programme")
            )

            EducationStage.EARLY_CAREER, EducationStage.MID_SENIOR_CAREER, EducationStage.CAREER_TRANSITION -> listOf(
                TaxonomyOption("prof_software", "Software & Information Technology", "Software engineering, DevOps, Cloud, AI, and cybersecurity"),
                TaxonomyOption("prof_finance", "Banking, Finance & Accounting", "Investment banking, corporate finance, accounting, and risk management"),
                TaxonomyOption("prof_healthcare", "Healthcare & Life Sciences", "Clinical practice, hospital management, pharmaceuticals, and health-tech"),
                TaxonomyOption("prof_core_engg", "Core Engineering & Manufacturing", "Electronics, mechanical systems, automotive, civil, and industrial operations"),
                TaxonomyOption("prof_creative", "Design, Media & Creative Arts", "Digital illustration, UI/UX design, visual storytelling, and content creation"),
                TaxonomyOption("prof_business", "Sales, Marketing & Business Growth", "Product management, digital marketing, operations, and enterprise strategy"),
                TaxonomyOption("prof_custom", "Other Professional Domain", "Any other industry or interdisciplinary trade")
            )

            EducationStage.HOBBY_PASSION, EducationStage.INDEPENDENT_SKILL_BUILDER -> listOf(
                TaxonomyOption("self_creative", "Creative Arts, Photography & Media", "Photography, illustration, music production, writing, and digital art"),
                TaxonomyOption("self_tech", "Computer Programming & Artificial Intelligence", "Coding, machine learning, web building, and robotics"),
                TaxonomyOption("self_finance", "Personal Finance & Wealth Creation", "Investing, stock trading, budgeting, and entrepreneurial ventures"),
                TaxonomyOption("self_languages", "Languages, Public Speaking & Communication", "Fluency in new languages, public speaking, rhetoric, and debate"),
                TaxonomyOption("self_health", "Health, Fitness & Nutrition", "Human physiology, exercise science, nutrition, and wellness"),
                TaxonomyOption("self_science", "Natural Sciences & Philosophy", "Astronomy, physics, evolutionary biology, and philosophy"),
                TaxonomyOption("self_custom", "Other Personal Interest", "Any hobby or specialized subject area")
            )
        }
    }

    // =============================================================================================
    // 3. PATHWAYS / DISCIPLINES / BRANCHES FOR STREAM (Step 3)
    // =============================================================================================
    fun getPathwaysForStream(streamId: String): List<TaxonomyOption> {
        return when (streamId) {
            // Engineering (UG & PG)
            "ug_engineering", "pg_engineering" -> listOf(
                TaxonomyOption("eng_cs_it", "Computer Science & Information Technology", "Operating systems, data structures, algorithms, and computing architecture", "Branch"),
                TaxonomyOption("eng_entc", "Electronics & Telecommunication (ENTC / ECE)", "Microcontrollers, embedded firmware, signal processing, and communication protocols", "Branch"),
                TaxonomyOption("eng_ai_ds", "Artificial Intelligence & Data Science", "Machine learning, neural networks, statistical modeling, and data pipelines", "Branch"),
                TaxonomyOption("eng_electrical", "Electrical & Electronics Engineering (EEE)", "Power systems, control theory, electric drives, and analog electronics", "Branch"),
                TaxonomyOption("eng_mechanical", "Mechanical Engineering", "Thermodynamics, robotics, fluid mechanics, CAD/CAM, and machine design", "Branch"),
                TaxonomyOption("eng_civil", "Civil & Structural Engineering", "Structural analysis, geotechnical engineering, surveying, and materials", "Branch"),
                TaxonomyOption("eng_chemical", "Chemical Engineering & Materials", "Process engineering, chemical kinetics, polymers, and mass transfer", "Branch"),
                TaxonomyOption("eng_custom", "Other Engineering Discipline", "Aerospace, biomedical, instrumentation, or agricultural engineering", "Custom")
            )

            // BCA / MCA
            "ug_bca" -> listOf(
                TaxonomyOption("bca_software_dev", "Software Development (Full-Stack & Apps)", "Core Java, Python, Web development, and mobile application engineering", "Specialisation"),
                TaxonomyOption("bca_web_technologies", "Modern Web & Frontend Engineering", "JavaScript, TypeScript, React, APIs, and responsive design", "Specialisation"),
                TaxonomyOption("bca_data_management", "Database Systems & Backend Engineering", "SQL, database modeling, REST APIs, and backend server logic", "Specialisation"),
                TaxonomyOption("bca_cyber_basics", "Cybersecurity & Network Administration", "Network protocols, security fundamentals, Linux administration, and ethical hacking", "Specialisation"),
                TaxonomyOption("bca_custom", "Other Computing Track", "Cloud foundations, mobile development, or gaming", "Custom")
            )

            "pg_mca" -> listOf(
                TaxonomyOption("mca_ai_ml", "Artificial Intelligence & Machine Learning", "Deep learning, NLP, computer vision, PyTorch, and predictive modeling", "Specialisation"),
                TaxonomyOption("mca_cloud_devops", "Cloud Architecture & DevOps Engineering", "Kubernetes, Docker, AWS/GCP, CI/CD pipelines, and microservices", "Specialisation"),
                TaxonomyOption("mca_enterprise_dev", "Enterprise Software Architecture", "Distributed systems, design patterns, microservices, and high-load backends", "Specialisation"),
                TaxonomyOption("mca_cybersecurity", "Advanced Cybersecurity & InfoSec", "Penetration testing, cryptography, secure coding, and cloud security", "Specialisation"),
                TaxonomyOption("mca_custom", "Other Postgraduate Computing Track", "Data engineering, blockchain, or quantum computing", "Custom")
            )

            // Commerce & Finance (JC, UG, PG)
            "jc_commerce", "ug_commerce", "pg_commerce", "prof_finance" -> listOf(
                TaxonomyOption("comm_accounting", "Accounting & Financial Reporting", "Financial accounting, cost accounting, corporate balance sheets, and audit", "Discipline"),
                TaxonomyOption("comm_ca_cma_cs", "Chartered Accountancy & Professional Certifications (CA / CMA / CS)", "ICAI CA Foundation/Inter/Final, CMA, or CS professional examinations", "Professional Qualification"),
                TaxonomyOption("comm_banking_investment", "Banking, Financial Markets & Investment", "Financial modeling, equity research, portfolio management, and investment analysis", "Career Destination"),
                TaxonomyOption("comm_business_mgmt", "Business Management & Operations", "Organizational behavior, human resources, supply chain, and business administration", "Discipline"),
                TaxonomyOption("comm_economics", "Business Economics & Econometrics", "Microeconomics, macroeconomic policy, econometrics, and quantitative market analysis", "Discipline"),
                TaxonomyOption("comm_taxation", "Corporate & Direct/Indirect Taxation", "Income tax regulations, GST compliance, international taxation, and tax planning", "Specialisation"),
                TaxonomyOption("comm_custom", "Other Commerce Specialisation", "Fintech, corporate secretarial, or venture capital", "Custom")
            )

            // Medicine & Healthcare (UG & PG)
            "ug_medicine", "pg_medicine", "jc_science_pcb", "prof_healthcare" -> listOf(
                TaxonomyOption("med_clinical_mbbs", "Medicine & Surgery (MBBS / Clinical Practice)", "Human anatomy, physiology, pathology, pharmacology, and internal medicine", "Academic & Clinical"),
                TaxonomyOption("med_pg_prep", "Postgraduate Medical Exam Preparation (NEET PG / INI-CET / USMLE)", "High-yield clinical vignettes, comprehensive 19-subject medical revision, and question banks", "Entrance Exam"),
                TaxonomyOption("med_dentistry", "Dentistry (BDS / MDS)", "Oral anatomy, conservative dentistry, prosthodontics, and periodontology", "Discipline"),
                TaxonomyOption("med_pharmacy", "Pharmacy & Pharmaceutical Sciences (B.Pharm / M.Pharm)", "Medicinal chemistry, pharmacology, dosage formulations, and pharmacokinetics", "Discipline"),
                TaxonomyOption("med_nursing", "Nursing & Patient Care", "Critical care nursing, pediatric care, community health, and pharmacology", "Discipline"),
                TaxonomyOption("med_physiotherapy", "Physiotherapy & Physical Rehabilitation", "Biomechanics, exercise therapy, orthopedics, and neurological rehabilitation", "Discipline"),
                TaxonomyOption("med_allied", "Allied Health Sciences & Diagnostics", "Medical lab technology, radiology, clinical biochemistry, and pathology testing", "Discipline"),
                TaxonomyOption("med_research", "Medical Research & Clinical Trials", "Biomedical research methods, epidemiology, clinical trial protocols, and biostatistics", "Specialisation"),
                TaxonomyOption("med_custom", "Other Healthcare Field", "Healthcare management, veterinary science, or public health", "Custom")
            )

            // Arts, Humanities & Design (JC, UG, PG, Working Professional)
            "jc_arts", "ug_arts", "pg_arts", "prof_creative" -> listOf(
                TaxonomyOption("arts_illustration", "Digital Illustration & Concept Art", "Digital drawing, color theory, light, character design, and concept art pipelines", "Specialisation"),
                TaxonomyOption("arts_fine_arts", "Fine Arts & Traditional Painting", "Drawing fundamentals, perspective, acrylics, oil painting, and sculptural form", "Discipline"),
                TaxonomyOption("arts_psychology", "Psychology & Behavioral Sciences", "Cognitive psychology, abnormal psychology, counseling theories, and psychometrics", "Discipline"),
                TaxonomyOption("arts_design", "UI/UX & Visual Communication Design", "Typography, user interface design, user research, wireframing, and design systems", "Discipline"),
                TaxonomyOption("arts_literature", "Literature, Creative Writing & Linguistics", "Literary analysis, creative writing, narrative craft, poetry, and rhetoric", "Discipline"),
                TaxonomyOption("arts_journalism", "Journalism & Media Communications", "News reporting, digital media production, investigative research, and broadcast", "Discipline"),
                TaxonomyOption("arts_pol_sci", "Political Science & International Relations", "Political philosophy, comparative governance, foreign policy, and diplomacy", "Discipline"),
                TaxonomyOption("arts_languages", "Language Studies & Translation", "Grammar, phonetic fluency, literary translation, and conversational proficiency", "Discipline"),
                TaxonomyOption("arts_custom", "Other Arts / Creative Specialisation", "History, sociology, anthropology, music, or performing arts", "Custom")
            )

            // Pure & Applied Sciences (JC, UG, PG, Self-Learner)
            "jc_science_pcm", "ug_pure_sciences", "pg_sciences", "self_science" -> listOf(
                TaxonomyOption("sci_physics", "Physics & Theoretical Mechanics", "Classical mechanics, electromagnetism, quantum mechanics, and thermodynamics", "Discipline"),
                TaxonomyOption("sci_chemistry", "Chemistry (Organic, Inorganic, Physical)", "Reaction mechanisms, coordination chemistry, spectroscopy, and chemical kinetics", "Discipline"),
                TaxonomyOption("sci_mathematics", "Pure & Applied Mathematics", "Calculus, linear algebra, discrete mathematics, statistics, and differential equations", "Discipline"),
                TaxonomyOption("sci_biology", "Biological Sciences & Life Systems", "Cell biology, molecular genetics, evolutionary ecology, and microbiology", "Discipline"),
                TaxonomyOption("sci_biotech", "Biotechnology & Genetic Engineering", "Recombinant DNA technology, bioinformatics, bioprocess engineering, and genomics", "Discipline"),
                TaxonomyOption("sci_research", "Scientific Research Methodology & Data Analysis", "Hypothesis testing, academic literature review, Python/R for scientific computation", "Specialisation"),
                TaxonomyOption("sci_custom", "Other Scientific Discipline", "Astronomy, environmental science, geology, or materials science", "Custom")
            )

            // Self-Learner / Hobbies
            "self_creative" -> listOf(
                TaxonomyOption("hobby_photography", "Professional Photography & Lightroom", "Camera exposure controls, composition, portrait lighting, street photography, and post-processing", "Hobby / Skill"),
                TaxonomyOption("hobby_illustration", "Digital Illustration & Procreate / Photoshop", "Digital sketching, digital ink, color theory, and character art", "Hobby / Skill"),
                TaxonomyOption("hobby_music", "Music Production & Sound Design", "DAW fundamentals, beat making, audio synthesis, mixing, and mastering", "Hobby / Skill"),
                TaxonomyOption("hobby_writing", "Creative Fiction & Storytelling", "Narrative structures, character development, dialogue, and publication craft", "Hobby / Skill"),
                TaxonomyOption("hobby_custom", "Other Creative Pursuit", "Crafts, sculpting, animation, or video editing", "Custom")
            )

            // School Students (Foundational)
            "school_science" -> listOf(
                TaxonomyOption("sch_phy_chem", "Physics & Chemistry Core Foundations", "Newton's laws, light, heat, atomic structure, and chemical reactions", "Subject Group"),
                TaxonomyOption("sch_bio", "Biology & Life Processes", "Plant and animal anatomy, cell structure, ecosystems, and human physiology", "Subject Group"),
                TaxonomyOption("sch_olympiad", "Science Olympiad & Competitive Prep", "Advanced conceptual problem solving for NSO / NTSE / Olympiads", "Competitive Prep"),
                TaxonomyOption("sch_custom", "Other School Science Focus", "Specific board chapter revision or practical science lab projects", "Custom")
            )

            // Fallback for custom or unmapped streams
            else -> listOf(
                TaxonomyOption("pathway_foundations", "Core Foundations & Fundamental Concepts", "Essential theoretical grounding and core principles", "Foundations"),
                TaxonomyOption("pathway_applied", "Applied Practice & Practical Projects", "Hands-on projects, industry tooling, and practical execution", "Applied"),
                TaxonomyOption("pathway_advanced", "Advanced Specialisation & Mastery", "High-level nuance, professional standards, and capstones", "Mastery"),
                TaxonomyOption("pathway_custom", "Custom Specialised Pathway", "Specify your exact focus area or syllabus", "Custom")
            )
        }
    }

    // =============================================================================================
    // 4. GOALS FOR PATHWAY (Step 4)
    // =============================================================================================
    fun getGoalsForPathway(pathwayId: String, streamId: String): List<GoalDefinition> {
        return when (pathwayId) {
            // ENTC / Electronics
            "eng_entc" -> listOf(
                GoalDefinition(
                    id = "goal_embedded_engineer",
                    title = "Embedded Systems Engineer",
                    description = "Master firmware development, microcontrollers, C, RTOS, and hardware interfacing",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("C Programming", "Microcontrollers", "RTOS", "Digital Electronics", "Communication Protocols")
                ),
                GoalDefinition(
                    id = "goal_gate_ece",
                    title = "GATE ECE Examination Preparation",
                    description = "Comprehensive prep for Signals, Networks, Electronic Devices, and Digital Circuits",
                    goalType = GoalType.ENTRANCE_EXAM,
                    suggestedSkills = listOf("Signals & Systems", "Network Theory", "Analog Circuits", "Digital Circuits", "Engineering Mathematics")
                ),
                GoalDefinition(
                    id = "goal_iot_dev",
                    title = "IoT Device & Firmware Development",
                    description = "Build connected hardware with ESP32, MQTT, BLE, sensors, and cloud telemetry",
                    goalType = GoalType.SKILL_DEVELOPMENT,
                    suggestedSkills = listOf("C/C++", "ESP32 & Arduino", "MQTT & Networking Protocols", "Sensor Interfacing", "Power Management")
                ),
                GoalDefinition(
                    id = "goal_dsp_engineer",
                    title = "Digital Signal Processing (DSP) Mastery",
                    description = "Filter design, Fourier analysis, audio/speech processing, and MATLAB/Python modeling",
                    goalType = GoalType.ACADEMIC_SUBJECT,
                    suggestedSkills = listOf("Discrete Fourier Transform", "IIR/FIR Filter Design", "MATLAB / SciPy", "Sampling Theory", "DSP Hardware Basics")
                )
            )

            // Commerce: CA / Professional Accounting
            "comm_ca_cma_cs" -> listOf(
                GoalDefinition(
                    id = "goal_ca_foundation",
                    title = "CA Foundation Examination",
                    description = "Clear ICAI CA Foundation in Accounting, Mercantile Law, Math/Stats, and Economics",
                    goalType = GoalType.ENTRANCE_EXAM,
                    suggestedSkills = listOf("Principles & Practice of Accounting", "Business Law & Commercial Knowledge", "Business Mathematics & Statistics", "Business Economics", "Logical Reasoning")
                ),
                GoalDefinition(
                    id = "goal_ca_intermediate",
                    title = "CA Intermediate Preparation",
                    description = "Advanced accounting, corporate law, direct/indirect taxation, and cost accounting",
                    goalType = GoalType.ENTRANCE_EXAM,
                    suggestedSkills = listOf("Advanced Accounting", "Corporate & Other Laws", "Cost & Management Accounting", "Taxation (Income Tax & GST)", "Auditing")
                ),
                GoalDefinition(
                    id = "goal_chartered_accountant",
                    title = "Chartered Accountant (CA) Career Readiness",
                    description = "Statutory audits, tax advisory, financial reporting, and corporate regulatory compliance",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Financial Reporting (Ind AS / IFRS)", "Statutory & Internal Auditing", "Corporate Taxation", "Company Law Compliance", "Financial Analysis")
                )
            )

            // Commerce: Accounting & Finance
            "comm_accounting", "comm_banking_investment" -> listOf(
                GoalDefinition(
                    id = "goal_financial_analyst",
                    title = "Financial Analyst & Equity Research",
                    description = "Financial statement modeling, DCF valuation, ratio analysis, and investment thesis",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Financial Statement Analysis", "Financial Modeling (Excel)", "Valuation Methodologies (DCF/Multiples)", "Corporate Finance", "Macroeconomic Analysis")
                ),
                GoalDefinition(
                    id = "goal_cfa_level1",
                    title = "CFA Level 1 Exam Preparation",
                    description = "Global standard in investment analysis: Ethics, FRA, Quantitative Methods, Equity, Fixed Income",
                    goalType = GoalType.PROFESSIONAL_CERTIFICATION,
                    suggestedSkills = listOf("Financial Reporting & Analysis", "Quantitative Methods", "Ethical & Professional Standards", "Economics for Investment", "Equity & Fixed Income")
                ),
                GoalDefinition(
                    id = "goal_tally_gst",
                    title = "Tally Prime & GST Accounting Certification",
                    description = "Hands-on voucher entry, ledger management, GST return filing, and inventory accounting",
                    goalType = GoalType.PROFESSIONAL_CERTIFICATION,
                    suggestedSkills = listOf("Tally Prime Operations", "Goods & Services Tax (GST) Filing", "TDS Computation", "Ledger & Voucher Entry", "Payroll Management")
                )
            )

            // Medical: Clinical MBBS & PG Prep
            "med_clinical_mbbs", "med_pg_prep" -> listOf(
                GoalDefinition(
                    id = "goal_neet_pg",
                    title = "NEET PG / INI-CET Medical Entrance",
                    description = "Target high-rank residency seats with high-yield clinical vignettes and subject mastery",
                    goalType = GoalType.ENTRANCE_EXAM,
                    suggestedSkills = listOf("Internal Medicine & Therapeutics", "General Surgery & Clinical Cases", "Obstetrics & Gynecology", "Pathology & Pharmacology", "Pediatrics & Community Medicine")
                ),
                GoalDefinition(
                    id = "goal_clinical_doctor",
                    title = "Clinical Physician / Doctor Readiness",
                    description = "Bedside examination, differential diagnosis, diagnostic ordering, and pharmacology",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Patient History & Clinical Examination", "Differential Diagnosis Formulation", "Clinical Pharmacology & Prescription", "ECG & Radiology Interpretation", "Emergency Medical Care")
                ),
                GoalDefinition(
                    id = "goal_usmle_step1",
                    title = "USMLE Step 1 Preparation",
                    description = "Integrated basic medical science foundation: Pathology, Physiology, Microbiology, Anatomy",
                    goalType = GoalType.ENTRANCE_EXAM,
                    suggestedSkills = listOf("General Pathology & Mechanisms", "Organ-System Physiology", "Medical Microbiology & Immunology", "Biochemistry & Genetics", "Clinical Anatomy")
                )
            )

            // BCA: Software Development
            "bca_software_dev" -> listOf(
                GoalDefinition(
                    id = "goal_fullstack_dev",
                    title = "Full-Stack Software Developer",
                    description = "Build complete web and mobile apps with frontend UI, backend APIs, and databases",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("JavaScript & TypeScript", "React / Next.js", "Node.js & Express / Python FastAPI", "SQL & Database Design (PostgreSQL)", "RESTful APIs & Git")
                ),
                GoalDefinition(
                    id = "goal_android_dev",
                    title = "Android Application Developer (Kotlin & Compose)",
                    description = "Modern Android development with Jetpack Compose, Room DB, Coroutines, and MVVM",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Kotlin Programming", "Jetpack Compose & Material 3", "Android Architecture (MVVM/Clean)", "Room Database & Coroutines", "REST API & Networking (Retrofit)")
                ),
                GoalDefinition(
                    id = "goal_dsa_prep",
                    title = "Data Structures & Algorithms (Campus Placements)",
                    description = "Master arrays, linked lists, trees, graphs, DP, and crack tech coding interviews",
                    goalType = GoalType.SKILL_DEVELOPMENT,
                    suggestedSkills = listOf("Arrays & String Manipulation", "Recursion & Dynamic Programming", "Trees & Graph Traversals", "Time & Space Complexity Analysis", "Sorting & Searching Algorithms")
                )
            )

            // MCA: AI & Machine Learning
            "mca_ai_ml" -> listOf(
                GoalDefinition(
                    id = "goal_ai_ml_engineer",
                    title = "AI & Machine Learning Engineer",
                    description = "End-to-end ML engineering: model training, deep neural nets, evaluation, and MLOps",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Python for Data Science", "Linear Algebra & Statistics", "Classical Machine Learning (Scikit-Learn)", "Deep Learning & Neural Networks (PyTorch)", "Model Evaluation & Deployment")
                ),
                GoalDefinition(
                    id = "goal_deep_learning_nlp",
                    title = "Large Language Models & Deep NLP",
                    description = "Transformer architectures, fine-tuning, embeddings, Hugging Face, and LLM inference",
                    goalType = GoalType.SKILL_DEVELOPMENT,
                    suggestedSkills = listOf("Transformer Architecture", "PyTorch & Hugging Face", "Tokenization & Embeddings", "Prompt Engineering & RAG", "Model Quantization & Optimization")
                ),
                GoalDefinition(
                    id = "goal_data_scientist",
                    title = "Data Scientist & Analytics Specialist",
                    description = "Exploratory data analysis, statistical modeling, feature engineering, and data storytelling",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Exploratory Data Analysis (Pandas)", "Statistical Hypothesis Testing", "Data Visualization (Seaborn/Plotly)", "Predictive Modeling", "SQL for Complex Data Queries")
                )
            )

            // Arts: Digital Illustration
            "arts_illustration", "hobby_illustration" -> listOf(
                GoalDefinition(
                    id = "goal_digital_illustrator",
                    title = "Professional Digital Illustrator",
                    description = "Create commercial digital artwork, book covers, concept paintings, and visual assets",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Drawing Fundamentals & Anatomy", "Perspective & Spatial Composition", "Color Theory & Lighting Value", "Digital Brushes & Layer Techniques", "Character & Environment Concept Art")
                ),
                GoalDefinition(
                    id = "goal_concept_artist",
                    title = "Concept Artist for Games & Animation",
                    description = "Character design sheets, props, world-building environments, and thumbnail iterations",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Rapid Thumbnail Sketching", "Costume & Silhouette Design", "Environment & Architectural Form", "Matte Painting & Texturing", "Visual Storytelling")
                ),
                GoalDefinition(
                    id = "goal_drawing_fundamentals",
                    title = "Drawing Fundamentals & Form Mastery",
                    description = "Foundational line control, gesture drawing, human anatomy, and light/shadow rendering",
                    goalType = GoalType.SKILL_DEVELOPMENT,
                    suggestedSkills = listOf("Line Confidence & Gesture", "Constructive Anatomy", "Light, Shadow & Core Values", "1-point, 2-point, 3-point Perspective", "Still Life & Observational Drawing")
                )
            )

            // Photography
            "hobby_photography" -> listOf(
                GoalDefinition(
                    id = "goal_professional_photographer",
                    title = "Professional Photographer & Visual Storyteller",
                    description = "Master manual camera operations, lighting setups, portraiture, and Adobe Lightroom workflow",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Manual Exposure (Aperture, Shutter, ISO)", "Composition (Rule of Thirds, Leading Lines)", "Natural & Studio Lighting Techniques", "Portrait & Street Photography", "Adobe Lightroom / Raw Photo Editing")
                ),
                GoalDefinition(
                    id = "goal_photo_composition",
                    title = "Creative Photography Composition & Framing",
                    description = "Visual aesthetics, focal lengths, color grading, and developing an artistic photographic style",
                    goalType = GoalType.SKILL_DEVELOPMENT,
                    suggestedSkills = listOf("Visual Framing & Balance", "Lens Focal Length Selection", "Color Harmony & Mood", "Depth of Field Control", "Storytelling through Imagery")
                )
            )

            // School Science
            "sch_phy_chem", "school_science" -> listOf(
                GoalDefinition(
                    id = "goal_school_science_exam",
                    title = "Class Science Board Exam Preparation",
                    description = "Complete textbook revision, conceptual clarity, formula memorization, and diagram mastery",
                    goalType = GoalType.ACADEMIC_SUBJECT,
                    suggestedSkills = listOf("Newton's Laws & Mechanics", "Electricity & Magnetism Basics", "Chemical Reactions & Equations", "Acids, Bases & Salts", "Life Processes & Plant/Human Biology")
                ),
                GoalDefinition(
                    id = "goal_olympiad_science",
                    title = "Junior Science Olympiad & Talent Search (NSO / NTSE)",
                    description = "High-order thinking questions, mathematical physics, and advanced chemistry concepts",
                    goalType = GoalType.ENTRANCE_EXAM,
                    suggestedSkills = listOf("Kinematics & Force Problems", "Atomic Structure & Chemical Bonding", "Genetics & Cellular Biology", "Experimental Reasoning", "Quantitative Problem Solving")
                )
            )

            // Default / Fallback goals
            else -> listOf(
                GoalDefinition(
                    id = "goal_career_mastery",
                    title = "Professional Career Mastery",
                    description = "Prepare for industry entry and professional competence in this field",
                    goalType = GoalType.CAREER_GOAL,
                    suggestedSkills = listOf("Foundational Principles", "Industry Standard Tooling", "Practical Workflows", "Problem Solving", "Professional Best Practices")
                ),
                GoalDefinition(
                    id = "goal_skill_building",
                    title = "Core Practical Skill Development",
                    description = "Hands-on projects and tangible techniques for real-world application",
                    goalType = GoalType.SKILL_DEVELOPMENT,
                    suggestedSkills = listOf("Essential Theory", "Technique & Execution", "Project Planning", "Troubleshooting & Refinement", "Portfolio Output")
                ),
                GoalDefinition(
                    id = "goal_academic_exam",
                    title = "Academic Curriculum & Exam Preparation",
                    description = "Comprehensive syllabus coverage, concept mastery, and exam question practice",
                    goalType = GoalType.ACADEMIC_SUBJECT,
                    suggestedSkills = listOf("Syllabus Core Concepts", "Problem Solving & Derivations", "Past Paper Questions", "Revision & Note Synthesis", "Time Management")
                )
            )
        }
    }

    // =============================================================================================
    // 5. GUIDED RECOMMENDATIONS ("Help Me Decide" experience)
    // =============================================================================================
    data class Recommendation(
        val pathwayId: String,
        val pathwayTitle: String,
        val goalTitle: String,
        val rationale: String
    )

    fun getGuidedRecommendations(
        stage: EducationStage,
        interestKey: String
    ): List<Recommendation> {
        val lower = interestKey.lowercase()

        return when {
            lower.contains("tech") || lower.contains("code") || lower.contains("build") || lower.contains("software") -> listOf(
                Recommendation(
                    pathwayId = "bca_software_dev",
                    pathwayTitle = "Software Development",
                    goalTitle = "Full-Stack Software Developer",
                    rationale = "Ideal if you enjoy building interactive tools, creating web applications, and problem-solving."
                ),
                Recommendation(
                    pathwayId = "mca_ai_ml",
                    pathwayTitle = "Artificial Intelligence & ML",
                    goalTitle = "AI & Machine Learning Engineer",
                    rationale = "Recommended if you like data analysis, pattern recognition, and working with modern AI models."
                )
            )

            lower.contains("art") || lower.contains("draw") || lower.contains("create") || lower.contains("design") -> listOf(
                Recommendation(
                    pathwayId = "arts_illustration",
                    pathwayTitle = "Digital Illustration & Art",
                    goalTitle = "Professional Digital Illustrator",
                    rationale = "Recommended if you want to turn visual imagination into professional digital artwork or concept design."
                ),
                Recommendation(
                    pathwayId = "hobby_photography",
                    pathwayTitle = "Professional Photography",
                    goalTitle = "Professional Photographer & Visual Storyteller",
                    rationale = "Perfect if you enjoy capturing real-world moments, composition, and visual storytelling."
                )
            )

            lower.contains("money") || lower.contains("finance") || lower.contains("business") || lower.contains("account") -> listOf(
                Recommendation(
                    pathwayId = "comm_ca_cma_cs",
                    pathwayTitle = "Chartered Accountancy",
                    goalTitle = "CA Foundation Examination",
                    rationale = "The premier professional qualification for mastering corporate finance, taxation, and accounting standards."
                ),
                Recommendation(
                    pathwayId = "comm_banking_investment",
                    pathwayTitle = "Banking & Financial Markets",
                    goalTitle = "Financial Analyst & Equity Research",
                    rationale = "Suited for learners interested in investment evaluation, stock markets, and financial forecasting."
                )
            )

            lower.contains("health") || lower.contains("doctor") || lower.contains("bio") || lower.contains("medicine") -> listOf(
                Recommendation(
                    pathwayId = "med_clinical_mbbs",
                    pathwayTitle = "Clinical Medicine (MBBS)",
                    goalTitle = "Clinical Physician / Doctor Readiness",
                    rationale = "Direct route to diagnosing illnesses, patient care, and human physiological health."
                ),
                Recommendation(
                    pathwayId = "med_pg_prep",
                    pathwayTitle = "Postgraduate Medical Preparation",
                    goalTitle = "NEET PG / INI-CET Medical Entrance",
                    rationale = "Crucial for medical graduates preparing for specialized clinical residency."
                )
            )

            else -> listOf(
                Recommendation(
                    pathwayId = "pathway_applied",
                    pathwayTitle = "Applied Skills & Practical Mastery",
                    goalTitle = "Hands-On Skill Builder",
                    rationale = "A structured, project-driven learning path that prioritizes practical capability over rote memorization."
                )
            )
        }
    }
}
