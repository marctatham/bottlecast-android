package com.bottlecast.app.common.profile.domain

import kotlinx.coroutines.flow.Flow

interface IProfileRepository {

    /** The signed-in Google account's profile, following the account as it changes; null for guests. */
    val profile: Flow<UserProfile?>

    /** Saves a pen name, trimmed. It shows at once and reaches the server when it can. */
    suspend fun savePenName(name: String)
}
