package com.bottlecast.app.common.performance

import android.util.Log
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.Firebase
import timber.log.Timber

/** A tree which logs important information for crash reporting.  */
internal class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        Firebase.crashlytics.log("${priority.logPriorityPrefix}: $message")
        t?.let { Firebase.crashlytics.recordException(it) }
    }
}


private val Int.logPriorityPrefix: String
    get() {
        return when (this) {
            Log.DEBUG -> "d"
            Log.VERBOSE -> "v"
            Log.INFO -> "i"
            Log.ERROR -> "e"
            Log.WARN -> "w"
            Log.ASSERT -> "a"
            else -> "undefined"
        }
    }
