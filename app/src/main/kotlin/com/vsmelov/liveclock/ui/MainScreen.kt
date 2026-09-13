package com.vsmelov.liveclock.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vsmelov.liveclock.R
import com.vsmelov.liveclock.data.Backup
import com.vsmelov.liveclock.domain.ActionSearch
import com.vsmelov.liveclock.domain.AppLanguage
import com.vsmelov.liveclock.domain.Confidence
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.Evidence
import com.vsmelov.liveclock.domain.LifeEvent
import com.vsmelov.liveclock.domain.LifeMath
import com.vsmelov.liveclock.domain.Streak
import com.vsmelov.liveclock.domain.WeekSummary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun MainScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RemainingHeader(uiState)
        StreaksSection(uiState.streaks)
        ActionsSection(
            uiState = uiState,
            onLog = viewModel::logEvent,
            onTogglePin = viewModel::togglePinned,
            onMovePin = viewModel::movePinned,
        )
        TodaySection(uiState = uiState, onUndo = viewModel::undoLastEvent)
        WeekSection(uiState.week)
        SettingsSection(
            uiState = uiState,
            onBirthDate = viewModel::setBirthDate,
            onBaseExpectancy = viewModel::setBaseExpectancyYears,
            onLanguage = viewModel::setLanguage,
        )
        BackupSection(viewModel)
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
                // The same shape as on the widget. Here the seconds really are
                // redrawn every second — the process is in the foreground.
                text = LifeMath.formatCountdown(uiState.life, uiState.now, uiState.zone),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            LifeProgressRow(fraction)
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

