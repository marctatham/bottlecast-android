package com.bottlecast.app.feature.reveal.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonSize
import com.bottlecast.app.common.components.BottlingButtonType
import com.bottlecast.app.common.components.BottlingDialog
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.ui.BottlingTheme

/**
 * Report this bottle? (spec §3.19): an optional reason, none chosen to start with, and "Also block
 * this writer". Reporting hides the bottle for the reporter at once; it never rewards them.
 */
@Composable
internal fun ReportDialog(
    onDismissRequest: () -> Unit,
    onReportHandler: (reason: ReportReason?, blockWriter: Boolean) -> Unit,
    initialReason: ReportReason? = null,
    initialBlock: Boolean = false,
) {
    var reason by rememberSaveable { mutableStateOf(initialReason) }
    var blockWriter by rememberSaveable { mutableStateOf(initialBlock) }
    val spacing = BottlingTheme.spacing
    val colorScheme = MaterialTheme.colorScheme

    BottlingDialog(onDismissRequest = onDismissRequest) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_flag),
            contentDescription = null,
            tint = colorScheme.error,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(BottlingTheme.sizes.icon),
        )
        Spacer(modifier = Modifier.height(spacing.m))
        Text(
            text = stringResource(R.string.report_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
        Spacer(modifier = Modifier.height(spacing.m))
        Text(
            text = stringResource(R.string.report_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.m))
        Text(
            text = stringResource(R.string.report_reason_heading),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Column(modifier = Modifier.selectableGroup()) {
            ReportReason.entries.forEach { option ->
                Choice(
                    label = option.label(),
                    role = Role.RadioButton,
                    selected = reason == option,
                    onSelect = { reason = option },
                ) {
                    RadioButton(
                        selected = reason == option,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colorScheme.primary,
                            unselectedColor = colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = BottlingTheme.sizes.hairline,
            color = colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = spacing.m),
        )
        Choice(
            label = R.string.report_block,
            role = Role.Checkbox,
            selected = blockWriter,
            onSelect = { blockWriter = !blockWriter },
            toggle = true,
        ) {
            Checkbox(
                checked = blockWriter,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    uncheckedColor = colorScheme.onSurfaceVariant,
                ),
            )
        }
        Text(
            text = stringResource(R.string.report_block_helper),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = CHOICE_INDENT),
        )
        Spacer(modifier = Modifier.height(spacing.l))
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottlingTextButton(text = R.string.report_cancel, onTapHandler = onDismissRequest)
            BottlingButton(
                text = R.string.report_confirm,
                buttonType = BottlingButtonType.DESTRUCTIVE,
                size = BottlingButtonSize.MEDIUM,
                onTapHandler = { onReportHandler(reason, blockWriter) },
            )
        }
    }
}

/** A 48dp row whose whole width picks the option, with its control at the start. */
@Composable
private fun Choice(
    @StringRes label: Int,
    role: Role,
    selected: Boolean,
    onSelect: () -> Unit,
    toggle: Boolean = false,
    control: @Composable () -> Unit,
) {
    val rowModifier = if (toggle) {
        Modifier.toggleable(value = selected, role = role, onValueChange = { onSelect() })
    } else {
        Modifier.selectable(selected = selected, role = role, onClick = onSelect)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BottlingTheme.sizes.touchTarget)
            .then(rowModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        Spacer(modifier = Modifier.width(BottlingTheme.spacing.m))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@StringRes
private fun ReportReason.label(): Int = when (this) {
    ReportReason.Hurtful -> R.string.report_reason_hurtful
    ReportReason.Sexual -> R.string.report_reason_sexual
    ReportReason.PersonalInfo -> R.string.report_reason_personal_info
    ReportReason.Spam -> R.string.report_reason_spam
    ReportReason.Other -> R.string.report_reason_other
}

// A radio button or checkbox without its own click handler is a 24dp slot, so the labels start 40dp
// in, as on the board; the block helper lines up under its label.
private val CHOICE_INDENT = 24.dp + BottlingTheme.spacing.m
