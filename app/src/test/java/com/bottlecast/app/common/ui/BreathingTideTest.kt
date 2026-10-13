package com.bottlecast.app.common.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class)
class BreathingTideTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var running by mutableStateOf(true)
    private lateinit var tide: BreathingTide

    @Before
    fun setUp() {
        // Compose tests cancel infinite animations unless the clock is advanced by hand
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { tide = rememberBreathingTide(running = running) }
    }

    @Test
    fun `starts at rest, as the still tide is`() {
        // given
        // when
        // then
        Assert.assertEquals(BreathingTide.Still.edgeOffset(), tide.edgeOffset())
        Assert.assertEquals(BreathingTide.Still.glintPhase(), tide.glintPhase(), 0f)
        Assert.assertEquals(BreathingTide.Still.haloAlpha(), tide.haloAlpha(), 0f)
    }

    @Test
    fun `drifts while it runs`() {
        // given
        // when
        composeTestRule.mainClock.advanceTimeBy(QUARTER_OF_THE_GLINTS_LOOP)

        // then
        Assert.assertEquals(0.25f, tide.glintPhase(), TOLERANCE)
    }

    @Test
    fun `holds still while it isn't running, then carries on from there`() {
        // given
        composeTestRule.mainClock.advanceTimeBy(QUARTER_OF_THE_GLINTS_LOOP)
        val drifted = tide.glintPhase()

        // when
        setTideRunning(false)
        composeTestRule.mainClock.advanceTimeBy(QUARTER_OF_THE_GLINTS_LOOP)
        val held = tide.glintPhase()
        setTideRunning(true)
        composeTestRule.mainClock.advanceTimeBy(EIGHTH_OF_THE_GLINTS_LOOP)

        // then: had it started again from rest, it would be about an eighth of the way round
        Assert.assertEquals(drifted, held, TOLERANCE)
        Assert.assertTrue("carried on from $held to ${tide.glintPhase()}", tide.glintPhase() > held + 0.1f)
    }

    // the test thread's write reaches the composition once the main thread has gone idle
    private fun setTideRunning(value: Boolean) {
        running = value
        composeTestRule.waitForIdle()
    }

    private companion object {
        // the glints cross the sea every 12s
        const val QUARTER_OF_THE_GLINTS_LOOP = 3_000L
        const val EIGHTH_OF_THE_GLINTS_LOOP = 1_500L

        // a few frames
        const val TOLERANCE = 0.01f
    }
}
