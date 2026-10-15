package com.bottlecast.app.feature.chest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.ChestPage
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.feature.chest.domain.BottleStanding
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Unit tests for [ChestViewModel]: the tabs, their filters, what each bottle stands as, and paging. */
class ChestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val clock = MutableClock(Instant.parse("2026-10-12T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 12)
    private val user = MutableStateFlow<UserEntity>(UserEntity.AuthenticatedUser(AuthProviderType.Google, "jwt", "finn", isAdmin = false))
    private val notKept = MutableStateFlow<List<FoundBottle>?>(emptyList())
    private val kept = MutableStateFlow<List<FoundBottle>>(emptyList())
    private val casts = MutableStateFlow<List<MessageEntity>>(emptyList())
    private val counts = MutableStateFlow<ChestCounts?>(ChestCounts(bottled = 12, found = 30, kept = 9))
    private val pending = MutableStateFlow<PendingSeaAction?>(null)
    private val keptLimits = mutableListOf<Int>()

    private val chestRepository = mockk<IChestRepository>(relaxed = true) {
        every { notKept } returns this@ChestViewModelTest.notKept
        every { kept(any()) } answers {
            val limit = firstArg<Int>()
            keptLimits += limit
            kept.map { ChestPage(it.take(limit), hasMore = it.size > limit) }
        }
        every { cast(any()) } answers { casts.map { ChestPage(it.take(firstArg()), hasMore = false) } }
        every { counts } returns this@ChestViewModelTest.counts
    }
    private val seaActions = mockk<ISeaActions>(relaxed = true) { every { pending } returns this@ChestViewModelTest.pending }
    private val auth = mockk<IAuthenticationRepository> { every { user } returns this@ChestViewModelTest.user }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns MutableStateFlow(true) }

    @Test
    fun `the Found tab lists the bottles not kept yet, then the kept ones, with their counts`() = runTest {
        // given
        notKept.value = listOf(found("held", daysAgo = 2))
        kept.value = listOf(found("kept", daysAgo = 1, kept = true))
        val viewModel = chest()

        // when
        runCurrent()

        // then
        val state = viewModel.state.value
        Assert.assertEquals(
            ChestList.Entries(
                listOf(
                    ChestEntry("held", "Message held", BottleStanding.NotKept(5), ChestMeta.Found(today.minusDays(2), "und")),
                    ChestEntry("kept", "Message kept", BottleStanding.Kept, ChestMeta.Found(today.minusDays(1), "und")),
                ),
                hasMore = false,
            ),
            state.list,
        )
        Assert.assertEquals(10, state.foundCount)
        Assert.assertEquals(12, state.castCount)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `an unkept bottle on its last days says so`() = runTest {
        // given
        notKept.value = listOf(found("tomorrow", daysAgo = 6), found("today", daysAgo = 7, plusHours = 2))
        val viewModel = chest()

        // when
        runCurrent()

        // then
        val entries = (viewModel.state.value.list as ChestList.Entries).entries
        Assert.assertEquals(listOf(ChestMeta.DriftsTomorrow, ChestMeta.DriftsToday), entries.map { it.meta })
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `filters narrow the tab, and an empty filter says so`() = runTest {
        // given
        kept.value = listOf(found("kept", daysAgo = 1, kept = true))
        val viewModel = chest()

        // when
        viewModel.onFoundFilter(FoundFilter.Kept)
        runCurrent()
        val keptOnly = (viewModel.state.value.list as ChestList.Entries).entries.map { it.messageId }
        viewModel.onFoundFilter(FoundFilter.NotKept)
        runCurrent()

        // then
        Assert.assertEquals(listOf("kept"), keptOnly)
        Assert.assertEquals(ChestList.NothingFiltered, viewModel.state.value.list)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `an empty tab is empty`() = runTest {
        // given
        val viewModel = chest()

        // when
        runCurrent()

        // then
        Assert.assertEquals(ChestList.Empty, viewModel.state.value.list)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `the Cast tab says how each bottle stands, never who holds it`() = runTest {
        // given
        val now = clock.instant()
        casts.value = listOf(
            cast("drifting"),
            cast("held", status = MessageStatus.Found, foundAt = now.minus(Duration.ofDays(1))),
            cast("ended", status = MessageStatus.Found, foundAt = now.minus(Duration.ofDays(8))),
            cast("kept", status = MessageStatus.Kept),
            cast("reported", reportCount = 3),
            cast("pulled", status = MessageStatus.UnderReview),
            cast("removed", status = MessageStatus.Removed),
        )
        val viewModel = chest()

        // when
        viewModel.onTab(ChestTab.Cast)
        runCurrent()
        val all = (viewModel.state.value.list as ChestList.Entries).entries
        viewModel.onCastFilter(CastFilter.UnderReview)
        runCurrent()
        val underReview = (viewModel.state.value.list as ChestList.Entries).entries.map { it.messageId }

        // then
        Assert.assertEquals(
            listOf(
                BottleStanding.Drifting,
                BottleStanding.Found,
                BottleStanding.Drifting,
                BottleStanding.Kept,
                BottleStanding.UnderReview,
                BottleStanding.UnderReview,
                BottleStanding.Removed,
            ),
            all.map { it.standing },
        )
        Assert.assertNull(all.last().excerpt)
        Assert.assertEquals(listOf("reported", "pulled", "removed"), underReview)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a guest's Cast tab asks them to sign in`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "guest", isAdmin = false)
        val viewModel = chest()

        // when
        viewModel.onTab(ChestTab.Cast)
        runCurrent()

        // then
        Assert.assertEquals(ChestList.SignInToTrack, viewModel.state.value.list)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a throw back waiting out its window is gone from the list, and its undo goes to the sea actions`() = runTest {
        // given
        notKept.value = listOf(found("held", daysAgo = 2))
        pending.value = PendingSeaAction.ThrowBack("action", "held")
        val viewModel = chest()

        // when
        runCurrent()
        viewModel.onUndo("action")

        // then
        Assert.assertEquals(ChestList.Empty, viewModel.state.value.list)
        Assert.assertEquals(0, viewModel.state.value.notKeptCount)
        Assert.assertEquals(pending.value, viewModel.state.value.pending)
        verify { seaActions.undo("action") }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a chest that can't be followed says so, and tries again`() = runTest {
        // given
        every { chestRepository.kept(any()) } throws IllegalStateException("broken")
        val viewModel = chest()
        runCurrent()
        val failed = viewModel.state.value.list

        // when
        every { chestRepository.kept(any()) } answers { kept.map { ChestPage(it, hasMore = false) } }
        viewModel.onRetry()
        runCurrent()

        // then
        Assert.assertEquals(ChestList.Failed, failed)
        Assert.assertEquals(ChestList.Empty, viewModel.state.value.list)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `the not-kept count stays true on the Cast tab`() = runTest {
        // given
        notKept.value = listOf(found("held", daysAgo = 2))
        val viewModel = chest()

        // when
        viewModel.onTab(ChestTab.Cast)
        runCurrent()

        // then
        Assert.assertEquals(1, viewModel.state.value.notKeptCount)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `near the end, the next page is read, and viewing marks the chest seen`() = runTest {
        // given
        kept.value = List(25) { found("kept$it", daysAgo = 1, kept = true) }
        val viewModel = chest()
        runCurrent()

        // when
        viewModel.onLoadMore()
        runCurrent()
        viewModel.onViewed()

        // then
        Assert.assertEquals(listOf(CHEST_PAGE_SIZE, CHEST_PAGE_SIZE * 2), keptLimits)
        Assert.assertEquals(25, (viewModel.state.value.list as ChestList.Entries).entries.size)
        verify { chestRepository.markViewed() }
        verify { chestRepository.refreshCounts() }
        viewModel.viewModelScope.cancel()
    }

    private fun chest(savedState: SavedStateHandle = SavedStateHandle()) =
        ChestViewModel(savedState, chestRepository, seaActions, auth, connectivity, clock)

    private fun found(id: String, daysAgo: Long, kept: Boolean = false, plusHours: Long = 0): FoundBottle {
        val foundAt = clock.instant().minus(Duration.ofDays(daysAgo)).plus(Duration.ofHours(plusHours))
        return FoundBottle(id, MessageEntity(id = id, message = "Message $id", userId = "writer"), foundAt, if (kept) foundAt else null)
    }

    private fun cast(id: String, status: MessageStatus = MessageStatus.Afloat, foundAt: Instant? = null, reportCount: Int = 0) =
        MessageEntity(id = id, message = "Message $id", userId = "finn", createdAt = clock.instant(), status = status, foundAt = foundAt, reportCount = reportCount)
}
