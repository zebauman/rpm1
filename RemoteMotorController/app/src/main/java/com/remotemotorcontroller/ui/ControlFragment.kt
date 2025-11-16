package com.remotemotorcontroller.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.remotemotorcontroller.R
import com.remotemotorcontroller.ble.BLEManager


class ControlFragment : Fragment(R.layout.fragment_control) {

    private lateinit var targetRpmEditText: TextInputEditText
    private lateinit var targetAngleEditText: TextInputEditText

    private lateinit var startStopMotorButton: MaterialButton
    private lateinit var sendAngleButton: MaterialButton

    private lateinit var calibrateButton: Button
    private var motorStart = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetRpmEditText = view.findViewById(R.id.inputRpm)
        targetAngleEditText = view.findViewById(R.id.inputAngle)

        startStopMotorButton = view.findViewById(R.id.buttonStartStopRpm)
        sendAngleButton = view.findViewById(R.id.buttonSendAngle)
        calibrateButton = view.findViewById(R.id.buttonCalibrate)

        calibrateButton.setOnClickListener {
            BLEManager.calibrate()
        }

        startStopMotorButton.setOnClickListener {
            if (motorStart) { // MOTOR STARTING
                val rpm = targetRpmEditText.text.toString().toIntOrNull()
                if (rpm != null) {
                    setStopUi()
                    BLEManager.setSpeed(rpm)
                    Toast.makeText(
                        requireContext(),
                        "Starting motor at $rpm RPM",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "Invalid RPM value", Toast.LENGTH_SHORT).show()
                }
            } else { // MOTOR STOPPING
                setStartUi()
                BLEManager.shutdown()
                Toast.makeText(requireContext(), "Stopping motor", Toast.LENGTH_SHORT).show()
            }
        }
        sendAngleButton.setOnClickListener {
            val angle = targetAngleEditText.text.toString().toIntOrNull()
            if (angle != null) {
                BLEManager.setPosition(angle)
                Toast.makeText(requireContext(), "Moving to $angle degrees", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Invalid angle value", Toast.LENGTH_SHORT).show()
            }
        }

    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
    }

    private fun setStartUi() {
        motorStart = true
        startStopMotorButton.text = "START MOTOR"
        startStopMotorButton.setIconResource(R.drawable.ic_play)
    }

    private fun setStopUi() {
        motorStart = false
        startStopMotorButton.text = "STOP MOTOR"
        startStopMotorButton.setIconResource(R.drawable.ic_stop)
    }
}