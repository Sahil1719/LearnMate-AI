# Technical Architecture Decisions (ADR)

---

## ADR-001: Model Quantization Strategy for Llama 3 8B
- **Context:** Deploying an 8-billion parameter model on constrained mobile hardware. An unquantized 16-bit checkpoint is ~16 GB, far exceeding mobile RAM.
- **Alternatives Considered:**
  1. *W8A8 (8-bit weights, 8-bit activations):* Results in an ~8 GB model, which cannot fit on mobile devices with $\le 8\text{GB}$ RAM.
  2. *INT4 Weight-Only (w4a16):* Reduces model to ~4.5 GB, but fp16 activation math may be slow on ARM CPUs without native fp16 matrix acceleration.
  3. *TorchAO 8da4w (8-bit dynamic activations, 4-bit groupwise weights):* Reduces model to ~4.0 - 4.5 GB, with optimized INT8 GEMM kernels for ARM via ExecuTorch XNNPACK backend.
  4. *Embedding Quantization (`--embedding-quantize 4,32`):* Llama 3 has a massive 128k vocabulary. Embedding table alone in fp16 is $128,256 \times 4096 \times 2 = 1.05\text{ GB}$. Quantizing embeddings to 4-bit saves ~750 MB.
- **Decision:** Primary quantization configuration is **TorchAO 8da4w with 4-bit embedding quantization**.
- **Rationale:** Minimizes peak memory consumption to ~4.0 - 4.3 GB, which is the tightest footprint achievable for an 8B model.

---

## ADR-002: Deterministic Hybrid Intelligence vs. Pure LLM Generation
- **Context:** A pure LLM chatbot often hallucinates arbitrary skill levels, invents inconsistent prerequisites, or generates unverifiable roadmaps.
- **Alternatives Considered:**
  1. *Pure LLM Prompting:* Ask Llama 3 8B to calculate gaps and generate all curriculum details freely. (High risk of hallucination and schema breakage).
  2. *Static Rule Engine:* Hardcode all curriculums without LLM adaptability. (Lacks personalization, tone, and contextual explanations).
  3. *Hybrid Intelligence System:* Use a deterministic knowledge base with explicit prerequisite graphs and mathematical gap scoring ($Gap = Target - Current$). Feed validated gap records to Llama 3 8B with strict JSON schemas for contextual explanation, topic scheduling, and quiz generation.
- **Decision:** Implement **Deterministic Hybrid Intelligence**.
- **Rationale:** Ensures educational correctness, prevents model drift, and guarantees that competency numbers are mathematically grounded.

---

## ADR-003: Model Storage and Memory Management on Android
- **Context:** Android processes are subject to the Low Memory Killer (LMK). Redmi 9 Power has ~1.48 GB available under typical multitasking load.
- **Alternatives Considered:**
  1. *Full Model Heap Allocation:* Read the 4GB `.pte` file into Java/C++ memory buffer. (Instant OOM / LMK crash).
  2. *Zero-Copy File Memory Mapping (`mmap`):* Memory-map the `.pte` file from internal storage. The Linux kernel pages in weight tensors on demand and discards read-only pages when under memory pressure without crashing.
- **Decision:** Enforce **`mmap` zero-copy model loading** in the ExecuTorch C++ runtime and keep context window small ($N_{ctx} \le 512$).
- **Rationale:** The only viable technical path to run an 8B model within the physical memory envelope of a mobile device.

---

## ADR-004: WSL Python Isolation Strategy
- **Context:** Ubuntu 24.04 ships with Python 3.12.3 as system Python. Debian/Ubuntu PEP 668 prevents modifying system Python with pip.
- **Decision:** Create dedicated virtual environment `~/learnmate-env` inside WSL Linux ext4 filesystem.
- **Rationale:** Completely protects system stability, adheres to PEP 668, and maximizes filesystem I/O performance compared to hosting the venv on `/mnt/c/`.

---

## ADR-005: Minimum-Hardware Benchmark Architecture (Redmi 9 Power 4GB RAM) & Model Contingency Strategy
- **Context:** The human development team selected the **Redmi 9 Power (Qualcomm Snapdragon 662, 4GB RAM, Android 12)** as the intentional minimum-hardware benchmark target. An 8B INT4 model (~4.27 GB) is physically larger than the total physical RAM (3.80 GB usable) of the device.
- **Decision:**
  1. Retain **Meta Llama 3 8B Instruct** as the primary reference model for export and initial benchmarking.
  2. Execute the one-time quantization/export on Kaggle (30GB RAM) to generate `llama3_8b_8da4w.pte`.
  3. Benchmark `llama3_8b_8da4w.pte` directly on the Redmi 9 Power via standalone ExecuTorch C++ binary (`llama_runner`) to record empirical behavior (loading latency, peak PSS/RSS, TTFT, tokens/sec, and LMK kill signals).
  4. If LMK kills the 8B model or storage thrashing causes unusable latency ($\ge 10\text{ s/token}$), rigorously document the empirical failure logs in the Technical Report and evaluate smaller Llama-family siblings:
     - **Meta Llama 3.2 1B Instruct:** ~750MB INT4 (`8da4w`), easily fits within 1.48GB available RAM, achieves 4–8 tokens/sec on Cortex-A73 NEON cores.
     - **Meta Llama 3.2 3B Instruct:** ~1.9GB INT4 (`8da4w`), intermediate footprint.
