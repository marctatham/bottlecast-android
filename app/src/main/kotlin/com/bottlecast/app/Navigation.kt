package com.bottlecast.app

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

/**
 * The app's type-safe routes (spec §2). Each carries the stable snake_case name that screen
 * analytics send: `destination.route` would be a qualified class name, so renaming a route must
 * never rename its screen.
 */
sealed interface BottlingRoute {
    val screenName: String
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

/** Today's discover screen, until slice 2's Discover and Reveal replace it. */
@Serializable
data object GetMessage : BottlingRoute {
    override val screenName: String get() = "get_messages"
}

/** Today's post screen, until slice 3's Compose replaces it. */
@Serializable
data object PostMessage : BottlingRoute {
    override val screenName: String get() = "post_message"
}

/** The sign-in explainer shown before posting while signed out. */
@Serializable
data object PostMessageLoginRequired : BottlingRoute {
    override val screenName: String get() = "post_message_login_required"
}

/** The admin's list of every message (spec §3.3: "Moderation"). */
@Serializable
data object Moderation : BottlingRoute {
    override val screenName: String get() = "view_messages"
}

/** Every route, so a destination can be matched back to its route. */
val bottlingRoutes: List<BottlingRoute> = listOf(
    Shore,
    Chest,
    Me,
    GetMessage,
    PostMessage,
    PostMessageLoginRequired,
    Moderation,
)

/** The analytics name of the route this destination was built from. */
fun NavDestination.screenName(): String =
    bottlingRoutes.firstOrNull { hasRoute(it::class) }?.screenName ?: UNKNOWN_SCREEN_NAME

internal const val UNKNOWN_SCREEN_NAME = "unknown"
