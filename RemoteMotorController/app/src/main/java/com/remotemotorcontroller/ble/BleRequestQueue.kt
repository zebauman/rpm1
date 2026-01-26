package com.remotemotorcontroller.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class BleRequestQueue(
    private val scope: CoroutineScope,
    private val gattProvider: () -> BluetoothGatt?
    ) {
    private val operationChannel = Channel<BleOperation>(Channel.UNLIMITED)

    private val processingMutex = Mutex()

    private val callbackSignal = Mutex(locked = true)  // PAUSES THE QUEUE UNTIL THE HARDWARE ACKS

    private var queueJob: Job? = null

    fun start(){
        if(queueJob?.isActive == true) return

        queueJob = scope.launch{
            for(op in operationChannel){
                processingMutex.withLock{
                    val gatt = gattProvider()
                    if(gatt == null){
                        Log.e("BLE", "GATT IS NULL -> DROPPING OPERATION")
                        return@withLock
                    }

                    val success = when(op){
                        is BleOperation.Write -> executeWrite(gatt, op)
                    }

                    if(success){
                        withTimeoutOrNull(2000){
                            callbackSignal.withLock {}
                        }
                    }
                }
            }
        }
    }

    fun stop(){
        queueJob?.cancel()
    }

    fun clear(){
        // EMPTY THE PENDING LIST
        while(operationChannel.tryReceive().isSuccess) {}

        // UNLOCK THE MUTEX
        if(callbackSignal.isLocked) {
            try{
                callbackSignal.unlock()
            }catch(e: Exception){}
        }
    }
    fun enqueueWrite(characteristic: BluetoothGattCharacteristic, data: ByteArray){
        operationChannel.trySend(BleOperation.Write(characteristic, data))
    }

    fun onWriteComplete() {
        if(callbackSignal.isLocked) {
            try {
                callbackSignal.unlock()
            } catch(e: Exception){
                Log.e("BLE", "FAILED TO UNLOCK CALLBACK SIGNAL: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun executeWrite(gatt: BluetoothGatt, op: BleOperation.Write): Boolean{
        if(!callbackSignal.isLocked) callbackSignal.tryLock()

        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            gatt.writeCharacteristic(op.characteristic, op.payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
        } else {
            op.characteristic.value = op.payload
            gatt.writeCharacteristic(op.characteristic)
        }
    }
}