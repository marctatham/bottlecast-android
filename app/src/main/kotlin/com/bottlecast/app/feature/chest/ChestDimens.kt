package com.bottlecast.app.feature.chest

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only the Chest uses, from spec §3.10, and the Chest boards (390×844) where the spec gives none. */
internal object ChestDimens {
    /** The compact dome behind the header: its edge at y≈180 of the spec's 800dp frame (spec §3.10). */
    val dome = DomeEdge(180f / 800f)

    /** The tab row that straddles the arc (spec §3.10). */
    val tabRowHeight = 48.dp

    /** A loading card: as tall as a card with a one-line excerpt (Chest-Found board). */
    val skeletonHeight = 96.dp
}
