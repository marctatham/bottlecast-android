package com.bottlecast.app.fakes

import com.bottlecast.app.common.messages.domain.Draft
import com.bottlecast.app.common.messages.domain.IDraftRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** The Compose draft in memory, saved at once, with a cast set aside until it's confirmed or refused. */
@Singleton
class FakeDraftRepository @Inject constructor() : IDraftRepository {

    private val ids = AtomicInteger()
    private val _draft = MutableStateFlow<Draft?>(null)
    override val draft: StateFlow<Draft?> = _draft

    private var setAside: Draft? = null

    override suspend fun load(): Draft? = _draft.value

    override fun update(text: String) {
        val current = _draft.value
        if (current?.text == text) return
        _draft.value = Draft(current?.messageId ?: "draft${ids.incrementAndGet().toString().padStart(15, '0')}", text)
    }

    override suspend fun flush() = Unit

    override fun clear(): Job {
        _draft.value = null
        return Job().apply { complete() }
    }

    override suspend fun setAside(messageId: String) {
        setAside = _draft.value?.takeIf { it.messageId == messageId }
        _draft.value = null
    }

    override suspend fun forget(messageId: String) {
        if (setAside?.messageId == messageId) setAside = null
    }

    override suspend fun restore(messageId: String): Boolean {
        val words = setAside?.takeIf { it.messageId == messageId } ?: return false
        setAside = null
        if (!_draft.value?.text.isNullOrBlank()) return false
        _draft.value = words
        return true
    }
}
