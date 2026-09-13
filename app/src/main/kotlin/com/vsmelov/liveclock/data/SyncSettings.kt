package com.vsmelov.liveclock.data

import java.time.Instant

/**
 * Настройки выгрузки лога наружу.
 *
 * По умолчанию всё выключено: приложение обязано полностью работать офлайн,
 * а источник правды — локальный DataStore. Синк здесь — необязательная
 * надстройка, которая никогда ничего не блокирует.
 */
data class SyncSettings(
    val enabled: Boolean = false,
    val endpointUrl: String = "",
    val bearerToken: String = "",
    /** Время самого позднего успешно выгруженного события. */
    val lastSyncedAt: Instant? = null,
) {
    /** Настроек достаточно, чтобы пытаться отправлять. */
    val isUsable: Boolean
        get() = enabled && endpointUrl.isNotBlank()
}
