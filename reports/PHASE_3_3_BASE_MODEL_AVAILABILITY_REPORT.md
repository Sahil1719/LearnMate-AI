# LearnMate AI | Phase 3.3 Engineering Report
## Original Base Model Availability and Integrity Investigation

**Project:** LearnMate AI (Offline On-Device AI Learning Companion for Android)  
**Phase:** 3.3  
**Date:** 2026-11-25  
**Type:** Read-Only Investigation and Architecture Planning  
**Primary Model Invariant:** Meta Llama 3.2 3B Instruct  
**Target Hardware:** Xiaomi Redmi 9 Power (`lime_in` / `M2010J19SI`, Qualcomm Snapdragon 662, 4 GB RAM)  
**Production Model Artifact:** `model/android/learnmate_llama3_2_3b.pte` (SHA-256: `8c545bde...`)  
**Production Tokenizer:** `model/android/tokenizer.model` (SHA-256: `82e9d319...`)  
**Git Branch:** `phase-3.2-quality-eval` (Commit: `e21c34086433481aa5189fb4ab35a33cc3ab69a7`)

---

## 1. Executive Summary

Phase 3.3 conducted an exhaustive, read-only investigation across all host storage systems, virtualized environments, and physical target hardware to determine the availability and provenance of the original, unmerged **Meta Llama 3.2 3B Instruct** base checkpoint.

In Phase 3.2, controlled physical-device evaluations revealed that the deployed model artifact (`learnmate_llama3_2_3b.pte`) suffers from severe technical quality degradations—including factual inversions (*"a microcontroller and a microprocessor are actually the same thing"*), circular tautologies (*"a pointer in C is a pointer to a pointer in C"*), goal bleeding (*"You're referring to an embedded systems engineer"*), and catastrophic 79-token repetition loops. However, because the upstream Kaggle export pipeline merged the 2-step LoRA smoke-test weights into the model in-memory prior to ExecuTorch AOT flatbuffer generation, the original base-model PTE was never serialized. A true controlled comparison between the base model and the merged model could not be executed without locating or acquiring the pristine base model checkpoint.

### Primary Investigation Findings

1. **Local Search Outcome:** The original, unmerged Meta Llama 3.2 3B Instruct base model weights do **NOT** exist locally in the Windows filesystem, user download folders, Hugging Face caches, WSL environments, or on the connected Android smartphone.
2. **Repository & Authentication Verification:** Access to the authorized Meta repository `meta-llama/Llama-3.2-3B-Instruct` was verified through the Hugging Face API. The user account (`1sahil7`) holds a valid, active authentication token with **explicitly granted access** to the gated Llama 3.2 community repository.
3. **Exact Download & Storage Requirements:** 
   - Hugging Face Safetensors format: **2 shards totaling 6,425,529,048 bytes (5.98 GiB / 6.43 GB)**.
   - Original PyTorch format: **1 consolidated file totaling 6,425,585,114 bytes (5.98 GiB / 6.43 GB)**.
   - Host storage is abundant: Windows C: drive has **242.85 GB free**; Android `/data` has **24 GB free** (ample headroom for the ~1.84 GB target PTE).
4. **Evaluation Feasibility:** The standalone physical-device runner (`/data/local/tmp/llama/llama_main`) and the Android application (`com.learnmate.ai`) are fully verified and operational, but they can **only** execute serialized `.pte` programs. Evaluating the pristine base model under identical hardware conditions on the Snapdragon 662 requires exporting the base checkpoint to a clean `base_llama3_2_3b.pte` using the exact same ExecuTorch INT4 8da4w quantization recipe.
5. **Phase Conclusion:**
   $$\mathbf{BASE\ CHECKPOINT\ ACCESSIBLE,\ DOWNLOAD\ REQUIRED}$$

---

## 2. Current Production Baseline Hashes & Invariants

Prior to and throughout Phase 3.3, all production model and tokenizer files were verified against their authoritative cryptographic hashes. No files have been modified, replaced, or deleted.

### Cryptographic Baseline Table

