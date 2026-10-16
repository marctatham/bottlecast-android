package com.bottlecast.app.fakes

import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.domain.BlockedWriter
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.Handover
import com.bottlecast.app.common.sea.domain.HandoverResult
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.TodayFind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for the sea, holding one seeded bottle. Tests switch it to a calm sea, a day
 * that already has a find, or a find that fails, and can hold a find back with [findGate] to see
 * the late-data state, rather than adding delays.
 */
@Singleton
class FakeSeaRepository @Inject constructor(
    private val authenticationRepository: FakeAuthenticationRepository,
) : ISeaRepository {

    @Volatile var seaCalm: Boolean = false
    @Volatile var alreadyFoundToday: String? = null
    @Volatile var failNextFind: Boolean = false
    @Volatile var findGate: CompletableDeferred<Unit>? = null

    private val _finds = CopyOnWriteArrayList<String>()
    val finds: List<String> get() = _finds.toList()

    private val today = MutableStateFlow<TodayFind?>(null)

    private val _candidate = MutableStateFlow<CandidateState>(CandidateState.Unknown)
    override val candidate: StateFlow<CandidateState> = _candidate.asStateFlow()

    override suspend fun prefetchCandidate(): CandidateState {
        authenticationRepository.ensureSignedIn()
        return (if (seaCalm) CandidateState.SeaCalm else CandidateState.Available(SEEDED)).also { _candidate.value = it }
    }

    override fun observeToday(date: LocalDate): Flow<TodayFind?> = today

    override suspend fun findTodaysBottle(): FindResult {
        authenticationRepository.ensureSignedIn()
        findGate?.await()
        if (failNextFind) {
            failNextFind = false
            return FindResult.Failed
        }
        // as the real sea answers: a daily record naming another bottle is the day's find, one naming
        // this bottle means an earlier attempt went through, so it's found again
        alreadyFoundToday?.takeIf { it != SEEDED_ID }?.let { return FindResult.AlreadyFound(it) }
        if (seaCalm) return FindResult.SeaCalm

        val found = SEEDED.copy(status = MessageStatus.Found, findCount = 1, foundAt = Instant.now())
        _finds += found.id
        today.value = TodayFind(LocalDate.now(), found.id, found, FindStatus.Held)
        return FindResult.Found(found)
    }

    override fun cachedMessage(messageId: String): MessageEntity? = SEEDED.takeIf { it.id == messageId }

    override suspend fun getMessage(messageId: String): MessageEntity? = cachedMessage(messageId)

    /** How many handovers were claimed: a guest signing in to an existing Google account brought today's find. */
    @Volatile var handovers: Int = 0
        private set

    // a guest's held find hands over; the fake's one day of finds simply stays with the new account
    override suspend fun prepareHandover(): Handover? {
        val guest = authenticationRepository.user.value as? UserEntity.AuthenticatedUser
        val find = today.value
        if (guest?.providerType != AuthProviderType.Anonymous || find?.status != FindStatus.Held) return null
        return Handover(find.messageId, guest.userId, "fakeToken")
    }

    override suspend fun claimHandover(handover: Handover, uid: String): HandoverResult {
        handovers += 1
        return HandoverResult.HandedOver
    }

    override suspend fun hasFoundToday(): Boolean = today.value != null

    /** The writers the account blocked; Settings lists them and can unblock one. */
    val blocked = MutableStateFlow<List<BlockedWriter>>(emptyList())

    override val blockedWriters: Flow<List<BlockedWriter>> = blocked

    override suspend fun unblock(writerUid: String) {
        blocked.value = blocked.value.filterNot { it.writerUid == writerUid }
    }

    /** Today's bottle was found on another phone, and it was another bottle, [messageId]. */
    fun foundElsewhere(messageId: String) {
        alreadyFoundToday = messageId
        val elsewhere = MessageEntity(
            id = messageId,
            message = ELSEWHERE_MESSAGE,
            userId = "writer",
            status = MessageStatus.Found,
            foundAt = Instant.now(),
        )
        today.value = TodayFind(LocalDate.now(), messageId, elsewhere, FindStatus.Held)
    }

    /** What a committed throw back or report leaves on today's find. */
    fun setTodayStatus(status: FindStatus) {
        today.value = today.value?.copy(status = status)
    }

    companion object {
        const val SEEDED_MESSAGE: String = "Ahoy! This bottle washed up from the test sea."
        const val SEEDED_ID: String = "seededBottle00000001"
        const val ELSEWHERE_MESSAGE: String = "Found this one on the other phone."

        val SEEDED = MessageEntity(
            id = SEEDED_ID,
            message = SEEDED_MESSAGE,
            userId = "writer",
            createdAt = Instant.now().minus(3, ChronoUnit.DAYS),
        )
    }
}
