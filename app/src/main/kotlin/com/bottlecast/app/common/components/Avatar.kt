package com.bottlecast.app.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The user's avatar: a plain moonContainer circle (decisions.md, 2026-10-13) with a person glyph,
 * until slice 4 settles which name its initial comes from. Decorative: the label beside it names
 * whose it is.
 *
 * @param rim a moon-coloured ring around the profile's avatar (spec §3.12).
 */
@Composable
fun Avatar(
    diameter: Dp,
    glyphSize: Dp,
    modifier: Modifier = Modifier,
    rim: Dp = 0.dp,
) {
    val colors = BottlingTheme.colors
    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics {}
            .then(if (rim > 0.dp) Modifier.border(rim, colors.moon, CircleShape) else Modifier)
            .background(colors.moonContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_person),
            contentDescription = null,
            tint = colors.onMoonContainer,
            modifier = Modifier.size(glyphSize),
        )
    }
}

@NightDawnPreviews
@Composable
private fun AvatarPreview() = BottlingPreview {
    val sizes = BottlingTheme.sizes
    Row(
        modifier = Modifier.padding(BottlingTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(diameter = sizes.navAvatar, glyphSize = sizes.navAvatarGlyph)
        Avatar(diameter = sizes.profileAvatar, glyphSize = sizes.profileAvatarGlyph, rim = sizes.profileAvatarRim)
    }
}
