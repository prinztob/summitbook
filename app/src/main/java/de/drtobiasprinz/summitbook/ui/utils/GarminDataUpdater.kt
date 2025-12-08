package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.DailyActivitySummary
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GarminDataUpdater(
    val sharedPreferences: SharedPreferences,
    private val pythonExecutor: GarminPythonExecutor,
    private val repository: DatabaseRepository,
    private val viewModel: DatabaseViewModel,
) {
    private var endDate: String = ""
    private var startDateForSync: String = ""
    private var startDate: String = ""
    private var activitiesAtBeginning: Int = 0
    private var activitiesAfterUpdate: Int = 0

    suspend fun update() {
        startDate = sharedPreferences.getString(Keys.PREF_THIRD_PARTY_START_DATE, null) ?: ""
        activitiesAtBeginning = MainActivity.activitiesDir?.listFiles()?.size ?: 0
        try {
            updateActivities()
        } catch (ex: RuntimeException) {
            Log.e(
                "GarminDataUpdater",
                "Error in updating activities and daily report data: ${ex.message}. Please try later"
            )
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
                    viewModel.saveActivitySummary(activitySummary)
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
        try {
            MainActivity.activitiesDir?.let {
                val newActivities = pythonExecutor?.downloadActivitiesByDate(
                    it, startDate, endDate
                )
                newActivities?.let { activityAggregationSummary ->
                    updateDailyActivitySummary(
                        activityAggregationSummary,
                    )
                }
            }
        } catch (e: RuntimeException) {
            Log.e("AsyncDownloadActivities", e.message ?: "")
        }
    }

    fun onFinish(progressBar: ProgressBar, context: Context, applyOnUpdates: () -> Unit = { }) {
        progressBar.visibility = View.GONE
        sharedPreferences.edit {
            putString(Keys.PREF_THIRD_PARTY_START_DATE, startDateForSync)
        }
        Log.i("AsyncUpdateGarminData", "Done.")
        activitiesAfterUpdate = MainActivity.activitiesDir?.listFiles()?.size ?: 0
        if (hasUpdates()) {
            Toast.makeText(
                context,
                context.getString(R.string.update_done_new_summits),
                Toast.LENGTH_LONG
            ).show()
            applyOnUpdates()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.update_done),
                Toast.LENGTH_LONG
            ).show()
        }


    }

    private fun hasUpdates(): Boolean {
        return activitiesAfterUpdate > activitiesAtBeginning
    }

}
