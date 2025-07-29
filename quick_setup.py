#!/usr/bin/env python3
"""
Quick Setup Script for BLE Bridge
=================================

Interactive script to help configure and test the BLE Bridge project.
"""

import os
import sys
import subprocess
from ble_config import config

def print_banner():
    """Print welcome banner."""
    print("🚀 BLE Browser-Android Bridge - Quick Setup")
    print("=" * 50)
    print()

def show_current_status():
    """Show current configuration and status."""
    print("📋 Current Configuration:")
    print(f"   Message Prefix: '{config.message_prefix}'")
    print(f"   Max User Message: {config.max_user_message_length} characters")
    print(f"   Service UUID: {config.service_uuid}")
    print()

def test_scanner():
    """Test the simple scanner."""
    print("🔍 Testing Simple Scanner...")
    print("This will run the simple scanner for 10 seconds to test connectivity.")
    print("Make sure your Android app is running in SERVER mode!")
    print()
    
    input("Press Enter to start scanning (or Ctrl+C to skip)...")
    
    try:
        # Run scanner with timeout
        result = subprocess.run([
            sys.executable, "simple_scanner.py"
        ], timeout=10, capture_output=True, text=True)
        
        print("Scanner output:")
        print(result.stdout)
        if result.stderr:
            print("Errors:")
            print(result.stderr)
            
    except subprocess.TimeoutExpired:
        print("✅ Scanner test completed (10 seconds)")
    except KeyboardInterrupt:
        print("❌ Scanner test cancelled")
    except Exception as e:
        print(f"❌ Error running scanner: {e}")

def change_prefix():
    """Interactive prefix change."""
    print("🔧 Change Message Prefix")
    print(f"Current prefix: '{config.message_prefix}'")
    print(f"Current max user message length: {config.max_user_message_length} chars")
    print()
    
    new_prefix = input("Enter new prefix (e.g., 'APP:', 'TEST:'): ").strip()
    
    if not new_prefix:
        print("❌ Prefix cannot be empty")
        return
    
    if len(new_prefix) >= config.max_total_message_length:
        print(f"❌ Prefix too long! Max {config.max_total_message_length-1} chars")
        return
    
    new_max_length = config.max_total_message_length - len(new_prefix)
    print(f"📏 New max user message length will be: {new_max_length} chars")
    
    confirm = input("Continue? (y/N): ").strip().lower()
    if confirm == 'y':
        try:
            subprocess.run([sys.executable, "config_helper.py", "--prefix", new_prefix], check=True)
            print("✅ Prefix updated successfully!")
            print()
            print("⚠️  IMPORTANT: Update Android app:")
            print(f"   Change MESSAGE_PREFIX to \"{new_prefix}\" in MainActivity.kt")
            print("   Then rebuild and reinstall the app")
        except subprocess.CalledProcessError as e:
            print(f"❌ Error updating prefix: {e}")

def show_android_instructions():
    """Show Android app update instructions."""
    print("📱 Android App Update Instructions")
    print("=" * 40)
    print()
    print("To update the Android app with the current prefix:")
    print()
    print("1. Open: android-app/app/src/main/java/com/github/blebrowserbridge/MainActivity.kt")
    print("2. Find the line:")
    print("   private val MESSAGE_PREFIX = \"...\"")
    print("3. Change it to:")
    print(f"   private val MESSAGE_PREFIX = \"{config.message_prefix}\"")
    print("4. Rebuild the app:")
    print("   cd android-app")
    print("   ./gradlew assembleDebug")
    print("5. Install the new APK on your device")
    print()

def main_menu():
    """Show main menu and handle user choice."""
    while True:
        print("\n🎯 What would you like to do?")
        print("1. Show current configuration")
        print("2. Change message prefix")
        print("3. Test simple scanner")
        print("4. Show Android app update instructions")
        print("5. Reset configuration to defaults")
        print("6. Exit")
        print()
        
        choice = input("Enter choice (1-6): ").strip()
        
        if choice == '1':
            show_current_status()
            
        elif choice == '2':
            change_prefix()
            
        elif choice == '3':
            test_scanner()
            
        elif choice == '4':
            show_android_instructions()
            
        elif choice == '5':
            confirm = input("Reset to defaults? This will set prefix back to 'BLE:' (y/N): ").strip().lower()
            if confirm == 'y':
                try:
                    subprocess.run([sys.executable, "config_helper.py", "--reset"], check=True)
                    print("✅ Configuration reset to defaults!")
                except subprocess.CalledProcessError as e:
                    print(f"❌ Error resetting config: {e}")
                    
        elif choice == '6':
            print("👋 Goodbye!")
            break
            
        else:
            print("❌ Invalid choice. Please enter 1-6.")

def main():
    """Main function."""
    print_banner()
    
    # Check if required files exist
    required_files = ["simple_scanner.py", "config_helper.py", "ble_config.py"]
    missing_files = [f for f in required_files if not os.path.exists(f)]
    
    if missing_files:
        print("❌ Missing required files:")
        for f in missing_files:
            print(f"   • {f}")
        print("\nPlease run this script from the project root directory.")
        return
    
    show_current_status()
    
    print("💡 Quick Tips:")
    print("   • Default prefix 'BLE:' allows 11-character user messages")
    print("   • Shorter prefixes like 'A:' allow longer messages (13 chars)")
    print("   • Always update Android app after changing prefix")
    print("   • Use simple_scanner.py to test message reception")
    
    try:
        main_menu()
    except KeyboardInterrupt:
        print("\n👋 Goodbye!")

if __name__ == "__main__":
    main()
