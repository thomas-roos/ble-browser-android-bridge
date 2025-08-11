#!/usr/bin/env python3
"""
Working BLE Broadcaster
=======================

A more reliable approach to BLE broadcasting using hciconfig and hcitool.
This bypasses some of the bluetoothctl issues.
"""

import subprocess
import sys
import time
import signal
from datetime import datetime

# Import configuration
try:
    from ble_config import config
except ImportError:
    print("Warning: Could not import ble_config, using defaults")
    class DefaultConfig:
        message_prefix = "BLE:"
        max_user_message_length = 11
    config = DefaultConfig()

class WorkingBLEBroadcaster:
    """BLE broadcaster using hciconfig/hcitool."""
    
    def __init__(self):
        self.is_advertising = False
        
    def check_requirements(self):
        """Check if required tools are available."""
        required_tools = ['hciconfig', 'hcitool']
        
        for tool in required_tools:
            try:
                result = subprocess.run(['which', tool], capture_output=True)
                if result.returncode != 0:
                    print(f"❌ Error: {tool} not found")
                    print("💡 Install with: sudo apt install bluetooth bluez-tools")
                    return False
            except Exception:
                print(f"❌ Error: Cannot check for {tool}")
                return False
                
        return True
        
    def validate_message(self, message):
        """Validate message length."""
        if len(message) > config.max_user_message_length:
            print(f"❌ Message too long! Max {config.max_user_message_length} characters")
            return False
        return True
        
    def start_advertising(self, message):
        """Start BLE advertising using hciconfig/hcitool."""
        if not self.validate_message(message):
            return False
            
        try:
            full_message = config.message_prefix + message
            print(f"📤 Broadcasting: '{full_message}'")
            
            # Enable BLE advertising
            subprocess.run(['sudo', 'hciconfig', 'hci0', 'up'], check=True)
            subprocess.run(['sudo', 'hciconfig', 'hci0', 'leadv', '3'], check=True)
            
            # Create advertisement data
            # This is a simplified approach - just make the device discoverable
            subprocess.run(['sudo', 'hciconfig', 'hci0', 'name', f'BLE-{message}'], check=True)
            subprocess.run(['sudo', 'hciconfig', 'hci0', 'piscan'], check=True)
            
            timestamp = datetime.now().strftime('%H:%M:%S')
            print(f"[{timestamp}] ✅ Started advertising as 'BLE-{message}'")
            print("📱 Your Android app should now see this device!")
            print("🛑 Press Ctrl+C to stop")
            
            self.is_advertising = True
            
            # Keep advertising
            try:
                while True:
                    time.sleep(1)
            except KeyboardInterrupt:
                self.stop_advertising()
                
            return True
            
        except subprocess.CalledProcessError as e:
            print(f"❌ Failed to start advertising: {e}")
            print("💡 Make sure to run with sudo")
            return False
        except Exception as e:
            print(f"❌ Error: {e}")
            return False
            
    def stop_advertising(self):
        """Stop BLE advertising."""
        try:
            subprocess.run(['sudo', 'hciconfig', 'hci0', 'noleadv'], capture_output=True)
            subprocess.run(['sudo', 'hciconfig', 'hci0', 'noscan'], capture_output=True)
            print("🛑 Stopped advertising")
            self.is_advertising = False
        except Exception as e:
            print(f"⚠️ Error stopping advertising: {e}")

def main():
    """Main function."""
    if len(sys.argv) != 2:
        print("Usage: sudo python3 working_broadcaster.py 'Your message'")
        print("Example: sudo python3 working_broadcaster.py 'Hi Android'")
        return
        
    message = sys.argv[1]
    broadcaster = WorkingBLEBroadcaster()
    
    if not broadcaster.check_requirements():
        return
        
    print("📡 Working BLE Broadcaster")
    print(f"🏷️ Will broadcast as: 'BLE-{message}'")
    print("📱 Android app should be in CLIENT mode")
    print("-" * 50)
    
    broadcaster.start_advertising(message)

if __name__ == "__main__":
    main()
