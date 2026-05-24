package com.test.floatingtest

import android.view.Choreographer

class FPSMonitor(private val onFpsUpdate: (Int) -> Unit) {

    private var frameCount = 0
    private var lastTime = System.nanoTime()
    private var running = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            frameCount++
            val elapsed = frameTimeNanos - lastTime
            if (elapsed >= 1_000_000_000L) {
                val fps = (frameCount * 1_000_000_000L / elapsed).toInt()
                onFpsUpdate(fps)
                frameCount = 0
                lastTime = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        frameCount = 0
        lastTime = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
}
