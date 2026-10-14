package com.bottlecast.app.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.nio.file.Files

/**
 * A real preferences DataStore on a file of its own, for checking what survives a restart. Its file
 * work runs off the test's scheduler, so wait for each write rather than advancing virtual time.
 * Each call makes a new file: only one DataStore may own a file at a time.
 */
fun filePreferences(scope: CoroutineScope): DataStore<Preferences> {
    val folder = Files.createTempDirectory("datastore").toFile()
    return PreferenceDataStoreFactory.create(scope = scope, produceFile = { File(folder, "test.preferences_pb") })
}

/** Preferences in memory, written as soon as they're edited, so virtual time alone decides when. */
class MemoryPreferences : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
