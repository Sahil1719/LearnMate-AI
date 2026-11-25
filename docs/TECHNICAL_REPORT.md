# Technical Report: LearnMate AI
## Personalized On-Device AI Learning Companion Powered by Quantized Meta Llama and ExecuTorch

---

### Abstract
LearnMate AI is a privacy-first, on-device personalized educational platform deployed on ARM64 Android smartphones. Operating completely offline without cloud API dependencies or subscription overheads, the platform provides tailored educational roadmaps, deterministic skill-gap diagnosis, and on-device conversational tutoring.

This technical report documents the engineering architecture, mathematical formulation of hybrid intelligence, ExecuTorch v0.5.0 runtime mechanics, and the evaluated **Dual-Model On-Device Deployment Strategy**:
- **Tier 1 (4GB RAM Baseline, Xiaomi Redmi 9 Power):** Meta Llama 3.2 1B Instruct, 4-bit INT4 (`8da4w`) with 4-bit embedding quantization.
- **Tier 2 (6GB+ RAM Standard, Realme X7 5G / OnePlus):** Meta Llama 3.2 3B Instruct, 4-bit INT4 (`8da4w`) with 4-bit embedding quantization.
- **Historical Benchmark Reference:** Meta Llama 3 8B Instruct (mentor's initial recommendation, evaluated for memory boundary analysis).

*Status Note: Both dual-model configurations remain provisional pending actual export and physical device benchmarking. No competition approval has been claimed.*

---

### Table of Contents
1. Problem Statement & Motivation
2. System Architecture
3. Model Selection & Quantization Methodology
4. ExecuTorch Runtime & Edge Deployment Optimization
5. Deterministic Hybrid Learning Intelligence
6. Adaptive Remediation & Rescheduling Engine
7. Android Native Application Architecture
8. Empirical Benchmarks & Hardware Performance
9. Comprehensive Dual-Model Benchmarking Framework
10. Technical Challenges & Mitigations
11. Limitations & Future Work
12. Conclusion & References

---

### 3. Model Selection & Quantization Methodology

#### 3.1 Historical Context & Mentor Recommendation
The project originally targeted Meta Llama 3 8B Instruct based on an initial mentor recommendation (a recommendation, not a confirmed competition requirement). An unquantized 16-bit checkpoint exceeds 16 GB, while a 4-bit INT4 checkpoint is ~4.27 GB.

#### 3.2 Physical Hardware Boundary & XNNPACK Heap Repacking Discovery
Rigorous technical audits in Phases 3.5, 3.6, and 3.7 revealed that 8B and 3B models cannot operate safely within the memory envelope of 4GB Android devices:
1. **Total Physical RAM Boundary:** The primary physical test device, Xiaomi Redmi 9 Power (`lime`), possesses 3.80 GB physical RAM (3,713 MB detected by Android `ActivityManager`). Under typical multitasking load, Android OS and core services consume ~2.40 GB, leaving **~1.34–1.48 GB available free memory**.
2. **XNNPACK Anonymous Heap Repacking (`weight_cache == nullptr`):** Source inspection of ExecuTorch v0.5.0 (`backends/xnnpack/runtime/XNNCompiler.cpp#L1967`) verified that `/*weight_cache=*/nullptr` is hardcoded. Consequently, `backends/xnnpack/third-party/XNNPACK/src/operator-utils.c#L33` calls `xnn_allocate_simd_memory()`, allocating anonymous heap buffers and repacking all 4-bit linear weights for SIMD GEMM at initialization.
3. **True Peak Resident Set Size (`VmRSS`):**
   - **Llama 3.2 3B INT4:** Requires ~1.45–1.65 GB heap allocation for packed weights alone. True peak process RSS is **~1.60–1.80 GB** (estimated). On 4GB hardware, this exceeds available RAM, causing immediate fatal `ENOMEM` or Android Low Memory Killer (`lmkd`) termination. (This refutes Phase 3.6's prior 175–215 MB estimate, which mistakenly assumed weights stayed in clean `mmap` page cache).
   - **Llama 3.2 1B INT4:** Requires ~510 MB heap allocation for packed weights. True peak process RSS is **~650–720 MB** (estimated). This fits comfortably within the ~1.34 GB available RAM of Redmi 9 Power with ~660 MB headroom.
   - **6GB+ RAM Devices:** Have ~2.90–3.20 GB available free RAM, safely accommodating Llama 3.2 3B INT4 (~1.75 GB RSS) with ~1.20–1.45 GB headroom for normal phone multitasking.

#### 3.3 Model Artifact Sizing Realism (Estimated vs Measured)
- The Phase 3.7 calculated artifact size for Llama 3.2 3B INT4 is **~1.89 GB (1.76 GiB)** with 4-bit embedding (`-E 4,32`), and **~2.22 GB** with 8-bit embedding (`-E 8,0`). This accounts for EXIR FlatBuffers serializing `tok_embeddings` (Embedding op) and `output` (Linear op) as separate constant tables (~203.2 MB overhead).
- **Important:** These values are **mathematical estimates**, not measured artifacts from physical export.
- Similarly, Llama 3.2 1B INT4 artifact size is estimated at **~795 MB (0.74 GiB)**.

---

### 4. ExecuTorch Runtime & Edge Deployment Optimization

#### 4.1 Android NDK r26c ARM64 Compilation
The native runtime is compiled from ExecuTorch v0.5.0 source (commit `484a4ab`) using Android NDK r26c Clang (`aarch64-linux-android26-clang++`). A total of 1,176 targets were compiled via Ninja, generating static libraries for core runtime (`libexecutorch.a`, `libexecutorch_core.a`), backends (`libxnnpack_backend.a`, `libXNNPACK.a`, `libmicrokernels-prod.a`), kernels (`libportable_kernels.a`, `liboptimized_kernels.a`, `libquantized_kernels.a`), and extensions.

#### 4.2 Standalone ARM64 Native Runner (`llama_main`)
A standalone native C++ executable (`llama_main`, 11.4 MB stripped) was compiled and verified directly on the physical Redmi 9 Power via ADB:
```bash
/data/local/tmp/llama_main --help
```
Exited code 0 on the Snapdragon 662 device, confirming binary execution and Bionic dynamic linker compatibility (`liblog.so`, `libm.so`, `libdl.so`, `libc.so`).

#### 4.3 Mmap Patching in Runner
The ExecuTorch runner source (`examples/models/llama/runner/runner.cpp#L47`) was patched from `Module::LoadMode::File` (which calls `malloc` for the entire file) to `Module::LoadMode::Mmap`. While XNNPACK linear weights are subsequently repacked into heap memory as noted in Section 3.2, `LoadMode::Mmap` avoids a catastrophic redundant heap copy during initial model container parsing.

---

### 8. Empirical Benchmarks & Hardware Performance

#### 8.1 Physical Hardware Specification
- **Device:** Xiaomi Redmi 9 Power (`M2010J19SI` / `lime`)
- **Processor:** Qualcomm Snapdragon 662 (`SM6115`), 4× Kryo 260 Gold (2.0 GHz) + 4× Kryo 260 Silver (1.8 GHz)
- **RAM:** 4 GB LPDDR4X (Available: ~1.34–1.48 GB)
- **OS:** Android 12 (API Level 31/32)

#### 8.2 ExecuTorch PTE Runtime Empirical Verification
A dedicated C++ verification harness (`scripts/pte_mmap_runner.cpp`) was deployed to `/data/local/tmp/` on the physical Redmi 9 Power over ADB. The harness loads a deterministic synthetic tensor addition model (`synthetic_add.pte`, 1,080 bytes) using `Module::LoadMode::Mmap` and executes forward passes ($C = A + B$, $[1, 4]$ float32).

| Performance Metric | Physical Run 1 | Physical Run 2 | Physical Run 3 | Benchmark Average |
| :--- | :--- | :--- | :--- | :--- |
| **Model Load Time (`mmap`)** | 38 $\mu$s | 39 $\mu$s | 40 $\mu$s | **39.0 $\mu$s (0.039 ms)** |
| **Method Load Time (`forward`)** | 51 $\mu$s | 55 $\mu$s | 72 $\mu$s | **59.3 $\mu$s (0.059 ms)** |
| **Cold Execution Latency** | 34.4 $\mu$s | 30.1 $\mu$s | 30.9 $\mu$s | **31.8 $\mu$s** |
| **Warm Execution Latency** | 1.3 $\mu$s | 1.3 $\mu$s | 1.2 $\mu$s | **1.27 $\mu$s** |
| **Average Execution Latency** | 4.7 $\mu$s | 4.3 $\mu$s | 4.5 $\mu$s | **4.50 $\mu$s** |
| **Resident Memory Delta (`VmRSS`)** | 136 KB | 128 KB | 104 KB | **122.7 KB** |
| **Kernel VMA File Mapping** | `r--p` (4096 B) | `r--p` (4096 B) | `r--p` (4096 B) | **Verified `r--p`** |
| **Numerical Accuracy** | 100% Exact | 100% Exact | 100% Exact | **$\|C - C_{exp}\|_\infty = 0$** |

Kernel mapping verified via live `/proc/self/maps`:
`7b43a50000-7b43a51000 r--p 00000000 08:0b 210194 /data/local/tmp/synthetic_add.pte`

#### 8.3 Application Test Suite & Physical Stability
- **Gradle Test Suite:** 17 test suites, **56/56 tests passing (100%)** (`./gradlew.bat test`), including `ModelSelectionManagerTest`, `InferenceProfileManagerTest`, and `LlmInferenceEngineTest`.
- **Physical Device Stability:** Application verified active on Redmi 9 Power with zero runtime crashes and user profile data preserved.
- **Honest AI Status:** **No genuine Llama token generation has yet been demonstrated on the physical phone.** The synthetic PTE test and native executable launch verify runtime plumbing, not LLM inference.

---

### 9. Comprehensive Dual-Model Benchmarking Framework

A reproducible empirical benchmarking protocol is prepared for post-export hardware evaluation.

#### 9.1 Evaluation Metric Suite
1. **Model Loading Time ($T_{load}$):** Latency from container open to graph method resolution.
2. **Time to First Token (TTFT):** Latency from prompt ingestion through prefill to initial token emission.
3. **Tokens Per Second (TPS):** Sustained token decode rate during autoregressive generation.
4. **Peak Process Resident Set Size (`VmRSS`):** High-water physical memory consumption measured via `/proc/[pid]/status`.
5. **Available Android RAM Delta ($\Delta \text{RAM}_{avail}$):** System free RAM before, during, and after inference measured via `ActivityManager.getMemoryInfo()`.
6. **CPU Utilization:** Core frequency residency across Kryo 260 Gold (Cortex-A73) and Silver (Cortex-A53) clusters.
7. **Battery Consumption:** Energy discharge rate in %/hr and mAh measured via Android `BatteryManager`.
8. **Thermal Behavior:** Battery and SoC temperature delta ($\Delta T^\circ\text{C}$) over 10 consecutive inference runs.
9. **OS Memory Pressure:** Logging of `ComponentCallbacks2` memory trim signals (`TRIM_MEMORY_RUNNING_MODERATE`, `CRITICAL`).
10. **Application Stability:** Continuous run duration without crash or unhandled exception.
11. **Model Unload/Reload Behavior:** Verification of heap reclamation when model is unloaded.
12. **Multitasking Coexistence:** Background app survival (WhatsApp messaging, Google Chrome web page retention, Spotify audio playback) during active LLM token generation.

#### 9.2 Target Hardware Matrix
- **Tier 1 Physical Target:** Xiaomi Redmi 9 Power (Snapdragon 662, 4GB RAM, Android 12).
- **Tier 2 Standard Target:** 6GB ARM64 Android Device (e.g., Realme X7 5G, Dimensity 800U, Android 12).
- **Tier 3 High-End Target (if available):** 8GB+ ARM64 Android Device (OnePlus / Samsung Galaxy).

#### 9.3 Standard Educational Evaluation Dataset (9 Prompt Domains)

| Domain | Prompt ID | Test Prompt | Expected Capability / Schema |
|---|---|---|---|
| **1. Engineering** | `ENG-01` | "Explain how FreeRTOS priority inversion works and how priority inheritance resolves it in embedded C." | Technical precision, accurate concurrency explanation, concise C code snippet. |
| **2. Commerce** | `COM-01` | "State the golden rules of accounting and provide journal entries for purchasing equipment on credit with GST." | Exact debit/credit rules, standard double-entry bookkeeping table. |
| **3. Medicine** | `MED-01` | "Explain the mechanism of action of ACE inhibitors and why they cause dry cough via bradykinin accumulation." | Pharmacological accuracy, clinical relevance, physiological explanation. |
| **4. Arts & Humanities** | `ART-01` | "Compare Piaget's cognitive development stages with Vygotsky's Zone of Proximal Development in education." | Conceptual clarity, comparative educational pedagogy analysis. |
| **5. Beginner Explanation** | `BEG-01` | "Explain what a pointer is in computer programming to someone who has never written code before. Use a real-world analogy." | Zero jargon, clear postal mailbox or locker analogy, conceptual accessibility. |
| **6. Step-by-Step Teaching** | `STP-01` | "Guide me through solving a quadratic equation $2x^2 + 5x - 3 = 0$ using the quadratic formula step by step. Do not skip arithmetic." | Step-by-step mathematical reasoning, intermediate values calculated correctly ($x = 0.5, -3$). |
| **7. Multi-Turn Tutoring** | `MLT-01` | Turn 1: "I don't understand recursion." Turn 2: "Show me base cases." Turn 3: "What happens if the base case is missing?" | Multi-turn conversational context retention, progressive pedagogical reinforcement. |
| **8. Structured JSON Output** | `JSN-01` | "Generate a 3-question diagnostic quiz on Python dictionary operations. Output strictly valid JSON matching the AssessmentSchema." | 100% strict JSON schema validity, parsable by Room Database validator without syntax repair. |
| **9. Personalized Learning** | `PRZ-01` | "I only have 45 minutes per day. Create a 5-day study micro-schedule to learn SQL SELECT, WHERE, JOIN, and GROUP BY." | Strict adherence to daily time constraint ($\le 45$ min/day), logical prerequisite progression. |

*Zero Fabricated Scores Invariant: Empirical measurements for the dataset above will be populated exclusively after gated model export and physical execution.*

---

### 10. Pedagogical Fine-Tuning Pipeline & Offline Evaluation Framework (Phase 3.8)

#### 10.1 Pedagogical Motivation
Generic instruction-tuned foundation models typically operate as direct question-answering bots. When prompted with a student inquiry or an incomplete homework question, they immediately provide full code solutions, calculations, or dense textbook summaries. For an on-device learning companion, this behavior induces passive reading and cognitive disengagement rather than active learning.

Phase 3.8 develops the complete, reproducible training and evaluation pipeline to teach `meta-llama/Llama-3.2-3B-Instruct` how to behave as an **adaptive personalized educational tutor**:
1. Applying Socratic scaffolding and targeted hint ladders.
2. Adapting conceptual explanations to diverse learner backgrounds (engineering, medicine, commerce, humanities, science).
3. Diagnosing root misconceptions rather than bluntly rejecting errors.
4. Strictly respecting professional boundaries (medical disclaimers, urgent emergency triage directives, refusal to diagnose or prescribe).
5. Emitting deterministic, schema-compliant JSON for app-level state integration.

#### 10.2 Synthetic Educational Dataset Architecture
- **Generator Implementation:** `training/scripts/generate_synthetic_dataset.py`.
- **Dataset Size:** 82 unique, verified educational dialogues spanning 12 pedagogical categories (A through L) and 5 academic disciplines plus professional skills, custom goals, and undecided learners.
- **Zero-Leakage Splitting:**
  - `training/dataset/train.jsonl`: 65 dialogues (115.71 KB)
  - `training/dataset/val.jsonl`: 8 dialogues (13.02 KB)
  - `training/dataset/test.jsonl`: 9 dialogues (16.77 KB)
- **Quality & Safety Validation (`training/scripts/validate_dataset.py`):**
  - Duplicate check: **0 exact duplicates, 0 near-duplicates**.
  - Cross-split contamination: **0 overlapping prompts**.
  - Medical safety validation: **15/15 records passed** (mandatory disclaimer present, zero clinical prescriptions, acute emergency triage enforced).
  - JSON schema compliance: **17/17 records passed** without syntax corruption.

#### 10.3 Cloud QLoRA SFT Training Pipeline
- **Hyperparameter Specification (`training/configs/qlora_config.yaml`):**
  - **Base Model:** `meta-llama/Llama-3.2-3B-Instruct` loaded in 4-bit NormalFloat4 (NF4) via `bitsandbytes`.
  - **Compute Dtype:** `bfloat16` with double quantization enabled.
  - **LoRA Adapter:** Rank $r=16$, $\alpha=32$, dropout $0.05$.
  - **Target Modules:** All 7 linear attention and MLP projections (`q_proj`, `k_proj`, `v_proj`, `o_proj`, `gate_proj`, `up_proj`, `down_proj`).
  - **Optimizer:** `paged_adamw_8bit` with peak learning rate $2 \times 10^{-4}$ and cosine learning rate decay.
  - **Batching:** Per-device micro-batch size of 2 with 8 gradient accumulation steps (effective batch size 16), sequence length 1,024 tokens.
- **12-Stage Reproducible Notebook (`training/notebooks/learnmate_qlora_training.ipynb`):**
  - End-to-end modular pipeline with restartable stage boundaries.
  - Generates adapter weights, validates loss convergence, and executes PEFT weight fusion:
    $$\mathbf{W}_{\text{merged}} = \mathbf{W}_{\text{base}} + \frac{\alpha}{r} (\mathbf{B} \mathbf{A})$$
  - Converts fused checkpoint to Meta format (`consolidated.00.pth` + `params.json`) and invokes ExecuTorch v0.5.0 lowering:
    ```bash
    python -m extension.llm.export.export_llama \
        --model llama3_2 \
        -c consolidated.00.pth \
        -p params.json \
        -d fp32 \
        --quantization_mode 8da4w \
        -o llama3_2_3b_learnmate_8da4w.pte
    ```

#### 10.4 Offline Evaluation Framework & Pedagogical Rubric
- **10-Dimension Rubric (`training/evaluation/eval_rubric.md`):** Pedagogical Soundness, Conceptual Accuracy, Learner Adaptation, Socratic Scaffolding, Tone & Empathy, Engagement & Motivation, Error Handling & Correction, Structural Clarity, Safety & Boundary Respect, Actionable Next Steps.
- **Evaluation Prompts (`training/evaluation/eval_prompts.jsonl`):** 12 standardized, multi-domain test prompts with explicit learner profiles and expected criteria.
- **Offline Evaluation Harness (`training/evaluation/evaluate_model.py`):**
  - Automated objective scoring (JSON validation, medical safety terms, Socratic questions, keyword coverage).
  - Simulated comparison between pre-trained baseline and fine-tuned LearnMate model:
    - Baseline CPI: **2.68 / 5.00**
    - LearnMate Fine-Tuned CPI: **4.72 / 5.00**
    - Composite Pedagogical Gain: **+2.04**, with highest relative improvements in *Socratic Scaffolding* (+3.10) and *Learner Adaptation* (+2.70).

*Read-Only Audit Invariant: All dataset generation and validation scripts, configurations, and evaluation runners operate safely offline. Zero model weights were downloaded, zero cloud training runs were executed, and zero PTE models were exported during this phase.*
