package com.bottlecast.app.common.authentication.data

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.IdTokenListener
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
import java.util.concurrent.ExecutionException

// the custom claim set by scripts/set_admin_claim.py, which firestore.rules also reads
private const val CLAIM_ADMIN = "admin"

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
            return
        }

        Timber.i("[onIdTokenChanged] - fetching user's current token")
        val taskGetIdToken: Task<GetTokenResult> = currentUser.getIdToken(false)
        taskGetIdToken
            .addOnSuccessListener { tokenResult ->
                val providerType = providerTypeOf(currentUser)
                Timber.d("Signed in with $providerType")
                _user.value = UserEntity.AuthenticatedUser(
                    providerType = providerType,
                    jwtToken = tokenResult.token.orEmpty(),
                    userId = currentUser.uid,
                    isAdmin = tokenResult.claims[CLAIM_ADMIN] == true,
                )
                Firebase.crashlytics.setUserId(currentUser.uid)
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

    override suspend fun firebaseAuthWithGoogle(idToken: String) = withContext(dispatcherNetwork) {
        Timber.i("[authenticateWithGoogle] Authenticating into FirebaseUser's for user's Google Provided IdToken")
        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
        val guest = firebaseAuth.currentUser?.takeIf { it.isAnonymous }
        val signedIn: FirebaseUser = if (guest == null) {
            Tasks.await(firebaseAuth.signInWithCredential(authCredential)).user!!
        } else {
            try {
                Tasks.await(guest.linkWithCredential(authCredential)).user!!
            } catch (exception: ExecutionException) {
                if (exception.cause !is FirebaseAuthUserCollisionException) throw exception
                // That Google account already exists: switch to it, and leave the guest's finds behind
                Timber.w("[authenticateWithGoogle] Google account already exists, so the guest isn't merged")
                Tasks.await(firebaseAuth.signInWithCredential(authCredential)).user!!
            }
        }
        // refresh, so the token carries the Google identity and any admin claim
        Tasks.await(signedIn.getIdToken(true))
        Timber.i("[authenticateWithGoogle] Authenticated successfully!")
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

}
