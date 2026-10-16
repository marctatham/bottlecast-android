package com.bottlecast.app.common.chest.domain

import com.bottlecast.app.common.messages.domain.HOLD_DAYS
import com.bottlecast.app.common.messages.domain.MessageEntity
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A bottle in the chest's Found tab (spec §3.10): one this account holds and hasn't kept yet, which
 * drifts back at [driftsBackAt], or one it kept.
 */
data class FoundBottle(
    val messageId: String,
    /** Null once the bottle can't be read: a moderator removed it. */
    val message: MessageEntity?,
    /** When this account's hold began, or null when that can't be read any more. */
    val foundAt: Instant?,
    /** When this account kept it, or null while it's Not kept. */
    val keptAt: Instant?,
) {
    val isKept: Boolean get() = keptAt != null

    /** When an unkept bottle drifts back to the sea, or null once it's kept. */
    val driftsBackAt: Instant? get() = if (keptAt == null) foundAt?.plus(HOLD_DAYS, ChronoUnit.DAYS) else null
}

/** One bottle as Keepsake detail shows it (spec §3.11): one this account found, or one it cast. */
sealed interface ChestItem {
    data class Found(val bottle: FoundBottle) : ChestItem

    data class Cast(val message: MessageEntity) : ChestItem
}

/** As much of a growing list as has been read: [items] so far, and whether there's more past them. */
data class ChestPage<T>(val items: List<T>, val hasMore: Boolean)

/** The Shore's and Me's stats (spec §3.3, §3.12). */
data class ChestCounts(
    /** Null for a guest, who can't cast. */
    val bottled: Int?,
    val found: Int,
    val kept: Int,
)

enum class KeepResult {
    Kept,

    /** Keeping needs a connection (spec §3.8a). */
    Offline,

    /** The hold isn't this account's any more: it ran out, or it was thrown back or reported. */
    NotHeld,

    Failed,
}

enum class PullResult {
    Pulled,

    Offline,

    /** Someone found it, or it's out for review, since the writer last looked. */
    NotDrifting,

    Failed,
}

/** Whole days left until [end], counting a part day as one: "Not kept · 5d left" (spec §1.7). */
fun daysLeftUntil(end: Instant, now: Instant): Int {
    val left = Duration.between(now, end)
    if (left <= Duration.ZERO) return 0
    val days = left.toDays()
    return (if (left > Duration.ofDays(days)) days + 1 else days).toInt()
}
