package com.bottlecast.app.feature.me

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only the Me tab uses, from spec §3.12, and the Profile board where the spec gives none. */
internal object MeDimens {
    /** The compact dome: its edge at y≈260 of the spec's 800dp frame (spec §3.12). */
    val dome = DomeEdge(260f / 800f)

    /** How far the stats card reaches above the arc it straddles: y≈236 against the edge at 260 (spec §3.12). */
    val cardAboveArc = 24.dp

    /** The stats card's padding (Profile board). */
    val cardPaddingTop = 20.dp
    val cardPaddingHorizontal = 12.dp
    val cardPaddingBottom = 16.dp
}
