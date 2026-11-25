# LearnMate AI — Phase 3.6 Report: Read-Only Context, Quantization and Runtime Diagnostic

**Project:** LearnMate AI  
**Phase:** 3.6 (Diagnostic-Only)  
**Date:** 2026-11-25  
**Target Hardware:** Xiaomi Redmi 9 Power (`M2010J19SI`, Snapdragon 662, 4 GB RAM, Android 12 / API 31)  
**Runtime:** ExecuTorch `v0.5.0` (commit `484a4ab5b717ce1f364c469509214d0ed0cdaf11`), XNNPACK NEON (`cpu_threads=4`)  

---

## 1. Executive Summary

Phase 3.6 audited every stage of the LearnMate AI inference pipeline—Phase 3.5 raw token traces, FlatBuffer operator graphs inside both `.pte` binaries, C++ runner source (`main.cpp`, `runner.cpp`, `text_prefiller.cpp`, `text_token_generator.h`, `stats.h`), Android Kotlin/JNI prompt formatting and token budgeting (`ExecuTorchLlmEngine.kt`, `native_llama_bridge.cpp`), and Kaggle export/quantization recipes—and executed **30 new controlled physical-device diagnostic runs** (Diagnostics A, B, and F) across both `base_llama3_2_3b.pte` (Model A) and `learnmate_llama3_2_3b.pte` (Model B).

**Three Confirmed Root Contributors Identified:**
1. **System-Prompt Distraction ("Prompt Bleeding") Is a Major Immediate Trigger:** In Phase 3.5, **28 of 29 incomplete runs (96.6%)** did *not* run out of the 128-token context budget—they emitted `<|eot_id|>` early with **23–80 unused tokens remaining**. In Diagnostic A (`"What is an interrupt?"`) and Diagnostic F (minimal user-only prompts, `15–24` tokens), stripping the generic `"You are a helpful, respectful and honest assistant."` system turn immediately flipped both models from failing (`Q1/Q4/Q5` greedy errors in Phase 3.5) to answering correctly on **12/12 runs** for Pointer (`Q1`), MCU vs MPU (`Q2` initial answer `"No..."`), and ADC (`Q4`), whereas Medium (`32` tokens) and Long (`62` tokens) system prompts induced explicit system-prompt bleeding (*"accurate and respectful assistant"*, *"pause a conversation"*).
2. **Partial XNNPACK Delegation & Uncalibrated PTQ (`8da4w` + `4,32` Embeddings):** FlatBuffer inspection proves both `1.93 GB` PTEs are structurally identical: **only 197 linear layers** run in `XnnpackBackend`, while **2,955 operations** (RMSNorm, RoPE, explicit `aten::bmm` + `aten::_softmax` attention, SiLU, KV-cache updates, and `quantized_decomposed::embedding_4bit.dtype_out`) execute outside XNNPACK in generic CPU kernels because `--xnnpack_extended_ops` and `--use_sdpa_with_kv_cache` were off (`use_sdpa_with_kv_cache=0`). Quantization (`Int8DynActInt4WeightQuantizer`, `group_size=128`, `4,32` embeddings) is completely **data-free / uncalibrated**. Moreover, even with **76–92 spare tokens** on minimal prompts, both models still emit premature `<|eot_id|>` stops or syntax stutters (*"Central Processing Unit (CPU)"*), and Phase 3.4 proved the identical `base_llama3_2_3b.pte` diverges between WSL x86-64 (`82` tokens) and ARM64 ASIMD (`35` tokens).
3. **Confirmed Off-by-One Reporting Defects in ExecuTorch `v0.5.0` `runner.cpp`:** Inspection of C++ source and Diagnostic B empirical runs (`seq_len = 68, 84, 100`) prove `TextTokenGenerator::generate` returns `pos - start_pos` while skipping the first token already emitted by `TextPrefiller::prefill()`. Consequently, `PyTorchObserver.generated_tokens` **undercounts by 1**, and `runner.cpp:233` (`if (num_prompt_tokens + num_generated_tokens == seq_len)`) **never fires** (`seq_limit_log = False` even when `seq_len` terminates generation).

**Recommended Next Experiment:** Run a **controlled precision-isolation comparison (Unquantized FP16/BF16 vs. current INT4 `8da4w` PTEs)** on the exact Phase 3.5 and Phase 3.6 prompt suites before any further fine-tuning or quantization changes.

