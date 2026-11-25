# LearnMate AI

**Offline On-Device Personalized AI Learning Companion for Android**  
**Competition:** Bharat AI-SoC Student Challenge | **Team:** LearnMateAI (`Sahil Nagvekar`, `Sanjana Choudhary`)

LearnMate AI is a self-contained, offline-first Android application built with Jetpack Compose and **Room SQLite v3**, executing **Meta Llama 3.2 3B Instruct INT4** locally on-device via **PyTorch ExecuTorch v0.5.0** (`XNNPACK NEON`, `mmap=1`, `seq_len=128`, `threads=4`) without cloud API dependencies.

- **Demo Video (Google Drive):** [Watch LearnMate AI Demo Recording](https://drive.google.com/file/d/1bT_EuWgy9iu33loTvx3gl8_Acp6NHhYN/view?usp=sharing)
- **Prebuilt Signed Release APK (`v1.0.0`):** [GitHub Release v1.0.0 — Pending Publication](https://github.com/Sahil1719/LearnMate-AI/releases/tag/v1.0.0) *(placeholder — release not yet published; verified SHA-256: `664409BA8E2F7B2EC4EBC0EA165691C493FD7D95EB439558E8B1DFF8C6AC2828`, size: `1,956,712,238` bytes)*
- **Technical Report:** [docs/FINAL_TECHNICAL_REPORT.md](docs/FINAL_TECHNICAL_REPORT.md) (authoritative verification log: [reports/FINAL_RELEASE_BUILD_AND_TEST_REPORT.md](reports/FINAL_RELEASE_BUILD_AND_TEST_REPORT.md) and [reports/final_submission_checksums.txt](reports/final_submission_checksums.txt))

---

## 1. Key Features

- **Self-Contained On-Device Packaging & Integrity Verification:** The signed Release APK (`com.learnmate.ai`, `1.0.0`, `arm64-v8a`) bundles `learnmate_llama3_2_3b.pte` (`1,929,907,360` bytes) and `tokenizer.model` (`2,183,982` bytes) stored uncompressed via `noCompress`. On first launch, `ModelAssetManager` checks free storage (`2,036,948,942`-byte precheck threshold), streams both assets into app-private storage, verifies SHA-256 digests, and caches `.verified_manifest` so cold relaunches skip re-copying.
- **Authentic Local Inference (No Silent Fallback):** Uses `libexecutorch_llama_jni.so` (`ExecuTorch v0.5.0`) with TikToken (`vocab_size=128256`). When `NativeLlamaBridge` is present and native initialization fails (`nativeHandle == 0L`), `ExecuTorchLlmEngine` emits `InferenceState.Error` rather than falling back to synthetic template responses.
- **Adaptive Learning Workflows:** Sequential domain-independent onboarding (`OnboardingFlow`), dashboard readiness tracking (`DashboardScreen`), deterministic skill-gap analysis (`SkillAnalysisScreen`), phase-based study roadmap (`RoadmapScreen`), daily study planner, and offline diagnostic quizzes with adaptive remediation (`QuizScreen`).
- **Multi-Profile Scoping & Persistent AI Tutor History (Room v3):** Explicit non-destructive `MIGRATION_2_3` isolates learner profiles (`SkillEntity`, `RoadmapPhaseEntity`, `StudyTaskEntity`, `AssessmentRecordEntity`) and cascade-linked AI Tutor conversations (`ConversationEntity`, `ChatMessageEntity`) across restarts (`android:allowBackup="false"`).

---

## 2. Architecture Overview

```text
MainActivity (Ready Gate)
 ├── ModelPreparationScreen ──▶ ModelAssetManager (first-launch SHA-256 extract / .verified_manifest reuse)
 └── Jetpack Compose App
      ├── OnboardingFlow / DashboardScreen (Profile Manager) / Roadmap / Skills / Study Plan / Quiz
      ├── AiTutorScreen ──▶ ExecuTorchLlmEngine ──▶ NativeLlamaBridge JNI (Llama 3.2 3B INT4 mmap)
      └── LearningRepository (flatMapLatest per profileId) ──▶ Room SQLite v3 (MIGRATION_2_3)
```

- **Application ID & SDKs:** `com.learnmate.ai`, `versionCode = 1`, `versionName = "1.0.0"`, `minSdk = 26`, `targetSdk = 35`, ABI `arm64-v8a`.
- **Competition Model Note:** The Bharat AI-SoC brief reviewed by our team specifies a Meta Llama-family model deployed via ExecuTorch on ARM Android, while mentor guidance identified **Meta Llama 3 8B Instruct**. Because the estimated 8B INT4 footprint exceeds the practical memory budget of a `4 GB` Redmi 9 Power and was not deployed/evaluated on device, the shipped release uses **Meta Llama 3.2 3B Instruct INT4** (`learnmate_llama3_2_3b.pte`) and does not satisfy strict 8B parameter compliance.

---

## 3. Repository Structure

```text
LearnMate-AI/
├── android-app/               # Kotlin / Jetpack Compose Android app, Room v3, ExecuTorch JNI bridge & unit tests
├── docs/                      # Final technical report, architecture docs, demo script, submission form data
├── knowledge-base/            # Career tracks, skill taxonomy, and assessment resources
├── model/                     # Quantization/export notebooks, guides, manifests, and native runner notes
├── reports/                   # Phase 3.x evaluation outputs, final release report, and SHA-256 checksums
├── scripts/                   # Evaluation, export-notebook, and device test helper scripts
├── tests/                     # Application, integration, and model test resources
└── training/                  # Synthetic pedagogical dataset, QLoRA config, training notebook & checkers
```

*(Private signing files `*.jks` / `keystore.properties`, `local.properties`, Gradle caches, large `.pte` model weights, and `FINAL_SUBMISSION/` binaries are excluded from Git via `.gitignore`.)*

---

## 4. Setup & Verification Instructions

### Prerequisites
- JDK 17 (e.g., Eclipse Temurin 17; configure `JAVA_HOME` to JDK 17 — newer runtimes such as JDK 25 / Android Studio JBR 25 are not supported by Gradle 8.11.1)
- Android SDK (`compileSdk 35`, `minSdk 26`) and Android NDK (`r26c` if rebuilding native C++ JNI sources; prebuilt `arm64-v8a/libexecutorch_llama_jni.so` is already included in `android-app/app/src/main/jniLibs/arm64-v8a/`)

### Run Android Unit Tests (No Model Weights Required)
```powershell
cd android-app
# Ensure JAVA_HOME points to JDK 17 (e.g. Eclipse Temurin 17)
.\gradlew.bat testDebugUnitTest testReleaseUnitTest --console=plain
```
Verified baseline: `BUILD SUCCESSFUL`, **81/81** Release Android unit tests passing (`0` failures, `0` errors, `0` skipped across 18 suites).

### Build a Self-Contained Release APK From Source
1. Obtain/export the verified model artifacts matching `model/android/learnmate_model_manifest.json` (`learnmate_llama3_2_3b.pte` SHA-256 `8C545BDE51BD434BFB922402AAA33C8952871735C04DE22A9DD84F587F936B91` and `tokenizer.model` SHA-256 `82E9D31979E92AB929CD544440F129D9ECD797B69E327F80F17E1C50D5551B55`) and place them in `android-app/app/src/main/assets/models/`.
2. Create a local `android-app/keystore.properties` (never commit to Git) pointing to your release `.jks` keystore (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).
3. Run:
   ```powershell
   .\gradlew.bat assembleRelease --console=plain
   ```
   Or download the prebuilt signed APK directly from the Releases page once published.

---

## 5. Hardware Verification & Known Limitations

- **Verified Device:** Clean-install offline validation was conducted on a physical **Xiaomi Redmi 9 Power** (Snapdragon 662, 4 GB RAM, Android 12) with Wi-Fi and mobile data disabled during inference testing: first-launch SHA-256 extraction, native ExecuTorch `mmap` initialization (`~31.4–34.2 s`), 3 real offline AI Tutor inferences (`0.49–0.86 tok/s`, `6.63–8.45 s` TTFT), and cold-relaunch manifest reuse + profile state restoration.
- **Scope & Limitations:** Compatibility across all Android devices is not claimed (only Redmi 9 Power was physically tested); `seq_len=128` context limit applies; Phase 3.7 precision-isolation evaluation was stopped because the merged Hugging Face checkpoint was unavailable.
- **Model License:** Built with **Meta Llama 3.2**. Usage of `meta-llama/Llama-3.2-3B-Instruct` weights is governed by the Meta Llama 3.2 Community License Agreement and Acceptable Use Policy.
