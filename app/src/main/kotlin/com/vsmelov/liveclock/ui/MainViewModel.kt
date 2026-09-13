package com.vsmelov.liveclock.ui

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vsmelov.liveclock.data.LifeRepository
import com.vsmelov.liveclock.data.SyncSettings
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeState
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
    val now: Instant = Instant.now(),
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    /** Сколько раз каждый тип попадал в лог — подсказка, что стоит закрепить. */
    val usageCounts: Map<EventType, Int> get() = life.usageCounts()
}

class MainViewModel(
    private val appContext: Context,
    private val repository: LifeRepository,
) : ViewModel() {

    /**
     * В приложении секунды можно показывать честно — процесс всё равно
     * на переднем плане. Ограничение «раз в полчаса» относится к виджету,
     * а не сюда.
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
            ticker,
        ) { life, sync, pinned, now ->
            MainUiState(life = life, sync = sync, pinned = pinned, now = now)
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

    /** Закрепить или открепить действие. Виджет перерисуется сразу. */
    fun togglePinned(type: EventType) = mutate { repository.togglePinned(type) }

    /** Сдвинуть закреплённое действие — порядок задаёт кнопки виджета. */
    fun movePinned(type: EventType, offset: Int) = mutate {
        repository.movePinned(type, offset)
    }

    fun setBirthDate(date: LocalDate) = mutate { repository.setBirthDate(date) }

    fun setBaseExpectancyYears(years: Double) = mutate {
        repository.setBaseExpectancyYears(years)
    }

    fun setSyncEnabled(enabled: Boolean) = mutate {
        repository.updateSyncSettings { it.copy(enabled = enabled) }
    }

    fun setSyncEndpoint(url: String) = mutate {
        repository.updateSyncSettings { it.copy(endpointUrl = url) }
    }

    fun setSyncToken(token: String) = mutate {
        repository.updateSyncSettings { it.copy(bearerToken = token) }
    }

    /**
     * Любая правка состояния должна доехать до виджета: он читает тот же
     * DataStore, но перерисовывается только когда его об этом попросят.
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
