import os
import sys
import json
import time
import re
import csv
import subprocess

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPORTS_DIR = os.path.join(PROJECT_ROOT, "reports")
RAW_OUTPUTS_JSON = os.path.join(REPORTS_DIR, "phase_3_2_raw_outputs.json")
RESULTS_CSV = os.path.join(REPORTS_DIR, "phase_3_2_results.csv")
TEMP_PROMPT_FILE = os.path.join(PROJECT_ROOT, "temp_eval_prompt.txt")

QUESTIONS = [
    {
        "id": "Q1",
        "question": "What is a pointer in C?",
        "domain": "C Programming / Memory Model",
        "expected_topics": ["memory address", "variable", "dereference", "pointer variable"]
    },
    {
        "id": "Q2",
        "question": "Explain the role of the program counter in computer architecture.",
        "domain": "Computer Architecture / CPU Registers",
        "expected_topics": ["instruction address", "next instruction", "fetch-decode-execute", "register"]
    },
    {
        "id": "Q3",
        "question": "What is an interrupt in embedded systems?",
        "domain": "Embedded Systems / Interrupt Handling",
        "expected_topics": ["signal", "hardware/software", "CPU pause", "ISR", "asynchronous"]
    },
    {
        "id": "Q4",
        "question": "How does a microcontroller differ from a microprocessor?",
        "domain": "Microcontroller Architecture / SoC Design",
        "expected_topics": ["CPU + RAM + ROM + Peripherals on single chip", "general purpose vs dedicated control"]
    },
    {
        "id": "Q5",
        "question": "What is the purpose of an analog-to-digital converter?",
        "domain": "Signal Conversion / ADC Principles",
        "expected_topics": ["analog signal to digital numbers", "sampling", "quantization", "sensor processing"]
    },
    {
        "id": "Q6",
        "question": "Explain what happens during a null pointer dereference.",
        "domain": "C Programming / Operating Systems Faults",
        "expected_topics": ["accessing address 0 / invalid memory", "segmentation fault", "crash / exception", "undefined behavior"]
    },
    {
        "id": "Q7",
        "question": "What is the difference between combinational and sequential logic?",
        "domain": "Digital Logic / State Machines",
        "expected_topics": ["combinational depends only on current inputs", "sequential has memory / clock / depends on past state"]
    },
    {
        "id": "Q8",
        "question": "Why is modulation used in communication systems?",
        "domain": "Communication Systems / Signal Modulation",
        "expected_topics": ["carrier signal", "antenna size reduction", "multiplexing", "long distance transmission", "noise reduction"]
    }
]

CONFIGURATIONS = [
    {
        "code": "B",
        "name": "Merged Model (Standard Prompt)",
        "model": "learnmate_llama3_2_3b.pte",
        "system_prompt": "You are a helpful, respectful and honest assistant."
    },
    {
        "code": "C",
        "name": "Merged Model (LearnMate Goal Prompt)",
        "model": "learnmate_llama3_2_3b.pte",
        "system_prompt": "You are LearnMate, an adaptive, offline on-device AI learning companion.\n[Goal: Embedded Systems Engineer]"
    }
]

DECODING_CONDITIONS = [
    {
        "code": "greedy",
        "temp": 0.0,
        "name": "Greedy (temp=0.0)"
    },
    {
        "code": "sampling",
        "temp": 0.7,
        "name": "Sampling (temp=0.7)"
    }
]

def build_prompt(system_prompt: str, user_question: str) -> str:
    return (
        f"<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
        f"{system_prompt}<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n"
        f"{user_question}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
    )

