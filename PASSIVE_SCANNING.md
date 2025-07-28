# Passive BLE Advertisement Scanning

## The Problem

**Web browsers cannot passively scan BLE advertisements** due to security and privacy restrictions. The Web Bluetooth API requires user interaction and explicit device selection.

This is intentional - browsers prevent websites from silently scanning for nearby devices to protect user privacy.

## Solutions for True Passive Scanning

### 1. Native Android App

Create an Android app that uses `BluetoothLeScanner.startScan()`:

```kotlin
// Example Android code for passive BLE scanning
private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

private val scanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val device = result.device
        val rssi = result.rssi
        val scanRecord = result.scanRecord
        
        // Extract manufacturer data (where our message is stored)
        val manufacturerData = scanRecord?.getManufacturerSpecificData(0x004C)
        if (manufacturerData != null) {
            val message = String(manufacturerData)
            Log.d("BLEScanner", "Received: $message from ${device.name}")
            // Display message in UI
        }
    }
}

// Start passive scanning
bluetoothLeScanner.startScan(scanCallback)
```

### 2. Python Script (Linux/Raspberry Pi)

Use the `bleak` library for passive BLE scanning:

```python
import asyncio
from bleak import BleakScanner

async def scan_for_advertisements():
    def detection_callback(device, advertisement_data):
        # Check for our service UUID
        if "12345678-1234-1234-1234-123456789abc" in advertisement_data.service_uuids:
            # Extract manufacturer data
            manufacturer_data = advertisement_data.manufacturer_data
            if 0x004C in manufacturer_data:  # Apple company ID
                message = manufacturer_data[0x004C].decode('utf-8', errors='ignore')
                print(f"📡 Received: '{message}' from {device.name} (RSSI: {device.rssi})")

    scanner = BleakScanner(detection_callback)
    await scanner.start()
    
    print("🔍 Scanning for BLE advertisements...")
    print("Press Ctrl+C to stop")
    
    try:
        while True:
            await asyncio.sleep(1)
    except KeyboardInterrupt:
        await scanner.stop()
        print("Scanning stopped")

# Run the scanner
asyncio.run(scan_for_advertisements())
```

### 3. Linux Command Line

Use `bluetoothctl` for basic scanning:

```bash
# Enable Bluetooth
sudo bluetoothctl power on

# Start scanning
sudo bluetoothctl scan on

# Monitor for devices (will show MAC addresses and names)
# Note: This won't decode the manufacturer data automatically
```

### 4. Node.js Desktop App

Use the `noble` library:

```javascript
const noble = require('@abandonware/noble');

noble.on('stateChange', (state) => {
    if (state === 'poweredOn') {
        console.log('🔍 Starting BLE advertisement scan...');
        noble.startScanning([], true); // Allow duplicates
    }
});

noble.on('discover', (peripheral) => {
    const advertisement = peripheral.advertisement;
    const manufacturerData = advertisement.manufacturerData;
    
    if (manufacturerData && manufacturerData.length > 2) {
        // Check for Apple company ID (0x004C)
        if (manufacturerData[0] === 0x4C && manufacturerData[1] === 0x00) {
            const message = manufacturerData.slice(2).toString('utf8');
            console.log(`📡 Received: "${message}" from ${peripheral.advertisement.localName || 'Unknown'} (RSSI: ${peripheral.rssi})`);
        }
    }
});
```

## Why Web Browsers Don't Allow This

1. **Privacy Protection**: Prevents websites from tracking users via BLE beacons
2. **Security**: Stops malicious sites from scanning for vulnerable devices  
3. **Battery Life**: Continuous scanning would drain device battery
4. **User Control**: Forces explicit user consent for BLE interactions

## Current Web Solution

The current web client works by:
1. **User clicks "Connect"** (explicit consent)
2. **Browser shows device picker** (user selects specific device)
3. **Connects via GATT** (reads the message being broadcast)
4. **No passive scanning** (but can read the broadcast content)

This provides access to the broadcast messages while respecting browser security policies.

## Recommendation

For true COVID-style passive scanning, create a **native mobile app** or use a **Raspberry Pi with Python**. Web browsers are intentionally limited for security reasons.
