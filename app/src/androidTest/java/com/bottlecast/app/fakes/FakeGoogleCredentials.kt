package com.bottlecast.app.fakes

import android.content.Context
import com.bottlecast.app.common.authentication.domain.GoogleCredentialResult
import com.bottlecast.app.common.authentication.domain.IGoogleCredentials
import javax.inject.Inject
import javax.inject.Singleton

/** Google's account picker, answered at once with [next]: an account's token unless a test says otherwise. */
@Singleton
class FakeGoogleCredentials @Inject constructor() : IGoogleCredentials {

    var next: GoogleCredentialResult = GoogleCredentialResult.Token("fakeIdToken")

    var requests = 0
        private set

    override suspend fun requestIdToken(context: Context): GoogleCredentialResult {
        requests += 1
        return next
    }
}
