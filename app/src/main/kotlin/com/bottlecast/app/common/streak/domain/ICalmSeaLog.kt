package com.bottlecast.app.common.streak.domain

import java.time.LocalDate

/** Notes that a look at the sea found it calm for an account on a local date, which keeps a streak. */
fun interface ICalmSeaLog {
    fun markCalm(uid: String, date: LocalDate)
}
