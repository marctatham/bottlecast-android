package com.bottlecast.app.feature.composepreview.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.composepreview.ComposePreviewDimens

/** Where it's going (spec §3.4b): "Drifting to: anyone at sea". Gifts add "For: Mum" in slice 7. */
@Composable
internal fun RecipientRow(modifier: Modifier = Modifier) {
    val spacing = BottlingTheme.spacing
    PreviewSurface(modifier = modifier, shape = BottlingTheme.shapes.field) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ComposePreviewDimens.settingRow)
                .padding(horizontal = spacing.m, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_waves),
                contentDescription = null,
                tint = BottlingTheme.colors.glass,
                modifier = Modifier.size(BottlingTheme.sizes.icon),
            )
            Text(
                text = stringResource(R.string.preview_recipient_sea),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
