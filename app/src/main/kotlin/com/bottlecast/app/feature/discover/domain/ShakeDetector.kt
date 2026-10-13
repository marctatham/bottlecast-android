package com.bottlecast.app.feature.discover.domain

import kotlin.math.sqrt

/**
 * Picks shakes out of accelerometer samples (spec §3.6): a peak above [thresholdG] counts, then
 * nothing more for [debounceMillis], so one shake isn't counted twice.
 */
class ShakeDetector(
    private val thresholdG: Float = THRESHOLD_G,
    private val debounceMillis: Long = DEBOUNCE_MILLIS,
) {
    private var lastShakeMillis: Long? = null

    /** Takes one sample, in m/s², and returns true when it's a shake. */
    fun onSample(x: Float, y: Float, z: Float, timestampMillis: Long): Boolean {
        val force = sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY
        if (force <= thresholdG) return false
        val last = lastShakeMillis
        if (last != null && timestampMillis - last < debounceMillis) return false
        lastShakeMillis = timestampMillis
        return true
    }

    private companion object {
        const val THRESHOLD_G = 2.4f
        const val DEBOUNCE_MILLIS = 250L

        // the same value as SensorManager.STANDARD_GRAVITY, without needing Android to test
        const val STANDARD_GRAVITY = 9.80665f
    }
}
