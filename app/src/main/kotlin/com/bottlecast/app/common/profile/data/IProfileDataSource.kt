package com.bottlecast.app.common.profile.data

import com.bottlecast.app.common.profile.domain.UserProfile
import kotlinx.coroutines.flow.Flow

interface IProfileDataSource {

    /** The profile, including writes still waiting for the server; an empty one when there's none. */
    fun observeProfile(uid: String): Flow<UserProfile>

    /** Merges the pen name in, without waiting for the server, so it queues while offline. */
    fun savePenName(uid: String, penName: String)
}