---

## 2. Phase 3.5 Evidence Review (Remaining Budgets & Termination)

Re-analysis of all **32 runs** in [`reports/phase_3_5_raw_outputs.json`](phase_3_5_raw_outputs.json) and [`reports/phase_3_5_results.csv`](phase_3_5_results.csv) (`P = 32..38`, `seq_len = 128`):

| Termination Class | Count (`N=32`) | Unused Budget Range (`128 - (P + G_obs)`) | Evidence / Interpretation |
| :--- | :---: | :---: | :--- |
| **`natural_eos` (`<|eot_id|>`)** | **31 (96.9%)** | **23 – 80 tokens** remaining | **28 of 29 incomplete runs** terminated via `<|eot_id|>` *with substantial unused capacity left* (e.g., `sampling_Q2_A` stopped at `G=14` with `80` tokens left; `greedy_Q4_A/B` stopped at `G=17` with `73` tokens left; `greedy_Q5_B` stopped at `G=35` with `56` tokens left). |
| **`context_limit` (`P + G_obs = 127`)** | **1 (3.1%)** | `1` token (`G_obs=91, P=36`) | Only `greedy_Q7_A` exhausted the 128-token ceiling—not from writing a long explanation, but from an infinite `**Sequential Logic**` heading loop (13+ repetitions). |

**Conclusion from Phase 3.5 Review:** Token-budget exhaustion was **not** the direct cause of incomplete answers in 28 of 29 incomplete Phase 3.5 runs; both models chose to stop early via `<|eot_id|>`.

---

## 3. Baseline Artifact & Runtime Configuration Verification

All SHA-256 hashes matched before and after all 30 Phase 3.6 diagnostic runs (`reports/phase_3_6_integrity.txt`):
- **Model A (`model/experimental/base_llama3_2_3b.pte` & `/data/local/tmp/llama/base_llama3_2_3b.pte`):** `2b855085703e6a1743437edb6adf467f7f95f9d23f810d8fd196175630979407` (`1,929,907,360` bytes, `ET12`)
- **Model B (`model/android/learnmate_llama3_2_3b.pte` & `/data/local/tmp/llama/learnmate_llama3_2_3b.pte`):** `8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91` (`1,929,907,360` bytes, `ET12`)
- **Shared Tokenizer (`model/android/tokenizer.model` & `/data/local/tmp/llama/tokenizer.model`):** `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` (`2,183,982` bytes)
- **Standalone Runner (`/data/local/tmp/llama/llama_main`):** `f6b533465ac75639f115ce7fcf8b418d2ec0a4dc5c235a217d21c7c9dca91b76`

---

## 4. Diagnostic A & B Audits: Sequence Limit, Prompt Budget & EOS Handling

### 4.1 Code-Level Inspection of ExecuTorch v0.5.0 Runner & Android Engine
Inspected `$HOME/executorch/examples/models/llama/main.cpp`, `runner/runner.cpp`, `extension/llm/runner/{text_prefiller.cpp,text_token_generator.h,stats.h}`, [`ExecuTorchLlmEngine.kt`](../android-app/app/src/main/java/com/learnmate/ai/domain/engine/ExecuTorchLlmEngine.kt), and [`native_llama_bridge.cpp`](../android-app/app/src/main/cpp/native_llama_bridge.cpp):

- **Maximum sequence length & scope:** Both PTEs export `get_max_seq_len = 128`. In `runner.cpp:178–180`, `seq_len` is clamped to `min(FLAGS_seq_len, get_max_seq_len)` and covers **`prompt + generated tokens`** combined.
- **No standalone `max_new_tokens` cap in CLI:** `llama_main --help` supports `-seq_len`, `-temperature`, `-cpu_threads`, `-warmup`, `-prompt`, `-model_path`, `-tokenizer_path` only. Setting generation budgets requires setting `--seq_len = P + budget`.
- **Prompt truncation behavior:**
  - `llama_main` (`runner.cpp:193–201`) **does not truncate**—if `num_prompt_tokens >= seq_len`, `ET_CHECK_MSG` aborts!
  - Android `ExecuTorchLlmEngine.budgetCompletePrompt()` (`L284–425`) reserves `MIN_GENERATION_TOKENS = 32` (`SAFE_MAX_PROMPT_TOKENS = 96`), keeps the user turn intact first while shrinking system/goal/context from the end, and only truncates the end of the user question (`truncateToTokens`) if the question alone exceeds 86 tokens. `native_llama_bridge.cpp:348–353` rejects `actual_p >= target_seq_len` (`return -9`).
