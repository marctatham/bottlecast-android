package com.bottlecast.app.common.messages

import com.bottlecast.app.common.messages.data.mapper.MessageDataModelMapper
import com.bottlecast.app.common.messages.domain.MessageEntity
import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [MessageDataModelMapper].
 */
class MessageDataModelMapperTest {

    private val mapper = MessageDataModelMapper()

    @Test
    fun `when document holds a message, it is mapped to an entity`() {
        // given
        val document = mapOf("messageContent" to "Hello from the sea", "userId" to "user-1")

        // when
        val result: MessageEntity? = mapper.mapFrom(document)

        // then
        Assert.assertEquals(MessageEntity("Hello from the sea", "user-1"), result)
    }

    @Test
    fun `when document fields were obfuscated by R8, no message is mapped`() {
        // given
        val document = mapOf("a" to "Hello from the sea", "b" to "user-1")

        // when
        val result: MessageEntity? = mapper.mapFrom(document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when message content is blank, no message is mapped`() {
        // given
        val document = mapOf("messageContent" to "   ", "userId" to "user-1")

        // when
        val result: MessageEntity? = mapper.mapFrom(document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when message content is not a string, no message is mapped`() {
        // given
        val document = mapOf("messageContent" to 42L, "userId" to "user-1")

        // when
        val result: MessageEntity? = mapper.mapFrom(document)

        // then
        Assert.assertNull(result)
    }

    @Test
    fun `when user id is missing, the message is still mapped`() {
        // given
        val document = mapOf("messageContent" to "Hello from the sea")

        // when
        val result: MessageEntity? = mapper.mapFrom(document)

        // then
        Assert.assertEquals(MessageEntity("Hello from the sea", ""), result)
    }
}
