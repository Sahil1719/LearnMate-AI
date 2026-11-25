import sys
from huggingface_hub import HfApi

def main():
    api = HfApi()
    print("=== HUGGING FACE AUTHENTICATION CHECK ===")
    try:
        user = api.whoami()
        print(f"Authenticated as: {user.get('name')}")
        print(f"User Type: {user.get('type')}")
        print("Auth Token Status: VALID")
    except Exception as e:
        print(f"Auth Token Status: FAILED ({e})")
        return

    repo_id = "meta-llama/Llama-3.2-3B-Instruct"
    print(f"\n=== REPOSITORY ACCESS CHECK: {repo_id} ===")
    try:
        info = api.model_info(repo_id)
        print(f"Access to {repo_id}: GRANTED")
        print(f"Author / Organization: {info.author}")
        print(f"Gated Status: {info.gated}")
        print(f"Private Status: {info.private}")
        print(f"Last Modified: {info.last_modified}")
        print(f"Commit SHA: {info.sha}")
        
        print("\n=== REPOSITORY FILES AND SIZES ===")
        siblings = info.siblings
        total_size = 0
        weight_files = []
        config_files = []
        tokenizer_files = []
        
        for s in siblings:
            rname = s.rfilename
            sz = s.size if hasattr(s, "size") and s.size else 0
            # If size is None on sibling, fetch file metadata if needed
            print(f"  File: {rname:<40} | Size: {sz}")
            if rname.endswith(".safetensors") or rname.endswith(".bin") or rname.endswith(".pth"):
                weight_files.append((rname, sz))
            elif "tokenizer" in rname:
                tokenizer_files.append((rname, sz))
            else:
                config_files.append((rname, sz))
                
    except Exception as e:
        print(f"Access to {repo_id}: FAILED ({e})")

if __name__ == "__main__":
    main()
