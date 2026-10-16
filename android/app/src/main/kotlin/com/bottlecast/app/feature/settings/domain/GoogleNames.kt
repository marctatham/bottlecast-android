package com.bottlecast.app.feature.settings.domain

/**
 * The Google name as Settings › Account shows it (spec §3.17): the first name and the last name's
 * initial, "Marc T." for "Marc Tatham". A single name shows as it is.
 */
fun shortGoogleName(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.size < 2) return words.firstOrNull().orEmpty()
    return "${words.first()} ${firstLetter(words.last())}."
}

/** Up to two initials for the account's avatar: "MT" for "Marc Tatham". */
fun googleInitials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.let { words ->
        listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull()).joinToString("") { firstLetter(it) }
    }

private fun firstLetter(word: String): String = String(Character.toChars(word.codePointAt(0))).uppercase()
