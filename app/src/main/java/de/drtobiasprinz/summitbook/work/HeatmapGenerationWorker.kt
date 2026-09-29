package de.drtobiasprinz.summitbook.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.GroupForHeatmap
import de.drtobiasprinz.summitbook.data.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.sync.GpxPyExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Generates a heatmap MBTiles file from GPX tracks in a long-running
 * WorkManager worker. Running as a foreground service (type dataSync) keeps
 * the generation alive even when the app is backgrounded or the screen is
 * off; progress is shown as a notification with a per-zoom-level progress
 * bar.
 *
 * The track files are loaded from the database inside the worker because
 * WorkManager's input Data is limited to 10 KB - passing ~1000 absolute track
 * paths would exceed that limit.
 */
@HiltWorker
class HeatmapGenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DatabaseRepository
) : CoroutineWorker(appContext, workerParams) {

    private var foregroundActive = false

    override suspend fun doWork(): Result {
        val sportGroupName = inputData.getString(KEY_SPORT_GROUP) ?: ""
        val outputFileName = inputData.getString(KEY_OUTPUT_FILE_NAME)
            ?: return Result.failure()
        val displayName = inputData.getString(KEY_DISPLAY_NAME) ?: outputFileName
        val outputFile = File(getHeatmapDir(applicationContext), outputFileName)

        createNotificationChannel()
        try {
            setForeground(createForegroundInfo(displayName, 0, 0))
            foregroundActive = true
        } catch (e: Exception) {
            // e.g. starting a foreground service is not allowed because the
            // process is in the background (process death + retry): continue
            // as a regular background worker instead of failing outright.
            Log.w(TAG, "Could not run as foreground service, continuing in background", e)
        }

        return try {
            val trackFiles = withContext(Dispatchers.IO) {
                loadTrackFiles(sportGroupName)
            }
            if (trackFiles.isEmpty()) {
                Log.w(TAG, "No GPX track files found for sport group '$sportGroupName'")
                return Result.failure(
                    workDataOf(
                        KEY_SUCCESS to false,
                        KEY_OUTPUT_PATH to outputFile.absolutePath,
                        KEY_ERROR to "no_tracks"
                    )
                )
            }

            withContext(Dispatchers.IO) {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(applicationContext))
                }
                GpxPyExecutor(Python.getInstance()).generateHeatmap(
                    trackFiles,
                    outputFile
                ) { currentZoom, totalZoomLevels ->
                    setProgressAsync(
                        workDataOf(
                            KEY_PROGRESS_CURRENT to currentZoom,
                            KEY_PROGRESS_TOTAL to totalZoomLevels
                        )
                    )
                    updateNotification(displayName, currentZoom, totalZoomLevels)
                }
            }
            if (outputFile.exists()) {
                Result.success(
                    workDataOf(KEY_SUCCESS to true, KEY_OUTPUT_PATH to outputFile.absolutePath)
                )
            } else {
                Log.e(TAG, "Heatmap generation finished without output file $outputFile")
                Result.failure(
                    workDataOf(KEY_SUCCESS to false, KEY_OUTPUT_PATH to outputFile.absolutePath)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Heatmap generation failed", e)
            Result.failure(
                workDataOf(
                    KEY_SUCCESS to false,
                    KEY_OUTPUT_PATH to outputFile.absolutePath,
                    KEY_ERROR to e.message
                )
            )
        }
    }

    /**
     * Loads the summits from the database and returns the existing GPX track
     * files for the given sport group. An empty [sportGroupName] selects all
     * activities with tracks.
     */
    private suspend fun loadTrackFiles(sportGroupName: String): List<File> {
        val sportGroup = runCatching { GroupForHeatmap.valueOf(sportGroupName) }.getOrNull()
        return repository.getAllSummits().first()
            .filter { summit ->
                (sportGroup == null || summit.sportType in sportGroup.sportTypes) &&
                    summit.hasGpsTrack()
            }
            .mapNotNull { summit ->
                val trackFile = summit.getGpsTrackPath().toFile()
                if (trackFile.exists()) trackFile else null
            }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.heatmap_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
    }

    private fun buildNotification(displayName: String, current: Int, total: Int) =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_heatmap_24)
            .setContentTitle(
                applicationContext.getString(R.string.heatmap_notification_title, displayName)
            )
            .setContentText(
                if (total > 0) {
                    applicationContext.getString(
                        R.string.heatmap_notification_text, current, total
                    )
                } else {
                    null
                }
            )
            .setProgress(total, current, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    private fun createForegroundInfo(displayName: String, current: Int, total: Int): ForegroundInfo {
        val notification = buildNotification(displayName, current, total)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(displayName: String, current: Int, total: Int) {
        if (!foregroundActive) {
            return
        }
        // Called from the Chaquopy callback thread; setForegroundAsync is
        // safe to call from any thread and updates the existing notification.
        setForegroundAsync(createForegroundInfo(displayName, current, total))
    }

    companion object {
        const val TAG = "HeatmapGenerationWorker"
        const val TAG_ITEM_PREFIX = "heatmap_item_"
        const val KEY_SPORT_GROUP = "sport_group"
        const val KEY_OUTPUT_FILE_NAME = "output_file_name"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_SUCCESS = "success"
        const val KEY_ERROR = "error"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_PROGRESS_CURRENT = "progress_current"
        const val KEY_PROGRESS_TOTAL = "progress_total"
        private const val CHANNEL_ID = "heatmap_generation"
        private const val NOTIFICATION_ID = 4242

        /** Must match the directory set up in MainActivityCompose. */
        private const val HEATMAP_DIR_NAME = "heatmaps"

        private fun getHeatmapDir(context: Context) = File(context.filesDir, HEATMAP_DIR_NAME)

        fun uniqueWorkName(outputFileName: String) = "heatmap_generation_$outputFileName"

        fun itemTag(outputFileName: String) = TAG_ITEM_PREFIX + outputFileName

        /**
         * Enqueues the generation for the given output file. ExistingWorkPolicy.KEEP
         * prevents enqueuing a duplicate while a generation for the same file is
         * already running or pending.
         *
         * Only small strings are passed as input data (see class doc); the worker
         * loads the tracks from the database itself.
         */
        fun enqueue(
            context: Context,
            sportGroup: GroupForHeatmap?,
            outputFileName: String,
            displayName: String
        ) {
            val request = OneTimeWorkRequestBuilder<HeatmapGenerationWorker>()
                .setInputData(
                    workDataOf(
                        KEY_SPORT_GROUP to (sportGroup?.name ?: ""),
                        KEY_OUTPUT_FILE_NAME to outputFileName,
                        KEY_DISPLAY_NAME to displayName
                    )
                )
                .addTag(TAG)
                .addTag(itemTag(outputFileName))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(outputFileName),
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
