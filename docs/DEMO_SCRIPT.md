# Demonstration Video Script: LearnMate AI

**Target Duration:** 3–5 Minutes  
**Focus:** 100% On-device execution, zero cloud calls (Airplane Mode enabled), responsive UI, deterministic gap engine, adaptive rescheduling, and dual-model on-device LLM architecture.

---

## Hardware Targets & Demonstration Options
- **Primary Experimental Target:** Xiaomi Redmi 9 Power (Snapdragon 662, 4GB RAM, Android 12) -> Demonstrates **Meta Llama 3.2 1B Instruct (INT4 `8da4w`)**, highlighting zero-LMK stability and multitasking headroom on constrained 4GB mobile hardware.
- **Standard Reference Target:** ARM64 Android Device with >= 6GB RAM (e.g., Realme X7 5G with Dimensity 800U, 6GB RAM) -> Demonstrates **Meta Llama 3.2 3B Instruct (INT4 `8da4w`)**.
- **Historical Reference:** Transparently explain the transition from the mentor's original Meta Llama 3 8B Instruct recommendation to the evaluated dual-model strategy based on physical hardware memory boundaries.

---

## Scene Breakdown

### Scene 1: Introduction & Hardware Demonstration (0:00 - 0:45)
- **Visual:** Present test device (Xiaomi Redmi 9 Power 4GB or Realme X7 5G 6GB) showing Android Settings -> About Phone.
- **Action:** Turn on **Airplane Mode** and disable Wi-Fi/Bluetooth to prove 100% offline functionality.
- **Narrative:** Introduce LearnMate AI as a fully autonomous on-device learning companion. Explain the evaluated Dual-Model On-Device Architecture powered by PyTorch ExecuTorch v0.5.0 with XNNPACK ARM NEON acceleration (1B INT4 on 4GB hardware, 3B INT4 on 6GB+ hardware).

### Scene 2: Onboarding & Profile Definition (0:45 - 1:30)
- **Visual:** Launch LearnMate AI app. Dark-themed sleek UI.
- **Action:** Enter learner details through context-aware sequential onboarding:
  - Career Goal: *Embedded Systems Engineer*
  - Background: *Electronics Engineering*
  - Available Time: *2 hours/day*
  - Initial Skills: C (3/5), Python (2/5), Digital Electronics (4/5), Microcontrollers (1/5), RTOS (0/5).
- **Result:** Click "Analyze My Path".

### Scene 3: Deterministic Skill Analysis & Roadmap Generation (1:30 - 2:30)
- **Visual:** Skill Analysis screen opens. Show clear gap bars ($Gap = \max(0, Target - Current)$).
- **Action:** Click "Generate Learning Roadmap".
- **Visual:** On-device model activates. Display UI Diagnostics Dialog showing live memory telemetry (Total RAM, Free RAM, Active Profile, Bounded Context).
- **Result:** Roadmap populates with structured phases:
  - Phase 1: Microcontroller Fundamentals & Peripherals
  - Phase 2: FreeRTOS & Concurrency
  - Phase 3: Hardware Project Integration
- **Visual:** Study Planner shows daily study breakdown adhering strictly to the 2 hours/day constraint.

### Scene 4: Adaptive Remediation & Rescheduling (2:30 - 3:30)
- **Visual:** Navigate to Quiz section on "C Pointers & Memory Management".
- **Action:** Intentionally answer questions incorrectly (simulate learner struggling, score 40%).
- **Result:**
  - System flags low comprehension.
  - Adaptive engine automatically inserts a simplified revision module ("Visualizing Pointer Arithmetic").
  - Dependent RTOS tasks in the Study Planner are automatically postponed by 3 days.
  - Notification displayed: *"Study plan adapted to reinforce fundamental concepts."*

### Scene 5: On-Device AI Tutor & Multitasking Demonstration (3:30 - 4:30)
- **Visual:** Open AI Tutor chat.
- **Action:** Ask: *"Explain how FreeRTOS priority inversion works in simple terms."*
- **Result:** On-device model streams response in real-time. Show token generation speed and memory usage overlay.
- **Multitasking Test:** Briefly switch to WhatsApp and play local background audio, then return to LearnMate AI without application reload or process termination, demonstrating stable memory coexistence.
- **Conclusion:** Summary of deliverables, offline privacy, and competition achievements.
