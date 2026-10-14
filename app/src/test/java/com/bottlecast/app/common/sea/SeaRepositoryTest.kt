package com.bottlecast.app.common.sea

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.data.FindOutcome
import com.bottlecast.app.common.sea.data.FindRecord
import com.bottlecast.app.common.sea.data.SeaRefusedException
import com.bottlecast.app.common.sea.data.SeaUnavailableException
import com.bottlecast.app.common.sea.data.SeaRepository
import com.bottlecast.app.common.sea.data.seaCandidates
import com.bottlecast.app.common.sea.data.sha256
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.Handover
import com.bottlecast.app.common.sea.domain.HandoverResult
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
import java.security.SecureRandom
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
    fun `a new day, or another account, picks again with a fresh look at the blocked writers`() = runTest {
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
        Assert.assertEquals(3, source.blockedReads)
    }

    @Test
    fun `a writer blocked today is left out of tomorrow's look`() = runTest {
        // given
        source.drifting = listOf(bottle("first"))
        val repository = repository()
        repository.prefetchCandidate()

        // when
        source.blocked = setOf("troll")
        source.drifting = listOf(bottle("theirs", writer = "troll"), bottle("ok"))
        clock.advance(Duration.ofDays(1))
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals("ok", (state as CandidateState.Available).message.id)
    }

    @Test
    fun `a page of bottles found before reads on to the next`() = runTest {
        // given
        source.drifting = (1..12).map { bottle("b$it", rand = it / 100.0) }
        (1..10).forEach { source.foundBefore += "b$it" to "finn" }
        val repository = repository()

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals("b11", (state as CandidateState.Available).message.id)
    }

    @Test
    fun `nothing this account could find above the random point, so it looks round from the bottom`() = runTest {
        // given
        source.drifting = listOf(bottle("above", rand = 0.9), bottle("below", rand = 0.2))
        source.foundBefore += "above" to "finn"
        val repository = repository(fromRand = 0.5)

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals("below", (state as CandidateState.Available).message.id)
    }

    @Test
    fun `when everything drifting was found before, a hold that has ended is found`() = runTest {
        // given
        source.drifting = listOf(bottle("seen"), bottle("also seen"))
        source.foundBefore += "seen" to "finn"
        source.foundBefore += "also seen" to "finn"
        source.expired = listOf(bottle("drifted back", status = MessageStatus.Found, foundAt = clock.instant().minus(Duration.ofDays(8))))
        val repository = repository()

        // when
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals("drifted back", (state as CandidateState.Available).message.id)
    }

    @Test
    fun `a bottle found before is remembered, so a later look doesn't read it again`() = runTest {
        // given
        source.drifting = listOf(bottle("seen"))
        source.foundBefore += "seen" to "finn"
        val repository = repository()
        repository.prefetchCandidate()
        val reads = source.hasFoundReads

        // when
        clock.advance(Duration.ofMinutes(10))
        val state = repository.prefetchCandidate()

        // then
        Assert.assertEquals(CandidateState.SeaCalm, state)
        Assert.assertEquals(reads, source.hasFoundReads)
    }

    @Test
    fun `yesterday's candidate is forgotten when today's look begins, even if it fails`() = runTest {
        // given
        val repository = repository()
        repository.prefetchCandidate()
        source.unreachable = true

        // when
        clock.advance(Duration.ofDays(1))
        val failure = runCatching { repository.prefetchCandidate() }.exceptionOrNull()

        // then
        Assert.assertTrue(failure is SeaUnavailableException)
        Assert.assertEquals(CandidateState.Unknown, repository.candidate.value)
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
        Assert.assertEquals(FindResult.Found(held(bottle("today"))), result)
        Assert.assertEquals(listOf("today"), source.findAttempts)
        Assert.assertEquals(held(bottle("today")), repository.cachedMessage("today"))
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
        Assert.assertEquals(FindResult.Found(held(bottle("next"))), result)
        Assert.assertEquals(listOf("taken", "next"), source.findAttempts)
    }

    @Test
    fun `bottles found before aren't kept to fall back on`() = runTest {
        // given
        source.drifting = listOf(bottle("taken"), bottle("seen"), bottle("also seen"), bottle("next"))
        source.foundBefore += "seen" to "finn"
        source.foundBefore += "also seen" to "finn"
        source.findOutcomes["taken"] = { FindOutcome.Taken }
        val repository = repository()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.Found(held(bottle("next"))), result)
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
        Assert.assertEquals(FindResult.Found(held(bottle("next"))), result)
    }

    // an earlier attempt went through though its answer was lost
    @Test
    fun `when today already has a find of the same bottle, it's found again, held from now`() = runTest {
        // given
        source.drifting = listOf(bottle("today"))
        source.findOutcomes["today"] = { FindOutcome.AlreadyFound("today") }
        val repository = repository()

        // when
        val result = repository.findTodaysBottle()

        // then
        Assert.assertEquals(FindResult.Found(held(bottle("today"))), result)
        Assert.assertEquals(held(bottle("today")), repository.cachedMessage("today"))
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
    fun `a bottle that can't be read any more is gone from today's find and from memory`() = runTest {
        // given
        source.drifting = listOf(bottle("today"))
        val repository = repository()
        repository.findTodaysBottle()
        source.daily.value = "today"
        source.findStatus.value = FindStatus.Held

        // when
        source.message.value = null
        val today = repository.observeToday(LocalDate.of(2026, 10, 13)).first()

        // then
        Assert.assertNull(today?.message)
        Assert.assertNull(repository.cachedMessage("today"))
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

    @Test
    fun `a guest arms a handover of today's live hold, with only a hash of its token`() = runTest {
        // given
        liveHold()
        val repository = repository()

        // when
        val handover = repository.prepareHandover()

        // then
        Assert.assertEquals("today", handover?.messageId)
        Assert.assertEquals("finn", handover?.fromUid)
        val (messageId, fromUid, hash) = source.armed.single()
        Assert.assertEquals("today" to "finn", messageId to fromUid)
        Assert.assertArrayEquals(sha256(handover!!.token), hash)
        Assert.assertEquals(43, handover.token.length)
        Assert.assertFalse(handover.toString().contains(handover.token))
    }

    @Test
    fun `there's nothing to hand over without a live hold`() = runTest {
        // given
        val cases: Map<String, (MessageEntity) -> Unit> = mapOf(
            "no find today" to { source.dailyRecords.clear() },
            "a thrown-back find" to { source.findRecords["today" to "finn"] = FindRecord(FindStatus.ThrownBack, it.foundAt) },
            "a hold that's moved on" to { source.findRecords["today" to "finn"] = FindRecord(FindStatus.Held, it.foundAt!!.minusSeconds(60)) },
            "a bottle that can't be read" to { source.serverMessages.clear() },
        )

        cases.forEach { (case, takeAway) ->
            takeAway(liveHold())

            // when
            val handover = repository().prepareHandover()

            // then
            Assert.assertNull(case, handover)
        }
        Assert.assertTrue(source.armed.isEmpty())
    }

    @Test
    fun `a hold that has run out isn't handed over`() = runTest {
        // given: found 8 days ago, though the daily record is today's
        liveHold(foundAt = clock.instant().minus(Duration.ofDays(8)))

        // when
        val handover = repository().prepareHandover()

        // then
        Assert.assertNull(handover)
    }

    @Test
    fun `a Google account has nothing to hand over, and an unreachable server means nothing`() = runTest {
        // given
        val repository = repository()
        user.value = google("gail")

        // when
        val asGoogle = repository.prepareHandover()
        user.value = guest("finn")
        source.unreachable = true
        val offline = repository.prepareHandover()

        // then
        Assert.assertNull(asGoogle)
        Assert.assertNull(offline)
    }

    @Test
    fun `the Google account claims the handover and holds the bottle from now`() = runTest {
        // given
        user.value = google("gail")
        val repository = repository()
        val handover = Handover("today", "finn", "token")

        // when
        val result = repository.claimHandover(handover)

        // then
        Assert.assertEquals(HandoverResult.HandedOver, result)
        Assert.assertEquals(listOf("today" to "token"), source.claims)
    }

    @Test
    fun `a Google account with its own find today keeps it`() = runTest {
        // given
        user.value = google("gail")
        source.dailyRecords["gail" to LocalDate.now(clock)] = "theirs"
        val repository = repository()

        // when
        val result = repository.claimHandover(Handover("today", "finn", "token"))

        // then
        Assert.assertEquals(HandoverResult.KeptOwnFind, result)
        Assert.assertTrue(source.claims.isEmpty())
    }

    @Test
    fun `a bottle the Google account found before, or a refused claim, doesn't move`() = runTest {
        // given
        user.value = google("gail")
        source.foundBefore += "today" to "gail"
        val repository = repository()

        // when
        val foundBefore = repository.claimHandover(Handover("today", "finn", "token"))
        source.foundBefore.clear()
        source.refuseClaim = true
        val refused = repository().claimHandover(Handover("today", "finn", "token"))

        // then
        Assert.assertEquals(HandoverResult.Failed, foundBefore)
        Assert.assertEquals(HandoverResult.Failed, refused)
    }

    // finn's guest holds "today", found an hour ago, as the server has it; returns the held bottle
    private fun liveHold(foundAt: Instant = clock.instant().minus(Duration.ofHours(1))): MessageEntity {
        val heldBottle = bottle("today", status = MessageStatus.Found, foundAt = foundAt)
        source.dailyRecords.clear()
        source.dailyRecords["finn" to LocalDate.now(clock)] = "today"
        source.findRecords["today" to "finn"] = FindRecord(FindStatus.Held, foundAt)
        source.serverMessages["today"] = heldBottle
        return heldBottle
    }

    // fromRand is where the look at the sea starts; from 0 it reads every bottle upwards
    private fun TestScope.repository(fromRand: Double = 0.0) = SeaRepository(
        source, auth, connectivity, clock, fixedRandom(fromRand), SecureRandom(), StandardTestDispatcher(testScheduler),
    )

    private fun fixedRandom(value: Double) = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = value
    }

    // a bottle as its find leaves it
    private fun held(bottle: MessageEntity) =
        bottle.copy(status = MessageStatus.Found, findCount = bottle.findCount + 1, foundAt = clock.instant())

    private fun guest(uid: String) = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "token", uid, isAdmin = false)

    private fun google(uid: String) = UserEntity.AuthenticatedUser(AuthProviderType.Google, "token", uid, isAdmin = false)

    private fun bottle(
        id: String,
        writer: String = "writer",
        status: MessageStatus = MessageStatus.Afloat,
        foundAt: Instant? = null,
        reportCount: Int = 0,
        rand: Double = 0.5,
    ) = MessageEntity(
        id = id,
        message = "Message $id",
        userId = writer,
        status = status,
        foundAt = foundAt,
        reportCount = reportCount,
        rand = rand,
    )
}
