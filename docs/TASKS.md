# Task Tracker: LearnMate AI

*Owner designation: [AI-Team: Role] or [Human: Member 1 / Member 2]*

---

## Sprint 1: Environment Audit & Setup (Day 1 - Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **ENV-001** | Inspect Windows 11 host resources (RAM, GPU, SSD) | AI-Team: Deployment | Done | 16GB RAM, RTX 3050 (6GB), 266.73 GB free SSD verified |
| **ENV-002** | Audit WSL2 Ubuntu 24.04 toolchains (GCC, Clang, CMake, Ninja) | AI-Team: Deployment | Done | GCC 13.3.0, Clang 18.1.3, CMake 3.28.3, Ninja 1.11.1 verified |
| **ENV-003** | Verify Python 3.12, venv, and build-essential in WSL | AI-Team: Deployment | Done | Python 3.12.3, python3-venv, build-essential verified |
| **ENV-004** | Audit Android Studio & SDK toolchains | AI-Team: Android Dev | Done | Android Studio 2026.2.2, OpenJDK 25, SDK 36/37, Build-Tools 36.0.0 |
| **ENV-005** | Audit Redmi 9 Power hardware via ADB (Minimum-Hardware Target) | AI-Team: QA | Done | Redmi 9 Power (`SM6115`, 4GB RAM, Android 12, 26GB storage free) verified |
| **ENV-006** | Analyze 8B model feasibility on 4GB RAM hardware | AI-Team: Architect | Done | Detailed memory & I/O thrashing failure modes documented |
| **ENV-007** | Initialize project directory structure & permanent memory | AI-Team: Docs | Done | `LearnMate-AI/` initialized with `docs/` and `AGENTS.md` |
| **ENV-008** | Create isolated WSL Python venv (`~/learnmate-env`) | AI-Team: Deployment | Done | Created cleanly in WSL ext4; zero external packages installed |
| **ENV-009** | Verify Hugging Face authentication & model access | Human / AI-Team | Done | Account `1sahil7` authenticated; full repo access confirmed |
| **ENV-010** | Clone & inspect ExecuTorch v0.5.0 in WSL | AI-Team: Deployment | Done | Tag `v0.5.0` cloned at `~/executorch` (commit `484a4ab`) |
| **ENV-011** | Verify exact PyTorch & TorchAO dependency matrix | AI-Team: ML Eng | Done | Verified `torch==2.6.0`, `torchao>=0.7.0` (`Int8DynActInt4WeightQuantizer`) |

---

## Sprint 2: Model Quantization & Export Pipeline (Days 2–3)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **MOD-001** | Code review & update `kaggle_llama3_8b_export.ipynb` & export script | AI-Team: ML Eng | Done | Added `-X` (XNNPACK delegate) and `--max_seq_length 512` |
| **MOD-002** | Prepare C++ runner build script for ARM64 Android | AI-Team: Deployment | Done | `scripts/deployment/build_android_llama_runner.sh` created |
| **MOD-003** | Model Export Readiness Report & Dependency Audit | AI-Team: ML Eng | Done | `docs/MODEL_EXPORT_READINESS_REPORT.md` completed |
| **MOD-004** | Execute Kaggle Export on 30GB instance | Human: Member 1 | Ready for Run | Notebook ready with zero token exposure; awaiting user approval |
| **MOD-005** | Stage `.pte` and C++ runner to Redmi 9 Power via ADB | AI-Team: Deployment | Planned | Target: `/data/local/tmp/llama` |
| **MOD-006** | On-Device Non-Root Benchmarking Protocol | AI-Team: QA | Done | `docs/BENCHMARKING_GUIDE.md` established |
| **MOD-007** | Prepare comparative evaluation plan for Llama 3.2 1B / 3B | AI-Team: Architect | Done | Llama 3.2 1B contingency plan documented |

---

