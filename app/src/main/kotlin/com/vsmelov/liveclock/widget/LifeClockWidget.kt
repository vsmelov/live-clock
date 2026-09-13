package com.vsmelov.liveclock.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vsmelov.liveclock.R
import com.vsmelov.liveclock.data.LifeRepository
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.domain.LifeMath
import com.vsmelov.liveclock.domain.LifeState
import com.vsmelov.liveclock.i18n.Localization
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** A pinned action with its label already resolved in the chosen language. */
private data class WidgetButton(val type: EventType, val label: String)

/**
 * The home screen widget: a countdown of life remaining and pinned quick actions.
 *
 * What refreshes, and how often:
 * - the seconds tick by themselves, on a Chronometer, with our process asleep;
 * - days, years and the day's total are recomputed on the system's schedule
 *   (30 minutes from appwidget-provider.xml plus a 15-minute worker) and
 *   immediately after a button press.
 *
 * The system will not allow more, and working around it is pointless: waking the
 * process once a second ends with the app being killed for battery use.
 */
class LifeClockWidget : GlanceAppWidget() {

    /**
     * The widget has to lay out from 2x2 upwards. Glance picks the closest
     * matching size and reports it through [LocalSize].
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = LifeRepository.from(context)
        // Read the first values before provideContent so the widget does not
        // flash empty while the composition starts.
        val initialState = repository.currentState()
        val initialPinned = repository.currentPinnedTypes()
        val language = repository.currentLanguage()

        // Labels are resolved here rather than inside the composition: Glance
        // gives the composition the process context, which answers in the device
        // language and would ignore the language chosen in the app.
        val localized = Localization.contextFor(context, language)
        val buttons = initialPinned.ifEmpty { EventType.DEFAULT_PINNED }
            .map { type -> WidgetButton(type, localized.getString(type.labelRes)) }
        val daysSuffix = localized.getString(R.string.widget_days_suffix)
        val todaySuffix = localized.getString(R.string.widget_today_suffix)

        provideContent {
            val state by repository.state.collectAsState(initial = initialState)
            WidgetBody(
                state = state,
                buttons = buttons,
                daysSuffix = daysSuffix,
                todaySuffix = todaySuffix,
            )
        }
    }

    companion object {
        /** Roughly two launcher cells. */
        private val COMPACT = DpSize(110.dp, 110.dp)

        /** Four cells wide, two tall. */
        private val WIDE = DpSize(250.dp, 110.dp)

        /** Four cells wide, three or more tall. */
        private val TALL = DpSize(250.dp, 190.dp)

        /** Below this width labels and type sizes shrink. */
        internal val COMPACT_WIDTH_THRESHOLD = 180.dp

        /** Above this height a second row of buttons fits. */
        internal val TALL_HEIGHT_THRESHOLD = 160.dp
    }
}

/** Everything that depends on the available space, in one place. */
private data class WidgetMetrics(
    val compact: Boolean,
    val tall: Boolean,
    val buttonsPerRow: Int,
    val buttonRows: Int,
    val chronometerHeight: Dp,
    val chronometerSizeSp: Float,
    val showSummary: Boolean,
) {
    val buttonBudget: Int get() = buttonsPerRow * buttonRows
}

@Composable
private fun rememberMetrics(): WidgetMetrics {
    val size = LocalSize.current
    val compact = size.width < LifeClockWidget.COMPACT_WIDTH_THRESHOLD
    val tall = size.height >= LifeClockWidget.TALL_HEIGHT_THRESHOLD
    return WidgetMetrics(
        compact = compact,
        tall = tall,
        buttonsPerRow = if (compact) 2 else 3,
        buttonRows = if (tall) 2 else 1,
        // Heights are chosen so the content fits its size without clipping. On
        // two cells of height (110dp less padding, so 90dp) the budget is tight:
        // countdown, progress bar and one row of buttons is all that fits, which
        // is why the summary line only appears on a tall widget.
        //
        // The countdown box allows about 1.5x the type size: sp scales with the
        // system font setting, and without headroom the digits get cut off.
        chronometerHeight = when {
            compact -> 22.dp
            tall -> 38.dp
            else -> 30.dp
        },
        chronometerSizeSp = when {
            compact -> 13f
            tall -> 24f
            else -> 20f
        },
        showSummary = tall,
    )
}

