package com.bottlecast.app.common.profile.data.mapper

import com.bottlecast.app.common.profile.data.model.ProfileFields
import com.bottlecast.app.common.profile.domain.UserProfile
import javax.inject.Inject

class ProfileMapper @Inject constructor() {

    /** A missing document is an empty profile; a field of the wrong type reads as missing. */
    fun mapFrom(data: Map<String, Any?>?): UserProfile = UserProfile(
        penName = (data?.get(ProfileFields.PEN_NAME) as? String)?.takeIf { it.isNotBlank() },
        termsVersion = (data?.get(ProfileFields.TERMS_VERSION) as? Number)?.toInt(),
    )
}
