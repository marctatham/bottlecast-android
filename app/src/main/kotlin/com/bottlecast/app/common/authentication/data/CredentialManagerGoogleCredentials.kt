package com.bottlecast.app.common.authentication.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.bottlecast.app.R
import com.bottlecast.app.common.authentication.domain.GoogleCredentialResult
import com.bottlecast.app.common.authentication.domain.IGoogleCredentials
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class CredentialManagerGoogleCredentials @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : IGoogleCredentials {

    override suspend fun requestIdToken(context: Context): GoogleCredentialResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.firebase_webclient_id)) // the server's client ID, not the Android one
            .setFilterByAuthorizedAccounts(false) // every Google account on the phone, not just ones used before
            .setAutoSelectEnabled(true) // straight in when there's exactly one
            .build()
        val request = GetCredentialRequest(listOf(googleIdOption))

        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleCredentialResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Timber.e("[requestIdToken] Unexpected credential type from Credential Manager: ${credential.type}")
                GoogleCredentialResult.Failed
            }
        } catch (exception: GetCredentialCancellationException) {
            Timber.i("[requestIdToken] The account picker was closed")
            GoogleCredentialResult.Cancelled
        } catch (exception: NoCredentialException) {
            // on a debug build this usually means the debug Android OAuth client is missing (CLAUDE.md)
            Timber.w(exception, "[requestIdToken] No Google account to sign in with")
            GoogleCredentialResult.Unavailable
        } catch (exception: GetCredentialProviderConfigurationException) {
            Timber.w(exception, "[requestIdToken] No credential provider on this phone")
            GoogleCredentialResult.Unavailable
        } catch (exception: GetCredentialUnsupportedException) {
            Timber.w(exception, "[requestIdToken] Credential Manager isn't supported on this phone")
            GoogleCredentialResult.Unavailable
        } catch (exception: GetCredentialException) {
            Timber.e(exception, "[requestIdToken] Couldn't sign in with Google")
            GoogleCredentialResult.Failed
        } catch (exception: GoogleIdTokenParsingException) {
            Timber.e(exception, "[requestIdToken] Received an invalid Google ID token credential")
            GoogleCredentialResult.Failed
        }
    }

    override suspend fun forgetAccount() {
        try {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        } catch (exception: ClearCredentialException) {
            Timber.w(exception, "[forgetAccount] Couldn't clear Credential Manager's state")
        }
    }
}
