package com.bottlecast.app.feature.reveal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.chest.domain.ChestItem
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.sea.domain.SeaActionOutcome
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.common.streak.domain.Streak
import com.bottlecast.app.feature.reveal.domain.CastAgo
import com.bottlecast.app.feature.shore.domain.ShoreNotice
import com.bottlecast.app.feature.shore.domain.ShoreNotices
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

class RevealViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val clock = MutableClock(Instant.parse("2026-10-13T09:00:00Z"))
    private val bottle = MessageEntity(
        id = "today",
        message = "Hoy el mar me devolvió algo que creía perdido: la calma.",
        userId = "writer",
        createdAt = clock.instant().minus(3, ChronoUnit.DAYS),
    )
    private val outcomes = MutableSharedFlow<SeaActionOutcome>(extraBufferCapacity = 8)

    private val seaRepository = mockk<ISeaRepository>(relaxed = true) {
        every { cachedMessage("today") } returns bottle
    }
    private val seaActions = mockk<ISeaActions>(relaxed = true) {
        every { outcomes } returns this@RevealViewModelTest.outcomes
        every { report(any(), any(), any(), any()) } returns "report-action"
    }

    // the bottle as the chest has it: found, not kept, unless a test says otherwise
    private val item = MutableStateFlow<ChestItem?>(ChestItem.Found(FoundBottle("today", bottle, clock.instant(), keptAt = null)))
    private val chestRepository = mockk<IChestRepository> {
        every { item(any()) } returns this@RevealViewModelTest.item
        coEvery { keep("today") } returns KeepResult.Kept
    }
    private val user = MutableStateFlow<UserEntity>(UserEntity.AuthenticatedUser(AuthProviderType.Google, "jwt", "finn", isAdmin = false))
    private val authenticationRepository = mockk<IAuthenticationRepository> { every { user } returns this@RevealViewModelTest.user }
    private val online = MutableStateFlow(true)
    private val connectivityMonitor = mockk<IConnectivityMonitor> { every { isOnline } returns online }
    private val streak = MutableStateFlow<Streak?>(null)
    private val streakRepository = mockk<IStreakRepository> { every { streak } returns this@RevealViewModelTest.streak }
    private val shoreNotices = ShoreNotices()

    private val expectedBottle = RevealedBottle(
        id = "today",
        text = "Hoy el mar me devolvió algo que creía perdido: la calma.",
        byline = Byline.Stranger,
        castAgo = CastAgo.Days(3),
    )

    @Test
    fun `a bottle just found shows at once, by a stranger at sea, cast 3 days ago`() = runTest {
        // given
        val viewModel = reveal(fresh = true)

        // when
        val state = viewModel.state.value

        // then
        Assert.assertEquals(RevealUiState.Showing(expectedBottle, RevealReport.None, fresh = true), state)
    }

    @Test
    fun `a bottle signed with a pen name says so`() = runTest {
        // given
        every { seaRepository.cachedMessage("today") } returns bottle.copy(penName = "Driftwood")

        // when
        val state = reveal().state.value as RevealUiState.Showing

        // then
        Assert.assertEquals(Byline.PenName("Driftwood"), state.bottle.byline)
    }

    @Test
    fun `reopened from the Shore, a bottle is read from the sea, or fails if it's gone`() = runTest {
        // given
        every { seaRepository.cachedMessage(any()) } returns null
        coEvery { seaRepository.getMessage("today") } returns bottle
        coEvery { seaRepository.getMessage("gone") } returns null

        // when
        val found = reveal()
        val gone = reveal(messageId = "gone")
        runCurrent()

        // then
        Assert.assertEquals(RevealUiState.Showing(expectedBottle, RevealReport.None, fresh = false), found.state.value)
        Assert.assertEquals(RevealUiState.Failed, gone.state.value)
    }

    @Test
    fun `a report waits out its window, then thanks the reporter`() = runTest {
        // given
        val viewModel = reveal()
        runCurrent()

        // when
        viewModel.onReport(ReportReason.Spam, blockWriter = true, windowMillis = 5_000)
        val pending = (viewModel.state.value as RevealUiState.Showing).report
        outcomes.emit(SeaActionOutcome(PendingSeaAction.Report("report-action", "today", blockWriter = true), queuedOffline = true))
        runCurrent()

        // then
        verify { seaActions.report(bottle, ReportReason.Spam, true, 5_000) }
        Assert.assertEquals(RevealReport.Pending, pending)
        Assert.assertEquals(
            RevealReport.Reported(blocked = true, queuedOffline = true),
            (viewModel.state.value as RevealUiState.Showing).report,
        )
    }

    @Test
    fun `undo takes the report back before it's sent`() = runTest {
        // given
        val viewModel = reveal()
        viewModel.onReport(reason = null, blockWriter = false, windowMillis = 5_000)

        // when
        viewModel.onUndoReport()

        // then
        verify { seaActions.undo("report-action") }
        Assert.assertEquals(RevealReport.None, (viewModel.state.value as RevealUiState.Showing).report)
    }

    @Test
    fun `leaving sends a report that's still waiting`() = runTest {
        // given
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(store, factory { reveal() })[RevealViewModel::class]
        viewModel.onReport(reason = null, blockWriter = false, windowMillis = 5_000)

        // when
        store.clear()

        // then
        verify { seaActions.commit("report-action") }
    }

    @Test
    fun `throwing back hands the bottle to the sea actions`() = runTest {
        // given
        val viewModel = reveal()

        // when
        viewModel.onThrowBack(windowMillis = null)

        // then
        verify { seaActions.throwBack(bottle, null) }
    }

    @Test
    fun `keeping writes the keep, then shows the bottle kept`() = runTest {
        // given
        val viewModel = reveal()
        runCurrent()

        // when
        viewModel.onKeep()
        runCurrent()

        // then
        coVerify { chestRepository.keep("today") }
        Assert.assertEquals(RevealKeep(kept = true), (viewModel.state.value as RevealUiState.Showing).keep)
        Assert.assertEquals(RevealEffect.KeepFinished(KeepResult.Kept), viewModel.effects.first())
    }

    @Test
    fun `a guest signs in before keeping`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "guest", isAdmin = false)
        val viewModel = reveal()
        runCurrent()

        // when
        viewModel.onKeep()
        runCurrent()

        // then
        Assert.assertEquals(RevealEffect.NeedsSignIn, viewModel.effects.first())
        coVerify(exactly = 0) { chestRepository.keep(any()) }
    }

    @Test
    fun `straight after the sign-in sheet, the keep goes ahead before the signed-in user catches up`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "guest", isAdmin = false)
        val viewModel = reveal()
        runCurrent()

        // when
        viewModel.onKeep(signedIn = true)
        runCurrent()

        // then
        coVerify { chestRepository.keep("today") }
    }

    @Test
    fun `a keep that doesn't go through leaves the pill, and says why`() = runTest {
        // given
        coEvery { chestRepository.keep("today") } returns KeepResult.NotHeld
        val viewModel = reveal()
        runCurrent()

        // when
        viewModel.onKeep()
        runCurrent()

        // then
        Assert.assertEquals(RevealKeep(kept = false), (viewModel.state.value as RevealUiState.Showing).keep)
        Assert.assertEquals(RevealEffect.KeepFinished(KeepResult.NotHeld), viewModel.effects.first())
    }

    @Test
    fun `a bottle already kept shows kept, and offline the pill waits`() = runTest {
        // given
        item.value = ChestItem.Found(FoundBottle("today", bottle, clock.instant(), keptAt = clock.instant()))
        online.value = false

        // when
        val viewModel = reveal()
        runCurrent()

        // then
        Assert.assertEquals(RevealKeep(kept = true, online = false), (viewModel.state.value as RevealUiState.Showing).keep)
    }

    @Test
    fun `the overline counts the streak's day once today's find is in it`() = runTest {
        // given
        streak.value = Streak(current = 6, best = 14, shields = 1, foundToday = true)

        // when
        val viewModel = reveal(fresh = true)
        runCurrent()

        // then
        Assert.assertEquals(6, (viewModel.state.value as RevealUiState.Showing).day)
    }

    @Test
    fun `leaving a fresh find unkept tells the Shore it waits in the chest`() = runTest {
        // given
        val store = ViewModelStore()
        ViewModelProvider.create(store, factory { reveal(fresh = true) })[RevealViewModel::class]
        runCurrent()

        // when
        store.clear()

        // then
        Assert.assertEquals(ShoreNotice.WaitsInChest, shoreNotices.pending.value)
    }

    @Test
    fun `leaving after keeping or throwing back, or a bottle reopened, tells the Shore nothing`() = runTest {
        // given
        val kept = ViewModelStore()
        ViewModelProvider.create(kept, factory { reveal(fresh = true) })[RevealViewModel::class].onKeep()
        val thrown = ViewModelStore()
        ViewModelProvider.create(thrown, factory { reveal(fresh = true) })[RevealViewModel::class].onThrowBack(null)
        val reopened = ViewModelStore()
        ViewModelProvider.create(reopened, factory { reveal(fresh = false) })[RevealViewModel::class]
        runCurrent()

        // when
        kept.clear()
        thrown.clear()
        reopened.clear()

        // then
        Assert.assertNull(shoreNotices.pending.value)
    }

    private fun reveal(messageId: String = "today", fresh: Boolean = false) = RevealViewModel(
        SavedStateHandle(mapOf("messageId" to messageId, "fresh" to fresh)),
        seaRepository,
        seaActions,
        chestRepository,
        authenticationRepository,
        connectivityMonitor,
        streakRepository,
        shoreNotices,
        clock,
    )

    private fun factory(create: () -> RevealViewModel) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = create() as T
    }
}
