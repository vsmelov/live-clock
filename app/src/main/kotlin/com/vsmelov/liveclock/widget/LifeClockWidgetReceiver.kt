package com.vsmelov.liveclock.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.vsmelov.liveclock.work.WidgetRefreshWorker

/**
 * The system's entry point into the widget. Registered in the manifest together
 * with life_clock_widget_info.xml.
 *
 * The periodic refresh is scheduled here rather than in Application: waking the
 * process every fifteen minutes only makes sense while a widget is actually on
 * a home screen.
 */
class LifeClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = LifeClockWidget()

    /** The first widget appeared. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }

    /** The last widget was removed. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }
}
