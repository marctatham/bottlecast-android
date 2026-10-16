package com.bottlecast.app.feature.composepreview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.CastOutcome
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.ILanguageDetector
import com.bottlecast.app.common.messages.domain.NewCast
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.settings.domain.ISettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Preview (spec §3.4b): the bottle as it will wash up, where the writer consents to it going out,
 * chooses whether to sign it and whether a finder may seal it, and on a first cast accepts the Terms.
 */
@HiltViewModel
class ComposePreviewViewModel @Inject constructor(
    private val drafts: IDraftRepository,
    private val castCoordinator: ICastCoordinator,
    private val profileRepository: IProfileRepository,
    private val languageDetector: ILanguageDetector,
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val settingsStore: ISettingsStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state: MutableStateFlow<ComposePreviewUiState> = MutableStateFlow(ComposePreviewUiState.Loading)
    val state: StateFlow<ComposePreviewUiState> = _state.asStateFlow()

    private val _effects = Channel<ComposePreviewEffect>(Channel.BUFFERED)
    val effects: Flow<ComposePreviewEffect> = _effects.receiveAsFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "[cast] Couldn't cast")
        update { it.copy(casting = false) }
        _effects.trySend(ComposePreviewEffect.CastFailed)
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            val draft = drafts.load()
            if (draft == null || draft.text.isBlank()) {
                // nothing to preview, such as after the system closed the app here
                _state.value = ComposePreviewUiState.NoDraft
                return@launch
            }
            _state.value = ComposePreviewUiState.Ready(
                messageId = draft.messageId,
                text = draft.text.trim(),
                // Settings' default, until the writer chooses for this bottle
                signWithPenName = savedStateHandle[SIGN] ?: settingsStore.settings.first().signWithPenName,
                sealable = savedStateHandle[SEALABLE] ?: true,
                termsTicked = savedStateHandle[TERMS] ?: false,
            )
            combine(
                profileRepository.profile,
                connectivityMonitor.isOnline,
                castCoordinator.castsLeftToday,
            ) { profile, online, left -> Triple(profile, online, left) }
                .collect { (profile, online, left) ->
                    update {
                        it.copy(
                            penName = profile?.penName,
                            needsTerms = profile?.acceptedTerms != true,
                            isOffline = !online,
                            castsLeft = left,
                        )
                    }
                }
        }
    }

    fun onSignWithPenName(sign: Boolean) {
        savedStateHandle[SIGN] = sign
        update { it.copy(signWithPenName = sign) }
    }

    /** A pen name was just chosen in the sheet, so the bottle is signed with it. */
    fun onPenNameSaved() = onSignWithPenName(true)

    fun onSetPenName() {
        _effects.trySend(ComposePreviewEffect.OpenPenName)
    }

    fun onSealable(sealable: Boolean) {
        savedStateHandle[SEALABLE] = sealable
        update { it.copy(sealable = sealable) }
    }

    fun onTermsTicked(ticked: Boolean) {
        savedStateHandle[TERMS] = ticked
        update { it.copy(termsTicked = ticked) }
    }

    /** "Cast it": a guest signs in first, then the cast carries on (spec §3.4b). */
    fun onCast() {
        val ready = _state.value as? ComposePreviewUiState.Ready ?: return
        if (!ready.canCast) return
        if (authenticationRepository.user.value.googleUid == null) {
            _effects.trySend(ComposePreviewEffect.NeedsSignIn)
            return
        }
        update { it.copy(casting = true) }
        viewModelScope.launch(exceptionHandler) {
            val cast = NewCast(
                messageId = ready.messageId,
                text = ready.text,
                lang = languageDetector.detect(ready.text),
                penName = ready.penName?.takeIf { ready.signWithPenName },
                sealable = ready.sealable,
                acceptTerms = ready.needsTerms && ready.termsTicked,
            )
            when (val outcome = castCoordinator.cast(cast)) {
                is CastOutcome.Cast -> _effects.send(ComposePreviewEffect.Cast(cast.messageId, outcome.queued, cast.sealable))
                CastOutcome.Refused -> {
                    update { it.copy(casting = false) }
                    _effects.send(ComposePreviewEffect.CastFailed)
                }
                CastOutcome.LimitReached -> update { it.copy(casting = false, castsLeft = 0) }
                CastOutcome.NotSignedIn -> {
                    update { it.copy(casting = false) }
                    _effects.send(ComposePreviewEffect.NeedsSignIn)
                }
            }
        }
    }

    private fun update(change: (ComposePreviewUiState.Ready) -> ComposePreviewUiState.Ready) =
        _state.update { (it as? ComposePreviewUiState.Ready)?.let(change) ?: it }

    private companion object {
        const val SIGN = "signWithPenName"
        const val SEALABLE = "sealable"
        const val TERMS = "termsTicked"
    }
}

sealed class ComposePreviewUiState {

    data object Loading : ComposePreviewUiState()

    /** The draft has gone; Preview goes back to Compose. */
    data object NoDraft : ComposePreviewUiState()

    data class Ready(
        /** The bottle's id, which seeds its shape. */
        val messageId: String,
        val text: String,
        /** The account's pen name, or null when it has none yet ("Set a pen name…"). */
        val penName: String? = null,
        /** "Sign with my pen name": Settings' default until the writer chooses (spec §3.17). */
        val signWithPenName: Boolean = false,
        /** "Let the finder seal this as a keepsake", on by default (spec §3.4b). */
        val sealable: Boolean = true,
        /** A first cast shows the Terms checkbox, unticked. */
        val needsTerms: Boolean = true,
        val termsTicked: Boolean = false,
        /** Offline the button reads "Cast when back online", and still casts. */
        val isOffline: Boolean = false,
        /** Today's casts left; at 0 the button waits for tomorrow. */
        val castsLeft: Int? = null,
        val casting: Boolean = false,
    ) : ComposePreviewUiState() {
        val signedWith: String? get() = penName?.takeIf { signWithPenName }
        val canCast: Boolean get() = !casting && (!needsTerms || termsTicked) && castsLeft != 0
    }
}

sealed interface ComposePreviewEffect {
    /** Cast: on to Cast success. */
    data class Cast(val messageId: String, val queued: Boolean, val sealable: Boolean) : ComposePreviewEffect

    /** "Couldn't cast. Your words are saved." [Try again]. */
    data object CastFailed : ComposePreviewEffect

    /** A guest tapped Cast it: the sign-in sheet, then the cast carries on. */
    data object NeedsSignIn : ComposePreviewEffect

    data object OpenPenName : ComposePreviewEffect
}
