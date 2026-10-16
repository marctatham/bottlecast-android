package com.bottlecast.app.common.messages

import com.bottlecast.app.common.messages.domain.MessageLimits
import com.bottlecast.app.common.messages.domain.characterCount
import com.bottlecast.app.common.messages.domain.fitEditToCast
import com.bottlecast.app.common.messages.domain.fitToCast
import com.bottlecast.app.common.messages.domain.fitsCast
import com.bottlecast.app.common.messages.domain.newMessageId
import com.bottlecast.app.common.messages.domain.utf8Size
import org.junit.Assert
import org.junit.Test
import kotlin.random.Random

/** A bottle holds 240 characters or 280 UTF-8 bytes, whichever runs out first (spec §3.4). */
class MessageLimitsTest {

    @Test
    fun `text within both limits is left alone`() {
        // given
        val text = "a".repeat(MessageLimits.MAX_CHARACTERS)

        // when
        val result = text.fitToCast()

        // then
        Assert.assertEquals(text, result)
        Assert.assertTrue(text.fitsCast())
    }

    @Test
    fun `plain text runs out of characters first`() {
        // given
        val text = "a".repeat(MessageLimits.MAX_CHARACTERS + 50)

        // when
        val result = text.fitToCast()

        // then
        Assert.assertEquals(MessageLimits.MAX_CHARACTERS, result.characterCount())
    }

    @Test
    fun `emoji run out of bytes first, and are never split`() {
        // given: 🌊 is 4 bytes and two UTF-16 chars
        val waves = "🌊".repeat(100)

        // when
        val result = waves.fitToCast()

        // then
        Assert.assertEquals(MessageLimits.MAX_BYTES / 4, result.characterCount())
        Assert.assertTrue(result.utf8Size() <= MessageLimits.MAX_BYTES)
        Assert.assertEquals(0, result.length % 2)
    }

    @Test
    fun `an emoji counts as one character`() {
        // given
        val text = "Hi 🌊"

        // when
        val count = text.characterCount()

        // then
        Assert.assertEquals(4, count)
    }

    @Test
    fun `an edit within the limits is left alone`() {
        // given
        val previous = "Hello from the sea"

        // when
        val result = "Hello, from the sea".fitEditToCast(previous)

        // then
        Assert.assertEquals("Hello, from the sea", result)
    }

    @Test
    fun `a paste in the middle of a long message keeps the message's end and shortens the paste`() {
        // given
        val previous = "start " + "x".repeat(220) + " end."
        val paste = "PASTE".repeat(10)

        // when
        val result = ("start " + paste + previous.removePrefix("start ")).fitEditToCast(previous)

        // then
        Assert.assertEquals(MessageLimits.MAX_CHARACTERS, result.characterCount())
        Assert.assertTrue(result.endsWith("x end."))
        Assert.assertEquals(previous, result.replaceFirst(Regex("^start P[PASTE]*"), "start "))
    }

    @Test
    fun `typing in the middle of a message at the limit changes nothing`() {
        // given
        val previous = "a".repeat(MessageLimits.MAX_CHARACTERS)

        // when
        val result = (previous.take(100) + "b" + previous.drop(100)).fitEditToCast(previous)

        // then
        Assert.assertEquals(previous, result)
    }

    @Test
    fun `a new bottle's id is 20 letters and digits, as firestore rules expect`() {
        // given
        val random = Random(42)

        // when
        val ids = List(50) { newMessageId(random) }

        // then
        ids.forEach { Assert.assertTrue(it, it.matches(Regex("^[A-Za-z0-9]{20}$"))) }
        Assert.assertEquals(ids.size, ids.toSet().size)
    }
}
