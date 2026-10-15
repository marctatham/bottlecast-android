package com.bottlecast.app.common.streak

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.streak.data.IStreakCheckpointStore
import com.bottlecast.app.common.streak.data.IStreakDataSource
import com.bottlecast.app.common.streak.data.Read
import com.bottlecast.app.common.streak.data.StreakRepository
import com.bottlecast.app.common.streak.domain.Streak
import com.bottlecast.app.common.streak.domain.StreakCheckpoint
import com.bottlecast.app.common.streak.domain.StreakDay
import com.bottlecast.app.common.streak.domain.StreakEvent
import com.bottlecast.app.testing.MutableClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Unit tests for [StreakRepository]: the live streak, its checkpoint, calm seas and carrying a guest's streak. */
class StreakRepositoryTest {

    private val clock = MutableClock(Instant.parse("2026-10-15T09:00:00Z"))
    private val today = LocalDate.of(2026, 10, 15)
    private val user = MutableStateFlow<UserEntity>(UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "finn", isAdmin = false))
    private val auth = mockk<IAuthenticationRepository> { every { user } returns this@StreakRepositoryTest.user }
    private val source = FakeStreakDataSource()
    private val checkpoints = MemoryCheckpointStore()

    @Test
    fun `the streak is folded from the account's finds and marks`() = runTest {
        // given
        source.finds.value = Read(setOf(today.minusDays(2), today.minusDays(1), today), fromServer = true)
        source.marks.value = Read(listOf(StreakDay(today.minusDays(2), cast = true)), fromServer = true)
        val repository = repository()

        // when
        val streak = watch(repository)

        // then
        Assert.assertEquals(Streak(current = 3, best = 3, shields = 1, foundToday = true), streak.value)
    }

    @Test
    fun `the checkpoint moves on once the server has confirmed the days, and bounds the next look`() = runTest {
        // given
        source.finds.value = Read(setOf(today.minusDays(5), today.minusDays(4), today.minusDays(1)), fromServer = false)
        val repository = repository()
        val streak = watch(repository)
        val fromCache = checkpoints.saved

        // when
        source.finds.value = source.finds.value.copy(fromServer = true)
        runCurrent()

        // then
        Assert.assertNull(fromCache)
        Assert.assertEquals(StreakCheckpoint(through = today.minusDays(3), current = 0, best = 2, shields = 0, lastEvent = StreakEvent.Rested(today.minusDays(3))), checkpoints.saved)
        Assert.assertEquals(1, streak.value?.current)
        Assert.assertEquals(listOf<LocalDate?>(null), source.queriedAfter)
    }

    @Test
    fun `a saved checkpoint is read on from, so only the last days are asked for`() = runTest {
        // given
        checkpoints.saved = StreakCheckpoint(through = today.minusDays(3), current = 6, best = 14, shields = 1)
        source.finds.value = Read(setOf(today.minusDays(2), today.minusDays(1)), fromServer = true)
        val repository = repository()

        // when
        val streak = watch(repository)

        // then
        Assert.assertEquals(Streak(current = 8, best = 14, shields = 1, foundToday = false), streak.value)
        Assert.assertEquals(listOf<LocalDate?>(today.minusDays(3)), source.queriedAfter)
    }

    @Test
    fun `there's no streak while nobody is signed in`() = runTest {
        // given
        user.value = UserEntity.UnauthenticatedUser
        val repository = repository()

        // when
        val streak = watch(repository)

        // then
        Assert.assertNull(streak.value)
    }

    @Test
    fun `a calm sea is marked once a day`() = runTest {
        // given
        val repository = repository()

        // when
        repository.markCalm("finn", today)
        repository.markCalm("finn", today)
        repository.markCalm("finn", today.plusDays(1))

        // then
        Assert.assertEquals(listOf("finn" to today, "finn" to today.plusDays(1)), source.calm)
    }

    @Test
    fun `a guest carries the days before today, and the account takes them on today`() = runTest {
        // given
        source.finds.value = Read(setOf(today.minusDays(1), today), fromServer = true)
        val repository = repository()
        watch(repository)

        // when
        val carryable = repository.carryable()
        repository.carry("gail", carryable)
        runCurrent()

        // then
        Assert.assertEquals(1, carryable)
        Assert.assertEquals(listOf(Triple("gail", today, 1)), source.carried)
    }

    @Test
    fun `nothing to carry writes nothing, and a refused carry is let go`() = runTest {
        // given
        val repository = repository()
        source.carryFails = true

        // when
        repository.carry("gail", 0)
        repository.carry("gail", 3)
        runCurrent()

        // then
        Assert.assertEquals(listOf(Triple("gail", today, 3)), source.carried)
    }

    @Test
    fun `a missed day's news shows until it's acknowledged`() = runTest {
        // given
        source.finds.value = Read(setOf(today.minusDays(3), today.minusDays(2)), fromServer = true)
        source.marks.value = Read(listOf(StreakDay(today.minusDays(3), cast = true)), fromServer = true)
        val repository = repository()
        watch(repository)
        val news = MutableStateFlow<StreakEvent?>(null)
        backgroundScope.launch { repository.notice.collect { news.value = it } }
        runCurrent()
        val shown = news.value

        // when
        repository.acknowledge(shown!!)
        runCurrent()

        // then
        Assert.assertEquals(StreakEvent.ShieldUsed(today.minusDays(1), 2), shown)
        Assert.assertNull(news.value)
    }

    private fun TestScope.repository() = StreakRepository(
        source, checkpoints, auth, clock, backgroundScope, StandardTestDispatcher(testScheduler),
    )

    private fun TestScope.watch(repository: StreakRepository): MutableStateFlow<Streak?> {
        val streak = MutableStateFlow<Streak?>(null)
        backgroundScope.launch { repository.streak.collect { streak.value = it } }
        runCurrent()
        return streak
    }
}

