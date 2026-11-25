import json
import csv
import os

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW_JSON = os.path.join(PROJECT_ROOT, "reports", "phase_3_5_raw_outputs.json")
MAP_JSON = os.path.join(PROJECT_ROOT, "scripts", "blind_map_3_5.json")
OUT_CSV = os.path.join(PROJECT_ROOT, "reports", "phase_3_5_results.csv")

# Blinded scores assigned prior to unblinding (keyed by BLIND_01..BLIND_32)
BLIND_SCORES = {
    "BLIND_01": (3, 2, 1, 4, 4, ["incomplete_answer", "premature_termination"], "Defines null pointer conceptually as absence of a pointer, but misses dereferencing consequences (UB/crash)."),
    "BLIND_02": (2, 3, 1, 1, 2, ["repetition_loop"], "Mentions interrupting a task, then degrades into a repetitive stuttering loop; omits hardware signal and ISR."),
    "BLIND_03": (0, 2, 0, 3, 1, ["technically_incorrect", "unsupported_claim"], "False statement ('a null pointer is a pointer that has been dereferenced'); misses UB/segmentation fault."),
    "BLIND_04": (1, 2, 1, 1, 1, ["repetition_loop", "technically_incorrect", "incomplete_sentence"], "Conflates modulation with error correction and repeats numbered section headers verbatim."),
    "BLIND_05": (3, 2, 0, 3, 5, ["premature_termination", "incomplete_answer"], "Correctly introduces ADC, then terminates mid-preamble before explaining sampling, quantization, or encoding."),
    "BLIND_06": (0, 2, 0, 3, 1, ["technically_incorrect", "unsupported_claim"], "False definition ('a null pointer is a pointer that has been dereferenced'); fails to explain dereferencing fault."),
    "BLIND_07": (3, 2, 0, 4, 4, ["incomplete_answer", "premature_termination"], "Valid generic statement ('both types of digital computers'), but omits all differences in memory/peripherals."),
    "BLIND_08": (0, 3, 0, 4, 0, ["technically_incorrect", "contradiction", "unsupported_claim"], "Explicitly false claim ('A microcontroller and a microprocessor are actually the same thing.')."),
    "BLIND_09": (3, 2, 0, 3, 5, ["premature_termination", "incomplete_sentence"], "Only expands RAM and ROM acronyms and terminates immediately without comparing volatility or roles."),
    "BLIND_10": (1, 2, 0, 1, 1, ["repetition_loop", "incomplete_answer", "context_limit_termination"], "Repeats '**Sequential Logic**' 13+ times until hitting 128-token context limit without explaining either logic type."),
    "BLIND_11": (5, 5, 5, 5, 5, [], "Concise, factually accurate, well-formed definition covering memory address storage of another variable."),
    "BLIND_12": (3, 2, 0, 2, 5, ["premature_termination", "incomplete_sentence"], "Introduces ADC, then cuts off at an unclosed bold heading before explaining working steps."),
    "BLIND_13": (1, 2, 0, 1, 1, ["repetition_loop", "technically_incorrect", "broken_markdown"], "Equates combinational logic with AND logic, repeats headings, and emits corrupted spelling ('Combinomial')."),
    "BLIND_14": (1, 2, 1, 1, 1, ["repetition_loop", "technically_incorrect"], "Wrongly claims Error Correction as modulation's purpose and loops into asking the prompt question."),
    "BLIND_15": (1, 2, 0, 1, 1, ["repetition_loop", "garbled_acronyms", "incomplete_answer"], "Degrades into 'Analog-digital-to-Digital-to-digital (Analogical conversion' without explaining ADC stages."),
    "BLIND_16": (2, 2, 0, 2, 4, ["repetition_loop", "premature_termination"], "Merely repeats the prompt question as a bold heading twice with no technical explanation."),
    "BLIND_17": (3, 4, 2, 2, 4, ["grammatical_stutter", "incomplete_answer"], "Touches carrier wave modification and encoding information, marred by grammatical stutter ('injecting a modulate')."),
    "BLIND_18": (5, 5, 5, 5, 5, [], "Clear, accurate, complete core definition stating memory address storage of another variable."),
    "BLIND_19": (0, 2, 0, 2, 0, ["technically_incorrect", "contradiction", "broken_markdown"], "Directly contradicts question by claiming Combinational Logic is 'also known as Sequential logic'."),
    "BLIND_20": (3, 3, 1, 4, 5, ["incomplete_answer", "premature_termination"], "Correctly expands RAM/ROM acronyms and notes computer hardware role, but omits volatility and differences."),
    "BLIND_21": (3, 3, 1, 4, 5, ["incomplete_answer", "premature_termination"], "Correctly expands RAM/ROM and notes different functions, but terminates before explaining volatility or roles."),
    "BLIND_22": (5, 5, 5, 5, 5, [], "Accurate, concise core definition stating memory address storage of a variable."),
    "BLIND_23": (2, 2, 0, 2, 4, ["repetition_loop", "broken_markdown", "premature_termination"], "Repeats question text and terminates on an isolated bold '**interrupt**' token."),
    "BLIND_24": (3, 3, 1, 4, 5, ["incomplete_answer", "premature_termination"], "Correctly expands RAM/ROM and notes distinct purposes, stopping before stating volatility/non-volatility."),
    "BLIND_25": (3, 2, 0, 4, 5, ["incomplete_answer", "premature_termination"], "Acknowledges two circuit types and offers to help, but stops without stating the difference."),
    "BLIND_26": (1, 2, 0, 1, 2, ["circular_definition", "repetition_loop"], "Circular tautology ('An interrupt is a type of interrupt, but what is an interrupt?')."),
    "BLIND_27": (1, 2, 0, 2, 2, ["technically_incorrect", "incomplete_sentence"], "Mischaracterizes null pointer ('pointer that is not used') and omits undefined behavior/crash."),
    "BLIND_28": (2, 3, 2, 2, 2, ["unsupported_claim", "repetition"], "Mentions transmitting/encoding info, but falsely claims modulation amplifies signals and omits carrier frequency."),
    "BLIND_29": (1, 2, 0, 1, 1, ["repetition_loop", "garbled_acronyms"], "Enters phrase repetition and corrupts acronym into 'Analog-to-digital-to-digital-to-digital'."),
    "BLIND_30": (4, 2, 0, 4, 5, ["incomplete_answer", "premature_termination"], "Accurate introductory classification ('both types of integrated circuits'), stops before differentiating."),
    "BLIND_31": (2, 4, 2, 3, 3, ["circular_definition"], "Circular phrasing ('Here's a pointer is a variable that holds a reference to a pointer') instead of variable address."),
    "BLIND_32": (0, 3, 0, 4, 0, ["technically_incorrect", "contradiction", "unsupported_claim"], "Explicitly false statement ('A microcontroller and a microprocessor are actually the same thing.')."),
}

