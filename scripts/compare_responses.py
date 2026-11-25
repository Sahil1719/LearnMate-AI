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

print(f"{'QID':<4} | {'Config B (Greedy)':<45} | {'Config C (Greedy)':<45}")
print("-" * 100)
for qid in sorted(by_q.keys()):
    b_gr = by_q[qid].get("B_greedy", {}).get("raw_output", "")[:42]
    c_gr = by_q[qid].get("C_greedy", {}).get("raw_output", "")[:42]
    print(f"{qid:<4} | {b_gr:<45} | {c_gr:<45}")

print("\n" + "="*100 + "\n")

print(f"{'QID':<4} | {'Config B (Sampling)':<45} | {'Config C (Sampling)':<45}")
print("-" * 100)
for qid in sorted(by_q.keys()):
    b_sm = by_q[qid].get("B_sampling", {}).get("raw_output", "")[:42]
    c_sm = by_q[qid].get("C_sampling", {}).get("raw_output", "")[:42]
    print(f"{qid:<4} | {b_sm:<45} | {c_sm:<45}")
