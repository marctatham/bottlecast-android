package com.bottlecast.app.common.chest

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.chest.data.ChestRepository
import com.bottlecast.app.common.chest.data.ChestSeen
import com.bottlecast.app.common.chest.data.ChestTotals
import com.bottlecast.app.common.chest.data.ChestUnavailableException
import com.bottlecast.app.common.chest.data.Claim
import com.bottlecast.app.common.chest.data.FindOf
import com.bottlecast.app.common.chest.data.IChestDataSource
import com.bottlecast.app.common.chest.data.IChestSeenStore
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.ChestItem
import com.bottlecast.app.common.chest.domain.ChestPage
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.chest.domain.daysLeftUntil
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.testing.MutableClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Unit tests for [ChestRepository]: keeping, the Found and Cast lists, one bottle, pulling, counts and news. */
class ChestRepositoryTest {

    private val clock = MutableClock(Instant.parse("2026-10-12T09:00:00Z"))
    private val google = UserEntity.AuthenticatedUser(AuthProviderType.Google, "jwt", "finn", isAdmin = false)
    private val guest = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "gus", isAdmin = false)
    private val user = MutableStateFlow<UserEntity>(google)
    private val online = MutableStateFlow(true)
    private val auth = mockk<IAuthenticationRepository> { every { user } returns this@ChestRepositoryTest.user }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns online }
    private val source = FakeChestDataSource()
    private val seen = MemorySeenStore()

    @Test
    fun `keeping goes to the server for a Google account, and counts again once kept`() = runTest {
        // given
        val repository = repository()

        // when
        val result = repository.keep("bottle")
        runCurrent()

        // then
        Assert.assertEquals(KeepResult.Kept, result)
        Assert.assertEquals(listOf("bottle" to "finn"), source.kept)
        Assert.assertEquals(1, source.counted)
    }

    @Test
    fun `offline, keeping waits for a connection, and a guest signs in first`() = runTest {
        // given
        val repository = repository()

        // when
        online.value = false
        val offline = repository.keep("bottle")
        online.value = true
        user.value = guest
        val asGuest = repository.keep("bottle")

        // then
        Assert.assertEquals(KeepResult.Offline, offline)
        Assert.assertEquals(KeepResult.NotHeld, asGuest)
        Assert.assertEquals(emptyList<Pair<String, String>>(), source.kept)
    }

    @Test
    fun `a keep that throws fails, rather than crashing`() = runTest {
        // given
        source.keepFails = true
        val repository = repository()

        // when
        val result = repository.keep("bottle")

        // then
        Assert.assertEquals(KeepResult.Failed, result)
    }

    @Test
    fun `not kept holds the live, held finds that can still be read, newest first`() = runTest {
        // given
        source.finds.value = listOf(
            FindOf("held", clock.instant().minus(Duration.ofDays(1)), FindStatus.Held),
            FindOf("thrown", clock.instant().minus(Duration.ofDays(2)), FindStatus.ThrownBack),
            FindOf("kept", clock.instant().minus(Duration.ofDays(3)), FindStatus.Kept),
            FindOf("ended", clock.instant().minus(Duration.ofDays(8)), FindStatus.Held),
            FindOf("pulled", clock.instant().minus(Duration.ofDays(1)), FindStatus.Held),
        )
        source.messages["held"] = bottle("held")
        val repository = repository()

        // when
        val notKept = repository.notKept.first { it != null }

        // then
        Assert.assertEquals(listOf(FoundBottle("held", bottle("held"), clock.instant().minus(Duration.ofDays(1)), keptAt = null)), notKept)
    }

    @Test
    fun `kept bottles come from the claims, with a removed one's words gone`() = runTest {
        // given
        val keptAt = clock.instant().minus(Duration.ofDays(1))
        source.claims.value = listOf(Claim("kept", keptAt), Claim("removed", keptAt))
        source.messages["kept"] = bottle("kept", status = MessageStatus.Kept)
        val repository = repository()

        // when
        val page = repository.kept(limit = 2).first()

        // then
        Assert.assertEquals(
            ChestPage(listOf(FoundBottle("kept", bottle("kept", status = MessageStatus.Kept), null, keptAt), FoundBottle("removed", null, null, keptAt)), hasMore = true),
            page,
        )
    }

    @Test
    fun `a guest has nothing kept or cast`() = runTest {
        // given
        user.value = guest
        source.claims.value = listOf(Claim("kept", clock.instant()))
        val repository = repository()

        // when
        val kept = repository.kept(20).first()
        val cast = repository.cast(20).first()

        // then
        Assert.assertEquals(ChestPage(emptyList<FoundBottle>(), hasMore = false), kept)
        Assert.assertEquals(ChestPage(emptyList<MessageEntity>(), hasMore = false), cast)
    }

    @Test
    fun `one bottle is a cast of the account's own, a kept find, an unkept find, or gone`() = runTest {
        // given
        val heldSince = clock.instant().minus(Duration.ofDays(2))
        source.messages["mine"] = bottle("mine", writer = "finn")
        val kept = bottle("kept", status = MessageStatus.Kept).copy(foundAt = heldSince)
        source.messages["kept"] = kept
        source.claimsById["kept"] = Claim("kept", heldSince)
        source.messages["held"] = bottle("held", status = MessageStatus.Found)
        source.findsById["held"] = FindOf("held", heldSince, FindStatus.Held)
        source.messages["thrown"] = bottle("thrown")
        source.findsById["thrown"] = FindOf("thrown", heldSince, FindStatus.ThrownBack)
        val repository = repository()

        // when
        val mine = repository.item("mine").first()
        val keptItem = repository.item("kept").first()
        val held = repository.item("held").first()
        val thrown = repository.item("thrown").first()

        // then
        Assert.assertEquals(ChestItem.Cast(bottle("mine", writer = "finn")), mine)
        Assert.assertEquals(ChestItem.Found(FoundBottle("kept", kept, heldSince, heldSince)), keptItem)
        Assert.assertEquals(ChestItem.Found(FoundBottle("held", bottle("held", status = MessageStatus.Found), heldSince, null)), held)
        Assert.assertNull(thrown)
    }

    @Test
    fun `pulling needs a connection, and counts again once pulled`() = runTest {
        // given
        val repository = repository()

        // when
        online.value = false
        val offline = repository.pull("mine")
        online.value = true
        val pulled = repository.pull("mine")
        runCurrent()

        // then
        Assert.assertEquals(PullResult.Offline, offline)
        Assert.assertEquals(PullResult.Pulled, pulled)
        Assert.assertEquals(listOf("mine"), source.pulled)
        Assert.assertEquals(1, source.counted)
    }

    @Test
    fun `the counts come once the server has counted, and a failed count leaves them unknown`() = runTest {
        // given
        source.totals = ChestTotals(bottled = 12, found = 30, kept = 9, castFinds = 40)
        val repository = repository()
        val counts = watch(repository.counts)

        // when
        runCurrent()
        val counted = counts.value
        source.countFails = true
        user.value = guest
        runCurrent()

        // then
        Assert.assertEquals(ChestCounts(bottled = 12, found = 30, kept = 9), counted)
        Assert.assertNull(counts.value)
    }

    @Test
    fun `counting again a moment later only repeats it, unless something changed`() = runTest {
        // given
        val repository = repository()
        watch(repository.counts)
        runCurrent()

        // when
        repository.refreshCounts()
        runCurrent()
        val soon = source.counted
        clock.advance(Duration.ofMinutes(1))
        repository.refreshCounts()
        runCurrent()

        // then
        Assert.assertEquals(1, soon)
        Assert.assertEquals(2, source.counted)
    }

    @Test
    fun `the first count is the baseline, and later finds of the account's bottles are news until viewed`() = runTest {
        // given
        source.totals = ChestTotals(bottled = 12, found = 30, kept = 9, castFinds = 40)
        val repository = repository()
        val away = watch(repository.castFindsSinceViewed)
        val badge = watch(repository.badge)
        watch(repository.counts)
        runCurrent()
        val atFirst = away.value to badge.value

        // when
        source.totals = source.totals.copy(castFinds = 42)
        clock.advance(Duration.ofMinutes(1))
        repository.refreshCounts()
        runCurrent()
        val later = away.value to badge.value
        repository.markViewed()
        runCurrent()

        // then
        Assert.assertEquals(0 to false, atFirst)
        Assert.assertEquals(2 to true, later)
        Assert.assertEquals(0 to false, away.value to badge.value)
    }

    @Test
    fun `a bottle not kept yet is news until the chest is viewed`() = runTest {
        // given
        source.finds.value = listOf(FindOf("held", clock.instant().minus(Duration.ofHours(1)), FindStatus.Held))
        source.messages["held"] = bottle("held")
        val repository = repository()
        val badge = watch(repository.badge)
        runCurrent()
        val unseen = badge.value

        // when
        clock.advance(Duration.ofMinutes(5))
        repository.markViewed()
        runCurrent()

        // then
        Assert.assertEquals(true, unseen)
        Assert.assertEquals(false, badge.value)
    }

    @Test
    fun `days left count a part day as one`() {
        val now = clock.instant()
        Assert.assertEquals(5, daysLeftUntil(now.plus(Duration.ofDays(5)), now))
        Assert.assertEquals(5, daysLeftUntil(now.plus(Duration.ofDays(4)).plusSeconds(1), now))
        Assert.assertEquals(1, daysLeftUntil(now.plusSeconds(60), now))
        Assert.assertEquals(0, daysLeftUntil(now.minusSeconds(1), now))
    }

    private fun TestScope.repository() = ChestRepository(
        source, seen, auth, connectivity, clock, backgroundScope, StandardTestDispatcher(testScheduler),
    )

    private fun <T> TestScope.watch(flow: Flow<T>): MutableStateFlow<T?> {
        val latest = MutableStateFlow<T?>(null)
        backgroundScope.launch { flow.collect { latest.value = it } }
        runCurrent()
        return latest
    }

    private fun bottle(id: String, writer: String = "writer", status: MessageStatus = MessageStatus.Afloat) =
        MessageEntity(id = id, message = "Message $id", userId = writer, status = status)
}