| Component | Filesystem / Device Path | Size (Bytes) | SHA-256 Hash | Integrity Status |
| :--- | :--- | :--- | :--- | :--- |
| **Model PTE (Local)** | `model/android/learnmate_llama3_2_3b.pte` | 1,929,907,360 | `8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91` | **MATCH (Preserved)** |
| **Model PTE (Device tmp)** | `/data/local/tmp/llama/learnmate_llama3_2_3b.pte` | 1,929,907,360 | `8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91` | **MATCH (Preserved)** |
| **Model PTE (App Scoped)** | `/storage/emulated/0/Android/data/...` | 1,929,907,360 | `8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91` | **MATCH (Preserved)** |
| **Tokenizer (Local)** | `model/android/tokenizer.model` | 2,183,982 | `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` | **MATCH (Preserved)** |
| **Tokenizer (Device tmp)** | `/data/local/tmp/llama/tokenizer.model` | 2,183,982 | `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` | **MATCH (Preserved)** |
| **Tokenizer (App Scoped)** | `/storage/emulated/0/Android/data/...` | 2,183,982 | `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` | **MATCH (Preserved)** |

### Architectural Parameters (`model/android/params.json`)
- `dim`: 3072 | `n_layers`: 28 | `n_heads`: 24 | `n_kv_heads`: 8 (Grouped Query Attention GQA 3:1)
- `vocab_size`: 128,256 | `max_seq_len`: 128 | `norm_eps`: 1e-05 | `rope_theta`: 500000.0

---

## 3. Search Locations Inspected

A systematic recursive audit was performed across all potential storage locations in the Windows host, WSL subsystem, and connected Android device.

### Search Audit Summary Table

| Environment | Target Directory / Path | Inspected Contents | Base Checkpoint Found? |
| :--- | :--- | :--- | :---: |
| **Windows Host** | `<USER_HOME>\.cache\huggingface` | Directory did not exist | **NO** |
| **Windows Host** | `<USER_HOME>\.cache\torch` | Directory did not exist | **NO** |
| **Windows Host** | `<USER_HOME>\Downloads` | Setup executables only; no `.safetensors`, `.bin`, `.pth` | **NO** |
| **Repository Root** | `.../LearnMate-AI/model/` | `learnmate_llama3_2_3b.pte` (Merged INT4), `tokenizer.model` | **NO** |
| **Repository Root** | `.../LearnMate-AI/model/checkpoints/` | `learnmate_smoke_test_adapter_checkpoint_2.tar.gz` (80 MB) | **NO** |
| **Repository Root** | `.../LearnMate-AI/training/adapters/` | Empty directory | **NO** |
| **Repository Root** | `.../LearnMate-AI/training/dataset/` | Dialogue JSON lines; no weights | **NO** |
| **WSL Ubuntu-24.04** | `$HOME/.cache/huggingface` | `token` and `stored_tokens` present; no `hub/` weights cache | **NO** |
| **WSL Ubuntu-24.04** | `$HOME/executorch` | Source tree and `cmake-out-android` build; no model weights | **NO** |
| **WSL Ubuntu-24.04** | `$HOME/learnmate-env` | Virtualenv site-packages; no model checkpoints | **NO** |
| **Redmi 9 Power** | `/data/local/tmp/llama/` | `learnmate_llama3_2_3b.pte` (Merged INT4), `llama_main` | **NO** |
| **Redmi 9 Power** | `/storage/emulated/0/Android/data/...` | Production `learnmate_llama3_2_3b.pte` (Merged INT4) | **NO** |
| **Redmi 9 Power** | `/sdcard/Download/` | No model weights | **NO** |

**Conclusion of Search:** The original base model weights were never downloaded or persisted to local storage.

---

## 4. Exact Checkpoint Provenance & Lineage Audit

To determine why the original base model is missing locally, we examined the project history and export notebooks:

1. **Training Script Origin:**
   - Adapter checkpoint `learnmate_smoke_test_adapter_checkpoint_2.tar.gz` (80,096,217 bytes) was produced during a pipeline verification smoke test.
   - As established in Phase 3.2, it was trained for **only 2 optimizer steps** on 65 synthetic dialogues using QLoRA.
