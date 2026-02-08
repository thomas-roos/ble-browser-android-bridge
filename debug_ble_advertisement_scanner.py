import asyncio
from bleak import BleakScanner

# --- CONFIGURATION ---
# If you have multiple Bluetooth adapters, you can specify which one to use here.
# On Linux, these are typically 'hci0', 'hci1', etc.
# Set to None to use the default adapter.
ADAPTER_DEVICE = None # or "hci0" or "hci1"
# ---------------------

# The manufacturer ID from the Android app
MANUFACTURER_ID = 0xFFFF

# Keep track of the last seen PDF name to avoid spamming the console
last_seen_pdf = None

def detection_callback(device, advertisement_data):
    """Called whenever a new advertisement is received."""
    global last_seen_pdf

    manufacturer_data = advertisement_data.manufacturer_data
    if MANUFACTURER_ID in manufacturer_data:
        pdf_name_bytes = manufacturer_data[MANUFACTURER_ID]
        try:
            pdf_name = pdf_name_bytes.decode('utf-8')

            if pdf_name != last_seen_pdf:
                print(f"Received PDF Name via Advertisement: '{pdf_name}' from {device.address}")
                last_seen_pdf = pdf_name
        except UnicodeDecodeError:
            print(f"Received undecodeable data from {device.address}")

async def run_scanner():
    """
    Creates a BleakScanner and runs it indefinitely, using the
    detection_callback to process advertisements.
    """
    print(f"Using Bluetooth adapter: {ADAPTER_DEVICE or 'default'}")
    scanner = BleakScanner(detection_callback=detection_callback, adapter=ADAPTER_DEVICE)

    print("Scanning for advertisements... Press Ctrl+C to stop.")
    await scanner.start()
    while True:
        await asyncio.sleep(5.0)

async def main():
    await run_scanner()

if __name__ == "__main__":
    print("--- BLE Advertisement Scanner for PDF Sync App ---")
    print("This script scans for BLE advertisements from your Android app.")

    if ADAPTER_DEVICE is None:
        print("\nNOTE: Using the default Bluetooth adapter. If you have multiple adapters, you may need to edit this script and set the ADAPTER_DEVICE variable (e.g., to 'hci0').")

    try:
        asyncio.run(main())
    except (KeyboardInterrupt, asyncio.CancelledError):
        print("\nScanner stopped.")
