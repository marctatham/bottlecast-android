package com.bottlecast.app.common.sea.data

import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.ITracker
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.isHoldLive
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.sea.domain.SeaActionOutcome
import com.bottlecast.app.common.threading.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * How long an action waits for its Undo when it has no window of its own (TalkBack is on), before it's
 * written anyway, so a forgotten Undo doesn't keep it from the server for good.
 */
internal const val SAFETY_WINDOW_MILLIS = 120_000L

/** How long settling waits for the server before the account switches anyway. */
internal const val SETTLE_TIMEOUT_MILLIS = 5_000L

/**
 * Holds each throw back or report for its undo window, then writes it. If the process dies first, the
 * action is lost: the bottle stays held until its hold ends, and then drifts back as usual.
 *
 * An action counts for analytics once it's written, not when it's chosen, so an undone one doesn't. It
 * may be written after its screen has gone, which is why it's tracked here rather than through
 * LocalTracker.
 */
class SeaActions @Inject constructor(
    private val source: ISeaDataSource,
    private val authenticationRepository: IAuthenticationRepository,
    private val connectivityMonitor: IConnectivityMonitor,
    private val clock: Clock,
    private val tracker: ITracker,
    @param:ApplicationScope private val scope: CoroutineScope,
) : ISeaActions {

    private class Scheduled(val action: PendingSeaAction, val uid: String, val write: (uid: String) -> Unit) {
        var timer: Job? = null
    }

    private val lock = Any()
    private val scheduled = mutableMapOf<String, Scheduled>()

    private val _pending = MutableStateFlow<PendingSeaAction?>(null)
    override val pending: StateFlow<PendingSeaAction?> = _pending.asStateFlow()

    private val _outcomes = MutableSharedFlow<SeaActionOutcome>(extraBufferCapacity = 8)
    override val outcomes: SharedFlow<SeaActionOutcome> = _outcomes.asSharedFlow()

    override fun throwBack(message: MessageEntity, windowMillis: Long?): String =
        schedule(PendingSeaAction.ThrowBack(newId(), message.id), windowMillis) { uid ->
            // Once the hold has ended the bottle is already back in the sea, and the rules won't
            // release it again, so only the find is marked. An orphan never goes back to the sea.
            val releaseHold = message.status == MessageStatus.Found && message.isHoldLive(clock.instant())
            source.commitThrowBack(message.id, uid, releaseHold = releaseHold)
        }

    override fun report(
        message: MessageEntity,
        reason: ReportReason?,
        blockWriter: Boolean,
        windowMillis: Long?,
    ): String = schedule(PendingSeaAction.Report(newId(), message.id, blockWriter), windowMillis) { uid ->
        source.commitReport(message.id, uid, reason, blockWriter)
        if (blockWriter && message.userId.isNotEmpty() && message.userId != uid) {
            source.blockWriter(uid, message.userId, message.id)
        }
    }

    override fun undo(actionId: String) {
        synchronized(lock) { take(actionId) } ?: return
        Timber.i("Undid a sea action")
    }

    override fun commit(actionId: String) {
        val entry = synchronized(lock) { take(actionId) } ?: return
        try {
            entry.write(entry.uid)
        } catch (exception: Exception) {
            Timber.e(exception, "Couldn't write a sea action")
            return
        }
        _outcomes.tryEmit(SeaActionOutcome(entry.action, queuedOffline = !connectivityMonitor.isOnline.value))
        when (val action = entry.action) {
            is PendingSeaAction.ThrowBack -> tracker.trackEvent(AnalyticsEvent.BottleThrownBack)
            is PendingSeaAction.Report -> tracker.trackEvent(
                AnalyticsEvent.BottleReported,
                mapOf(AnalyticsEvent.PARAM_BLOCKED to action.blockWriter.toString()),
            )
        }
    }

    override suspend fun settle() {
        synchronized(lock) { scheduled.keys.toList() }.forEach(::commit)
        // offline, the writes stay queued for this account, which is all there is to do
        withTimeoutOrNull(SETTLE_TIMEOUT_MILLIS) { source.awaitPendingWrites() }
            ?: Timber.w("[settle] The server hadn't taken every sea action before the account switched")
    }

    private fun schedule(action: PendingSeaAction, windowMillis: Long?, write: (uid: String) -> Unit): String {
        // one action waits at a time, so whatever was waiting is written first
        synchronized(lock) { scheduled.keys.toList() }.forEach(::commit)

        val uid = (authenticationRepository.user.value as? UserEntity.AuthenticatedUser)?.userId
        if (uid == null) {
            Timber.e("Nobody is signed in, so a sea action was dropped")
            return action.id
        }
        val entry = Scheduled(action, uid, write)
        synchronized(lock) {
            scheduled[action.id] = entry
            _pending.value = action
        }
        entry.timer = scope.launch {
            delay(windowMillis ?: SAFETY_WINDOW_MILLIS)
            commit(action.id)
        }
        return action.id
    }

    // Removes an action from the schedule, under the lock.
    private fun take(actionId: String): Scheduled? {
        val entry = scheduled.remove(actionId) ?: return null
        entry.timer?.cancel()
        if (_pending.value?.id == actionId) _pending.value = null
        return entry
    }

    private fun newId() = UUID.randomUUID().toString()
}
