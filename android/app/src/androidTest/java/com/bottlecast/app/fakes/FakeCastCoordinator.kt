package com.bottlecast.app.fakes

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.messages.domain.CastOutcome
import com.bottlecast.app.common.messages.domain.CastStatus
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.common.messages.domain.MessageLimits
import com.bottlecast.app.common.messages.domain.NewCast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Casting with a server that answers at once: yes, unless [refuseNext]. Offline, a cast is queued until
 * [confirmQueued], as Firestore would send it when the phone is back online.
 */
@Singleton
class FakeCastCoordinator @Inject constructor(
    private val authenticationRepository: FakeAuthenticationRepository,
    private val connectivityMonitor: FakeConnectivityMonitor,
    private val drafts: FakeDraftRepository,
) : ICastCoordinator {

    val left = MutableStateFlow(MessageLimits.CASTS_PER_DAY)
    override val castsLeftToday: StateFlow<Int?> get() = left

    /** The server turns the next cast down. */
    @Volatile var refuseNext = false

    private val _casts = CopyOnWriteArrayList<NewCast>()
    val casts: List<NewCast> get() = _casts.toList()

    private val statuses = ConcurrentHashMap<String, MutableStateFlow<CastStatus>>()

    override suspend fun cast(cast: NewCast): CastOutcome {
        val user = authenticationRepository.user.value as? UserEntity.AuthenticatedUser
        if (user?.providerType != AuthProviderType.Google) return CastOutcome.NotSignedIn
        if (left.value <= 0) return CastOutcome.LimitReached
        drafts.setAside(cast.messageId)
        if (refuseNext) {
            refuseNext = false
            drafts.restore(cast.messageId)
            return CastOutcome.Refused
        }
        _casts += cast
        left.value -= 1
        val queued = !connectivityMonitor.isOnline.value
        statuses[cast.messageId] = MutableStateFlow(if (queued) CastStatus.Queued else CastStatus.Confirmed)
        if (!queued) drafts.forget(cast.messageId)
        return CastOutcome.Cast(queued)
    }

    override fun status(messageId: String): Flow<CastStatus> = statuses.getOrPut(messageId) { MutableStateFlow(CastStatus.Confirmed) }

    /** The phone's back online, and the server has every queued cast. */
    fun confirmQueued() {
        statuses.values.forEach { it.value = CastStatus.Confirmed }
    }
}
