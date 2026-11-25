import os
import sys
import json
import time
import re
import subprocess

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROMPTS_JSON = os.path.join(PROJECT_ROOT, "scripts", "phase_3_5_prompts.json")
REPORTS_DIR = os.path.join(PROJECT_ROOT, "reports")
RAW_JSON = os.path.join(REPORTS_DIR, "phase_3_5_raw_outputs.json")
TEMP_PROMPT = os.path.join(PROJECT_ROOT, "temp_eval_3_5_prompt.txt")
DEVICE_SH_LOCAL = os.path.join(PROJECT_ROOT, "scripts", "run_phase_3_5_device.sh")

EXPECTED_HASHES = {
    "/data/local/tmp/llama/base_llama3_2_3b.pte": "2b855085703e6a1743437edb6adf467f7f95f9d23f810d8fd196175630979407",
    "/data/local/tmp/llama/learnmate_llama3_2_3b.pte": "8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91",
    "/data/local/tmp/llama/tokenizer.model": "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55",
}

MODELS = {
    "A": {
        "id": "Model_A_Base",
        "label": "Original Base Meta Llama 3.2 3B Instruct",
        "device_file": "base_llama3_2_3b.pte",
        "sha256": EXPECTED_HASHES["/data/local/tmp/llama/base_llama3_2_3b.pte"],
    },
    "B": {
        "id": "Model_B_Merged",
        "label": "Current Merged 2-Step LoRA Model",
        "device_file": "learnmate_llama3_2_3b.pte",
        "sha256": EXPECTED_HASHES["/data/local/tmp/llama/learnmate_llama3_2_3b.pte"],
    },
}

DECODINGS = [
    {"mode": "greedy", "temperature": 0.0, "seq_len": 128, "cpu_threads": 4},
    {"mode": "sampling", "temperature": 0.7, "seq_len": 128, "cpu_threads": 4},
]


def verify_device_hashes():
    paths = list(EXPECTED_HASHES.keys()) + ["/data/local/tmp/llama/llama_main"]
    res = subprocess.run(["adb", "shell", "sha256sum " + " ".join(paths)], capture_output=True, text=True, check=True)
    observed = {}
    for line in res.stdout.strip().splitlines():
        parts = line.split()
        if len(parts) >= 2:
            observed[parts[1]] = parts[0]
    for path, exp in EXPECTED_HASHES.items():
        act = observed.get(path)
        if act != exp:
            raise RuntimeError(f"HASH MISMATCH for {path}: expected {exp}, got {act}")
    print("Verified all device model & tokenizer SHA-256 hashes match expected values.")
    return observed


def get_device_telemetry():
    cmd = (
        "grep MemAvailable /proc/meminfo; "
        "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null; "
        "dumpsys battery | grep temperature"
    )
    res = subprocess.run(["adb", "shell", cmd], capture_output=True, text=True)
    mem_avail_kb = None
    pmic_mc = None
    batt_dc = None
    for line in res.stdout.splitlines():
        m1 = re.search(r"MemAvailable:\s+(\d+)\s+kB", line)
        if m1:
            mem_avail_kb = int(m1.group(1))
        elif line.strip().isdigit():
            pmic_mc = int(line.strip())
        m2 = re.search(r"temperature:\s*(\d+)", line)
        if m2:
            batt_dc = int(m2.group(1))
    return {
        "mem_available_mb": round(mem_avail_kb / 1024.0, 2) if mem_avail_kb else None,
        "pmic_temp_c": round(pmic_mc / 1000.0, 1) if pmic_mc else None,
        "battery_temp_c": round(batt_dc / 10.0, 1) if batt_dc else None,
    }


