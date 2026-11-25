import subprocess
import json
import re

prompt = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>

You are a helpful, respectful and honest assistant.<|eot_id|><|start_header_id|>user<|end_header_id|>

What is a pointer in C?<|eot_id|><|start_header_id|>assistant<|end_header_id|>

"""

with open("temp_test_prompt.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write(prompt)

subprocess.run(["adb", "push", "temp_test_prompt.txt", "/data/local/tmp/llama/prompt.txt"], check=True)

print("Starting test inference...")
proc = subprocess.run(
    ["adb", "shell", "/data/local/tmp/llama/run_one.sh 0"],
    capture_output=True,
    text=True
)

print("--- STDOUT ---")
print(proc.stdout)
print("--- STDERR ---")
print(proc.stderr)