private class FakeStreakDataSource : IStreakDataSource {
    val finds = MutableStateFlow(Read(emptySet<LocalDate>(), fromServer = true))
    val marks = MutableStateFlow(Read(emptyList<StreakDay>(), fromServer = true))
    val queriedAfter = mutableListOf<LocalDate?>()
    val calm = mutableListOf<Pair<String, LocalDate>>()
    val carried = mutableListOf<Triple<String, LocalDate, Int>>()
    var carryFails = false

    override fun observeFindDays(uid: String, after: LocalDate?): Flow<Read<Set<LocalDate>>> {
        queriedAfter += after
        return finds.map { read -> read.copy(value = read.value.filter { after == null || it > after }.toSet()) }
    }

    override fun observeMarks(uid: String, after: LocalDate?): Flow<Read<List<StreakDay>>> =
        marks.map { read -> read.copy(value = read.value.filter { after == null || it.date > after }) }

    override fun markCalm(uid: String, date: LocalDate) {
        calm += uid to date
    }

    override fun carry(uid: String, date: LocalDate, days: Int) {
        carried += Triple(uid, date, days)
        if (carryFails) error("refused")
    }
}

private class MemoryCheckpointStore : IStreakCheckpointStore {
    var saved: StreakCheckpoint? = null
    private val seen = MutableStateFlow<LocalDate?>(null)

    override suspend fun load(uid: String): StreakCheckpoint? = saved

    override fun save(uid: String, checkpoint: StreakCheckpoint): Job {
        saved = checkpoint
        return Job().apply { complete() }
    }

    override fun noticeSeenThrough(uid: String): Flow<LocalDate?> = seen

    override fun markNoticeSeen(uid: String, date: LocalDate): Job {
        seen.value = date
        return Job().apply { complete() }
    }

    override fun clear(): Job {
        saved = null
        seen.value = null
        return Job().apply { complete() }
    }
}
