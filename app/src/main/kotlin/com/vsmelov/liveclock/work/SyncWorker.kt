package com.vsmelov.liveclock.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.vsmelov.liveclock.data.LifeRepository
import com.vsmelov.liveclock.sync.SyncClientProvider
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "SyncWorker"

/**
 * Выгружает наружу события, которых сервер ещё не видел.
 *
 * Сюда вынесено всё, чего не должно быть в ActionCallback: сеть, ожидание
 * и повторы. Неуспех не теряет данные — лог остаётся в DataStore, а воркер
 * повторит попытку позже.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = LifeRepository.from(applicationContext)
        val settings = repository.currentSyncSettings()

        // Синк выключен или не настроен — это норма, а не ошибка.
        if (!settings.isUsable) return Result.success()

        val pending = repository.currentState().eventsAfter(settings.lastSyncedAt)
        if (pending.isEmpty()) return Result.success()

        return try {
            SyncClientProvider.forSettings(settings).push(pending)
            // События отсортированы, поэтому последнее — самое позднее.
            repository.markSyncedUpTo(pending.last().at)
            Result.success()
        } catch (error: IOException) {
            Log.w(TAG, "Синк не удался, попытка $runAttemptCount", error)
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "life-clock-sync"
        private const val MAX_ATTEMPTS = 5

        /**
         * Ставит разовую выгрузку в очередь.
         *
         * REPLACE, а не APPEND: каждый запуск отправляет весь несинхронизированный
         * хвост, поэтому серия быстрых нажатий должна схлопнуться в одну отправку.
         */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
