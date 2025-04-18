package de.drtobiasprinz.summitbook.ui

import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.SportGroup
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.SportType.Companion.getSportTypeFromGarminId
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.DATETIME_FORMAT_COMPLEX
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.system.measureTimeMillis

class GarminPythonExecutor(
    val username: String, val password: String
) {
    private var pythonModule: PyObject? = null
    private var client: PyObject? = null

    private fun login() {
        val time = measureTimeMillis {
            val storage = MainActivity.storage
            if (client == null) {
                if (Python.isStarted()) {
                    pythonModule = pythonInstance?.getModule("entry_point")
                    Log.i(TAG, "do login")
                    if (storage != null) {
                        val result =
                            pythonModule?.callAttr(
                                "init_api",
                                username,
                                password,
                                storage.absolutePath
                            )
                        checkOutput(result)
                        client = result
                    }
                }
            }
        }
        Log.i(TAG, "Login took $time")
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

    fun downloadTcxFile(
        garminActivityId: String,
        downloadPathTcx: String,
        downloadPathGpx: String
    ) {
        val time = measureTimeMillis {
            if (client == null) {
                login()
            }
            val result = pythonModule?.callAttr(
                Keys.PREF_DOWNLOAD_TCX,
                client,
                garminActivityId,
                downloadPathTcx,
                downloadPathGpx
            )
            checkOutput(result)
        }
        Log.i(TAG, "downloadTcxFile took $time")
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

    fun getExerciseSet(activityId: String): JsonObject {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr(
            "get_exercise_set",
            client,
            activityId,
            activitiesDir?.absolutePath
        )
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

    companion object {

        const val TAG = "GarminPythonExecutor"

        fun getSummitsAtDate(activities: JsonArray): List<Summit> {
            val entries = ArrayList<Summit>()
            for (i in 0 until activities.size()) {
                val row = activities[i] as JsonObject
                try {
                    entries.add(parseJsonObjectFromParentActivity(row))
                } catch (e: ParseException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            }
            return entries
        }

        fun getAllDownloadedSummitsFromGarmin(
            directory: File?,
            activityIdsInSummitBook: List<String> = emptyList(),
            activitiesIdIgnored: List<String> = emptyList()
        ): MutableList<Summit> {
            val entries = mutableListOf<Summit>()
            if (directory != null && directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                if (files?.isNotEmpty() == true) {
                    files.sortByDescending { it.absolutePath }
                    files.forEach {
                        if (
                            (it.name.startsWith("activity_") || it.name.startsWith("child_")) &&
                            !it.name.endsWith("_splits.json") &&
                            !it.name.endsWith("_exercise_set.json")
                        ) {
                            try {
                                val gson = JsonParser.parseString(it.readText()) as JsonObject
                                val entry = if (it.name.startsWith("child_")) {
                                    val parentId = gson.getAsJsonPrimitive("parentId").toString()
                                    val gsonParentFile =
                                        File(it.parentFile, "activity_${parentId}.json")
                                    var gsonParent: JsonObject? = null
                                    if (gsonParentFile.exists()) {
                                        gsonParent = JsonParser.parseString(
                                            gsonParentFile.readText()
                                        ) as JsonObject
                                    }
                                    parseJsonObjectFromChildActivity(gson, gsonParent)
                                } else {
                                    parseJsonObjectFromParentActivity(gson)
                                }
                                if (entry.garminData?.activityId !in activityIdsInSummitBook && entry.garminData?.activityId !in activitiesIdIgnored) {
                                    entries.add(entry)
                                    if (entries.size > 20) {
                                        return entries
                                    }
                                }
                            } catch (ex: IllegalArgumentException) {
                                Log.i(TAG, "Could not parse file ${it.absolutePath}")
                            } catch (ex: NullPointerException) {
                                Log.i(TAG, "Could not parse file ${it.absolutePath}")
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

        fun parseJsonObjectFromParentActivity(jsonObject: JsonObject): Summit {
            val date = SimpleDateFormat(
                Summit.DATETIME_FORMAT_SIMPLE,
                Locale.ENGLISH
            ).parse(jsonObject.getAsJsonPrimitive("startTimeLocal").asString) ?: Date()
            val sportType = parseSportType(jsonObject["activityType"].asJsonObject)
            val duration: Double =
                if (jsonObject["movingDuration"] != JsonNull.INSTANCE && sportType in SportGroup.OnABicycle.sportTypes) jsonObject["movingDuration"].asDouble else jsonObject["duration"].asDouble
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
                getFtp(activityIds),
                getVo2max(jsonObject),
                getJsonObjectEntryNotNull(jsonObject, "aerobicTrainingEffect"),
                getJsonObjectEntryNotNull(jsonObject, "anaerobicTrainingEffect"),
                getJsonObjectEntryNotNull(jsonObject, "grit"),
                getJsonObjectEntryNotNull(jsonObject, "avgFlow"),
                getJsonObjectEntryNotNull(jsonObject, "activityTrainingLoad"),
            )
            val elevationData = try {
                ElevationData.parse(
                    getJsonObjectEntryNotNull(jsonObject, "maxElevation").toInt(),
                    getJsonObjectEntryNotNull(jsonObject, "elevationGain").toInt()
                )
            } catch (_: NullPointerException) {
                ElevationData(0, 0)
            }
            val velocityData = try {
                VelocityData.parse(
                    if (jsonObject["maxSpeed"] != JsonNull.INSTANCE) round(
                        convertMphToKmh(
                            getJsonObjectEntryNotNull(jsonObject, "maxSpeed").toDouble()
                        )
                    ) else 0.0
                )
            } catch (_: NullPointerException) {
                VelocityData(0.0, 0.0)
            }
            return Summit(
                date,
                jsonObject["activityName"].asString,
                sportType,
                emptyList(),
                emptyList(),
                "",
                elevationData,
                roundToTwoDigits(
                    convertMeterToKm(
                        getJsonObjectEntryNotNull(
                            jsonObject, "distance"
                        ).toDouble()
                    )
                ),
                velocityData,
                null,
                null,
                emptyList(),
                emptyList(),
                isFavorite = false,
                isPeak = false,
                imageIds = mutableListOf(),
                garminData = garminData,
                trackBoundingBox = null,
                duration = duration.toInt()
            )
        }

        fun parseJsonObjectFromChildActivity(json: JsonObject, jsonParent: JsonObject?): Summit {
            val summaryDTO = json.getAsJsonObject("summaryDTO")
            val date = SimpleDateFormat(
                DATETIME_FORMAT_COMPLEX,
                Locale.ENGLISH
            ).parse(summaryDTO.getAsJsonPrimitive("startTimeLocal").asString) ?: Date()
            val sportType = parseSportType(json["activityTypeDTO"].asJsonObject)
            val duration: Double =
                if (summaryDTO["movingDuration"] != JsonNull.INSTANCE && sportType in SportGroup.OnABicycle.sportTypes) summaryDTO["movingDuration"].asDouble else summaryDTO["duration"].asDouble
            val activityIds: MutableList<String> = mutableListOf(json["activityId"].asString)
            val garminData = GarminData(
                activityIds,
                getJsonObjectEntryNotNull(summaryDTO, "calories"),
                getJsonObjectEntryNotNull(summaryDTO, "averageHR"),
                getJsonObjectEntryNotNull(summaryDTO, "maxHR"),
                if (getJsonObjectEntryNotNull(summaryDTO, "averagePower") > 0) {
                    getPower(jsonParent ?: json)
                } else {
                    PowerData(0f, 0f, 0f)
                },
                getFtp(activityIds),
                getVo2max(jsonParent ?: json),
                getJsonObjectEntryNotNull(summaryDTO, "aerobicTrainingEffect"),
                getJsonObjectEntryNotNull(summaryDTO, "anaerobicTrainingEffect"),
                getJsonObjectEntryNotNull(summaryDTO, "grit"),
                getJsonObjectEntryNotNull(summaryDTO, "avgFlow"),
                getJsonObjectEntryNotNull(summaryDTO, "activityTrainingLoad"),
            )
            val elevationData = try {
                ElevationData.parse(
                    getJsonObjectEntryNotNull(summaryDTO, "maxElevation").toInt(),
                    getJsonObjectEntryNotNull(summaryDTO, "elevationGain").toInt()
                )
            } catch (_: NullPointerException) {
                ElevationData(0, 0)
            }
            val velocityData = try {
                VelocityData.parse(
                    if (summaryDTO["maxSpeed"] != JsonNull.INSTANCE) round(
                        convertMphToKmh(
                            getJsonObjectEntryNotNull(summaryDTO, "maxSpeed").toDouble()
                        )
                    ) else 0.0
                )
            } catch (_: NullPointerException) {
                VelocityData(0.0, 0.0)
            }
            return Summit(
                date,
                json["activityName"].asString,
                sportType,
                emptyList(),
                emptyList(),
                "",
                elevationData,
                roundToTwoDigits(
                    convertMeterToKm(
                        getJsonObjectEntryNotNull(
                            summaryDTO, "distance"
                        ).toDouble()
                    )
                ),
                velocityData,
                null,
                null,
                emptyList(),
                emptyList(),
                isFavorite = false,
                isPeak = false,
                imageIds = mutableListOf(),
                garminData = garminData,
                trackBoundingBox = null,
                duration = duration.toInt()
            )
        }

        private fun getVo2max(json: JsonObject): Float {
            return if (json.has("vo2MaxPreciseValue")) {
                roundToTwoDigits(getJsonObjectEntryNotNull(json, "vo2MaxPreciseValue"))
            } else if (json.has("vO2MaxValue")) {
                roundToTwoDigits(getJsonObjectEntryNotNull(json, "vO2MaxValue"))
            } else {
                0.0f
            }
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
                        ftp =
                            summaryDTO.getAsJsonPrimitive("functionalThresholdPower").asDouble.toInt()
                    }
                }
                Log.d(TAG, "FTP: $ftp")
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
            return if (jsonObject.has(key)) {
                if (jsonObject[key].isJsonNull) 0.0f else jsonObject[key].asFloat
            } else {
                0f
            }
        }

        private fun convertMphToKmh(mph: Double): Double {
            return BigDecimal(mph * 3.6).setScale(2, RoundingMode.HALF_UP).toDouble()
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