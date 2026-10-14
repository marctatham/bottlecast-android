package com.bottlecast.app.fakes

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for Firebase Auth, the user starts out signed out.
 */
@Singleton
class FakeAuthenticationRepository @Inject constructor() : IAuthenticationRepository {

    private val _user: MutableStateFlow<UserEntity> = MutableStateFlow(UserEntity.UnauthenticatedUser)
    override val user: StateFlow<UserEntity> = _user.asStateFlow()

    override suspend fun firebaseAuthWithGoogle(idToken: String) = signIn(AuthProviderType.Google)

    // the app's silent guest: an anonymous account, made the first time one is needed
    override suspend fun ensureSignedIn(): String {
        (user.value as? UserEntity.AuthenticatedUser)?.let { return it.userId }
        signIn(AuthProviderType.Anonymous)
        return FAKE_USER_ID
    }

    override suspend fun signOut() {
        _user.value = UserEntity.UnauthenticatedUser
    }

    /** Signs in as the admin, who's offered the Shore's moderation menu. */
    fun signInAsAdmin() {
        _user.value = UserEntity.AuthenticatedUser(
            providerType = AuthProviderType.Google,
            jwtToken = "fakeToken",
            userId = "fakeAdminId",
            isAdmin = true
        )
    }

    private fun signIn(providerType: AuthProviderType) {
        _user.value = UserEntity.AuthenticatedUser(
            providerType = providerType,
            jwtToken = "fakeToken",
            userId = FAKE_USER_ID,
            isAdmin = false
        )
    }

    companion object {
        const val FAKE_USER_ID: String = "fakeUserId"
    }
}
