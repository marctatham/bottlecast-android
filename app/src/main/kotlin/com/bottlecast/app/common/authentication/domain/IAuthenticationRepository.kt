package com.bottlecast.app.common.authentication.domain

import kotlinx.coroutines.flow.StateFlow

interface IAuthenticationRepository {

    val user: StateFlow<UserEntity>

    /**
     * Signs in with Google. A guest's account is linked rather than replaced, so its finds come
     * along, unless that Google account already exists (merging the two arrives in slice 3).
     */
    suspend fun firebaseAuthWithGoogle(idToken: String)

    /**
     * Returns the signed-in user's uid, first signing in a silent guest if nobody is. Call it where
     * an account is first needed (today's first prefetch or find), never during onboarding.
     */
    suspend fun ensureSignedIn(): String

    suspend fun signOut()
}
