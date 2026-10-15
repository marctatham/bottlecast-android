package com.bottlecast.app.feature.keepsake

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.chest.domain.ChestItem
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.feature.chest.domain.BottleStanding
import com.bottlecast.app.feature.chest.domain.castStanding
import com.bottlecast.app.feature.chest.domain.standing
import com.bottlecast.app.feature.reveal.Byline
import com.bottlecast.app.feature.reveal.domain.CastAgo
import com.bottlecast.app.feature.reveal.domain.castAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Keepsake detail (spec §3.11): one bottle from the chest, found or cast, with its journey. An unkept
 * find can be kept, thrown back or reported, and a drifting cast pulled from the sea. Sealing, and
 * everything on-chain, waits for slice 6.
 */
@HiltViewModel
class KeepsakeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chestRepository: IChestRepository,
    private val seaActions: ISeaActions,
    private val authenticationRepository: IAuthenticationRepository,
    connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
) : ViewModel() {

    private val messageId: String = checkNotNull(savedStateHandle[KEY_MESSAGE_ID]) { "Keepsake detail needs a message id" }

    private val _state: MutableStateFlow<KeepsakeUiState> = MutableStateFlow(KeepsakeUiState.Loading)
    val state: StateFlow<KeepsakeUiState> = _state.asStateFlow()

    private val _effects = Channel<KeepsakeEffect>(Channel.BUFFERED)
    val effects: Flow<KeepsakeEffect> = _effects.receiveAsFlow()

    // the bottle as it stands, for the actions that need it
    private var message: MessageEntity? = null
    private var keeping = false

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "Couldn't follow a bottle in the chest")
        _state.value = KeepsakeUiState.Gone
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            combine(chestRepository.item(messageId), connectivityMonitor.isOnline) { item, online -> item to online }
                .collect { (item, online) ->
                    message = when (item) {
                        is ChestItem.Found -> item.bottle.message
                        is ChestItem.Cast -> item.message
                        null -> null
                    }
                    _state.value = item?.let { KeepsakeUiState.Showing(detailFor(it, online)) } ?: KeepsakeUiState.Gone
                }
        }
    }

    /**
     * Keeps an unkept find (spec §1.7), once a guest has signed in with Google. [signedIn] straight
     * after the sign-in sheet, before the signed-in user may have caught up.
     */
    fun onKeep(signedIn: Boolean = false) {
        if (keeping) return
        if (!signedIn && authenticationRepository.user.value.googleUid == null) {
            _effects.trySend(KeepsakeEffect.NeedsSignIn)
            return
        }
        setKeeping(true)
        viewModelScope.launch {
            val result = chestRepository.keep(messageId)
            setKeeping(false)
            _effects.send(KeepsakeEffect.KeepFinished(result))
        }
    }

    /** Throws an unkept find back; the Chest offers the Undo for [windowMillis]. */
    fun onThrowBack(windowMillis: Long?) {
        val bottle = message ?: return
        seaActions.throwBack(bottle, windowMillis)
        _effects.trySend(KeepsakeEffect.Left)
    }

    /** Reports an unkept find; the Chest offers the Undo for [windowMillis]. */
    fun onReport(reason: ReportReason?, blockWriter: Boolean, windowMillis: Long?) {
        val bottle = message ?: return
        seaActions.report(bottle, reason, blockWriter, windowMillis)
        _effects.trySend(KeepsakeEffect.Left)
    }

    /** Pulls a drifting bottle of one's own from the sea, after the confirmation (spec §3.11). */
    fun onPull() {
        viewModelScope.launch {
            _effects.send(KeepsakeEffect.PullFinished(chestRepository.pull(messageId)))
        }
    }

    private fun setKeeping(busy: Boolean) {
        keeping = busy
        val showing = _state.value as? KeepsakeUiState.Showing ?: return
        val keep = showing.detail.keep ?: return
        _state.value = showing.copy(detail = showing.detail.copy(keep = keep.copy(keeping = busy)))
    }

    private fun detailFor(item: ChestItem, online: Boolean): KeepsakeDetail {
        val now = clock.instant()
        val zone = clock.zone
        return when (item) {
            is ChestItem.Found -> foundDetail(item.bottle, now, zone, online)
            is ChestItem.Cast -> castDetail(item.message, now, zone)
        }
    }

    private fun foundDetail(bottle: FoundBottle, now: Instant, zone: ZoneId, online: Boolean): KeepsakeDetail {
        val standing = bottle.standing(now)
        val message = bottle.message
        val notKept = standing as? BottleStanding.NotKept
        val driftsOn = bottle.driftsBackAt?.atZone(zone)?.toLocalDate()
        val today = LocalDate.now(clock)
        return KeepsakeDetail(
            messageId = bottle.messageId,
            text = message?.message?.takeIf { standing != BottleStanding.Removed },
            byline = message?.penName?.let { Byline.PenName(it) } ?: Byline.Stranger,
            castAgo = message?.createdAt?.let { castAgo(it, ZonedDateTime.now(clock)) },
            lang = message?.lang,
            standing = standing,
            notice = when {
                standing == BottleStanding.UnderReview -> KeepsakeNotice.UnderReview
                standing == BottleStanding.Removed -> KeepsakeNotice.Removed
                notKept != null && driftsOn == today -> KeepsakeNotice.DriftsToday
                notKept != null && driftsOn == today.plusDays(1) -> KeepsakeNotice.DriftsTomorrow
                else -> null
            },
            journey = listOf(
                JourneyStop(JourneyStep.Cast, message?.createdAt?.localDate(zone)),
                JourneyStop(JourneyStep.Found, bottle.foundAt?.localDate(zone)),
                JourneyStop(JourneyStep.Kept, bottle.keptAt?.localDate(zone)),
            ),
            ownCast = false,
            keep = notKept?.let { KeepsakeKeep(daysLeft = it.daysLeft, online = online, keeping = keeping) },
            actions = if (notKept != null) setOf(KeepsakeAction.ThrowBack, KeepsakeAction.Report) else emptySet(),
            shareable = message != null && standing != BottleStanding.Removed && standing != BottleStanding.UnderReview,
        )
    }

    private fun castDetail(message: MessageEntity, now: Instant, zone: ZoneId): KeepsakeDetail {
        val standing = message.castStanding(now)
        val found = standing == BottleStanding.Found || standing == BottleStanding.Kept
        return KeepsakeDetail(
            messageId = message.id,
            text = message.message.takeIf { standing != BottleStanding.Removed },
            byline = message.penName?.let { Byline.PenName(it) } ?: Byline.Stranger,
            castAgo = message.createdAt?.let { castAgo(it, ZonedDateTime.now(clock)) },
            lang = message.lang,
            standing = standing,
            notice = when (standing) {
                BottleStanding.UnderReview -> KeepsakeNotice.UnderReview
                BottleStanding.Removed -> KeepsakeNotice.Removed
                else -> null
            },
            // the writer sees when it was found and kept, never by whom
            journey = listOf(
                JourneyStop(JourneyStep.Cast, message.createdAt?.localDate(zone) ?: LocalDate.now(clock)),
                JourneyStop(JourneyStep.FoundBySomeone, message.foundAt?.takeIf { found }?.localDate(zone)),
                JourneyStop(JourneyStep.Kept, message.keptAt?.localDate(zone)),
            ),
            ownCast = true,
            keep = null,
            actions = if (standing == BottleStanding.Drifting) setOf(KeepsakeAction.Pull) else emptySet(),
            shareable = standing != BottleStanding.Removed && standing != BottleStanding.UnderReview,
        )
    }

    private fun Instant.localDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

    private companion object {
        // the Keepsake route's argument
        const val KEY_MESSAGE_ID = "messageId"
    }
}

