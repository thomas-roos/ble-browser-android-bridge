#!/usr/bin/env python3
"""
Simple BLE Scanner for Android App Debugging
===========================================

A minimal script to scan for BLE advertisements from the Android app.
Perfect for quick debugging and message verification.

Setup:
    pip install bleak

Usage:
    python simple_scanner.py
"""

import asyncio
from datetime import datetime
from bleak import BleakScanner

SERVICE_UUID = "12345678-1234-1234-1234-123456789abc"
COMPANY_ID = 0x004C

async def main():
    print("🔍 BLE Scanner - Listening for Android app messages...")
    print(f"📡 Service UUID: {SERVICE_UUID}")
    print("🛑 Press Ctrl+C to stop\n")
    
    def callback(device, ad_data):
        # Check if it's our service
        if SERVICE_UUID in [str(uuid) for uuid in (ad_data.service_uuids or [])]:
            # Extract message from manufacturer data
            message = "No message"
            if ad_data.manufacturer_data and COMPANY_ID in ad_data.manufacturer_data:
                try:
                    data = ad_data.manufacturer_data[COMPANY_ID]
                    message = data.decode('utf-8', errors='ignore').rstrip('\x00')
                except:
                    message = "Decode error"
            
            timestamp = datetime.now().strftime('%H:%M:%S')
            device_name = device.name or "Unknown"
            rssi = ad_data.rssi or "N/A"
            
            print(f"[{timestamp}] 📱 {device_name}: \"{message}\" (RSSI: {rssi})")
    
    scanner = BleakScanner(callback)
    await scanner.start()
    
    try:
        while True:
            await asyncio.sleep(1)
    except KeyboardInterrupt:
        print("\n🛑 Stopping scanner...")
    finally:
        await scanner.stop()
        print("👋 Done!")

if __name__ == "__main__":
    asyncio.run(main())
