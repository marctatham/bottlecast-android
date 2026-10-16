package com.bottlecast.app.common.preferences

import com.bottlecast.app.testing.filePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class OnboardingStoreTest {

    @Test
    fun `onboarding is unseen until it's marked seen`() = runTest {
        // given
        val store = OnboardingStore(filePreferences(backgroundScope), backgroundScope)
        Assert.assertFalse(store.seen.first())

        // when
        store.markSeen().join()

        // then
        Assert.assertTrue(store.seen.first())
    }
}
