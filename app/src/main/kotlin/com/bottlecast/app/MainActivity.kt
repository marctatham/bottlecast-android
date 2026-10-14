package com.bottlecast.app

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.analytics.LocalTracker
import com.bottlecast.app.common.ui.BottlingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var analyticsProvider: ITracker

    // the same one the NavHost asks for, which holds where the app starts
    private val navigationState: AppNavigationStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            BottlingTheme {
                // ensure that the analytics provider as has been configured
                // via Dagger Hilt is made available to the entire app
                CompositionLocalProvider(LocalTracker provides analyticsProvider) {
                    MessageInABottleApp(analyticsProvider)
                }
            }
        }
    }
}
