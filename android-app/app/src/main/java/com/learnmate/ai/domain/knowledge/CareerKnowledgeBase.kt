package com.learnmate.ai.domain.knowledge

import com.learnmate.ai.domain.model.CareerPathway
import com.learnmate.ai.domain.model.RoadmapPhaseTemplate

/**
 * Structured Career Knowledge Base (Section 10 & 12).
 * Deterministically dictates required competencies and prerequisite graphs.
 * The LLM interprets and contextualizes these validated records, preventing hallucination.
 */
object CareerKnowledgeBase {

    val pathways: Map<String, CareerPathway> = listOf(
        CareerPathway(
            id = "embedded_systems",
            title = "Embedded Systems Engineer",
            description = "Design and programming of firmware, microcontrollers, and real-time operating systems for hardware integration.",
            requiredSkills = mapOf(
                "C Programming" to 4,
                "Microcontrollers" to 4,
                "RTOS" to 3,
                "Digital Electronics" to 3,
                "Python" to 2,
                "Communication Protocols" to 3,
                "Debugging & Logic Analyzers" to 3
            ),
            prerequisiteGraph = mapOf(
                "Microcontrollers" to listOf("C Programming", "Digital Electronics"),
                "RTOS" to listOf("C Programming", "Microcontrollers"),
                "Communication Protocols" to listOf("Microcontrollers")
            ),
            phases = listOf(
                RoadmapPhaseTemplate(
                    phaseNumber = 1,
                    title = "Hardware Foundations & C Mastery",
                    objective = "Master low-level pointer arithmetic, memory management, bitwise manipulation, and basic circuit interfacing.",
                    topics = listOf("Pointers & Memory Allocation in C", "Bitwise Operations & Register Masking", "GPIO, Timers & Interrupt Handling"),
                    practicalProject = "Bare-Metal GPIO & Timer Driver on ARM Cortex-M",
                    estimatedWeeks = 3
                ),
                RoadmapPhaseTemplate(
                    phaseNumber = 2,
                    title = "Peripherals & Serial Communication",
                    objective = "Implement standard communication buses to interface sensors, actuators, and displays.",
                    topics = listOf("UART Transmission & Ring Buffers", "I2C Protocol & Sensor Polling", "SPI High-Speed Data Transfers"),
                    practicalProject = "Multi-Sensor Environmental Telemetry Station over I2C/SPI",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseTemplate(
                    phaseNumber = 3,
                    title = "Real-Time Operating Systems (RTOS)",
                    objective = "Implement multitasking, synchronization primitives, and deterministic scheduling.",
                    topics = listOf("Task Scheduling & Priorities in FreeRTOS", "Semaphores, Mutexes & Priority Inversion", "Queues & Event Groups"),
                    practicalProject = "Multithreaded Data Acquisition & Control System using FreeRTOS",
                    estimatedWeeks = 4
                )
            )
        ),
        CareerPathway(
            id = "ai_ml",
            title = "AI/ML Engineer",
            description = "Development, training, optimization, and edge deployment of deep neural networks and machine learning models.",
            requiredSkills = mapOf(
                "Python" to 4,
                "PyTorch" to 4,
                "Linear Algebra & Calculus" to 3,
                "Data Preprocessing" to 3,
                "Model Quantization & Edge AI" to 3,
                "MLOps & Deployment" to 3
            ),
            prerequisiteGraph = mapOf(
                "PyTorch" to listOf("Python", "Linear Algebra & Calculus"),
                "Model Quantization & Edge AI" to listOf("PyTorch"),
                "MLOps & Deployment" to listOf("PyTorch")
            ),
            phases = listOf(
                RoadmapPhaseTemplate(
                    phaseNumber = 1,
                    title = "Mathematical & Programming Core",
                    objective = "Establish computational fluency in vector calculus, linear algebra, and NumPy/PyTorch tensor fundamentals.",
                    topics = listOf("Matrix Operations & Eigenvalues", "Automatic Differentiation & Backprop", "PyTorch Tensor Operations"),
                    practicalProject = "Custom Neural Network from Scratch without Autograd",
                    estimatedWeeks = 3
                ),
                RoadmapPhaseTemplate(
                    phaseNumber = 2,
                    title = "Deep Learning Architectures",
                    objective = "Master convolution, attention mechanisms, and transformer blocks.",
                    topics = listOf("CNNs & Spatial Features", "Self-Attention & Multi-Head Transformer", "Transfer Learning & Fine-tuning"),
                    practicalProject = "Transformer Classifier for Multimodal Educational Content",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseTemplate(
                    phaseNumber = 3,
                    title = "Edge Optimization & ExecuTorch",
                    objective = "Quantize and lower models to ARM mobile execution with TorchAO and ExecuTorch.",
                    topics = listOf("Post-Training Quantization (INT8/INT4)", "Graph Capture via torch.export", "ExecuTorch Runtime Integration"),
                    practicalProject = "On-Device Neural Network Running on ARM64 Android",
                    estimatedWeeks = 4
                )
            )
        ),
        CareerPathway(
            id = "data_analyst",
            title = "Data Analyst",
            description = "Extracting actionable insights from complex datasets via statistical modeling, SQL querying, and interactive visualization.",
            requiredSkills = mapOf(
                "SQL" to 4,
                "Python" to 3,
                "Data Visualization" to 4,
                "Applied Statistics" to 3,
                "Business Intelligence Tools" to 3
            ),
            prerequisiteGraph = mapOf(
                "Data Visualization" to listOf("Python", "SQL"),
                "Applied Statistics" to listOf("Python")
            ),
            phases = listOf(
                RoadmapPhaseTemplate(
                    phaseNumber = 1,
                    title = "Relational Querying & SQL Architecture",
                    objective = "Write high-performance window functions, subqueries, and aggregate metrics.",
                    topics = listOf("Complex Joins & Window Functions", "Query Plan Optimization", "Data Warehousing Basics"),
                    practicalProject = "E-Commerce User Retention & Cohort Analysis Database",
                    estimatedWeeks = 3
                ),
                RoadmapPhaseTemplate(
                    phaseNumber = 2,
                    title = "Exploratory Analysis & Visual Analytics",
                    objective = "Build compelling visual dashboards and statistical hypothesis tests.",
                    topics = listOf("Pandas Data Wrangling", "A/B Testing & Significance", "Interactive Dashboard Design"),
                    practicalProject = "End-to-End Educational Performance Dashboard",
                    estimatedWeeks = 3
                )
            )
        ),
        CareerPathway(
            id = "full_stack",
            title = "Full Stack Developer",
            description = "Building scalable client interfaces and robust backend microservices with modern web and mobile stacks.",
            requiredSkills = mapOf(
                "JavaScript / TypeScript" to 4,
                "React / Compose" to 4,
                "Backend APIs (Node/Kotlin)" to 4,
                "Database Systems" to 3,
                "System Architecture" to 3
            ),
            prerequisiteGraph = mapOf(
                "React / Compose" to listOf("JavaScript / TypeScript"),
                "System Architecture" to listOf("Backend APIs (Node/Kotlin)", "Database Systems")
            ),
            phases = listOf(
                RoadmapPhaseTemplate(
                    phaseNumber = 1,
                    title = "Reactive Frontend & State Management",
                    objective = "Build responsive, accessible declarative UIs with clean state lifting.",
                    topics = listOf("Declarative Component Architecture", "Unidirectional Data Flow", "Local State Persistence"),
                    practicalProject = "Responsive Study Planner Client Application",
                    estimatedWeeks = 4
                ),
                RoadmapPhaseTemplate(
                    phaseNumber = 2,
                    title = "Backend Services & Concurrency",
                    objective = "Architect RESTful/gRPC microservices with relational schema isolation.",
                    topics = listOf("API Design & Authentication", "Database Indexing & Transactions", "Asynchronous Task Queues"),
                    practicalProject = "Distributed Learning Progress Tracking Service",
                    estimatedWeeks = 4
                )
            )
        ),
        CareerPathway(
            id = "cybersecurity",
            title = "Cybersecurity Analyst",
            description = "Securing digital infrastructure, identifying vulnerabilities, and formulating incident response protocols.",
            requiredSkills = mapOf(
                "Computer Networks" to 4,
                "Linux Systems Administration" to 4,
                "Security Protocols & Cryptography" to 3,
                "Threat Detection & SIEM" to 3,
                "Python Scripting" to 2
            ),
            prerequisiteGraph = mapOf(
                "Security Protocols & Cryptography" to listOf("Computer Networks"),
                "Threat Detection & SIEM" to listOf("Linux Systems Administration", "Computer Networks")
            ),
            phases = listOf(
                RoadmapPhaseTemplate(
                    phaseNumber = 1,
                    title = "Networking & System Hardening",
                    objective = "Analyze packets, configure firewalls, and audit Linux operating system security.",
                    topics = listOf("TCP/IP & Packet Analysis (Wireshark)", "Linux Permissions & Auditd", "Firewall Configuration (iptables/nftables)"),
                    practicalProject = "Automated Network Intrusion Detection Script",
                    estimatedWeeks = 4
                )
            )
        ),
        CareerPathway(
            id = "vlsi_design",
            title = "VLSI Design Engineer",
            description = "Designing, simulating, and verifying integrated circuits using hardware description languages and synthesis tools.",
            requiredSkills = mapOf(
                "Digital Electronics" to 4,
                "Verilog / SystemVerilog" to 4,
                "Computer Architecture" to 3,
                "Static Timing Analysis" to 3,
                "FPGA Prototyping" to 3
            ),
            prerequisiteGraph = mapOf(
                "Verilog / SystemVerilog" to listOf("Digital Electronics"),
                "Static Timing Analysis" to listOf("Digital Electronics", "Computer Architecture"),
                "FPGA Prototyping" to listOf("Verilog / SystemVerilog")
            ),
            phases = listOf(
                RoadmapPhaseTemplate(
                    phaseNumber = 1,
                    title = "Digital Logic & RTL Modeling",
                    objective = "Design synchronous state machines and combinational arithmetic units in Verilog.",
                    topics = listOf("Combinational & Sequential Circuits", "FSM Design & Optimization", "Testbench Writing & Simulation"),
                    practicalProject = "Pipelined 32-bit ALU Design and Verification",
                    estimatedWeeks = 4
                )
            )
        ),
        CareerPathway(
            id = "cloud_engineer",
            title = "Cloud Engineer",
            description = "Architecting resilient, automated cloud infrastructure with containers, orchestration, and CI/CD pipelines.",
            requiredSkills = mapOf(
                "Linux & Shell Scripting" to 4,
                "Docker & Containers" to 4,
                "Kubernetes" to 3,
                "Infrastructure as Code" to 3,
                "Cloud Networking & IAM" to 3
            ),
            prerequisiteGraph = mapOf(
                "Docker & Containers" to listOf("Linux & Shell Scripting"),
                "Kubernetes" to listOf("Docker & Containers"),
                "Infrastructure as Code" to listOf("Cloud Networking & IAM")
            ),
            phases = listOf(
                RoadmapPhaseTemplate(
                    phaseNumber = 1,
                    title = "Containerization & Microservices Infrastructure",
                    objective = "Build lightweight Docker images and orchestrate multi-tier applications.",
                    topics = listOf("Docker Container Internals", "Compose Multi-Container Stacks", "Networking & Volume Mounts"),
                    practicalProject = "Containerized Scalable Educational Microservice",
                    estimatedWeeks = 4
                )
            )
        )
    ).associateBy { it.id }

    fun getPathway(id: String): CareerPathway? = pathways[id]
    fun getAllPathways(): List<CareerPathway> = pathways.values.toList()
}
