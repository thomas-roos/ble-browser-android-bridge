package com.github.blebrowserbridge

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.text.Editable
import android.text.TextWatcher
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
    
    // Server mode (broadcasting)
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var isAdvertising = false
    
    // Client mode (scanning)
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private var scanCallback: ScanCallback? = null
    
    private var currentMessage = ""  // Start with empty message
    private var currentMode = Mode.SERVER
    
    // Configuration - can be modified here to change prefix
    private val MESSAGE_PREFIX = "BLE:"  // Change this to customize the prefix
    private val MAX_TOTAL_MESSAGE_LENGTH = 15  // BLE advertisement size limit
    private val MAX_USER_MESSAGE_LENGTH = MAX_TOTAL_MESSAGE_LENGTH - MESSAGE_PREFIX.length
    
    enum class Mode {
        SERVER, CLIENT
    }

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
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        
        if (bluetoothLeAdvertiser == null) {
            addMessage("❌ BLE advertising not supported on this device")
        }
        
        if (bluetoothLeScanner == null) {
            addMessage("❌ BLE scanning not supported on this device")
        }

        setupUI()
        checkAndRequestPermissions()
    }

    private fun setupUI() {
        // Set up message input with character counter
        binding.messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                val remaining = MAX_USER_MESSAGE_LENGTH - length
                
                if (remaining >= 0) {
                    binding.characterCounter.text = "$length/$MAX_USER_MESSAGE_LENGTH"
                    binding.characterCounter.setTextColor(getColor(android.R.color.darker_gray))
                } else {
                    binding.characterCounter.text = "TOO LONG! ($length/$MAX_USER_MESSAGE_LENGTH)"
                    binding.characterCounter.setTextColor(getColor(android.R.color.holo_red_dark))
                }
                
                // Enable/disable send button based on length
                binding.sendMessageButton.isEnabled = remaining >= 0 && length > 0
            }
        })
        
        // Mode toggle buttons
        binding.serverModeButton.setOnClickListener {
            switchToServerMode()
        }
        
        binding.clientModeButton.setOnClickListener {
            switchToClientMode()
        }
        
        // Main action button (context-sensitive)
        binding.startServerButton.setOnClickListener {
            when (currentMode) {
                Mode.SERVER -> {
                    if (isAdvertising) {
                        stopServerMode()
                    } else {
                        if (hasAllRequiredPermissions()) {
                            startServerMode()
                        } else {
                            addMessage("❌ Missing permissions - requesting now...")
                            checkAndRequestPermissions()
                        }
                    }
                }
                Mode.CLIENT -> {
                    if (isScanning) {
                        stopClientMode()
                    } else {
                        if (hasAllRequiredPermissions()) {
                            startClientMode()
                        } else {
                            addMessage("❌ Missing permissions - requesting now...")
                            checkAndRequestPermissions()
                        }
                    }
                }
            }
        }

        // Message input (for server mode)
        binding.sendMessageButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isNotEmpty() && message.length <= MAX_USER_MESSAGE_LENGTH) {
                updateMessage(message)
                binding.messageInput.text.clear()
            } else if (message.length > MAX_USER_MESSAGE_LENGTH) {
                Toast.makeText(this, "Message too long! Max $MAX_USER_MESSAGE_LENGTH characters", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize in server mode
        switchToServerMode()
        
        addMessage("📡 BLE Dual-Mode App with Message Filtering")
        addMessage("🔄 Switch between Server (broadcast) and Client (scan) modes")
        addMessage("🏷️ Messages use '$MESSAGE_PREFIX' prefix for filtering")
        addMessage("⚠️ User messages limited to $MAX_USER_MESSAGE_LENGTH characters")
        addMessage("")
    }

    private fun switchToServerMode() {
        currentMode = Mode.SERVER
        stopClientMode() // Stop client if running
        
        binding.serverModeButton.isEnabled = false
        binding.clientModeButton.isEnabled = true
        
        addMessage("📡 Switched to SERVER mode")
        addMessage("• Broadcast messages with '$MESSAGE_PREFIX' prefix")
        addMessage("• Other devices can scan and receive your messages")
        
        updateUI()
    }

    private fun switchToClientMode() {
        currentMode = Mode.CLIENT
        stopServerMode() // Stop server if running
        
        binding.serverModeButton.isEnabled = true
        binding.clientModeButton.isEnabled = false
        
        addMessage("📱 Switched to CLIENT mode")
        addMessage("• Scan for BLE advertisements with '$MESSAGE_PREFIX' prefix")
        addMessage("• Filter out non-app messages")
        
        updateUI()
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
            addMessage("Ready to start ${currentMode.name.lowercase()} mode")
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
                addMessage("🎉 All permissions granted! Ready to start.")
            }
        }
    }

    // SERVER MODE FUNCTIONS
    private fun startServerMode() {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Cannot start server - missing permissions")
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
                        runOnUiThread {
                            addMessage("🌐 Device connected: $deviceName")
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        runOnUiThread {
                            addMessage("🔌 Device disconnected: $deviceName")
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
                // For GATT, send the message with prefix
                val fullMessage = MESSAGE_PREFIX + currentMessage
                bluetoothGattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, fullMessage.toByteArray()
                )
                runOnUiThread {
                    addMessage("📖 Device read: \"$fullMessage\"")
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
                val receivedMessage = String(value ?: byteArrayOf())
                // Remove prefix if present
                val userMessage = if (receivedMessage.startsWith(MESSAGE_PREFIX)) {
                    receivedMessage.substring(MESSAGE_PREFIX.length)
                } else {
                    receivedMessage
                }
                
                updateMessage(userMessage)

                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null
                    )
                }

                runOnUiThread {
                    addMessage("📝 Device updated message: \"$userMessage\"")
                }
            }
        }

        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)

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
    }

    private fun startAdvertising() {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Cannot start advertising - missing permissions")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        // Add prefix to message for advertisement
        val fullMessage = MESSAGE_PREFIX + currentMessage
        val messageBytes = fullMessage.toByteArray()
        
        addMessage("📏 Full message: \"$fullMessage\" (${messageBytes.size} bytes)")
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Save space for message
            .setIncludeTxPowerLevel(false) // Save space for message
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addManufacturerData(0x004C, messageBytes)
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                isAdvertising = true
                runOnUiThread {
                    addMessage("📡 SERVER: Broadcasting \"$fullMessage\"")
                    addMessage("🚀 Laptop scanner can now detect this message")
                    updateUI()
                }
            }

            override fun onStartFailure(errorCode: Int) {
                isAdvertising = false
                runOnUiThread {
                    val errorMsg = when (errorCode) {
                        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising not supported"
                        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many BLE advertisers running"
                        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Message still too large! Try shorter message"
                        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error"
                        else -> "Unknown error (code: $errorCode)"
                    }
                    addMessage("❌ SERVER: Advertising failed: $errorMsg")
                    
                    if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                        addMessage("💡 Try a message with 8 characters or less")
                        Toast.makeText(this@MainActivity, "Message too large! Try shorter text", Toast.LENGTH_LONG).show()
                    }
                    
                    updateUI()
                }
            }
        }

        bluetoothLeAdvertiser?.startAdvertising(settings, data, callback)
    }

    private fun stopServerMode() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED || 
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        }
        
        bluetoothGattServer?.close()
        bluetoothGattServer = null
        isAdvertising = false
        
        addMessage("🛑 SERVER: Stopped broadcasting")
        updateUI()
    }

    // CLIENT MODE FUNCTIONS
    private fun startClientMode() {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Cannot start client - missing permissions")
            checkAndRequestPermissions()
            return
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
        )

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi
                val scanRecord = result.scanRecord
                
                val deviceName = if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.name ?: "Unknown Device"
                } else {
                    "Device ${device.address}"
                }

                // Extract message from manufacturer data
                var message = "No message data"
                scanRecord?.getManufacturerSpecificData(0x004C)?.let { data ->
                    try {
                        val fullMessage = String(data, Charsets.UTF_8)
                        // Check for our prefix and remove it
                        if (fullMessage.startsWith(MESSAGE_PREFIX)) {
                            message = fullMessage.substring(MESSAGE_PREFIX.length)
                        } else {
                            message = "Non-app message: $fullMessage"
                        }
                    } catch (e: Exception) {
                        message = "Invalid message data"
                    }
                }

                runOnUiThread {
                    if (message.startsWith("Non-app message:")) {
                        addMessage("🔍 CLIENT: $message")
                    } else {
                        addMessage("📡 CLIENT: Received \"$message\"")
                    }
                    addMessage("   └─ From: $deviceName (RSSI: ${rssi}dBm)")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                runOnUiThread {
                    val errorMsg = when (errorCode) {
                        SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                        SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                        SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning not supported"
                        SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                        else -> "Unknown error (code: $errorCode)"
                    }
                    addMessage("❌ CLIENT: Scan failed: $errorMsg")
                    isScanning = false
                    updateUI()
                }
            }
        }

        bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
        isScanning = true
        
        addMessage("🔍 CLIENT: Scanning for BLE advertisements...")
        addMessage("📱 Looking for messages with '$MESSAGE_PREFIX' prefix...")
        updateUI()
    }

    private fun stopClientMode() {
        scanCallback?.let { callback ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || 
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothLeScanner?.stopScan(callback)
            }
        }
        scanCallback = null
        isScanning = false
        
        addMessage("🛑 CLIENT: Stopped scanning")
        updateUI()
    }

    private fun updateMessage(newMessage: String) {
        val safeMessage = newMessage.take(MAX_USER_MESSAGE_LENGTH)
        
        if (newMessage.length > MAX_USER_MESSAGE_LENGTH) {
            addMessage("⚠️ Message truncated to $MAX_USER_MESSAGE_LENGTH characters")
        }
        
        currentMessage = safeMessage
        addMessage("📤 Updated message: \"$currentMessage\"")
        addMessage("📡 Will broadcast as: \"$MESSAGE_PREFIX$currentMessage\"")
        
        if (currentMode == Mode.SERVER && isAdvertising) {
            addMessage("🔄 Restarting advertising with new message...")
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
            
            binding.root.postDelayed({
                startAdvertising()
            }, 100)
        }
    }

    private fun addMessage(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formattedMessage = "[$timestamp] $message\n"
        binding.messagesText.append(formattedMessage)
        
        binding.scrollView.post {
            binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun updateUI() {
        when (currentMode) {
            Mode.SERVER -> {
                binding.startServerButton.text = if (isAdvertising) "Stop Broadcasting" else "Start Broadcasting"
                binding.statusText.text = if (isAdvertising) {
                    "Status: 📡 SERVER - Broadcasting \"$MESSAGE_PREFIX$currentMessage\""
                } else {
                    "Status: 🔴 SERVER - Not Broadcasting"
                }
                binding.sendMessageButton.isEnabled = binding.messageInput.text.length <= MAX_USER_MESSAGE_LENGTH && binding.messageInput.text.isNotEmpty()
                binding.messageInput.isEnabled = true
                binding.sendMessageButton.text = if (isAdvertising) "Update Message" else "Set Message"
                binding.messageInput.hint = "Message to broadcast (max $MAX_USER_MESSAGE_LENGTH chars)"
            }
            Mode.CLIENT -> {
                binding.startServerButton.text = if (isScanning) "Stop Scanning" else "Start Scanning"
                binding.statusText.text = if (isScanning) {
                    "Status: 📱 CLIENT - Scanning for '$MESSAGE_PREFIX' messages"
                } else {
                    "Status: 🔴 CLIENT - Not Scanning"
                }
                binding.sendMessageButton.isEnabled = false
                binding.messageInput.isEnabled = false
                binding.sendMessageButton.text = "Send Message"
                binding.messageInput.hint = "Message input (SERVER mode only)"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServerMode()
        stopClientMode()
    }
}
