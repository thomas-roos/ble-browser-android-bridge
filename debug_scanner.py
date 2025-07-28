#!/usr/bin/env python3
"""
BLE Advertisement Scanner for Debugging Android App
==================================================

This script scans for BLE advertisements from the Android app and prints
received messages to the console. Perfect for debugging and verification.

Requirements:
- Python 3.7+
- bleak library: pip install bleak
- Bluetooth adapter on laptop

Usage:
    python debug_scanner.py

The script will continuously scan for BLE advertisements from the Android app
and print any messages it receives along with device information.
"""

import asyncio
import sys
import time
from datetime import datetime
from bleak import BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData

# Configuration
SERVICE_UUID = "12345678-1234-1234-1234-123456789abc"
APPLE_COMPANY_ID = 0x004C  # Company ID used in Android app
SCAN_DURATION = None  # None = scan indefinitely

# Statistics
message_count = 0
device_count = 0
seen_devices = set()

def print_header():
    """Print the script header and information."""
    print("=" * 60)
    print("🔍 BLE Advertisement Scanner - Debug Mode")
    print("=" * 60)
    print(f"📡 Scanning for service UUID: {SERVICE_UUID}")
    print(f"🏭 Looking for manufacturer data from company ID: 0x{APPLE_COMPANY_ID:04X}")
    print(f"⏰ Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    print("📱 Make sure your Android app is in SERVER mode and broadcasting!")
    print("🛑 Press Ctrl+C to stop scanning")
    print("=" * 60)
    print()

def print_statistics():
    """Print current scanning statistics."""
    print(f"\n📊 Statistics: {message_count} messages received from {device_count} unique devices")

def format_rssi(rssi):
    """Format RSSI value with signal strength indicator."""
    if rssi is None:
        return "N/A"
    
    if rssi >= -50:
        strength = "🟢 Excellent"
    elif rssi >= -60:
        strength = "🟡 Good"
    elif rssi >= -70:
        strength = "🟠 Fair"
    else:
        strength = "🔴 Weak"
    
    return f"{rssi} dBm ({strength})"

def decode_manufacturer_data(manufacturer_data):
    """Decode manufacturer data to extract the message."""
    if APPLE_COMPANY_ID not in manufacturer_data:
        return None
    
    try:
        data = manufacturer_data[APPLE_COMPANY_ID]
        message = data.decode('utf-8', errors='ignore').rstrip('\x00')
        return message if message else None
    except Exception as e:
        print(f"⚠️  Error decoding manufacturer data: {e}")
        return None

def detection_callback(device: BLEDevice, advertisement_data: AdvertisementData):
    """Callback function called when a BLE advertisement is detected."""
    global message_count, device_count, seen_devices
    
    # Check if this advertisement contains our service UUID
    service_uuids = advertisement_data.service_uuids or []
    if SERVICE_UUID not in [str(uuid) for uuid in service_uuids]:
        return
    
    # Track unique devices
    device_id = device.address
    if device_id not in seen_devices:
        seen_devices.add(device_id)
        device_count += 1
    
    # Extract message from manufacturer data
    message = decode_manufacturer_data(advertisement_data.manufacturer_data or {})
    
    if message:
        message_count += 1
        timestamp = datetime.now().strftime('%H:%M:%S.%f')[:-3]  # Include milliseconds
        
        print(f"[{timestamp}] 📡 MESSAGE RECEIVED")
        print(f"  📱 Device: {device.name or 'Unknown'} ({device.address})")
        print(f"  💬 Message: \"{message}\"")
        print(f"  📶 Signal: {format_rssi(advertisement_data.rssi)}")
        print(f"  🔢 Total Messages: {message_count}")
        print("-" * 50)
    else:
        # Advertisement from our service but no readable message
        timestamp = datetime.now().strftime('%H:%M:%S.%f')[:-3]
        print(f"[{timestamp}] 📡 ADVERTISEMENT DETECTED (no message)")
        print(f"  📱 Device: {device.name or 'Unknown'} ({device.address})")
        print(f"  📶 Signal: {format_rssi(advertisement_data.rssi)}")
        print(f"  ⚠️  No readable message in manufacturer data")
        print("-" * 50)

async def scan_for_advertisements():
    """Main scanning function."""
    print_header()
    
    try:
        # Create scanner with detection callback
        scanner = BleakScanner(detection_callback)
        
        print("🔍 Starting BLE advertisement scan...")
        print("📡 Listening for messages from Android devices...")
        print()
        
        # Start scanning
        await scanner.start()
        
        try:
            if SCAN_DURATION:
                print(f"⏱️  Scanning for {SCAN_DURATION} seconds...")
                await asyncio.sleep(SCAN_DURATION)
            else:
                print("♾️  Scanning indefinitely... (Press Ctrl+C to stop)")
                while True:
                    await asyncio.sleep(1)
                    
        except KeyboardInterrupt:
            print("\n🛑 Scan interrupted by user")
            
        finally:
            await scanner.stop()
            print("📊 Scanning stopped")
            print_statistics()
            
    except Exception as e:
        print(f"❌ Error during scanning: {e}")
        print("\n💡 Troubleshooting tips:")
        print("   • Make sure Bluetooth is enabled on your laptop")
        print("   • Check that your laptop has BLE support")
        print("   • Try running with sudo on Linux: sudo python debug_scanner.py")
        print("   • Make sure the Android app is in SERVER mode and broadcasting")

def check_requirements():
    """Check if required dependencies are available."""
    try:
        import bleak
        print(f"✅ bleak library found (version: {bleak.__version__})")
        return True
    except ImportError:
        print("❌ bleak library not found!")
        print("📦 Install it with: pip install bleak")
        return False

def main():
    """Main function."""
    if not check_requirements():
        sys.exit(1)
    
    try:
        # Run the scanner
        asyncio.run(scan_for_advertisements())
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
