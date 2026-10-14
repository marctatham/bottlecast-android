package com.bottlecast.app.feature.discover.domain

import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [UncorkMachine]: shakes and taps loosen the cork a third at a time and never undo,
 * a hold fills the ring over 1.2s and drains if let go early, and one activation pops it at once.
 */
class UncorkMachineTest {

    private val machine = UncorkMachine()

    @Test
    fun `three shakes pop the cork, the last one leading in with its own tick`() {
        // when
        val first = machine.shake()
        val second = machine.shake()
        val third = machine.shake()

        // then
        Assert.assertEquals(listOf(UncorkEffect.Step(1)), first)
        Assert.assertEquals(listOf(UncorkEffect.Step(2)), second)
        Assert.assertEquals(listOf(UncorkEffect.Pop(UncorkMethod.Shake, afterStep = true)), third)
        Assert.assertTrue(machine.popped)
    }

    @Test
    fun `progress from shakes never decays`() {
        // given
        machine.shake()

        // when
        val later = machine.progressAt(nowMillis = 60_000)

        // then
        Assert.assertEquals(1f / 3, later, 0.0001f)
    }

    @Test
    fun `a quick press is a tap, and counts as a step`() {
        // when
        machine.press(nowMillis = 0)
        val effects = machine.release(nowMillis = 100)

        // then
        Assert.assertEquals(listOf(UncorkEffect.Step(1)), effects)
        Assert.assertEquals(1, machine.steps)
    }

    @Test
    fun `holding the bottle fills the ring and pops it after 1·2s`() {
        // given
        machine.press(nowMillis = 0)

        // when
        val halfway = machine.frame(nowMillis = 600)
        val progress = machine.progressAt(nowMillis = 600)
        val full = machine.frame(nowMillis = 1_200)

        // then
        Assert.assertEquals(listOf(UncorkEffect.HoldTick(1)), halfway)
        Assert.assertEquals(0.5f, progress, 0.0001f)
        Assert.assertEquals(listOf(UncorkEffect.Pop(UncorkMethod.Hold, afterStep = false)), full)
    }

    @Test
    fun `each third ticks once as a hold fills, and not for thirds the shakes already filled`() {
        // given
        machine.shake()
        machine.press(nowMillis = 0)

        // when: a hold carries on from a third, so it reaches two thirds after 400ms (and pops at 800ms)
        val ticks = (50L..750L step 50L).flatMap { machine.frame(it) }

        // then
        Assert.assertEquals(listOf(UncorkEffect.HoldTick(2)), ticks)
    }

    @Test
    fun `letting go early drains the ring back to what the shakes reached`() {
        // given
        machine.shake()
        machine.press(nowMillis = 0)
        machine.frame(nowMillis = 600)

        // when
        machine.release(nowMillis = 600)
        val draining = machine.progressAt(nowMillis = 700)
        machine.frame(nowMillis = 5_000)
        val settled = machine.progressAt(nowMillis = 5_000)

        // then
        Assert.assertTrue(draining > 1f / 3 && draining < 1f)
        Assert.assertEquals(1f / 3, settled, 0.0001f)
        Assert.assertFalse(machine.isMoving)
        Assert.assertFalse(machine.popped)
    }

    @Test
    fun `a single activation pops the cork at once`() {
        // when
        val effects = machine.activate(UncorkMethod.Accessibility)

        // then
        Assert.assertEquals(listOf(UncorkEffect.Pop(UncorkMethod.Accessibility, afterStep = false)), effects)
        Assert.assertEquals(1f, machine.progressAt(0), 0f)
    }

    @Test
    fun `once popped, nothing pops it again`() {
        // given
        machine.activate(UncorkMethod.Button)

        // when
        val effects = machine.shake() + machine.activate(UncorkMethod.Button) + machine.release(0) + machine.frame(0)
        machine.press(nowMillis = 0)

        // then
        Assert.assertTrue(effects.isEmpty())
        Assert.assertFalse(machine.isMoving)
    }

    @Test
    fun `a press that a scroll takes, however short, is no tap`() {
        // given
        val machine = UncorkMachine()
        machine.press(nowMillis = 0)

        // when
        val effects = machine.cancel(nowMillis = 100)

        // then
        Assert.assertTrue(effects.isEmpty())
        Assert.assertEquals(0, machine.steps)
    }

    @Test
    fun `a cancelled hold drains back to the taps and shakes`() {
        // given
        val machine = UncorkMachine()
        machine.shake()
        machine.press(nowMillis = 0)
        machine.frame(nowMillis = 600)

        // when
        machine.cancel(nowMillis = 600)
        machine.frame(nowMillis = 600 + UncorkMachine.HOLD_DRAIN_MILLIS)

        // then
        Assert.assertEquals(1f / 3, machine.progressAt(600 + UncorkMachine.HOLD_DRAIN_MILLIS), 0.0001f)
        Assert.assertFalse(machine.isMoving)
        Assert.assertFalse(machine.popped)
    }

    @Test
    fun `progress saved before a restart carries on, short of popping`() {
        // when
        val restored = UncorkMachine(initialSteps = 2)
        val tooFar = UncorkMachine(initialSteps = 5)

        // then
        Assert.assertEquals(2, restored.steps)
        Assert.assertEquals(2, tooFar.steps)
        Assert.assertEquals(listOf(UncorkEffect.Pop(UncorkMethod.Shake, afterStep = true)), restored.shake())
    }
}
