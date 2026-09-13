package com.vsmelov.liveclock.data

import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/**
 * Формат хранения состояния в DataStore и одновременно формат тела синка.
 *
 * Доменные типы (`LocalDate`, `Instant`, `EventType`) сознательно не
 * сериализуются напрямую: DTO даёт стабильную схему, которую видно глазами
 * и которую можно версионировать, не трогая доменную модель.
 */
@Serializable
data class LifeStateDto(
    @SerialName("schema_version") val schemaVersion: Int = SCHEMA_VERSION,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("base_expectancy_years") val baseExpectancyYears: Double,
    val events: List<LifeEventDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

@Serializable
data class LifeEventDto(
    /** [EventType.id], а не `name` — переименования в enum'е безопасны. */
    val type: String,
    @SerialName("at_epoch_second") val atEpochSecond: Long,
    @SerialName("delta_minutes") val deltaMinutes: Int,
)

/** Тело POST-запроса в [com.vsmelov.liveclock.sync.HttpSyncClient]. */
@Serializable
data class SyncPayload(
    val events: List<LifeEventDto>,
    @SerialName("sent_at_epoch_second") val sentAtEpochSecond: Long,
)

/**
 * Настройки с `ignoreUnknownKeys`: если схема уедет вперёд, а потом откатится,
 * старая сборка не уронит чтение, а просто проигнорирует лишние поля.
 */
internal val LifeClockJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

fun LifeState.toDto(): LifeStateDto = LifeStateDto(
    birthDate = birthDate.toString(),
    baseExpectancyYears = baseExpectancyYears,
    events = events.map { it.toDto() },
)

fun LifeEvent.toDto(): LifeEventDto = LifeEventDto(
    type = type.id,
    atEpochSecond = at.epochSecond,
    deltaMinutes = deltaMinutes,
)

/**
 * Обратное преобразование.
 *
 * События неизвестного типа молча пропускаются: если тип удалён из
 * [EventType], лучше показать остальной лог, чем не открыть приложение.
 * Их дельта при этом перестаёт влиять на расчёт — это осознанный размен.
 */
fun LifeStateDto.toDomain(): LifeState = LifeState(
    birthDate = LocalDate.parse(birthDate),
    baseExpectancyYears = baseExpectancyYears,
    events = events.mapNotNull { it.toDomain() }.sortedBy { it.at },
)

fun LifeEventDto.toDomain(): LifeEvent? {
    val eventType = EventType.fromId(type) ?: return null
    return LifeEvent(
        type = eventType,
        at = Instant.ofEpochSecond(atEpochSecond),
        deltaMinutes = deltaMinutes,
    )
}
