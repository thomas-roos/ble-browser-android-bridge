import asyncio
from bleak import BleakClient

# UUIDs from the Android app
SERVICE_UUID = "0000180D-0000-1000-8000-00805F9B34FB"
PDF_NAME_CHARACTERISTIC_UUID = "00002A37-0000-1000-8000-00805F9B34FB"

def notification_handler(sender: int, data: bytearray):
    """Simple notification handler which decodes and prints the data."""
    pdf_name = data.decode('utf-8')
    print(f"Received PDF Name: {pdf_name}")

async def run_client(address):
    """
    Connects to a BLE device at the given address, subscribes to notifications
    for the PDF name characteristic, and prints received names.
    """
    print(f"Attempting to connect to {address}...")
    async with BleakClient(address) as client:
        if client.is_connected:
            print(f"Connected to {address}")
            print(f"Subscribing to notifications on {PDF_NAME_CHARACTERISTIC_UUID}...")

            await client.start_notify(PDF_NAME_CHARACTERISTIC_UUID, notification_handler)

            print("Waiting for notifications... Press Ctrl+C to exit.")
            # Keep the script running to receive notifications
            while client.is_connected:
                await asyncio.sleep(1)
        else:
            print(f"Failed to connect to {address}")

async def main():
    """
    Main function to run the BLE client script.

    You need to replace 'XX:XX:XX:XX:XX:XX' with the actual Bluetooth address
    of your Android device running the app in server mode.

    You can find the address in your device's Bluetooth settings
    under 'Device name & address' or similar.
    """
    # IMPORTANT: Replace this with your Android device's Bluetooth address
    # For example: "12:34:56:78:9A:BC"
    device_address = "XX:XX:XX:XX:XX:XX"

    if device_address == "XX:XX:XX:XX:XX:XX":
        print("Please edit this script and replace 'XX:XX:XX:XX:XX:XX' with your Android device's address.")
        return

    await run_client(device_address)

if __name__ == "__main__":
    print("--- BLE Debug Client for PDF Sync App ---")
    print("This script connects to your Android app running in SERVER mode.")
    print("It will print any PDF names it receives via BLE notifications.")
    print("Ensure the 'bleak' library is installed (`pip install bleak`).")

    try:
        asyncio.run(main())
    except asyncio.CancelledError:
        print("Script stopped.")
