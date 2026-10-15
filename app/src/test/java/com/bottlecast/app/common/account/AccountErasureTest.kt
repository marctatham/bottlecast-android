package com.bottlecast.app.common.account

import com.bottlecast.app.common.account.domain.BottleErasure
import com.bottlecast.app.common.account.domain.OwnBottle
import com.bottlecast.app.common.account.domain.erasureOf
import org.junit.Assert
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Unit tests for [erasureOf]: which of a deleted account's bottles stay with someone, anonymised. */
class AccountErasureTest {

    private val now = Instant.parse("2026-10-15T09:00:00Z")

    @Test
    fun `a bottle someone kept, or whose finder's hold still runs, is anonymised`() {
        // given
        val kept = OwnBottle("kept", "kept", foundAt = now.minus(Duration.ofDays(30)), kept = true)
        val held = OwnBottle("held", "found", foundAt = now.minus(Duration.ofDays(6)), kept = false)
        val heldUnderReview = OwnBottle("review", "under_review", foundAt = now.minus(Duration.ofHours(1)), kept = false)

        // when
        val erasures = listOf(kept, held, heldUnderReview).map { erasureOf(it, now) }

        // then
        Assert.assertEquals(List(3) { BottleErasure.Anonymise }, erasures)
    }

    @Test
    fun `a drifting bottle, or one whose hold has ended, is deleted`() {
        // given
        val drifting = OwnBottle("drifting", "afloat", foundAt = null, kept = false)
        val expired = OwnBottle("expired", "found", foundAt = now.minus(Duration.ofDays(7)), kept = false)
        val removed = OwnBottle("removed", "removed", foundAt = null, kept = false)

        // when
        val erasures = listOf(drifting, expired, removed).map { erasureOf(it, now) }

        // then
        Assert.assertEquals(List(3) { BottleErasure.Delete }, erasures)
    }
}
