package com.bottlecast.app.feature.shore

import androidx.compose.ui.unit.dp

/**
 * Sizes only the Shore uses, from the Main, Shore-A-Dawn and Shore-B boards (390×844) where the
 * spec gives none. Positions are measured from the dome's edge, the status bar or the navigation
 * bar, so they hold on any screen height.
 */
internal object ShoreDimens {
    /** The top row sits this far under the status bar. */
    val topRowOffset = 4.dp

    /** The streak pill: 36dp inside its 48dp target (spec §3.3). */
    val streakPillHeight = 36.dp
    val streakPillPaddingStart = 10.dp
    val streakPillPaddingEnd = 12.dp
    val shieldIcon = 14.dp

    /** The bottle rig (Main board): 188dp tall, floating on the tide. */
    val bottleHeight = 188.dp

    /** Below this height the bottle drops out. */
    val bottleMinHeight = 96.dp

    /** State A: from the body's last line down to the dome's edge. */
    val textAboveArc = 74.dp

    /** State B: how far the today's-find card rises above the dome's edge, to straddle it. */
    val cardAboveArc = 38.dp

    /** The stats: three items 24dp apart in one 48dp target (spec §3.3). */
    val statsGap = 24.dp
    val statsAbovePills = 20.dp

    /** The last pill sits this far above the navigation bar. */
    val pillsAboveBar = 32.dp

    /** State A's moon: 40dp, this far in from the top-end of the screen below the status bar. */
    val moonInset = 60.dp

    /** State B's tide ring sits this close under the top row, and the countdown under it. */
    val belowTopRow = 2.dp
    val belowTideRing = 4.dp

    /** The reminder chip: a 32dp pill in a 48dp target. */
    val reminderChipHeight = 32.dp
    val reminderChipPaddingStart = 12.dp
    val reminderChipPaddingEnd = 16.dp

    /** The today's-find card. */
    val findCardMinHeight = 64.dp
    val findCardPaddingStart = 20.dp
    val findCardPaddingEnd = 12.dp
    val findCardPaddingVertical = 12.dp
}
