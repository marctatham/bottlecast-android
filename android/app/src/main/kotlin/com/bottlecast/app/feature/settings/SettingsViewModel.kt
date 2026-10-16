package com.bottlecast.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.account.domain.DeleteAccountResult
import com.bottlecast.app.common.account.domain.DeleteAccountUseCase
import com.bottlecast.app.common.account.domain.SignOutUseCase
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.reminders.domain.effectiveTime
import com.bottlecast.app.common.sea.domain.BlockedWriter
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.common.settings.domain.IThemeApplier
import com.bottlecast.app.common.settings.domain.ThemeChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalTime
import javax.inject.Inject

/**
 * Settings (spec §3.17), with the rows that exist so far: reminders, privacy, appearance and feel,
 * about, and the account, including signing out and deleting it.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    authenticationRepository: IAuthenticationRepository,
    profileRepository: IProfileRepository,
    private val seaRepository: ISeaRepository,
    private val settingsStore: ISettingsStore,
    private val themeApplier: IThemeApplier,
    private val reminderStore: IReminderStore,
    private val notificationAccess: INotificationAccess,
    private val signOut: SignOutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val _state: MutableStateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState.Loading)
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    // read again each time Settings comes back, since it can change in Android's settings meanwhile
    private val notificationsAllowed = MutableStateFlow(notificationAccess.allowed())

    // what's under way with the account, and its news for a snackbar
    private data class AccountWork(val busy: AccountTask? = null, val notice: SettingsNotice? = null)
    private val accountWork = MutableStateFlow(AccountWork())

    private val version: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[settings] Couldn't follow or change the settings")
        accountWork.value = AccountWork(notice = SettingsNotice.Failed)
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            @OptIn(ExperimentalCoroutinesApi::class)
            val account = authenticationRepository.user.flatMapLatest { user ->
                if (user.googleUid == null) {
                    flowOf(SettingsAccount.Guest)
                } else {
                    profileRepository.profile.map { profile ->
                        SettingsAccount.Google(googleName = (user as? UserEntity.AuthenticatedUser)?.googleName, penName = profile?.penName)
                    }
                }
            }
            val reminders = combine(reminderStore.settings, notificationsAllowed) { settings, allowed -> settings to allowed }
            combine(account, settingsStore.settings, reminders, seaRepository.blockedWriters, accountWork) { account, app, (reminders, allowed), blocked, work ->
                SettingsUiState.Showing(
                    account = account,
                    app = app,
                    reminders = reminders,
                    notificationsAllowed = allowed,
                    blockedWriters = if (account is SettingsAccount.Google) blocked else emptyList(),
                    version = version,
                    busy = work.busy,
                    notice = work.notice,
                )
            }.collect { _state.value = it }
        }
    }

    /** Settings is on screen again: notifications may have been turned on or off in Android's settings. */
    fun onResume() {
        notificationsAllowed.value = notificationAccess.allowed()
    }

    /**
     * The daily reminder's switch. Turning it on needs Android's permission first, which the reminder
     * sheet asks for: returns true when the sheet should open instead.
     */
    fun onRemindersChange(on: Boolean): Boolean {
        if (on && !notificationAccess.allowed()) return true
        viewModelScope.launch(exceptionHandler) {
            reminderStore.setEnabled(on).join()
            if (on) accountWork.update { it.copy(notice = SettingsNotice.RemindersOn(reminderStore.settings.first().effectiveTime())) }
        }
        return false
    }

    /** The reminder sheet turned reminders on, at [time]. */
    fun onRemindersEnabled(time: LocalTime) {
        notificationsAllowed.value = notificationAccess.allowed()
        accountWork.update { it.copy(notice = SettingsNotice.RemindersOn(time)) }
    }

    fun onReminderTimeChange(time: LocalTime) {
        reminderStore.setTime(time)
    }

    fun onQuietHoursChange(start: LocalTime, end: LocalTime) {
        reminderStore.setQuietHours(start, end)
    }

    fun onSignWithPenNameChange(sign: Boolean) {
        settingsStore.setSignWithPenName(sign)
    }

    fun onUnblock(writer: BlockedWriter) {
        viewModelScope.launch(exceptionHandler) { seaRepository.unblock(writer.writerUid) }
    }

    fun onThemeChange(theme: ThemeChoice) {
        settingsStore.setTheme(theme)
        themeApplier.apply(theme)
    }

    fun onReduceMotionChange(reduce: Boolean) {
        settingsStore.setReduceMotion(reduce)
    }

    fun onShakeToUncorkChange(on: Boolean) {
        settingsStore.setShakeToUncork(on)
    }

    fun onHapticsChange(on: Boolean) {
        settingsStore.setHaptics(on)
    }

    fun onSignOut() {
        if (accountWork.value.busy != null) return
        accountWork.value = AccountWork(busy = AccountTask.SigningOut)
        viewModelScope.launch(exceptionHandler) {
            signOut()
            accountWork.value = AccountWork(notice = SettingsNotice.SignedOut)
        }
    }

    /** Deletes the account; [context] is the activity Google's account picker shows over. */
    fun onDeleteAccount(context: Context) {
        if (accountWork.value.busy != null) return
        accountWork.value = AccountWork(busy = AccountTask.Deleting)
        viewModelScope.launch(exceptionHandler) {
            val notice = when (deleteAccount(context)) {
                DeleteAccountResult.Deleted -> SettingsNotice.Deleted
                DeleteAccountResult.Cancelled -> null
                DeleteAccountResult.Offline -> SettingsNotice.DeleteOffline
                DeleteAccountResult.WrongAccount -> SettingsNotice.DeleteWrongAccount
                DeleteAccountResult.Failed -> SettingsNotice.DeleteFailed
            }
            accountWork.value = AccountWork(notice = notice)
        }
    }

    fun onNoticeShown(notice: SettingsNotice) {
        accountWork.update { if (it.notice == notice) it.copy(notice = null) else it }
    }
}

sealed class SettingsUiState {
    data object Loading : SettingsUiState()

    data class Showing(
        val account: SettingsAccount,
        val app: AppSettings,
        val reminders: ReminderSettings,
        /** Android lets the reminder through: the permission, the app's switch and its channel. */
        val notificationsAllowed: Boolean,
        val blockedWriters: List<BlockedWriter>,
        val version: String,
        /** Signing out or deleting the account is under way: everything waits. */
        val busy: AccountTask? = null,
        val notice: SettingsNotice? = null,
    ) : SettingsUiState() {
        /** The reminder is on but Android holds it back, or Android was asked and said no (spec §3.17). */
        val notificationsOff: Boolean get() = !notificationsAllowed && (reminders.enabled || reminders.deniedAt != null)

        val remindersOn: Boolean get() = reminders.enabled && notificationsAllowed
    }
}

enum class AccountTask { SigningOut, Deleting }

sealed interface SettingsAccount {
    data object Guest : SettingsAccount

    /** [googleName] shows only here, in Settings › Account (decided 2026-10-15). */
    data class Google(val googleName: String?, val penName: String?) : SettingsAccount
}

/** One-time news from Settings, for a snackbar or, once the account is deleted, the goodbye screen. */
sealed interface SettingsNotice {
    data class RemindersOn(val time: LocalTime) : SettingsNotice
    data object SignedOut : SettingsNotice
    data object Deleted : SettingsNotice
    data object DeleteOffline : SettingsNotice
    data object DeleteWrongAccount : SettingsNotice
    data object DeleteFailed : SettingsNotice
    data object Failed : SettingsNotice
}
