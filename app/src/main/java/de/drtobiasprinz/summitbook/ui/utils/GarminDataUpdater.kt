package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class GarminDataUpdater(
    val sharedPreferences: SharedPreferences,
    private val pythonExecutor: GarminPythonExecutor,
) {
    private var endDate: String = ""
    private var startDateForSync: String = ""
    private var startDate: String = ""
    private var activitiesAtBeginning: Int = 0
    private var activitiesAfterUpdate: Int = 0

    fun update() {
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

    private fun updateActivities() {
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

    private fun asyncDownloadActivities(
        pythonExecutor: GarminPythonExecutor?,
        startDate: String,
        endDate: String
    ) {
        try {
            MainActivity.activitiesDir?.let {
                pythonExecutor?.downloadActivitiesByDate(
                    it, startDate, endDate
                )
            }
        } catch (e: RuntimeException) {
            Log.e("AsyncDownloadActivities", e.message ?: "")
        }
    }

    fun onFinish(progressBar: ProgressBar, context: Context, applyOnUpdates: () -> Unit = { }) {
        progressBar.visibility = View.GONE
        val edit = sharedPreferences.edit()
        edit.putString(Keys.PREF_THIRD_PARTY_START_DATE, startDateForSync)
        edit.apply()
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
