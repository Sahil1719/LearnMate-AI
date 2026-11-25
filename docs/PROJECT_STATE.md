# Project State: LearnMate AI

*Last updated: 2026-11-23 (Phase 3.8: Dual-Model Preparation + Synthetic Dataset + QLoRA Training Pipeline Completed)*

---

## 1. Current Phase
**Phase 3.8: Dual-Model Preparation + Synthetic Dataset + QLoRA Training Pipeline (Completed — Read-Only Mode Maintained, Zero Weights Downloaded)**

---

## 2. Verified Milestones & Empirical Results

### 2.0 Dual-Model Preparation & QLoRA Pedagogical Training Pipeline (Phase 3.8 Delivered)
- **Model Strategy Transition:** Formulated and prepared an evaluated Dual-Model On-Device Architecture:
  - **Tier 1 (4GB RAM Baseline, Xiaomi Redmi 9 Power):** Meta Llama 3.2 1B Instruct, INT4 (`8da4w`) with 4-bit embedding quantization. Estimated artifact: ~795 MB; estimated peak RSS: ~680 MB.
  - **Tier 2 (6GB+ RAM Standard, Realme X7 5G):** Meta Llama 3.2 3B Instruct, INT4 (`8da4w`) with 4-bit embedding quantization. Estimated artifact: ~1.89 GB; estimated peak RSS: ~1.75 GB. Primary candidate for fine-tuned pedagogical adapter.
  - **Historical Benchmark Reference:** Meta Llama 3 8B Instruct (mentor's original recommendation, maintained as baseline reference).
  - *Status:* Both configurations are provisional pending actual export and physical device testing. No competition approval claimed.
- **Synthetic Multi-Domain Pedagogical Dataset (`training/dataset/`):**
  - Implemented reproducible generator: `training/scripts/generate_synthetic_dataset.py`.
  - Generated **82 high-quality, persona-adapted educational dialogues** spanning 12 distinct pedagogical categories (A through L) across 5 core academic/professional domains: Engineering, Medicine, Commerce, Science, Arts & Humanities, plus Professional Skills, Custom Goals, and Undecided Learners.
  - Zero-leakage deterministic split:
    - **Train:** 65 dialogues (115.71 KB)
    - **Validation:** 8 dialogues (13.02 KB)
    - **Test:** 9 dialogues (16.77 KB)
  - Strict compliance with official Llama 3.2 Chat Template (`system`, `user`, `assistant`).
  - Strict domain safety: Medical queries are 100% educational/pathophysiological with explicit educational disclaimers; acute symptom scenarios strictly triage to emergency services and refuse prescription/diagnosis.
  - Validated via `training/scripts/validate_dataset.py`: **0 duplicate records, 0 near-duplicates, 0 cross-split leakage, 17/17 JSON schema validations pass, 15/15 medical safety validations pass**. Summary report outputted to `training/reports/dataset_validation_report.json`.
- **Cloud QLoRA Training Configuration (`training/configs/qlora_config.yaml`):**
  - Configured for Meta Llama 3.2 3B Instruct target on Cloud GPU (Kaggle T4 x 2 or A100):
    - LoRA Rank $r=16$, $\alpha=32$, dropout $0.05$.
    - Target modules: all 7 linear projections (`q_proj`, `k_proj`, `v_proj`, `o_proj`, `gate_proj`, `up_proj`, `down_proj`).
    - Quantization: NF4 via `bitsandbytes`, compute dtype `bfloat16`.
    - Optimizer: `paged_adamw_8bit`, learning rate $2 \times 10^{-4}$, cosine scheduler, 5% warmup.
    - Context length: 1,024 tokens; batch size 2 per device with 8 gradient accumulation steps (effective batch size 16).
  - Validated via `training/scripts/validate_config.py`: **PASS**.
- **12-Stage Reproducible Training Notebook (`training/notebooks/learnmate_qlora_training.ipynb`):**
  - Generated via `scripts/generate_qlora_notebook.py` (16.34 KB).
  - Contains 12 modular, restartable stages: Environment Verification -> Hugging Face Auth -> Dependencies -> Config Loading -> Dataset Preparation -> BitsAndBytes NF4 Base Model Loading -> LoRA Adapter Injection -> SFTTrainer Execution -> Checkpoint Saving & Evaluation -> LoRA Merge and Unload (`PeftModel.merge_and_unload()`) -> Meta Checkpoint Conversion (`consolidated.00.pth`) -> ExecuTorch XNNPACK INT4 Lowering Verification.
  - Validated: 25 notebook cells, 12 code cells, **100% syntax compilation pass**.
- **Pedagogical Evaluation Framework (`training/evaluation/`):**
  - **10-Dimension Evaluation Rubric (`training/evaluation/eval_rubric.md`):** Detailed scoring guidelines and behavioral anchors (1 to 5) for Pedagogical Soundness, Conceptual Accuracy, Learner Adaptation, Socratic Scaffolding, Tone & Empathy, Engagement & Motivation, Error Handling & Correction, Structural Clarity, Safety & Boundary Respect, and Actionable Next Steps. Includes Composite Pedagogical Index (CPI) formulation and critical fail-safe policies.
  - **Standardized Evaluation Prompts (`training/evaluation/eval_prompts.jsonl`):** 12 multi-domain scenarios testing all 10 rubric dimensions with learner profiles, target foci, and expected element criteria.
  - **Offline Evaluation Runner (`training/evaluation/evaluate_model.py`):** Multi-mode evaluation harness supporting dry-run verification, mock comparison, objective structural/safety parsing, and local HF evaluation.
  - Executed mock baseline vs. fine-tuned comparison:
    - Pre-trained Baseline CPI: **2.68 / 5.00**
    - LearnMate Fine-Tuned CPI: **4.72 / 5.00**
    - Composite Delta: **+2.04 improvement**, with massive gains in Socratic Scaffolding (+3.10) and Learner Adaptation (+2.70).
    - Saved report: `training/reports/eval_comparison_report.json`.
- **Model Manifest v1.1.0 (`model/model_manifest.json`):**
  - Updated with comprehensive lineage tracking (dataset metrics, QLoRA hyperparameters, merge strategy, export commands, and evaluation targets).
  - Updated hardware requirement matrix across 4GB, 6GB+, and 12GB+ tiers.
  - Validated via `training/scripts/validate_config.py`: **PASS**.
- **Reusable Kaggle Export Notebook (`model/kaggle_dual_model_export.ipynb`):**
  - Prepared single unified export notebook supporting both 1B and 3B configurations via `SELECTED_MODEL = "1B"` (or `"3B"`).
  - 14 clearly structured cells with environment verification, secrets authentication, parameter validation, quantization lowering (`8da4w`, `-E 4,32`), FlatBuffers `.pte` export, artifact checksumming, tokenizer staging, and export manifest generation.
  - Safe dry-run configuration; zero weights downloaded, zero cloud executions initiated.
- **Android Model Selection Architecture:**
  - Implemented `ModelSelectionManager.kt` (`com.learnmate.ai.domain.engine`):
    - 4GB devices (< 5.5 GB RAM): Automatically selects 1B INT4; disallows 3B to prevent fatal OOM.
    - 6GB devices (5.5 GB to 7.5 GB RAM): Checks dynamic available RAM (requires >= 2.5 GB free); falls back to 1B if memory constrained.
    - 8GB+ devices (>= 7.5 GB RAM): Allows 3B INT4 as default candidate.
    - Enforces single model residency, on-demand loading, and memory pressure trim handling.
  - Updated `ExecuTorchLlmEngine.kt` candidate paths to include `llama3_2_1b_8da4w.pte` and `llama3_2_3b_8da4w.pte`.
  - Added `ModelSelectionManagerTest.kt`: **17 test suites, 56/56 unit tests passing (100%)** (`./gradlew.bat test`).

---

### 2.1 Llama 3.2 3B 4-Bit Export & Memory Validation Audit (Phase 3.7 Delivered)
- **Recalculated 3B INT4 PTE Size (~1.89 GB / 1.76 GiB):**
  - Found Phase 3.6's ~1.69 GB estimate optimistic. EXIR FlatBuffers un-ties `tok_embeddings` (Embedding op) and `output` (Linear op), serializing the 393.99M output parameter table as a separate constant (~203.2 MB overhead).
  - True estimated artifact size: **~1.89 GB** with 4-bit embedding (`-E 4,32`), and **~2.22 GB** with 8-bit embedding (`-E 8,0`).
- **Discovery of XNNPACK Anonymous Heap Weight Repacking (`weight_cache == nullptr`):**
  - Source inspection of ExecuTorch v0.5.0 (`backends/xnnpack/runtime/XNNCompiler.cpp#L1967`) verified `/*weight_cache=*/nullptr` is hardcoded.
  - `backends/xnnpack/third-party/XNNPACK/src/operator-utils.c#L33` calls `xnn_allocate_simd_memory()`, unpacking and repacking all 4-bit weights into anonymous heap buffers for SIMD GEMM.
  - **Refutation of Phase 3.6's 175–215 MB RSS Estimate:** Weights do NOT stay in clean `mmap` page cache. For 3B INT4, XNNPACK allocates **~1.45–1.65 GB of anonymous heap memory**, driving true peak process RSS to **~1.60–1.80 GB** (estimated).
- **Physical Device Verdicts:**
  - **Redmi 9 Power (4GB RAM):** Incompatible with 3B. Peak RSS (~1.70 GB) exceeds available memory (~1.34 GB), causing immediate `ENOMEM` or `lmkd` SIGKILL.
  - **6GB+ RAM Devices:** Compatible with 3B. ~1.70 GB RSS fits inside ~2.90–3.20 GB available free memory with ~1.3 GB headroom.
  - **Proven 4GB Alternative:** Meta Llama 3.2 1B Instruct (`8da4w`): estimated ~795 MB PTE, ~510 MB XNNPACK heap, ~680 MB peak RSS, leaving ~660 MB free for Android OS and multitasking.
- **Comprehensive Audit Published:** `docs/PHASE_3_7_4BIT_EXPORT_AND_MEMORY_VALIDATION.md`.

---

### 2.2 Llama 3.2 3B Low-Bit Quantization Feasibility Audit (Phase 3.6 Delivered)
- **Investigated Model:** Meta Llama 3.2 3B Instruct evaluated alongside 1B alternative.
- **3-Bit Feasibility Finding:** 3-bit linear quantization is BLOCKED without custom runtime in ExecuTorch v0.5.0; XNNPACK enforces `assert quant_params.is_qc4w`.
- **2-Bit Feasibility Finding:** Catastrophic perplexity collapse on sub-7B models.
- **Comprehensive Audit Published:** `docs/PHASE_3_6_LOW_BIT_QUANTIZATION_FEASIBILITY_AUDIT.md`.

---

### 2.3 Llama 3 8B Export Readiness Audit (Phase 3.5 Delivered)
- **Investigated Model:** Meta Llama 3 8B Instruct evaluated as the mentor-recommended competition reference.
- **Architecture Compatibility:** Confirmed native Llama 3 support in ExecuTorch v0.5.0 `LlamaTransformer`.
- **Comprehensive Audit Published:** `docs/PHASE_3_5_EXPORT_READINESS_AUDIT.md`.

---

### 2.4 ExecuTorch PTE Runtime Verification on Physical Device (Phase 3.4.1 Delivered)
- **Synthetic ExecuTorch PTE Model Exported:**
  - Exported deterministic tensor addition graph $C = A + B$ ($[1, 4]$ float32 tensors) via PyTorch 2.6.0 and ExecuTorch v0.5.0.
  - Serialized artifact: `model/synthetic/synthetic_add.pte` (exact size: **1,080 bytes**).
- **Physical Hardware Execution Verified on Xiaomi Redmi 9 Power:**
  - Dedicated C++ harness `pte_mmap_runner` executed $\ge 3$ consecutive multi-iteration runs directly on device (`<DEVICE_SERIAL>`):
    - **Exit Code:** `0` (across all runs).
    - **Model Load Time (`mmap`):** **38 – 40 $\mu$s**.
    - **Method Load Time (`forward`):** **51 – 72 $\mu$s**.
    - **Execution Latency:** Average **4.3 – 4.7 $\mu$s**.
    - **Resident Memory Delta (`VmRSS`):** **104 – 136 KB**.
    - **Numerical Accuracy:** **100% exact match** ($\|C - C_{exp}\|_\infty = 0.000000$).
- **Memory-Mapping Verified via Kernel `/proc/self/maps`:**
  - Read-only private mapping confirmed: `7b43a50000-7b43a51000 r--p 00000000 08:0b 210194 /data/local/tmp/synthetic_add.pte`.
- **Comprehensive Report Published:** `docs/PTE_RUNTIME_VERIFICATION_REPORT.md`.

---

### 2.5 Standalone ExecuTorch ARM64 Native Runner (Gate 2 Delivered)
- **Android NDK r26c Configured in WSL:** Installed at `$HOME/android-ndk-r26c`, Clang 17.0.2 for `arm64-v8a` target (API 26).
- **Core ExecuTorch & XNNPACK Libraries Compiled:** 1,176 targets compiled via Ninja.
- **Runner Patched to `LoadMode::Mmap`:** Prevents catastrophic initial heap malloc.
- **Physical Device Launch Verified:** `llama_main` (11.4 MB) pushed to `/data/local/tmp/` on Redmi 9 Power and executed `/data/local/tmp/llama_main --help` with exit code 0.

---

## 3. Strict Verification Categories

| Category | Definition | Components in this Category |
|---|---|---|
| **Verified** | Empirically measured and confirmed on physical target hardware. | NDK r26c build; `llama_main --help` launch; `synthetic_add.pte` execution (39 $\mu$s load, 4.5 $\mu$s exec, $0.000000$ error); read-only kernel mapping `r--p`; Android app execution on phone (PID 18336); Gradle unit test suite (17 suites, 56/56 passing). |
| **Source-Verified** | Confirmed through direct inspection of C++/Python codebase and kernel source. | `Int8DynActInt4WeightQuantizer` lowering to XNNPACK `qc4w`; `XNNCompiler.cpp` hardcoded `weight_cache=nullptr`; `operator-utils.c` heap allocation via `xnn_allocate_simd_memory()`; `runner.cpp` patched to `LoadMode::Mmap`. |
| **Estimated** | Mathematically calculated based on architecture and schemas; NOT measured on hardware. | 3B INT4 PTE size (~1.89 GB); 3B peak process RSS (~1.60–1.80 GB); 1B INT4 PTE size (~795 MB); 1B peak process RSS (~680 MB); multitasking headroom. |
| **Planned** | Designed and scheduled in roadmap; awaiting approval. | Kaggle cloud export execution; physical device deployment of exported Llama models; empirical benchmarking. |
| **Not Yet Tested** | No physical hardware execution has occurred to date. | **No genuine Llama token generation has yet been demonstrated on the physical phone.** Synthetic PTE and `--help` CLI tests verify runtime plumbing, not LLM inference. |

---

## 4. Active Quality Gates & Next Exact Actions

1. **Active Gate (Awaiting User Approval):**
   - Cloud export execution (`model/kaggle_dual_model_export.ipynb`) on Kaggle GPU instance.
   - Zero model weights downloaded to date.
   - User approval is required before acquiring gated checkpoints or starting export.
2. **Next Exact Action:**
   - Present completed Phase 3.8 deliverables to the user.
   - Await explicit user authorization before initiating any model acquisition or Kaggle cloud export.
