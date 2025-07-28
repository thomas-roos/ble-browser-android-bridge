package com.github.blebrowserbridge

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
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
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    companion object {
        private const val TAG = "BLEBridge"
        private const val REQUEST_PERMISSIONS = 1001
        
        val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val CHAR_UUID: UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321")
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            Toast.makeText(this, "BLE advertising not supported", Toast.LENGTH_LONG).show()
            return
        }

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.startServerButton.setOnClickListener {
            if (isAdvertising) {
                stopBleServer()
            } else {
                startBleServer()
            }
        }

        binding.sendMessageButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToBrowser(message)
                binding.messageInput.text.clear()
            }
        }

        updateUI()
    }

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(this, "Permissions required for BLE functionality", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startBleServer() {
        if (!hasRequiredPermissions()) {
            checkPermissions()
            return
        }

        val gattServerCallback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                val deviceName = device?.name ?: device?.address ?: "Unknown"
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "Device connected: $deviceName")
                        runOnUiThread {
                            addMessage("Browser connected: $deviceName")
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Device disconnected: $deviceName")
                        runOnUiThread {
                            addMessage("Browser disconnected: $deviceName")
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
                val response = "Hello from Android! Time: ${System.currentTimeMillis()}"
                bluetoothGattServer?.sendResponse(
                    device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, response.toByteArray()
                )
                Log.d(TAG, "Sent response to browser: $response")
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
                val message = String(value ?: byteArrayOf())
                Log.d(TAG, "Received from browser: $message")

                runOnUiThread {
                    addMessage("Browser: $message")
                }

                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null
                    )
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

        startAdvertising()
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "Advertising started successfully")
                isAdvertising = true
                runOnUiThread {
                    addMessage("BLE Server started - Ready for browser connections!")
                    updateUI()
                }
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "Advertising failed: $errorCode")
                isAdvertising = false
                runOnUiThread {
                    addMessage("Failed to start BLE server (Error: $errorCode)")
                    updateUI()
                }
            }
        }

        bluetoothLeAdvertiser?.startAdvertising(settings, data, callback)
    }

    private fun stopBleServer() {
        bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        bluetoothGattServer?.close()
        bluetoothGattServer = null
        isAdvertising = false
        
        addMessage("BLE Server stopped")
        updateUI()
    }

    private fun sendMessageToBrowser(message: String) {
        // This would require implementing notifications
        // For now, messages are sent when browser reads the characteristic
        addMessage("Android: $message")
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
        binding.startServerButton.text = if (isAdvertising) "Stop BLE Server" else "Start BLE Server"
        binding.statusText.text = if (isAdvertising) "Status: Advertising (Ready for connections)" else "Status: Stopped"
        binding.sendMessageButton.isEnabled = isAdvertising
        binding.messageInput.isEnabled = isAdvertising
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBleServer()
    }
}
