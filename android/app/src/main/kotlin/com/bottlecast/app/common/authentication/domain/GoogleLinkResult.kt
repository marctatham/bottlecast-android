package com.bottlecast.app.common.authentication.domain

/** How signing in with Google went for whoever was on the phone (spec §3.2). */
enum class GoogleLinkResult {

    /** The guest's account gained Google and kept its uid, so its finds and casts stay with it. */
    Linked,

    /** Nobody was signed in, so the Google account simply signed in. */
    SignedIn,

    /**
     * That Google account already exists, so the guest couldn't be linked to it and is still signed
     * in. [IAuthenticationRepository.switchToGoogle] moves to the Google account.
     */
    AccountExists,
}
