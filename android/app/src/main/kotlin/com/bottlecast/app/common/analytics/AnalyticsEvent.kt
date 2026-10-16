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

    /** A throw back was written, once its undo window ended. */
    data object BottleThrownBack : AnalyticsEvent("bottle_thrown_back")

    /** A report was sent, once its undo window ended; param [PARAM_BLOCKED]: whether the writer was blocked too. */
    data object BottleReported : AnalyticsEvent("bottle_reported")

    /** A bottle was kept; param [PARAM_SOURCE]: reveal or keepsake. */
    data object BottleKept : AnalyticsEvent("bottle_kept")

    /** A writer pulled a drifting bottle of theirs from the sea. */
    data object BottlePulled : AnalyticsEvent("bottle_pulled")

    /** Signed in with Google from the sheet; param [PARAM_RESULT]: linked, signed_in, switched or handed_over. */
    data object SignedIn : AnalyticsEvent("signed_in")

    /** The sheet's sign-in didn't go through; param [PARAM_REASON]: unavailable or failed. A closed picker isn't counted. */
    data object SignInFailed : AnalyticsEvent("sign_in_failed")

    /** A pen name was saved; param [PARAM_SOURCE]: typed or surprise. */
    data object PenNameSaved : AnalyticsEvent("pen_name_saved")

    /** Onboarding ended; param [PARAM_HOW]: skip, find or cast. */
    data object OnboardingFinished : AnalyticsEvent("onboarding_finished")

    /**
     * The server took a cast, which may be long after Preview has gone; params [PARAM_QUEUED] (it waited
     * for a connection), [PARAM_SIGNED] (with a pen name) and [PARAM_SEALABLE].
     */
    data object BottleCast : AnalyticsEvent("bottle_cast")

    /** The server turned a cast down, and its words went back to the draft. */
    data object CastRefused : AnalyticsEvent("cast_refused")

    /** The daily reminder was turned on; param [PARAM_SOURCE]: first_find, shore or settings. */
    data object RemindersTurnedOn : AnalyticsEvent("reminders_turned_on")

    /** The account signed out, back to a guest on this phone. */
    data object SignedOut : AnalyticsEvent("signed_out")

    /** The account and its data were deleted. */
    data object AccountDeleted : AnalyticsEvent("account_deleted")

    companion object {
        const val PARAM_METHOD = "method"
        const val PARAM_BLOCKED = "blocked"
        const val PARAM_RESULT = "result"
        const val PARAM_REASON = "reason"
        const val PARAM_SOURCE = "source"
        const val PARAM_HOW = "how"
        const val PARAM_QUEUED = "queued"
        const val PARAM_SIGNED = "signed"
        const val PARAM_SEALABLE = "sealable"
    }
}
