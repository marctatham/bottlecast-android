package com.bottlecast.app.feature.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.feature.discover.domain.UncorkEffect
import com.bottlecast.app.feature.discover.domain.UncorkMachine
import com.bottlecast.app.feature.discover.domain.UncorkMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.TimeSource

/**
 * Discover (spec §3.6): today's bottle, waiting to be uncorked. The cork's progress lives here, so
 * it survives a rotation, and the find is recorded only at the pop: backing out before it leaves
 * the day unused.
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val seaRepository: ISeaRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
    timeSource: TimeSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private data class Model(
        val look: BottleLook = BottleLook.Default,
        val seaCalm: Boolean = false,
        val online: Boolean = true,
        val alreadyFound: Boolean = false,
        val steps: Int = 0,
        val moving: Boolean = false,
        val pop: PopPhase = PopPhase.NotPopped,
        /** What the find came back with, when it wasn't a bottle. */
        val outcome: DiscoverUiState? = null,
    )

    private val start = timeSource.markNow()
    private val machine = UncorkMachine(initialSteps = savedStateHandle[KEY_STEPS] ?: 0)

    // everything the screen depends on; the state is worked out from it after each change
    private var model = Model(steps = machine.steps, online = connectivityMonitor.isOnline.value)

    private val _state: MutableStateFlow<DiscoverUiState> = MutableStateFlow(model.toUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    private val _ringProgress = MutableStateFlow(machine.progressAt(now()))
    val ringProgress: StateFlow<Float> = _ringProgress.asStateFlow()

    private val _effects = Channel<UncorkEffect>(Channel.BUFFERED)

    /** Each shake, tick and pop as it happens, for haptics, the wobble and analytics. */
    val effects: Flow<UncorkEffect> = _effects.receiveAsFlow()

    private var finding = false

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "There was a problem getting today's bottle ready")
        if (machine.popped) {
            finding = false
            update { it.copy(outcome = DiscoverUiState.Failed) }
        }
    }

    init {
        // today already has a find (on another device, or a moment ago): back to the Shore
        viewModelScope.launch(exceptionHandler) {
            if (seaRepository.observeToday(LocalDate.now(clock)).first() != null && !machine.popped) {
                update { it.copy(alreadyFound = true) }
            }
        }
        viewModelScope.launch(exceptionHandler) {
            seaRepository.candidate.collect { candidate ->
                when (candidate) {
                    is CandidateState.Available -> update {
                        it.copy(look = bottleLookFor(candidate.message.id), seaCalm = false)
                    }

                    CandidateState.SeaCalm -> update { it.copy(seaCalm = true) }
                    CandidateState.Unknown -> Unit
                }
            }
        }
        // the Shore has usually picked it already, and then this costs nothing
        viewModelScope.launch(exceptionHandler) { seaRepository.prefetchCandidate() }
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { online -> update { it.copy(online = online) } }
        }
    }

    fun onShake() = apply(machine.shake())

    fun onBottlePressed() {
        machine.press(now())
        sync()
    }

    fun onBottleReleased() = apply(machine.release(now()))

    /** Moves a hold on by a frame; the screen calls it each frame while the ring is moving. */
    fun onFrame() = apply(machine.frame(now()))

    /** Pops the cork at once: the pill, TalkBack, a keyboard, or reduced motion's Open bottle. */
    fun onUncork(method: UncorkMethod) = apply(machine.activate(method))

    /** Tries again after a find that failed, or once the phone is back online. */
    fun onTryAgain() {
        update { it.copy(outcome = null) }
        if (machine.popped) find()
    }

    private fun apply(effects: List<UncorkEffect>) {
        sync()
        effects.forEach { effect ->
            _effects.trySend(effect)
            if (effect is UncorkEffect.Pop) find()
        }
    }

    private fun sync() {
        savedStateHandle[KEY_STEPS] = machine.steps
        _ringProgress.value = machine.progressAt(now())
        update { it.copy(steps = machine.steps, moving = machine.isMoving) }
    }

    // The find runs once at a time. The scroll waits half out while it's late, then the bottle
    // slips back; the find itself carries on, and trying again picks it up.
    private fun find() {
        if (finding) return
        finding = true
        update { it.copy(pop = PopPhase.Recording, outcome = null) }
        viewModelScope.launch(exceptionHandler) {
            val result = async { seaRepository.findTodaysBottle() }
            val outcome = withTimeoutOrNull(LATE_AFTER_MILLIS) { result.await() }
                ?: run {
                    update { it.copy(pop = PopPhase.Late) }
                    withTimeoutOrNull(GIVE_UP_AFTER_MILLIS) { result.await() }
                }
            finding = false
            update { current ->
                when (outcome) {
                    is FindResult.Found -> current.copy(pop = PopPhase.Revealed(outcome.message.id))
                    is FindResult.AlreadyFound -> current.copy(outcome = DiscoverUiState.AlreadyFound)
                    FindResult.SeaCalm -> current.copy(outcome = DiscoverUiState.SeaCalm)
                    FindResult.Offline -> current.copy(outcome = DiscoverUiState.Offline)
                    FindResult.Failed, null -> current.copy(outcome = DiscoverUiState.Failed)
                }
            }
        }
    }

    private fun update(change: (Model) -> Model) {
        model = change(model)
        _state.value = model.toUiState()
    }

    private fun now(): Long = start.elapsedNow().inWholeMilliseconds

    private fun Model.toUiState(): DiscoverUiState = when {
        alreadyFound -> DiscoverUiState.AlreadyFound
        outcome != null -> outcome
        // before the pop, a calm sea or a lost connection shows at once
        pop == PopPhase.NotPopped && seaCalm -> DiscoverUiState.SeaCalm
        pop == PopPhase.NotPopped && !online -> DiscoverUiState.Offline
        else -> DiscoverUiState.Ready(look = look, steps = steps, moving = moving, pop = pop)
    }

    private companion object {
        const val KEY_STEPS = "steps"

        /** The scroll waits half out once the find has taken this long (spec §3.6)... */
        const val LATE_AFTER_MILLIS = 600L

        /** ...and the bottle slips back after this much longer. */
        const val GIVE_UP_AFTER_MILLIS = 4_000L
    }
}

sealed class DiscoverUiState {

    /** The bottle, before and during the pop. [moving] while a hold fills or drains the ring. */
    data class Ready(
        val look: BottleLook,
        val steps: Int,
        val moving: Boolean,
        val pop: PopPhase,
    ) : DiscoverUiState()

    data object SeaCalm : DiscoverUiState()

    data object Offline : DiscoverUiState()

    data object Failed : DiscoverUiState()

    /** Today already has a find, so this is the Shore's to show. */
    data object AlreadyFound : DiscoverUiState()
}

sealed interface PopPhase {
    data object NotPopped : PopPhase

    /** The cork is out and the find is being recorded. */
    data object Recording : PopPhase

    /** The find is taking a while: the scroll waits half out, shimmering. */
    data object Late : PopPhase

    data class Revealed(val messageId: String) : PopPhase
}
