package com.bottlecast.app.common.streak.data

import com.bottlecast.app.common.streak.domain.StreakDay
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Firestore reads and writes for the streak. The blocking calls run on the caller's dispatcher. */
interface IStreakDataSource {

    /** The dates the account found a bottle on, after [after] (all of them when null). */
    fun observeFindDays(uid: String, after: LocalDate?): Flow<Read<Set<LocalDate>>>

    /** The account's cast, calm and carried marks, after [after] (all of them when null). */
    fun observeMarks(uid: String, after: LocalDate?): Flow<Read<List<StreakDay>>>

    /** Marks a calm sea on [date]. It writes without waiting, so it queues while offline. */
    fun markCalm(uid: String, date: LocalDate)

    /** Carries a guest's streak of [days] onto [date], and waits for the server. */
    fun carry(uid: String, date: LocalDate, days: Int)
}

/** What a listener heard, and whether the server has confirmed it, or it's only what the phone had. */
data class Read<T>(val value: T, val fromServer: Boolean)