2. **Kaggle Export Notebook Lineage (`model/kaggle_export_llama3_2_3b_merged.ipynb`):**
   - In Cell 14, the Kaggle GPU instance loaded `meta-llama/Llama-3.2-3B-Instruct` from Hugging Face into ephemeral GPU RAM.
   - It immediately attached the LoRA adapter from `learnmate_smoke_test_adapter_checkpoint_2.tar.gz` and called `model.merge_and_unload()`.
   - In Cell 20, the merged model was quantized via ExecuTorch INT4 8da4w and serialized directly to `learnmate_llama3_2_3b.pte`.
   - **Crucial Architectural Realization:** The Kaggle notebook environment was ephemeral. Once the session shut down, the downloaded base weights in Kaggle's `/tmp` were destroyed. Only the resulting merged artifact (`learnmate_llama3_2_3b.pte`) was downloaded to the repository.

**Provenance Distinction:**
- `learnmate_llama3_2_3b.pte`: Contains the base model **plus** the 2-step corrupted LoRA adapter weights.
- `meta-llama/Llama-3.2-3B-Instruct`: The pristine, uncorrupted base model weights developed by Meta, which have never been exported to `.pte` in this project.

---

## 5. Hugging Face Repository & Licensing Verification

We investigated authorized access to the upstream foundation model repository without exposing private credentials:

### 1. Repository Identification
- **Exact Repository ID:** `meta-llama/Llama-3.2-3B-Instruct`
- **Author / Organization:** `meta-llama`
- **Current Canonical Commit SHA:** `0cb88a4f764b7a12671c53f0838cd831a0843b95`
- **Last Modified:** 2024-10-24 15:07:29 UTC
- **Gated Status:** `manual` (Gated community license requiring Meta acceptance)

### 2. Authentication & Authorization Status
- **Client Library:** `huggingface_hub` v2.4.0 (WSL `~/learnmate-env`)
- **Authenticated Account:** `1sahil7`
- **User Type:** `user`
- **Token State:** **VALID** (stored securely in `~/.cache/huggingface/token`)
- **Access Permission to `meta-llama/Llama-3.2-3B-Instruct`:** **GRANTED**
- **Download Capability:** Verified via API metadata retrieval. The authenticated account has full rights to download all files, weights, and tokenizer definitions.

### 3. Licensing & Legal Compliance
- **Governing License:** *Llama 3.2 Community License Agreement* (Effective September 25, 2024).
- **Grant of Rights (Section 1.a):** Meta grants a non-exclusive, worldwide, royalty-free limited license to use, reproduce, distribute, copy, create derivative works of, and modify the Llama Materials.
- **Applicability to LearnMate AI:**
  - Evaluative benchmarking, technical quality validation, fine-tuning, and on-device offline inference are **100% permitted**.
  - Commercial threshold (Section 2): Applies only to entities with $> 700$ million monthly active users; LearnMate AI is fully exempt.
  - Attribution requirement: Must provide license copy, retain copyright notices, and display *"Built with Llama"*.
  - Acceptable Use Policy: Verified compliant. Educational learning companions for STEM and computer science do not violate any prohibited use categories.

---

## 6. Exact File and Storage Requirements

Using `api.get_paths_info()` against `meta-llama/Llama-3.2-3B-Instruct` (Commit `0cb88a4`), the exact byte sizes of all repository assets were verified:

### Complete Remote File Inventory

