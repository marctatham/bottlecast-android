package com.bottlecast.app.feature.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.bottlecast.app.R
import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.components.Avatar
import com.bottlecast.app.common.components.BottlingTimePickerDialog
import com.bottlecast.app.common.components.DomeScaffold
import com.bottlecast.app.common.components.ImmersiveTopBar
import com.bottlecast.app.common.components.bleed
import com.bottlecast.app.common.components.rememberBottlingSheetState
import com.bottlecast.app.common.legal.LegalLinks
import com.bottlecast.app.common.legal.openInCustomTab
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ThemeChoice
import com.bottlecast.app.common.ui.BottlingPreview
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.NightDawnPreviews
import com.bottlecast.app.common.ui.clockTime
import com.bottlecast.app.common.ui.shortClockTime
import com.bottlecast.app.feature.penname.PenNameSheet
import com.bottlecast.app.feature.reminders.ReminderSheet
import com.bottlecast.app.feature.reminders.ReminderSheetSource
import com.bottlecast.app.feature.settings.components.AccountBusyOverlay
import com.bottlecast.app.feature.settings.components.BlockedWritersSheet
import com.bottlecast.app.feature.settings.components.DeleteAccountDialog
import com.bottlecast.app.feature.settings.components.QuietHoursSheet
import com.bottlecast.app.feature.settings.components.SettingsDivider
import com.bottlecast.app.feature.settings.components.SettingsRow
import com.bottlecast.app.feature.settings.components.SettingsSection
import com.bottlecast.app.feature.settings.components.SettingsSwitchRow
import com.bottlecast.app.feature.settings.components.SignOutDialog
import com.bottlecast.app.feature.settings.components.ThemeRow
import com.bottlecast.app.feature.settings.domain.googleInitials
import com.bottlecast.app.feature.settings.domain.shortGoogleName
import com.bottlecast.app.feature.signin.SignInReason
import com.bottlecast.app.feature.signin.SignInSheet
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** What Settings has open over it, kept across a rotation. */
internal enum class SettingsOverlay {
    ReminderSheet, ReminderTime, QuietHours, PenName, BlockedWriters, SignIn, SignOut, DeleteAccount,
}

