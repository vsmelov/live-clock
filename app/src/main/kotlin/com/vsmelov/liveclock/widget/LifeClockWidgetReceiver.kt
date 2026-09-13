package com.vsmelov.liveclock.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Точка входа системы в виджет. Зарегистрирован в манифесте вместе
 * с life_clock_widget_info.xml.
 */
class LifeClockWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = LifeClockWidget()
}
