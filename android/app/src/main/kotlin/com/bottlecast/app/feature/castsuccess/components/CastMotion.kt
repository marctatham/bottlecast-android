package com.bottlecast.app.feature.castsuccess.components

import androidx.compose.runtime.Stable

/**
 * Cast success's motion, read at draw time (spec §1.5): [timeline] is the cast's moment in
 * milliseconds, from 0 to [CastTimeline.END]; [bob] and [pulse] are the settled bottle's bob and the
 * Drifting dot's pulse, from 0 to 1, which hold still under reduced motion.
 */
@Stable
internal class CastMotion(
    val timeline: () -> Float,
    val bob: () -> Float,
    val pulse: () -> Float,
) {
    companion object {
        /** A still frame at [millis], for previews and screenshot tests. */
        fun at(millis: Float) = CastMotion(timeline = { millis }, bob = { 0f }, pulse = { 1f })

        val Settled = at(CastTimeline.END)
    }
}