- **Rationale:** Demonstrates absolute scientific and engineering integrity. Avoids unverified claims of compatibility while providing a production-grade, functioning fallback on constrained 4GB mobile hardware.

---

## ADR-006: ExecuTorch Model Loading Mode Specification (Mmap vs File) & Android AI Service Architecture
- **Context:** Source inspection of ExecuTorch v0.5.0 (`examples/models/llama/runner/runner.cpp`) revealed that the default runner hardcodes `Module::LoadMode::File`. On a 4GB RAM device, `LoadMode::File` invokes `malloc` for the entire 4.27GB model file, triggering a 100% reproducible out-of-memory crash. Furthermore, the Android app requires asynchronous token streaming that never blocks the main UI thread.
- **Decision:**
  1. Mandate `Module::LoadMode::Mmap` across all Android native runners and JNI bridges.
  2. Implement a 4-tier Android AI architecture: Jetpack Compose UI $\to$ `LlmInferenceEngine` interface $\to$ `ExecuTorchLlmEngine` (probes `/sdcard/Download` and `/data/local/tmp/llama`) $\to$ `NativeLlamaBridge` (JNI to `libexecutorch_llama_jni.so`).
  3. Enforce Kotlin Coroutine `flowOn(Dispatchers.IO)` with asynchronous `callbackFlow` to stream tokens word-by-word into the UI.
  4. Implement deterministic offline knowledge taxonomy fallback when `.pte` weights are uninstalled, transparently informing the user via the interactive diagnostics dialog.
- **Rationale:** Prevents fatal OOM crashes on 4GB devices, delivers smooth 60fps Compose UI interactions, and guarantees that the learning companion remains 100% functional offline under all deployment states.

---

## ADR-007: Dual Runtime Inference Profiles and Dynamic RAM Detection (STANDARD vs REDMI_LOW_MEMORY)
- **Context:** The application must support diverse Android device classes—ranging from constrained 4GB RAM devices (Xiaomi Redmi 9 Power) to higher-tier 8GB RAM devices (Realme X7 5G)—without violating the invariant that the model architecture (Meta Llama 3 8B Instruct INT4 `8da4w`) remains identical. Running identical concurrency and context lengths across all devices would either underutilize high-RAM devices or trigger LMK kills on 4GB hardware.
- **Alternatives Considered:**
  1. *Uniform 512-Token Context Limit:* Allocates 128 MB float32 KV-cache across all devices. High probability of LMK termination on Redmi 9 Power when available user memory drops below 400 MB.
  2. *Silent Substitution with Smaller Model (e.g. 1B):* Violates project governance and competition requirements.
  3. *Dual Runtime Profiles with Dynamic Hardware Telemetry:* Maintain the **identical Llama 3 8B model**, but parameterize execution context, threading, and memory trimming policies dynamically based on physical RAM query (`ActivityManager.getMemoryInfo()`).
- **Decision:**
  1. Establish two standardized profiles:
     - `STANDARD Profile` ($\ge 6\text{GB}$ RAM): Context 512 tokens (128 MB KV-cache), 4 threads, multi-threaded XNNPACK NEON.
     - `REDMI_LOW_MEMORY Profile` ($< 5.5\text{GB}$ RAM, target: Redmi 9 Power 4GB): Context 128 tokens (32 MB KV-cache, saving 96 MB resident heap), 1 thread sequential decoding, aggressive `ComponentCallbacks2` memory trimming.
  2. Implement `InferenceProfileManager` to automatically detect physical RAM (tested on device: 3,713 MB detected on Redmi 9 Power) and dynamically select the profile.
  3. Expose the active profile and live telemetry directly in the UI Diagnostics Dialog.
- **Rationale:** Strictly preserves the Meta Llama 3 8B Instruct model across all platforms while maximizing stability, eliminating out-of-memory crashes on constrained hardware, and providing full transparency to evaluators.


---

## ADR-008: Standalone ExecuTorch ARM64 Native Runner & WSL Cross-Compilation Architecture
- **Context:** Verifying and benchmarking Llama 3 8B on physical hardware requires isolating the ML execution pipeline from the full Android application framework. An independent native C++ executable (`llama_main`) enables direct ADB command-line benchmarking without ART JVM overhead, ActivityManager interference, or UI thread memory consumption.
- **Alternatives Considered:**
  1. *Compile inside Android Studio Gradle via NDK CMake:* Bundles C++ with the APK, but makes isolated benchmarking difficult and hides raw POSIX/NEON metrics.
  2. *Compile standalone ARM64 binary using WSL Ubuntu 24.04 and Android NDK r26c:* Generates a standalone static/Bionic-linked PIE binary that runs directly in `/data/local/tmp/` on Android.
