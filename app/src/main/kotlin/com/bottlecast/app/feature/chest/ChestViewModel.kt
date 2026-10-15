package com.bottlecast.app.feature.chest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.ChestPage
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.LANG_UNDETERMINED
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.time.localDates
import com.bottlecast.app.feature.chest.domain.BottleStanding
import com.bottlecast.app.feature.chest.domain.castStanding
import com.bottlecast.app.feature.chest.domain.standing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/** How many bottles a page of the chest holds; scrolling near the end reads another. */
internal const val CHEST_PAGE_SIZE = 20

/**
 * The Chest (spec §3.10): the Found tab, with the bottles not kept yet and then the kept ones, and the
 * Cast tab, with the bottles this account cast. Each tab has its filters; the tab and filters live in
 * the saved state, so the Shore's [See them] can pick the Cast tab.
 */
@HiltViewModel
class ChestViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chestRepository: IChestRepository,
    private val seaActions: ISeaActions,
    authenticationRepository: IAuthenticationRepository,
    connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
) : ViewModel() {

    private val _state: MutableStateFlow<ChestUiState> = MutableStateFlow(ChestUiState(today = LocalDate.now(clock)))
    val state: StateFlow<ChestUiState> = _state.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "Couldn't follow the chest")
        _state.update { it.copy(list = ChestList.Failed) }
    }

    private val tab = savedStateHandle.getStateFlow(KEY_TAB, ChestTab.Found.name).map { ChestTab.valueOf(it) }
    private val foundFilter = savedStateHandle.getStateFlow(KEY_FOUND_FILTER, FoundFilter.All.name).map { FoundFilter.valueOf(it) }
    private val castFilter = savedStateHandle.getStateFlow(KEY_CAST_FILTER, CastFilter.All.name).map { CastFilter.valueOf(it) }
    private val keptLimit = MutableStateFlow(CHEST_PAGE_SIZE)
    private val castLimit = MutableStateFlow(CHEST_PAGE_SIZE)

    private data class Choice(val tab: ChestTab, val foundFilter: FoundFilter, val castFilter: CastFilter)

    private data class Account(val isGuest: Boolean, val online: Boolean, val today: LocalDate)

    // the account's numbers, and what's waiting out its undo window
    private data class Extras(val counts: ChestCounts?, val pending: PendingSeaAction?, val notKept: List<FoundBottle>?)

    private val account = combine(authenticationRepository.user, connectivityMonitor.isOnline, clock.localDates()) { user, online, today ->
        Account(isGuest = user.googleUid == null, online = online, today = today)
    }

    private var following: Job? = null

    init {
        chestRepository.refreshCounts()
        follow()
    }

    /** Follows the chest again, after it failed. */
    fun onRetry() {
        _state.update { it.copy(list = ChestList.Loading) }
        follow()
    }

    private fun follow() {
        following?.cancel()
        val choice = combine(tab, foundFilter, castFilter) { tab, found, cast -> Choice(tab, found, cast) }
        val extras = combine(chestRepository.counts, seaActions.pending, chestRepository.notKept) { counts, pending, notKept ->
            Extras(counts, pending, notKept)
        }
        following = viewModelScope.launch(exceptionHandler) {
            combine(choice, account, list(), extras) { choice, account, list, extras -> stateFor(choice, account, list, extras) }
                .collect { _state.value = it }
        }
    }

    fun onTab(tab: ChestTab) {
        savedStateHandle[KEY_TAB] = tab.name
    }

    fun onFoundFilter(filter: FoundFilter) {
        savedStateHandle[KEY_FOUND_FILTER] = filter.name
    }

    fun onCastFilter(filter: CastFilter) {
        savedStateHandle[KEY_CAST_FILTER] = filter.name
    }

    /** The list is near its end: read the next page of the current tab, if there is one. */
    fun onLoadMore() {
        val list = _state.value.list as? ChestList.Entries ?: return
        if (!list.hasMore) return
        when (_state.value.tab) {
            ChestTab.Found -> keptLimit.value = list.pageLimit + CHEST_PAGE_SIZE
            ChestTab.Cast -> castLimit.value = list.pageLimit + CHEST_PAGE_SIZE
        }
    }

    /** The Chest is on screen, so its news is seen and its dot clears (spec §3.10). */
    fun onViewed() = chestRepository.markViewed()

    /** Takes back a throw back or report from Keepsake detail, still waiting out its window. */
    fun onUndo(actionId: String) = seaActions.undo(actionId)

    /** Writes a throw back or report at once: its snackbar was swiped away. */
    fun onCommit(actionId: String) = seaActions.commit(actionId)

    // What the current tab holds, read only while it's the current one.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun list(): Flow<Listing> = tab.distinctUntilChanged().flatMapLatest { tab ->
        val source: Flow<Listing?> = when (tab) {
            ChestTab.Found -> keptLimit.flatMapLatest { limit ->
                combine(chestRepository.notKept, chestRepository.kept(limit)) { notKept, kept ->
                    notKept?.let { Listing.Found(it, kept, limit) }
                }
            }
            ChestTab.Cast -> castLimit.flatMapLatest { limit -> chestRepository.cast(limit).map { Listing.Cast(it, limit) } }
        }
        // a new tab shows its skeleton until its first page is in, while more pages just add on
        source.onStart { emit(null) }.map { it ?: Listing.Loading }
    }

    private fun stateFor(choice: Choice, account: Account, listing: Listing, extras: Extras): ChestUiState {
        val now = clock.instant()
        val zone = clock.zone
        val pending = extras.pending
        val list = when {
            choice.tab == ChestTab.Cast && account.isGuest -> ChestList.SignInToTrack
            listing is Listing.Loading -> ChestList.Loading
            listing is Listing.Found -> {
                // a throw back or report waiting out its window is already gone from here
                val notKeptEntries = listing.notKept.filter { it.messageId != pending?.messageId }.map { it.toEntry(now, zone, account.today) }
                val keptEntries = listing.kept.items.map { it.toEntry(now, zone, account.today) }
                val all = notKeptEntries + keptEntries
                val shown = when (choice.foundFilter) {
                    FoundFilter.All -> all
                    FoundFilter.NotKept -> notKeptEntries
                    FoundFilter.Kept -> keptEntries
                }
                listFor(all, shown, hasMore = choice.foundFilter != FoundFilter.NotKept && listing.kept.hasMore, listing.limit)
            }
            listing is Listing.Cast -> {
                val all = listing.page.items.map { it.toCastEntry(now, zone) }
                val shown = all.filter { choice.castFilter.admits(it.standing) }
                listFor(all, shown, listing.page.hasMore, listing.limit)
            }
            else -> ChestList.Loading
        }
        return ChestUiState(
            tab = choice.tab,
            foundFilter = choice.foundFilter,
            castFilter = choice.castFilter,
            today = account.today,
            isOffline = !account.online,
            isGuest = account.isGuest,
            keptCount = extras.counts?.kept,
            // a throw back or report waiting out its window is already gone from the count too
            notKeptCount = extras.notKept?.count { it.messageId != pending?.messageId },
            castCount = extras.counts?.bottled,
            list = list,
            pending = pending,
        )
    }

    private fun listFor(all: List<ChestEntry>, shown: List<ChestEntry>, hasMore: Boolean, limit: Int): ChestList = when {
        all.isEmpty() && !hasMore -> ChestList.Empty
        shown.isEmpty() && !hasMore -> ChestList.NothingFiltered
        else -> ChestList.Entries(shown, hasMore, limit)
    }

    private fun FoundBottle.toEntry(now: Instant, zone: ZoneId, today: LocalDate): ChestEntry {
        val standing = standing(now)
        val driftsOn = driftsBackAt?.atZone(zone)?.toLocalDate()
        val meta = when {
            driftsOn == today -> ChestMeta.DriftsToday
            driftsOn == today.plusDays(1) -> ChestMeta.DriftsTomorrow
            else -> ChestMeta.Found(foundAt?.atZone(zone)?.toLocalDate(), message?.lang ?: LANG_UNDETERMINED)
        }
        return ChestEntry(
            messageId = messageId,
            excerpt = message?.message?.takeIf { standing != BottleStanding.Removed },
            standing = standing,
            meta = meta,
        )
    }

    private fun MessageEntity.toCastEntry(now: Instant, zone: ZoneId): ChestEntry {
        val standing = castStanding(now)
        return ChestEntry(
            messageId = id,
            excerpt = message.takeIf { standing != BottleStanding.Removed },
            standing = standing,
            meta = ChestMeta.Cast(createdAt?.let { ZonedDateTime.ofInstant(it, zone).toLocalDate() }),
        )
    }

    // What the current tab's sources hold.
    private sealed interface Listing {
        data object Loading : Listing

        data class Found(val notKept: List<FoundBottle>, val kept: ChestPage<FoundBottle>, val limit: Int) : Listing

        data class Cast(val page: ChestPage<MessageEntity>, val limit: Int) : Listing
    }

    internal companion object {
        // in the saved state, so the Shore can choose the tab before the Chest opens
        const val KEY_TAB = "tab"
        const val KEY_FOUND_FILTER = "foundFilter"
        const val KEY_CAST_FILTER = "castFilter"
    }
}

