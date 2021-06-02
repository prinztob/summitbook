package de.drtobiasprinz.summitbook.ui

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.MainActivity
import java.io.File

class GarminPythonExecutor(val username: String, val password: String) {
   lateinit var pythonInstance: Python
   lateinit var pythonModule: PyObject
   var client: PyObject? = null
    fun login() {
        if (!Python.isStarted()) {
            MainActivity.mainActivity?.let { AndroidPlatform(it) }?.let { Python.start(it) }
        }
        pythonInstance = Python.getInstance()
        pythonModule = pythonInstance.getModule("start")
        val result = pythonModule.callAttr("get_authenticated_client", username, password)
        checkOutput(result)
        client = result
    }

    fun getActivityJson(dateAsString: String): JsonArray {
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
}