# Development Guide

## 🏗️ Project Structure

```
ble-browser-android-bridge/
├── android-app/                 # Android BLE server application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/github/blebrowserbridge/
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
├── web-client/                  # Web Bluetooth client
│   └── index.html
├── .github/workflows/           # CI/CD pipelines
│   ├── android-build.yml        # Android APK build
│   └── pages.yml               # GitHub Pages deployment
└── README.md
```

## 🔧 Development Setup

### Prerequisites
- **Android Development**: Android Studio, JDK 17+
- **Web Development**: Any modern browser with Web Bluetooth support
- **CI/CD**: GitHub account with Actions enabled

### Android App Development

1. **Open in Android Studio**:
   ```bash
   cd android-app
   # Open this directory in Android Studio
   ```

2. **Build and Run**:
   ```bash
   ./gradlew assembleDebug
   # Or use Android Studio's build/run buttons
   ```

3. **Key Components**:
   - `MainActivity.kt`: Main BLE server logic
   - `AndroidManifest.xml`: Permissions and app configuration
   - `activity_main.xml`: UI layout

### Web Client Development

1. **Local Testing**:
   ```bash
   cd web-client
   python -m http.server 8000
   # Visit http://localhost:8000
   ```

2. **HTTPS Requirement**:
   - Web Bluetooth requires HTTPS
   - Use ngrok for local HTTPS testing:
   ```bash
   ngrok http 8000
   ```

## 🚀 Deployment

### Automatic Deployment

1. **Android APK**: 
   - Push tags starting with `v` (e.g., `v1.0.0`)
   - GitHub Actions builds and releases APK automatically

2. **Web Client**:
   - Push changes to `main` branch
   - GitHub Pages deploys automatically

### Manual Deployment

1. **Build Android APK**:
   ```bash
   cd android-app
   ./gradlew assembleRelease
   # APK location: app/build/outputs/apk/release/
   ```

2. **Deploy Web Client**:
   - Copy `web-client/index.html` to any web server
   - Ensure HTTPS is enabled

## 🧪 Testing

### Android App Testing

1. **Permissions**: Ensure all Bluetooth permissions are granted
2. **BLE Support**: Test on devices with BLE capability
3. **Advertising**: Verify BLE advertising starts successfully

### Web Client Testing

1. **Browser Compatibility**: Test on Chrome/Edge with experimental features
2. **Device Discovery**: Verify Android device appears in scan results
3. **Communication**: Test bidirectional messaging

### Integration Testing

1. Start Android app and enable BLE server
2. Open web client in Chrome
3. Enable experimental Web Platform features
4. Connect and test messaging

## 🔍 Debugging

### Android Debugging

```bash
# View logs
adb logcat | grep BLEBridge

# Check BLE advertising
adb shell dumpsys bluetooth_manager
```

### Web Debugging

```javascript
// Browser console debugging
navigator.bluetooth.getAvailability().then(available => {
    console.log('Bluetooth available:', available);
});

// Check experimental features
console.log('Web Bluetooth supported:', 'bluetooth' in navigator);
```

## 📝 Common Issues

### Android Issues

1. **Advertising Fails**: 
   - Check permissions (Location + Bluetooth)
   - Verify BLE hardware support
   - Ensure Bluetooth is enabled

2. **Connection Drops**:
   - Check power management settings
   - Verify app stays in foreground

### Web Issues

1. **"Web Bluetooth not available"**:
   - Enable experimental features: `chrome://flags/#enable-experimental-web-platform-features`
   - Use HTTPS or localhost

2. **Device Not Found**:
   - Ensure Android app is advertising
   - Check device proximity (BLE range ~10-30m)
   - Verify service UUID matches

## 🔄 CI/CD Pipeline

### Android Build Pipeline
- Triggers: Push to main/develop, tags, PRs
- Actions: Build debug/release APKs, upload artifacts
- Release: Auto-create GitHub release for version tags

### Web Deployment Pipeline
- Triggers: Push to main, changes in web-client/
- Actions: Deploy to GitHub Pages
- Result: Live web client at username.github.io/repo-name

## 🛠️ Customization

### Changing UUIDs
Update these UUIDs in both Android and web client:
```kotlin
// Android: MainActivity.kt
val SERVICE_UUID: UUID = UUID.fromString("your-service-uuid")
val CHAR_UUID: UUID = UUID.fromString("your-characteristic-uuid")
```

```javascript
// Web: index.html
const SERVICE_UUID = 'your-service-uuid';
const CHAR_UUID = 'your-characteristic-uuid';
```

### Adding Features
- **File Transfer**: Implement chunked data transfer
- **Multiple Clients**: Support multiple browser connections
- **Encryption**: Add custom encryption layer
- **Notifications**: Use BLE notifications for real-time updates

## 📚 Resources

- [Web Bluetooth API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Bluetooth_API)
- [Android BLE Guide](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Pages Guide](https://docs.github.com/en/pages)
