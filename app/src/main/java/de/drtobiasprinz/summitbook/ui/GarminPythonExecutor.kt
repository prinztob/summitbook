package de.drtobiasprinz.summitbook.ui

import android.os.AsyncTask
import android.util.Log
import android.view.View
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.*
import de.drtobiasprinz.summitbook.ui.dialog.BaseDialog
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader.Companion.getTempGpsFilePath
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToLong

class GarminPythonExecutor(var pythonInstance: Python, val username: String, val password: String) {
    private lateinit var pythonModule: PyObject
    var client: PyObject? = null

    private fun login() {
        if (client == null) {
            if (!Python.isStarted()) {
                MainActivity.mainActivity?.let { AndroidPlatform(it) }?.let { Python.start(it) }
            }
            pythonModule = pythonInstance.getModule("start")
            Log.i("GarminPythonExecutor", "do login")
            val result = pythonModule.callAttr("get_authenticated_client", username, password)
            checkOutput(result)
            client = result
        }
    }

    fun getActivityJsonAtDate(dateAsString: String): List<Summit> {
        if (client == null) {
            login()
        }
        val result = pythonModule.callAttr("get_activity_json_for_date", client, dateAsString)
        checkOutput(result)
        val jsonResponse = JsonParser().parse(result.toString()) as JsonArray
        return getSummitsAtDate(jsonResponse)
    }

    fun downloadGpxFile(garminActivityId: String, downloadPath: String) {
        if (client == null) {
            login()
        }
        val result = pythonModule.callAttr("download_gpx", client, garminActivityId, downloadPath)
        checkOutput(result)
    }

    fun downloadTcxFile(garminActivityId: String, downloadPath: String) {
        if (client == null) {
            login()
        }
        val result = pythonModule.callAttr("download_tcx", client, garminActivityId, downloadPath)
        checkOutput(result)
    }

    fun downloadActivitiesByDate(activitiesDir: File, startDate: String, endDate: String) {
        if (client == null) {
            login()
        }
        if (!activitiesDir.exists()) {
            activitiesDir.mkdirs()
        }
        val result = pythonModule.callAttr("download_activities_by_date", client, activitiesDir.absolutePath, startDate, endDate)
        checkOutput(result)
    }

    fun downloadSpeedDataForActivity(activitiesDir: File, activityId: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule.callAttr("get_split_data", client, activityId, activitiesDir.absolutePath)
        checkOutput(result)
        return JsonParser().parse(result.toString()) as JsonObject
    }

    fun getMultiSportData(activityId: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule.callAttr("get_multi_sport_data", client, activityId)
        checkOutput(result)
        return JsonParser().parse(result.toString()) as JsonObject
    }

    fun getMultiSportPowerData(dateAsString: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule.callAttr("get_power_data", client, dateAsString)
        checkOutput(result)
        return JsonParser().parse(result.toString()) as JsonObject
    }

    private fun checkOutput(result: PyObject?) {
        if (result == null || result.toString() == "") {
            throw RuntimeException("Execution failed")
        }
        if (result.toString().startsWith("return code: 1")) {
            throw RuntimeException(result.toString().replace("return code: 1", ""))
        }
    }

    companion object {

        class AsyncDownloadGpxViaPython(garminPythonExecutor: GarminPythonExecutor, entries: List<Summit>, private val sortFilterHelper: SortFilterHelper, useTcx: Boolean = false, private val dialog: BaseDialog, private val index: Int = -1) : AsyncTask<Void?, Void?, Void?>() {
            private val downloader = GarminTrackAndDataDownloader(entries, garminPythonExecutor, useTcx)
            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    downloader.downloadTracks()
                } catch (e: RuntimeException) {
                    Log.e("AsyncDownloadGpxViaPython.doInBackground", e.message ?: "")
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                try {
                    downloader.extractFinalSummit()
                    if (dialog.isStepByStepDownload()) {
                        val activityId = downloader.finalEntry?.garminData?.activityId
                        if (activityId != null) {
                            downloader.composeFinalTrack(getTempGpsFilePath(activityId).toFile())
                        }
                    } else {
                        downloader.composeFinalTrack()
                        downloader.updateFinalEntry(sortFilterHelper)
                    }
                    if (index != -1) {
                        dialog.doInPostExecute(index, downloader.downloadedTracks.none { !it.exists() })
                    }
                } catch (e: RuntimeException) {
                    Log.e("AsyncDownloadGpxViaPython", e.message ?: "")
                } finally {
                    SummitViewFragment.updateNewSummits(SummitViewFragment.activitiesDir, sortFilterHelper.entries, dialog.getDialogContext())
                    val progressBar = dialog.getProgressBarForAsyncTask()
                    if (progressBar != null) {
                        progressBar.visibility = View.GONE
                        progressBar.tooltipText = ""
                    }
                }
            }
        }

