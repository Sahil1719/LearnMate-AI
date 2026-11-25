#!/system/bin/sh
TEMP=$1
if [ -z "$TEMP" ]; then
    TEMP=0
fi
cd /data/local/tmp/llama
PROMPT="$(cat prompt.txt)"
# Ensure trailing newlines after assistant header are preserved
PROMPT="${PROMPT}

"
./llama_main --model_path=learnmate_llama3_2_3b.pte --tokenizer_path=tokenizer.model --prompt="$PROMPT" --seq_len=128 --temperature=$TEMP --cpu_threads=4