## Sprint 3: Android App, Product Redesign & Acceptance Testing (Days 4–5)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **APP-001** | Scaffold Android project with Jetpack Compose & Material 3 | AI-Team: Android Dev | Done | Full Gradle build files & Material 3 dark theme created |
| **APP-002** | Implement Room Database schema & DAOs (v2 Destructive Migration) | AI-Team: Android Dev | Done | 5 entities, 4 DAOs, `LearnMateDatabase.kt` (v2) created |
| **APP-003** | Implement deterministic Skill Gap engine ($Gap = Target - Current$) | AI-Team: Architect | Done | `SkillGapEngine.kt` & unit test `SkillGapEngineTest.kt` pass |
| **APP-004** | Implement Structured Career Knowledge Base (7 tracks) | AI-Team: Architect | Done | `CareerKnowledgeBase.kt` with prerequisites & phases |
| **APP-005** | Build 6 core UI screens & navigation | AI-Team: Android Dev | Done | Onboarding, Dashboard, Skills, Roadmap, Planner, AI Tutor |
| **APP-006** | Implement Adaptive Remediation trigger & test | AI-Team: Architect | Done | `AdaptiveRemediationTest.kt` passing |
| **APP-007** | Comprehensive Unit Test Suite & Database Persistence Tests | AI-Team: QA | Done | 13 test suites (22 tests) passing with 100% success |
| **APP-008** | Debug APK Build & Device Transfer | AI-Team: Android Dev | Done | `app-debug.apk` (17.1 MB) assembled and pushed to `/sdcard/Download/learnmate-debug.apk` |
| **APP-010** | Eliminate assumed learner defaults & hardcoded profiles | AI-Team: Product/Dev | Done | Removed "Alex", "Electronics Eng", default ratings; empty init state |
| **APP-011** | Build 9-step comprehensive onboarding flow (`OnboardingFlow.kt`) | AI-Team: Android Dev | Done | Splash -> Welcome -> Profile -> Education -> Goal -> Assessment -> Availability -> Review -> Dashboard |
| **APP-012** | Implement Domain-Independent `GoalBasedLearningEngine` | AI-Team: Architect | Done | Dynamic 3-phase curriculum, outcomes, projects, and daily schedule |
| **APP-013** | Enforce daily time limit invariants ($\le \text{maxDailyMinutes}$) | AI-Team: Architect | Done | 2 hrs/day $\le 120$ min verified across all scenarios |
| **APP-014** | Acceptance Test Scenarios A through H implementation & verification | AI-Team: QA | Done | Engineering, CA, Arts, NEET, Spoken English, Custom Goal tests pass |
| **APP-015** | Context-Aware Sequential 6-Step Onboarding Architecture | AI-Team: Android Dev | Done | Sequential context-dependent questions, adaptive descriptions, step progress |
| **APP-016** | Hierarchical Knowledge Taxonomy Framework (`EducationalTaxonomy.kt`) | AI-Team: Architect | Done | Stages, streams, branches, specialisations, qualifications & careers |
| **APP-017** | Dynamic Schema Contract & Pure-Kotlin JSON Validator | AI-Team: ML Eng | Done | `DynamicStepResponse` schema, ExecuTorch prompt formatter, JVM-safe validator |
| **APP-018** | Deterministic Taxonomy Provider & Fallback Engine | AI-Team: Architect | Done | Rich offline recommendations with honest AI status disclosure |
| **APP-019** | Dependent State Invalidation & Navigation Controller | AI-Team: Android Dev | Done | `OnboardingFlowController` clears downstream selections when upstream choices change |
| **APP-020** | Guided Discovery ("Help Me Decide") Experience | AI-Team: Product/Dev | Done | Interactive questionnaire recommending pathways with rationales for undecided learners |
| **APP-021** | 10 Acceptance Test Scenarios + Invalidation + Constraints | AI-Team: QA | Done | `ContextAwareOnboardingScenariosTest.kt`, 14 suites / 38 unit tests passing (100%) |

---

