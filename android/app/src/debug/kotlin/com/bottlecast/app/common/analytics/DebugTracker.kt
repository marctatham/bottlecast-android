package com.bottlecast.app.common.analytics

/**
 * Debug tracker tracks via both Timber and the debug-specific Firebase project
 */
class DebugTracker(
    private val firebaseTracker: FirebaseTracker,
    private val loggingTracker: LoggingTracker
) : ITracker {

    override fun trackScreen(screenName: String) {
        loggingTracker.trackScreen(screenName)
        firebaseTracker.trackScreen(screenName)
    }

    override fun trackEvent(analyticsEvent: AnalyticsEvent, params: Map<String, String>?) {
        loggingTracker.trackEvent(
            analyticsEvent = analyticsEvent,
            params = params
        )
        firebaseTracker.trackEvent(
            analyticsEvent = analyticsEvent,
            params = params
        )
    }
}
