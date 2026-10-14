package com.bottlecast.app.common.sea.domain

import com.bottlecast.app.common.messages.domain.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
}
