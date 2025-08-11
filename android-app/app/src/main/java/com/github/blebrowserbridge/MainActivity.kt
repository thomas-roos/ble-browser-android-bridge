package com.github.blebrowserbridge

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.blebrowserbridge.databinding.ActivityMainBinding
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    
    private val PERMISSION_REQUEST_CODE = 1001
    private val PDF_PICK_REQUEST = 1002
    private var currentPage = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initBluetooth()
        setupUI()
        requestPermissions()
    }
    
    private fun initBluetooth() {
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupUI() {
        binding.selectPdfButton.setOnClickListener {
            selectPDF()
        }
        
        binding.startServerButton.setOnClickListener {
            Toast.makeText(this, "BLE Server Started", Toast.LENGTH_SHORT).show()
        }
        
        binding.startClientButton.setOnClickListener {
            Toast.makeText(this, "BLE Client Started", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun selectPDF() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, PDF_PICK_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_PICK_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                loadPDF(uri)
            }
        }
    }
    
    private fun loadPDF(uri: Uri) {
        binding.pdfView.fromUri(uri)
            .onPageChange(object : OnPageChangeListener {
                override fun onPageChanged(page: Int, pageCount: Int) {
                    currentPage = page
                    binding.pageInfo.text = "Page ${page + 1} of $pageCount"
                }
            })
            .load()
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
        }
    }
}
