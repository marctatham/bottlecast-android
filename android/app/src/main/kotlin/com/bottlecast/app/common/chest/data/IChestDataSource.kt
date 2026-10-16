package com.bottlecast.app.common.chest.data

import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.FindStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** Firestore reads and writes for the chest. The blocking calls run on the caller's dispatcher. */
interface IChestDataSource {

    /** One transaction: marks the bottle and this account's find kept, and creates its claim. */
    fun keep(messageId: String, uid: String, now: Instant): KeepResult

    /** One transaction: deletes this account's bottle, if it still drifts. */
    fun pull(messageId: String, uid: String, now: Instant): PullResult

    /** The account's finds since [since], newest first, whatever became of them. */
    fun observeFinds(uid: String, since: Instant): Flow<List<FindOf>>

    /** The account's claims, newest first, up to [limit] of them. */
    fun observeClaims(uid: String, limit: Int): Flow<List<Claim>>

    /** The bottles the account cast, newest first, up to [limit] of them. */
    fun observeCasts(uid: String, limit: Int): Flow<List<MessageEntity>>

    /** A bottle, or null when it can't be read. */
    fun observeMessage(messageId: String): Flow<MessageEntity?>

    fun observeFind(messageId: String, uid: String): Flow<FindOf?>

    /** This account's claim on a bottle, or null when it has none. */
    fun observeClaim(messageId: String): Flow<Claim?>

    /** A bottle, from the phone when it has it, since its words never change; null when it can't be read. */
    fun readMessage(messageId: String): MessageEntity?

    /** The account's numbers, from the server. A guest has no casts and no claims to count. */
    fun count(uid: String, canCast: Boolean): ChestTotals
}

/** One of the account's finds. */
data class FindOf(val messageId: String, val foundAt: Instant, val status: FindStatus?)

/** The account's claim on a bottle it kept. */
data class Claim(val messageId: String, val keptAt: Instant?)

data class ChestTotals(
    val bottled: Int?,
    val found: Int,
    val kept: Int,
    /** How many times the account's bottles have been found in all, or null for a guest. */
    val castFinds: Int?,
)

/** The phone couldn't reach Firestore. */
class ChestUnavailableException(cause: Throwable) : Exception(cause)
