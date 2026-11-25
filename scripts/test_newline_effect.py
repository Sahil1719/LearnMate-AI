import subprocess

cmd = [
    "adb", "shell",
    "cd /data/local/tmp/llama && PROMPT=\"$(cat prompt.txt)\" && PROMPT=\"${PROMPT}\n\n\" && ./llama_main --model_path=learnmate_llama3_2_3b.pte --tokenizer_path=tokenizer.model --prompt=\"$PROMPT\" --seq_len=128 --temperature=0 --cpu_threads=4"
]

print("Running with explicit newlines...")
proc = subprocess.run(cmd, capture_output=True, text=True)
print("STDOUT:")
print(repr(proc.stdout))
print("RAW:")
print(proc.stdout)
