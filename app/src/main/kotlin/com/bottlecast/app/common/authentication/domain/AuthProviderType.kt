package com.bottlecast.app.common.authentication.domain

/** How an account signed in: as a guest, or with Google (which a guest may link later). */
sealed class AuthProviderType {

    data object Anonymous : AuthProviderType()

    data object Google : AuthProviderType()
}
