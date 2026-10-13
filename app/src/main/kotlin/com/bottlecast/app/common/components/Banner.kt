package com.bottlecast.app.common.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/** A banner's colouring (spec §3.18). */
enum class BannerStyle {
    /** glassContainer: offline, "While you were away". */
    Info,

    /** errorContainer. */
    Error,
}

/**
 * A banner placed under the top bar (spec §3.18): a 16dp radius and at least 48dp tall, so it
 * grows with the text. It's announced politely when it appears.
 */
@Composable
fun Banner(
    text: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    style: BannerStyle = BannerStyle.Info,
) {
    val colors = BottlingTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val spacing = BottlingTheme.spacing
    val (container, content) = when (style) {
        BannerStyle.Info -> colors.glassContainer to colors.onGlassContainer
        BannerStyle.Error -> colorScheme.errorContainer to colorScheme.onErrorContainer
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.touchTarget)
            .clip(BottlingTheme.shapes.field)
            .background(container)
            .padding(horizontal = spacing.m, vertical = spacing.s)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(BottlingTheme.sizes.bannerIcon),
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = content)
    }
}

@NightDawnPreviews
@Composable
private fun BannerPreview() = BottlingPreview {
    Column(
        modifier = Modifier.padding(BottlingTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
    ) {
        Banner(text = "You're offline — today's bottle will be here when you're back", icon = R.drawable.ic_symbol_wifi_off)
        Banner(text = "Something went adrift", icon = R.drawable.ic_symbol_flag, style = BannerStyle.Error)
    }
}
