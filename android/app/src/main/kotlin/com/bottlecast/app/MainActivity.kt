package com.bottlecast.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.reminders.data.EXTRA_OPEN
import com.bottlecast.app.common.reminders.data.OPEN_DISCOVER
import com.bottlecast.app.common.reminders.data.OPEN_SHORE
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.common.ui.FeelPreferences
import com.bottlecast.app.common.ui.LocalFeelPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var analyticsProvider: ITracker

    @Inject
    lateinit var settingsStore: ISettingsStore

    // the same one the NavHost asks for, which holds where the app starts
    private val navigationState: AppNavigationStateViewModel by viewModels()

    // where the daily reminder asked the app to go (spec §2, flow 7), until it has
    private var launchTarget by mutableStateOf<LaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) takeLaunch(intent)

        // enforced from Android 15 onwards, opting in keeps older devices consistent
        enableEdgeToEdge()
        // the bars are transparent (spec §1.1): with 3-button navigation the system would otherwise
        // lay a scrim over the app's own navigation bar
        window.isNavigationBarContrastEnforced = false

        // The first frame waits, under the platform's splash, until the start destination is known,
        // so neither onboarding nor the Shore flashes up before the other.
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val ready = navigationState.startDestination.value != null
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })

        setContent {
            // the theme itself follows the app's night mode, which Settings sets (UiModeThemeApplier)
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val feel = FeelPreferences(
                reduceMotion = settings.reduceMotion,
                haptics = settings.haptics,
                shakeToUncork = settings.shakeToUncork,
            )
            BottlingTheme {
                // ensure that the analytics provider as has been configured
                // via Dagger Hilt is made available to the entire app
                CompositionLocalProvider(LocalTracker provides analyticsProvider, LocalFeelPreferences provides feel) {
                    MessageInABottleApp(
                        analyticsProvider = analyticsProvider,
                        launchTarget = launchTarget,
                        onLaunchTargetOpened = { launchTarget = null },
                    )
                }
            }
        }
    }

    // a notification tapped while the app is open comes back here (singleTop)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeLaunch(intent)
    }

    // Recents relaunches a task with the intent that started it, which isn't a tap on a notification
    private fun takeLaunch(intent: Intent?) {
        if (intent == null || (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        when (intent.getStringExtra(EXTRA_OPEN)) {
            OPEN_SHORE -> launchTarget = LaunchTarget.Shore
            OPEN_DISCOVER -> launchTarget = LaunchTarget.Discover
        }
    }
}
