package com.bottlecast.app.feature.compose.domain

import com.bottlecast.app.common.messages.domain.LANG_UNDETERMINED
import com.bottlecast.app.common.messages.domain.MessageLimits
import com.bottlecast.app.common.messages.domain.characterCount
import com.bottlecast.app.common.messages.domain.utf8Size

/**
 * How full the bottle is (spec §3.4): its characters against 240 and its bytes against 280, whichever
 * runs out first, with the minimum measured on the trimmed text so whitespace never passes.
 */
data class DraftMeter(
    val characters: Int,
    val bytes: Int,
    val trimmedCharacters: Int,
) {
    /** What's left before the bottle is full. */
    val left: Int get() = minOf(MessageLimits.MAX_CHARACTERS - characters, MessageLimits.MAX_BYTES - bytes).coerceAtLeast(0)

    /** "20 left" and counting down, in ink, so the warning isn't colour alone. */
    val countingDown: Boolean get() = left <= MessageLimits.MAX_CHARACTERS - MessageLimits.COUNTDOWN_FROM

    val atLimit: Boolean get() = left == 0

    /** The bytes run out before the characters do: emoji and other scripts take more room. */
    val bytesBind: Boolean get() = MessageLimits.MAX_BYTES - bytes < MessageLimits.MAX_CHARACTERS - characters

    val meetsMinimum: Boolean get() = trimmedCharacters >= MessageLimits.MIN_TRIMMED

    companion object {
        fun of(text: String) = DraftMeter(text.characterCount(), text.utf8Size(), text.trim().characterCount())
    }
}

/** The line under the editor, left of the counter (spec §3.4). */
sealed interface ComposeHelper {
    /** "At least 10 characters", neutral until Preview is tapped too soon. */
    data class Minimum(val asError: Boolean) : ComposeHelper

    /** "English · detected on your phone". */
    data class Language(val tag: String) : ComposeHelper

    /** "That's the limit — bottles are small." */
    data object AtLimit : ComposeHelper

    /** "Emoji take extra room". */
    data object EmojiRoom : ComposeHelper

    data object None : ComposeHelper
}

/** Which helper shows: the error first, then the limit, the emoji, the language, then the minimum. */
fun composeHelper(meter: DraftMeter, minimumTapped: Boolean, lang: String?): ComposeHelper = when {
    !meter.meetsMinimum && minimumTapped -> ComposeHelper.Minimum(asError = true)
    meter.atLimit -> ComposeHelper.AtLimit
    meter.countingDown && meter.bytesBind -> ComposeHelper.EmojiRoom
    !meter.meetsMinimum -> ComposeHelper.Minimum(asError = false)
    lang != null && lang != LANG_UNDETERMINED -> ComposeHelper.Language(lang)
    else -> ComposeHelper.None
}
