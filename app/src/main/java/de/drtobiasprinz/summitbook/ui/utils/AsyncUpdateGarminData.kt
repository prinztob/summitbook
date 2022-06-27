package de.drtobiasprinz.summitbook.ui.utils

import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.SolarIntensity
import de.drtobiasprinz.summitbook.ui.dialog.ShowNewSummitsFromGarminDialog
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


class AsyncUpdateGarminData(val resultReceiver: FragmentResultReceiver) : AsyncTask<Uri, Int?, Void?>() {

    private var endDate: String = ""
    private var startDate: String = ""

    override fun doInBackground(vararg uri: Uri): Void? {
        startDate = resultReceiver.getSharedPreference().getString("garmin_start_date", null) ?: ""
        updateSolarIntensities()
        updateActivities()
        return null
    }

    private fun updateActivities() {
        if (resultReceiver.getPythonExecutor() != null && startDate != "") {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            endDate = current.format(formatter)
            @Suppress("DEPRECATION")
            ShowNewSummitsFromGarminDialog.Companion.AsyncDownloadActivities(resultReceiver.getSortFilterHelper().entries, resultReceiver.getAllActivitiesFromThirdParty(), resultReceiver.getPythonExecutor(), startDate, endDate, null).execute()
        }
    }

    private fun updateSolarIntensities() {
        val solarIntensities = resultReceiver.getSortFilterHelper().database.solarIntensityDao()?.getAll()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        if (startDate != "") {
            var deviceId: String? = null
            val datesBetween = getDatesBetween(startDate)
            solarIntensities?.filter { !it.isForWholeDay }?.forEach {
                try {
                    var deviceIdLocal = deviceId
                    if (deviceIdLocal == null) {
                        deviceIdLocal = resultReceiver.getPythonExecutor()?.getDeviceIdForSolarOutput()
                        deviceId = deviceIdLocal
                    }
                    if (deviceIdLocal != null) {
                        val solarIntensityJson = resultReceiver.getPythonExecutor()?.getSolarIntensityForDate(df.format(it.date), deviceIdLocal)
                        val solarIntensity = solarIntensityJson?.let { SolarIntensity.parseFromJson(it) }
                        if (solarIntensity != null) {
                            solarIntensity.entryId = it.entryId
                            resultReceiver.getSortFilterHelper().database.solarIntensityDao()?.update(solarIntensity)
                        }
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
                            deviceIdForLoop = resultReceiver.getPythonExecutor()?.getDeviceIdForSolarOutput()
                            deviceId = deviceIdForLoop
                        }
                        if (deviceIdForLoop != null) {
                            Log.i("Scheduler", "Check solar intensity for date ${df.format(dateToCheck)}.")
                            val solarIntensityJson = resultReceiver.getPythonExecutor()?.getSolarIntensityForDate(df.format(dateToCheck), deviceIdForLoop)
                            val solarIntensity = solarIntensityJson?.let { SolarIntensity.parseFromJson(it) }
                                    ?: SolarIntensity(0, dateToCheck, 0.0, 0.0, false)
                            val entryToUpdate = solarIntensities.firstOrNull { it.date == dateToCheck }
                            if (entryToUpdate != null) {
                                solarIntensity.entryId = entryToUpdate.entryId
                                resultReceiver.getSortFilterHelper().database.solarIntensityDao()?.update(solarIntensity)
                            } else {
                                resultReceiver.getSortFilterHelper().database.solarIntensityDao()?.add(solarIntensity)
                            }
                            solarIntensities.add(solarIntensity)
                        }
                    }
                } catch (e: RuntimeException) {
                    Log.e("AsyncUpdateGarminData", e.message ?: "")
                }
            }
        }
    }

    override fun onPostExecute(param: Void?) {
        resultReceiver.getProgressBar()?.visibility = View.GONE
        val edit = resultReceiver.getSharedPreference().edit()
        edit.putString("garmin_start_date", endDate)
        edit.apply()
        Log.i("AsyncUpdateGarminData", "Done.")
        Toast.makeText(resultReceiver.getContext(), resultReceiver.getContext().getString(R.string.update_done), Toast.LENGTH_SHORT).show()
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