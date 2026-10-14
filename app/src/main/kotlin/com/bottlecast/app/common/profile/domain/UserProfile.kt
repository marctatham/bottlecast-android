package com.bottlecast.app.common.profile.domain

/** The Terms version a cast needs. firestore.rules' currentTermsVersion() holds the same number. */
const val TERMS_VERSION = 1

/** A pen name is 3 to 20 characters, trimmed (spec §3.2). firestore.rules checks the same. */
const val PEN_NAME_MIN = 3
const val PEN_NAME_MAX = 20

/** A Google account's profile (users/{uid}): its pen name, and the Terms it accepted. */
data class UserProfile(
    val penName: String? = null,
    val termsVersion: Int? = null,
) {
    /** Accepted the current Terms, so its casts skip Preview's checkbox (spec §3.4b). */
    val acceptedTerms: Boolean get() = (termsVersion ?: 0) >= TERMS_VERSION
}

/**
 * Whether [name], once trimmed, can be a pen name. Its length is in UTF-16 units, as the rules'
 * size() counts it, so an emoji counts as two.
 */
fun isValidPenName(name: String): Boolean = name.trim().length in PEN_NAME_MIN..PEN_NAME_MAX

/** [name] cut to the longest a pen name can be, never splitting an emoji into half a character. */
fun limitPenName(name: String): String {
    if (name.length <= PEN_NAME_MAX) return name
    val end = if (Character.isHighSurrogate(name[PEN_NAME_MAX - 1])) PEN_NAME_MAX - 1 else PEN_NAME_MAX
    return name.substring(0, end)
}
