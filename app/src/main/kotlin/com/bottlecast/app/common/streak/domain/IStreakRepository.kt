package com.bottlecast.app.common.streak.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** The tide streak and its calm-sea shields (spec §3.3), for whoever is signed in, guest or not. */
interface IStreakRepository {

    /** The signed-in account's streak today, or null while it loads or nobody is signed in. */
    val streak: StateFlow<Streak?>

    /** A missed day's news for the Shore, until it's acknowledged: a shield used, or a rest. */
    val notice: Flow<StreakEvent?>

    fun acknowledge(event: StreakEvent)

    /**
     * As a guest about to switch to a Google account that already exists: the streak it can bring
     * along (spec §3.2), as the sign-in sheet showed it. Today's find moves on its own, by a handover.
     */
    suspend fun carryable(): Int

    /** As the account [uid], just signed in: carries a guest's streak of [days] over onto today. */
    suspend fun carry(uid: String, days: Int)
}
