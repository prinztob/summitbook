package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.DailyReportData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GarminDataUpdater(
    val sharedPreferences: SharedPreferences,
    private val pythonExecutor: GarminPythonExecutor,
    val entries: List<Summit>?,
    private val dailyReportData: List<DailyReportData>?,
    private val databaseViewModel: DatabaseViewModel?
) {
    private var endDate: String = ""
    private var startDate: String = ""

    fun update() {
        startDate = sharedPreferences.getString("garmin_start_date", null) ?: ""
        try {
            updateDailyReportData()
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

    private fun updateDailyReportData() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -3)
        val filterEntriesOldThan = calendar.time
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        if (startDate != "") {
            val datesBetween = getDatesBetween(startDate)
            dailyReportData?.filter { !it.isForWholeDay && it.date.after(filterEntriesOldThan) }
                ?.forEach {
                    try {
                        val dailyEventsJsonArray =
                            pythonExecutor.getDailyEventsForDate(df.format(it.date))
                        val summaryData =
                            pythonExecutor.getSummaryData(df.format(it.date))
                        val hrvData =
                            pythonExecutor.getHearRateVariabilityData(df.format(it.date))
                        val reportData =
                            DailyReportData.parseFromJson(
                                it.date,
                                dailyEventsJsonArray,
                                summaryData,
                                hrvData
                            )
                        reportData.entryId = it.entryId
                        databaseViewModel?.saveDailyReportData(true, reportData)
                    } catch (e: RuntimeException) {
                        Log.e("AsyncUpdateGarminData", e.message ?: "")
                    }
                }
            for (dateToCheck in datesBetween) {
                try {
                    if (dateToCheck != null && dailyReportData?.none { it.date == dateToCheck } == true) {
                        Log.i(
                            "Scheduler",
                            "Check daily report data for date ${df.format(dateToCheck)}."
                        )
                        val dailyEventsJsonArray =
                            pythonExecutor.getDailyEventsForDate(df.format(dateToCheck))
                        val stepsData =
                            pythonExecutor.getSummaryData(df.format(dateToCheck))
                        val floorsClimbedData =
                            pythonExecutor.getHearRateVariabilityData(df.format(dateToCheck))
                        val reportData =
                            DailyReportData.parseFromJson(
                                dateToCheck,
                                dailyEventsJsonArray,
                                stepsData,
                                floorsClimbedData
                            )
                        val entryToUpdate =
                            dailyReportData.firstOrNull { it.date == dateToCheck }
                        if (entryToUpdate != null) {
                            reportData.entryId = entryToUpdate.entryId
                            databaseViewModel?.saveDailyReportData(true, reportData)
                        } else {
                            databaseViewModel?.saveDailyReportData(false, reportData)
                        }
                    }
                } catch (e: RuntimeException) {
                    Log.e("AsyncUpdateGarminData", e.message ?: "")
                }
            }
        }
    }

    fun onFinish(progressBar: ProgressBar, context: Context) {
        progressBar.visibility = View.GONE
        val edit = sharedPreferences.edit()
        edit.putString("garmin_start_date", endDate)
        edit.apply()
        Log.i("AsyncUpdateGarminData", "Done.")
        Toast.makeText(
            context,
            context.getString(R.string.update_done),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getDatesBetween(startDate: String, endDate: String? = null): List<Date?> {
        var start = LocalDate.parse(startDate)
        val end = if (endDate != null) LocalDate.parse(endDate) else LocalDate.now()
        val totalDates: MutableList<Date> = ArrayList()
        while (!start.isAfter(end)) {
            totalDates.add(Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            start = start.plusDays(1)
        }
        return totalDates
    }

}
