package com.bottlecast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enforced from Android 15 onwards, opting in keeps older devices consistent
        enableEdgeToEdge()
        // the bars are transparent (spec §1.1): with 3-button navigation the system would otherwise
        // lay a scrim over the app's own navigation bar
        window.isNavigationBarContrastEnforced = false

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
