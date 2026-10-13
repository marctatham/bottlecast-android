package com.bottlecast.app.feature.shore.components

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
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.BottleRig
import com.bottlecast.app.common.components.bottle.Ripples
import com.bottlecast.app.feature.shore.ShoreDimens

/**
 * State A's bottle (spec §3.3, the Main board): the rig leaning on the tide over its ripples, bobbing
 * up and back as [bob] goes from 0 to 1. It replaces the Lottie, which only the cast-success screen
 * keeps until slice 3.
 */
@Composable
internal fun ShoreBottle(
    height: Dp,
    bob: () -> Float,
    modifier: Modifier = Modifier,
    look: BottleLook = BottleLook.Default,
) {
    // the neck leans towards the start edge, mirrored in right-to-left layouts
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Ripples(
            modifier = Modifier
                .width(ShoreDimens.ripplesWidth)
                .offset(y = height / 2 - ShoreDimens.ripplesUnderBottle),
        )
        BottleRig(
            look = look,
            modifier = Modifier
                .height(height)
                .graphicsLayer {
                    val lean = ShoreDimens.BOTTLE_TILT + ShoreDimens.BOB_TILT * bob()
                    rotationZ = if (rtl) -lean else lean
                    scaleX = if (rtl) -1f else 1f
                    translationY = -ShoreDimens.bobHeight.toPx() * bob()
                    transformOrigin = TransformOrigin(0.5f, PIVOT_Y)
                },
        )
    }
}

// the bottle leans and bobs about a point near its base, where it meets the water
private const val PIVOT_Y = 0.85f
