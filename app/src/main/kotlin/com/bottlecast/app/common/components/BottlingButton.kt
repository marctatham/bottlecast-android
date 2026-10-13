package com.bottlecast.app.common.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews

/** The three pills (spec §1.4): filled sage, outlined sage and tonal. */
enum class BottlingButtonType {
    PRIMARY, SECONDARY, TONAL
}

@Composable
fun BottlingButton(
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    buttonType: BottlingButtonType = BottlingButtonType.PRIMARY,
    @DrawableRes leadingIcon: Int? = null,
    onTapHandler: () -> Unit = {},
) = BottlingButton(
    text = stringResource(text),
    modifier = modifier,
    enabled = enabled,
    isLoading = isLoading,
    buttonType = buttonType,
    leadingIcon = leadingIcon,
    onTapHandler = onTapHandler,
)

/**
 * A full-width pill, at least 56dp tall so it grows with large text. [leadingIcon] is a decorative
 * 20dp glyph before the label.
 */
@Composable
internal fun BottlingButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    buttonType: BottlingButtonType = BottlingButtonType.PRIMARY,
    @DrawableRes leadingIcon: Int? = null,
    interactionSource: MutableInteractionSource? = null,
    onTapHandler: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val pillModifier = modifier
        .fillMaxWidth()
        .heightIn(min = BottlingTheme.sizes.pillMinHeight)
    val contentPadding = PaddingValues(
        horizontal = BottlingTheme.spacing.l,
        vertical = BottlingTheme.spacing.xs,
    )
    val content: @Composable RowScope.() -> Unit = { ButtonContent(text, isLoading, leadingIcon) }

    when (buttonType) {
        BottlingButtonType.PRIMARY -> Button(
            onClick = onTapHandler,
            modifier = pillModifier,
            enabled = enabled,
            shape = BottlingTheme.shapes.pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
            ),
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

        BottlingButtonType.SECONDARY -> OutlinedButton(
            onClick = onTapHandler,
            modifier = pillModifier,
            enabled = enabled,
            shape = BottlingTheme.shapes.pill,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary),
            // a 1dp sage stroke (spec §3.3); Material's default border is outlineVariant
            border = BorderStroke(
                width = BottlingTheme.sizes.hairline,
                color = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = DISABLED_BORDER_ALPHA),
            ),
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )

        BottlingButtonType.TONAL -> FilledTonalButton(
            onClick = onTapHandler,
            modifier = pillModifier,
            enabled = enabled,
            shape = BottlingTheme.shapes.pill,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
            ),
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content,
        )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    @DrawableRes leadingIcon: Int?,
) {
    val sizes = BottlingTheme.sizes
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // while loading, the spinner sits at the end and the label keeps clear of it on both sides,
        // so it stays centred
        val spinnerClearance = if (isLoading) sizes.icon + sizes.pillIconGap else 0.dp
        Row(
            modifier = Modifier.padding(horizontal = spinnerClearance),
            horizontalArrangement = Arrangement.spacedBy(sizes.pillIconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(sizes.pillIcon),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(sizes.icon),
                color = LocalContentColor.current,
            )
        }
    }
}

private const val DISABLED_BORDER_ALPHA = 0.12f

@NightDawnPreviews
@Composable
private fun BottlingButtonPreview() = BottlingPreview {
    Column(
        modifier = Modifier.padding(BottlingTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs),
    ) {
        BottlingButton(
            text = "Find today's bottle",
            buttonType = BottlingButtonType.PRIMARY,
            leadingIcon = R.drawable.ic_glyph_bottle,
        )
        BottlingButton(text = "Cast a bottle", buttonType = BottlingButtonType.SECONDARY)
        BottlingButton(text = "Keep it", buttonType = BottlingButtonType.TONAL)
        BottlingButton(text = "Cast a bottle", buttonType = BottlingButtonType.SECONDARY, isLoading = true)
    }
}
