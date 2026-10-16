package com.bottlecast.app.common.authentication.domain

import android.content.Context

/** Google's account picker, through Credential Manager (spec §3.2). Instrumented tests fake it. */
interface IGoogleCredentials {

    /** Shows the picker over [context], an activity, and returns the chosen account's ID token. */
    suspend fun requestIdToken(context: Context): GoogleCredentialResult

    /** Forgets the account chosen last, on signing out, so the picker asks again next time. */
    suspend fun forgetAccount()
}

sealed interface GoogleCredentialResult {

    class Token(val idToken: String) : GoogleCredentialResult {
        // never in a log
        override fun toString(): String = "Token(…)"
    }

    /** The person closed the picker; the sheet stays, with no error. */
    data object Cancelled : GoogleCredentialResult

    /** No Google account or provider on this phone. */
    data object Unavailable : GoogleCredentialResult

    data object Failed : GoogleCredentialResult
}
