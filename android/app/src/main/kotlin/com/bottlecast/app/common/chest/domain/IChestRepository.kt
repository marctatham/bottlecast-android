package com.bottlecast.app.common.chest.domain

import com.bottlecast.app.common.messages.domain.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** The chest (spec §3.10): the bottles the signed-in account found and kept, and the ones it cast. */
interface IChestRepository {

    /**
     * Keeps a bottle this account holds (spec §1.7): in its chest, for good. Online only, and only a
     * Google account; straight after a sign-in it waits a moment for the new account to show up.
     */
    suspend fun keep(messageId: String): KeepResult

    /** The bottles this account holds and hasn't kept, newest first: at most a week's finds. */
    val notKept: StateFlow<List<FoundBottle>?>

    /** The bottles this account kept, newest first, up to [limit] of them. */
    fun kept(limit: Int): Flow<ChestPage<FoundBottle>>

    /** The bottles this account cast, newest first, up to [limit] of them. */
    fun cast(limit: Int): Flow<ChestPage<MessageEntity>>

    /** One bottle in the chest, kept up to date, or null once it isn't in this account's chest. */
    fun item(messageId: String): Flow<ChestItem?>

    /** Takes a drifting bottle of this account's out of the sea, so no one finds it. Online only. */
    suspend fun pull(messageId: String): PullResult

    /** The account's numbers, or null until they're known; they aren't while offline. */
    val counts: StateFlow<ChestCounts?>

    /** Counts again, unless that happened a moment ago. */
    fun refreshCounts()

    /** Whether the Chest tab has news: a bottle not kept yet, or a cast found, since it was last viewed. */
    val badge: StateFlow<Boolean>

    /** How many finds of this account's bottles there have been since the Chest was last viewed. */
    val castFindsSinceViewed: StateFlow<Int>

    /** The Chest has been looked at, so its news is seen (spec §3.10: the dot clears). */
    fun markViewed()
}
