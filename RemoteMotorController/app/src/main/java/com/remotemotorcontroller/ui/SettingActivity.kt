package com.remotemotorcontroller.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.remotemotorcontroller.settings.SettingsRepository
import com.remotemotorcontroller.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class SettingActivity : AppCompatActivity() {

    private lateinit var repo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<View>(R.id.topAppBar)?.setOnClickListener { finish() }

        repo = SettingsRepository(applicationContext)

        findViewById<View>(R.id.rowBleConfig).setOnClickListener{ showBleSheet() }
        findViewById<View>(R.id.rowARConfig).setOnClickListener { showArSheet() }
    }

    fun showBleSheet(){
        val sheet = ConfigBottomSheet.new("BLE Configuration", R.layout.bs_ble_config)
        sheet.show(supportFragmentManager,"ble")

        supportFragmentManager.executePendingTransactions()
        val root = sheet.dialog?.findViewById<View>(R.id.bsContent) ?: return

        val swFilter = root.findViewById<MaterialSwitch>(R.id.swFilter)
        val spMode = root.findViewById<Spinner>(R.id.spScanMode)
        val inCleanup = root.findViewById<TextInputEditText>(R.id.inCleanup)
        val saveButton = root.findViewById<MaterialButton>(R.id.btnSaveBle)

        ArrayAdapter.createFromResource(
            this, R.array.scan_mode_names, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spMode.adapter = it
        }

        // DISPLAY THE CURRENT VALUES
        lifecycleScope.launch{
            val cfg = repo.settings.first()
            swFilter.isChecked = cfg.ble.filterScanDevice

            val values = resources.getIntArray(R.array.scan_mode_values)
            val idx = values.indexOf(cfg.ble.scanMode).coerceAtLeast(0)
            spMode.setSelection(idx)

            inCleanup.setText(cfg.ble.cleanupDurationMs.toString())
        }

        saveButton.setOnClickListener {
            lifecycleScope.launch{
                val values = resources.getIntArray(R.array.scan_mode_values)
                val newMode = values.getOrNull(spMode.selectedItemPosition) ?: 2

                repo.enableFilter(swFilter.isChecked)
                repo.setScanMode(newMode)
                repo.setCleanupDuration(inCleanup.text.toString().toLongOrNull() ?: 5_000L)

                Toast.makeText(this@SettingActivity, "Saved", Toast.LENGTH_SHORT).show()
                sheet.dismiss()
            }
        }
    }

    fun showArSheet(){

    }
}