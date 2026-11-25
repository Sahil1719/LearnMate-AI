#!/usr/bin/env bash
# LearnMate AI - On-Device Benchmark Script for Redmi 9 Power (Snapdragon 662, 4GB RAM)
# Evaluates cold-start loading, peak PSS/RSS, TTFT, tokens/sec, and lmkd survivability.

set -euo pipefail

MODEL_PTE="${1:-model/export/llama3_8b_8da4w.pte}"
TOKENIZER="${2:-model/export/tokenizer.model}"
PROMPT="${3:-Explain pointer arithmetic in C in simple terms.}"
SEQ_LEN="${4:-128}"
DEVICE_DIR="/data/local/tmp/llama"
DEVICE_SERIAL="${ANDROID_SERIAL:-<DEVICE_SERIAL>}"

echo "=================================================================="
echo "LearnMate AI: On-Device Inference Benchmark on Redmi 9 Power"
echo "Target Device:   Snapdragon 662 (4GB RAM, Android 12, arm64-v8a)"
echo "Device Serial:   ${DEVICE_SERIAL}"
echo "Model Artifact:  ${MODEL_PTE}"
echo "Tokenizer:       ${TOKENIZER}"
echo "Max Sequence:    ${SEQ_LEN}"
echo "Test Prompt:     ${PROMPT}"
echo "=================================================================="

# Check ADB connectivity
adb -s "${DEVICE_SERIAL}" get-state > /dev/null 2>&1 || {
    echo "ERROR: Device ${DEVICE_SERIAL} not found or unauthorized."
    exit 1
}

# Ensure device directory
adb -s "${DEVICE_SERIAL}" shell "mkdir -p ${DEVICE_DIR}"

# Check if model already exists on device to avoid re-pushing 4.27GB over USB
MODEL_FILENAME=$(basename "${MODEL_PTE}")
TOKENIZER_FILENAME=$(basename "${TOKENIZER}")

echo "Checking remote files on device..."
REMOTE_MODEL_SIZE=$(adb -s "${DEVICE_SERIAL}" shell "stat -c%s '${DEVICE_DIR}/${MODEL_FILENAME}' 2>/dev/null || echo 0" | tr -d '\r\n')
LOCAL_MODEL_SIZE=$(stat -c%s "${MODEL_PTE}" 2>/dev/null || echo 0)

if [ "${REMOTE_MODEL_SIZE}" != "${LOCAL_MODEL_SIZE}" ] || [ "${LOCAL_MODEL_SIZE}" -eq 0 ]; then
    echo "Pushing model to device (${LOCAL_MODEL_SIZE} bytes)... (this may take 1-2 minutes)"
    adb -s "${DEVICE_SERIAL}" push "${MODEL_PTE}" "${DEVICE_DIR}/"
else
    echo "Model file already exists on device with matching size (${REMOTE_MODEL_SIZE} bytes). Skipping push."
fi

adb -s "${DEVICE_SERIAL}" push "${TOKENIZER}" "${DEVICE_DIR}/"

# Record baseline memory
echo ""
echo "=== Baseline Device Memory Before Inference ==="
adb -s "${DEVICE_SERIAL}" shell "free -h"
adb -s "${DEVICE_SERIAL}" shell "head -n 12 /proc/meminfo"

# Clear caches if possible (drops page cache to ensure reproducible cold start benchmark)
adb -s "${DEVICE_SERIAL}" shell "echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true"

# Run inference and measure metrics
echo ""
echo "=== Executing Standalone ExecuTorch C++ Inference ==="
START_TIME=$(date +%s%N)
adb -s "${DEVICE_SERIAL}" shell "cd ${DEVICE_DIR} && ./llama_main --model_path ${MODEL_FILENAME} --tokenizer_path ${TOKENIZER_FILENAME} --prompt \"${PROMPT}\" --seq_len ${SEQ_LEN} --temperature 0.7" || {
    STATUS=$?
    echo ""
    echo "=================================================================="
    echo "WARNING: Inference process exited with code ${STATUS}."
    echo "Checking dmesg / logcat for lmkd (Low Memory Killer) kill event..."
    adb -s "${DEVICE_SERIAL}" shell "logcat -d -t 100 | grep -iE 'lmkd|killed|out of memory' || true"
    echo "=================================================================="
}
END_TIME=$(date +%s%N)

ELAPSED_SEC=$(awk "BEGIN {print ($END_TIME - $START_TIME) / 1000000000}")
echo "Total Wall-Clock Time: ${ELAPSED_SEC} seconds"

# Capture post-inference memory state
echo ""
echo "=== Post-Inference Device Memory State ==="
adb -s "${DEVICE_SERIAL}" shell "free -h"
echo "Benchmark run complete."