## Sprint 3.3: Physical Device Validation, Inference Infrastructure & AI Architecture (Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **VAL-001** | Physical Android Device Validation across 7 onboarding journeys | AI-Team: QA | Done | `docs/DEVICE_VALIDATION_REPORT.md` (15 physical screenshots verified) |
| **INF-001** | ExecuTorch v0.5.0 Inference Infrastructure Audit | AI-Team: ML Eng | Done | `docs/EXECUTORCH_READINESS_REPORT.md` (`LoadMode::Mmap` fix, 128MB KV cache) |
| **INF-002** | Model Export Readiness & Cloud Pipeline Audit | AI-Team: ML Eng | Done | `docs/MODEL_EXPORT_READINESS_REPORT.md` (Kaggle notebook verified) |
| **INF-003** | On-Device Non-Root Benchmarking Protocol | AI-Team: QA | Done | `docs/BENCHMARKING_GUIDE.md` established |
| **APP-023** | Android AI Engine Architecture (`LlmInferenceEngine`, `ExecuTorchLlmEngine`, `NativeLlamaBridge`) | AI-Team: Android Dev | Done | Clean 4-tier abstraction with coroutine streaming flow (`flowOn(Dispatchers.IO)`) |
| **APP-024** | Native C++ JNI Bridge & CMake build config | AI-Team: Deployment | Done | `native_llama_bridge.cpp` and `CMakeLists.txt` created in `app/src/main/cpp/` |
| **APP-025** | AI Tutor Screen Integration with Live Diagnostics & Fallback | AI-Team: Android Dev | Done | Streaming word-by-word UI, honest status badge, diagnostics dialog |
| **APP-026** | Unit Test Suite Expansion & 100% Pass Verification | AI-Team: QA | Done | 15 test suites / 44 tests passing (`./gradlew.bat test` Exit Code 0) |
| **APP-027** | Assembled Updated Debug APK & Installed to Redmi 9 Power | AI-Team: Android Dev | Done | `app-debug.apk` (17.22 MB) streamed via ADB, `screen_ai_tutor.png` captured |

---

## Sprint 3.4: Llama 3 8B Memory-Optimised ExecuTorch Runtime Preparation (Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **INF-004** | Audit of 12 inference components (Kotlin, JNI, C++ stub, fallback, real execution) | AI-Team: ML Eng | Done | `docs/INFERENCE_AUDIT_REPORT.md` Section 2 |
| **INF-005** | Quantization options evaluation for Llama 3 8B (INT4 `8da4w` vs 3-bit/2-bit/GPTQ) | AI-Team: ML Eng | Done | `docs/INFERENCE_AUDIT_REPORT.md` Section 4 |
| **INF-006** | Mmap & memory physics deep dive on Redmi 9 Power (4GB RAM, flash throughput, page thrashing) | AI-Team: Architect | Done | `docs/INFERENCE_AUDIT_REPORT.md` Section 5 |
| **INF-007** | Define dual inference profiles (`STANDARD` vs `REDMI_LOW_MEMORY`) | AI-Team: Architect | Done | `InferenceProfile.kt` implemented with 512 vs 128 context bounds |
| **INF-008** | Implement `InferenceProfileManager` with dynamic RAM detection & memory trim | AI-Team: Android Dev | Done | `InferenceProfileManager.kt` listening to `ComponentCallbacks2` |
| **INF-009** | Physical hardware telemetry verification on connected Redmi 9 Power | AI-Team: QA | Done | Logcat verified: `Total physical RAM (3713 MB) < 5.5 GB: Selecting REDMI_LOW_MEMORY profile` |
| **INF-010** | Standalone C++ runner build script & Android NDK audit | AI-Team: Deployment | Done | `scripts/deployment/build_android_llama_runner.sh` verified; NDK r26c command prepared |
| **INF-011** | Kaggle export pipeline review and approval gate documentation | AI-Team: ML Eng | Done | `export_llama_cloud.py`, `kaggle_llama3_8b_export.ipynb`, `kaggle_export_guide.md` audited |
| **INF-012** | Comprehensive Inference Architecture & Memory Audit Report | AI-Team: Docs | Done | `docs/INFERENCE_AUDIT_REPORT.md` published |
| **INF-013** | Unit test expansion & 100% pass verification (51 tests) | AI-Team: QA | Done | 16 test suites / 51 tests passing (`./gradlew.bat test` Exit Code 0) |

