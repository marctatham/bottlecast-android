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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The user's avatar: a moonContainer circle (decisions.md, 2026-10-13) holding [initial], the pen
 * name's first letter (decided 2026-10-15), or a person glyph without one. Decorative: the label
 * beside it names whose it is. The initial is sized to the circle and doesn't grow with the font
 * scale, since the name beside it does.
 *
 * @param rim a moon-coloured ring around the profile's avatar (spec §3.12).
 */
@Composable
fun Avatar(
    diameter: Dp,
    glyphSize: Dp,
    modifier: Modifier = Modifier,
    rim: Dp = 0.dp,
    initial: String? = null,
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
        if (initial != null) {
            val size = with(LocalDensity.current) { (diameter * INITIAL_SCALE).toSp() }
            Text(
                text = initial,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = size, lineHeight = size),
                color = colors.onMoonContainer,
                maxLines = 1,
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_person),
                contentDescription = null,
                tint = colors.onMoonContainer,
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}

// the initial's height against the circle's, as the Profile board's 24/32 initials sit in its avatar
private const val INITIAL_SCALE = 0.42f

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
        Avatar(diameter = sizes.navAvatar, glyphSize = sizes.navAvatarGlyph, initial = "D")
        Avatar(diameter = sizes.profileAvatar, glyphSize = sizes.profileAvatarGlyph, rim = sizes.profileAvatarRim, initial = "D")
    }
}
