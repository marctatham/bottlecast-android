package com.bottlecast.app.common.sea.domain

import com.bottlecast.app.common.messages.domain.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate

/** The finder's side of the sea: today's candidate, the find, and today's record. */
interface ISeaRepository {

    /** Today's candidate, pinned in memory for the account and the day, so backing out never re-rolls it. */
    val candidate: StateFlow<CandidateState>

    /** Picks today's candidate unless there already is one, signing in a silent guest if nobody is. */
    suspend fun prefetchCandidate(): CandidateState

    /** Today's find, or null until there is one. Follows the account as it changes. */
    fun observeToday(date: LocalDate): Flow<TodayFind?>

    /**
     * Records today's find, at the cork's pop. Tries the candidate, then the next one if someone took
     * it first. If today already has a find of the same bottle, it's Found again.
     */
    suspend fun findTodaysBottle(): FindResult

    /** A bottle this session already holds (the candidate, or one just found), with no waiting. */
    fun cachedMessage(messageId: String): MessageEntity?

    /** A bottle by id, or null when it doesn't exist or can't be read. */
    suspend fun getMessage(messageId: String): MessageEntity?

    /**
     * Whether the signed-in account has found today's bottle, as the server says, for the daily
     * reminder. False when there's nobody signed in or the server can't be reached.
     */
    suspend fun hasFoundToday(): Boolean

    /**
     * As the guest, before it signs in to a Google account that already exists: arms a handoff of
     * today's live hold, or returns null when there's none to hand over (spec §3.2).
     */
    suspend fun prepareHandover(): Handover?

    /**
     * As the Google account [uid], just signed in: takes over the guest's hold, unless it has a find
     * today. The uid comes from the sign-in itself, since the signed-in user may not have caught up.
     */
    suspend fun claimHandover(handover: Handover, uid: String): HandoverResult

    /** The writers the signed-in account blocked, newest first, following the account (spec §3.17). */
    val blockedWriters: Flow<List<BlockedWriter>>

    /** Lets [writerUid]'s bottles reach the account again, from the next look at the sea. */
    suspend fun unblock(writerUid: String)
}

/** A writer the account blocked from the Report dialog. Their uid is never shown, only when. */
data class BlockedWriter(val writerUid: String, val blockedAt: Instant?)
