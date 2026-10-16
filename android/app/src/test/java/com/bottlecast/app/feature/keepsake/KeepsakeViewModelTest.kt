package com.bottlecast.app.feature.keepsake

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.chest.domain.ChestItem
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.feature.chest.domain.BottleStanding
import com.bottlecast.app.feature.reveal.Byline
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Unit tests for [KeepsakeViewModel]: a found bottle's or a cast's detail, and keeping, throwing back, reporting or pulling it. */
class KeepsakeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val clock = MutableClock(Instant.parse("2026-10-12T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 12)
    private val heldSince = clock.instant().minus(Duration.ofDays(2))
    private val bottle = MessageEntity(
        id = "bottle",
        message = "Somewhere a lighthouse is blinking just for you.",
        userId = "writer",
        createdAt = clock.instant().minus(Duration.ofDays(4)),
        status = MessageStatus.Found,
        foundAt = heldSince,
    )
    private val item = MutableStateFlow<ChestItem?>(ChestItem.Found(FoundBottle("bottle", bottle, heldSince, keptAt = null)))
    private val user = MutableStateFlow<UserEntity>(UserEntity.AuthenticatedUser(AuthProviderType.Google, "jwt", "finn", isAdmin = false))

    private val chestRepository = mockk<IChestRepository> {
        every { item("bottle") } returns this@KeepsakeViewModelTest.item
        coEvery { keep("bottle") } returns KeepResult.Kept
        coEvery { pull("bottle") } returns PullResult.Pulled
    }
    private val seaActions = mockk<ISeaActions>(relaxed = true)
    private val auth = mockk<IAuthenticationRepository> { every { user } returns this@KeepsakeViewModelTest.user }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns MutableStateFlow(true) }

    @Test
    fun `an unkept find can be kept, thrown back or reported, and has its journey so far`() = runTest {
        // given
        val viewModel = keepsake()

        // when
        runCurrent()

        // then
        val detail = (viewModel.state.value as KeepsakeUiState.Showing).detail
        Assert.assertEquals(BottleStanding.NotKept(5), detail.standing)
        Assert.assertEquals(KeepsakeKeep(daysLeft = 5, online = true, keeping = false), detail.keep)
        Assert.assertEquals(setOf(KeepsakeAction.ThrowBack, KeepsakeAction.Report), detail.actions)
        Assert.assertEquals(
            listOf(
                JourneyStop(JourneyStep.Cast, today.minusDays(4)),
                JourneyStop(JourneyStep.Found, today.minusDays(2)),
                JourneyStop(JourneyStep.Kept, null),
            ),
            detail.journey,
        )
        Assert.assertEquals(Byline.Stranger, detail.byline)
        Assert.assertTrue(detail.shareable)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a kept find has nothing more to do until sealing`() = runTest {
        // given
        item.value = ChestItem.Found(FoundBottle("bottle", bottle.copy(status = MessageStatus.Kept), heldSince, keptAt = clock.instant()))
        val viewModel = keepsake()

        // when
        runCurrent()

        // then
        val detail = (viewModel.state.value as KeepsakeUiState.Showing).detail
        Assert.assertEquals(BottleStanding.Kept, detail.standing)
        Assert.assertNull(detail.keep)
        Assert.assertEquals(emptySet<KeepsakeAction>(), detail.actions)
        Assert.assertEquals(JourneyStop(JourneyStep.Kept, today), detail.journey.last())
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a removed kept bottle hides its words, and can't be shared`() = runTest {
        // given
        item.value = ChestItem.Found(FoundBottle("bottle", message = null, foundAt = heldSince, keptAt = clock.instant()))
        val viewModel = keepsake()

        // when
        runCurrent()

        // then
        val detail = (viewModel.state.value as KeepsakeUiState.Showing).detail
        Assert.assertNull(detail.text)
        Assert.assertEquals(KeepsakeNotice.Removed, detail.notice)
        Assert.assertFalse(detail.shareable)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a drifting cast can be pulled, and a found one says only that someone found it`() = runTest {
        // given
        val mine = bottle.copy(userId = "finn", status = MessageStatus.Afloat, foundAt = null)
        item.value = ChestItem.Cast(mine)
        val viewModel = keepsake()

        // when
        runCurrent()
        val drifting = (viewModel.state.value as KeepsakeUiState.Showing).detail
        item.value = ChestItem.Cast(mine.copy(status = MessageStatus.Found, foundAt = heldSince))
        runCurrent()
        val found = (viewModel.state.value as KeepsakeUiState.Showing).detail

        // then
        Assert.assertEquals(setOf(KeepsakeAction.Pull), drifting.actions)
        Assert.assertEquals(JourneyStop(JourneyStep.FoundBySomeone, null), drifting.journey[1])
        Assert.assertEquals(emptySet<KeepsakeAction>(), found.actions)
        Assert.assertEquals(JourneyStop(JourneyStep.FoundBySomeone, today.minusDays(2)), found.journey[1])
        Assert.assertTrue(found.ownCast)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `keeping goes through the chest, and a guest signs in first`() = runTest {
        // given
        val viewModel = keepsake()
        runCurrent()

        // when
        viewModel.onKeep()
        runCurrent()
        val kept = viewModel.effects.first()
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "guest", isAdmin = false)
        viewModel.onKeep()
        val guest = viewModel.effects.first()

        // then
        Assert.assertEquals(KeepsakeEffect.KeepFinished(KeepResult.Kept), kept)
        Assert.assertEquals(KeepsakeEffect.NeedsSignIn, guest)
        coVerify(exactly = 1) { chestRepository.keep("bottle") }
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `throwing back or reporting hands the bottle to the sea actions, and leaves`() = runTest {
        // given
        val viewModel = keepsake()
        runCurrent()

        // when
        viewModel.onThrowBack(windowMillis = 5_000)
        val afterThrowBack = viewModel.effects.first()
        viewModel.onReport(ReportReason.Spam, blockWriter = true, windowMillis = 5_000)
        val afterReport = viewModel.effects.first()

        // then
        verify { seaActions.throwBack(bottle, 5_000) }
        verify { seaActions.report(bottle, ReportReason.Spam, true, 5_000) }
        Assert.assertEquals(KeepsakeEffect.Left, afterThrowBack)
        Assert.assertEquals(KeepsakeEffect.Left, afterReport)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `pulling says how it went`() = runTest {
        // given
        coEvery { chestRepository.pull("bottle") } returns PullResult.NotDrifting
        val viewModel = keepsake()
        runCurrent()

        // when
        viewModel.onPull()
        runCurrent()

        // then
        Assert.assertEquals(KeepsakeEffect.PullFinished(PullResult.NotDrifting), viewModel.effects.first())
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `a bottle that's left the chest is gone`() = runTest {
        // given
        item.value = null
        val viewModel = keepsake()

        // when
        runCurrent()

        // then
        Assert.assertEquals(KeepsakeUiState.Gone, viewModel.state.value)
        viewModel.viewModelScope.cancel()
    }

    private fun keepsake() = KeepsakeViewModel(
        SavedStateHandle(mapOf("messageId" to "bottle")),
        chestRepository,
        seaActions,
        auth,
        connectivity,
        clock,
    )
}
