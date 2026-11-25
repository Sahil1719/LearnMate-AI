"""
LearnMate AI - Meta Llama 3 8B Instruct Cloud Export Pipeline
Export Target: ExecuTorch v0.5.0 with TorchAO 8da4w (INT4 groupwise weights, INT8 dynamic activations)
Target Hardware: ARM64 Android (Qualcomm Snapdragon 662, Redmi 9 Power 4GB RAM)
Backend Delegation: XNNPACK (-X flag enabled for ARM NEON acceleration)
"""

import os
import sys
import subprocess
import shutil
from pathlib import Path

MODEL_ID = "meta-llama/Meta-Llama-3-8B-Instruct"
OUTPUT_PTE_NAME = "llama3_8b_8da4w.pte"
GROUP_SIZE = 128
EMBEDDING_QUANTIZE = "4,32"  # 4-bit embedding quantization with group size 32 (saves ~750MB)
MAX_SEQ_LENGTH = 512         # Bounded context window to restrain KV cache memory on mobile

def check_environment():
    print("=== Step 1: Checking System Memory & Storage ===")
    import psutil
    ram_gb = psutil.virtual_memory().total / (1024**3)
    print(f"Total System RAM: {ram_gb:.2f} GB")
    if ram_gb < 24.0:
        print(f"WARNING: Host RAM ({ram_gb:.2f} GB) is below the recommended 24-32 GB.")
        print("Export may encounter out-of-memory (OOM) errors without adequate swap.")
    else:
        print("System RAM is sufficient for 8B model tracing.")

def setup_dependencies():
    print("\n=== Step 2: Verifying Dependencies ===")
    import torch
    import torchao
    print(f"PyTorch Version: {torch.__version__} (CUDA Available: {torch.cuda.is_available()})")
    print(f"TorchAO Version: {torchao.__version__}")
    
    # Verify Int8DynActInt4WeightQuantizer
    from torchao.quantization.quant_api import Int8DynActInt4WeightQuantizer
    print("Successfully verified Int8DynActInt4WeightQuantizer from TorchAO.")

def download_checkpoint(destination_dir: str):
    print(f"\n=== Step 3: Downloading Llama 3 8B Checkpoint to {destination_dir} ===")
    from huggingface_hub import snapshot_download
    os.makedirs(destination_dir, exist_ok=True)
    
    path = snapshot_download(
        repo_id=MODEL_ID,
        local_dir=destination_dir,
        allow_patterns=[
            "*.safetensors",
            "model.safetensors.index.json",
            "params.json",
            "original/params.json",
            "tokenizer.json",
            "original/tokenizer.model"
        ]
    )
    print(f"Download complete: {path}")
    return path

def run_export(checkpoint_dir: str, output_dir: str):
    print("\n=== Step 4: Executing ExecuTorch Llama Export with XNNPACK Delegation ===")
    os.makedirs(output_dir, exist_ok=True)
    pte_output_path = os.path.join(output_dir, OUTPUT_PTE_NAME)
    
    # Locate params.json
    params_path = os.path.join(checkpoint_dir, "original", "params.json")
    if not os.path.exists(params_path):
        params_path = os.path.join(checkpoint_dir, "params.json")
        
    cmd = [
        sys.executable, "-m", "examples.models.llama.export_llama",
        "--checkpoint", checkpoint_dir,
        "--params", params_path,
        "-kv",
        "--use_sdpa_with_kv_cache",
        "-qmode", "8da4w",
        "--group_size", str(GROUP_SIZE),
        "--embedding-quantize", EMBEDDING_QUANTIZE,
        "--max_seq_length", str(MAX_SEQ_LENGTH),
        "-X",  # Critical: Enables XNNPACK delegate for DQLinear operators on mobile CPU
        "--output_name", pte_output_path
    ]
    
    print("Executing command:")
    print(" ".join(cmd))
    subprocess.run(cmd, check=True)
    
    if os.path.exists(pte_output_path):
        size_gb = os.path.getsize(pte_output_path) / (1024**3)
        print(f"\nSUCCESS: Exported artifact {pte_output_path} ({size_gb:.2f} GB)")
    else:
        print(f"\nERROR: Output artifact not found at {pte_output_path}")

if __name__ == "__main__":
    check_environment()
    setup_dependencies()
