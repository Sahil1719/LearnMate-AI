import csv
import hashlib
import json
import os
import re
import subprocess
import time

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROMPTS_JSON = os.path.join(PROJECT_ROOT, "scripts", "phase_3_6_prompts.json")
REPORTS_DIR = os.path.join(PROJECT_ROOT, "reports")
RAW_JSON = os.path.join(REPORTS_DIR, "phase_3_6_raw_outputs.json")
CSV_PATH = os.path.join(REPORTS_DIR, "phase_3_6_diagnostic_results.csv")
TEMP_PROMPT = os.path.join(PROJECT_ROOT, "temp_eval_3_6_prompt.txt")
DEVICE_SH_LOCAL = os.path.join(PROJECT_ROOT, "scripts", "run_phase_3_6_device.sh")

EXPECTED_HASHES = {
    "/data/local/tmp/llama/base_llama3_2_3b.pte": "2b855085703e6a1743437edb6adf467f7f95f9d23f810d8fd196175630979407",
    "/data/local/tmp/llama/learnmate_llama3_2_3b.pte": "8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91",
    "/data/local/tmp/llama/tokenizer.model": "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55",
}
EXPECTED_LOCAL = {
    os.path.join(PROJECT_ROOT, "model", "experimental", "base_llama3_2_3b.pte"): EXPECTED_HASHES["/data/local/tmp/llama/base_llama3_2_3b.pte"],
    os.path.join(PROJECT_ROOT, "model", "android", "learnmate_llama3_2_3b.pte"): EXPECTED_HASHES["/data/local/tmp/llama/learnmate_llama3_2_3b.pte"],
    os.path.join(PROJECT_ROOT, "model", "android", "tokenizer.model"): EXPECTED_HASHES["/data/local/tmp/llama/tokenizer.model"],
}

MODELS = {
    "A": {
        "id": "Model_A_Base",
        "device_file": "base_llama3_2_3b.pte",
        "sha256": EXPECTED_HASHES["/data/local/tmp/llama/base_llama3_2_3b.pte"],
    },
    "B": {
        "id": "Model_B_Merged",
        "device_file": "learnmate_llama3_2_3b.pte",
        "sha256": EXPECTED_HASHES["/data/local/tmp/llama/learnmate_llama3_2_3b.pte"],
    },
}


