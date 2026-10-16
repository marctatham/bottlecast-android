package com.bottlecast.app.common.analytics

import android.os.Bundle
import timber.log.Timber

class LoggingTracker : ITracker {

    override fun trackScreen(screenName: String) {
        Timber.i("Screen: $screenName")
    }

    override fun trackEvent(analyticsEvent: AnalyticsEvent, params: Map<String, String>?) {
        val eventParams: Bundle? = params?.let {
            Bundle().apply {
                it.forEach { (key, value) ->
                    putString(key, value)
                }
            }
        }

        Timber.i("Event: ${analyticsEvent.eventName} with params: $eventParams")
    }
}
