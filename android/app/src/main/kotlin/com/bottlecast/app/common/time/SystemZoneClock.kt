package com.bottlecast.app.common.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The system clock in whatever time zone the phone is in now. `Clock.systemDefaultZone()` keeps the zone
 * it was created in, so the tide would miss a time-zone change while the app runs.
 */
object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
