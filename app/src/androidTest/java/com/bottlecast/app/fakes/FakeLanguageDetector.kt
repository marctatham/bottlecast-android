package com.bottlecast.app.fakes

import com.bottlecast.app.common.messages.domain.ILanguageDetector
import javax.inject.Inject
import javax.inject.Singleton

/** Every bottle reads as English, with no model to wait for. */
@Singleton
class FakeLanguageDetector @Inject constructor() : ILanguageDetector {
    override suspend fun detect(text: String): String = "en"
}
