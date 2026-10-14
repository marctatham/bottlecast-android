package com.bottlecast.app.common.messages.domain

/** Tells which language a bottle is written in, on the phone (spec §3.4b). */
interface ILanguageDetector {

    /** The language as BCP 47, such as "en" or "es", or "und" when it can't tell, or takes too long. */
    suspend fun detect(text: String): String
}
