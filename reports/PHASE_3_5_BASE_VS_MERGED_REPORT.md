# LearnMate AI — Phase 3.5 Report: Controlled Base vs Merged Model Evaluation on Redmi 9 Power

**Project:** LearnMate AI  
**Phase:** 3.5 (Evaluation-Only)  
**Date:** 2026-11-25  
**Target Device:** Xiaomi Redmi 9 Power (`M2010J19SI`, Snapdragon 662, 4 GB RAM, Android 12 / API 31)  
**Runtime:** ExecuTorch v0.5.0, XNNPACK NEON (`cpu_threads=4`, `seq_len=128`, INT4 `8da4w`)  
**Evaluation Status:** Completed (`32 / 32` runs, zero crashes, all exit codes `0`)  

> ## Final Verdict: `INCONCLUSIVE: MORE EVIDENCE REQUIRED`
>
> **Summary of Findings:** The controlled on-device comparison refutes the hypothesis that the 2-step LoRA merge caused the degradation seen in Phase 3.2. On identical hardware, prompts, and decoding parameters, **Model A (unmerged original base `meta-llama/Llama-3.2-3B-Instruct`, INT4 `8da4w`)** scored **9.75 / 25 (39.0%)** overall, performing **worse** than **Model B (2-step LoRA merged, INT4 `8da4w`)**, which scored **12.81 / 25 (51.2%)** overall (Model B won **8/16** pairs, tied **6/16**, lost **2/16**). Both models exhibited identical catastrophic failure modes—including false definitions (*"A microcontroller and a microprocessor are actually the same thing"* on Q4 greedy and *"a null pointer is a pointer that has been dereferenced"* on Q6 greedy), acronym corruption, repetition loops, and premature `<|eot_id|>` termination. Because severe `8da4w` quantization/export distortion (`39.0%–51.2%` quality ceiling), a `128`-token context limit, unsupported sampling flags (`--top_p`, `--repetition_penalty`), and the absence of an unquantized FP16/BF16 reference baseline obscure the underlying adapter signal, the intrinsic merit of the 2-step LoRA adapter remains **INCONCLUSIVE: MORE EVIDENCE REQUIRED** (though neither exported artifact is viable for student educational deployment today).

---

## 1. Objective & Scope

Phase 3.5 isolates whether LearnMate AI's current two-step LoRA adapter improves, preserves, degrades, or has negligible effect on `meta-llama/Llama-3.2-3B-Instruct` when both models undergo identical ExecuTorch v0.5.0 INT4 `8da4w` (`group_size=128`, embedding `4,32`) export and run on physical hardware under identical prompts and decoding configurations.

Per Phase 3.5 restrictions, this phase is strictly **evaluation-only**:
- No training, fine-tuning, LoRA merging, or model export occurred.
- Production artifacts (`model/android/learnmate_llama3_2_3b.pte` and `tokenizer.model`), Android source code, JNI C++ bridge, and `/data/local/tmp/llama/llama_main` were not modified.

---

## 2. Baseline, Hardware & Artifact Integrity

Full verification records are also archived in [`reports/phase_3_5_integrity.txt`](phase_3_5_integrity.txt).

### 2.1 Git & Device Environment
| Parameter | Observed Value |
| :--- | :--- |
| **Git Branch** | `phase-3.2-quality-eval` |
| **Git HEAD Commit** | `e21c34086433481aa5189fb4ab35a33cc3ab69a7` |
| **Working Tree** | Pre-existing Phase 3.1–3.4 dirty/untracked files; zero Android/JNI/model edits in Phase 3.5 |
| **ADB Device Serial** | `<DEVICE_SERIAL>` |
| **Device Model / OS** | `M2010J19SI` (Xiaomi Redmi 9 Power), Android `12` (SDK `31`) |
| **Memory (`/proc/meminfo`)** | `MemTotal: 3802456 kB` (~3713 MB), `MemFree` pre-eval `599208 kB`, `MemAvailable` pre-eval `1939848 kB` |
| **Storage (`df -h /data`)** | `/dev/block/sda11`: `47G` size, `25G` used, `22G` available (`55%` used) |
| **Runner Binary & Hash** | `/data/local/tmp/llama/llama_main` — `f6b533465ac75639f115ce7fcf8b418d2ec0a4dc5c235a217d21c7c9dca91b76` |
| **CPU Threads / Runtime** | `4` threads (`cpu_threads=4`), ExecuTorch `v0.5.0` (XNNPACK NEON) |

### 2.2 Model & Tokenizer SHA-256 Verification
All local and on-device SHA-256 digests matched expected reference hashes **before and after** all 32 evaluation runs:

| Artifact | Device Path | Local Path | Verified SHA-256 | Match |
| :--- | :--- | :--- | :--- | :--- |
| **Model A (Base)** | `/data/local/tmp/llama/base_llama3_2_3b.pte` | `model/experimental/base_llama3_2_3b.pte` | `2b855085703e6a1743437edb6adf467f7f95f9d23f810d8fd196175630979407` | YES |
| **Model B (Merged)** | `/data/local/tmp/llama/learnmate_llama3_2_3b.pte` | `model/android/learnmate_llama3_2_3b.pte` | `8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91` | YES |
| **Shared Tokenizer** | `/data/local/tmp/llama/tokenizer.model` | `model/android/tokenizer.model` | `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` | YES |

### 2.3 Confirmed Runtime Metadata (Logged by `llama_main`)
Observed directly from `llama_main` execution logs across all 16 Model A runs and all 16 Model B runs:

| Metadata Method | Model A (`base_llama3_2_3b.pte`) | Model B (`learnmate_llama3_2_3b.pte`) | Parity |
| :--- | :--- | :--- | :--- |
| `get_vocab_size` | `128256` | `128256` | Identical |
| `get_bos_id` | `128000` | `128000` | Identical |
| `eos_ids` | `[128009, 128001]` | `[128009, 128001]` | Identical |
| `get_max_seq_len` | `128` | `128` | Identical |
| `use_kv_cache` | `1` | `1` | Identical |
| `use_sdpa_with_kv_cache` | `0` | `0` | Identical |
| `enable_dynamic_shape` | `1` | `1` | Identical |

---

## 3. Experimental Matrix & Prompt Tokenization

### 3.1 Controlled Protocol
- **Matrix:** 8 standard technical questions × 2 models (A, B) × 2 decoding conditions = **32 runs**.
- **Primary Condition (Greedy, Runs 1–16):** `--temperature 0.0`, `--seq_len 128`, `--cpu_threads 4`.
- **Secondary Condition (Sampling, Runs 17–32):** `--temperature 0.7`, `--seq_len 128`, `--cpu_threads 4`.
  - **Runner Sampling Limitation:** Inspected `llama_main --help` on device; ExecuTorch v0.5.0 `examples/models/llama/main.cpp` implements `--temperature` only and **does not support `--top_p` or `--repetition_penalty`**. Per Section 3 & Section 5 rules, `llama_main` was not modified and identical `--temperature 0.7` flags were applied to both models.
