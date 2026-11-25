# ExecuTorch Runtime Readiness Report: LearnMate AI
**Target Runtime:** ExecuTorch v0.5.0  
**Target Hardware:** Qualcomm Snapdragon 662 (Redmi 9 Power, 4GB RAM, ARM64 Android 12)  
**Execution Environment:** Standalone C++ Native Binary (`llama_main`) + Android JNI Bridge (`libexecutorch_llama_jni.so`)  
**Audit Date:** 2026-11-15  
**Auditor:** Principal AI/ML & LLM Deployment Engineer

---

## 1. Executive Summary

This report delivers a comprehensive source-level audit of **ExecuTorch v0.5.0** for deploying quantized large language models to constrained mobile devices.

We have inspected the source code in Ubuntu 24.04 WSL, audited operator delegation, calculated exact static memory footprints for KV caches and weights, identified a critical memory loading defect in the default ExecuTorch Llama runner (`LoadMode::File`), and established the architectural design for our ARM64 JNI bridge and native C++ runner.

---

## 2. Source Code & Component Audit

### 2.1 Git Repository & Branch State
- **Repository Path:** `~/executorch` in WSL Ubuntu 24.04 (`1sahil7` authenticated).
- **Git Commit / Tag:** Checked out cleanly at official release **`v0.5.0`** (tag: `v0.5.0`).
- **Core Components Inspected:**
  1. `examples/models/llama/llama_transformer.py`: Python export model definition.
  2. `examples/models/llama/runner/runner.cpp`: C++ inference pipeline and memory loader.
  3. `examples/models/llama/main.cpp`: Standalone CLI benchmark runner.
  4. `extension/llm/tokenizer/`: `tiktoken.cpp`, `llama_tiktoken.cpp`, and `bpe_tokenizer.cpp`.
  5. `backends/xnnpack`: CPU delegation graph partitioner and GEMM microkernels.

---

## 3. Critical Technical Findings & Architecture Corrections

### 3.1 Defect in Default Model Loading (`LoadMode::File` vs `LoadMode::Mmap`)
In `examples/models/llama/runner/runner.cpp` (lines 48–56):
```cpp
Runner::Runner(
    const std::string& model_path,
    const std::string& tokenizer_path,
    const float temperature)
    : temperature_(temperature),
      module_(std::make_unique<Module>(model_path, Module::LoadMode::File)),
      tokenizer_path_(tokenizer_path),
      ...
```
- **The Problem:** The default constructor hardcodes `Module::LoadMode::File`. As documented in `extension/module/module.h`:
  ```cpp
  enum class LoadMode {
    /// Load the whole file as a buffer.
    File,
    /// Use mmap to load pages into memory.
    Mmap,
    ...
  };
  ```
  `LoadMode::File` invokes standard C++ heap allocation (`malloc` / `std::vector`) to read the **entire `.pte` file into contiguous RAM**. For our INT4 quantized Llama 3 8B model ($4.27\text{ GB}$), this requires a single allocation exceeding the total physical RAM of the Redmi 9 Power ($3.62\text{ GB}$ usable, ~1.5 GB free). On mobile, this results in an **immediate fatal crash (`std::bad_alloc`) or immediate termination by `lmkd`**.
- **The Solution:** For memory-constrained devices, `Module::LoadMode::Mmap` is mandatory. With `mmap`, the OS kernel maps the virtual memory space to the `.pte` file on flash storage and faults in only the active layer weights during the forward pass.
- **Action Taken:** Our custom Android JNI bridge (`native_llama_bridge.cpp`) and native runner scripts explicitly parameterize `use_mmap = true` to guarantee zero-heap model loading.

### 3.2 KV-Cache Precision & Memory Reality
In `examples/models/llama/source_transformation/sdpa.py`:
```python
self.kv_cache = kv_cache.to(torch.float)  # "Custom op only supports float32 currently."
```
- **Precision:** KV-cache is strictly **$\text{float32}$ (4 bytes/element)** in ExecuTorch v0.5.0; 16-bit or 8-bit KV caches are not currently supported by the custom SDPA kernel.
- **Exact Shape Across 32 Layers:**
  $$\text{Shape} = (B, n_{\text{kv\_heads}}, L_{\text{max}}, d_{\text{head}}) = (1, 8, L_{\text{max}}, 128)$$
  $$\text{Elements per Token} = 32 \times 2 \times 8 \times 128 = 65,536\text{ float32 elements}$$
