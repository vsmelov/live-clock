package com.vsmelov.liveclock.widget

import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.layout.fillMaxSize
import com.vsmelov.liveclock.R
import java.time.Duration

/**
 * Секундный обратный отсчёт остатка жизни.
 *
 * Единственная часть виджета, которая обновляется чаще, чем раз в полчаса —
 * и обновляется она без нас: [android.widget.Chronometer] тикает средствами
 * лаунчера. Поднимать ради секунд свой процесс система всё равно не позволит.
 *
 * Тикает только остаток внутри суток: Chronometer форматирует время как
 * «Ч:ММ:СС» и не умеет сутки, поэтому 17310 дней он показал бы как
 * 415440 часов. Сами сутки рисуются отдельной надписью рядом.
 */
@Composable
fun CountdownChronometer(
    withinDay: Duration,
    textSizeSp: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val remoteViews = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        setChronometerCountDown(R.id.countdown, true)
        setChronometer(R.id.countdown, elapsedRealtimeBaseIn(withinDay), null, true)
        setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    }
    // Явный fillMaxSize — иначе AndroidRemoteViews раздувается на всю
    // оставшуюся высоту колонки и выдавливает кнопки за край виджета.
    AndroidRemoteViews(remoteViews = remoteViews, modifier = modifier.fillMaxSize())
}

/**
 * Целевой момент отсчёта в таймбазе [SystemClock.elapsedRealtime].
 *
 * Chronometer считает не по стенным часам, а по времени с момента загрузки,
 * поэтому цель нужно выражать в той же шкале.
 */
internal fun elapsedRealtimeBaseIn(remaining: Duration): Long =
    SystemClock.elapsedRealtime() + remaining.toMillis()
