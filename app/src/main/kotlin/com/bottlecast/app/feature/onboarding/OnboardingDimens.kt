package com.bottlecast.app.feature.onboarding

import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.DomeEdge

/** Sizes only onboarding uses, from spec §3.1 and the Onboarding boards (390×844) where it's silent. */
internal object OnboardingDimens {
    /** The dome's edge sits higher than the Shore's, so the copy fits (spec §6). */
    val dome = DomeEdge(440f / 844f)

    /** The moon's centre, as shares of the screen (spec §3.1). */
    const val MOON_X = 0.72f
    const val MOON_Y = 0.12f

    /** The illustration: about 240dp wide, or 140dp once text passes 150% and the copy scrolls (spec §3.1). */
    val art = 240.dp
    val artLargeText = 140.dp
    const val LARGE_TEXT_FROM = 1.5f

    /** The headline starts this far under the arc (the boards' 468, against an edge at 440). */
    val copyBelowArc = 28.dp

    /** Page dots: 8dp, the current one a 24×8 pill, 8dp apart, 16dp over the pills (spec §3.1, the boards). */
    val dot = 8.dp
    val dotsAbovePills = 16.dp
    val activeDot = 24.dp

    /** The pills sit 24dp above the gesture inset (spec §3.1); room for two keeps the dots still. */
    val pillsBottom = 24.dp
    val pillsAreaMinHeight = 120.dp

    /** Page 1: the Main board's bottle rig. */
    val bottleHeight = 188.dp

    /** Page 2: the tide ring under the moon, and the bottle lying on its sand. */
    const val TIDE_SHARE = 2f / 3f
    val sandHeight = 28.dp
    val sandWidth = 220.dp
    val lyingBottleHeight = 104.dp
    const val LYING_BOTTLE_TILT = -78f

    /** Page 3: the parchment, tilted 4°, with the seal overhanging its corner (Onboarding-3 board). */
    const val PARCHMENT_TILT = -4f
    val seal = 72.dp
    const val SEAL_TILT = 12f
    val sealOverhangX = 22.dp
    val sealOverhangY = 28.dp
}
