package com.bottlecast.app.fakes

import com.bottlecast.app.common.analytics.AnalyticsEvent
import com.bottlecast.app.common.analytics.ITracker
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records analytics instead of sending them, so tests can check what was counted and the debug
 * project's analytics stay clean. The app records on the main thread and tests read on their own.
 */
@Singleton
class FakeTracker @Inject constructor() : ITracker {

    private val _screens = CopyOnWriteArrayList<String>()
    val screens: List<String> get() = _screens.toList()

    private val _events = CopyOnWriteArrayList<AnalyticsEvent>()
    val events: List<AnalyticsEvent> get() = _events.toList()

    override fun trackScreen(screenName: String) {
        _screens += screenName
    }

    override fun trackEvent(analyticsEvent: AnalyticsEvent, params: Map<String, String>?) {
        _events += analyticsEvent
    }
}
