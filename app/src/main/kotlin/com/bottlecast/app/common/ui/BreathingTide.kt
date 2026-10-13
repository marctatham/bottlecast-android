package com.bottlecast.app.common.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Shore's breathing tide (spec §1.5, signature moment 1):
 * - the dome's edge rises and falls ±2dp on an 8s sine;
 * - three moon glints drift across the sea over 12s;
 * - the moon's halo brightens between 0.9 and 1.0 over 6s.
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
        /** No motion: under reduced motion, while the screen isn't resumed, and in screenshot tests. */
        val Still = BreathingTide(edgeOffset = { 0.dp }, glintPhase = { 0f }, haloAlpha = { 1f })
    }
}

/**
 * The breathing tide, running only while the screen is resumed and motion isn't reduced; it's
 * [BreathingTide.Still] otherwise. It's built on an infinite transition, which Compose tests know
 * to skip, so they can still go idle.
 */
@Composable
fun rememberBreathingTide(): BreathingTide {
    val reducedMotion = rememberReducedMotion()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    if (reducedMotion || !lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) return BreathingTide.Still

    val transition = rememberInfiniteTransition(label = "breathing tide")
    val edgePhase = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(EDGE_PERIOD_MILLIS, easing = LinearEasing)), label = "edge")
    val glintPhase = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(GLINT_PERIOD_MILLIS, easing = LinearEasing)), label = "glints")
    val haloPhase = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(HALO_PERIOD_MILLIS, easing = LinearEasing)), label = "halo")
    return remember(transition) {
        BreathingTide(
            edgeOffset = { EDGE_AMPLITUDE * sin(FULL_TURN * edgePhase.value) },
            glintPhase = { glintPhase.value },
            // starts dim, peaks halfway, like the boards' ease-in-out pulse
            haloAlpha = { HALO_MID - HALO_AMPLITUDE * cos(FULL_TURN * haloPhase.value) },
        )
    }
}

private const val EDGE_PERIOD_MILLIS = 8_000
private const val GLINT_PERIOD_MILLIS = 12_000
private const val HALO_PERIOD_MILLIS = 6_000
private val EDGE_AMPLITUDE = 2.dp
private const val HALO_MID = 0.95f
private const val HALO_AMPLITUDE = 0.05f
private const val FULL_TURN = (2 * PI).toFloat()
