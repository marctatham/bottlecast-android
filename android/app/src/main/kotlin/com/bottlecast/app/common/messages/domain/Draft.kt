package com.bottlecast.app.common.messages.domain

/**
 * The bottle being written in Compose (spec §3.4). Its [messageId] is chosen with the draft, so
 * Preview, the cast animation and the cast bottle all have the same seeded shape (spec §1.6).
 */
data class Draft(
    val messageId: String,
    val text: String,
)
