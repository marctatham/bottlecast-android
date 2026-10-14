package com.bottlecast.app.common.ui

import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [timedControlMillis], which keeps Undo up long enough for everyone (spec §4).
 */
class AccessibleTimeoutsTest {

    @Test
    fun `a control stays up for the spec's minimum when nothing asks for longer`() {
        // when
        val millis = timedControlMillis(5_000, hasAction = true, touchExplorationEnabled = false) { it }

        // then
        Assert.assertEquals(5_000L, millis)
    }

    @Test
    fun `a longer recommended timeout from accessibility settings wins`() {
        // when
        val millis = timedControlMillis(5_000, hasAction = true, touchExplorationEnabled = false) { 20_000 }

        // then
        Assert.assertEquals(20_000L, millis)
    }

    @Test
    fun `a shorter recommendation never cuts below the minimum`() {
        // when
        val millis = timedControlMillis(5_000, hasAction = true, touchExplorationEnabled = false) { 1_000 }

        // then
        Assert.assertEquals(5_000L, millis)
    }

    @Test
    fun `with TalkBack on, a control with an action waits for the user`() {
        // when
        val millis = timedControlMillis(5_000, hasAction = true, touchExplorationEnabled = true) { it }

        // then
        Assert.assertNull(millis)
    }

    @Test
    fun `with TalkBack on, a message without an action still times out`() {
        // when
        val millis = timedControlMillis(5_000, hasAction = false, touchExplorationEnabled = true) { it }

        // then
        Assert.assertEquals(5_000L, millis)
    }
}
