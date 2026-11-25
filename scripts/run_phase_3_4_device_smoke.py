import subprocess
import json
import os
import re

PROMPT = (
    "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
    "You are a helpful, respectful and honest assistant.<|eot_id|>"
    "<|start_header_id|>user<|end_header_id|>\n\n"
    "What is a pointer in C?<|eot_id|>"
    "<|start_header_id|>assistant<|end_header_id|>\n\n"
)

DEVICE_SH = """#!/system/bin/sh
cd /data/local/tmp/llama
PROMPT="$(cat base_smoke_prompt.txt)"
PROMPT="${PROMPT}

"
./llama_main --model_path=base_llama3_2_3b.pte --tokenizer_path=tokenizer.model --prompt="$PROMPT" --seq_len=128 --temperature=0 --cpu_threads=4
"""

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    p_local = os.path.join(repo_root, "temp_base_smoke_prompt.txt")
    sh_local = os.path.join(repo_root, "scripts", "run_base_smoke_device.sh")

    with open(p_local, "w", encoding="utf-8", newline="\n") as f:
        f.write(PROMPT)
    with open(sh_local, "w", encoding="utf-8", newline="\n") as f:
        f.write(DEVICE_SH)

    subprocess.run(["adb", "push", p_local, "/data/local/tmp/llama/base_smoke_prompt.txt"], check=True)
    subprocess.run(["adb", "push", sh_local, "/data/local/tmp/llama/run_base_smoke.sh"], check=True)
    subprocess.run(["adb", "shell", "sed -i 's/\\r$//' /data/local/tmp/llama/run_base_smoke.sh && chmod +x /data/local/tmp/llama/run_base_smoke.sh"], check=True)

    print("Executing physical-device smoke test on Redmi 9 Power...")
    res = subprocess.run(["adb", "shell", "/data/local/tmp/llama/run_base_smoke.sh"], capture_output=True, text=True)
    print("Exit code:", res.returncode)
    print("=== DEVICE STDOUT ===")
    print(res.stdout)
    print("=== DEVICE STDERR ===")
    print(res.stderr)

    # Parse PyTorchObserver
    observer = {}
    for line in res.stdout.splitlines():
        if line.startswith("PyTorchObserver "):
            observer = json.loads(line.replace("PyTorchObserver ", "").strip())

    # Parse metadata & stats from stderr
    metadata = {}
    for m in re.finditer(r"Metadata:\s*(\S+)\s*=\s*(\S+)", res.stderr):
        metadata[m.group(1)] = m.group(2)
    eos_ids = [int(m.group(1)) for m in re.finditer(r"eos_id\s*=\s*(\d+)", res.stderr)]

    rss_load = None
    m_rss = re.search(r"RSS after loading model:\s*([\d\.]+)\s*MiB", res.stderr)
    if m_rss:
        rss_load = float(m_rss.group(1))
    rss_end = None
    m_rss2 = re.search(r"RSS after finishing text generation:\s*([\d\.]+)\s*MiB", res.stderr)
    if m_rss2:
        rss_end = float(m_rss2.group(1))

    # Extract generated text
    gen_text = res.stdout
    marker = "<|start_header_id|>assistant<|end_header_id|>\n\n"
    if marker in gen_text:
        gen_text = gen_text.split(marker, 1)[1]
    gen_text = gen_text.split("PyTorchObserver ")[0].strip()

    host_raw_path = os.path.join(repo_root, "reports", "phase_3_4_host_smoke_raw.json")
    host_data = {}
    if os.path.exists(host_raw_path):
        with open(host_raw_path, "r", encoding="utf-8") as f:
            host_data = json.load(f)

    combined_smoke = {
        "phase": "3.4",
        "model_under_test": "base_llama3_2_3b.pte",
        "model_sha256": "2b855085703e6a1743437edb6adf467f7f95f9d23f810d8fd196175630979407",
        "model_size_bytes": 1929907360,
        "tokenizer_sha256": "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55",
        "host_smoke_test": host_data,
        "device_smoke_test": {
            "device": "Xiaomi Redmi 9 Power (<DEVICE_SERIAL>)",
            "runner": "/data/local/tmp/llama/llama_main",
            "exit_code": res.returncode,
            "prompt": PROMPT,
            "generated_text_raw": gen_text,
            "metadata": metadata,
            "eos_ids": eos_ids,
            "observer": observer,
            "rss_after_load_mib": rss_load,
            "rss_after_gen_mib": rss_end,
            "stdout": res.stdout,
            "stderr": res.stderr,
        }
    }

    out_path = os.path.join(repo_root, "reports", "phase_3_4_base_model_smoke_test.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(combined_smoke, f, indent=2, ensure_ascii=False)
    print(f"Saved combined smoke test report to {out_path}")

    if os.path.exists(p_local):
        os.remove(p_local)
    assert res.returncode == 0, f"Device smoke test failed with exit code {res.returncode}"

if __name__ == "__main__":
    main()
