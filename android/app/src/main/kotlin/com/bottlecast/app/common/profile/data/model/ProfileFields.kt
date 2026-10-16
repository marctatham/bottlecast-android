package com.bottlecast.app.common.profile.data.model

/** users/{uid}: a Google account's profile. Writes merge, so a field may be missing. */
object ProfileFields {
    const val COLLECTION = "users"

    const val PEN_NAME = "penName"
    const val TERMS_VERSION = "termsVersion"
    const val TERMS_ACCEPTED_AT = "termsAcceptedAt"
}
