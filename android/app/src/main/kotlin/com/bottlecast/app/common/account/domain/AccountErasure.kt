package com.bottlecast.app.common.account.domain

import com.bottlecast.app.common.messages.domain.HOLD_DAYS
import java.time.Instant
import java.time.temporal.ChronoUnit

/** One of the account's own bottles, as deleting the account needs to know it. */
data class OwnBottle(
    val id: String,
    val status: String?,
    /** When its current hold began, if it was ever found. */
    val foundAt: Instant?,
    val kept: Boolean,
)

/** What deleting the account does to one of its bottles (decided 2026-10-15). */
enum class BottleErasure {
    /** Someone holds or kept it: it stays with them, without the writer's uid or pen name. */
    Anonymise,

    /** Nobody else has it, so it goes. */
    Delete,
}

/** As firestore.rules decides it: a bottle someone kept, or whose finder's hold still runs, is anonymised. */
fun erasureOf(bottle: OwnBottle, now: Instant): BottleErasure {
    val holdRunning = bottle.foundAt?.let { now.isBefore(it.plus(HOLD_DAYS, ChronoUnit.DAYS)) } == true
    return if (bottle.kept || holdRunning) BottleErasure.Anonymise else BottleErasure.Delete
}

/** Removes an account's records from the server, once it's been shown to be the account's own. */
interface IAccountEraser {

    /**
     * Marks [uid] as being deleted, then anonymises or deletes its bottles and deletes the rest of its
     * records. Each step waits for the server, and throws when it can't be reached or turns a write
     * down. It's safe to run again after a failure: whatever's already gone is skipped.
     */
    suspend fun erase(uid: String)
}