with open(MAP_JSON, "r", encoding="utf-8") as f:
    blind_map = json.load(f)
with open(RAW_JSON, "r", encoding="utf-8") as f:
    raw_data = json.load(f)

test_to_blind = {v: k for k, v in blind_map.items()}

for r in raw_data["results"]:
    tid = r["test_id"]
    bid = test_to_blind[tid]
    f_s, rel_s, comp_s, clar_s, hall_s, flags, expl = BLIND_SCORES[bid]
    tot = f_s + rel_s + comp_s + clar_s + hall_s
    r["blind_id"] = bid
    r["scores"] = {
        "factual_correctness": f_s,
        "relevance": rel_s,
        "completeness": comp_s,
        "clarity": clar_s,
        "hallucination_control": hall_s,
        "total_score_25": tot,
    }
    r["failure_flags"] = flags
    r["score_explanation"] = expl

with open(RAW_JSON, "w", encoding="utf-8") as f:
    json.dump(raw_data, f, indent=2, ensure_ascii=False)

# Export CSV
fieldnames = [
    "test_id", "blind_id", "execution_order", "question_id", "domain", "model_key", "model_id",
    "decoding_mode", "temperature", "prompt_tokens", "generated_tokens", "total_tokens",
    "model_load_ms", "ttft_ms", "generation_time_ms", "total_inference_ms", "tok_per_sec",
    "rss_after_load_mib", "rss_after_gen_mib", "pre_mem_avail_mb", "post_batt_temp_c",
    "termination_reason", "exit_code", "factual_correctness", "relevance", "completeness",
    "clarity", "hallucination_control", "total_score_25", "failure_flags", "score_explanation",
    "question_text", "response_text"
]

