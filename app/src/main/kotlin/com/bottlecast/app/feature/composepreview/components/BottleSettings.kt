package com.bottlecast.app.feature.composepreview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.composepreview.ComposePreviewDimens

/**
 * The bottle's settings (spec §3.4b), saved on it when it's cast: whether it's signed with the pen
 * name, or "Set a pen name…" while there's none, and whether a finder may seal it.
 */
@Composable
internal fun BottleSettings(
    penName: String?,
    signWithPenName: Boolean,
    sealable: Boolean,
    onSignWithPenName: (Boolean) -> Unit,
    onSetPenName: () -> Unit,
    onSealable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BottlingTheme.spacing
    PreviewSurface(modifier = modifier) {
        if (penName == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ComposePreviewDimens.settingRow)
                    .clickable(role = Role.Button, onClick = onSetPenName)
                    .padding(horizontal = spacing.m, vertical = spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.preview_set_pen_name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(BottlingTheme.sizes.icon),
                )
            }
        } else {
            SwitchRow(
                label = stringResource(R.string.preview_sign_pen_name, penName),
                checked = signWithPenName,
                onCheckedChange = onSignWithPenName,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = spacing.m),
            thickness = BottlingTheme.sizes.hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        SwitchRow(
            label = stringResource(R.string.preview_sealable),
            checked = sealable,
            onCheckedChange = onSealable,
            helper = stringResource(R.string.preview_sealable_helper),
        )
    }
}

// the whole row toggles, and TalkBack reads it as one switch
@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, helper: String? = null) {
    val spacing = BottlingTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ComposePreviewDimens.settingRow)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = spacing.m, vertical = spacing.s),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.m),
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_symbol_check),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = BottlingTheme.colors.surfaceHigh,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.xxs),
            )
        }
    }
}
