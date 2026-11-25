# LearnMate AI | Phase 3.4 Final Report
## Original Base Meta Llama 3.2 3B Instruct Download and ExecuTorch Export

**Project:** LearnMate AI  
**Phase:** 3.4  
**Date:** 2026-11-25  
**Model:** `meta-llama/Llama-3.2-3B-Instruct` (Original Unmerged Base)  
**Runtime:** ExecuTorch `v0.5.0` (XNNPACK CPU Delegate, `8da4w`)  
**Target Device:** Xiaomi Redmi 9 Power (`<DEVICE_SERIAL>`, Snapdragon 662, ARM64-v8a, 4 GB RAM, Android 12)  
**Executed Notebook:** [`model/learnmate_phase_3_4_base_export.ipynb`](../model/learnmate_phase_3_4_base_export.ipynb) (Kaggle Version 5, Version ID `373902701`)

---

## 1. Executive Summary

Phase 3.4 successfully acquired the original, unmerged **Meta Llama 3.2 3B Instruct** checkpoint from the authorized Hugging Face repository (`meta-llama/Llama-3.2-3B-Instruct`), converted its `3,212,749,824` base FP16 parameters without attaching or merging any LoRA adapter, and exported it via ExecuTorch `v0.5.0` (`8da4w`, `group_size=128`, `embedding-quantize=4,32`, `max_seq_length=128`, `-X`) to **`base_llama3_2_3b.pte`**.

The exported artifact (`1,929,907,360` bytes, SHA-256 `2b855085703e6a1743437edb6adf467f7f95f9d23f810d8fd196175630979407`) achieves **100% structural and metadata parity** with the existing production merged model (`learnmate_llama3_2_3b.pte`), including identical file size (`1,929,907,360` bytes), identical FlatBuffer format (`ET12`), identical KV-cache/SDPA flags (`use_kv_cache=1`, `use_sdpa_with_kv_cache=0`), and identical BOS/EOS/tokenizer bindings. Both the local WSL ExecuTorch host runtime (`llama_main`) and the physical Redmi 9 Power standalone runner (`/data/local/tmp/llama/llama_main`) loaded and executed `base_llama3_2_3b.pte` cleanly with exit status `0`, while keeping the existing production model and tokenizer untouched.

---

## 2. Repository and Checkpoint Provenance

- **Repository ID:** `meta-llama/Llama-3.2-3B-Instruct`
- **Canonical Revision / Commit SHA:** `0cb88a4f764b7a12671c53f0838cd831a0843b95`
- **Last Modified:** `2024-10-24 15:07:29+00:00`
- **Gated Status:** `manual`
- **Authenticated Account:** `1sahil7` (`type=user`, token retrieved from Kaggle Secrets and never logged)
- **License:** *Llama 3.2 Community License Agreement* (Release Date: September 25, 2024; `LICENSE.txt` and `USE_POLICY.md` verified and downloaded alongside checkpoint)
- **Adapter Attachment:** **None** (`0` LoRA/PEFT parameters; `3,212,749,824` base model parameters loaded directly in `torch.float16` on `cuda:0`)

---

## 3. Downloaded File List, Sizes & SHA-256 Hashes

Verified directly in Step 3 of Kaggle Version 5 ([`model/learnmate_phase_3_4_base_export.ipynb`](../model/learnmate_phase_3_4_base_export.ipynb)):

