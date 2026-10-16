package com.bottlecast.app.common.components.bottle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A corked bottle floating on the tide (the Main, Onboarding-1 and Cast-Success boards): the rig
 * leaning over its ripples, bobbing up and back as [bob] goes from 0 to 1. The Shore's state A,
 * onboarding's first page and cast success float one.
 */
@Composable
fun FloatingBottle(
    height: Dp,
    bob: () -> Float,
    modifier: Modifier = Modifier,
    look: BottleLook = BottleLook.Default,
    ripplesWidth: Dp = FloatingBottleDefaults.ripplesWidth,
) {
    // the neck leans towards the start edge, mirrored in right-to-left layouts
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Ripples(
            modifier = Modifier
                .width(ripplesWidth)
                .offset(y = height / 2 - FloatingBottleDefaults.ripplesUnderBottle),
        )
        BottleRig(
            look = look,
            modifier = Modifier
                .height(height)
                .graphicsLayer {
                    val lean = TILT + BOB_TILT * bob()
                    rotationZ = if (rtl) -lean else lean
                    scaleX = if (rtl) -1f else 1f
                    translationY = -BobHeight.toPx() * bob()
                    transformOrigin = TransformOrigin(0.5f, PIVOT_Y)
                },
        )
    }
}

/** The floating bottle's sizes, from the boards. */
object FloatingBottleDefaults {
    /** 240dp of ripples under the Main board's 188dp bottle. */
    val ripplesWidth = 240.dp

    /** The ripples sit this far above the bottle's foot, where it meets the water. */
    val ripplesUnderBottle = 12.dp
}

// It leans 14° on the tide, and at the top of the swell it's up 6dp and 5° more upright (the
// boards' float).
private const val TILT = -14f
private const val BOB_TILT = 5f
private val BobHeight = 6.dp

// the bottle leans and bobs about a point near its base, where it meets the water
private const val PIVOT_Y = 0.85f