enum class ChestTab { Found, Cast }

/** The Found tab's filters (spec §3.10). Sealed waits for sealing (slice 6). */
enum class FoundFilter { All, NotKept, Kept }

/** The Cast tab's filters (spec §3.10). Sealed and Gifts wait for slices 6 and 7. */
enum class CastFilter {
    All, Drifting, Found, Kept, UnderReview;

    fun admits(standing: BottleStanding): Boolean = when (this) {
        All -> true
        Drifting -> standing == BottleStanding.Drifting
        Found -> standing == BottleStanding.Found
        Kept -> standing == BottleStanding.Kept
        UnderReview -> standing == BottleStanding.UnderReview || standing == BottleStanding.Removed
    }
}

data class ChestUiState(
    val today: LocalDate,
    val tab: ChestTab = ChestTab.Found,
    val foundFilter: FoundFilter = FoundFilter.All,
    val castFilter: CastFilter = CastFilter.All,
    val isOffline: Boolean = false,
    val isGuest: Boolean = false,
    /** Null until the server has counted them. */
    val keptCount: Int? = null,
    val notKeptCount: Int? = null,
    val castCount: Int? = null,
    val list: ChestList = ChestList.Loading,
    /** A throw back or report from Keepsake detail, waiting out its window: the snackbar's Undo. */
    val pending: PendingSeaAction? = null,
) {
    /** "Found (14)": the bottles not kept yet and the kept ones, once both are known. */
    val foundCount: Int? get() = if (keptCount != null && notKeptCount != null) keptCount + notKeptCount else null
}

