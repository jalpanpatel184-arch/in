# Nova Assistant — Installation Guide

## Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android device or emulator running Android 12+ (API 31+)
- OpenAI account with API key

---

## Step 1 — Clone & Configure

```bash
# 1. Open project in Android Studio
# File → Open → select NovaAssistant/

# 2. Create local.properties (already in .gitignore)
cat > local.properties << 'PROPS'
sdk.dir=/Users/yourname/Library/Android/sdk
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxx
PROPS
```

---

## Step 2 — Download Vosk Model (Offline STT)

```bash
cd app/src/main/assets/
wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip vosk-model-small-en-us-0.15.zip
mv vosk-model-small-en-us-0.15 vosk-model
```

---

## Step 3 — Build & Install

```bash
# Via Gradle
./gradlew assembleDebug
./gradlew installDebug

# Or use Android Studio Run button (Shift+F10)
```

---

## Step 4 — First Launch Setup

On first run, tap through the **Permissions Screen**:

1. **Microphone** → tap "Grant" → Allow
2. **Accessibility Service** → tap "Grant" → Settings opens → find "Nova AI Assistant" → Enable
3. **Overlay Permission** → tap "Grant" → Allow

---

## Step 5 — Enable Notification Access

```
Settings → Notifications → Notification access
→ Find "Nova Assistant" → Enable
```

---

## Step 6 — Test

1. Say **"Hey Nova"** (or tap the orb)
2. Say **"What time is it?"**
3. Nova should respond with the current time

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Nova doesn't hear wake word | Check microphone permission; try tapping orb manually |
| Can't tap buttons in apps | Enable Accessibility Service (Step 4, item 2) |
| No AI response | Check `OPENAI_API_KEY` in `local.properties`; check internet |
| STT transcription empty | Ensure microphone not used by another app |
| App crashes on start | Check logcat for missing resource; ensure API 31+ |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    Nova Assistant                    │
├──────────┬──────────────┬───────────────────────────┤
│  Voice   │      AI      │       Automation          │
│  Layer   │    Layer     │         Layer             │
├──────────┼──────────────┼───────────────────────────┤
│ Vosk     │ GPT-4o       │ AccessibilityService      │
│ Whisper  │ Command      │ AppLauncher               │
│ TTS      │ Parser       │ SystemController          │
│ WakeWord │ Context Mgr  │ UIInteractor              │
│ VAD      │ Memory       │ OCREngine                 │
└──────────┴──────────────┴───────────────────────────┘
         │          │                │
         └──────────┴────────────────┘
                    │
            Room Database
          (Conversations, Memory, Routines)
```

---

## Folder Quick Reference

| Folder | Purpose |
|---|---|
| `engine/voice/` | STT, TTS, wake word |
| `engine/ai/` | GPT, command parsing, context |
| `engine/automation/` | App launching, UI tapping, system controls |
| `engine/routine/` | Macro engine |
| `service/` | Background services |
| `ui/screen/` | All Compose screens |
| `ui/components/` | Orb, waveform, subtitle components |
| `data/remote/` | OpenAI API clients |
| `data/local/` | Room database (memory, conversations, routines) |
| `domain/model/` | Data models |
| `domain/usecase/` | Business logic |
| `core/di/` | Hilt dependency injection |
