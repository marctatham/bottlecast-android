package com.bottlecast.app.feature.reminders

import androidx.compose.ui.unit.dp

/** Sizes only the reminder sheet uses, from spec §3.16a, and the Notify-Rationale board where the spec gives none. */
internal object ReminderDimens {
    /** The bottle with a bell and the moon (spec §3.16a): a glassContainer disc. */
    val illustration = 96.dp
    val illustrationBottle = 48.dp

    /** The bell's badge at the disc's lower end, and the moon at its upper end, each ringed in the sheet's colour. */
    val bellBadge = 36.dp
    val bellIcon = 20.dp
    val moonDot = 20.dp
    val badgeRing = 3.dp

    /** "Remind me at": an outlined row at least 56dp tall (Notify-Rationale board). */
    val timeRowMinHeight = 56.dp

    /** The moon beside the quiet-hours caption. */
    val captionIcon = 16.dp
}
