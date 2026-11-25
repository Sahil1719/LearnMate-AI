# Standalone ExecuTorch ARM64 Native Runners

This directory contains standalone Android ARM64 native executables compiled for offline benchmarking and execution on physical ARM64 Android devices (including the Xiaomi Redmi 9 Power, Snapdragon 662).

## 1. Compiled Binaries

| Binary | Size | Format | Architecture | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `arm64-v8a/pte_mmap_runner` | ~10.7 MB | ELF 64-bit LSB PIE (stripped) | ARM aarch64 | Minimal ExecuTorch PTE runner with `LoadMode::Mmap` for synthetic/portable graph execution |
| `arm64-v8a/llama_main` | ~11.4 MB | ELF 64-bit LSB PIE (stripped) | ARM aarch64 | Standalone LLaMA LLM runner with TikToken BPE tokenizer, XNNPACK backend, sampler, and mmap loader |
| `arm64-v8a/executor_runner` | ~9.8 MB | ELF 64-bit LSB PIE (stripped) | ARM aarch64 | General ExecuTorch program runner for testing portable/optimized PTE artifacts |

> **Repository Note on Binary Artifacts:** In accordance with Git and GitHub best practices, compiled native executables are kept locally on disk for ADB execution and excluded from version control tracking via `.gitignore`. All executables can be reproducibly recompiled from source at any time:
> - `pte_mmap_runner`: Run `bash scripts/compile_pte_mmap_runner.sh` (Source: `scripts/pte_mmap_runner.cpp`)
> - `llama_main`: Run `bash scripts/deployment/build_android_llama_runner.sh`

## 2. Compilation Toolchain & Flags

- **NDK Version:** Android NDK r26c (Clang 17.0.2)
- **Target ABI:** `arm64-v8a`
- **Target Android API:** API 26 (Android 8.0 Oreo and higher; physical test device is Android 12)
- **Build System:** CMake 3.28.3 + Ninja 1.11.1
- **ExecuTorch Version:** v0.5.0 (Commit `484a4ab`)
- **Key Flags:**
  - `-DEXECUTORCH_BUILD_XNNPACK=ON` (ARMv8.0-A NEON GEMM microkernels enabled)
  - `-DEXECUTORCH_BUILD_KERNELS_OPTIMIZED=ON`
  - `-DEXECUTORCH_BUILD_KERNELS_QUANTIZED=ON`
  - `-DEXECUTORCH_BUILD_KERNELS_CUSTOM=ON`
  - `-DEXECUTORCH_BUILD_EXTENSION_RUNNER_UTIL=ON`
  - `-DEXECUTORCH_BUILD_EXTENSION_DATA_LOADER=ON`
  - `-DEXECUTORCH_BUILD_EXTENSION_MODULE=ON`

## 3. Critical Low-Memory Patch (`runner.cpp`)

Default ExecuTorch `examples/models/llama/runner/runner.cpp` line 47 hardcodes:
```cpp
module_(std::make_unique<Module>(model_path, Module::LoadMode::File))
```
On a 4GB RAM device (such as the Redmi 9 Power with ~1.4GB free RAM), `LoadMode::File` invokes `malloc(3.5GB)` to read the entire `.pte` into heap memory, causing an instant `ENOMEM` / OOM abort.

**Applied Patch:**
```cpp
module_(std::make_unique<Module>(model_path, Module::LoadMode::Mmap))
```
With `LoadMode::Mmap`, the OS kernel memory-maps the file into virtual address space, faulting pages on demand during forward passes with zero upfront physical RAM allocation.

## 4. Dynamic Linking Verification

Inspection via `aarch64-linux-android-readelf -d llama_main`:
```
 0x0000000000000001 (NEEDED)             Shared library: [liblog.so]
 0x0000000000000001 (NEEDED)             Shared library: [libm.so]
 0x0000000000000001 (NEEDED)             Shared library: [libdl.so]
 0x0000000000000001 (NEEDED)             Shared library: [libc.so]
```
Zero non-standard shared libraries are required. C++ runtime (`libc++`) and ExecuTorch libraries are statically linked.

## 5. Physical Device Execution Protocol

Push and execute directly on the physical phone over ADB without rooting:

```bash
# Push binary to Android temporary execution directory
adb push arm64-v8a/llama_main /data/local/tmp/llama_main
adb shell chmod 755 /data/local/tmp/llama_main

# Verify help & flag diagnostics
adb shell /data/local/tmp/llama_main --help

# Execution syntax (once .pte model and tokenizer are staged)
adb shell "/data/local/tmp/llama_main \
  --model_path /data/local/tmp/llama/llama3_8b_8da4w.pte \
  --tokenizer_path /data/local/tmp/llama/tokenizer.model \
  --prompt \"Explain binary search step-by-step.\" \
  --seq_len 128 \
  --temperature 0.7 \
  --cpu_threads 1"
```
