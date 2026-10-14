package com.bottlecast.app.main.postmessage

import com.bottlecast.app.feature.postmessage.domain.MAXIMUM_MESSAGE_BYTES
import com.bottlecast.app.feature.postmessage.domain.fitEditToMessageLimit
import com.bottlecast.app.feature.postmessage.domain.fitToMessageLimit
import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [fitToMessageLimit] and [fitEditToMessageLimit], which keep a cast within what
 * firestore.rules accepts.
 */
class MessageLimitTest {

    @Test
    fun `text within the limit is left alone`() {
        // given
        val text = "a".repeat(MAXIMUM_MESSAGE_BYTES)

        // when
        val result = text.fitToMessageLimit()

        // then
        Assert.assertEquals(text, result)
    }

    @Test
    fun `text over the limit is cut to fit`() {
        // given
        val text = "a".repeat(MAXIMUM_MESSAGE_BYTES + 50)

        // when
        val result = text.fitToMessageLimit()

        // then
        Assert.assertEquals(MAXIMUM_MESSAGE_BYTES, result.length)
    }

    @Test
    fun `multi-byte characters count by their UTF-8 size and are never split`() {
        // given: € is 3 bytes, and 🌊 is 4 bytes and two UTF-16 chars
        val euros = "€".repeat(400)
        val waves = "🌊".repeat(300)

        // when
        val fittedEuros = euros.fitToMessageLimit()
        val fittedWaves = waves.fitToMessageLimit()

        // then
        Assert.assertEquals(333, fittedEuros.length)
        Assert.assertEquals(250, fittedWaves.codePointCount(0, fittedWaves.length))
        Assert.assertTrue(fittedWaves.encodeToByteArray().size <= MAXIMUM_MESSAGE_BYTES)
    }

    @Test
    fun `an edit within the limit is left alone`() {
        // given
        val previous = "Hello from the sea"

        // when
        val result = "Hello, from the sea".fitEditToMessageLimit(previous)

        // then
        Assert.assertEquals("Hello, from the sea", result)
    }

    @Test
    fun `a paste in the middle of a long message keeps the message's end and shortens the paste`() {
        // given
        val previous = "start " + "x".repeat(980) + " end."
        val paste = "PASTE".repeat(10)

        // when
        val result = ("start " + paste + previous.removePrefix("start ")).fitEditToMessageLimit(previous)

        // then
        Assert.assertEquals(MAXIMUM_MESSAGE_BYTES, result.encodeToByteArray().size)
        Assert.assertTrue(result.endsWith("x end."))
        Assert.assertEquals(previous, result.replaceFirst(Regex("^start P[PASTE]*"), "start "))
    }

    @Test
    fun `typing in the middle of a message at the limit changes nothing`() {
        // given
        val previous = "a".repeat(MAXIMUM_MESSAGE_BYTES)

        // when
        val result = (previous.take(500) + "b" + previous.drop(500)).fitEditToMessageLimit(previous)

        // then
        Assert.assertEquals(previous, result)
    }
}
