package com.bottlecast.app.common.sea

import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.data.FindOutcome
import com.bottlecast.app.common.sea.data.ISeaDataSource
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ReportReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate

/** An in-memory sea for the repository's and the actions' tests, recording what they read and write. */
class FakeSeaDataSource : ISeaDataSource {

    var drifting: List<MessageEntity> = emptyList()
    var expired: List<MessageEntity> = emptyList()
    val foundBefore = mutableSetOf<Pair<String, String>>()
    var blocked: Set<String> = emptySet()

    /** What recordFind answers for each bottle; Found by default. */
    val findOutcomes = mutableMapOf<String, () -> FindOutcome>()

    val seaQueries = mutableListOf<Pair<Double, Boolean>>()
    var blockedReads = 0
    val findAttempts = mutableListOf<String>()
    val throwBacks = mutableListOf<Triple<String, String, Boolean>>()
    val reports = mutableListOf<Pair<String, ReportReason?>>()
    val blocks = mutableListOf<String>()

    val daily = MutableStateFlow<String?>(null)
    val findStatus = MutableStateFlow<FindStatus?>(null)
    val message = MutableStateFlow<MessageEntity?>(null)

    override fun querySea(fromRand: Double, wrap: Boolean): List<MessageEntity> {
        seaQueries += fromRand to wrap
        return if (wrap) emptyList() else drifting
    }

    override fun queryExpiredHolds(before: Instant): List<MessageEntity> = expired

    override fun hasFound(messageId: String, uid: String): Boolean = (messageId to uid) in foundBefore

    override fun blockedWriters(uid: String): Set<String> = blocked.also { blockedReads++ }

    override fun recordFind(messageId: String, uid: String, date: LocalDate, zone: String, now: Instant): FindOutcome {
        findAttempts += messageId
        return findOutcomes[messageId]?.invoke()
            ?: FindOutcome.Found((drifting + expired).first { it.id == messageId })
    }

    override fun getMessage(messageId: String): MessageEntity? = (drifting + expired).firstOrNull { it.id == messageId }

    override fun observeDaily(uid: String, date: LocalDate): Flow<String?> = daily

    override fun observeFindStatus(messageId: String, uid: String): Flow<FindStatus?> = findStatus

    override fun observeMessage(messageId: String): Flow<MessageEntity?> = message

    override fun commitThrowBack(messageId: String, uid: String, releaseHold: Boolean) {
        throwBacks += Triple(messageId, uid, releaseHold)
    }

    override fun commitReport(messageId: String, uid: String, reason: ReportReason?, blockedWriter: Boolean) {
        reports += messageId to reason
    }

    override fun blockWriter(uid: String, writerUid: String, messageId: String) {
        blocks += writerUid
    }
}
