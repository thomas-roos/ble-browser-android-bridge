package com.github.blebrowserbridge

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.blebrowserbridge.databinding.ActivityMainBinding
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    
    // BLE Server/Client
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isAdvertising = false
    private var isScanning = false
    private var scanCallback: ScanCallback? = null
    
    // PDF state
    private var currentPdfUri: Uri? = null
    private var currentPage = 0
    private var totalPages = 0
    private var isServer = true
    
    companion object {
        private const val TAG = "BLEPDFSync"
        private const val REQUEST_PERMISSIONS = 1001
        private const val REQUEST_PDF_FILE = 1002
        
        // BLE Configuration
        private val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        private val CHAR_UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBluetooth()
        setupUI()
        requestPermissions()
    }

    private fun setupBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun setupUI() {
        binding.btnSelectPdf.setOnClickListener { selectPdfFile() }
        binding.btnStartServer.setOnClickListener { startServer() }
        binding.btnStartClient.setOnClickListener { startClient() }
        binding.btnStop.setOnClickListener { stopAll() }
        binding.btnPrevPage.setOnClickListener { previousPage() }
        binding.btnNextPage.setOnClickListener { nextPage() }
        
        updateUI()
    }

    private fun selectPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_PDF_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PDF_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                loadPdf(uri)
            }
        }
    }

    private fun loadPdf(uri: Uri) {
        currentPdfUri = uri
        binding.pdfView.fromUri(uri)
            .onPageChange(object : OnPageChangeListener {
                override fun onPageChanged(page: Int, pageCount: Int) {
                    currentPage = page
                    totalPages = pageCount
                    updatePageInfo()
                    if (isServer && isAdvertising) {
                        broadcastPageChange()
                    }
                }
            })
            .load()
        
        updateUI()
    }

    private fun startServer() {
        if (currentPdfUri == null) {
            Toast.makeText(this, "Please select a PDF first", Toast.LENGTH_SHORT).show()
            return
        }
        
        isServer = true
        startBleServer()
        updateUI()
    }

    private fun startClient() {
        isServer = false
        startBleScanning()
        updateUI()
    }

    private fun stopAll() {
        stopBleServer()
        stopBleScanning()
        updateUI()
    }

    private fun startBleServer() {
        if (!hasPermissions()) return
        
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        
        // Start GATT server
        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)
        
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristic)
        bluetoothGattServer?.addService(service)
    }

    private fun stopBleServer() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        bluetoothGattServer?.close()
        isAdvertising = false
    }

    private fun startBleScanning() {
        if (!hasPermissions()) return
        
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true) {
                    connectToDevice(result.device)
                }
            }
        }
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        
        bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
    }

    private fun stopBleScanning() {
        scanCallback?.let { bluetoothLeScanner?.stopScan(it) }
        isScanning = false
    }

    private fun connectToDevice(device: BluetoothDevice) {
        device.connectGatt(this, false, gattClientCallback)
    }

    private fun broadcastPageChange() {
        val pageData = "PAGE:$currentPage"
        // Send via BLE characteristic
        bluetoothGattServer?.getService(SERVICE_UUID)
            ?.getCharacteristic(CHAR_UUID)
            ?.setValue(pageData.toByteArray())
    }

    private fun goToPage(page: Int) {
        if (currentPdfUri != null && page >= 0 && page < totalPages) {
            binding.pdfView.jumpTo(page)
        }
    }

    private fun previousPage() {
        if (currentPage > 0) {
            goToPage(currentPage - 1)
        }
    }

    private fun nextPage() {
        if (currentPage < totalPages - 1) {
            goToPage(currentPage + 1)
        }
    }

    private fun updatePageInfo() {
        binding.tvPageInfo.text = "Page ${currentPage + 1} of $totalPages"
    }

    private fun updateUI() {
        binding.btnStartServer.isEnabled = !isAdvertising && currentPdfUri != null
        binding.btnStartClient.isEnabled = !isScanning
        binding.btnStop.isEnabled = isAdvertising || isScanning
        binding.btnPrevPage.isEnabled = currentPage > 0
        binding.btnNextPage.isEnabled = currentPage < totalPages - 1
        
        binding.tvStatus.text = when {
            isAdvertising -> "Server: Broadcasting page changes"
            isScanning -> "Client: Listening for page changes"
            else -> "Stopped"
        }
    }

    // BLE Callbacks
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            runOnUiThread { 
                Toast.makeText(this@MainActivity, "Server started", Toast.LENGTH_SHORT).show()
                updateUI()
            }
        }

        override fun onStartFailure(errorCode: Int) {
            runOnUiThread { 
                Toast.makeText(this@MainActivity, "Server failed: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val pageData = "PAGE:$currentPage"
            bluetoothGattServer?.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS, 0, pageData.toByteArray()
            )
        }
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val characteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
            characteristic?.let { gatt.readCharacteristic(it) }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val data = String(characteristic.value)
            if (data.startsWith("PAGE:")) {
                val page = data.substring(5).toIntOrNull()
                page?.let { 
                    runOnUiThread { goToPage(it) }
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        return permissions.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
    }

    private fun requestPermissions() {
        if (!hasPermissions()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS)
        }
    }
}
