package com.test.floatingtest

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.net.HttpURLConnection
import java.net.URL

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var infoView: View? = null
    private var webView: WebView? = null
    private var fpsMonitor: FPSMonitor? = null
    private var isOn = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    companion object {
        const val CHANNEL_ID = "floating_service_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        webView = WebView(applicationContext).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(ScriptBridge(), "Android")
            webViewClient = WebViewClient()
            loadData("<html><body></body></html>", "text/html", "utf-8")
        }

        showFloatingButton()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Floating Button Service", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps the floating button visible" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Button Active")
            .setContentText("Tap to open app")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openIntent)
            .build()
    }

    private fun showFloatingButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30; y = 200
        }

        windowManager.addView(floatingView, params)
        updateButtonState()

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    isDragging = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) isDragging = true
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        isOn = !isOn
                        updateButtonState()
                        if (isOn) turnOn() else turnOff()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun turnOn() {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val url = prefs.getString(MainActivity.KEY_URL, "") ?: ""
        val localCode = prefs.getString(MainActivity.KEY_CODE, "") ?: ""

        if (url.isNotEmpty()) {
            updateButtonLabel("...")
            Thread {
                val code = fetchUrl(url)
                mainHandler.post {
                    if (code != null) {
                        updateButtonLabel("ON")
                        webView?.evaluateJavascript(code, null)
                    } else {
                        updateButtonLabel("ERR")
                        Toast.makeText(applicationContext, "Failed to load script from URL", Toast.LENGTH_SHORT).show()
                        isOn = false
                        updateButtonState()
                    }
                }
            }.start()
        } else if (localCode.isNotEmpty()) {
            webView?.evaluateJavascript(localCode, null)
        }
    }

    private fun fetchUrl(urlString: String): String? {
        return try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code == 200) {
                connection.inputStream.bufferedReader().readText()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun turnOff() {
        fpsMonitor?.stop()
        removeInfoOverlay()
    }

    private fun showInfoOverlay(text: String, color: String, x: Int, y: Int) {
        mainHandler.post {
            if (infoView == null) {
                infoView = LayoutInflater.from(this).inflate(R.layout.layout_info_overlay, null)
                val p = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    this.x = x; this.y = y
                }
                windowManager.addView(infoView, p)
            }
            try {
                infoView?.findViewById<TextView>(R.id.overlayText)?.apply {
                    this.text = text
                    setTextColor(Color.parseColor(color))
                }
            } catch (e: Exception) {
                infoView?.findViewById<TextView>(R.id.overlayText)?.text = text
            }
        }
    }

    private fun updateInfoText(text: String) {
        mainHandler.post {
            infoView?.findViewById<TextView>(R.id.overlayText)?.text = text
        }
    }

    private fun removeInfoOverlay() {
        mainHandler.post {
            infoView?.let {
                try { windowManager.removeView(it) } catch (_: Exception) {}
                infoView = null
            }
        }
    }

    private fun updateButtonState() {
        val label = floatingView.findViewById<TextView>(R.id.btnLabel)
        val dot = floatingView.findViewById<View>(R.id.statusDot)
        if (isOn) {
            label.text = "ON"
            dot.setBackgroundResource(R.drawable.dot_on)
        } else {
            label.text = "OFF"
            dot.setBackgroundResource(R.drawable.dot_off)
        }
    }

    private fun updateButtonLabel(text: String) {
        floatingView.findViewById<TextView>(R.id.btnLabel).text = text
    }

    override fun onDestroy() {
        fpsMonitor?.stop()
        removeInfoOverlay()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        webView?.destroy()
        super.onDestroy()
    }

    inner class ScriptBridge {

        @JavascriptInterface
        fun showFPS(x: Int = 20, y: Int = 60, color: String = "#00FF88") {
            showInfoOverlay("FPS: --", color, x, y)
            fpsMonitor?.stop()
            fpsMonitor = FPSMonitor { fps ->
                if (isOn) updateInfoText("FPS: $fps")
            }
            fpsMonitor?.start()
        }

        @JavascriptInterface
        fun showText(text: String, x: Int = 20, y: Int = 60, color: String = "#FFFFFF") {
            showInfoOverlay(text, color, x, y)
        }

        @JavascriptInterface
        fun updateText(text: String) {
            updateInfoText(text)
        }

        @JavascriptInterface
        fun hideOverlay() {
            fpsMonitor?.stop()
            removeInfoOverlay()
        }

        @JavascriptInterface
        fun toast(message: String) {
            mainHandler.post {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun alert(message: String) {
            mainHandler.post {
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
        }

        @JavascriptInterface
        fun log(message: String) {
            android.util.Log.d("FloatingScript", message)
        }
    }
}
