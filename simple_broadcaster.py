#!/usr/bin/env python3
"""
Simple BLE Broadcaster
======================

A simpler approach to broadcasting BLE messages from laptop using bluetoothctl.
This creates a script that uses system Bluetooth tools to advertise messages.

Requirements:
- Linux system with BlueZ
- bluetoothctl command available
- Root/sudo access for BLE advertising

Usage:
    python simple_broadcaster.py "Hello Android"
    python simple_broadcaster.py --interactive
"""

import subprocess
import sys
import time
import argparse
from datetime import datetime
import signal
import os

# Import configuration
try:
    from ble_config import config
except ImportError:
    print("Warning: Could not import ble_config, using defaults")
    class DefaultConfig:
        message_prefix = "BLE:"
        service_uuid = "12345678-1234-1234-1234-123456789abc"
        apple_company_id = 0x004C
        max_user_message_length = 11
    config = DefaultConfig()

class SimpleBLEBroadcaster:
    """Simple BLE broadcaster using system bluetoothctl."""
    
    def __init__(self):
        self.current_message = ""
        self.is_advertising = False
        
    def print_header(self):
        """Print broadcaster header."""
        print("📡 Simple BLE Broadcaster (bluetoothctl)")
        print(f"🏷️ Prefix: '{config.message_prefix}' | Max message: {config.max_user_message_length} chars")
        print("📱 Android app should be in CLIENT mode to receive messages")
        print("-" * 60)
        
    def check_requirements(self):
        """Check if required tools are available."""
        try:
            # Check if bluetoothctl is available
            result = subprocess.run(['which', 'bluetoothctl'], 
                                  capture_output=True, text=True)
            if result.returncode != 0:
                print("❌ Error: bluetoothctl not found")
                print("💡 Install with: sudo apt install bluetooth bluez")
                return False
                
            # Check if running as root
            if os.geteuid() != 0:
                print("⚠️ Warning: Not running as root")
                print("💡 BLE advertising usually requires sudo")
                print("💡 Try: sudo python3 simple_broadcaster.py")
                
            return True
            
        except Exception as e:
            print(f"❌ Error checking requirements: {e}")
            return False
            
    def validate_message(self, message):
        """Validate message length."""
        if len(message) > config.max_user_message_length:
            print(f"❌ Message too long! Max {config.max_user_message_length} characters")
            print(f"   Your message: '{message}' ({len(message)} chars)")
            return False
        return True
        
    def create_advertisement_script(self, message):
        """Create a bluetoothctl script for advertising."""
        full_message = config.message_prefix + message
        message_hex = full_message.encode('utf-8').hex()
        
        # Create manufacturer data (Apple company ID 0x004C)
        company_id = f"{config.apple_company_id:04x}"
        company_id_le = company_id[2:4] + company_id[0:2]  # Little endian
        manufacturer_data = company_id_le + message_hex
        
        script_content = f"""#!/bin/bash
# BLE Advertisement Script
echo "Starting BLE advertisement..."

# Power on Bluetooth
bluetoothctl power on
sleep 1

# Set discoverable
bluetoothctl discoverable on
sleep 1

# Remove any existing advertisement
bluetoothctl remove-advertisement 0 2>/dev/null || true
sleep 1

# Create new advertisement
bluetoothctl advertise on
bluetoothctl advertise manufacturer {config.apple_company_id} {message_hex}
bluetoothctl advertise name "Laptop-BLE"
bluetoothctl advertise uuids {config.service_uuid}

echo "Advertisement started with message: '{full_message}'"
echo "Press Ctrl+C to stop"

# Keep running
while true; do
    sleep 1
done
"""
        
        script_path = "/tmp/ble_advertise.sh"
        with open(script_path, 'w') as f:
            f.write(script_content)
        os.chmod(script_path, 0o755)
        
        return script_path, full_message
        
    def start_advertising(self, message):
        """Start BLE advertising with the given message."""
        if not self.validate_message(message):
            return False
            
        try:
            script_path, full_message = self.create_advertisement_script(message)
            
            print(f"📤 Broadcasting: '{full_message}'")
            timestamp = datetime.now().strftime('%H:%M:%S')
            print(f"[{timestamp}] ✅ Starting advertisement...")
            print("📱 Your Android app in CLIENT mode should now see this message!")
            print("🛑 Press Ctrl+C to stop")
            print()
            
            # Run the script
            process = subprocess.run(['bash', script_path], 
                                   capture_output=False, text=True)
            
            return True
            
        except KeyboardInterrupt:
            print("\n🛑 Stopping advertisement...")
            self.stop_advertising()
            return True
        except Exception as e:
            print(f"❌ Failed to start advertising: {e}")
            return False
            
    def stop_advertising(self):
        """Stop BLE advertising."""
        try:
            subprocess.run(['bluetoothctl', 'advertise', 'off'], 
                         capture_output=True, text=True)
            print("🛑 Advertisement stopped")
        except Exception as e:
            print(f"⚠️ Error stopping advertisement: {e}")
            
    def interactive_mode(self):
        """Interactive mode for sending multiple messages."""
        self.print_header()
        print("🎮 Interactive Mode - Type messages to broadcast")
        print("💡 Commands: 'quit' to exit")
        print("⚠️ Each message starts a new advertisement session")
        print()
        
        while True:
            try:
                message = input("📝 Enter message to broadcast: ").strip()
                
                if message.lower() in ['quit', 'exit', 'q']:
                    break
                elif not message:
                    print("⚠️ Empty message, try again")
                    continue
                    
                print(f"\n🚀 Starting advertisement for: '{message}'")
                self.start_advertising(message)
                print()
                
            except KeyboardInterrupt:
                break
            except EOFError:
                break
                
        print("👋 Goodbye!")

def main():
    """Main function."""
    parser = argparse.ArgumentParser(
        description='Simple BLE Broadcaster using bluetoothctl',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  sudo python3 simple_broadcaster.py "Hello Android"
  sudo python3 simple_broadcaster.py --interactive

Setup Steps:
  1. Put Android app in CLIENT mode (📱 CLIENT button)
  2. Start scanning on Android app ("Start Scanning")
  3. Run this script with sudo to broadcast messages
  4. Android app should receive and display messages

Requirements:
  - Linux with BlueZ/bluetoothctl
  - Root access (sudo)
  - Bluetooth enabled
        """
    )
    
    parser.add_argument('message', nargs='?', help='Message to broadcast')
    parser.add_argument('--interactive', '-i', action='store_true', 
                       help='Interactive mode for multiple messages')
    parser.add_argument('--config', action='store_true', 
                       help='Show current configuration')
    
    args = parser.parse_args()
    
    if args.config:
        print("🔧 Current Broadcaster Configuration:")
        print(f"   Message Prefix: '{config.message_prefix}'")
        print(f"   Service UUID: {config.service_uuid}")
        print(f"   Company ID: 0x{config.apple_company_id:04X}")
        print(f"   Max User Message: {config.max_user_message_length} chars")
        return
    
    broadcaster = SimpleBLEBroadcaster()
    
    if not broadcaster.check_requirements():
        sys.exit(1)
    
    if args.interactive:
        broadcaster.interactive_mode()
    elif args.message:
        broadcaster.print_header()
        broadcaster.start_advertising(args.message)
    else:
        print("❌ Error: Provide a message or use --interactive mode")
        print("💡 Usage: sudo python3 simple_broadcaster.py 'Your message'")
        print("💡 Or: sudo python3 simple_broadcaster.py --interactive")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")
    except Exception as e:
        print(f"❌ Error: {e}")
        print("💡 Make sure to run with sudo and check Bluetooth is enabled")
