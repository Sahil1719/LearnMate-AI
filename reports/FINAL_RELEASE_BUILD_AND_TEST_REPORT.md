# LearnMate AI — Final Release Build & Device Verification Report

**Project:** LearnMate AI (`com.learnmate.ai`)  
**Competition:** Bharat AI-SoC Student Challenge  
**Date:** 2026-11-25 (IST)  
**Target Physical Hardware:** Xiaomi Redmi 9 Power (Qualcomm Snapdragon 662, 4 GB RAM, Android 12, ADB serial `<DEVICE_SERIAL>`)  
**Primary On-Device Model:** Meta Llama 3.2 3B Instruct INT4 (`8da4w`) via ExecuTorch v0.5.0 (`XNNPACK NEON`, `mmap=1`, `seq_len=128`, `threads=4`)

---

## 1. Executive Summary

LearnMate AI has been packaged, signed, unit-tested, and verified on a clean physical-device install as a self-contained **1.96 GB** Release APK (`app-release.apk`) requiring **no ADB, no manual file pushing, no separate model download, and no internet connection**.

- **Bundled Native Assets:** Uncompressed `assets/models/learnmate_llama3_2_3b.pte` (`1,929,907,360` bytes) and `assets/models/tokenizer.model` (`2,183,982` bytes), extracted once on first launch by `ModelAssetManager.kt`, verified byte-for-byte against embedded SHA-256 checksums, and cached in `context.filesDir/models/`.
- **Authentic On-Device Inference Guarantee:** Removed silent fallback to `synthesizeDeterministicResponse()` in `ExecuTorchLlmEngine.kt` when `NativeLlamaBridge.isAvailable()` is `true` and `nativeHandle == 0L`, instead emitting `InferenceState.Error` and throwing explicitly.
- **Persistent Multi-Profile Architecture (Room v3):** Upgraded `LearnMateDatabase` from v2 to v3 with explicit non-destructive `MIGRATION_2_3`, profile scoping (`profileId`) across `SkillEntity`, `RoadmapPhaseEntity`, `StudyTaskEntity`, and `AssessmentRecordEntity`, plus cascade-linked `ConversationEntity` and `ChatMessageEntity` tables for durable per-profile AI Tutor chat history.
- **Clean Fresh-Install Experience:** Disabled Android cloud/auto backup (`android:allowBackup="false"`), gated main navigation on `ModelPreparationScreen`, and verified clean Welcome / Onboarding start on fresh installation.

---

## 2. Final Artifact Inventory & Checksums

Recorded in [final_submission_checksums.txt](final_submission_checksums.txt):

| Artifact | Path | Size (Bytes) | SHA-256 |
|---|---|---:|---|
| Signed Release APK | `android-app/app/build/outputs/apk/release/app-release.apk` | `1,956,712,238` | `664409BA8E2F7B2EC4EBC0EA165691C493FD7D95EB439558E8B1DFF8C6AC2828` |
| Merged INT4 Model | `model/android/learnmate_llama3_2_3b.pte` | `1,929,907,360` | `8C545BDE51BD434BFB922402AAA33C8952871735C04DE22A9DD84F587F936B91` |
| TikToken Tokenizer | `model/android/tokenizer.model` | `2,183,982` | `82E9D31979E92AB929CD544440F129D9ECD797B69E327F80F17E1C50D5551B55` |

### APK & Build Configuration Verified

- `applicationId`: `com.learnmate.ai`
- `versionCode`: `1`, `versionName`: `"1.0.0"`
- `minSdk`: `26`, `targetSdk`: `35`, `compileSdk`: `35`, ABI filter `arm64-v8a`
- Uncompressed inside APK (`compress_type=0`): `lib/arm64-v8a/libexecutorch_llama_jni.so` (`13,255,408` bytes), `assets/models/learnmate_llama3_2_3b.pte`, and `assets/models/tokenizer.model`.
- Signed with release keystore (`apksigner` v2 verified).
- `.gitignore` excludes `*.jks`, `*.keystore`, `keystore.properties`, `android-app/app/src/main/assets/models/`, and `.pte` weights from Git source control; verified via `git status --short`.

---

## 3. Architectural Changes Delivered for Release

