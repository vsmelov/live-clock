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
import androidx.glance.LocalContext
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
import java.time.ZoneId

/**
 * Виджет на домашний экран: остаток жизни в годах, отсчёт до конца суток
 * и две кнопки быстрых действий.
 *
 * Что здесь обновляется и как часто:
 * - крупное число — по расписанию системы (30 минут из appwidget-provider.xml
 *   плюс периодический воркер на 15 минут) и сразу после нажатия кнопки;
 * - секунды — сами, на Chronometer, без участия нашего процесса.
 *
 * Чаще система обновлять не даст, и обходить это бессмысленно: попытка
 * будить процесс раз в секунду закончится тем, что систему сожрёт батарея,
 * а нас прибьют.
 */
class LifeClockWidget : GlanceAppWidget() {

    /**
     * Виджет должен верстаться от 2x2 до 4x2. Glance выберет ближайший
     * подходящий размер и подставит его в [LocalSize].
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = LifeRepository.from(context)
        // Первое значение читаем до provideContent, чтобы виджет не моргал
        // пустотой на старте композиции.
        val initialState = repository.currentState()

        provideContent {
            val state by repository.state.collectAsState(initial = initialState)
            WidgetBody(state)
        }
    }

    companion object {
        /** Примерно две ячейки лаунчера. */
        private val COMPACT_SIZE = DpSize(110.dp, 110.dp)

        /** Примерно четыре ячейки в ширину. */
        private val WIDE_SIZE = DpSize(250.dp, 110.dp)

        /** Ниже этой ширины подписи кнопок ужимаются. */
        private val COMPACT_WIDTH_THRESHOLD = 180.dp

        /** Что висит на кнопках виджета. Остальные типы живут в Activity. */
        val WIDGET_BUTTONS: List<EventType> = listOf(EventType.SMOKE, EventType.REST)

        internal fun isCompact(width: androidx.compose.ui.unit.Dp): Boolean =
            width < COMPACT_WIDTH_THRESHOLD
    }
}

@Composable
private fun WidgetBody(state: LifeState) {
    val compact = LifeClockWidget.isCompact(LocalSize.current.width)
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    val yearsSuffix = LocalContext.current.getString(R.string.years_suffix)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .cornerRadius(16.dp)
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${LifeMath.formatRemainingYears(state, now, zone)} $yearsSuffix",
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_primary),
                fontSize = if (compact) 19.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )

        Spacer(GlanceModifier.height(2.dp))

        CountdownChronometer(
            now = now,
            zone = zone,
            textSizeSp = if (compact) 11f else 13f,
        )

        Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            LifeClockWidget.WIDGET_BUTTONS.forEachIndexed { index, type ->
                if (index > 0) {
                    Spacer(GlanceModifier.width(6.dp))
                }
                QuickActionButton(
                    type = type,
                    compact = compact,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

/**
 * Широкая кнопка события.
 *
 * Нажатие уходит в [LogEventAction] — тип передаётся строковым id, чтобы
 * добавление нового типа не требовало правок здесь.
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
            .padding(horizontal = 4.dp, vertical = if (compact) 7.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            // На 2x2 эмодзи съело бы место у подписи, поэтому остаётся только текст.
            text = if (compact) type.label else "${type.emoji} ${type.label}",
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = if (compact) 10.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}