with open(OUT_CSV, "w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=fieldnames)
    w.writeheader()
    for r in raw_data["results"]:
        s = r["scores"]
        w.writerow({
            "test_id": r["test_id"],
            "blind_id": r["blind_id"],
            "execution_order": r["execution_order"],
            "question_id": r["question_id"],
            "domain": r["domain"],
            "model_key": r["model_key"],
            "model_id": r["model_id"],
            "decoding_mode": r["decoding_mode"],
            "temperature": r["temperature"],
            "prompt_tokens": r["observed_prompt_tokens"],
            "generated_tokens": r["generated_tokens"],
            "total_tokens": r["total_tokens_used"],
            "model_load_ms": r["model_load_ms"],
            "ttft_ms": r["ttft_ms"],
            "generation_time_ms": r["generation_time_ms"],
            "total_inference_ms": r["total_inference_ms"],
            "tok_per_sec": r["tok_per_sec"],
            "rss_after_load_mib": r["rss_after_load_mib"],
            "rss_after_gen_mib": r["rss_after_gen_mib"],
            "pre_mem_avail_mb": r["pre_run_telemetry"].get("mem_available_mb"),
            "post_batt_temp_c": r["post_run_telemetry"].get("battery_temp_c"),
            "termination_reason": r["termination_reason"],
            "exit_code": r["exit_code"],
            "factual_correctness": s["factual_correctness"],
            "relevance": s["relevance"],
            "completeness": s["completeness"],
            "clarity": s["clarity"],
            "hallucination_control": s["hallucination_control"],
            "total_score_25": s["total_score_25"],
            "failure_flags": "; ".join(r["failure_flags"]),
            "score_explanation": r["score_explanation"],
            "question_text": r["question_text"],
            "response_text": r["response_text"].replace("\n", "\\n"),
        })

# Print Unblinded Comparative Analysis
def summarize(subset, label):
    n = len(subset)
    avg = lambda k: sum(r["scores"][k] for r in subset) / n
    avg_ttft = sum(r["ttft_ms"] for r in subset) / n
    avg_rate = sum(r["tok_per_sec"] for r in subset) / n
    avg_load = sum(r["model_load_ms"] for r in subset) / n
    avg_gen = sum(r["generated_tokens"] for r in subset) / n
    nat_eos = sum(1 for r in subset if r["termination_reason"] == "natural_eos")
    incorrect = sum(1 for r in subset if "technically_incorrect" in r["failure_flags"])
    incomplete = sum(1 for r in subset if "incomplete_answer" in r["failure_flags"] or "premature_termination" in r["failure_flags"] or r["scores"]["completeness"] <= 2)
    rep_loops = sum(1 for r in subset if "repetition_loop" in r["failure_flags"])
    print(f"=== {label} (N={n}) ===")
    print(f"  Avg Factual: {avg('factual_correctness'):.2f}/5 | Relevance: {avg('relevance'):.2f}/5 | Completeness: {avg('completeness'):.2f}/5 | Clarity: {avg('clarity'):.2f}/5 | HallucCtrl: {avg('hallucination_control'):.2f}/5")
    print(f"  Overall Mean Quality: {avg('total_score_25'):.2f}/25 ({avg('total_score_25')/25*100:.1f}%)")
    print(f"  Incorrect: {incorrect}/{n} | Incomplete: {incomplete}/{n} | Repetition Loops: {rep_loops}/{n} | Natural EOS: {nat_eos}/{n} ({nat_eos/n*100:.1f}%)")
    print(f"  Avg Load: {avg_load:.0f} ms | Avg TTFT: {avg_ttft:.0f} ms | Avg Rate: {avg_rate:.3f} tok/s | Avg Gen Tokens: {avg_gen:.1f}")

print()
for mk in ["A", "B"]:
    sub_all = [r for r in raw_data["results"] if r["model_key"] == mk]
    sub_g = [r for r in sub_all if r["decoding_mode"] == "greedy"]
    sub_s = [r for r in sub_all if r["decoding_mode"] == "sampling"]
    summarize(sub_g, f"Model {mk} - Greedy (temp=0.0)")
    summarize(sub_s, f"Model {mk} - Sampling (temp=0.7)")
    summarize(sub_all, f"Model {mk} - OVERALL")
    print()

print("=== PAIRED QUESTION COMPARISON (Total Score / 25) ===")
by_key = {(r["decoding_mode"], r["question_id"], r["model_key"]): r for r in raw_data["results"]}
for mode in ["greedy", "sampling"]:
    print(f"\n--- {mode.upper()} ---")
    for qid in [f"Q{i}" for i in range(1, 9)]:
        ra = by_key[(mode, qid, "A")]
        rb = by_key[(mode, qid, "B")]
        sa = ra["scores"]["total_score_25"]
        sb = rb["scores"]["total_score_25"]
        diff = sb - sa
        winner = "TIE" if diff == 0 else ("A (Base)" if sa > sb else "B (Merged)")
        print(f"  {qid}: A={sa:2d}/25 (G={ra['generated_tokens']:2d}) vs B={sb:2d}/25 (G={rb['generated_tokens']:2d}) | Diff(B-A)={diff:+3d} | Winner={winner}")
