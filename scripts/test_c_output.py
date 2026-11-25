import subprocess

prompt = """<|begin_of_text|><|start_header_id|>system<|end_header_id|>

You are LearnMate, an adaptive, offline on-device AI learning companion.
[Goal: Embedded Systems Engineer]<|eot_id|><|start_header_id|>user<|end_header_id|>

Explain the role of the program counter in computer architecture.<|eot_id|><|start_header_id|>assistant<|end_header_id|>

"""

with open("temp_c_test.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write(prompt)

subprocess.run(["adb", "push", "temp_c_test.txt", "/data/local/tmp/llama/prompt.txt"], check=True)

proc = subprocess.run(["adb", "shell", "/data/local/tmp/llama/run_one.sh 0"], capture_output=True, text=True)
print("--- STDOUT ---")
print(repr(proc.stdout))
print("--- RAW PRINT ---")
print(proc.stdout)
