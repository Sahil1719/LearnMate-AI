import subprocess
import json
import re
import os

PROMPT = (
    "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
    "You are a helpful, respectful and honest assistant.<|eot_id|>"
    "<|start_header_id|>user<|end_header_id|>\n\n"
    "What is a pointer in C?<|eot_id|>"
    "<|start_header_id|>assistant<|end_header_id|>\n\n"
)

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    # Write prompt with Unix newlines inside repo temp file accessible from WSL
    prompt_win = os.path.join(repo_root, "temp_phase3_4_prompt.txt")
    with open(prompt_win, "w", encoding="utf-8", newline="\n") as f:
        f.write(PROMPT)

    wsl_script = (
        "PROMPT=\"$(cat './temp_phase3_4_prompt.txt')\"; "
        "PROMPT=\"${PROMPT}\n\n\"; "
        "$HOME/executorch/cmake-out-host/examples/models/llama/llama_main "
        "--model_path './model/experimental/base_llama3_2_3b.pte' "
        "--tokenizer_path './model/android/tokenizer.model' "
        "--prompt \"$PROMPT\" "
        "--seq_len 128 "
        "--temperature 0 "
        "--cpu_threads 4"
    )
    sh_win = os.path.join(repo_root, "scripts", "run_host_smoke.sh")
    with open(sh_win, "w", encoding="utf-8", newline="\n") as f:
        f.write("#!/usr/bin/env bash\nset -euo pipefail\n" + wsl_script + "\n")

    print("Running WSL host ExecuTorch llama_main smoke test...")
    res = subprocess.run(
        ["wsl", "-d", "Ubuntu-24.04", "-e", "bash", "./scripts/run_host_smoke.sh"],
        capture_output=True,
        text=True,
    )
    print("Exit code:", res.returncode)
    print("=== HOST STDOUT ===")
    print(res.stdout)
    print("=== HOST STDERR ===")
    print(res.stderr)

    out_json = os.path.join(repo_root, "reports", "phase_3_4_host_smoke_raw.json")
    os.makedirs(os.path.dirname(out_json), exist_ok=True)
    with open(out_json, "w", encoding="utf-8") as f:
        json.dump({
            "exit_code": res.returncode,
            "stdout": res.stdout,
            "stderr": res.stderr,
        }, f, indent=2)

    if os.path.exists(prompt_win):
        os.remove(prompt_win)
    assert res.returncode == 0, f"Host smoke test failed with exit code {res.returncode}"

if __name__ == "__main__":
    main()
