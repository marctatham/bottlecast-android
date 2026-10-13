package com.bottlecast.app.common.messages

import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.google.firebase.Timestamp
import org.junit.Assert
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for [MessageDataModelMapper].
 */
class MessageDataModelMapperTest {

    private val mapper = MessageDataModelMapper()

    private val created = Instant.parse("2026-10-10T08:30:00Z")
    private val found = Instant.parse("2026-10-12T21:00:00Z")

    private val bottle = mapOf(
        "id" to "bottle1",
        "messageContent" to "Hello from the sea",
        "userId" to "user-1",
        "createdAt" to Timestamp(created),
        "lang" to "es",
        "status" to "found",
        "rand" to 0.42,
        "penName" to "Driftwood",
        "sealable" to true,
        "reportCount" to 1L,
        "findCount" to 2L,
        "foundAt" to Timestamp(found),
    )

    @Test
    fun `when document is a bottle in the slice 2 shape, every field is mapped`() {
        // given
        val document = bottle

        // when
        val result: MessageEntity? = mapper.mapFrom("bottle1", document)

        // then
        val expected = MessageEntity(
            id = "bottle1",
            message = "Hello from the sea",
            userId = "user-1",
            createdAt = created,
            lang = "es",
            status = MessageStatus.Found,
            penName = "Driftwood",
            sealable = true,
            reportCount = 1,
            findCount = 2,
            foundAt = found,
        )
        Assert.assertEquals(expected, result)
    }

    @Test
    fun `when document is drifting, it has no hold and no pen name`() {
        // given
        val document = bottle + mapOf("status" to "afloat", "foundAt" to null, "penName" to null)

        // when
        val result: MessageEntity? = mapper.mapFrom("bottle1", document)

        // then
        Assert.assertEquals(MessageStatus.Afloat, result?.status)
        Assert.assertNull(result?.foundAt)
        Assert.assertNull(result?.penName)
    }

    @Test
    fun `when document is an old bottle that hasn't been migrated, no message is mapped`() {
        // given
        val document = mapOf("messageContent" to "Hello from the sea", "userId" to "user-1")

        // when
        val result: MessageEntity? = mapper.mapFrom("old", document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when document has a status the app doesn't know, no message is mapped`() {
        // given
        val document = bottle + ("status" to "kept-somewhere-new")

        // when
        val result: MessageEntity? = mapper.mapFrom("bottle1", document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when document fields were obfuscated by R8, no message is mapped`() {
        // given
        val document = mapOf("a" to "Hello from the sea", "b" to "user-1")

        // when
        val result: MessageEntity? = mapper.mapFrom("obfuscated", document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when message content is blank, no message is mapped`() {
        // given
        val document = bottle + ("messageContent" to "   ")

        // when
        val result: MessageEntity? = mapper.mapFrom("bottle1", document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when message content is not a string, no message is mapped`() {
        // given
        val document = bottle + ("messageContent" to 42L)

        // when
        val result: MessageEntity? = mapper.mapFrom("bottle1", document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when user id is missing, the message is still mapped`() {
        // given
        val document = bottle - "userId"

        // when
        val result: MessageEntity? = mapper.mapFrom("bottle1", document)

        // then
        Assert.assertEquals("", result?.userId)
    }
}
