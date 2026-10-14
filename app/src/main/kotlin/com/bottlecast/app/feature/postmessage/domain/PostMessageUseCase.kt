package com.bottlecast.app.feature.postmessage.domain

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.messages.domain.IMessageRepository
import com.bottlecast.app.common.messages.domain.MessageEntity
import timber.log.Timber
import javax.inject.Inject

const val MINIMUM_MESSAGE_LENGTH: Int = 10

/** The most text firestore.rules accepts in a bottle, in UTF-8 bytes. Slice 3's Compose lowers it. */
const val MAXIMUM_MESSAGE_BYTES: Int = 1000

/** The longest start of this text that fits in [MAXIMUM_MESSAGE_BYTES] of UTF-8. */
fun String.fitToMessageLimit(): String = fitToBytes(MAXIMUM_MESSAGE_BYTES)

/**
 * This text, an edit of [previous], cut to fit [MAXIMUM_MESSAGE_BYTES] of UTF-8 by shortening only
 * what the edit inserted. The rest of the message is never lost: a paste in the middle of a long
 * message keeps the message's end, and loses the end of the paste instead.
 */
fun String.fitEditToMessageLimit(previous: String): String {
    if (encodeToByteArray().size <= MAXIMUM_MESSAGE_BYTES) return this
    // both keep surrogate pairs whole
    val prefix = commonPrefixWith(previous).length
    val suffix = minOf(commonSuffixWith(previous).length, length - prefix, previous.length - prefix)
    val kept = substring(0, prefix) + substring(length - suffix)
    val room = MAXIMUM_MESSAGE_BYTES - kept.encodeToByteArray().size
    if (room <= 0) return kept.fitToMessageLimit()
    return substring(0, prefix) + substring(prefix, length - suffix).fitToBytes(room) + substring(length - suffix)
}

// The longest start of this text that fits in [maxBytes] of UTF-8, never splitting a character.
private fun String.fitToBytes(maxBytes: Int): String {
    if (encodeToByteArray().size <= maxBytes) return this
    var bytes = 0
    var end = 0
    while (end < length) {
        val codePoint = codePointAt(end)
        val size = when {
            codePoint < 0x80 -> 1
            codePoint < 0x800 -> 2
            codePoint < 0x10000 -> 3
            else -> 4
        }
        if (bytes + size > maxBytes) break
        bytes += size
        end += Character.charCount(codePoint)
    }
    return substring(0, end)
}

class PostMessageUseCase @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
    private val messageRepository: IMessageRepository
) {

    suspend fun postMessage(message: String): PostMessageResult {
        Timber.i("[postMessage] Initiating post of message to datasource")

        val trimmedMessage = message.trim()
        if (trimmedMessage.length < MINIMUM_MESSAGE_LENGTH) {
            Timber.e("[postMessage] Message is too short, please enter a message with at least $MINIMUM_MESSAGE_LENGTH characters")
            return PostMessageResult.InsufficientLength
        }

        return when (val user: UserEntity = authenticationRepository.user.value) {
            is UserEntity.AuthenticatedUser -> {
                val messageEntity = messageRepository.postMessage(trimmedMessage, user.userId)
                Timber.i("[postMessage] Message posted successfully")
                PostMessageResult.Success(messageEntity)
            }

            UserEntity.UnauthenticatedUser -> {
                Timber.e("[postMessage] User is not authenticated, please authenticate to post a message")
                PostMessageResult.Unauthenticated
            }
        }
    }
}

sealed class PostMessageResult {
    data class Success(val message: MessageEntity) : PostMessageResult()

    data object Unauthenticated : PostMessageResult()

    data object InsufficientLength : PostMessageResult()
}