@Composable
private fun WidgetBody(
    state: LifeState,
    buttons: List<WidgetButton>,
    daysSuffix: String,
    todaySuffix: String,
) {
    val metrics = rememberMetrics()
    val zone = ZoneId.systemDefault()
    val now = Instant.now()

    val days = LifeMath.remainingWholeDays(state, now, zone)
    val withinDay = LifeMath.remainingWithinDay(state, now, zone)
    val todayDelta = state.deltaOn(LocalDate.now(zone), zone)
    val visible = buttons.take(metrics.buttonBudget)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .cornerRadius(16.dp)
            .padding(horizontal = if (metrics.compact) 8.dp else 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // An explicit height: without it AndroidRemoteViews takes the whole
        // remaining column height and pushes everything else off the widget.
        Box(modifier = GlanceModifier.fillMaxWidth().height(metrics.chronometerHeight)) {
            CountdownChronometer(
                format = "$days$daysSuffix %s",
                withinDay = withinDay,
                textSizeSp = metrics.chronometerSizeSp,
            )
        }

        Spacer(GlanceModifier.height(4.dp))

        LifeProgressRow(
            fraction = LifeMath.elapsedFraction(state, now, zone),
            compact = metrics.compact,
        )

        if (metrics.showSummary) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = buildSummary(state, now, zone, todayDelta, todaySuffix),
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_secondary),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.height(if (metrics.compact) 6.dp else 8.dp))

        visible.chunked(metrics.buttonsPerRow).forEachIndexed { rowIndex, rowButtons ->
            if (rowIndex > 0) {
                Spacer(GlanceModifier.height(6.dp))
            }
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                rowButtons.forEachIndexed { index, button ->
                    if (index > 0) {
                        Spacer(GlanceModifier.width(6.dp))
                    }
                    QuickActionButton(
                        button = button,
                        compact = metrics.compact,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
                // Pad out an incomplete row so the buttons do not stretch across
                // the full width when there are fewer of them than slots.
                repeat(metrics.buttonsPerRow - rowButtons.size) {
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

private fun buildSummary(
    state: LifeState,
    now: Instant,
    zone: ZoneId,
    todayDelta: Int,
    todaySuffix: String,
): String {
    val years = LifeMath.formatRemainingYears(state, now, zone)
    if (todayDelta == 0) return years
    val sign = if (todayDelta > 0) "+" else "−"
    return "$years · $todaySuffix $sign${abs(todayDelta)}"
}

/**
 * The lived-fraction bar: a baby on the left, a skull on the right.
 *
 * The number beside it is the fraction lived to three decimals. It moves slowly
 * enough to refresh on the widget's ordinary schedule, but visibly enough that
 * the difference shows from day to day.
 */
@Composable
private fun LifeProgressRow(fraction: Double, compact: Boolean) {
    val labelSize = if (compact) 9.sp else 11.sp

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "👶", style = TextStyle(fontSize = labelSize), maxLines = 1)
        Spacer(GlanceModifier.width(4.dp))
        LinearProgressIndicator(
            progress = fraction.toFloat(),
            modifier = GlanceModifier.defaultWeight().height(6.dp),
            color = ColorProvider(R.color.widget_progress),
            backgroundColor = ColorProvider(R.color.widget_progress_track),
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(text = "💀", style = TextStyle(fontSize = labelSize), maxLines = 1)
        Spacer(GlanceModifier.width(3.dp))
        Text(
            text = LifeMath.formatElapsedPercent(fraction),
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_secondary),
                fontSize = labelSize,
            ),
            maxLines = 1,
        )
    }
}

/**
 * A pinned action button.
 *
 * The press goes to [LogEventAction] carrying the type's string id, so adding a
 * new type needs no edits here.
 */
@Composable
private fun QuickActionButton(
    button: WidgetButton,
    compact: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    val type = button.type
    val backgroundColor = if (type.isGain) {
        R.color.widget_button_gain_background
    } else {
        R.color.widget_button_cost_background
    }
    val textColor = if (type.isGain) {
        R.color.widget_button_gain_text
    } else {
        R.color.widget_button_cost_text
    }

    Box(
        modifier = modifier
            .background(ColorProvider(backgroundColor))
            .cornerRadius(12.dp)
            .clickable(
                actionRunCallback<LogEventAction>(
                    actionParametersOf(LogEventAction.EventTypeKey to type.id),
                ),
            )
            .padding(horizontal = 4.dp, vertical = if (compact) 6.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // On a narrow widget the emoji would steal room from the label.
            text = if (compact) button.label else "${type.emoji} ${button.label}",
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}