| Repository Path | File Role | Exact Size (Bytes) | Size (MB / GB) | Required for Export? |
| :--- | :--- | :---: | :---: | :---: |
| `model-00001-of-00002.safetensors` | Model Weights (Shard 1/2) | 4,965,799,096 | 4.62 GiB (4.97 GB) | **YES (Option 1)** |
| `model-00002-of-00002.safetensors` | Model Weights (Shard 2/2) | 1,459,729,952 | 1.36 GiB (1.46 GB) | **YES (Option 1)** |
| `model.safetensors.index.json` | Safetensors Index Map | 20,919 | 0.02 MB | **YES (Option 1)** |
| `config.json` | Model Architecture Config | 878 | 0.00 MB | **YES** |
| `generation_config.json` | Generation Parameters | 189 | 0.00 MB | Recommended |
| `original/consolidated.00.pth` | PyTorch Weights (Consolidated) | 6,425,585,114 | 5.98 GiB (6.43 GB) | **YES (Option 2)** |
| `original/params.json` | Native Architecture Params | 220 | 0.00 MB | **YES (Option 2)** |
| `original/tokenizer.model` | TikToken Model (Byte-identical) | 2,183,982 | 2.08 MB | Already Local |
| `special_tokens_map.json` | Special Tokens Mapping | 296 | 0.00 MB | Optional |
| `tokenizer.json` | Hugging Face Fast Tokenizer | 9,085,657 | 8.66 MB | Optional |
| `tokenizer_config.json` | Tokenizer Configurations | 54,528 | 0.05 MB | Optional |
| `LICENSE.txt` / `USE_POLICY.md` | Legal Terms & Policies | 55,477 | 0.05 MB | Documented |

### Download Option Analysis

#### Option 1: Hugging Face Safetensors Pipeline (Recommended for Kaggle / Transformers)
- Weight Shards: `model-00001-of-00002.safetensors` + `model-00002-of-00002.safetensors`
- Total Weight Bytes: **6,425,529,048 bytes**
- Total Package Bytes (including configs): **6,434,748,511 bytes = 6.43 GB (5.99 GiB)**
- Advantage: Compatible with standard Hugging Face `from_pretrained()` export scripts.

#### Option 2: Original Meta PyTorch Consolidated Pipeline (ExecuTorch Llama native)
- Weight File: `original/consolidated.00.pth` (Single shard)
- Total Weight Bytes: **6,425,585,114 bytes**
- Total Package Bytes: **6,427,769,316 bytes = 6.43 GB (5.98 GiB)**
- Advantage: Direct input into ExecuTorch's native `examples/models/llama/export_llama.py`.

---

## 7. Storage Capacity and Feasibility Audit

We verified actual available disk space across all participating systems:

### Storage Capacity Matrix

