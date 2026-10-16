package com.bottlecast.app.feature.composepreview.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.composepreview.ComposePreviewDimens

/**
 * What casting means (spec §3.4b, G15): it can't be edited, the finder can read and share it, and if
 * they keep and seal it, it goes on a public chain. The last line only shows while sealing is allowed.
 */
@Composable
internal fun ConsentChecklist(sealable: Boolean, modifier: Modifier = Modifier) {
    PreviewSurface(modifier = modifier.fillMaxWidth()) {
        val spacing = BottlingTheme.spacing
        Column(modifier = Modifier.padding(vertical = spacing.xs)) {
            ChecklistRow(R.drawable.ic_symbol_lock, R.string.preview_consent_edit)
            ChecklistRow(R.drawable.ic_symbol_share, R.string.preview_consent_share)
            if (sealable) ChecklistRow(R.drawable.ic_glyph_seal, R.string.preview_consent_chain)
        }
    }
}

@Composable
private fun ChecklistRow(@DrawableRes icon: Int, @StringRes text: Int) {
    val spacing = BottlingTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ComposePreviewDimens.checklistRow)
            .padding(horizontal = spacing.m, vertical = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(BottlingTheme.sizes.iconSmall),
        )
        Text(text = stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