        fun getSummitsAtDate(activities: JsonArray): List<Summit> {
            val entries = ArrayList<Summit>()
            for (i in 0 until activities.size()) {
                val row = activities[i] as JsonObject
                try {
                    entries.add(parseJsonObject(row))
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            }
            return entries
        }

        fun getAllDownloadedSummitsFromGarmin(directory: File): MutableList<Summit> {
            val entries = mutableListOf<Summit>()
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files?.isNotEmpty() == true) {
                    files.forEach {
                        if (it.name.startsWith("activity_") && !it.name.endsWith("_splits.json")) {
                            try {
                                val gson = JsonParser().parse(it.readText()) as JsonObject
                                entries.add(parseJsonObject(gson))
                            } catch (ex: IllegalArgumentException) {
                                it.delete()
                            }
                        }
                    }
                }
            }
            return entries
        }

        @Throws(ParseException::class)
        fun parseJsonObject(jsonObject: JsonObject): Summit {
            val date = SimpleDateFormat(Summit.DATETIME_FORMAT, Locale.ENGLISH)
                    .parse(jsonObject.getAsJsonPrimitive("startTimeLocal").asString) ?: Date()
            val duration: Double = if (jsonObject["movingDuration"] != JsonNull.INSTANCE) jsonObject["movingDuration"].asDouble else jsonObject["duration"].asDouble
            val averageSpeed = convertMphToKmh(jsonObject["distance"].asDouble / duration)
            val activityIds: MutableList<String> = mutableListOf(jsonObject["activityId"].asString)
            if (jsonObject.has("childIds")) {
                activityIds.addAll(jsonObject["childIds"].asJsonArray.map { it.asString })
            }
            val garminData = GarminData(
                    activityIds,
                    getJsonObjectEntryNotNull(jsonObject, "calories"),
                    getJsonObjectEntryNotNull(jsonObject, "averageHR"),
                    getJsonObjectEntryNotNull(jsonObject, "maxHR"),
                    getPower(jsonObject),
                    getJsonObjectEntryNotNull(jsonObject, "maxFtp").toInt(),
                    getJsonObjectEntryNotNull(jsonObject, "vO2MaxValue").toInt(),
                    getJsonObjectEntryNotNull(jsonObject, "aerobicTrainingEffect"),
                    getJsonObjectEntryNotNull(jsonObject, "anaerobicTrainingEffect"),
                    getJsonObjectEntryNotNull(jsonObject, "grit"),
                    getJsonObjectEntryNotNull(jsonObject, "avgFlow"),
                    getJsonObjectEntryNotNull(jsonObject, "activityTrainingLoad")
            )
            return Summit(date,
                    jsonObject["activityName"].asString,
                    parseSportType(jsonObject["activityType"].asJsonObject),
                    emptyList(), emptyList(), "",
                    ElevationData.parse(if (jsonObject["maxElevation"] != JsonNull.INSTANCE) round(jsonObject["maxElevation"].asDouble, 2).toInt() else 0,
                            getJsonObjectEntryNotNull(jsonObject, "elevationGain").toInt()),
                    round(convertMeterToKm(getJsonObjectEntryNotNull(jsonObject, "distance").toDouble()), 2),
                    VelocityData.parse(round(averageSpeed, 2),
                        if (jsonObject["maxSpeed"] != JsonNull.INSTANCE) round(convertMphToKmh(jsonObject["maxSpeed"].asDouble), 2) else 0.0),
                    null, null,
                    emptyList(),
                    false,
                    mutableListOf(),
                    garminData,
                    null
            )
        }


        private fun getPower(jsonObject: JsonObject) =
                PowerData(
                        getJsonObjectEntryNotNull(jsonObject, "avgPower"),
                        getJsonObjectEntryNotNull(jsonObject, "maxPower"),
                        getJsonObjectEntryNotNull(jsonObject, "normPower"),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_1").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_2").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_5").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_10").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_20").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_30").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_60").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_120").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_300").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_600").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_1200").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_1800").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_3600").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_7200").toInt(),
                        getJsonObjectEntryNotNull(jsonObject, "maxAvgPower_18000").toInt()
                )

        private fun getJsonObjectEntryNotNull(jsonObject: JsonObject, key: String): Float {
            return if (jsonObject[key].isJsonNull) 0.0f else jsonObject[key].asFloat
        }

        private fun convertMphToKmh(mph: Double): Double {
            return mph * 3.6
        }

        private fun convertMeterToKm(meter: Double): Double {
            return meter / 1000.0
        }

        private fun parseSportType(jsonObject: JsonObject): SportType {
            return when (jsonObject["typeId"].asInt) {
                1 -> SportType.Running
                2 -> SportType.Bicycle
                5 -> SportType.Mountainbike
                25 -> SportType.IndoorTrainer
                89 -> SportType.BikeAndHike
                169 -> SportType.Skitour
                else -> SportType.Hike
            }
        }

        private fun round(value: Double, precision: Int): Double {
            val scale = 10.0.pow(precision.toDouble()).toInt()
            return (value * scale).roundToLong().toDouble() / scale
        }
    }

}