private class FakeChestDataSource : IChestDataSource {
    val finds = MutableStateFlow<List<FindOf>>(emptyList())
    val claims = MutableStateFlow<List<Claim>>(emptyList())
    val casts = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages = mutableMapOf<String, MessageEntity>()
    val findsById = mutableMapOf<String, FindOf>()
    val claimsById = mutableMapOf<String, Claim>()
    val kept = mutableListOf<Pair<String, String>>()
    val pulled = mutableListOf<String>()
    var keepFails = false
    var countFails = false
    var counted = 0
    var totals = ChestTotals(bottled = 0, found = 0, kept = 0, castFinds = 0)

    override fun keep(messageId: String, uid: String, now: Instant): KeepResult {
        if (keepFails) error("broken")
        kept += messageId to uid
        return KeepResult.Kept
    }

    override fun pull(messageId: String, uid: String, now: Instant): PullResult {
        pulled += messageId
        return PullResult.Pulled
    }

    override fun observeFinds(uid: String, since: Instant): Flow<List<FindOf>> = finds.map { all -> all.filter { it.foundAt > since } }

    override fun observeClaims(uid: String, limit: Int): Flow<List<Claim>> = claims.map { it.take(limit) }

    override fun observeCasts(uid: String, limit: Int): Flow<List<MessageEntity>> = casts.map { it.take(limit) }

    override fun observeMessage(messageId: String): Flow<MessageEntity?> = MutableStateFlow(messages[messageId])

    override fun observeFind(messageId: String, uid: String): Flow<FindOf?> = MutableStateFlow(findsById[messageId])

    override fun observeClaim(messageId: String): Flow<Claim?> = MutableStateFlow(claimsById[messageId])

    override fun readMessage(messageId: String): MessageEntity? = messages[messageId]

    override fun count(uid: String, canCast: Boolean): ChestTotals {
        counted++
        if (countFails) throw ChestUnavailableException(IllegalStateException("offline"))
        return if (canCast) totals else totals.copy(bottled = null, kept = 0, castFinds = null)
    }
}

private class MemorySeenStore : IChestSeenStore {
    private val seen = MutableStateFlow<Map<String, ChestSeen>>(emptyMap())

    override fun seen(uid: String): Flow<ChestSeen?> = seen.map { it[uid] }

    override fun save(uid: String, seen: ChestSeen): Job {
        this.seen.value = this.seen.value + (uid to seen)
        return Job().apply { complete() }
    }
}
