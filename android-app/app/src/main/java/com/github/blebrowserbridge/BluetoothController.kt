package com.github.blebrowserbridge

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.nio.charset.Charset

class BluetoothController(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    companion object {
        private const val TAG = "BluetoothController"
        private const val MANUFACTURER_ID = 0xFFFF
        private const val MAX_ADVERTISEMENT_BYTES = 18 // Reduced for safety
    }

    var onPdfNameReceived: ((String) -> Unit)? = null
    val bleEvents = mutableListOf<String>()
    private var lastReceivedPdfName: String? = null

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun startServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            bleEvents.add("ERROR: BLUETOOTH_ADVERTISE permission missing.")
            return
        }
        Log.d(TAG, "Starting BLE Server with initial advertisement")
        sendPdfNameViaAdvertisement("server-ready") // Initial advertisement
    }

    fun sendPdfNameViaAdvertisement(pdfName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            bleEvents.add("ERROR: BLUETOOTH_ADVERTISE permission missing.")
            return
        }
        Log.d(TAG, "Updating advertisement with PDF name: $pdfName")
        bleEvents.add("Advertising PDF: $pdfName")

        try {
             advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while stopping advertising", e)
            bleEvents.add("ERROR: Permission missing to stop advertising.")
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        var nameBytes = pdfName.toByteArray(Charset.defaultCharset())
        if (nameBytes.size > MAX_ADVERTISEMENT_BYTES) {
            nameBytes = nameBytes.sliceArray(0 until MAX_ADVERTISEMENT_BYTES)
            Log.w(TAG, "PDF name was truncated for advertisement")
            bleEvents.add("Warning: PDF name truncated to ${nameBytes.size} bytes")
        }

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(MANUFACTURER_ID, nameBytes)
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while starting advertising", e)
            bleEvents.add("ERROR: Permission missing to start advertising.")
        }
    }

    fun startClient() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            bleEvents.add("ERROR: BLUETOOTH_SCAN permission missing.")
            return
        }
        Log.d(TAG, "Starting BLE Client")
        val scanFilters = listOf(ScanFilter.Builder().build())

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(scanFilters, scanSettings, scanCallback)
            bleEvents.add("Client scan started.")
        } catch(e: SecurityException) {
            Log.e(TAG, "Security exception while starting scan", e)
            bleEvents.add("ERROR: Permission missing to start scan.")
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping BLE operations")
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                advertiser?.stopAdvertising(advertiseCallback)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                scanner?.stopScan(scanCallback)
            }
            bleEvents.add("BLE operations stopped.")
        } catch(e: SecurityException) {
            Log.e(TAG, "Security exception while stopping BLE", e)
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertising started successfully")
            bleEvents.add("Advertising successfully started.")
        }

        override fun onStartFailure(errorCode: Int) {
            val reason = when (errorCode) {
                AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
                AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                else -> "Unknown error"
            }
            val errorMessage = "Advertising onStartFailure: $errorCode - $reason"
            Log.e(TAG, errorMessage)
            bleEvents.add(errorMessage)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID)?.let { data ->
                val pdfName = data.toString(Charset.defaultCharset())
                if (pdfName != lastReceivedPdfName) {
                    lastReceivedPdfName = pdfName
                    Log.d(TAG, "Received new advertisement with PDF Name: $pdfName")
                    bleEvents.add("Received PDF: $pdfName")
                    onPdfNameReceived?.invoke(pdfName)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {}

        override fun onScanFailed(errorCode: Int) {
            val errorMessage = "Scan failed with error code: $errorCode"
            Log.e(TAG, errorMessage)
            bleEvents.add(errorMessage)
        }
    }
}
