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
 * The per-second countdown of life remaining.
 *
 * The only part of the widget that updates more often than twice an hour — and it
 * updates without us: [android.widget.Chronometer] is ticked by the launcher. The
 * system would never let us wake our own process once a second anyway.
 *
 * Only the within-day remainder ticks: a Chronometer formats time as "H:MM:SS"
 * and cannot do days, so 17,310 days would come out as 415,440 hours. The days
 * are substituted into [format] and refreshed on the widget's ordinary schedule,
 * giving "17310d 1:56:26" on one line.
 */
@Composable
fun CountdownChronometer(
    format: String,
    withinDay: Duration,
    textSizeSp: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val remoteViews = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        setChronometerCountDown(R.id.countdown, true)
        setChronometer(R.id.countdown, elapsedRealtimeBaseIn(withinDay), format, true)
        setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    }
    // An explicit fillMaxSize: without it AndroidRemoteViews swells to the whole
    // remaining column height and pushes the buttons off the edge of the widget.
    AndroidRemoteViews(remoteViews = remoteViews, modifier = modifier.fillMaxSize())
}

/**
 * The countdown target expressed in the [SystemClock.elapsedRealtime] timebase.
 *
 * A Chronometer counts from boot time rather than from the wall clock, so the
 * target has to be expressed on the same scale.
 */
internal fun elapsedRealtimeBaseIn(remaining: Duration): Long =
    SystemClock.elapsedRealtime() + remaining.toMillis()
