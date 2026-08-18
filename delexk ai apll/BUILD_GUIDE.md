# Delex AI Controller - Build & Deployment Guide

## Quick Start

### Prerequisites
- Android Studio 2023.1.1 or newer
- JDK 17 or newer
- Android SDK 34
- Gradle 8.2+
- Shizuku Manager app (for system commands)

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/yourusername/delex-ai-controller.git
cd delex-ai-controller

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Installation

### Via ADB (Recommended for Development)

```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.delexai.controller/.ui.MainActivity

# View logs
adb logcat -s "DelexAI" "Shizuku" "Accessibility"
```

### Via Android Studio

1. Open project in Android Studio
2. Click "Run" (Shift + F10)
3. Select target device/emulator
4. Build and install automatically

## Gradle Build Targets

### Debug Build
```bash
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
# Properties:
# - Unminified bytecode
# - Debuggable=true
# - All logging enabled
```

### Release Build
```bash
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
# Properties:
# - ProGuard obfuscation enabled
# - Debuggable=false
# - Size optimized
# - Signing config required (see below)
```

### Build with Specific Variant
```bash
./gradlew clean
./gradlew build           # All variants
./gradlew buildDebug      # Debug only
./gradlew buildRelease    # Release only
```

## Signing Configuration (Release)

For Play Store releases, create or use existing keystore:

### Generate Signing Key
```bash
keytool -genkey -v -keystore release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias delexai-release-key \
  -dname "CN=DelexAI, OU=Mobile, O=DelexAI, L=City, ST=State, C=Country"
```

### Configure gradle.properties
```properties
KEYSTORE_FILE=release-keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=delexai-release-key
KEY_PASSWORD=your_key_password
```

### Configure build.gradle.kts
```kotlin
signingConfigs {
    release {
        storeFile = file(properties["KEYSTORE_FILE"] ?: "")
        storePassword = properties["KEYSTORE_PASSWORD"].toString()
        keyAlias = properties["KEY_ALIAS"].toString()
        keyPassword = properties["KEY_PASSWORD"].toString()
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## Testing

### Unit Tests
```bash
./gradlew test                   # All tests
./gradlew testDebugUnitTest      # Debug tests only
```

### Instrumented Tests (Device Required)
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Toggle Master Switch ON → Floating bubble appears
- [ ] Tap bubble → Turns glowing blue (listening)
- [ ] Drag bubble → Position updates
- [ ] Tap bubble again → Back to inactive state
- [ ] Grant Shizuku permission → Status shows "Authorized"
- [ ] Say "Open WhatsApp" → App launches
- [ ] Say "Turn on Wi-Fi" → Wi-Fi toggles (Shizuku required)
- [ ] Toggle OFF → Bubble disappears, service stops

## Logging & Debugging

### Timber Logging Setup
```bash
# View all app logs
adb logcat | grep -i "delexai"

# View specific service
adb logcat | grep "FloatingBubbleService"

# View with timestamps
adb logcat -v time | grep -i "delexai"

