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
    python simple_scanner.py --prefix "APP:"  # Use custom prefix
    python simple_scanner.py --config         # Show current configuration
"""

import asyncio
import sys
import argparse
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
        max_user_message_length = 11
    config = DefaultConfig()

# Statistics
app_messages_received = 0
other_messages_filtered = 0

def print_header(message_prefix):
    """Print simple header with current configuration."""
    print("🔍 Simple BLE Scanner - App Messages Only")
    print(f"📱 Prefix: '{message_prefix}' | Max user message: {config.max_user_message_length} chars")
    print(f"🎯 Service UUID: {config.service_uuid}")
    print("🛑 Press Ctrl+C to stop")
    print("-" * 60)

def detection_callback(device: BLEDevice, advertisement_data: AdvertisementData):
    """Handle detected BLE advertisements."""
    global app_messages_received, other_messages_filtered
    
    # Extract manufacturer data first (most reliable method)
    manufacturer_data = advertisement_data.manufacturer_data or {}
    if config.apple_company_id not in manufacturer_data:
        return
    
    try:
        data = manufacturer_data[config.apple_company_id]
        full_message = data.decode('utf-8', errors='ignore').rstrip('\x00')
        
        # Only show app messages (with our prefix)
        if full_message.startswith(config.message_prefix):
            user_message = full_message[len(config.message_prefix):]
            timestamp = datetime.now().strftime('%H:%M:%S')
            device_name = device.name or "Unknown Device"
            rssi = advertisement_data.rssi
            
            # Signal strength indicator
            if rssi >= -50:
                signal_icon = "🟢"  # Strong
            elif rssi >= -70:
                signal_icon = "🟡"  # Medium
            else:
                signal_icon = "🔴"  # Weak
            
            app_messages_received += 1
            print(f"[{timestamp}] 📡 \"{user_message}\" from {device_name} {signal_icon} {rssi}dBm")
        else:
            # Count filtered messages but don't display them
            other_messages_filtered += 1
            
    except Exception:
        pass  # Ignore decode errors in simple mode

async def main():
    """Main scanning function."""
    parser = argparse.ArgumentParser(description='Simple BLE Scanner for App Messages')
    parser.add_argument('--prefix', help='Override message prefix (e.g., "APP:")')
    parser.add_argument('--config', action='store_true', help='Show current configuration and exit')
    args = parser.parse_args()
    
    # Handle config display
    if args.config:
        print("🔧 Current BLE Scanner Configuration:")
        print(f"   Message Prefix: '{config.message_prefix}'")
        print(f"   Service UUID: {config.service_uuid}")
        print(f"   Company ID: 0x{config.apple_company_id:04X}")
        print(f"   Max Total Length: {config.max_total_message_length}")
        print(f"   Max User Length: {config.max_user_message_length}")
        print(f"   Debug Mode: {config.debug_mode}")
        print("\n💡 To change prefix: python simple_scanner.py --prefix 'NEW:'")
        print("💡 To update config: python config_helper.py --prefix 'NEW:'")
        return
    
    # Override prefix if specified
    message_prefix = args.prefix if args.prefix else config.message_prefix
    if args.prefix:
        print(f"🔄 Using custom prefix: '{args.prefix}' (not saved to config)")
        print()
    
    print_header(message_prefix)
    
    def custom_callback(device: BLEDevice, advertisement_data: AdvertisementData):
        """Custom callback with potentially overridden prefix."""
        global app_messages_received, other_messages_filtered
        
        manufacturer_data = advertisement_data.manufacturer_data or {}
        if config.apple_company_id not in manufacturer_data:
            return
        
        try:
            data = manufacturer_data[config.apple_company_id]
            full_message = data.decode('utf-8', errors='ignore').rstrip('\x00')
            
            # Use the potentially overridden prefix
            if full_message.startswith(message_prefix):
                user_message = full_message[len(message_prefix):]
                timestamp = datetime.now().strftime('%H:%M:%S')
                device_name = device.name or "Unknown Device"
                rssi = advertisement_data.rssi
                
                # Signal strength indicator
                if rssi >= -50:
                    signal_icon = "🟢"
                elif rssi >= -70:
                    signal_icon = "🟡"
                else:
                    signal_icon = "🔴"
                
                app_messages_received += 1
                print(f"[{timestamp}] 📡 \"{user_message}\" from {device_name} {signal_icon} {rssi}dBm")
            else:
                other_messages_filtered += 1
                
        except Exception:
            pass
    
    try:
        scanner = BleakScanner(custom_callback)
        await scanner.start()
        
        try:
            while True:
                await asyncio.sleep(1)
        except KeyboardInterrupt:
            await scanner.stop()
            
    except Exception as e:
        print(f"❌ Error: {e}")
        print("💡 Try: sudo python3 simple_scanner.py (on Linux)")
        print("💡 Check: Bluetooth is enabled and working")
        return
    
    print(f"\n📊 Statistics:")
    print(f"   📡 App messages received: {app_messages_received}")
    print(f"   🚫 Other messages filtered: {other_messages_filtered}")
    print("👋 Goodbye!")

if __name__ == "__main__":
    asyncio.run(main())
