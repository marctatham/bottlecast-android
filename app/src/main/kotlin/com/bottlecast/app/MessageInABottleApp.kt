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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
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
    val viewModel: AppNavigationStateViewModel = hiltViewModel()
    TrackScreens(navController, analyticsProvider, viewModel)
    SystemBarAppearance()

    // a second tap while the screen is still closing would otherwise pop the Shore itself and
    // leave nothing on screen
    val backHandler: () -> Unit = {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }
    // a pop rather than a fresh navigation, so the Shore keeps its state and the pop transitions run
    val homeHandler: () -> Unit = { navController.popBackStack<Shore>(inclusive = false) }
    // tabs keep their own state, and back from any tab leads to the Shore, then out (spec §2). A
    // full-screen route belongs to no tab, so leaving one for a tab drops it rather than saving it
    // with the Shore's: saved, it would reopen the next time the Shore tab was chosen. It's one
    // navigation either way, so only the chosen tab is counted as a screen view
    val tabHandler: (TopLevelDestination) -> Unit = { destination ->
        val fromTabRoot = navController.currentDestination?.isTopLevel() == true
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = fromTabRoot }
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
        // a tab root stays put under whatever opens over it, so it's already in place, bar and
        // all, when that screen closes again
        exitTransition = {
            if (initialState.destination.isTopLevel()) {
                ExitTransition.KeepUntilTransitionsFinished
            } else {
                fadeOut(BottlingMotion.screen())
            }
        },
        // closing a screen fades it away over the one already underneath
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { fadeOut(BottlingMotion.screen()) },
        // a back swipe follows the finger: a full-screen route shrinks away with rounded corners
        // (spec §1.5), while a tab fades back to the Shore
        predictivePopEnterTransition = { EnterTransition.None },
        predictivePopExitTransition = {
            if (initialState.destination.isTopLevel()) fadeOut(BottlingMotion.screen()) else PredictiveBackExit
        },
    ) {
        composable<Shore> {
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
        composable<Chest> {
            TabFrame(selected = TopLevelDestination.CHEST, onSelect = tabHandler) { contentPadding ->
                ChestScreen(
                    onGoToShoreHandler = { tabHandler(TopLevelDestination.SHORE) },
                    contentPadding = contentPadding,
                )
            }
        }
        composable<Me> {
            TabFrame(selected = TopLevelDestination.ME, onSelect = tabHandler) { contentPadding ->
                MeScreen(
                    onGoToShoreHandler = { tabHandler(TopLevelDestination.SHORE) },
                    contentPadding = contentPadding,
                )
            }
        }

        immersive<GetMessage> {
            LegacyScreen { GetMessageScreen(homeHandler) }
        }
        immersive<PostMessageLoginRequired> {
            LegacyScreen {
                LoginRequiredToPostScreen(
                    // one step, so the Shore never becomes current in between
                    onProceedHandler = {
                        navController.navigate(PostMessage) {
                            popUpTo<PostMessageLoginRequired> { inclusive = true }
                        }
                    },
                    onBackHandler = backHandler,
                    onCancelHandler = homeHandler
                )
            }
        }
        immersive<PostMessage> {
            LegacyScreen {
                PostMessageScreen(
                    onBackHandler = backHandler,
                    onHomeHandler = homeHandler,
                )
            }
        }
        immersive<Moderation> {
            LegacyScreen { ViewMessagesScreen() }
        }
    }
}

/** A full-screen route, which hides the navigation bar (spec §2), in an [ImmersiveFrame]. */
private inline fun <reified T : Any> NavGraphBuilder.immersive(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        val scope = this
        ImmersiveFrame { scope.content(entry) }
    }
}

/**
 * A full-screen route's frame. It gains rounded corners as it closes, so a back swipe shrinks it
 * away like a card (spec §1.5). It also takes every tap that lands on it: the screen underneath
 * stays drawn while this one opens and closes over it, and a tap that fell through would act on
 * that screen, such as opening a second screen or switching tabs.
 */
@Composable
private fun AnimatedContentScope.ImmersiveFrame(content: @Composable () -> Unit) {
    val cornerRadius = BottlingTheme.sizes.predictiveBackCorner
    val corner by transition.animateDp(
        transitionSpec = { tween(BottlingMotion.SCREEN_MILLIS, easing = LinearEasing) },
        label = "predictive back corner",
    ) { state -> if (state == EnterExitState.PostExit) cornerRadius else 0.dp }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                shape = RoundedCornerShape(corner)
                clip = true
            }
            .pointerInput(Unit) {}
    ) { content() }
}

/**
 * A screen from before the edge-to-edge redesign. It keeps its old layout inside the system bars,
 * cutouts and keyboard, over an opaque background.
 */
@Composable
private fun LegacyScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
private fun TrackScreens(navController: NavController, tracker: ITracker, viewModel: AppNavigationStateViewModel) {
    val currentTracker by rememberUpdatedState(tracker)
    DisposableEffect(navController, viewModel) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.id != viewModel.lastTrackedDestinationId) {
                viewModel.lastTrackedDestinationId = destination.id
                currentTracker.trackScreen(destination.screenName())
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}

// a full-screen route shrinks to 90% as back is swiped (spec §1.5), following the finger, and
// fades in the last moment, once the gesture is let go
private val PredictiveBackExit: ExitTransition =
    scaleOut(
        targetScale = PREDICTIVE_BACK_SCALE,
        animationSpec = tween(BottlingMotion.SCREEN_MILLIS, easing = LinearEasing),
    ) + fadeOut(tween(PREDICTIVE_BACK_FADE_MILLIS, delayMillis = BottlingMotion.SCREEN_MILLIS - PREDICTIVE_BACK_FADE_MILLIS))

private const val PREDICTIVE_BACK_SCALE = 0.9f
private const val PREDICTIVE_BACK_FADE_MILLIS = 80
