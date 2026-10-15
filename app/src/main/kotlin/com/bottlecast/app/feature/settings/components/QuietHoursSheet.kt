package com.bottlecast.app.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingBottomSheet
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingTimePickerDialog
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.clockTime
import java.time.LocalTime

// which end of quiet hours the time picker is changing
private enum class QuietEnd { Start, End }

/**
 * Quiet hours (spec §3.16b, §3.17): from when and until when nothing from BottleCast arrives, the
 * reminder included. Each time opens the time picker, and changes save at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuietHoursSheet(
    start: LocalTime,
    end: LocalTime,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onChange: (start: LocalTime, end: LocalTime) -> Unit,
    onDone: () -> Unit,
) {
    val spacing = BottlingTheme.spacing
    var picking by rememberSaveable { mutableStateOf<QuietEnd?>(null) }
    BottlingBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.l, end = spacing.l, bottom = spacing.l),
        ) {
            Text(
                text = stringResource(R.string.settings_quiet_hours),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = stringResource(R.string.settings_quiet_hours_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(spacing.m))
            SettingsSection(title = R.string.settings_quiet_hours) {
                SettingsRow(
                    headline = stringResource(R.string.settings_quiet_from),
                    value = clockTime(start),
                    trailingIcon = R.drawable.ic_symbol_chevron_right,
                    onClick = { picking = QuietEnd.Start },
                )
                SettingsDivider()
                SettingsRow(
                    headline = stringResource(R.string.settings_quiet_until),
                    value = clockTime(end),
                    trailingIcon = R.drawable.ic_symbol_chevron_right,
                    onClick = { picking = QuietEnd.End },
                )
            }
            Spacer(modifier = Modifier.height(spacing.l))
            BottlingButton(text = R.string.settings_done, onTapHandler = onDone)
        }
    }
    when (picking) {
        QuietEnd.Start -> BottlingTimePickerDialog(
            title = R.string.settings_quiet_from_title,
            initial = start,
            onDismissRequest = { picking = null },
            onConfirm = { time ->
                onChange(time, end)
                picking = null
            },
        )
        QuietEnd.End -> BottlingTimePickerDialog(
            title = R.string.settings_quiet_until_title,
            initial = end,
            onDismissRequest = { picking = null },
            onConfirm = { time ->
                onChange(start, time)
                picking = null
            },
        )
        null -> Unit
    }
}