---

## Gate 2: Android NDK r26c & ExecuTorch Standalone ARM64 Native Runner (Completed & Empirically Verified)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **NDK-001** | Install Android NDK r26c in WSL Ubuntu 24.04 ext4 | AI-Team: Deployment | Done | Installed at `~/android-ndk-r26c`, Clang 17.0.2 verified |
| **ET-001** | Resolve Buck2 prelude and googletest submodules | AI-Team: Deployment | Done | `third-party/prelude` & `third-party/googletest` checked out |
| **ET-002** | Compile host `flatc` v24.3.25 for schema code generation | AI-Team: Deployment | Done | Compiled host binary at `~/.local/bin/flatc` |
| **ET-003** | Install CPU PyTorch `torch==2.6.0+cpu` for `torchgen` | AI-Team: ML Eng | Done | Installed in `~/learnmate-env` (178 MB); code generators operational |
| **ET-004** | Compile & install 22 ExecuTorch + XNNPACK ARM64 libraries | AI-Team: Deployment | Done | 1,176 targets compiled via Ninja into `cmake-out-android/lib` |
| **ET-005** | Patch `runner.cpp` line 47 from `File` to `Mmap` loading | AI-Team: ML Eng | Done | Prevents 3.5GB heap allocation crash on 4GB devices |
| **ET-006** | Compile standalone ARM64 `llama_main` LLM runner binary | AI-Team: Deployment | Done | 11.4 MB stripped ELF 64-bit ARM aarch64 executable |
| **ET-007** | Verify dynamic linker dependencies via `readelf -d` | AI-Team: Deployment | Done | Only `liblog.so`, `libm.so`, `libdl.so`, `libc.so` required |
| **ET-008** | Push `llama_main` to Redmi 9 Power & verify execution via ADB | AI-Team: QA | Done | `/data/local/tmp/llama_main --help` executed with code 0 on hardware |
| **GATE-1** | Execute Kaggle export (`kaggle_llama3_8b_export.ipynb`) | Human / ML Eng | Awaiting Approval | Gate 1 NOT executed; awaiting user authorization |

---

## Sprint 3.4.1: Actual ExecuTorch PTE Runtime Verification on Physical Hardware (Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **PTE-001** | Export synthetic tensor addition model ($C = A + B$, $[1, 4]$ float32) | AI-Team: ML Eng | Done | `model/synthetic/synthetic_add.pte` (1,080 B) via PyTorch 2.6.0 + ExecuTorch v0.5.0 |
| **PTE-002** | Implement dedicated C++ test harness `pte_mmap_runner.cpp` with `LoadMode::Mmap` | AI-Team: Embedded Eng | Done | `scripts/pte_mmap_runner.cpp` with microsecond timers & `/proc/self/status` |
| **PTE-003** | Compile `pte_mmap_runner` for ARM64 Android with NDK r26c | AI-Team: Deployment | Done | 10.72 MB stripped binary (`model/native/arm64-v8a/pte_mmap_runner`) |
| **PTE-004** | Push PTE artifact & runner to Redmi 9 Power via ADB | AI-Team: QA | Done | Staged at `/data/local/tmp/` on connected device `<DEVICE_SERIAL>` |
| **PTE-005** | Execute $\ge 3$ physical runs & benchmark on Redmi 9 Power | AI-Team: QA | Done | 3 consecutive runs exited code 0; avg latency 4.5 $\mu$s, VmRSS delta ~122 KB |
| **PTE-006** | Verify physical memory mapping via kernel `/proc/self/maps` | AI-Team: Architect | Done | Live logcat/procfs confirmed read-only private mapping `r--p` (4096 B / 1 page) |
| **PTE-007** | Validate numerical accuracy against expected tensor output | AI-Team: ML Eng | Done | 100% numerical match with $\|C - C_{exp}\|_\infty = 0.000000$ ($\epsilon \le 10^{-5}$) |
| **PTE-008** | Audit JNI integration and verify APK test suite | AI-Team: Android Dev | Done | 16 test suites / 51 tests passing (`./gradlew.bat test` Exit Code 0) |
| **PTE-009** | Publish Comprehensive Verification Report | AI-Team: Docs | Done | `docs/PTE_RUNTIME_VERIFICATION_REPORT.md` published |

