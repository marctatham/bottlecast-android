package com.bottlecast.app.fakes

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.GoogleLinkResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for Firebase Auth, the user starts out signed out. A guest links Google and
 * keeps its uid, unless [googleAccountExists], when it must switch to [GOOGLE_USER_ID] instead.
 */
@Singleton
class FakeAuthenticationRepository @Inject constructor() : IAuthenticationRepository {

    private val _user: MutableStateFlow<UserEntity> = MutableStateFlow(UserEntity.UnauthenticatedUser)
    override val user: StateFlow<UserEntity> = _user.asStateFlow()

    /** The Google account already exists, so a guest can't be linked to it. */
    var googleAccountExists = false

    override suspend fun linkGoogle(idToken: String): GoogleLinkResult {
        val current = user.value as? UserEntity.AuthenticatedUser
        return when {
            current == null -> {
                signIn(AuthProviderType.Google)
                GoogleLinkResult.SignedIn
            }
            googleAccountExists -> GoogleLinkResult.AccountExists
            else -> {
                signIn(AuthProviderType.Google, current.userId)
                GoogleLinkResult.Linked
            }
        }
    }

    override suspend fun switchToGoogle(idToken: String): String {
        signIn(AuthProviderType.Google, GOOGLE_USER_ID)
        return GOOGLE_USER_ID
    }

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

    /** Signs straight in with Google, as someone who signed in on an earlier day. */
    fun signInAsGoogle() = signIn(AuthProviderType.Google)

    /** Signs in the silent guest, as today's first look at the sea would. */
    fun signInAsGuest() = signIn(AuthProviderType.Anonymous)

    fun signedInUid(): String? = (user.value as? UserEntity.AuthenticatedUser)?.userId

    private fun signIn(providerType: AuthProviderType, userId: String = FAKE_USER_ID) {
        _user.value = UserEntity.AuthenticatedUser(
            providerType = providerType,
            jwtToken = "fakeToken",
            userId = userId,
            isAdmin = false
        )
    }

    companion object {
        const val FAKE_USER_ID: String = "fakeUserId"
        const val GOOGLE_USER_ID: String = "fakeGoogleUserId"
    }
}
