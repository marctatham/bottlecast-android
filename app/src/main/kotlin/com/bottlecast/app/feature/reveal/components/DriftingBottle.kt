package com.bottlecast.app.feature.reveal.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.BottleRig
import com.bottlecast.app.common.components.bottle.Ripples
import com.bottlecast.app.feature.reveal.RevealDimens

/** The empty bottle, drifting on its side once its message is out (Reveal board). Decorative. */
@Composable
internal fun DriftingBottle(look: BottleLook, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Ripples(modifier = Modifier.width(RevealDimens.driftingRipplesWidth).offset(y = 24.dp))
        BottleRig(
            look = look,
            corked = false,
            showScroll = false,
            modifier = Modifier
                .width(RevealDimens.driftingBottleWidth)
                .graphicsLayer {
                    rotationZ = RevealDimens.DRIFTING_BOTTLE_TILT
                    alpha = RevealDimens.DRIFTING_BOTTLE_ALPHA
                },
        )
    }
}