### 3.1 Self-Contained Extraction & Integrity Verification
- **`domain/engine/ModelAssetManager.kt`**: Checks `StatFs` for at least `2.03 GB` free storage before extracting `models/tokenizer.model` and `models/learnmate_llama3_2_3b.pte` to `context.filesDir/models/`, computes streaming SHA-256 digests and exact file lengths, writes `.verified_manifest` so subsequent launches bypass extraction, and initializes `ExecuTorchLlmEngine`.
- **`ui/screens/ModelPreparationScreen.kt`**: Displays extraction progress bar, megabytes copied, SHA-256 verification status, native runtime initialization progress, and actionable retry states on storage, checksum, or init failures.
- **`ExecuTorchLlmEngine.kt`**: Prioritizes `context.filesDir/models/` in `findModelFile()` and `findTokenizerFile()`, and prevents silent fallback to synthetic templates if native initialization failed.

### 3.2 Multi-Profile & Persistent AI Tutor History (Room v3)
- **`data/local/LearnMateDatabase.kt`**: Upgraded to `version = 3` with explicit `MIGRATION_2_3` matching KSP-generated schema and removed `.fallbackToDestructiveMigration()`.
- **Per-Profile Scoping**: Added `profileId` columns and composite keys on `SkillEntity`, `RoadmapPhaseEntity`, `StudyTaskEntity`, and `AssessmentRecordEntity`, along with updated DAOs and `LearningRepository` (`SharedPreferences` active profile ID + `flatMapLatest` reactive streams).
- **Persistent Conversations**: Added `ConversationEntity`, `ChatMessageEntity` (cascade foreign key + index), and `ConversationDao`.
- **UI**: Updated `DashboardScreen.kt` (Profile Manager dialog for Switch, Create New, Edit, Delete), `OnboardingFlow.kt` (`editingProfile` / `isCreatingNewProfile`), `AiTutorScreen.kt` (conversation history dialog with timestamps, New Chat, Reopen, Delete, per-profile scoping, explicit native error banner), and `MainActivity.kt` (`ModelPreparationState.Ready` gate).

---

## 4. Automated Unit Test Verification

Executed with Android Studio JBR (`JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`) after final edits:

```powershell
.\gradlew.bat testDebugUnitTest testReleaseUnitTest --rerun-tasks --console=plain
```

- Result: **`BUILD SUCCESSFUL in 1m 55s`** (`51 actionable tasks: 51 executed`)
- Release test suite breakdown (`app/build/test-results/testReleaseUnitTest/*.xml`): **81 tests run, 0 failures, 0 errors, 0 skipped** across 18 test suites:
  - `AdaptiveRemediationTest`: 1/1
  - `AdaptiveRemediationWorkflowTest`: 3/3
  - `AllCareerPathwaysSkillGapTest`: 5/5
  - `ContextAwareOnboardingScenariosTest`: 13/13
  - `DatabaseEntitiesPersistenceTest`: 7/7
  - `InferenceProfileManagerTest`: 7/7
  - `LlmInferenceEngineTest`: 24/24
  - `ModelSelectionManagerTest`: 5/5
  - `OfflineQuizBankTest`: 5/5
  - `ScenarioA_EngineeringStudentTest` through `ScenarioH_PersistenceAndResetTest`: 10/10
  - `SkillGapEngineTest`: 1/1

---

## 5. Physical Device Clean-Install & Offline Runtime Verification

Verified on **Xiaomi Redmi 9 Power** (`<DEVICE_SERIAL>`, Android 12, 4 GB RAM) after full uninstall (`adb uninstall com.learnmate.ai` and removal of `/storage/emulated/0/Android/data/com.learnmate.ai`) and direct on-device APK installation via system package installer:

1. **First-Launch Self-Contained Extraction & Native Init**
   - `20:07:07.433`: `LearnMate_NativeLlama: JNI_OnLoad: Initialized JavaVM pointer`
   - `20:07:41.910`: `ModelAssetManager: Verified extracted model & tokenizer SHA-256 in /data/user/0/com.learnmate.ai/files/models`
   - `20:07:41.923`: `LearnMate_NativeLlama: nativeInit: model=/data/user/0/com.learnmate.ai/files/models/learnmate_llama3_2_3b.pte, tokenizer=/data/user/0/com.learnmate.ai/files/models/tokenizer.model, mmap=1, seq_len=128, threads=4`
   - `20:08:16.131`: `LearnMate_NativeLlama: nativeInit: Dedicated TikToken loaded successfully, vocab_size=128256`
   - `20:08:16.140`: `ExecuTorchLlmEngine: Native model loaded in 34222ms via mmap (Redmi Low-Memory Profile (4GB RAM), handle=1001)`
   - UI verified via `uiautomator dump` cleanly on `"Personalized On-Device AI Learning"` / `"Get Started"` screen with zero pre-seeded/sample profiles.

