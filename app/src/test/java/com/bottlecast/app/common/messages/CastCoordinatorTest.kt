package com.bottlecast.app.common.messages

import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.data.CAST_ACK_WINDOW_MILLIS
import com.bottlecast.app.common.messages.data.CastAck
import com.bottlecast.app.common.messages.data.CastCoordinator
import com.bottlecast.app.common.messages.data.CastTally
import com.bottlecast.app.common.messages.data.ICastDataSource
import com.bottlecast.app.common.messages.data.PendingCast
import com.bottlecast.app.common.messages.data.TallyWrite
import com.bottlecast.app.common.messages.domain.CastOutcome
import com.bottlecast.app.common.messages.domain.CastStatus
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.NewCast
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/** Unit tests for [CastCoordinator]: the tally, the server's answer and what it does to the draft. */
class CastCoordinatorTest {

    private val clock = MutableClock(Instant.parse("2026-10-14T09:00:00Z"))
    private val today = LocalDate.parse("2026-10-14")
    private val user = MutableStateFlow<UserEntity>(google("gail"))
    private val online = MutableStateFlow(true)

    private val source = FakeCastDataSource()
    private val drafts = mockk<IDraftRepository>(relaxed = true)
    private val auth = mockk<IAuthenticationRepository> { every { this@mockk.user } returns this@CastCoordinatorTest.user }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns online }
    private val tracker = mockk<ITracker>(relaxed = true)

    private val cast = NewCast("bottle00000000000001", "Leave the porch light on", "en", penName = null, sealable = true, acceptTerms = true)

    @Test
    fun `a guest can't cast`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "token", "guest", isAdmin = false)

        // when
        val outcome = coordinator().cast(cast)

        // then
        Assert.assertEquals(CastOutcome.NotSignedIn, outcome)
        Assert.assertTrue(source.writes.isEmpty())
    }

    @Test
    fun `the sixth cast of the day writes nothing`() = runTest {
        // given
        source.tally = CastTally(today, 5)

        // when
        val outcome = coordinator().cast(cast)

        // then
        Assert.assertEquals(CastOutcome.LimitReached, outcome)
        Assert.assertTrue(source.writes.isEmpty())
        coVerify(exactly = 0) { drafts.setAside(any()) }
    }

    @Test
    fun `the day's first cast starts the tally, and later ones count on`() = runTest {
        // given
        val coordinator = coordinator()
        source.tally = CastTally(today.minusDays(1), 5)
        source.answerAtOnce(CastAck.Confirmed)

        // when
        coordinator.cast(cast)
        source.tally = CastTally(today, 2)
        coordinator.cast(cast.copy(messageId = "bottle00000000000002"))

        // then
        Assert.assertEquals(listOf(TallyWrite.Start(today), TallyWrite.Increment(today)), source.writes.map { it.second })
    }

    @Test
    fun `a tally that can't be read counts as the day's first`() = runTest {
        // given
        source.failRead = true
        source.answerAtOnce(CastAck.Confirmed)

        // when
        coordinator().cast(cast)

        // then
        Assert.assertEquals(TallyWrite.Start(today), source.writes.single().second)
    }

    @Test
    fun `online, the server's yes is a cast, and the draft's words are forgotten`() = runTest {
        // given
        source.answerAtOnce(CastAck.Confirmed)

        // when
        val outcome = coordinator().cast(cast)
        runCurrent()

        // then
        Assert.assertEquals(CastOutcome.Cast(queued = false), outcome)
        coVerifyOrder {
            drafts.setAside(cast.messageId)
            drafts.forget(cast.messageId)
        }
        verify { tracker.trackEvent(AnalyticsEvent.BottleCast, match { it[AnalyticsEvent.PARAM_QUEUED] == "false" }) }
    }

    @Test
    fun `online, a refusal brings the words back before Preview hears of it`() = runTest {
        // given
        source.answerAtOnce(CastAck.Refused)

        // when
        val outcome = coordinator().cast(cast)

        // then
        Assert.assertEquals(CastOutcome.Refused, outcome)
        coVerify { drafts.restore(cast.messageId) }
        verify { tracker.trackEvent(AnalyticsEvent.CastRefused) }
    }

    @Test
    fun `offline, it's queued at once, and confirmed when the server has it`() = runTest {
        // given
        online.value = false
        val answer = CompletableDeferred<CastAck>()
        source.answer = answer

        // when
        val outcome = coordinator().cast(cast)
        answer.complete(CastAck.Confirmed)
        runCurrent()

        // then
        Assert.assertEquals(CastOutcome.Cast(queued = true), outcome)
        coVerify { drafts.forget(cast.messageId) }
        verify { tracker.trackEvent(AnalyticsEvent.BottleCast, match { it[AnalyticsEvent.PARAM_QUEUED] == "true" }) }
    }

    @Test
    fun `a slow server shows the cast as queued, and a late refusal still brings the words back`() = runTest {
        // given
        val answer = CompletableDeferred<CastAck>()
        source.answer = answer
        val coordinator = coordinator()

        // when
        val outcome = async { coordinator.cast(cast) }
        advanceTimeBy(CAST_ACK_WINDOW_MILLIS + 1)
        val shown = outcome.await()
        answer.complete(CastAck.Refused)
        runCurrent()

        // then
        Assert.assertEquals(CastOutcome.Cast(queued = true), shown)
        coVerify { drafts.restore(cast.messageId) }
    }

    @Test
    fun `casts left count down from 5 for today only`() = runTest {
        // given
        val coordinator = coordinator()
        val collected = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { coordinator.castsLeftToday.collect { collected += it } }
        runCurrent()

        // when
        source.tallyFlow.value = CastTally(today, 4)
        runCurrent()
        source.tallyFlow.value = CastTally(today.minusDays(1), 5)
        runCurrent()

        // then
        Assert.assertEquals(listOf(5, 1, 5), collected.filterNotNull())
    }

    private fun TestScope.coordinator() = CastCoordinator(
        source, drafts, auth, connectivity, clock, Random(1), tracker, backgroundScope, StandardTestDispatcher(testScheduler),
    )

    private fun google(uid: String) = UserEntity.AuthenticatedUser(AuthProviderType.Google, "token", uid, isAdmin = false)

    private class FakeCastDataSource : ICastDataSource {
        var tally: CastTally? = null
        var failRead = false
        val tallyFlow = MutableStateFlow<CastTally?>(null)
        val writes = mutableListOf<Pair<NewCast, TallyWrite>>()
        var answer = CompletableDeferred<CastAck>()

        fun answerAtOnce(ack: CastAck) {
            answer = CompletableDeferred(ack)
        }

        override fun observeTally(uid: String): Flow<CastTally?> = tallyFlow

        override fun readTally(uid: String): CastTally? {
            if (failRead) throw IllegalStateException("offline, nothing cached")
            return tally
        }

        override fun writeCast(uid: String, cast: NewCast, rand: Double, tally: TallyWrite): PendingCast {
            writes += cast to tally
            val answer = answer
            return PendingCast { answer.await() }
        }

        override fun observeCast(messageId: String): Flow<CastStatus> = flowOf(CastStatus.Queued)
    }
}
