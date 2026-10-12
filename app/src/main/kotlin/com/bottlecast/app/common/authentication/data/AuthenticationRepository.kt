package com.bottlecast.app.common.authentication.data

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.IdTokenListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.UserEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val ADMIN_USER_ID = "GxSib4PSfLgfL8IQzPDmjQcgpSk1"

// Serves as a single source of truth to indicate whether the
// user is currently signed into the application
class AuthenticationRepository constructor(
    private val firebaseAuth: FirebaseAuth,
    private val dispatcherNetwork: CoroutineDispatcher
) : IAuthenticationRepository, IdTokenListener {

    private var _user: MutableStateFlow<UserEntity> =
        MutableStateFlow(UserEntity.UnauthenticatedUser)
    override val user: StateFlow<UserEntity> = _user.asStateFlow()

    init {
        firebaseAuth.addIdTokenListener(this)
    }

    override fun onIdTokenChanged(auth: FirebaseAuth) {
        if (auth.currentUser !== null) {
            Timber.i("[onIdTokenChanged] - fetching user's current token")

            val currentUser: FirebaseUser = auth.currentUser!!
            val taskGetIdToken: Task<GetTokenResult> = currentUser.getIdToken(false)
            taskGetIdToken.addOnSuccessListener { tokenResult ->
                Timber.d("Received token result...")
                Timber.d("Sign in Provider - [${tokenResult.signInProvider}]")

                val providerType: AuthProviderType = getProviderTypeForSignInProvider(tokenResult.signInProvider!!)
                _user.value = UserEntity.AuthenticatedUser(providerType, tokenResult.token!!, currentUser.uid, currentUser.uid == ADMIN_USER_ID)
                Firebase.crashlytics.setUserId(currentUser.uid)
            }

        } else {
            Timber.i("[onIdTokenChanged] - no token")
            _user.value = UserEntity.UnauthenticatedUser
        }
    }

    private fun getProviderTypeForSignInProvider(signInProvider:String):AuthProviderType {
        return when (signInProvider) {
            AuthProviderType.PROVIDER_GOOGLE -> AuthProviderType.Google
            AuthProviderType.PROVIDER_ANONYMOUS -> AuthProviderType.Anonymous
            else -> throw IllegalArgumentException("Unsupported SignInProvider $signInProvider")
        }
    }

    override suspend fun firebaseAuthWithGoogle(idToken: String) = withContext(dispatcherNetwork) {
        Timber.i("[authenticateWithGoogle] Authenticating into FirebaseUser's for user's Google Provided IdToken")
        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = Tasks.await(firebaseAuth.signInWithCredential(authCredential))
        Tasks.await(authResult.user!!.getIdToken(false))
        Timber.i("[authenticateWithGoogle] Authenticated successfully!")
    }

    override suspend fun signInAnonymously() = withContext(dispatcherNetwork) {
        Timber.i("[authenticateAnonymously] Authenticating with Firebase anonymously...")
        Tasks.await(firebaseAuth.signInAnonymously())
        Timber.i("[authenticateAnonymously] Authenticated successfully!")
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        _user.value = UserEntity.UnauthenticatedUser
        Timber.i("[signOut] Signed out complete.")
    }

}
