package com.bottlecast.app.common.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class FirebaseTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : ITracker {

    override fun trackScreen(screenName: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }

    override fun trackEvent(analyticsEvent: AnalyticsEvent, params: Map<String, String>?) {
        val eventParams: Bundle? = params?.let {
            Bundle().apply {
                it.forEach { (key, value) ->
                    putString(key, value)
                }
            }
        }

        firebaseAnalytics.logEvent(analyticsEvent.eventName, eventParams)
    }
}
