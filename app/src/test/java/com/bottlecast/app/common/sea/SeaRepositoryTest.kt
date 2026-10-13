package com.bottlecast.app.common.sea

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.data.FindOutcome
import com.bottlecast.app.common.sea.data.SeaRefusedException
import com.bottlecast.app.common.sea.data.SeaUnavailableException
import com.bottlecast.app.common.sea.data.SeaRepository
import com.bottlecast.app.common.sea.data.seaCandidates
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.TodayFind
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/**
 * Unit tests for [SeaRepository]: picking today's candidate, and recording the find.
 */
class SeaRepositoryTest {

    private val source = FakeSeaDataSource()
    private val user = MutableStateFlow<UserEntity>(guest("finn"))
    private val online = MutableStateFlow(true)
    private val clock = MutableClock(Instant.parse("2026-10-13T09:00:00Z"))

    private val auth = mockk<IAuthenticationRepository> {
        every { this@mockk.user } returns this@SeaRepositoryTest.user
        coEvery { ensureSignedIn() } answers { (user.value as UserEntity.AuthenticatedUser).userId }
    }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns online }

    @Test
    fun `the candidate is the first bottle this account could find`() = runTest {
        // given
        source.drifting = listOf(bottle("mine", writer = "finn"), bottle("blocked", writer = "troll"), bottle("ok"))
        source.blocked = setOf("troll")
        val repository = repository()

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals(CandidateState.Available(bottle("ok")), state)
        Assert.assertEquals(state, repository.candidate.value)
    }

    @Test
    fun `a bottle this account found before is skipped`() = runTest {
        // given
        source.drifting = listOf(bottle("seen"), bottle("new"))
        source.foundBefore += "seen" to "finn"
        val repository = repository()

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals(CandidateState.Available(bottle("new")), state)
    }

    @Test
    fun `when nothing is drifting, a bottle whose hold has ended is found instead`() = runTest {
        // given
        source.expired = listOf(bottle("drifted back", status = MessageStatus.Found, foundAt = clock.instant().minus(Duration.ofDays(8))))
        val repository = repository()

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals("drifted back", (state as CandidateState.Available).message.id)
    }

    @Test
    fun `when the sea has nothing this account could find, the sea is calm`() = runTest {
        // given
        source.drifting = listOf(bottle("mine", writer = "finn"))
        val repository = repository()

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals(CandidateState.SeaCalm, state)
    }

    @Test
    fun `the candidate is pinned for the day, so looking again doesn't re-roll it`() = runTest {
        // given
        source.drifting = listOf(bottle("first"), bottle("second"))
        val repository = repository()
        repository.prefetchCandidate()
        val queries = source.seaQueries.size
        source.drifting = listOf(bottle("second"))

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals("first", (state as CandidateState.Available).message.id)
        Assert.assertEquals(queries, source.seaQueries.size)
    }

    @Test
    fun `a new day, or another account, picks again`() = runTest {
        // given
        source.drifting = listOf(bottle("first"))
        val repository = repository()
        repository.prefetchCandidate()
        source.drifting = listOf(bottle("tomorrow"))

        // when
        clock.advance(Duration.ofDays(1))
        val nextDay = repository.prefetchCandidate()
        user.value = guest("fern")
        source.drifting = listOf(bottle("theirs"))
        val otherAccount = repository.prefetchCandidate()

        // then
        Assert.assertEquals("tomorrow", (nextDay as CandidateState.Available).message.id)
        Assert.assertEquals("theirs", (otherAccount as CandidateState.Available).message.id)
        Assert.assertEquals(2, source.blockedReads)
    }

    @Test
    fun `a calm sea is looked at again after ten minutes`() = runTest {
        // given
        val repository = repository()
        repository.prefetchCandidate()
        source.drifting = listOf(bottle("just cast"))

        // when
        clock.advance(Duration.ofMinutes(5))
        val soon = repository.prefetchCandidate()
        clock.advance(Duration.ofMinutes(5))
        val later = repository.prefetchCandidate()

        // then
        Assert.assertEquals(CandidateState.SeaCalm, soon)
        Assert.assertEquals("just cast", (later as CandidateState.Available).message.id)
    }

    @Test
    fun `finding records the candidate and keeps it for Reveal`() = runTest {
        // given
        source.drifting = listOf(bottle("today"))
        val repository = repository()
        repository.prefetchCandidate()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.Found(bottle("today")), result)
        Assert.assertEquals(listOf("today"), source.findAttempts)
        Assert.assertEquals(bottle("today"), repository.cachedMessage("today"))
    }

    @Test
    fun `when someone else takes the candidate first, the next one is found`() = runTest {
        // given
        source.drifting = listOf(bottle("taken"), bottle("next"))
        source.findOutcomes["taken"] = { FindOutcome.Taken }
        val repository = repository()
        repository.prefetchCandidate()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.Found(bottle("next")), result)
        Assert.assertEquals(listOf("taken", "next"), source.findAttempts)
    }

    @Test
    fun `when the rules turn a candidate down, the next one is found`() = runTest {
        // given
        source.drifting = listOf(bottle("refused"), bottle("next"))
        source.findOutcomes["refused"] = { throw SeaRefusedException(IllegalStateException("denied")) }
        val repository = repository()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.Found(bottle("next")), result)
    }

    @Test
    fun `when today already has a find of the same bottle, it's found again`() = runTest {
        // given
        source.drifting = listOf(bottle("today"))
        source.findOutcomes["today"] = { FindOutcome.AlreadyFound("today") }
        val repository = repository()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.Found(bottle("today")), result)
    }

    @Test
    fun `when today already has a find of another bottle, that's the answer`() = runTest {
        // given
        source.drifting = listOf(bottle("today"))
        source.findOutcomes["today"] = { FindOutcome.AlreadyFound("earlier") }
        val repository = repository()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.AlreadyFound("earlier"), result)
    }

    @Test
    fun `finding offline doesn't try at all, and losing the connection mid-way says so`() = runTest {
        // given
        source.drifting = listOf(bottle("today"))
        source.findOutcomes["today"] = { throw SeaUnavailableException(IllegalStateException("offline")) }
        val repository = repository()

        // when
        online.value = false
        val offline = repository.findTodaysBottle()
        online.value = true
        val dropped = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.Offline, offline)
        Assert.assertEquals(FindResult.Offline, dropped)
    }

    @Test
    fun `when nothing can be found, the sea is calm`() = runTest {
        // given
        val repository = repository()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.SeaCalm, result)
    }

    @Test
    fun `today's find combines the daily record, the find and the bottle`() = runTest {
        // given
        val repository = repository()
        source.daily.value = "today"
        source.findStatus.value = FindStatus.Reported
        source.message.value = bottle("today")

        // when
        val today = repository.observeToday(LocalDate.of(2026, 10, 13)).first()

        // then
        Assert.assertEquals(TodayFind(LocalDate.of(2026, 10, 13), "today", bottle("today"), FindStatus.Reported), today)
    }

    @Test
    fun `there's no find while nobody is signed in`() = runTest {
        // given
        user.value = UserEntity.UnauthenticatedUser
        source.daily.value = "today"
        val repository = repository()

        // when
        val today = repository.observeToday(LocalDate.of(2026, 10, 13)).first()

        // then
        Assert.assertNull(today)
    }

    @Test
    fun `candidates leave out the account's own, blocked writers', tried and reported-out bottles`() {
        // given
        val pool = listOf(
            bottle("mine", writer = "finn"), bottle("blocked", writer = "troll"), bottle("tried"),
            bottle("reported", reportCount = 3), bottle("ok"), bottle("ok"),
        )

        // when
        val candidates = seaCandidates(pool, uid = "finn", blockedWriters = setOf("troll"), exclude = setOf("tried"))

        // then
        Assert.assertEquals(listOf(bottle("ok")), candidates)
    }

    private fun TestScope.repository() =
        SeaRepository(source, auth, connectivity, clock, Random(7), StandardTestDispatcher(testScheduler))

    private fun guest(uid: String) = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "token", uid, isAdmin = false)

    private fun bottle(
        id: String,
        writer: String = "writer",
        status: MessageStatus = MessageStatus.Afloat,
        foundAt: Instant? = null,
        reportCount: Int = 0,
    ) = MessageEntity(id = id, message = "Message $id", userId = writer, status = status, foundAt = foundAt, reportCount = reportCount)
}