def run_single(q_rec, model_key, decoding, order_index):
    model_info = MODELS[model_key]
    test_id = f"{decoding['mode']}_{q_rec['question_id']}_{model_key}"
    prompt_str = q_rec["prompt"]

    # Check device memory before run
    pre_telem = get_device_telemetry()
    if pre_telem["mem_available_mb"] is not None and pre_telem["mem_available_mb"] < 400.0:
        print(f"  [WARN] Low MemAvailable ({pre_telem['mem_available_mb']} MB), cooling/waiting 15s...", flush=True)
        time.sleep(15)
        pre_telem = get_device_telemetry()
        if pre_telem["mem_available_mb"] is not None and pre_telem["mem_available_mb"] < 300.0:
            raise RuntimeError(f"Severe memory pressure on device: {pre_telem['mem_available_mb']} MB available")

    with open(TEMP_PROMPT, "w", encoding="utf-8", newline="\n") as f:
        f.write(prompt_str)
    subprocess.run(["adb", "push", TEMP_PROMPT, "/data/local/tmp/llama/eval_prompt_3_5.txt"], check=True, capture_output=True)

    t0 = time.time()
    proc = subprocess.run(
        ["adb", "shell", f"/data/local/tmp/llama/run_eval_3_5.sh {model_info['device_file']} {decoding['temperature']}"],
        capture_output=True,
        text=True,
    )
    wall_s = round(time.time() - t0, 2)
    post_telem = get_device_telemetry()

    stdout = proc.stdout
    stderr = proc.stderr

    # Parse PyTorchObserver
    observer = {}
    for line in stdout.splitlines():
        if line.startswith("PyTorchObserver "):
            try:
                observer = json.loads(line.replace("PyTorchObserver ", "").strip())
            except Exception:
                pass

    # Parse metadata & RSS from stderr
    metadata = {}
    for m in re.finditer(r"Metadata:\s*(\S+)\s*=\s*(\S+)", stderr):
        metadata[m.group(1)] = m.group(2)
    eos_ids = [int(m.group(1)) for m in re.finditer(r"eos_id\s*=\s*(\d+)", stderr)]

    def parse_rss(pattern):
        m = re.search(pattern, stderr)
        return float(m.group(1)) if m else None

    rss_load = parse_rss(r"RSS after loading model:\s*([\d\.]+)\s*MiB")
    rss_prefill = parse_rss(r"RSS after prompt prefill:\s*([\d\.]+)\s*MiB")
    rss_end = parse_rss(r"RSS after finishing text generation:\s*([\d\.]+)\s*MiB")

    # Extract generated output after assistant header
    marker = "<|start_header_id|>assistant<|end_header_id|>\n\n"
    if marker in stdout:
        tail = stdout.split(marker, 1)[1]
    elif "<|start_header_id|>assistant<|end_header_id|>" in stdout:
        tail = stdout.split("<|start_header_id|>assistant<|end_header_id|>", 1)[1].lstrip("\r\n")
    else:
        tail = stdout

    content_lines = [ln for ln in tail.splitlines() if not ln.startswith("PyTorchObserver ")]
    raw_with_eos = "\n".join(content_lines).strip()

    has_eot = "<|eot_id|>" in raw_with_eos
    has_end_of_text = "<|end_of_text|>" in raw_with_eos
    clean_text = raw_with_eos.split("<|eot_id|>")[0].split("<|end_of_text|>")[0].strip()

    prompt_tokens = observer.get("prompt_tokens", 0)
    generated_tokens = observer.get("generated_tokens", 0)
    load_ms = (observer.get("model_load_end_ms", 0) - observer.get("model_load_start_ms", 0)) if observer else 0
    inf_start = observer.get("inference_start_ms", 0)
    inf_end = observer.get("inference_end_ms", 0)
    prefill_end = observer.get("prompt_eval_end_ms", 0)
    ttft_ms = (prefill_end - inf_start) if (prefill_end and inf_start) else 0
    gen_ms = (inf_end - prefill_end) if (inf_end and prefill_end) else 0
    total_inf_ms = (inf_end - inf_start) if (inf_end and inf_start) else 0
    tok_per_sec = round(generated_tokens / (gen_ms / 1000.0), 3) if gen_ms > 0 else 0.0

    reached_end_log = "Reached to the end of generation" in stderr
    if proc.returncode != 0:
        termination_reason = "process_failure"
    elif has_eot or has_end_of_text:
        termination_reason = "natural_eos"
    elif (prompt_tokens + generated_tokens) >= 127:
        termination_reason = "context_limit"
    elif reached_end_log:
        termination_reason = "stopped_without_visible_eos"
    else:
        termination_reason = "unknown_stop"

    if prompt_tokens != q_rec["prompt_tokens"]:
        print(f"  [WARN] {test_id}: observed prompt_tokens={prompt_tokens} != TikToken {q_rec['prompt_tokens']}", flush=True)

    return {
        "test_id": test_id,
        "execution_order": order_index,
        "question_id": q_rec["question_id"],
        "domain": q_rec["domain"],
        "question_text": q_rec["question_text"],
        "model_key": model_key,
        "model_id": model_info["id"],
        "model_file": model_info["device_file"],
        "model_sha256": model_info["sha256"],
        "decoding_mode": decoding["mode"],
        "temperature": decoding["temperature"],
        "seq_len_limit": decoding["seq_len"],
        "cpu_threads": decoding["cpu_threads"],
        "load_condition": "cold_process_per_run",
        "prompt": prompt_str,
        "prompt_token_ids": q_rec["token_ids"],
        "expected_prompt_tokens": q_rec["prompt_tokens"],
        "observed_prompt_tokens": prompt_tokens,
        "generated_tokens": generated_tokens,
        "total_tokens_used": prompt_tokens + generated_tokens,
        "raw_response_with_eos": raw_with_eos,
        "response_text": clean_text,
        "has_eot_id": has_eot,
        "has_end_of_text": has_end_of_text,
        "termination_reason": termination_reason,
        "exit_code": proc.returncode,
        "model_load_ms": load_ms,
        "ttft_ms": ttft_ms,
        "generation_time_ms": gen_ms,
        "total_inference_ms": total_inf_ms,
        "wall_duration_s": wall_s,
        "tok_per_sec": tok_per_sec,
        "rss_after_load_mib": rss_load,
        "rss_after_prefill_mib": rss_prefill,
        "rss_after_gen_mib": rss_end,
        "pre_run_telemetry": pre_telem,
        "post_run_telemetry": post_telem,
        "metadata": metadata,
        "eos_ids": eos_ids,
        "observer": observer,
    }


