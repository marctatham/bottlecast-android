package com.bottlecast.app.common.analytics


/** An analytics event, by its snake_case name. Params never carry message text, tokens or emails. */
sealed class AnalyticsEvent(val eventName: String) {
    data object GetMessageTapped : AnalyticsEvent("get_message_tapped")
    data object PostMessageTapped : AnalyticsEvent("post_message_tapped")

    /** The cork popped; param [PARAM_METHOD]: shake, tap, hold, button or accessibility. */
    data object BottleUncorked : AnalyticsEvent("bottle_uncorked")

    /** The "No shaking needed" sheet opened. */
    data object UncorkHelpOpened : AnalyticsEvent("uncork_help_opened")

    data object BottleShared : AnalyticsEvent("bottle_shared")

    data object BottleThrownBack : AnalyticsEvent("bottle_thrown_back")

    /** A report was sent; param [PARAM_BLOCKED]: whether the writer was blocked too. */
    data object BottleReported : AnalyticsEvent("bottle_reported")

    companion object {
        const val PARAM_METHOD = "method"
        const val PARAM_BLOCKED = "blocked"
    }
}
