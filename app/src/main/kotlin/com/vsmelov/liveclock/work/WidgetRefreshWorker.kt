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
 * Пересчитывает крупное число на виджете.
 *
 * Нужен потому, что updatePeriodMillis сам по себе ненадёжен в Doze:
 * система вправе придержать обновление. Воркер даёт второй, более
 * дружелюбный к батарее канал.
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
         * Пятнадцать минут — жёсткий минимум WorkManager
         * ([androidx.work.PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]).
         * Просить чаще бессмысленно: система поднимет период до этого значения.
         * Секунды на виджете идут не отсюда, а с Chronometer.
         */
        const val INTERVAL_MINUTES: Long = 15

        /** Вызывается, когда на экране появляется первый виджет. */
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

        /** Вызывается, когда убран последний виджет — будить процесс больше незачем. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
