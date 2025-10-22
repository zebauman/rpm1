package com.remotemotorcontroller.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.remotemotorcontroller.R

class MainActivity : AppCompatActivity() {

    private lateinit var navScanButton: Button
    private lateinit var navControlButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navScanButton = findViewById(R.id.NavScanButton)
        navControlButton = findViewById(R.id.NavControlButton)

        navScanButton.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }
        navControlButton.setOnClickListener {
            val intent = Intent(this, ControlActivity::class.java)
            startActivity(intent)
        }
    }

}