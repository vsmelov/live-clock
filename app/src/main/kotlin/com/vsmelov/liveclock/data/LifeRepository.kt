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
import com.vsmelov.liveclock.domain.AppLanguage
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
import java.time.ZoneId

private const val TAG = "LifeRepository"
private const val DATA_STORE_NAME = "life_clock"

private val Context.lifeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATA_STORE_NAME,
)

/**
 * The single source of truth. The widget, the Activity and the workers all come
 * here.
 *
 * The state sits in one JSON blob under [KEY_STATE], which makes writing an event
 * atomic: `edit` serialises concurrent edits, so tapping a button on the widget
 * and in the Activity at the same time cannot lose one of them.
 */
class LifeRepository(private val dataStore: DataStore<Preferences>) {

    val state: Flow<LifeState> = dataStore.data
        .catch { error -> emitEmptyOnIoError(error) }
        .map { preferences -> decodeState(preferences[KEY_STATE]) }

    val syncSettings: Flow<SyncSettings> = dataStore.data
        .catch { error -> emitEmptyOnIoError(error) }
        .map { preferences -> preferences.toSyncSettings() }

    /**
     * Pinned actions in the order the user chose. The widget buttons are built
     * from these. Until something is pinned, the default applies.
     */
    val pinnedTypes: Flow<List<EventType>> = dataStore.data
        .catch { error -> emitEmptyOnIoError(error) }
        .map { preferences -> decodePinned(preferences[KEY_PINNED]) }

    /** Interface language. Read by the Activity and by the widget alike. */
    val language: Flow<AppLanguage> = dataStore.data
        .catch { error -> emitEmptyOnIoError(error) }
        .map { preferences -> AppLanguage.fromId(preferences[KEY_LANGUAGE]) }

    suspend fun currentState(): LifeState = state.first()

    suspend fun currentSyncSettings(): SyncSettings = syncSettings.first()

    suspend fun currentPinnedTypes(): List<EventType> = pinnedTypes.first()

    suspend fun currentLanguage(): AppLanguage = language.first()

    /** Applies [transform] atomically and returns the state as saved. */
    suspend fun update(transform: (LifeState) -> LifeState): LifeState {
        var updated = LifeState()
        dataStore.edit { preferences ->
            updated = transform(decodeState(preferences[KEY_STATE]))
            preferences[KEY_STATE] = LifeClockJson.encodeToString(updated.toDto())
        }
        return updated
    }

    /**
     * Records an event. Called from the widget and from the Activity.
     *
     * The price is computed against the current log: for types with an allowance
     * the first events in a period may cost differently from later ones. The
     * computed value is frozen into the event, so the log stays an honest record
     * of what happened rather than being recomputed after the fact.
     */
    suspend fun addEvent(
        type: EventType,
        at: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): LifeState = update { current ->
        current.plusEvent(
            LifeEvent(type = type, at = at, deltaMinutes = current.deltaFor(type, at, zone)),
        )
    }

    /** The "undo last" button — mistapping the widget is far too easy. */
    suspend fun undoLastEvent(): LifeState = update(LifeState::withoutLastEvent)

    suspend fun setBirthDate(date: LocalDate): LifeState =
        update { current -> current.copy(birthDate = date) }

    suspend fun setBaseExpectancyYears(years: Double): LifeState =
        update { current -> current.copy(baseExpectancyYears = years) }

    /** Replaces the whole state — used when restoring a backup. */
    suspend fun replaceState(state: LifeState): LifeState = update { state }

    /** Pins or unpins a type, keeping the order of the rest. */
    suspend fun togglePinned(type: EventType) {
        dataStore.edit { preferences ->
            val current = decodePinned(preferences[KEY_PINNED])
            val updated = if (type in current) current - type else current + type
            preferences[KEY_PINNED] = updated.joinToString(PINNED_SEPARATOR) { it.id }
        }
    }

    /** Moves a pinned type by one position — the order decides the widget buttons. */
    suspend fun movePinned(type: EventType, offset: Int) {
        dataStore.edit { preferences ->
            val current = decodePinned(preferences[KEY_PINNED]).toMutableList()
            val from = current.indexOf(type)
            val to = from + offset
            if (from >= 0 && to in current.indices) {
                current.removeAt(from)
                current.add(to, type)
                preferences[KEY_PINNED] = current.joinToString(PINNED_SEPARATOR) { it.id }
            }
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences -> preferences[KEY_LANGUAGE] = language.id }
    }

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
            // A corrupt or incompatible blob. Losing the rest of the app over it
            // helps nobody — fall back to defaults and leave a trace in the log.
            Log.e(TAG, "Could not read the saved state, falling back to defaults", error)
            LifeState()
        }
    }

    /**
     * An absent key and an empty string mean different things on purpose: the
     * first is "the user has not chosen yet" and gets the default, the second is
     * "everything was deliberately unpinned" and does not.
     */
    private fun decodePinned(raw: String?): List<EventType> {
        if (raw == null) return EventType.DEFAULT_PINNED
        return raw.split(PINNED_SEPARATOR).mapNotNull(EventType::fromId)
    }

    private suspend fun FlowCollector<Preferences>.emitEmptyOnIoError(error: Throwable) {
        if (error is IOException) {
            Log.e(TAG, "Could not read the DataStore", error)
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
        private val KEY_PINNED = stringPreferencesKey("pinned_types")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private const val PINNED_SEPARATOR = ","
        private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        private val KEY_SYNC_URL = stringPreferencesKey("sync_url")
        private val KEY_SYNC_TOKEN = stringPreferencesKey("sync_token")
        private val KEY_SYNC_LAST_AT = longPreferencesKey("sync_last_at")

        /** The DataStore is a process singleton, so widget and Activity see the same thing. */
        fun from(context: Context): LifeRepository =
            LifeRepository(context.applicationContext.lifeDataStore)
    }
}
