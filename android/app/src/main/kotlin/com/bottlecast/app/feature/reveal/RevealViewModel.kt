package com.bottlecast.app.feature.reveal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.chest.domain.ChestItem
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.feature.reveal.domain.CastAgo
import com.bottlecast.app.feature.reveal.domain.castAgo
import com.bottlecast.app.feature.shore.domain.ShoreNotice
import com.bottlecast.app.feature.shore.domain.ShoreNotices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Reveal (spec §3.7): a found bottle's message, to keep, share, throw back or report. A report waits
 * out its undo window here; leaving sends it at once. Leaving a fresh find unkept tells the Shore it'll
 * wait in the chest.
 */
@HiltViewModel
class RevealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seaRepository: ISeaRepository,
    private val seaActions: ISeaActions,
    private val chestRepository: IChestRepository,
    private val authenticationRepository: IAuthenticationRepository,
    connectivityMonitor: IConnectivityMonitor,
    streakRepository: IStreakRepository,
    private val shoreNotices: ShoreNotices,
    private val clock: Clock,
) : ViewModel() {

    private val messageId: String = checkNotNull(savedStateHandle[KEY_MESSAGE_ID]) { "Reveal needs a message id" }
    private val fresh: Boolean = savedStateHandle[KEY_FRESH] ?: false

    private var message: MessageEntity? = seaRepository.cachedMessage(messageId)

    // the report waiting out its undo window, if any
    private var pendingReport: PendingSeaAction.Report? = null
    private var thrownBack = false

    // what the bottle's Keep shows, kept apart so a reload doesn't lose it
    private var keep = RevealKeep()
    private var day: Int? = null

    private val _state: MutableStateFlow<RevealUiState> = MutableStateFlow(
        message?.let { RevealUiState.Showing(it.toRevealed(), RevealReport.None, fresh, keep, day) } ?: RevealUiState.Loading
    )
    val state: StateFlow<RevealUiState> = _state.asStateFlow()

    private val _effects = Channel<RevealEffect>(Channel.BUFFERED)
    val effects: Flow<RevealEffect> = _effects.receiveAsFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "There was a problem showing a found bottle")
        _state.value = RevealUiState.Failed
    }

    // only Keep's state hangs on this, so a failure here isn't a failure to show the bottle
    private val observeHandler = CoroutineExceptionHandler { _, exception ->
        Timber.w(exception, "Stopped following whether a found bottle is kept")
    }

    init {
        if (message == null) load()
        // Keep's state: whether it's kept already, and whether there's a connection to keep it; and the
        // streak's day for the overline, once today's find counts
        viewModelScope.launch(observeHandler) {
            combine(chestRepository.item(messageId), connectivityMonitor.isOnline, streakRepository.streak) { item, online, streak ->
                val kept = (item as? ChestItem.Found)?.bottle?.isKept == true
                Triple(kept, online, streak?.takeIf { it.foundToday }?.current)
            }.collect { (kept, online, streakDay) ->
                keep = keep.copy(kept = kept || keep.kept, online = online)
                day = streakDay
                _state.update { (it as? RevealUiState.Showing)?.copy(keep = keep, day = day) ?: it }
            }
        }
        viewModelScope.launch {
            seaActions.outcomes.collect { outcome ->
                val report = outcome.action as? PendingSeaAction.Report ?: return@collect
                if (report.id != pendingReport?.id) return@collect
                pendingReport = null
                updateReport(RevealReport.Reported(blocked = report.blockWriter, queuedOffline = outcome.queuedOffline))
            }
        }
    }

    /** Tries reading the bottle again, after it failed to load. */
    fun onRetry() {
        _state.value = RevealUiState.Loading
        load()
    }

    /** Reports the bottle once [windowMillis] passes without an undo (null: once the user moves on). */
    fun onReport(reason: ReportReason?, blockWriter: Boolean, windowMillis: Long?) {
        val bottle = message ?: return
        val actionId = seaActions.report(bottle, reason, blockWriter, windowMillis)
        pendingReport = PendingSeaAction.Report(actionId, bottle.id, blockWriter)
        updateReport(RevealReport.Pending)
    }

    fun onUndoReport() {
        val report = pendingReport ?: return
        seaActions.undo(report.id)
        pendingReport = null
        updateReport(RevealReport.None)
    }

    /** Throws the bottle back, once [windowMillis] passes without an undo on the Shore. */
    fun onThrowBack(windowMillis: Long?) {
        val bottle = message ?: return
        thrownBack = true
        seaActions.throwBack(bottle, windowMillis)
    }

    /**
     * Keeps the bottle (spec §1.7), once a guest has signed in with Google. [signedIn] straight after
     * the sign-in sheet, before the signed-in user may have caught up.
     */
    fun onKeep(signedIn: Boolean = false) {
        if (keep.kept || keep.keeping) return
        if (!signedIn && authenticationRepository.user.value.googleUid == null) {
            _effects.trySend(RevealEffect.NeedsSignIn)
            return
        }
        updateKeep(keep.copy(keeping = true))
        viewModelScope.launch {
            val result = chestRepository.keep(messageId)
            updateKeep(keep.copy(keeping = false, kept = keep.kept || result == KeepResult.Kept))
            _effects.send(RevealEffect.KeepFinished(result))
        }
    }

    // Leaving Reveal sends a report that's still waiting, rather than stranding it. A fresh find left
    // as it was waits in the chest, which the Shore says once (spec §3.7).
    override fun onCleared() {
        pendingReport?.let { seaActions.commit(it.id) }
        val showing = _state.value as? RevealUiState.Showing
        if (fresh && showing != null && showing.report == RevealReport.None && !thrownBack && !keep.kept && !keep.keeping) {
            shoreNotices.post(ShoreNotice.WaitsInChest)
        }
    }

    private fun load() {
        viewModelScope.launch(exceptionHandler) {
            val loaded = seaRepository.getMessage(messageId)
            message = loaded
            _state.value = loaded?.let { RevealUiState.Showing(it.toRevealed(), RevealReport.None, fresh, keep, day) }
                ?: RevealUiState.Failed
        }
    }

    private fun updateReport(report: RevealReport) {
        _state.update { (it as? RevealUiState.Showing)?.copy(report = report) ?: it }
    }

    private fun updateKeep(updated: RevealKeep) {
        keep = updated
        _state.update { (it as? RevealUiState.Showing)?.copy(keep = updated) ?: it }
    }

    private fun MessageEntity.toRevealed() = RevealedBottle(
        id = id,
        text = message,
        byline = penName?.let { Byline.PenName(it) } ?: Byline.Stranger,
        castAgo = createdAt?.let { castAgo(it, ZonedDateTime.now(clock)) },
    )

    private companion object {
        // the Reveal route's arguments
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_FRESH = "fresh"
    }
}