2. **Offline Multi-Profile Interaction & Real On-Device AI Tutor Inference**
   - Disabled radios via ADB (`settings get global wifi_on -> 0`, `mobile_data -> 0`).
   - User completed interactive verification across profiles (UI dump verified active Dashboard profile `"Hello, Tanushree"` — `Clinical Physician / Doctor Readiness` / `Medicine & Surgery (MBBS / Clinical Practice)`), including **3 completed on-device Llama 3.2 3B inferences** with zero fallbacks or crashes:
     - Run 1 (`20:12:59`–`20:13:32`): `promptTokens=42`, `tokensEmitted=28`, `totalMs=32685`, `ttftMs=6630`, `tokPerSec=0.8566621`, `handle=1001`, `<|eot_id|>` suppressed cleanly.
     - Run 2 (`20:16:54`–`20:17:10`): `promptTokens=49`, `tokensEmitted=8`, `totalMs=16195`, `ttftMs=8447`, `tokPerSec=0.49397963`, `handle=1001`.
     - Run 3 (`20:17:39`–`20:18:00`): `promptTokens=61`, `tokensEmitted=14`, `totalMs=21241`, `ttftMs=8305`, `tokPerSec=0.6591027`, `handle=1001`.

3. **Cold Relaunch Persistence & Extraction Skip**
   - Executed `adb shell am force-stop com.learnmate.ai` while offline and relaunched `.MainActivity`:
   - `20:19:42.050`: `ModelAssetManager: Persisted model & tokenizer already verified on disk (/data/user/0/com.learnmate.ai/files/models)` — instant `.verified_manifest` validation bypassed re-copying the 1.93 GB PTE.
   - `20:20:13.441`: `ExecuTorchLlmEngine: Native model loaded in 31379ms via mmap (Redmi Low-Memory Profile (4GB RAM), handle=1001)`.
   - UI dump after cold launch confirmed active profile `"Hello, Tanushree"` restored cleanly.
   - Cleaned up temporary `/sdcard/Download/LearnMate-AI-release.apk` and `/sdcard/ui.xml` and restored Wi-Fi (`svc wifi enable`).

---

## 6. Submission Notes & Historical Doc Caveat

1. **Pre-Release Docs Note**: Existing draft documents ([FINAL_TECHNICAL_REPORT.md](../docs/FINAL_TECHNICAL_REPORT.md), [DEMO_VIDEO_SCRIPT.md](../docs/DEMO_VIDEO_SCRIPT.md), and [SUBMISSION_FORM_DATA.md](../docs/SUBMISSION_FORM_DATA.md)) reflect earlier development milestones (e.g., dual-model 1B/3B tables, Room v2, 62 unit tests, synthetic fallback framing) and were left untouched per documentation-integrity rules. Use the verified metrics in Sections 2–5 of this report for final submission text and video narration.
2. **2-Minute Demonstration Video Plan (Updated for Final Release)**:
   - **0:00–0:20**: Show Redmi 9 Power in **Airplane Mode / Wi-Fi OFF**; highlight clean fresh install + `ModelPreparationScreen` one-time SHA-256 asset extraction & offline native `mmap` load (~31–34 s).
   - **0:20–0:45**: Walk through adaptive onboarding to create **Profile 1**, show personalized Roadmap, Skills, Planner, and Adaptive Quiz/Remediation.
   - **0:45–1:20**: Open **AI Tutor**, run a live offline prompt (`Llama 3.2 3B INT4` streaming at ~0.5–0.9 tok/s via ExecuTorch JNI), close and reopen the app to show persisted conversation history + instant cached launch.
   - **1:20–1:45**: Open Dashboard **Profile Manager**, switch to/create **Profile 2** to demonstrate isolated curricula & separate AI Tutor chat histories.
   - **1:45–2:00**: Show `app-release.apk` SHA-256 verification and closing Bharat AI-SoC challenge summary card.
