import subprocess

prompt = (
    "<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n"
    "You are LearnMate, an adaptive, offline on-device AI learning companion.\n"
    "[Goal: Embedded Systems Engineer]<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n"
    "What is a pointer in C?<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
)

with open("temp_c_q1.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write(prompt)

subprocess.run(["adb", "push", "temp_c_q1.txt", "/data/local/tmp/llama/prompt.txt"], check=True)

proc = subprocess.run(["adb", "shell", "/data/local/tmp/llama/run_one.sh 0"], capture_output=True, text=True)
print("--- STDOUT ---")
print(proc.stdout)
