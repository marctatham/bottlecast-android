package com.bottlecast.app.feature.keepsake

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only Keepsake detail uses, from spec §3.11, and the Keepsake-Detail board where the spec gives none. */
internal object KeepsakeDimens {
    /** The compact dome: its edge at y=236 of the board's 844 (Keepsake-Detail board). */
    val dome = DomeEdge(236f / 844f)

    /** A journey stop's dot (spec §3.11), and how far down it sits, level with the label's middle. */
    val journeyDot = 12.dp
    val journeyDotTop = 4.dp

    /** The gap from the card to the status row, and between the sections below (Keepsake-Detail board). */
    val sectionGap = 20.dp

    /** The top bar reaches past the gutter, to 4dp from the screen's edges (the boards). */
    val topBarBleed = 12.dp
}
