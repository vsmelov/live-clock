package com.vsmelov.liveclock.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Виджет на домашний экран: обратный отсчёт остатка жизни и закреплённые
 * кнопки быстрых действий.
 *
 * Что здесь обновляется и как часто:
 * - секунды тикают сами, на Chronometer, без участия нашего процесса;
 * - сутки, годы и итог за день пересчитываются по расписанию системы
 *   (30 минут из appwidget-provider.xml плюс воркер на 15 минут)
 *   и сразу после нажатия кнопки.
 *
 * Чаще система обновлять не даст, и обходить это бессмысленно: попытка
 * будить процесс раз в секунду закончится тем, что нас прибьют за батарею.
 */
class LifeClockWidget : GlanceAppWidget() {

    /**
     * Виджет должен верстаться от 2x2 и выше. Glance выберет ближайший
     * подходящий размер и подставит его в [LocalSize].
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = LifeRepository.from(context)
        // Первые значения читаем до provideContent, чтобы виджет не моргал
        // пустотой на старте композиции.
        val initialState = repository.currentState()
        val initialPinned = repository.currentPinnedTypes()

        provideContent {
            val state by repository.state.collectAsState(initial = initialState)
            val pinned by repository.pinnedTypes.collectAsState(initial = initialPinned)
            WidgetBody(state = state, pinned = pinned)
        }
    }

    companion object {
        /** Примерно две ячейки лаунчера. */
        private val COMPACT = DpSize(110.dp, 110.dp)

        /** Четыре ячейки в ширину, две в высоту. */
        private val WIDE = DpSize(250.dp, 110.dp)

        /** Четыре ячейки в ширину, три и выше. */
        private val TALL = DpSize(250.dp, 190.dp)

        /** Ниже этой ширины подписи и шрифты ужимаются. */
        internal val COMPACT_WIDTH_THRESHOLD = 180.dp

        /** Выше этой высоты помещается второй ряд кнопок. */
        internal val TALL_HEIGHT_THRESHOLD = 160.dp
    }
}

/** Во что превращается доступное место. Всё, что зависит от размера, — здесь. */
private data class WidgetMetrics(
    val compact: Boolean,
    val tall: Boolean,
    val buttonsPerRow: Int,
    val buttonRows: Int,
    val chronometerHeight: androidx.compose.ui.unit.Dp,
    val chronometerSizeSp: Float,
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
        // Запас примерно в 1.6 от кегля: sp растёт вместе с системной
        // настройкой размера текста, и без запаса цифры обрежет.
        chronometerHeight = if (compact) 26.dp else 38.dp,
        chronometerSizeSp = if (compact) 16f else 24f,
    )
}

@Composable
private fun WidgetBody(state: LifeState, pinned: List<EventType>) {
    val metrics = rememberMetrics()
    val zone = ZoneId.systemDefault()
    val now = Instant.now()

    val days = LifeMath.remainingWholeDays(state, now, zone)
    val withinDay = LifeMath.remainingWithinDay(state, now, zone)
    val todayDelta = state.deltaOn(LocalDate.now(zone), zone)

    val buttons = (pinned.ifEmpty { EventType.DEFAULT_PINNED }).take(metrics.buttonBudget)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .cornerRadius(16.dp)
            .padding(horizontal = if (metrics.compact) 8.dp else 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$days дней",
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_secondary),
                fontSize = if (metrics.compact) 12.sp else 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )

        // Явная высота: без неё AndroidRemoteViews забирает всю оставшуюся
        // высоту колонки и выдавливает кнопки за край виджета.
        Box(modifier = GlanceModifier.fillMaxWidth().height(metrics.chronometerHeight)) {
            CountdownChronometer(
                withinDay = withinDay,
                textSizeSp = metrics.chronometerSizeSp,
            )
        }

        if (!metrics.compact) {
            Text(
                text = buildSummary(state, now, zone, todayDelta),
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_secondary),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.height(if (metrics.compact) 6.dp else 10.dp))

        buttons.chunked(metrics.buttonsPerRow).forEachIndexed { rowIndex, rowButtons ->
            if (rowIndex > 0) {
                Spacer(GlanceModifier.height(6.dp))
            }
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                rowButtons.forEachIndexed { index, type ->
                    if (index > 0) {
                        Spacer(GlanceModifier.width(6.dp))
                    }
                    QuickActionButton(
                        type = type,
                        compact = metrics.compact,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
                // Добиваем неполный ряд, чтобы кнопки не растягивались
                // на всю ширину, когда их меньше, чем мест.
                repeat(metrics.buttonsPerRow - rowButtons.size) {
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

private fun buildSummary(state: LifeState, now: Instant, zone: ZoneId, todayDelta: Int): String {
    val years = LifeMath.formatRemainingYears(state, now, zone)
    return if (todayDelta == 0) {
        "$years лет"
    } else {
        val sign = if (todayDelta > 0) "+" else "−"
        "$years лет · сегодня $sign${kotlin.math.abs(todayDelta)} мин"
    }
}

/**
 * Кнопка закреплённого действия.
 *
 * Нажатие уходит в [LogEventAction] — тип передаётся строковым id, поэтому
 * добавление нового типа не требует правок здесь.
 */
@Composable
private fun QuickActionButton(
    type: EventType,
    compact: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
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
            .padding(horizontal = 4.dp, vertical = if (compact) 7.dp else 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // На узком виджете эмодзи съело бы место у подписи.
            text = if (compact) type.label else "${type.emoji} ${type.label}",
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
