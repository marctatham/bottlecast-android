package com.bottlecast.app.common.messages.domain

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * The Compose draft, saved on the phone a second after each change (spec §3.4), and the cast still
 * waiting for the server. Memory is the source of truth, so Preview reads exactly what Compose wrote.
 */
interface IDraftRepository {

    /** The draft, once [load]ed; null when there's none. */
    val draft: StateFlow<Draft?>

    /** Reads the saved draft the first time, then answers from memory. */
    suspend fun load(): Draft?

    /** Writing: memory now, the phone a second later. A new draft gets its bottle's id. */
    fun update(text: String)

    /** Saves at once, such as before Preview opens. */
    suspend fun flush()

    /** Tossed, or started fresh: gone from memory at once, and from the phone when the returned job ends. */
    fun clear(): Job

    /** Cast: the draft steps aside as the cast waiting for the server, and the editor is clear. */
    suspend fun setAside(messageId: String)

    /** The server confirmed the cast, so its words aren't needed any more. */
    suspend fun forget(messageId: String)

    /**
     * The server refused the cast: its words come back as the draft, unless a newer draft exists.
     * Returns whether they did.
     */
    suspend fun restore(messageId: String): Boolean
}
