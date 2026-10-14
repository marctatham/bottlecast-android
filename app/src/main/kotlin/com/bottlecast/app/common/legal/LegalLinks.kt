package com.bottlecast.app.common.legal

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import timber.log.Timber

/** The Terms, Community guidelines and Privacy Policy, on the web pages' Hosting (spec §3.2, §3.4b). */
object LegalLinks {
    const val TERMS = "https://bottling-messages.web.app/terms.html"
    const val GUIDELINES = "https://bottling-messages.web.app/guidelines.html"
    const val PRIVACY = "https://bottling-messages.web.app/privacy.html"
}

/** Opens [url] in a Custom Tab, over the app, or in whatever browser the phone has. */
fun openInCustomTab(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, url.toUri())
    } catch (exception: ActivityNotFoundException) {
        Timber.w(exception, "[openInCustomTab] No browser to open a page in")
    }
}
