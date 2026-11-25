# LearnMate AI: Integrated LoRA Smoke-Test Recovery & ExecuTorch INT4 Export Guide (v3.3)

**Base Model:** `meta-llama/Llama-3.2-3B-Instruct`  
**Adapter State:** **Successfully Trained & Verified** (2/2 steps, loss 2.549154, weights 46.425 MB, archive 76.39 MB)  
**Local Checkpoint Backup:** [`model/checkpoints/learnmate_smoke_test_adapter_checkpoint_2.tar.gz`](checkpoints/learnmate_smoke_test_adapter_checkpoint_2.tar.gz)  
**Notebook File:** [`model/kaggle_export_llama3_2_3b_merged.ipynb`](kaggle_export_llama3_2_3b_merged.ipynb)  
**Target Hardware:** Xiaomi Redmi 9 Power (Snapdragon 662, 4 GB RAM, Android 12)  

---

## 1. Technical Audit Findings & Engineering Resolutions (v3.3)

| Audit Area | Root Cause & Analysis | Engineering Resolution in v3.3 |
|---|---|---|
| **1. Checkpoint Persistence & Recovery** | Ephemeral `/kaggle/working` container resets destroy uncommitted outputs. | Cell 10 includes standalone archive detection/extraction from `/kaggle/working/` or `/kaggle/input/` keeping the archive intact, re-verifies SHA256 hashes from disk, and validates the 24.3M trainable parameters. |
| **2. GPU Memory Management (Cell 14)** | Global references `trainer` and `train_base_model` hold ~3.0 GB VRAM. | Cell 14 verifies adapter checkpoint on disk first, explicitly deletes `trainer` and `train_base_model`, invokes `gc.collect()` and `torch.cuda.empty_cache()`, and asserts >= 8.0 GB free VRAM before loading base FP16 model. |
| **3. Disk Capacity Preflight (Cell 12)** | Consolidated FP16 weights (~6.42 GB) + PTE (~2.0 GB) require substantial disk. Staging in `/kaggle/temp/` (RAM `tmpfs`) causes host OOM. | Cell 12 runs `shutil.disk_usage('/kaggle/working')`, enforces minimum 11.5 GB free space (8.5 GB export + 3.0 GB safety margin), and routes all intermediate checkpoints to NVMe scratch disk (`/kaggle/working/llama3_2_3b_meta/`). |
| **4. Local Model Snapshot & Auth (Cells 12, 14, 18)** | Requesting gated repo when local snapshot exists risks unnecessary network calls or auth failures. | If `cached_snapshot` is a valid directory, it is used directly as `model_source` with `local_files_only=True`. Original repo ID is preserved in metadata. Cell 18 searches local snapshot for `original/tokenizer.model` before remote fallback. |
| **5. Structural Tensor Remapping (Cell 16)** | Must guarantee 100% parameter preservation and tensor shape fidelity across all 28 layers. | Cell 16 maps HF keys to Meta keys, asserts exact 255 tensor count, verifies all shapes ($dim=3072, n\_heads=24, n\_kv=8$), confirms `torch.float16` dtype, and notes that structural verification is distinct from on-device runtime execution. |
| **6. Tokenizer & PTE Validation (Cells 18, 20, 22)** | Validating `ET12` header alone does not prove execution plan presence. | Cell 18 verifies 16-byte binary header and token records. Cell 20 executes ExecuTorch v0.5.0 AOT export. Cell 22 traverses FlatBuffer table hierarchy to verify Program vtable and `num_execution_plans >= 1`. |

---

## 2. Cell Execution Structure & Verified Invariants