sealed class RevealUiState {
    data object Loading : RevealUiState()

    /**
     * [fresh] when the bottle was just uncorked, so the moonlight sweeps across it once. [day] is the
     * streak's day, once today's find counts in it.
     */
    data class Showing(
        val bottle: RevealedBottle,
        val report: RevealReport,
        val fresh: Boolean,
        val keep: RevealKeep = RevealKeep(),
        val day: Int? = null,
    ) : RevealUiState()

    /** The bottle can't be read: it was taken out of the sea, or there's no connection to fetch it. */
    data object Failed : RevealUiState()
}

data class RevealedBottle(
    val id: String,
    val text: String,
    val byline: Byline,
    /** Null for a bottle whose cast time isn't known yet. */
    val castAgo: CastAgo?,
)

/** Who the byline says wrote the bottle: never a name or photo from the writer's account (spec §3.7). */
sealed interface Byline {
    data object Stranger : Byline
    data class PenName(val name: String) : Byline
}

/** Keep's pill (spec §3.7): until [kept], and then the "In your chest · Kept" chip. */
data class RevealKeep(
    val kept: Boolean = false,
    /** The keep is on its way to the server: the pill shows a spinner. */
    val keeping: Boolean = false,
    /** Keeping needs a connection (spec §3.8a). */
    val online: Boolean = true,
)

sealed interface RevealEffect {
    /** A guest taps Keep: the sign-in sheet first (spec §3.7). */
    data object NeedsSignIn : RevealEffect

    data class KeepFinished(val result: KeepResult) : RevealEffect
}

sealed interface RevealReport {
    data object None : RevealReport

    /** Reported, with its undo window still open. */
    data object Pending : RevealReport

    data class Reported(val blocked: Boolean, val queuedOffline: Boolean) : RevealReport
}
