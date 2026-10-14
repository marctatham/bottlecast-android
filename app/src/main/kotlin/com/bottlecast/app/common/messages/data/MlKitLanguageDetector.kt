package com.bottlecast.app.common.messages.data

import com.bottlecast.app.common.messages.domain.ILanguageDetector
import com.bottlecast.app.common.messages.domain.LANG_UNDETERMINED
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * ML Kit's language identification, on the phone. Play services keeps the model, and until it has
 * fetched it, or for text too short to tell, the answer is "und".
 */
@Singleton
class MlKitLanguageDetector @Inject constructor() : ILanguageDetector {

    private val identifier by lazy { LanguageIdentification.getClient() }

    override suspend fun detect(text: String): String {
        val tag = withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                identifier.identifyLanguage(text)
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener {
                        Timber.w(it, "[detect] Couldn't tell the language")
                        if (continuation.isActive) continuation.resume(LANG_UNDETERMINED)
                    }
            }
        }
        // only a tag the rules accept: "en", "zh-Latn" and the like
        return tag?.takeIf { it.matches(BCP_47) } ?: LANG_UNDETERMINED
    }

    private companion object {
        const val TIMEOUT_MILLIS = 1_000L
        val BCP_47 = Regex("^[a-z]{2,3}(-[A-Za-z0-9]{1,8})*$")
    }
}
