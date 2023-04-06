package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.SolarIntensity
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.dialog.ShowNewSummitsFromGarminDialog
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class AsyncUpdateGarminData(
    val sharedPreferences: SharedPreferences,
    private val pythonExecutor: GarminPythonExecutor,
    val database: AppDatabase,
    val entries: List<Summit>,
    val context: Context,
    val progressBar: ProgressBar
) : AsyncTask<Uri, Int?, Void?>() {

    private var endDate: String = ""
    private var startDate: String = ""

    override fun doInBackground(vararg uri: Uri): Void? {
        startDate = sharedPreferences.getString("garmin_start_date", null) ?: ""
        updateSolarIntensities()
        updateActivities()
        return null
    }

    private fun updateActivities() {
        if (startDate != "") {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            endDate = current.format(formatter)
            @Suppress("DEPRECATION")
            ShowNewSummitsFromGarminDialog.Companion.AsyncDownloadActivities(
                pythonExecutor,
                startDate,
                endDate,
                null
            ).execute()
        }
    }

    private fun updateSolarIntensities() {
        val solarIntensities = database.solarIntensityDao()?.getAll()
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
                        solarIntensityJson.let { SolarIntensity.parseFromJson(it) }
                    if (solarIntensity != null) {
                        solarIntensity.entryId = it.entryId
                        database.solarIntensityDao()?.update(solarIntensity)
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
                            database.solarIntensityDao()?.update(solarIntensity)
                        } else {
                            database.solarIntensityDao()?.add(solarIntensity)
                        }
                        solarIntensities.add(solarIntensity)
                    }
                } catch (e: RuntimeException) {
                    Log.e("AsyncUpdateGarminData", e.message ?: "")
                }
            }
        }
    }

    override fun onPostExecute(param: Void?) {
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