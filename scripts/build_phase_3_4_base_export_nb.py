import json
import os
import sys
import ast


def build_notebook():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out_notebook = os.path.join(repo_root, "model", "learnmate_phase_3_4_base_export.ipynb")

    cells = []

    def add_md(text):
        cells.append({
            "cell_type": "markdown",
            "metadata": {},
            "source": [line + "\n" for line in text.strip().split("\n")]
        })

    def add_code(text):
        cells.append({
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [line + "\n" for line in text.strip().split("\n")]
        })

    # Cell 0: Header
    add_md("""# LearnMate AI | Phase 3.4: Original Base Model Download & ExecuTorch INT4 Export
**Model:** `meta-llama/Llama-3.2-3B-Instruct` (Original Unmerged Base Checkpoint)  
**Runtime:** ExecuTorch `v0.5.0` with XNNPACK CPU Delegate (`-X`)  
**Quantization:** `8da4w` (INT8 dynamic activations, INT4 groupwise weights, `group_size=128`, `embedding-quantize=4,32`)  
**Target Artifact:** `base_llama3_2_3b.pte`  
**Target Hardware:** Xiaomi Redmi 9 Power (Snapdragon 662, ARM64-v8a, 4 GB RAM, Android 12)

### Strict Phase 3.4 Invariants:
1. **NO Adapter / NO Fine-Tuning:** Loads pristine Meta Llama 3.2 3B Instruct weights only. Zero PEFT/LoRA adapters attached or merged.
2. **Exact Configuration Parity with Merged PTE (`learnmate_llama3_2_3b.pte`):**
   - `max_seq_length = 128`
   - `use_kv_cache = 1` (`-kv`)
   - `use_sdpa_with_kv_cache = 0` (disabled to match verified merged PTE metadata)
   - `enable_dynamic_shape = 1` (default)
   - `get_bos_id = 128000`, `get_eos_ids = [128009, 128001]`
   - Same `tokenizer.model` (`SHA-256: 82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55`)
3. **Zero Token Exposure:** Hugging Face credentials are read safely without ever printing token values to logs.
4. **Isolated Python 3.12 Export Environment:** Uses `uv` to stage an isolated Python 3.12 + PyTorch 2.6.0 + TorchAO 0.8.0 + ExecuTorch v0.5.0 toolchain so container updates (e.g. Python 3.13 / PyTorch 2.11) never break `export_llama`.""")

    # Cell 1: Preflight Hardware & Environment Gate
    add_md("### Step 1: Preflight Hardware, Environment & Storage Verification")
    add_code("""import os
import sys
import json
import time
import shutil
import psutil
import platform
import torch

print("=== STEP 1: PREFLIGHT HARDWARE & ENVIRONMENT VERIFICATION ===")
print(f"Timestamp (UTC): {time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())}")
print(f"OS / Platform: {platform.platform()}")
print(f"Host Python Version: {sys.version}")
print(f"Host PyTorch Version: {torch.__version__}")

assert torch.cuda.is_available(), "CRITICAL: CUDA GPU is required! Enable GPU T4/P100 in Kaggle settings."
gpu_name = torch.cuda.get_device_name(0)
cc = torch.cuda.get_device_capability(0)
vram_free_gb = torch.cuda.mem_get_info(0)[0] / (1024**3)
vram_total_gb = torch.cuda.mem_get_info(0)[1] / (1024**3)
print(f"GPU 0: {gpu_name} (Compute Capability {cc[0]}.{cc[1]})")
print(f"GPU 0 VRAM: {vram_free_gb:.2f} GB Free / {vram_total_gb:.2f} GB Total")
assert vram_free_gb >= 8.0, f"Insufficient free VRAM ({vram_free_gb:.2f} GB)! Minimum 8.0 GB required."

ram_total_gb = psutil.virtual_memory().total / (1024**3)
ram_avail_gb = psutil.virtual_memory().available / (1024**3)
working_disk = shutil.disk_usage("/kaggle/working")
working_free_gb = working_disk.free / (1024**3)
tmp_free_gb = shutil.disk_usage("/tmp").free / (1024**3)
print(f"System RAM: {ram_avail_gb:.2f} GB Available / {ram_total_gb:.2f} GB Total")
print(f"Disk Free (/kaggle/working): {working_free_gb:.2f} GB | (/tmp): {tmp_free_gb:.2f} GB")

assert ram_avail_gb >= 12.0, f"Insufficient system RAM ({ram_avail_gb:.2f} GB)! Minimum 12.0 GB required."
assert working_free_gb >= 15.0, f"Insufficient disk space ({working_free_gb:.2f} GB)! Minimum 15.0 GB required."

# Secure HF_TOKEN retrieval (never logged)
hf_token = None
try:
    from kaggle_secrets import UserSecretsClient
    hf_token = UserSecretsClient().get_secret("HF_TOKEN")
    assert hf_token and len(hf_token) > 10
    print("Authentication: HF_TOKEN retrieved from Kaggle Secrets (value redacted).")
except Exception:
    hf_token = os.environ.get("HF_TOKEN", None)
    assert hf_token and len(hf_token) > 10, "CRITICAL: HF_TOKEN missing in Kaggle Secrets and environment!"
    print("Authentication: HF_TOKEN retrieved from environment variable (value redacted).")

env_report = {
    "host_python_version": sys.version,
    "host_pytorch_version": torch.__version__,
    "gpu_name": gpu_name,
    "gpu_vram_total_gb": round(vram_total_gb, 2),
    "gpu_vram_free_gb": round(vram_free_gb, 2),
    "system_ram_total_gb": round(ram_total_gb, 2),
    "working_disk_free_gb": round(working_free_gb, 2),
}
print("[PREFLIGHT GATE: PASSED]")""")

    # Cell 2: Dependency & ExecuTorch v0.5.0 Setup (Isolated Python 3.12 venv)
    add_md("### Step 2: Isolated Python 3.12 + PyTorch 2.6.0 + ExecuTorch v0.5.0 Setup")
    add_code("""import subprocess

print("=== STEP 2: ISOLATED PYTHON 3.12 + EXECUTORCH v0.5.0 SETUP ===")
subprocess.run([sys.executable, "-m", "pip", "install", "-q", "uv"], check=True)

ET_VENV = "/tmp/et_py312"
ET_PY = os.path.join(ET_VENV, "bin", "python")
ET_BIN = os.path.join(ET_VENV, "bin")

if not os.path.exists(ET_PY):
    print("Creating isolated Python 3.12 environment via uv...")
    subprocess.run(["uv", "venv", "--seed", "--python", "3.12", ET_VENV], check=True)

print("Installing PyTorch 2.6.0 (CPU) + TorchAO 0.8.0 + ExecuTorch 0.5.0 (PyPI cp312 wheel)...")
subprocess.run([
    "uv", "pip", "install", "--python", ET_PY,
    "torch==2.6.0", "torchvision==0.21.0", "torchaudio==2.6.0",
    "--index-url", "https://download.pytorch.org/whl/cpu",
], check=True)

subprocess.run([
    "uv", "pip", "install", "--python", ET_PY,
    "torchao==0.8.0", "sentencepiece", "tiktoken", "blobfile",
    "setuptools<75", "wheel", "cmake==3.31.6", "ninja", "zstd", "pyyaml", "tomli",
    "numpy==2.0.0", "pandas==2.2.2", "flatbuffers", "ruamel.yaml",
    "sympy", "tabulate", "typing-extensions", "mpmath==1.3.0",
    "packaging", "expecttest", "hypothesis", "parameterized", "pytest", "pytest-xdist",
], check=True)

subprocess.run([
    "uv", "pip", "install", "--python", ET_PY,
    "executorch==0.5.0", "--no-deps",
], check=True)

et_env = os.environ.copy()
et_env["PATH"] = f"{ET_BIN}:{et_env['PATH']}"

verify_code = (
    "import sys, torch, torchao, executorch; "
    "import executorch.examples.models.checkpoint; "
    "from executorch.examples.models.llama import export_llama_lib; "
    "print('PY:', sys.version.split()[0]); "
    "print('TORCH:', torch.__version__); "
    "print('TORCHAO:', torchao.__version__); "
    "print('EXECUTORCH:', getattr(executorch, '__version__', '0.5.0'))"
)
verify_out = subprocess.check_output(
    [ET_PY, "-c", verify_code],
    cwd="/tmp",
    env=et_env,
    text=True,
)
print(verify_out.strip())

env_report["export_python_venv"] = ET_VENV
env_report["executorch_version"] = "0.5.0"
env_report["executorch_package_source"] = "PyPI executorch==0.5.0 (cp312-manylinux1_x86_64)"
env_report["export_torch_version"] = "2.6.0+cpu"
env_report["export_torchao_version"] = "0.8.0"
env_report["cmake_version_pinned"] = "3.31.6"
print("[DEPENDENCY GATE: PASSED]")""")

    # Cell 3: Repository, License & File Verification + Base Download
    add_md("### Step 3: Verify Authorized Repository, License & Download Original Base Checkpoint")
    add_code("""import hashlib
from huggingface_hub import HfApi, snapshot_download

REPO_ID = "meta-llama/Llama-3.2-3B-Instruct"
EXPECTED_TOKENIZER_SHA256 = "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55"

print(f"=== STEP 3: REPOSITORY VERIFICATION & DOWNLOAD ({REPO_ID}) ===")
api = HfApi(token=hf_token)
who = api.whoami()
print(f"Authenticated HF Account: {who.get('name')} (type={who.get('type')})")

info = api.model_info(REPO_ID)
print(f"Repository Commit SHA: {info.sha}")
print(f"Gated Status: {info.gated} | Last Modified: {info.last_modified}")

required_patterns = [
    "LICENSE.txt",
    "USE_POLICY.md",
    "config.json",
    "generation_config.json",
    "model-00001-of-00002.safetensors",
    "model-00002-of-00002.safetensors",
    "model.safetensors.index.json",
    "special_tokens_map.json",
    "tokenizer.json",
    "tokenizer_config.json",
    "original/tokenizer.model",
    "original/params.json",
]

BASE_DOWNLOAD_DIR = "/tmp/base_hf_checkpoint"
os.makedirs(BASE_DOWNLOAD_DIR, exist_ok=True)

print(f"Downloading original base checkpoint files to {BASE_DOWNLOAD_DIR}...")
snapshot_download(
    repo_id=REPO_ID,
    revision=info.sha,
    token=hf_token,
    local_dir=BASE_DOWNLOAD_DIR,
    allow_patterns=required_patterns,
)

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(4 * 1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

downloaded_files = {}
print("\\nDownloaded File Inventory & SHA-256 Verification:")
for root, _, files in os.walk(BASE_DOWNLOAD_DIR):
    if ".cache" in root:
        continue
    for fn in sorted(files):
        fp = os.path.join(root, fn)
        rel = os.path.relpath(fp, BASE_DOWNLOAD_DIR).replace("\\\\", "/")
        sz = os.path.getsize(fp)
        digest = sha256_file(fp)
        downloaded_files[rel] = {"size_bytes": sz, "sha256": digest}
        print(f"  {rel:40s} | {sz:13,d} bytes | {digest}")

tok_orig_rel = "original/tokenizer.model"
assert tok_orig_rel in downloaded_files, "Missing original/tokenizer.model!"
assert downloaded_files[tok_orig_rel]["sha256"] == EXPECTED_TOKENIZER_SHA256, (
    f"Tokenizer SHA-256 mismatch! Got {downloaded_files[tok_orig_rel]['sha256']}"
)
print("\\nVerified: original/tokenizer.model matches production tokenizer SHA-256.")
print("[DOWNLOAD & PROVENANCE GATE: PASSED]")""")

    # Cell 4: Architecture Verification & params.json Generation
    add_md("### Step 4: Architecture Validation & `params.json` Generation")
    add_code("""from transformers import AutoConfig

print("=== STEP 4: ARCHITECTURE VALIDATION & PARAMS.JSON ===")
model_config = AutoConfig.from_pretrained(BASE_DOWNLOAD_DIR, local_files_only=True)

dim = model_config.hidden_size
n_layers = model_config.num_hidden_layers
n_heads = model_config.num_attention_heads
n_kv_heads = model_config.num_key_value_heads
vocab_size = model_config.vocab_size
intermediate_size = model_config.intermediate_size
norm_eps = model_config.rms_norm_eps
rope_theta = float(getattr(model_config, "rope_theta", 500000.0))

assert dim == 3072, f"dim={dim} != 3072"
assert n_layers == 28, f"n_layers={n_layers} != 28"
assert n_heads == 24, f"n_heads={n_heads} != 24"
assert n_kv_heads == 8, f"n_kv_heads={n_kv_heads} != 8"
assert vocab_size == 128256, f"vocab_size={vocab_size} != 128256"

rope_scaling = getattr(model_config, "rope_scaling", None)
rope_scale_factor = int(rope_scaling.get("factor", 32)) if rope_scaling else 32
assert rope_scale_factor == 32, f"Expected rope_scale_factor=32, got {rope_scale_factor}"

META_EXPORT_DIR = "/tmp/base_llama3_2_3b_meta"
os.makedirs(META_EXPORT_DIR, exist_ok=True)

params_dict = {
    "dim": dim,
    "n_layers": n_layers,
    "n_heads": n_heads,
    "n_kv_heads": n_kv_heads,
    "vocab_size": vocab_size,
    "multiple_of": 256,
    "ffn_dim_multiplier": 1.0,
    "norm_eps": norm_eps,
    "rope_theta": rope_theta,
    "use_scaled_rope": True,
    "rope_scale_factor": rope_scale_factor,
}
params_path = os.path.join(META_EXPORT_DIR, "params.json")
with open(params_path, "w", encoding="utf-8") as f:
    json.dump(params_dict, f, indent=2)

print("Generated params.json:")
print(json.dumps(params_dict, indent=2))
print("[ARCHITECTURE GATE: PASSED]")""")

    # Cell 5: Load Original FP16 Base Model (NO ADAPTER) & Remap to Meta Consolidated
    add_md("### Step 5: Load Pristine FP16 Base Model (Zero Adapter) & Convert to Meta Consolidated")
    add_code("""import gc
from transformers import AutoModelForCausalLM

print("=== STEP 5: LOAD ORIGINAL BASE MODEL (NO ADAPTER) & REMAP ===")
print("Loading unmerged base model in torch.float16 on cuda:0...")
base_model = AutoModelForCausalLM.from_pretrained(
    BASE_DOWNLOAD_DIR,
    torch_dtype=torch.float16,
    device_map={"": "cuda:0"},
    local_files_only=True,
)
base_model.eval()

total_params = sum(p.numel() for p in base_model.parameters())
print(f"Loaded base model parameters: {total_params:,} ({total_params / 1e9:.3f}B)")

hf_sd = base_model.state_dict()
meta_state_dict = {}

key_mapping = {
    "self_attn.q_proj.weight": "attention.wq.weight",
    "self_attn.k_proj.weight": "attention.wk.weight",
    "self_attn.v_proj.weight": "attention.wv.weight",
    "self_attn.o_proj.weight": "attention.wo.weight",
    "mlp.gate_proj.weight": "feed_forward.w1.weight",
    "mlp.down_proj.weight": "feed_forward.w2.weight",
    "mlp.up_proj.weight": "feed_forward.w3.weight",
    "input_layernorm.weight": "attention_norm.weight",
    "post_attention_layernorm.weight": "ffn_norm.weight",
}

for k, v in hf_sd.items():
    v_cpu = v.detach().cpu()
    if k == "model.embed_tokens.weight":
        meta_state_dict["tok_embeddings.weight"] = v_cpu
    elif k == "model.norm.weight":
        meta_state_dict["norm.weight"] = v_cpu
    elif k == "lm_head.weight":
        meta_state_dict["output.weight"] = v_cpu
    elif k.startswith("model.layers."):
        parts = k.split(".")
        layer_idx = parts[2]
        sub = ".".join(parts[3:])
        if sub in key_mapping:
            meta_state_dict[f"layers.{layer_idx}.{key_mapping[sub]}"] = v_cpu

EXPECTED_TENSOR_COUNT = 1 + (n_layers * 9) + 1 + 1  # 255
assert len(meta_state_dict) == EXPECTED_TENSOR_COUNT, (
    f"Expected {EXPECTED_TENSOR_COUNT} tensors, got {len(meta_state_dict)}"
)
invalid_dtypes = [k for k, v in meta_state_dict.items() if v.dtype != torch.float16]
assert not invalid_dtypes, f"Non-FP16 tensors found: {invalid_dtypes[:5]}"

consolidated_path = os.path.join(META_EXPORT_DIR, "consolidated.00.pth")
torch.save(meta_state_dict, consolidated_path)
print(f"Saved unmerged Meta consolidated.00.pth: {os.path.getsize(consolidated_path):,} bytes")

del base_model, hf_sd, meta_state_dict
gc.collect()
torch.cuda.empty_cache()

# Clean up raw HF safetensors shards after hashing and converting to consolidated.00.pth
for shard in ["model-00001-of-00002.safetensors", "model-00002-of-00002.safetensors"]:
    sp = os.path.join(BASE_DOWNLOAD_DIR, shard)
    if os.path.exists(sp):
        os.remove(sp)
        print(f"Cleaned temporary HF shard to free disk: {shard}")

print("[CONSOLIDATED BASE GATE: PASSED]")""")

    # Cell 6: ExecuTorch v0.5.0 PTE Export
    add_md("### Step 6: ExecuTorch v0.5.0 Export (`base_llama3_2_3b.pte`)")
    add_code("""import struct

print("=== STEP 6: EXECUTORCH v0.5.0 BASE PTE EXPORT ===")
EXPORT_DIR = "/kaggle/working/export"
os.makedirs(EXPORT_DIR, exist_ok=True)

tok_model_dst = os.path.join(EXPORT_DIR, "tokenizer.model")
shutil.copy2(os.path.join(BASE_DOWNLOAD_DIR, "original", "tokenizer.model"), tok_model_dst)
assert sha256_file(tok_model_dst) == EXPECTED_TOKENIZER_SHA256

PTE_OUTPUT_PATH = os.path.join(EXPORT_DIR, "base_llama3_2_3b.pte")
if os.path.exists(PTE_OUTPUT_PATH):
    os.remove(PTE_OUTPUT_PATH)

# Exact parity with verified learnmate_llama3_2_3b.pte metadata:
# use_kv_cache = 1 (-kv), use_sdpa_with_kv_cache = 0 (flag omitted),
# enable_dynamic_shape = 1 (default), get_max_seq_len = 128,
# get_bos_id = 128000, get_eos_ids = [128009, 128001]
export_cmd = [
    ET_PY, "-m", "executorch.examples.models.llama.export_llama",
    "--model", "llama3_2",
    "--checkpoint", consolidated_path,
    "--params", params_path,
    "-kv",
    "-qmode", "8da4w",
    "--group_size", "128",
    "--embedding-quantize", "4,32",
    "--max_seq_length", "128",
    "-X",
    "--metadata", '{"get_bos_id": 128000, "get_eos_ids": [128009, 128001]}',
    "--output_name", PTE_OUTPUT_PATH,
]

print("Executing:", " ".join(export_cmd))
t0 = time.time()
res = subprocess.run(export_cmd, cwd="/tmp", env=et_env, capture_output=True, text=True)
export_elapsed_s = time.time() - t0
print(f"Exit code: {res.returncode} | Elapsed: {export_elapsed_s:.1f}s")
if res.returncode != 0:
    print(res.stdout[-1500:])
    print(res.stderr[-3000:])
    raise RuntimeError("ExecuTorch export_llama failed!")

pte_size = os.path.getsize(PTE_OUTPUT_PATH)
pte_sha256 = sha256_file(PTE_OUTPUT_PATH)
with open(PTE_OUTPUT_PATH, "rb") as f:
    header_16 = f.read(16)
root_offset = struct.unpack("<I", header_16[:4])[0]
file_id = header_16[4:8]
assert file_id == b"ET12", f"Invalid FlatBuffer ID: {file_id}"

print(f"Exported Base PTE: {PTE_OUTPUT_PATH}")
print(f"  Size: {pte_size:,} bytes ({pte_size / (1024**2):.2f} MiB)")
print(f"  SHA-256: {pte_sha256}")
print(f"  FlatBuffer: {file_id} (root_offset={root_offset})")

# Preserve params.json in export dir, then remove large temporary consolidated checkpoint
shutil.copy2(params_path, os.path.join(EXPORT_DIR, "params.json"))
if os.path.exists(consolidated_path):
    os.remove(consolidated_path)
    print("Cleaned temporary consolidated.00.pth after verified PTE export.")

print("[EXPORT GATE: PASSED]")""")

    # Cell 7: Fast FlatBuffer & Python Runtime Verification + Manifest
    add_md("### Step 7: PTE Program Load Verification & Export Manifest")
    add_code("""print("=== STEP 7: PTE PROGRAM LOAD VERIFICATION & EXPORT MANIFEST ===")

# Verify PTE loads cleanly in PyPI ExecuTorch v0.5.0 Python Runtime (method inspection)
rt_check_code = (
    "from executorch.runtime import Runtime; "
    "rt = Runtime.get(); "
    f"prog = rt.load_program('{PTE_OUTPUT_PATH}'); "
    "print('Loaded program methods:', sorted(list(prog.method_names)))"
)
rt_res = subprocess.run(
    [ET_PY, "-c", rt_check_code],
    cwd="/tmp",
    env=et_env,
    capture_output=True,
    text=True,
)
print("Runtime load_program exit code:", rt_res.returncode)
print("Runtime stdout:", rt_res.stdout.strip())
if rt_res.returncode != 0:
    print("Runtime stderr:", rt_res.stderr.strip())
assert rt_res.returncode == 0, "ExecuTorch Runtime.load_program() verification failed!"

manifest = {
    "phase": "3.4",
    "model_name": "Meta Llama 3.2 3B Instruct (Original Unmerged Base)",
    "repository": REPO_ID,
    "repository_commit_sha": info.sha,
    "adapter_merged": False,
    "environment": env_report,
    "export_configuration": {
        "model": "llama3_2",
        "quantization": "8da4w",
        "group_size": 128,
        "embedding_quantize": "4,32",
        "max_seq_length": 128,
        "use_kv_cache": True,
        "use_sdpa_with_kv_cache": False,
        "enable_dynamic_shape": True,
        "xnnpack": True,
        "get_bos_id": 128000,
        "get_eos_ids": [128009, 128001],
    },
    "downloaded_files": downloaded_files,
    "artifacts": {
        "base_pte": {
            "filename": "base_llama3_2_3b.pte",
            "size_bytes": pte_size,
            "sha256": pte_sha256,
            "flatbuffer_id": "ET12",
            "root_table_offset": root_offset,
        },
        "tokenizer_model": {
            "filename": "tokenizer.model",
            "size_bytes": os.path.getsize(tok_model_dst),
            "sha256": EXPECTED_TOKENIZER_SHA256,
        },
        "params_json": {
            "filename": "params.json",
            "size_bytes": os.path.getsize(os.path.join(EXPORT_DIR, "params.json")),
            "sha256": sha256_file(os.path.join(EXPORT_DIR, "params.json")),
        },
    },
    "runtime_load_verification": {
        "exit_code": rt_res.returncode,
        "stdout": rt_res.stdout.strip(),
    },
    "export_elapsed_seconds": round(export_elapsed_s, 2),
}

manifest_out = os.path.join(EXPORT_DIR, "base_export_manifest.json")
with open(manifest_out, "w", encoding="utf-8") as f:
    json.dump(manifest, f, indent=2)
print(f"Saved base_export_manifest.json to {manifest_out}")
print("Export directory contents:")
for fn in sorted(os.listdir(EXPORT_DIR)):
    fp = os.path.join(EXPORT_DIR, fn)
    print(f"  - {fn:30s} ({os.path.getsize(fp):,} bytes)")
print("[EXPORT & MANIFEST GATE: PASSED]")""")

    notebook_obj = {
        "cells": cells,
        "metadata": {
            "accelerator": "GPU",
            "kaggle": {
                "accelerator": "gpu",
                "isGpuEnabled": True,
                "isInternetEnabled": True,
            },
            "language_info": {
                "name": "python",
                "version": "3.10.12",
            },
        },
        "nbformat": 4,
        "nbformat_minor": 2,
    }

    for idx, c in enumerate(cells):
        if c["cell_type"] == "code":
            src = "".join(c["source"])
            ast.parse(src)
            print(f"Cell {idx} AST OK")

    with open(out_notebook, "w", encoding="utf-8") as f:
        json.dump(notebook_obj, f, indent=2)

    print(f"Wrote {out_notebook} ({os.path.getsize(out_notebook):,} bytes)")


if __name__ == "__main__":
    build_notebook()