| File Path (HF Repo) | Exact Size (Bytes) | SHA-256 Hash |
| :--- | :---: | :--- |
| `LICENSE.txt` | 7,712 | `0b4284c1f87029e67654c7953afa16279961632cf73dcfe33374c4c2f298fa35` |
| `USE_POLICY.md` | 6,021 | `40e2777d7faa6beaf98400654170f414d8ab29b921b5163ad4ea0a1d39894201` |
| `config.json` | 878 | `39fb36dc5416f445ebc4e71cb71fbcf6727e80a35836d8ba1a1474c318467b7a` |
| `generation_config.json` | 189 | `88effbb63300dbbc7390143fbbdd9d9fa50587b37e8bfd16c8c90d4970a74a36` |
| `model-00001-of-00002.safetensors` | 4,965,799,096 | `13cbd6d16e927a0c5bad54102514e6e18b4a47b3a6eb911e39d678d328d19f55` |
| `model-00002-of-00002.safetensors` | 1,459,729,952 | `7b770216613ac5c34d7c54bdff1fa616bc4e338a9d0b20af6303e48c295ee23c` |
| `model.safetensors.index.json` | 20,919 | `0a57c30afc07c81610ae7fe013bea3b9d50365f9a8145ae98e8714220f3d7c7c` |
| `special_tokens_map.json` | 296 | `6f38c73729248f6c127296386e3cdde96e254636cc58b4169d3fd32328d9a8ec` |
| `tokenizer.json` | 9,085,657 | `79e3e522635f3171300913bb421464a87de6222182a0570b9b2ccba2a964b2b4` |
| `tokenizer_config.json` | 54,528 | `9823dcfdc1121869029da45192238e85cf44f0b232a6d9dc20e4fe6f4242a14e` |
| `original/params.json` | 220 | `0aa31c4da011c8cb7ab8d40dfe4402971426063038f622001e34008ce452e953` |
| `original/tokenizer.model` | 2,183,982 | `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` |

*Tokenizer Verification:* Downloaded `original/tokenizer.model` matches the production `model/android/tokenizer.model` byte-for-byte (`2,183,982 bytes`, SHA-256 `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55`).

---

## 4. Export Environment

### Kaggle Cloud Export Environment (`Version 5`, `scriptVersionId=373902701`)
- **Platform:** `Linux-6.18.55+-x86_64-with-glibc2.39`
- **GPU:** `Tesla T4` (`Compute Capability 7.5`, `14.46 GB Free / 14.56 GB Total VRAM`)
- **System RAM:** `29.73 GB Available / 31.35 GB Total`
- **Container Host Python / PyTorch:** Python `3.13.16`, PyTorch `2.11.0+cu130` (used for Step 5 FP16 `cuda:0` consolidation into `consolidated.00.pth`, `7,213,587,081 bytes`, 255 Meta tensors)
- **Isolated `uv` Export Toolchain (`/tmp/et_py312`):**
  - Python: `3.12.3`
  - PyTorch: `2.6.0+cpu`
  - TorchAO: `0.8.0`
  - ExecuTorch: `0.5.0` (official `executorch==0.5.0` cp312 wheel matching `v0.5.0` tag `484a4ab`)

### Local Host Runtime Environment (WSL `Ubuntu-24.04`)
- **Host ExecuTorch Source:** `$HOME/executorch` (`v0.5.0`, commit `484a4ab`)
- **Host Runner Binary:** `$HOME/executorch/cmake-out-host/examples/models/llama/llama_main` (ELF 64-bit x86-64, built with `cmake 3.28.3`, `g++ 13.3.0`, XNNPACK + quantized + optimized + custom kernels)

---

## 5. Exact Export Configuration

Executed in Step 6 (`562.3s`, exit code `0`):
```bash
/tmp/et_py312/bin/python -m executorch.examples.models.llama.export_llama \
  --model llama3_2 \
  --checkpoint /tmp/base_llama3_2_3b_meta/consolidated.00.pth \
  --params /tmp/base_llama3_2_3b_meta/params.json \
  -kv \
  -qmode 8da4w \
  --group_size 128 \
  --embedding-quantize 4,32 \
  --max_seq_length 128 \
  -X \
  --metadata '{"get_bos_id": 128000, "get_eos_ids": [128009, 128001]}' \
  --output_name /kaggle/working/export/base_llama3_2_3b.pte
```

### Verified Parity vs. Existing Production Merged Artifact

