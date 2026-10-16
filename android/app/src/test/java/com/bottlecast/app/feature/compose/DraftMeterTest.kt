package com.bottlecast.app.feature.compose

import com.bottlecast.app.feature.compose.domain.ComposeHelper
import com.bottlecast.app.feature.compose.domain.DailyPrompt
import com.bottlecast.app.feature.compose.domain.DraftMeter
import com.bottlecast.app.feature.compose.domain.composeHelper
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate

/** The counter and helper under Compose's editor (spec §3.4). */
class DraftMeterTest {

    @Test
    fun `whitespace never meets the minimum`() {
        // given
        val meter = DraftMeter.of("   short   ")

        // when
        val helper = composeHelper(meter, minimumTapped = false, lang = null)

        // then
        Assert.assertFalse(meter.meetsMinimum)
        Assert.assertEquals(ComposeHelper.Minimum(asError = false), helper)
    }

    @Test
    fun `the minimum turns to an error only once Preview is tapped`() {
        // given
        val meter = DraftMeter.of("Hello sea")

        // when
        val helper = composeHelper(meter, minimumTapped = true, lang = null)

        // then
        Assert.assertEquals(ComposeHelper.Minimum(asError = true), helper)
    }

    @Test
    fun `the counter counts down from 220 characters`() {
        // given
        val before = DraftMeter.of("a".repeat(219))
        val from = DraftMeter.of("a".repeat(220))

        // then
        Assert.assertFalse(before.countingDown)
        Assert.assertTrue(from.countingDown)
        Assert.assertEquals(20, from.left)
    }

    @Test
    fun `emoji run the bytes out first, and say so`() {
        // given: 30 letters and 58 emoji, 262 bytes
        val meter = DraftMeter.of("The tide came in gold tonight " + "🌊🌙".repeat(29))

        // when
        val helper = composeHelper(meter, minimumTapped = false, lang = "en")

        // then
        Assert.assertEquals(18, meter.left)
        Assert.assertTrue(meter.bytesBind)
        Assert.assertEquals(ComposeHelper.EmojiRoom, helper)
    }

    @Test
    fun `a full bottle says it's the limit`() {
        // given
        val meter = DraftMeter.of("a".repeat(240))

        // when
        val helper = composeHelper(meter, minimumTapped = false, lang = "en")

        // then
        Assert.assertTrue(meter.atLimit)
        Assert.assertEquals(ComposeHelper.AtLimit, helper)
    }

    @Test
    fun `a detected language shows once there's enough to tell, and an undetermined one doesn't`() {
        // given
        val meter = DraftMeter.of("If you're reading this, drink some water.")

        // then
        Assert.assertEquals(ComposeHelper.Language("en"), composeHelper(meter, minimumTapped = false, lang = "en"))
        Assert.assertEquals(ComposeHelper.None, composeHelper(meter, minimumTapped = false, lang = "und"))
    }

    @Test
    fun `the placeholder changes each day, and comes round again`() {
        // given
        val day = LocalDate.of(2026, 10, 14)

        // when
        val prompts = (0L until 3L).map { DailyPrompt.forDate(day.plusDays(it)) }

        // then
        Assert.assertEquals(3, prompts.toSet().size)
        Assert.assertEquals(DailyPrompt.forDate(day), DailyPrompt.forDate(day.plusDays(3)))
    }
}
