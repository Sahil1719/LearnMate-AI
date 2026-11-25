package com.learnmate.ai.domain.quiz

object OfflineQuizBank {

    val quizSets: List<QuizSet> = listOf(
        // --- 1. Engineering / Computer Science ---
        QuizSet(
            id = "quiz_c_pointers",
            topicName = "Pointers & Memory Architecture",
            relatedSkill = "C Programming",
            domain = "Engineering",
            description = "Test understanding of memory addresses, pointer dereferencing, and dynamic allocation in C/C++.",
            questions = listOf(
                QuizQuestion(
                    id = "q_c_ptr_1",
                    domain = "Engineering",
                    topic = "Pointers & Memory Architecture",
                    relatedSkill = "C Programming",
                    questionText = "If int *p = malloc(sizeof(int)), what does the expression *p evaluate to?",
                    options = listOf(
                        "The memory address allocated on the heap",
                        "The integer value stored at the allocated heap address",
                        "The size of the pointer variable on the stack",
                        "A null reference exception"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Dereferencing a pointer using the unary * operator retrieves the value stored at that memory location."
                ),
                QuizQuestion(
                    id = "q_c_ptr_2",
                    domain = "Engineering",
                    topic = "Pointers & Memory Architecture",
                    relatedSkill = "C Programming",
                    questionText = "What critical issue occurs if a pointer continues pointing to a memory block after free(ptr) is called without reassigning to NULL?",
                    options = listOf(
                        "Memory leak",
                        "Dangling pointer",
                        "Stack overflow",
                        "Immediate compilation error"
                    ),
                    correctOptionIndex = 1,
                    explanation = "A dangling pointer points to deallocated memory; accessing it causes undefined behavior, security vulnerabilities, or segmentation faults."
                ),
                QuizQuestion(
                    id = "q_c_ptr_3",
                    domain = "Engineering",
                    topic = "Pointers & Memory Architecture",
                    relatedSkill = "C Programming",
                    questionText = "On a standard 64-bit ARM architecture, what is the size of any raw pointer variable (e.g., char*, int*)?",
                    options = listOf(
                        "4 bytes (32 bits)",
                        "8 bytes (64 bits)",
                        "16 bytes (128 bits)",
                        "Variable depending on the target data type size"
                    ),
                    correctOptionIndex = 1,
                    explanation = "In 64-bit architectures (like ARM64/AArch64), all memory addresses are 64 bits (8 bytes) wide."
                ),
                QuizQuestion(
                    id = "q_c_ptr_4",
                    domain = "Engineering",
                    topic = "Pointers & Memory Architecture",
                    relatedSkill = "C Programming",
                    questionText = "Which memory segment stores local variables declared inside a function?",
                    options = listOf(
                        "Heap",
                        "Call Stack",
                        "BSS segment",
                        "Data segment"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Local variables with automatic storage duration are allocated on the call stack frame and released on function return."
                ),
                QuizQuestion(
                    id = "q_c_ptr_5",
                    domain = "Engineering",
                    topic = "Pointers & Memory Architecture",
                    relatedSkill = "C Programming",
                    questionText = "What does the 'volatile' keyword signify when declaring a hardware pointer in embedded systems?",
                    options = listOf(
                        "The variable must be stored in high-speed CPU registers",
                        "The compiler must not optimize reads/writes because the value can change outside program control",
                        "The variable is immutable and placed in ROM",
                        "The pointer is thread-safe and protected by a mutex"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Volatile informs the compiler that the value may be modified by hardware peripherals or ISRs, suppressing caching in registers."
                )
            )
        ),

        // --- 2. Data Structures & Algorithms ---
        QuizSet(
            id = "quiz_dsa_trees",
            topicName = "Binary Search Trees & Complexity",
            relatedSkill = "Data Structures",
            domain = "Engineering",
            description = "Evaluate time complexity, balancing properties, and traversal mechanics of search trees.",
            questions = listOf(
                QuizQuestion(
                    id = "q_dsa_1",
                    domain = "Engineering",
                    topic = "Binary Search Trees & Complexity",
                    relatedSkill = "Data Structures",
                    questionText = "What is the worst-case time complexity of searching an element in an unbalanced Binary Search Tree of N nodes?",
                    options = listOf(
                        "O(1)",
                        "O(log N)",
                        "O(N)",
                        "O(N log N)"
                    ),
                    correctOptionIndex = 2,
                    explanation = "When elements are inserted in sorted order, an unbalanced BST degenerates into a linked list, yielding O(N) search time."
                ),
                QuizQuestion(
                    id = "q_dsa_2",
                    domain = "Engineering",
                    topic = "Binary Search Trees & Complexity",
                    relatedSkill = "Data Structures",
                    questionText = "Which tree traversal order visits nodes in strictly ascending sorted order for a standard BST?",
                    options = listOf(
                        "Pre-order (Root, Left, Right)",
                        "In-order (Left, Root, Right)",
                        "Post-order (Left, Right, Root)",
                        "Breadth-first Level-order"
                    ),
                    correctOptionIndex = 1,
                    explanation = "In-order traversal visits the left subtree (smaller), then current node, then right subtree (larger), producing sorted order."
                ),
                QuizQuestion(
                    id = "q_dsa_3",
                    domain = "Engineering",
                    topic = "Binary Search Trees & Complexity",
                    relatedSkill = "Data Structures",
                    questionText = "What is the average lookup time in a properly sized Hash Table with good hash distribution?",
                    options = listOf(
                        "O(1)",
                        "O(log N)",
                        "O(N)",
                        "O(N^2)"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Hash tables achieve constant expected time O(1) for search, insert, and delete when hash collisions are minimal."
                ),
                QuizQuestion(
                    id = "q_dsa_4",
                    domain = "Engineering",
                    topic = "Binary Search Trees & Complexity",
                    relatedSkill = "Data Structures",
                    questionText = "What balance condition distinguishes an AVL tree?",
                    options = listOf(
                        "Every node must have exactly two children",
                        "The heights of left and right subtrees of any node differ by at most 1",
                        "The root node must always equal the median element",
                        "Nodes must alternate colors between red and black"
                    ),
                    correctOptionIndex = 1,
                    explanation = "An AVL tree maintains height balance such that the balance factor (|height(left) - height(right)|) <= 1 for all nodes."
                ),
                QuizQuestion(
                    id = "q_dsa_5",
                    domain = "Engineering",
                    topic = "Binary Search Trees & Complexity",
                    relatedSkill = "Data Structures",
                    questionText = "Which data structure operates on a First-In, First-Out (FIFO) principle?",
                    options = listOf(
                        "Stack",
                        "Queue",
                        "Max-Heap",
                        "Trie"
                    ),
                    correctOptionIndex = 1,
                    explanation = "A Queue adheres to FIFO ordering, where elements are enqueued at the back and dequeued from the front."
                )
            )
        ),

        // --- 3. Programming & Software: Python Fundamentals ---
        QuizSet(
            id = "quiz_python_core",
            topicName = "Python Concurrency & Data Structures",
            relatedSkill = "Python Programming",
            domain = "Technology",
            description = "Test comprehension of Python memory model, GIL, generators, and data structures.",
            questions = listOf(
                QuizQuestion(
                    id = "q_py_1",
                    domain = "Technology",
                    topic = "Python Concurrency & Data Structures",
                    relatedSkill = "Python Programming",
                    questionText = "What is the role of Python's Global Interpreter Lock (GIL) in CPython?",
                    options = listOf(
                        "It prevents multiple Python processes from sharing memory",
                        "It ensures only one native thread executes Python bytecode at any given moment",
                        "It automatically accelerates numerical loops via SIMD vectorization",
                        "It encrypts variable names during bytecode compilation"
                    ),
                    correctOptionIndex = 1,
                    explanation = "The CPython GIL prevents thread race conditions on reference counts by restricting bytecode execution to one thread at a time."
                ),
                QuizQuestion(
                    id = "q_py_2",
                    domain = "Technology",
                    topic = "Python Concurrency & Data Structures",
                    relatedSkill = "Python Programming",
                    questionText = "What is the primary memory advantage of using a generator expression or 'yield' over a list comprehension?",
                    options = listOf(
                        "Generators calculate values lazily one-at-a-time, consuming O(1) auxiliary memory",
                        "Generators execute in C rather than Python bytecode",
                        "Generators bypass the garbage collector entirely",
                        "Generators serialize directly to disk"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Generators stream items on-demand using lazy evaluation, avoiding the need to allocate memory for the entire collection upfront."
                ),
                QuizQuestion(
                    id = "q_py_3",
                    domain = "Technology",
                    topic = "Python Concurrency & Data Structures",
                    relatedSkill = "Python Programming",
                    questionText = "In Python, which of the following types is mutable?",
                    options = listOf(
                        "tuple",
                        "str",
                        "list",
                        "frozenset"
                    ),
                    correctOptionIndex = 2,
                    explanation = "Lists in Python are mutable sequences that support in-place modification, appending, and item reassignment."
                ),
                QuizQuestion(
                    id = "q_py_4",
                    domain = "Technology",
                    topic = "Python Concurrency & Data Structures",
                    relatedSkill = "Python Programming",
                    questionText = "What does the expression [x**2 for x in range(5) if x % 2 == 0] produce?",
                    options = listOf(
                        "[0, 4, 16]",
                        "[1, 9]",
                        "[0, 1, 4, 9, 16]",
                        "[4, 16]"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Range(5) is 0,1,2,3,4. The even numbers are 0, 2, 4. Their squares are 0, 4, 16."
                ),
                QuizQuestion(
                    id = "q_py_5",
                    domain = "Technology",
                    topic = "Python Concurrency & Data Structures",
                    relatedSkill = "Python Programming",
                    questionText = "Which module in Python is recommended for CPU-bound parallel execution to bypass the GIL?",
                    options = listOf(
                        "threading",
                        "multiprocessing",
                        "asyncio",
                        "socket"
                    ),
                    correctOptionIndex = 1,
                    explanation = "The multiprocessing module spawns separate OS processes with independent Python interpreters and memory spaces, overcoming GIL limits."
                )
            )
        ),

        // --- 4. Commerce & Finance ---
        QuizSet(
            id = "quiz_fin_accounting",
            topicName = "Financial Accounting & Valuation",
            relatedSkill = "Financial Analysis",
            domain = "Commerce",
            description = "Assess knowledge of financial statements, balance sheet equations, and working capital.",
            questions = listOf(
                QuizQuestion(
                    id = "q_fin_1",
                    domain = "Commerce",
                    topic = "Financial Accounting & Valuation",
                    relatedSkill = "Financial Analysis",
                    questionText = "What is the fundamental accounting equation represented on every corporate balance sheet?",
                    options = listOf(
                        "Assets = Liabilities + Shareholders' Equity",
                        "Net Income = Revenue - Expenses - Dividends",
                        "Cash Flow = Operating Profit + Working Capital",
                        "Assets + Liabilities = Retained Earnings"
                    ),
                    correctOptionIndex = 0,
                    explanation = "The fundamental accounting equation states that everything a company owns (Assets) was financed by borrowing (Liabilities) or owners (Equity)."
                ),
                QuizQuestion(
                    id = "q_fin_2",
                    domain = "Commerce",
                    topic = "Financial Accounting & Valuation",
                    relatedSkill = "Financial Analysis",
                    questionText = "How is Net Working Capital formally defined?",
                    options = listOf(
                        "Total Assets minus Total Liabilities",
                        "Current Assets minus Current Liabilities",
                        "Operating Cash Flow minus Capital Expenditures",
                        "Gross Margin minus Operating Overhead"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Net Working Capital measures short-term liquidity by comparing short-term assets (cash, receivables, inventory) against short-term debts."
                ),
                QuizQuestion(
                    id = "q_fin_3",
                    domain = "Commerce",
                    topic = "Financial Accounting & Valuation",
                    relatedSkill = "Financial Analysis",
                    questionText = "Which financial statement reports a company's cash generation from operations, investments, and financing over a given period?",
                    options = listOf(
                        "Balance Sheet",
                        "Cash Flow Statement",
                        "Income Statement",
                        "Statement of Retained Earnings"
                    ),
                    correctOptionIndex = 1,
                    explanation = "The Cash Flow Statement reconciles accrual-based net income to actual cash movements across operating, investing, and financing activities."
                ),
                QuizQuestion(
                    id = "q_fin_4",
                    domain = "Commerce",
                    topic = "Financial Accounting & Valuation",
                    relatedSkill = "Financial Analysis",
                    questionText = "What does a high Price-to-Earnings (P/E) ratio typically signal about investor expectations for a stock?",
                    options = listOf(
                        "Investors anticipate high future earnings growth",
                        "The company is facing severe liquidity risk",
                        "The stock is fundamentally undervalued by market analysts",
                        "The company distributes 100% of profits as dividends"
                    ),
                    correctOptionIndex = 0,
                    explanation = "A elevated P/E ratio indicates market willingness to pay a premium per dollar of earnings because robust future growth is expected."
                ),
                QuizQuestion(
                    id = "q_fin_5",
                    domain = "Commerce",
                    topic = "Financial Accounting & Valuation",
                    relatedSkill = "Financial Analysis",
                    questionText = "If a firm purchases manufacturing machinery for cash, how is the balance sheet impacted immediately?",
                    options = listOf(
                        "Total assets increase while liabilities remain unchanged",
                        "Non-current assets increase and current assets decrease by equal amounts (total assets unchanged)",
                        "Retained earnings decrease immediately by the full machinery price",
                        "Liabilities decrease"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Cash (Current Asset) drops and Property/Plant/Equipment (Non-current Asset) rises equally; total assets stay constant."
                )
            )
        ),

        // --- 5. Medicine & Healthcare ---
        QuizSet(
            id = "quiz_med_triage",
            topicName = "Clinical Triage & Physiology",
            relatedSkill = "Clinical Assessment",
            domain = "Medicine",
            description = "Test fundamental emergency triage, oxygen saturation thresholds, and cardiovascular physiology.",
            questions = listOf(
                QuizQuestion(
                    id = "q_med_1",
                    domain = "Medicine",
                    topic = "Clinical Triage & Physiology",
                    relatedSkill = "Clinical Assessment",
                    questionText = "What is the primary physiological determinant of mean arterial blood pressure (MAP)?",
                    options = listOf(
                        "Cardiac Output multiplied by Systemic Vascular Resistance",
                        "Total Blood Volume divided by Respiratory Rate",
                        "Venous Return minus Pulmonary Wedge Pressure",
                        "Glomerular Filtration Rate times Hemoglobin Count"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Mean Arterial Pressure is governed by hemodynamics: MAP = Cardiac Output (CO) × Systemic Vascular Resistance (SVR)."
                ),
                QuizQuestion(
                    id = "q_med_2",
                    domain = "Medicine",
                    topic = "Clinical Triage & Physiology",
                    relatedSkill = "Clinical Assessment",
                    questionText = "In emergency triage, an adult patient presents with acute crushing chest pressure radiating to the left jaw with diaphoresis. What is the immediate priority?",
                    options = listOf(
                        "Schedule an elective outpatient stress test next week",
                        "Immediate 12-lead ECG and emergency escalation for acute coronary syndrome",
                        "Prescribe oral antacids and discharge home",
                        "Check routine lipid panel with 24-hour turnaround"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Acute crushing chest pain with radiation and diaphoresis is hallmark acute myocardial infarction requiring emergent ECG and urgent care."
                ),
                QuizQuestion(
                    id = "q_med_3",
                    domain = "Medicine",
                    topic = "Clinical Triage & Physiology",
                    relatedSkill = "Clinical Assessment",
                    questionText = "What normal arterial oxygen saturation (SpO2) range is expected in a healthy individual breathing room air at sea level?",
                    options = listOf(
                        "85% - 89%",
                        "90% - 92%",
                        "95% - 100%",
                        "70% - 75%"
                    ),
                    correctOptionIndex = 2,
                    explanation = "Normal physiological SpO2 on room air is 95% to 100%. Levels below 90% represent clinically significant hypoxemia."
                ),
                QuizQuestion(
                    id = "q_med_4",
                    domain = "Medicine",
                    topic = "Clinical Triage & Physiology",
                    relatedSkill = "Clinical Assessment",
                    questionText = "What does the Glasgow Coma Scale (GCS) evaluate in acute neurological assessment?",
                    options = listOf(
                        "Eye opening, verbal response, and motor response",
                        "Pupillary reflex, tendon jerk, and blood pressure",
                        "Cranial nerve pairs I through XII",
                        "Blood glucose, heart rate, and body temperature"
                    ),
                    correctOptionIndex = 0,
                    explanation = "GCS scores neurological impairment across 3 categories: Eye opening (1-4), Verbal response (1-5), and Motor response (1-6)."
                ),
                QuizQuestion(
                    id = "q_med_5",
                    domain = "Medicine",
                    topic = "Clinical Triage & Physiology",
                    relatedSkill = "Clinical Assessment",
                    questionText = "Which term describes the amount of blood ejected by the left ventricle in a single cardiac contraction?",
                    options = listOf(
                        "Cardiac Output",
                        "Stroke Volume",
                        "Ejection Fraction",
                        "End-Systolic Volume"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Stroke volume is the volume of blood pumped from the left ventricle per beat, typically ~70 mL in an adult."
                )
            )
        ),

        // --- 6. Science & Physics ---
        QuizSet(
            id = "quiz_sci_thermo",
            topicName = "Thermodynamics & Electromagnetism",
            relatedSkill = "Physics",
            domain = "Science",
            description = "Examine core physical laws of energy conservation, entropy, and electromagnetic induction.",
            questions = listOf(
                QuizQuestion(
                    id = "q_sci_1",
                    domain = "Science",
                    topic = "Thermodynamics & Electromagnetism",
                    relatedSkill = "Physics",
                    questionText = "What does the Second Law of Thermodynamics dictate regarding isolated systems?",
                    options = listOf(
                        "Total energy must decrease continuously over time",
                        "Total entropy of an isolated system always increases or remains constant in reversible processes",
                        "Absolute zero can be achieved with finite cooling stages",
                        "Work can be converted completely to heat with 100% thermal efficiency in any cyclic engine"
                    ),
                    correctOptionIndex = 1,
                    explanation = "The Second Law dictates that the total entropy (disorder) of an isolated system never decreases over time: dS >= 0."
                ),
                QuizQuestion(
                    id = "q_sci_2",
                    domain = "Science",
                    topic = "Thermodynamics & Electromagnetism",
                    relatedSkill = "Physics",
                    questionText = "According to Faraday's Law of Induction, what induces an electromotive force (EMF) in a closed conductive circuit?",
                    options = listOf(
                        "A constant uniform magnetic field",
                        "A time-varying magnetic flux through the circuit loop",
                        "A static electric potential in adjacent vacuum",
                        "High electrical resistance in the wire"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Faraday's Law states that EMF = -dPhi_B / dt: EMF is proportional to the rate of change of magnetic flux."
                ),
                QuizQuestion(
                    id = "q_sci_3",
                    domain = "Science",
                    topic = "Thermodynamics & Electromagnetism",
                    relatedSkill = "Physics",
                    questionText = "What is the kinetic energy of an object of mass 2 kg traveling at a velocity of 3 m/s?",
                    options = listOf(
                        "6 Joules",
                        "9 Joules",
                        "12 Joules",
                        "18 Joules"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Kinetic energy KE = 0.5 * m * v^2 = 0.5 * 2 * 3^2 = 9 Joules."
                ),
                QuizQuestion(
                    id = "q_sci_4",
                    domain = "Science",
                    topic = "Thermodynamics & Electromagnetism",
                    relatedSkill = "Physics",
                    questionText = "Which thermodynamic process occurs at constant pressure?",
                    options = listOf(
                        "Isochoric",
                        "Isothermal",
                        "Isobaric",
                        "Adiabatic"
                    ),
                    correctOptionIndex = 2,
                    explanation = "Isobaric describes a thermodynamic process where pressure (bar) remains strictly constant throughout."
                ),
                QuizQuestion(
                    id = "q_sci_5",
                    domain = "Science",
                    topic = "Thermodynamics & Electromagnetism",
                    relatedSkill = "Physics",
                    questionText = "What fundamental particle carries the electromagnetic force according to the Standard Model?",
                    options = listOf(
                        "Gluon",
                        "Photon",
                        "W Boson",
                        "Higgs Boson"
                    ),
                    correctOptionIndex = 1,
                    explanation = "The photon is the gauge boson that mediates electromagnetic interactions between charged particles."
                )
            )
        ),

        // --- 7. Arts & Humanities ---
        QuizSet(
            id = "quiz_hum_logic",
            topicName = "Critical Thinking & Epistemology",
            relatedSkill = "Critical Thinking",
            domain = "Arts & Humanities",
            description = "Examine formal logic, cognitive biases, deductive reasoning, and philosophical epistemology.",
            questions = listOf(
                QuizQuestion(
                    id = "q_hum_1",
                    domain = "Arts & Humanities",
                    topic = "Critical Thinking & Epistemology",
                    relatedSkill = "Critical Thinking",
                    questionText = "Which logical fallacy occurs when an opponent's argument is mischaracterized in an exaggerated form to make it easier to attack?",
                    options = listOf(
                        "Ad Hominem",
                        "Straw Man",
                        "Post Hoc Ergo Propter Hoc",
                        "Begging the Question"
                    ),
                    correctOptionIndex = 1,
                    explanation = "A Straw Man fallacy replaces an opponent's actual argument with a distorted, weakened, or extreme counterfeit."
                ),
                QuizQuestion(
                    id = "q_hum_2",
                    domain = "Arts & Humanities",
                    topic = "Critical Thinking & Epistemology",
                    relatedSkill = "Critical Thinking",
                    questionText = "What distinguishes deductive reasoning from inductive reasoning?",
                    options = listOf(
                        "If the premises are true, a valid deductive argument guarantees the truth of its conclusion",
                        "Deductive reasoning always moves from specific observations to broad probabilistic generalizations",
                        "Deductive reasoning relies exclusively on empirical sensory experimentation",
                        "Deductive conclusions can never be falsified"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Deductive logic guarantees necessary conclusions from true premises; inductive reasoning produces probable conclusions based on evidence."
                ),
                QuizQuestion(
                    id = "q_hum_3",
                    domain = "Arts & Humanities",
                    topic = "Critical Thinking & Epistemology",
                    relatedSkill = "Critical Thinking",
                    questionText = "Which cognitive bias describes the psychological tendency to seek, interpret, and recall information that confirms preexisting beliefs?",
                    options = listOf(
                        "Anchoring Bias",
                        "Confirmation Bias",
                        "Availability Heuristic",
                        "Dunning-Kruger Effect"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Confirmation bias leads individuals to favor information confirming their prior hypotheses while discounting contrary evidence."
                ),
                QuizQuestion(
                    id = "q_hum_4",
                    domain = "Arts & Humanities",
                    topic = "Critical Thinking & Epistemology",
                    relatedSkill = "Critical Thinking",
                    questionText = "What is the traditional tripartite definition of knowledge in classical epistemology?",
                    options = listOf(
                        "Justified True Belief",
                        "Empirical Sensory Observation",
                        "Deductive Rationalism",
                        "Consensus Authority"
                    ),
                    correctOptionIndex = 0,
                    explanation = "Classical epistemology tracing from Plato defines knowledge as Justified True Belief (JTB)."
                ),
                QuizQuestion(
                    id = "q_hum_5",
                    domain = "Arts & Humanities",
                    topic = "Critical Thinking & Epistemology",
                    relatedSkill = "Critical Thinking",
                    questionText = "What is the primary function of peer review in academic and scientific scholarship?",
                    options = listOf(
                        "To guarantee zero typographical errors in publishing",
                        "To independently evaluate research validity, methodology, and originality by domain experts",
                        "To ensure all published research aligns with popular political opinions",
                        "To automatically transfer copyright patents to academic journals"
                    ),
                    correctOptionIndex = 1,
                    explanation = "Peer review subjects academic manuscripts to critical scrutiny by independent experts to ensure rigorous methodology and scholarly integrity."
                )
            )
        )
    )

    fun getAllQuizzes(): List<QuizSet> = quizSets

    fun getQuizById(quizId: String): QuizSet? {
        return quizSets.firstOrNull { it.id.equals(quizId, ignoreCase = true) }
    }

    fun getQuizForSkill(skillName: String): QuizSet? {
        return quizSets.firstOrNull { it.relatedSkill.equals(skillName, ignoreCase = true) }
            ?: quizSets.firstOrNull { it.topicName.contains(skillName, ignoreCase = true) || skillName.contains(it.relatedSkill, ignoreCase = true) }
    }

    fun getRecommendedQuiz(targetDomain: String, activeSkills: List<String>): QuizSet {
        // First match against any active skill
        for (skill in activeSkills) {
            val match = getQuizForSkill(skill)
            if (match != null) return match
        }
        // Next match by domain
        val domainMatch = quizSets.firstOrNull { it.domain.equals(targetDomain, ignoreCase = true) }
        if (domainMatch != null) return domainMatch

        // Fallback to first quiz
        return quizSets.first()
    }
}