- **Footprint by Context Length:**
  - **$\text{Max Seq } 512$:** $65,536 \times 512 \times 4\text{ bytes} = 134,217,728\text{ bytes} = \mathbf{128\text{ MB}}$.
  - **$\text{Max Seq } 256$:** $65,536 \times 256 \times 4\text{ bytes} = \mathbf{64\text{ MB}}$.
  - **$\text{Max Seq } 128$:** $65,536 \times 128 \times 4\text{ bytes} = \mathbf{32\text{ MB}}$.
- **Allocation Timing:** Statically registered as model buffers (`register_buffer("k_cache", ...)`) and allocated ahead-of-time during model loading, not dynamically per token.

### 3.3 XNNPACK Operator Delegation (`-X` Flag)
- When `-X` is enabled during export, the `XnnpackPartitioner` intercepts all dynamically quantized linear operators (`wq`, `wk`, `wv`, `wo`, `w1`, `w2`, `w3`) and delegates them to XNNPACK `DYNAMIC_QUANT` microkernels.
- On the Qualcomm Snapdragon 662 (ARMv8.0-A, Cortex-A73):
  - The chip lacks native ARMv8.2-A `dotprod` instructions (`asimddp`).
  - XNNPACK gracefully falls back to baseline 128-bit NEON microkernels (`xnn_qs8_gemm_minmax_fp32_*_neon`).
  - SIMD vectorization remains active, but will require ~2× CPU cycles per multiply-accumulate compared to newer chips with native dot-product instructions.

---

## 4. Android JNI Bridge & Service Architecture

We have established a clean 4-tier separation of concerns:

```
+--------------------------------------------------------------------+
|                         Jetpack Compose UI                         |
|  AiTutorScreen: Streaming TextFlow, Status Badge, Diagnostics Dialog|
+--------------------------------------------------------------------+
                                   │
                                   ▼
+--------------------------------------------------------------------+
|                 domain.engine.ExecuTorchLlmEngine                  |
|  Probes Storage, Coroutine flowOn(Dispatchers.IO), Fallback Engine |
+--------------------------------------------------------------------+
                                   │
                                   ▼
+--------------------------------------------------------------------+
|                 domain.engine.NativeLlamaBridge                    |
|  JNI Bindings: nativeInit, nativeGenerate, nativeStop, nativeRelease|
+--------------------------------------------------------------------+
                                   │
                                   ▼
+--------------------------------------------------------------------+
|               libexecutorch_llama_jni.so (C++17)                   |
|  ExecuTorch Module (LoadMode::Mmap), XNNPACK Backend, ARM64 NEON  |
+--------------------------------------------------------------------+
```

### 4.1 Native JNI Implementation
Created `app/src/main/cpp/native_llama_bridge.cpp` and `CMakeLists.txt`:
- Supports streaming token callbacks directly to Kotlin coroutine flows.
- Prevents UI freezes by offloading generation to background threads.
- Gracefully handles absent `.pte` weights by signaling the Kotlin engine to fall back to the offline pedagogical taxonomy without throwing uncaught native exceptions.

---

## 5. Toolchain Requirements & Remaining Setup

To compile the standalone ARM64 C++ runner (`llama_main`) and shared library (`libexecutorch_llama_jni.so`):
1. **Android NDK:** Install Android NDK `r26c` (or `r27b`) in WSL Ubuntu 24.04:
   ```bash
   # Target Path: ~/android-ndk-r26c
   export ANDROID_NDK=~/android-ndk-r26c
   ```
2. **ExecuTorch Submodules:** Populate Git submodules in `~/executorch`:
   ```bash
   cd ~/executorch
   git submodule update --init --recursive third-party/gflags third-party/flatbuffers backends/xnnpack extension/llm/third-party/sentencepiece
   ```
3. **Compilation Command:**
   ```bash
   bash scripts/deployment/build_android_llama_runner.sh ~/executorch
   ```
