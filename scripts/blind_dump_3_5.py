import json
import os
import random

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW_JSON = os.path.join(PROJECT_ROOT, "reports", "phase_3_5_raw_outputs.json")
MAP_JSON = os.path.join(PROJECT_ROOT, "scripts", "blind_map_3_5.json")

with open(RAW_JSON, "r", encoding="utf-8") as f:
    data = json.load(f)

results = data["results"]
rng = random.Random(42)
indices = list(range(len(results)))
rng.shuffle(indices)

blind_map = {}
for b_idx, orig_idx in enumerate(indices, 1):
    r = results[orig_idx]
    blind_id = f"BLIND_{b_idx:02d}"
    blind_map[blind_id] = r["test_id"]
    print(f"=== {blind_id} | {r['decoding_mode']} | {r['question_id']}: {r['question_text']} ===")
    print(f"  Tokens: P={r['observed_prompt_tokens']} G={r['generated_tokens']} (Total={r['total_tokens_used']}) | Term={r['termination_reason']}")
    print(f"  Response:\n{r['response_text']}")
    print()

with open(MAP_JSON, "w", encoding="utf-8") as f:
    json.dump(blind_map, f, indent=2)
