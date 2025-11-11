package com.remotemotorcontroller.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.remotemotorcontroller.App

import com.remotemotorcontroller.R
import com.remotemotorcontroller.ble.BLEManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navScanButton: Button
    private lateinit var navControlButton: Button
    private lateinit var navSettingButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navScanButton = findViewById(R.id.NavScanButton)
        navControlButton = findViewById(R.id.NavControlButton)
        navSettingButton = findViewById(R.id.SettingsButton)

        navScanButton.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }
        navControlButton.setOnClickListener {
            val intent = Intent(this, ControlActivity::class.java)
            startActivity(intent)
        }
        navSettingButton.setOnClickListener{
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                (application as App).repo.settings.collect { appSettings ->
                    BLEManager.applyConfig(
                        autoReconnectEnabled = appSettings.ar.autoReconnect,
                        companyId = appSettings.ar.companyId,
                        deviceId = appSettings.ar.deviceId6,
                        arTimeoutMs = appSettings.ar.timeoutMs,
                        arRetryMs = appSettings.ar.retryInterval,
                        scanMode = appSettings.ble.scanMode,
                        cleanupDurationMs = appSettings.ble.cleanupDurationMs,
                        filterScanDevice = appSettings.ble.filterScanDevice
                    )
                }
            }
        }
    }

}