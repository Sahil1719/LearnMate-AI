# Development Roadmap: LearnMate AI
**Primary Hardware Benchmark Target:** Xiaomi Redmi 9 Power (Qualcomm Snapdragon 662, 4GB RAM, Android 12)  
**Standard Deployment Target:** ARM64 Android devices with >= 6GB RAM (e.g. Realme X7 5G)  
**Baseline Plan vs. Verified Progress:** Empirical results supersede estimates.

---

## Timeline & Milestone Breakdown

### Days 1–2: Environment Audit, Hardware Target & Model Feasibility (Completed)
- [x] Comprehensive audit of Windows 11 host (CPU, RAM, GPU, free SSD space).
- [x] Comprehensive audit of Ubuntu 24.04 WSL2 (compilers, CMake, Ninja, Python 3.12).
- [x] Target device established: Xiaomi Redmi 9 Power (`SM6115`, 4GB RAM, Android 12).
- [x] Establish isolated WSL Python virtual environment (`~/learnmate-env`).
- [x] Clone and inspect ExecuTorch v0.5.0 (`~/executorch`).
- [x] Verify exact PyTorch (2.6.0) & TorchAO (0.8.0) dependency requirements.
- [x] Authenticate Hugging Face credentials (`1sahil7`) and verify Meta Llama repo access.
- [x] Conduct initial 8B model feasibility on 4GB RAM: identify `mmap` requirement and contingency planning.

### Days 3–4: App Scaffolding, Product Redesign & Context-Aware Onboarding (Completed)
- [x] Scaffold Android app with Jetpack Compose, Material 3, and Room Database.
- [x] Implement deterministic Skill Gap engine ($Gap = \max(0, Target - Current)$).
- [x] Redesign onboarding flow: eliminate assumed defaults.
- [x] Implement 6-step context-aware sequential onboarding (`EducationalTaxonomy.kt`, `OnboardingFlowController.kt`).
- [x] Implement "Help Me Decide" guided discovery mode for undecided learners.
- [x] Build pure-Kotlin JSON schema validator (`DynamicOnboardingSchema.kt`).
- [x] Pass 100% acceptance tests (38/38 unit tests across 14 suites).

### Day 5: Physical Validation, Inference Infrastructure & Native Toolchains (Completed)
- [x] Deploy and stream APK directly to connected Redmi 9 Power via ADB (`adb install -r`).
- [x] Empirically validate all 7 learner journeys on physical device (Commerce, Medical, Engineering, BCA, Arts, Custom Python, Undecided).
- [x] Capture and verify 15 physical device screenshots at native $1080 \times 2340$ resolution (`docs/screenshots/`).
- [x] Gate 2 Delivered: Install Android NDK r26c in WSL, compile 1,176 ExecuTorch and XNNPACK ARM64 targets, patch `runner.cpp` to `LoadMode::Mmap`, build standalone `llama_main` (11.4 MB), and verify on Redmi 9 Power via ADB (`/data/local/tmp/llama_main --help` code 0).
- [x] Phase 3.4.1 Delivered: Export synthetic PTE model (`synthetic_add.pte`, 1,080 bytes) and execute on Redmi 9 Power via dedicated C++ harness `pte_mmap_runner`. Verify read-only private memory mapping (`r--p`) in `/proc/self/maps`, 39 $\mu$s load time, 4.5 $\mu$s execution, and 100% numerical match.