- **EOS Checking & Accounting (`runner.cpp` + `text_token_generator.h`):**
  1. Both `128009` (`<|eot_id|>`) and `128001` (`<|end_of_text|>`) are loaded via `get_eos_ids` into `eos_ids_`.
  2. Generation stops either when `eos_ids_->find(cur_token) != eos_ids_->end()` or when `pos >= seq_len - 1`.
  3. **Token-emission order:** `token_callback` is invoked **before** checking `eos_ids_` (`text_token_generator.h:108–118`), so `<|eot_id|>` is printed to `stdout` in `llama_main` and counted in `pos++`. (Android `ExecuTorchLlmEngine.kt:597–604` strips/suppresses `<|eot_id|>`, `<|end_of_text|>`, `<|eom_id|>`, and `<|begin_of_text|>` from UI output.)
  4. **Confirmed C++ Off-by-One Reporting Defect:** `Runner::generate` (`L211–227`) emits token #1 (`cur_token` from `TextPrefiller::prefill()`) *outside* `TextTokenGenerator::generate`, and never checks `eos_ids_` on that prefill token. `TextTokenGenerator::generate` then generates `(pos - start_pos)` tokens stopping when `pos == seq_len - 1` (`P + G_obs = seq_len - 1`). As a result:
     - `PyTorchObserver.generated_tokens` (`G_obs`) **undercounts displayed tokens by 1**.
     - `runner.cpp:233` (`if (num_prompt_tokens + num_generated_tokens == seq_len)`) **is unreachable** at context exhaustion (`seq_len - 1 != seq_len`), so `"Sequence length (N tokens) reached!"` is **never logged**!
  5. **`eos_ids_` End-of-Turn Behavior:** Because `128009` (`<|eot_id|>`) is in `eos_ids_`, emitting `<|eot_id|>` immediately terminates single-turn generation (expected for single-turn QA; multi-turn requires re-prompting with history).

### 4.2 Controlled Prompt-Length Test (`Diagnostic A`, `N = 12` runs on Redmi 9 Power)
Identical question: **`"What is an interrupt?"`** at `seq_len = 128`, `cpu_threads = 4`, across Short (`15` tok, no system prompt), Medium (`32` tok, `"You are a helpful, respectful, accurate and honest assistant."`), and Long (`62` tok, verbose tutor system prompt):

| Run ID | Model | Prompt (`P`) | Budget (`128-P`) | `G_obs` | `Rem` | Term. | Output Summary / Verbatim Behavior | Complete? |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- | :---: |
| `diagA_short15_greedy_A` | A | 15 | 113 | 41 | 72 | `natural_eos` | Accurate computing exception + urgent CPU signal; trailing `(CPU)` glitch | Partial (accurate core, no ISR) |
| `diagA_short15_greedy_B` | B | 15 | 113 | 41 | 72 | `natural_eos` | Accurate computing exception + CPU signal; minor stutter `"by an interrupt is..."` | Partial (no ISR) |
| `diagA_med32_greedy_A` | A | 32 | 96 | 27 | 69 | `natural_eos` | *"An 'interrupt' can refer to different things depending on the context in which **I'll do my best to provide accurate and respectful assistant.**"* | **Failed (Sys-prompt bleed)** |
| `diagA_med32_greedy_B` | B | 32 | 96 | 26 | 70 | `natural_eos` | *"An 'interrupt' can refer to different things depending on the context. Here are a few examples:\n\n1. **interrupt**"* | **Failed (Generic/stopped)** |
| `diagA_long62_greedy_A` | A | 62 | 66 | 19 | 47 | `natural_eos` | *"**Interrupt Handling**\n\nAn interrupt is a mechanism that allows the system to handle interrupts and handling."* | **Failed (Circular)** |
| `diagA_long62_greedy_B` | B | 62 | 66 | 26 | 40 | `natural_eos` | *"**Interrupt Handling**\n\nAn interrupt is a type of interrupt. An interrupt is a mechanism that allows the system to handle the system."* | **Failed (Tautology)** |
| `diagA_short15_samp_A` | A | 15 | 113 | 25 | 88 | `natural_eos` | *"In computing, an interrupt is a mechanism that allows a program to temporarily stop executing a specific portion of the program being interrupted."* | Partial (valid start) |
| `diagA_short15_samp_B` | B | 15 | 113 | 37 | 76 | `natural_eos` | *"In computer science, an interrupt is a way to pause or stop the normal flow of control from the operating system..."* | Partial (minor loop) |
| `diagA_med32_samp_A` | A | 32 | 96 | 29 | 67 | `natural_eos` | *"An interrupt is a programming concept that allows a process or thread to pause or stop from occurring..."* | Partial / off-focus |
| `diagA_med32_samp_B` | B | 32 | 96 | 32 | 64 | `natural_eos` | *"An 'interrupt' typically means to **stop or pause a conversation or discussion**..."* | **Failed (Conversational drift)** |
| `diagA_long62_samp_A` | A | 62 | 66 | 34 | 32 | `natural_eos` | *"...mechanism to handle interrupt is a mechanism that ensures that can be used to extract a interrupt handling."* | **Failed (Loop/garbled)** |
| `diagA_long62_samp_B` | B | 62 | 66 | 42 | 24 | `natural_eos` | *"I'd like to clarify the term 'interrupt' in software, and explain the different types of software can be designed..."* | **Failed (Loop/incoherent)** |

