#!/system/bin/sh
cd /data/local/tmp/llama
PROMPT="$(cat base_smoke_prompt.txt)"
PROMPT="${PROMPT}

"
./llama_main --model_path=base_llama3_2_3b.pte --tokenizer_path=tokenizer.model --prompt="$PROMPT" --seq_len=128 --temperature=0 --cpu_threads=4
