package com.bottlecast.app.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.reminders.domain.DEFAULT_QUIET_END
import com.bottlecast.app.common.reminders.domain.DEFAULT_QUIET_START
import com.bottlecast.app.common.reminders.domain.DEFAULT_REMINDER_TIME
import com.bottlecast.app.common.reminders.domain.INotificationAccess
import com.bottlecast.app.common.reminders.domain.IReminderStore
import com.bottlecast.app.common.reminders.domain.effectiveTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

/**
 * The reminder sheet (spec §3.16a): pick a time, then turn the daily reminder on, asking Android for
 * POST_NOTIFICATIONS on the way. A refusal isn't asked about again for a week; once Android won't ask
 * any more, the sheet points to Android's settings instead. It lives as long as the screen that hosts
 * the sheet, so [reset] starts each opening afresh.
 */
@HiltViewModel
class ReminderSheetViewModel @Inject constructor(
    private val store: IReminderStore,
    private val access: INotificationAccess,
    private val clock: Clock,
) : ViewModel() {

    private val _state: MutableStateFlow<ReminderSheetUiState> = MutableStateFlow(ReminderSheetUiState())
    val state: StateFlow<ReminderSheetUiState> = _state.asStateFlow()

    // the sheet sent the user to Android's settings, so coming back with notifications on turns the reminder on
    private var openedSettings = false

    // turning on didn't go through: the sheet stays, so it can be tried again
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[turnOn] Couldn't turn the daily reminder on")
        _state.update { it.copy(outcome = null) }
    }

    init {
        // once seen, the sheet doesn't open by itself again
        store.markRationaleOffered()
        viewModelScope.launch(exceptionHandler) {
            store.settings.collect { settings ->
                _state.update { it.copy(time = settings.time, quietStart = settings.quietStart, quietEnd = settings.quietEnd) }
            }
        }
    }

    fun onTimeChange(time: LocalTime) {
        store.setTime(time)
    }

    /** "Turn on reminders". Returns true when Android must be asked for the permission first. */
    fun onTurnOn(): Boolean = when {
        access.allowed() -> {
            enable()
            false
        }
        // the permission is there, but notifications are off in Android's settings: asking again does nothing
        access.permissionGranted() -> {
            block()
            false
        }
        else -> true
    }

    /** Android's answer. [canAskAgain] is false once Android won't show its prompt any more. */
    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        when {
            granted && access.allowed() -> enable()
            granted -> block()
            else -> {
                store.markDenied(clock.instant())
                if (canAskAgain) _state.update { it.copy(outcome = ReminderOutcome.Declined) } else block()
            }
        }
    }

    fun onOpenSettings() {
        openedSettings = true
    }

    /** The sheet is back on screen, perhaps from Android's settings. */
    fun onResume() {
        if (openedSettings && access.allowed()) enable()
    }

    /** The sheet has closed: the next opening asks afresh, with the reminder's settings as they are. */
    fun reset() {
        openedSettings = false
        _state.update { ReminderSheetUiState(time = it.time, quietStart = it.quietStart, quietEnd = it.quietEnd) }
    }

    private fun block() = _state.update { it.copy(blocked = true) }

    private fun enable() {
        openedSettings = false
        viewModelScope.launch(exceptionHandler) {
            store.markDenied(null)
            store.setEnabled(true).join()
            val time = store.settings.first().effectiveTime()
            _state.update { it.copy(outcome = ReminderOutcome.Enabled(time)) }
        }
    }
}

/** What the sheet asks with, and how it ended, once it has. */
data class ReminderSheetUiState(
    val time: LocalTime = DEFAULT_REMINDER_TIME,
    val quietStart: LocalTime = DEFAULT_QUIET_START,
    val quietEnd: LocalTime = DEFAULT_QUIET_END,
    /** Android won't ask for the permission, or notifications are off in its settings: [Open settings]. */
    val blocked: Boolean = false,
    val outcome: ReminderOutcome? = null,
)

sealed interface ReminderOutcome {
    /** The reminder is on, at [time] once quiet hours have had their say: the sheet closes with a snackbar. */
    data class Enabled(val time: LocalTime) : ReminderOutcome

    /** Android's prompt was refused: the sheet closes, and the Shore doesn't ask again for a week. */
    data object Declined : ReminderOutcome
}
