# Delex AI Controller - Developer API Reference

## Core APIs & Usage Patterns

### 🎤 Speech Recognition API

#### SpeechRecognitionEngine
```kotlin
val speechEngine = SpeechRecognitionEngine(context)

// Initialize
speechEngine.initialize()

// Set up callbacks
speechEngine.setOnWakeWordDetected { wakeWord ->
    Log.d("TAG", "Wake word detected: $wakeWord")
    // Activate floating bubble
}

speechEngine.setOnCommandRecognized { command ->
    Log.d("TAG", "Command: $command")
    // Route to CommandRouter
}

speechEngine.setOnError { errorCode, errorMessage ->
    Log.e("TAG", "Recognition error: $errorMessage")
}

// Start listening (continuous, auto-restart)
speechEngine.startListening()

// Stop listening
speechEngine.stopListening()

// Check dismissal word
val isDismissal = speechEngine.isDismissalWord("go away")

// Cleanup
speechEngine.destroy()
```

**Callback Events**:
- `onWakeWordDetected(String)` - Triggered when "hey", "hello", etc. detected
- `onCommandRecognized(String)` - Triggered on actual command speech
- `onError(Int, String)` - Triggered on recognition errors

**Auto-Restart**: Engine automatically restarts listening after each recognition or error

---

### 🗣️ Natural Language Processing

#### IntentParser
```kotlin
val parser = IntentParser(context)

// Parse a command
val intent = parser.parseCommand("Open WhatsApp and send hello")
// Returns: ParsedIntent(
//   action = "launch_app",
//   target = "com.whatsapp",
//   parameters = { "app_name" -> "whatsapp" },
//   confidence = 0.95f
// )

// Resolve app package name
val packageName = parser.resolveAppPackage("instagram")
// Returns: "com.instagram.android" (or null)

// Launch an app directly
val success = parser.launchApp("com.whatsapp")
// Uses PackageManager.getLaunchIntentForPackage()

// Parse result types
when (intent.action) {
    "launch_app" -> intent.target  // package name
    "toggle_setting" -> {
        val setting = intent.target  // "wifi", "bluetooth", etc.
        val enable = intent.parameters["enable"]?.toBoolean()
    }
    "ui_interaction" -> intent.target  // element to click
    "type_text" -> intent.target  // text to type
    "navigate" -> {
        val location = intent.target
        val app = intent.parameters["app"] ?: "maps"
    }
}

// Confidence scoring
if (intent.confidence > 0.8) {
    // High confidence, execute immediately
} else {
    // Lower confidence, may want user confirmation
}
```

**Recognized Commands**:
- Launch: "Open [app]", "Launch [app]"
- Settings: "Turn [on/off] [wifi|bluetooth|gps|nfc|brightness|auto-rotate|flashlight]"
- UI: "Click [button]", "Tap [element]"
- Text: "Type [text]", "Send [message]", "Say [message]"
- Navigate: "Navigate to [location]", "Go to [location]"

---

### ⚙️ System Command Execution

#### SystemCommandExecutor
```kotlin
val executor = SystemCommandExecutor(context)

// Register Shizuku state listener (optional but recommended)
executor.registerShizukuStateListener()

// WiFi control
launch {
    val success = executor.toggleWiFi(enable = true)
    if (!success) Log.w("TAG", "Shizuku may be unavailable")
}

// Bluetooth control
launch {
    executor.toggleBluetooth(enable = false)
}

// GPS/Location services
launch {
    executor.toggleGPS(enable = true)
}

// NFC control
launch {
    executor.toggleNFC(enable = true)
}

// Screen brightness (0-255)
launch {
    executor.setBrightness(brightness = 128)
}

// Auto-rotate screen
launch {
    executor.toggleAutoRotate(enable = true)
}

// Flashlight/torch
launch {
    executor.toggleFlashlight(enable = true)
}

// Generic shell command execution
launch {
    val success = executor.executeShellCommand("settings put global setting_name value")
}
```

