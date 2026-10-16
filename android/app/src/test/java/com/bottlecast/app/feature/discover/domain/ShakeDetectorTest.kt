package com.bottlecast.app.feature.discover.domain

import org.junit.Assert
import org.junit.Test

/** Unit tests for [ShakeDetector]: a peak above 2.4g counts, then nothing for 250ms. */
class ShakeDetectorTest {

    private val detector = ShakeDetector()

    @Test
    fun `resting in a hand isn't a shake`() {
        // given: 1g, straight down
        val resting = floatArrayOf(0f, 0f, 9.81f)

        // when
        val shook = detector.onSample(resting[0], resting[1], resting[2], timestampMillis = 0)

        // then
        Assert.assertFalse(shook)
    }

    @Test
    fun `a sharp jolt is, whichever way it goes`() {
        // given
        val other = ShakeDetector()

        // when
        val sideways = detector.onSample(24f, -12f, 9f, timestampMillis = 0)
        val backwards = other.onSample(-30f, 0f, 0f, timestampMillis = 0)

        // then
        Assert.assertTrue(sideways)
        Assert.assertTrue(backwards)
    }

    @Test
    fun `just under the threshold isn't enough, and just over is`() {
        // given
        val g = 9.80665f

        // when
        val under = detector.onSample(2.3f * g, 0f, 0f, timestampMillis = 0)
        val over = detector.onSample(2.5f * g, 0f, 0f, timestampMillis = 0)

        // then
        Assert.assertFalse(under)
        Assert.assertTrue(over)
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
