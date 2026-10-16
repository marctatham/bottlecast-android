package com.bottlecast.app.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The wax seal (spec §1.6): a scalloped moon disc with an embossed bottle and the check that means
 * "Bottled by a verified account". It's the one visual that means on-chain, so it keeps its own
 * colours in both themes.
 *
 * @param stamp how far the seal has dropped onto the card (spec §1.5), read at draw time: 0 is lifted
 * at 1.4× and still clear, 1 is pressed on. A spring past 1 squashes it a little, then settles.
 * @param contentDescription null when the seal is part of a larger illustration with its own label.
 */
@Composable
fun WaxSeal(
    size: Dp,
    modifier: Modifier = Modifier,
    stamp: () -> Float = { 1f },
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.ic_glyph_wax_seal),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val landed = stamp()
                val scale = LIFTED_SCALE - (LIFTED_SCALE - 1f) * landed
                scaleX = scale
                scaleY = scale
                // it shows up quickly as it falls, rather than fading in on the card
                alpha = (landed * APPEAR_SPEED).coerceIn(0f, 1f)
            },
    )
}

private const val LIFTED_SCALE = 1.4f
private const val APPEAR_SPEED = 3f

@NightDawnPreviews
@Composable
private fun WaxSealPreview() = BottlingPreview {
    WaxSeal(size = BottlingTheme.sizes.touchTarget)
}
