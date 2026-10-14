package com.bottlecast.app.common.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/**
 * The transparent top bar of a full-screen route under the dome (spec §3.6, §3.7): 64dp, with the
 * back arrow at the start and the screen's own buttons at the end, each a 48dp target.
 */
@Composable
fun ImmersiveTopBar(
    onBackHandler: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.topBarHeight)
            .padding(horizontal = BottlingTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackHandler,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            // auto-mirrored, so it points the right way in right-to-left layouts
            Icon(
                painter = painterResource(R.drawable.ic_symbol_arrow_back),
                contentDescription = stringResource(R.string.button_back),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        actions()
    }
}

@NightDawnPreviews
@Composable
private fun ImmersiveTopBarPreview() = BottlingPreview {
    ImmersiveTopBar(onBackHandler = {})
}
