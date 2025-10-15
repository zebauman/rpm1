package com.remotemotorcontroller.adapter

import android.bluetooth.BluetoothDevice
import java.time.Instant

data class BleTimeDevice(
    val bDevice: BluetoothDevice, // ACTUAL BLE DEVICE
    var rssi: Int, // SIGNAL STRENGTH
    var isConnectable: Boolean, // CAN BE CONNECTED
    var time: Instant // last time this device was seen
)
