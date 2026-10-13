package com.bottlecast.app.common.ui

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Shore's breathing tide (spec §1.5, signature moment 1):
 * - the dome's edge rises and falls ±2dp on an 8s sine;
 * - three moon glints drift across the sea over 12s;
 * - the moon's halo dims to 0.9 and brightens back to 1.0 over 6s.
 *
 * Each value is a lambda read at draw time, so the tide redraws without recomposing.
 */
@Stable
class BreathingTide(
    /** How far the dome's edge sits from its resting place. */
    val edgeOffset: () -> Dp,
    /** How far the glints have drifted, from 0 to 1 across the screen, then round again. */
    val glintPhase: () -> Float,
    /** The moon's halo opacity. */
    val haloAlpha: () -> Float,
) {
    companion object {
        /** The tide at rest, where it starts: for previews and screenshot tests. */
        val Still = BreathingTide(edgeOffset = { 0.dp }, glintPhase = { 0f }, haloAlpha = { 1f })
    }
}

/**
 * The breathing tide. It runs while idle motion may ([rememberIdleMotionEnabled]); otherwise it
 * holds still where it is, and carries on from there, so nothing jumps when the screen comes back.
 * It's driven by infinite-animation frames, which Compose tests cancel, so tests see it at rest
 * and can still go idle.
 */
@Composable
fun rememberBreathingTide(): BreathingTide {
    val running = rememberIdleMotionEnabled()
    // the tide's own clock, which only moves while the tide runs
    val elapsedMillis = remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val startMillis = withInfiniteAnimationFrameMillis { it } - elapsedMillis.longValue
        while (true) {
            withInfiniteAnimationFrameMillis { frameMillis -> elapsedMillis.longValue = frameMillis - startMillis }
        }
    }
    return remember {
        BreathingTide(
            edgeOffset = { EDGE_AMPLITUDE * sin(FULL_TURN * elapsedMillis.phase(EDGE_PERIOD_MILLIS)) },
            glintPhase = { elapsedMillis.phase(GLINT_PERIOD_MILLIS) },
            // starts bright, as at rest, and dims halfway, like the boards' ease-in-out pulse
            haloAlpha = { HALO_MID + HALO_AMPLITUDE * cos(FULL_TURN * elapsedMillis.phase(HALO_PERIOD_MILLIS)) },
        )
    }
}

/** How far through a loop of [periodMillis] the clock is, from 0 to 1. */
private fun LongState.phase(periodMillis: Int): Float = (longValue % periodMillis).toFloat() / periodMillis

private const val EDGE_PERIOD_MILLIS = 8_000
private const val GLINT_PERIOD_MILLIS = 12_000
private const val HALO_PERIOD_MILLIS = 6_000
private val EDGE_AMPLITUDE = 2.dp
private const val HALO_MID = 0.95f
private const val HALO_AMPLITUDE = 0.05f
private const val FULL_TURN = (2 * PI).toFloat()
