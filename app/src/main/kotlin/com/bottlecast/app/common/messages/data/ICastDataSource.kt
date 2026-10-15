package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.domain.CastStatus
import com.bottlecast.app.common.messages.domain.NewCast
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Firestore reads and writes for casting. The blocking calls run on the caller's dispatcher. */
interface ICastDataSource {

    /** The account's cast tally, including a cast still waiting for the server; null while there's none. */
    fun observeTally(uid: String): Flow<CastTally?>

    /** The tally as the phone knows it now: the server's, or the cache's offline. */
    fun readTally(uid: String): CastTally?

    /**
     * One commit: the bottle, the tally counting it, the day's shield mark when it's the day's first
     * cast, and the profile when the Terms are accepted or the bottle is signed. It's on the phone at
     * once, so it queues while offline.
     */
    fun writeCast(uid: String, cast: NewCast, rand: Double, tally: TallyWrite): PendingCast

    /** Where the cast is: on the phone, at the server, or gone after a refusal. */
    fun observeCast(messageId: String): Flow<CastStatus>
}

data class CastTally(val date: LocalDate, val count: Int)

/** How a cast counts on the tally: the day's first, or one more on the same date. */
sealed interface TallyWrite {
    val date: LocalDate

    data class Start(override val date: LocalDate) : TallyWrite

    data class Increment(override val date: LocalDate) : TallyWrite
}

/** A cast written to the phone, waiting for the server's answer. */
fun interface PendingCast {
    suspend fun awaitServer(): CastAck
}

enum class CastAck {
    Confirmed,

    /** The server turned it down; Firestore won't try again. */
    Refused,
}