**Error Handling**:
- All methods return `Boolean` - `true` if executed, `false` if failed
- Methods are `suspend` - call from coroutine scope
- Automatically handles SecurityException, DeadObjectException
- Falls back to standard Android APIs if Shizuku unavailable

**Supported Commands** (via Shizuku shell):
- `svc wifi enable/disable` - Wi-Fi toggle
- `svc bluetooth enable/disable` - Bluetooth toggle
- `svc nfc enable/disable` - NFC toggle
- `settings put system screen_brightness <value>` - Brightness
- `settings put system accelerometer_rotation <0|1>` - Auto-rotate
- `settings put secure location_providers_allowed <providers>` - GPS

---

### 🖱️ UI Automation & Accessibility

#### AccessibilityActionService (Direct Use)
```kotlin
// Find nodes by text
val nodes = accessibilityService.findNodesByText("Send")
if (nodes.isNotEmpty()) {
    val sendButton = nodes[0]
    accessibilityService.clickNode(sendButton)
}

// Find node by resource ID
val inputField = accessibilityService.findNodeById("com.whatsapp:id/input")
inputField?.let {
    // Interact with input field
}

// Type text into focused field
accessibilityService.typeText("Hello world")

// Gesture swipes (for TikTok, Instagram, etc.)
accessibilityService.swipeUp()      // Scroll up or navigate forward
accessibilityService.swipeDown()    // Scroll down or navigate back
accessibilityService.swipeLeft()    // Navigate left
accessibilityService.swipeRight()   // Navigate right

// Click at absolute coordinates
accessibilityService.clickAtCoordinates(x = 500f, y = 1000f)

// System gestures
accessibilityService.pressBack()    // Back button
accessibilityService.pressHome()    // Home button
accessibilityService.openRecents()  // Recent apps
```

#### AccessibilityExecutor (Convenience Wrapper)
```kotlin
val executor = AccessibilityExecutor()
executor.setAccessibilityService(accessibilityService)

// All above methods available, with null-safety checks
val node = executor.findNodeByText("Send")
executor.clickNode(node)
executor.typeText("Message")
executor.swipeUp()
executor.clickAtCoordinates(x, y)
executor.pressBack()
```

**Dynamic Node Discovery** - No hardcoded IDs or coordinates needed!

---

### 🎯 Command Routing & Orchestration

#### CommandRouter
```kotlin
val router = CommandRouter(
    context,
    systemCommandExecutor,
    accessibilityExecutor
)

// Route a voice command
router.routeCommand("Open WhatsApp and send hello")

// Internally:
// 1. IntentParser parses command
// 2. CommandRouter matches intent.action
// 3. Dispatches to appropriate executor
// 4. Handles errors gracefully
```

**Command Routing**:
```
launch_app → IntentParser.launchApp(packageName)
toggle_setting → SystemCommandExecutor.toggleXxx()
ui_interaction → AccessibilityExecutor.findNodeByText() + clickNode()
type_text → AccessibilityExecutor.typeText()
navigate → Google Maps intent
```

---

### 🔊 Audio Focus Management

#### AudioFocusManager
```kotlin
val audioManager = AudioFocusManager(context)

// Request audio focus for microphone
val focusGranted = audioManager.requestAudioFocus()
if (!focusGranted) {
    Log.w("TAG", "Audio focus denied - another app has audio")
}

// Register callbacks for focus changes
audioManager.setOnAudioFocusLost {
    // Another app started playing audio or call came in
    Log.d("TAG", "Audio focus lost, pausing listening")
}

audioManager.setOnAudioFocusGained {
    // Other app released audio
    Log.d("TAG", "Audio focus regained, resuming listening")
}

// Mute/unmute microphone
audioManager.mute()
audioManager.unmute()

// Check mute status
if (audioManager.isMicrophoneMuted()) {
    // Unmute to enable listening
}

// Adjust voice call volume
audioManager.setVoiceCallVolume(volume = 7)

// Get current volume
val currentVolume = audioManager.getVoiceCallVolume()

// Abandon audio focus when done
audioManager.abandonAudioFocus()
```

