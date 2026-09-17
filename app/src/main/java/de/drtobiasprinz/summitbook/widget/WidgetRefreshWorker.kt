package de.drtobiasprinz.summitbook.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Refreshes the widget shortly after device boot or app update.
 *
 * After a reboot the system restores widgets from cached views and the
 * updatePeriodMillis timer starts over, so the widget would otherwise stay
 * on its initial layout until the app is opened. Enqueuing this worker from
 * the receiver gives Glance a fresh, non-time-constrained environment to
 * run provideGlance in.
 */
class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Refreshing widget after boot/update")
            SummitBookGlanceWidget().updateAll(applicationContext)
            Log.i(TAG, "Widget refreshed")
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing widget", e)
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        private const val UNIQUE_WORK_NAME = "widget-refresh-after-boot"
        private const val MAX_ATTEMPTS = 3
        private const val INITIAL_DELAY_SECONDS = 30L

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.i(TAG, "Scheduled widget refresh in ${INITIAL_DELAY_SECONDS}s")
        }
    }
}