sealed interface ChestList {
    data object Loading : ChestList

    /** [pageLimit] is how many the current page asked for, so the next page asks for more. */
    data class Entries(val entries: List<ChestEntry>, val hasMore: Boolean, val pageLimit: Int = CHEST_PAGE_SIZE) : ChestList

    /** Nothing in the tab at all. */
    data object Empty : ChestList

    /** Nothing under the chosen filter: "Nothing here yet". */
    data object NothingFiltered : ChestList

    /** A guest's Cast tab: only a Google account casts (spec §3.10). */
    data object SignInToTrack : ChestList

    /** The chest couldn't be followed: "Something went adrift" (spec §3.18), with Try again. */
    data object Failed : ChestList
}

/** A bottle in the chest's list (spec §3.10). */
data class ChestEntry(
    val messageId: String,
    /** The words, or null when they're hidden: a bottle removed for breaking the guidelines. */
    val excerpt: String?,
    val standing: BottleStanding,
    val meta: ChestMeta,
)

/** A list item's meta line (spec §3.10). */
sealed interface ChestMeta {
    /** "Found 5 Oct · Spanish", or "Found today". A null [date] isn't known any more. */
    data class Found(val date: LocalDate?, val lang: String) : ChestMeta

    /** "Cast 3 Oct", or "Cast today"; a cast still on its way has no date yet. */
    data class Cast(val date: LocalDate?) : ChestMeta

    /** An unkept bottle on its last days: "Drifts away tomorrow — keep it to hold on to it." */
    data object DriftsTomorrow : ChestMeta

    data object DriftsToday : ChestMeta
}
