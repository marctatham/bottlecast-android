package com.bottlecast.app.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/** The button at the start of an [ImmersiveTopBar]: back, or close for a flow that's left whole (spec §3.4). */
enum class TopBarNavigation {
    Back, Close
}

/**
 * The transparent top bar of a full-screen route (spec §3.4, §3.6, §3.7): 64dp, with the back arrow
 * or close X at the start, an optional centred [title], and the screen's own buttons at the end,
 * each a 48dp target.
 *
 * @param navigationLabel what TalkBack says for the start button, such as "Edit" on Preview (spec §3.4b).
 */
@Composable
fun ImmersiveTopBar(
    onBackHandler: () -> Unit,
    modifier: Modifier = Modifier,
    navigation: TopBarNavigation = TopBarNavigation.Back,
    @StringRes navigationLabel: Int = if (navigation == TopBarNavigation.Back) R.string.button_back else R.string.button_close,
    @StringRes title: Int? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.topBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (title != null) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    // clear of the buttons on both sides, so it stays centred on the screen
                    .padding(horizontal = BottlingTheme.sizes.touchTarget + BottlingTheme.spacing.xs)
                    .semantics { heading() },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BottlingTheme.sizes.topBarHeight)
                .padding(horizontal = BottlingTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBackHandler,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Icon(
                    // the arrow is auto-mirrored, so it points the right way in right-to-left layouts
                    painter = painterResource(
                        if (navigation == TopBarNavigation.Back) R.drawable.ic_symbol_arrow_back else R.drawable.ic_symbol_close
                    ),
                    contentDescription = stringResource(navigationLabel),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            actions()
        }
    }
}

@NightDawnPreviews
@Composable
private fun ImmersiveTopBarPreview() = BottlingPreview {
    ImmersiveTopBar(onBackHandler = {}, navigation = TopBarNavigation.Close, title = R.string.compose_title)
}
