package com.bottlecast.app.common.settings.data

import android.app.UiModeManager
import android.content.Context
import com.bottlecast.app.common.settings.domain.IThemeApplier
import com.bottlecast.app.common.settings.domain.ThemeChoice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Sets the app's own night mode (Android 12 and up), which the system keeps across restarts. The
 * resources follow it, so the window background and the splash match, and Compose's
 * isSystemInDarkTheme() reads it: nothing has to hold the first frame for a stored choice.
 */
class UiModeThemeApplier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : IThemeApplier {

    override fun apply(theme: ThemeChoice) {
        val mode = when (theme) {
            ThemeChoice.System -> UiModeManager.MODE_NIGHT_AUTO
            ThemeChoice.Night -> UiModeManager.MODE_NIGHT_YES
            ThemeChoice.Dawn -> UiModeManager.MODE_NIGHT_NO
        }
        context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(mode)
    }
}
