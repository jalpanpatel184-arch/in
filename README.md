# 🤖 Nova AI Assistant — Production Android Project

> Jarvis-level Android AI voice assistant built with Kotlin + Jetpack Compose

---

## 📁 Project Structure

```
NovaAssistant/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/nova/assistant/
│   │   │   ├── NovaApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   │
│   │   │   ├── core/
│   │   │   │   ├── di/                          # Hilt DI modules
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   └── ServiceModule.kt
│   │   │   │   ├── utils/
│   │   │   │   │   ├── PermissionManager.kt
│   │   │   │   │   ├── Extensions.kt
│   │   │   │   │   └── Constants.kt
│   │   │   │   └── base/
│   │   │   │       └── BaseViewModel.kt
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── database/
│   │   │   │   │   │   ├── NovaDatabase.kt
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── ConversationDao.kt
│   │   │   │   │   │   │   ├── MemoryDao.kt
│   │   │   │   │   │   │   └── RoutineDao.kt
│   │   │   │   │   │   └── entities/
│   │   │   │   │   │       ├── ConversationEntity.kt
│   │   │   │   │   │       ├── MemoryEntity.kt
│   │   │   │   │   │       └── RoutineEntity.kt
│   │   │   │   │   └── preferences/
│   │   │   │   │       └── NovaPreferences.kt
│   │   │   │   ├── remote/
│   │   │   │   │   ├── api/
│   │   │   │   │   │   ├── OpenAIApi.kt
│   │   │   │   │   │   └── WhisperApi.kt
│   │   │   │   │   └── models/
│   │   │   │   │       ├── ChatRequest.kt
│   │   │   │   │       ├── ChatResponse.kt
│   │   │   │   │       └── WhisperResponse.kt
│   │   │   │   └── repository/
│   │   │   │       ├── ConversationRepository.kt
│   │   │   │       ├── MemoryRepository.kt
│   │   │   │       └── AIRepository.kt
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── Command.kt
│   │   │   │   │   ├── NovaResponse.kt
│   │   │   │   │   ├── Memory.kt
│   │   │   │   │   └── Routine.kt
│   │   │   │   ├── usecase/
│   │   │   │   │   ├── ProcessCommandUseCase.kt
│   │   │   │   │   ├── ExecuteAutomationUseCase.kt
│   │   │   │   │   ├── ManageMemoryUseCase.kt
│   │   │   │   │   └── SpeakResponseUseCase.kt
│   │   │   │   └── repository/ (interfaces)
│   │   │   │       ├── IConversationRepository.kt
│   │   │   │       └── IAIRepository.kt
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── NovaForegroundService.kt      # Main background service
│   │   │   │   ├── NovaAccessibilityService.kt   # UI automation
│   │   │   │   ├── WakeWordService.kt             # "Hey Nova" detection
│   │   │   │   └── FloatingBubbleService.kt       # Overlay bubble
│   │   │   │
│   │   │   ├── engine/
│   │   │   │   ├── voice/
│   │   │   │   │   ├── VoiceEngine.kt             # STT coordinator
│   │   │   │   │   ├── WhisperEngine.kt           # Online STT
│   │   │   │   │   ├── VoskEngine.kt              # Offline STT
│   │   │   │   │   └── TTSEngine.kt               # Text-to-speech
│   │   │   │   ├── ai/
│   │   │   │   │   ├── AIEngine.kt                # AI coordinator
│   │   │   │   │   ├── CommandParser.kt           # Intent extraction
│   │   │   │   │   └── ContextManager.kt          # Conversation memory
│   │   │   │   ├── automation/
│   │   │   │   │   ├── AutomationEngine.kt        # Central coordinator
│   │   │   │   │   ├── AppLauncher.kt
│   │   │   │   │   ├── UIInteractor.kt            # Tap/scroll/type
│   │   │   │   │   ├── SystemController.kt        # Brightness/WiFi/etc
│   │   │   │   │   ├── NotificationReader.kt
│   │   │   │   │   └── OCREngine.kt               # Screen vision
│   │   │   │   └── routine/
│   │   │   │       ├── RoutineEngine.kt
│   │   │   │       └── MacroExecutor.kt
│   │   │   │
│   │   │   └── ui/
│   │   │       ├── theme/
│   │   │       │   ├── Theme.kt
│   │   │       │   ├── Color.kt
│   │   │       │   └── Type.kt
│   │   │       ├── components/
│   │   │       │   ├── NovaOrb.kt                 # Animated orb
│   │   │       │   ├── SubtitleOverlay.kt
│   │   │       │   ├── SuggestionChips.kt
│   │   │       │   └── WaveformVisualizer.kt
│   │   │       ├── screen/
│   │   │       │   ├── HomeScreen.kt
│   │   │       │   ├── PermissionsScreen.kt
│   │   │       │   ├── SettingsScreen.kt
│   │   │       │   ├── MemoryScreen.kt
│   │   │       │   └── RoutineScreen.kt
│   │   │       └── viewmodel/
│   │   │           ├── HomeViewModel.kt
│   │   │           └── SettingsViewModel.kt
│   │   │
│   │   └── res/
│   │       ├── values/
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       └── xml/
│   │           └── accessibility_service_config.xml
│   │
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 Setup & Installation

### Prerequisites
- Android Studio Hedgehog or newer
- Android 12+ device or emulator (API 31+)
- OpenAI API key

### 1. Clone & Open
```bash
git clone <repo>
cd NovaAssistant
# Open in Android Studio
```

### 2. Configure API Key
In `local.properties`:
```properties
OPENAI_API_KEY=sk-your-key-here
```

### 3. Grant Permissions (Runtime)
The app will guide you through:
- Microphone
- Accessibility Service (for UI automation)
- Overlay (floating bubble)
- Notification access
- Device admin (optional, for lock/unlock)

### 4. Build & Run
```bash
./gradlew assembleDebug
```

---

## 🎙️ Example Voice Commands

| Command | What Nova Does |
|---|---|
| "Hey Nova, open Instagram and send 'hello' to Rahul" | Opens app → Messages → Search → Chat → Type → Send |
| "Hey Nova, turn on Wi-Fi" | Toggles Wi-Fi via system settings |
| "Hey Nova, set brightness to 50%" | Adjusts screen brightness |
| "Hey Nova, read my notifications" | Reads all pending notifications aloud |
| "Hey Nova, send a WhatsApp message to Mom saying I'll be late" | Opens WhatsApp → contacts → types → sends |
| "Hey Nova, play Spotify and skip to next" | Launches Spotify, sends media key |
| "Hey Nova, set an alarm for 7 AM tomorrow" | Creates alarm via Clock app or AlarmManager |
| "Hey Nova, what's on my screen?" | OCR + AI describes current screen content |
| "Hey Nova, remember that my meeting is on Friday at 3pm" | Stores in memory database |
| "Hey Nova, run my morning routine" | Executes saved macro sequence |

---

## 🔑 API Integration

### OpenAI (GPT-4o + Whisper)
- Chat completions: `https://api.openai.com/v1/chat/completions`
- Whisper STT: `https://api.openai.com/v1/audio/transcriptions`
- Model: `gpt-4o` for commands, `whisper-1` for transcription

### Offline Mode
- Vosk for offline STT (model downloaded on first run)
- Android built-in TTS for offline speech output
- Local GPT.cpp / Gemma Nano (Android 14+) for offline AI

---

## ⚡ Architecture

```
Voice Input → WakeWordService → VoiceEngine (Whisper/Vosk)
     ↓
Transcription → AIEngine (GPT-4o) → CommandParser (Intent)
     ↓
AutomationEngine → [AppLauncher | UIInteractor | SystemController | OCREngine]
     ↓
Result → TTSEngine → Voice Output + UI Update
```
