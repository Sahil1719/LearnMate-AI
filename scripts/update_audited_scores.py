import json
import csv
import os

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
json_path = os.path.join(PROJECT_ROOT, "reports", "phase_3_2_raw_outputs.json")
csv_path = os.path.join(PROJECT_ROOT, "reports", "phase_3_2_results.csv")

with open(json_path, "r", encoding="utf-8") as f:
    data = json.load(f)

# Expert Engineering Audit Scoring Matrix
# Rubric: factual_correctness (0-5), technical_relevance (0-5), completeness (0-5), clarity (0-5), hallucination_control (0-5)
AUDITED_SCORES = {
    "B_greedy_Q1": {"factual": 5, "relevance": 5, "completeness": 5, "clarity": 5, "hallucination": 5, "fails": []},
    "B_greedy_Q2": {"factual": 2, "relevance": 4, "completeness": 2, "clarity": 2, "hallucination": 4, "fails": ["Incomplete sentence / unclosed parenthesis", "Repetitive phrasing"]},
    "B_greedy_Q3": {"factual": 2, "relevance": 3, "completeness": 2, "clarity": 2, "hallucination": 3, "fails": ["Recursive stuttering loop ('interrupted or stopped from executing...')"]},
    "B_greedy_Q4": {"factual": 1, "relevance": 4, "completeness": 1, "clarity": 4, "hallucination": 1, "fails": ["Severe factual error: claims MCU and MPU are the same thing"]},
    "B_greedy_Q5": {"factual": 2, "relevance": 2, "completeness": 1, "clarity": 3, "hallucination": 4, "fails": ["Pedantic non-answer; corrects user to 'analog-digital converter'"]},
    "B_greedy_Q6": {"factual": 2, "relevance": 3, "completeness": 2, "clarity": 3, "hallucination": 3, "fails": ["Digression into 'null pointer arithmetic operations on arrays'; misses segfault/crash"]},
    "B_greedy_Q7": {"factual": 1, "relevance": 4, "completeness": 1, "clarity": 3, "hallucination": 1, "fails": ["Severe factual error: claims Combinational logic is also known as Sequential logic"]},
    "B_greedy_Q8": {"factual": 3, "relevance": 3, "completeness": 2, "clarity": 3, "hallucination": 5, "fails": ["Generic platitude; omits RF propagation, antenna size, multiplexing"]},

    "B_sampling_Q1": {"factual": 5, "relevance": 5, "completeness": 5, "clarity": 5, "hallucination": 5, "fails": []},
    "B_sampling_Q2": {"factual": 2, "relevance": 4, "completeness": 2, "clarity": 2, "hallucination": 3, "fails": ["Syntactically garbled acronym nesting ('PC is a CPU (Central Processing Unit (CPU)')"]},
    "B_sampling_Q3": {"factual": 2, "relevance": 3, "completeness": 2, "clarity": 2, "hallucination": 2, "fails": ["Garbled conclusion ('prevents the system from the computer from a computer')"]},
    "B_sampling_Q4": {"factual": 1, "relevance": 4, "completeness": 1, "clarity": 4, "hallucination": 1, "fails": ["Severe factual error: claims MCU and MPU are actually the same thing"]},
    "B_sampling_Q5": {"factual": 2, "relevance": 2, "completeness": 1, "clarity": 3, "hallucination": 4, "fails": ["Echoes query without explaining purpose"]},
    "B_sampling_Q6": {"factual": 2, "relevance": 3, "completeness": 1, "clarity": 2, "hallucination": 4, "fails": ["Halts abruptly after 24 tokens without explanation"]},
    "B_sampling_Q7": {"factual": 2, "relevance": 3, "completeness": 1, "clarity": 2, "hallucination": 3, "fails": ["Broken markdown formatting and incomplete question"]},
    "B_sampling_Q8": {"factual": 2, "relevance": 3, "completeness": 1, "clarity": 1, "hallucination": 2, "fails": ["Degraded repetition loop: '1. Here are some reasons why is used in communication systems?'"]},

    "C_greedy_Q1": {"factual": 2, "relevance": 4, "completeness": 2, "clarity": 2, "hallucination": 4, "fails": ["Tautological definition: 'a pointer in C is a pointer to a pointer in C'"]},
    "C_greedy_Q2": {"factual": 2, "relevance": 3, "completeness": 2, "clarity": 3, "hallucination": 5, "fails": ["Conversational filler preamble, halts at section heading"]},
    "C_greedy_Q3": {"factual": 1, "relevance": 1, "completeness": 1, "clarity": 2, "hallucination": 1, "fails": ["Severe goal hallucination bleeding: echoes 'embedded systems engineer' instead of answering"]},
    "C_greedy_Q4": {"factual": 1, "relevance": 4, "completeness": 1, "clarity": 3, "hallucination": 1, "fails": ["Severe category error: claims MPU and MCU are 'both types of microcontrollers'"]},
    "C_greedy_Q5": {"factual": 3, "relevance": 4, "completeness": 3, "clarity": 3, "hallucination": 3, "fails": ["Hallucinates 'digital-to-analog-to-digital converter'"]},
    "C_greedy_Q6": {"factual": 1, "relevance": 2, "completeness": 1, "clarity": 2, "hallucination": 1, "fails": ["Nonsense claim: 'null pointer is not a null'"]},
    "C_greedy_Q7": {"factual": 1, "relevance": 1, "completeness": 1, "clarity": 1, "hallucination": 1, "fails": ["Catastrophic 79-token repetition loop, exceeded sequence boundary without EOS"]},
    "C_greedy_Q8": {"factual": 3, "relevance": 3, "completeness": 1, "clarity": 4, "hallucination": 5, "fails": ["Abrupt termination after single generic sentence (10 tokens)"]},

    "C_sampling_Q1": {"factual": 5, "relevance": 5, "completeness": 5, "clarity": 5, "hallucination": 5, "fails": []},
    "C_sampling_Q2": {"factual": 2, "relevance": 3, "completeness": 1, "clarity": 2, "hallucination": 5, "fails": ["Echoes question header as sentence fragment (10 tokens)"]},
    "C_sampling_Q3": {"factual": 2, "relevance": 3, "completeness": 2, "clarity": 2, "hallucination": 2, "fails": ["Word salad repetition: 'a specific to a specific to the system'"]},
    "C_sampling_Q4": {"factual": 1, "relevance": 3, "completeness": 2, "clarity": 2, "hallucination": 1, "fails": ["Category error: 'both types of microcontrollers in a computer system'"]},
    "C_sampling_Q5": {"factual": 4, "relevance": 4, "completeness": 3, "clarity": 3, "hallucination": 4, "fails": ["Conversational artifact: 'You're referring to an analog-to-digital converter is...'"]},
    "C_sampling_Q6": {"factual": 1, "relevance": 2, "completeness": 1, "clarity": 1, "hallucination": 1, "fails": ["Hallucinates AI systems into null pointer explanation"]},
    "C_sampling_Q7": {"factual": 2, "relevance": 3, "completeness": 2, "clarity": 3, "hallucination": 4, "fails": ["Tautological non-answer: 'used to study the difference between combinational and sequential'"]},
    "C_sampling_Q8": {"factual": 3, "relevance": 3, "completeness": 2, "clarity": 3, "hallucination": 4, "fails": ["Tautological padding: 'particularly in the context of communication systems'"]},
}

