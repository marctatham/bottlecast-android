package com.bottlecast.app

import org.junit.Assert
import org.junit.Test

/**
 * Pins each route's analytics screen name: the names predate the type-safe routes, and renaming
 * one would break every report built on it. Discover keeps the name of the find screen it replaced.
 */
class BottlingRouteTest {

    @Test
    fun `routes keep their analytics screen names`() {
        // given
        val expected = mapOf(
            Onboarding::class to "onboarding",
            Shore::class to "home",
            Chest::class to "chest",
            Me::class to "me",
            Discover::class to "get_messages",
            Reveal::class to "reveal",
            Compose::class to "post_message",
            ComposePreview::class to "compose_preview",
            CastSuccess::class to "cast_success",
            Keepsake::class to "keepsake",
            Moderation::class to "view_messages",
            Settings::class to "settings",
            HowItWorks::class to "how_it_works",
            Licences::class to "licences",
            Goodbye::class to "goodbye",
        )

        // when
        val actual = routeScreenNames

        // then
        Assert.assertEquals(expected, actual)
        Assert.assertEquals("reveal", Reveal("bottle00000000000001").screenName)
    }

    @Test
    fun `screen names are unique`() {
        // given
        val names = routeScreenNames.values.toList()

        // when
        val distinct = names.distinct()

        // then
        Assert.assertEquals(names, distinct)
    }
}
