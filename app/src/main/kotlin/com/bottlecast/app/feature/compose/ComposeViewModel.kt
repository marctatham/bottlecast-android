package com.bottlecast.app.feature.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.ILanguageDetector
import com.bottlecast.app.feature.compose.domain.ComposeHelper
import com.bottlecast.app.feature.compose.domain.DailyPrompt
import com.bottlecast.app.feature.compose.domain.DraftMeter
import com.bottlecast.app.feature.compose.domain.composeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Compose (spec §3.4): writing a bottle, saved as a draft a second after each change. Its words are
 * the draft's, so Preview reads exactly what's written here.
 */
@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val drafts: IDraftRepository,
    castCoordinator: ICastCoordinator,
    private val languageDetector: ILanguageDetector,
    clock: Clock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The editor's text, kept here so it outlives a rotation. */
    val editor = TextFieldState()

    private val _state: MutableStateFlow<ComposeUiState> = MutableStateFlow(ComposeUiState.Loading)
    val state: StateFlow<ComposeUiState> = _state.asStateFlow()

    private val _effects = Channel<ComposeEffect>(Channel.BUFFERED)
    val effects: Flow<ComposeEffect> = _effects.receiveAsFlow()

    private val placeholder = DailyPrompt.forDate(LocalDate.now(clock))
    private var minimumTapped = false
    private var lang: String? = null

    // The editor still opens if the saved draft can't be read, so writing never waits on it.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[compose] Something went wrong while writing")
        if (_state.value == ComposeUiState.Loading) _state.value = ComposeUiState.Writing(placeholder = placeholder)
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            val saved = drafts.load()
            if (saved != null && editor.text.isEmpty()) {
                editor.setTextAndPlaceCursorAtEnd(saved.text)
                // once per visit, not again when Preview's Edit comes back to it
                if (savedStateHandle.get<Boolean>(RESTORED_SHOWN) != true) {
                    savedStateHandle[RESTORED_SHOWN] = true
                    _effects.send(ComposeEffect.RestoredDraft)
                }
            }
            if (savedStateHandle.get<Boolean>(JUST_SIGNED_IN) == true && savedStateHandle.get<Boolean>(SIGNED_IN_SHOWN) != true) {
                savedStateHandle[SIGNED_IN_SHOWN] = true
                _effects.send(ComposeEffect.SignedIn)
            }
            refresh()
            launch { followEditor() }
            launch { followLanguage() }
            launch { followRestores() }
            castCoordinator.castsLeftToday.collect { left -> update { it.copy(castsLeft = left) } }
        }
    }

    private suspend fun followEditor() {
        snapshotFlow { editor.text.toString() }.distinctUntilChanged().collect { text ->
            // nothing written yet isn't a draft
            if (text.isNotEmpty() || drafts.draft.value != null) drafts.update(text)
            refresh()
        }
    }

    // A cast the server refused after Preview had gone comes back as the draft (spec §3.4b). It only
    // comes back when nothing newer is written, so an empty editor takes it.
    private suspend fun followRestores() {
        drafts.draft.collect { draft ->
            if (draft != null && draft.text.isNotBlank() && editor.text.isEmpty()) {
                editor.setTextAndPlaceCursorAtEnd(draft.text)
                _effects.send(ComposeEffect.RestoredDraft)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun followLanguage() {
        snapshotFlow { editor.text.toString() }
            .distinctUntilChanged()
            .debounce(LANGUAGE_DEBOUNCE_MILLIS)
            .collect { text ->
                lang = if (DraftMeter.of(text).meetsMinimum) languageDetector.detect(text.trim()) else null
                refresh()
            }
    }

    /** Preview, tapped with enough written: saved first, so Preview has every word. */
    fun onPreview() {
        viewModelScope.launch(exceptionHandler) {
            drafts.flush()
            _effects.send(ComposeEffect.OpenPreview)
        }
    }

    /** Preview, tapped too soon: the minimum turns red, with a buzz (spec §3.4). */
    fun onPreviewTooShort() {
        minimumTapped = true
        refresh()
        _effects.trySend(ComposeEffect.Rejected)
    }

    /** The close X or back: with words written, ask before they go. */
    fun onClose() {
        if (editor.text.isBlank()) {
            _effects.trySend(ComposeEffect.Close)
        } else {
            update { it.copy(discarding = true) }
        }
    }

    fun onKeepWriting() = update { it.copy(discarding = false) }

    fun onToss() {
        drafts.clear()
        editor.clearText()
        update { it.copy(discarding = false) }
        _effects.trySend(ComposeEffect.Close)
    }

    /** The restored-draft snackbar's "Start fresh". */
    fun onStartFresh() {
        drafts.clear()
        editor.clearText()
        minimumTapped = false
    }

    private fun refresh() {
        val meter = DraftMeter.of(editor.text.toString())
        if (meter.meetsMinimum) minimumTapped = false
        val writing = (_state.value as? ComposeUiState.Writing) ?: ComposeUiState.Writing(placeholder = placeholder)
        _state.value = writing.copy(meter = meter, helper = composeHelper(meter, minimumTapped, lang))
    }

    private fun update(change: (ComposeUiState.Writing) -> ComposeUiState.Writing) =
        _state.update { (it as? ComposeUiState.Writing)?.let(change) ?: it }

    private companion object {
        // the route's argument
        const val JUST_SIGNED_IN = "justSignedIn"
        const val RESTORED_SHOWN = "restoredShown"
        const val SIGNED_IN_SHOWN = "signedInShown"
        const val LANGUAGE_DEBOUNCE_MILLIS = 600L
    }
}

sealed class ComposeUiState {

    /** Until the saved draft is read, so a restored one doesn't flash in. */
    data object Loading : ComposeUiState()

    data class Writing(
        @param:StringRes val placeholder: Int,
        val meter: DraftMeter = DraftMeter(0, 0, 0),
        val helper: ComposeHelper = ComposeHelper.Minimum(asError = false),
        /** Today's casts left; null for a guest. At 1, a chip; at 0, the limit (spec §3.4). */
        val castsLeft: Int? = null,
        /** "Toss this draft?" is open. */
        val discarding: Boolean = false,
    ) : ComposeUiState()
}

sealed interface ComposeEffect {
    /** "We kept your draft." [Start fresh]. */
    data object RestoredDraft : ComposeEffect

    /** "Signed in. Your bottles stay anonymous unless you pick a pen name." [Pick one]. */
    data object SignedIn : ComposeEffect

    data object Rejected : ComposeEffect

    data object OpenPreview : ComposeEffect

    data object Close : ComposeEffect
}
