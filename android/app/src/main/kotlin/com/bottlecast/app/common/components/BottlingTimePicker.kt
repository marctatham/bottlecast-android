package com.bottlecast.app.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bottlecast.app.R
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import java.time.LocalTime

/**
 * Material's time picker in the theme's dialog (spec §3.16a, §3.17): the dial, or two fields behind the
 * keyboard toggle, then [Cancel] [OK]. It follows the phone's 12- or 24-hour setting.
 */
@Composable
fun BottlingTimePickerDialog(
    @StringRes title: Int,
    initial: LocalTime,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    startTyping: Boolean = false,
) {
    val spacing = BottlingTheme.spacing
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    var typing by rememberSaveable { mutableStateOf(startTyping) }
    BottlingDialog(onDismissRequest = onDismissRequest) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(spacing.m))
        if (typing) {
            TimeInput(state = state, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            TimePicker(
                state = state,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                // the dial stands out from the dialog, which is the colour Material would give it
                colors = TimePickerDefaults.colors(clockDialColor = MaterialTheme.colorScheme.surface),
                layoutType = TimePickerLayoutType.Vertical,
            )
        }
        Spacer(modifier = Modifier.height(spacing.s))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { typing = !typing }) {
                Icon(
                    painter = painterResource(if (typing) R.drawable.ic_symbol_schedule else R.drawable.ic_symbol_keyboard),
                    contentDescription = stringResource(if (typing) R.string.time_picker_dial else R.string.time_picker_type),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            BottlingTextButton(text = R.string.time_picker_cancel, onTapHandler = onDismissRequest)
            BottlingTextButton(text = R.string.time_picker_ok, onTapHandler = { onConfirm(LocalTime.of(state.hour, state.minute)) })
        }
    }
}

@NightDawnPreviews
@Composable
private fun BottlingTimePickerDialogPreview() = BottlingPreview {
    BottlingTimePickerDialog(
        title = R.string.reminder_time_title,
        initial = LocalTime.of(9, 0),
        onDismissRequest = {},
        onConfirm = {},
    )
}