- **Bias Mitigation & Thermal Control:** Alternating A/B question order (`Q1: A->B`, `Q2: B->A`, `Q3: A->B`, ...), cold process launch per run, `5`-second cooldown pause between runs.
- **Prompt Verification:** Generated via [`scripts/generate_phase_3_5_prompts.py`](../scripts/generate_phase_3_5_prompts.py) into [`scripts/phase_3_5_prompts.json`](../scripts/phase_3_5_prompts.json) using standard Llama 3.2 Instruct chat formatting:
  ```text
  <|begin_of_text|><|start_header_id|>system<|end_header_id|>

  You are a helpful, respectful and honest assistant.<|eot_id|><|start_header_id|>user<|end_header_id|>

  {QUESTION}<|eot_id|><|start_header_id|>assistant<|end_header_id|>

  ```
  Every run's on-device `prompt_tokens` log matched the `tiktoken` pre-check token counts with **zero discrepancies**.

### 3.2 Prompt Token IDs & Budgets (All 8 Questions)

| QID | Domain | Prompt Tokens | Max Gen Budget | Full Prompt Token IDs (`128000` BOS ... `[128006, 78191, 128007, 271]` Assistant Header) |
| :--- | :--- | :---: | :---: | :--- |
| **Q1** | C Programming | 32 | 96 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 3923, 374, 264, 7597, 304, 356, 30, 128009, 128006, 78191, 128007, 271]` |
| **Q2** | Computer Architecture | 34 | 94 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 3923, 374, 279, 6811, 1990, 22813, 323, 31786, 30, 128009, 128006, 78191, 128007, 271]` |
| **Q3** | Embedded Systems | 33 | 95 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 3923, 374, 459, 12956, 304, 23711, 6067, 30, 128009, 128006, 78191, 128007, 271]` |
| **Q4** | Microcontroller Architecture | 38 | 90 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 3923, 374, 279, 6811, 1990, 264, 8162, 7299, 323, 264, 8162, 30574, 30, 128009, 128006, 78191, 128007, 271]` |
| **Q5** | Signal Conversion | 37 | 91 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 849, 21435, 279, 3318, 315, 459, 22858, 304, 459, 23711, 1887, 13, 128009, 128006, 78191, 128007, 271]` |
| **Q6** | Memory Addressing | 38 | 90 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 3923, 8741, 994, 264, 854, 7597, 374, 26344, 809, 5886, 304, 356, 30, 128009, 128006, 78191, 128007, 271]` |
| **Q7** | Digital Electronics | 36 | 92 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 3923, 374, 279, 6811, 1990, 68225, 1697, 323, 52100, 12496, 30, 128009, 128006, 78191, 128007, 271]` |
| **Q8** | Communication Systems | 35 | 93 | `[128000, 128006, 9125, 128007, 271, 2675, 527, 264, 11190, 11, 49150, 323, 10978, 18328, 13, 128009, 128006, 882, 128007, 271, 3923, 374, 279, 7580, 315, 67547, 304, 10758, 6067, 30, 128009, 128006, 78191, 128007, 271]` |

---

## 4. Aggregate Quantitative Comparison

All 32 outputs were shuffled and anonymized (`BLIND_01`–`BLIND_32` via [`scripts/blind_dump_3_5.py`](../scripts/blind_dump_3_5.py)) prior to scoring across 5 equally weighted axes (0–5 each, max **25 points**): **Factual Correctness**, **Relevance**, **Completeness**, **Clarity**, and **Hallucination Control**.

| Slice | N | Factual (/5) | Relevance (/5) | Completeness (/5) | Clarity (/5) | HallucCtrl (/5) | Overall (/25) | Quality % | Incorrect | Incomplete | Rep. Loops | Natural EOS | Avg Load (ms) | Avg TTFT (ms) | Avg Rate (tok/s) | Avg Gen Tok |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Model A (Base) — Greedy (`T=0.0`)** | 8 | 1.12 | 2.50 | 0.50 | 2.25 | 1.75 | **8.12** | **32.5%** | 3/8 (37.5%) | 8/8 (100%) | 4/8 (50%) | 7/8 (87.5%) | 29,887 | 6,328 | 1.476 | 45.6 |
| **Model B (Merged) — Greedy (`T=0.0`)** | 8 | 1.75 | 2.75 | 1.00 | 2.75 | 2.38 | **10.62** | **42.5%** | 4/8 (50.0%) | 7/8 (87.5%) | 2/8 (25%) | 8/8 (100%) | 30,166 | 6,495 | 1.502 | 29.4 |
| **Model A (Base) — Sampling (`T=0.7`)** | 8 | 2.38 | 2.50 | 0.88 | 2.50 | 3.12 | **11.38** | **45.5%** | 2/8 (25.0%) | 7/8 (87.5%) | 3/8 (37.5%) | 8/8 (100%) | 30,117 | 6,378 | 1.432 | 31.6 |
| **Model B (Merged) — Sampling (`T=0.7`)** | 8 | 3.12 | 2.75 | 1.12 | 3.50 | 4.50 | **15.00** | **60.0%** | 0/8 (0.0%) | 7/8 (87.5%) | 1/8 (12.5%) | 8/8 (100%) | 30,737 | 7,485 | 1.367 | 28.6 |
| **Model A (Base) — OVERALL** | **16** | **1.75** | **2.50** | **0.69** | **2.38** | **2.44** | **9.75** | **39.0%** | **5/16 (31.2%)** | **15/16 (93.8%)** | **7/16 (43.8%)** | **15/16 (93.8%)** | **30,002** | **6,353** | **1.454** | **38.6** |
| **Model B (Merged) — OVERALL** | **16** | **2.44** | **2.75** | **1.06** | **3.12** | **3.44** | **12.81** | **51.2%** | **4/16 (25.0%)** | **14/16 (87.5%)** | **3/16 (18.8%)** | **16/16 (100%)** | **30,451** | **6,990** | **1.435** | **29.0** |

**Key Quantitative Observations:**
1. **Model B outperformed Model A in both decoding regimes:** `+2.50` points under Greedy (`10.62` vs `8.12`) and `+3.62` points under Sampling (`15.00` vs `11.38`), for an overall gap of **`+3.06` points (`51.2%` vs `39.0%`)**.
2. **Sampling (`temp=0.7`) significantly outperformed Greedy (`temp=0.0`) for both models:** Model A improved `+3.26` points (`+13.0` pct pts) and Model B improved `+4.38` points (`+17.5` pct pts), eliminating factual errors on Model B Sampling (`0/8` technically incorrect) though `7/8` answers still stopped prematurely before completing all expected concepts.
3. **High failure floor on both artifacts:** Even the best slice (`Model B - Sampling`) achieved only `60.0%` quality with `87.5%` incomplete answers; overall `15/16` (A) and `14/16` (B) runs failed to cover complete expected concepts.

---

## 5. Paired Question-by-Question Comparison

| Decoding | QID | Topic | Model A Score (Gen Tok) | Model B Score (Gen Tok) | Diff (`B - A`) | Pair Winner | Key Behavioral Difference |
| :--- | :--- | :--- | :---: | :---: | :---: | :--- | :--- |
| Greedy | Q1 | Pointer in C | 14/25 (35) | **25/25** (17) | **+11** | **B (Merged)** | A gives circular definition; B gives exact core definition |
| Greedy | Q2 | RAM vs ROM | 16/25 (28) | 16/25 (29) | 0 | **TIE** | Both expand acronyms cleanly, then stop on `<|eot_id|>` before volatility |
| Greedy | Q3 | Interrupt | 6/25 (47) | **9/25** (44) | **+3** | **B (Merged)** | A outputs tautology loop; B mentions task interruption before looping |
| Greedy | Q4 | MCU vs MPU | 7/25 (17) | 7/25 (17) | 0 | **TIE** | **Identical verbatim hallucination:** *"actually the same thing."* |
| Greedy | Q5 | ADC working | 5/25 (61) | **12/25** (35) | **+7** | **B (Merged)** | A garbles acronym (`Analog-to-digital-to-digital...`); B stops at heading |
| Greedy | Q6 | Null pointer deref | 6/25 (16) | 6/25 (16) | 0 | **TIE** | **Identical verbatim error:** *"a null pointer is a pointer that has been dereferenced."* |
| Greedy | Q7 | Comb vs Seq logic | **5/25** (91) | 4/25 (28) | **-1** | **A (Base)** | A loops `**Sequential Logic**` 13+ times to ctx limit; B contradicts directly |
| Greedy | Q8 | Modulation | 6/25 (70) | 6/25 (49) | 0 | **TIE** | Both wrongly assign *"1. Error Correction"* as modulation's purpose |
| **Greedy Tally** | — | **N=8 pairs** | **65/200** | **85/200** | **+20** | **B: 3, Tie: 4, A: 1** | — |
| Sampling | Q1 | Pointer in C | 25/25 (18) | 25/25 (17) | 0 | **TIE** | Both deliver accurate, concise, complete core definitions |
| Sampling | Q2 | RAM vs ROM | 13/25 (14) | **16/25** (31) | **+3** | **B (Merged)** | A stops immediately after acronyms; B adds hardware purpose sentence |
| Sampling | Q3 | Interrupt | 10/25 (31) | 10/25 (39) | 0 | **TIE** | Both repeat prompt text and terminate early without explaining ISR |
| Sampling | Q4 | MCU vs MPU | **15/25** (15) | 13/25 (18) | **-2** | **A (Base)** | A gives *"integrated circuits (ICs)"* intro; B gives *"digital computers"* intro |
| Sampling | Q5 | ADC working | 5/25 (58) | **13/25** (25) | **+8** | **B (Merged)** | A garbles acronym (`Analogical conversion`); B stops at clean preamble |
| Sampling | Q6 | Null pointer deref | 7/25 (27) | **14/25** (16) | **+7** | **B (Merged)** | A misdefines null pointer; B defines absence of pointer cleanly (misses UB) |
| Sampling | Q7 | Comb vs Seq logic | 5/25 (49) | **14/25** (41) | **+9** | **B (Merged)** | A loops `Combinomial Logic`; B acknowledges circuit types & stops early |
| Sampling | Q8 | Modulation | 11/25 (41) | **15/25** (42) | **+4** | **B (Merged)** | A falsely claims modulation *amplifies*; B describes modifying carrier wave |
| **Sampling Tally** | — | **N=8 pairs** | **91/200** | **120/200** | **+29** | **B: 5, Tie: 2, A: 1** | — |
| **TOTAL** | — | **N=16 pairs** | **156/400** | **205/400** | **+49** | **B: 8, Tie: 6, A: 2** | — |

---

## 6. Complete Raw Outputs & Blinded Scores (All 32 Runs)

### 6.1 Primary Evaluation: Greedy Decoding (`temp = 0.0`)

#### Q1 — "What is a pointer in C?" (`P = 32` tokens)
- **Model A (`greedy_Q1_A`, `BLIND_31`, Order 1):** `G=35`, `Tot=67`, `term=natural_eos`, `load=30151ms`, `ttft=5701ms`, `gen=23390ms`, `1.496 tok/s`, `RSS=2012.5 MiB`, `Temp=32.9->33.4°C`
  - **Scores:** Factual `2`, Rel `4`, Comp `2`, Clar `3`, Halluc `3` -> **`14/25`** | Flags: `circular_definition`
  - **Explanation:** Circular phrasing (*"Here's a pointer is a variable that holds a reference to a pointer"*) instead of variable address.
  - **Output:**
    ```text
    **Pointers in C**
    ================

    In C, pointers are a fundamental concept in C programming. Here's a pointer is a variable that holds a reference to a pointer.
    ```
- **Model B (`greedy_Q1_B`, `BLIND_18`, Order 2):** `G=17`, `Tot=49`, `term=natural_eos`, `load=30125ms`, `ttft=5775ms`, `gen=11339ms`, `1.499 tok/s`, `RSS=2038.1 MiB`, `Temp=33.5->33.8°C`
  - **Scores:** Factual `5`, Rel `5`, Comp `5`, Clar `5`, Halluc `5` -> **`25/25`** | Flags: none
  - **Explanation:** Clear, accurate, complete core definition stating memory address storage of another variable.
  - **Output:**
    ```text
    In C, a pointer is a variable that holds the memory address of another variable.
    ```

#### Q2 — "What is the difference between RAM and ROM?" (`P = 34` tokens)
- **Model B (`greedy_Q2_B`, `BLIND_21`, Order 3):** `G=29`, `Tot=63`, `term=natural_eos`, `load=29597ms`, `ttft=5741ms`, `gen=18811ms`, `1.542 tok/s`, `RSS=1972.2 MiB`, `Temp=33.7->34.2°C`
  - **Scores:** Factual `3`, Rel `3`, Comp `1`, Clar `4`, Halluc `5` -> **`16/25`** | Flags: `incomplete_answer`, `premature_termination`
  - **Explanation:** Correctly expands RAM/ROM and notes different functions, but terminates before explaining volatility or roles.
  - **Output:**
    ```text
    RAM (Random Access Memory) and ROM (Read-Only Memory) are two types of computer memory that serves different purposes and has different functions.
    ```
- **Model A (`greedy_Q2_A`, `BLIND_24`, Order 4):** `G=28`, `Tot=62`, `term=natural_eos`, `load=29764ms`, `ttft=5780ms`, `gen=18021ms`, `1.554 tok/s`, `RSS=2067.1 MiB`, `Temp=34.1->34.5°C`
  - **Scores:** Factual `3`, Rel `3`, Comp `1`, Clar `4`, Halluc `5` -> **`16/25`** | Flags: `incomplete_answer`, `premature_termination`
  - **Explanation:** Correctly expands RAM/ROM and notes distinct purposes, stopping before stating volatility/non-volatility.
  - **Output:**
    ```text
    RAM (Random Access Memory) and ROM (Read-Only Memory) are two types of computer memory that serves different purposes in the computer.
    ```

#### Q3 — "What is an interrupt in embedded systems?" (`P = 33` tokens)
- **Model A (`greedy_Q3_A`, `BLIND_26`, Order 5):** `G=47`, `Tot=80`, `term=natural_eos`, `load=29748ms`, `ttft=5767ms`, `gen=31352ms`, `1.499 tok/s`, `RSS=2056.4 MiB`, `Temp=34.4->34.7°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `0`, Clar `1`, Halluc `2` -> **`6/25`** | Flags: `circular_definition`, `repetition_loop`
  - **Explanation:** Circular tautology (*"An interrupt is a type of interrupt, but what is an interrupt?"*).
  - **Output:**
    ```text
    An interrupt is a crucial concept in embedded systems. So, let's break it down:

    **What is an interrupt?**

    An interrupt is a type of interrupt, but what is an interrupt?

    An interrupt is a type of interrupt?
    ```