def sha256_local(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(4 * 1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def verify_all_hashes():
    for lp, exp in EXPECTED_LOCAL.items():
        act = sha256_local(lp)
        if act != exp:
            raise RuntimeError(f"LOCAL HASH MISMATCH {lp}: expected {exp}, got {act}")
    paths = list(EXPECTED_HASHES.keys()) + ["/data/local/tmp/llama/llama_main"]
    res = subprocess.run(["adb", "shell", "sha256sum " + " ".join(paths)], capture_output=True, text=True, check=True)
    obs = {}
    for line in res.stdout.strip().splitlines():
        parts = line.split()
        if len(parts) >= 2:
            obs[parts[1]] = parts[0]
    for dp, exp in EXPECTED_HASHES.items():
        act = obs.get(dp)
        if act != exp:
            raise RuntimeError(f"DEVICE HASH MISMATCH {dp}: expected {exp}, got {act}")
    return obs


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


def run_case(idx, spec, prompts):
    case = prompts[spec["case_id"]]
    model_key = spec["model_key"]
    model_info = MODELS[model_key]
    seq_len = spec["seq_len"]
    temp = spec["temperature"]
    prompt_str = case["prompt"]

    pre_telem = get_device_telemetry()
    if pre_telem["mem_available_mb"] is not None and pre_telem["mem_available_mb"] < 400.0:
        time.sleep(15)
        pre_telem = get_device_telemetry()

    with open(TEMP_PROMPT, "w", encoding="utf-8", newline="\n") as f:
        f.write(prompt_str)
    subprocess.run(["adb", "push", TEMP_PROMPT, "/data/local/tmp/llama/eval_prompt_3_6.txt"], check=True, capture_output=True)

    t0 = time.time()
    proc = subprocess.run(
        ["adb", "shell", f"/data/local/tmp/llama/run_eval_3_6.sh {model_info['device_file']} {temp} {seq_len}"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    wall_s = round(time.time() - t0, 2)
    post_telem = get_device_telemetry()

    stdout = proc.stdout
    stderr = proc.stderr

    observer = {}
    for line in stdout.splitlines():
        if line.startswith("PyTorchObserver "):
            try:
                observer = json.loads(line.replace("PyTorchObserver ", "").strip())
            except Exception:
                pass

    rss_load = None
    rss_prefill = None
    rss_end = None
    for attr, pat in [
        ("load", r"RSS after loading model:\s*([\d\.]+)\s*MiB"),
        ("prefill", r"RSS after prompt prefill:\s*([\d\.]+)\s*MiB"),
        ("end", r"RSS after finishing text generation:\s*([\d\.]+)\s*MiB"),
    ]:
        m = re.search(pat, stderr)
        if m:
            if attr == "load":
                rss_load = float(m.group(1))
            elif attr == "prefill":
                rss_prefill = float(m.group(1))
            else:
                rss_end = float(m.group(1))

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
    observer_gen_tokens = observer.get("generated_tokens", 0)
    load_ms = (observer.get("model_load_end_ms", 0) - observer.get("model_load_start_ms", 0)) if observer else 0
    inf_start = observer.get("inference_start_ms", 0)
    inf_end = observer.get("inference_end_ms", 0)
    prefill_end = observer.get("prompt_eval_end_ms", 0)
    ttft_ms = (prefill_end - inf_start) if (prefill_end and inf_start) else 0
    gen_ms = (inf_end - prefill_end) if (inf_end and prefill_end) else 0
    tok_per_sec = round(observer_gen_tokens / (gen_ms / 1000.0), 3) if gen_ms > 0 else 0.0

    reached_end_log = "Reached to the end of generation" in stderr
    seq_limit_log = "Sequence length (" in stderr and "reached!" in stderr

    if proc.returncode != 0:
        term = "process_failure"
    elif has_eot or has_end_of_text or reached_end_log:
        term = "natural_eos"
    elif seq_limit_log or (prompt_tokens + observer_gen_tokens) >= (seq_len - 1):
        term = "seq_limit"
    else:
        term = "unknown_stop"

    gen_budget = seq_len - case["prompt_tokens"]
    rem_budget = seq_len - (prompt_tokens + observer_gen_tokens)

    return {
        "run_order": idx,
        "diag_group": spec["diag_group"],
        "run_id": spec["run_id"],
        "case_id": spec["case_id"],
        "question": case["question"],
        "system_msg": case["system_msg"],
        "model_key": model_key,
        "model_id": model_info["id"],
        "temperature": temp,
        "seq_len_param": seq_len,
        "expected_prompt_tokens": case["prompt_tokens"],
        "observed_prompt_tokens": prompt_tokens,
        "generation_budget": gen_budget,
        "observer_generated_tokens": observer_gen_tokens,
        "total_tokens_observer": prompt_tokens + observer_gen_tokens,
        "remaining_budget_observer": rem_budget,
        "has_eot_id": has_eot,
        "has_end_of_text": has_end_of_text,
        "reached_end_log": reached_end_log,
        "seq_limit_log": seq_limit_log,
        "termination_reason": term,
        "exit_code": proc.returncode,
        "model_load_ms": load_ms,
        "ttft_ms": ttft_ms,
        "generation_time_ms": gen_ms,
        "wall_duration_s": wall_s,
        "tok_per_sec": tok_per_sec,
        "rss_after_load_mib": rss_load,
        "rss_after_prefill_mib": rss_prefill,
        "rss_after_gen_mib": rss_end,
        "pre_run_telemetry": pre_telem,
        "post_run_telemetry": post_telem,
        "raw_response_with_eos": raw_with_eos,
        "response_text": clean_text,
    }


def main():
    with open(PROMPTS_JSON, "r", encoding="utf-8") as f:
        prompts = json.load(f)

    pre_hashes = verify_all_hashes()
    print("Pre-run hashes verified:", json.dumps(pre_hashes, indent=2), flush=True)

    # Ensure LF line endings on shell script before pushing
    with open(DEVICE_SH_LOCAL, "r", encoding="utf-8") as f:
        sh_txt = f.read().replace("\r\n", "\n")
    with open(DEVICE_SH_LOCAL, "w", encoding="utf-8", newline="\n") as f:
        f.write(sh_txt)

    subprocess.run(["adb", "push", DEVICE_SH_LOCAL, "/data/local/tmp/llama/run_eval_3_6.sh"], check=True, capture_output=True)
    subprocess.run(["adb", "shell", "chmod +x /data/local/tmp/llama/run_eval_3_6.sh"], check=True, capture_output=True)

    schedule = []
    # Diagnostic A: Prompt length comparison on "What is an interrupt?" (short=15, med=32, long=62)
    for temp, tlabel in [(0.0, "greedy"), (0.7, "samp")]:
        for cid, clabel in [("diagA_short", "short15"), ("diagA_med", "med32"), ("diagA_long", "long62")]:
            for mk in ["A", "B"]:
                schedule.append({
                    "diag_group": "DiagA_PromptLength",
                    "run_id": f"diagA_{clabel}_{tlabel}_{mk}",
                    "case_id": cid,
                    "model_key": mk,
                    "temperature": temp,
                    "seq_len": 128,
                })

    # Diagnostic B: Generation budgets (32, 48, 64) via --seq_len (P+32, P+48, P+64) on diagB_q7 (P=36)
    for bud, sl in [(32, 68), (48, 84), (64, 100)]:
        for mk in ["A", "B"]:
            schedule.append({
                "diag_group": "DiagB_GenBudget",
                "run_id": f"diagB_bud{bud}_greedy_{mk}",
                "case_id": "diagB_q7",
                "model_key": mk,
                "temperature": 0.0,
                "seq_len": sl,
            })

    # Diagnostic F: Controlled minimal prompt on Q1, Q2, Q4 (Q3 covered by diagA_short)
    for temp, tlabel in [(0.0, "greedy"), (0.7, "samp")]:
        for cid in ["diagF_q1", "diagF_q2", "diagF_q4"]:
            for mk in ["A", "B"]:
                schedule.append({
                    "diag_group": "DiagF_MinimalPrompt",
                    "run_id": f"{cid}_{tlabel}_{mk}",
                    "case_id": cid,
                    "model_key": mk,
                    "temperature": temp,
                    "seq_len": 128,
                })

    results = []
    print(f"Starting {len(schedule)} diagnostic runs on Redmi 9 Power...", flush=True)
    for i, spec in enumerate(schedule, 1):
        print(f"[{i:02d}/{len(schedule)}] {spec['run_id']} (case={spec['case_id']}, seq_len={spec['seq_len']}, temp={spec['temperature']})...", flush=True)
        rec = run_case(i, spec, prompts)
        results.append(rec)
        print(
            f"   -> P={rec['observed_prompt_tokens']} G_obs={rec['observer_generated_tokens']} "
            f"Rem={rec['remaining_budget_observer']} term={rec['termination_reason']} "
            f"eot={rec['has_eot_id']} seqlog={rec['seq_limit_log']} rate={rec['tok_per_sec']} | {rec['response_text']!r}",
            flush=True,
        )
        with open(RAW_JSON, "w", encoding="utf-8") as f:
            json.dump({"pre_hashes": pre_hashes, "results": results}, f, indent=2, ensure_ascii=False)
        if i < len(schedule):
            time.sleep(3)

    post_hashes = verify_all_hashes()
    print("Post-run hashes verified:", json.dumps(post_hashes, indent=2), flush=True)

    with open(RAW_JSON, "w", encoding="utf-8") as f:
        json.dump({"pre_hashes": pre_hashes, "post_hashes": post_hashes, "results": results}, f, indent=2, ensure_ascii=False)

    csv_fields = [
        "run_order", "diag_group", "run_id", "case_id", "model_key", "temperature",
        "seq_len_param", "expected_prompt_tokens", "observed_prompt_tokens",
        "generation_budget", "observer_generated_tokens", "total_tokens_observer",
        "remaining_budget_observer", "has_eot_id", "reached_end_log", "seq_limit_log",
        "termination_reason", "exit_code", "model_load_ms", "ttft_ms",
        "generation_time_ms", "tok_per_sec", "rss_after_load_mib", "rss_after_gen_mib",
        "question", "response_text",
    ]
    with open(CSV_PATH, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=csv_fields)
        w.writeheader()
        for r in results:
            w.writerow({k: r.get(k) for k in csv_fields})

    if os.path.exists(TEMP_PROMPT):
        os.remove(TEMP_PROMPT)
    print(f"Wrote {RAW_JSON} and {CSV_PATH} cleanly.")


if __name__ == "__main__":
    main()
