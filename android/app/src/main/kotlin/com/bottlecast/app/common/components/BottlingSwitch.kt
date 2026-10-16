package com.bottlecast.app.common.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * A switch in the theme's colours, with a check on its thumb while on (Settings board). Its row does
 * the toggling, so TalkBack reads row and switch as one: pass a null [onCheckedChange] then.
 */
@Composable
fun BottlingSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
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

@NightDawnPreviews
@Composable
private fun BottlingSwitchPreview() = BottlingPreview {
    BottlingSwitch(checked = true, onCheckedChange = {})
}
