package com.remotemotorcontroller.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.remotemotorcontroller.App
import com.remotemotorcontroller.R
import com.remotemotorcontroller.adapter.AnalyticsViewModel
import com.remotemotorcontroller.ble.BLEManager
import com.remotemotorcontroller.data.GitHubRawRepo
import com.remotemotorcontroller.ui.widgets.DeviceHeader
import com.remotemotorcontroller.ui.widgets.LiveSummaryView
import kotlinx.coroutines.launch
import kotlin.getValue

class ShellActivity : AppCompatActivity() {

    private lateinit var deviceHeader: DeviceHeader
    private lateinit var liveSummary: LiveSummaryView

    private var hasCheckedForUpdate = false  // TRACKS if the current device has checked for a hardware update (remove redundancy)

    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shell)


        val navHost = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setupWithNavController(navHost.navController)

        deviceHeader = findViewById(R.id.deviceHeader)
        liveSummary  = findViewById(R.id.liveSummary)

        deviceHeader.setOnDisconnectClick { BLEManager.disconnect() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    (application as App).repo.settings.collect { s ->
                        BLEManager.applyConfig(
                            autoReconnectEnabled = s.ar.autoReconnect,
                            companyId            = s.ar.companyId,
                            deviceId             = s.ar.deviceId6,
                            arTimeoutMs          = s.ar.timeoutMs,
                            arRetryMs            = s.ar.retryInterval,
                            scanMode             = s.ble.scanMode,
                            cleanupDurationMs    = s.ble.cleanupDurationMs,
                            filterScanDevice     = s.ble.filterScanDevice
                        )
                        viewModel.applyConfig(s.analy.maxPoints)
                    }
                }

                // Connection header
                launch {
                    BLEManager.connectedSummary.collect { cs ->
                        if (!cs.connected) {
                            deviceHeader.setConnectionTitle(getString(R.string.msg_not_connected))
                            deviceHeader.setSubtitle("")
                            liveSummary.isVisible = false
                            deviceHeader.setDisconnectVisible(false)

                            hasCheckedForUpdate = false
                        } else {
                            val name = cs.name ?: "Unknown"
                            deviceHeader.setConnectionTitle(
                                "$name • ${getString(R.string.status_connected)}"
                            )
                            deviceHeader.setSubtitle("")
                            deviceHeader.setDisconnectVisible(true)

                            liveSummary.isVisible = true
                            if(!hasCheckedForUpdate){
                                hasCheckedForUpdate = true
                                checkForFirmwareUpdate()
                            }
                        }
                    }
                }

                // Live summary
                launch {
                    BLEManager.telemetry.collect { t ->
                        liveSummary.setRpm(t.rpm)
                        liveSummary.setAngle(t.angle)
                    }
                }
            }
        }
    }

    private fun checkForFirmwareUpdate(){
        // todo: GET THE ACTUAL HARDWARE VERSION VIA BLUETOOTH (ADD TO THE BLE PROTOCOL)
        val version = 1

        GitHubRawRepo.checkForUpdate(
            currentVersion = version,
            onUpdateFound = {manifest ->
                runOnUiThread {
                    showUpdateDialog(manifest.version, manifest.notes, manifest.downloadUrl)
                }
            },
            onError = { msg ->
                runOnUiThread {
                    Toast.makeText(this, "Error Checking for Update: $msg", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun showUpdateDialog(newVersion: Int, notes: String, downloadUrl: String){
        AlertDialog.Builder(this)
            .setTitle("New Firmware Available")
            .setMessage("Version $newVersion is available. \n\nChanges:\n$notes")
            .setPositiveButton("Update Now"){_, _ ->
                startDownload(downloadUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun startDownload(url: String){
        Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()

        GitHubRawRepo.downloadFirmware(
            url = url,
            onSuccess = {firmwareBytes ->
                BLEManager.startFirmwareUpdate(
                    binaryData = firmwareBytes,
                    onProgress = {percentage ->
                        //todo: add progress bar for percentage (int out of 100)
                    },
                    onResult = {success, msg ->
                        runOnUiThread{
                            Toast.makeText(this, "Update Complete: $success", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            },
            onFailure = {msg ->
                runOnUiThread{
                    Toast.makeText(this, "Update Failure: $msg",Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}