**Audio Focus Levels**:
- `AUDIOFOCUS_GAIN` - Full focus granted
- `AUDIOFOCUS_LOSS_TRANSIENT_MAY_DUCK` - Another app has audio, can run at lower volume
- `AUDIOFOCUS_LOSS_TRANSIENT` - Temporary loss (call, notification)
- `AUDIOFOCUS_LOSS` - Permanent loss, must release

---

### 📹 Screen Capture & Visual Analysis

#### ScreenCaptureManager
```kotlin
val screenCapture = ScreenCaptureManager(context)

// Initialize with MediaProjection from activity result
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == 42 && resultCode == Activity.RESULT_OK) {
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data!!)
        screenCapture.initialize(mediaProjection)
    }
}

// Set frame callback
screenCapture.setOnFrameAvailable { bitmap ->
    // Process frame
    processFrame(bitmap)
}

// Start capturing frames
if (screenCapture.startCapture()) {
    Log.d("TAG", "Screen capture started")
}

// Stop capturing
screenCapture.stopCapture()

// Check if capturing
if (screenCapture.isCapturing()) {
    Log.d("TAG", "Currently capturing frames")
}

// Cleanup
screenCapture.destroy()
```

#### VisualAnalyzer
```kotlin
val analyzer = VisualAnalyzer()

// Detect color clusters (e.g., blue ball on screen)
val matches = analyzer.detectColorClusters(
    bitmap = frame,
    targetColor = 0x2196F3,  // Blue color
    tolerance = 20           // Color distance tolerance
)

for (match in matches) {
    Log.d("TAG", "Found blue object at (${match.x}, ${match.y}) " +
                 "with radius ${match.radius} and confidence ${match.confidence}")
    
    // Click the detected object
    if (match.confidence > 0.7) {
        accessibilityExecutor.clickAtCoordinates(
            match.x.toFloat(),
            match.y.toFloat()
        )
    }
}

// Detect motion between frames
val motionPoints = analyzer.detectMotion(
    previousFrame = lastBitmap,
    currentFrame = currentBitmap,
    threshold = 20
)

if (motionPoints.isNotEmpty()) {
    Log.d("TAG", "Detected ${motionPoints.size} pixels with motion")
}

// Check if point is within circular region
val isInZone = analyzer.isPointInCircle(
    x = clickX, y = clickY,
    centerX = match.x, centerY = match.y,
    radius = match.radius
)

if (isInZone) {
    Log.d("TAG", "Point is within target region")
}

// Cleanup
analyzer.destroy()
```

**Color Detection**:
- `targetColor` format: `0xRRGGBB` (e.g., `0xFF0000` for red)
- Returns list of `ColorMatch` objects with x, y, confidence, radius
- Automatic flood-fill clustering of adjacent matching pixels

---

### 🔐 Permission Management

#### ShizukuPermissionManager
```kotlin
val shizukuManager = ShizukuPermissionManager(context)

// Check current permission status
if (shizukuManager.isShizukuPermissionGranted()) {
    Log.d("TAG", "Shizuku is authorized")
} else {
    // Request permission (shows Shizuku prompt)
    shizukuManager.requestShizukuPermission()
}

// Register listeners for state changes
shizukuManager.registerShizukuListeners(
    onBinderReceived = {
        Log.d("TAG", "Shizuku service is now available")
        // Resume system commands
    },
    onBinderDead = {
        Log.d("TAG", "Shizuku service disconnected")
        // Fall back to standard APIs
    }
)

// Check if Shizuku is installed and available
if (shizukuManager.isShizukuAvailable()) {
    Log.d("TAG", "Shizuku Manager is installed")
}
```

#### SystemPermissionManager
```kotlin
val permissionManager = SystemPermissionManager(context)

// Check overlay permission
if (!permissionManager.canDrawOverlays()) {
    // Open system settings to grant overlay
    permissionManager.openDisplaySettings()
}

// Request battery optimization bypass
permissionManager.requestBatteryOptimizationBypass()

// Open accessibility settings
permissionManager.openAccessibilitySettings()

// Open sound settings
permissionManager.openSoundSettings()
```