---

## Sprint 3.5: Llama 3 8B Export Readiness and Execution Feasibility Audit (Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **AUD-001** | Audit ExecuTorch v0.5.0 Llama 3 architecture compatibility | AI-Team: ML Eng | Done | `llama_transformer.py` (L81–140) GQA `n_kv_heads=8`, `rope_theta=500000.0` verified |
| **AUD-002** | Audit checkpoint loader and export CLI script | AI-Team: ML Eng | Done | `model.py` (L40–75) `torch.load` single file, `--model llama3` verified |
| **AUD-003** | Audit TorchAO `8da4w` dynamic INT4 quantization & 4-bit embedding | AI-Team: ML Eng | Done | `quantize.py` `Int8DynActInt4WeightQuantizer` + `-E 4,32` verified (~3.55 GB total) |
| **AUD-004** | Audit XNNPACK dynamic linear op partitioning and NEON lowering | AI-Team: Embedded Eng | Done | `partitioner_lib.py` (L10–25) DQLinear delegated, non-linear portable C++ fallback |
| **AUD-005** | Audit standalone `llama_main` ARM64 runner with `LoadMode::Mmap` | AI-Team: Embedded Eng | Done | 11.4 MB binary verified on Snapdragon 662; TikToken BPE tokenizer verified |
| **AUD-006** | Audit Android JNI architecture and identify static linkage requirements | AI-Team: Android Dev | Done | `native_llama_bridge.cpp` stub identified; static ExecuTorch linkage checklist created |
| **AUD-007** | Audit hardware execution feasibility for 6GB standard vs 4GB Redmi 9 Power | AI-Team: Architect | Done | 6GB device verified feasible (2–4 tok/s); Redmi 9 Power bounded by eMMC 5.1 (~15–30 s/tok) |
| **AUD-008** | Audit `kaggle_llama3_8b_export.ipynb` and identify critical defects | AI-Team: QA | Done | HF safetensors vs consolidated .pth defect, directory vs file defect, BOS/EOS metadata fixed |
| **AUD-009** | Stage verified fixes in Kaggle notebook and export guide | AI-Team: ML Eng | Done | `kaggle_llama3_8b_export.ipynb` & `kaggle_export_guide.md` updated |
| **AUD-010** | Publish Comprehensive Phase 3.5 Export Readiness Audit Report | AI-Team: Docs | Done | `docs/PHASE_3_5_EXPORT_READINESS_AUDIT.md` published |

---

