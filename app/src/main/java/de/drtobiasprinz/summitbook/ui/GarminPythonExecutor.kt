package de.drtobiasprinz.summitbook.ui

import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.db.entities.*
import de.drtobiasprinz.summitbook.db.entities.SportType.Companion.getSportTypeFromGarminId
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class GarminPythonExecutor(
    val username: String, val password: String
) {
    private var pythonModule: PyObject? = null
    private var client: PyObject? = null

    private fun login() {
        val storage = MainActivity.storage
        if (client == null) {
            if (Python.isStarted()) {
                pythonModule = pythonInstance?.getModule("entry_point")
                Log.i("GarminPythonExecutor", "do login")
                if (storage != null) {
                    val result =
                        pythonModule?.callAttr("init_api", username, password, storage.absolutePath)
                    checkOutput(result)
                    client = result
                }
            }
        }
    }

    fun getActivityJsonAtDate(dateAsString: String): List<Summit> {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("get_activity_json_for_date", client, dateAsString)
        checkOutput(result)
        val jsonResponse = JsonParser.parseString(result.toString()) as JsonArray
        return getSummitsAtDate(jsonResponse)
    }

    fun downloadGpxFile(garminActivityId: String, downloadPath: String) {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("download_gpx", client, garminActivityId, downloadPath)
        checkOutput(result)
    }

    fun downloadTcxFile(garminActivityId: String, downloadPath: String) {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("download_tcx", client, garminActivityId, downloadPath)
        checkOutput(result)
    }

    fun downloadActivitiesByDate(activitiesDir: File, startDate: String, endDate: String) {
        if (client == null) {
            login()
        }
        if (!activitiesDir.exists()) {
            activitiesDir.mkdirs()
        }
        val result = pythonModule?.callAttr(
            "download_activities_by_date", client, activitiesDir.absolutePath, startDate, endDate
        )
        checkOutput(result)
    }

    fun getDailyEventsForDate(date: String): JsonArray {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("get_daily_events", client, date)
        checkOutput(result)
        return JsonParser.parseString(result.toString()) as JsonArray
    }

    fun downloadSpeedDataForActivity(activityId: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr(
            "get_split_data", client, activityId, activitiesDir?.absolutePath
        )
        checkOutput(result)
        return JsonParser.parseString(result.toString()) as JsonObject
    }

    fun getExerciseSet(activityId: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("get_exercise_set", client, activityId)
        checkOutput(result)
        return JsonParser.parseString(result.toString()) as JsonObject
    }

    fun getMultiSportPowerData(dateAsString: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("get_power_data", client, dateAsString)
        checkOutput(result)
        return JsonParser.parseString(result.toString()) as JsonObject
    }

    private fun checkOutput(result: PyObject?) {
        if (result == null || result.toString() == "") {
            throw RuntimeException("Execution failed")
        }
        if (result.toString().startsWith("return code: 1")) {
            throw RuntimeException(result.toString().replace("return code: 1", ""))
        }
    }

    fun getSummaryData(startDate: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("get_user_summary", client, startDate)
        checkOutput(result)
        return JsonParser.parseString(result.toString()) as JsonObject
    }

    fun getHearRateVariabilityData(startDate: String): JsonObject {
        return try {
            if (client == null) {
                login()
            }
            val result = pythonModule?.callAttr("get_hrv", client, startDate)
            checkOutput(result)
            JsonParser.parseString(result.toString()) as JsonObject
        } catch (_: RuntimeException) {
            JsonObject()
        }
    }

    companion object {

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

        fun getAllDownloadedSummitsFromGarmin(directory: File?): MutableList<Summit> {
            val entries = mutableListOf<Summit>()
            if (directory != null && directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files?.isNotEmpty() == true) {
                    files.sortByDescending { it.absolutePath }
                    files.forEach {
                        if (
                            it.name.startsWith("activity_") &&
                            !it.name.endsWith("_splits.json") &&
                            !it.name.endsWith("_exercise_set.json")
                        ) {
                            try {
                                val gson = JsonParser.parseString(it.readText()) as JsonObject
                                entries.add(parseJsonObject(gson))
                            } catch (ex: IllegalArgumentException) {
                                Log.i(
                                    "GarminPythonExecutor",
                                    "Could not parse file ${it.absolutePath}"
                                )
                                it.delete()
                            }
                        }
                    }
                }
            }
            return entries
        }

        private fun roundToTwoDigits(value: Float): Float {
            return (value * 100f).roundToInt() / 100f
        }

        private fun roundToTwoDigits(value: Double): Double {
            return (value * 100.0).roundToInt() / 100.0
        }

        @Throws(ParseException::class)
        fun parseJsonObject(jsonObject: JsonObject): Summit {
            val date = SimpleDateFormat(
                Summit.DATETIME_FORMAT,
                Locale.ENGLISH
            ).parse(jsonObject.getAsJsonPrimitive("startTimeLocal").asString) ?: Date()
            val sportType = parseSportType(jsonObject["activityType"].asJsonObject)
            val duration: Double =
                if (jsonObject["movingDuration"] != JsonNull.INSTANCE && sportType in SportGroup.Bike.sportTypes) jsonObject["movingDuration"].asDouble else jsonObject["duration"].asDouble
            val averageSpeed =
                if (jsonObject["distance"] != JsonNull.INSTANCE && duration != 0.0) convertMphToKmh(
                    jsonObject["distance"].asDouble / duration
                ) else 0.0
            val activityIds: MutableList<String> = mutableListOf(jsonObject["activityId"].asString)
            if (jsonObject.has("childIds")) {
                activityIds.addAll(jsonObject["childIds"].asJsonArray.map { it.asString })
            }
            val vo2max = if (jsonObject.has("vo2MaxPreciseValue")) {
                roundToTwoDigits(getJsonObjectEntryNotNull(jsonObject, "vo2MaxPreciseValue"))
            } else {
                roundToTwoDigits(getJsonObjectEntryNotNull(jsonObject, "vO2MaxValue"))
            }
            val garminData = GarminData(
                activityIds,
                getJsonObjectEntryNotNull(jsonObject, "calories"),
                getJsonObjectEntryNotNull(jsonObject, "averageHR"),
                getJsonObjectEntryNotNull(jsonObject, "maxHR"),
                getPower(jsonObject),
                getFtp(activityIds),
                vo2max,
                getJsonObjectEntryNotNull(jsonObject, "aerobicTrainingEffect"),
                getJsonObjectEntryNotNull(jsonObject, "anaerobicTrainingEffect"),
                getJsonObjectEntryNotNull(jsonObject, "grit"),
                getJsonObjectEntryNotNull(jsonObject, "avgFlow"),
                getJsonObjectEntryNotNull(jsonObject, "activityTrainingLoad")
            )

            return Summit(
                date,
                jsonObject["activityName"].asString,
                sportType,
                emptyList(),
                emptyList(),
                "",
                ElevationData.parse(
                    if (jsonObject["maxElevation"] != JsonNull.INSTANCE) round(
                        jsonObject["maxElevation"].asDouble
                    ).toInt() else 0, getJsonObjectEntryNotNull(jsonObject, "elevationGain").toInt()
                ),
                roundToTwoDigits(
                    convertMeterToKm(
                        getJsonObjectEntryNotNull(
                            jsonObject, "distance"
                        ).toDouble()
                    )
                ),
                VelocityData.parse(
                    round(averageSpeed), if (jsonObject["maxSpeed"] != JsonNull.INSTANCE) round(
                        convertMphToKmh(
                            jsonObject["maxSpeed"].asDouble
                        )
                    ) else 0.0
                ),
                null,
                null,
                emptyList(),
                emptyList(),
                isFavorite = false,
                isPeak = false,
                imageIds = mutableListOf(),
                garminData = garminData,
                trackBoundingBox = null
            )
        }

        private fun getFtp(activityIds: MutableList<String>): Int {
            var ftp = 0
            val exerciseSet =
                File(activitiesDir, "activity_${activityIds[0]}_exercise_set.json")
            if (exerciseSet.exists()) {
                val gsonExerciseSet =
                    JsonParser.parseString(exerciseSet.readText()) as JsonObject
                if (gsonExerciseSet.has("summaryDTO")) {
                    val summaryDTO =
                        gsonExerciseSet.getAsJsonObject("summaryDTO")
                    if (summaryDTO.has("functionalThresholdPower")) {
                        ftp = summaryDTO.getAsJsonPrimitive("functionalThresholdPower").asDouble.toInt()
                    }
                }
                Log.i("PythonExecutor", "FTP: ${ftp}")
            }
            return ftp
        }


        private fun getPower(jsonObject: JsonObject) = PowerData(
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
            return getSportTypeFromGarminId(jsonObject["typeId"].asInt)
        }

        private fun round(value: Double): Double {
            val precision = 2
            val scale = 10.0.pow(precision.toDouble()).toInt()
            return (value * scale).roundToLong().toDouble() / scale
        }
    }

}