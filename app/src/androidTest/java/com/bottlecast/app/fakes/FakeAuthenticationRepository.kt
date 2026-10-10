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

    override suspend fun signInAnonymously() = signIn(AuthProviderType.Anonymous)

    override suspend fun signOut() {
        _user.value = UserEntity.UnauthenticatedUser
    }

    private fun signIn(providerType: AuthProviderType) {
        _user.value = UserEntity.AuthenticatedUser(
            providerType = providerType,
            jwtToken = "fakeToken",
            userId = "fakeUserId",
            isAdmin = false
        )
    }
}
