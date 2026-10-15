package com.bottlecast.app.fakes

import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.chest.domain.ChestCounts
import com.bottlecast.app.common.chest.domain.ChestItem
import com.bottlecast.app.common.chest.domain.ChestPage
import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.IChestRepository
import com.bottlecast.app.common.chest.domain.KeepResult
import com.bottlecast.app.common.chest.domain.PullResult
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.sea.domain.FindStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for the chest, over [FakeSeaRepository]'s one day of finds: today's held find is
 * Not kept until it's kept, and the casts are whatever a test puts in [casts]. A keep answers at once,
 * or refuses once after [refuseNextKeep].
 */
@Singleton
class FakeChestRepository @Inject constructor(
    private val seaRepository: FakeSeaRepository,
    private val authenticationRepository: FakeAuthenticationRepository,
) : IChestRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val today = seaRepository.observeToday(LocalDate.now())
    private val keptAt = MutableStateFlow<Map<String, Instant>>(emptyMap())

    /** The bottles the signed-in account cast. */
    val casts = MutableStateFlow<List<MessageEntity>>(emptyList())

    @Volatile var refuseNextKeep: Boolean = false

    private val _keeps = CopyOnWriteArrayList<String>()
    val keeps: List<String> get() = _keeps.toList()

    @Volatile var viewed: Int = 0
        private set

    override suspend fun keep(messageId: String): KeepResult {
        if (authenticationRepository.user.value.googleUid == null) return KeepResult.NotHeld
        if (refuseNextKeep) {
            refuseNextKeep = false
            return KeepResult.NotHeld
        }
        _keeps += messageId
        keptAt.value = keptAt.value + (messageId to Instant.now())
        seaRepository.setTodayStatus(FindStatus.Kept)
        return KeepResult.Kept
    }

    override val notKept: StateFlow<List<FoundBottle>?> = today
        .map { find ->
            val message = find?.message
            if (find?.status == FindStatus.Held && message != null) {
                listOf(FoundBottle(find.messageId, message, message.foundAt, keptAt = null))
            } else {
                emptyList()
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override fun kept(limit: Int): Flow<ChestPage<FoundBottle>> = combine(keptAt, today) { kept, find ->
        val bottles = kept.mapNotNull { (id, at) ->
            val message = find?.message?.takeIf { it.id == id } ?: return@mapNotNull null
            FoundBottle(id, message.copy(status = MessageStatus.Kept, keptAt = at), message.foundAt, at)
        }
        ChestPage(bottles.take(limit), hasMore = false)
    }

    override fun cast(limit: Int): Flow<ChestPage<MessageEntity>> = casts.map { ChestPage(it.take(limit), hasMore = false) }

    override fun item(messageId: String): Flow<ChestItem?> = combine(today, keptAt, casts) { find, kept, casts ->
        val cast = casts.firstOrNull { it.id == messageId }
        val message = find?.message?.takeIf { it.id == messageId }
        when {
            cast != null -> ChestItem.Cast(cast)
            message != null && messageId in kept -> ChestItem.Found(FoundBottle(messageId, message, message.foundAt, kept.getValue(messageId)))
            message != null && find.status == FindStatus.Held -> ChestItem.Found(FoundBottle(messageId, message, message.foundAt, keptAt = null))
            else -> null
        }
    }

    override suspend fun pull(messageId: String): PullResult {
        if (casts.value.none { it.id == messageId }) return PullResult.NotDrifting
        casts.value = casts.value.filterNot { it.id == messageId }
        return PullResult.Pulled
    }

    override val counts: StateFlow<ChestCounts?> = combine(keptAt, casts) { kept, casts ->
        ChestCounts(bottled = casts.size, found = seaRepository.finds.size, kept = kept.size)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override fun refreshCounts() = Unit

    override val badge = MutableStateFlow(false)

    override val castFindsSinceViewed = MutableStateFlow(0)

    override fun markViewed() {
        viewed++
    }
}
