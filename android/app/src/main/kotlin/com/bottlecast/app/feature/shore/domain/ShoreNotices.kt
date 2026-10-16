package com.bottlecast.app.feature.shore.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** A notice another screen leaves for the Shore, to show once it's back in view. */
enum class ShoreNotice {
    /** Reveal closed on a fresh find that wasn't kept (spec §3.7): "It'll wait in your chest for 7 days." */
    WaitsInChest,
}

/**
 * Notices on their way to the Shore, kept in memory for as long as the app runs. A screen posts one as
 * it goes, however it goes, and the Shore takes it once it's shown.
 */
@Singleton
class ShoreNotices @Inject constructor() {

    private val _pending = MutableStateFlow<ShoreNotice?>(null)
    val pending: StateFlow<ShoreNotice?> = _pending.asStateFlow()

    fun post(notice: ShoreNotice) {
        _pending.value = notice
    }

    /** The Shore showed [notice], so it's done with. */
    fun consume(notice: ShoreNotice) = _pending.update { if (it == notice) null else it }
}
