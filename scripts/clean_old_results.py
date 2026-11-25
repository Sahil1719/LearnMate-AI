import json
import os

p = r".\reports\phase_3_2_raw_outputs.json"
with open(p, "r", encoding="utf-8") as f:
    data = json.load(f)

# Keep only valid Configuration B results (except B_sampling_Q8 which had 0 tokens)
cleaned_results = []
for r in data.get("results", []):
    if r.get("config") == "B" and r.get("test_id") != "B_sampling_Q8":
        cleaned_results.append(r)

print(f"Retained {len(cleaned_results)} valid Configuration B results.")
data["results"] = cleaned_results

with open(p, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print("Saved cleaned raw outputs.")
