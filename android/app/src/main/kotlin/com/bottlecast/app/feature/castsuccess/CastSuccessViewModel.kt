package com.bottlecast.app.feature.castsuccess

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.messages.domain.CastStatus
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Cast success (spec §3.5): the cast plays once, then the bottle drifts. A queued cast shows as
 * Corked until the server has it, then turns to Drifting.
 */
@HiltViewModel
class CastSuccessViewModel @Inject constructor(
    castCoordinator: ICastCoordinator,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val messageId: String = checkNotNull(savedStateHandle[MESSAGE_ID])

    private val _state: MutableStateFlow<CastSuccessUiState> = MutableStateFlow(
        CastSuccessUiState(
            look = bottleLookFor(messageId),
            queued = savedStateHandle[QUEUED] ?: false,
            sealable = savedStateHandle[SEALABLE] ?: true,
            played = savedStateHandle[PLAYED] ?: false,
        )
    )
    val state: StateFlow<CastSuccessUiState> = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.w(throwable, "[status] Stopped following the cast")
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            castCoordinator.status(messageId).collect { status ->
                // a refusal brings the words back to the draft; what this screen says then is the
                // tidy-up slice's, so it stays as it was
                if (status != CastStatus.Refused) _state.update { it.copy(queued = status == CastStatus.Queued) }
            }
        }
    }

    /** The cast has played, so a rotation shows the bottle drifting rather than playing it again. */
    fun onPlayed() {
        savedStateHandle[PLAYED] = true
        _state.update { it.copy(played = true) }
    }

    private companion object {
        // the route's arguments
        const val MESSAGE_ID = "messageId"
        const val QUEUED = "queued"
        const val SEALABLE = "sealable"
        const val PLAYED = "played"
    }
}

data class CastSuccessUiState(
    val look: BottleLook,
    /** Corked, "Waiting for connection", while the cast waits for the server. */
    val queued: Boolean,
    /** Left out of the journey's Sealed when the writer switched sealing off. */
    val sealable: Boolean,
    val played: Boolean,
)
