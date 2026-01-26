package com.remotemotorcontroller.ble

import android.bluetooth.BluetoothGattCharacteristic

sealed class BleOperation {
    data class Write(
        val characteristic: BluetoothGattCharacteristic,
        val payload: ByteArray
    ) : BleOperation() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Write

            if (characteristic != other.characteristic) return false
            if (!payload.contentEquals(other.payload)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = characteristic.hashCode()
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }
}