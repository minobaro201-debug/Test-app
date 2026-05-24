package com.test.floatingtest

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val grantButton = findViewById<Button>(R.id.grantButton)

        updateStatus(statusText)

        grantButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            } else {
                updateStatus(statusText)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val statusText = findViewById<TextView>(R.id.statusText)
        updateStatus(statusText)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            val statusText = findViewById<TextView>(R.id.statusText)
            updateStatus(statusText)
        }
    }

    private fun updateStatus(statusText: TextView) {
        val granted = Settings.canDrawOverlays(this)
        val grantButton = findViewById<Button>(R.id.grantButton)
        if (granted) {
            statusText.text = "Overlay permission: GRANTED"
            grantButton.text = "Permission already granted"
            grantButton.isEnabled = false
        } else {
            statusText.text = "Overlay permission: NOT GRANTED"
            grantButton.text = "Grant Overlay Permission"
            grantButton.isEnabled = true
        }
    }
}