def main():
    os.makedirs(REPORTS_DIR, exist_ok=True)
    device_hashes = verify_device_hashes()

    # Stage device runner script
    subprocess.run(["adb", "push", DEVICE_SH_LOCAL, "/data/local/tmp/llama/run_eval_3_5.sh"], check=True)
    subprocess.run(["adb", "shell", "sed -i 's/\\r$//' /data/local/tmp/llama/run_eval_3_5.sh && chmod +x /data/local/tmp/llama/run_eval_3_5.sh"], check=True)

    with open(PROMPTS_JSON, "r", encoding="utf-8") as f:
        questions = json.load(f)

    data = {"metadata": {}, "results": []}
    if os.path.exists(RAW_JSON):
        try:
            with open(RAW_JSON, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            pass

    data["metadata"] = {
        "phase": "3.5",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "device_serial": "<DEVICE_SERIAL>",
        "device_model": "M2010J19SI (Xiaomi Redmi 9 Power)",
        "android_version": "12 (API 31)",
        "runtime": "ExecuTorch v0.5.0 (XNNPACK NEON)",
        "runner_path": "/data/local/tmp/llama/llama_main",
        "runner_sha256": device_hashes.get("/data/local/tmp/llama/llama_main"),
        "device_hashes": device_hashes,
        "sampling_limitations": "llama_main v0.5.0 supports --temperature only (--top_p and --repetition_penalty flags are not implemented in examples/models/llama/main.cpp).",
    }

    completed = {r["test_id"] for r in data.get("results", [])}
    schedule_list = []
    for dec in DECODINGS:
        for idx, q in enumerate(questions):
            # Alternate model order between questions
            pair = ["A", "B"] if (idx % 2 == 0) else ["B", "A"]
            for mk in pair:
                schedule_list.append((q, mk, dec))

    total = len(schedule_list)
    print(f"Total scheduled runs: {total} | Already completed: {len(completed)}", flush=True)

    for order_idx, (q, mk, dec) in enumerate(schedule_list, 1):
        tid = f"{dec['mode']}_{q['question_id']}_{mk}"
        if tid in completed:
            print(f"[{order_idx}/{total}] Skipping completed {tid}", flush=True)
            continue

        print(f"[{order_idx}/{total}] Running {tid} ({MODELS[mk]['device_file']}, temp={dec['temperature']})...", flush=True)
        res = run_single(q, mk, dec, order_idx)
        data["results"].append(res)
        completed.add(tid)
        with open(RAW_JSON, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(
            f"  Done {tid} | P={res['observed_prompt_tokens']} G={res['generated_tokens']} | "
            f"Rate={res['tok_per_sec']} tok/s | Term={res['termination_reason']} | "
            f"Temp={res['post_run_telemetry'].get('battery_temp_c')}C | Out={res['response_text'][:70]!r}",
            flush=True,
        )
        time.sleep(5)

    if os.path.exists(TEMP_PROMPT):
        os.remove(TEMP_PROMPT)
    print("\nPhase 3.5 evaluation matrix complete! Saved:", RAW_JSON, flush=True)


if __name__ == "__main__":
    main()
