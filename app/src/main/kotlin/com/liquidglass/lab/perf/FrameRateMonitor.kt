package com.liquidglass.lab.perf

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Arrays
import kotlin.math.roundToInt

@Immutable
data class FramePerfSnapshot(
    val fps: Int = 0,
    val avgFrameMs: Float = 0f,
    val p95FrameMs: Float = 0f,
    val maxFrameMs: Float = 0f,
    val targetHz: Int = 120
)

/**
 * Hardware frame performance monitor combining [Choreographer] vsync cadence with Android's
 * [Window.OnFrameMetricsAvailableListener] for accurate GPU+CPU frame render times (ms).
 */
class FrameRateMonitor(private val targetHz: Int = 120) {
    var snapshot by mutableStateOf(FramePerfSnapshot(targetHz = targetHz))
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var attachedWindow: Window? = null
    private var running = false

    private val frameTimesMs = FloatArray(90)
    private var sampleIdx = 0
    private var sampleCount = 0

    private var fpsWindowStartNs = 0L
    private var framesInWindow = 0
    private var lastPublishNs = 0L

    private val frameMetricsListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        val totalNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        if (totalNs > 0L && totalNs < 500_000_000L) {
            val ms = totalNs / 1_000_000f
            synchronized(frameTimesMs) {
                frameTimesMs[sampleIdx] = ms
                sampleIdx = (sampleIdx + 1) % frameTimesMs.size
                if (sampleCount < frameTimesMs.size) sampleCount++
            }
        }
    }

    private val choreographerCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (fpsWindowStartNs == 0L) {
                fpsWindowStartNs = frameTimeNanos
                lastPublishNs = frameTimeNanos
            }
            framesInWindow++
            val elapsedNs = frameTimeNanos - fpsWindowStartNs
            if (elapsedNs >= 450_000_000L) {
                val fps = ((framesInWindow * 1_000_000_000.0) / elapsedNs).roundToInt().coerceIn(0, 240)
                val (avg, p95, mx) = computeFrameStats()
                snapshot = FramePerfSnapshot(
                    fps = fps,
                    avgFrameMs = avg,
                    p95FrameMs = p95,
                    maxFrameMs = mx,
                    targetHz = targetHz
                )
                fpsWindowStartNs = frameTimeNanos
                framesInWindow = 0
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun start(window: Window) {
        if (running) return
        running = true
        attachedWindow = window
        runCatching {
            window.addOnFrameMetricsAvailableListener(frameMetricsListener, handler)
        }
        fpsWindowStartNs = 0L
        framesInWindow = 0
        Choreographer.getInstance().postFrameCallback(choreographerCallback)
    }

    fun stop() {
        if (!running) return
        running = false
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
        attachedWindow?.let { w ->
            runCatching { w.removeOnFrameMetricsAvailableListener(frameMetricsListener) }
        }
        attachedWindow = null
    }

    private fun computeFrameStats(): Triple<Float, Float, Float> = synchronized(frameTimesMs) {
        if (sampleCount == 0) return Triple(0f, 0f, 0f)
        val copy = FloatArray(sampleCount)
        var sum = 0f
        var mx = 0f
        for (i in 0 until sampleCount) {
            val v = frameTimesMs[i]
            copy[i] = v
            sum += v
            if (v > mx) mx = v
        }
        Arrays.sort(copy)
        val p95Index = ((sampleCount - 1) * 0.95f).roundToInt().coerceIn(0, sampleCount - 1)
        Triple(sum / sampleCount, copy[p95Index], mx)
    }
}
