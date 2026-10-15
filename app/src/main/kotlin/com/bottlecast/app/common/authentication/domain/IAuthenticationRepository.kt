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

    /**
     * Signs in to the Google account, leaving the guest behind, and returns its uid. [user] follows a
     * moment later, from Firebase's listener on the main thread, so use the uid returned here.
     */
    suspend fun switchToGoogle(idToken: String): String

    /**
     * Returns the signed-in user's uid, first signing in a silent guest if nobody is. Call it where
     * an account is first needed (today's first prefetch or find), never during onboarding.
     */
    suspend fun ensureSignedIn(): String

    suspend fun signOut()

    /**
     * Signs the Google account in again with [idToken], as deleting it needs a recent sign-in. Returns
     * false when [idToken] is another Google account's: nothing changes then.
     */
    suspend fun reauthenticateWithGoogle(idToken: String): Boolean

    /** Deletes the signed-in account from Firebase Auth, which signs it out. Its records go first. */
    suspend fun deleteUser()
}