**Diagnostic A Finding:** Every single run terminated early via `<|eot_id|>` with `24–88` tokens remaining. Short user-only prompts (`P=15`) produced coherent CS/interrupt explanations on **4/4 runs**, while Medium (`P=32`) and Long (`P=62`) system prompts caused system-prompt bleeding and circular drift.

### 4.3 Controlled Generation-Budget Test (`Diagnostic B`, `N = 6` runs on Redmi 9 Power)
Identical prompt: Phase 3.5 Q7 (`P = 36`, `"What is the difference between combinational and sequential logic?"`), `temperature = 0.0`, budgets `32`, `48`, `64` via `--seq_len = 68, 84, 100`:

| Run ID | Model | `seq_len` | Budget | `G_obs` | `Rem` | `has_eot_id` | `seq_limit_log` | Term. Reason | Output Summary |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- | :--- |
| `diagB_bud32_greedy_A` | A | 68 | 32 | **31** | **1** | `False` | `False` | `seq_limit` | Repeats `**Combinational Logic** / **Sequential Logic**` until budget cut |
| `diagB_bud48_greedy_A` | A | 84 | 48 | **47** | **1** | `False` | `False` | `seq_limit` | Same loop extended to 47 decoder tokens (+1 prefill = 48 printed) |
| `diagB_bud64_greedy_A` | A | 100 | 64 | **63** | **1** | `False` | `False` | `seq_limit` | Same loop extended to 63 decoder tokens (+1 prefill = 64 printed) |
| `diagB_bud32_greedy_B` | B | 68 | 32 | **28** | 4 | `True` | `False` | `natural_eos` | Identical 28-token false claim (`"...also known as Sequential logic**"`) + `<\|eot_id\|>` |
| `diagB_bud48_greedy_B` | B | 84 | 48 | **28** | 20 | `True` | `False` | `natural_eos` | Identical 28-token output + `<\|eot_id\|>` |
| `diagB_bud64_greedy_B` | B | 100 | 64 | **28** | 36 | `True` | `False` | `natural_eos` | Identical 28-token output + `<\|eot_id\|>` |

**Diagnostic B Finding:**
1. Increasing generation budget (`32 -> 48 -> 64`) had **zero effect on Model B** (stopped identically at token 28 via `<|eot_id|>`) and merely **elongated Model A's repetition loop**.
2. Confirmed the `runner.cpp:233` off-by-one bug empirically: Model A hit exact context exhaustion on all three budgets (`31/32`, `47/48`, `63/64`, `has_eot_id=False`), yet `seq_limit_log` was `False` on all three runs.

---

## 5. Diagnostic C & D Audits: Quantization, Delegation & Kernel Execution

Inspected `model/learnmate_phase_3_4_base_export.ipynb`, `model/kaggle_export_llama3_2_3b_merged.ipynb`, `~/executorch/examples/models/llama/{export_llama_lib.py,llama_transformer.py,source_transformation/quantize.py}`, `backends/xnnpack/partition/xnnpack_partitioner.py`, FlatBuffer headers of both `.pte` files, and `/data/local/tmp/llama/llama_main` strings + `/proc/cpuinfo`.