| Parameter / Metadata Field | `learnmate_llama3_2_3b.pte` (Merged) | `base_llama3_2_3b.pte` (Unmerged Base) | Parity |
| :--- | :---: | :---: | :---: |
| **Architecture (`dim` / `layers` / `heads` / `kv_heads`)** | `3072 / 28 / 24 / 8` | `3072 / 28 / 24 / 8` | **EXACT** |
| **RoPE Scaling (`use_scaled_rope` / `factor` / `theta`)** | `true / 32 / 500000.0` | `true / 32 / 500000.0` | **EXACT** |
| **Quantization** | `8da4w` (`group_size=128`, `emb=4,32`) | `8da4w` (`group_size=128`, `emb=4,32`) | **EXACT** |
| **Delegate** | `XNNPACK (-X)` | `XNNPACK (-X)` | **EXACT** |
| **`use_kv_cache`** | `1` | `1` | **EXACT** |
| **`use_sdpa_with_kv_cache`** | `0` | `0` | **EXACT** |
| **`enable_dynamic_shape`** | `1` | `1` | **EXACT** |
| **`get_max_seq_len`** | `128` | `128` | **EXACT** |
| **`get_vocab_size`** | `128256` | `128256` | **EXACT** |
| **`get_bos_id`** | `128000` | `128000` | **EXACT** |
| **`get_eos_ids`** | `[128009, 128001]` | `[128009, 128001]` | **EXACT** |
| **File Size (Bytes)** | `1,929,907,360` | `1,929,907,360` | **EXACT** |
| **FlatBuffer Identifier / Root Offset** | `ET12` | `ET12` (`60`) | **EXACT** |

---

## 6. PTE Size and Cryptographic Integrity

- **Kaggle Export Path:** `/kaggle/working/export/base_llama3_2_3b.pte`
- **Local Experimental Path:** [`model/experimental/base_llama3_2_3b.pte`](../model/experimental/base_llama3_2_3b.pte)
- **Device Experimental Path:** `/data/local/tmp/llama/base_llama3_2_3b.pte`
- **Size:** `1,929,907,360 bytes` (`1,840.50 MiB`)
- **SHA-256 (Kaggle / Local / Redmi):** `2b855085703e6a1743437edb6adf467f7f95f9d23f810d8fd196175630979407` (**3-way verified match**)

---

## 7. Host Runtime Smoke-Test Results (WSL x86-64 `llama_main`)

Executed using `$HOME/executorch/cmake-out-host/examples/models/llama/llama_main` (`--seq_len 128 --temperature 0 --cpu_threads 4`) with standard Llama 3.2 Instruct formatting on `"What is a pointer in C?"`:

- **Exit Code:** `0`
- **Prompt Tokens:** `32`
- **Generated Tokens:** `82`
- **Model Load Time:** `21.203 s` (`1795605405264 - 1795605384061 = 21,203 ms`)
- **Time to First Token (TTFT):** `1.153 s` (`1,153 ms`)
- **Total Inference Time:** `21.534 s` (`21,534 ms`)
- **Generation Throughput:** `~4.02 tok/s` (`82 tokens / 20.381 s`)
- **EOS Termination:** Reached `<|eot_id|>` cleanly
- **Raw Output:**
  > `**Pointers in C**\n\nIn C, a pointer is a variable that holds the memory address of a variable. It can be used to store and manipulate the memory address of a variable. It can be used to store and manipulate the memory address of a variable. It can be used to store and manipulate the memory address of a variable. It can be used to store and manipulate the memory of a variable.<|eot_id|>`

---

## 8. Physical-Device Smoke-Test Results (Xiaomi Redmi 9 Power)

Transferred `base_llama3_2_3b.pte` via ADB (`35.3 MB/s`, `52.176 s`) to `/data/local/tmp/llama/base_llama3_2_3b.pte` and executed using the unmodified standalone runner `/data/local/tmp/llama/llama_main` (`--seq_len=128 --temperature=0 --cpu_threads=4`):

