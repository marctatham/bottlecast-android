package com.bottlecast.app.common.authentication.data

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.GoogleLinkResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.IdTokenListener
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.ExecutionException

// the custom claim set by scripts/set_admin_claim.py, which firestore.rules also reads
private const val CLAIM_ADMIN = "admin"

// Firebase Auth's code for signing in again with another account than the one signed in
private const val ERROR_USER_MISMATCH = "ERROR_USER_MISMATCH"

// Serves as a single source of truth to indicate whether the
// user is currently signed into the application
class AuthenticationRepository constructor(
    private val firebaseAuth: FirebaseAuth,
    private val dispatcherNetwork: CoroutineDispatcher
) : IAuthenticationRepository, IdTokenListener {

    private var _user: MutableStateFlow<UserEntity> =
        MutableStateFlow(UserEntity.UnauthenticatedUser)
    override val user: StateFlow<UserEntity> = _user.asStateFlow()

    // so two callers needing an account at once don't sign in two guests
    private val signInMutex = Mutex()

    init {
        firebaseAuth.addIdTokenListener(this)
    }

    override fun onIdTokenChanged(auth: FirebaseAuth) {
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser == null) {
            Timber.i("[onIdTokenChanged] - no token")
            _user.value = UserEntity.UnauthenticatedUser
            // crashes from here on aren't the signed-out (or deleted) account's
            Firebase.crashlytics.setUserId("")
            return
        }

        // The account is known before its token is, and a token past its hour can't refresh offline.
        // So the account is published at once, and its token and admin claim follow once they're read;
        // otherwise a guest restored offline would look signed out, and its throw backs would be lost.
        val known = (_user.value as? UserEntity.AuthenticatedUser)?.takeIf { it.userId == currentUser.uid }
        _user.value = UserEntity.AuthenticatedUser(
            providerType = providerTypeOf(currentUser),
            jwtToken = known?.jwtToken.orEmpty(),
            userId = currentUser.uid,
            isAdmin = known?.isAdmin == true,
            createdAt = createdAtOf(currentUser),
            googleName = googleNameOf(currentUser),
        )
        Firebase.crashlytics.setUserId(currentUser.uid)

        Timber.i("[onIdTokenChanged] - fetching user's current token")
        val taskGetIdToken: Task<GetTokenResult> = currentUser.getIdToken(false)
        taskGetIdToken
            .addOnSuccessListener { tokenResult ->
                // the account may have changed while its token was read
                if (auth.currentUser?.uid != currentUser.uid) return@addOnSuccessListener
                val providerType = providerTypeOf(currentUser)
                Timber.d("Signed in with $providerType")
                _user.value = UserEntity.AuthenticatedUser(
                    providerType = providerType,
                    jwtToken = tokenResult.token.orEmpty(),
                    userId = currentUser.uid,
                    isAdmin = tokenResult.claims[CLAIM_ADMIN] == true,
                    createdAt = createdAtOf(currentUser),
                    googleName = googleNameOf(currentUser),
                )
            }
            .addOnFailureListener { Timber.w(it, "[onIdTokenChanged] - couldn't read the user's token") }
    }

    // A guest who linked Google keeps signing in anonymously until its token refreshes, so the
    // account's providers, not the token, say whether it has Google.
    private fun providerTypeOf(user: FirebaseUser): AuthProviderType =
        if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            AuthProviderType.Google
        } else {
            AuthProviderType.Anonymous
        }

    private fun createdAtOf(user: FirebaseUser): Instant? =
        user.metadata?.creationTimestamp?.takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    // a guest that linked Google may not have taken the Google profile's name onto the account itself
    private fun googleNameOf(user: FirebaseUser): String? =
        user.providerData.firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }?.displayName?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }

    // Under the same lock as a guest sign-in, so one starting in the background can't land after
    // this and replace the Google account.
    override suspend fun linkGoogle(idToken: String): GoogleLinkResult = withContext(dispatcherNetwork) {
        signInMutex.withLock {
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val guest = firebaseAuth.currentUser?.takeIf { it.isAnonymous }
            if (guest == null) {
                Timber.i("[linkGoogle] Nobody to link, so signing in with Google")
                refreshed(Tasks.await(firebaseAuth.signInWithCredential(authCredential)).user!!)
                return@withLock GoogleLinkResult.SignedIn
            }
            try {
                refreshed(Tasks.await(guest.linkWithCredential(authCredential)).user!!)
                Timber.i("[linkGoogle] Linked the guest to Google")
                GoogleLinkResult.Linked
            } catch (exception: ExecutionException) {
                if (exception.cause !is FirebaseAuthUserCollisionException) throw exception
                Timber.i("[linkGoogle] That Google account already exists, so the guest stays signed in for now")
                GoogleLinkResult.AccountExists
            }
        }
    }

    override suspend fun switchToGoogle(idToken: String): String = withContext(dispatcherNetwork) {
        signInMutex.withLock {
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val account = Tasks.await(firebaseAuth.signInWithCredential(authCredential)).user!!
            refreshed(account)
            Timber.i("[switchToGoogle] Switched to the Google account")
            account.uid
        }
    }

    // refreshes the token, so it carries the Google identity and any admin claim
    private fun refreshed(user: FirebaseUser) {
        Tasks.await(user.getIdToken(true))
    }

    override suspend fun ensureSignedIn(): String = withContext(dispatcherNetwork) {
        signInMutex.withLock {
            firebaseAuth.currentUser?.uid ?: run {
                Timber.i("[ensureSignedIn] Signing in a guest")
                Tasks.await(firebaseAuth.signInAnonymously()).user!!.uid
            }
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        _user.value = UserEntity.UnauthenticatedUser
        Timber.i("[signOut] Signed out complete.")
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): Boolean = withContext(dispatcherNetwork) {
        val account = checkNotNull(firebaseAuth.currentUser) { "Nobody is signed in" }
        try {
            Tasks.await(account.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)))
            Timber.i("[reauthenticateWithGoogle] Signed in again")
            true
        } catch (exception: ExecutionException) {
            // another Google account than this one
            if ((exception.cause as? FirebaseAuthInvalidUserException)?.errorCode != ERROR_USER_MISMATCH) throw exception
            Timber.i("[reauthenticateWithGoogle] That was another Google account")
            false
        }
    }

    override suspend fun deleteUser() = withContext(dispatcherNetwork) {
        val account = checkNotNull(firebaseAuth.currentUser) { "Nobody is signed in" }
        Tasks.await(account.delete())
        _user.value = UserEntity.UnauthenticatedUser
        Timber.i("[deleteUser] Deleted the account")
    }

}
