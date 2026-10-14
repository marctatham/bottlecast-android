package com.bottlecast.app.feature.discover.domain

import kotlin.math.max

/** How the cork came out, for analytics. */
enum class UncorkMethod(val value: String) {
    Shake("shake"),
    Tap("tap"),
    Hold("hold"),
    Button("button"),
    Accessibility("accessibility"),
}

/** Something the user should feel or see as the cork loosens (spec §1.5's haptics). */
sealed interface UncorkEffect {

    /** A shake or tap loosened the cork: [step] 1 or 2 of [UncorkMachine.STEPS]. */
    data class Step(val step: Int) : UncorkEffect

    /** A held bottle reached the [third] third: 1 or 2. */
    data class HoldTick(val third: Int) : UncorkEffect

    /** The cork popped. [afterStep] when the last shake or tap popped it. */
    data class Pop(val method: UncorkMethod, val afterStep: Boolean) : UncorkEffect
}

/**
 * Loosening the cork (spec §3.6). Each shake or tap moves the ring on a third, and that progress
 * never decays. Holding the bottle fills the ring over [holdFillMillis]; letting go early drains it
 * back to what the shakes and taps had reached. A single activation (the Tap to uncork pill, a
 * TalkBack double-tap, a keyboard, reduced motion's Open bottle) pops it at once. It pops once.
 *
 * Times are milliseconds from any fixed start, from a monotonic clock.
 */
class UncorkMachine(
    initialSteps: Int = 0,
    private val holdFillMillis: Long = HOLD_FILL_MILLIS,
    private val holdDrainMillis: Long = HOLD_DRAIN_MILLIS,
    private val tapMaxMillis: Long = TAP_MAX_MILLIS,
) {

    private sealed interface Hold {
        data object Idle : Hold
        data class Filling(val startMillis: Long, val from: Float) : Hold
        data class Draining(val startMillis: Long, val from: Float) : Hold
    }

    /** Shakes and taps so far: never more than [STEPS] - 1, since the last one pops the cork. */
    var steps: Int = initialSteps.coerceIn(0, STEPS - 1)
        private set

    var popped: Boolean = false
        private set

    private var hold: Hold = Hold.Idle

    // the thirds this hold has already ticked for, so each ticks once
    private var holdTicked = 0

    /** True while the ring is still moving on its own, so the screen keeps sending [frame]s: filling under a hold, or draining after one. */
    val isMoving: Boolean get() = hold != Hold.Idle

    /** How full the ring is at [nowMillis], from 0 to 1. */
    fun progressAt(nowMillis: Long): Float = if (popped) 1f else max(stepLevel(), holdLevel(nowMillis))

    fun shake(): List<UncorkEffect> = step(UncorkMethod.Shake)

    /** Takes hold of the bottle, carrying on from wherever the ring is. */
    fun press(nowMillis: Long) {
        if (popped) return
        val from = progressAt(nowMillis)
        hold = Hold.Filling(startMillis = nowMillis, from = from)
        // thirds the ring is already past don't tick again
        holdTicked = (from * STEPS).toInt().coerceAtMost(STEPS - 1)
    }

    /** Lets go of the bottle. A press shorter than [tapMaxMillis] was a tap, and counts as one. */
    fun release(nowMillis: Long): List<UncorkEffect> = letGo(nowMillis, mayBeTap = true)

    /**
     * Ends a press that wasn't let go of on the bottle: a flick that scrolled, a drag off it, or the
     * bottle leaving the screen. The ring drains as after a hold, but it's never a tap.
     */
    fun cancel(nowMillis: Long): List<UncorkEffect> = letGo(nowMillis, mayBeTap = false)

    private fun letGo(nowMillis: Long, mayBeTap: Boolean): List<UncorkEffect> {
        val filling = hold as? Hold.Filling ?: return emptyList()
        val effects = frame(nowMillis)
        if (popped) return effects
        hold = Hold.Draining(startMillis = nowMillis, from = holdLevel(nowMillis))
        val tapped = mayBeTap && nowMillis - filling.startMillis < tapMaxMillis
        return if (tapped) effects + step(UncorkMethod.Tap) else effects
    }

    /** Moves a hold on to [nowMillis]: the ring fills or drains, ticks at each third, and pops when full. */
    fun frame(nowMillis: Long): List<UncorkEffect> {
        if (popped) return emptyList()
        return when (hold) {
            is Hold.Filling -> {
                val level = holdLevel(nowMillis)
                if (level >= 1f) return pop(UncorkMethod.Hold, afterStep = false)
                val effects = mutableListOf<UncorkEffect>()
                for (third in holdTicked + 1 until STEPS) {
                    // a third the shakes and taps already filled doesn't tick again
                    if (level < third / STEPS.toFloat()) break
                    holdTicked = third
                    if (third > steps) effects += UncorkEffect.HoldTick(third)
                }
                effects
            }

            is Hold.Draining -> {
                if (holdLevel(nowMillis) <= stepLevel()) hold = Hold.Idle
                emptyList()
            }

            Hold.Idle -> emptyList()
        }
    }

    /** Pops the cork at once. */
    fun activate(method: UncorkMethod): List<UncorkEffect> = if (popped) emptyList() else pop(method, afterStep = false)

    private fun step(method: UncorkMethod): List<UncorkEffect> {
        if (popped) return emptyList()
        steps += 1
        if (steps >= STEPS) {
            steps = STEPS - 1
            return pop(method, afterStep = true)
        }
        return listOf(UncorkEffect.Step(steps))
    }

    private fun pop(method: UncorkMethod, afterStep: Boolean): List<UncorkEffect> {
        popped = true
        hold = Hold.Idle
        return listOf(UncorkEffect.Pop(method, afterStep))
    }

    private fun stepLevel(): Float = steps / STEPS.toFloat()

    private fun holdLevel(nowMillis: Long): Float = when (val current = hold) {
        is Hold.Filling -> (current.from + (nowMillis - current.startMillis) / holdFillMillis.toFloat()).coerceAtMost(1f)
        is Hold.Draining -> max(stepLevel(), current.from - (nowMillis - current.startMillis) / holdDrainMillis.toFloat())
        Hold.Idle -> 0f
    }

    companion object {
        /** Three shakes or taps pop the cork. */
        const val STEPS = 3

        /** Holding fills the whole ring in 1.2s (spec §3.6). */
        const val HOLD_FILL_MILLIS = 1_200L
        const val HOLD_DRAIN_MILLIS = 600L

        /** A press shorter than this is a tap. */
        const val TAP_MAX_MILLIS = 300L
    }
}
