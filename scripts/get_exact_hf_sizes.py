from huggingface_hub import HfApi

api = HfApi()
repo_id = "meta-llama/Llama-3.2-3B-Instruct"

files = [
    ".gitattributes", "LICENSE.txt", "README.md", "USE_POLICY.md",
    "config.json", "generation_config.json",
    "model-00001-of-00002.safetensors", "model-00002-of-00002.safetensors",
    "model.safetensors.index.json",
    "original/consolidated.00.pth", "original/orig_params.json",
    "original/params.json", "original/tokenizer.model",
    "special_tokens_map.json", "tokenizer.json", "tokenizer_config.json"
]

print(f"=== EXACT FILE METADATA FOR {repo_id} ===")
paths_info = api.get_paths_info(repo_id=repo_id, paths=files)
total_bytes = 0
safetensors_bytes = 0
pth_bytes = 0

for p in paths_info:
    sz = p.size
    total_bytes += sz
    print(f"  {p.path:<38} | Size: {sz:>12,} bytes ({sz/(1024*1024):>8.2f} MB)")
    if "safetensors" in p.path and not p.path.endswith(".json"):
        safetensors_bytes += sz
    elif "consolidated" in p.path:
        pth_bytes += sz

print("-" * 70)
print(f"Total Repository Size (all formats): {total_bytes:,} bytes ({total_bytes/(1024**3):.2f} GB)")
print(f"HuggingFace Safetensors Shards:     {safetensors_bytes:,} bytes ({safetensors_bytes/(1024**3):.2f} GB)")
print(f"Original PyTorch Consolidated .pth:  {pth_bytes:,} bytes ({pth_bytes/(1024**3):.2f} GB)")
