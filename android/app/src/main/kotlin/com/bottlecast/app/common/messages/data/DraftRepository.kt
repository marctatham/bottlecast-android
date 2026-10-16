package com.bottlecast.app.common.messages.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bottlecast.app.common.messages.domain.Draft
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.newMessageId
import com.bottlecast.app.common.threading.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DraftRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val scope: CoroutineScope,
    private val random: Random,
) : IDraftRepository {

    private val _draft = MutableStateFlow<Draft?>(null)
    override val draft: StateFlow<Draft?> = _draft.asStateFlow()

    private val loadMutex = Mutex()
    private var loaded = false
    private var pendingSave: Job? = null

    override suspend fun load(): Draft? = loadMutex.withLock {
        if (!loaded) {
            val saved = read()
            val id = saved[DRAFT_ID]
            val text = saved[DRAFT_TEXT]
            // writing may already have started in memory before the saved draft was read
            if (_draft.value == null && id != null && !text.isNullOrEmpty()) _draft.value = Draft(id, text)
            loaded = true
        }
        _draft.value
    }

    override fun update(text: String) {
        val current = _draft.value
        if (current?.text == text) return
        _draft.value = Draft(messageId = current?.messageId ?: newMessageId(random), text = text)
        pendingSave?.cancel()
        pendingSave = scope.launch {
            delay(SAVE_DELAY_MILLIS)
            save()
        }
    }

    override suspend fun flush() {
        pendingSave?.cancel()
        save()
    }

    override fun clear(): Job {
        pendingSave?.cancel()
        _draft.value = null
        return scope.launch { edit { it.removeDraft() } }
    }

    override suspend fun setAside(messageId: String) {
        pendingSave?.cancel()
        val draft = _draft.value?.takeIf { it.messageId == messageId }
        _draft.value = null
        edit {
            it.removeDraft()
            if (draft != null) {
                it[PENDING_ID] = draft.messageId
                it[PENDING_TEXT] = draft.text
            }
        }
    }

    override suspend fun forget(messageId: String) {
        edit { if (it[PENDING_ID] == messageId) it.removePending() }
    }

    override suspend fun restore(messageId: String): Boolean {
        val saved = read()
        val text = saved[PENDING_TEXT]?.takeIf { saved[PENDING_ID] == messageId }
        val newer = !_draft.value?.text.isNullOrBlank()
        val restored = text != null && !newer
        if (restored) _draft.value = Draft(messageId, text!!)
        if (text != null && newer) Timber.w("[restore] A refused cast's words gave way to a newer draft")
        edit {
            if (it[PENDING_ID] == messageId) it.removePending()
            if (restored) {
                it[DRAFT_ID] = messageId
                it[DRAFT_TEXT] = text!!
            }
        }
        return restored
    }

    private suspend fun save() {
        val draft = _draft.value
        edit {
            if (draft == null || draft.text.isEmpty()) {
                it.removeDraft()
            } else {
                it[DRAFT_ID] = draft.messageId
                it[DRAFT_TEXT] = draft.text
            }
        }
    }

    private suspend fun read(): Preferences = runCatching { dataStore.data.first() }
        .onFailure { Timber.w(it, "[read] Couldn't read the saved draft") }
        .getOrDefault(emptyPreferences())

    private suspend fun edit(change: (MutablePreferences) -> Unit) {
        runCatching { dataStore.edit(change) }.onFailure { Timber.w(it, "[edit] Couldn't save the draft") }
    }

    private fun MutablePreferences.removeDraft() {
        remove(DRAFT_ID)
        remove(DRAFT_TEXT)
    }

    private fun MutablePreferences.removePending() {
        remove(PENDING_ID)
        remove(PENDING_TEXT)
    }

    private companion object {
        const val SAVE_DELAY_MILLIS = 1_000L
        val DRAFT_ID = stringPreferencesKey("draft_message_id")
        val DRAFT_TEXT = stringPreferencesKey("draft_text")
        // the cast still waiting for the server; the tidy-up slice reads it after a restart
        val PENDING_ID = stringPreferencesKey("pending_cast_message_id")
        val PENDING_TEXT = stringPreferencesKey("pending_cast_text")
    }
}
