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

# Import configuration
try:
    from ble_config import config
except ImportError:
    print("Warning: Could not import ble_config, using defaults")
    class DefaultConfig:
        message_prefix = "BLE:"
        service_uuid = "12345678-1234-1234-1234-123456789abc"
        apple_company_id = 0x004C
        debug_mode = True
        log_timestamps = True
        show_rssi_details = True
    config = DefaultConfig()

# Configuration from config file
SERVICE_UUID = config.service_uuid
APPLE_COMPANY_ID = config.apple_company_id
MESSAGE_PREFIX = config.message_prefix
SCAN_DURATION = None  # None = scan indefinitely

# Statistics
message_count = 0
app_message_count = 0
filtered_message_count = 0
device_count = 0
seen_devices = set()

def print_header():
    """Print the script header and information."""
    print("=" * 70)
    print("🔍 BLE Advertisement Scanner - Debug Mode with Filtering")
    print("=" * 70)
    print(f"📡 Scanning for service UUID: {SERVICE_UUID}")
    print(f"🏭 Looking for manufacturer data from company ID: 0x{APPLE_COMPANY_ID:04X}")
    print(f"🏷️ Filtering for messages with prefix: '{MESSAGE_PREFIX}'")
    print(f"⏰ Started at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)
    print("📱 Make sure your Android app is in SERVER mode and broadcasting!")
    print("🛑 Press Ctrl+C to stop scanning")
    print("=" * 70)
    print()

def print_statistics():
    """Print current scanning statistics."""
    print(f"\n📊 Statistics:")
    print(f"   • Total messages: {message_count}")
    print(f"   • App messages (with '{MESSAGE_PREFIX}'): {app_message_count}")
    print(f"   • Filtered messages (non-app): {filtered_message_count}")
    print(f"   • Unique devices: {device_count}")

def format_rssi(rssi):
    """Format RSSI value with signal strength indicator."""
    if rssi is None:
        return "N/A"
    
    if config.show_rssi_details:
        if rssi >= -50:
            strength = "🟢 Excellent"
        elif rssi >= -60:
            strength = "🟡 Good"
        elif rssi >= -70:
            strength = "🟠 Fair"
        else:
            strength = "🔴 Weak"
        
        return f"{rssi} dBm ({strength})"
    else:
        return f"{rssi} dBm"

def decode_manufacturer_data(manufacturer_data):
    """Decode manufacturer data to extract the message."""
    if APPLE_COMPANY_ID not in manufacturer_data:
        return None, False
    
    try:
        data = manufacturer_data[APPLE_COMPANY_ID]
        full_message = data.decode('utf-8', errors='ignore').rstrip('\x00')
        
        if not full_message:
            return None, False
        
        # Check if message has our app prefix
        if full_message.startswith(MESSAGE_PREFIX):
            user_message = full_message[len(MESSAGE_PREFIX):]
            return user_message, True
        else:
            return full_message, False
            
    except Exception as e:
        if config.debug_mode:
            print(f"⚠️  Error decoding manufacturer data: {e}")
        return None, False

def format_timestamp():
    """Format current timestamp if enabled."""
    if config.log_timestamps:
        return f"[{datetime.now().strftime('%H:%M:%S')}] "
    return ""

def detection_callback(device: BLEDevice, advertisement_data: AdvertisementData):
    """Callback function called when a BLE advertisement is detected."""
    global message_count, app_message_count, filtered_message_count, device_count, seen_devices
    
    # Check if this advertisement contains our service UUID
    service_uuids = advertisement_data.service_uuids or []
    if SERVICE_UUID not in [str(uuid) for uuid in service_uuids]:
        return
    
    # Track unique devices
    device_id = device.address
    if device_id not in seen_devices:
        seen_devices.add(device_id)
        device_count += 1
    
    # Get device name
    device_name = device.name or f"Device {device.address}"
    
    # Extract and decode message
    manufacturer_data = advertisement_data.manufacturer_data or {}
    message, is_app_message = decode_manufacturer_data(manufacturer_data)
    
    message_count += 1
    timestamp = format_timestamp()
    
    if message:
        if is_app_message:
            app_message_count += 1
            print(f"{timestamp}📡 APP MESSAGE: \"{message}\"")
            print(f"{timestamp}   └─ Device: {device_name}")
            print(f"{timestamp}   └─ Signal: {format_rssi(advertisement_data.rssi)}")
            print(f"{timestamp}   └─ Full payload: \"{MESSAGE_PREFIX}{message}\"")
        else:
            filtered_message_count += 1
            print(f"{timestamp}🔍 FILTERED (non-app): \"{message}\"")
            print(f"{timestamp}   └─ Device: {device_name}")
            print(f"{timestamp}   └─ Signal: {format_rssi(advertisement_data.rssi)}")
            print(f"{timestamp}   └─ Reason: Missing '{MESSAGE_PREFIX}' prefix")
    else:
        filtered_message_count += 1
        print(f"{timestamp}❌ INVALID MESSAGE DATA")
        print(f"{timestamp}   └─ Device: {device_name}")
        print(f"{timestamp}   └─ Signal: {format_rssi(advertisement_data.rssi)}")
    
    print()  # Add spacing between messages

async def scan_for_advertisements():
    """Main scanning function."""
    print_header()
    
    # Print configuration
    if config.debug_mode:
        print("🔧 Configuration:")
        print(f"   • Message prefix: '{MESSAGE_PREFIX}'")
        print(f"   • Service UUID: {SERVICE_UUID}")
        print(f"   • Company ID: 0x{APPLE_COMPANY_ID:04X}")
        print(f"   • Debug mode: {config.debug_mode}")
        print(f"   • Show timestamps: {config.log_timestamps}")
        print(f"   • Show RSSI details: {config.show_rssi_details}")
        print()
    
    try:
        print("🚀 Starting BLE scan...")
        print("💡 Only messages with the correct prefix will be shown as APP MESSAGES")
        print("🔍 Other messages will be shown as FILTERED")
        print()
        
        # Start scanning
        scanner = BleakScanner(detection_callback)
        await scanner.start()
        
        if SCAN_DURATION:
            print(f"⏱️  Scanning for {SCAN_DURATION} seconds...")
            await asyncio.sleep(SCAN_DURATION)
            await scanner.stop()
        else:
            print("♾️  Scanning indefinitely... Press Ctrl+C to stop")
            try:
                while True:
                    await asyncio.sleep(1)
            except KeyboardInterrupt:
                print("\n🛑 Stopping scan...")
                await scanner.stop()
                
    except Exception as e:
        print(f"❌ Error during scanning: {e}")
        return False
    
    return True

def main():
    """Main function."""
    try:
        # Run the async scanner
        success = asyncio.run(scan_for_advertisements())
        
        if success:
            print_statistics()
            print("\n✅ Scan completed successfully!")
            
            if app_message_count == 0:
                print("\n💡 Troubleshooting tips:")
                print("   • Make sure your Android app is running")
                print("   • Switch to SERVER mode in the app")
                print("   • Set a message and start broadcasting")
                print("   • Check that Bluetooth is enabled on both devices")
                print("   • Try moving devices closer together")
        else:
            print("\n❌ Scan failed!")
            
    except KeyboardInterrupt:
        print_statistics()
        print("\n👋 Goodbye!")
    except Exception as e:
        print(f"\n💥 Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
