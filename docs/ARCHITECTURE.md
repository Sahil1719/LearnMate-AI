# System Architecture: LearnMate AI

```
+---------------------------------------------------------------------------------------------------+
|                                        LEARNMATE AI CLIENT                                        |
|                     (Android 12+, Jetpack Compose, M3, Minimum Target: 4GB RAM)                   |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|  [PRESENTATION TIER]                                                                              |
|  +--------------------+  +--------------------+  +--------------------+                           |
|  |  Onboarding Screen |  |  Dashboard Screen  |  | Skill Analysis UI  |                           |
|  +--------------------+  +--------------------+  +--------------------+                           |
|  +--------------------+  +--------------------+  +--------------------+                           |
|  |  Roadmap View      |  |  Study Planner     |  | AI Tutor Chat      |                           |
|  +--------------------+  +--------------------+  +--------------------+                           |
|                                                                                                   |
|  [CORE LOGIC & HYBRID INTELLIGENCE TIER]                                                           |
|  +---------------------------------------------------------------------------------------------+  |
|  |                               Deterministic Skill Gap Engine                                |  |
|  |                    Gap = max(0, TargetCompetency[track, skill] - CurrentCompetency)         |  |
|  +---------------------------------------------------------------------------------------------+  |
|                                                |                                                  |
|                                                v                                                  |
|  +---------------------------------------------------------------------------------------------+  |
|  |                             Structured Career Knowledge Base (Local Room DB)                |  |
|  |                 7 Career Pathways | 50+ Verified Skills | Prerequisite Graphs               |  |
|  +---------------------------------------------------------------------------------------------+  |
|                                                |                                                  |
|                                                v                                                  |
|  +---------------------------------------------------------------------------------------------+  |
|  |                          Adaptive Orchestrator & Strict JSON Schema Validator               |  |
|  +---------------------------------------------------------------------------------------------+  |
|               |                                                |                                  |
|               v (Prompt Contracts)                             v (Validated Records)              |
|  [MODEL SELECTION & RUNTIME TIER]                [DATA STORAGE TIER]                              |
|  +------------------------------------+          +---------------------------------------------+  |
|  | ModelSelectionManager              |          | Room Database (SQLite)                      |  |
|  | - Evaluates RAM & OS Pressure      |          | - LearnerProfileEntity                      |  |
|  | - Single Model Residency           |          | - SkillEntity & GapEntity                   |  |
|  | - On-Demand Load / Unload Lifecycle|          | - RoadmapPhaseEntity                        |  |
|  +------------------------------------+          | - StudyTaskEntity                           |  |
|                   |                              | - AssessmentRecordEntity                    |  |
|                   v                              +---------------------------------------------+  |
|  +------------------------------------+                                                           |
|  | ExecuTorch C++ / JNI Runtime       |                                                           |
|  | - Backend: XNNPACK (ARM NEON)      |                                                           |
|  | - Tier 1: Llama 3.2 1B (4GB RAM)   |                                                           |
|  | - Tier 2: Llama 3.2 3B (6GB+ RAM)  |                                                           |
|  | - Reference: Llama 3 8B Benchmark  |                                                           |
|  | - Quantization: TorchAO 8da4w      |                                                           |
|  | - XNNPACK Anonymous Heap Repack    |                                                           |
|  +------------------------------------+                                                           |
+---------------------------------------------------------------------------------------------------+
```

---

## 1. Architectural Components & Hardware Boundary

### 1.1 Hardware Deployment Classes
- **Experimental Minimum-Hardware Baseline (Redmi 9 Power):**
  - **SoC:** Qualcomm Snapdragon 662 (`SM6115` / `lime` / `bengal`)
  - **CPU:** 4× Kryo 260 Gold (Cortex-A73 @ 2.0 GHz) + 4× Kryo 260 Silver (Cortex-A53 @ 1.8 GHz)
  - **Vector Acceleration:** ARMv8.0-A baseline NEON 128-bit SIMD.
  - **Physical RAM:** 3.80 GB total (3,713 MB detected), ~1.34–1.48 GB available to user processes.
  - **Storage:** 26 GB free on internal flash storage (`/data`).
- **Standard ARM64 Android Target (6GB+ RAM Devices):**
  - **Reference Devices:** Realme X7 5G (Dimensity 800U, 6GB RAM), OnePlus, Samsung Galaxy.
  - **Physical RAM:** >= 6.0 GB total, ~2.90–3.20 GB available free RAM.

