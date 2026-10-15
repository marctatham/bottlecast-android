package com.bottlecast.app.common.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.bottlecast.app.common.messages.domain.LANG_UNDETERMINED
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A date as the chest shows it, in the phone's language: "5 Oct" this year, "5 Oct 2025" before it
 * (spec §3.10, §3.11).
 */
@Composable
@ReadOnlyComposable
fun shortDate(date: LocalDate, today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    val skeleton = if (date.year == today.year) "dMMM" else "dMMMyyyy"
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
}

/**
 * The name of a bottle's language in the phone's, such as "Spanish", or null when it isn't worth saying:
 * the language isn't known, or it's the phone's own (spec §3.10).
 */
@Composable
@ReadOnlyComposable
fun foreignLanguageName(lang: String): String? {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    val language = Locale.forLanguageTag(lang).language
    if (language.isEmpty() || language == LANG_UNDETERMINED || language == locale.language) return null
    return Locale.forLanguageTag(lang).getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }.takeIf { it.isNotBlank() }
}

/** [time] as the user reads clock times, "9:00 AM": in their locale, and 24-hour if the phone is set to it. */
@Composable
@ReadOnlyComposable
fun clockTime(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm"
    return time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
}

/** A time on the hour without its minutes, "10 PM", as quiet hours read (spec §3.16a); otherwise as [clockTime]. */
@Composable
@ReadOnlyComposable
fun shortClockTime(time: LocalTime): String {
    if (time.minute != 0 || DateFormat.is24HourFormat(LocalContext.current)) return clockTime(time)
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    return time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "h"), locale))
}

/** A month as "Oct 2026", in the phone's language: the Me tab's "Casting since" (spec §3.12). */
@Composable
@ReadOnlyComposable
fun monthYear(month: YearMonth): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ROOT
    return month.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMyyyy"), locale))
}
