#!/usr/bin/env python3
"""
Configuration Test Script
=========================

Quick test to verify configuration system is working correctly.
"""

from ble_config import config

def test_configuration():
    """Test the configuration system."""
    print("🧪 Testing BLE Configuration System")
    print("=" * 40)
    
    # Test basic config loading
    print("✅ Configuration loaded successfully")
    print(f"   Prefix: '{config.message_prefix}'")
    print(f"   Service UUID: {config.service_uuid}")
    print(f"   Company ID: 0x{config.apple_company_id:04X}")
    print()
    
    # Test message length calculations
    print("📏 Message Length Analysis:")
    print(f"   Max total length: {config.max_total_message_length}")
    print(f"   Prefix length: {len(config.message_prefix)}")
    print(f"   Max user message: {config.max_user_message_length}")
    print()
    
    # Test with example messages
    test_messages = ["Hi", "Hello", "Test message", "This is a longer test"]
    
    print("📝 Example Messages:")
    for msg in test_messages:
        full_msg = config.message_prefix + msg
        status = "✅ OK" if len(full_msg) <= config.max_total_message_length else "❌ TOO LONG"
        print(f"   '{msg}' → '{full_msg}' ({len(full_msg)} bytes) {status}")
    print()
    
    # Configuration validation
    print("🔍 Configuration Validation:")
    
    if config.max_user_message_length > 0:
        print("✅ User message length is positive")
    else:
        print("❌ User message length is zero or negative!")
    
    if len(config.message_prefix) < config.max_total_message_length:
        print("✅ Prefix length is reasonable")
    else:
        print("❌ Prefix is too long!")
    
    if config.apple_company_id == 0x004C:
        print("✅ Using Apple company ID (good compatibility)")
    else:
        print(f"⚠️  Using custom company ID: 0x{config.apple_company_id:04X}")
    
    print()
    print("🎯 Recommendations:")
    
    if config.max_user_message_length < 5:
        print("⚠️  Consider using a shorter prefix for longer messages")
        print(f"   Current: '{config.message_prefix}' → {config.max_user_message_length} chars")
        print("   Example: 'A:' → 13 chars, 'BLE:' → 11 chars")
    
    if config.max_user_message_length >= 10:
        print("✅ Good message length capacity")
    
    print()
    print("💡 To change prefix: python config_helper.py --prefix 'NEW:'")
    print("💡 To test scanner: python simple_scanner.py")

if __name__ == "__main__":
    test_configuration()
