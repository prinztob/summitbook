package de.drtobiasprinz.summitbook.ui

import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonInstance
import java.io.File
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class GarminPythonExecutor(
    val username: String, val password: String
) {
    private var pythonModule: PyObject? = null
    private var client: PyObject? = null

    private fun login() {
        val time = measureTimeMillis {
            val storage = MainActivityCompose.storage
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

    fun getVo2MaxAtDate(dateAsString: String): Float {
        if (client == null) {
            login()
        }
        val result = pythonModule?.callAttr("get_vo2max", client, dateAsString)
        Log.i(TAG, "vo2max result $result")
        checkOutput(result)
        return try {
            result?.toFloat() ?: 0f
        } catch (_: Exception) {
            0f
        }
    }

    fun downloadGpxFile(garminActivityId: String, downloadPath: String) {
        val time = measureTimeMillis {
            if (client == null) {
                login()
            }
            val result = pythonModule?.callAttr("download_gpx", client, garminActivityId, downloadPath)
            checkOutput(result)
        }
        Log.i(TAG, "downloadGpxFile took $time")
    }

    fun downloadTcxFile(
        garminActivityId: String,
        downloadPathTcx: String,
        downloadPathGpx: String,
        downloadPathYaml: String
    ) {
        val time = measureTimeMillis {
            if (client == null) {
                login()
            }
            val result = pythonModule?.callAttr(
                "download_gpx_and_transfer_tcx_to_extension",
                client,
                garminActivityId,
                downloadPathGpx,
                downloadPathYaml,
                downloadPathTcx
            )
            checkOutput(result)
        }
        Log.i(TAG, "downloadTcxFile took $time")
    }

    fun downloadActivitiesByDate(activitiesDir: File, startDate: String, endDate: String): String {
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
        return result?.toString() ?: ""
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
                                }
                            } catch (_: IllegalArgumentException) {
                                Log.i(TAG, "Could not parse file ${it.absolutePath}")
                            } catch (_: NullPointerException) {
                                Log.i(TAG, "Could not parse file ${it.absolutePath}")
                            }
                        }
                    }
                }
            }
            return entries
        }

        fun roundToTwoDigits(value: Float): Float {
            return (value * 100f).roundToInt() / 100f
        }

        fun roundToTwoDigits(value: Double): Double {
            return (value * 100.0).roundToInt() / 100.0
        }

        fun parseJsonObjectFromParentActivity(jsonObject: JsonObject): Summit {
            return Summit.parseFromGarminJson(jsonObject)
        }

        fun parseJsonObjectFromChildActivity(json: JsonObject, jsonParent: JsonObject?): Summit {
            val summaryDTO = json.getAsJsonObject("summaryDTO")
            return Summit.parseFromGarminJson(summaryDTO, jsonParent, json)
        }

        fun getJsonObjectEntryNotNull(jsonObject: JsonObject, key: String): Float {
            return if (jsonObject.has(key)) {
                if (jsonObject[key].isJsonNull) 0.0f else jsonObject[key].asFloat
            } else {
                0f
            }
        }

    }

}