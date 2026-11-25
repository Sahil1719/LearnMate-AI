#!/usr/bin/env bash
set -euo pipefail

source "$HOME/learnmate-env/bin/activate"
export PATH="$HOME/.local/bin:$PATH"

EXECUTORCH_ROOT="$HOME/executorch"
HOST_BUILD="$EXECUTORCH_ROOT/cmake-out-host"
rm -rf "$HOST_BUILD"
mkdir -p "$HOST_BUILD"
cd "$EXECUTORCH_ROOT"

cmake -G Ninja \
    -DPYTHON_EXECUTABLE="$HOME/learnmate-env/bin/python3" \
    -DBUCK2="$HOME/.local/bin/buck2" \
    -DCMAKE_INSTALL_PREFIX="$HOST_BUILD" \
    -DCMAKE_BUILD_TYPE=Release \
    -DEXECUTORCH_BUILD_XNNPACK=ON \
    -DEXECUTORCH_BUILD_EXTENSION_RUNNER_UTIL=ON \
    -DEXECUTORCH_BUILD_EXTENSION_DATA_LOADER=ON \
    -DEXECUTORCH_BUILD_EXTENSION_MODULE=ON \
    -DEXECUTORCH_BUILD_EXTENSION_TENSOR=ON \
    -DEXECUTORCH_BUILD_KERNELS_OPTIMIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_QUANTIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_CUSTOM=ON \
    -B "$HOST_BUILD" \
    .

cmake --build "$HOST_BUILD" -j8 --target install

LLAMA_BUILD="$HOST_BUILD/examples/models/llama"
mkdir -p "$LLAMA_BUILD"

cmake -G Ninja \
    -DPYTHON_EXECUTABLE="$HOME/learnmate-env/bin/python3" \
    -DBUCK2="$HOME/.local/bin/buck2" \
    -DCMAKE_PREFIX_PATH="$HOST_BUILD" \
    -DCMAKE_BUILD_TYPE=Release \
    -DEXECUTORCH_BUILD_KERNELS_OPTIMIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_QUANTIZED=ON \
    -DEXECUTORCH_BUILD_KERNELS_CUSTOM=ON \
    -DEXECUTORCH_BUILD_XNNPACK=ON \
    -B "$LLAMA_BUILD" \
    examples/models/llama

cmake --build "$LLAMA_BUILD" -j8 --target llama_main
file "$LLAMA_BUILD/llama_main"
