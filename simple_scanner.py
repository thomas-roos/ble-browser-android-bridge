#!/usr/bin/env python3
"""
Simple BLE Scanner for Android App Messages
===========================================

A lightweight scanner that shows only app messages with the configured prefix.
Perfect for quick testing and verification.

Requirements:
- Python 3.7+
- bleak library: pip install bleak

Usage:
    python simple_scanner.py
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
    config = DefaultConfig()

# Configuration
SERVICE_UUID = config.service_uuid
APPLE_COMPANY_ID = config.apple_company_id
MESSAGE_PREFIX = config.message_prefix

# Statistics
app_messages_received = 0

def print_header():
    """Print simple header."""
    print("🔍 Simple BLE Scanner")
    print(f"📱 Looking for '{MESSAGE_PREFIX}' messages...")
    print("🛑 Press Ctrl+C to stop")
    print("-" * 40)

def detection_callback(device: BLEDevice, advertisement_data: AdvertisementData):
    """Handle detected BLE advertisements."""
    global app_messages_received
    
    # Check for our service UUID
    service_uuids = advertisement_data.service_uuids or []
    if SERVICE_UUID not in [str(uuid) for uuid in service_uuids]:
        return
    
    # Extract manufacturer data
    manufacturer_data = advertisement_data.manufacturer_data or {}
    if APPLE_COMPANY_ID not in manufacturer_data:
        return
    
    try:
        data = manufacturer_data[APPLE_COMPANY_ID]
        full_message = data.decode('utf-8', errors='ignore').rstrip('\x00')
        
        # Only show app messages (with our prefix)
        if full_message.startswith(MESSAGE_PREFIX):
            user_message = full_message[len(MESSAGE_PREFIX):]
            timestamp = datetime.now().strftime('%H:%M:%S')
            device_name = device.name or f"Device {device.address}"
            rssi = advertisement_data.rssi
            
            app_messages_received += 1
            print(f"[{timestamp}] 📡 \"{user_message}\" from {device_name} ({rssi}dBm)")
            
    except Exception:
        pass  # Ignore decode errors in simple mode

async def main():
    """Main scanning function."""
    print_header()
    
    try:
        scanner = BleakScanner(detection_callback)
        await scanner.start()
        
        try:
            while True:
                await asyncio.sleep(1)
        except KeyboardInterrupt:
            await scanner.stop()
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return
    
    print(f"\n📊 Received {app_messages_received} app messages")
    print("👋 Goodbye!")

if __name__ == "__main__":
    asyncio.run(main())
