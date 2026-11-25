from huggingface_hub import hf_hub_download
import os

repo_id = "meta-llama/Llama-3.2-3B-Instruct"

# Download small text files only (LICENSE.txt, USE_POLICY.md, config.json)
# Note: These are small documentation/text files (< 50 KB), NOT multi-GB model weights!
lic_path = hf_hub_download(repo_id=repo_id, filename="LICENSE.txt")
use_path = hf_hub_download(repo_id=repo_id, filename="USE_POLICY.md")
cfg_path = hf_hub_download(repo_id=repo_id, filename="config.json")

print("=== LICENSE.TXT (First 30 lines) ===")
with open(lic_path, "r", encoding="utf-8") as f:
    for i in range(30):
        line = f.readline()
        if not line: break
        print(line.rstrip())

print("\n=== USE_POLICY.MD (First 30 lines) ===")
with open(use_path, "r", encoding="utf-8") as f:
    for i in range(30):
        line = f.readline()
        if not line: break
        print(line.rstrip())
