package com.bottlecast.app.feature.discover.components

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Everything that moves on Discover, each read at draw time so the motion redraws without
 * recomposing. The screen drives these from its animations; screenshot tests pass fixed values, so
 * no capture catches a running animation.
 */
@Stable
internal class DiscoverMotion(
    /** The bottle's wobble from a shake or tap, in degrees. */
    val wobble: () -> Float = { 0f },
    /** How far up its bob the bottle is, from 0 to 1. */
    val bob: () -> Float = { 0f },
    val corkFlight: () -> Float = { 0f },
    val scrollOut: () -> Float = { 0f },
    /** Moonlight washing over the dome as the cork pops. */
    val flash: () -> Float = { 0f },
    /** The sheen on the scroll while the message is late, or null. */
    val shimmer: () -> Float? = { null },
    val glintPhase: () -> Float = { 0f },
    val haloAlpha: () -> Float = { 1f },
    val edgeOffset: () -> Dp = { 0.dp },
) {
    companion object {
        /** Everything at rest, before the pop. */
        val Still = DiscoverMotion()

        /** After the pop: the cork gone, and the scroll [scrollOut] of the way out. */
        fun popped(scrollOut: Float, shimmer: Float? = null) = DiscoverMotion(
            corkFlight = { 1f },
            scrollOut = { scrollOut },
            shimmer = { shimmer },
        )
    }
}