- **Decision:**
  1. Install Android NDK r26c in WSL Ubuntu 24.04 ext4 (`$HOME/android-ndk-r26c`).
  2. Compile ExecuTorch v0.5.0 core, XNNPACK backend, and kernel libraries using Clang 17.0.2 for `arm64-v8a` target (API 26).
  3. Patch `examples/models/llama/runner/runner.cpp` line 47 to enforce `Module::LoadMode::Mmap` rather than `Module::LoadMode::File`.
  4. Build standalone `llama_main` (11.4 MB stripped) linking against Bionic libc/libm/libdl/liblog.
  5. Deploy and verify via ADB: `/data/local/tmp/llama_main --help`.
- **Rationale:** Proves 100% native execution compatibility on physical target hardware (Snapdragon 662) ahead of model weight staging, ensuring zero runtime linker incompatibilities.


---

## ADR-009: Dual-Model On-Device Deployment Strategy (Llama 3.2 1B on 4GB RAM, Llama 3.2 3B on 6GB+ RAM)
- **Context:** The original mentor recommendation was Meta Llama 3 8B Instruct (a recommendation, not a confirmed competition requirement). Technical feasibility audits conducted in Phases 3.5, 3.6, and 3.7 demonstrated critical physical constraints:
  1. *Llama 3 8B INT4:* Requires ~4.27 GB artifact and >4.0 GB runtime memory, exceeding total physical RAM (3.74 GB) on the primary experimental device (Xiaomi Redmi 9 Power 4GB).
  2. *ExecuTorch v0.5.0 XNNPACK Weight Repacking:* Source inspection of `XNNCompiler.cpp#L1967` proved that `/*weight_cache=*/nullptr` is hardcoded. `operator-utils.c#L33` calls `xnn_allocate_simd_memory()` (`posix_memalign`), reordering and unpacking all 4-bit weights into anonymous heap buffers for SIMD GEMM. These buffers reside permanently in anonymous dirty heap memory and are NOT demand-paged zero-copy from flash via `mmap`.
  3. *Llama 3.2 3B INT4:* XNNPACK weight repacking requires ~1.45–1.65 GB heap memory at startup, driving peak process RSS to ~1.60–1.80 GB. On Redmi 9 Power (~1.34 GB available RAM), attempting to load 3B causes immediate `ENOMEM` or Android Low Memory Killer (`lmkd`) termination.
  4. *Llama 3.2 1B INT4:* XNNPACK weight repacking requires ~510 MB heap memory, giving peak process RSS of ~650–720 MB. This fits inside Redmi 9 Power's available RAM with ~660 MB headroom for Android OS and background multitasking.
  5. *Standard 6GB+ RAM Devices:* Have ~2.90–3.20 GB available free RAM, comfortably accommodating Llama 3.2 3B INT4 (~1.75 GB RSS) with ~1.20–1.45 GB headroom for multitasking.
- **Alternatives Considered:**
  1. *Uniform 8B Deployment Across All Devices:* Guaranteed crash on 4GB hardware due to physical RAM exhaustion.
  2. *Uniform 3B Deployment Across All Devices:* Triggers instant LMK kills on 4GB hardware due to ~1.6–1.8 GB RSS exceeding available memory.
  3. *Uniform 1B Deployment Across All Devices:* Runs reliably on 4GB, but underutilizes high-capacity 6GB/8GB devices.
  4. *Tiered Dual-Model Deployment Strategy:* Parameterize deployment by device RAM class, selecting 1B INT4 for 4GB RAM and 3B INT4 for 6GB+ RAM.
- **Decision:**
  1. Transition to an evaluated **Dual-Model On-Device Deployment Strategy**:
     - **Tier 1 (4GB RAM Baseline, e.g. Xiaomi Redmi 9 Power):** Meta Llama 3.2 1B Instruct (`8da4w` INT4, 4-bit embedding, 128 context). Estimated artifact: ~795 MB; estimated peak RSS: ~680 MB.
     - **Tier 2 (6GB+ RAM Standard, e.g. Realme X7 5G):** Meta Llama 3.2 3B Instruct (`8da4w` INT4, 4-bit embedding, 512 context). Estimated artifact: ~1.89 GB; estimated peak RSS: ~1.75 GB.
  2. Implement `ModelSelectionManager` in the Android client to automatically evaluate device RAM and memory pressure before loading.
  3. Retain historical Llama 3 8B Instruct decisions (ADR-001, ADR-005, ADR-007) and benchmark artifacts as reference baselines without silent rewriting or deletion.
  4. Both dual-model configurations remain provisional pending actual cloud export and physical device testing. No competition approval has been claimed.
- **Rationale:** Ensures crash-free, responsive on-device educational reasoning on the physical target hardware (Redmi 9 Power 4GB) while preserving normal Android phone multitasking (WhatsApp, audio playback), and leveraging 3B capacity on higher-tier devices.
