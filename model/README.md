# Model Acquisition, Quantization & Export Pipeline

## 1. Updated Model Strategy Overview
LearnMate AI employs an evaluated **Dual-Model On-Device Architecture** designed to balance educational reasoning capability with physical smartphone memory constraints:

| Device Class | Model Candidate | Quantisation | Embedding Precision | Group Size | Estimated PTE Size | Target RAM |
|---|---|---|---|---|---|---|
| **Tier 1 (4GB Baseline)** | `meta-llama/Llama-3.2-1B-Instruct` | `8da4w` (INT4 weight, INT8 dynamic activation) | 4-bit (`-E 4,32`) | 128 | **~795 MB (0.74 GiB)** | 4GB RAM (Redmi 9 Power) |
| **Tier 2 (6GB+ Standard)** | `meta-llama/Llama-3.2-3B-Instruct` | `8da4w` (INT4 weight, INT8 dynamic activation) | 4-bit (`-E 4,32`) | 128 | **~1.89 GB (1.76 GiB)** | 6GB+ RAM (Realme X7 5G) |
| **Historical Reference** | `meta-llama/Meta-Llama-3-8B-Instruct` | `8da4w` (INT4 weight, INT8 dynamic activation) | 4-bit (`-E 4,32`) | 128 | **~4.27 GB (3.98 GiB)** | Benchmark baseline |

*Important: Both dual-model configurations remain provisional pending actual export and physical device benchmarking. No competition approval has been claimed. All artifact sizes and memory footprints above are mathematical estimates, not measured artifacts.*

---

## 2. Reusable Cloud Export Pipeline (`kaggle_dual_model_export.ipynb`)
The model export pipeline is unified into a single reusable Jupyter notebook:
- **Location:** `model/kaggle_dual_model_export.ipynb`
- **Execution Target:** Kaggle Cloud GPU instance (T4 or P100 with >= 24GB system RAM).
- **Selector Variable:** Set `SELECTED_MODEL = "1B"` or `SELECTED_MODEL = "3B"` to select configuration.
- **Workflow Steps (14 Structured Cells):**
  1. Host resource and scratch disk verification (RAM >= 24GB check).
  2. Pinned ExecuTorch v0.5.0 + PyTorch 2.6.0 + TorchAO 0.8.0 stack setup.
  3. Model configuration selection (`CONFIGS["1B"]` vs `CONFIGS["3B"]`).
  4. Kaggle Secrets authentication (`HF_TOKEN`).
  5. Checkpoint acquisition from Hugging Face (`consolidated.00.pth`, `params.json`, `tokenizer.model`).
  6. Checkpoint format verification and fallback converter.
  7. Model architectural parameter validation (`dim`, `n_layers`, `n_heads`, `vocab_size`).
  8. ExecuTorch PTE export command staging with XNNPACK CPU delegate (`-X`) and dynamic quantization (`-qmode 8da4w -kv --use_sdpa_with_kv_cache`).
  9. Artifact size inspection and MD5 checksum computation.
  10. Tokenizer file staging (`tokenizer.model`, `tokenizer.json`).
  11. Official export manifest generation (`model_manifest.json`).

---

## 3. Model Manifest Template (`model_manifest.json`)
The formal model manifest specification is stored at:
- `model/model_manifest.json`

Tracks complete model lineage, precision details, estimated memory budgets, device tiers, checksums, and validation status flags (`source_verified`, `estimated`, `planned`, `not_tested`).

---

## 4. Execution Restrictions & Safety Gates
- **Zero Weight Downloads in Local Repository:** Gated model weights must never be downloaded to the local repository or committed to Git.
- **Human Approval Gate:** The Kaggle export notebook must NOT be executed without explicit human authorization.
- **Honest AI Status:** No genuine Llama token generation has yet been demonstrated on the physical phone. Synthetic PTE execution and `--help` CLI tests confirm runtime infrastructure, not LLM inference.
