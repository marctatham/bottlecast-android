package com.bottlecast.app.feature.composepreview.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.bottlecast.app.R
import com.bottlecast.app.common.components.LinkedText
import com.bottlecast.app.common.components.TextLink
import com.bottlecast.app.common.legal.LegalLinks
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * The first cast's Terms (spec §3.4b): unticked, and Cast it waits until it's ticked. The links open
 * the Community guidelines and the Terms; a tap anywhere else ticks it.
 */
@Composable
internal fun TermsCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.touchTarget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xxs / 2)) {
            LinkedText(
                template = stringResource(R.string.preview_terms),
                links = listOf(
                    TextLink(stringResource(R.string.preview_terms_guidelines), LegalLinks.GUIDELINES),
                    TextLink(stringResource(R.string.preview_terms_terms), LegalLinks.TERMS),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                linkColor = BottlingTheme.colors.glass,
            )
            Text(
                text = stringResource(R.string.preview_terms_first_cast),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
