package com.bottlecast.app.common.authentication.domain

import kotlinx.coroutines.flow.StateFlow

interface IAuthenticationRepository {

    val user: StateFlow<UserEntity>

    /**
     * Signs in with Google. A guest's account is linked rather than replaced, so its uid, finds and
     * casts stay. If that Google account already exists, the guest stays signed in, so it can hand
     * today's find over first (spec §3.2), and [switchToGoogle] then moves to the Google account.
     */
    suspend fun linkGoogle(idToken: String): GoogleLinkResult

    /** Signs in to the Google account, leaving the guest behind. */
    suspend fun switchToGoogle(idToken: String)

    /**
     * Returns the signed-in user's uid, first signing in a silent guest if nobody is. Call it where
     * an account is first needed (today's first prefetch or find), never during onboarding.
     */
    suspend fun ensureSignedIn(): String

    suspend fun signOut()
}