---

### 🎛️ Floating Bubble UI

#### FloatingBubbleService
```kotlin
// Start service (from MainActivity)
val intent = Intent(context, FloatingBubbleService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(intent)
} else {
    context.startService(intent)
}

// Stop service
context.stopService(Intent(context, FloatingBubbleService::class.java))

// Check if service is running
if (FloatingBubbleService.isServiceRunning) {
    Log.d("TAG", "Bubble service is active")
}
```

**Bubble Interactions** (automatic):
- Tap bubble → Toggles listening state (isListening boolean)
- Drag bubble → Tracks position updates
- Automatic visual feedback → Blue (inactive) or Glow (active)

---

### 🎛️ Main Dashboard

#### MainActivity
```kotlin
// Master Toggle behavior
binding.masterToggle.setOnCheckedChangeListener { _, isChecked ->
    if (isChecked) {
        startFloatingBubbleService()
    } else {
        stopFloatingBubbleService()
    }
}

// Check & display Shizuku status
val hasShizukuPermission = shizukuPermissionManager.isShizukuPermissionGranted()
binding.shizukuStatusText.text = if (hasShizukuPermission) {
    "Shizuku: Authorized"
} else {
    "Shizuku: Not Authorized"
}

// Battery optimization button
binding.batteryOptimizationButton.setOnClickListener {
    systemPermissionManager.requestBatteryOptimizationBypass()
}
```

---

## 🔄 Complete Example: Voice → Execution Flow

```kotlin
// Step 1: Voice Recognition
val speechEngine = SpeechRecognitionEngine(context)
speechEngine.setOnCommandRecognized { command ->
    Log.d("TAG", "Recognized: $command")
    
    // Step 2: Parse Command
    val intent = IntentParser(context).parseCommand(command)
    
    // Step 3: Route Command
    val router = CommandRouter(context, executor, accessor)
    router.routeCommand(command)
    
    // Internally handles:
    // - launch_app: IntentParser.launchApp(packageName)
    // - toggle_setting: SystemCommandExecutor.toggleXxx()
    // - ui_interaction: AccessibilityExecutor.clickNode()
    // - type_text: AccessibilityExecutor.typeText()
    // - navigate: Intent(ACTION_VIEW, geo:...)
}

speechEngine.startListening()
```

**Voice Command Examples**:
- "Open WhatsApp" → `router.routeCommand()` → `IntentParser.launchApp("com.whatsapp")`
- "Turn on Wi-Fi" → `router.routeCommand()` → `SystemCommandExecutor.toggleWiFi(true)`
- "Click send button" → `router.routeCommand()` → `AccessibilityExecutor.findNodeByText("send")` → `clickNode()`
- "Navigate to restaurant" → `router.routeCommand()` → `Intent(ACTION_VIEW, geo:...)`

---

## 📊 Lifecycle Management

### Service Lifecycle
```
MainActivity.masterToggle ON
    ↓
FloatingBubbleService.onCreate()
    ├─ createNotification() - Persistent notification
    ├─ createBubbleUI() - Add overlay view
    └─ START_STICKY - Auto-restart if killed

MainActivity.masterToggle OFF
    ↓
FloatingBubbleService.onDestroy()
    └─ windowManager.removeView() - Remove overlay
    └─ stopListening() - Stop audio
```

### Permission Lifecycle
```
App Start → ShizukuPermissionManager.checkPermissionStatus()
              ├─ Not granted → systemPermissionManager.requestShizukuPermission()
              │   └─ Opens Shizuku Manager for user auth
              └─ Granted → Ready for system commands
```

---

**API Level**: Android 10+ (minSdkVersion 29)
**Language**: Kotlin with Coroutines
**Logging**: Timber with automatic context
**Thread Safety**: Main thread for UI, Default thread for background ops
