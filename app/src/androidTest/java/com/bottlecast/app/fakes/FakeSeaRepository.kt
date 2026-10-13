package com.bottlecast.app.fakes

import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.FindStatus
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
        alreadyFoundToday?.let { return FindResult.AlreadyFound(it) }
        if (seaCalm) return FindResult.SeaCalm

        val found = SEEDED.copy(status = MessageStatus.Found, findCount = 1, foundAt = Instant.now())
        _finds += found.id
        today.value = TodayFind(LocalDate.now(), found.id, found, FindStatus.Held)
        return FindResult.Found(found)
    }

    override fun cachedMessage(messageId: String): MessageEntity? = SEEDED.takeIf { it.id == messageId }

    override suspend fun getMessage(messageId: String): MessageEntity? = cachedMessage(messageId)

    /** What a committed throw back or report leaves on today's find. */
    fun setTodayStatus(status: FindStatus) {
        today.value = today.value?.copy(status = status)
    }

    companion object {
        const val SEEDED_MESSAGE: String = "Ahoy! This bottle washed up from the test sea."
        const val SEEDED_ID: String = "seededBottle00000001"

        val SEEDED = MessageEntity(
            id = SEEDED_ID,
            message = SEEDED_MESSAGE,
            userId = "writer",
            createdAt = Instant.now().minus(3, ChronoUnit.DAYS),
        )
    }
}