---

### 1.2 Model Memory Hierarchy & XNNPACK Heap Repacking Mechanics

#### 1.2.1 Source Verification of XNNPACK Weight Repacking (`weight_cache == nullptr`)
In ExecuTorch v0.5.0, `backends/xnnpack/runtime/XNNCompiler.cpp#L1967` hardcodes:
```cpp
/*weight_cache=*/nullptr, // TODO - support weight cache
```
When `weights_cache` is `NULL`, `backends/xnnpack/third-party/XNNPACK/src/operator-utils.c#L33` executes:
```c
op->packed_weights.pointer = xnn_allocate_simd_memory(aligned_weights_size);
```
During initialization (`create_fully_connected_nc_qd8_f32_qc4w`), XNNPACK un-packs, transposes, and repacks all 4-bit linear weights into architecture-specific SIMD memory buffers (`posix_memalign`).
- **Critical Memory Consequence:** These packed weights reside permanently in **anonymous dirty heap memory**. They are **NOT** demand-paged zero-copy from flash via `mmap`. Android's kernel cannot page them out under memory pressure without swapping (ZRAM).

#### 1.2.2 Refutation of Prior Phase 3.6 175–215 MB RSS Estimate
Phase 3.6 estimated peak process RSS at 175–215 MB by mistakenly assuming all weights remained in clean `mmap` page cache. Phase 3.7 source verification definitively refuted this:
- **Llama 3.2 3B INT4:** Requires ~1.45–1.65 GB of anonymous heap allocation for packed weights alone. Adding runtime arena and base process overhead brings true peak resident set size (`VmRSS`) to **~1.60–1.80 GB** (estimated).
- **Llama 3.2 1B INT4:** Requires ~510 MB of heap allocation for packed weights. True peak `VmRSS` is **~650–720 MB** (estimated).
- **Llama 3 8B INT4:** Requires ~3.80 GB heap allocation, exceeding total physical RAM on 4GB devices.

---

### 1.3 Dual-Model Deployment Strategy & Rationale
The original mentor recommendation was Meta Llama 3 8B Instruct (a recommendation, not a confirmed competition requirement). Technical audits established that 8B and 3B models exceed the available memory envelope of 4GB devices.

| Device Class | Assigned Model | Quantisation | Estimated PTE Size | Estimated Peak RSS | Multitasking Feasibility |
|---|---|---|---|---|---|
| **4GB RAM Baseline (Redmi 9 Power)** | **Meta Llama 3.2 1B Instruct** | `8da4w` (INT4 weight, INT8 dynamic act) | ~795 MB (0.74 GiB) | ~680 MB | **Feasible:** Leaves ~660 MB free RAM for Android OS, WhatsApp, and audio. |
| **6GB+ RAM Standard (Realme X7 5G)** | **Meta Llama 3.2 3B Instruct** | `8da4w` (INT4 weight, INT8 dynamic act) | ~1.89 GB (1.76 GiB) | ~1.75 GB | **Feasible:** Leaves ~1.20–1.45 GB free RAM for background apps. |
| **Historical Reference / Benchmark** | **Meta Llama 3 8B Instruct** | `8da4w` (INT4 weight, INT8 dynamic act) | ~4.27 GB (3.98 GiB) | ~4.05 GB | **Fatal on 4GB:** Exceeds total RAM; triggers instant `lmkd` SIGKILL. |

*Note: Both dual-model configurations remain provisional pending actual export and physical device benchmarking. No competition approval has been claimed.*

---

### 1.4 Android Model Selection Architecture (`ModelSelectionManager`)
The Android application incorporates a dedicated `ModelSelectionManager` (`com.learnmate.ai.domain.engine.ModelSelectionManager`) with the following behavioral invariants:

```
                          [DeviceMemorySnapshot]
                                    │
                                    ▼
                     Total Physical RAM < 5.5 GB?
                     ├── YES ──► Select Llama 3.2 1B INT4
                     │           (Disallow 3B/8B; prevent lmkd crash)
                     └── NO
                          │
                          ▼
                     Total Physical RAM < 7.5 GB? (6GB Class)
                     ├── YES ──► Available Free RAM >= 2.5 GB?
                     │           ├── YES ──► Permit Llama 3.2 3B INT4
                     │           └── NO  ──► Fallback to Llama 3.2 1B INT4 (Low free RAM warning)
                     └── NO  ──► (8GB+ Class) Permit Llama 3.2 3B INT4 Default
```

