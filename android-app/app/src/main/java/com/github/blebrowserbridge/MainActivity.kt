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
            addMessage("Please enable Bluetooth in Android settings")
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            addMessage("BLE advertising not supported on this device")
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
                    addMessage("Please grant all required permissions")
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
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            addMessage("Requesting permissions: ${missingPermissions.joinToString(", ")}")
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            addMessage("All permissions granted - ready to start BLE advertising!")
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
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                addMessage("✅ All permissions granted! You can now start BLE advertising.")
            } else {
                addMessage("❌ Some permissions were denied. Please grant all permissions in Settings.")
                
                // Show which permissions were denied
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        val permissionName = permission.substringAfterLast(".")
                        addMessage("Missing: $permissionName")
                    }
                }
                
                Toast.makeText(this, "Please grant all permissions in Android Settings → Apps → BLE Browser Bridge → Permissions", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startBleAdvertising() {
        if (!hasAllRequiredPermissions()) {
            addMessage("❌ Missing permissions. Please grant all permissions first.")
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

        // Encode message in manufacturer data (limited to ~20 bytes)
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
                    addMessage("🚀 BLE Advertisement active - browsers can now scan!")
                    updateUI()
                }
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Advertising failed: $errorCode")
                isAdvertising = false
                runOnUiThread {
                    val errorMsg = when (errorCode) {
                        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising not supported"
                        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many BLE advertisers"
                        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Message too large (max ~18 chars)"
                        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                        else -> "Unknown error ($errorCode)"
                    }
                    addMessage("❌ Failed to start advertising: $errorMsg")
                    updateUI()
                }
            }
        }

        bluetoothLeAdvertiser?.startAdvertising(settings, data, callback)
    }

    private fun updateAdvertisementMessage(newMessage: String) {
        currentMessage = newMessage
        addMessage("📤 Updating message: \"$newMessage\"")
        
        if (isAdvertising) {
            // Restart advertising with new message
            startAdvertisingWithMessage(newMessage)
        }
    }

    private fun stopBleAdvertising() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED || 
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        }
        isAdvertising = false
        
        addMessage("🛑 BLE Advertising stopped")
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
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleAdvertising()
    }
}
