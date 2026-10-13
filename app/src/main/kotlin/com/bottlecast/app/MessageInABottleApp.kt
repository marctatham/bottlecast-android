package com.bottlecast.app

import android.app.Activity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.components.TabFrame
import com.bottlecast.app.common.ui.BottlingMotion
import com.bottlecast.app.common.ui.BottlingTheme
import com.bottlecast.app.feature.chest.ChestScreen
import com.bottlecast.app.feature.getmessage.GetMessageScreen
import com.bottlecast.app.feature.login.LoginRequiredToPostScreen
import com.bottlecast.app.feature.me.MeScreen
import com.bottlecast.app.feature.postmessage.ui.PostMessageScreen
import com.bottlecast.app.feature.shore.ShoreScreen
import com.bottlecast.app.feature.viewmessages.ViewMessagesScreen

/**
 * The app's shell: one NavHost, drawn edge to edge. The tab roots (Shore, Chest, Me) each draw
 * themselves in a [TabFrame] with the navigation bar; every other route is immersive (spec §2).
 */
@Composable
fun MessageInABottleApp(analyticsProvider: ITracker) {
    val navController = rememberNavController()
    TrackScreens(navController, analyticsProvider)
    SystemBarAppearance()

    val viewModel: AppNavigationStateViewModel = hiltViewModel()
    val backHandler: () -> Unit = { navController.popBackStack() }
    // a pop rather than a fresh navigation, so the Shore keeps its state and the pop transitions run
    val homeHandler: () -> Unit = { navController.popBackStack<Shore>(inclusive = false) }
    // tabs keep their own state, and back from any tab leads to the Shore, then out (spec §2)
    val tabHandler: (TopLevelDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Shore,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(BottlingMotion.screen()) },
        exitTransition = { fadeOut(BottlingMotion.screen()) },
        popEnterTransition = { fadeIn(BottlingMotion.screen()) },
        popExitTransition = { fadeOut(BottlingMotion.screen()) },
        // a back swipe uses these instead of the pop transitions: the screen underneath shows as it
        // is, while a full-screen route shrinks away and a tab fades (spec §1.5)
        predictivePopEnterTransition = { EnterTransition.None },
        predictivePopExitTransition = {
            if (initialState.destination.isTopLevel()) fadeOut(BottlingMotion.screen()) else PredictiveBackExit
        },
    ) {
        // a tab root stays put under whatever opens over it, so it's already there, bar and all,
        // when a predictive back gesture reveals it
        composable<Shore>(exitTransition = { KeepTabRoot }, popEnterTransition = { EnterTransition.None }) {
            TabFrame(selected = TopLevelDestination.SHORE, onSelect = tabHandler) { contentPadding ->
                ShoreScreen(
                    onFindBottleHandler = { navController.navigate(GetMessage) },
                    onCastHandler = { navController.navigate(viewModel.onNavigateToPostMessage()) },
                    onModerationHandler = { navController.navigate(Moderation) },
                    onOpenChestHandler = { tabHandler(TopLevelDestination.CHEST) },
                    onOpenMeHandler = { tabHandler(TopLevelDestination.ME) },
                    contentPadding = contentPadding,
                )
            }
        }
        composable<Chest>(exitTransition = { KeepTabRoot }, popEnterTransition = { EnterTransition.None }) {
            TabFrame(selected = TopLevelDestination.CHEST, onSelect = tabHandler) { contentPadding ->
                ChestScreen(
                    onGoToShoreHandler = { tabHandler(TopLevelDestination.SHORE) },
                    contentPadding = contentPadding,
                )
            }
        }
        composable<Me>(exitTransition = { KeepTabRoot }, popEnterTransition = { EnterTransition.None }) {
            TabFrame(selected = TopLevelDestination.ME, onSelect = tabHandler) { contentPadding ->
                MeScreen(
                    onGoToShoreHandler = { tabHandler(TopLevelDestination.SHORE) },
                    contentPadding = contentPadding,
                )
            }
        }

        composable<GetMessage>(popExitTransition = { PredictiveBackExit }) {
            ImmersiveScreen { GetMessageScreen(homeHandler) }
        }
        composable<PostMessageLoginRequired>(
            // its one way forward replaces it with PostMessage, which fades in over it
            exitTransition = { ExitTransition.None },
            popExitTransition = { PredictiveBackExit },
        ) {
            ImmersiveScreen {
                LoginRequiredToPostScreen(
                    onProceedHandler = {
                        navController.popBackStack(PostMessageLoginRequired, inclusive = true)
                        navController.navigate(PostMessage)
                    },
                    onBackHandler = backHandler,
                    onCancelHandler = homeHandler
                )
            }
        }
        composable<PostMessage>(popExitTransition = { PredictiveBackExit }) {
            ImmersiveScreen {
                PostMessageScreen(
                    onBackHandler = backHandler,
                    onHomeHandler = homeHandler,
                )
            }
        }
        composable<Moderation>(popExitTransition = { PredictiveBackExit }) {
            ImmersiveScreen { ViewMessagesScreen() }
        }
    }
}

/**
 * An immersive screen that predates the edge-to-edge redesign. It keeps its old layout by sitting
 * inside the system bars, cutouts and keyboard, over an opaque background, and it gains rounded
 * corners as a predictive back gesture shrinks it away (spec §1.5).
 */
@Composable
private fun AnimatedContentScope.ImmersiveScreen(content: @Composable () -> Unit) {
    val corner by transition.animateDp(
        transitionSpec = { PredictiveBackSpec() },
        label = "predictive back corner",
    ) { state -> if (state == EnterExitState.PostExit) PREDICTIVE_BACK_CORNER else 0.dp }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shape = RoundedCornerShape(corner)
                clip = true
            }
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            // stops oversized backgrounds (CustomBox's circle) painting into the system bars
            .clipToBounds()
    ) { content() }
}

/**
 * The status and navigation bars are transparent, so their icons follow the theme: light on
 * Night, dark on Dawn (spec §1.1).
 */
@Composable
private fun SystemBarAppearance() {
    val isNight = BottlingTheme.colors.isNight
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isNight
                isAppearanceLightNavigationBars = !isNight
            }
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

// a tab root stays fully visible for as long as the screen opening over it takes to fade in
private val KeepTabRoot: ExitTransition = fadeOut(BottlingMotion.screen(), targetAlpha = 1f)

// a full-screen route shrinks to 90% as back is swiped (spec §1.5), following the finger, and
// fades once it's let go
private fun <T> PredictiveBackSpec() = tween<T>(BottlingMotion.SCREEN_MILLIS, easing = LinearEasing)

private val PredictiveBackExit: ExitTransition =
    scaleOut(targetScale = PREDICTIVE_BACK_SCALE, animationSpec = PredictiveBackSpec()) +
        fadeOut(tween(PREDICTIVE_BACK_FADE_MILLIS, delayMillis = BottlingMotion.SCREEN_MILLIS - PREDICTIVE_BACK_FADE_MILLIS))

private const val PREDICTIVE_BACK_SCALE = 0.9f
private const val PREDICTIVE_BACK_FADE_MILLIS = 80
private val PREDICTIVE_BACK_CORNER = 32.dp
private const val NO_DESTINATION = 0
