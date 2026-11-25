#!/usr/bin/env bash
# LearnMate AI - ExecuTorch Standalone C++ Llama Runner Build Script for Android ARM64
# Target Architecture: arm64-v8a (Android 12+, Snapdragon 662 / Cortex-A73)
# Validated with ExecuTorch v0.5.0 & Android NDK r26c/r27b

set -euo pipefail

# Auto-detect Android NDK if not explicitly set
if [ -z "${ANDROID_NDK:-}" ]; then
    if [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME}/ndk" ]; then
        LATEST_NDK=$(find "${ANDROID_HOME}/ndk" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -n 1)
        if [ -n "${LATEST_NDK}" ]; then
            export ANDROID_NDK="${LATEST_NDK}"
            echo "Auto-detected ANDROID_NDK: ${ANDROID_NDK}"
        fi
    elif [ -d "$HOME/android-ndk" ]; then
        export ANDROID_NDK="$HOME/android-ndk"
    fi
fi

if [ -z "${ANDROID_NDK:-}" ] || [ ! -f "${ANDROID_NDK}/build/cmake/android.toolchain.cmake" ]; then
    echo "=================================================================="
    echo "ERROR: ANDROID_NDK environment variable is not set or invalid."
    echo "Please set ANDROID_NDK to your Android NDK path (tested with r26c/r27b)."
    echo "Example: export ANDROID_NDK=~/android-ndk-r26c"
    echo "=================================================================="
    exit 1
fi

EXECUTORCH_ROOT="${1:-$HOME/executorch}"
BUILD_DIR="${EXECUTORCH_ROOT}/cmake-out-android"

echo "=================================================================="
echo "LearnMate AI: Building ExecuTorch Standalone Runner for Android ARM64"
echo "ExecuTorch Root: ${EXECUTORCH_ROOT}"
echo "Android NDK:     ${ANDROID_NDK}"
echo "Output Prefix:   ${BUILD_DIR}"
echo "Target ABI:      arm64-v8a (Snapdragon 662, Android 12, API 26+)"
echo "=================================================================="

mkdir -p "${BUILD_DIR}"
cd "${EXECUTORCH_ROOT}"

# -------------------------------------------------------------------------
# Phase 1: Build & Install ExecuTorch Runtime, Backends, and Kernels
# -------------------------------------------------------------------------
echo "=== Phase 1: Building and installing core ExecuTorch runtime ==="
cmake \
    -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-26 \
    -DCMAKE_INSTALL_PREFIX="${BUILD_DIR}" \
    -DCMAKE_BUILD_TYPE=Release \
    -DEXECUTORCH_BUILD_XNNPACK=ON \
    -DEXECUTORCH_BUILD_EXTENSION_RUNNER_UTIL=ON \
    -DEXECUTORCH_BUILD_EXTENSION_DATA_LOADER=ON \
    -DEXECUTORCH_BUILD_KERNELS_OPTIMIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_QUANTIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_CUSTOM=ON \
    -B"${BUILD_DIR}" \
    .

echo "=== Compiling and installing ExecuTorch libraries ==="
cmake --build "${BUILD_DIR}" -j"$(nproc)" --target install

# -------------------------------------------------------------------------
# Phase 2: Build Standalone Llama Runner Binary (llama_main)
# -------------------------------------------------------------------------
echo "=== Phase 2: Building Llama standalone runner (llama_main) ==="
LLAMA_BUILD_DIR="${BUILD_DIR}/examples/models/llama"
mkdir -p "${LLAMA_BUILD_DIR}"

cmake \
    -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-26 \
    -DCMAKE_PREFIX_PATH="${BUILD_DIR}" \
    -DCMAKE_BUILD_TYPE=Release \
    -DEXECUTORCH_BUILD_KERNELS_OPTIMIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_QUANTIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_CUSTOM=ON \
    -DEXECUTORCH_BUILD_XNNPACK=ON \
    -B"${LLAMA_BUILD_DIR}" \
    examples/models/llama

cmake --build "${LLAMA_BUILD_DIR}" -j"$(nproc)" --config Release

echo "=================================================================="
echo "BUILD SUCCESSFUL!"
echo "Binary available at: ${LLAMA_BUILD_DIR}/llama_main"
echo ""
echo "To deploy to connected device via ADB:"
echo "  adb shell mkdir -p /data/local/tmp/llama"
echo "  adb push ${LLAMA_BUILD_DIR}/llama_main /data/local/tmp/llama/"
echo "  adb shell chmod +x /data/local/tmp/llama/llama_main"
echo "=================================================================="
