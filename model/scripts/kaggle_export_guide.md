# Kaggle / Colab Cloud Export Guide: Meta Llama 3 8B to ExecuTorch 8da4w

This guide details how to perform the one-time quantization and export of Meta Llama 3 8B Instruct using **Kaggle (100% Free, 30GB RAM)** or **Google Colab Pro (51GB High-RAM)**.

---

## Option A: Kaggle Notebook (Recommended & 100% Free)

### Step 1: Create a New Kaggle Notebook
1. Go to [kaggle.com/code](https://www.kaggle.com/code) and click **New Notebook**.
2. In the right-hand **Notebook Settings**:
   - **Accelerator:** None (or GPU T4 × 2). *Kaggle provides ~30 GB of system RAM.*
   - **Internet:** On (Required to pull weights and packages).

### Step 2: Add Your Hugging Face Token as a Secret
1. In the top menu, click **Add-ons** -> **Secrets**.
2. Add a new secret with Label: `HF_TOKEN` and Value: `your_huggingface_read_token`.

### Step 3: Run the Export Pipeline
Create code cells in the notebook with the following:

#### Cell 1: Install Dependencies
```bash
!pip install -q --upgrade pip
!pip install -q torch==2.6.0 torchvision==0.21.0 torchaudio==2.6.0 --index-url https://download.pytorch.org/whl/cpu
!pip install -q torchao==0.8.0 sentencepiece tiktoken blobfile huggingface_hub

# Clone ExecuTorch v0.5.0
!git clone --depth 1 --branch v0.5.0 https://github.com/pytorch/executorch.git
%cd executorch
!pip install -e .
```

#### Cell 2: Authenticate and Download Checkpoint (~16 GB)
```python
from kaggle_secrets import UserSecretsClient
from huggingface_hub import snapshot_download
import os

user_secrets = UserSecretsClient()
hf_token = user_secrets.get_secret("HF_TOKEN")

checkpoint_dir = "/kaggle/temp/llama3_8b"
os.makedirs(checkpoint_dir, exist_ok=True)

print("Downloading Meta Llama 3 8B Instruct consolidated checkpoint, params and tokenizer...")
snapshot_download(
    repo_id="meta-llama/Meta-Llama-3-8B-Instruct",
    token=hf_token,
    local_dir=checkpoint_dir,
    allow_patterns=[
        "original/consolidated.00.pth",
        "original/params.json",
        "original/tokenizer.model"
    ]
)
print("Download complete.")
```

#### Cell 3: Execute ExecuTorch Quantization & Export with XNNPACK Delegation
```bash
CHECKPOINT_PATH="/kaggle/temp/llama3_8b/original/consolidated.00.pth"
PARAMS_PATH="/kaggle/temp/llama3_8b/original/params.json"

mkdir -p /kaggle/working/export

python -m examples.models.llama.export_llama \
  --model llama3 \
  --checkpoint "$CHECKPOINT_PATH" \
  --params "$PARAMS_PATH" \
  -kv \
  --use_sdpa_with_kv_cache \
  -qmode 8da4w \
  --group_size 128 \
  --embedding-quantize 4,32 \
  --max_seq_length 512 \
  -X \
  --metadata '{"get_bos_id": 128000, "get_eos_ids": [128001, 128009]}' \
  --output_name /kaggle/working/export/llama3_8b_8da4w.pte
```

#### Cell 4: Copy Tokenizer & Verify Artifact
```python
import os
import shutil

pte_path = "/kaggle/working/export/llama3_8b_8da4w.pte"
size_gb = os.path.getsize(pte_path) / (1024**3)
print(f"Exported .pte Model Size: {size_gb:.2f} GB")

# Copy tokenizer to output
tokenizer_src = "/kaggle/temp/llama3_8b/original/tokenizer.model"
if not os.path.exists(tokenizer_src):
    tokenizer_src = "/kaggle/temp/llama3_8b/tokenizer.json"
shutil.copy(tokenizer_src, "/kaggle/working/export/")
print("Export ready for download in /kaggle/working/export/")
```

### Step 4: Download to Local Laptop
Download `llama3_8b_8da4w.pte` and `tokenizer.model` directly from the Kaggle Output tab to your laptop (`<PROJECT_ROOT>\model\export\`).

---

## Option B: Deploying the Artifact to Redmi 9 Power (or Realme X7 5G) via ADB

Once downloaded locally to `model/export/`:
```powershell
# For Primary Target: Xiaomi Redmi 9 Power (4GB RAM, Snapdragon 662)
# Option 1: User-accessible Download folder (Recommended, zero permissions needed)
adb -s <DEVICE_SERIAL> push model/export/llama3_8b_8da4w.pte /sdcard/Download/
adb -s <DEVICE_SERIAL> push model/export/tokenizer.model /sdcard/Download/

# Option 2: High-speed local temp directory (for standalone runner)
adb -s <DEVICE_SERIAL> shell mkdir -p /data/local/tmp/llama
adb -s <DEVICE_SERIAL> push model/export/llama3_8b_8da4w.pte /data/local/tmp/llama/
adb -s <DEVICE_SERIAL> push model/export/tokenizer.model /data/local/tmp/llama/

# For Secondary Reference Device: Realme X7 5G (8GB RAM, Dimensity 800U)
# adb -s 8DPB7LC6LN7DH6YL push model/export/llama3_8b_8da4w.pte /sdcard/Download/
```
