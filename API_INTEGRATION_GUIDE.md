# Nova Assistant — API Integration Guide

## 1. OpenAI Setup

### Get API Key
1. Go to https://platform.openai.com/api-keys
2. Click "Create new secret key"
3. Copy key to `local.properties`:
   ```
   OPENAI_API_KEY=sk-proj-xxxxxxxxxxxx
   ```

### Models Used
| Feature | Model | Cost |
|---|---|---|
| Chat / Commands | `gpt-4o` | ~$5/1M tokens |
| Speech-to-Text | `whisper-1` | $0.006/min |

### Whisper STT (Online)
```kotlin
// Already implemented in WhisperEngine.kt
// Sends 16-bit PCM as WAV to:
// POST https://api.openai.com/v1/audio/transcriptions
// Returns: { "text": "your transcription" }
```

---

## 2. Vosk Offline STT Setup

### Download Model
```bash
# Download small English model (~50MB)
wget https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip vosk-model-small-en-us-0.15.zip -d app/src/main/assets/vosk-model/
```

### Add Vosk AAR
1. Download `vosk-android-demo-0.3.50.aar` from:
   https://github.com/alphacep/vosk-android-demo/releases
2. Place in `app/libs/`
3. Add to `app/build.gradle.kts`:
   ```kotlin
   implementation(files("libs/vosk-android-demo-0.3.50.aar"))
   ```

### Full Vosk Integration (replace stub in VoskEngine.kt)
```kotlin
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService

// In VoskEngine class:
private var model: Model? = null
private var recognizer: Recognizer? = null

fun init(context: Context, onReady: () -> Unit) {
    StorageService.unpack(context, "vosk-model", "model",
        { m -> model = m; recognizer = Recognizer(m, 16000.0f); onReady() },
        { ex -> Log.e("Vosk", "Init failed", ex) }
    )
}

suspend fun transcribe(audioData: ShortArray): String {
    val r = recognizer ?: return ""
    val bytes = ByteArray(audioData.size * 2)
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audioData)
    r.acceptWaveForm(bytes, bytes.size)
    val result = JSONObject(r.finalResult)
    return result.optString("text", "")
}
```

---

## 3. Wake Word — Porcupine Setup (Recommended)

### Get Picovoice Key
1. Sign up at https://console.picovoice.ai/
2. Create a new project
3. Generate "Hey Nova" keyword file (`.ppn`)

### Add Dependency
```kotlin
// app/build.gradle.kts
implementation("ai.picovoice:porcupine-android:3.0.1")
```

### Replace WakeWordDetector stub
```kotlin
// In WakeWordDetector.kt, replace listenForWakeWord():
val porcupine = Porcupine.Builder()
    .setAccessKey("YOUR_PICOVOICE_ACCESS_KEY")
    .setKeywordPath("hey-nova_android.ppn")  // place in assets/
    .setSensitivity(0.7f)
    .build(context)

val audioRecord = AudioRecord(
    MediaRecorder.AudioSource.VOICE_RECOGNITION,
    porcupine.sampleRate,
    AudioFormat.CHANNEL_IN_MONO,
    AudioFormat.ENCODING_PCM_16BIT,
    512
)
audioRecord.startRecording()

while (isRunning) {
    val buffer = ShortArray(porcupine.frameLength)
    audioRecord.read(buffer, 0, buffer.size)
    if (porcupine.process(buffer) >= 0) {
        withContext(Dispatchers.Main) { onDetected(true) }
    }
}
audioRecord.stop()
porcupine.delete()
```

---

## 4. Neural TTS (Piper / Android)

### Option A: Android Neural TTS (No setup)
- Already used in `VoiceEngine.kt`
- Works on Android 10+ with Google TTS engine
- Set via: `tts.voice = voices.firstOrNull { it.name.contains("en-us") }`

### Option B: Piper TTS (Better quality, offline)
1. Download Piper binary + voice model from:
   https://github.com/rhasspy/piper/releases
2. Package model in `assets/piper/`
3. Integrate via JNI or subprocess:
```kotlin
// Run Piper process
val process = ProcessBuilder(
    "${filesDir}/piper/piper",
    "--model", "${filesDir}/piper/en_US-amy-medium.onnx",
    "--output_file", "${cacheDir}/tts_output.wav"
).start()
process.outputStream.write(text.toByteArray())
process.outputStream.close()
process.waitFor()
// Play WAV file
```

---

## 5. Accessibility Service Setup

1. Build and install the app
2. Go to **Settings → Accessibility → Downloaded Apps → Nova AI Assistant**
3. Enable "Nova AI Assistant"
4. Confirm the permission dialog

### Test Accessibility
```kotlin
// Test in Activity:
val svc = NovaAccessibilityService.instance
svc?.tapByText("OK")  // Taps any button with text "OK"
svc?.typeText("Hello, World!")  // Types in focused field
```

---

## 6. Notification Listener Setup

1. Go to **Settings → Notifications → Notification access**
2. Find "Nova Assistant" and enable it
3. Or trigger from app: `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`

---

## 7. Testing Voice Commands

### Example Prompts to Test
```
"Hey Nova, what time is it?"
"Hey Nova, open WhatsApp"
"Hey Nova, send a WhatsApp message to John saying I'll be there at 5"
"Hey Nova, turn on flashlight"
"Hey Nova, set brightness to 80 percent"
"Hey Nova, set alarm for 7 AM tomorrow"
"Hey Nova, play Spotify"
"Hey Nova, read my notifications"
"Hey Nova, remember that my password hint is my dog's name"
"Hey Nova, what did I tell you to remember about passwords?"
"Hey Nova, open Instagram and search for travel photos"
"Hey Nova, volume up"
"Hey Nova, turn off WiFi"
"Hey Nova, run my morning routine"
```

---

## 8. GPT-4o System Prompt Customization

Edit the `SYSTEM_PROMPT` in `AIEngine.kt`:
```kotlin
private val SYSTEM_PROMPT = """
You are Nova, an AI assistant for [USER_NAME].
Personality: [friendly/professional/casual]
...
""".trimIndent()
```

---

## 9. Adding Custom Commands

In `AutomationEngine.kt`, add a new `CommandType` case:
```kotlin
CommandType.MY_CUSTOM -> handleMyCustomCommand(cmd)

private fun handleMyCustomCommand(cmd: Command): Boolean {
    // Your automation logic here
    return true
}
```

Then add it to GPT's system prompt so it knows to emit that action type.
