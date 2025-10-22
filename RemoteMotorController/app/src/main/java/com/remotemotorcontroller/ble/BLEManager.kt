package com.remotemotorcontroller.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
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

class BLEManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    private val scannedDevices = mutableListOf<BleTimeDevice>()

    // BLE GATT CLIENT -> ALLOWS FOR CONNECTION TO BLE GATT SERVERS
    // INFORMATION REGARDING DISCOVERING SERVICES, READING, AND WRITING CHARACTERISTICS
    private var bluetoothGatt: BluetoothGatt? = null

    private var connectedDevice: BluetoothDevice? = null

    // FUNCTION TO NOTIFY THE APP WHEN CONNECTION STATE CHANGES -> LISTENER FUNCTIONS
    private var onConnectionStateChange: ((BluetoothDevice, Boolean) -> Unit)? = null

    // FUNCTION TO CALL WHEN A DEVICE IS FOUND
    private var onDeviceFound: ((BleTimeDevice) -> Unit)? = null

    // FUNCTION TO CALL WHEN A DEVICE IS TO BE REMOVED DEPRECIATED
    private var onDeviceRemoved: ((BleTimeDevice) -> Unit)? = null

    // GATT CHARACTERISTICS
    private var charCmd: BluetoothGattCharacteristic? = null
    private var charTelem: BluetoothGattCharacteristic? = null


    // COROUTINE SCOPE TO MANAGE BACKGROUND JOB's LIFECYCLE
    //COROUTINE is A FUNCTION THAT CAN PAUSE AND RESUME ITS EXECUTION WITHOUT BLOCKING THE THREAD
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var cleanupJob: Job? = null

    // CALLBACK FUNCTIONS
    // CALLBACK FUNCTION FOR GATT
    private val gattCallback = object : BluetoothGattCallback() {
        // FUNCTION WHEN THE CONNECTION STATE CHANGES OF THE CONNECTED DEVICE -> MANAGED BY GATT
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.i("BLE", "CONNECTED TO ${gatt.device?.address}")
                onConnectionStateChange?.invoke(gatt.device, true)
                gatt.discoverServices()
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.i("BLE", "DISCONNECTED FROM ${gatt.device?.address}")
                onConnectionStateChange?.invoke(gatt.device, false)
                gatt.close()
                bluetoothGatt = null
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                val serv = gatt.getService(BLEContract.SERVICE_MOTOR)
                if(serv == null){
                    Log.e("BLE", "Motor SERVICE NOT FOUND")
                    return
                }
                charCmd = serv.getCharacteristic(BLEContract.CHAR_CMD)
                charTelem = serv.getCharacteristic(BLEContract.CHAR_TELEM)

                // charTelem?.let{ enableNotifications()}
                Log.i("BLE", "SERVICES AND CHARACTERISTICS CACHED.")
            }else{
                Log.e("BLE", "FAILED TO DISCOVER SERVICES for ${gatt.device?.address}")
            }
        }

    }

    // TELEMETRY CALLBACK
    private var telemCallback: ((status: Int, speed: Long, position: Long) -> Unit)? = null

    // CALLBACK FUNCTION FOR BLE SCAN
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

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice){
        bluetoothGatt?.close() // CLOSE ANY PREVIOUS CONNECTIONS
        connectedDevice = device
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // WRITE/READ FROM THE BLE MOTOR DEVICE
    // TODO: ADD OPERATION QUEUE FOR WRITING AND READING
    @SuppressLint("MissingPermission")
    fun writeCommand(cmd: Byte, value: Int){
        val ch = charCmd
        if(ch == null){
            Log.e("BLE", "Command Characteristic not found/ready.")
            return
        }
        // CURRENT FORMAT FOR THE MOTOR STM32 BLE SYSTEM: 1 BYTE COMMAND, 4 BYTES VALUE
        val b0 = cmd
        val b1 = (value and 0xFF).toByte()
        val b2 = ((value shr 8) and 0xFF).toByte()
        val b3 = ((value shr 16) and 0xFF).toByte()
        val b4 = ((value shr 24) and 0xFF).toByte()

        val payload = byteArrayOf(b0, b1, b2, b3, b4)

        Log.i("BLE", "Sending command: $cmd, $value")


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt?.writeCharacteristic(ch, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        }else{
            ch.value = payload
            bluetoothGatt?.writeCharacteristic(ch)
        }
    }

    // HELPER FUNCTIONS FOR SENDING COMMANDS
    fun shutdown() = writeCommand(BLEContract.CMD_SHUTDOWN,0)
    fun calibrate() = writeCommand(BLEContract.CMD_CALIBRATE,0)
    fun setSpeed(rpm: Int) = writeCommand(BLEContract.CMD_SPEED, rpm)
    fun setPosition(pos: Int) = writeCommand(BLEContract.CMD_POSITION, pos)

    // SETTERS FOR THE LISTENER FUNCTIONS
    fun setTelemetryListener(l: (status: Int, speed: Long, position: Long) -> Unit) {
        telemCallback = l
    }
    fun setDeviceFoundListener(listener: (BleTimeDevice) -> Unit){
        onDeviceFound = listener
    }
    fun setConnectionStateListener(listener: (BluetoothDevice, Boolean) -> Unit) {
        onConnectionStateChange = listener
    }
    fun setDeviceRemovedListener(listener: (BleTimeDevice) -> Unit){
        onDeviceRemoved = listener
    }

    private fun startCleanupJob() : Job{
        return coroutineScope.launch {
            while (true) {
                delay(5000)
                val now = Instant.now()
                val timeout = Duration.ofSeconds(10).toMillis()

                val toRemove = mutableListOf<Int>()
                scannedDevices.forEachIndexed { index, device ->
                    val age = Duration.between(device.time, now).toMillis()
                    if (age > timeout) {
                        toRemove.add(index)
                    }
                }
                toRemove.reversed().forEach {

                    onDeviceRemoved?.invoke(scannedDevices[it])
                    scannedDevices.removeAt(it)
                }
            }

        }
    }

}
