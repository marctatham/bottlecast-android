package com.bottlecast.app.common.authentication.domain

import java.time.Instant

sealed class UserEntity {

    data class AuthenticatedUser(
        val providerType: AuthProviderType,
        val jwtToken: String,
        val userId: String,
        val isAdmin: Boolean,
        /** When the account was made: the Me tab's "casting since" (spec §3.12). */
        val createdAt: Instant? = null,
        /** The Google account's name, which only Settings › Account shows (decided 2026-10-15). Never logged. */
        val googleName: String? = null,
    ) : UserEntity() {
        // never in a log
        override fun toString(): String = "AuthenticatedUser($providerType, admin=$isAdmin)"
    }

    data object UnauthenticatedUser : UserEntity()
}

/**
 * The uid when this is a Google account, or null for a guest or nobody. Only a Google account casts,
 * or has a profile (spec §3.2).
 */
val UserEntity.googleUid: String?
    get() = (this as? UserEntity.AuthenticatedUser)?.takeIf { it.providerType == AuthProviderType.Google }?.userId