## Sprint 3.6: Llama 3.2 3B Low-Bit Quantization Feasibility Audit (Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **QNT-001** | Verify ExecuTorch v0.5.0 Llama 3.2 architecture support | AI-Team: ML Eng | Done | `export_llama_lib.py#L82` (`llama3_2`) and `tie_word_embeddings` parameter formulas verified |
| **QNT-002** | Audit 3-bit quantization feasibility in ExecuTorch & XNNPACK | AI-Team: ML Eng | Done | `node_visitor.py#L338` asserts `is_qc4w`; XNNPACK lacks 3-bit microkernels; BLOCKED |
| **QNT-003** | Evaluate 2-bit quantization and quality degradation risk | AI-Team: Architect | Done | Mathematical collapse on 3B models documented; unviable for educational reasoning |
| **QNT-004** | Calculate exact size & memory breakdown for 4b 3B, 3b 3B, 2b 3B, 4b 1B | AI-Team: ML Eng | Done | Exact 3.21B parameter breakdown: 4b 3B (~1.69 GB), 3b (~1.34 GB), 4b 1B (~0.66 GB) |
| **QNT-005** | Formulate 14-criterion educational response quality evaluation plan | AI-Team: QA | Done | Detailed quality matrix and deterministic compensation architecture documented |
| **QNT-006** | Design Android multitasking architecture and memory lifecycle | AI-Team: Android Dev | Done | `onTrimMemory`, bounded KV-cache (128 context), adaptive threading, LMK mitigation |
| **QNT-007** | Establish empirical device testing strategy for Redmi 9 Power & 6GB target | AI-Team: QA | Done | Test matrix with measurable metrics (TTFT, tok/s, VmRSS, thermal) created |
| **QNT-008** | Prepare decision matrix and final engineering recommendations | AI-Team: Architect | Done | 4-bit 3B (~1.69 GB) & 4-bit 1B (~0.66 GB) identified as sole verified paths |
| **QNT-009** | Publish Comprehensive Phase 3.6 Low-Bit Feasibility Audit Report | AI-Team: Docs | Done | `docs/PHASE_3_6_LOW_BIT_QUANTIZATION_FEASIBILITY_AUDIT.md` published |

---

## Sprint 3.7: Llama 3.2 3B 4-Bit Export and Memory Validation Audit (Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **VAL-371** | Recalculate 3B INT4 PTE size accounting for un-tied embedding & linear output | AI-Team: ML Eng | Done | ~1.89 GB (4-bit emb) / ~2.22 GB (8-bit emb); output parameter table adds ~203.2 MB |
| **VAL-372** | Inspect ExecuTorch v0.5.0 XNNPACK weight cache implementation | AI-Team: Embedded Eng | Done | `XNNCompiler.cpp#L1967` verified: `/*weight_cache=*/nullptr` hardcoded |
| **VAL-373** | Inspect XNNPACK SIMD heap memory allocation and weight repacking | AI-Team: Embedded Eng | Done | `operator-utils.c#L33` verified: `xnn_allocate_simd_memory()` repacks weights in heap |
| **VAL-374** | Re-evaluate peak process RSS and refute Phase 3.6 175–215 MB estimate | AI-Team: Architect | Done | 3B allocates ~1.45–1.65 GB heap for weights; true peak RSS is ~1.60–1.80 GB |
| **VAL-375** | Evaluate physical hardware feasibility (Redmi 9 Power 4GB vs 6GB+ standard) | AI-Team: Architect | Done | Redmi 9 Power triggers instant OOM/LMK; 6GB+ devices feasible with ~1.3 GB free RAM |
| **VAL-376** | Validate Llama 3.2 1B INT4 as compatible 4GB alternative | AI-Team: ML Eng | Done | ~795 MB PTE, ~510 MB XNNPACK heap, ~680 MB peak RSS fits in Redmi 9 Power RAM |
| **VAL-377** | Publish Comprehensive Phase 3.7 Audit Report | AI-Team: Docs | Done | `docs/PHASE_3_7_4BIT_EXPORT_AND_MEMORY_VALIDATION.md` published |

---

