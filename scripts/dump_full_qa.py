import csv
import os

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
csv_path = os.path.join(PROJECT_ROOT, "reports", "phase_3_2_results.csv")

with open(csv_path, "r", encoding="utf-8") as f:
    rows = list(csv.DictReader(f))

by_q = {}
for r in rows:
    qid = r["question_id"]
    cfg_mode = f"{r['config']}_{r['decoding_mode']}"
    by_q.setdefault(qid, {})[cfg_mode] = r

for qid in sorted(by_q.keys()):
    q_text = by_q[qid]["B_greedy"]["question_text"]
    domain = by_q[qid]["B_greedy"]["domain"]
    print(f"================================================================================")
    print(f"QUESTION {qid}: \"{q_text}\" ({domain})")
    print(f"================================================================================")
    for cfg in ["B_greedy", "B_sampling", "C_greedy", "C_sampling"]:
        data = by_q[qid].get(cfg, {})
        txt = data.get("raw_output", "")
        toks = data.get("generated_tokens", "")
        rate = data.get("tok_per_sec", "")
        eos = data.get("eos_reached", "")
        score = data.get("total_score_25", "")
        fails = data.get("failure_modes", "None")
        print(f"[{cfg}] (Tokens: {toks}, Rate: {rate} tok/s, EOS: {eos}, Score: {score}/25, Fails: {fails})")
        print(f"  Output: {txt}")
    print()
