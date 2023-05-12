package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SolarIntensity
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class GarminDataUpdater(
    val sharedPreferences: SharedPreferences,
    private val pythonExecutor: GarminPythonExecutor,
    val entries: List<Summit>?,
    private val solarIntensities: List<SolarIntensity>?,
    private val databaseViewModel: DatabaseViewModel?
) {
    private var endDate: String = ""
    private var startDate: String = ""

    fun update() {
        startDate = sharedPreferences.getString("garmin_start_date", null) ?: ""
        updateSolarIntensities()
        updateActivities()
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

    private fun updateSolarIntensities() {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        if (startDate != "") {
            var deviceId: String? = null
            val datesBetween = getDatesBetween(startDate)
            solarIntensities?.filter { !it.isForWholeDay }?.forEach {
                try {
                    var deviceIdLocal = deviceId
                    if (deviceIdLocal == null) {
                        deviceIdLocal =
                            pythonExecutor.getDeviceIdForSolarOutput()
                        deviceId = deviceIdLocal
                    }
                    val solarIntensityJson = pythonExecutor
                        .getSolarIntensityForDate(df.format(it.date), deviceIdLocal)
                    val solarIntensity =
                        solarIntensityJson.let { jsonObject ->
                            SolarIntensity.parseFromJson(
                                jsonObject
                            )
                        }
                    if (solarIntensity != null) {
                        solarIntensity.entryId = it.entryId
                        databaseViewModel?.saveSolarIntensity(true, solarIntensity)
                    }
                } catch (e: RuntimeException) {
                    Log.e("AsyncUpdateGarminData", e.message ?: "")
                }
            }
            for (dateToCheck in datesBetween) {
                try {
                    if (dateToCheck != null && solarIntensities?.none { it.date == dateToCheck } == true) {
                        var deviceIdForLoop = deviceId
                        if (deviceIdForLoop == null) {
                            deviceIdForLoop = pythonExecutor.getDeviceIdForSolarOutput()
                            deviceId = deviceIdForLoop
                        }
                        Log.i(
                            "Scheduler",
                            "Check solar intensity for date ${df.format(dateToCheck)}."
                        )
                        val solarIntensityJson = pythonExecutor.getSolarIntensityForDate(
                            df.format(dateToCheck),
                            deviceIdForLoop
                        )
                        val solarIntensity =
                            solarIntensityJson.let { SolarIntensity.parseFromJson(it) }
                                ?: SolarIntensity(0, dateToCheck, 0.0, 0.0, false)
                        val entryToUpdate =
                            solarIntensities.firstOrNull { it.date == dateToCheck }
                        if (entryToUpdate != null) {
                            solarIntensity.entryId = entryToUpdate.entryId
                            databaseViewModel?.saveSolarIntensity(true, solarIntensity)
                        } else {
                            databaseViewModel?.saveSolarIntensity(false, solarIntensity)
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