package com.bottlecast.app.common.sea

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.data.SAFETY_WINDOW_MILLIS
import com.bottlecast.app.common.sea.data.SeaActions
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.sea.domain.SeaActionOutcome
import com.bottlecast.app.testing.MutableClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Unit tests for [SeaActions]: throw backs and reports wait out their undo window, then are written.
 */
class SeaActionsTest {

    private val source = FakeSeaDataSource()
    private val user = MutableStateFlow<UserEntity>(
        UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "token", "finn", isAdmin = false),
    )
    private val online = MutableStateFlow(true)
    private val clock = MutableClock(Instant.parse("2026-10-13T09:00:00Z"))

    private val auth = mockk<IAuthenticationRepository> { every { this@mockk.user } returns this@SeaActionsTest.user }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns online }

    private val held = MessageEntity(
        id = "today", message = "Hello", userId = "writer",
        status = MessageStatus.Found, foundAt = clock.instant().minus(Duration.ofHours(1)),
    )

    @Test
    fun `a throw back waits out its window, then releases the bottle`() = runTest {
        // given
        val actions = actions()
        val outcomes = collectOutcomes(actions)

        // when
        actions.throwBack(held, windowMillis = 5_000)
        advanceTimeBy(4_999)
        val beforeTheEnd = source.throwBacks.toList()
        advanceTimeBy(2)

        // then
        Assert.assertTrue(beforeTheEnd.isEmpty())
        Assert.assertEquals(listOf(Triple("today", "finn", true)), source.throwBacks)
        Assert.assertNull(actions.pending.value)
        Assert.assertEquals(1, outcomes.size)
        Assert.assertFalse(outcomes.single().queuedOffline)
    }

    @Test
    fun `while it waits, the action is pending`() = runTest {
        // given
        val actions = actions()

        // when
        val id = actions.report(held, ReportReason.Spam, blockWriter = true, windowMillis = 5_000)

        // then
        Assert.assertEquals(PendingSeaAction.Report(id, "today", blockWriter = true), actions.pending.value)
    }

    @Test
    fun `undo means nothing is written`() = runTest {
        // given
        val actions = actions()
        val id = actions.throwBack(held, windowMillis = 5_000)

        // when
        actions.undo(id)
        advanceTimeBy(10_000)

        // then
        Assert.assertTrue(source.throwBacks.isEmpty())
        Assert.assertNull(actions.pending.value)
    }

    @Test
    fun `commit writes straight away, with the block on its own`() = runTest {
        // given
        val actions = actions()
        val id = actions.report(held, ReportReason.Spam, blockWriter = true, windowMillis = 5_000)

        // when
        actions.commit(id)

        // then
        Assert.assertEquals(listOf("today" to ReportReason.Spam), source.reports)
        Assert.assertEquals(listOf("writer"), source.blocks)
        Assert.assertNull(actions.pending.value)
    }

    @Test
    fun `a report without a block blocks nobody, and nobody blocks themselves`() = runTest {
        // given
        val actions = actions()

        // when
        actions.commit(actions.report(held, reason = null, blockWriter = false, windowMillis = 5_000))
        actions.commit(actions.report(held.copy(userId = "finn"), reason = null, blockWriter = true, windowMillis = 5_000))

        // then
        Assert.assertEquals(2, source.reports.size)
        Assert.assertTrue(source.blocks.isEmpty())
    }

    @Test
    fun `with no window, it waits for the user, up to the safety limit`() = runTest {
        // given
        val actions = actions()

        // when
        actions.throwBack(held, windowMillis = null)
        advanceTimeBy(SAFETY_WINDOW_MILLIS - 1)
        val beforeTheLimit = source.throwBacks.toList()
        advanceTimeBy(2)

        // then
        Assert.assertTrue(beforeTheLimit.isEmpty())
        Assert.assertEquals(1, source.throwBacks.size)
    }

    @Test
    fun `a second action writes the first one straight away`() = runTest {
        // given
        val actions = actions()
        actions.throwBack(held, windowMillis = 5_000)

        // when
        actions.report(held.copy(id = "other"), reason = null, blockWriter = false, windowMillis = 5_000)

        // then
        Assert.assertEquals(1, source.throwBacks.size)
        Assert.assertTrue(actions.pending.value is PendingSeaAction.Report)
    }

    @Test
    fun `once the hold has ended, a throw back only marks the find`() = runTest {
        // given
        val actions = actions()
        val ended = held.copy(foundAt = clock.instant().minus(Duration.ofDays(8)))

        // when
        actions.commit(actions.throwBack(ended, windowMillis = 5_000))

        // then
        Assert.assertEquals(listOf(Triple("today", "finn", false)), source.throwBacks)
    }

    @Test
    fun `an action written offline says it's queued`() = runTest {
        // given
        val actions = actions()
        val outcomes = collectOutcomes(actions)
        online.value = false

        // when
        actions.commit(actions.report(held, reason = null, blockWriter = false, windowMillis = 5_000))
        runCurrent()

        // then
        Assert.assertTrue(outcomes.single().queuedOffline)
    }

    @Test
    fun `with nobody signed in, the action is dropped`() = runTest {
        // given
        user.value = UserEntity.UnauthenticatedUser
        val actions = actions()

        // when
        actions.throwBack(held, windowMillis = 5_000)
        advanceTimeBy(10_000)

        // then
        Assert.assertNull(actions.pending.value)
        Assert.assertTrue(source.throwBacks.isEmpty())
    }

    private fun TestScope.actions() = SeaActions(source, auth, connectivity, clock, backgroundScope)

    private fun TestScope.collectOutcomes(actions: SeaActions): List<SeaActionOutcome> {
        val outcomes = mutableListOf<SeaActionOutcome>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { actions.outcomes.toList(outcomes) }
        return outcomes
    }
}
