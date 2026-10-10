package com.bottlecast.app.feature.login

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginBottomSheet(
    onSignInCompleteHandler: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    // Create a scope that is automatically cancelled  if the
    // user closes the app while async work is still happening
    val context = LocalContext.current
    val uiState: State by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val clickAnon: () -> Unit = { viewModel.initiateAnonymousLogin() }
    val clickGoogle: () -> Unit = {
        scope.launch {
            val idToken: String = signInWithGoogle(context) ?: return@launch

            // Got an ID token from Google. Use it to authenticate with your backend.
            Timber.i("Sign in with Google successful, attempting authenticate to backend with idToken")
            viewModel.initiateLoginWithGoogle(idToken)
        }
    }
    LoginBottomSheet(
        signInWithGoogleHandler = clickGoogle,
        signInAnonymouslyHandler = clickAnon,
        onDismissRequest = onDismissRequest,
        onSignInCompleteHandler = onSignInCompleteHandler,
        bottomSheetState = sheetState,
        uiState = uiState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginBottomSheet(
    signInWithGoogleHandler: () -> Unit,
    signInAnonymouslyHandler: () -> Unit,
    onDismissRequest: () -> Unit,
    onSignInCompleteHandler: () -> Unit,
    bottomSheetState: SheetState,
    uiState: State,
) {
    if (uiState is State.AuthenticatedUser) {
        LaunchedEffect(Unit) {
            onSignInCompleteHandler()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = bottomSheetState,
    ) {
        val isLoading = uiState is State.Loading
        val areLoginOptionsEnabled: Boolean = isLoading.not()

        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.loginBottomSheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                BottlingButton(
                    text = R.string.btnSignInWithGoogle,
                    buttonType = BottlingButtonType.PRIMARY,
                    enabled = areLoginOptionsEnabled,
                    onTapHandler = signInWithGoogleHandler,
                    modifier = Modifier.padding(top = 8.dp)
                )
                BottlingButton(
                    text = R.string.btnSignInAnonymously,
                    buttonType = BottlingButtonType.SECONDARY,
                    enabled = areLoginOptionsEnabled,
                    onTapHandler = signInAnonymouslyHandler,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (uiState is State.Failure) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = stringResource(id = R.string.loginBottomSheet_error),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 0.dp
                    ),
                    text = stringResource(id = R.string.loginBottomSheet_subtext),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 4.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun FailureContentPreview(
) {


//    LoginBottomSheet(
//        signInWithGoogleHandler = {},
//        signInAnonymouslyHandler = {},
//        onDismissRequest = {},
//        onSignInCompleteHandler = { },
//        bottomSheetState = SheetState(true, SheetValue.Expanded, { true }),
//        uiState = State.Loading,
//    )
    // TODO: Fix
    Text(
        text = "TODO: Fix the bottom sheet implementation",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun LoadingContentPreview(
) {
//    LoginBottomSheet(
//        signInWithGoogleHandler = {},
//        signInAnonymouslyHandler = {},
//        onDismissRequest = {},
//        onSignInCompleteHandler = { },
//        bottomSheetState = SheetState(true, SheetValue.Expanded, { true }),
//        uiState = State.Failure,
//    )

    // TODO: Fix
    Text(
        text = "TODO: Fix the bottom sheet implementation",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

/**
 * Presents Credential Manager's Google account picker & returns the chosen account's ID token.
 * Returns null if the user cancelled, or if signing in failed (in which case they've been told).
 */
private suspend fun signInWithGoogle(context: Context): String? {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(context.getString(R.string.firebase_webclient_id)) // Your server's client ID, not your Android client ID.
        .setFilterByAuthorizedAccounts(false) // Offer all of the user's Google accounts, not just those previously used to sign in.
        .setAutoSelectEnabled(true) // Automatically sign in when exactly one credential is retrieved.
        .build()
    val request = GetCredentialRequest(listOf(googleIdOption))

    try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        Timber.e("Unexpected credential type from Credential Manager: ${credential.type}")
    } catch (exception: GetCredentialCancellationException) {
        // The user dismissed the account picker, which continues presenting the login options
        Timber.i("Sign in with Google was cancelled by the user")
        return null
    } catch (exception: GetCredentialException) {
        Timber.e(exception, "Couldn't sign in with Google")
    } catch (exception: GoogleIdTokenParsingException) {
        Timber.e(exception, "Received an invalid Google ID token credential")
    }

    Toast.makeText(context, "There was a problem signing in with Google", Toast.LENGTH_SHORT).show()
    return null
}

