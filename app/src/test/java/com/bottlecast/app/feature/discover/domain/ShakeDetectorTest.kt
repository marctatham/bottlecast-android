package com.bottlecast.app.feature.discover.domain

import org.junit.Assert
import org.junit.Test

/** Unit tests for [ShakeDetector]: a peak above 2.4g counts, then nothing for 250ms. */
class ShakeDetectorTest {

    private val detector = ShakeDetector()

    @Test
    fun `resting in a hand isn't a shake`() {
        // 1g, straight down
        Assert.assertFalse(detector.onSample(0f, 0f, 9.81f, timestampMillis = 0))
    }

    @Test
    fun `a sharp jolt is, whichever way it goes`() {
        Assert.assertTrue(detector.onSample(24f, -12f, 9f, timestampMillis = 0))
        Assert.assertTrue(ShakeDetector().onSample(-30f, 0f, 0f, timestampMillis = 0))
    }

    @Test
    fun `just under the threshold isn't enough, and just over is`() {
        Assert.assertFalse(detector.onSample(2.3f * 9.80665f, 0f, 0f, timestampMillis = 0))
        Assert.assertTrue(detector.onSample(2.5f * 9.80665f, 0f, 0f, timestampMillis = 0))
    }

    @Test
    fun `one shake's peaks within 250ms count once`() {
        // when
        val first = detector.onSample(30f, 0f, 0f, timestampMillis = 1_000)
        val echo = detector.onSample(30f, 0f, 0f, timestampMillis = 1_200)
        val next = detector.onSample(30f, 0f, 0f, timestampMillis = 1_250)

        // then
        Assert.assertTrue(first)
        Assert.assertFalse(echo)
        Assert.assertTrue(next)
    }
}
