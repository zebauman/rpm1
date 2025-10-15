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
import com.remotemotorcontroller.adapter.BleTimeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class BLEManager(context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    // FUNCTION TO CALL WHEN A DEVICE IS FOUND
    private var onDeviceFound: ((BleTimeDevice) -> Unit)? = null
    private val scannedDevices = mutableListOf<BleTimeDevice>()

    // COROUTINE SCOPE TO MANAGE BACKGROUND JOB's LIFECYCLE
    //COROUTINE is A FUNCTION THAT CAN PAUSE AND RESUME ITS EXECUTION WITHOUT BLOCKING THE THREAD
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var cleanupJob: Job? = null
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
        // DON'T START MULTIPLE JOBS -> ONLY ONE
        if(cleanupJob == null || !cleanupJob!!.isActive){
            cleanupJob = startCleanupJob()
        }

        scanner.startScan(leScanCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        scanner?.stopScan(leScanCallback)
        // CANCEL THE JOB TO STOP THE INFINITE LOOP
        cleanupJob?.cancel()
        cleanupJob = null
    }

    fun connect(device: BluetoothDevice){

    }

    fun setDeviceFoundListener(listener: (BleTimeDevice) -> Unit){
        onDeviceFound = listener
    }

    @SuppressLint("MissingPermission")
    private val leScanCallback = object : ScanCallback() {

        // Called when a device is found immediately
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return

            val existing = scannedDevices.find{ it.bDevice.address == device.address}
            val now = Instant.now()

            if(existing != null){
                existing.time = now
                existing.rssi = result.rssi
                existing.isConnectable = result.isConnectable
                // INVOKE IS UPDATING THE UI EVERY TIE THE DEVICE IS FOUND/UPDATED -> CALLBACK FUNCTION
                onDeviceFound?.invoke(existing)
            }else{
                val newDevice = BleTimeDevice(
                    device,
                    result.rssi,
                    result.isConnectable,
                    now)
                scannedDevices.add(newDevice)
                onDeviceFound?.invoke(newDevice)
            }
        }

        override fun onScanFailed(errorCode: Int){
            Log.e("BLE", "Scan failed with error code $errorCode")
        }
    }

    private fun startCleanupJob() : Job{
        return coroutineScope.launch {

            delay(5000)
            val now = Instant.now()
            val timeout = Duration.ofSeconds(10)

            val iterator = scannedDevices.iterator()
            while(iterator.hasNext()){
                val device = iterator.next()
                val age = Duration.between(device.time, now)
                if(age > timeout){
                    iterator.remove()
                }
            }
        }
    }


}
