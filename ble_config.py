#!/usr/bin/env python3
"""
BLE Configuration Reader
========================

Shared configuration reader for all Python BLE tools.
Reads settings from config.properties file.
"""

import os
from typing import Dict, Any

class BLEConfig:
    """Configuration reader for BLE Bridge project."""
    
    def __init__(self, config_file: str = "config.properties"):
        """Initialize configuration reader.
        
        Args:
            config_file: Path to configuration file
        """
        self.config_file = config_file
        self.config = self._load_config()
    
    def _load_config(self) -> Dict[str, str]:
        """Load configuration from properties file."""
        config = {}
        
        # Default values
        defaults = {
            'MESSAGE_PREFIX': 'BLE:',
            'SERVICE_UUID': '12345678-1234-1234-1234-123456789abc',
            'CHARACTERISTIC_UUID': '87654321-4321-4321-4321-cba987654321',
            'APPLE_COMPANY_ID': '0x004C',
            'MAX_TOTAL_MESSAGE_LENGTH': '15',
            'DEBUG_MODE': 'true',
            'LOG_TIMESTAMPS': 'true',
            'SHOW_RSSI_DETAILS': 'true'
        }
        
        # Start with defaults
        config.update(defaults)
        
        # Try to read from file
        if os.path.exists(self.config_file):
            try:
                with open(self.config_file, 'r') as f:
                    for line in f:
                        line = line.strip()
                        if line and not line.startswith('#'):
                            if '=' in line:
                                key, value = line.split('=', 1)
                                config[key.strip()] = value.strip()
            except Exception as e:
                print(f"Warning: Could not read config file {self.config_file}: {e}")
                print("Using default values...")
        else:
            print(f"Config file {self.config_file} not found, using defaults")
        
        return config
    
    @property
    def message_prefix(self) -> str:
        """Get the message prefix for filtering."""
        return self.config.get('MESSAGE_PREFIX', 'BLE:')
    
    @property
    def service_uuid(self) -> str:
        """Get the BLE service UUID."""
        return self.config.get('SERVICE_UUID', '12345678-1234-1234-1234-123456789abc')
    
    @property
    def characteristic_uuid(self) -> str:
        """Get the BLE characteristic UUID."""
        return self.config.get('CHARACTERISTIC_UUID', '87654321-4321-4321-4321-cba987654321')
    
    @property
    def apple_company_id(self) -> int:
        """Get the Apple company ID as integer."""
        hex_str = self.config.get('APPLE_COMPANY_ID', '0x004C')
        try:
            return int(hex_str, 16) if hex_str.startswith('0x') else int(hex_str)
        except ValueError:
            return 0x004C
    
    @property
    def max_total_message_length(self) -> int:
        """Get the maximum total message length."""
        try:
            return int(self.config.get('MAX_TOTAL_MESSAGE_LENGTH', '15'))
        except ValueError:
            return 15
    
    @property
    def max_user_message_length(self) -> int:
        """Get the maximum user message length (total - prefix)."""
        return self.max_total_message_length - len(self.message_prefix)
    
    @property
    def debug_mode(self) -> bool:
        """Check if debug mode is enabled."""
        return self.config.get('DEBUG_MODE', 'true').lower() == 'true'
    
    @property
    def log_timestamps(self) -> bool:
        """Check if timestamps should be logged."""
        return self.config.get('LOG_TIMESTAMPS', 'true').lower() == 'true'
    
    @property
    def show_rssi_details(self) -> bool:
        """Check if RSSI details should be shown."""
        return self.config.get('SHOW_RSSI_DETAILS', 'true').lower() == 'true'
    
    def get(self, key: str, default: Any = None) -> str:
        """Get a configuration value by key."""
        return self.config.get(key, default)
    
    def print_config(self):
        """Print current configuration."""
        print("🔧 BLE Configuration:")
        print(f"   Message Prefix: '{self.message_prefix}'")
        print(f"   Service UUID: {self.service_uuid}")
        print(f"   Company ID: 0x{self.apple_company_id:04X}")
        print(f"   Max Total Length: {self.max_total_message_length}")
        print(f"   Max User Length: {self.max_user_message_length}")
        print(f"   Debug Mode: {self.debug_mode}")

# Global configuration instance
config = BLEConfig()

if __name__ == "__main__":
    # Test the configuration reader
    config.print_config()