/** The lived-fraction bar: a baby on the left, a skull on the right. */
@Composable
private fun LifeProgressRow(fraction: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "👶", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { fraction.toFloat() },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = "💀", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            text = LifeMath.formatElapsedPercent(fraction),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Runs of days.
 *
 * A habit is held together by continuity, and a day's total says nothing about
 * that. Only types that appear in the log are counted: "never smoked" would be
 * an infinite streak and mean nothing.
 */
@Composable
private fun StreaksSection(streaks: List<Streak>) {
    SectionCard(title = stringResource(R.string.streaks_title)) {
        if (streaks.isEmpty()) {
            Text(
                text = stringResource(R.string.streaks_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        streaks.forEach { streak ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${streak.type.emoji} " + if (streak.clean) {
                        stringResource(R.string.streak_clean_of, stringResource(streak.type.labelRes))
                    } else {
                        stringResource(streak.type.labelRes)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.streak_days, streak.days),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Actions: search, the pinned widget buttons and the full list.
 *
 * The list is built from EventType.entries, so a new type turns up here and in
 * search on its own.
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
    val context = LocalContext.current
    val found = EventType.entries.filter { type ->
        ActionSearch.matches(
            query = query,
            id = type.id,
            label = context.getString(type.labelRes),
            keywords = context.getString(type.keywordsRes),
        )
    }
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
                { TextButton(onClick = { query = "" }) { Text("×") } }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        // The pinned block hides while searching: there you want a list, not settings.
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
                    // The price of the NEXT tap specifically: for types with an
                    // allowance it shifts through the period, and that is what
                    // matters, not the headline figure.
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

/** A pinned action row: ordering and unpinning. */
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
            text = "${type.emoji} ${stringResource(type.labelRes)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Text("↑") }
        TextButton(onClick = { onMove(1) }, enabled = canMoveDown) { Text("↓") }
        TextButton(onClick = onUnpin) { Text("★") }
    }
}

/**
 * An action row: tapping records the event, the star pins it to the widget, "i"
 * shows where the number came from.
 *
 * Beside it is how many times the action has been logged — so you pin what you
 * actually use.
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
            // Zero-valued actions stay tappable: they do not move the estimate,
            // but as a habit tracker "took my vitamins" is still worth logging.
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "${type.emoji} ${stringResource(type.labelRes)}",
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
            R.string.norm_left, stringResource(dosing.period.labelRes), normLeft, dosing.normal,
        )
        else -> stringResource(
            R.string.norm_exceeded, stringResource(dosing.period.labelRes), usedInPeriod, dosing.normal,
        )
    }

    Text(
        text = listOfNotNull(
            stringResource(type.evidence.confidence.labelRes),
            normHint,
            if (usedTimes == 0) {
                stringResource(R.string.actions_never_used)
            } else {
                stringResource(R.string.actions_used_times, usedTimes)
            },
        ).joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        // A used-up allowance is highlighted: that is the "from here it costs" signal.
        color = if (dosing != null && normLeft == 0) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
    )
}

/**
 * The proof behind an action.
 *
 * Shows not only a link but what was measured, how the number follows and what
 * not to trust. Without that last part a proof turns into the appearance of
 * being well-founded.
 */
@Composable
private fun EvidenceDialog(type: EventType, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val evidence: Evidence = type.evidence

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${type.emoji} ${stringResource(type.labelRes)}") },
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
                            stringResource(dosing.period.labelRes),
                            dosing.withinNormalMinutes.withExplicitSign(),
                            dosing.beyondNormalMinutes.withExplicitSign(),
                        ),
                    )
                }
                EvidenceBlock(stringResource(R.string.evidence_exposure), stringResource(evidence.exposureRes))
                EvidenceBlock(stringResource(R.string.evidence_basis), stringResource(evidence.basisRes))
                EvidenceBlock(stringResource(R.string.evidence_caveat), stringResource(evidence.caveatRes))
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
        text = stringResource(confidence.labelRes),
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

@Composable
private fun TodaySection(uiState: MainUiState, onUndo: () -> Unit) {
    val today = uiState.life.eventsOn(uiState.today, uiState.zone)

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
            text = "${event.type.emoji} ${stringResource(event.type.labelRes)}",
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
 * The week, not the day.
 *
 * The allowances are weekly, and one bad evening says far less than a bad week.
 */
@Composable
private fun WeekSection(week: WeekSummary) {
    SectionCard(title = stringResource(R.string.week_title)) {
        if (week.isEmpty) {
            Text(
                text = stringResource(R.string.today_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        Text(
            text = stringResource(R.string.week_total, week.totalMinutes.withExplicitSign()),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.week_events, week.eventCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        week.best?.let { (type, minutes) ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.week_best,
                    "${type.emoji} ${stringResource(type.labelRes)} ${minutes.withExplicitSign()}",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        week.worst?.let { (type, minutes) ->
            Text(
                text = stringResource(
                    R.string.week_worst,
                    "${type.emoji} ${stringResource(type.labelRes)} ${minutes.withExplicitSign()}",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSection(
    uiState: MainUiState,
    onBirthDate: (LocalDate) -> Unit,
    onBaseExpectancy: (Double) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
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

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = uiState.language == language,
                    onClick = { onLanguage(language) },
                    label = { Text(stringResource(language.labelRes)) },
                )
            }
        }
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
                            // The picker hands back midnight UTC, so it is read
                            // back in UTC: through the system zone the date could
                            // slip by a day.
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

/**
 * Export and restore.
 *
 * Sync is off by default and the DataStore is the only copy of anything, so a
 * lost phone is a lost history. This is the cheapest thing that closes that.
 */
@Composable
private fun BackupSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    val exportFailed = stringResource(R.string.export_failed)
    val exportDone = stringResource(R.string.export_done)
    val importFailed = stringResource(R.string.import_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = runCatching {
                val payload = viewModel.exportPayload()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(payload.toByteArray(Charsets.UTF_8))
                } ?: error("no output stream")
                exportDone
            }.getOrElse { exportFailed }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                }
            }.getOrNull()
            val restored = raw?.let { viewModel.restoreFrom(it) }
            message = if (restored == null) {
                importFailed
            } else {
                context.getString(R.string.import_done, restored)
            }
        }
    }

    SectionCard(title = stringResource(R.string.export_title)) {
        Text(
            text = stringResource(R.string.export_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { exportLauncher.launch(Backup.fileName(LocalDate.now().toString())) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.export_button))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.import_button))
        }
        message?.let { text ->
            Spacer(Modifier.height(8.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
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

/** "+30" reads better than "30" when "-15" sits next to it. */
private fun Int.withExplicitSign(): String = when {
    this > 0 -> "+$this"
    else -> toString()
}