- **Model B (`greedy_Q3_B`, `BLIND_02`, Order 6):** `G=44`, `Tot=77`, `term=natural_eos`, `load=30683ms`, `ttft=5636ms`, `gen=28205ms`, `1.560 tok/s`, `RSS=2067.2 MiB`, `Temp=34.7->35.0°C`
  - **Scores:** Factual `2`, Rel `3`, Comp `1`, Clar `1`, Halluc `2` -> **`9/25`** | Flags: `repetition_loop`
  - **Explanation:** Mentions interrupting a task, then degrades into a repetitive stuttering loop; omits hardware signal and ISR.
  - **Output:**
    ```text
    An interrupt in the context of embedded systems, an interrupt is a mechanism that allows a task or process to be interrupted or stopped from executing a task that is being interrupted or stopped from executing a task that is being interrupted.
    ```

#### Q4 — "What is the difference between a microcontroller and a microprocessor?" (`P = 38` tokens)
- **Model B (`greedy_Q4_B`, `BLIND_08`, Order 7):** `G=17`, `Tot=55`, `term=natural_eos`, `load=29668ms`, `ttft=7191ms`, `gen=11506ms`, `1.477 tok/s`, `RSS=2000.3 MiB`, `Temp=35.0->35.2°C`
  - **Scores:** Factual `0`, Rel `3`, Comp `0`, Clar `4`, Halluc `0` -> **`7/25`** | Flags: `technically_incorrect`, `contradiction`, `unsupported_claim`
  - **Explanation:** Explicitly false claim (*"A microcontroller and a microprocessor are actually the same thing."*).
  - **Output:**
    ```text
    A fundamental question!

    A microcontroller and a microprocessor are actually the same thing.
    ```
