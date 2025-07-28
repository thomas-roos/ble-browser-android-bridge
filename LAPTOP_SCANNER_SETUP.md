# Laptop BLE Scanner Setup Guide

This guide helps you set up BLE scanning on your laptop to debug and verify messages from the Android app.

## 📋 Quick Start

1. **Download the scanner script**: `simple_scanner.py` or `debug_scanner.py`
2. **Install Python dependencies**: `pip install bleak`
3. **Run the scanner**: `python simple_scanner.py`
4. **Start Android app** in SERVER mode and broadcast messages
5. **See messages appear** on your laptop console!

## 🖥️ Platform-Specific Setup

### Windows 10/11

```bash
# 1. Install Python (if not already installed)
# Download from: https://python.org

# 2. Install bleak library
pip install bleak

# 3. Run the scanner
python simple_scanner.py
```

**Requirements:**
- Windows 10 version 1903+ (for BLE support)
- Built-in Bluetooth adapter or USB Bluetooth dongle

### macOS

```bash
# 1. Install Python (if not already installed)
# Use Homebrew: brew install python
# Or download from: https://python.org

# 2. Install bleak library
pip3 install bleak

# 3. Run the scanner
python3 simple_scanner.py
```

**Requirements:**
- macOS 10.15+ (Catalina or newer)
- Built-in Bluetooth or USB Bluetooth adapter

### Linux (Ubuntu/Debian)

```bash
# 1. Install Python and Bluetooth libraries
sudo apt update
sudo apt install python3 python3-pip bluetooth bluez

# 2. Install bleak library
pip3 install bleak

# 3. Run the scanner (may need sudo for Bluetooth access)
sudo python3 simple_scanner.py
```

**Requirements:**
- BlueZ Bluetooth stack
- Bluetooth adapter with BLE support

### Raspberry Pi

```bash
# 1. Update system
sudo apt update && sudo apt upgrade

# 2. Install dependencies
sudo apt install python3-pip bluetooth bluez

# 3. Install bleak
pip3 install bleak

# 4. Enable Bluetooth
sudo systemctl enable bluetooth
sudo systemctl start bluetooth

# 5. Run scanner
sudo python3 simple_scanner.py
```

## 🔧 Troubleshooting

### Common Issues

#### "No module named 'bleak'"
```bash
# Solution: Install bleak
pip install bleak
# or
pip3 install bleak
```

#### "Permission denied" (Linux)
```bash
# Solution: Run with sudo
sudo python3 simple_scanner.py

# Or add user to bluetooth group
sudo usermod -a -G bluetooth $USER
# Then logout and login again
```

#### "Bluetooth adapter not found"
- **Check Bluetooth is enabled**: Look for Bluetooth icon in system tray
- **Install drivers**: Make sure Bluetooth adapter drivers are installed
- **USB Bluetooth**: Try a USB Bluetooth 4.0+ adapter if built-in doesn't work

#### "No advertisements detected"
- **Check Android app**: Make sure it's in SERVER mode and broadcasting
- **Check distance**: Move Android device closer to laptop
- **Check permissions**: Android app needs all Bluetooth permissions granted
- **Restart Bluetooth**: Turn Bluetooth off and on again on both devices

### Testing Bluetooth

#### Windows
```cmd
# Check Bluetooth status
Get-PnpDevice -Class Bluetooth
```

#### macOS
```bash
# Check Bluetooth status
system_profiler SPBluetoothDataType
```

#### Linux
```bash
# Check Bluetooth status
bluetoothctl show

# Scan for devices (general test)
bluetoothctl scan on
```

## 📱 Android App Setup

1. **Install latest APK** (v3.0.0+) from GitHub releases
2. **Grant all permissions** when prompted
3. **Switch to SERVER mode** (📡 SERVER button)
4. **Start broadcasting** (tap "Start Broadcasting")
5. **Set a message** (type message and tap "Update Message")

## 🔍 Expected Output

When working correctly, you should see:

```
🔍 BLE Scanner - Listening for Android app messages...
📡 Service UUID: 12345678-1234-1234-1234-123456789abc
🛑 Press Ctrl+C to stop

[14:32:15] 📱 Galaxy S21: "Hello World!" (RSSI: -45)
[14:32:18] 📱 Galaxy S21: "Test message" (RSSI: -43)
[14:32:22] 📱 Galaxy S21: "Debug works!" (RSSI: -47)
```

## 🚀 Advanced Usage

### Debug Scanner (Detailed Output)

Use `debug_scanner.py` for more detailed information:

```bash
python debug_scanner.py
```

Output includes:
- Device MAC addresses
- Signal strength indicators
- Message statistics
- Error details
- Troubleshooting tips

### Custom Filtering

Modify the scanner to filter specific devices:

```python
def callback(device, ad_data):
    # Only show messages from specific device
    if device.name == "My Android Phone":
        # ... process message
```

## 💡 Tips

- **Signal Strength**: RSSI values closer to 0 are stronger (-30 is better than -70)
- **Message Limit**: Android app limits messages to 18 characters for BLE advertisements
- **Update Rate**: Messages update in real-time when changed in Android app
- **Multiple Devices**: Scanner can receive from multiple Android devices simultaneously
- **Battery**: Continuous scanning uses laptop battery - plug in for long sessions

## 🔗 Related Files

- `simple_scanner.py` - Basic scanner for quick testing
- `debug_scanner.py` - Detailed scanner with statistics and debugging info
- `PASSIVE_SCANNING.md` - Technical details about BLE advertisement scanning
