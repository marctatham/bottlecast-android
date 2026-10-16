package com.bottlecast.app.fakes

import com.bottlecast.app.common.streak.domain.ICalmSeaLog
import com.bottlecast.app.common.streak.domain.IStreakRepository
import com.bottlecast.app.common.streak.domain.Streak
import com.bottlecast.app.common.streak.domain.StreakEvent
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory stand-in for the streak: whatever a test sets, and a record of what was carried. */
@Singleton
class FakeStreakRepository @Inject constructor() : IStreakRepository, ICalmSeaLog {

    override val streak = MutableStateFlow<Streak?>(Streak.None)

    override val notice = MutableStateFlow<StreakEvent?>(null)

    private val _carried = CopyOnWriteArrayList<Pair<String, Int>>()
    val carried: List<Pair<String, Int>> get() = _carried.toList()

    override fun acknowledge(event: StreakEvent) {
        if (notice.value == event) notice.value = null
    }

    override suspend fun carryable(): Int = streak.value?.carryable ?: 0

    override suspend fun carry(uid: String, days: Int) {
        if (days > 0) _carried += uid to days
    }

    override fun markCalm(uid: String, date: LocalDate) = Unit
}
