#!/usr/bin/env bash
set -euo pipefail

echo "=== Building pte_mmap_runner for Android ARM64-v8a ==="

NDK="$HOME/android-ndk-r26c"
CXX="${NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++"
STRIP="${NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"
ET_ROOT="$HOME/executorch"
ET_BUILD="${ET_ROOT}/cmake-out-android"
SRC="./scripts/pte_mmap_runner.cpp"
OUT_DIR="./model/native/arm64-v8a"
OUT="${OUT_DIR}/pte_mmap_runner"

mkdir -p "${OUT_DIR}"

echo "Compiling ${SRC} -> ${OUT}..."

"${CXX}" -std=c++17 -O2 -fPIE -pie \
  -ffunction-sections -fdata-sections -fno-exceptions -fno-rtti \
  -I$HOME \
  -I"${ET_ROOT}" \
  -I"${ET_BUILD}" \
  -I"${ET_BUILD}/include" \
  "${SRC}" \
  -o "${OUT}" \
  -static-libstdc++ \
  -Wl,--gc-sections \
  -Wl,--whole-archive \
  "${ET_BUILD}/lib/liboptimized_native_cpu_ops_lib.a" \
  "${ET_BUILD}/lib/liboptimized_kernels.a" \
  "${ET_BUILD}/lib/libportable_kernels.a" \
  "${ET_BUILD}/lib/libquantized_kernels.a" \
  "${ET_BUILD}/lib/libquantized_ops_lib.a" \
  "${ET_BUILD}/lib/libxnnpack_backend.a" \
  -Wl,--no-whole-archive \
  "${ET_BUILD}/lib/libextension_module.a" \
  "${ET_BUILD}/lib/libextension_data_loader.a" \
  "${ET_BUILD}/lib/libextension_tensor.a" \
  "${ET_BUILD}/lib/libXNNPACK.a" \
  "${ET_BUILD}/lib/libmicrokernels-prod.a" \
  "${ET_BUILD}/lib/libpthreadpool.a" \
  "${ET_BUILD}/lib/libcpuinfo.a" \
  "${ET_BUILD}/lib/libcpublas.a" \
  "${ET_BUILD}/lib/libeigen_blas.a" \
  "${ET_BUILD}/lib/libexecutorch.a" \
  "${ET_BUILD}/lib/libexecutorch_core.a" \
  -llog -ldl -latomic -lm

echo "Original binary size:"
ls -lh "${OUT}"

echo "Stripping binary..."
"${STRIP}" --strip-unneeded "${OUT}"

echo "Stripped binary size:"
ls -lh "${OUT}"

echo "=== Build Complete ==="
