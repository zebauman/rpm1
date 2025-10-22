package com.remotemotorcontroller

import android.app.Application
import com.remotemotorcontroller.ble.BLEManager

// APPLICATION SUB CLASS
class App : Application(){
    override fun onCreate() {
        super.onCreate()
        BLEManager.init(this)
    }
}