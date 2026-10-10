package com.bottlecast.app.feature.login

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.EXTRA_SEND_INTENT_EXCEPTION
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.bottlecast.app.R
import com.bottlecast.app.common.components.BottlingButton
import com.bottlecast.app.common.components.BottlingButtonType
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    // configure & remember launcher that facilitates the Google One Tap Sign-in UI
    val launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult> =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                // The user cancelled the login, was it due to an Exception?
                if (result.data?.action == ActivityResultContracts.StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST) {
                    val exception = result.data?.getSerializableExtra(EXTRA_SEND_INTENT_EXCEPTION)
                    Timber.e("Couldn't sign in using Google One Tap: $exception")
                    Toast.makeText(
                        context,
                        "There was a problem signing in with Google",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@rememberLauncherForActivityResult
            }
            val oneTapClient = Identity.getSignInClient(context)
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                // Got an ID token from Google. Use it to authenticate with your backend.
                Timber.i("Sign in with Google successful, attempting authenticate to backend with idToken")
                viewModel.initiateLoginWithGoogle(idToken)
            } else {
                Timber.e("Null idToken from google, sign in to google failed")
            }
        }

    val clickAnon: () -> Unit = { viewModel.initiateAnonymousLogin() }
    val clickGoogle: () -> Unit = { scope.launch { signInWithGoogle(context, launcher) } }
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

suspend fun signInWithGoogle(
    context: Context,
    launcher: ActivityResultLauncher<IntentSenderRequest>
) {
    val oneTapClient = Identity.getSignInClient(context)
    val signInRequest = BeginSignInRequest.builder()
        .setPasswordRequestOptions(
            BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build()
        )
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(context.getString(R.string.firebase_webclient_id)) // Your server's client ID, not your Android client ID.
                .setFilterByAuthorizedAccounts(false) // Only show accounts previously used to sign in.
                .build()
        )
        .setAutoSelectEnabled(true) // Automatically sign in when exactly one credential is retrieved.
        .build()

    try {
        // Use await() from https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-play-services
        // Instead of listeners that aren't cleaned up automatically
        val result = oneTapClient.beginSignIn(signInRequest).await()

        // Now construct the IntentSenderRequest the launcher requires
        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
        launcher.launch(intentSenderRequest)
    } catch (exception: Exception) {
        // No saved credentials found. Launch the One Tap sign-up flow, or
        // do nothing which results in no state changes & therefore continues
        // presenting the UnauthenticatedView.
        Timber.e(exception, "there was a problem Signing in with Google One Tap")
    }
}

