import json
import csv
import os

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
json_path = os.path.join(PROJECT_ROOT, "reports", "phase_3_2_raw_outputs.json")
csv_path = os.path.join(PROJECT_ROOT, "reports", "phase_3_2_results.csv")

with open(json_path, "r", encoding="utf-8") as f:
    j = json.load(f)

with open(csv_path, "r", encoding="utf-8") as f:
    c = list(csv.DictReader(f))

print(f"JSON results count: {len(j.get('results', []))}")
print(f"CSV rows count: {len(c)}")

summary = {}
perf_summary = {}

for r in c:
    key = (r["config"], r["decoding_mode"])
    score = float(r["total_score_25"])
    tok_s = float(r["tok_per_sec"])
    ttft = float(r["ttft_ms"])
    gen_tok = int(r["generated_tokens"])
    
    summary.setdefault(key, []).append(score)
    perf_summary.setdefault(key, {
        "tok_s": [], "ttft": [], "gen_tok": [], "eos_count": 0, "rep_count": 0
    })
    perf_summary[key]["tok_s"].append(tok_s)
    perf_summary[key]["ttft"].append(ttft)
    perf_summary[key]["gen_tok"].append(gen_tok)
    if r["eos_reached"] == "True":
        perf_summary[key]["eos_count"] += 1
    if r["repetition_loop"] == "True":
        perf_summary[key]["rep_count"] += 1

print("\n--- Evaluation Quality Scores ---")
for (cfg, mode), scores in sorted(summary.items()):
    avg = sum(scores) / len(scores)
    print(f"Config {cfg} ({mode:8s}): N={len(scores)}, Mean Score = {avg:5.2f} / 25 ({avg/25*100:5.1f}%)")

print("\n--- Performance Metrics ---")
for (cfg, mode), p in sorted(perf_summary.items()):
    mean_rate = sum(p["tok_s"]) / len(p["tok_s"])
    mean_ttft = sum(p["ttft"]) / len(p["ttft"])
    mean_gen = sum(p["gen_tok"]) / len(p["gen_tok"])
    print(f"Config {cfg} ({mode:8s}): Mean Gen Rate = {mean_rate:.2f} tok/s | Mean TTFT = {mean_ttft:.0f} ms | Mean Tokens = {mean_gen:.1f} | EOS = {p['eos_count']}/{len(p['tok_s'])} | Rep Loops = {p['rep_count']}")
