package com.bottlecast.app.common.account.data

import com.bottlecast.app.common.account.domain.IAccountLocalData
import com.bottlecast.app.common.chest.data.IChestSeenStore
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.streak.data.IStreakCheckpointStore
import kotlinx.coroutines.joinAll
import javax.inject.Inject

/** The streak's checkpoint, what the chest had seen, and the unsent draft. Settings stay with the phone. */
class AccountLocalData @Inject constructor(
    private val streakCheckpoints: IStreakCheckpointStore,
    private val chestSeen: IChestSeenStore,
    private val drafts: IDraftRepository,
) : IAccountLocalData {
    override suspend fun clear() {
        listOf(streakCheckpoints.clear(), chestSeen.clear(), drafts.clear()).joinAll()
    }
}
