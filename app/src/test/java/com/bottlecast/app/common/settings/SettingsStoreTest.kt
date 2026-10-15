package com.bottlecast.app.common.settings

import com.bottlecast.app.common.settings.data.SettingsStore
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ThemeChoice
import com.bottlecast.app.testing.filePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/** Unit tests for [SettingsStore], on a real file. */
class SettingsStoreTest {

    @Test
    fun `the defaults follow the system, with haptics and shaking on and pen names off`() = runTest {
        // given
        val store = SettingsStore(filePreferences(backgroundScope), backgroundScope)

        // when
        val settings = store.settings.first()

        // then
        Assert.assertEquals(AppSettings(), settings)
    }

    @Test
    fun `every setting is kept`() = runTest {
        // given
        val store = SettingsStore(filePreferences(backgroundScope), backgroundScope)

        // when
        store.setTheme(ThemeChoice.Dawn).join()
        store.setReduceMotion(true).join()
        store.setHaptics(false).join()
        store.setShakeToUncork(false).join()
        store.setSignWithPenName(true).join()

        // then
        Assert.assertEquals(
            AppSettings(theme = ThemeChoice.Dawn, reduceMotion = true, haptics = false, shakeToUncork = false, signWithPenName = true),
            store.settings.first(),
        )
    }
}
