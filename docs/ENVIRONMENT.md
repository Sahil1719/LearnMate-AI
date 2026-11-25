# Environment and Hardware Audit

*Last verified: 2026-11-15 (Target Device Updated to Redmi 9 Power 4GB RAM)*

---

## 1. Development Host Hardware (Windows 11)
- **Host OS:** Windows 11 Home / Pro (x64)
- **Processor:** Intel Core 7 240H (16 physical/logical cores)
- **Host Physical RAM:** 16.0 GB
- **Host Dedicated GPU:** NVIDIA GeForce RTX 3050 Laptop GPU (6144 MiB VRAM)
- **NVIDIA Windows Driver:** 617.14 | **CUDA UMD Version:** 13.4
- **Primary Storage (C:\):** 475.42 GB Total, **266.73 GB Free** (Empirically verified via `DriveInfo`)

---

## 2. Windows Toolchains & SDKs
| Tool | Detected Version | Executable Path / Location | Status |
| :--- | :--- | :--- | :--- |
| **Android Studio** | 2026.2.2 (Build AI-262.10968.63) | `C:\Program Files\Android\Android Studio` | Installed & Verified |
| **Java JDK (Bundled)** | OpenJDK 25.0.4 (JBR) | `C:\Program Files\Android\Android Studio\jbr\bin\java.exe` | Installed & Verified |
| **Android SDK** | API 36/37 (`android-37.0`) | `<USER_HOME>\AppData\Local\Android\Sdk` | Installed & Verified |
| **Android Build-Tools** | 36.0.0 | `...\Android\Sdk\build-tools\36.0.0` | Installed & Verified |
| **ADB (Platform Tools)** | Android Debug Bridge version | `C:\platform-tools\adb.exe` | Installed & Verified |
| **Git for Windows** | Git 2.43.0+ | `C:\Program Files\Git\cmd\git.exe` | Installed & Verified |
| **Windows Python** | Python 3.12.10 | `...\Python\Python312\python.exe` | Installed & Verified |
| **NVIDIA CLI** | nvidia-smi 617.14 | `C:\Windows\system32\nvidia-smi.exe` | Installed & Verified |

---

## 3. WSL2 Subsystem (Ubuntu 24.04.4 LTS)
- **WSL Distribution:** `Ubuntu-24.04` (WSL2, Linux Kernel `6.18.33.2-microsoft-standard-WSL2`)
- **Assigned RAM:** 7.6 GiB (Free: ~5.9 GiB, Available: ~7.0 GiB)
- **Assigned Swap:** 2.0 GiB (Free: 2.0 GiB)
- **Filesystem Capacity:** 1007 GB virtual (backed by Windows physical SSD).
- **GPU Passthrough:** NVIDIA RTX 3050 Laptop GPU (6144 MiB total, driver 617.14) visible via WSL `nvidia-smi`.
- **Isolated Virtual Environment:** `~/learnmate-env` (Python 3.12.3, zero external packages, clean state).
- **Hugging Face Authentication:** Configured and active. User: `1sahil7` (Token stored in `~/.cache/huggingface/token`). Full access to `meta-llama/Meta-Llama-3-8B-Instruct` confirmed.
- **ExecuTorch Repository:** Cloned at `~/executorch` on tag `v0.5.0` (commit `484a4ab5b717ce1f364c469509214d0ed0cdaf11`).

---

## 4. Primary Target Smartphone Hardware (Minimum-Hardware Benchmark)
- **Device Model:** Xiaomi Redmi 9 Power (`M2010J19SI`, codename `lime` / `lime_in`)
- **ADB Serial:** `<DEVICE_SERIAL>`
- **ADB Connection State:** `device` (Authorized and online)
- **Target Architecture (ABI):** `arm64-v8a` (64-bit ARM)
- **OS Version:** Android 12 (`ro.build.version.release = 12`, SDK 31)
- **SoC Platform:** Qualcomm Snapdragon 662 (`SM6115` / platform `bengal`)
- **CPU Cores:** Octa-core CPU:
  - 4× Kryo 260 Gold (Cortex-A73 derivative @ 2.0 GHz, CPU part `0x800`)
  - 4× Kryo 260 Silver (Cortex-A53 derivative @ 1.8 GHz, CPU part `0x801`)
- **CPU Vector ISA:** ARMv8.0-A baseline NEON (`fp asimd aes pmull sha1 sha2 crc32 cpuid`). *Note: No native hardware dotprod (`asimddp`) or fp16 arithmetic (`fphp`).*
- **GPU:** Qualcomm Adreno 610
- **Physical Memory Reported (`/proc/meminfo`):** `3,802,456 kB` (~3.62 GB usable; ~380 MB reserved for modem/radio/display/GPU).
- **Live Memory Telemetry (Empirical):**
  - Total Usable RAM: **3.62 GB**
  - Active OS & Background Memory: **3.5 GB**
  - Available Memory (`MemAvailable`): **~1.48 GB** (1,488,128 kB)
  - Free Physical Memory (`MemFree`): **~91.8 MB** (91,896 kB)
  - Configured zRAM Swap: **2.1 GB** total (1.1 GB used, 1.0 GB free)
- **Internal Storage (`/data`):**
  - Total Size: 47 GB
  - Used: 21 GB
  - **Available Free Space: 26 GB** (Ample room for model weights, C++ runner, and Room DB)

---

## 5. Constraint & Feasibility Evaluation: 8B on 4GB Hardware
- **Model vs. RAM Paradox:** The 4-bit quantized Llama 3 8B model artifact (`llama3_8b_8da4w.pte`) is **~4.27 GB**. The total physical RAM of the Redmi 9 Power is **3.80 GB** (with only ~1.48 GB currently available).
- **Physical Consequence:** The model file is strictly larger than the total physical RAM of the device ($4.27\text{ GB} > 3.80\text{ GB}$).
- **Failure Risk:** Severe flash thrashing during sequential layer reads (estimated $\ge 10\text{ seconds/token}$) and high probability of `SIGKILL` by Android `lmkd`.
- **Engineering Protocol:**
  1. We do not abandon 8B prematurely; we execute the authorized Kaggle export to produce `llama3_8b_8da4w.pte`.
  2. We deploy and benchmark `llama3_8b_8da4w.pte` via standalone ExecuTorch C++ CLI on the Redmi 9 Power.
  3. We empirically measure loading, memory peaks, TTFT, tokens/sec, and crash logs.
  4. If LMK kills the 8B model, we present the measured logs as rigorous proof of mobile hardware boundaries and evaluate Llama 3.2 1B (`8da4w` size ~750MB) and 3B (`8da4w` size ~1.9GB) as technically viable low-memory alternatives.
