package com.bottlecast.app.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        // Each halo layer is its own circle grown by the shadow's spread: spreading the disc's own
        // shadow would keep its corner radius and draw a rounded square. Layers are listed CSS-style,
        // first on top, so they're drawn last to first. They use dropShadow rather than a
        // graphicsLayer alpha, which would clip them to the disc.
        BottlingTheme.effects.moonHalo.asReversed().forEach { shadow ->
            Box(
                modifier = Modifier
                    .requiredSize(diameter + shadow.spread * 2)
                    .dropShadow(CircleShape) {
                        radius = shadow.radius.toPx()
                        color = shadow.color
                        alpha = haloAlpha()
                    }
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(BottlingTheme.colors.moonDisc, CircleShape)
        )
    }
}

@NightDawnPreviews
@Composable
private fun MoonDiscPreview() = BottlingPreview {
    Box(modifier = Modifier.padding(BottlingTheme.spacing.xxl)) { MoonDisc() }
}
