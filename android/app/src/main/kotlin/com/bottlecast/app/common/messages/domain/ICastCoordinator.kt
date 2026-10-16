package com.bottlecast.app.common.messages.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Casting (spec §3.4b, §3.5). A cast is written at once and may wait for the server, so it outlives
 * Preview: the server's answer can come long after, as a confirmation or a refusal, which brings the
 * words back to the draft.
 */
interface ICastCoordinator {

    /** How many of today's 5 casts are left, for the signed-in Google account; null for anyone else. */
    val castsLeftToday: StateFlow<Int?>

    /** Casts it. Online, it waits a few seconds for the server's answer; offline, it's queued at once. */
    suspend fun cast(cast: NewCast): CastOutcome

    /** Where the cast with [messageId] is, following it to the server, after a restart too. */
    fun status(messageId: String): Flow<CastStatus>
}
