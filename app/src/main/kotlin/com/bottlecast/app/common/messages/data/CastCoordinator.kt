package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.CastOutcome
import com.bottlecast.app.common.messages.domain.CastStatus
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.MessageLimits
import com.bottlecast.app.common.messages.domain.NewCast
import com.bottlecast.app.common.threading.ApplicationScope
import com.bottlecast.app.common.threading.DispatcherIO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** How long Preview waits online for the server's answer before showing the cast as queued. */
internal const val CAST_ACK_WINDOW_MILLIS = 8_000L

/**
 * Casts from Preview and follows each cast to the server's answer, in the app's scope: a queued cast
 * can be confirmed, or refused, long after Preview has gone. Until then its words are set aside, and
 * a refusal brings them back to the draft (spec §3.4b: "Your words are saved"). If the app dies first,
 * Firestore still sends the cast on the next launch, but a refusal then is the tidy-up slice's.
 *
 * A cast counts for analytics once the server has it, so it's tracked here rather than through
 * LocalTracker.
 */
@Singleton
class CastCoordinator @Inject constructor(
    private val source: ICastDataSource,
    private val drafts: IDraftRepository,
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
    private val random: Random,
    private val tracker: ITracker,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:DispatcherIO private val dispatcher: CoroutineDispatcher,
) : ICastCoordinator {

    private val googleUid: Flow<String?> = authenticationRepository.user
        .map { googleUidOf(it) }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val castsLeftToday: StateFlow<Int?> = googleUid
        .flatMapLatest { uid -> if (uid == null) flowOf(null) else source.observeTally(uid).map { castsLeft(it) } }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    override suspend fun cast(cast: NewCast): CastOutcome {
        val uid = googleUidOf(authenticationRepository.user.value) ?: return CastOutcome.NotSignedIn
        val today = LocalDate.now(clock)
        val tally = try {
            withContext(dispatcher) { source.readTally(uid) }
        } catch (exception: Exception) {
            // nothing to go on, so it counts as the day's first; the rules have the last word
            Timber.w(exception, "[cast] Couldn't read the cast tally")
            null
        }
        val sameDay = tally?.date == today
        if (sameDay && tally!!.count >= MessageLimits.CASTS_PER_DAY) return CastOutcome.LimitReached

        val offline = !connectivityMonitor.isOnline.value
        drafts.setAside(cast.messageId)
        val pending = withContext(dispatcher) {
            source.writeCast(uid, cast, random.nextDouble(), if (sameDay) TallyWrite.Increment(today) else TallyWrite.Start(today))
        }
        val ack = scope.async { pending.awaitServer() }
        // the answer is handled here whenever it comes, whether or not Preview is still waiting
        val settled = scope.launch {
            when (ack.await()) {
                CastAck.Confirmed -> {
                    drafts.forget(cast.messageId)
                    tracker.trackEvent(
                        AnalyticsEvent.BottleCast,
                        mapOf(
                            AnalyticsEvent.PARAM_QUEUED to offline.toString(),
                            AnalyticsEvent.PARAM_SIGNED to (cast.penName != null).toString(),
                            AnalyticsEvent.PARAM_SEALABLE to cast.sealable.toString(),
                        ),
                    )
                }
                CastAck.Refused -> {
                    drafts.restore(cast.messageId)
                    tracker.trackEvent(AnalyticsEvent.CastRefused)
                }
            }
        }

        if (offline) return CastOutcome.Cast(queued = true)
        return when (withTimeoutOrNull(CAST_ACK_WINDOW_MILLIS) { ack.await() }) {
            CastAck.Confirmed -> CastOutcome.Cast(queued = false)
            CastAck.Refused -> {
                // the words are back in the draft before Preview offers to try again
                settled.join()
                CastOutcome.Refused
            }
            null -> CastOutcome.Cast(queued = true)
        }
    }

    override fun status(messageId: String): Flow<CastStatus> = source.observeCast(messageId)

    private fun castsLeft(tally: CastTally?): Int {
        val today = LocalDate.now(clock)
        val castToday = if (tally?.date == today) tally.count else 0
        return (MessageLimits.CASTS_PER_DAY - castToday).coerceAtLeast(0)
    }

    private fun googleUidOf(user: UserEntity): String? =
        (user as? UserEntity.AuthenticatedUser)?.takeIf { it.providerType == AuthProviderType.Google }?.userId

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
