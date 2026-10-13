package com.bottlecast.app

import com.bottlecast.app.Chest
import com.bottlecast.app.GetMessage
import com.bottlecast.app.Me
import com.bottlecast.app.Moderation
import com.bottlecast.app.PostMessage
import com.bottlecast.app.PostMessageLoginRequired
import com.bottlecast.app.Shore
import com.bottlecast.app.bottlingRoutes
import org.junit.Assert
import org.junit.Test

/**
 * Pins each route's analytics screen name: the names predate the type-safe routes, and renaming
 * one would break every report built on it.
 */
class BottlingRouteTest {

    @Test
    fun `routes keep their analytics screen names`() {
        // given
        val expected = mapOf(
            Shore to "home",
            Chest to "chest",
            Me to "me",
            GetMessage to "get_messages",
            PostMessage to "post_message",
            PostMessageLoginRequired to "post_message_login_required",
            Moderation to "view_messages",
        )

        // when
        val actual = bottlingRoutes.associateWith { it.screenName }

        // then
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `screen names are unique`() {
        // given
        val names = bottlingRoutes.map { it.screenName }

        // when
        val distinct = names.distinct()

        // then
        Assert.assertEquals(names, distinct)
    }
}
