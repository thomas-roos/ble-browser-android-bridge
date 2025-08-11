#!/usr/bin/env python3
"""
Debug Broadcast Test
===================

Simple test to verify BLE broadcasting is working.
"""

import subprocess
import time
import sys

def test_bluetooth_basic():
    """Test basic Bluetooth functionality."""
    print("🔍 Testing Bluetooth Setup...")
    
    # Check if bluetoothctl exists
    try:
        result = subprocess.run(['bluetoothctl', '--version'], 
                              capture_output=True, text=True, timeout=5)
        print(f"✅ bluetoothctl version: {result.stdout.strip()}")
    except Exception as e:
        print(f"❌ bluetoothctl not working: {e}")
        return False
    
    # Check Bluetooth status
    try:
        result = subprocess.run(['bluetoothctl', 'show'], 
                              capture_output=True, text=True, timeout=5)
        if "Powered: yes" in result.stdout:
            print("✅ Bluetooth is powered on")
        else:
            print("❌ Bluetooth is not powered on")
            return False
    except Exception as e:
        print(f"❌ Cannot check Bluetooth status: {e}")
        return False
    
    return True

def test_simple_advertisement():
    """Test a very simple BLE advertisement."""
    print("\n📡 Testing Simple BLE Advertisement...")
    print("This will advertise for 10 seconds...")
    
    try:
        # Create a simple advertisement script
        script = """
bluetoothctl power on
sleep 1
bluetoothctl advertise on
bluetoothctl advertise name "TEST-LAPTOP"
echo "Advertisement started - check your Android app!"
sleep 10
bluetoothctl advertise off
echo "Advertisement stopped"
"""
        
        with open('/tmp/test_ad.sh', 'w') as f:
            f.write(script)
        
        subprocess.run(['chmod', '+x', '/tmp/test_ad.sh'])
        
        print("🚀 Starting advertisement...")
        print("📱 Check your Android app in CLIENT mode now!")
        
        result = subprocess.run(['bash', '/tmp/test_ad.sh'], 
                              capture_output=True, text=True, timeout=15)
        
        print("✅ Advertisement test completed")
        print("📱 Did you see 'TEST-LAPTOP' device in your Android app?")
        
        return True
        
    except Exception as e:
        print(f"❌ Advertisement test failed: {e}")
        return False

def main():
    print("🧪 BLE Broadcast Debug Test")
    print("=" * 40)
    
    if not test_bluetooth_basic():
        print("\n❌ Basic Bluetooth test failed")
        print("💡 Try: sudo systemctl start bluetooth")
        return
    
    print("\n📋 Before running advertisement test:")
    print("1. Open your Android app")
    print("2. Switch to CLIENT mode (📱 CLIENT button)")
    print("3. Tap 'Start Scanning'")
    print("4. Keep devices close (within 5 meters)")
    
    input("\nPress Enter when Android app is ready...")
    
    if test_simple_advertisement():
        print("\n✅ If you saw 'TEST-LAPTOP' in your Android app, BLE is working!")
        print("💡 The issue might be with message format or filtering")
    else:
        print("\n❌ Basic advertisement failed")
        print("💡 Try running with sudo: sudo python3 debug_broadcast_test.py")

if __name__ == "__main__":
    main()