# Update results
for r in data["results"]:
    tid = r["test_id"]
    if tid in AUDITED_SCORES:
        aud = AUDITED_SCORES[tid]
        tot = aud["factual"] + aud["relevance"] + aud["completeness"] + aud["clarity"] + aud["hallucination"]
        r["scores"] = {
            "factual_correctness": aud["factual"],
            "technical_relevance": aud["relevance"],
            "completeness": aud["completeness"],
            "clarity": aud["clarity"],
            "hallucination_control": aud["hallucination"],
            "total_score_25": tot
        }
        r["failure_modes"] = aud["fails"]

with open(json_path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

# Export updated CSV
fieldnames = [
    "test_id", "question_id", "domain", "config", "decoding_mode", "temperature",
    "prompt_tokens", "generated_tokens", "tok_per_sec", "ttft_ms", "total_time_ms",
    "rss_mb", "eos_reached", "repetition_loop", "factual_correctness",
    "technical_relevance", "completeness", "clarity", "hallucination_control",
    "total_score_25", "failure_modes", "question_text", "raw_output"
]

with open(csv_path, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    for r in data["results"]:
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
            "repetition_loop": len(r.get("failure_modes", [])) > 0 and any("loop" in f.lower() or "repetition" in f.lower() or "stutter" in f.lower() for f in r.get("failure_modes", [])),
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

print("Successfully updated raw JSON and CSV with rigorous audited engineering scores!")
