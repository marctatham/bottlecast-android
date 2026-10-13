package com.bottlecast.app.feature.reveal

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only Reveal uses, from spec §3.7, and the Reveal board (390×844) where the spec gives none. */
internal object RevealDimens {
    /** The compact dome: its edge at y≈230 of the spec's 800 (spec §3.7). */
    val dome = DomeEdge(230f / 800f)

    /** The parchment card: at least 220dp tall, and no more than 60% of the screen (spec §3.7). */
    val cardMinHeight = 220.dp
    const val CARD_MAX_SHARE = 0.6f

    /** Messages longer than this use the smaller titleLargeLong (spec §3.7). */
    const val LONG_MESSAGE = 120

    /** The fade at the top and bottom of a long message that scrolls inside the card. */
    val cardFade = 16.dp

    /** The gap from the card to the actions (spec §3.7). */
    val actionsGap = 20.dp

    /** The empty bottle drifting under the actions (Reveal board): 64dp wide, lying almost on its side. */
    val driftingBottleWidth = 64.dp
    const val DRIFTING_BOTTLE_TILT = -64f
    const val DRIFTING_BOTTLE_ALPHA = 0.7f
    val driftingRipplesWidth = 240.dp

    /** The outlined flag: a 40dp circle in its 48dp target, with a 20dp icon (Reveal board). */
    val flagIcon = 20.dp

    /** The top bar reaches past the gutter, to 4dp from the screen's edges (the boards). */
    val topBarBleed = 12.dp
}
