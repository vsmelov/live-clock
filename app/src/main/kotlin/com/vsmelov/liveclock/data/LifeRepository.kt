package com.vsmelov.liveclock.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

private const val TAG = "LifeRepository"
private const val DATA_STORE_NAME = "life_clock"

private val Context.lifeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME,
)

/**
 * Единственный источник правды. Всё — виджет, Activity и воркеры — ходит сюда.
 *
 * Состояние лежит одним JSON-блобом под [KEY_STATE], поэтому запись события
 * атомарна: `edit` сериализует конкурентные правки, и одновременное нажатие
 * кнопки на виджете и в Activity не теряет одно из событий.
 */
class LifeRepository(private val dataStore: DataStore<Preferences>) {

    val state: Flow<LifeState> = dataStore.data
        .catch { error -> emitEmptyOnIoError(error) }
        .map { preferences -> decodeState(preferences[KEY_STATE]) }

    val syncSettings: Flow<SyncSettings> = dataStore.data
        .catch { error -> emitEmptyOnIoError(error) }
        .map { preferences -> preferences.toSyncSettings() }

    suspend fun currentState(): LifeState = state.first()

    suspend fun currentSyncSettings(): SyncSettings = syncSettings.first()

    /** Атомарно применяет [transform] и возвращает уже сохранённое состояние. */
    suspend fun update(transform: (LifeState) -> LifeState): LifeState {
        var updated = LifeState()
        dataStore.edit { preferences ->
            updated = transform(decodeState(preferences[KEY_STATE]))
            preferences[KEY_STATE] = LifeClockJson.encodeToString(updated.toDto())
        }
        return updated
    }

    /** Запись события. Вызывается и с виджета, и из Activity. */
    suspend fun addEvent(type: EventType, at: Instant = Instant.now()): LifeState =
        update { current -> current.plusEvent(LifeEvent.now(type, at)) }

    /** Кнопка «отменить последнее» — промахнуться по виджету слишком легко. */
    suspend fun undoLastEvent(): LifeState = update(LifeState::withoutLastEvent)

    suspend fun setBirthDate(date: LocalDate): LifeState =
        update { current -> current.copy(birthDate = date) }

    suspend fun setBaseExpectancyYears(years: Double): LifeState =
        update { current -> current.copy(baseExpectancyYears = years) }

    suspend fun updateSyncSettings(transform: (SyncSettings) -> SyncSettings) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toSyncSettings())
            preferences[KEY_SYNC_ENABLED] = updated.enabled
            preferences[KEY_SYNC_URL] = updated.endpointUrl
            preferences[KEY_SYNC_TOKEN] = updated.bearerToken
            val syncedAt = updated.lastSyncedAt
            if (syncedAt == null) {
                preferences.remove(KEY_SYNC_LAST_AT)
            } else {
                preferences[KEY_SYNC_LAST_AT] = syncedAt.epochSecond
            }
        }
    }

    suspend fun markSyncedUpTo(instant: Instant) {
        updateSyncSettings { settings -> settings.copy(lastSyncedAt = instant) }
    }

    private fun decodeState(raw: String?): LifeState {
        if (raw.isNullOrBlank()) return LifeState()
        return try {
            LifeClockJson.decodeFromString<LifeStateDto>(raw).toDomain()
        } catch (error: IllegalArgumentException) {
            // Битый или несовместимый блоб. Терять дальнейшую работу приложения
            // из-за него незачем — откатываемся на дефолт и пишем в лог.
            Log.e(TAG, "Не удалось прочитать сохранённое состояние, беру дефолтное", error)
            LifeState()
        }
    }

    private suspend fun FlowCollector<Preferences>.emitEmptyOnIoError(error: Throwable) {
        if (error is IOException) {
            Log.e(TAG, "Не удалось прочитать DataStore", error)
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    private fun Preferences.toSyncSettings(): SyncSettings = SyncSettings(
        enabled = this[KEY_SYNC_ENABLED] ?: false,
        endpointUrl = this[KEY_SYNC_URL].orEmpty(),
        bearerToken = this[KEY_SYNC_TOKEN].orEmpty(),
        lastSyncedAt = this[KEY_SYNC_LAST_AT]?.let(Instant::ofEpochSecond),
    )

    companion object {
        private val KEY_STATE = stringPreferencesKey("life_state")
        private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        private val KEY_SYNC_URL = stringPreferencesKey("sync_url")
        private val KEY_SYNC_TOKEN = stringPreferencesKey("sync_token")
        private val KEY_SYNC_LAST_AT = longPreferencesKey("sync_last_at")

        /** DataStore процессно-одиночный, поэтому виджет и Activity видят одно и то же. */
        fun from(context: Context): LifeRepository =
            LifeRepository(context.applicationContext.lifeDataStore)
    }
}
