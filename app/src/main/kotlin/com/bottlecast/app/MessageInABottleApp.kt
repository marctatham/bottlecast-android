package com.bottlecast.app

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.getmessage.GetMessageScreen
import com.bottlecast.app.feature.home.HomeScreen
import com.bottlecast.app.feature.login.LoginRequiredToPostScreen
import com.bottlecast.app.feature.postmessage.ui.PostMessageScreen
import com.bottlecast.app.feature.viewmessages.ViewMessagesScreen

@Composable
fun MessageInABottleApp(analyticsProvider: ITracker) {
    val navController = rememberNavController()
    TrackScreens(navController, analyticsProvider)

    val viewModel: AppNavigationStateViewModel = hiltViewModel()
    val backHandler: () -> Unit = { navController.popBackStack() }
    // a pop rather than a fresh navigation, so the Shore keeps its state and the pop transitions run
    val homeHandler: () -> Unit = { navController.popBackStack<Shore>(inclusive = false) }

    // the status bar's icons contrast with the primary strip painted below: dark on Night's pale
    // sage, light on Dawn's deep sage
    val isNight = BottlingTheme.colors.isNight
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isNight
        }
    }

    // the app draws edge-to-edge, so the system bar areas are handled here once for every screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // keep the status bar painted in the primary colour, as it was before edge-to-edge
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.primary)
        )

        NavHost(
            navController = navController,
            startDestination = Shore,
            // keep content clear of the navigation bar, display cutouts & the keyboard,
            // and stop oversized backgrounds (e.g. CustomBox's circle) painting over the status bar
            modifier = Modifier
                .weight(1f)
                .clipToBounds()
                .consumeWindowInsets(WindowInsets.statusBars)
                .safeDrawingPadding(),
        ) {
            composable<Shore> {
                HomeScreen(
                    onGetMessageHandler = { navController.navigate(GetMessage) },
                    onPostMessageHandler = { navController.navigate(viewModel.onNavigateToPostMessage()) },
                    onViewMessagesHandler = { navController.navigate(Moderation) },
                )
            }
            composable<GetMessage> { GetMessageScreen(homeHandler) }
            composable<PostMessageLoginRequired> {
                LoginRequiredToPostScreen(
                    onProceedHandler = {
                        navController.popBackStack(PostMessageLoginRequired, inclusive = true)
                        navController.navigate(PostMessage)
                    },
                    onBackHandler = backHandler,
                    onCancelHandler = homeHandler
                )
            }
            composable<PostMessage> {
                PostMessageScreen(
                    onBackHandler = backHandler,
                    onHomeHandler = homeHandler,
                )
            }
            composable<Moderation> { ViewMessagesScreen() }
        }
    }
}

/**
 * Sends a screen view each time the destination changes. A repeat of the same destination (a
 * rotation, or re-selecting the current tab) isn't a new screen, so it's skipped.
 */
@Composable
private fun TrackScreens(navController: NavController, tracker: ITracker) {
    val currentTracker by rememberUpdatedState(tracker)
    var lastTrackedDestinationId by rememberSaveable { mutableIntStateOf(NO_DESTINATION) }
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.id != lastTrackedDestinationId) {
                lastTrackedDestinationId = destination.id
                currentTracker.trackScreen(destination.screenName())
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}

private const val NO_DESTINATION = 0
