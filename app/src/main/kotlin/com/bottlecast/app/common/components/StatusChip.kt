package com.bottlecast.app.common.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/** The status chip colourings in use (spec §1.2's chip table). */
enum class StatusChipStyle {
    /** "Drifting", "Sealing…" and info chips. */
    Glass,

    /** "Not kept · 5d left": an outline border on a surface. */
    Outline,

    /** "Kept". */
    PrimaryContainer,

    /** "Under review" and "Sealing failed". */
    ErrorContainer,

    /** Any chip on parchment, in ink tokens so it keeps its contrast. */
    Parchment,
}

/**
 * A 32dp pill pairing an icon with text, so status is never shown by colour alone (spec §4).
 * Its height is a minimum: it grows with the text.
 */
@Composable
fun StatusChip(
    text: String,
    @DrawableRes icon: Int,
    style: StatusChipStyle,
    modifier: Modifier = Modifier,
) {
    val colors = BottlingTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val sizes = BottlingTheme.sizes
    val shape = BottlingTheme.shapes.pill

    val (container, content, border) = when (style) {
        StatusChipStyle.Glass -> Triple(colors.glassContainer, colors.onGlassContainer, null)
        StatusChipStyle.Outline -> Triple(Color.Transparent, colorScheme.onSurfaceVariant, colorScheme.outline)
        StatusChipStyle.PrimaryContainer -> Triple(colorScheme.primaryContainer, colorScheme.onPrimaryContainer, null)
        StatusChipStyle.ErrorContainer -> Triple(colorScheme.errorContainer, colorScheme.onErrorContainer, null)
        StatusChipStyle.Parchment -> Triple(Color.Transparent, colors.ink, colors.inkMuted)
    }

    Row(
        modifier = modifier
            .heightIn(min = sizes.chipMinHeight)
            .clip(shape)
            .background(container)
            .then(if (border != null) Modifier.border(sizes.hairline, border, shape) else Modifier)
            .padding(start = sizes.chipPaddingStart, end = sizes.chipPaddingEnd)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(sizes.chipIconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(sizes.iconSmall),
        )
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@NightDawnPreviews
@Composable
private fun StatusChipPreview() = BottlingPreview {
    Column(
        modifier = Modifier.padding(BottlingTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
    ) {
        StatusChip("Drifting", R.drawable.ic_symbol_waves, StatusChipStyle.Glass)
        StatusChip("Not kept · 5d left", R.drawable.ic_symbol_waves, StatusChipStyle.Outline)
        StatusChip("Kept", R.drawable.ic_symbol_inventory_2, StatusChipStyle.PrimaryContainer)
        StatusChip("Under review", R.drawable.ic_symbol_flag, StatusChipStyle.ErrorContainer)
    }
}