- **Exit Code:** `0` (`Reached to the end of generation`)
- **Runner Metadata Verified:** `use_sdpa_with_kv_cache = 0`, `use_kv_cache = 1`, `get_vocab_size = 128256`, `get_bos_id = 128000`, `get_max_seq_len = 128`, `enable_dynamic_shape = 1`, `eos_id = 128009`, `eos_id = 128001`
- **Model Load Time:** `32.489 s`
- **Prompt Tokens:** `32`
- **Generated Tokens:** `35`
- **Time to First Token (TTFT):** `7.115 s` (`4.50 tok/s` prompt eval)
- **Total Inference Time:** `35.946 s` (`0.974 tok/s` overall)
- **Generation Rate:** `1.214 tok/s` (`35` tokens in `28.831 s`)
- **Memory Footprint (RSS):** `1,929.22 MiB` (stable after load, after prefill, and after generation; zero OOM or LMK instability)
- **Raw Output:**
  > `**Pointers in C**\n================\n\nIn C, pointers are a fundamental concept in C programming. Here's a pointer is a variable that holds a reference to a pointer.<|eot_id|>`

Raw smoke-test telemetry for both host and device runs is stored in [`reports/phase_3_4_base_model_smoke_test.json`](phase_3_4_base_model_smoke_test.json).

---

## 9. Iteration Failures & Scope Limitations Encountered

1. **Kaggle Container Environment Drift (Resolved):**
   - Kaggle's updated container runs Python `3.13.16` and PyTorch `2.11.0+cu130`, causing `Version 1`–`Version 3` failures (`mesonpy` missing under Python 3.13, `editable_wheel` `cmake_cache_dir` bug in `v0.5.0` `setup.py`, and CMake `4.4.5` removing `< 3.5` policy compatibility).
   - Resolved cleanly in `Version 5` using an isolated Python `3.12.3` `uv` environment with `torch==2.6.0`, `torchao==0.8.0`, `cmake==3.31.6`, and the official PyPI `executorch==0.5.0` wheel.
2. **Kaggle Step 7 Python Runtime Operator Limitation (Mitigated on Host/Device):**
   - Kaggle `Version 5` Step 7 called `executorch.runtime.Runtime.get().load_program()` using the PyPI `_portable_lib` bindings, which lack `quantized_decomposed::embedding_4bit.dtype_out`. Because Step 6 had already written and verified `base_llama3_2_3b.pte` (`ET12`, `1,929,907,360 bytes`), the PTE was retrieved from `Version 5` Output and fully smoke-tested on both the WSL x86-64 `llama_main` host binary (compiled from ExecuTorch `v0.5.0` with quantized + XNNPACK kernels) and the physical Redmi 9 Power `llama_main` binary.
3. **Host `Release` Build Logging:**
   - The local WSL `llama_main` build omitted `-DEXECUTORCH_ENABLE_LOGGING=ON`, so `stderr` metadata lines were not emitted on host (verified instead directly via `runner.cpp` logs on Redmi `llama_main`).
4. **Prompt Trailing Newline Note:**
   - Both the host and Redmi smoke test wrapper scripts produced `32` prompt tokens (vs. `31` in Phase 3.2's `B_greedy_Q1`) due to an extra trailing newline in the shell heredoc; Phase 3.5 must ensure identical prompt construction across both PTEs.

---

## 10. Confirmation of Baseline Preservation & Stop Condition

- **Production Merged PTE (`model/android/learnmate_llama3_2_3b.pte` & `/data/local/tmp/llama/learnmate_llama3_2_3b.pte`):** `1,929,907,360 bytes`, SHA-256 `8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91` — **UNCHANGED**.
- **Production Tokenizer (`model/android/tokenizer.model` & `/data/local/tmp/llama/tokenizer.model`):** `2,183,982 bytes`, SHA-256 `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` — **UNCHANGED**.
- **Android Application & JNI Source:** Zero modifications.
- **Training / Merging / Git Weight Commits:** Zero training or adapter merging performed; large `.pte` artifacts remain untracked/excluded from Git.
- **Stop Condition:** Execution stopped after base PTE smoke verification. **Awaiting explicit approval before starting Phase 3.5 controlled base-vs-merged evaluation.**
