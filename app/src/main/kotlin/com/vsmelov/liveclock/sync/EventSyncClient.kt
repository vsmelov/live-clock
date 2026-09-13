package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.domain.LifeEvent
import java.io.IOException

/**
 * Uploads the log somewhere else.
 *
 * The contract is deliberately narrow: a client only sends events and returns
 * nothing. The local DataStore is the source of truth and a server cannot
 * overwrite local state, which is what lets the app work entirely offline.
 *
 * An implementation must throw [SyncException] on failure so the worker can tell
 * "retry later" from "delivered".
 */
interface EventSyncClient {

    /** Sends [events]. An empty list is no reason to touch the network. */
    suspend fun push(events: List<LifeEvent>)
}

/** Sync failed. Extending [IOException] is exactly the right semantics here. */
class SyncException(message: String, cause: Throwable? = null) : IOException(message, cause)
