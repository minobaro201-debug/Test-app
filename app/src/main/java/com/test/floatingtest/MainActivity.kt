package com.test.floatingtest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 1001

    private lateinit var statusText: TextView
    private lateinit var grantButton: Button
    private lateinit var showGuiSwitch: Switch
    private lateinit var guiCard: View
    private lateinit var codeCard: View
    private lateinit var codeInput: EditText
    private lateinit var urlInput: EditText
    private lateinit var applyButton: Button
    private lateinit var savedLabel: TextView

    private var serviceRunning = false

    companion object {
        const val PREFS_NAME = "FloatingPrefs"
        const val KEY_CODE = "saved_code"
        const val KEY_URL = "saved_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        grantButton = findViewById(R.id.grantButton)
        showGuiSwitch = findViewById(R.id.showGuiSwitch)
        guiCard = findViewById(R.id.guiCard)
        codeCard = findViewById(R.id.codeCard)
        codeInput = findViewById(R.id.codeInput)
        urlInput = findViewById(R.id.urlInput)
        applyButton = findViewById(R.id.applyButton)
        savedLabel = findViewById(R.id.savedLabel)

        grantButton.setOnClickListener {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQUEST_CODE_OVERLAY
            )
        }

        showGuiSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startService(Intent(this, FloatingService::class.java))
                serviceRunning = true
            } else {
                stopService(Intent(this, FloatingService::class.java))
                serviceRunning = false
            }
        }

        applyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            val url = urlInput.text.toString().trim()
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_CODE, code)
                .putString(KEY_URL, url)
                .apply()
            savedLabel.visibility = View.VISIBLE
            if (url.isNotEmpty()) {
                savedLabel.text = "Saved \u2713  Will fetch from URL when toggle turns ON"
            } else {
                savedLabel.text = "Saved \u2713  Will run pasted code when toggle turns ON"
            }
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_CODE, "") ?: ""
        val savedUrl = prefs.getString(KEY_URL, "") ?: ""
        if (savedCode.isNotEmpty()) codeInput.setText(savedCode)
        if (savedUrl.isNotEmpty()) urlInput.setText(savedUrl)
        if (savedCode.isNotEmpty() || savedUrl.isNotEmpty()) {
            savedLabel.visibility = View.VISIBLE
            savedLabel.text = "Previously saved \u2713"
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) updateUI()
    }

    private fun updateUI() {
        val granted = Settings.canDrawOverlays(this)
        if (granted) {
            statusText.text = "Overlay permission: GRANTED"
            grantButton.visibility = View.GONE
            guiCard.visibility = View.VISIBLE
            codeCard.visibility = View.VISIBLE
            showGuiSwitch.visibility = View.VISIBLE
        } else {
            statusText.text = "Overlay permission: NOT GRANTED\nGrant it to use the floating button."
            grantButton.visibility = View.VISIBLE
            guiCard.visibility = View.GONE
            codeCard.visibility = View.GONE
            showGuiSwitch.visibility = View.GONE
            if (serviceRunning) {
                stopService(Intent(this, FloatingService::class.java))
                serviceRunning = false
                showGuiSwitch.isChecked = false
            }
        }
    }
}