# Save logs to file
adb logcat -d | tee app_logs.txt
```

### Android Debugger

1. Build in debug mode: `./gradlew assembleDebug`
2. In Android Studio: Run → Debug 'app'
3. Set breakpoints in code
4. Step through execution
5. Inspect variables in Variables panel

### Profiler

1. Select device with running app
2. Android Studio → Profiler tab
3. Monitor CPU, Memory, Network, Energy usage

## Shizuku Setup (Critical for System Commands)

### One-Time Setup

1. Install Shizuku Manager from Play Store
2. Open Shizuku app
3. Follow setup wizard (requires ADB via computer):
   ```bash
   adb shell sh /sdcard/Android/data/moe.shizuku.manager/start.sh
   ```
4. Return to Shizuku app, continue setup
5. Open Delex AI controller
6. Tap "Request Shizuku Permission"
7. Verify in Shizuku Manager → Authorized Applications → "Delex AI controller"

### Troubleshooting Shizuku

- **"Shizuku: Not Authorized"** → Open Shizuku app, grant permission explicitly
- **Commands not working** → Check logcat for SecurityException
- **Service keeps dying** → Ensure battery optimization exemption is requested
- **Reconnect Shizuku** → Restart Shizuku Manager app, then reconnect

## GitHub Actions CI/CD

### Automated Builds on Push

Configured in `.github/workflows/android.yml`:

- Triggers on push to `main` or `develop` branches
- Builds both debug and release APKs
- Runs lint checks
- Runs unit tests
- Uploads APKs as artifacts
- Creates releases on `main` branch with APK downloads

### Manual Workflow Trigger

1. Go to Actions tab in GitHub
2. Select "Android Build & Release" workflow
3. Click "Run workflow"
4. Select branch
5. Artifacts available after completion

## Distribution

### Upload to Google Play Store

1. Build signed release APK:
   ```bash
   ./gradlew assembleRelease
   ```

2. In Google Play Console:
   - Create app listing
   - Upload APK in Release section
   - Complete store listing (description, screenshots, etc.)
   - Set pricing and distribution
   - Submit for review

### Direct Distribution

1. Generate release APK (see Signing Configuration)
2. Host on server or GitHub Releases
3. Users install via ADB or APK file download

## Troubleshooting Build Issues

### Gradle Cache Issues
```bash
./gradlew clean
./gradlew build --no-build-cache
```

### Dependency Resolution
```bash
./gradlew dependencies
./gradlew app:dependencies
```

### Kotlin Compilation Errors
```bash
# Force recompilation
./gradlew clean assemble --no-build-cache

# Check Kotlin version
./gradlew -v
```

### APK Installation Fails
```bash
# Uninstall existing app
adb uninstall com.delexai.controller

# Reinstall fresh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Performance Optimization

### ProGuard Rules (release/proguard-rules.pro)
- Shizuku API, ML Kit, OpenCV all kept via -keep
- Maintains all interfaces and @Retention annotations
- Enables aggressive name obfuscation

### Memory Profiling
```bash
# Start profiler
adb shell am start --pct 30 -n com.delexai.controller/.ui.MainActivity

# Monitor memory
adb shell dumpsys meminfo com.delexai.controller
```

### Battery Optimization
- Foreground service with low-priority notification
- Screen state receiver pauses capture when off
- Audio focus management to avoid redundant listening
- Graceful teardown on service termination

## Support & Issues

### Common Issues

1. **Floating bubble not showing**
   - Check overlay permission granted
   - Verify `FloatingBubbleService` is running: `adb shell dumpsys activity services | grep FloatingBubble`
   - Restart app

2. **Voice commands not working**
   - Ensure microphone permission granted
   - Check `SpeechRecognizer.isRecognitionAvailable(context)`
   - Verify listener is in glowing (active) state

3. **Accessibility Service stops responding**
   - Re-enable in Settings > Accessibility
   - Check that app is not excluded from battery saver
   - Verify `AccessibilityActionService` is active

### Report Issues

1. Reproduce the issue
2. Collect logs: `adb logcat -d | tee logs.txt`
3. Note exact steps and expected vs actual behavior
4. Open GitHub Issue with: logs, reproduction steps, device info, Android version

## Advanced Topics

### Custom Commands via Shizuku
```kotlin
// In SystemCommandExecutor.kt, extend with custom shell commands
suspend fun executeCustom(command: String): Boolean {
    return executeShellCommand(command)
}

// Usage:
val executor = SystemCommandExecutor(context)
executor.executeCustom("settings put global setting_name value")
```

### Custom Wake Words
```kotlin
// In SpeechRecognitionEngine.kt, modify companion object:
private val WAKE_WORDS = listOf(
    "hey", "hello", "custom_word", "another_trigger"
)
```

### Custom App Routing
```kotlin
// In IntentParser.kt, extend app database:
val appDatabase = mapOf(
    "custom_app" to "com.example.customapp",
    // Add your apps here
)
```

## Contact & Support

- GitHub: [Link to repository]
- Email: support@delexai.app
- Issues: GitHub Issues tracker
- Discussions: GitHub Discussions forum
