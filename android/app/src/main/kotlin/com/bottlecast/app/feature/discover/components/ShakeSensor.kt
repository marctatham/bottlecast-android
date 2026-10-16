package com.bottlecast.app.feature.discover.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.bottlecast.app.feature.discover.domain.ShakeDetector

/**
 * Listens for shakes while the screen is resumed and [enabled] (spec §3.6): the accelerometer at
 * game rate, registered only while resumed, so a paused screen never counts one.
 */
@Composable
internal fun ShakeEffect(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)
    val sensorManager = remember(context) { context.getSystemService(SensorManager::class.java) }
    val accelerometer = remember(sensorManager) { sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (!enabled || sensorManager == null || accelerometer == null) return

    LifecycleResumeEffect(sensorManager, accelerometer) {
        val detector = ShakeDetector()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                if (detector.onSample(x, y, z, event.timestamp / NANOS_PER_MILLI)) currentOnShake()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose { sensorManager.unregisterListener(listener) }
    }
}

/** Whether the phone has an accelerometer; without one, Discover asks for taps instead. */
@Composable
internal fun rememberHasAccelerometer(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }
}

private const val NANOS_PER_MILLI = 1_000_000L
