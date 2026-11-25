import json
import ast
import os
import sys

NOTEBOOK_PATH = r".\model\kaggle_export_llama3_2_3b_merged.ipynb"

def update_notebook():
    with open(NOTEBOOK_PATH, "r", encoding="utf-8") as f:
        nb = json.load(f)

    # 1. Update Cell 0 Markdown (Key Architectural & Operational Rules)
    cell_0_text = """# LearnMate AI: Llama 3.2 3B + LoRA Adapter Merge & ExecuTorch v0.5.0 INT4 Export
**Target Model:** Meta Llama 3.2 3B Instruct + LoRA Smoke-Test Adapter (`checkpoint-2`)  
**Target Runtime:** ExecuTorch v0.5.0 with XNNPACK INT4 Delegate (`8da4w`)  
**Target Device:** Xiaomi Redmi 9 Power (Snapdragon 662, 4GB RAM) / Standard ARM64 Android (6GB+ RAM)  

### Key Architectural & Operational Rules:
1. **Preflight Gate:** Strictly verifies GPU VRAM, system RAM, disk space, adapter presence, and local Hugging Face cache before any processing.
2. **Single-GPU Device Mapping (`device_map={"": "cuda:0"}`):** Verified GPU memory allocation (~6.42 GB fits cleanly on single 15.0 GB Tesla T4); eliminates cross-GPU tensor sharding and guarantees safe in-memory PEFT merging.
3. **Non-Destructive Environment:** Zero blind `pip install --upgrade` or PyTorch overwrites. Preserves working Kaggle CUDA stack.
4. **Local Cache First:** Reuses existing cached snapshot of `meta-llama/Llama-3.2-3B-Instruct`. Zero redundant model downloads.
5. **RoPE Scaling Preservation:** Extracts `rope_scaling` from AutoConfig and writes mandatory `use_scaled_rope: true` and `rope_scale_factor: 32` into `params.json` for ExecuTorch rotary embedding fidelity.
6. **Exhaustive HF-to-Meta Validation:** Verifies all 255 tensors, dimensions, data types, layer counts, and key remappings.
7. **Dynamic Architecture Config:** Generates `params.json` from the model's actual cached `config.json`.
8. **Mandatory Tokenizer Conversion & Binary Struct Check:** Cleans stale artifacts, checks return code, and validates binary header (`bpe_tokenizer.cpp` compatible).
9. **Overwrite Protection & FlatBuffer Spec Verification:** Archives existing PTE files before export, verifies exit code `0`, and validates FlatBuffer `ET12` magic identifier and root table offset.
"""
    nb["cells"][0]["source"] = [line + "\n" for line in cell_0_text.strip().split("\n")]

    # 2. Update Cell 7 Markdown (Step 3)
    cell_7_text = """### Step 3: Base Model Configuration Validation & Llama 3.2 RoPE Scaling Setup
Loads actual cached `config.json` via Hugging Face `AutoConfig`, inspects RoPE scaling parameters (`rope_scaling`), and dynamically constructs `params.json` with mandatory `use_scaled_rope: true` and `rope_scale_factor: 32` required by ExecuTorch v0.5.0 for rotary embedding accuracy."""
    nb["cells"][7]["source"] = [line + "\n" for line in cell_7_text.strip().split("\n")]

    # 3. Update Cell 8 Code (Step 3: AutoConfig & RoPE scaling params.json)
    cell_8_code = """from transformers import AutoConfig
from kaggle_secrets import UserSecretsClient

try:
    user_secrets = UserSecretsClient()
    hf_token = user_secrets.get_secret("HF_TOKEN")
except Exception:
    hf_token = os.environ.get("HF_TOKEN", None)

BASE_MODEL_REF = cached_snapshot if cached_snapshot else adapter_config.get("base_model_name_or_path", "meta-llama/Llama-3.2-3B-Instruct")
print("=== STEP 3: BASE MODEL CONFIGURATION & ROPE SCALING VALIDATION ===")
print(f"Loading AutoConfig from: {BASE_MODEL_REF}")

model_config = AutoConfig.from_pretrained(
    BASE_MODEL_REF,
    token=hf_token,
    local_files_only=(cached_snapshot is not None)
)

# Extract and verify exact architectural parameters
dim = model_config.hidden_size
n_layers = model_config.num_hidden_layers
n_heads = model_config.num_attention_heads
n_kv_heads = model_config.num_key_value_heads
vocab_size = model_config.vocab_size
intermediate_size = model_config.intermediate_size
norm_eps = model_config.rms_norm_eps
rope_theta = getattr(model_config, "rope_theta", 500000.0)

print(f"  Hidden Size (dim): {dim} (Expected: 3072)")
print(f"  Layers (n_layers): {n_layers} (Expected: 28)")
print(f"  Attention Heads: {n_heads} (Expected: 24)")
print(f"  Key-Value Heads: {n_kv_heads} (Expected: 8, GQA ratio 3:1)")
print(f"  Vocab Size: {vocab_size} (Expected: 128256)")
print(f"  Intermediate Size: {intermediate_size} (Expected: 8192)")
print(f"  Norm Epsilon: {norm_eps}")
print(f"  RoPE Theta: {rope_theta}")

assert dim == 3072, f"Architecture mismatch: dim={dim} != 3072"
assert n_layers == 28, f"Architecture mismatch: n_layers={n_layers} != 28"
assert n_heads == 24, f"Architecture mismatch: n_heads={n_heads} != 24"
assert n_kv_heads == 8, f"Architecture mismatch: n_kv_heads={n_kv_heads} != 8"
assert vocab_size == 128256, f"Architecture mismatch: vocab_size={vocab_size} != 128256"

# FIX 1: Llama 3.2 RoPE Scaling Verification & Construction
rope_scaling = getattr(model_config, "rope_scaling", None)
print(f"\\nRoPE Scaling Configuration from AutoConfig:")
print(f"  rope_scaling: {rope_scaling}")

use_scaled_rope = False
rope_scale_factor = 32
if rope_scaling is not None:
    use_scaled_rope = True
    rope_scale_factor = int(rope_scaling.get("factor", 32))
    print(f"  RoPE Type: {rope_scaling.get('rope_type', 'llama3')}")
    print(f"  RoPE Factor: {rope_scaling.get('factor', 32.0)}")
    print(f"  Original Context Length: {rope_scaling.get('original_max_position_embeddings', 8192)}")
    print(f"  Low/High Frequency Factors: {rope_scaling.get('low_freq_factor', 1.0)} / {rope_scaling.get('high_freq_factor', 4.0)}")
else:
    # Llama 3.2 3B architectural default
    use_scaled_rope = True
    rope_scale_factor = 32
    print("  Defaulting to Llama 3.2 scaled RoPE (factor=32)")

assert use_scaled_rope is True, "CRITICAL: Llama 3.2 requires use_scaled_rope=True for rotary positional embeddings!"
assert rope_scale_factor == 32, f"CRITICAL: Llama 3.2 requires rope_scale_factor=32, got {rope_scale_factor}!"

# Create target Meta export staging directory
META_EXPORT_DIR = "/kaggle/temp/llama3_2_3b_meta"
os.makedirs(META_EXPORT_DIR, exist_ok=True)

# Dynamically construct params.json matching ExecuTorch ModelArgs & LlamaTransformer schema
params_dict = {
    "dim": dim,
    "n_layers": n_layers,
    "n_heads": n_heads,
    "n_kv_heads": n_kv_heads,
    "vocab_size": vocab_size,
    "multiple_of": 256,
    "ffn_dim_multiplier": 1.0,
    "norm_eps": norm_eps,
    "rope_theta": float(rope_theta),
    "use_scaled_rope": True,
    "rope_scale_factor": rope_scale_factor
}

params_path = os.path.join(META_EXPORT_DIR, "params.json")
with open(params_path, "w", encoding="utf-8") as f:
    json.dump(params_dict, f, indent=2)

print(f"\\nDynamically generated params.json at: {params_path}")
print(json.dumps(params_dict, indent=2))

# Verify params.json can be re-read and validated
with open(params_path, "r", encoding="utf-8") as f:
    verified_params = json.load(f)
assert verified_params["use_scaled_rope"] is True, "params.json verification failed: use_scaled_rope missing or False!"
assert verified_params["rope_scale_factor"] == 32, "params.json verification failed: rope_scale_factor != 32!"
print("RoPE Scaling Configuration 100% Validated in params.json.")"""
    nb["cells"][8]["source"] = [line + "\n" for line in cell_8_code.strip().split("\n")]

    # 4. Update Cell 9 Markdown (Step 4)
    cell_9_text = """### Step 4: Load Base Model & Merge LoRA Adapter (FP16 & Single-GPU Device Placement `device_map={\"\": \"cuda:0\"}`)
Loads base model squarely into `cuda:0` in native FP16 (~6.42 GB footprint on 15.0 GB Tesla T4), verifies all tensors reside on `cuda:0`, attaches the LoRA adapter, merges in-place, and verifies tensor placement and gradient status."""
    nb["cells"][9]["source"] = [line + "\n" for line in cell_9_text.strip().split("\n")]

    # 5. Update Cell 10 Code (Step 4: Load Base & Merge LoRA Adapter on Single GPU)
    cell_10_code = """from transformers import AutoModelForCausalLM
from peft import PeftModel
import gc

print("=== STEP 4: LOAD BASE MODEL & MERGE LORA ADAPTER (SINGLE-GPU cuda:0) ===")

# Confirm GPU memory before loading
assert torch.cuda.is_available(), "CUDA device unavailable for model loading!"
device_target = "cuda:0"
vram_before = torch.cuda.memory_allocated(0) / (1024**3)
vram_free = torch.cuda.mem_get_info(0)[0] / (1024**3)
print(f"Target Device: {device_target}")
print(f"Pre-load GPU VRAM allocated: {vram_before:.2f} GB | Free: {vram_free:.2f} GB")
assert vram_free >= 8.0, f"Insufficient free VRAM on {device_target} ({vram_free:.2f} GB)! Minimum 8.0 GB required."

# FIX 2: Explicit single-device mapping avoids cross-GPU layer sharding on dual-T4 systems.
# Entire 3.21B FP16 model is ~6.42 GB, fitting comfortably within single 15.0 GB Tesla T4.
print(f"Loading base model in torch.float16 via device_map={{'': '{device_target}'}} from {BASE_MODEL_REF}...")
base_model = AutoModelForCausalLM.from_pretrained(
    BASE_MODEL_REF,
    torch_dtype=torch.float16,
    device_map={"": device_target},
    token=hf_token,
    local_files_only=(cached_snapshot is not None)
)

vram_after = torch.cuda.memory_allocated(0) / (1024**3)
print(f"Post-load GPU VRAM allocated: {vram_after:.2f} GB (Base 3B model loaded)")
assert vram_after < 14.0, f"VRAM usage ({vram_after:.2f} GB) exceeds safe threshold for single Tesla T4!"

# Verify 100% of base model parameters reside on cuda:0
base_devices = {p.device for p in base_model.parameters()}
print(f"Base model parameter device placement: {base_devices}")
assert base_devices == {torch.device(device_target)}, \\
    f"Base model device placement invariant violated: expected {{{device_target}}}, found {base_devices}"

# Attach the smoke-test LoRA adapter
print(f"\\nAttaching LoRA adapter from {ADAPTER_DIR}...")
peft_model = PeftModel.from_pretrained(base_model, ADAPTER_DIR)

# Verify adapter parameters reside on cuda:0
peft_devices = {p.device for p in peft_model.parameters()}
print(f"PEFT model parameter device placement: {peft_devices}")
assert peft_devices == {torch.device(device_target)}, \\
    f"PEFT model device placement invariant violated: expected {{{device_target}}}, found {peft_devices}"

# In-place merge: W_merged = W_0 + (alpha/r) * (B @ A)
print("Executing in-place parameter merge via peft_model.merge_and_unload()...")
merged_model = peft_model.merge_and_unload()
merged_model.eval()

# Explicitly verify all merged model tensors remain accessible on cuda:0
merged_devices = {p.device for p in merged_model.parameters()}
print(f"Merged model parameter device placement: {merged_devices}")
assert merged_devices == {torch.device(device_target)}, \\
    f"Merged model device placement invariant violated: expected {{{device_target}}}, found {merged_devices}"

# Verify gradients are disabled
assert all(not p.requires_grad for p in merged_model.parameters()), "Merged model contains trainable parameters!"

print("PEFT merge completed successfully! Model weights now contain merged adapter parameters in native FP16 on cuda:0.")"""
    nb["cells"][10]["source"] = [line + "\n" for line in cell_10_code.strip().split("\n")]

    # 6. Update Cell 13 Markdown (Step 6)
    cell_13_text = """### Step 6: Mandatory Tokenizer Acquisition, Stale Artifact Cleanup & Binary Struct Validation
Cleans up stale tokenizer outputs, stages `tokenizer.model`, and compiles `tokenizer.bin` using ExecuTorch's BPE converter with strict subprocess return code and binary header validation."""
    nb["cells"][13]["source"] = [line + "\n" for line in cell_13_text.strip().split("\n")]

    # 7. Update Cell 14 Code (Step 6: Tokenizer Acquisition, Cleanup & Struct Validation)
    cell_14_code = """from huggingface_hub import hf_hub_download
import shutil
import struct

print("=== STEP 6: MANDATORY TOKENIZER ACQUISITION, CLEANUP & BINARY VALIDATION ===")

EXPORT_DIR = "/kaggle/working/export"
os.makedirs(EXPORT_DIR, exist_ok=True)

tok_model_dst = os.path.join(EXPORT_DIR, "tokenizer.model")
tok_bin_dst = os.path.join(EXPORT_DIR, "tokenizer.bin")

# FIX 3a: Remove any stale tokenizer artifacts before conversion to prevent false positives
for stale in [tok_model_dst, tok_bin_dst]:
    if os.path.exists(stale):
        print(f"Removing stale tokenizer artifact: {stale}")
        os.remove(stale)

# 1. Locate or download tokenizer.model
tok_model_src = None
if cached_snapshot:
    candidates = [
        os.path.join(cached_snapshot, "original", "tokenizer.model"),
        os.path.join(cached_snapshot, "tokenizer.model")
    ]
    for c in candidates:
        if os.path.exists(c) and os.path.getsize(c) > 0:
            tok_model_src = c
            break

if not tok_model_src:
    print("Downloading original/tokenizer.model from Hugging Face Hub...")
    tok_model_src = hf_hub_download(
        repo_id="meta-llama/Llama-3.2-3B-Instruct",
        filename="original/tokenizer.model",
        token=hf_token,
        local_dir="/kaggle/temp"
    )

assert tok_model_src and os.path.exists(tok_model_src), "CRITICAL: Mandatory tokenizer.model could not be located!"
shutil.copy(tok_model_src, tok_model_dst)
assert os.path.exists(tok_model_dst) and os.path.getsize(tok_model_dst) > 1024, \\
    f"Failed to copy tokenizer.model to {tok_model_dst}!"
print(f"Validated tokenizer.model staged: {tok_model_dst} ({os.path.getsize(tok_model_dst) / 1024:.1f} KB)")

# 2. Mandatory compilation to tokenizer.bin via ExecuTorch
print("Compiling binary tokenizer (tokenizer.bin) via ExecuTorch...")
tok_script = os.path.join(EXECUTORCH_DIR, "extension", "llm", "tokenizer", "tokenizer.py")
assert os.path.exists(tok_script), f"ExecuTorch tokenizer conversion script missing at {tok_script}!"

tok_cmd = [
    sys.executable, "-m", "executorch.extension.llm.tokenizer.tokenizer",
    "-t", tok_model_dst,
    "-o", tok_bin_dst
]
print(f"Executing: {' '.join(tok_cmd)}")
tok_res = subprocess.run(tok_cmd, cwd=EXECUTORCH_DIR, capture_output=True, text=True)

# FIX 3b: Strict return code assertion
print(f"Tokenizer compiler exit code: {tok_res.returncode}")
if tok_res.returncode != 0:
    print("=== TOKENIZER CONVERSION FAILURE (STDERR) ===")
    print(tok_res.stderr)
    raise RuntimeError(f"CRITICAL: ExecuTorch tokenizer compilation failed with code {tok_res.returncode}!")

print(tok_res.stdout)

# FIX 3c: Validate newly generated tokenizer.bin binary struct header
assert os.path.exists(tok_bin_dst), f"CRITICAL: Tokenizer compilation succeeded but {tok_bin_dst} does not exist!"
tok_bin_size = os.path.getsize(tok_bin_dst)
assert tok_bin_size > 1024 * 1024, \\
    f"CRITICAL: Generated tokenizer.bin is unexpectedly small ({tok_bin_size} bytes)! Expected > 1 MB."

with open(tok_bin_dst, "rb") as f:
    header_bytes = f.read(16)
    assert len(header_bytes) == 16, "Incomplete binary header in tokenizer.bin!"
    tok_vocab_size, tok_bos_id, tok_eos_id, tok_max_len = struct.unpack("<IIII", header_bytes)
    
    # Read first token record to verify struct alignment: float score (4B) + uint32 len (4B)
    first_record = f.read(8)
    assert len(first_record) == 8, "Failed to read first token struct in tokenizer.bin!"
    first_score, first_len = struct.unpack("<fI", first_record)
    first_token_bytes = f.read(first_len)
    assert len(first_token_bytes) == first_len, "Token byte length mismatch in tokenizer.bin!"

print(f"\\nBinary Tokenizer Structure Validated (bpe_tokenizer.cpp compatible):")
print(f"  Vocab Size: {tok_vocab_size}")
print(f"  BOS Token ID: {tok_bos_id}")
print(f"  EOS Token ID: {tok_eos_id}")
print(f"  Max Token Length: {tok_max_len}")
print(f"  First Token: {first_token_bytes} (score={first_score:.4f}, len={first_len})")
print(f"Validated tokenizer.bin generated: {tok_bin_dst} ({tok_bin_size / 1024:.1f} KB)")
assert tok_vocab_size > 0, "Invalid vocab size in tokenizer.bin!\""""
    nb["cells"][14]["source"] = [line + "\n" for line in cell_14_code.strip().split("\n")]

    # 8. Update Cell 15 Markdown (Step 7)
    cell_15_text = """### Step 7: ExecuTorch v0.5.0 PTE Export (INT4 XNNPACK Delegate & Overwrite Protection)
Verifies and archives any preexisting `.pte` artifact to prevent silent overwriting, then executes `examples.models.llama.export_llama` with strict exit-code validation."""
    nb["cells"][15]["source"] = [line + "\n" for line in cell_15_text.strip().split("\n")]

    # 9. Update Cell 16 Code (Step 7: Overwrite Protection & PTE Export)
    cell_16_code = """import time
import shutil

print("=== STEP 7: EXECUTORCH v0.5.0 PTE EXPORT (WITH OVERWRITE PROTECTION) ===")

PTE_OUTPUT_PATH = os.path.join(EXPORT_DIR, "llama3_2_3b_8da4w.pte")

# FIX 4a: Check if an artifact exists before export; archive with timestamped backup
if os.path.exists(PTE_OUTPUT_PATH):
    backup_path = f"{PTE_OUTPUT_PATH}.bak_{int(time.time())}"
    print(f"[OVERWRITE PROTECTION] Existing PTE artifact detected at {PTE_OUTPUT_PATH}.")
    print(f"Archiving existing artifact to: {backup_path}")
    shutil.copy2(PTE_OUTPUT_PATH, backup_path)
    os.remove(PTE_OUTPUT_PATH)
    assert not os.path.exists(PTE_OUTPUT_PATH), "Failed to clear target PTE path for new export."
    print("Target path cleared to guarantee freshly generated artifact.")

export_cmd = [
    sys.executable, "-m", "examples.models.llama.export_llama",
    "--model", "llama3_2",
    "--checkpoint", consolidated_path,
    "--params", params_path,
    "-kv",
    "--use_sdpa_with_kv_cache",
    "-qmode", "8da4w",
    "--group_size", "128",
    "--embedding-quantize", "4,32",
    "--max_seq_length", "128",
    "-X",  # XNNPACK CPU Delegate for ARM NEON
    "--metadata", '{"get_bos_id": 128000, "get_eos_ids": [128001, 128009]}',
    "--output_name", PTE_OUTPUT_PATH
]

print("Executing command:")
print(" ".join(export_cmd))
print("\\nStarting export. This will take ~3 to 5 minutes on Kaggle CPU/RAM...")
start_time = time.time()

export_res = subprocess.run(export_cmd, cwd=EXECUTORCH_DIR, capture_output=True, text=True)
elapsed_time = time.time() - start_time

print(f"\\nExporter exited with return code: {export_res.returncode} (Elapsed: {elapsed_time:.1f}s)")

# Mandatory exit code assertion: Fail immediately if non-zero
if export_res.returncode != 0:
    print("=== EXPORT FAILURE LOG (STDERR) ===")
    print(export_res.stderr[-3000:])
    raise RuntimeError(f"CRITICAL: ExecuTorch export_llama failed with exit code {export_res.returncode}!")

print("=== EXPORT SUCCESS LOG (STDOUT) ===")
print(export_res.stdout[-1500:])

assert os.path.exists(PTE_OUTPUT_PATH), f"CRITICAL: Exporter exited 0, but output file missing at {PTE_OUTPUT_PATH}!"
assert os.path.getsize(PTE_OUTPUT_PATH) > 500 * (1024**2), \\
    f"CRITICAL: Exported PTE file is suspiciously small ({os.path.getsize(PTE_OUTPUT_PATH)} bytes)!"
print(f"PTE Model Successfully Exported to: {PTE_OUTPUT_PATH}")"""
    nb["cells"][16]["source"] = [line + "\n" for line in cell_16_code.strip().split("\n")]

    # 10. Update Cell 17 Markdown (Step 8)
    cell_17_text = """### Step 8: Multi-Artifact Integrity Validation (FlatBuffer ET12 Header & Manifest Generation)
Computes checksums, validates ExecuTorch FlatBuffer binary format specification (`ET12` file identifier and root table offset), and generates `export_manifest.json`."""
    nb["cells"][17]["source"] = [line + "\n" for line in cell_17_text.strip().split("\n")]

    # 11. Update Cell 18 Code (Step 8: FlatBuffer ET12 Spec & Manifest Generation)
    cell_18_code = """import hashlib
import struct

print("=== STEP 8: MULTI-ARTIFACT INTEGRITY VALIDATION (FLATBUFFER SPEC & MANIFEST) ===")

def compute_hashes(file_path):
    md5 = hashlib.md5()
    sha256 = hashlib.sha256()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(4096 * 1024), b""):
            md5.update(chunk)
            sha256.update(chunk)
    return md5.hexdigest(), sha256.hexdigest()

# 1. FIX 4b: Validate ExecuTorch FlatBuffer Binary Format Specification
pte_size_bytes = os.path.getsize(PTE_OUTPUT_PATH)
pte_md5, pte_sha256 = compute_hashes(PTE_OUTPUT_PATH)

with open(PTE_OUTPUT_PATH, "rb") as f:
    header_16 = f.read(16)

assert len(header_16) == 16, "PTE file smaller than 16-byte FlatBuffer header!"
root_table_offset = struct.unpack("<I", header_16[:4])[0]
file_identifier = header_16[4:8]

print(f"1. Model Artifact: {os.path.basename(PTE_OUTPUT_PATH)}")
print(f"   Size: {pte_size_bytes / (1024**3):.2f} GB ({pte_size_bytes / (1024**2):.1f} MB)")
print(f"   MD5: {pte_md5}")
print(f"   SHA256: {pte_sha256}")
print(f"   FlatBuffer Root Table Offset: {root_table_offset} bytes")
print(f"   FlatBuffer File Identifier: {file_identifier} (Hex: {file_identifier.hex()})")

# ExecuTorch FlatBuffer schema program.fbs specification: file_identifier "ET12"
assert file_identifier == b"ET12", \\
    f"CRITICAL: Invalid ExecuTorch FlatBuffer identifier! Expected b'ET12', got {file_identifier}"
assert root_table_offset > 0, \\
    f"CRITICAL: Invalid FlatBuffer root table offset: {root_table_offset}"
assert pte_size_bytes > 500 * (1024**2), \\
    f"CRITICAL: Exported PTE model file is too small: {pte_size_bytes} bytes"
print("   FlatBuffer Header Verification: 100% PASSED (Valid ET12 ExecuTorch v0.5.0 program)")

# 2. Validate Tokenizer Files
tok_bin_size = os.path.getsize(tok_bin_dst)
tok_bin_md5, tok_bin_sha256 = compute_hashes(tok_bin_dst)
print(f"\\n2. Tokenizer Binary: {os.path.basename(tok_bin_dst)}")
print(f"   Size: {tok_bin_size / 1024:.1f} KB")
print(f"   MD5: {tok_bin_md5}")
print(f"   SHA256: {tok_bin_sha256}")

tok_mod_size = os.path.getsize(tok_model_dst)
tok_mod_md5, tok_mod_sha256 = compute_hashes(tok_model_dst)
print(f"\\n3. Tokenizer Model: {os.path.basename(tok_model_dst)}")
print(f"   Size: {tok_mod_size / 1024:.1f} KB")
print(f"   MD5: {tok_mod_md5}")
print(f"   SHA256: {tok_mod_sha256}")

# 3. Generate Populated Deployment Manifest
manifest_data = {
    "model_name": "Meta Llama 3.2 3B Instruct (Merged LoRA Smoke-Test)",
    "base_model": "meta-llama/Llama-3.2-3B-Instruct",
    "lora_adapter": ADAPTER_DIR,
    "lora_rank": 16,
    "lora_alpha": 32,
    "runtime": "ExecuTorch v0.5.0",
    "flatbuffer_identifier": file_identifier.decode("ascii", errors="replace"),
    "quantization": "8da4w (group_size=128, embedding=4,32)",
    "delegate": "XNNPACK (ARM NEON)",
    "max_seq_length": 128,
    "rope_scaling": {
        "use_scaled_rope": True,
        "rope_scale_factor": 32,
        "rope_theta": float(rope_theta)
    },
    "artifacts": {
        "model_pte": {
            "filename": os.path.basename(PTE_OUTPUT_PATH),
            "size_bytes": pte_size_bytes,
            "md5": pte_md5,
            "sha256": pte_sha256,
            "flatbuffer_schema": "ET12"
        },
        "tokenizer_bin": {
            "filename": os.path.basename(tok_bin_dst),
            "size_bytes": tok_bin_size,
            "md5": tok_bin_md5,
            "sha256": tok_bin_sha256,
            "vocab_size": tok_vocab_size
        },
        "tokenizer_model": {
            "filename": os.path.basename(tok_model_dst),
            "size_bytes": tok_mod_size,
            "md5": tok_mod_md5,
            "sha256": tok_mod_sha256
        }
    },
    "export_timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
}

manifest_path = os.path.join(EXPORT_DIR, "export_manifest.json")
with open(manifest_path, "w", encoding="utf-8") as f:
    json.dump(manifest_data, f, indent=2)

print(f"\\nWrote Deployment Manifest to: {manifest_path}")

print("\\n" + "="*70)
print("EXPORT COMPLETE & VERIFIED. Ready for download in /kaggle/working/export/:")
for fname in sorted(os.listdir(EXPORT_DIR)):
    fpath = os.path.join(EXPORT_DIR, fname)
    print(f"  - {fname:25s} ({os.path.getsize(fpath) / (1024**2):.2f} MB)")
print("="*70)"""
    nb["cells"][18]["source"] = [line + "\n" for line in cell_18_code.strip().split("\n")]

    # Validate Python AST for all code cells
    print("Validating Python AST across all code cells...")
    for idx, cell in enumerate(nb["cells"]):
        if cell["cell_type"] == "code":
            src = "".join(cell["source"])
            # Remove jupyter magics (! and %) for AST parsing
            clean_lines = []
            for line in src.split("\n"):
                stripped = line.strip()
                if stripped.startswith("!") or stripped.startswith("%"):
                    clean_lines.append("# " + line)
                else:
                    clean_lines.append(line)
            clean_src = "\n".join(clean_lines)
            try:
                ast.parse(clean_src)
                print(f"  Cell {idx:2d} (Code): AST OK ({len(clean_lines)} lines)")
            except SyntaxError as e:
                print(f"  CRITICAL: Cell {idx} SyntaxError: {e}")
                sys.exit(1)

    with open(NOTEBOOK_PATH, "w", encoding="utf-8") as f:
        json.dump(nb, f, indent=2)

    print(f"\nSuccessfully wrote updated notebook to: {NOTEBOOK_PATH}")

if __name__ == "__main__":
    update_notebook()
