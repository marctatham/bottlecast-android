package com.bottlecast.app.feature.compose.domain

import androidx.annotation.StringRes
import com.bottlecast.app.R
import java.time.LocalDate

/** The editor's placeholder, a different one each day (spec §3.4). */
object DailyPrompt {

    private val prompts = listOf(R.string.compose_prompt_1, R.string.compose_prompt_2, R.string.compose_prompt_3)

    @StringRes
    fun forDate(date: LocalDate): Int = prompts[Math.floorMod(date.toEpochDay(), prompts.size.toLong()).toInt()]
}
