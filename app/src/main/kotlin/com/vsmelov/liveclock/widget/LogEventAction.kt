package com.vsmelov.liveclock.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.vsmelov.liveclock.data.LifeRepository
import com.vsmelov.liveclock.domain.EventType
import com.vsmelov.liveclock.work.SyncWorker
import java.time.Instant

private const val TAG = "LogEventAction"

/**
 * A button press on the widget.
 *
 * There is deliberately no network here and nothing blocking: write to the
 * DataStore, redraw the widget, leave. Everything else is the worker's job. The
 * callback runs inside a narrow window the system grants, and any delay here
 * turns into a button that feels stuck.
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
            Log.w(TAG, "A button of an unknown type was pressed: $typeId")
            return
        }

        LifeRepository.from(context).addEvent(type, Instant.now())
        LifeClockWidget().updateAll(context)
        // Not the network — the worker deals with that and survives being offline.
        SyncWorker.enqueue(context)
    }

    companion object {
        /** The type travels as its string id; enums do not go into parameters. */
        val EventTypeKey: ActionParameters.Key<String> = ActionParameters.Key("event_type_id")
    }
}
