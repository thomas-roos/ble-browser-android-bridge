#!/usr/bin/env python3
"""
Laptop BLE Broadcaster
======================

Broadcasts BLE messages from laptop that can be received by the Android app in CLIENT mode.
This is the reverse of the normal setup - laptop sends, Android receives.

Requirements:
- Python 3.7+
- bleak library: pip install bleak
- Linux system with BlueZ (BLE advertising support)

Usage:
    python laptop_broadcaster.py "Hello Android"
    python laptop_broadcaster.py --interactive
    python laptop_broadcaster.py --help
"""

import asyncio
import sys
import argparse
from datetime import datetime
import signal

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

# Check if we're on Linux (required for BLE advertising)
import platform
if platform.system() != 'Linux':
    print("❌ Error: BLE advertising from Python is only supported on Linux")
    print("💡 Alternative: Use the Android app in SERVER mode instead")
    sys.exit(1)

try:
    from bleak import BleakScanner
    from bleak.backends.bluezdbus.advertisement import Advertisement
    from bleak.backends.bluezdbus.manager import get_global_bluez_manager
    import dbus
    import dbus.exceptions
    import dbus.mainloop.glib
    import dbus.service
    from gi.repository import GLib
except ImportError as e:
    print(f"❌ Error: Missing required libraries: {e}")
    print("💡 Install with: pip install bleak dbus-python PyGObject")
    sys.exit(1)

class LaptopBLEBroadcaster:
    """BLE broadcaster that mimics the Android app's advertising."""
    
    def __init__(self):
        self.current_message = ""
        self.is_advertising = False
        self.advertisement = None
        self.manager = None
        
    def print_header(self):
        """Print broadcaster header."""
        print("📡 Laptop BLE Broadcaster")
        print(f"🏷️ Prefix: '{config.message_prefix}' | Max message: {config.max_user_message_length} chars")
        print(f"🎯 Service UUID: {config.service_uuid}")
        print("📱 Android app should be in CLIENT mode to receive messages")
        print("-" * 60)
        
    def validate_message(self, message):
        """Validate message length."""
        if len(message) > config.max_user_message_length:
            print(f"❌ Message too long! Max {config.max_user_message_length} characters")
            print(f"   Your message: '{message}' ({len(message)} chars)")
            return False
        return True
        
    def create_advertisement_data(self, message):
        """Create BLE advertisement data with message."""
        full_message = config.message_prefix + message
        message_bytes = full_message.encode('utf-8')
        
        print(f"📤 Broadcasting: '{full_message}' ({len(message_bytes)} bytes)")
        
        # Create manufacturer data (same format as Android app)
        manufacturer_data = {
            config.apple_company_id: message_bytes
        }
        
        return manufacturer_data
        
    async def start_advertising(self, message):
        """Start BLE advertising with the given message."""
        if not self.validate_message(message):
            return False
            
        try:
            # Initialize D-Bus
            dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
            
            # Get BlueZ manager
            self.manager = get_global_bluez_manager()
            
            # Create advertisement data
            manufacturer_data = self.create_advertisement_data(message)
            
            # Create advertisement
            self.advertisement = Advertisement(
                local_name="Laptop-BLE-Bridge",
                service_uuids=[config.service_uuid],
                manufacturer_data=manufacturer_data,
                include_tx_power=True
            )
            
            # Register advertisement
            await self.manager.register_advertisement(self.advertisement)
            
            self.current_message = message
            self.is_advertising = True
            
            timestamp = datetime.now().strftime('%H:%M:%S')
            print(f"[{timestamp}] ✅ Started advertising: '{config.message_prefix}{message}'")
            print("📱 Your Android app in CLIENT mode should now see this message!")
            print("🛑 Press Ctrl+C to stop")
            
            return True
            
        except Exception as e:
            print(f"❌ Failed to start advertising: {e}")
            print("💡 Try running with sudo: sudo python3 laptop_broadcaster.py")
            print("💡 Make sure Bluetooth is enabled and not in use by other apps")
            return False
            
    async def stop_advertising(self):
        """Stop BLE advertising."""
        if self.advertisement and self.manager:
            try:
                await self.manager.unregister_advertisement(self.advertisement)
                self.is_advertising = False
                print("🛑 Stopped advertising")
            except Exception as e:
                print(f"⚠️ Error stopping advertisement: {e}")
                
    async def interactive_mode(self):
        """Interactive mode for sending multiple messages."""
        self.print_header()
        print("🎮 Interactive Mode - Type messages to broadcast")
        print("💡 Commands: 'quit' to exit, 'status' for info")
        print()
        
        while True:
            try:
                message = input("📝 Enter message to broadcast: ").strip()
                
                if message.lower() in ['quit', 'exit', 'q']:
                    break
                elif message.lower() == 'status':
                    if self.is_advertising:
                        print(f"📡 Currently broadcasting: '{config.message_prefix}{self.current_message}'")
                    else:
                        print("🔴 Not currently advertising")
                    continue
                elif not message:
                    print("⚠️ Empty message, try again")
                    continue
                    
                # Stop current advertising
                if self.is_advertising:
                    await self.stop_advertising()
                    await asyncio.sleep(0.5)  # Brief pause
                    
                # Start new advertising
                await self.start_advertising(message)
                
            except KeyboardInterrupt:
                break
            except EOFError:
                break
                
        await self.stop_advertising()
        print("👋 Goodbye!")

async def main():
    """Main function."""
    parser = argparse.ArgumentParser(
        description='Laptop BLE Broadcaster - Send messages to Android app',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python laptop_broadcaster.py "Hello Android"     # Broadcast single message
  python laptop_broadcaster.py --interactive       # Interactive mode
  sudo python3 laptop_broadcaster.py "Test"        # With sudo if needed

Setup:
  1. Put Android app in CLIENT mode (📱 CLIENT button)
  2. Start scanning on Android app
  3. Run this script to broadcast messages
  4. Android app should receive and display messages
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
    
    broadcaster = LaptopBLEBroadcaster()
    
    # Handle Ctrl+C gracefully
    def signal_handler(signum, frame):
        print("\n🛑 Stopping broadcaster...")
        asyncio.create_task(broadcaster.stop_advertising())
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    
    if args.interactive:
        await broadcaster.interactive_mode()
    elif args.message:
        broadcaster.print_header()
        success = await broadcaster.start_advertising(args.message)
        if success:
            try:
                # Keep advertising until interrupted
                while True:
                    await asyncio.sleep(1)
            except KeyboardInterrupt:
                await broadcaster.stop_advertising()
                print("👋 Goodbye!")
    else:
        print("❌ Error: Provide a message or use --interactive mode")
        print("💡 Usage: python laptop_broadcaster.py 'Your message'")
        print("💡 Or: python laptop_broadcaster.py --interactive")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")
    except Exception as e:
        print(f"❌ Error: {e}")
        print("💡 Try running with sudo if you get permission errors")
        print("💡 Make sure Bluetooth is enabled and BlueZ is installed")
