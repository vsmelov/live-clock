package com.vsmelov.liveclock.widget

import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import com.vsmelov.liveclock.R
import com.vsmelov.liveclock.domain.LifeMath
import java.time.Instant
import java.time.ZoneId

/**
 * Секундный отсчёт до конца суток.
 *
 * Единственная часть виджета, которая обновляется чаще, чем раз в полчаса —
 * и обновляется она без нас: [android.widget.Chronometer] тикает средствами
 * лаунчера. Поднимать ради секунд свой процесс система всё равно не позволит.
 */
@Composable
fun CountdownChronometer(
    now: Instant,
    zone: ZoneId,
    textSizeSp: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val remoteViews = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        setChronometerCountDown(R.id.countdown, true)
        setChronometer(R.id.countdown, elapsedRealtimeBaseFor(now, zone), null, true)
        setTextViewTextSize(R.id.countdown, android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    }
    AndroidRemoteViews(remoteViews = remoteViews, modifier = modifier)
}

/**
 * Начало следующих суток, пересчитанное в таймбазу [SystemClock.elapsedRealtime].
 *
 * Chronometer считает не по стенным часам, а по времени с момента загрузки,
 * поэтому целевой момент нужно перевести в ту же шкалу. Сдвиг между шкалами
 * берётся один раз здесь.
 */
internal fun elapsedRealtimeBaseFor(now: Instant, zone: ZoneId): Long {
    val targetWallClockMillis = LifeMath.startOfNextDay(now, zone).toEpochMilli()
    return SystemClock.elapsedRealtime() + (targetWallClockMillis - System.currentTimeMillis())
}
