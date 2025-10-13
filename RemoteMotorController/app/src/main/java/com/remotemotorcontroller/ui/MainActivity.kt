package com.remotemotorcontroller.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.remotemotorcontroller.R
import com.remotemotorcontroller.ble.BLEManager

class MainActivity : AppCompatActivity() {
    private lateinit var  bleManager: BLEManager
    private lateinit var scanButton: Button
    private var isScanning = false

    // PERMISSIONS REQUIRED
    private val requiredPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        )

    // PERMISSION LAUNCHER
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> val allGranted = permissions.all { it.value }
        if (allGranted) startScan()
        else Log.e("MainActivity", "Cannot scan: missing permissions")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bleManager = BLEManager(this)
        scanButton = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            toggleScan()
        }
    }
    private fun toggleScan(){
        if(isScanning){
            stopScan()
        }else{
            checkPermissionsAndScan()
        }
    }
    private fun checkPermissionsAndScan(){
        // CHECK IF PERMISSIONS ALREADY GRANTED
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if(missingPermissions.isNotEmpty()){
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }else{
            startScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan(){
        bleManager.startScan()
        scanButton.text = getString(R.string.stop_scanning)
        isScanning = true
    }

    @SuppressLint("MissingPermission")
    private fun stopScan(){
        bleManager.stopScan()
        scanButton.text = getString(R.string.start_scanning)
        isScanning = false
    }


}