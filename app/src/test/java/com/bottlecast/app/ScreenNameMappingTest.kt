package com.bottlecast.app

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that [screenName] finds the route behind each destination of a real type-safe graph, so
 * screen analytics never fall back to "unknown".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class)
class ScreenNameMappingTest {

    @Test
    fun `every destination maps to its route's screen name`() {
        // given
        val navController = NavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        val graph = navController.createGraph(startDestination = Shore) {
            composable<Onboarding> {}
            composable<Shore> {}
            composable<Chest> {}
            composable<Me> {}
            composable<Discover> {}
            composable<Reveal> {}
            composable<Compose> {}
            composable<ComposePreview> {}
            composable<CastSuccess> {}
            composable<Keepsake> {}
            composable<Moderation> {}
            composable<Settings> {}
            composable<HowItWorks> {}
            composable<Licences> {}
            composable<Goodbye> {}
        }

        // when
        val screenNames = graph.map { it.screenName() }

        // then
        Assert.assertFalse(screenNames.contains(UNKNOWN_SCREEN_NAME))
        Assert.assertEquals(routeScreenNames.values.toSet(), screenNames.toSet())
    }
}
