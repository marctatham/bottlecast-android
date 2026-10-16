package com.bottlecast.app.common.analytics

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

// defines the LocalTracker that by default will provide the basic LoggingAnalyticsProvider
// this can be overridden, normally within the App-Level of the Compose application
val LocalTracker: ProvidableCompositionLocal<ITracker> = compositionLocalOf {
    LoggingTracker()
}
