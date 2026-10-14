package com.bottlecast.app.feature.castsuccess

import com.bottlecast.app.feature.castsuccess.components.CastTimeline
import com.bottlecast.app.feature.castsuccess.components.castPose
import org.junit.Assert
import org.junit.Test

/** The cast's phases (spec §1.5): roll, slip, cork, toss, ripples and settle, in about 2.4s. */
class CastTimelineTest {

    @Test
    fun `it starts with nothing done, and ends with everything done`() {
        // when
        val start = castPose(0f)
        val end = castPose(CastTimeline.END)

        // then
        Assert.assertEquals(0f, start.rolled)
        Assert.assertEquals(0f, start.tossed)
        Assert.assertEquals(0f, start.words)
        Assert.assertEquals(1f, end.rolled)
        Assert.assertEquals(1f, end.slipped)
        Assert.assertEquals(1f, end.corked, 0.0001f)
        Assert.assertEquals(1f, end.tossed)
        Assert.assertEquals(listOf(1f, 1f, 1f), end.rings)
        Assert.assertEquals(1f, end.settled)
        Assert.assertEquals(1f, end.words)
    }

    @Test
    fun `the parchment rolls in 280ms, then slips in by 500`() {
        // then
        Assert.assertEquals(0.5f, castPose(140f).rolled, 0.01f)
        Assert.assertEquals(0f, castPose(140f).slipped)
        Assert.assertEquals(1f, castPose(CastTimeline.ROLL_END).rolled)
        Assert.assertEquals(1f, castPose(CastTimeline.SLIP_END).slipped)
    }

    @Test
    fun `the cork overshoots a little as it springs on`() {
        // when
        val springing = (500..700 step 10).map { castPose(it.toFloat()).corked }

        // then
        Assert.assertTrue(springing.max() > 1f)
    }

    @Test
    fun `the ripples start 170ms apart, from the splash`() {
        // when
        val atSplash = castPose(CastTimeline.SPLASH + 1f).rings
        val later = castPose(CastTimeline.SPLASH + CastTimeline.RING_GAP + 1f).rings

        // then
        Assert.assertTrue(atSplash[0] > 0f && atSplash[1] == 0f)
        Assert.assertTrue(later[1] > 0f && later[2] == 0f)
    }

    @Test
    fun `the words fade in only once the bottle is out`() {
        // then
        Assert.assertEquals(0f, castPose(CastTimeline.WORDS_START).words)
        Assert.assertTrue(castPose(CastTimeline.WORDS_START + 100f).words > 0f)
    }
}
