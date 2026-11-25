# On-Device Benchmarking Guide: Redmi 9 Power (4GB RAM)
**Target Device:** Xiaomi Redmi 9 Power (`<DEVICE_SERIAL>`, Snapdragon 662, 4GB RAM, Android 12)  
**Target Runtime:** ExecuTorch v0.5.0 (ARM64 Standalone Runner & Android App JNI)  
**Guide Scope:** Non-Root Empirical Benchmarking Protocol  
**Compliance:** Strict adherence to Project Rule 2 (Zero Fabricated Benchmarks)

---

## 1. Objective & Non-Negotiables

This guide establishes the standardized testing methodology for evaluating large language model inference on the Xiaomi Redmi 9 Power hardware benchmark.

Every metric reported in Project LearnMate AI must be **empirically measured and reproducible**. Never extrapolate, interpolate, or fabricate latency, tokens per second, or memory consumption.

---

## 2. Benchmark Metrics & Measurement Methodology

```
========================================================================================
                          ON-DEVICE BENCHMARKING METRICS
========================================================================================
Metric                          Unit            Measurement Tool / Command
----------------------------------------------------------------------------------------
Cold Model Load Time            Milliseconds    C++ Runner timer (after drop_caches)
Warm Model Load Time            Milliseconds    C++ Runner timer (subsequent load)
Prefill Latency                 Milliseconds    Time to encode prompt & prime KV cache
Time to First Token (TTFT)      Milliseconds    Wall-clock time from prompt to token 1
Decoding Speed                  Tokens / Sec    (Total Tokens - 1) / Decoding Duration
Total Inference Latency         Seconds         Total generation time
Peak PSS (Proportional Set)     Megabytes       `dumpsys meminfo com.learnmate.ai`
Peak RSS (Resident Set Size)    Megabytes       `/proc/$PID/statm` (rss * 4 kB)
Available System RAM            Megabytes       `grep MemAvailable /proc/meminfo`
Thermal Headroom                Celsius         `/sys/class/thermal/thermal_zone*/temp`
LMKD Stability                  Boolean / Exit  `logcat -d | grep -iE "lmkd|kill"`
========================================================================================
```

---

## 3. Preparation & Device Setup

### 3.1 Device Directory Structure
Ensure the deployment directory exists on the device:
```bash
adb shell mkdir -p /data/local/tmp/llama
```

### 3.2 Staging the Model & Tokenizer
Push the compiled standalone runner, exported `.pte` model, and tokenizer:
```bash
# Push binary and grant execute permissions
adb push cmake-out-android/examples/models/llama/llama_main /data/local/tmp/llama/
adb shell chmod +x /data/local/tmp/llama/llama_main

# Push tokenizer
adb push model/export/tokenizer.model /data/local/tmp/llama/

# Push model artifact (e.g. llama3_8b_8da4w.pte or stories110m.pte)
adb push model/export/llama3_8b_8da4w.pte /data/local/tmp/llama/
```

---

## 4. Execution Protocols

### 4.1 Cold-Start Benchmark Protocol
A cold start evaluates performance when no model pages exist in the Linux kernel page cache.
1. Force background cache flush (or reboot device):
   ```bash
   adb shell am force-stop com.learnmate.ai
   # Clear application state if testing inside APK
   ```
2. Record baseline available memory:
   ```bash
   adb shell "grep -E 'MemTotal|MemFree|MemAvailable|Cached' /proc/meminfo"
   ```
3. Record baseline thermal temperature:
   ```bash
   adb shell "cat /sys/class/thermal/thermal_zone0/temp"
   # Temperature in millidegrees Celsius (e.g., 34000 = 34.0°C)
   ```
4. Run inference with standard test prompt:
   ```bash
   adb shell "cd /data/local/tmp/llama && ./llama_main \
     --model_path llama3_8b_8da4w.pte \
     --tokenizer_path tokenizer.model \
     --prompt 'Explain pointer arithmetic in C in simple terms.' \
     --seq_len 128 \
     --temperature 0.7"
   ```

### 4.2 Warm-Start Benchmark Protocol
Immediately re-run the benchmark command without stopping the process or clearing caches to measure in-memory page cache acceleration:
```bash
adb shell "cd /data/local/tmp/llama && ./llama_main \
  --model_path llama3_8b_8da4w.pte \
  --tokenizer_path tokenizer.model \
  --prompt 'Explain pointer arithmetic in C in simple terms.' \
  --seq_len 128 \
  --temperature 0.7"
```

### 4.3 In-App Android Memory Profiling
When running inside the LearnMate AI application package (`com.learnmate.ai`):
1. In terminal 1, launch the generation query.
2. In terminal 2, poll memory consumption every 500ms:
   ```bash
   adb shell "dumpsys meminfo com.learnmate.ai | grep -E 'TOTAL PSS|Native Heap|Dalvik Heap'"
   ```
3. Check for low-memory killer events:
   ```bash
   adb shell "logcat -d -t 200 | grep -iE 'lmkd|lowmemorykiller|kill'"
   ```

---

## 5. Standard Test Prompts Across Disciplines

To ensure representative benchmarking, evaluate prompts across all supported domains:

| Category | Domain | Standard Test Prompt |
|---|---|---|
| **Technical** | C Programming | `"Explain pointer arithmetic in C in simple terms."` |
| **Commerce** | Accounting | `"Explain the golden rules of debit and credit in accounting."` |
| **Medical** | Healthcare | `"Summarize the key clinical criteria for diagnosing acute pancreatitis."` |
| **General** | Study Planning | `"How should a student structure a 2-hour daily study block for entrance exams?"` |

---

## 6. Empirical Results Logging Template

Record all measured data into the official benchmark table:

```
==================================================================================================
                              EMPIRICAL BENCHMARK RECORD TABLE
==================================================================================================
Date & Time:
Device Serial: <DEVICE_SERIAL> (Redmi 9 Power)
Model Evaluated:
PTE File Size (Bytes):
Quantization Scheme: TorchAO 8da4w + XNNPACK
Sequence Length:
Prompt Tokens:
Generated Tokens:
--------------------------------------------------------------------------------------------------
METRIC                          COLD START RUN                  WARM START RUN
--------------------------------------------------------------------------------------------------
Model Load Time (ms):
Prefill Latency (ms):
Time to First Token (TTFT, ms):
Generation Duration (s):
Tokens Per Second (tok/s):
Baseline MemAvailable (MB):
Peak Native PSS (MB):
Post-Run MemAvailable (MB):
Initial Device Temp (°C):
Post-Run Device Temp (°C):
LMKD Kill Encountered (Y/N):
Process Exit Status Code:
==================================================================================================
```