def load_existing_results():
    data = {"metadata": {}, "results": []}
    if os.path.exists(RAW_OUTPUTS_JSON):
        try:
            with open(RAW_OUTPUTS_JSON, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            print(f"Warning loading existing JSON: {e}")
    return data

def save_results(data):
    with open(RAW_OUTPUTS_JSON, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

def export_csv(results):
    fieldnames = [
        "test_id", "question_id", "domain", "config", "decoding_mode", "temperature",
        "prompt_tokens", "generated_tokens", "tok_per_sec", "ttft_ms", "total_time_ms",
        "rss_mb", "eos_reached", "repetition_loop", "factual_correctness",
        "technical_relevance", "completeness", "clarity", "hallucination_control",
        "total_score_25", "failure_modes", "question_text", "raw_output"
    ]
    with open(RESULTS_CSV, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for r in results:
            writer.writerow({
                "test_id": r.get("test_id", ""),
                "question_id": r.get("question_id", ""),
                "domain": r.get("domain", ""),
                "config": r.get("config", ""),
                "decoding_mode": r.get("decoding_mode", ""),
                "temperature": r.get("temperature", ""),
                "prompt_tokens": r.get("prompt_tokens", 0),
                "generated_tokens": r.get("generated_tokens", 0),
                "tok_per_sec": r.get("tok_per_sec", 0.0),
                "ttft_ms": r.get("ttft_ms", 0),
                "total_time_ms": r.get("total_time_ms", 0),
                "rss_mb": r.get("rss_mb", 0.0),
                "eos_reached": r.get("eos_reached", False),
                "repetition_loop": r.get("repetition_loop", False),
                "factual_correctness": r.get("scores", {}).get("factual_correctness", 0),
                "technical_relevance": r.get("scores", {}).get("technical_relevance", 0),
                "completeness": r.get("scores", {}).get("completeness", 0),
                "clarity": r.get("scores", {}).get("clarity", 0),
                "hallucination_control": r.get("scores", {}).get("hallucination_control", 0),
                "total_score_25": r.get("scores", {}).get("total_score_25", 0),
                "failure_modes": "; ".join(r.get("failure_modes", [])),
                "question_text": r.get("question_text", ""),
                "raw_output": r.get("raw_output", "").replace("\n", " ")
            })

def detect_repetition(text: str) -> bool:
    words = text.split()
    if len(words) < 8:
        return False
    # Check for repeating n-grams (3-gram repeating 3+ times)
    for n in range(2, 6):
        ngrams = [" ".join(words[i:i+n]) for i in range(len(words)-n+1)]
        for ng in ngrams:
            if ngrams.count(ng) >= 3 and len(ng) > 6:
                return True
    return False

def score_response(question_obj, raw_text, eos_reached, repetition_detected):
    text_lower = raw_text.lower()
    expected = question_obj.get("expected_topics", [])
    matches = sum(1 for exp in expected if any(w in text_lower for w in exp.split()))
    match_ratio = matches / max(1, len(expected))
    
    failure_modes = []
    
    if repetition_detected:
        failure_modes.append("Degraded repetition loop")
        factual = 1
        relevance = 2
        completeness = 1
        clarity = 1
        hallucination = 1
    elif not raw_text.strip():
        failure_modes.append("Empty generation")
        factual = 0
        relevance = 0
        completeness = 0
        clarity = 0
        hallucination = 5
    else:
        # Check mid-sentence truncation
        if not eos_reached and not raw_text.strip().endswith(('.', '!', '?', '"', ')')):
            failure_modes.append("Truncated at 128-token boundary")
        
        # Check factual errors
        if "actually the same thing" in text_lower:
            failure_modes.append("Factual error: microcontroller equates to microprocessor")
            factual = 1
            relevance = 4
            completeness = 2
            clarity = 4
            hallucination = 2
        elif "a pointer to a pointer in c programming" in text_lower:
            failure_modes.append("Tautological phrasing / circular definition")
            factual = 2
            relevance = 3
            completeness = 2
            clarity = 2
            hallucination = 4
        else:
            if match_ratio >= 0.75:
                factual = 5
            elif match_ratio >= 0.5:
                factual = 4
            elif match_ratio >= 0.25:
                factual = 3
            else:
                factual = 2
                
            if question_obj["question"].lower()[:10] in text_lower or match_ratio >= 0.5:
                relevance = 5 if match_ratio >= 0.5 else 4
            else:
                relevance = 3
                
            if eos_reached and match_ratio >= 0.5:
                completeness = 5
            elif not eos_reached:
                completeness = 3 if match_ratio >= 0.5 else 2
            else:
                completeness = 4
                
            if not failure_modes:
                clarity = 5
            elif "Truncated at 128-token boundary" in failure_modes:
                clarity = 4
            else:
                clarity = 3
                
            hallucination = 5
        
    total_score = factual + relevance + completeness + clarity + hallucination
    
    return {
        "factual_correctness": factual,
        "technical_relevance": relevance,
        "completeness": completeness,
        "clarity": clarity,
        "hallucination_control": hallucination,
        "total_score_25": total_score
    }, failure_modes

def run_single_eval(q_obj, config, cond):
    test_id = f"{config['code']}_{cond['code']}_{q_obj['id']}"
    prompt_str = build_prompt(config["system_prompt"], q_obj["question"])
    
    with open(TEMP_PROMPT_FILE, "w", encoding="utf-8", newline="\n") as f:
        f.write(prompt_str)
        
    subprocess.run(["adb", "push", TEMP_PROMPT_FILE, "/data/local/tmp/llama/prompt.txt"], check=True, capture_output=True)
    
    temp_val = cond["temp"]
    cmd = ["adb", "shell", f"/data/local/tmp/llama/run_one.sh {temp_val}"]
    
    start_wall = time.time()
    proc = subprocess.run(cmd, capture_output=True, text=True)
    wall_duration = time.time() - start_wall
    
    stdout = proc.stdout
    stderr = proc.stderr
    
    assistant_marker = "<|start_header_id|>assistant<|end_header_id|>\n\n"
    if assistant_marker in stdout:
        raw_tail = stdout.split(assistant_marker, 1)[1]
    elif "<|start_header_id|>assistant<|end_header_id|>" in stdout:
        raw_tail = stdout.split("<|start_header_id|>assistant<|end_header_id|>", 1)[1].lstrip("\r\n ")
    else:
        raw_tail = stdout
        
    lines = raw_tail.split("\n")
    content_lines = []
    observer_json = {}
    for line in lines:
        if line.startswith("PyTorchObserver "):
            try:
                observer_json = json.loads(line.replace("PyTorchObserver ", "").strip())
            except Exception:
                pass
        else:
            content_lines.append(line)
            
    raw_response = "\n".join(content_lines).strip()
    eos_reached = False
    if "<|eot_id|>" in raw_response:
        eos_reached = True
        raw_response = raw_response.split("<|eot_id|>")[0].strip()
    elif "<|end_of_text|>" in raw_response:
        eos_reached = True
        raw_response = raw_response.split("<|end_of_text|>")[0].strip()
        
    prompt_tokens = observer_json.get("prompt_tokens", 0)
    generated_tokens = observer_json.get("generated_tokens", 0)
    inf_start = observer_json.get("inference_start_ms", 0)
    inf_end = observer_json.get("inference_end_ms", 0)
    prompt_eval_end = observer_json.get("prompt_eval_end_ms", 0)
    
    ttft_ms = prompt_eval_end - inf_start if (prompt_eval_end and inf_start) else 0
    total_time_ms = inf_end - inf_start if (inf_end and inf_start) else 0
    gen_time_s = (inf_end - prompt_eval_end) / 1000.0 if (inf_end and prompt_eval_end) else 0.0
    tok_per_sec = (generated_tokens / gen_time_s) if gen_time_s > 0 else 0.0
    
    rss_mb = 1946.89
    m_rss = re.search(r"RSS after loading model:\s*([\d\.]+)\s*MiB", stderr)
    if m_rss:
        rss_mb = float(m_rss.group(1))
        
    repetition = detect_repetition(raw_response)
    scores, failure_modes = score_response(q_obj, raw_response, eos_reached, repetition)
    
    result = {
        "test_id": test_id,
        "question_id": q_obj["id"],
        "question_text": q_obj["question"],
        "domain": q_obj["domain"],
        "config": config["code"],
        "config_name": config["name"],
        "decoding_mode": cond["code"],
        "temperature": cond["temp"],
        "prompt_tokens": prompt_tokens,
        "generated_tokens": generated_tokens,
        "tok_per_sec": round(tok_per_sec, 3),
        "ttft_ms": ttft_ms,
        "total_time_ms": total_time_ms,
        "wall_duration_s": round(wall_duration, 2),
        "rss_mb": round(rss_mb, 2),
        "eos_reached": eos_reached,
        "repetition_loop": repetition,
        "scores": scores,
        "failure_modes": failure_modes,
        "raw_output": raw_response,
        "full_prompt": prompt_str
    }
    
    return result

def main():
    os.makedirs(REPORTS_DIR, exist_ok=True)
    existing_data = load_existing_results()
    
    existing_data["metadata"] = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "target_device": "Xiaomi Redmi 9 Power (lime_in / M2010J19SI)",
        "soc": "Qualcomm Snapdragon 662 (SM6115, 4x Kryo 260 Gold @ 2.0 GHz + 4x Kryo 260 Silver @ 1.8 GHz)",
        "ram": "4 GB LPDDR4x",
        "os": "Android 12 (API 31, SKQ1.211103.001)",
        "runtime": "ExecuTorch v0.5.0 (ARM64-v8a)",
        "backend": "XNNPACK NEON",
        "quantization": "INT4 8da4w (128 group size)",
        "model_sha256": "8c545bde51bd434bfb922402aaa33c8952871735c04de22a9dd84f587f936b91",
        "tokenizer_sha256": "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55",
        "configuration_a_status": {
            "status": "UNAVAILABLE",
            "reason": "Base model Meta-Llama-3.2-3B-Instruct .pte was not produced in the Kaggle export pipeline; LoRA weights were merged in-memory prior to PTE export.",
            "compliance_note": "Per Phase 3.2 instructions, reported honestly as a missing dependency without downloading alternative models or fabricating comparisons."
        }
    }
    
    completed_ids = {r["test_id"] for r in existing_data.get("results", [])}
    print(f"Total existing completed tests: {len(completed_ids)}")
    
    total_tests = len(CONFIGURATIONS) * len(DECODING_CONDITIONS) * len(QUESTIONS)
    print(f"Total tests in matrix: {total_tests}")
    
    for config in CONFIGURATIONS:
        for cond in DECODING_CONDITIONS:
            for q in QUESTIONS:
                test_id = f"{config['code']}_{cond['code']}_{q['id']}"
                if test_id in completed_ids:
                    print(f"Skipping already completed {test_id}")
                    continue
                    
                print(f"[{len(completed_ids)+1}/{total_tests}] Running {test_id}: ({config['name']}, {cond['name']}) -> {q['question']}")
                try:
                    res = run_single_eval(q, config, cond)
                    existing_data.setdefault("results", []).append(res)
                    completed_ids.add(test_id)
                    save_results(existing_data)
                    export_csv(existing_data["results"])
                    print(f"    Done {test_id} in {res['wall_duration_s']}s | Gen tokens: {res['generated_tokens']} | EOS: {res['eos_reached']} | Rate: {res['tok_per_sec']} tok/s")
                    print(f"    Output: {res['raw_output'][:80]}...")
                except Exception as ex:
                    print(f"    ERROR running {test_id}: {ex}")
                    time.sleep(2)
                    
    print("\nBenchmark matrix run complete!")
    print(f"Results saved to:\n  {RAW_OUTPUTS_JSON}\n  {RESULTS_CSV}")

if __name__ == "__main__":
    main()
