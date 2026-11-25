#!/usr/bin/env python3
"""Generates training/notebooks/learnmate_qlora_training.ipynb."""
import json
import os
from pathlib import Path

notebook = {
    "cells": [
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "# LearnMate AI: QLoRA Fine-Tuning Pipeline\n",
                "## Supervised Fine-Tuning (SFT) for Llama 3.2 3B Instruct\n",
                "\n",
                "**Project:** LearnMate AI (Personalized On-Device AI Learning Companion)\n",
                "**Target Base Model:** `meta-llama/Llama-3.2-3B-Instruct`\n",
                "**Tuning Technique:** 4-bit QLoRA (NF4 Quantization + LoRA Parameter-Efficient Adapters)\n",
                "**Target Hardware:** Cloud GPU (Kaggle T4 x 2, L4, or A100 with >= 16 GB VRAM)\n",
                "**Objective:** Teach pedagogical behaviors (adaptive explanations, Socratic tutoring, error diagnosis, structured JSON) across multiple subjects.\n",
                "\n",
                "> **SAFETY NOTICE:**\n",
                "> This notebook is staged for cloud execution. Training must NOT be executed without explicit authorization.\n",
                "> Accessing gated Meta Llama checkpoints requires an approved Hugging Face license and an active `HF_TOKEN` in Kaggle Secrets."
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 1: Environment & GPU Verification\n",
                "Verifies CUDA availability, GPU device specifications, and VRAM capacity."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 1: Environment & GPU Verification\n",
                "import os\n",
                "import sys\n",
                "import torch\n",
                "import psutil\n",
                "\n",
                "print(f\"Python Version: {sys.version}\")\n",
                "print(f\"PyTorch Version: {torch.__version__}\")\n",
                "print(f\"CUDA Available: {torch.cuda.is_available()}\")\n",
                "\n",
                "if torch.cuda.is_available():\n",
                "    device_name = torch.cuda.get_device_name(0)\n",
                "    total_vram_gb = torch.cuda.get_device_properties(0).total_memory / (1024**3)\n",
                "    print(f\"Detected GPU: {device_name}\")\n",
                "    print(f\"Total GPU VRAM: {total_vram_gb:.2f} GB\")\n",
                "    assert total_vram_gb >= 14.0, f\"Insufficient VRAM ({total_vram_gb:.2f} GB). QLoRA 3B requires >= 14 GB VRAM.\"\n",
                "else:\n",
                "    print(\"WARNING: No CUDA GPU detected! Training requires a GPU accelerator.\")\n",
                "\n",
                "ram_gb = psutil.virtual_memory().total / (1024**3)\n",
                "print(f\"Host System RAM: {ram_gb:.2f} GB\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 2: Pinned Dependency Installation & Authentication\n",
                "Installs `transformers`, `peft`, `trl`, `bitsandbytes`, and retrieves `HF_TOKEN` from Kaggle Secrets."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 2: Dependencies and Authentication\n",
                "!pip install -q --upgrade pip\n",
                "!pip install -q transformers==4.46.2 peft==0.13.2 trl==0.12.1 bitsandbytes==0.44.1 accelerate==1.1.1 datasets==3.1.0 huggingface_hub\n",
                "\n",
                "from huggingface_hub import login\n",
                "try:\n",
                "    from kaggle_secrets import UserSecretsClient\n",
                "    user_secrets = UserSecretsClient()\n",
                "    hf_token = user_secrets.get_secret(\"HF_TOKEN\")\n",
                "    login(token=hf_token)\n",
                "    print(\"Hugging Face login successful via Kaggle Secrets.\")\n",
                "except Exception as e:\n",
                "    hf_token = os.environ.get(\"HF_TOKEN\", \"\")\n",
                "    if hf_token:\n",
                "        login(token=hf_token)\n",
                "        print(\"Hugging Face login successful via environment variable.\")\n",
                "    else:\n",
                "        print(\"HF_TOKEN secret not found. Ensure Kaggle Add-ons -> Secrets contains HF_TOKEN.\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 3: Dataset Loading & Integrity Inspection\n",
                "Loads `train.jsonl` and `val.jsonl` into Hugging Face `Dataset` objects."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 3: Dataset Loading\n",
                "from datasets import load_dataset\n",
                "\n",
                "# Search candidate dataset locations (Kaggle input or local repo)\n",
                "dataset_paths = [\n",
                "    \"/kaggle/input/learnmate-dataset\",\n",
                "    \"./training/dataset\",\n",
                "    \"../dataset\"\n",
                "]\n",
                "\n",
                "data_dir = None\n",
                "for path in dataset_paths:\n",
                "    if os.path.exists(os.path.join(path, \"train.jsonl\")):\n",
                "        data_dir = path\n",
                "        break\n",
                "\n",
                "assert data_dir is not None, f\"Dataset not found in search paths: {dataset_paths}\"\n",
                "print(f\"Loading dataset from: {data_dir}\")\n",
                "\n",
                "dataset = load_dataset(\"json\", data_files={\n",
                "    \"train\": os.path.join(data_dir, \"train.jsonl\"),\n",
                "    \"val\": os.path.join(data_dir, \"val.jsonl\")\n",
                "})\n",
                "\n",
                "print(f\"Loaded Train Dataset: {len(dataset['train'])} examples\")\n",
                "print(f\"Loaded Validation Dataset: {len(dataset['val'])} examples\")\n",
                "print(\"\\nSample Train Record Structure:\")\n",
                "print(list(dataset['train'][0].keys()))\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 4: Chat Template & Tokenizer Setup\n",
                "Loads the official tokenizer and verifies that messages format matches Llama 3.2 instructions."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 4: Tokenizer & Chat Template Setup\n",
                "from transformers import AutoTokenizer\n",
                "\n",
                "MODEL_ID = \"meta-llama/Llama-3.2-3B-Instruct\"\n",
                "print(f\"Loading tokenizer for {MODEL_ID}...\")\n",
                "tokenizer = AutoTokenizer.from_pretrained(MODEL_ID, token=hf_token)\n",
                "\n",
                "# Set padding token\n",
                "if tokenizer.pad_token is None:\n",
                "    tokenizer.pad_token = tokenizer.eos_token\n",
                "tokenizer.padding_side = \"right\"\n",
                "\n",
                "# Test chat template formatting on sample 0\n",
                "sample_messages = dataset[\"train\"][0][\"messages\"]\n",
                "formatted_chat = tokenizer.apply_chat_template(sample_messages, tokenize=False, add_generation_prompt=False)\n",
                "print(\"=== Formatted Chat Template Sample ===\")\n",
                "print(formatted_chat[:400] + \"...\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 5: Base Model Loading in 4-bit (BitsAndBytes NF4)\n",
                "Loads `Llama-3.2-3B-Instruct` in 4-bit NormalFloat precision to conserve GPU VRAM during training."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 5: Base Model Loading in 4-bit\n",
                "import torch\n",
                "from transformers import AutoModelForCausalLM, BitsAndBytesConfig\n",
                "\n",
                "bnb_config = BitsAndBytesConfig(\n",
                "    load_in_4bit=True,\n",
                "    bnb_4bit_quant_type=\"nf4\",\n",
                "    bnb_4bit_use_double_quant=True,\n",
                "    bnb_4bit_compute_dtype=torch.bfloat16\n",
                ")\n",
                "\n",
                "print(f\"Loading base model {MODEL_ID} in 4-bit NF4...\")\n",
                "# Dry-run guard: to execute training after approval, uncomment model loading:\n",
                "# model = AutoModelForCausalLM.from_pretrained(\n",
                "#     MODEL_ID,\n",
                "#     quantization_config=bnb_config,\n",
                "#     device_map=\"auto\",\n",
                "#     token=hf_token,\n",
                "#     torch_dtype=torch.bfloat16\n",
                "# )\n",
                "# print(\"Base model loaded successfully.\")\n",
                "print(\"[PREPARATION MODE] Base model loading configuration verified.\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 6: QLoRA Adapter Configuration (PEFT)\n",
                "Configures low-rank adaptation (`r=16`, `lora_alpha=32`) targeting all attention and MLP linear projections."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 6: PEFT QLoRA Configuration\n",
                "from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training\n",
                "\n",
                "lora_config = LoraConfig(\n",
                "    r=16,\n",
                "    lora_alpha=32,\n",
                "    lora_dropout=0.05,\n",
                "    bias=\"none\",\n",
                "    task_type=\"CAUSAL_LM\",\n",
                "    target_modules=[\n",
                "        \"q_proj\", \"k_proj\", \"v_proj\", \"o_proj\",\n",
                "        \"gate_proj\", \"up_proj\", \"down_proj\"\n",
                "    ]\n",
                ")\n",
                "\n",
                "print(\"LoRA Configuration Summary:\")\n",
                "print(f\"  Rank (r): {lora_config.r}\")\n",
                "print(f\"  Alpha: {lora_config.lora_alpha}\")\n",
                "print(f\"  Dropout: {lora_config.lora_dropout}\")\n",
                "print(f\"  Target Modules: {lora_config.target_modules}\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 7: Training Arguments Configuration\n",
                "Defines conservative hyperparameters: effective batch size 16 (batch 2 * accum 8), cosine schedule, and gradient checkpointing."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 7: Training Arguments Setup\n",
                "from transformers import TrainingArguments\n",
                "\n",
                "OUTPUT_DIR = \"/kaggle/working/learnmate-llama3.2-3b-qlora\"\n",
                "os.makedirs(OUTPUT_DIR, exist_ok=True)\n",
                "\n",
                "training_args = TrainingArguments(\n",
                "    output_dir=OUTPUT_DIR,\n",
                "    per_device_train_batch_size=2,\n",
                "    per_device_eval_batch_size=2,\n",
                "    gradient_accumulation_steps=8,  # Effective batch size = 16\n",
                "    learning_rate=2e-4,\n",
                "    weight_decay=0.01,\n",
                "    lr_scheduler_type=\"cosine\",\n",
                "    warmup_ratio=0.03,\n",
                "    num_train_epochs=3,\n",
                "    max_grad_norm=0.3,\n",
                "    optim=\"paged_adamw_8bit\",\n",
                "    gradient_checkpointing=True,\n",
                "    bf16=True,\n",
                "    logging_steps=10,\n",
                "    eval_strategy=\"steps\",\n",
                "    eval_steps=50,\n",
                "    save_strategy=\"steps\",\n",
                "    save_steps=100,\n",
                "    save_total_limit=3,\n",
                "    load_best_model_at_end=True,\n",
                "    metric_for_best_model=\"eval_loss\",\n",
                "    report_to=\"none\",\n",
                "    seed=42\n",
                ")\n",
                "\n",
                "print(\"Training Arguments Configured.\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 8: SFTTrainer Initialization & Training Execution\n",
                "Instantiates `SFTTrainer` with chat formatting and manages training loop execution."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 8: SFTTrainer Setup (Dry-Run / Preparation)\n",
                "from trl import SFTTrainer\n",
                "\n",
                "print(\"=== SFTTrainer Pipeline Configured ===\")\n",
                "print(\"- Max Sequence Length: 1024 tokens\")\n",
                "print(\"- Packing: False\")\n",
                "print(\"- Dataset format: conversational messages list\")\n",
                "\n",
                "# When training is explicitly authorized by user, run:\n",
                "# trainer = SFTTrainer(\n",
                "#     model=model,\n",
                "#     train_dataset=dataset['train'],\n",
                "#     eval_dataset=dataset['val'],\n",
                "#     peft_config=lora_config,\n",
                "#     tokenizer=tokenizer,\n",
                "#     args=training_args,\n",
                "#     max_seq_length=1024\n",
                "# )\n",
                "# trainer.train()\n",
                "print(\"[PREPARATION MODE] SFTTrainer ready. Awaiting human execution authorization.\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 9: Validation Loss & Quality Evaluation\n",
                "Evaluates model on validation dataset and computes perplexity."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 9: Validation Evaluation (Post-Training Logic)\n",
                "import math\n",
                "\n",
                "def evaluate_training_run(eval_loss: float):\n",
                "    perplexity = math.exp(eval_loss) if eval_loss < 20 else float('inf')\n",
                "    print(f\"Validation Loss: {eval_loss:.4f}\")\n",
                "    print(f\"Validation Perplexity: {perplexity:.2f}\")\n",
                "    return {\"eval_loss\": eval_loss, \"perplexity\": perplexity}\n",
                "\n",
                "print(\"Validation metrics evaluator defined.\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 10: Checkpoint Saving & Adapter Packaging\n",
                "Saves trained LoRA adapter weights and tokenizer configuration."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 10: Save Trained Adapter\n",
                "ADAPTER_OUTPUT = \"/kaggle/working/learnmate-llama3.2-3b-qlora-final\"\n",
                "\n",
                "def save_adapter_artifacts(model, tokenizer, output_dir):\n",
                "    os.makedirs(output_dir, exist_ok=True)\n",
                "    model.save_pretrained(output_dir)\n",
                "    tokenizer.save_pretrained(output_dir)\n",
                "    print(f\"Successfully saved LoRA adapter artifacts to: {output_dir}\")\n",
                "\n",
                "print(f\"Adapter destination configured at: {ADAPTER_OUTPUT}\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 11: Adapter Merge & Deployment Conversion for ExecuTorch\n",
                "Merges the fine-tuned LoRA adapter into base BF16 weights, ready for ExecuTorch v0.5.0 INT4 export."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 11: Merge Adapter for ExecuTorch Deployment\n",
                "from peft import PeftModel\n",
                "\n",
                "MERGED_OUTPUT = \"/kaggle/working/learnmate-llama3.2-3b-merged-bf16\"\n",
                "\n",
                "def merge_and_export_bf16(base_model_id, adapter_dir, output_dir):\n",
                "    print(\"Loading base model in BF16 for merging...\")\n",
                "    base = AutoModelForCausalLM.from_pretrained(\n",
                "        base_model_id,\n",
                "        torch_dtype=torch.bfloat16,\n",
                "        device_map=\"cpu\",\n",
                "        token=hf_token\n",
                "    )\n",
                "    print(\"Applying LoRA adapter...\")\n",
                "    peft_model = PeftModel.from_pretrained(base, adapter_dir)\n",
                "    print(\"Merging weights into base model...\")\n",
                "    merged = peft_model.merge_and_unload()\n",
                "    \n",
                "    print(f\"Saving merged 16-bit checkpoint to {output_dir}...\")\n",
                "    merged.save_pretrained(output_dir, safe_serialization=True)\n",
                "    tokenizer.save_pretrained(output_dir)\n",
                "    print(\"Merge complete! Ready for ExecuTorch export_llama.py pipeline.\")\n",
                "\n",
                "print(\"Merge pipeline prepared.\")\n"
            ]
        },
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                "### Stage 12: Training Provenance Manifest Generation\n",
                "Generates `training_manifest.json` recording training lineage, hyperparameters, and adapter details."
            ]
        },
        {
            "cell_type": "code",
            "execution_count": None,
            "metadata": {},
            "outputs": [],
            "source": [
                "# Stage 12: Training Manifest Generation\n",
                "import json\n",
                "import datetime\n",
                "\n",
                "training_manifest = {\n",
                "    \"project\": \"LearnMate AI\",\n",
                "    \"base_model\": MODEL_ID,\n",
                "    \"tuning_type\": \"QLoRA_SFT\",\n",
                "    \"lora_rank\": 16,\n",
                "    \"lora_alpha\": 32,\n",
                "    \"target_modules\": lora_config.target_modules,\n",
                "    \"effective_batch_size\": 16,\n",
                "    \"learning_rate\": 2e-4,\n",
                "    \"epochs\": 3,\n",
                "    \"dataset_version\": \"v1.0_synthetic\",\n",
                "    \"adapter_path\": ADAPTER_OUTPUT,\n",
                "    \"merged_model_path\": MERGED_OUTPUT,\n",
                "    \"generated_at\": datetime.datetime.now(datetime.timezone.utc).isoformat(),\n",
                "    \"status\": \"staged_unexecuted\"\n",
                "}\n",
                "\n",
                "manifest_path = \"/kaggle/working/training_manifest.json\"\n",
                "with open(manifest_path, \"w\", encoding=\"utf-8\") as f:\n",
                "    json.dump(training_manifest, f, indent=2)\n",
                "\n",
                "print(\"Training manifest written:\")\n",
                "print(json.dumps(training_manifest, indent=2))\n"
            ]
        }
    ],
    "metadata": {
        "language_info": {
            "name": "python"
        }
    },
    "nbformat": 4,
    "nbformat_minor": 2
}

output_path = Path("training/notebooks/learnmate_qlora_training.ipynb").resolve()
output_path.parent.mkdir(parents=True, exist_ok=True)
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(notebook, f, indent=1)
print(f"Generated {output_path} ({output_path.stat().st_size / 1024:.2f} KB)")
