package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.data.SyncSettings

/**
 * Picks an implementation from the settings.
 *
 * Until the user turns sync on and enters a URL, [NoopSyncClient] is what runs —
 * that is the default behaviour.
 */
object SyncClientProvider {

    fun forSettings(settings: SyncSettings): EventSyncClient =
        if (settings.isUsable) {
            HttpSyncClient(
                endpointUrl = settings.endpointUrl,
                bearerToken = settings.bearerToken,
            )
        } else {
            NoopSyncClient
        }
}
