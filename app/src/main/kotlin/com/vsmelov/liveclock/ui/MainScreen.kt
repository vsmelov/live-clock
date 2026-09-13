package com.vsmelov.liveclock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vsmelov.liveclock.R
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeMath
import androidx.compose.ui.res.stringResource
import java.time.Duration
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

        ActionsSection(onLog = viewModel::logEvent)

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
    val remaining = LifeMath.remaining(uiState.life, uiState.now, uiState.zone)
    val expected = LifeMath.expectedDeathInstant(uiState.life, uiState.zone)

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
                text = "${LifeMath.formatYears(
                    LifeMath.remainingYears(uiState.life, uiState.now, uiState.zone),
                )} ${stringResource(R.string.years_suffix)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = remaining.asBreakdown(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
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

@Composable
private fun Duration.asBreakdown(): String {
    val total = if (isNegative) negated() else this
    val days = total.toDays()
    val hours = total.toHours() % 24
    val minutes = total.toMinutes() % 60
    return stringResource(R.string.remaining_breakdown, days.toString(), hours.toString(), minutes.toString())
}

/**
 * Сетка всех типов событий — полный набор, а не только те два, что висят
 * на виджете. Строится из EventType.entries, поэтому новый тип появляется
 * здесь сам.
 */
@Composable
private fun ActionsSection(onLog: (EventType) -> Unit) {
    SectionCard(title = stringResource(R.string.actions_title)) {
        EventType.entries.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { type ->
                    Button(
                        onClick = { onLog(type) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${type.emoji} ${type.label}", textAlign = TextAlign.Center)
                            Text(
                                text = stringResource(
                                    R.string.minutes_delta,
                                    type.deltaMinutes.withExplicitSign(),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                // Добиваем неполный ряд, чтобы кнопки не растягивались.
                repeat(COLUMNS - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
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
private fun Int.withExplicitSign(): String = if (this >= 0) "+$this" else toString()

private const val COLUMNS = 2
