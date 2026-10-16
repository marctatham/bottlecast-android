package com.bottlecast.app.feature.castsuccess

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only cast success uses, from spec §1.5 and §3.5, and the Cast-Success board where it's silent. */
internal object CastSuccessDimens {
    /** The dome starts expanded, as Preview left the bottle, and settles to full (spec §1.5). */
    val startDome = DomeEdge.Expanded
    val dome = DomeEdge.Full

    /** The bottle starts at the rig's 188dp, and ends at 12% of it on the horizon (spec §1.5, §3.5). */
    val bottle = 188.dp
    const val FAR_SCALE = 0.12f
    const val TOSS_TILT = -25f

    /** Where it starts, as a share of the screen's height, and how far above the arc it floats in the end. */
    const val START_Y = 0.36f
    val horizonAboveArc = 36.dp

    /** How high the toss arcs above a straight line, as a share of the screen's height. */
    const val TOSS_ARC = 0.18f

    /** The parchment that rolls into the bottle: about a card, above the neck. */
    val parchmentWidth = 160.dp
    val parchmentHeight = 104.dp
    val parchmentAboveBottle = 24.dp
    val parchmentCorner = 12.dp
    val inkLine = 3.dp

    /** The bottle's bob once it's landed, as the boards' float: up 6dp and 5° more upright. */
    val bobHeight = 6.dp

    /** The ripples under the far bottle: three rings, the widest 132dp across (Cast-Success board). */
    val ripplesWidth = 132.dp
    val ringStroke = 1.5.dp

    /** The moon sits as it does on the Shore, 60dp in from the top-end below the status bar. */
    val moonInset = 60.dp

    /** The title sits this far above the arc (spec §3.5: y≈250 of the board's 506). */
    val titleAboveArc = 256.dp

    /** The journey row sits this far under the arc (spec §3.5: y≈540). */
    val journeyBelowArc = 34.dp

    /** The journey's chips: 32dp pills, joined by 12dp lines 4dp from each (Cast-Success board). */
    val connector = 12.dp
    val connectorGap = 4.dp
    val pulseDot = 8.dp
    val journeyRowGap = 8.dp
}