| Storage Location | Total Capacity | Available Free Space | Required Storage | Headroom Status |
| :--- | :---: | :---: | :---: | :---: |
| **Windows Host (`C:\`)** | 475.42 GB | **242.85 GB Free** | ~8.3 GB (Raw + PTE) | **EXCELLENT (29x Headroom)** |
| **WSL (`Ubuntu-24.04`)** | (Backed by C:) | **242.85 GB Free** | ~12.0 GB (Peak Export) | **EXCELLENT (20x Headroom)** |
| **Redmi 9 Power (`/data`)**| 47.00 GB | **24.00 GB Free** | 1.84 GB (Base PTE) | **EXCELLENT (13x Headroom)** |

**Feasibility Verdict:** Storage capacity is completely unconstrained. The download and export can proceed without risk of disk exhaustion on any system.

---

## 8. Evaluation Environment Compatibility Analysis

To plan the controlled comparison without making modifications, we analyzed four potential inference environments:

### 1. Host Python Environment (PyTorch / Transformers on Host)
- **Status:** Feasible with setup in WSL; Not feasible on Windows directly.
- **Findings:**
  - Windows host lacks `torch` and `transformers`.
  - WSL `~/learnmate-env` has `torch 2.6.0+cpu`, but lacks `transformers`.
  - Evaluating raw FP16/BF16 weights on CPU requires ~7–14 GB RAM and executes very slowly on x86 CPU without GPU acceleration.
  - Furthermore, running unquantized FP16 weights on host CPU would not be a valid apples-to-apples comparison with the INT4 8da4w model deployed on the Snapdragon 662.

### 2. Host ExecuTorch Runtime
- **Status:** Requires building host binary.
- **Findings:**
  - `~/executorch` in WSL currently only has `cmake-out-android` (ARM64 binary for Android).
  - An x86_64 host build of `llama_main` does not exist.
  - ExecuTorch cannot run unexported safetensors; it requires a serialized `.pte`.

### 3. Xiaomi Redmi 9 Power Standalone Runner (`/data/local/tmp/llama/llama_main`)
- **Status:** **OPTIMAL AND FULLY READY (Requires Base PTE)**
- **Findings:**
  - The standalone runner on the device is already fully operational and benchmarked (achieving 1.45–1.55 tok/s across Kryo 260 Gold cores with temp=0.0 and temp=0.7 support).
  - The runner accepts `--model_path=base_llama3_2_3b.pte`.
  - Running both the base PTE and the merged PTE on the exact same device CLI under identical temperature and sequence length settings provides the **gold-standard scientific comparison**.

### 4. Android Production Application (`com.learnmate.ai`)
- **Status:** **FULLY READY (Requires Base PTE)**
- **Findings:**
  - `ModelSelectionManager.kt` automatically enumerates all `.pte` files in `/storage/emulated/0/Android/data/com.learnmate.ai/files/models/`.
  - If `base_llama3_2_3b.pte` is placed in this directory, it can be loaded and tested through the UI without changing a single line of application source code.

---

## 9. Missing Dependencies & Technical Blockers

To execute the controlled base-versus-merged model quality validation, the following specific technical dependencies are currently missing:

1. **Missing Base Checkpoint Files:** The raw base model weights (`model-00001-of-00002.safetensors` + `model-00002-of-00002.safetensors` totaling 6.43 GB) have not been downloaded.
2. **Missing Base PTE Artifact (`base_llama3_2_3b.pte`):** The ExecuTorch INT4 8da4w flatbuffer program corresponding to the clean base model does not exist.
3. **Execution Pipeline for Base Export:**
   - To export `base_llama3_2_3b.pte` without modifying the merged model, an AOT export script identical to `kaggle_export_llama3_2_3b_merged.ipynb` (minus the LoRA merge step) must be executed either in Kaggle GPU (recommended to avoid local CPU overhead) or locally.

---

## 10. Proposed Next Steps (Phase 3.4 Roadmap)

Upon receiving explicit approval from the project owner, the following sequence should be executed:

1. **Step 1: Obtain Base Checkpoint & Export Base PTE (Remote Kaggle GPU)**
   - Authorize downloading the authorized `meta-llama/Llama-3.2-3B-Instruct` base checkpoint in a Kaggle GPU notebook (or local WSL environment).
   - Run the exact ExecuTorch v0.5.0 INT4 8da4w export pipeline **without** attaching the LoRA adapter.
   - Produce `base_llama3_2_3b.pte` (Expected size: ~1.84 GB; max sequence length: 128 tokens; KV cache enabled).
2. **Step 2: Deploy Base PTE to Redmi 9 Power**
   - Push `base_llama3_2_3b.pte` to `/data/local/tmp/llama/base_llama3_2_3b.pte` and `/storage/emulated/0/Android/data/com.learnmate.ai/files/models/base_llama3_2_3b.pte`.
   - Preserve `learnmate_llama3_2_3b.pte` untouched.
3. **Step 3: Run Controlled Base-vs-Merged Evaluation Matrix**
   - Run the identical 8 technical benchmark questions across the pristine base model under both greedy ($T=0.0$) and sampling ($T=0.7$) modes.
   - Compare base outputs directly against the 32 runs collected in Phase 3.2.
   - Conclusively determine whether the factual errors and repetition loops observed in Phase 3.2 originated from the 2-step LoRA adapter or the quantization/128-token context boundary.

---

## 11. Explicit Approval Requirements & Stop Condition

In strict accordance with Phase 3.3 instructions, **all operations have stopped.**

### Required User Approvals Before Phase 3.4:
1. **Authorization to Download:** Explicit permission to download the 6.43 GB base weights from `meta-llama/Llama-3.2-3B-Instruct` using the verified `1sahil7` account credentials.
2. **Authorization to Export Base Model:** Permission to run the ExecuTorch INT4 8da4w AOT export to generate `base_llama3_2_3b.pte`.
3. **Target Environment Decision:** Authorization to execute the export via a remote Kaggle notebook (recommended) or locally in WSL.

**Current State:**
- Working tree: Clean baseline preserved.
- Production PTE hash: `8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91` (Verified unmodified).
- Production Tokenizer hash: `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` (Verified unmodified).
- No model weights were downloaded.
- No training, quantization, or export was initiated.
- No application source code was modified.
