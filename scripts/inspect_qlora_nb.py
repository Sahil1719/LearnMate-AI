import json

nb_path = r".\training\notebooks\learnmate_qlora_training.ipynb"
with open(nb_path, "r", encoding="utf-8") as f:
    nb = json.load(f)

for i, c in enumerate(nb["cells"]):
    if c.get("cell_type") == "code":
        print(f"==================== CODE CELL {i} ====================")
        print("".join(c.get("source", [])))
