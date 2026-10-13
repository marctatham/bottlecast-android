package com.bottlecast.app.main.postmessage

import com.bottlecast.app.feature.postmessage.domain.MAXIMUM_MESSAGE_BYTES
import com.bottlecast.app.feature.postmessage.domain.fitToMessageLimit
import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [fitToMessageLimit], which keeps a cast within what firestore.rules accepts.
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
}
