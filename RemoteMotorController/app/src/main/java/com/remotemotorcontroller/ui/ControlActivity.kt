package com.remotemotorcontroller.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.remotemotorcontroller.R
import com.remotemotorcontroller.ble.BLEManager


class ControlActivity : AppCompatActivity() {

    // ACTIVITY CONTROL UI ELEMENTS
    private lateinit var connectionText: TextView
    private lateinit var updateRpmText: TextView
    private lateinit var updateAngleText: TextView

    private lateinit var targetRpmEditText: EditText
    private lateinit var targetAngleEditText: EditText

    private lateinit var startMotorButton: MaterialButton
    private lateinit var stopMotorButton: MaterialButton
    private lateinit var sendAngleButton: MaterialButton

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

        startMotorButton = findViewById(R.id.buttonStartRpm)
        stopMotorButton = findViewById(R.id.buttonStopRpm)
        sendAngleButton = findViewById(R.id.buttonSendAngle)
        calibrateButton = findViewById(R.id.buttonCalibrate)

        calibrateButton.setOnClickListener{
            BLEManager.calibrate()
        }

        startMotorButton.setOnClickListener{
            val rpm = targetRpmEditText.text.toString().toIntOrNull()
            if(rpm != null){
                BLEManager.setSpeed(rpm)
                Toast.makeText(this, "Starting motor at $rpm RPM", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Invalid RPM value", Toast.LENGTH_SHORT).show()
            }
        }
        stopMotorButton.setOnClickListener{
            BLEManager.shutdown()
            Toast.makeText(this, "Stopping motor", Toast.LENGTH_SHORT).show()
        }
        sendAngleButton.setOnClickListener {
            val angle = targetAngleEditText.text.toString().toIntOrNull()
            if(angle != null){
                BLEManager.setPosition(angle)
                Toast.makeText(this, "Moving to $angle degrees", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Invalid angle value", Toast.LENGTH_SHORT).show()
            }
        }




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