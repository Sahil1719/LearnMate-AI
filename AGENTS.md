# LearnMate AI - Multi-Agent Operating Guidelines and Permanent Project Rules

## 1. Project Overview & Identity
- **Project Name:** LearnMate AI
- **Tagline:** Learn smarter. Progress faster. Your AI-powered learning companion.
- **Core Technology:**
  - Evaluated Dual-Model Architecture: Meta Llama 3.2 1B Instruct (4GB RAM tier) & Meta Llama 3.2 3B Instruct (6GB+ RAM tier).
  - Historical Reference / Competition Baseline: Meta Llama 3 8B Instruct (mentor's original recommendation, evaluated for memory boundaries).
  - Quantization: 4-bit Weight Quantization (`8da4w` / TorchAO: INT4 per-channel weight, INT8 dynamic activation, 4-bit embedding).
  - Runtime: PyTorch ExecuTorch v0.5.0 with XNNPACK ARM NEON backend.
  - Client Application: Android 12+ (Kotlin, Jetpack Compose, Material 3, Room Database, JNI).
- **Primary Physical Benchmark Target:** Xiaomi Redmi 9 Power (`SM6115` / `lime`, Snapdragon 662, 4GB RAM, Android 12).
- **Standard Deployment Target:** ARM64 Android devices with >= 6GB RAM (e.g., Realme X7 5G, Dimensity 800U).

---

## 2. Engineering Team Roles & Perspectives
Every operation must be viewed through these collaborative lenses:
1. **AI/ML Engineer:** Validates model weights, tokenizers, PyTorch/TorchAO quantization, precision loss, and KV-cache settings.
2. **LLM Deployment Engineer:** Orchestrates ExecuTorch build, operator delegations (XNNPACK CPU), `.pte` export, and memory footprint management.
3. **Android Application Developer:** Architects modern Jetpack Compose UI (dark-first, midnight navy / electric blue theme), Room DB schema, background threads, and JNI/AAR bindings.
4. **Software Architect:** Enforces deterministic skill gap calculation, hybrid intelligence, clear separation of concerns, and offline-first reliability.
5. **QA & Testing Engineer:** Validates all assertions with empirical tests, checks edge cases, memory limits, JSON parsing validity, and crashes.
6. **Technical Documentation Engineer:** Maintains persistent project memory across conversations, architecture specs, session logs, and technical reports.
7. **Project Manager:** Monitors roadmap, guards against scope creep, prioritizes functional MVP over cosmetic distractions, and tracks human approvals.

---

## 3. Mandatory Engineering Rules & Non-Negotiables
1. **Transparent Model Strategy & No Silent Rewrites:**
   - The mentor's original recommendation was Meta Llama 3 8B Instruct (a recommendation, not a confirmed competition requirement).
   - Technical audits (Phase 3.5, 3.6, 3.7) revealed that 8B (4.9 GB artifact) and 3B (1.7–1.9 GB artifact, 1.6–1.8 GB RSS) cannot run on 4GB hardware (Redmi 9 Power) without fatal OOM / `lmkd` termination.
   - The project is evaluating a practical Meta Llama-only dual-model strategy:
     - 4GB RAM devices (Redmi 9 Power): Meta Llama 3.2 1B Instruct (`8da4w`).
     - 6GB+ RAM devices: Meta Llama 3.2 3B Instruct (`8da4w`).
   - Both configurations remain provisional until actual export and physical device testing.
   - Never delete or silently rewrite the historical 8B decision. Record the transition and technical justification transparently.
   - Do not claim competition approval has been obtained.
2. **Strict Verification Categories:**
   Maintain an unambiguous distinction across all documentation and reports:
   - **Verified:** Empirically measured and confirmed on physical target hardware (e.g. synthetic PTE tensor addition, native binary launch).
   - **Source-Verified:** Confirmed through direct inspection of C++/Python codebase and kernel source (e.g. `XNNCompiler.cpp` `weight_cache=nullptr`, `operator-utils.c` `xnn_allocate_simd_memory`).
   - **Estimated:** Mathematically calculated based on architecture and schemas; NOT measured on hardware (e.g. 1.89 GB 3B PTE size, 1.60–1.80 GB peak RSS, 1B RAM and stability).
   - **Planned:** Designed and scheduled in roadmap; not yet executed.
   - **Not Yet Tested:** No physical hardware execution has occurred (e.g. 1B on Redmi 9 Power, 3B on 6GB phone).
3. **Honest LLM Status:**
   - **No genuine Llama token generation has yet been demonstrated on the physical phone.**
   - Synthetic PTE execution (`synthetic_add.pte`, 1,080 bytes) and native executable launch (`llama_main --help`) confirm runtime infrastructure, NOT genuine LLM inference.
   - Do not represent estimated memory numbers (e.g. Phase 3.6's refuted 175–215 MB RSS) as verified.
4. **Zero Fabricated Benchmarks:**
   - Never invent or extrapolate memory usage, latency, or tokens-per-second. Benchmark directly on target hardware with empirical logging.
5. **Memory Realism:**
   - Total runtime memory = anonymous heap repacked weights + KV cache + activations + runtime arena + Android OS/base process overhead.
   - In ExecuTorch v0.5.0, XNNPACK does NOT execute weights zero-copy from flash; weights are repacked into anonymous heap memory during initialization.
   - Never claim a model fits on a device without verifying actual free memory and heap allocation limits.
6. **Deterministic Hybrid Intelligence (No Arbitrary LLM Invention):**
   - Competency scoring (0 to 5) and skill gaps ($Gap = \max(0, Target - Current)$) must be calculated deterministically via the structured knowledge base and rule engine.
   - The LLM reasons, explains, contextualizes, and drafts study schedules from validated data; it does not fabricate competency numbers.
   - All LLM JSON responses must be validated against strict schemas before touching the Room Database.
7. **Safe File and Tool Usage:**
   - Large model checkpoints, .pte files, build outputs, and virtual environments must never be committed to Git.
   - Sensitive tokens (Hugging Face tokens, API keys) must never be logged or hardcoded.
   - Destructive commands or cloud executions require explicit human approval.

---

## 4. Session Start & Handoff Protocol
At the beginning of each session:
1. Read `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/ROADMAP.md`, `docs/TASKS.md`, `docs/DECISIONS.md`, and `docs/SESSION_LOG.md`.
2. Inspect Git status and live hardware state.
3. Resume from the exact unverified milestone or next approved task.

At the end of each session:
1. Update `PROJECT_STATE.md`, `TASKS.md`, `SESSION_LOG.md`.
2. Output the standardized Session Handoff summary block.
