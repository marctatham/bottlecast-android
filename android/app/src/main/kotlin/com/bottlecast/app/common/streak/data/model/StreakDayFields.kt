package com.bottlecast.app.common.streak.data.model

import java.time.LocalDate

/**
 * streakDays/{uid}_{yyyy-MM-dd}: what a local date did for the streak besides a find (spec §3.3).
 * Written by merging, so each mark is added on its own; the marks only ever turn true.
 */
object StreakDayFields {
    const val COLLECTION = "streakDays"

    const val UID = "uid"
    const val DATE = "date"

    /** The day's first cast, which gives a calm-sea shield. Written in the cast's own commit. */
    const val CAST = "cast"

    /** A look at the sea found it calm, so the day can't break a streak. */
    const val CALM = "calm"

    /** A guest's streak, carried over to the account it signed in to. */
    const val CARRIED = "carried"

    fun id(uid: String, date: LocalDate) = "${uid}_$date"
}