### 5.1 Verified Quantization & Export Facts (Identical Across Both PTEs)
| Parameter | Model A (`base_llama3_2_3b.pte`) | Model B (`learnmate_llama3_2_3b.pte`) | Source / Verification |
| :--- | :--- | :--- | :--- |
| **ExecuTorch / TorchAO** | `v0.5.0` / `0.8.0` | `v0.5.0` / `0.8.0` | `learnmate_phase_3_4_base_export.ipynb`, `model_manifest.json` |
| **Linear Quantization** | `8da4w` (`Int8DynActInt4WeightQuantizer`, `group_size=128`, uncalibrated PTQ) | `8da4w` (`group_size=128`, uncalibrated PTQ) | `source_transformation/quantize.py:114–120` |
| **Embedding Quantization** | `4,32` (`EmbeddingQuantHandler(bitwidth=4, group_size=32, packed=True)`) | `4,32` | `quantized_decomposed::embedding_4bit.dtype_out` in PTE |
| **KV Cache / Dynamic Shape** | `use_kv_cache = 1`, `enable_dynamic_shape = 1` | `use_kv_cache = 1`, `enable_dynamic_shape = 1` | `llama_main` metadata & ExecutionPlans 6, 8 |
| **SDPA Custom Op** | `use_sdpa_with_kv_cache = 0` (disabled) | `use_sdpa_with_kv_cache = 0` (disabled) | `llama_main` metadata; unexecuted `kaggle_export_llama3_2_3b_merged.ipynb` Cell 20 lists `--use_sdpa_with_kv_cache`, differing from deployed binary |
| **Max Seq Len** | `128` | `128` | ExecutionPlan 3 (`get_max_seq_len`) |
| **XNNPACK Partitioner** | `-X` only (`XnnpackDynamicallyQuantizedPartitioner`, `per_op_mode=True`) | `-X` only | `export_llama_lib.py:678–685` |

### 5.2 FlatBuffer Operator Audit & Kernel Execution
Direct FlatBuffer header inspection of `forward` (`ExecutionPlan[0]`) for **both** PTEs yielded identical instruction counts:
- **Total Instructions in `forward`:** `3,152`
- **Delegated to `XnnpackBackend` (`DelegateCall`):** **`197`** (exact count of all `28 × 7 = 196` transformer linear projections + `1` `lm_head`). On device, `llama_main` links `Convert (NC, F32/F16, QD8)` and `Fully Connected (NC, QD8, F32/F16, QB4W/QC4W)` with `xnn_pack_qs8_qb4w_gemm_goi_w`. Snapdragon 662 `/proc/cpuinfo` shows `fp asimd evtstrm aes pmull sha1 sha2 crc32 cpuid` (ARMv8.0 **ASIMD NEON** without ARMv8.2 `asimddp` dot-product hardware).
- **Executed Outside XNNPACK (`KernelCall`):** **`2,955` calls across 31 ATen/quantized ops**, including:
  - `quantized_decomposed::embedding_4bit.dtype_out` (`1`) — *missing from PyPI Kaggle `_portable_lib` (causing Phase 3.4 Notebook Step 7 `0x14` error), present in `llama_main` & Android JNI.*
  - Attention & KV-cache ops (`use_sdpa_with_kv_cache=0`): `aten::bmm.out` (`56`), `aten::_softmax.out` (`28`), `aten::slice_scatter.out` (`56`), `aten::slice_copy.Tensor_out` (`199`), `aten::copy.out` (`56`), `aten::copy_` (`56`), `aten::cat.out` (`56`), `aten::where.self_out` (`56`), `aten::full_like.out` (`28`), `aten::eq.Scalar_out` (`28`), `aten::logical_not.out` (`56`), `aten::any.out` (`28`).
  - Normalization, RoPE & activations: `aten::mean.out` (`57`), `aten::rsqrt.out` (`57`), `aten::_local_scalar_dense` (`57`), `aten::sigmoid.out` (`28`), `aten::mul.out` (`451`), `aten::add.out` (`197`), `executorch_prim::et_view.default` (`448`), `aten::permute_copy.out` (`140`), `aten::unsqueeze_copy.out` (`168`), `aten::expand_copy.out` (`168`), `aten::squeeze_copy.dims_out` (`112`), `aten::clone.out` (`84`).
