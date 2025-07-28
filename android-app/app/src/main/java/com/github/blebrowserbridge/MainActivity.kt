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
    private var isAdvertising = false
    private var currentMessage = "Hello from Android!"

    companion object {
        private const val TAG = "BLEBridge"
        private const val REQUEST_PERMISSIONS = 1001
        
        val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        
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
                stopBleAdvertising()
            } else {
                if (hasAllRequiredPermissions()) {
                    startBleAdvertising()
                } else {
                    addMessage("❌ Missing permissions - requesting now...")
                    checkAndRequestPermissions()
                }
            }
        }

        binding.sendMessageButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotEmpty()) {
                updateAdvertisementMessage(message)
                binding.messageInput.text.clear()
            }
        }

        updateUI()
        addMessage("📡 BLE Advertisement Broadcaster")
        addMessage("This app broadcasts messages via BLE advertisements")
        addMessage("Web browsers can scan and receive messages without connecting")
        addMessage("")
        addMessage("⚠️ Note: Messages are limited to ~18 characters due to BLE advertisement size limits")
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
            addMessage("Ready to start BLE advertising")
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
                addMessage("🎉 All permissions granted! Ready to start advertising.")
            }
        }
    }

    private fun startBleAdvertising() {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Cannot start - missing permissions")
            checkAndRequestPermissions()
            return
        }

        startAdvertisingWithMessage(currentMessage)
    }

    private fun startAdvertisingWithMessage(message: String) {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Cannot start advertising - missing permissions")
            return
        }

        // Stop current advertising if running
        if (isAdvertising) {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false) // No connection needed - just advertisement
            .build()

        // Encode message in manufacturer data (limited to ~18 bytes)
        val messageBytes = message.take(18).toByteArray() // Limit message length
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Save space for message
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addManufacturerData(0x004C, messageBytes) // Apple company ID for compatibility
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "Advertising started successfully with message: $message")
                isAdvertising = true
                runOnUiThread {
                    addMessage("📡 Broadcasting: \"$message\"")
                    addMessage("🚀 BLE Advertisement active!")
                    addMessage("Browsers can now scan and receive this message")
                    updateUI()
                }
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Advertising failed: $errorCode")
                isAdvertising = false
                runOnUiThread {
                    val errorMsg = when (errorCode) {
                        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising not supported on this device"
                        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many BLE advertisers running"
                        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Message too large (max ~18 characters)"
                        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error"
                        else -> "Unknown error (code: $errorCode)"
                    }
                    addMessage("❌ Advertising failed: $errorMsg")
                    
                    if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                        addMessage("💡 Try a shorter message (18 characters or less)")
                    }
                    
                    updateUI()
                }
            }
        }

        bluetoothLeAdvertiser?.startAdvertising(settings, data, callback)
    }

    private fun updateAdvertisementMessage(newMessage: String) {
        if (newMessage.length > 18) {
            addMessage("⚠️ Message too long! Truncating to 18 characters...")
            currentMessage = newMessage.take(18)
            addMessage("📝 Truncated to: \"$currentMessage\"")
        } else {
            currentMessage = newMessage
        }
        
        addMessage("📤 Updating broadcast message: \"$currentMessage\"")
        
        if (isAdvertising) {
            // Restart advertising with new message
            startAdvertisingWithMessage(currentMessage)
        }
    }

    private fun stopBleAdvertising() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED || 
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        }
        isAdvertising = false
        
        addMessage("🛑 BLE Advertising stopped")
        addMessage("Browsers will no longer receive messages")
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
        binding.startServerButton.text = if (isAdvertising) "Stop BLE Advertising" else "Start BLE Advertising"
        binding.statusText.text = if (isAdvertising) {
            "Status: 📡 Broadcasting \"$currentMessage\""
        } else {
            "Status: 🔴 Not Broadcasting"
        }
        binding.sendMessageButton.isEnabled = true // Always enabled
        binding.messageInput.isEnabled = true
        binding.sendMessageButton.text = if (isAdvertising) "Update Message" else "Set Message"
        
        // Show character count hint
        binding.messageInput.hint = "Message (max 18 chars)"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleAdvertising()
    }
}