#### Core Operational Invariants:
1. **Dynamic Memory Inspection:** Does not rely solely on total RAM; inspects `ActivityManager.MemoryInfo.availMem` and `lowMemory` flag before permitting 3B load.
2. **Single Model Residency:** Enforces that only one model is loaded in memory at any time. Previous models are explicitly released (`module_->unload_method()`, JNI release) before initiating another load.
3. **On-Demand Loading:** Models are loaded on demand when the learner enters an AI-assisted session, and unloaded when inactive or during critical memory trimming (`TRIM_MEMORY_RUNNING_CRITICAL`).
4. **Independent Conversation Persistence:** Learner chat history and assessment states are persisted directly in Room Database (`AssessmentRecordEntity`, `StudyTaskEntity`), completely independent of native model memory residency.
5. **Generation Cancellation:** Active coroutine token streams can be cancelled immediately via Kotlin Job cancellation or UI user intervention without corrupting native runtime state.

---

### 1.5 Verification Categories & Empirical Grounding

| Milestone / Metric | Status Category | Empirical Reality |
|---|---|---|
| **ExecuTorch Native ARM64 Runner** | **Verified** | `llama_main` (11.4 MB) executed on physical Redmi 9 Power via ADB (`--help` code 0). |
| **PTE Mmap Runtime Harness** | **Verified** | `synthetic_add.pte` (1,080 B) executed on Redmi 9 Power. Read-only kernel mapping (`r--p`) verified in `/proc/self/maps`. |
| **Android Client App & Room DB** | **Verified** | Active on Redmi 9 Power (17.22 MB debug APK). 17 test suites, 56/56 unit tests passing (100%). |
| **ExecuTorch v0.5.0 XNNPACK Lowering** | **Source-Verified** | `Int8DynActInt4WeightQuantizer` lowers to XNNPACK `qc4w`. `XNNCompiler.cpp` `weight_cache=nullptr` confirmed. |
| **3B INT4 PTE Size (~1.89 GB)** | **Estimated** | Mathematical calculation accounting for un-tied embedding and linear output tables in EXIR FlatBuffers. |
| **3B Peak Process RSS (~1.60–1.80 GB)** | **Estimated** | Mathematical calculation of XNNPACK heap weight repacking (~1.55 GB) + arena + base process. |
| **1B Memory & Multitasking (~680 MB RSS)** | **Estimated** | Mathematical calculation of 1B weight repacking (~510 MB heap) fitting in Redmi 9 Power free RAM. |
| **Genuine Llama Token Generation** | **Not Yet Tested** | **No genuine Llama token generation has yet been demonstrated on the physical phone.** Synthetic PTE and `--help` CLI tests verify runtime plumbing, not LLM inference. |
| **Model Weight Acquisition & Export** | **Planned / Gated** | Zero model weights downloaded to date. Reusable Kaggle export notebook prepared; awaiting authorization. |

---

## 2. Pedagogical Fine-Tuning & Offline Evaluation Architecture (Phase 3.8)

