# BLE Configuration Management

This document explains how to configure and customize the BLE Browser-Android Bridge, especially the message prefix used for filtering.

## Overview

The project uses a shared configuration system that allows you to customize:
- **Message prefix** for filtering app-specific messages
- **BLE service UUIDs** for device identification
- **Message length limits** based on BLE constraints
- **Debug and logging settings**

## Configuration Files

### `config.properties`
Main configuration file with all settings:
```properties
# Message prefix to identify app-specific BLE advertisements
MESSAGE_PREFIX=BLE:

# BLE Service UUID used for GATT communication
SERVICE_UUID=12345678-1234-1234-1234-123456789abc

# Maximum message length constraints
MAX_TOTAL_MESSAGE_LENGTH=15
```

### `ble_config.py`
Python configuration reader that all Python scripts use to load settings from `config.properties`.

## Changing the Message Prefix

The message prefix is used to filter app-specific messages from other BLE advertisements. Here's how to change it:

### Method 1: Using the Configuration Helper (Recommended)

```bash
# View current configuration
python config_helper.py

# Change prefix to "APP:"
python config_helper.py --prefix "APP:"

# Change prefix to "TEST:"
python config_helper.py --prefix "TEST:"

# Reset to defaults
python config_helper.py --reset

# Check if Android app is in sync
python config_helper.py --check
```

### Method 2: Manual Configuration

1. **Edit `config.properties`:**
   ```properties
   MESSAGE_PREFIX=YOUR_PREFIX:
   ```

2. **Update Android app:**
   - Open `android-app/app/src/main/java/com/github/blebrowserbridge/MainActivity.kt`
   - Find: `private val MESSAGE_PREFIX = "BLE:"`
   - Change to: `private val MESSAGE_PREFIX = "YOUR_PREFIX:"`
   - Rebuild and reinstall the Android app

3. **Python scripts automatically use the new prefix**

## Message Length Constraints

BLE advertisements have strict size limits (~31 bytes total). The configuration system automatically calculates available space:

- **Total message length**: 15 bytes (after BLE overhead)
- **User message length**: Total length - Prefix length
- **Example**: With "BLE:" prefix (4 chars), users can send 11-character messages

### Prefix Length Guidelines

| Prefix | User Message Length | Example |
|--------|-------------------|---------|
| `BLE:` | 11 chars | `BLE:Hello World` |
| `APP:` | 11 chars | `APP:Hello World` |
| `TEST:` | 10 chars | `TEST:Hello Test` |
| `A:` | 13 chars | `A:Hello Testing!` |

## Component Synchronization

### Python Scripts (Automatic)
All Python scripts automatically read from `config.properties`:
- `simple_scanner.py`
- `debug_scanner.py`
- `debug_all_scanner.py`

### Android App (Manual)
The Android app requires manual updates:
1. Change `MESSAGE_PREFIX` in `MainActivity.kt`
2. Rebuild the app
3. Install on device

### Web Client
Currently uses hardcoded values. Future versions will read from configuration.

## Testing Configuration Changes

1. **Change the prefix:**
   ```bash
   python config_helper.py --prefix "TEST:"
   ```

2. **Update Android app:**
   - Modify `MainActivity.kt`
   - Rebuild and install

3. **Test with Python scanner:**
   ```bash
   python debug_scanner.py
   ```

4. **Verify filtering:**
   - Android app messages should show as "APP MESSAGE"
   - Other BLE devices should show as "FILTERED"

## Troubleshooting

### Python Scripts Not Using New Prefix
- Check if `config.properties` exists and is readable
- Verify `ble_config.py` is in the same directory
- Run `python config_helper.py` to see current settings

### Android App Not Filtering Correctly
- Verify `MESSAGE_PREFIX` in `MainActivity.kt` matches `config.properties`
- Rebuild and reinstall the Android app
- Check app logs for prefix-related messages

### Messages Too Long
- Check current prefix length: `python config_helper.py`
- Use shorter prefix or shorter messages
- Remember: Total message = Prefix + User message ≤ 15 chars

## Advanced Configuration

### Custom Service UUIDs
Edit `config.properties`:
```properties
SERVICE_UUID=your-custom-uuid-here
CHARACTERISTIC_UUID=your-custom-char-uuid
```

### Debug Settings
```properties
DEBUG_MODE=true
LOG_TIMESTAMPS=true
SHOW_RSSI_DETAILS=true
```

### BLE Settings
```properties
SCAN_MODE=LOW_LATENCY
ADVERTISE_MODE=LOW_LATENCY
TX_POWER_LEVEL=HIGH
```

## Configuration Validation

Use the helper script to validate your configuration:

```bash
# Show all current settings
python config_helper.py

# Check Android app synchronization
python config_helper.py --check

# Test with a specific prefix
python config_helper.py --prefix "DEMO:"
```

## Best Practices

1. **Keep prefixes short** to maximize user message length
2. **Use descriptive prefixes** for different testing scenarios
3. **Always update Android app** after changing prefix
4. **Test with Python scanners** before deploying
5. **Document custom configurations** for team members

## Examples

### Development Setup
```bash
# Set development prefix
python config_helper.py --prefix "DEV:"

# Update Android app with DEV: prefix
# Test with debug scanner
python debug_scanner.py
```

### Production Setup
```bash
# Set production prefix
python config_helper.py --prefix "PROD:"

# Update Android app with PROD: prefix
# Deploy to devices
```

### Testing Multiple Configurations
```bash
# Test different prefixes
python config_helper.py --prefix "TEST1:"
# ... test ...

python config_helper.py --prefix "TEST2:"
# ... test ...

# Reset to defaults
python config_helper.py --reset
```
