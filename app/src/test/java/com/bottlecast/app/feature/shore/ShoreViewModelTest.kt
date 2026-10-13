package com.bottlecast.app.feature.shore

import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.TodayFind
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class ShoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val user = MutableStateFlow<UserEntity>(UserEntity.UnauthenticatedUser)
    private val isOnline = MutableStateFlow(true)
    private val candidate = MutableStateFlow<CandidateState>(CandidateState.Unknown)
    private val pending = MutableStateFlow<PendingSeaAction?>(null)

    // today's find for each date; none unless a test adds one
    private val finds = MutableStateFlow<Map<LocalDate, TodayFind>>(emptyMap())

    // 16:48 on the Shore-B board's day, so the next tide is 7h 12m away
    private val clock = MutableClock(Instant.parse("2026-10-13T16:48:00Z"))
    private val today = LocalDate.of(2026, 10, 13)

    private val authenticationRepository = mockk<IAuthenticationRepository> { every { user } returns this@ShoreViewModelTest.user }
    private val connectivityMonitor = mockk<IConnectivityMonitor> { every { isOnline } returns this@ShoreViewModelTest.isOnline }
    private val seaRepository = mockk<ISeaRepository>(relaxed = true) {
        every { candidate } returns this@ShoreViewModelTest.candidate
        every { observeToday(any()) } answers { todayFor(firstArg()) }
        coEvery { prefetchCandidate() } returns CandidateState.Unknown
    }
    private val seaActions = mockk<ISeaActions>(relaxed = true) { every { pending } returns this@ShoreViewModelTest.pending }

    @Test
    fun `today's bottle waits, with no streak or stats until they're recorded`() = runTest {
        // given
        val viewModel = shore()

        // when
        runCurrent()

        // then
        Assert.assertEquals(ShoreUiState.BottleWaiting(isAdmin = false, isOffline = false, streak = null, stats = null), viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `while today's bottle waits, the Shore looks at the sea ahead of the tap`() = runTest {
        // given
        val viewModel = shore()

        // when
        runCurrent()

        // then
        coVerify(atLeast = 1) { seaRepository.prefetchCandidate() }
        close(viewModel)
    }

    @Test
    fun `offline, the Shore doesn't look at the sea`() = runTest {
        // given
        isOnline.value = false
        val viewModel = shore()

        // when
        runCurrent()

        // then
        coVerify(exactly = 0) { seaRepository.prefetchCandidate() }
        Assert.assertTrue(viewModel.state.value.isOffline)
        close(viewModel)
    }

    @Test
    fun `a calm sea offers casting instead`() = runTest {
        // given
        val viewModel = shore()

        // when
        candidate.value = CandidateState.SeaCalm
        runCurrent()

        // then
        Assert.assertEquals(ShoreUiState.BottleWaiting(seaCalm = true), viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `once today's bottle is found, the next tide counts down to midnight`() = runTest {
        // given
        finds.value = mapOf(today to found("today"))
        val viewModel = shore()

        // when
        runCurrent()

        // then
        val state = viewModel.state.value as ShoreUiState.FoundToday
        Assert.assertEquals(Duration.ofHours(7).plusMinutes(12), state.tide.untilNextTide)
        Assert.assertEquals(TodaysFind("today", "Message today"), state.todaysFind)
        coVerify(exactly = 0) { seaRepository.prefetchCandidate() }
        close(viewModel)
    }

    @Test
    fun `the countdown moves on with every minute`() = runTest {
        // given
        finds.value = mapOf(today to found("today"))
        val viewModel = shore()
        runCurrent()

        // when
        passMinutes(1)

        // then
        Assert.assertEquals(Duration.ofHours(7).plusMinutes(11), (viewModel.state.value as ShoreUiState.FoundToday).tide.untilNextTide)
        close(viewModel)
    }

    @Test
    fun `at midnight the tide turns, and a new bottle waits`() = runTest {
        // given
        finds.value = mapOf(today to found("today"))
        val viewModel = shore()
        runCurrent()

        // when
        passMinutes(7 * 60 + 12)

        // then
        Assert.assertEquals(ShoreUiState.BottleWaiting(), viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `a throw back waiting out its window hides the card and offers its undo`() = runTest {
        // given
        finds.value = mapOf(today to found("today"))
        val viewModel = shore()
        val throwBack = PendingSeaAction.ThrowBack(id = "action", messageId = "today")

        // when
        pending.value = throwBack
        runCurrent()

        // then
        val state = viewModel.state.value as ShoreUiState.FoundToday
        Assert.assertNull(state.todaysFind)
        Assert.assertEquals(throwBack, state.pendingThrowBack)
        close(viewModel)
    }

    @Test
    fun `a find that's been thrown back or reported leaves no card`() = runTest {
        // given
        val viewModel = shore()

        // when
        finds.value = mapOf(today to found("today", FindStatus.Reported))
        runCurrent()
        val reported = (viewModel.state.value as ShoreUiState.FoundToday).todaysFind
        finds.value = mapOf(today to found("today", FindStatus.ThrownBack))
        runCurrent()
        val thrownBack = (viewModel.state.value as ShoreUiState.FoundToday).todaysFind

        // then
        Assert.assertNull(reported)
        Assert.assertNull(thrownBack)
        close(viewModel)
    }

    @Test
    fun `undo and a swiped-away snackbar go to the sea actions`() = runTest {
        // given
        val viewModel = shore()

        // when
        viewModel.onUndoThrowBack("undone")
        viewModel.onCommitThrowBack("committed")

        // then
        verify { seaActions.undo("undone") }
        verify { seaActions.commit("committed") }
        close(viewModel)
    }

    @Test
    fun `an admin is offered moderation`() = runTest {
        // given
        val viewModel = shore()

        // when
        user.value = UserEntity.AuthenticatedUser(
            providerType = AuthProviderType.Google,
            jwtToken = "token",
            userId = "admin",
            isAdmin = true,
        )
        runCurrent()

        // then
        Assert.assertTrue(viewModel.state.value.isAdmin)
        close(viewModel)
    }

    @Test
    fun `a signed-in user who isn't an admin isn't offered moderation`() = runTest {
        // given
        val viewModel = shore()

        // when
        user.value = UserEntity.AuthenticatedUser(
            providerType = AuthProviderType.Anonymous,
            jwtToken = "token",
            userId = "guest",
            isAdmin = false,
        )
        runCurrent()

        // then
        Assert.assertFalse(viewModel.state.value.isAdmin)
        close(viewModel)
    }

    @Test
    fun `losing the connection shows the offline state, and getting it back clears it`() = runTest {
        // given
        val viewModel = shore()
        runCurrent()

        // when
        isOnline.value = false
        runCurrent()
        val offline = viewModel.state.value.isOffline
        isOnline.value = true
        runCurrent()
        val online = viewModel.state.value.isOffline

        // then
        Assert.assertTrue(offline)
        Assert.assertFalse(online)
        close(viewModel)
    }

    @Test
    fun `a failure still offers today's bottle`() = runTest {
        // given
        every { connectivityMonitor.isOnline } throws IllegalStateException("no network service")

        // when
        val viewModel = shore()
        runCurrent()

        // then
        Assert.assertEquals(ShoreUiState.BottleWaiting(), viewModel.state.value)
        close(viewModel)
    }

    private fun shore() = ShoreViewModel(authenticationRepository, connectivityMonitor, seaRepository, seaActions, clock)

    private fun todayFor(date: LocalDate): Flow<TodayFind?> = finds.map { it[date] }

    private fun found(id: String, status: FindStatus = FindStatus.Held) =
        TodayFind(today, id, MessageEntity(id = id, message = "Message $id", userId = "writer"), status)

    // moves the wall clock and the coroutines' virtual time on together, a minute at a time
    private fun TestScope.passMinutes(minutes: Int) {
        repeat(minutes) {
            clock.advance(Duration.ofMinutes(1))
            advanceTimeBy(Duration.ofMinutes(1).toMillis())
            runCurrent()
        }
    }

    // the Shore's minute ticker runs for as long as the view model does
    private fun close(viewModel: ShoreViewModel) = viewModel.viewModelScope.cancel()
}
