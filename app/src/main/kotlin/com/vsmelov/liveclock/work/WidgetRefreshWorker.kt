package com.vsmelov.liveclock.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vsmelov.liveclock.widget.LifeClockWidget
import java.util.concurrent.TimeUnit

/**
 * Recomputes the large figure on the widget.
 *
 * Needed because updatePeriodMillis alone is unreliable under Doze: the system
 * is free to hold an update back. The worker is a second, more battery-friendly
 * channel.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        LifeClockWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "life-clock-widget-refresh"

        /**
         * Fifteen minutes is WorkManager's hard floor
         * ([androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]).
         * Asking for more is pointless: the system raises the period to this
         * anyway. The seconds on the widget come from the Chronometer, not here.
         */
        const val INTERVAL_MINUTES: Long = 15

        /** Called when the first widget appears on a home screen. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Called when the last widget is removed — nothing left to wake up for. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
