package com.bottlecast.app.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The moon: a soft disc with a halo, never a face (spec §1.6). Night shows the moon at 85%; Dawn a
 * pale sun-moon. Decorative, so it has no semantics.
 *
 * @param haloAlpha the halo's opacity, read at draw time so the breathing tide can animate it
 *   without recomposing. The disc itself never changes.
 */
@Composable
fun MoonDisc(
    modifier: Modifier = Modifier,
    diameter: Dp = BottlingTheme.sizes.moonDisc,
    haloAlpha: () -> Float = { 1f },
) {
    val halo = BottlingTheme.effects.moonHalo
    // the halo is drawn with dropShadow, never a graphicsLayer alpha, which would clip it to the disc;
    // the first shadow is outermost, so it's drawn first, underneath the rest
    val haloModifier = halo.fold(Modifier as Modifier) { acc, shadow ->
        acc.dropShadow(CircleShape) {
            radius = shadow.radius.toPx()
            spread = shadow.spread.toPx()
            color = shadow.color
            alpha = haloAlpha()
        }
    }
    Box(
        modifier = modifier
            .size(diameter)
            .then(haloModifier)
            .background(BottlingTheme.colors.moonDisc, CircleShape)
            .clearAndSetSemantics {},
    )
}

@NightDawnPreviews
@Composable
private fun MoonDiscPreview() = BottlingPreview {
    Box(modifier = Modifier.padding(BottlingTheme.spacing.xxl)) { MoonDisc() }
}
