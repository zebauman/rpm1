package com.remotemotorcontroller.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.remotemotorcontroller.R
import com.remotemotorcontroller.adapter.DeviceAdapter
import com.remotemotorcontroller.ble.BLEManager


class ScanActivity : AppCompatActivity() {
    private lateinit var bleManager: BLEManager
    private lateinit var scanButton: Button

    private lateinit var recyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter

    private var isScanning = false

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_devices)

        recyclerView = findViewById(R.id.deviceRecyclerView)
        deviceAdapter = DeviceAdapter(mutableListOf()){ device ->
            connectToDevice(device)
        }
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = deviceAdapter

        bleManager = BLEManager(this)
        bleManager.setDeviceFoundListener{ device ->
            runOnUiThread {
                deviceAdapter.addOrUpdateDevice(device)
            }
        }
        bleManager.setDeviceRemovedListener { device ->
            runOnUiThread {
                deviceAdapter.removeDevice(device)
            }
        }

        bleManager.setConnectionStateListener { device, connected ->
            runOnUiThread {
                if(connected){
                    Toast.makeText(this, "Connected to ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "Disconnected from ${device.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        scanButton = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            toggleScan()
        }
    }

    private fun toggleScan(){
        if(isScanning){
            stopScan()
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

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice){
        bleManager.connect(device)
    }
}