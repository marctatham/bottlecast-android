package com.bottlecast.app.feature.signin

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** Sizes only the sign-in sheet uses, from spec §3.2 and the SignIn-Sheet board (390×844) where it's silent. */
internal object SignInDimens {
    /** The 64dp glassContainer disc and its bottle glyph, drawn at 36dp. */
    val glyph = 64.dp
    val glyphIcon = 36.dp

    /** The moon dot on the glyph's top-end edge, 4dp in and 2dp down, and its glow (0 0 12px 3px, moon at 35%). */
    val moonDot = 14.dp
    val moonDotOffset = DpOffset((-4).dp, 2.dp)
    val moonDotGlowBlur = 12.dp
    val moonDotGlowSpread = 3.dp
    const val MOON_DOT_GLOW_ALPHA = 0.35f

    /** The carry-over chip: a pill that wraps its words onto two lines if it must. */
    val chipPaddingVertical = 6.dp
    val chipPaddingStart = 10.dp
    val chipPaddingEnd = 16.dp
}
