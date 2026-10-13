package com.bottlecast.app.feature.reveal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.feature.reveal.domain.CastAgo
import com.bottlecast.app.feature.reveal.domain.castAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Reveal (spec §3.7): a found bottle's message, to share, throw back or report. Keep arrives with the
 * Chest in slice 4. A report waits out its undo window here; leaving sends it at once.
 */
@HiltViewModel
class RevealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seaRepository: ISeaRepository,
    private val seaActions: ISeaActions,
    private val clock: Clock,
) : ViewModel() {

    private val messageId: String = checkNotNull(savedStateHandle[KEY_MESSAGE_ID]) { "Reveal needs a message id" }
    private val fresh: Boolean = savedStateHandle[KEY_FRESH] ?: false

    private var message: MessageEntity? = seaRepository.cachedMessage(messageId)

    // the report waiting out its undo window, if any
    private var pendingReport: PendingSeaAction.Report? = null

    private val _state: MutableStateFlow<RevealUiState> = MutableStateFlow(
        message?.let { RevealUiState.Showing(it.toRevealed(), RevealReport.None, fresh) } ?: RevealUiState.Loading
    )
    val state: StateFlow<RevealUiState> = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "There was a problem showing a found bottle")
        _state.value = RevealUiState.Failed
    }

    init {
        if (message == null) load()
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
        seaActions.throwBack(bottle, windowMillis)
    }

    // Leaving Reveal sends a report that's still waiting, rather than stranding it.
    override fun onCleared() {
        pendingReport?.let { seaActions.commit(it.id) }
    }

    private fun load() {
        viewModelScope.launch(exceptionHandler) {
            val loaded = seaRepository.getMessage(messageId)
            message = loaded
            _state.value = loaded?.let { RevealUiState.Showing(it.toRevealed(), RevealReport.None, fresh) }
                ?: RevealUiState.Failed
        }
    }

    private fun updateReport(report: RevealReport) {
        _state.update { (it as? RevealUiState.Showing)?.copy(report = report) ?: it }
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

    /** [fresh] when the bottle was just uncorked, so the moonlight sweeps across it once. */
    data class Showing(val bottle: RevealedBottle, val report: RevealReport, val fresh: Boolean) : RevealUiState()

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

sealed interface RevealReport {
    data object None : RevealReport

    /** Reported, with its undo window still open. */
    data object Pending : RevealReport

    data class Reported(val blocked: Boolean, val queuedOffline: Boolean) : RevealReport
}
