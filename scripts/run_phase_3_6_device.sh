#!/system/bin/sh
MODEL=$1
TEMP=$2
SEQ_LEN=$3
cd /data/local/tmp/llama
PROMPT="$(cat eval_prompt_3_6.txt)"
PROMPT="${PROMPT}

"
./llama_main --model_path="$MODEL" --tokenizer_path=tokenizer.model --prompt="$PROMPT" --seq_len="$SEQ_LEN" --temperature="$TEMP" --cpu_threads=4
