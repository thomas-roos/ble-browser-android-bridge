package com.github.blebrowserbridge

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.blebrowserbridge.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var isAdvertising = false
    private var currentMessage = "Hello from Android!"

    companion object {
        private const val TAG = "BLEBridge"
        private const val REQUEST_PERMISSIONS = 1001
        
        val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val CHAR_UUID: UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321")
        
        private fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ (API 31+) - includes Android 16
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                // Android 11 and below
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            addMessage("❌ Bluetooth is disabled")
            addMessage("Please enable Bluetooth in Android Settings")
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            addMessage("❌ BLE advertising not supported on this device")
            Toast.makeText(this, "BLE advertising not supported", Toast.LENGTH_LONG).show()
            return
        }

        setupUI()
        checkAndRequestPermissions()
    }

    private fun setupUI() {
        binding.startServerButton.setOnClickListener {
            if (isAdvertising) {
                stopBleServices()
            } else {
                if (hasAllRequiredPermissions()) {
                    startBleServices()
                } else {
                    addMessage("❌ Missing permissions - requesting now...")
                    checkAndRequestPermissions()
                }
            }
        }

        binding.sendMessageButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotEmpty()) {
                updateMessage(message)
                binding.messageInput.text.clear()
            }
        }

        updateUI()
        addMessage("📡 BLE Advertisement + GATT Server")
        addMessage("This app broadcasts messages AND allows browser connections")
        addMessage("• Advertisements: Broadcast messages (like COVID apps)")
        addMessage("• GATT Server: Allow browser to connect and read/write")
        addMessage("")
        addMessage("⚠️ Advertisement messages limited to ~18 characters")
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            addMessage("🔐 Requesting ${missingPermissions.size} permissions...")
            missingPermissions.forEach { permission ->
                val permissionName = permission.substringAfterLast(".").replace("_", " ")
                addMessage("  • $permissionName")
            }
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            addMessage("✅ All permissions granted!")
            addMessage("Ready to start BLE services")
        }
    }

    private fun hasAllRequiredPermissions(): Boolean {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val grantedPermissions = mutableListOf<String>()
            val deniedPermissions = mutableListOf<String>()
            
            permissions.forEachIndexed { index, permission ->
                val permissionName = permission.substringAfterLast(".").replace("_", " ")
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    grantedPermissions.add(permissionName)
                } else {
                    deniedPermissions.add(permissionName)
                }
            }
            
            if (grantedPermissions.isNotEmpty()) {
                addMessage("✅ Granted permissions:")
                grantedPermissions.forEach { addMessage("  • $it") }
            }
            
            if (deniedPermissions.isNotEmpty()) {
                addMessage("❌ Denied permissions:")
                deniedPermissions.forEach { addMessage("  • $it") }
                addMessage("")
                addMessage("⚠️ To fix this:")
                addMessage("1. Go to Android Settings")
                addMessage("2. Apps → BLE Browser Bridge")
                addMessage("3. Permissions → Enable ALL permissions")
                addMessage("4. Return to this app and try again")
                
                Toast.makeText(this, "Please grant ALL permissions in Settings", Toast.LENGTH_LONG).show()
            }
            
            if (deniedPermissions.isEmpty()) {
                addMessage("🎉 All permissions granted! Ready to start services.")
            }
        }
    }

    private fun startBleServices() {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Cannot start - missing permissions")
            checkAndRequestPermissions()
            return
        }

        startGattServer()
        startAdvertising()
    }

    private fun startGattServer() {
        val gattServerCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                val deviceName = if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device?.name ?: device?.address ?: "Unknown"
                } else {
                    device?.address ?: "Unknown"
                }
                
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Browser connected: $deviceName")
                        runOnUiThread {
                            addMessage("🌐 Browser connected: $deviceName")
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Browser disconnected: $deviceName")
                        runOnUiThread {
                            addMessage("🔌 Browser disconnected: $deviceName")
                        }
                    }
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?
            ) {
                Log.d(TAG, "Browser reading message: $currentMessage")
                bluetoothGattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, currentMessage.toByteArray()
                )
                runOnUiThread {
                    addMessage("📖 Browser read: \"$currentMessage\"")
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?
            ) {
                val newMessage = String(value ?: byteArrayOf())
                Log.d(TAG, "Browser sent message: $newMessage")

                // Update our current message and restart advertising
                updateMessage(newMessage)

                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null
                    )
                }

                runOnUiThread {
                    addMessage("📝 Browser updated message: \"$newMessage\"")
                }
            }
        }

        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)

        // Create service and characteristic
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or 
            BluetoothGattCharacteristic.PROPERTY_WRITE or
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or 
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(characteristic)
        bluetoothGattServer?.addService(service)

        addMessage("🔧 GATT Server started - browsers can connect")
    }

    private fun startAdvertising() {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Cannot start advertising - missing permissions")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true) // Allow connections for GATT
            .build()

        // Encode message in manufacturer data (limited to ~18 bytes)
        val messageBytes = currentMessage.take(18).toByteArray()
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true) // Include device name for easier identification
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addManufacturerData(0x004C, messageBytes) // Apple company ID for compatibility
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "Advertising started successfully")
                isAdvertising = true
                runOnUiThread {
                    addMessage("📡 Broadcasting: \"$currentMessage\"")
                    addMessage("🚀 BLE Services active!")
                    addMessage("• Advertising message in BLE advertisements")
                    addMessage("• GATT server ready for browser connections")
                    updateUI()
                }
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Advertising failed: $errorCode")
                isAdvertising = false
                runOnUiThread {
                    val errorMsg = when (errorCode) {
                        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising not supported"
                        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many BLE advertisers running"
                        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Message too large (max ~18 characters)"
                        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error"
                        else -> "Unknown error (code: $errorCode)"
                    }
                    addMessage("❌ Advertising failed: $errorMsg")
                    updateUI()
                }
            }
        }

        bluetoothLeAdvertiser?.startAdvertising(settings, data, callback)
    }

    private fun updateMessage(newMessage: String) {
        val truncatedMessage = if (newMessage.length > 18) {
            addMessage("⚠️ Message too long! Truncating to 18 characters...")
            newMessage.take(18)
        } else {
            newMessage
        }
        
        currentMessage = truncatedMessage
        addMessage("📤 Updated message: \"$currentMessage\"")
        
        if (isAdvertising) {
            // Restart advertising with new message
            addMessage("🔄 Restarting advertising with new message...")
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
            
            // Small delay before restarting
            binding.root.postDelayed({
                startAdvertising()
            }, 100)
        }
    }

    private fun stopBleServices() {
        // Stop advertising
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED || 
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        }
        
        // Stop GATT server
        bluetoothGattServer?.close()
        bluetoothGattServer = null
        
        isAdvertising = false
        
        addMessage("🛑 BLE Services stopped")
        addMessage("• Advertising stopped")
        addMessage("• GATT server stopped")
        updateUI()
    }

    private fun addMessage(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedMessage = "[$timestamp] $message\n"
        binding.messagesText.append(formattedMessage)
        
        // Auto-scroll to bottom
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun updateUI() {
        binding.startServerButton.text = if (isAdvertising) "Stop BLE Services" else "Start BLE Services"
        binding.statusText.text = if (isAdvertising) {
            "Status: 📡 Broadcasting \"$currentMessage\" + GATT Server"
        } else {
            "Status: 🔴 Services Stopped"
        }
        binding.sendMessageButton.isEnabled = true // Always enabled
        binding.messageInput.isEnabled = true
        binding.sendMessageButton.text = if (isAdvertising) "Update Message" else "Set Message"
        
        // Show character count hint
        binding.messageInput.hint = "Message (max 18 chars for ads)"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleServices()
    }
}
