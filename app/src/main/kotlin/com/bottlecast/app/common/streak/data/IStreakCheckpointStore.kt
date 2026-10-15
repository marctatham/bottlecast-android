package com.bottlecast.app.common.streak.data

import com.bottlecast.app.common.streak.domain.StreakCheckpoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The streak folded so far, kept on the phone for one account, so a look at the streak only reads the
 * last few days. It's a cache: a new phone, or another account, folds the whole history again.
 */
interface IStreakCheckpointStore {

    /** The account's checkpoint, or null when there's none for it, or it's from an older fold. */
    suspend fun load(uid: String): StreakCheckpoint?

    fun save(uid: String, checkpoint: StreakCheckpoint): Job

    /** The newest missed day the account has seen news of on the Shore. */
    fun noticeSeenThrough(uid: String): Flow<LocalDate?>

    fun markNoticeSeen(uid: String, date: LocalDate): Job

    /** Forgets the account's checkpoint and news, as it's deleted. */
    fun clear(): Job
}
