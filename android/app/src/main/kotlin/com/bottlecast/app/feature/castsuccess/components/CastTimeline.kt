package com.bottlecast.app.feature.castsuccess.components

import kotlin.math.pow

/**
 * The cast (spec §1.5), about 2.4s: the parchment rolls up and slips into the bottle, the cork goes
 * on, the bottle is tossed out to the horizon, three ripples spread as it lands, and the dome settles.
 * [castPose] turns a moment of it, in milliseconds, into where everything is, so a screenshot can
 * stop at any frame.
 */
internal object CastTimeline {
    const val ROLL_END = 280f
    const val SLIP_END = 500f
    const val CORK_END = 700f

    /** The cork lands, with a thud. */
    const val CORK_LANDS = 600f
    const val TOSS_END = 1_600f

    /** The bottle meets the water, with a thud. */
    const val SPLASH = TOSS_END
    const val RING_GAP = 170f
    // the third ripple, 340ms after the splash, still finishes before the end
    const val RING_SPREAD = 450f
    const val SETTLE_END = 2_400f

    /** The words fade in over the last half-second; reduced motion starts here, for a 200ms crossfade. */
    const val WORDS_START = 1_900f
    const val END = SETTLE_END
}

/** Where the cast is, at one moment. Each part goes from 0 (not yet) to 1 (done). */
internal data class CastPose(
    /** The parchment rolling up: its height goes from 1 to 0.08. */
    val rolled: Float,
    /** The rolled scroll slipping into the bottle. */
    val slipped: Float,
    /** The cork going on, overshooting a little as a spring would. */
    val corked: Float,
    /** The toss, along its arc out to the horizon. */
    val tossed: Float,
    /** Each of the three ripples, spreading and fading. */
    val rings: List<Float>,
    /** The dome settling from 70% to 60%. */
    val settled: Float,
    /** The words, title to button, fading in. */
    val words: Float,
)

internal fun castPose(millis: Float): CastPose = with(CastTimeline) {
    CastPose(
        rolled = progress(millis, 0f, ROLL_END),
        slipped = progress(millis, ROLL_END, SLIP_END),
        corked = easeOutBack(progress(millis, SLIP_END, CORK_END)),
        tossed = easeInOut(progress(millis, CORK_END, TOSS_END)),
        rings = List(RINGS) { ring -> progress(millis, SPLASH + ring * RING_GAP, SPLASH + ring * RING_GAP + RING_SPREAD) },
        settled = easeInOut(progress(millis, SPLASH, SETTLE_END)),
        words = progress(millis, WORDS_START, END),
    )
}

internal const val RINGS = 3

private fun progress(millis: Float, from: Float, until: Float): Float = ((millis - from) / (until - from)).coerceIn(0f, 1f)

private fun easeInOut(t: Float): Float = if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

// overshoots by about 10% before settling, the bounce of a spatial spring
private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
}
