package com.bottlecast.app.feature.discover

import androidx.compose.ui.unit.dp

/**
 * Sizes only Discover uses, from spec §3.6, and the Discover-Idle and Discover-Progress boards
 * (390×844) where the spec gives none.
 */
internal object DiscoverDimens {
    /** The bottle rig's height (spec §3.6). */
    val bottleHeight = 200.dp

    /** The bottle leans this far (spec §3.6), its neck towards the start edge. */
    const val BOTTLE_TILT = -12f

    /** Each shake or tap springs the bottle this far (spec §1.5). */
    const val WOBBLE_DEGREES = 8f

    /** How far the bottle bobs, at the top of its swell (the boards' float). */
    val bobHeight = 6.dp

    /** The cork ring around the neck (spec §3.6), and the ticks at its thirds. */
    val corkRing = 72.dp
    val corkRingStroke = 4.dp
    val corkRingTick = 10.dp
    val corkRingTickWidth = 2.dp

    /** The faint sand mound under the bottle. */
    val moundWidth = 220.dp
    val moundHeight = 36.dp

    /** The text ends this far above the dome's edge, and the pill starts this far below it. */
    val textAboveArc = 32.dp
    val pillBelowArc = 40.dp

    /**
     * The moon, from the status bar and the end edge. The Discover-Idle board puts it 20dp down, but
     * the spec's help button sits there, so it hangs just below the top bar instead.
     */
    val moonTop = 72.dp
    val moonEnd = 40.dp

    /** The top bar reaches past the gutter, to 4dp from the screen's edges (the boards). */
    val topBarBleed = 12.dp
}
