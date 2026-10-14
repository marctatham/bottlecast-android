package com.bottlecast.app.common.preferences

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

/** Whether first-launch onboarding has been seen (spec §3.1), kept on the phone. */
interface IOnboardingStore {

    val seen: Flow<Boolean>

    /** Marks it seen. The write outlives the screen that asks for it; join it to wait for it. */
    fun markSeen(): Job
}
