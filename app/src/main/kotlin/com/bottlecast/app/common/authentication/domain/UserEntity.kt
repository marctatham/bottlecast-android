package com.bottlecast.app.common.authentication.domain

sealed class UserEntity {

    data class AuthenticatedUser(
        val providerType: AuthProviderType,
        val jwtToken: String,
        val userId: String,
        val isAdmin: Boolean
    ) : UserEntity()

    data object UnauthenticatedUser : UserEntity()
}

/**
 * The uid when this is a Google account, or null for a guest or nobody. Only a Google account casts,
 * or has a profile (spec §3.2).
 */
val UserEntity.googleUid: String?
    get() = (this as? UserEntity.AuthenticatedUser)?.takeIf { it.providerType == AuthProviderType.Google }?.userId
