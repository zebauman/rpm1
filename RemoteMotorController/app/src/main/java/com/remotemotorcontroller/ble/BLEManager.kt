package com.remotemotorcontroller.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission

class BLEManager(context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    private var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        if (scanner == null) {
            Log.e("BLE", "Bluetooth scanner not available.")
            return
        }
        if(!(bluetoothAdapter?.isEnabled ?: false)){
            Log.e("BLE", "Bluetooth is disabled")
            return
        }

        Log.i("BLE", "Starting BLE scan...")
        scanner.startScan(leScanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        scanner?.stopScan(leScanCallback)
        Log.i("BLE", "Scan Stopped.")
    }

    fun connect(device: BluetoothDevice){

    }

    fun setDeviceFoundListener(listener: (BluetoothDevice) -> Unit){
        onDeviceFound = listener
    }

    @SuppressLint("MissingPermission")
    private val leScanCallback = object : ScanCallback() {

        // Called when a device is found immediately
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            onDeviceFound?.invoke(device)
            Log.i("BLE", "Device found: ${device.name ?: "Unknown"} - ${device.address}")
        }

        // Called when multiple devices are found in a batch
        override fun onBatchScanResults(results: List<ScanResult>?) {
            results?.forEach { scanResult ->
                val device = scanResult.device
                Log.i("BLE", "Device found (batch): ${device.name ?: "Unknown"} - ${device.address}")
            }
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int){
            Log.e("BLE", "Scan failed with error code $errorCode")
        }
    }

}
