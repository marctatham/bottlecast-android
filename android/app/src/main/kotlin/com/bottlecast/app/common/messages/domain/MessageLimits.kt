package com.bottlecast.app.common.messages.domain

import kotlin.random.Random

/** What a cast may hold (spec §3.4). firestore.rules checks the same minimum, bytes and daily casts. */
object MessageLimits {

    /** The fewest characters a bottle needs, once trimmed, so whitespace never passes. */
    const val MIN_TRIMMED = 10

    /** The most characters, counted as code points, so an emoji is one or more. */
    const val MAX_CHARACTERS = 240

    /** The most UTF-8 bytes, which emoji and other scripts reach before the characters run out. */
    const val MAX_BYTES = 280

    /** From here the counter reads "20 left" in ink, so the warning isn't colour alone. */
    const val COUNTDOWN_FROM = 220

    /** The casts one local date allows. */
    const val CASTS_PER_DAY = 5
}

/** Its length in characters, as the counter shows it: code points, not UTF-16 units. */
fun String.characterCount(): Int = codePointCount(0, length)

fun String.utf8Size(): Int = encodeToByteArray().size

/** Whether this text fits a bottle, trimmed or not. */
fun String.fitsCast(): Boolean =
    characterCount() <= MessageLimits.MAX_CHARACTERS && utf8Size() <= MessageLimits.MAX_BYTES

/** The longest start of this text that fits a bottle, never splitting a character. */
fun String.fitToCast(): String = fitTo(MessageLimits.MAX_CHARACTERS, MessageLimits.MAX_BYTES)

/**
 * This text, an edit of [previous], cut to fit a bottle by shortening only what the edit inserted.
 * The rest of the message is never lost: a paste in the middle of a long message keeps the
 * message's end, and loses the end of the paste instead.
 */
fun String.fitEditToCast(previous: String): String {
    if (fitsCast()) return this
    // both keep surrogate pairs whole
    val prefix = commonPrefixWith(previous).length
    val suffix = minOf(commonSuffixWith(previous).length, length - prefix, previous.length - prefix)
    val kept = substring(0, prefix) + substring(length - suffix)
    val characterRoom = MessageLimits.MAX_CHARACTERS - kept.characterCount()
    val byteRoom = MessageLimits.MAX_BYTES - kept.utf8Size()
    if (characterRoom <= 0 || byteRoom <= 0) return kept.fitToCast()
    return substring(0, prefix) + substring(prefix, length - suffix).fitTo(characterRoom, byteRoom) + substring(length - suffix)
}

/** A new bottle's id: 20 letters and digits, like Firestore's own ids, as firestore.rules expects. */
fun newMessageId(random: Random): String = buildString(ID_LENGTH) {
    repeat(ID_LENGTH) { append(ID_ALPHABET[random.nextInt(ID_ALPHABET.length)]) }
}

private const val ID_LENGTH = 20
private const val ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

// The longest start of this text within both limits, never splitting a character.
private fun String.fitTo(maxCharacters: Int, maxBytes: Int): String {
    var characters = 0
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
        if (characters + 1 > maxCharacters || bytes + size > maxBytes) break
        characters += 1
        bytes += size
        end += Character.charCount(codePoint)
    }
    return substring(0, end)
}
