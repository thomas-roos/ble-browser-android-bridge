# Laptop to Android Testing Guide

## 🎯 Goal
Send BLE messages from your laptop and receive them on the Android app (reverse of normal setup).

## 📋 Setup Overview

```
Laptop (Broadcaster) ──BLE──> Android App (Client/Scanner)
```

## 🔧 Step-by-Step Instructions

### **Step 1: Prepare Android App**

1. **Open the Android app** on your device
2. **Switch to CLIENT mode**:
   - Tap the "📱 CLIENT" button
   - You should see: "Status: 🔴 CLIENT - Not Scanning"
3. **Start scanning**:
   - Tap "Start Scanning"
   - You should see: "Status: 📱 CLIENT - Scanning for 'BLE:' messages"
   - App will show: "🔍 CLIENT: Scanning for BLE advertisements..."

### **Step 2: Test Laptop Broadcasting**

#### **Option A: Simple Broadcaster (Recommended)**

```bash
# Check configuration
python simple_broadcaster.py --config

# Test with a simple message
sudo python3 simple_broadcaster.py "Hi Android"

# Interactive mode for multiple messages
sudo python3 simple_broadcaster.py --interactive
```

#### **Option B: Advanced Broadcaster (Linux only)**

```bash
# Install additional dependencies if needed
pip install dbus-python PyGObject

# Test broadcasting
sudo python3 laptop_broadcaster.py "Hello from laptop"

# Interactive mode
sudo python3 laptop_broadcaster.py --interactive
```

### **Step 3: Verify Communication**

#### **On Android App (CLIENT mode):**
You should see messages like:
```
[14:32:15] 📡 CLIENT: Received "Hi Android"
   └─ From: Laptop-BLE (RSSI: -45dBm)
```

#### **On Laptop:**
You should see:
```
[14:32:15] ✅ Started advertising: 'BLE:Hi Android'
📱 Your Android app in CLIENT mode should now see this message!
```

## 🛠️ Troubleshooting

### **Android App Issues**

#### **"No messages received"**
1. ✅ Verify app is in CLIENT mode (📱 CLIENT button pressed)
2. ✅ Verify scanning is active ("Start Scanning" pressed)
3. ✅ Check app shows "Scanning for 'BLE:' messages"
4. ✅ Try moving devices closer (within 10 meters)

#### **"Wrong mode"**
- Make sure you're in CLIENT mode, not SERVER mode
- CLIENT mode = receives messages
- SERVER mode = sends messages

### **Laptop Broadcasting Issues**

#### **"Permission denied" or "Bluetooth error"**
```bash
# Try with sudo
sudo python3 simple_broadcaster.py "Test message"

# Check Bluetooth is enabled
bluetoothctl show
```

#### **"bluetoothctl not found"**
```bash
# Install BlueZ on Ubuntu/Debian
sudo apt update
sudo apt install bluetooth bluez

# Install on other Linux distros
# Fedora: sudo dnf install bluez
# Arch: sudo pacman -S bluez
```

#### **"No Bluetooth adapter"**
- Check if Bluetooth hardware is available
- Try: `lsusb | grep -i bluetooth`
- Try: `hciconfig -a`

### **Message Length Issues**

#### **"Message too long"**
- Max user message: 11 characters (with "BLE:" prefix)
- Try shorter messages: "Hi", "Test", "Hello"
- Check current limits: `python test_config.py`

### **Distance/Signal Issues**

#### **"Weak signal" or "No detection"**
- Move devices closer (within 5-10 meters)
- Remove obstacles between devices
- Try different locations (away from WiFi routers)

## 🧪 Testing Scenarios

### **Basic Test**
```bash
# Laptop
sudo python3 simple_broadcaster.py "Hi"

# Android should show:
# [TIME] 📡 CLIENT: Received "Hi"
```

### **Interactive Test**
```bash
# Laptop
sudo python3 simple_broadcaster.py --interactive

# Then type various messages:
# "Test1"
# "Hello"
# "Working"
# "quit"
```

### **Prefix Test**
```bash
# Change prefix temporarily
python config_helper.py --prefix "TEST:"

# Update Android app MainActivity.kt:
# private val MESSAGE_PREFIX = "TEST:"

# Test with new prefix
sudo python3 simple_broadcaster.py "Message"
```

## 📊 Expected Results

### **Successful Communication**
- **Laptop**: Shows "✅ Started advertising"
- **Android**: Shows "📡 CLIENT: Received [message]"
- **Signal strength**: Typically -30 to -70 dBm

### **Message Format**
- **Laptop sends**: `BLE:Hi` (full message with prefix)
- **Android receives**: `Hi` (user message without prefix)
- **Android displays**: "📡 CLIENT: Received 'Hi'"

## 🔄 Switching Back to Normal Mode

When done testing laptop-to-Android:

1. **Stop laptop broadcaster**: Press Ctrl+C
2. **Switch Android to SERVER mode**: Tap "📡 SERVER" button
3. **Resume normal testing**: Android broadcasts, laptop scans

## 💡 Tips

### **For Better Results**
- Use short messages (under 10 characters)
- Keep devices close during initial testing
- Test in areas with less BLE interference
- Use `simple_broadcaster.py` for more reliable results

### **For Development**
- Check configuration: `python test_config.py`
- Monitor with debug scanner: `python debug_scanner.py`
- Use interactive mode for rapid testing

### **Common Commands**
```bash
# Quick test
sudo python3 simple_broadcaster.py "Test"

# Check config
python simple_broadcaster.py --config

# Interactive mode
sudo python3 simple_broadcaster.py -i

# Stop advertising
# Press Ctrl+C in broadcaster terminal
```

## 🎉 Success Indicators

✅ **Working correctly when:**
- Android app shows received messages in CLIENT mode
- Laptop shows "Started advertising" without errors
- Messages appear with reasonable signal strength (-30 to -70 dBm)
- No "permission denied" or Bluetooth errors

This setup lets you test the full bidirectional communication of your BLE bridge system!
