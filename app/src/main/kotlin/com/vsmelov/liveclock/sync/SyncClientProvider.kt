package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.data.SyncSettings

/**
 * Выбор реализации по настройкам.
 *
 * Пока пользователь не включил синк и не вписал URL, работает [NoopSyncClient] —
 * это и есть поведение по умолчанию.
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
