package com.bottlecast.app.fakes

import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A profile in memory. It starts with no pen name and the Terms unaccepted, as a first cast's does. */
@Singleton
class FakeProfileRepository @Inject constructor() : IProfileRepository {

    val current = MutableStateFlow<UserProfile?>(UserProfile())

    override val profile: Flow<UserProfile?> = current

    override suspend fun savePenName(name: String) {
        current.value = (current.value ?: UserProfile()).copy(penName = name.trim())
    }
}
