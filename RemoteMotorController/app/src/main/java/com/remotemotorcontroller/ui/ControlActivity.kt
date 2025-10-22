package com.remotemotorcontroller.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.remotemotorcontroller.R
import com.remotemotorcontroller.ble.BLEManager


class ControlActivity : AppCompatActivity() {

    // ACTIVITY CONTROL UI ELEMENTS
    private lateinit var connectionText: TextView
    private lateinit var updateRpmText: TextView
    private lateinit var updateAngleText: TextView

    private lateinit var targetRpmEditText: EditText
    private lateinit var targetAngleEditText: EditText

    private lateinit var toggleMotorButton: ToggleButton
    private lateinit var calibrateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        // INITIALIZE UI ELEMENTS
        connectionText = findViewById(R.id.textConnection)
        updateRpmText = findViewById(R.id.textRpm)
        updateAngleText = findViewById(R.id.textAngle)

        targetRpmEditText = findViewById(R.id.inputRpm)
        targetAngleEditText = findViewById(R.id.inputAngle)

        toggleMotorButton = findViewById(R.id.toggleMotor)
        calibrateButton = findViewById(R.id.buttonCalibrate)

    }
    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()

        val dev = BLEManager.getConectedDevice()
        connectionText.text = if (dev != null)
            "Connected to ${dev.name ?: dev.address}"
        else
            "Not connected..."


    }
}