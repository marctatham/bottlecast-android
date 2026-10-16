package com.bottlecast.app.common.messages

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.bottlecast.app.common.messages.data.DraftRepository
import com.bottlecast.app.common.messages.domain.Draft
import com.bottlecast.app.testing.MemoryPreferences
import com.bottlecast.app.testing.filePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DraftRepositoryTest {

    private fun TestScope.repository(
        dataStore: DataStore<Preferences> = MemoryPreferences(),
        scope: CoroutineScope = backgroundScope,
    ) = DraftRepository(dataStore, scope, Random(1))

    // a second repository on the same DataStore, as after the app restarts
    private suspend fun DraftRepository.reloaded(scope: TestScope, dataStore: DataStore<Preferences>): Draft? =
        DraftRepository(dataStore, scope.backgroundScope, Random(2)).load()

    @Test
    fun `a new draft gets its bottle's id, and keeps it as the writing goes on`() = runTest {
        // given
        val drafts = repository()

        // when
        drafts.update("Hello")
        val first = drafts.draft.value
        drafts.update("Hello from the sea")

        // then
        Assert.assertEquals(20, first!!.messageId.length)
        Assert.assertEquals(Draft(first.messageId, "Hello from the sea"), drafts.draft.value)
    }

    @Test
    fun `writing is saved a second after the last change`() = runTest {
        // given
        val dataStore = MemoryPreferences()
        val drafts = repository(dataStore)

        // when
        drafts.update("Leave the porch light on")
        advanceTimeBy(999)
        runCurrent()
        val early = drafts.reloaded(this, dataStore)
        advanceTimeBy(2)
        runCurrent()
        val late = drafts.reloaded(this, dataStore)

        // then
        Assert.assertNull(early)
        Assert.assertEquals("Leave the porch light on", late?.text)
    }

    @Test
    fun `flushing saves at once`() = runTest {
        // given
        val dataStore = MemoryPreferences()
        val drafts = repository(dataStore)
        drafts.update("Leave the porch light on")

        // when
        drafts.flush()

        // then
        Assert.assertEquals(drafts.draft.value, drafts.reloaded(this, dataStore))
    }

    @Test
    fun `a saved draft loads after a restart`() = runTest {
        // given: on a real file
        val dataStore = filePreferences(backgroundScope)
        repository(dataStore).apply {
            update("Leave the porch light on")
            flush()
        }

        // when
        val loaded = repository(dataStore).load()

        // then
        Assert.assertEquals("Leave the porch light on", loaded?.text)
    }

    @Test
    fun `clearing forgets the draft`() = runTest {
        // given
        val dataStore = MemoryPreferences()
        val drafts = repository(dataStore)
        drafts.update("Leave the porch light on")
        drafts.flush()

        // when
        drafts.clear().join()

        // then
        Assert.assertNull(drafts.draft.value)
        Assert.assertNull(drafts.reloaded(this, dataStore))
    }

    @Test
    fun `a cast steps aside, and a refusal brings it back`() = runTest {
        // given
        val dataStore = MemoryPreferences()
        val drafts = repository(dataStore)
        drafts.update("Leave the porch light on")
        val id = drafts.draft.value!!.messageId

        // when
        drafts.setAside(id)
        val whileCasting = drafts.draft.value
        val restored = drafts.restore(id)

        // then
        Assert.assertNull(whileCasting)
        Assert.assertTrue(restored)
        Assert.assertEquals(Draft(id, "Leave the porch light on"), drafts.draft.value)
        Assert.assertEquals(drafts.draft.value, drafts.reloaded(this, dataStore))
    }

    @Test
    fun `a refused cast gives way to a newer draft`() = runTest {
        // given
        val drafts = repository()
        drafts.update("Leave the porch light on")
        val id = drafts.draft.value!!.messageId
        drafts.setAside(id)
        drafts.update("Something new")

        // when
        val restored = drafts.restore(id)

        // then
        Assert.assertFalse(restored)
        Assert.assertEquals("Something new", drafts.draft.value?.text)
    }

    @Test
    fun `a confirmed cast can't be restored`() = runTest {
        // given
        val drafts = repository()
        drafts.update("Leave the porch light on")
        val id = drafts.draft.value!!.messageId
        drafts.setAside(id)

        // when
        drafts.forget(id)
        val restored = drafts.restore(id)

        // then
        Assert.assertFalse(restored)
        Assert.assertNull(drafts.draft.value)
    }
}
