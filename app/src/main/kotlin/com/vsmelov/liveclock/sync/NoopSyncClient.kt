package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.domain.LifeEvent

/**
 * Реализация по умолчанию: никуда не ходит и ничего не делает.
 *
 * Включена из коробки, потому что приложение должно быть полезным
 * без единой строчки настроек и без сети.
 */
object NoopSyncClient : EventSyncClient {

    override suspend fun push(events: List<LifeEvent>) = Unit
}
