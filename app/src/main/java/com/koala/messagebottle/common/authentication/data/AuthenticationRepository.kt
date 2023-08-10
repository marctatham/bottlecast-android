package com.koala.messagebottle.common.authentication.data

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.IdTokenListener
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.GoogleAuthProvider
import com.koala.messagebottle.common.authentication.domain.IAuthenticationRepository
import com.koala.messagebottle.common.authentication.domain.ProviderType
import com.koala.messagebottle.common.authentication.domain.UserEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

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
            val taskGetIdToken: Task<GetTokenResult> = auth.currentUser!!.getIdToken(false)
            taskGetIdToken.addOnSuccessListener { tokenResult ->
                Timber.d("Received token result...")
                Timber.d("Token - [${tokenResult.token}]")
                Timber.d("Signin Provider - [${tokenResult.signInProvider}]")

                // TODO: plug in the user ID here too!
                _user.value = UserEntity.AuthenticatedUser(ProviderType.Google, tokenResult.token!!, "TODO: user ID here too")
            }

        } else {
            Timber.i("[onIdTokenChanged] - no token")
            _user.value = UserEntity.UnauthenticatedUser
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
