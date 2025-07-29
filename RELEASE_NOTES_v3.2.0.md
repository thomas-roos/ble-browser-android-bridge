# Release Notes - v3.2.0

## 🎉 Enhanced Configuration System & Improved Usability

This release focuses on making the BLE Browser-Android Bridge much easier to configure and use, with better defaults and comprehensive tooling.

### ✨ **New Features**

#### **Configurable Message Prefix System**
- 🔧 Easy prefix changes via `python config_helper.py --prefix "APP:"`
- 📏 Automatic message length calculations based on prefix
- 🎯 Keeps "BLE:" as default but now easily customizable
- ⚡ No more manual code editing required for prefix changes

#### **Enhanced Simple Scanner**
- 🎯 **Better Filtering**: Only shows app messages, filters out other BLE devices
- 📶 **Signal Strength Indicators**: 🟢 Strong, 🟡 Medium, 🔴 Weak signals
- 📊 **Statistics**: Shows count of app messages vs filtered messages
- 🛠️ **Command Line Options**: 
  - `--config` to show current configuration
  - `--prefix "CUSTOM:"` to temporarily use different prefix

#### **New Helper Tools**
- 🧪 **`test_config.py`**: Validates configuration and shows example messages
- 🚀 **`quick_setup.py`**: Interactive setup and testing tool
- 🔧 **Enhanced `config_helper.py`**: Easy prefix changes with validation
- 📋 **`CONFIGURATION_UPDATES.md`**: Comprehensive documentation

### 🐛 **Bug Fixes**

#### **Message Length Issues**
- ✅ Android app now starts with "Hi" instead of empty message (fits in 11-char limit)
- ✅ Better validation prevents messages that are too long
- ✅ Clear character counters and length feedback
- ✅ Improved error messages when messages exceed limits

#### **Scanner Filtering Problems**
- ✅ Simple scanner now properly filters to show only app messages
- ✅ Eliminates noise from other BLE devices
- ✅ Better message detection and parsing
- ✅ Consistent filtering across all Python tools

### 🔧 **Improvements**

#### **Android App**
- 📱 Better default message ("Hi" instead of empty)
- 💬 Clear comments showing where to change `MESSAGE_PREFIX`
- ✅ Improved length validation and user feedback
- 🎯 Better error handling for message length issues

#### **Configuration Management**
- 📝 Enhanced `config.properties` with better comments and examples
- 🔄 Automatic synchronization checking between components
- 📊 Message length examples for different prefixes
- 🛡️ Validation and error prevention

#### **Documentation**
- 📚 Comprehensive setup and usage instructions
- 🎯 Clear troubleshooting guides
- 💡 Usage examples and best practices
- 🔗 Step-by-step Android app update instructions

### 📊 **Message Length Examples**

| Prefix | User Message Length | Example Full Message |
|--------|-------------------|---------------------|
| `BLE:` | 11 chars | `BLE:Hello World` |
| `APP:` | 11 chars | `APP:Hello World` |
| `TEST:` | 10 chars | `TEST:Hello Test` |
| `A:` | 13 chars | `A:Hello Testing!` |

### 🚀 **Quick Start**

```bash
# Test current configuration
python test_config.py

# Change prefix to "APP:"
python config_helper.py --prefix "APP:"

# Test scanner (shows only app messages)
python simple_scanner.py

# Interactive setup
python quick_setup.py
```

### 🔄 **Migration Guide**

If upgrading from v3.1.0 or earlier:

1. **No breaking changes** - existing setup continues to work
2. **New tools available** - try `python test_config.py` to validate setup
3. **Enhanced scanner** - `simple_scanner.py` now filters better
4. **Easy prefix changes** - use `python config_helper.py --prefix "NEW:"`

### 🛠️ **For Developers**

#### **Android App Updates**
To change the message prefix in the Android app:
1. Open `MainActivity.kt`
2. Find: `private val MESSAGE_PREFIX = "BLE:"`
3. Change to your desired prefix
4. Rebuild: `./gradlew assembleDebug`

#### **Python Tools**
All Python scripts now automatically read from `config.properties`:
- `simple_scanner.py` - Enhanced filtering and display
- `debug_scanner.py` - Detailed debugging information
- `config_helper.py` - Interactive configuration management

### 📋 **Requirements**

- **Android**: Android 5.0+ (API 21+), Bluetooth LE support
- **Python**: Python 3.7+, `bleak` library (`pip install bleak`)
- **Browser**: Chrome 56+ or Edge 79+ with experimental features enabled

### 🐛 **Known Issues**

- Android app prefix must be manually updated after changing `config.properties`
- Web client still uses hardcoded values (will be addressed in future release)
- Very long prefixes (>12 chars) leave minimal space for user messages

### 🙏 **Contributors**

Thanks to all contributors who helped improve the configuration system and usability!

---

**Full Changelog**: https://github.com/YOUR_USERNAME/ble-browser-android-bridge/compare/v3.1.0...v3.2.0
