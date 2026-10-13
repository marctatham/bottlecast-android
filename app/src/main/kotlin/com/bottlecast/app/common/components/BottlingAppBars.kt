package com.bottlecast.app.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottlingAppBar(
    modifier: Modifier = Modifier,
    onBackHandler: () -> Unit,
    @StringRes title: Int = Int.MIN_VALUE
) {
    CenterAlignedTopAppBar(
        title = {
            if (title != Int.MIN_VALUE) {
                Text(
                    modifier = Modifier.padding(BottlingTheme.spacing.xs),
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        navigationIcon = {
            FilledIconButton(
                onClick = onBackHandler,
                modifier = Modifier.padding(BottlingTheme.spacing.xs),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                // auto-mirrored, so it points the right way in right-to-left layouts
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_arrow_back),
                    contentDescription = stringResource(id = R.string.button_back),
                    modifier = Modifier.size(BottlingTheme.sizes.icon)
                )
            }
        },
    )
}

@NightDawnPreviews
@Composable
private fun BottlingAppBarPreview() = BottlingPreview {
    BottlingAppBar(
        onBackHandler = {},
        modifier = Modifier.fillMaxWidth(),
        title = R.string.app_name
    )
}