| Cell Index | Cell Type | Stage Name | Technical Action & Invariants Enforced |
|---|---|---|---|
| **Cell 0** | Markdown | Notebook Overview | Documents storage governance, single-GPU isolation (`cuda:0`), and hardware target. |
| **Cell 1** | Markdown | Step 0 Header | Preflight Health & System Verification. |
| **Cell 2** | Code | Preflight Health Gate | Asserts CUDA available, free VRAM >= 8GB, host RAM >= 12GB, disk >= 15GB, resolves `cached_snapshot`. |
| **Cell 3** | Markdown | Step 1 Header | Pinned Environment & ExecuTorch Setup. |
| **Cell 4** | Code | Dependency Gate | Verifies pinned packages, clones ExecuTorch v0.5.0 (`--depth 1 --branch v0.5.0`). |
| **Cell 5** | Markdown | Step 2 Header | Synthetic Dataset Inflation. |
| **Cell 6** | Code | Dataset Gate | Validates 65 train & 8 val records, fallback base64 inflation if missing. |
| **Cell 7** | Markdown | Step 3 Header | Reproducible 2-Step LoRA Smoke-Test. |
| **Cell 8** | Code | Smoke-Test Gate | TRL 1.18.2 / Transformers 5.20.2 compatible `SFTConfig` (`warmup_steps=0`, `max_length=1024`, `dataset_text_field="text"`, `fp16=False`, `bf16=False`), skips if checkpoint exists. |
| **Cell 9** | Markdown | Step 4 Header | LoRA Adapter Verification & Packaging. |
| **Cell 10** | Code | Adapter Gate | Unpacks uploaded archive if directory missing, verifies $r=16, \alpha=32$, 7 target modules, 24.3M params, SHA256 hashes, packages `learnmate_smoke_test_adapter_checkpoint_2.tar.gz`. |
| **Cell 11** | Markdown | Step 5 Header | Base Model Config, RoPE & Disk Preflight. |
| **Cell 12** | Code | Config & Disk Gate | Enforces >= 11.5 GB free disk, resolves `model_source`, validates RoPE ($factor=32, \theta=500000, high=4, low=1, ctx=8192$), writes `params.json` to NVMe scratch. |
| **Cell 13** | Markdown | Step 6 Header | Single-GPU FP16 Base Load & Merge. |
| **Cell 14** | Code | Merge Gate | Releases `trainer`/`train_base_model`, clears VRAM, asserts >= 8GB free VRAM, loads FP16 base on `cuda:0` (~6.42GB), attaches adapter, executes `merge_and_unload()`. |
| **Cell 15** | Markdown | Step 7 Header | HF to Meta Consolidated Conversion. |
| **Cell 16** | Code | Consolidated Gate | Maps 255 tensors to Meta format, validates all shapes and FP16 dtypes, saves `consolidated.00.pth` (6.42GB) to NVMe scratch, purges memory. |
| **Cell 17** | Markdown | Step 8 Header | Tokenizer Compilation & Validation. |
| **Cell 18** | Code | Tokenizer Gate | Searches local snapshot first, stages `tokenizer.model`, compiles `tokenizer.bin` via ExecuTorch BPE compiler, asserts `<IIII` binary struct. |
| **Cell 19** | Markdown | Step 9 Header | ExecuTorch v0.5.0 PTE Export. |
| **Cell 20** | Code | Export Gate | Runs `export_llama.py` with `-qmode 8da4w --group_size 128 --embedding-quantize 4,32 --max_seq_length 128 -X`, overwrite protection. |
| **Cell 21** | Markdown | Step 10 Header | Multi-Layer PTE Validation & Manifest. |
| **Cell 22** | Code | Validation Gate | Bounds size ($1.5\text{ GB} \le N \le 2.8\text{ GB}$), traverses FlatBuffer hierarchy (`ET12`, vtable, `num_plans >= 1`), writes `export_manifest.json`. |

---

## 3. Kaggle Resumption Instructions

If the Kaggle session disconnects or resets:
1. Ensure GPU accelerator is enabled (GPU T4 x 2 or P100).
2. Upload the local backup file `learnmate_smoke_test_adapter_checkpoint_2.tar.gz` to `/kaggle/working/` (or attach it as a private Kaggle dataset).
3. Run the notebook from Cell 2:
   - Cell 8 will detect the existing checkpoint or extract the archive, verify weights, and cleanly set `skip_training = True`.
   - Cell 10 will verify the extracted adapter weights and compute SHA256 hashes.
   - Cells 12 to 22 will proceed with disk preflight, FP16 merge, conversion, tokenizer compilation, and INT4 PTE export.

---

## 4. Physical On-Device Deployment Handoff

Once Cell 22 completes:
1. Download artifacts from `/kaggle/working/export/`:
   - `llama3_2_3b_8da4w.pte` (~1.8–2.0 GB)
   - `tokenizer.bin` (~4.5 MB)
   - `export_manifest.json` (~2 KB)
2. Stage artifacts onto connected Redmi 9 Power:
   ```bash
   adb push llama3_2_3b_8da4w.pte /data/local/tmp/
   adb push tokenizer.bin /data/local/tmp/
   ```
3. Execute offline inference with Airplane Mode enabled to capture authentic tokens.
