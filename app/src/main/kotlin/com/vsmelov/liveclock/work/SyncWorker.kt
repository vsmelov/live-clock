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
 * Uploads the events a server has not seen yet.
 *
 * Everything that must not live in an ActionCallback goes here: the network, the
 * waiting and the retries. A failure loses nothing — the log stays in the
 * DataStore and the worker tries again later.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = LifeRepository.from(applicationContext)
        val settings = repository.currentSyncSettings()

        // Sync is off or unconfigured. That is normal, not an error.
        if (!settings.isUsable) return Result.success()

        val pending = repository.currentState().eventsAfter(settings.lastSyncedAt)
        if (pending.isEmpty()) return Result.success()

        return try {
            SyncClientProvider.forSettings(settings).push(pending)
            // Events are sorted, so the last one is the latest.
            repository.markSyncedUpTo(pending.last().at)
            Result.success()
        } catch (error: IOException) {
            Log.w(TAG, "Sync failed, attempt $runAttemptCount", error)
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "life-clock-sync"
        private const val MAX_ATTEMPTS = 5

         /**
         * Queues a one-off upload.
         *
         * REPLACE rather than APPEND: every run sends the whole unsynced tail, so
         * a burst of quick taps should collapse into a single upload.
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
