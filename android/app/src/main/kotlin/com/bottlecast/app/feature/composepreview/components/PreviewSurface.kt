package com.bottlecast.app.feature.composepreview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.bottlecast.app.common.ui.BottlingTheme

/** A group on `surface`: the recipient row (16dp), the checklist and the settings (24dp) (spec §3.4b). */
@Composable
internal fun PreviewSurface(
    modifier: Modifier = Modifier,
    shape: Shape = BottlingTheme.shapes.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface),
        content = content,
    )
}
