package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.domain.LifeEvent
import java.io.IOException

/**
 * Выгрузка лога наружу.
 *
 * Контракт намеренно узкий: клиент только отправляет события и ничего не
 * возвращает. Источник правды — локальный DataStore, сервер не может
 * переписать локальное состояние, поэтому приложение целиком работает офлайн.
 *
 * Реализация обязана бросить [SyncException] на неуспехе, чтобы воркер
 * отличил «повторить позже» от «отправлено».
 */
interface EventSyncClient {

    /** Отправляет [events]. Пустой список — не повод ходить в сеть. */
    suspend fun push(events: List<LifeEvent>)
}

/** Синк не удался. Наследник [IOException] — это ровно сетевая семантика. */
class SyncException(message: String, cause: Throwable? = null) : IOException(message, cause)