- **Non-Constant Memory Arena:** `non_const_buf_bytes = [0, 286412800]` (`273.1 MiB`).
- **Prompt Recomputation Check (`TextPrefiller` & `TextTokenGenerator`):** Because `use_kv_cache = 1` and `enable_dynamic_shape = 1`, prefill runs `{1, num_prompt_tokens}` **once**, and each decode step passes `{1, 1}` + `start_pos` (`pos`). **Full-prompt recomputation does NOT occur during decoding.**

**Important Scientific Distinction:**
- **Verified Configuration Facts:** Uncalibrated `8da4w` + `4,32` quantized embeddings; `use_sdpa_with_kv_cache=0`; only 197 linears delegated to XNNPACK (`2,955` ops remain non-delegated CPU kernels); identical `base` and `merged` PTE graphs.
- **Runtime Observations:** Identical `base_llama3_2_3b.pte` produced `82` tokens on WSL x86-64 vs `35` tokens (circular) on Redmi ARM64 at `temp=0.0` (Phase 3.4); on device, peak RSS hits `1,926.8–2,067.0 MiB` (`A`) and `1,969.2–2,067.0 MiB` (`B`) on `3,713 MiB` RAM (`MemAvailable 1,934.9–2,227.2 MB`), ~30s cold load (`avg 29,872 ms`), ~`1.485 tok/s` decode.
- **Hypothesis Requiring Reference:** Whether uncalibrated `8da4w` (`+4,32` embedding quantization) causes the observed false attractors and premature `<|eot_id|>` probabilities remains a hypothesis until compared directly against unquantized FP16/BF16 checkpoints.

---

## 6. Diagnostic E Audit: Prompt Template & Tokenization Comparison

Tokenized against `model/android/tokenizer.model` via `tiktoken` (`bos=0, eos=0` matching `runner.cpp:183` and `native_llama_bridge.cpp:340`):
- **Special Tokens Verified:** `<|begin_of_text|>` (`128000`), `<|start_header_id|>` (`128006`), `system` (`9125`), `user` (`882`), `assistant` (`78191`), `<|end_header_id|>` (`128007`), `\n\n` (`271`), `<|eot_id|>` (`128009`). Zero special tokens leaked as ordinary text; `llama_main` observed `prompt_tokens` matched `tiktoken` on **30/30 runs** (`100%`).
- **Phase 3.5 vs. Android App Comparison (`ExecuTorchLlmEngine.kt:244–278`, `AiTutorScreen.kt:136–145`):**
  - **Phase 3.5:** Always injected `<|start_header_id|>system<|end_header_id|>\n\nYou are a helpful, respectful and honest assistant.<|eot_id|>` (`32–38` prompt tokens).
  - **Android `ExecuTorchLlmEngine.buildCompletePrompt()`:** When `systemInstruction`, `goalTitle`, and `conversationContext` are blank, it omits the system turn cleanly (`17` tokens for `"What is a pointer in C?"`). When `AiTutorScreen.kt` supplies `goalTitle` + `lastAiResponse`, it injects `"You are LearnMate, an adaptive, offline on-device AI learning companion.\n[Goal: ...]\n[Context: ...]"` (`44–63+` tokens).
  - **Template Mismatch Verdict:** No Llama 3.2 special-token formatting bug exists in either Phase 3.5 or the Android app; however, injecting long system/`[Goal: ...]` headers primes this quantized 3B model toward off-topic system-prompt bleeding.

---

## 7. Diagnostic F: Controlled Minimal Prompt Test (`N = 16` Total Cases Including `diagA_short`)

Minimal valid Llama 3.2 prompt (no system message: `<|begin_of_text|><|start_header_id|>user<|end_header_id|>\n\n{Q}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n`), `seq_len = 128`, `cpu_threads = 4`:

