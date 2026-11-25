import os
import glob

def search_locations():
    locations_to_check = [
        r"<USER_HOME>\.cache\huggingface",
        r"<USER_HOME>\.cache\huggingface\hub",
        r"<USER_HOME>\.cache\torch",
        r".\model",
        r".\training",
        r".\scripts",
        r"<USER_HOME>\Downloads",
        r"\\wsl$\Ubuntu-24.04\home\<user>\.cache\huggingface\hub",
        r"\\wsl$\Ubuntu-24.04\home\<user>\.cache\huggingface\hub"
    ]
    
    print("=== SEARCHING LOCAL FILESYSTEM LOCATIONS ===")
    for loc in locations_to_check:
        exists = os.path.exists(loc)
        print(f"Checking: {loc} -> Exists: {exists}")
        if exists:
            try:
                entries = os.listdir(loc)
                print(f"  Entries ({len(entries)}): {entries[:10]}")
            except Exception as e:
                print(f"  Error reading {loc}: {e}")
                
    print("\n=== SEARCHING FOR SAFETENSORS / BIN / PTE / CHECKPOINT FILES IN REPO ===")
    repo_root = r"."
    for root, dirs, files in os.walk(repo_root):
        for f in files:
            ext = os.path.splitext(f)[1].lower()
            name_lower = f.lower()
            if ext in [".safetensors", ".bin", ".pth", ".pt", ".pte", ".tar.gz"] or "llama" in name_lower or "model" in name_lower:
                full_p = os.path.join(root, f)
                try:
                    sz = os.path.getsize(full_p)
                    print(f"Found: {full_p} | Size: {sz:,} bytes ({sz/(1024*1024):.2f} MB)")
                except Exception as ex:
                    print(f"Found (err sizing): {full_p}: {ex}")

if __name__ == "__main__":
    search_locations()
