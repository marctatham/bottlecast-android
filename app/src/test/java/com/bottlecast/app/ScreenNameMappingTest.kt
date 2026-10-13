package com.bottlecast.app

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import com.bottlecast.app.Chest
import com.bottlecast.app.GetMessage
import com.bottlecast.app.Me
import com.bottlecast.app.Moderation
import com.bottlecast.app.PostMessage
import com.bottlecast.app.PostMessageLoginRequired
import com.bottlecast.app.Shore
import com.bottlecast.app.UNKNOWN_SCREEN_NAME
import com.bottlecast.app.bottlingRoutes
import com.bottlecast.app.screenName
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
            composable<Shore> {}
            composable<Chest> {}
            composable<Me> {}
            composable<GetMessage> {}
            composable<PostMessage> {}
            composable<PostMessageLoginRequired> {}
            composable<Moderation> {}
        }

        // when
        val screenNames = graph.map { it.screenName() }

        // then
        Assert.assertFalse(screenNames.contains(UNKNOWN_SCREEN_NAME))
        Assert.assertEquals(bottlingRoutes.map { it.screenName }.toSet(), screenNames.toSet())
    }
}
