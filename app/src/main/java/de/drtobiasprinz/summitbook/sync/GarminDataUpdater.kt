package de.drtobiasprinz.summitbook.sync

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.data.appstate.AppState
import de.drtobiasprinz.summitbook.data.db.entities.DailyActivitySummary
import de.drtobiasprinz.summitbook.data.repository.DatabaseRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class GarminSyncResult {
    data class Success(val hasNewActivities: Boolean) : GarminSyncResult()
    data class Failed(val message: String?) : GarminSyncResult()
}

class GarminDataUpdater(
    val sharedPreferences: SharedPreferences,
    private val pythonExecutor: GarminPythonExecutor,
    private val repository: DatabaseRepository,
) {
    private var endDate: String = ""
    private var startDateForSync: String = ""
    private var startDate: String = ""
    private var activitiesAtBeginning: Int = 0
    private var activitiesAfterUpdate: Int = 0

    suspend fun update(): GarminSyncResult {
        startDate = sharedPreferences.getString(Keys.PREF_THIRD_PARTY_START_DATE, null) ?: ""
        activitiesAtBeginning = AppState.activitiesDir?.listFiles()?.size ?: 0
        return try {
            updateActivities()
            activitiesAfterUpdate = AppState.activitiesDir?.listFiles()?.size ?: 0
            GarminSyncResult.Success(hasUpdates())
        } catch (ex: RuntimeException) {
            Log.e(
                "GarminDataUpdater",
                "Error in updating activities and daily report data: ${ex.message}. Please try later",
                ex
            )
            GarminSyncResult.Failed(ex.message)
        }
    }

    /** Persists the new sync window. Only call this after a [GarminSyncResult.Success],
     * otherwise failed days would be skipped in subsequent syncs. */
    fun persistSyncWindow() {
        sharedPreferences.edit {
            putString(Keys.PREF_THIRD_PARTY_START_DATE, startDateForSync)
        }
    }

    private suspend fun updateDailyActivitySummary(
        activityAggregationSummary: String,
    ) {
        try {
            val gson = Gson()
            val listType = object : TypeToken<List<DailyActivitySummary>>() {}.type
            val dailyActivitySummaries: List<DailyActivitySummary> =
                gson.fromJson(activityAggregationSummary, listType)

            var insertedCount = 0
            var skippedCount = 0

            dailyActivitySummaries.forEach { activitySummary ->
                val existingActivitySummary =
                    repository.getDailyActivitySummaryByDateSync(activitySummary.activityId)
                if (existingActivitySummary != null) {
                    skippedCount++
                } else {
                    repository.saveDailyActivitySummary(activitySummary)
                    insertedCount++
                }
            }
            Log.i(
                "GarminDataUpdater",
                "Daily activity summary: $insertedCount inserted, and $skippedCount skipped"
            )
        } catch (e: Exception) {
            Log.e("GarminDataUpdater", "Failed to parse or save activity summary: ${e.message}")
        }
    }

    private suspend fun updateActivities() {
        if (startDate != "") {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            endDate = current.format(formatter)
            startDateForSync = (current.minusDays(1)).format(formatter)
            asyncDownloadActivities(
                pythonExecutor,
                startDate,
                endDate,
            )
        }
    }

    private suspend fun asyncDownloadActivities(
        pythonExecutor: GarminPythonExecutor?,
        startDate: String,
        endDate: String,
    ) {
        AppState.activitiesDir?.let {
            val newActivities = pythonExecutor?.downloadActivitiesByDate(
                it, startDate, endDate
            )
            newActivities?.let { activityAggregationSummary ->
                updateDailyActivitySummary(
                    activityAggregationSummary,
                )
            }
        }
    }

    private fun hasUpdates(): Boolean {
        return activitiesAfterUpdate > activitiesAtBeginning
    }

}
