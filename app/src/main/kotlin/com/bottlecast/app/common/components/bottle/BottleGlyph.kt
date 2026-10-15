package com.bottlecast.app.common.components.bottle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * A bottle in a list (spec §3.10): the rig in its seeded silhouette and tint, small, on a circle in the
 * tint's container colour. It's decorative; the row it sits in says what it is.
 */
@Composable
fun BottleGlyph(look: BottleLook, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BottlingTheme.sizes.bottleGlyph)
            .background(look.tint.container(), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BottleRig(look = look, modifier = Modifier.height(BottlingTheme.sizes.bottleGlyphRig))
    }
}

// the circle behind each tint (Chest boards): glass behind sea glass, moon behind amber, sage behind moonglass
@Composable
private fun BottleTint.container(): Color = when (this) {
    BottleTint.SeaGlass -> BottlingTheme.colors.glassContainer
    BottleTint.Amber -> BottlingTheme.colors.moonContainer
    BottleTint.Moonglass -> MaterialTheme.colorScheme.primaryContainer
}

@NightDawnPreviews
@Composable
private fun BottleGlyphPreview() = BottlingPreview {
    Row(modifier = Modifier.padding(BottlingTheme.spacing.m)) {
        BottleSilhouette.entries.zip(BottleTint.entries).forEach { (silhouette, tint) ->
            BottleGlyph(look = BottleLook(silhouette, tint), modifier = Modifier.padding(BottlingTheme.spacing.xs))
        }
    }
}
