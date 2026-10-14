package com.bottlecast.app.common.messages.domain

/** A bottle about to be cast from Preview (spec §3.4b), with the writer's choices. */
data class NewCast(
    val messageId: String,
    val text: String,
    /** BCP 47, or "und" when the phone couldn't tell. */
    val lang: String,
    /** The pen name to sign it with, or null for "— A stranger at sea". */
    val penName: String?,
    /** "Let the finder seal this as a keepsake". */
    val sealable: Boolean,
    /** The first cast's Terms checkbox was ticked, so the acceptance goes with this cast. */
    val acceptTerms: Boolean,
)

sealed interface CastOutcome {

    /** It's cast. [queued] while it waits for a connection or the server: "Corked" (spec §3.5). */
    data class Cast(val queued: Boolean) : CastOutcome

    /** The server turned it down. Its words are back in the draft. */
    data object Refused : CastOutcome

    /** That's today's 5 already (spec §3.4); nothing was written. */
    data object LimitReached : CastOutcome

    /** Only a Google account casts. */
    data object NotSignedIn : CastOutcome
}

/** Where a cast is, as Cast success follows it. */
enum class CastStatus {
    /** On the phone, waiting for the server: Corked, "Waiting for connection". */
    Queued,

    /** The server has it: Drifting. */
    Confirmed,

    /** The server turned it down, so it's gone from the phone too. */
    Refused,
}
