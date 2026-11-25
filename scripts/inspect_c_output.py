import json
import os

p = r".\reports\phase_3_2_raw_outputs.json"
with open(p, "r", encoding="utf-8") as f:
    data = json.load(f)

for r in data.get("results", []):
    if r["test_id"] in ["B_greedy_Q1", "C_greedy_Q2", "C_greedy_Q4"]:
        print(f"=== TEST ID: {r['test_id']} ===")
        print("Prompt tokens:", r.get("prompt_tokens"))
        print("Generated tokens:", r.get("generated_tokens"))
        print("Raw output:")
        print(repr(r.get("raw_output")))
        print("Full prompt:")
        print(repr(r.get("full_prompt")))