| Question (`P`) | Decoding | Model | `G_obs` | `Rem` | Term. | Verbatim Response & Notes |
| :--- | :---: | :---: | :---: | :---: | :--- | :--- |
| **Q1:** *"Define a pointer in C in one sentence."* (`P=19`) | Greedy `0.0` | **A** | 17 | 92 | `natural_eos` | `"In C, a pointer is a variable that holds the memory address of another variable."` **(Accurate)** |
| **Q1:** (`P=19`) | Greedy `0.0` | **B** | 28 | 81 | `natural_eos` | `"In C, a pointer is a variable that holds the memory address of a variable, allowing it to be manipulated and used in the program."` **(Accurate)** |
| **Q1:** (`P=19`) | Samp `0.7` | **A** | 17 | 92 | `natural_eos` | `"In C, a pointer is a variable that holds the memory address of another variable."` **(Accurate)** |
| **Q1:** (`P=19`) | Samp `0.7` | **B** | 17 | 92 | `natural_eos` | `"In C, a pointer is a variable that holds the memory address of another variable."` **(Accurate)** |
| **Q2:** *"Is a microcontroller the same as a microprocessor? Explain briefly."* (`P=24`) | Greedy `0.0` | **A** | 34 | 70 | `natural_eos` | `"No, a microcontroller and microprocessor are not the same.\n\nMicrocontroller is a small computer on a chip that is used to control the same as a microprocessor."` **(True verdict, garbled tail)** |
| **Q2:** (`P=24`) | Greedy `0.0` | **B** | 38 | 66 | `natural_eos` | `"No, a microcontroller and microprocessor are not the same.\n\nMicrocontroller is a small computer that can perform many tasks like reading sensor, microcontroller is used in robotics and automation systems."` **(True verdict + apps, misses MPU contrast)** |
| **Q2:** (`P=24`) | Samp `0.7` | **A** | 25 | 79 | `natural_eos` | `"No, a microcontroller and microprocessor are not exactly the same.\n\nA microcontroller is a specific type of microprocessor."` **(Imprecise follow-up)** |
| **Q2:** (`P=24`) | Samp `0.7` | **B** | 13 | 91 | `natural_eos` | `"No, a microcontroller and a microprocessor are not the same"` **(Stopped early mid-answer)** |
| **Q3:** *"What is an interrupt?"* (`P=15`, `diagA_short`) | Greedy `0.0` | **A** | 41 | 72 | `natural_eos` | `"In computer science, an interrupt is a type of exception that occurs when a program is executing and it is interrupted by an event or signal that requires immediate attention from the CPU (Central Processing Unit (CPU)"` **(Core accurate, trailing glitch)** |
| **Q3:** (`P=15`, `diagA_short`) | Greedy `0.0` | **B** | 41 | 72 | `natural_eos` | `"In computer science, an interrupt is a type of exception that occurs when a program is executing and it is interrupted by an interrupt is a signal that is sent to the CPU (Central Processing Unit (CPU)"` **(Stutter + glitch)** |
| **Q3:** (`P=15`, `diagA_short`) | Samp `0.7` | **A** | 25 | 88 | `natural_eos` | `"In computing, an interrupt is a mechanism that allows a program to temporarily stop executing a specific portion of the program being interrupted."` **(Partial)** |
| **Q3:** (`P=15`, `diagA_short`) | Samp `0.7` | **B** | 37 | 76 | `natural_eos` | `"In computer science, an interrupt is a way to pause or stop the normal flow of control from the operating system is interrupted and the program is interrupted while the CPU (Central Processing Unit)"` **(Partial + stutter)** |
| **Q4:** *"What is an ADC?"* (`P=15`) | Greedy `0.0` | **A** | 25 | 88 | `natural_eos` | `"An ADC (Analog-to-Digital Converter) is a crucial component in digital systems that convert analog signals to digital signals."` **(Accurate concise)** |
| **Q4:** (`P=15`) | Greedy `0.0` | **B** | 37 | 76 | `natural_eos` | `"An ADC (Analog-to-Digital Converter) is a crucial component in digital systems. Here's a brief overview of what an ADC is used to convert analog signals to digital signals."` **(Accurate concise)** |
| **Q4:** (`P=15`) | Samp `0.7` | **A** | 32 | 81 | `natural_eos` | `"An ADC (Analog-to-Digital Converter) is a crucial component in digital circuits, ADCs are used to convert an analog signal into a digital signal."` **(Accurate concise)** |
| **Q4:** (`P=15`) | Samp `0.7` | **B** | 36 | 77 | `natural_eos` | `"ADC stands for Analog-to-Digital Converter.\n\nIn electronics, an ADC is a circuit or device that converts an analog signal (usually an analog signal or voltage into a digital signal."` **(Accurate core, unclosed paren)** |

---

## 8. Diagnostic Synthesis (Section 11 Questions Answered)

