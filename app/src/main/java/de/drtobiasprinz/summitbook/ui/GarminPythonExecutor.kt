package de.drtobiasprinz.summitbook.ui

import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.models.GarminActivityData
import de.drtobiasprinz.summitbook.models.PowerData
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.SummitEntry
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToLong

class GarminPythonExecutor(val username: String, val password: String) {
    lateinit var pythonInstance: Python
    lateinit var pythonModule: PyObject
    var client: PyObject? = null
    fun login() {
        if (client == null) {
            if (!Python.isStarted()) {
                MainActivity.mainActivity?.let { AndroidPlatform(it) }?.let { Python.start(it) }
            }
            pythonInstance = Python.getInstance()
            pythonModule = pythonInstance.getModule("start")
            Log.i("GarminPythonExecutor", "do login")
            val result = pythonModule.callAttr("get_authenticated_client", username, password)
            checkOutput(result)
            client = result
        }
    }

    fun getActivityJsonAtDate(dateAsString: String): JsonArray {
        if (client == null) {
            login()
        }
        val result = pythonModule.callAttr("get_activity_json_for_date", client, dateAsString)
        checkOutput(result)
        return JsonParser().parse(result.toString()) as JsonArray
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

        fun getSummitsAtDate(activities: JsonArray): ArrayList<SummitEntry> {
            val entries = ArrayList<SummitEntry>()
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

        fun getAllDownloadedSummitsFromGarmin(directory: File): MutableList<SummitEntry> {
            val entries = mutableListOf<SummitEntry>()
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files?.isNotEmpty() == true) {
                    files.forEach {
                        if (it.name.startsWith("activity_")) {
                            val gson = JsonParser().parse(it.readText()) as JsonObject
                            entries.add(parseJsonObject(gson))
                        }
                    }
                }
            }
            return entries
        }

        @Throws(ParseException::class)
        private fun parseJsonObject(jsonObject: JsonObject): SummitEntry {
            val date = SimpleDateFormat(SummitEntry.DATETIME_FORMAT, Locale.ENGLISH)
                    .parse(jsonObject.getAsJsonPrimitive("startTimeLocal").asString) ?: Date()
            val duration: Double = if (jsonObject["movingDuration"] != JsonNull.INSTANCE) jsonObject["movingDuration"].asDouble else jsonObject["duration"].asDouble
            val averageSpeed = convertMphToKmh(jsonObject["distance"].asDouble / duration)
            val entry = SummitEntry(date,
                    jsonObject["activityName"].asString,
                    parseSportType(jsonObject["activityType"].asJsonObject),
                    emptyList(), emptyList(), "",
                    getJsonObjectEntryNotNull(jsonObject, "elevationGain").toInt(),
                    round(convertMeterToKm(getJsonObjectEntryNotNull(jsonObject, "distance").toDouble()), 2),
                    round(averageSpeed, 2),
                    if (jsonObject["maxSpeed"] != JsonNull.INSTANCE) round(convertMphToKmh(jsonObject["maxSpeed"].asDouble), 2) else 0.0,
                    if (jsonObject["maxElevation"] != JsonNull.INSTANCE) round(jsonObject["maxElevation"].asDouble, 2).toInt() else 0,
                    emptyList(),
                    mutableListOf()
            )
            val activityIds: MutableList<String> = mutableListOf(jsonObject["activityId"].asString)
            if (jsonObject.has("childIds")) {
                activityIds.addAll(jsonObject["childIds"].asJsonArray.map { it.asString })
            }
            val activityData = GarminActivityData(
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
            entry.activityData = activityData
            return entry
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