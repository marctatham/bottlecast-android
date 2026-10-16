package com.bottlecast.app.fakes

import com.bottlecast.app.common.account.domain.IAccountEraser
import com.bottlecast.app.common.account.domain.IAccountLocalData
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** Records which accounts were erased, rather than reaching Firestore. */
@Singleton
class FakeAccountEraser @Inject constructor() : IAccountEraser {
    val erased = CopyOnWriteArrayList<String>()

    override suspend fun erase(uid: String) {
        erased += uid
    }
}

/** What the phone kept about the account, forgotten. */
@Singleton
class FakeAccountLocalData @Inject constructor() : IAccountLocalData {
    @Volatile var cleared = false
        private set

    override suspend fun clear() {
        cleared = true
    }
}
