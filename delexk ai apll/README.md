# Delex AI Controller

A production-ready Android AI Assistant application built with Kotlin. Provides system-level automation, floating UI overlay, voice command recognition, and advanced device control via Shizuku.

## Features

- **Floating Bubble UI**: Always-on overlay interface with drag and tap controls
- **Voice Command Recognition**: Wake-word detection and natural language processing
- **System Automation**: Control Wi-Fi, Bluetooth, GPS, DPI, Brightness, and other settings via Shizuku
- **UI Automation**: Accessibility Service integration for clicking, swiping, and typing in any app
- **Screen Monitoring**: Real-time screen capture and visual pattern matching with OpenCV
- **Background Persistence**: Foreground service with persistent notification for continuous operation
- **Graceful Fallbacks**: Automatic fallback to standard Android APIs when Shizuku is unavailable

## Requirements

- Android 10 (API 29) or higher
- Shizuku Manager app installed and running (for system-level commands)
- [Optional] OpenCV for advanced visual pattern matching

## Building

### Prerequisites

1. Install Android Studio (latest version recommended)
2. Clone this repository
3. Ensure you have JDK 17 or higher

### Build Steps

```bash
# Clone the repository
git clone https://github.com/yourusername/delex-ai-controller.git
cd delex-ai-controller

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Run tests
./gradlew test
```

### APK Location

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Installation

### Via ADB

```bash
# Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.delexai.controller/.ui.MainActivity
```

### Via Android Studio

1. Open the project in Android Studio
2. Select your device or emulator
3. Click "Run" (Shift + F10)

## Setup & Permissions

The app requires several permissions to function properly:

1. **Overlay Permission** (`SYSTEM_ALERT_WINDOW`): For the floating bubble
2. **Accessibility Service**: For UI automation and gesture control
3. **Microphone** (`RECORD_AUDIO`): For voice command listening
4. **Shizuku Permission**: For system-level ADB commands
5. **Battery Optimization Bypass**: To keep the service alive

On first launch, the app will guide you to grant these permissions via Android Settings.

## Shizuku Setup

To use Shizuku for system commands:

1. Install [Shizuku Manager](https://github.com/RikkaApps/Shizuku)
2. Follow Shizuku's setup instructions (requires ADB)
3. Grant permission to "Delex AI controller" in the Shizuku app
4. The app will automatically detect when Shizuku is available

## Architecture

The app follows strict MVVM and clean architecture principles:

```
app/src/main/java/com/delexai/controller/
├── DelexAIApplication.kt          # Global app setup
├── ui/
│   └── MainActivity.kt            # Dashboard & Master Toggle
├── service/
│   ├── FloatingBubbleService.kt   # Overlay UI management
│   ├── AccessibilityActionService.kt  # UI automation
│   └── SpeechRecognitionService.kt    # Voice processing
├── executor/
│   ├── SystemCommandExecutor.kt   # Shizuku shell commands
│   └── AccessibilityExecutor.kt   # Gesture automation
├── nlp/
│   ├── IntentParser.kt            # NLP & command parsing
│   └── CommandRouter.kt           # Route commands to executors
├── manager/
│   ├── ShizukuPermissionManager.kt
│   ├── SystemPermissionManager.kt
│   └── AudioManager.kt            # Microphone lifecycle
├── screen/
│   ├── ScreenCaptureManager.kt    # MediaProjection
│   └── VisualAnalyzer.kt          # OpenCV pattern matching
└── receiver/
    └── ScreenStateReceiver.kt     # System event handling
```

## Testing

### Manual Testing

1. Launch the app
2. Toggle the Master Switch ON
3. Observe the floating bubble appear
4. Tap the bubble to activate listening
5. Speak a command (e.g., "Open WhatsApp")
6. Observe the system execute the command

### Automated Testing

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew testDebugUnitTestCoverage
```

## Debugging

### Logcat Output

```bash
# View all logs
adb logcat | grep -i "delex\|shizuku\|accessibility"

# View specific service logs
adb logcat | grep "FloatingBubbleService"
```

### Timber Logging

The app uses Timber for structured logging. All logs include:
- Service name
- Event type
- Error stack traces (if applicable)

### Debugger

1. Open Android Studio
2. Select "Debug 'app'" instead of "Run"
3. Set breakpoints in the code
4. Step through execution

## Security & Privacy

- The app requires explicit permission grants for all sensitive operations
- No data is transmitted without user consent
- All system commands are logged for audit purposes
- The app respects Android's security model and accessibility guidelines

## Troubleshooting

### Floating Bubble Not Appearing

- Ensure overlay permission is granted
- Check that `FloatingBubbleService` is running: `adb shell dumpsys activity services`
- Verify the Master Toggle is ON
- Restart the app

### Shizuku Commands Not Working

- Ensure Shizuku Manager is installed and running
- Grant "Delex AI controller" permission in Shizuku app
- Check logcat for permission errors
- On newer Android versions, system settings changes may be restricted

### Voice Recognition Not Working

- Ensure microphone permission is granted
- Check that foreground service is running
- Verify the bubble is in listening state (should be glowing)
- Test with Google Assistant to ensure microphone works

### Accessibility Service Not Enabled

- Go to Settings > Accessibility > Delex AI controller
- Toggle the service ON
- Ensure the app is not excluded from battery optimization

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

## Roadmap

- [ ] Advanced NLP with ML Kit
- [ ] Custom wake-word training
- [ ] Dark/Light theme support
- [ ] Multi-language support
- [ ] Advanced screen monitoring with real-time OCR
- [ ] Customizable command profiles
- [ ] Cloud sync for settings
- [ ] Notification center integration