- **Model A (`greedy_Q4_A`, `BLIND_32`, Order 8):** `G=17`, `Tot=55`, `term=natural_eos`, `load=30409ms`, `ttft=7255ms`, `gen=11390ms`, `1.493 tok/s`, `RSS=2067.2 MiB`, `Temp=35.1->35.3°C`
  - **Scores:** Factual `0`, Rel `3`, Comp `0`, Clar `4`, Halluc `0` -> **`7/25`** | Flags: `technically_incorrect`, `contradiction`, `unsupported_claim`
  - **Explanation:** Explicitly false statement (*"A microcontroller and a microprocessor are actually the same thing."*).
  - **Output:**
    ```text
    A fundamental question!

    A microcontroller and a microprocessor are actually the same thing.
    ```

#### Q5 — "Explain the working of an ADC in an embedded system." (`P = 37` tokens)
- **Model A (`greedy_Q5_A`, `BLIND_29`, Order 9):** `G=61`, `Tot=98`, `term=natural_eos`, `load=29561ms`, `ttft=7305ms`, `gen=39655ms`, `1.538 tok/s`, `RSS=2067.4 MiB`, `Temp=35.2->35.6°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `0`, Clar `1`, Halluc `1` -> **`5/25`** | Flags: `repetition_loop`, `garbled_acronyms`
  - **Explanation:** Enters phrase repetition and corrupts acronym into `'Analog-to-digital-to-digital-to-digital'`.
  - **Output:**
    ```text
    An Analog-to-Digital (ADC) converter is a crucial component in many electronic systems, particularly in analog-to-digital (ADC) converter is a crucial component in many electronic systems, particularly in the context of an ADC (Analog-to-digital-to-digital-to-digital (ADC) converter.
    ```
- **Model B (`greedy_Q5_B`, `BLIND_12`, Order 10):** `G=35`, `Tot=72`, `term=natural_eos`, `load=30131ms`, `ttft=7318ms`, `gen=23539ms`, `1.487 tok/s`, `RSS=2067.2 MiB`, `Temp=35.5->35.7°C`
  - **Scores:** Factual `3`, Rel `2`, Comp `0`, Clar `2`, Halluc `5` -> **`12/25`** | Flags: `premature_termination`, `incomplete_sentence`
  - **Explanation:** Introduces ADC, then cuts off at an unclosed bold heading before explaining working steps.
  - **Output:**
    ```text
    An Analog-to-Digital Converter (ADC) is a crucial component in many embedded systems. Let's break down the ADC.

    **Analog-to-Digital Converter (ADC)
    ```

#### Q6 — "What happens when a null pointer is dereferenced in C?" (`P = 38` tokens)
- **Model B (`greedy_Q6_B`, `BLIND_03`, Order 11):** `G=16`, `Tot=54`, `term=natural_eos`, `load=30323ms`, `ttft=7356ms`, `gen=10401ms`, `1.538 tok/s`, `RSS=2067.5 MiB`, `Temp=35.6->35.7°C`
  - **Scores:** Factual `0`, Rel `2`, Comp `0`, Clar `3`, Halluc `1` -> **`6/25`** | Flags: `technically_incorrect`, `unsupported_claim`
  - **Explanation:** False statement (*"a null pointer is a pointer that has been dereferenced"*); misses UB/segmentation fault.
  - **Output:**
    ```text
    In C, a null pointer is a pointer that has been dereferenced.
    ```
- **Model A (`greedy_Q6_A`, `BLIND_06`, Order 12):** `G=16`, `Tot=54`, `term=natural_eos`, `load=29667ms`, `ttft=7229ms`, `gen=10236ms`, `1.563 tok/s`, `RSS=2067.2 MiB`, `Temp=35.7->35.9°C`
  - **Scores:** Factual `0`, Rel `2`, Comp `0`, Clar `3`, Halluc `1` -> **`6/25`** | Flags: `technically_incorrect`, `unsupported_claim`
  - **Explanation:** False definition (*"a null pointer is a pointer that has been dereferenced"*); fails to explain dereferencing fault.
  - **Output:**
    ```text
    In C, a null pointer is a pointer that has been dereferenced.
    ```

#### Q7 — "What is the difference between combinational and sequential logic?" (`P = 36` tokens)
- **Model A (`greedy_Q7_A`, `BLIND_10`, Order 13):** `G=91`, `Tot=127`, `term=context_limit`, `load=29908ms`, `ttft=5784ms`, `gen=72573ms`, `1.254 tok/s`, `RSS=2067.4 MiB`, `Temp=35.8->36.0°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `0`, Clar `1`, Halluc `1` -> **`5/25`** | Flags: `repetition_loop`, `incomplete_answer`, `context_limit_termination`
  - **Explanation:** Repeats `**Sequential Logic**` 13+ times until hitting 128-token context limit without explaining either logic type.
  - **Output:**
    ```text
    A fundamental question in digital electronics!

    **Combinational Logic (AND) vs. Sequential Logic**

    **Combinational Logic**

    **Sequential Logic**

    Sequential Logic**

    **Combinational Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    **Sequential Logic**

    I think you are looking at a **Sequential Logic**Sequential Logic
    ```
- **Model B (`greedy_Q7_B`, `BLIND_19`, Order 14):** `G=28`, `Tot=64`, `term=natural_eos`, `load=29772ms`, `ttft=5867ms`, `gen=18237ms`, `1.535 tok/s`, `RSS=2059.1 MiB`, `Temp=36.0->36.1°C`
  - **Scores:** Factual `0`, Rel `2`, Comp `0`, Clar `2`, Halluc `0` -> **`4/25`** | Flags: `technically_incorrect`, `contradiction`, `broken_markdown`
  - **Explanation:** Directly contradicts question by claiming Combinational Logic is *"also known as Sequential logic"*.
  - **Output:**
    ```text
    A fundamental question in digital electronics!

    **Combinational Logic** (also known as **Sequential** logic** is a type of logic.
    ```

#### Q8 — "What is the purpose of modulation in communication systems?" (`P = 35` tokens)
- **Model B (`greedy_Q8_B`, `BLIND_14`, Order 15):** `G=49`, `Tot=84`, `term=natural_eos`, `load=31028ms`, `ttft=7076ms`, `gen=35642ms`, `1.375 tok/s`, `RSS=1823.3 MiB`, `Temp=36.0->36.2°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `1`, Clar `1`, Halluc `1` -> **`6/25`** | Flags: `repetition_loop`, `technically_incorrect`
  - **Explanation:** Wrongly claims Error Correction as modulation's purpose and loops into asking the prompt question.
  - **Output:**
    ```text
    Modulation is a crucial process in communication systems that enables the efficient use of modulation is to achieve the following purposes:

    1. **Error Correction**: In the context of communication systems, modulation is used to achieve the purpose of modulation in communication systems?
    ```
- **Model A (`greedy_Q8_A`, `BLIND_04`, Order 16):** `G=70`, `Tot=105`, `term=natural_eos`, `load=29887ms`, `ttft=5805ms`, `gen=49674ms`, `1.409 tok/s`, `RSS=2024.9 MiB`, `Temp=36.2->36.2°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `1`, Clar `1`, Halluc `1` -> **`6/25`** | Flags: `repetition_loop`, `technically_incorrect`, `incomplete_sentence`
  - **Explanation:** Conflates modulation with error correction and repeats numbered section headers verbatim.
  - **Output:**
    ```text
    Modulation is a crucial process in communication systems that enables the efficient use of modulation is to achieve the following purposes:

    1. **Error Correction**: In the context of communication systems, modulation is used to achieve the following purposes:

    1. **Error Correction**: In the context of communication systems that are used to achieve the following the purpose of communication systems.
    ```

---

### 6.2 Secondary Evaluation: Sampling (`temp = 0.7`)

#### Q1 — "What is a pointer in C?" (`P = 32` tokens)
- **Model A (`sampling_Q1_A`, `BLIND_11`, Order 17):** `G=18`, `Tot=50`, `term=natural_eos`, `load=29562ms`, `ttft=5825ms`, `gen=11742ms`, `1.533 tok/s`, `RSS=2067.2 MiB`, `Temp=36.2->36.2°C`
  - **Scores:** Factual `5`, Rel `5`, Comp `5`, Clar `5`, Halluc `5` -> **`25/25`** | Flags: none
  - **Explanation:** Concise, factually accurate, well-formed definition covering memory address storage of another variable.
  - **Output:**
    ```text
    In C programming, a pointer is a variable that stores the memory address of another variable.
    ```
- **Model B (`sampling_Q1_B`, `BLIND_22`, Order 18):** `G=17`, `Tot=49`, `term=natural_eos`, `load=30552ms`, `ttft=5761ms`, `gen=11045ms`, `1.539 tok/s`, `RSS=2066.9 MiB`, `Temp=36.2->36.2°C`
  - **Scores:** Factual `5`, Rel `5`, Comp `5`, Clar `5`, Halluc `5` -> **`25/25`** | Flags: none
  - **Explanation:** Accurate, concise core definition stating memory address storage of a variable.
  - **Output:**
    ```text
    In C, a pointer is a variable that stores the memory address of a variable.
    ```

#### Q2 — "What is the difference between RAM and ROM?" (`P = 34` tokens)
- **Model B (`sampling_Q2_B`, `BLIND_20`, Order 19):** `G=31`, `Tot=65`, `term=natural_eos`, `load=29576ms`, `ttft=5760ms`, `gen=20182ms`, `1.536 tok/s`, `RSS=2067.1 MiB`, `Temp=36.2->36.3°C`
  - **Scores:** Factual `3`, Rel `3`, Comp `1`, Clar `4`, Halluc `5` -> **`16/25`** | Flags: `incomplete_answer`, `premature_termination`
  - **Explanation:** Correctly expands RAM/ROM acronyms and notes computer hardware role, but omits volatility and differences.
  - **Output:**
    ```text
    RAM (Random Access Memory) and ROM (Read-Only Memory) are two types of computer memory that serves as a crucial role in the computer hardware.
    ```
- **Model A (`sampling_Q2_A`, `BLIND_09`, Order 20):** `G=14`, `Tot=48`, `term=natural_eos`, `load=30222ms`, `ttft=5895ms`, `gen=9536ms`, `1.468 tok/s`, `RSS=2067.2 MiB`, `Temp=36.3->36.3°C`
  - **Scores:** Factual `3`, Rel `2`, Comp `0`, Clar `3`, Halluc `5` -> **`13/25`** | Flags: `premature_termination`, `incomplete_sentence`
  - **Explanation:** Only expands RAM and ROM acronyms and terminates immediately without comparing volatility or roles.
  - **Output:**
    ```text
    RAM (Random Access Memory) and ROM (Read-Only Memory)
    ```

#### Q3 — "What is an interrupt in embedded systems?" (`P = 33` tokens)
- **Model A (`sampling_Q3_A`, `BLIND_16`, Order 21):** `G=31`, `Tot=64`, `term=natural_eos`, `load=29754ms`, `ttft=5831ms`, `gen=20103ms`, `1.542 tok/s`, `RSS=2067.2 MiB`, `Temp=36.3->36.4°C`
  - **Scores:** Factual `2`, Rel `2`, Comp `0`, Clar `2`, Halluc `4` -> **`10/25`** | Flags: `repetition_loop`, `premature_termination`
  - **Explanation:** Merely repeats the prompt question as a bold heading twice with no technical explanation.
  - **Output:**
    ```text
    An interrupt in the context of embedded systems.

    **What is an interrupt in embedded systems?**

    **What is an interrupt in the context of embedded systems?
    ```
- **Model B (`sampling_Q3_B`, `BLIND_23`, Order 22):** `G=39`, `Tot=72`, `term=natural_eos`, `load=33799ms`, `ttft=14827ms`, `gen=33887ms`, `1.151 tok/s`, `RSS=1748.8 MiB`, `Temp=36.4->36.7°C`
  - **Scores:** Factual `2`, Rel `2`, Comp `0`, Clar `2`, Halluc `4` -> **`10/25`** | Flags: `repetition_loop`, `broken_markdown`, `premature_termination`
  - **Explanation:** Repeats question text and terminates on an isolated bold `**interrupt**` token.
  - **Output:**
    ```text
    An interrupt is a fundamental concept in the context of embedded systems.

    **What is an interrupt in the context of embedded systems.

    An interrupt is a programming concept in embedded systems?

    An **interrupt**
    ```

#### Q4 — "What is the difference between a microcontroller and a microprocessor?" (`P = 38` tokens)
- **Model B (`sampling_Q4_B`, `BLIND_07`, Order 23):** `G=18`, `Tot=56`, `term=natural_eos`, `load=31358ms`, `ttft=7235ms`, `gen=12794ms`, `1.407 tok/s`, `RSS=2067.3 MiB`, `Temp=36.7->36.7°C`
  - **Scores:** Factual `3`, Rel `2`, Comp `0`, Clar `4`, Halluc `4` -> **`13/25`** | Flags: `incomplete_answer`, `premature_termination`
  - **Explanation:** Valid generic statement (*"both types of digital computers"*), but omits all differences in memory/peripherals.
  - **Output:**
    ```text
    Microcontrollers and microprocessors are both types of digital computers that are used in various applications.
    ```
- **Model A (`sampling_Q4_A`, `BLIND_30`, Order 24):** `G=15`, `Tot=53`, `term=natural_eos`, `load=32273ms`, `ttft=7285ms`, `gen=10221ms`, `1.468 tok/s`, `RSS=2067.0 MiB`, `Temp=36.7->36.6°C`
  - **Scores:** Factual `4`, Rel `2`, Comp `0`, Clar `4`, Halluc `5` -> **`15/25`** | Flags: `incomplete_answer`, `premature_termination`
  - **Explanation:** Accurate introductory classification (*"both types of integrated circuits"*), stops before differentiating.
  - **Output:**
    ```text
    Microcontrollers and microprocessors are both types of integrated circuits (ICs)
    ```

#### Q5 — "Explain the working of an ADC in an embedded system." (`P = 37` tokens)
- **Model A (`sampling_Q5_A`, `BLIND_15`, Order 25):** `G=58`, `Tot=95`, `term=natural_eos`, `load=29570ms`, `ttft=7192ms`, `gen=44462ms`, `1.304 tok/s`, `RSS=2067.4 MiB`, `Temp=36.6->36.7°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `0`, Clar `1`, Halluc `1` -> **`5/25`** | Flags: `repetition_loop`, `garbled_acronyms`, `incomplete_answer`
  - **Explanation:** Degrades into `'Analog-digital-to-Digital-to-digital (Analogical conversion'` without explaining ADC stages.
  - **Output:**
    ```text
    An Analog-to-Digital (ADC) conversion is a crucial process in embedded systems. In this response, I'll explain the process of ADC (Analog-digital-to-Digital-to-digital (Analogical conversion is a process in an embedded system is typically used in embedded system.
    ```
- **Model B (`sampling_Q5_B`, `BLIND_05`, Order 26):** `G=25`, `Tot=62`, `term=natural_eos`, `load=30656ms`, `ttft=7301ms`, `gen=18700ms`, `1.337 tok/s`, `RSS=2067.0 MiB`, `Temp=36.7->36.7°C`
  - **Scores:** Factual `3`, Rel `2`, Comp `0`, Clar `3`, Halluc `5` -> **`13/25`** | Flags: `premature_termination`, `incomplete_answer`
  - **Explanation:** Correctly introduces ADC, then terminates mid-preamble before explaining sampling, quantization, or encoding.
  - **Output:**
    ```text
    An Analog-to-Digital Converter (ADC) is a crucial component in many embedded systems. Let's break down the working principle
    ```

#### Q6 — "What happens when a null pointer is dereferenced in C?" (`P = 38` tokens)
- **Model B (`sampling_Q6_B`, `BLIND_01`, Order 27):** `G=16`, `Tot=54`, `term=natural_eos`, `load=30166ms`, `ttft=7358ms`, `gen=10686ms`, `1.497 tok/s`, `RSS=2067.3 MiB`, `Temp=36.7->36.7°C`
  - **Scores:** Factual `3`, Rel `2`, Comp `1`, Clar `4`, Halluc `4` -> **`14/25`** | Flags: `incomplete_answer`, `premature_termination`
  - **Explanation:** Defines null pointer conceptually as absence of a pointer, but misses dereferencing consequences (UB/crash).
  - **Output:**
    ```text
    In C, a null pointer is used to represent the absence of a pointer.
    ```
- **Model A (`sampling_Q6_A`, `BLIND_27`, Order 28):** `G=27`, `Tot=65`, `term=natural_eos`, `load=29802ms`, `ttft=7358ms`, `gen=20485ms`, `1.318 tok/s`, `RSS=2067.1 MiB`, `Temp=36.7->36.7°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `0`, Clar `2`, Halluc `2` -> **`7/25`** | Flags: `technically_incorrect`, `incomplete_sentence`
  - **Explanation:** Mischaracterizes null pointer (*"pointer that is not used"*) and omits undefined behavior/crash.
  - **Output:**
    ```text
    In C, a null pointer is a pointer that is not used, but it's dereferenced.

    Null pointer dereference operations.
    ```

#### Q7 — "What is the difference between combinational and sequential logic?" (`P = 36` tokens)
- **Model A (`sampling_Q7_A`, `BLIND_13`, Order 29):** `G=49`, `Tot=85`, `term=natural_eos`, `load=29837ms`, `ttft=5863ms`, `gen=34548ms`, `1.418 tok/s`, `RSS=2030.6 MiB`, `Temp=36.7->36.8°C`
  - **Scores:** Factual `1`, Rel `2`, Comp `0`, Clar `1`, Halluc `1` -> **`5/25`** | Flags: `repetition_loop`, `technically_incorrect`, `broken_markdown`
  - **Explanation:** Equates combinational logic with AND logic, repeats headings, and emits corrupted spelling (`Combinomial`).
  - **Output:**
    ```text
    A fundamental concept in digital electronics!

    **Combinational Logic (AND Logic)**

    **Sequential Logic**

    **Sequential Logic**

    In sequential logic, also known as sequential logic

    **Combinomial Logic**

    Combinomial logic**

    **T**
    ```
- **Model B (`sampling_Q7_B`, `BLIND_25`, Order 30):** `G=41`, `Tot=77`, `term=natural_eos`, `load=30088ms`, `ttft=5817ms`, `gen=33928ms`, `1.208 tok/s`, `RSS=2067.2 MiB`, `Temp=36.8->36.8°C`
  - **Scores:** Factual `3`, Rel `2`, Comp `0`, Clar `4`, Halluc `5` -> **`14/25`** | Flags: `incomplete_answer`, `premature_termination`
  - **Explanation:** Acknowledges two circuit types and offers to help, but stops without stating the difference.
  - **Output:**
    ```text
    A fundamental question in the realm of digital logic, combinational and sequential logic are two types of digital circuits. However, I'd be happy to help you understand the difference between combinational and sequential logic.
    ```

#### Q8 — "What is the purpose of modulation in communication systems?" (`P = 35` tokens)
- **Model B (`sampling_Q8_B`, `BLIND_17`, Order 31):** `G=42`, `Tot=77`, `term=natural_eos`, `load=29700ms`, `ttft=5822ms`, `gen=33231ms`, `1.264 tok/s`, `RSS=2059.4 MiB`, `Temp=36.8->36.8°C`
  - **Scores:** Factual `3`, Rel `4`, Comp `2`, Clar `2`, Halluc `4` -> **`15/25`** | Flags: `grammatical_stutter`, `incomplete_answer`
  - **Explanation:** Touches carrier wave modification and encoding information, marred by grammatical stutter (*"injecting a modulate"*).
  - **Output:**
    ```text
    Modulation is a fundamental concept in communication systems. 

    Modulation is a process of modifying a carrier wave by injecting a modulate a signal onto a physical signal to encode information onto a physical, electrical signals.
    ```
- **Model A (`sampling_Q8_A`, `BLIND_28`, Order 32):** `G=41`, `Tot=76`, `term=natural_eos`, `load=29918ms`, `ttft=5774ms`, `gen=29163ms`, `1.406 tok/s`, `RSS=2015.3 MiB`, `Temp=36.8->36.8°C`
  - **Scores:** Factual `2`, Rel `3`, Comp `2`, Clar `2`, Halluc `2` -> **`11/25`** | Flags: `unsupported_claim`, `repetition`
  - **Explanation:** Mentions transmitting/encoding info, but falsely claims modulation amplifies signals and omits carrier frequency.
  - **Output:**
    ```text
    Modulation is a crucial process in communication systems that serves to amplify, transmit, and encode information into a communication system, which is to convey information to be transmitted, and to be used to carry information.
    ```

---

## 7. Failure-Mode, Context-Limit & Termination Investigation

1. **Both Base & Merged Suffer Identical Greedy Hallucinations:**
   - **Q4 Greedy (`greedy_Q4_A` & `greedy_Q4_B`):** Both unmerged base and merged models emitted the exact same 17 tokens ending in `"A microcontroller and a microprocessor are actually the same thing."`
   - **Q6 Greedy (`greedy_Q6_A` & `greedy_Q6_B`):** Both emitted the exact same 16 tokens: `"In C, a null pointer is a pointer that has been dereferenced."`
   - **Q8 Greedy (`greedy_Q8_A` & `greedy_Q8_B`):** Both began identically (*"Modulation is a crucial process in communication systems that enables the efficient use of modulation..."*) and hallucinated *"1. **Error Correction**"*.
   - **Implication:** These catastrophic errors originate in the shared `8da4w` (`group_size=128`, `4,32` quantized embedding, `use_sdpa_with_kv_cache=0`) quantized weights/export graph of Llama 3.2 3B, **not** from the 2-step LoRA adapter.

2. **Context Window (`seq_len = 128`) vs Premature EOS:**
   - **31 of 32 runs (`96.9%`)** stopped via **`natural_eos` (`<|eot_id|>`)**, NOT context-limit truncation! Total tokens used for those 31 runs ranged from `48` to `105` out of `128`.
   - Only **`greedy_Q7_A`** hit the `context_limit` ceiling (`36` prompt + `91` generated = `127` tokens) while looping `**Sequential Logic**` 13+ times.
   - Instead of hitting the 128-token wall cleanly after giving an answer, both models frequently emit `<|eot_id|>` right after an introductory sentence (`Q2`, `Q4`, `Q5`, `Q7`)—showing corrupted probability calibration / early stop-token attraction under INT4 `8da4w`.

3. **Why Sampling (`temp = 0.7`) Beat Greedy (`temp = 0.0`):**
   - Under `temp = 0.0`, the argmax path gets trapped in low-entropy repetitive attractors (e.g., Q3 circular definitions, Q5 `Analog-to-digital-to-digital`, Q7 `**Sequential Logic**` loop) and the shared false greedy attractors on Q4/Q6/Q8.
   - Injecting mild stochasticity (`temp = 0.7`) broke the deterministic wrong attractors cleanly—lifting Model A from `32.5%` to `45.5%` and Model B from `42.5%` to `60.0%`, eliminating Q1 circular phrasing on Base A (`14 -> 25`) and dropping Model B factual errors from `4/8` to `0/8`.

---

## 8. Runtime Performance, Memory (RSS) & Thermal Telemetry

| Metric | Model A (`base_llama3_2_3b.pte`) | Model B (`learnmate_llama3_2_3b.pte`) |
| :--- | :--- | :--- |
| **Model Load Time (ms)** | Avg `30,002` (Range `29,561 – 32,273`) | Avg `30,451` (Range `29,576 – 33,799`) |
| **Time to First Token / Prefill (ms)** | Avg `6,353` (Range `5,701 – 7,358`) | Avg `6,990` (Range `5,636 – 14,827`*) |
| **Decode Rate (tok/s)** | Avg `1.454` (Range `1.254 – 1.563`) | Avg `1.435` (Range `1.151* – 1.560`) |
| **RSS After Load (MiB)** | Min `2012.5`, Max `2067.4`, Avg `2054.9` | Min `1578.1`, Max `2067.5`, Avg `2008.5` |
| **RSS After Prefill (MiB)** | Min `2012.5`, Max `2067.4`, Avg `2054.9` | Min `1728.2`, Max `2067.5`, Avg `2017.8` |
| **RSS After Gen (MiB)** | Min `2012.5`, Max `2067.4`, Avg `2054.9` | Min `1748.8`, Max `2067.5`, Avg `2019.1` |
| **Process Exit Status** | `0` (`16/16` clean exits) | `0` (`16/16` clean exits) |

*\*Note:* Run 22 (`sampling_Q3_B`) experienced OS page-reclaim/swap paging during cold load on the 4 GB RAM device (`RSS after load = 1578.1 MiB -> prefill = 1728.2 MiB -> gen = 1748.8 MiB`), temporarily increasing load time to `33,799 ms`, TTFT to `14,827 ms`, and lowering throughput to `1.151 tok/s`.

- **Device RAM (`MemAvailable`):** Remained between `2057.54 MB` and `2257.89 MB` pre-run (`2084.67 – 2263.27 MB` post-run); peak resident footprint of ~2,067 MiB represents heavy memory pressure on a 3,713 MiB device (`1,209 MB` swap free post-eval), explaining ~30s mmap load times.
- **Thermal Drift:** Battery temperature rose smoothly from `32.9°C` (pre-Run 1) to `36.8°C` (post-Run 32), while PMIC `pmi632-tz` remained `37.0°C`, indicating minor gradual warming without thermal throttling crashes.

---

## 9. Android Application UI Parity & Validation Status

- **Validation Status:** **`NOT TESTED` (Read-Only Architectural Audit Completed)**
- Read-only inspection of [`ExecuTorchLlmEngine.kt`](../android-app/app/src/main/java/com/learnmate/ai/domain/engine/ExecuTorchLlmEngine.kt) and [`ModelSelectionManager.kt`](../android-app/app/src/main/java/com/learnmate/ai/domain/engine/ModelSelectionManager.kt) confirmed:
  1. `ExecuTorchLlmEngine.findModelFile()` prioritizes exact match `learnmate_llama3_2_3b.pte` inside `getExternalFilesDir("models")` (`L160–186`). There is **no UI toggle or runtime argument** in the app to select `base_llama3_2_3b.pte`.
  2. `ModelSelectionManager.kt` rejects 3B models (`isPermitted = false`) on 4 GB RAM devices (<5.8 GB threshold).
  3. On device, `/storage/emulated/0/Android/data/com.learnmate.ai/files/models/` contains `learnmate_llama3_2_3b.pte` and `tokenizer.model`.
- Testing Model A inside the Android APK would require either modifying Kotlin application code or overwriting the production model file—both explicitly prohibited by Section 3.

---

## 10. Limitations

1. **Missing Runner Sampling Controls:** `llama_main` v0.5.0 ignores/lacks `--top_p` and `--repetition_penalty`; sampling could only test `--temperature 0.7`.
2. **Severe Quantization Noise (`8da4w` uncalibrated):** Both models fail >39%–51% overall, producing identical nonsensical greedy answers, which compresses dynamic range and masks subtle fine-tuning behavior.
3. **No Unquantized FP16/BF16 Baseline:** Neither Model A nor Model B was evaluated in FP16 on desktop/GPU alongside this `8da4w` comparison to separate LoRA fine-tuning shift from INT4 quantization shift.
4. **Small Sample Size & Short Context:** `N = 8` technical questions and `seq_len = 128` (`89–96` max generated tokens) restrict multi-paragraph educational explanations.
5. **Android UI Parity Untested for Model A:** Blocked by Section 3 restrictions (no app selector or production overwrite permitted).

---

## 11. Conclusion & Recommendations

### Formal Verdict: `INCONCLUSIVE: MORE EVIDENCE REQUIRED`

**Why `INCONCLUSIVE: MORE EVIDENCE REQUIRED` (and NOT `DISCARD CURRENT ADAPTER` purely on blame):**
- **What IS proven:** The 2-step LoRA adapter **did NOT degrade** the base model relative to the unmerged `base_llama3_2_3b.pte` export. On the contrary, Model B (`12.81/25`, `51.2%`) scored **+3.06 points higher** than Model A (`9.75/25`, `39.0%`), winning 8/16 pairs vs 2/16, with fewer repetition loops (`18.8%` vs `43.8%`) and `0/8` factual errors under sampling (`temp=0.7`) vs `2/8` on Base A.
- **Why more evidence is required:** Both `base_llama3_2_3b.pte` and `learnmate_llama3_2_3b.pte` suffer severe quality collapse under the current uncalibrated INT4 `8da4w` export (`group_size=128`, `4,32` quantized embedding, `use_sdpa_with_kv_cache=0`, `128` seq len), hitting identical corrupted greedy attractors. Quantization damage dominates the variance (`32.5%` greedy accuracy on the untouched Meta Llama 3.2 3B Instruct base!), and we lack both (a) an unquantized FP16 comparison of Base vs 2-step Merged checkpoints and (b) a calibrated/higher-precision or smaller-parameter (`1B` 4-bit/SpinQuant/QAT) baseline where the model can finish full explanations before judging LoRA adapter efficacy. Neither current 3B PTE is ready for student tutoring in production.

### Recommended Next Steps (For Phase 4 Approval)
1. **Verify Base vs Merged in Unquantized FP16/BF16 (Host/Kaggle):** Run the exact 8 questions against unquantized `meta-llama/Llama-3.2-3B-Instruct` vs the merged HF checkpoint to separate true LoRA effects from INT4 quantization collapse.
2. **Re-evaluate Model Size & Quantization Recipe for 4 GB RAM:** Investigate `Llama-3.2-1B-Instruct` (fits ~4 GB RAM well under `ModelSelectionManager` limits with higher token speed and larger `seq_len`), SpinQuant/GPTQ/QAT calibration, and `use_sdpa_with_kv_cache=True` instead of uncalibrated 3B `8da4w`.
3. **Complete Properly Trained Curriculum Adapter (If Fine-Tuning Continues):** Replace the 2-step smoke-test LoRA with a converged curriculum fine-tune validated at FP16 before on-device quantization.

---

## 12. Phase 3.5 Artifacts Index

- Integrity & Environment Log: [`reports/phase_3_5_integrity.txt`](phase_3_5_integrity.txt)
- Full Raw JSON (all 32 runs + metadata + scores): [`reports/phase_3_5_raw_outputs.json`](phase_3_5_raw_outputs.json)
- Tabular Results CSV: [`reports/phase_3_5_results.csv`](phase_3_5_results.csv)
- Prompt Definitions & Token IDs: [`scripts/phase_3_5_prompts.json`](../scripts/phase_3_5_prompts.json)
- Blinded Mapping: [`scripts/blind_map_3_5.json`](../scripts/blind_map_3_5.json)

---
**PHASE 3.5 COMPLETE — STOPPED AND AWAITING EXPLICIT PROJECT OWNER APPROVAL BEFORE PHASE 4.**