sealed class KeepsakeUiState {
    data object Loading : KeepsakeUiState()

    data class Showing(val detail: KeepsakeDetail) : KeepsakeUiState()

    /** It isn't in the chest any more: thrown back, drifted away, or pulled, elsewhere. */
    data object Gone : KeepsakeUiState()
}

/** A bottle as Keepsake detail shows it (spec §3.11). */
data class KeepsakeDetail(
    val messageId: String,
    /** Null when its words are hidden: a bottle removed for breaking the guidelines. */
    val text: String?,
    val byline: Byline,
    val castAgo: CastAgo?,
    val lang: String?,
    val standing: BottleStanding,
    val notice: KeepsakeNotice?,
    val journey: List<JourneyStop>,
    /** One of the user's own bottles: the journey never says who found or kept it. */
    val ownCast: Boolean,
    /** "Keep this bottle · 5 days left", for an unkept find. */
    val keep: KeepsakeKeep?,
    /** What the overflow menu offers; nothing hides it. */
    val actions: Set<KeepsakeAction>,
    val shareable: Boolean,
)

data class KeepsakeKeep(val daysLeft: Int, val online: Boolean, val keeping: Boolean)

/** The status row's line, where the spec gives one (spec §3.11, §3.18). */
enum class KeepsakeNotice { UnderReview, Removed, DriftsTomorrow, DriftsToday }

enum class KeepsakeAction { ThrowBack, Report, Pull }

/** A stop on the journey; a null [date] is still to come. */
data class JourneyStop(val step: JourneyStep, val date: LocalDate?)

enum class JourneyStep { Cast, Found, FoundBySomeone, Kept }

sealed interface KeepsakeEffect {
    /** A guest taps Keep: the sign-in sheet first. */
    data object NeedsSignIn : KeepsakeEffect

    data class KeepFinished(val result: KeepResult) : KeepsakeEffect

    data class PullFinished(val result: PullResult) : KeepsakeEffect

    /** Thrown back or reported: back to the Chest, which offers the Undo. */
    data object Left : KeepsakeEffect
}
