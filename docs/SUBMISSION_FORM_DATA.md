# LearnMate AI: Competition Submission Form Data
**Competition:** Bharat AI-SoC Student Challenge  
**Google Form URL:** https://forms.gle/3KyscquPFG8rS9ASA  
**Deadline:** 25 November 2026, 11:59 PM IST  

> [!IMPORTANT]
> Submission has **not** occurred yet, and no demo video has been recorded yet. Review every field, insert your recorded video link once available, and submit the form manually.

---

### Field 1: Project Title
`LearnMate AI — Self-Contained Offline On-Device Personalized AI Learning Companion for Android`

---

### Field 2: GitHub Repository URL
`https://github.com/Sahil1719/LearnMate-AI`

*(Verify repository visibility and ensure `.gitignore` excludes `release-keystore.jks`, `keystore.properties`, `assets/models/`, and `.pte` weights before pushing.)*

---

### Field 3: Video Demonstration URL
`[PASTE YOUR RECORDED YOUTUBE OR GOOGLE DRIVE LINK HERE]`

*(No demo video has been recorded yet. Follow [DEMO_VIDEO_SCRIPT.md](DEMO_VIDEO_SCRIPT.md) when recording, and set Drive permissions to "Anyone with the link can view".)*

---

### Field 4: Technical Report Link / Reference
- Primary Architecture & Release Summary: [FINAL_TECHNICAL_REPORT.md](FINAL_TECHNICAL_REPORT.md)
- Authoritative Verification Log & Checksums: [FINAL_RELEASE_BUILD_AND_TEST_REPORT.md](../reports/FINAL_RELEASE_BUILD_AND_TEST_REPORT.md) & [final_submission_checksums.txt](../reports/final_submission_checksums.txt)

---

### Field 5: Release Deliverable & Checksums
```
Application ID: com.learnmate.ai
Version: 1.0.0 (versionCode = 1)
SDK & ABI: minSdk 26, targetSdk 35, arm64-v8a
Signed Release APK Path: android-app/app/build/outputs/apk/release/app-release.apk
APK Size: 1,956,712,238 bytes
APK SHA-256: 664409BA8E2F7B2EC4EBC0EA165691C493FD7D95EB439558E8B1DFF8C6AC2828
Bundled Model: Meta Llama 3.2 3B Instruct merged INT4 PTE (1,929,907,360 bytes, SHA-256: 8C545BDE51BD434BFB922402AAA33C8952871735C04DE22A9DD84F587F936B91)
Bundled Tokenizer: tokenizer.model (2,183,982 bytes, SHA-256: 82E9D31979E92AB929CD544440F129D9ECD797B69E327F80F17E1C50D5551B55)
```

---

### Field 6: Executive Summary / Abstract
```
LearnMate AI is a self-contained, offline-first personalized AI learning companion for Android built with Jetpack Compose, Room SQLite v3, and Meta Llama 3.2 3B Instruct (merged INT4 PTE) executing locally on-device via ExecuTorch v0.5.0 (XNNPACK NEON JNI).

Designed so evaluators and learners need no ADB commands, separate downloads, or internet access, the signed Release APK (1,956,712,238 bytes) packages the 1.93 GB INT4 PTE model and TikToken tokenizer as uncompressed APK assets. On first launch, LearnMate checks storage availability, extracts both assets once into app-private storage, verifies exact SHA-256 digests, and writes a verification manifest so cold relaunches skip re-extraction. If native inference initialization fails when the JNI library is available, LearnMate raises an explicit error instead of falling back to synthetic template replies.

Room SQLite v3 (with non-destructive MIGRATION_2_3) isolates learner profiles, roadmaps, skills, study plans, diagnostic assessments, and persistent AI Tutor conversation histories per profile. Clean-install verification was completed offline on a physical Xiaomi Redmi 9 Power (Snapdragon 662, 4 GB RAM, Android 12) with Wi-Fi and mobile data disabled, confirming SHA-256 extraction, native mmap loading (~31–34 s), three live offline AI Tutor inferences (0.49–0.86 tok/s), and cold-relaunch profile restoration without re-extracting assets. Debug and Release unit test builds succeeded, with 81/81 passing Release unit tests (0 failures, 0 errors, 0 skipped).
```

---

### Field 7: Key Technical Highlights
```
1. Self-Contained Single-APK Packaging: Bundles Meta Llama 3.2 3B INT4 PTE and tokenizer directly inside a signed 1.96 GB Release APK with one-time first-launch extraction, SHA-256 verification, and manifest-based reuse on subsequent launches.
2. Genuine On-Device ExecuTorch Inference: Executes Llama 3.2 3B INT4 natively via ARM64 JNI bindings (mmap=1, seq_len=128, threads=4) with explicit failure reporting and no silent synthetic fallback when native runtime is present.
3. Persistent Multi-Profile Room v3 Architecture: Non-destructive MIGRATION_2_3 schema scoping learner tracks, study plans, assessments, and cascade-linked AI Tutor conversation histories cleanly per profile.
4. Empirical Physical Verification: Tested from a clean install on a physical Xiaomi Redmi 9 Power with Wi-Fi/mobile data off during offline inference testing (3 live inferences, 0.49–0.86 tok/s), plus 81/81 passing Release Android unit tests across 18 suites.
```

---

### Field 8: Hardware & Software Stack
```
- Verified Physical Device: Xiaomi Redmi 9 Power (Qualcomm Snapdragon 662, 4 GB RAM, Android 12)
- Edge AI Runtime: PyTorch ExecuTorch v0.5.0 with XNNPACK NEON backend and ARM64 JNI (libexecutorch_llama_jni.so)
- Model Artifact: Meta Llama 3.2 3B Instruct merged INT4 PTE (learnmate_llama3_2_3b.pte) + TikToken tokenizer.model
- Android Stack: Kotlin, Jetpack Compose, Material3, Coroutines/Flow, minSdk 26, targetSdk 35, arm64-v8a
- Persistence: Room SQLite Database v3 (MIGRATION_2_3, android:allowBackup="false")
- Build & Verification: Gradle 8.11.1, Debug & Release unit test builds passing, 81/81 Release unit tests passed
```

---

### Field 9: Known Limitations & Disclosures
```
1. Verified Hardware Scope: Physical clean-install testing was conducted only on a Xiaomi Redmi 9 Power (4 GB RAM, Android 12); compatibility across all Android devices is not claimed.
2. On-Device Latency & Context Ceiling: On the Redmi 9 Power, native mmap loading takes ~31–34 seconds, generation runs at ~0.49–0.86 tokens/s (16.2–32.7 s total for 8–28 tokens, TTFT 6.6–8.4 s), and sequence length is bounded to seq_len=128 to fit within 4 GB RAM limits.
3. Phase 3.7 Status: Phase 3.7 precision-isolation evaluation was stopped during read-only checkpoint discovery because the merged Hugging Face checkpoint was unavailable; precision-isolation comparison was not completed.
4. Pre-Submission State: No demo video has been recorded yet and submission has not occurred yet.
```
