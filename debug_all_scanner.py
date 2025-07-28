#!/usr/bin/env python3
"""
Comprehensive BLE Advertisement Scanner
======================================

This script scans for ALL BLE advertisements and shows detailed information
about each device, including which ones contain app messages with the configured prefix.

Requirements:
- Python 3.7+
- bleak library: pip install bleak

Usage:
    python debug_all_scanner.py
"""

import asyncio
import sys
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

# Configuration
SERVICE_UUID = config.service_uuid
APPLE_COMPANY_ID = config.apple_company_id
MESSAGE_PREFIX = config.message_prefix

# Statistics
total_advertisements = 0
app_service_advertisements = 0
app_messages_found = 0
unique_devices = set()

def print_header():
    """Print comprehensive header."""
    print("=" * 80)
    print("🌐 Comprehensive BLE Advertisement Scanner")
    print("=" * 80)
    print(f"📡 Target service UUID: {SERVICE_UUID}")
    print(f"🏷️ App message prefix: '{MESSAGE_PREFIX}'")
    print(f"🏭 Manufacturer data company ID: 0x{APPLE_COMPANY_ID:04X}")
    print(f"⏰ Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 80)
    print("📱 This scanner shows ALL BLE devices, highlighting app messages")
    print("🛑 Press Ctrl+C to stop")
    print("=" * 80)
    print()

def format_rssi(rssi):
    """Format RSSI with signal strength."""
    if rssi is None:
        return "N/A"
    
    if rssi >= -50:
        strength = "🟢"
    elif rssi >= -60:
        strength = "🟡"
    elif rssi >= -70:
        strength = "🟠"
    else:
        strength = "🔴"
    
    return f"{strength} {rssi}dBm"

def format_services(service_uuids):
    """Format service UUIDs list."""
    if not service_uuids:
        return "None"
    
    services = []
    for uuid in service_uuids:
        uuid_str = str(uuid)
        if uuid_str == SERVICE_UUID:
            services.append(f"🎯 {uuid_str} (APP SERVICE)")
        else:
            services.append(uuid_str)
    
    return ", ".join(services)

def decode_manufacturer_data(manufacturer_data):
    """Decode all manufacturer data."""
    if not manufacturer_data:
        return "None", None, False
    
    results = []
    app_message = None
    is_app_message = False
    
    for company_id, data in manufacturer_data.items():
        try:
            decoded = data.decode('utf-8', errors='ignore').rstrip('\x00')
            company_name = f"0x{company_id:04X}"
            
            if company_id == APPLE_COMPANY_ID:
                company_name += " (Apple/Target)"
                
                # Check if this is an app message
                if decoded.startswith(MESSAGE_PREFIX):
                    app_message = decoded[len(MESSAGE_PREFIX):]
                    is_app_message = True
                    results.append(f"🎯 {company_name}: \"{decoded}\" → APP MESSAGE: \"{app_message}\"")
                else:
                    results.append(f"📱 {company_name}: \"{decoded}\" (no app prefix)")
            else:
                results.append(f"🏭 {company_name}: \"{decoded}\"")
                
        except Exception:
            results.append(f"🏭 0x{company_id:04X}: [Binary data, {len(data)} bytes]")
    
    return "; ".join(results), app_message, is_app_message

def detection_callback(device: BLEDevice, advertisement_data: AdvertisementData):
    """Handle all BLE advertisements."""
    global total_advertisements, app_service_advertisements, app_messages_found, unique_devices
    
    total_advertisements += 1
    unique_devices.add(device.address)
    
    # Check if this has our target service
    service_uuids = advertisement_data.service_uuids or []
    has_app_service = SERVICE_UUID in [str(uuid) for uuid in service_uuids]
    
    if has_app_service:
        app_service_advertisements += 1
    
    # Decode manufacturer data
    manufacturer_data = advertisement_data.manufacturer_data or {}
    mfg_info, app_message, is_app_message = decode_manufacturer_data(manufacturer_data)
    
    if is_app_message:
        app_messages_found += 1
    
    # Format timestamp
    timestamp = datetime.now().strftime('%H:%M:%S') if config.log_timestamps else ""
    timestamp_prefix = f"[{timestamp}] " if timestamp else ""
    
    # Device info
    device_name = device.name or "Unknown"
    device_info = f"{device_name} ({device.address})"
    
    # Print device information
    if is_app_message:
        print(f"{timestamp_prefix}🎯 APP MESSAGE DEVICE:")
        print(f"{timestamp_prefix}   📱 Device: {device_info}")
        print(f"{timestamp_prefix}   📡 Signal: {format_rssi(advertisement_data.rssi)}")
        print(f"{timestamp_prefix}   💬 Message: \"{app_message}\"")
        print(f"{timestamp_prefix}   🏷️ Full payload: \"{MESSAGE_PREFIX}{app_message}\"")
        print(f"{timestamp_prefix}   🔧 Services: {format_services(service_uuids)}")
        print(f"{timestamp_prefix}   🏭 Manufacturer: {mfg_info}")
    elif has_app_service:
        print(f"{timestamp_prefix}📡 APP SERVICE (no message):")
        print(f"{timestamp_prefix}   📱 Device: {device_info}")
        print(f"{timestamp_prefix}   📡 Signal: {format_rssi(advertisement_data.rssi)}")
        print(f"{timestamp_prefix}   🔧 Services: {format_services(service_uuids)}")
        print(f"{timestamp_prefix}   🏭 Manufacturer: {mfg_info}")
    else:
        # Show other devices in compact format
        print(f"{timestamp_prefix}📟 OTHER: {device_info} | {format_rssi(advertisement_data.rssi)} | Services: {len(service_uuids)} | Mfg: {len(manufacturer_data)} companies")
    
    print()  # Add spacing

def print_statistics():
    """Print scanning statistics."""
    print("\n" + "=" * 60)
    print("📊 SCANNING STATISTICS")
    print("=" * 60)
    print(f"📡 Total advertisements: {total_advertisements}")
    print(f"📱 Unique devices seen: {len(unique_devices)}")
    print(f"🎯 App service advertisements: {app_service_advertisements}")
    print(f"💬 App messages found: {app_messages_found}")
    print(f"🏷️ Message prefix: '{MESSAGE_PREFIX}'")
    print("=" * 60)

async def main():
    """Main scanning function."""
    print_header()
    
    try:
        scanner = BleakScanner(detection_callback)
        await scanner.start()
        
        print("🚀 Scanning started... All BLE devices will be shown")
        print("🎯 App messages will be highlighted")
        print()
        
        try:
            while True:
                await asyncio.sleep(1)
        except KeyboardInterrupt:
            print("\n🛑 Stopping scan...")
            await scanner.stop()
            
    except Exception as e:
        print(f"❌ Error during scanning: {e}")
        return
    
    print_statistics()
    
    if app_messages_found == 0:
        print("\n💡 No app messages found. Troubleshooting tips:")
        print("   • Make sure Android app is in SERVER mode")
        print("   • Set a message and start broadcasting")
        print("   • Check Bluetooth is enabled on both devices")
        print("   • Try moving devices closer together")
    
    print("\n👋 Goodbye!")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")
    except Exception as e:
        print(f"\n💥 Unexpected error: {e}")
        sys.exit(1)
