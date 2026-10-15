package com.bottlecast.app

import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * The app's type-safe routes (spec §2). Each carries the stable snake_case name that screen
 * analytics send: `destination.route` would be a qualified class name, so renaming a route must
 * never rename its screen.
 */
sealed interface BottlingRoute {
    val screenName: String
}

/** First-launch onboarding (spec §3.1), the start until it's been seen. */
@Serializable
data object Onboarding : BottlingRoute {
    override val screenName: String get() = "onboarding"
}

/** The Shore, the app's home and first tab. */
@Serializable
data object Shore : BottlingRoute {
    override val screenName: String get() = "home"
}

/** The Chest tab. */
@Serializable
data object Chest : BottlingRoute {
    override val screenName: String get() = "chest"
}

/** The Me tab. */
@Serializable
data object Me : BottlingRoute {
    override val screenName: String get() = "me"
}

/**
 * Discover: uncorking today's bottle (spec §3.6). It keeps the name of the find screen it
 * replaced, so analytics carry on across the redesign.
 */
@Serializable
data object Discover : BottlingRoute {
    override val screenName: String get() = "get_messages"
}

/**
 * Reveal: a found bottle's message (spec §3.7). [fresh] when it has just been uncorked, so the
 * moonlight sweeps across it once; reopened from the Shore, it shows still.
 */
@Serializable
data class Reveal(val messageId: String, val fresh: Boolean = false) : BottlingRoute {
    override val screenName: String get() = SCREEN_NAME

    companion object {
        const val SCREEN_NAME = "reveal"
    }
}

/**
 * Compose: writing a bottle (spec §3.4). It keeps the name of the post screen it replaced. When
 * [justSignedIn], it offers a pen name.
 */
@Serializable
data class Compose(val justSignedIn: Boolean = false) : BottlingRoute {
    override val screenName: String get() = SCREEN_NAME

    companion object {
        const val SCREEN_NAME = "post_message"
    }
}

/** Preview: the bottle as it will wash up, and the writer's consent (spec §3.4b). */
@Serializable
data object ComposePreview : BottlingRoute {
    override val screenName: String get() = "compose_preview"
}

/** The cast and its success (spec §3.5). [queued] while it waits for a connection or the server. */
@Serializable
data class CastSuccess(val messageId: String, val queued: Boolean, val sealable: Boolean) : BottlingRoute {
    override val screenName: String get() = SCREEN_NAME

    companion object {
        const val SCREEN_NAME = "cast_success"
    }
}

/** Keepsake detail: one bottle from the chest, found or cast (spec §3.11). */
@Serializable
data class Keepsake(val messageId: String) : BottlingRoute {
    override val screenName: String get() = SCREEN_NAME

    companion object {
        const val SCREEN_NAME = "keepsake"
    }
}

/** The admin's list of reported and pulled bottles (spec §3.3: "Moderation"). */
@Serializable
data object Moderation : BottlingRoute {
    override val screenName: String get() = "view_messages"
}

/** The navigation bar's tabs, in order (spec §2). Other routes are immersive and hide the bar. */
enum class TopLevelDestination(val route: BottlingRoute, @StringRes val label: Int) {
    SHORE(Shore, R.string.nav_shore),
    CHEST(Chest, R.string.nav_chest),
    ME(Me, R.string.nav_me),
}

/**
 * Every route's analytics name, by its class, so a destination can be matched back to its route.
 * A route with arguments has no instance to ask, so the names are listed here.
 */
val routeScreenNames: Map<KClass<out BottlingRoute>, String> = mapOf(
    Onboarding::class to Onboarding.screenName,
    Shore::class to Shore.screenName,
    Chest::class to Chest.screenName,
    Me::class to Me.screenName,
    Discover::class to Discover.screenName,
    Reveal::class to Reveal.SCREEN_NAME,
    Compose::class to Compose.SCREEN_NAME,
    ComposePreview::class to ComposePreview.screenName,
    CastSuccess::class to CastSuccess.SCREEN_NAME,
    Keepsake::class to Keepsake.SCREEN_NAME,
    Moderation::class to Moderation.screenName,
)

/** True for the tab roots, which show the navigation bar; every other route is immersive. */
fun NavDestination.isTopLevel(): Boolean = TopLevelDestination.entries.any { hasRoute(it.route::class) }

/** The analytics name of the route this destination was built from. */
fun NavDestination.screenName(): String =
    routeScreenNames.entries.firstOrNull { (route, _) -> hasRoute(route) }?.value ?: UNKNOWN_SCREEN_NAME

internal const val UNKNOWN_SCREEN_NAME = "unknown"
