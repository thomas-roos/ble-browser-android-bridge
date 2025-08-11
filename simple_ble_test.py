#!/usr/bin/env python3
"""
Simple BLE Test
===============

Test if we can make the laptop visible to Android using Python bleak.
"""

import asyncio
import sys
from bleak import BleakScanner

async def test_scan_from_laptop():
    """Test if laptop can scan for BLE devices (basic functionality test)."""
    print("🔍 Testing laptop BLE scanning capability...")
    
    devices_found = []
    
    def detection_callback(device, advertisement_data):
        devices_found.append(device.name or device.address)
    
    try:
        scanner = BleakScanner(detection_callback)
        await scanner.start()
        
        print("📡 Scanning for 5 seconds...")
        await asyncio.sleep(5)
        
        await scanner.stop()
        
        if devices_found:
            print(f"✅ Found {len(devices_found)} BLE devices:")
            for device in devices_found[:5]:  # Show first 5
                print(f"   • {device}")
            print("✅ Laptop BLE scanning works!")
        else:
            print("❌ No BLE devices found")
            print("💡 This might indicate BLE issues or no devices nearby")
            
    except Exception as e:
        print(f"❌ BLE scanning failed: {e}")
        print("💡 Try running with sudo: sudo python3 simple_ble_test.py")

async def main():
    print("🧪 Simple BLE Functionality Test")
    print("=" * 40)
    
    await test_scan_from_laptop()
    
    print("\n💡 Next steps:")
    print("1. If scanning worked, BLE is functional on laptop")
    print("2. If no devices found, try moving closer to other BLE devices")
    print("3. The issue might be with advertising, not scanning")

if __name__ == "__main__":
    asyncio.run(main())
