package com.vsmelov.liveclock.data

import java.time.Instant

/**
 * Settings for uploading the log elsewhere.
 *
 * Everything is off by default: the app has to be fully useful offline, and the
 * local DataStore is the source of truth. Sync is an optional add-on that never
 * blocks anything.
 */
data class SyncSettings(
    val enabled: Boolean = false,
    val endpointUrl: String = "",
    val bearerToken: String = "",
    /** The timestamp of the latest successfully uploaded event. */
    val lastSyncedAt: Instant? = null,
) {
    /** There is enough configured to attempt an upload. */
    val isUsable: Boolean
        get() = enabled && endpointUrl.isNotBlank()
}
