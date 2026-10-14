package com.bottlecast.app.feature.login


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Details screen.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository
) : ViewModel() {

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    // Everyone has a silent guest account once they've been to the Shore, so only a Google account,
    // or choosing to carry on as a guest, counts as signed in here.
    private val continuingAsGuest = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(authenticationRepository.user, continuingAsGuest) { user, asGuest ->
                val signedIn = user is UserEntity.AuthenticatedUser &&
                    (user.providerType == AuthProviderType.Google || asGuest)
                if (signedIn) State.AuthenticatedUser else State.Anonymous
            }.collect { _state.value = it }
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Timber.e(exception, "There was a problem signing into your account")
        _state.value = State.Failure
    }


    fun initiateLoginWithGoogle(idToken: String) {
        viewModelScope.launch(exceptionHandler) {
            _state.value = State.Loading
            authenticationRepository.firebaseAuthWithGoogle(idToken)
        }
    }

    fun initiateAnonymousLogin() {
        viewModelScope.launch(exceptionHandler) {
            _state.value = State.Loading
            authenticationRepository.ensureSignedIn()
            continuingAsGuest.value = true
        }
    }

    // TODO: this will need to be re-introduced via the profile screen that's coming
    fun initiateSignOut() {
        viewModelScope.launch {
            _state.value = State.Loading
            authenticationRepository.signOut()
        }
    }
}

// the different states the login screen can be in
sealed class State {

    data object Anonymous : State()

    data object Loading : State()

    data object AuthenticatedUser : State()

    data object Failure : State()
}
