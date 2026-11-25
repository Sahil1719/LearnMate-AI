#!/usr/bin/env bash
set -e
export PATH="$HOME/learnmate-env/bin:$HOME/.local/bin:$PATH"
python3 "./scripts/export_synthetic_pte.py"
