# LearnMate AI: Official Demonstration Video Script (Plan)
**Competition:** Bharat AI-SoC Student Challenge  
**Status:** Script / Plan Only — **No demo video has been recorded yet.**  
**Target Physical Device:** Xiaomi Redmi 9 Power (Snapdragon 662, 4 GB RAM, Android 12)  
**Target Package:** `com.learnmate.ai` (`1.0.0`, self-contained Release APK `app-release.apk`)  
**Target Duration:** ~2 minutes (120 seconds), 1920×1080 MP4

---

## Shot-by-Shot Plan (Aligned With Verified Release Behavior)

```
====================================================================================================
TIMECODE        SCREEN / ACTION                                   NARRATION POINTS
====================================================================================================
00:00 - 00:25   [FRESH INSTALL & OFFLINE ASSET EXTRACTION]        "LearnMate AI is an offline-first,
                Show Redmi 9 Power with Wi-Fi and Mobile Data     on-device AI learning companion built
                turned OFF in the status shade. Launch fresh      for Android using Meta Llama 3.2 3B
                installed app showing ModelPreparationScreen      Instruct INT4 and ExecuTorch v0.5.0.
                copying bundled weights from APK and verifying    The 1.96 GB APK bundles the model
                SHA-256 hashes (note: native mmap load takes      directly—no manual file copy, no ADB,
                ~34 s on Redmi 9 Power; speed up or cut cleanly   and zero internet needed. First launch
                while showing real load completion).              verifies SHA-256 integrity once."

----------------------------------------------------------------------------------------------------
00:25 - 00:45   [ADAPTIVE ONBOARDING & DASHBOARD]                 "From a clean install, adaptive
                Tap Get Started. Complete onboarding to create    onboarding builds a personalized
                Profile 1 (e.g., Clinical / Medicine track or     learning roadmap, skill baseline, and
                your chosen track). Show Dashboard, Roadmap,      daily study plan stored locally in
                and Study Plan views.                             Room SQLite v3."

----------------------------------------------------------------------------------------------------
00:45 - 01:25   [OFFLINE ON-DEVICE AI TUTOR]                      "Let's ask the AI Tutor a question
                Open AI Tutor tab while offline. Send a concise   completely offline. Responses stream
                question. Wait for genuine on-device Llama 3.2    directly from the local ExecuTorch
                3B INT4 streaming output (~6.6–8.4 s TTFT,        native runner—no cloud calls and no
                ~0.5–0.86 tok/s on Snapdragon 662). Show reply.   silent template fallbacks."

----------------------------------------------------------------------------------------------------
01:25 - 01:45   [COLD RELAUNCH REUSE & MULTI-PROFILE ISOLATION]   "Closing and relaunching offline skips
                Swipe app from Recents and reopen offline:        model re-extraction using the verified
                confirm extraction is skipped and Profile 1 +     manifest, and restores the saved
                chat history persist. Open Dashboard Profile      profile and AI Tutor conversation.
                Manager to switch or create Profile 2 showing     Separate learner profiles maintain
                isolated profile data and chat histories.         isolated progress and chat histories."

----------------------------------------------------------------------------------------------------
01:45 - 02:00   [RELEASE VERIFICATION & CLOSING]                  "Validated on the physical Redmi 9
                Show final artifact card:                         Power with 81/81 passing Release
                - Package: com.learnmate.ai v1.0.0                unit tests and self-contained offline
                - Model: Llama 3.2 3B INT4 (ExecuTorch v0.5.0)    execution. Thank you."
                - APK SHA-256: 664409BA8E2F7B2EC4EBC0EA165691C493FD7D95EB439558E8B1DFF8C6AC2828
                - 81/81 Release Android Unit Tests Passed
====================================================================================================
```

---

## Recording Notes for Presenter

1. **Offline Proof:** Pull down Android quick settings at the beginning to show Wi-Fi and Mobile Data are **OFF**.
2. **Realistic Pacing:** On the Redmi 9 Power (4 GB RAM), native `mmap` model initialization takes ~31–34 seconds and token generation runs at ~0.49–0.86 tokens/s. You may keep a short wait visible to demonstrate authentic local execution and use a clear time-lapse/cut label if condensing wait times to fit 2 minutes.
3. **Stick to Verified Claims:** Mention only verified metrics (`1,956,712,238` bytes APK, Llama 3.2 3B INT4 PTE, Room v3, `81/81` Release unit tests, tested on Xiaomi Redmi 9 Power). Do not claim compatibility across all Android devices or cite pre-release metrics.
