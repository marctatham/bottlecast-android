package com.bottlecast.app.common.profile.data

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.UserProfile
import com.bottlecast.app.common.threading.DispatcherIO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val source: IProfileDataSource,
    private val authenticationRepository: IAuthenticationRepository,
    @param:DispatcherIO private val dispatcher: CoroutineDispatcher,
) : IProfileRepository {

    // only Google accounts have a profile; a guest's would be refused
    private val googleUid: Flow<String?> = authenticationRepository.user
        .map { it.googleUid }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val profile: Flow<UserProfile?> = googleUid.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else source.observeProfile(uid)
    }

    override suspend fun savePenName(name: String) = withContext(dispatcher) {
        val uid = authenticationRepository.user.value.googleUid
        if (uid == null) {
            Timber.e("[savePenName] Only a Google account has a pen name")
            return@withContext
        }
        source.savePenName(uid, name.trim())
    }
}
