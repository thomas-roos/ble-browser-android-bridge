# BLE Browser-Android Bridge

A Bluetooth Low Energy (BLE) communication bridge between web browsers and Android devices. This project enables direct browser-to-Android communication using Web Bluetooth API and Android BLE peripheral mode.

## 🚀 Quick Start

### For Android App
1. Download the latest APK from [Releases](../../releases)
2. Install on your Android device
3. Grant Bluetooth permissions when prompted
4. Tap "Start BLE Server" to begin advertising

### For Web Browser
1. Visit the test page: [https://YOUR_USERNAME.github.io/ble-browser-android-bridge](https://YOUR_USERNAME.github.io/ble-browser-android-bridge)
2. Enable experimental features in Chrome: `chrome://flags/#enable-experimental-web-platform-features`
3. Click "Connect to Android" and select your device
4. Start messaging!

## 📱 Features

- **Real-time Communication**: Direct browser-to-Android messaging via BLE
- **Cross-platform**: Works with any Web Bluetooth compatible browser
- **No Internet Required**: Direct device-to-device communication
- **Easy Installation**: Automated APK builds via GitHub Actions
- **Live Demo**: Hosted test page on GitHub Pages

## 🛠️ Development

### Android App
```bash
cd android-app
./gradlew assembleDebug
```

### Web Client
The web client is a static HTML page that can be served locally:
```bash
cd web-client
python -m http.server 8000
# Visit http://localhost:8000
```

## 🔧 Technical Details

- **Android**: Kotlin, BLE Peripheral mode, GATT Server
- **Web**: Vanilla JavaScript, Web Bluetooth API
- **Communication**: Custom GATT service with read/write characteristics
- **Range**: ~10-30 meters (typical BLE range)

## 📋 Requirements

### Android
- Android 5.0+ (API 21+)
- Bluetooth LE support
- Location permission (required for BLE advertising)

### Browser
- Chrome 56+ or Edge 79+
- Experimental Web Platform features enabled
- HTTPS connection (or localhost for development)

## 🚀 CI/CD

This project includes automated workflows:
- **Android APK Build**: Automatic APK generation on releases
- **GitHub Pages**: Auto-deployment of web client
- **Release Management**: Tagged releases with downloadable APKs

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 🐛 Troubleshooting

### Common Issues
- **"Device not found"**: Ensure Android app is running and advertising
- **"Connection failed"**: Check Bluetooth is enabled on both devices
- **"Permission denied"**: Grant location permission on Android
- **"Web Bluetooth not available"**: Enable experimental features in browser

### Debug Tips
- Check browser console for detailed error messages
- Use Android Studio logcat for Android app debugging
- Ensure devices are within BLE range (~10-30m)
