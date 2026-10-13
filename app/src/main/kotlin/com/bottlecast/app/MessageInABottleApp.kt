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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    navController.addOnDestinationChangedListener { _, destination, _ ->
        analyticsProvider.trackScreen(destination.route ?: "Unknown")
    }

    val viewModel: AppNavigationStateViewModel = hiltViewModel()
    val backHandler: () -> Unit = { navController.popBackStack() }
    val homeHandler: () -> Unit = {
        navController.navigate(Screen.HOME) {
            popUpTo(Screen.HOME) {
                inclusive = true // Pop up to the "home" destination, including "home" itself
            }
        }
    }

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
            startDestination = Screen.HOME,
            // keep content clear of the navigation bar, display cutouts & the keyboard,
            // and stop oversized backgrounds (e.g. CustomBox's circle) painting over the status bar
            modifier = Modifier
                .weight(1f)
                .clipToBounds()
                .consumeWindowInsets(WindowInsets.statusBars)
                .safeDrawingPadding(),
        ) {
            composable(Screen.HOME) {
                HomeScreen(
                    onGetMessageHandler = { navController.navigate(Screen.GET_MESSAGES) },
                    onPostMessageHandler = {
                        val destination: String = viewModel.onNavigateToPostMessage()
                        navController.navigate(destination)
                    },
                    onViewMessagesHandler = { navController.navigate(Screen.VIEW_MESSAGES) },
                )
            }
            composable(Screen.GET_MESSAGES) { GetMessageScreen(homeHandler) }
            composable(Screen.POST_MESSAGE_EDUCATIONAL) {
                LoginRequiredToPostScreen(
                    onProceedHandler = {
                        navController.popBackStack(Screen.POST_MESSAGE_EDUCATIONAL, inclusive = true)
                        navController.navigate(Screen.POST_MESSAGE)
                    },
                    onBackHandler = backHandler,
                    onCancelHandler = homeHandler
                )
            }
            composable(Screen.POST_MESSAGE) {
                PostMessageScreen(
                    onBackHandler = backHandler,
                    onHomeHandler = homeHandler,
                )
            }
            composable(Screen.VIEW_MESSAGES) { ViewMessagesScreen() }
        }
    }
}