### Audits & Model Strategy Evolution (Phases 3.5, 3.6, 3.7, 3.8 - Completed)
- [x] **Phase 3.5 (Llama 3 8B Export Readiness):** Confirmed GQA and TorchAO `8da4w` support in ExecuTorch v0.5.0. Staged verified fixes in export scripts. Published `docs/PHASE_3_5_EXPORT_READINESS_AUDIT.md`.
- [x] **Phase 3.6 (Low-Bit Quantization Feasibility):** Proved 3-bit linear quantization is blocked without custom microkernels in v0.5.0; 2-bit quantization suffers perplexity collapse. Evaluated 4-bit alternatives. Published `docs/PHASE_3_6_LOW_BIT_QUANTIZATION_FEASIBILITY_AUDIT.md`.
- [x] **Phase 3.7 (4-Bit Export & Memory Validation):** Discovered XNNPACK heap weight repacking (`XNNCompiler.cpp#L1967` hardcodes `weight_cache=nullptr`); refuted Phase 3.6's 175–215 MB estimate; proved 3B peak RSS is ~1.60–1.80 GB (fatal on 4GB Redmi 9 Power; feasible on 6GB+); validated Llama 3.2 1B INT4 as 4GB alternative (~680 MB peak RSS). Published `docs/PHASE_3_7_4BIT_EXPORT_AND_MEMORY_VALIDATION.md`.
- [x] **Phase 3.8 (Dual-Model Preparation + Synthetic Dataset + QLoRA Training Pipeline):**
  - Updated model strategy to provisional Dual-Model Architecture: Llama 3.2 1B for 4GB RAM (Redmi 9 Power) and Llama 3.2 3B for 6GB+ RAM; preserved historical 8B benchmark reference.
  - Implemented multi-domain synthetic dataset generator `training/scripts/generate_synthetic_dataset.py`, generating 82 high-quality dialogues across 12 pedagogical categories (A-L) and 5 major subjects with strict Llama 3.2 chat template formatting and medical safety boundaries.
  - Partitioned dataset with zero prompt leakage: 65 train (115.71 KB), 8 val (13.02 KB), 9 test (16.77 KB).
  - Implemented and verified comprehensive validation suite `training/scripts/validate_dataset.py`: 0 duplicates, 0 leakage, 17/17 JSON schema pass, 15/15 medical safety pass.
  - Configured cloud GPU QLoRA fine-tuning parameters (`training/configs/qlora_config.yaml`, $r=16$, $\alpha=32$, NF4, 1024 context).
  - Built 12-stage reproducible training notebook `training/notebooks/learnmate_qlora_training.ipynb` covering end-to-end flow from Hugging Face authentication to `PeftModel.merge_and_unload()` and ExecuTorch XNNPACK INT4 lowering verification (100% syntax compilation pass).
  - Developed 10-dimension pedagogical evaluation rubric (`training/evaluation/eval_rubric.md`), standardized 12-prompt evaluation suite (`training/evaluation/eval_prompts.jsonl`), and offline evaluation harness (`training/evaluation/evaluate_model.py`) with simulated baseline (2.68 CPI) vs. fine-tuned (4.72 CPI) comparison.
  - Updated model manifest `model/model_manifest.json` (v1.1.0) with full lineage tracking and 3-tier hardware requirement matrix.
  - Implemented Android `ModelSelectionManager.kt` with dynamic available RAM checks, single-model residency, on-demand loading, and memory pressure trim handling.
  - Verified 17 test suites, 56/56 unit tests passing (100%).
  - Prepared reusable Kaggle dual-model export notebook `model/kaggle_dual_model_export.ipynb` supporting both 1B and 3B configurations with 14 structured safety cells.

---

### Upcoming Milestones (Phase 4: Cloud Export & Hardware Benchmarking)
- [ ] User review and authorization for Kaggle export notebook execution.
- [ ] Execute `kaggle_dual_model_export.ipynb` in Kaggle cloud environment:
  - Configuration A: `meta-llama/Llama-3.2-1B-Instruct` INT4 (`8da4w`) -> `llama3_2_1b_8da4w.pte`.
  - Configuration B: `meta-llama/Llama-3.2-3B-Instruct` INT4 (`8da4w`) -> `llama3_2_3b_8da4w.pte`.
- [ ] Deploy exported `.pte` artifacts and tokenizers to Redmi 9 Power and 6GB target device.
- [ ] Execute empirical benchmarks across 9 educational domains and record exact hardware metrics (TTFT, TPS, `VmRSS`, CPU, thermal, battery, multitasking).
- [ ] End-to-end integration with Android AI Tutor screen and verified offline execution under Airplane Mode.
- [ ] Package signed release APK and record physical demonstration video.