```
+---------------------------------------------------------------------------------------------------+
|                                  OFFLINE PEDAGOGICAL TRAINING & EVALUATION PIPELINE               |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|  [DATASET GENERATION & VALIDATION TIER]                                                           |
|  +-------------------------------------+         +---------------------------------------------+  |
|  | generate_synthetic_dataset.py        |         | validate_dataset.py                         |  |
|  | - 82 Multi-Domain Educational Records| ------> | - Zero Duplicate Records (0 exact, 0 near)  |  |
|  | - 12 Pedagogical Categories (A to L)|         | - Zero Cross-Split Leakage                  |  |
|  | - 5 Subjects + Undecided + Custom   |         | - Strict Llama 3.2 Chat Template Formatting |  |
|  +-------------------------------------+         | - Medical Safety & Triage Boundary Passed   |  |
|                    |                             +---------------------------------------------+  |
|                    v                                                                              |
|  [DATASET PARTITIONING: ZERO PROMPT LEAKAGE]                                                      |
|  +---------------------------+   +--------------------------+   +------------------------------+  |
|  | train.jsonl (65 dialogues)|   | val.jsonl (8 dialogues)  |   | test.jsonl (9 dialogues)     |  |
|  | 115.71 KB                 |   | 13.02 KB                 |   | 16.77 KB                     |  |
|  +---------------------------+   +--------------------------+   +------------------------------+  |
|                                                |                                                  |
|                                                v                                                  |
|  [CLOUD QLoRA SFT TRAINING TIER (Kaggle T4 x 2 / A100)]                                           |
|  +---------------------------------------------------------------------------------------------+  |
|  | learnmate_qlora_training.ipynb (12 Modular Restartable Stages)                              |  |
|  | - Base Model: meta-llama/Llama-3.2-3B-Instruct (NF4 4-bit via bitsandbytes)                 |  |
|  | - LoRA Configuration: r=16, alpha=32, dropout=0.05, 7 Linear Target Modules                   |  |
|  | - Optimization: paged_adamw_8bit, LR 2e-4, Cosine Decay, Batch Size 2 x 8 = 16 Accumulation    |  |
|  | - Context Window: 1024 tokens                                                               |  |
|  +---------------------------------------------------------------------------------------------+  |
|                                                |                                                  |
|                                                v                                                  |
|  [ADAPTER MERGING & EXECUTORCH LOWERING TIER]                                                     |
|  +---------------------------------------------------------------------------------------------+  |
|  | 1. PeftModel.merge_and_unload() -> Merged Full BF16 Safetensors Checkpoint                  |  |
|  | 2. Convert to Meta Format -> consolidated.00.pth + params.json                              |  |
|  | 3. ExecuTorch v0.5.0 export_llama.py --model llama3_2 --quantization_mode 8da4w             |  |
|  |    -> Lowers to XNNPACK ARM NEON qc4w microkernels                                          |  |
|  |    -> Outputs final on-device runtime artifact: llama3_2_3b_learnmate_8da4w.pte             |  |
|  +---------------------------------------------------------------------------------------------+  |
|                                                |                                                  |
|                                                v                                                  |
|  [OFFLINE PEDAGOGICAL EVALUATION TIER]                                                            |
|  +-------------------------------------+         +---------------------------------------------+  |
|  | Standardized Test Prompts           |         | 10-Dimension Evaluation Rubric              |  |
|  | - eval_prompts.jsonl (12 Scenarios) | ------> | - eval_rubric.md (Scale 1 to 5 per dim)     |  |
|  | - Structured JSON, Socratic, Safety |         | - Composite Pedagogical Index (CPI >= 4.0)  |  |
|  +-------------------------------------+         +---------------------------------------------+  |
|                    │                                                    │                         |
|                    └──────────────────────────┬─────────────────────────┘                         |
|                                               ▼                                                   |
|                                +-----------------------------+                                    |
|                                | evaluate_model.py           |                                    |
|                                | - Dry-run verification      |                                    |
|                                | - Automated JSON & Safety   |                                    |
|                                | - Mock Comparison (Report): |                                    |
|                                |   Baseline: 2.68 CPI        |                                    |
|                                |   LearnMate: 4.72 CPI (+2.04)|                                   |
|                                +-----------------------------+                                    |
+---------------------------------------------------------------------------------------------------+
```

### 2.1 The 12 Pedagogical Dataset Categories
The synthetic training dataset addresses the limitations of standard conversational chatbots by explicitly teaching personalized pedagogical tutoring:
- **A. Beginner to Advanced:** Multi-level explanations progressing from intuitive analogy to formal mechanics.
- **B. Socratic Dialogue:** Guiding questions that elicit learner discovery rather than premature answer delivery.
- **C. Step-by-Step Problem Solving:** Structured analytical breakdowns with clear intermediate milestones.
- **D. Analogy & Real-World Framing:** Domain-grounded physical or situational models.
- **E. Multi-Turn Progression:** Coherent context retention and progressive challenge advancement.
- **F. Misconception Correction:** Empathetic diagnosis and resolution of common cognitive traps.
- **G. Practice & Retention:** Micro-quizzes and practice exercises for immediate self-testing.
- **H. Persona Adaptation:** Customized explanations matching learner background, pace, and domain.
- **I. Study Planning & Meta-Learning:** Time-boxed milestone roadmaps and learning strategy guidance.
- **J. Structured JSON Output:** Strict schema compliance for programmatic app integration.
- **K. Domain Safety Boundary:** Medical and legal disclaimers, triage directives, and clinical refusal.
- **L. Socratic Scaffolding:** Hint ladders and targeted probes rather than direct code/formula leakage.