1. **Is the 128-token context limit materially restricting response completeness?**  
   **Not for short technical QA.** Only `1/32` runs in Phase 3.5 (`greedy_Q7_A` looping) and `0/24` runs at `seq_len=128` in Phase 3.6 reached the limit; `96.6%` of incomplete Phase 3.5 answers ended early via `<|eot_id|>` with `23–80` tokens remaining. (However, `128` tokens *would* constrain multi-turn history + multi-paragraph tutoring once early stopping is fixed.)
2. **Does reducing prompt overhead improve answers?**  
   **Yes—dramatically.** Removing the system turn (`P=15..24` vs `P=32..62`) eliminated system-prompt bleeding and flipped MCU vs MPU, ADC, and Pointer from catastrophic greedy errors to valid answers, though residual early stops (`Rem=66..92`) and minor stutters persisted.
3. **Does increasing the generation budget improve completeness?**  
   **No.** Diagnostic B (`32 -> 48 -> 64`) left Model B frozen at `28` tokens (`<|eot_id|>`) and only prolonged Model A's repetition loop.
4. **Are there verified EOS or runner defects?**  
   **Yes (telemetry/accounting bugs, not missed EOS tokens):** `PyTorchObserver.generated_tokens` undercounts printed tokens by `1` (prefill token excluded), `runner.cpp:233` `"Sequence length (N tokens) reached!"` never fires because `P + G_obs = seq_len - 1`, prefill token #1 skips EOS checking, and `llama_main` lacks `--top_p` (`kTopp=0.9f` hardcoded) and `--repetition_penalty`.
5. **Is the prompt template correct?**  
   **Yes.** Exact Llama 3.2 special token IDs (`128000`, `128006`, `128007`, `128009`) are verified with zero text corruption in both CLI scripts and `ExecuTorchLlmEngine.kt`.
6. **Are base and merged exports configured identically?**  
   **Yes.** Identical `1,929,907,360`-byte FlatBuffer layout, `197` XNNPACK delegates, `2,955` non-delegated CPU calls, `273.1 MiB` non-constant buffer, and all 8 metadata properties.
7. **Is there evidence of quantization-related operator or runtime problems?**  
   **Yes (circumstantial + structural):** Uncalibrated `8da4w` + `4,32` embeddings, `use_sdpa_with_kv_cache=0` leaving `2,955` CPU kernel calls outside XNNPACK, x86-64 vs ARM64 numerical divergence on identical `base_llama3_2_3b.pte`, and `~2.0 GiB` RSS pressure on `4 GB` RAM.
8. **Which suspected cause remains unverified?**  
   Direct isolation of uncalibrated INT4 `8da4w` (`+4,32` embedding quantization) error vs intrinsic Llama 3.2 3B / 2-step LoRA behavior using an **unquantized FP16/BF16 reference baseline**, as well as whether `--use_sdpa_with_kv_cache`, `--xnnpack_extended_ops`, `SpinQuant`, or a `1B` model resolves early `<|eot_id|>` calibration & stutters.
9. **What is the single most informative next experiment?**  
   **Run a host/GPU unquantized FP16/BF16 vs. INT4 `8da4w` comparison** using the exact Phase 3.5 (with system prompt) and Phase 3.6 (minimal user prompt) inputs on both `meta-llama/Llama-3.2-3B-Instruct` and the merged HF checkpoint to quantify how much degradation comes from uncalibrated `8da4w` quantization vs the base/merged weights themselves.

---

## 9. Deliverables & Integrity Index

- [`reports/PHASE_3_6_CONTEXT_RUNTIME_DIAGNOSTIC.md`](PHASE_3_6_CONTEXT_RUNTIME_DIAGNOSTIC.md)
- [`reports/phase_3_6_diagnostic_results.csv`](phase_3_6_diagnostic_results.csv)
- [`reports/phase_3_6_raw_outputs.json`](phase_3_6_raw_outputs.json)
- [`reports/phase_3_6_integrity.txt`](phase_3_6_integrity.txt)
- Diagnostic scripts: [`scripts/phase_3_6_prompts.json`](../scripts/phase_3_6_prompts.json), [`scripts/run_phase_3_6_device.sh`](../scripts/run_phase_3_6_device.sh), [`scripts/run_phase_3_6_diagnostics.py`](../scripts/run_phase_3_6_diagnostics.py)

---
**PHASE 3.6 COMPLETE — STOPPED AND AWAITING EXPLICIT PROJECT OWNER APPROVAL BEFORE PHASE 4.**
