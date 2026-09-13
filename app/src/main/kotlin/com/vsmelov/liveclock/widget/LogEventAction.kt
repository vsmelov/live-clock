package com.vsmelov.liveclock.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.vsmelov.liveclock.data.LifeRepository
import com.vsmelov.liveclock.domain.EventType
import java.time.Instant

private const val TAG = "LogEventAction"

/**
 * Нажатие кнопки на виджете.
 *
 * Здесь сознательно нет ни сети, ни блокирующих вызовов: записали в DataStore,
 * перерисовали виджет — и вышли. Всё остальное делает воркер.
 * Коллбэк работает в узком окне, которое даёт система, и любая задержка
 * здесь превращается в подвисшую кнопку.
 */
class LogEventAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val typeId = parameters[EventTypeKey]
        val type = typeId?.let(EventType::fromId)
        if (type == null) {
            Log.w(TAG, "Нажата кнопка неизвестного типа: $typeId")
            return
        }

        LifeRepository.from(context).addEvent(type, Instant.now())
        LifeClockWidget().updateAll(context)
    }

    companion object {
        /** Тип события передаётся строковым id — enum в параметры не кладётся. */
        val EventTypeKey: ActionParameters.Key<String> = ActionParameters.Key("event_type_id")
    }
}
