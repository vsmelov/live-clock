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
 * The storage format for the state, and at the same time the body of a sync
 * request and of an exported backup.
 *
 * The domain types (`LocalDate`, `Instant`, `EventType`) are deliberately not
 * serialised directly: a DTO gives a stable schema that can be read by eye and
 * versioned without touching the domain model.
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
    /** [EventType.id], not `name` — renaming enum entries stays safe. */
    val type: String,
    @SerialName("at_epoch_second") val atEpochSecond: Long,
    @SerialName("delta_minutes") val deltaMinutes: Int,
)

/** The POST body used by [com.vsmelov.liveclock.sync.HttpSyncClient]. */
@Serializable
data class SyncPayload(
    val events: List<LifeEventDto>,
    @SerialName("sent_at_epoch_second") val sentAtEpochSecond: Long,
)

/**
 * `ignoreUnknownKeys` on purpose: if the schema moves forward and is later rolled
 * back, an older build should ignore the extra fields rather than fail to read.
 * `prettyPrint` because this same format is what a human opens after an export.
 */
internal val LifeClockJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
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
 * The reverse mapping.
 *
 * Events of an unknown type are dropped silently: if a type was deleted from
 * [EventType], showing the rest of the log beats failing to open the app. Their
 * delta then stops counting, which is a deliberate trade.
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
