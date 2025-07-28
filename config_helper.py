#!/usr/bin/env python3
"""
BLE Configuration Helper
========================

Helper script to view and modify BLE configuration settings,
especially the message prefix used for filtering.

Usage:
    python config_helper.py                    # Show current config
    python config_helper.py --prefix "APP:"   # Change prefix to "APP:"
    python config_helper.py --reset           # Reset to defaults
"""

import argparse
import os
import sys
from ble_config import config

def show_current_config():
    """Display current configuration."""
    print("🔧 Current BLE Configuration:")
    print("=" * 50)
    config.print_config()
    print("=" * 50)
    print(f"📏 Max user message length: {config.max_user_message_length} chars")
    print(f"📦 Config file: {config.config_file}")
    print()

def update_prefix(new_prefix):
    """Update the message prefix in config file."""
    if not new_prefix:
        print("❌ Error: Prefix cannot be empty")
        return False
    
    if len(new_prefix) >= config.max_total_message_length:
        print(f"❌ Error: Prefix too long! Max {config.max_total_message_length-1} chars")
        return False
    
    # Calculate new max user message length
    new_max_user_length = config.max_total_message_length - len(new_prefix)
    
    if new_max_user_length < 1:
        print(f"❌ Error: Prefix '{new_prefix}' leaves no room for user message!")
        return False
    
    print(f"🔄 Updating prefix from '{config.message_prefix}' to '{new_prefix}'")
    print(f"📏 New max user message length: {new_max_user_length} chars")
    
    # Read current config file
    config_lines = []
    if os.path.exists(config.config_file):
        with open(config.config_file, 'r') as f:
            config_lines = f.readlines()
    
    # Update or add the prefix line
    prefix_updated = False
    new_config_lines = []
    
    for line in config_lines:
        if line.strip().startswith('MESSAGE_PREFIX='):
            new_config_lines.append(f'MESSAGE_PREFIX={new_prefix}\n')
            prefix_updated = True
        else:
            new_config_lines.append(line)
    
    # If prefix wasn't found, add it
    if not prefix_updated:
        new_config_lines.append(f'MESSAGE_PREFIX={new_prefix}\n')
    
    # Write updated config
    try:
        with open(config.config_file, 'w') as f:
            f.writelines(new_config_lines)
        
        print(f"✅ Configuration updated successfully!")
        print(f"📝 Updated {config.config_file}")
        print()
        print("⚠️  IMPORTANT: You need to update the Android app manually:")
        print(f"   1. Open MainActivity.kt")
        print(f"   2. Change: private val MESSAGE_PREFIX = \"{config.message_prefix}\"")
        print(f"   3. To:     private val MESSAGE_PREFIX = \"{new_prefix}\"")
        print(f"   4. Rebuild and reinstall the Android app")
        print()
        print("🔄 Python scripts will use the new prefix automatically")
        
        return True
        
    except Exception as e:
        print(f"❌ Error writing config file: {e}")
        return False

def reset_config():
    """Reset configuration to defaults."""
    print("🔄 Resetting configuration to defaults...")
    
    default_config = """# BLE Browser-Android Bridge Configuration
# ========================================
# This file contains shared configuration settings for all components
# (Android app, web client, Python debug tools)

# Message prefix to identify app-specific BLE advertisements
# This prefix is automatically added to all outgoing messages and
# used to filter incoming messages from other BLE devices
MESSAGE_PREFIX=BLE:

# BLE Service UUID used for GATT communication
SERVICE_UUID=12345678-1234-1234-1234-123456789abc

# BLE Characteristic UUID for read/write operations
CHARACTERISTIC_UUID=87654321-4321-4321-4321-cba987654321

# Apple Company ID used for manufacturer data in BLE advertisements
# Using Apple's ID (0x004C) for broader compatibility
APPLE_COMPANY_ID=0x004C

# Maximum message length constraints
# Total BLE advertisement payload is limited to ~31 bytes
# After accounting for service UUID, flags, and other overhead,
# we have approximately 15 bytes for the actual message
MAX_TOTAL_MESSAGE_LENGTH=15

# Debug and logging settings
DEBUG_MODE=true
LOG_TIMESTAMPS=true
SHOW_RSSI_DETAILS=true

# Scanning and advertising settings
SCAN_MODE=LOW_LATENCY
ADVERTISE_MODE=LOW_LATENCY
TX_POWER_LEVEL=HIGH

# Connection settings
CONNECTION_TIMEOUT_MS=5000
SCAN_TIMEOUT_MS=10000
"""
    
    try:
        with open(config.config_file, 'w') as f:
            f.write(default_config)
        
        print("✅ Configuration reset to defaults!")
        print(f"📝 Updated {config.config_file}")
        print()
        print("⚠️  Remember to update the Android app if you changed the prefix:")
        print("   • Set MESSAGE_PREFIX = \"BLE:\" in MainActivity.kt")
        print("   • Rebuild and reinstall the app")
        
        return True
        
    except Exception as e:
        print(f"❌ Error resetting config: {e}")
        return False

def validate_android_sync():
    """Check if Android app might be out of sync."""
    print("🔍 Android App Sync Check:")
    print("=" * 40)
    print("To ensure the Android app uses the same prefix:")
    print()
    print("1. Open: android-app/app/src/main/java/com/github/blebrowserbridge/MainActivity.kt")
    print(f"2. Find: private val MESSAGE_PREFIX = \"...\"")
    print(f"3. Should be: private val MESSAGE_PREFIX = \"{config.message_prefix}\"")
    print("4. If different, update and rebuild the app")
    print()

def main():
    """Main function."""
    parser = argparse.ArgumentParser(
        description="BLE Configuration Helper",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python config_helper.py                    # Show current config
  python config_helper.py --prefix "APP:"   # Change prefix to "APP:"
  python config_helper.py --prefix "TEST:"  # Change prefix to "TEST:"
  python config_helper.py --reset           # Reset to defaults
  python config_helper.py --check           # Check Android sync
        """
    )
    
    parser.add_argument('--prefix', type=str, help='Set new message prefix')
    parser.add_argument('--reset', action='store_true', help='Reset config to defaults')
    parser.add_argument('--check', action='store_true', help='Check Android app sync')
    
    args = parser.parse_args()
    
    if args.reset:
        if reset_config():
            print("\n" + "="*50)
            # Reload config to show new values
            from ble_config import BLEConfig
            new_config = BLEConfig()
            new_config.print_config()
        return
    
    if args.prefix:
        if update_prefix(args.prefix):
            print("\n" + "="*50)
            # Reload config to show new values
            from ble_config import BLEConfig
            new_config = BLEConfig()
            new_config.print_config()
        return
    
    if args.check:
        validate_android_sync()
        return
    
    # Default: show current config
    show_current_config()
    
    print("💡 Usage tips:")
    print("   • Use --prefix to change the message prefix")
    print("   • Use --reset to restore defaults")
    print("   • Use --check to verify Android app sync")
    print("   • Remember to update Android app after changing prefix")

if __name__ == "__main__":
    main()
