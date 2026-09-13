package com.vsmelov.liveclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsmelov.liveclock.R
import com.vsmelov.liveclock.domain.Confidence
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.Evidence
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeMath
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RemainingHeader(uiState)

        ActionsSection(
            uiState = uiState,
            onLog = viewModel::logEvent,
            onTogglePin = viewModel::togglePinned,
            onMovePin = viewModel::movePinned,
        )

        TodaySection(uiState = uiState, onUndo = viewModel::undoLastEvent)

        SettingsSection(
            uiState = uiState,
            onBirthDate = viewModel::setBirthDate,
            onBaseExpectancy = viewModel::setBaseExpectancyYears,
        )

        SyncSection(
            uiState = uiState,
            onEnabled = viewModel::setSyncEnabled,
            onUrl = viewModel::setSyncEndpoint,
            onToken = viewModel::setSyncToken,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RemainingHeader(uiState: MainUiState) {
    val expected = LifeMath.expectedDeathInstant(uiState.life, uiState.zone)
    val fraction = LifeMath.elapsedFraction(uiState.life, uiState.now, uiState.zone)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.remaining_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                // Тот же вид, что на виджете. Здесь секунды перерисовываются
                // честно каждую секунду — процесс на переднем плане.
                text = LifeMath.formatCountdown(uiState.life, uiState.now, uiState.zone),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            LifeProgressRow(fraction = fraction)
            Spacer(Modifier.height(12.dp))

            Text(
                text = "${LifeMath.formatYears(
                    LifeMath.remainingYears(uiState.life, uiState.now, uiState.zone),
                )} ${stringResource(R.string.years_suffix)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.expected_moment,
                    DATE_FORMAT.format(expected.atZone(uiState.zone)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Полоса прожитого: от ребёнка слева к черепу справа. */
@Composable
private fun LifeProgressRow(fraction: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "\uD83D\uDC76", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { fraction.toFloat() },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = "\uD83D\uDC80", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            text = LifeMath.formatElapsedPercent(fraction),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Действия: поиск, закреплённые кнопки виджета и полный список.
 *
 * Список строится из EventType.entries, поэтому новый тип появляется
 * здесь и в поиске сам.
 */
@Composable
private fun ActionsSection(
    uiState: MainUiState,
    onLog: (EventType) -> Unit,
    onTogglePin: (EventType) -> Unit,
    onMovePin: (EventType, Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var evidenceFor by remember { mutableStateOf<EventType?>(null) }
    val found = EventType.entries.filter { it.matches(query) }
    val searching = query.isNotBlank()

    evidenceFor?.let { type ->
        EvidenceDialog(type = type, onDismiss = { evidenceFor = null })
    }

    SectionCard(title = stringResource(R.string.actions_title)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.actions_search)) },
            singleLine = true,
            trailingIcon = if (searching) {
                {
                    TextButton(onClick = { query = "" }) { Text("×") }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // Закреплённое прячем во время поиска: там нужен список, а не настройки.
        if (!searching) {
            Text(
                text = stringResource(R.string.pinned_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            if (uiState.pinned.isEmpty()) {
                Text(
                    text = stringResource(R.string.pinned_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.pinned.forEachIndexed { index, type ->
                    PinnedRow(
                        type = type,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.pinned.lastIndex,
                        onMove = { offset -> onMovePin(type, offset) },
                        onUnpin = { onTogglePin(type) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.pinned_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
        }

        if (found.isEmpty()) {
            Text(
                text = stringResource(R.string.actions_nothing_found, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            found.forEach { type ->
                ActionRow(
                    type = type,
                    // Цена именно СЛЕДУЮЩЕГО нажатия: у типов с нормой она
                    // меняется по ходу периода, и видеть надо её, а не номинал.
                    nextDelta = uiState.life.deltaFor(type, uiState.now, uiState.zone),
                    normLeft = uiState.life.remainingInNorm(type, uiState.now, uiState.zone),
                    usedInPeriod = uiState.life.countInPeriod(type, uiState.now, uiState.zone),
                    usedTimes = uiState.usageCounts[type] ?: 0,
                    pinned = type in uiState.pinned,
                    onLog = { onLog(type) },
                    onTogglePin = { onTogglePin(type) },
                    onShowEvidence = { evidenceFor = type },
                )
            }
        }
    }
}

/** Строка закреплённого действия: порядок и открепление. */
@Composable
private fun PinnedRow(
    type: EventType,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onUnpin: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${type.emoji} ${type.label}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Text("↑") }
        TextButton(onClick = { onMove(1) }, enabled = canMoveDown) { Text("↓") }
        TextButton(onClick = onUnpin) { Text("★") }
    }
}

@Composable
private fun TodaySection(uiState: MainUiState, onUndo: () -> Unit) {
    val today = uiState.life.eventsOn(LocalDate.now(uiState.zone), uiState.zone)

    SectionCard(title = stringResource(R.string.today_title)) {
        if (today.isEmpty()) {
            Text(
                text = stringResource(R.string.today_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            today.forEach { event -> TodayRow(event = event, zone = uiState.zone) }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.today_total,
                    today.sumOf { it.deltaMinutes }.withExplicitSign(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onUndo,
            enabled = uiState.life.events.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.undo_last))
        }
    }
}

@Composable
private fun TodayRow(event: LifeEvent, zone: ZoneId) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = TIME_FORMAT.format(event.at.atZone(zone)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(0.08f))
        Text(
            text = "${event.type.emoji} ${event.type.label}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.minutes_delta, event.deltaMinutes.withExplicitSign()),
            style = MaterialTheme.typography.bodyMedium,
            color = if (event.deltaMinutes >= 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

/**
 * Строка действия: нажатие записывает событие, звезда закрепляет на виджете,
 * «i» показывает, откуда взялась цифра.
 *
 * Рядом видно, сколько раз действие уже вносили — чтобы закреплять то,
 * чем реально пользуешься.
 */
@Composable
private fun ActionRow(
    type: EventType,
    nextDelta: Int,
    normLeft: Int,
    usedInPeriod: Int,
    usedTimes: Int,
    pinned: Boolean,
    onLog: () -> Unit,
    onTogglePin: () -> Unit,
    onShowEvidence: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onLog,
            // Нулевые действия жать можно: на остаток они не влияют, но как
            // трекер привычки «принял витамины» кнопка всё равно полезна.
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "${type.emoji} ${type.label}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )
            Text(
                text = stringResource(R.string.minutes_delta, nextDelta.withExplicitSign()),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        TextButton(onClick = onShowEvidence) {
            Text(text = "ⓘ", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onTogglePin) {
            Text(
                text = if (pinned) "★" else "☆",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    val dosing = type.dosing
    val normHint = when {
        dosing == null -> null
        normLeft > 0 -> stringResource(
            R.string.norm_left, dosing.period.label, normLeft, dosing.normal,
        )
        else -> stringResource(
            R.string.norm_exceeded, dosing.period.label, usedInPeriod, dosing.normal,
        )
    }

    Text(
        text = listOfNotNull(
            type.evidence.confidence.label,
            normHint,
            if (usedTimes == 0) {
                stringResource(R.string.actions_never_used)
            } else {
                stringResource(R.string.actions_used_times, usedTimes)
            },
        ).joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        // Исчерпанная норма подсвечивается: это и есть сигнал «дальше в минус».
        color = if (dosing != null && normLeft == 0) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
    )
}

/**
 * Пруф по действию.
 *
 * Показывает не только ссылку, но и что именно мерили, как из этого вышло
 * число и чему верить не стоит. Без последнего пункта пруф превращается
 * в видимость обоснованности.
 */
@Composable
private fun EvidenceDialog(type: EventType, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val evidence: Evidence = type.evidence

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${type.emoji} ${type.label}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.evidence_value,
                        type.deltaMinutes.withExplicitSign(),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                ConfidenceBadge(evidence.confidence)
                type.dosing?.let { dosing ->
                    EvidenceBlock(
                        stringResource(R.string.evidence_norm),
                        stringResource(
                            R.string.evidence_norm_body,
                            dosing.normal,
                            dosing.period.label,
                            dosing.withinNormalMinutes.withExplicitSign(),
                            dosing.beyondNormalMinutes.withExplicitSign(),
                        ),
                    )
                }
                EvidenceBlock(stringResource(R.string.evidence_exposure), evidence.exposure)
                EvidenceBlock(stringResource(R.string.evidence_basis), evidence.basis)
                EvidenceBlock(stringResource(R.string.evidence_caveat), evidence.caveat)
                EvidenceBlock(stringResource(R.string.evidence_source), evidence.sourceTitle)
            }
        },
        confirmButton = {
            TextButton(onClick = { uriHandler.openUri(evidence.sourceUrl) }) {
                Text(stringResource(R.string.evidence_open_source))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.evidence_close)) }
        },
    )
}

@Composable
private fun ConfidenceBadge(confidence: Confidence) {
    val color = when (confidence) {
        Confidence.STRONG -> MaterialTheme.colorScheme.primary
        Confidence.MODERATE -> MaterialTheme.colorScheme.secondary
        Confidence.WEAK, Confidence.NONE, Confidence.ESTIMATE -> MaterialTheme.colorScheme.error
        Confidence.CHOSEN -> MaterialTheme.colorScheme.tertiary
    }
    Text(
        text = confidence.label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
    )
}

@Composable
private fun EvidenceBlock(title: String, body: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = body, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSection(
    uiState: MainUiState,
    onBirthDate: (LocalDate) -> Unit,
    onBaseExpectancy: (Double) -> Unit,
) {
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var expectancyText by rememberSaveable(uiState.life.baseExpectancyYears) {
        mutableStateOf(uiState.life.baseExpectancyYears.toString())
    }
    val parsedExpectancy = expectancyText.replace(',', '.').toDoubleOrNull()
    val expectancyValid = parsedExpectancy != null && parsedExpectancy > 0

    SectionCard(title = stringResource(R.string.settings_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.birth_date_label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { pickingDate = true }) {
                Text(DATE_FORMAT.format(uiState.life.birthDate))
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = expectancyText,
            onValueChange = { typed ->
                expectancyText = typed
                val parsed = typed.replace(',', '.').toDoubleOrNull()
                if (parsed != null && parsed > 0) onBaseExpectancy(parsed)
            },
            label = { Text(stringResource(R.string.base_expectancy_label)) },
            isError = !expectancyValid,
            supportingText = if (expectancyValid) {
                null
            } else {
                { Text(stringResource(R.string.base_expectancy_invalid)) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.life.birthDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            // DatePicker отдаёт полночь в UTC, поэтому и читаем в UTC:
                            // через системную зону дата могла бы съехать на сутки.
                            onBirthDate(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                            )
                        }
                        pickingDate = false
                    },
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SyncSection(
    uiState: MainUiState,
    onEnabled: (Boolean) -> Unit,
    onUrl: (String) -> Unit,
    onToken: (String) -> Unit,
) {
    SectionCard(title = stringResource(R.string.sync_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sync_enabled_label),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = uiState.sync.enabled, onCheckedChange = onEnabled)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.sync.endpointUrl,
            onValueChange = onUrl,
            label = { Text(stringResource(R.string.sync_url_label)) },
            enabled = uiState.sync.enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.sync.bearerToken,
            onValueChange = onToken,
            label = { Text(stringResource(R.string.sync_token_label)) },
            enabled = uiState.sync.enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.sync_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** «+30» читается лучше, чем «30», когда рядом стоит «-15». */
private fun Int.withExplicitSign(): String = when {
    this > 0 -> "+$this"
    else -> toString()
}

