package com.vsmelov.liveclock.ui

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsmelov.liveclock.data.Backup
import com.vsmelov.liveclock.data.LifeRepository
import com.vsmelov.liveclock.data.SyncSettings
import com.vsmelov.liveclock.domain.AppLanguage
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeState
import com.vsmelov.liveclock.domain.Streak
import com.vsmelov.liveclock.domain.WeekSummary
import com.vsmelov.liveclock.widget.LifeClockWidget
import com.vsmelov.liveclock.work.SyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MainUiState(
    val life: LifeState = LifeState(),
    val sync: SyncSettings = SyncSettings(),
    val pinned: List<EventType> = EventType.DEFAULT_PINNED,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val now: Instant = Instant.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    /** How often each type has been logged — a hint about what is worth pinning. */
    val usageCounts: Map<EventType, Int> get() = life.usageCounts()

    val today: LocalDate get() = LocalDate.now(zone)

    val week: WeekSummary get() = life.weekSummary(now, zone)

    val streaks: List<Streak> get() = life.streaks(today, zone)
}

class MainViewModel(
    private val appContext: Context,
    private val repository: LifeRepository,
) : ViewModel() {

    /**
     * Inside the app the seconds can be shown honestly — the process is in the
     * foreground. The "twice an hour" limit applies to the widget, not here.
     */
    private val ticker: Flow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<MainUiState> =
        combine(
            repository.state,
            repository.syncSettings,
            repository.pinnedTypes,
            repository.language,
            ticker,
        ) { life, sync, pinned, language, now ->
            MainUiState(life = life, sync = sync, pinned = pinned, language = language, now = now)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MainUiState(),
        )

    fun logEvent(type: EventType) = mutate {
        repository.addEvent(type, Instant.now())
        SyncWorker.enqueue(appContext)
    }

    fun undoLastEvent() = mutate { repository.undoLastEvent() }

    /** Pins or unpins an action. The widget redraws immediately. */
    fun togglePinned(type: EventType) = mutate { repository.togglePinned(type) }

    /** Moves a pinned action — the order decides the widget buttons. */
    fun movePinned(type: EventType, offset: Int) = mutate {
        repository.movePinned(type, offset)
    }

    fun setBirthDate(date: LocalDate) = mutate { repository.setBirthDate(date) }

    fun setBaseExpectancyYears(years: Double) = mutate {
        repository.setBaseExpectancyYears(years)
    }

    fun setLanguage(language: AppLanguage) = mutate { repository.setLanguage(language) }

    fun setSyncEnabled(enabled: Boolean) = mutate {
        repository.updateSyncSettings { it.copy(enabled = enabled) }
    }

    fun setSyncEndpoint(url: String) = mutate {
        repository.updateSyncSettings { it.copy(endpointUrl = url) }
    }

    fun setSyncToken(token: String) = mutate {
        repository.updateSyncSettings { it.copy(bearerToken = token) }
    }

    /** The backup payload. Read through the repository so it is always current. */
    suspend fun exportPayload(): String = Backup.encode(repository.currentState())

    /**
     * Restores from a backup, returning how many events came back, or null if the
     * file could not be read. Replaces the state wholesale — a restore is a
     * restore, not a merge, and merging two logs by timestamp would silently
     * double every event the user re-imported.
     */
    suspend fun restoreFrom(raw: String): Int? {
        val restored = Backup.decode(raw) ?: return null
        repository.replaceState(restored)
        LifeClockWidget().updateAll(appContext)
        return restored.events.size
    }

    /**
     * Any change to the state has to reach the widget: it reads the same
     * DataStore but only redraws when asked.
     */
    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            LifeClockWidget().updateAll(appContext)
        }
    }

    companion object {
        private const val TICK_MILLIS = 1_000L
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    MainViewModel(appContext, LifeRepository.from(appContext))
                }
            }
        }
    }
}
