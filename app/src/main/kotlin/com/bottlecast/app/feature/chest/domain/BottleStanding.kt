package com.bottlecast.app.feature.chest.domain

import com.bottlecast.app.common.chest.domain.FoundBottle
import com.bottlecast.app.common.chest.domain.daysLeftUntil
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.messages.domain.MessageStatus
import com.bottlecast.app.common.messages.domain.REPORT_THRESHOLD
import com.bottlecast.app.common.messages.domain.isHoldLive
import java.time.Instant

/** Where a bottle in the chest stands, as its chip says it (spec §1.7, §3.10). */
sealed interface BottleStanding {

    /** Found and not kept: it drifts back in [daysLeft] days. */
    data class NotKept(val daysLeft: Int) : BottleStanding

    data object Kept : BottleStanding

    /** One of your own, afloat in the sea. */
    data object Drifting : BottleStanding

    /** One of your own, held by a finder who hasn't kept it. */
    data object Found : BottleStanding

    /** Out of the sea while a moderator looks, or reported enough times to wait for one. */
    data object UnderReview : BottleStanding

    /** Taken out of the sea for breaking the community guidelines; its words are hidden. */
    data object Removed : BottleStanding
}

/** A bottle this account found, as it stands now. */
fun FoundBottle.standing(now: Instant): BottleStanding = when {
    message?.status == MessageStatus.UnderReview -> BottleStanding.UnderReview
    // a kept bottle that can't be read any more was removed
    isKept && message == null -> BottleStanding.Removed
    isKept -> BottleStanding.Kept
    else -> BottleStanding.NotKept(driftsBackAt?.let { daysLeftUntil(it, now) } ?: 0)
}

/** A bottle this account cast, as it stands now. The writer never learns who holds or kept it. */
fun MessageEntity.castStanding(now: Instant): BottleStanding = when {
    status == MessageStatus.Removed -> BottleStanding.Removed
    status == MessageStatus.UnderReview || reportCount >= REPORT_THRESHOLD -> BottleStanding.UnderReview
    status == MessageStatus.Kept -> BottleStanding.Kept
    isHoldLive(now) -> BottleStanding.Found
    else -> BottleStanding.Drifting
}
