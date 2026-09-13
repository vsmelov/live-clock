package com.vsmelov.liveclock.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.vsmelov.liveclock.work.WidgetRefreshWorker

/**
 * Точка входа системы в виджет. Зарегистрирован в манифесте вместе
 * с life_clock_widget_info.xml.
 *
 * Периодическое обновление заводится здесь, а не в Application: будить
 * процесс раз в пятнадцать минут имеет смысл только пока виджет реально
 * висит на экране.
 */
class LifeClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = LifeClockWidget()

    /** Появился первый виджет. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.schedule(context)
    }

    /** Убран последний виджет. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshWorker.cancel(context)
    }
}
