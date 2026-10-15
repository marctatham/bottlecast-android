package com.bottlecast.app.common.streak

import com.bottlecast.app.common.streak.data.StreakCheckpointStore
import com.bottlecast.app.common.streak.domain.StreakCheckpoint
import com.bottlecast.app.common.streak.domain.StreakEvent
import com.bottlecast.app.testing.filePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate

/** Unit tests for [StreakCheckpointStore]: one account's checkpoint and seen news, kept in a file. */
class StreakCheckpointStoreTest {

    private val day = LocalDate.of(2026, 10, 12)

    @Test
    fun `a checkpoint is kept for its account, whatever its last event`() = runTest {
        // given
        val store = StreakCheckpointStore(filePreferences(backgroundScope), backgroundScope)
        val shield = StreakCheckpoint(day, current = 6, best = 14, shields = 1, lastEvent = StreakEvent.ShieldUsed(day, 6))
        val rested = shield.copy(current = 0, lastEvent = StreakEvent.Rested(day))
        val quiet = shield.copy(lastEvent = null)

        // when
        store.save("finn", shield).join()
        val loadedShield = store.load("finn")
        store.save("finn", rested).join()
        val loadedRested = store.load("finn")
        store.save("finn", quiet).join()
        val loadedQuiet = store.load("finn")

        // then
        Assert.assertEquals(shield, loadedShield)
        Assert.assertEquals(rested, loadedRested)
        Assert.assertEquals(quiet, loadedQuiet)
    }

    @Test
    fun `another account folds afresh`() = runTest {
        // given
        val store = StreakCheckpointStore(filePreferences(backgroundScope), backgroundScope)
        store.save("finn", StreakCheckpoint(day, current = 6, best = 14, shields = 1)).join()

        // when
        val other = store.load("gail")

        // then
        Assert.assertNull(other)
    }

    @Test
    fun `seen news is kept for its account`() = runTest {
        // given
        val store = StreakCheckpointStore(filePreferences(backgroundScope), backgroundScope)

        // when
        store.markNoticeSeen("finn", day).join()

        // then
        Assert.assertEquals(day, store.noticeSeenThrough("finn").first())
        Assert.assertNull(store.noticeSeenThrough("gail").first())
    }
}
