package com.bottlecast.app.feature.reminders

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.BottlingBottomSheet
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingTextButton
import com.bottlecast.app.common.components.BottlingTimePickerDialog
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.clockTime
import com.bottlecast.app.common.ui.shortClockTime
import com.bottlecast.app.feature.reminders.components.ReminderIllustration
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalTime

/** Where the reminder sheet opened, for analytics. */
enum class ReminderSheetSource(val param: String) {
    FirstFind("first_find"),
    ShoreChip("shore"),
    Settings("settings"),
}

/**
 * The reminder sheet (spec §3.16a): "Want a daily nudge?", the time it nudges at, quiet hours, and
 * "Turn on reminders", which asks Android for POST_NOTIFICATIONS. It hands the reminder's time to
 * [onEnabled] for the host's snackbar, and closes through [onDismissRequest] otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    source: ReminderSheetSource,
    onDismissRequest: () -> Unit,
    onEnabled: (LocalTime) -> Unit,
    viewModel: ReminderSheetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val tracker = LocalTracker.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberBottlingSheetState()
    val currentOnEnabled by rememberUpdatedState(onEnabled)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    var picking by rememberSaveable { mutableStateOf(false) }

    fun canAskAgain() = activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted = granted, canAskAgain = canAskAgain())
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    // The next opening starts afresh, whatever this one ended on. A rotation isn't a new opening.
    DisposableEffect(viewModel) { onDispose { if (activity?.isChangingConfigurations != true) viewModel.reset() } }

    LaunchedEffect(uiState.outcome) {
        when (val outcome = uiState.outcome) {
            is ReminderOutcome.Enabled -> {
                tracker.trackEvent(AnalyticsEvent.RemindersTurnedOn, mapOf(AnalyticsEvent.PARAM_SOURCE to source.param))
                sheetState.hide()
                currentOnEnabled(outcome.time)
            }
            ReminderOutcome.Declined -> {
                sheetState.hide()
                currentOnDismiss()
            }
            null -> Unit
        }
    }

    ReminderSheet(
        uiState = uiState,
        showRationale = canAskAgain(),
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        onPickTime = { picking = true },
        onTurnOn = { if (viewModel.onTurnOn()) permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onOpenSettings = {
            viewModel.onOpenSettings()
            openNotificationSettings(context)
        },
        onNotNow = { scope.launch { sheetState.hide() }.invokeOnCompletion { currentOnDismiss() } },
    )

    if (picking) {
        BottlingTimePickerDialog(
            title = R.string.reminder_time_title,
            initial = uiState.time,
            onDismissRequest = { picking = false },
            onConfirm = { time ->
                viewModel.onTimeChange(time)
                picking = false
            },
        )
    }
}

/** The sheet itself, for previews and screenshot tests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderSheet(
    uiState: ReminderSheetUiState,
    showRationale: Boolean,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onPickTime: () -> Unit,
    onTurnOn: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit,
) {
    val spacing = BottlingTheme.spacing
    BottlingBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.l, end = spacing.l, bottom = spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReminderIllustration()
            Spacer(modifier = Modifier.height(spacing.m))
            Text(
                text = stringResource(R.string.reminder_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = when {
                    uiState.blocked -> stringResource(R.string.reminder_sheet_blocked)
                    showRationale -> stringResource(R.string.reminder_sheet_body) + " " + stringResource(R.string.reminder_sheet_body_rationale)
                    else -> stringResource(R.string.reminder_sheet_body)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(spacing.l))
            ReminderTimeRow(time = uiState.time, onPickTime = onPickTime)
            Spacer(modifier = Modifier.height(spacing.s))
            QuietHoursCaption(start = uiState.quietStart, end = uiState.quietEnd)
            Spacer(modifier = Modifier.height(spacing.l))
            if (uiState.blocked) {
                BottlingButton(text = R.string.reminder_sheet_open_settings, onTapHandler = onOpenSettings)
            } else {
                BottlingButton(text = R.string.reminder_sheet_turn_on, onTapHandler = onTurnOn)
            }
            Spacer(modifier = Modifier.height(spacing.xs))
            BottlingTextButton(text = R.string.reminder_sheet_not_now, onTapHandler = onNotNow, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** "Remind me at" and the time chip that opens the picker, in an outlined row (Notify-Rationale board). */
@Composable
private fun ReminderTimeRow(time: LocalTime, onPickTime: () -> Unit) {
    val spacing = BottlingTheme.spacing
    val colorScheme = MaterialTheme.colorScheme
    val shape = BottlingTheme.shapes.pill
    val formatted = clockTime(time)
    val description = stringResource(R.string.reminder_time_description, formatted)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ReminderDimens.timeRowMinHeight)
            .border(BottlingTheme.sizes.hairline, colorScheme.outlineVariant, BottlingTheme.shapes.field)
            .padding(start = spacing.m, end = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.reminder_time_label),
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        // the touch target reaches 48dp around the chip, while its outline stays 32dp tall
        Row(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .heightIn(min = BottlingTheme.sizes.chipMinHeight)
                .clip(shape)
                .clickable(
                    interactionSource = null,
                    indication = ripple(focusRingShape = shape),
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.reminder_time_change),
                    onClick = onPickTime,
                )
                .semantics { contentDescription = description }
                .border(BottlingTheme.sizes.hairline, colorScheme.outline, shape)
                .padding(start = BottlingTheme.sizes.chipPaddingStart, end = BottlingTheme.sizes.chipPaddingEnd),
            horizontalArrangement = Arrangement.spacedBy(BottlingTheme.sizes.chipIconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_symbol_schedule),
                contentDescription = null,
                tint = BottlingTheme.colors.glass,
                modifier = Modifier.size(BottlingTheme.sizes.iconSmall),
            )
            Text(text = formatted, style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
        }
    }
}

/** "Quiet hours: nothing between 10 PM and 8 AM." with the moon (spec §3.16a). */
@Composable
private fun QuietHoursCaption(start: LocalTime, end: LocalTime) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BottlingTheme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_symbol_bedtime),
            contentDescription = null,
            tint = BottlingTheme.colors.moon,
            modifier = Modifier.size(ReminderDimens.captionIcon),
        )
        Text(
            text = stringResource(R.string.reminder_quiet_caption, shortClockTime(start), shortClockTime(end)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Android's notification settings for this app, where a permanently refused permission is turned back
 * on, or the app's details where a phone doesn't have that screen.
 */
private fun openNotificationSettings(context: Context) {
    val notifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    try {
        context.startActivity(notifications)
    } catch (exception: ActivityNotFoundException) {
        Timber.w(exception, "[openNotificationSettings] No notification settings screen, so the app's details instead")
        runCatching { context.startActivity(details) }.onFailure { Timber.w(it, "[openNotificationSettings] No app details screen either") }
    }
}