/**
 * Settings (spec §3.17): a full-screen route under a low dome, its sections in the spec's order with
 * the rows that exist so far.
 *
 * @param onHowItWorksHandler replays onboarding.
 * @param onDeletedHandler the account is gone: the goodbye screen takes over.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackHandler: () -> Unit,
    onHowItWorksHandler: () -> Unit,
    onLicencesHandler: () -> Unit,
    onDeletedHandler: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val tracker = LocalTracker.current
    val scope = rememberCoroutineScope()
    var overlay by rememberSaveable { mutableStateOf<SettingsOverlay?>(null) }
    val close: () -> Unit = { overlay = null }
    val showing = uiState as? SettingsUiState.Showing

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }

    // one-time news: a snackbar, or the goodbye screen once the account is deleted
    val notice = showing?.notice
    val noticeText = when (notice) {
        is SettingsNotice.RemindersOn -> stringResource(R.string.reminder_turned_on, clockTime(notice.time))
        SettingsNotice.SignedOut -> stringResource(R.string.settings_signed_out)
        SettingsNotice.DeleteOffline -> stringResource(R.string.settings_delete_offline)
        SettingsNotice.DeleteWrongAccount -> stringResource(R.string.settings_delete_wrong_account)
        SettingsNotice.DeleteFailed -> stringResource(R.string.settings_delete_failed)
        SettingsNotice.Failed -> stringResource(R.string.settings_failed)
        SettingsNotice.Deleted, null -> null
    }
    LaunchedEffect(notice) {
        when (notice) {
            null -> return@LaunchedEffect
            SettingsNotice.Deleted -> {
                tracker.trackEvent(AnalyticsEvent.AccountDeleted)
                viewModel.onNoticeShown(notice)
                onDeletedHandler()
            }
            else -> {
                if (notice == SettingsNotice.SignedOut) tracker.trackEvent(AnalyticsEvent.SignedOut)
                snackbarHostState.showSnackbar(noticeText ?: return@LaunchedEffect)
                viewModel.onNoticeShown(notice)
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onBackHandler = onBackHandler,
        onRemindersChange = { on -> if (viewModel.onRemindersChange(on)) overlay = SettingsOverlay.ReminderSheet },
        onTurnOnNotificationsHandler = { overlay = SettingsOverlay.ReminderSheet },
        onReminderTimeHandler = { overlay = SettingsOverlay.ReminderTime },
        onQuietHoursHandler = { overlay = SettingsOverlay.QuietHours },
        onPenNameHandler = { overlay = SettingsOverlay.PenName },
        onSignWithPenNameChange = viewModel::onSignWithPenNameChange,
        onBlockedWritersHandler = { overlay = SettingsOverlay.BlockedWriters },
        onThemeChange = viewModel::onThemeChange,
        onReduceMotionChange = viewModel::onReduceMotionChange,
        onShakeToUncorkChange = viewModel::onShakeToUncorkChange,
        onHapticsChange = viewModel::onHapticsChange,
        onHowItWorksHandler = dropUnlessResumed(block = onHowItWorksHandler),
        onLinkHandler = { url -> openInCustomTab(context, url) },
        onLicencesHandler = dropUnlessResumed(block = onLicencesHandler),
        onSignInHandler = { overlay = SettingsOverlay.SignIn },
        onSignOutHandler = { overlay = SettingsOverlay.SignOut },
        onDeleteAccountHandler = { overlay = SettingsOverlay.DeleteAccount },
    )

    if (showing == null) return
    val sheetState = rememberBottlingSheetState()
    val hideThen: (() -> Unit) -> Unit = { then -> scope.launch { sheetState.hide() }.invokeOnCompletion { then() } }
    when (overlay) {
        SettingsOverlay.ReminderSheet -> ReminderSheet(
            source = ReminderSheetSource.Settings,
            onDismissRequest = close,
            onEnabled = { time ->
                close()
                viewModel.onRemindersEnabled(time)
            },
        )
        SettingsOverlay.ReminderTime -> BottlingTimePickerDialog(
            title = R.string.reminder_time_title,
            initial = showing.reminders.time,
            onDismissRequest = close,
            onConfirm = { time ->
                viewModel.onReminderTimeChange(time)
                close()
            },
        )
        SettingsOverlay.QuietHours -> QuietHoursSheet(
            start = showing.reminders.quietStart,
            end = showing.reminders.quietEnd,
            sheetState = sheetState,
            onDismissRequest = close,
            onChange = viewModel::onQuietHoursChange,
            onDone = { hideThen(close) },
        )
        SettingsOverlay.PenName -> PenNameSheet(onDismissRequest = close, onSaved = { close() })
        SettingsOverlay.BlockedWriters -> BlockedWritersSheet(
            writers = showing.blockedWriters,
            today = LocalDate.now(),
            zone = ZoneId.systemDefault(),
            sheetState = sheetState,
            onDismissRequest = close,
            onUnblock = viewModel::onUnblock,
        )
        SettingsOverlay.SignIn -> SignInSheet(reason = SignInReason.Account, onDismissRequest = close, onSignedIn = { close() })
        SettingsOverlay.SignOut -> SignOutDialog(
            onDismissRequest = close,
            onSignOut = {
                close()
                viewModel.onSignOut()
            },
        )
        SettingsOverlay.DeleteAccount -> DeleteAccountDialog(
            onDismissRequest = close,
            onDelete = {
                close()
                activity?.let(viewModel::onDeleteAccount)
            },
        )
        null -> Unit
    }
    showing.busy?.let { task -> AccountBusyOverlay(deleting = task == AccountTask.Deleting) }
}

/** The stateless Settings, for previews and screenshot tests. */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onBackHandler: () -> Unit,
    onRemindersChange: (Boolean) -> Unit,
    onTurnOnNotificationsHandler: () -> Unit,
    onReminderTimeHandler: () -> Unit,
    onQuietHoursHandler: () -> Unit,
    onPenNameHandler: () -> Unit,
    onSignWithPenNameChange: (Boolean) -> Unit,
    onBlockedWritersHandler: () -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onShakeToUncorkChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onHowItWorksHandler: () -> Unit,
    onLinkHandler: (String) -> Unit,
    onLicencesHandler: () -> Unit,
    onSignInHandler: () -> Unit,
    onSignOutHandler: () -> Unit,
    onDeleteAccountHandler: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    contentPadding: PaddingValues = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
) {
    val spacing = BottlingTheme.spacing
    val showing = uiState as? SettingsUiState.Showing
    val chevron = R.drawable.ic_symbol_chevron_right
    val external = R.drawable.ic_symbol_open_in_new

    DomeScaffold(
        edge = SettingsDimens.dome,
        contentPadding = contentPadding,
        modifier = modifier,
        windowInsets = windowInsets,
    ) { _ ->
        ImmersiveTopBar(onBackHandler = onBackHandler, title = R.string.settings_title, modifier = Modifier.bleed(SettingsDimens.topBarBleed))
        if (showing == null) return@DomeScaffold
        val google = showing.account as? SettingsAccount.Google
        val reminders = showing.reminders
        val remindersEnabled = !showing.notificationsOff
        Spacer(modifier = Modifier.height(spacing.xxs))

        SettingsSection(title = R.string.settings_section_reminders) {
            if (showing.notificationsOff) {
                SettingsRow(
                    headline = stringResource(R.string.settings_notifications_off),
                    value = stringResource(R.string.settings_notifications_turn_on),
                    leadingIcon = R.drawable.ic_symbol_notifications,
                    trailingIcon = chevron,
                    onClick = onTurnOnNotificationsHandler,
                )
                SettingsDivider()
            }
            SettingsSwitchRow(
                headline = stringResource(R.string.settings_daily_reminder),
                checked = showing.remindersOn,
                onCheckedChange = onRemindersChange,
                enabled = remindersEnabled,
            )
            SettingsDivider()
            SettingsRow(
                headline = stringResource(R.string.settings_remind_me_at),
                value = clockTime(reminders.time),
                trailingIcon = chevron,
                enabled = remindersEnabled,
                onClick = onReminderTimeHandler,
            )
            SettingsDivider()
            SettingsRow(
                headline = stringResource(R.string.settings_quiet_hours),
                value = stringResource(R.string.settings_quiet_hours_value, shortClockTime(reminders.quietStart), shortClockTime(reminders.quietEnd)),
                trailingIcon = chevron,
                enabled = remindersEnabled,
                onClick = onQuietHoursHandler,
            )
            SettingsDivider()
            // until push exists, the Chest's dot and the Shore's banner stand in (decided 2026-10-15)
            SettingsSwitchRow(
                headline = stringResource(R.string.settings_found_notifications),
                supporting = stringResource(R.string.settings_coming_soon),
                checked = false,
                onCheckedChange = {},
                enabled = false,
            )
        }
        Spacer(modifier = Modifier.height(SettingsDimens.sectionGap))

        SettingsSection(title = R.string.settings_section_privacy) {
            SettingsRow(
                headline = stringResource(R.string.settings_pen_name),
                supporting = if (google == null) stringResource(R.string.settings_sign_in_first) else null,
                value = google?.let { it.penName ?: stringResource(R.string.settings_pen_name_none) },
                trailingIcon = chevron,
                enabled = google != null,
                onClick = onPenNameHandler,
            )
            SettingsDivider()
            SettingsSwitchRow(
                headline = stringResource(R.string.settings_sign_with_pen_name),
                checked = showing.app.signWithPenName,
                onCheckedChange = onSignWithPenNameChange,
                enabled = google != null,
            )
            SettingsDivider()
            SettingsRow(
                headline = stringResource(R.string.settings_blocked),
                value = google?.let { showing.blockedWriters.size.toString() },
                trailingIcon = chevron,
                enabled = google != null,
                onClick = onBlockedWritersHandler,
            )
        }
        Spacer(modifier = Modifier.height(SettingsDimens.sectionGap))

        SettingsSection(title = R.string.settings_section_feel) {
            ThemeRow(theme = showing.app.theme, onThemeChange = onThemeChange)
            SettingsDivider()
            SettingsSwitchRow(
                headline = stringResource(R.string.settings_reduce_motion),
                supporting = if (showing.app.reduceMotion) null else stringResource(R.string.settings_reduce_motion_system),
                checked = showing.app.reduceMotion,
                onCheckedChange = onReduceMotionChange,
            )
            SettingsDivider()
            SettingsSwitchRow(
                headline = stringResource(R.string.settings_shake),
                checked = showing.app.shakeToUncork,
                onCheckedChange = onShakeToUncorkChange,
            )
            SettingsDivider()
            SettingsSwitchRow(
                headline = stringResource(R.string.settings_haptics),
                checked = showing.app.haptics,
                onCheckedChange = onHapticsChange,
            )
        }
        Spacer(modifier = Modifier.height(SettingsDimens.sectionGap))

        SettingsSection(title = R.string.settings_section_about) {
            SettingsRow(headline = stringResource(R.string.settings_how_it_works), trailingIcon = chevron, onClick = onHowItWorksHandler)
            SettingsDivider()
            SettingsRow(
                headline = stringResource(R.string.settings_guidelines),
                trailingIcon = external,
                onClick = { onLinkHandler(LegalLinks.GUIDELINES) },
            )
            SettingsDivider()
            SettingsRow(
                headline = stringResource(R.string.settings_privacy_policy),
                trailingIcon = external,
                onClick = { onLinkHandler(LegalLinks.PRIVACY) },
            )
            SettingsDivider()
            SettingsRow(headline = stringResource(R.string.settings_terms), trailingIcon = external, onClick = { onLinkHandler(LegalLinks.TERMS) })
            SettingsDivider()
            SettingsRow(headline = stringResource(R.string.settings_licences), trailingIcon = chevron, onClick = onLicencesHandler)
            SettingsDivider()
            SettingsRow(headline = stringResource(R.string.settings_version), value = showing.version)
        }
        Spacer(modifier = Modifier.height(SettingsDimens.sectionGap))

        SettingsSection(title = R.string.settings_section_account) {
            if (google == null) {
                SettingsRow(
                    headline = stringResource(R.string.sign_in_google),
                    leading = {
                        Image(
                            painter = painterResource(R.drawable.ic_glyph_google),
                            contentDescription = null,
                            modifier = Modifier.size(BottlingTheme.sizes.pillIcon),
                        )
                    },
                    trailingIcon = chevron,
                    onClick = onSignInHandler,
                )
            } else {
                SettingsRow(
                    headline = stringResource(R.string.settings_signed_in_google),
                    supporting = google.googleName?.let(::shortGoogleName),
                    leading = {
                        Avatar(
                            diameter = SettingsDimens.accountAvatar,
                            glyphSize = BottlingTheme.sizes.iconSmall,
                            initial = google.googleName?.let(::googleInitials)?.takeIf { it.isNotEmpty() },
                        )
                    },
                )
                SettingsDivider()
                SettingsRow(
                    headline = stringResource(R.string.settings_sign_out),
                    leadingIcon = R.drawable.ic_symbol_logout,
                    onClick = onSignOutHandler,
                )
                SettingsDivider()
                SettingsRow(
                    headline = stringResource(R.string.settings_delete_account),
                    leadingIcon = R.drawable.ic_symbol_delete,
                    headlineColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteAccountHandler,
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.l))
    }
}

@NightDawnPreviews
@Composable
private fun SettingsScreenPreview() = BottlingPreview {
    SettingsScreen(
        uiState = SettingsUiState.Showing(
            account = SettingsAccount.Google(googleName = "Marc Tatham", penName = "Driftwood"),
            app = AppSettings(),
            reminders = ReminderSettings(enabled = true),
            notificationsAllowed = true,
            blockedWriters = emptyList(),
            version = "1.2.0",
        ),
        onBackHandler = {},
        onRemindersChange = {},
        onTurnOnNotificationsHandler = {},
        onReminderTimeHandler = {},
        onQuietHoursHandler = {},
        onPenNameHandler = {},
        onSignWithPenNameChange = {},
        onBlockedWritersHandler = {},
        onThemeChange = {},
        onReduceMotionChange = {},
        onShakeToUncorkChange = {},
        onHapticsChange = {},
        onHowItWorksHandler = {},
        onLinkHandler = {},
        onLicencesHandler = {},
        onSignInHandler = {},
        onSignOutHandler = {},
        onDeleteAccountHandler = {},
    )
}
