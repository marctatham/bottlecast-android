package com.bottlecast.app.fakes

import com.bottlecast.app.common.preferences.IOnboardingStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Onboarding's flag in memory. Tests start on the Shore, as if onboarding was seen long ago, unless a
 * [FirstLaunchRule] says this is the first launch.
 */
@Singleton
class FakeOnboardingStore @Inject constructor() : IOnboardingStore {

    private val _seen = MutableStateFlow(!firstLaunch)
    override val seen: Flow<Boolean> = _seen

    override fun markSeen(): Job {
        _seen.value = true
        return Job().apply { complete() }
    }

    val isSeen: Boolean get() = _seen.value

    companion object {
        @Volatile
        internal var firstLaunch = false
    }
}

/** Makes the next launch a first launch, with onboarding unseen. Order it before the activity's rule. */
class FirstLaunchRule : TestWatcher() {

    override fun starting(description: Description) {
        FakeOnboardingStore.firstLaunch = true
    }

    override fun finished(description: Description) {
        FakeOnboardingStore.firstLaunch = false
    }
}