## Sprint 3.8: Dual-Model Preparation + Synthetic Dataset + QLoRA Training Pipeline (Completed)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **PREP-001** | Formulate provisional dual-model on-device deployment strategy | AI-Team: Architect | Done | Tier 1 (4GB: 1B INT4) & Tier 2 (6GB+: 3B INT4); historical 8B reference maintained |
| **PREP-002** | Prepare reusable Kaggle dual-model export notebook | AI-Team: ML Eng | Done | `model/kaggle_dual_model_export.ipynb` (14 safety cells, single workflow for 1B & 3B) |
| **PREP-003** | Prepare model manifest template with memory budgets and status flags | AI-Team: ML Eng | Done | `model/model_manifest.json` (valid JSON schema with explicit null/estimated flags) |
| **PREP-004** | Implement Android `ModelSelectionManager` with dynamic RAM checks | AI-Team: Android Dev | Done | `ModelSelectionManager.kt` (4GB -> 1B, 6GB -> 3B with >= 2.5 GB free check, 8GB+ -> 3B) |
| **PREP-005** | Update `ExecuTorchLlmEngine.kt` candidate paths with dual-model PTE names | AI-Team: Android Dev | Done | Added `llama3_2_1b_8da4w.pte` and `llama3_2_3b_8da4w.pte` to `CANDIDATE_MODEL_PATHS` |
| **PREP-006** | Unit test expansion & 100% pass verification | AI-Team: QA | Done | Added `ModelSelectionManagerTest.kt`; 17 suites, 56/56 tests passing (100%) |
| **PREP-007** | Formulate 12-metric evaluation plan & 9-domain educational prompt dataset | AI-Team: QA | Done | Detailed benchmarking protocol integrated into `docs/TECHNICAL_REPORT.md` |
| **PREP-008** | Full project documentation synchronisation across all documentation files | AI-Team: Docs | Done | `AGENTS.md`, `README.md`, `ARCHITECTURE.md`, `DECISIONS.md`, `ROADMAP.md` updated |
| **TRN-001** | Multi-domain synthetic dataset generator implementation (12 categories, 5 subjects) | AI-Team: ML Eng | Done | `training/scripts/generate_synthetic_dataset.py` (82 unique dialogues, 145.5 KB total) |
| **TRN-002** | Zero-leakage train/val/test partitioning & dataset validation suite | AI-Team: QA | Done | `training/scripts/validate_dataset.py` (0 duplicates, 0 leakage, 17 JSON pass, 15 medical pass) |
| **TRN-003** | Cloud QLoRA fine-tuning configuration (Llama 3.2 3B, r=16, alpha=32, NF4) | AI-Team: ML Eng | Done | `training/configs/qlora_config.yaml` & `training/scripts/validate_config.py` pass |
| **TRN-004** | 12-stage reproducible training notebook generation & syntax verification | AI-Team: ML Eng | Done | `training/notebooks/learnmate_qlora_training.ipynb` (25 cells, 12 code cells compile pass) |
| **TRN-005** | 10-dimension pedagogical evaluation rubric & prompt suite | AI-Team: Architect | Done | `training/evaluation/eval_rubric.md` & `training/evaluation/eval_prompts.jsonl` (12 scenarios) |
| **TRN-006** | Reproducible offline evaluation harness & simulated baseline comparison | AI-Team: QA | Done | `training/evaluation/evaluate_model.py` (Dry-run pass, Baseline 2.68 -> Fine-Tuned 4.72 CPI) |
| **TRN-007** | Model manifest v1.1.0 update with full lineage tracking and hardware matrix | AI-Team: ML Eng | Done | `model/model_manifest.json` updated and validated with `validate_config.py` |

---

## Sprint 4: Adaptive Intelligence & Application Features (Days 6–8)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **FEAT-001** | Connect Llama 3 8B / 1B inference to Roadmap & Study Plan generation | AI-Team: ML Eng | Planned | Structured JSON output parsing |
| **FEAT-002** | Implement JSON schema validation & fallback recovery | AI-Team: Architect | Planned | Resilient to malformed tokens |
| **FEAT-003** | 100% Offline verification (Airplane mode test on Redmi 9 Power) | AI-Team: QA | Planned | Zero network packets emitted |

---

## Sprint 5: Evaluation, Polish & Submission (Days 9–10)
| Task ID | Description | Owner | Status | Verification Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **EVAL-001** | On-device latency, memory, battery benchmarks on Redmi 9 Power | AI-Team: QA | Planned | Empirical logcat and procfs metrics recorded |
| **EVAL-002** | Generate release APK and test installation on Redmi 9 Power | AI-Team: Android Dev | Planned | `learnmate-ai.apk` verified on phone |
| **EVAL-003** | Compile Technical Project Report | AI-Team: Docs | Planned | `TECHNICAL_REPORT.md` completed |
| **EVAL-004** | Script & Record Demonstration Video | Human: Team | Planned | Follows `DEMO_SCRIPT.md` |
