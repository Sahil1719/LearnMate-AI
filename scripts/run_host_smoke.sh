#!/usr/bin/env bash
set -euo pipefail
PROMPT="$(cat './temp_phase3_4_prompt.txt')"; PROMPT="${PROMPT}

"; $HOME/executorch/cmake-out-host/examples/models/llama/llama_main --model_path './model/experimental/base_llama3_2_3b.pte' --tokenizer_path './model/android/tokenizer.model' --prompt "$PROMPT" --seq_len 128 --temperature 0 --cpu_threads 4
