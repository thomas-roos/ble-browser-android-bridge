package com.github.blebrowserbridge

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.blebrowserbridge.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex = 0
    private var isServer = false
    
    private val PERMISSION_REQUEST_CODE = 1001
    private val PDF_PICK_REQUEST = 1002
    private val TAG = "BLE_PDF_SYNC"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initBluetooth()
        setupUI()
        requestPermissions()
        
        Log.d(TAG, "App started")
    }
    
    private fun initBluetooth() {
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Bluetooth not enabled")
        } else {
            Log.d(TAG, "Bluetooth initialized successfully")
        }
    }
    
    private fun setupUI() {
        binding.selectPdfButton.setOnClickListener {
            selectPDF()
        }
        
        binding.startServerButton.setOnClickListener {
            startBLEServer()
        }
        
        binding.startClientButton.setOnClickListener {
            startBLEClient()
        }
        
        binding.prevButton.setOnClickListener {
            Log.d(TAG, "Previous button clicked")
            showPreviousPage()
        }
        
        binding.nextButton.setOnClickListener {
            Log.d(TAG, "Next button clicked")
            showNextPage()
        }
        
        binding.debugButton.setOnClickListener {
            showDebugInfo()
        }
    }
    
    private fun selectPDF() {
        Log.d(TAG, "Selecting PDF")
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PDF_PICK_REQUEST)
    }
    
    private fun startBLEServer() {
        Log.d(TAG, "Starting BLE Server")
        isServer = true
        binding.statusText.text = "Status: BLE Server Started"
        Toast.makeText(this, "BLE Server Started (Debug Mode)", Toast.LENGTH_SHORT).show()
    }
    
    private fun startBLEClient() {
        Log.d(TAG, "Starting BLE Client")
        isServer = false
        binding.statusText.text = "Status: BLE Client Started"
        Toast.makeText(this, "BLE Client Started (Debug Mode)", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDebugInfo() {
        val info = """
            PDF Loaded: ${pdfRenderer != null}
            Current Page: ${currentPageIndex + 1}
            Total Pages: ${pdfRenderer?.pageCount ?: 0}
            Is Server: $isServer
            Bluetooth Enabled: ${bluetoothAdapter.isEnabled}
        """.trimIndent()
        
        Log.d(TAG, "Debug Info: $info")
        Toast.makeText(this, info, Toast.LENGTH_LONG).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_PICK_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                Log.d(TAG, "PDF selected: $uri")
                loadPDF(uri)
            }
        }
    }
    
    private fun loadPDF(uri: Uri) {
        try {
            val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.let {
                pdfRenderer?.close() // Close previous PDF if any
                pdfRenderer = PdfRenderer(it)
                currentPageIndex = 0
                showPage(currentPageIndex)
                Log.d(TAG, "PDF loaded successfully. Pages: ${pdfRenderer?.pageCount}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading PDF", e)
            Toast.makeText(this, "Error loading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showPage(index: Int) {
        pdfRenderer?.let { renderer ->
            if (index < 0 || index >= renderer.pageCount) {
                Log.w(TAG, "Invalid page index: $index")
                return
            }
            
            try {
                currentPage?.close()
                currentPage = renderer.openPage(index)
                
                val bitmap = Bitmap.createBitmap(
                    currentPage!!.width, currentPage!!.height, Bitmap.Config.ARGB_8888
                )
                currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                binding.pdfImageView.setImageBitmap(bitmap)
                
                currentPageIndex = index
                updatePageInfo()
                Log.d(TAG, "Showing page: ${index + 1}")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing page $index", e)
            }
        } ?: run {
            Log.w(TAG, "No PDF loaded")
            Toast.makeText(this, "Please select a PDF first", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showPreviousPage() {
        if (currentPageIndex > 0) {
            showPage(currentPageIndex - 1)
        } else {
            Log.d(TAG, "Already at first page")
            Toast.makeText(this, "Already at first page", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showNextPage() {
        pdfRenderer?.let { renderer ->
            if (currentPageIndex < renderer.pageCount - 1) {
                showPage(currentPageIndex + 1)
            } else {
                Log.d(TAG, "Already at last page")
                Toast.makeText(this, "Already at last page", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updatePageInfo() {
        pdfRenderer?.let { renderer ->
            binding.pageInfo.text = "Page ${currentPageIndex + 1} of ${renderer.pageCount}"
        } ?: run {
            binding.pageInfo.text = "No PDF loaded"
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
            Log.d(TAG, "Requesting permissions")
        } else {
            Log.d(TAG, "All permissions granted")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        currentPage?.close()
        pdfRenderer?.close()
        Log.d(TAG, "App destroyed")
    }
}
