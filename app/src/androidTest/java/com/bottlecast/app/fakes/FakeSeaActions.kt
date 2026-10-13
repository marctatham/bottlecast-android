package com.bottlecast.app.fakes

import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.PendingSeaAction
import com.bottlecast.app.common.sea.domain.ReportReason
import com.bottlecast.app.common.sea.domain.SeaActionOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds throw backs and reports until a test ends their window with [expireAll], or the app commits or
 * undoes them, so undo windows never need waiting out.
 */
@Singleton
class FakeSeaActions @Inject constructor(
    private val seaRepository: FakeSeaRepository,
) : ISeaActions {

    override val pending = MutableStateFlow<PendingSeaAction?>(null)
    override val outcomes = MutableSharedFlow<SeaActionOutcome>(extraBufferCapacity = 8)

    private val _committed = CopyOnWriteArrayList<PendingSeaAction>()
    val committed: List<PendingSeaAction> get() = _committed.toList()

    override fun throwBack(message: MessageEntity, windowMillis: Long?): String =
        schedule(PendingSeaAction.ThrowBack(UUID.randomUUID().toString(), message.id))

    override fun report(message: MessageEntity, reason: ReportReason?, blockWriter: Boolean, windowMillis: Long?): String =
        schedule(PendingSeaAction.Report(UUID.randomUUID().toString(), message.id, blockWriter))

    override fun undo(actionId: String) {
        if (pending.value?.id == actionId) pending.value = null
    }

    override fun commit(actionId: String) {
        val action = pending.value?.takeIf { it.id == actionId } ?: return
        pending.value = null
        _committed += action
        seaRepository.setTodayStatus(if (action is PendingSeaAction.ThrowBack) FindStatus.ThrownBack else FindStatus.Reported)
        outcomes.tryEmit(SeaActionOutcome(action, queuedOffline = false))
    }

    /** Ends the undo window of whatever is waiting, as time running out would. */
    fun expireAll() {
        pending.value?.let { commit(it.id) }
    }

    private fun schedule(action: PendingSeaAction): String {
        expireAll()
        pending.value = action
        return action.id
    }
}
