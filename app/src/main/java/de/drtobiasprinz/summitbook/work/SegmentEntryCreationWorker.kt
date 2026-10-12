package de.drtobiasprinz.summitbook.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import de.drtobiasprinz.summitbook.data.analytics.SegmentEntryCreator
import de.drtobiasprinz.summitbook.data.repository.DatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Creates segment entries and mountain passes in the background for newly
 * downloaded activities that were detected as the same course as an existing
 * activity: all segment entries and mountain passes of the matched activity
 * are used as templates and recreated for the new activity's own track.
 * Skips entries the new activity already has and slices that cannot be found
 * in the new track. Works silently, the segments UI refreshes itself via
 * Room's reactive queries.
 */
@HiltWorker
class SegmentEntryCreationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: DatabaseRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                val activityIdPairs = inputData.getStringArray(KEY_ACTIVITY_ID_PAIRS).orEmpty()
                    .mapNotNull { pair ->
                        val parts = pair.split(PAIR_SEPARATOR)
                        val newActivityId = parts.getOrNull(0)?.toLongOrNull()
                        val matchedActivityId = parts.getOrNull(1)?.toLongOrNull()
                        if (newActivityId != null && matchedActivityId != null) {
                            newActivityId to matchedActivityId
                        } else {
                            null
                        }
                    }
                var createdCount = 0
                for ((newActivityId, matchedActivityId) in activityIdPairs) {
                    createdCount += createEntriesForActivity(newActivityId, matchedActivityId)
                }
                Log.i(TAG, "Created $createdCount segment entries in total.")
                Result.success(workDataOf(KEY_CREATED_COUNT to createdCount))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Creating segment entries failed.", e)
            Result.retry()
        }
    }

    private suspend fun createEntriesForActivity(
        newActivityId: Long,
        matchedActivityId: Long
    ): Int {
        return try {
            val summits = repository.getAllSummits().first()
            val newSummit = summits.firstOrNull { it.activityId == newActivityId }
            if (newSummit == null || !newSummit.hasGpsTrack()) {
                Log.w(TAG, "No summit with track for activityId $newActivityId, skipping.")
                return 0
            }
            val segmentEntries = repository.getAllSegments().first()
                .flatMap { it.segmentEntries }
            val mountainPasses = repository.getAllMountainPasses().first()
            val templates = (segmentEntries + mountainPasses)
                .filter { it.activityId == matchedActivityId }
            if (templates.isEmpty()) {
                return 0
            }
            val existingEntries = (segmentEntries + mountainPasses)
                .filter { it.activityId == newActivityId }
                .toMutableList()
            var createdCount = 0
            for (template in templates) {
                if (SegmentEntryCreator.isAlreadyCreated(existingEntries, template)) {
                    continue
                }
                val entry = SegmentEntryCreator.createSegmentEntry(newSummit, template)
                if (entry != null) {
                    repository.saveSegmentEntry(entry)
                    existingEntries.add(entry)
                    createdCount++
                    Log.i(
                        TAG,
                        "Created segment entry for activity $newActivityId from activity " +
                                "$matchedActivityId (segmentId ${template.segmentId}, " +
                                "mountain pass ${template.isMountainPass})."
                    )
                } else {
                    Log.i(
                        TAG,
                        "Skipped entry of activity $matchedActivityId (segmentId " +
                                "${template.segmentId}, mountain pass ${template.isMountainPass}): " +
                                "slice not found in the track of activity $newActivityId."
                    )
                }
            }
            createdCount
        } catch (e: Exception) {
            Log.e(TAG, "Creating segment entries for activity $newActivityId failed.", e)
            0
        }
    }

    companion object {
        private const val TAG = "SegmentEntryCreationWorker"
        private const val UNIQUE_WORK_NAME = "SegmentEntryCreationWork"
        private const val PAIR_SEPARATOR = ","
        const val KEY_ACTIVITY_ID_PAIRS = "activity_id_pairs"
        const val KEY_CREATED_COUNT = "created_segment_entries"

        /**
         * Enqueues background creation of segment entries and mountain passes
         * for the given pairs of new activity id and the id of the activity
         * whose info was adopted. Consecutive enqueues are appended so no
         * download's work is lost.
         */
        fun enqueue(context: Context, activityIdPairs: List<Pair<Long, Long>>) {
            if (activityIdPairs.isEmpty()) {
                return
            }
            val request = OneTimeWorkRequestBuilder<SegmentEntryCreationWorker>()
                .setInputData(
                    workDataOf(
                        KEY_ACTIVITY_ID_PAIRS to activityIdPairs
                            .map { "${it.first}$PAIR_SEPARATOR${it.second}" }
                            .toTypedArray()
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
