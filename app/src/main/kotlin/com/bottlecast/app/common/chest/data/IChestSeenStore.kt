package com.bottlecast.app.common.chest.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * When an account last viewed its chest, and how many finds of its bottles there had been by then,
 * kept on the phone: the Chest tab's dot and the Shore's "While you were away" banner (spec §3.10, G5)
 * show what's new since.
 */
interface IChestSeenStore {

    /** What the account had seen, or null when it never has on this phone. */
    fun seen(uid: String): Flow<ChestSeen?>

    fun save(uid: String, seen: ChestSeen): Job

    /** Forgets what the account had seen, as it's deleted. */
    fun clear(): Job
}

/** [castFinds] counts every find of the account's bottles, null for a guest. */
data class ChestSeen(val viewedAt: Instant, val castFinds: Int?)
