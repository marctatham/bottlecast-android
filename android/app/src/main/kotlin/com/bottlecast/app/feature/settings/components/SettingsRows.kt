package com.bottlecast.app.feature.settings.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingSwitch
import com.bottlecast.app.common.components.OverlineText
import com.bottlecast.app.common.settings.domain.ThemeChoice
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.isLargeText
import com.bottlecast.app.feature.settings.SettingsDimens

// a disabled row's content, as Material dims a disabled list item's
private const val DISABLED_ALPHA = 0.38f

/** A section of Settings (spec §3.17): its overline header, then its rows in a 24dp-radius surface group. */
@Composable
internal fun SettingsSection(@StringRes title: Int, modifier: Modifier = Modifier, rows: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        OverlineText(
            text = stringResource(title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = BottlingTheme.spacing.m)
                .semantics { heading() },
        )
        Spacer(modifier = Modifier.height(SettingsDimens.headerGap))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(BottlingTheme.shapes.card)
                .background(MaterialTheme.colorScheme.surface),
            content = rows,
        )
    }
}

/** The line between two rows of a group, edge to edge (Settings board). */
@Composable
internal fun SettingsDivider() {
    HorizontalDivider(thickness = BottlingTheme.sizes.hairline, color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * A row: an optional leading icon, the headline over its supporting text, then a [value] and a
 * chevron or [trailingIcon] when it opens something. At large text the value goes under the headline.
 */
@Composable
internal fun SettingsRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    value: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    headlineColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val spacing = BottlingTheme.spacing
    val valueBelow = isLargeText()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.rowMinHeight)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = spacing.m, end = if (trailingIcon != null) spacing.s else spacing.m, top = spacing.xs, bottom = spacing.xs)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> Box(modifier = Modifier.width(SettingsDimens.leadingSlot), contentAlignment = Alignment.CenterStart) { leading() }
            leadingIcon != null -> Box(modifier = Modifier.width(SettingsDimens.leadingSlot), contentAlignment = Alignment.CenterStart) {
                Icon(painter = painterResource(leadingIcon), contentDescription = null, tint = iconTint)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = headline, style = MaterialTheme.typography.bodyLarge, color = headlineColor)
            supporting?.let { SupportingText(it) }
            if (valueBelow) value?.let { SupportingText(it) }
        }
        if (!valueBelow && value != null) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailingIcon?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SettingsDimens.chevron),
            )
        }
    }
}

/** A row with a switch. The whole row toggles, and TalkBack reads it as one switch. */
@Composable
internal fun SettingsSwitchRow(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    val spacing = BottlingTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.rowMinHeight)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = spacing.m, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)) {
            Text(text = headline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            supporting?.let { SupportingText(it) }
        }
        BottlingSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun SupportingText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * "Theme": System, Night or Dawn, as segmented buttons beside the headline (Settings board). When they
 * don't fit beside it, such as at large text, they wrap under it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeRow(theme: ThemeChoice, onThemeChange: (ThemeChoice) -> Unit, modifier: Modifier = Modifier) {
    val spacing = BottlingTheme.spacing
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.rowMinHeight)
            .padding(start = spacing.m, end = spacing.xs, top = spacing.xxs, bottom = spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(spacing.xxs, Alignment.CenterVertically),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = spacing.s),
        )
        ThemeButtons(theme, onThemeChange)
    }
}

@Composable
private fun ThemeButtons(theme: ThemeChoice, onThemeChange: (ThemeChoice) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val choices = ThemeChoice.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.selectableGroup()) {
        choices.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = choice == theme,
                onClick = { onThemeChange(choice) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
                contentPadding = PaddingValues(horizontal = BottlingTheme.spacing.s),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colorScheme.primaryContainer,
                    activeContentColor = colorScheme.onPrimaryContainer,
                    activeBorderColor = colorScheme.outline,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = colorScheme.onSurface,
                    inactiveBorderColor = colorScheme.outline,
                ),
                icon = {
                    if (choice == theme) {
                        Icon(
                            painter = painterResource(R.drawable.ic_symbol_check),
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                        )
                    }
                },
                label = { Text(text = stringResource(choice.label()), style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@StringRes
private fun ThemeChoice.label(): Int = when (this) {
    ThemeChoice.System -> R.string.settings_theme_system
    ThemeChoice.Night -> R.string.settings_theme_night
    ThemeChoice.Dawn -> R.string.settings_theme_dawn
}

@NightDawnPreviews
@Composable
private fun SettingsRowsPreview() = BottlingPreview {
    SettingsSection(title = R.string.settings_section_feel) {
        ThemeRow(theme = ThemeChoice.System, onThemeChange = {})
        SettingsDivider()
        SettingsSwitchRow(headline = "Haptics", checked = true, onCheckedChange = {})
        SettingsDivider()
        SettingsRow(headline = "Pen name", value = "Driftwood", trailingIcon = R.drawable.ic_symbol_chevron_right, onClick = {})
    }
}
