package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.JsonArray
import com.google.gson.JsonNull.INSTANCE
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.GarminActivityData
import de.drtobiasprinz.summitbook.models.PowerData
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import org.json.JSONArray
import java.io.*
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.roundToLong


class GarminConnectAccess {
    var wasLoginSuccessful = false
    private lateinit var addSummitDialog: AddSummitDialog
    private lateinit var progressBar: RelativeLayout
    private lateinit var context: Context
    private lateinit var requestQueue: RequestQueue
    private lateinit var username: String
    private lateinit var password: String
    private var manager: CookieManager? = null
    private var mDialog: AlertDialog? = null
    private var listItemsGpsDownloadSuccessful: BooleanArray? = null
    private val tag = "GaminConnectAccess"
    private val gcLoginUrl = "https://sso.garmin.com/sso/signin?"
    private val gcReferer = "https://connect.garmin.com/modern/activities"
    private val gcProfileUrl = "https://connect.garmin.com/modern/profile"
    private val gcActivityListUrl = "https://connect.garmin.com/modern/proxy/activitylist-service/activities/search/activities?"
    fun setAddSummitDialog(addSummitDialog: AddSummitDialog) {
        this.addSummitDialog = addSummitDialog
    }

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

    fun parseSportType(jsonObject: JsonObject): SportType {
        return when (jsonObject["typeId"].asInt) {
            1 -> SportType.Running
            2 -> SportType.Bicycle
            5 -> SportType.Mountainbike
            89 -> SportType.BikeAndHike
            169 -> SportType.Skitour
            else -> SportType.Hike
        }
    }

    @Throws(ParseException::class)
    private fun parseJsonObject(jsonObject: JsonObject): SummitEntry {
        val date = SimpleDateFormat(SummitEntry.DATETIME_FORMAT, Locale.ENGLISH)
                .parse(jsonObject.getAsJsonPrimitive("startTimeLocal").asString) ?: Date()
        val duration: Double = if (jsonObject["movingDuration"] != INSTANCE) jsonObject["movingDuration"].asDouble else jsonObject["duration"].asDouble
        val averageSpeed = convertMphToKmh(jsonObject["distance"].asDouble / duration)
        val entry = SummitEntry(date,
                jsonObject["activityName"].asString,
                parseSportType(jsonObject["activityType"].asJsonObject),
                emptyList(), emptyList(), "",
                jsonObject["elevationGain"].asInt,
                round(convertMeterToKm(jsonObject["distance"].asDouble), 2),
                round(averageSpeed, 2),
                if (jsonObject["maxSpeed"] != INSTANCE) round(convertMphToKmh(jsonObject["maxSpeed"].asDouble), 2) else 0.0,
                if (jsonObject["maxElevation"] != INSTANCE) convertCmToMeter(jsonObject["maxElevation"].asInt) else 0,
                emptyList(),
                mutableListOf()
        )
        val activityData = GarminActivityData(
                mutableListOf(jsonObject["activityId"].asString),
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

    private fun convertCmToMeter(cm: Int): Int {
        return (cm / 100.0).roundToLong().toInt()
    }

    fun loginToGarminConnect(activityContext: Context, date: String) {
        context = activityContext
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        username = sharedPreferences.getString("garmin_username", null) ?: ""
        password = sharedPreferences.getString("garmin_password", null) ?: ""
        if (username != "" && password != "") {
            requestQueue = Volley.newRequestQueue(context)
            if (manager == null) {
                manager = CookieManager()
                manager?.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
                CookieHandler.setDefault(manager)
            }
            progressBar.visibility = View.VISIBLE
            if (wasLoginSuccessful) {
                val url: String
                try {
                    url = gcActivityListUrl +
                            URLEncoder.encode("startDate", "UTF-8") + "=" + URLEncoder.encode(date, "UTF-8") + "&" +
                            URLEncoder.encode("endDate", "UTF-8") + "=" + URLEncoder.encode(date, "UTF-8")
                    getActivityList(url)
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            } else {
                requestQueue.add(getLoginPostRequest(date))
            }
        } else {
            Toast.makeText(context,
                    "Please set Username and Password for Garmin connect in settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun getLoginPostRequest(date: String): StringRequest {
        return object : StringRequest(Method.POST, "$url#",
                Response.Listener { response: String ->
                    try {
                        checkTicketInLogin(response)
                        wasLoginSuccessful = true
                        requestQueue.add(testLoginSuccessful(date))
                    } catch (e: Exception) {
                        wasLoginSuccessful = false
                    }
                },
                Response.ErrorListener {
                    Log.e(tag, "Could not requested$url#")
                    wasLoginSuccessful = false
                }) {
            override fun getParams(): Map<String, String> {
                return getParamsForRequest()
            }

            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["referer"] = gcLoginUrl
                headers["User-Agent"] = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2816.0 Safari/537.36"
                return headers
            }
        }
    }


    private fun testLoginSuccessful(date: String): StringRequest {
        return object : StringRequest(Method.GET, gcProfileUrl,
                Response.Listener { response: String? ->
                    try {
                        getDisplayName(response)
                        val url = gcActivityListUrl +
                                URLEncoder.encode("startDate", "UTF-8") + "=" + URLEncoder.encode(date, "UTF-8") + "&" +
                                URLEncoder.encode("endDate", "UTF-8") + "=" + URLEncoder.encode(date, "UTF-8")
                        getActivityList(url)
                    } catch (e: Exception) {
                        wasLoginSuccessful = false
                        Log.e(tag, "Could not get displayName when requesting $gcProfileUrl")
                    }
                },
                Response.ErrorListener {
                    Log.e(tag, "Could not requested $gcProfileUrl")
                    wasLoginSuccessful = false
                }) {
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["User-Agent"] = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2816.0 Safari/537.36"
                return headers
            }
        }
    }

    private fun downloadGpxForSummitEntry(entry: SummitEntry, index: Int) {
        val garminId = entry.activityData?.activityId
        if (garminId != null) {
            downloadGpxFile(garminId, getTempGpsFilePath(entry.date).toFile(), index)
        }
    }

    private fun downloadMultiSportGpx(entry: SummitEntry, index: Int) {
        val gcActivityServiceUrl = "https://connect.garmin.com/modern/proxy/activity-service/activity/" // get chirldren for multi sport
        val activityServiceUrl = gcActivityServiceUrl + entry.activityData?.activityIds

        val jsonRequest: JsonObjectRequest = object : JsonObjectRequest(Method.GET, activityServiceUrl, null, Response.Listener
        { response -> //now handle the response
            Log.i("tag", response.toString())
            val ids = response.getJSONObject("metadataDTO").getJSONArray("childIds")
            val numberOfGpxTracks = ids.length()
            val gpxFiles = ArrayList<File>()
            for (i in 0 until numberOfGpxTracks) {
                gpxFiles.add(getTempGpsFilePath(ids.get(i).toString()).toFile())
                downloadGpxFile(ids.get(i).toString(), gpxFiles[i], index, gpxFiles, getTempGpsFilePath(entry.date).toFile())
            }
        }, Response.ErrorListener
        { _ -> //handle the error
            wasLoginSuccessful = false
            Log.e(tag, "Could not requested$url")
        }) {
            //this is the part, that adds the header to the request
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["nk"] = "NT"
                headers["User-Agent"] = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2816.0 Safari/537.36"
                return headers
            }
        }
        requestQueue.add(jsonRequest)
    }


    private fun downloadGpxFile(garmin_id: String?, tempGpxFile: File, index: Int, gpxFilesToDownload: ArrayList<File>? = null, finalGpxFileName: File? = null) {
        val gcGpxActivityUrl = "https://connect.garmin.com/modern/proxy/download-service/export/gpx/activity/"
        requestQueue.add(
                InputStreamVolleyRequest(Request.Method.GET, String.format("%s%s?full=true", gcGpxActivityUrl, garmin_id),
                        Response.Listener { response: ByteArray? ->
                            try {
                                if (response != null) {
                                    val outputStream = FileOutputStream(tempGpxFile)
                                    outputStream.write(response)
                                    outputStream.close()
                                    Toast.makeText(context, context.getString(R.string.download_complete), Toast.LENGTH_LONG).show()
                                }
                                if (gpxFilesToDownload != null && finalGpxFileName != null) {
                                    val areAllFilesDownloaded = gpxFilesToDownload.count { !it.exists() } == 0
                                    if (areAllFilesDownloaded) {
                                        val gpsUtils = GpsUtils()
                                        gpsUtils.write(finalGpxFileName, gpsUtils.composeGpxFile(gpxFilesToDownload, "MultiSportMerge"), "MultiSportMerge")
                                    }
                                    listItemsGpsDownloadSuccessful?.set(index, areAllFilesDownloaded)
                                } else {
                                    listItemsGpsDownloadSuccessful?.set(index, true)
                                }
                                mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = listItemsGpsDownloadSuccessful?.contains(false) == false
                            } catch (e: Exception) {
                                listItemsGpsDownloadSuccessful?.set(index, true)
                                mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = listItemsGpsDownloadSuccessful?.contains(false) == false
                                Toast.makeText(context, context.getString(R.string.download_gps_failed, e.message), Toast.LENGTH_LONG).show()
                            }
                        },
                        Response.ErrorListener { error: VolleyError ->
                            Toast.makeText(context, context.getString(R.string.download_gps_failed, error.message), Toast.LENGTH_LONG).show()
                        }, getParamsForRequest())
        )
    }

    private fun getParamsForRequest(): MutableMap<String, String> {
        val params: MutableMap<String, String> = HashMap()
        params["username"] = username
        params["password"] = password
        params["embed"] = "false"
        params["rememberme"] = "on"
        return params
    }

    private fun getActivityList(url: String) {
        requestQueue.add(object : JsonArrayRequest(Method.GET, url, null,
                Response.Listener { response: JSONArray ->
                    val gson = JsonParser().parse(response.toString()) as JsonArray
                    val entries = getSummitsAtDate(gson)
                    val mBuilder = AlertDialog.Builder(context)
                    mBuilder.setTitle(context.getString(R.string.choose_item))
                    val listItems = arrayOfNulls<String>(entries.size)
                    val listItemsChecked = BooleanArray(entries.size)
                    listItemsGpsDownloadSuccessful = BooleanArray(entries.size)
                    for (i in entries.indices) {
                        listItems[i] = entries[i].toReadableString(context)
                        listItemsChecked[i] = false
                        listItemsGpsDownloadSuccessful?.set(i, true)
                    }
                    mBuilder
                            .setMultiChoiceItems(listItems, listItemsChecked) { _: DialogInterface, which: Int, isChecked: Boolean ->
                                val entry = entries[which]
                                listItemsGpsDownloadSuccessful?.set(which, false)
                                listItemsChecked[which] = isChecked
                                mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = listItemsGpsDownloadSuccessful?.contains(false) == false
                                if (entry.sportType == SportType.BikeAndHike) {
                                    downloadMultiSportGpx(entry, which)
                                } else {
                                    downloadGpxForSummitEntry(entry, which)
                                }
                            }
                            .setPositiveButton(R.string.saveButtonText) { _: DialogInterface?, _: Int ->
                                var entry: SummitEntry? = null
                                val files: ArrayList<File> = ArrayList()
                                for (i in entries.indices) {
                                    if (listItemsChecked[i]) {
                                        entry = extractSummitEntry(entries, i, files, entry)
                                    }
                                }
                                val gpsUtils = GpsUtils()
                                val activityData = entry?.activityData
                                if (entry != null && activityData != null) {
                                    gpsUtils.write(getTempGpsFilePath(activityData.activityId).toFile(), gpsUtils.composeGpxFile(files, entry.name), entry.name)
                                    addSummitDialog.updateDialogFields(entry, !addSummitDialog.isUpdate)
                                    Toast.makeText(context, context.getString(R.string.garmin_add_successful, entry.name), Toast.LENGTH_LONG).show()
                                    progressBar.visibility = View.GONE
                                }
                            }
                            .setNegativeButton(R.string.cancelButtonText) { _: DialogInterface?, _: Int ->
                                Toast.makeText(context, context.getString(R.string.garmin_add_cancel), Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                            }

                    mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                    mDialog = mBuilder.create()
                    mDialog?.show()
                },
                Response.ErrorListener {
                    wasLoginSuccessful = false
                    Log.e(tag, "Could not requested$url")
                }) {
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["referer"] = gcReferer
                headers["accept"] = "application/json"
                headers["nk"] = "NT"
                headers["User-Agent"] = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:85.0) Gecko/20100101 Firefox/85.0"
                return headers
            }
        })
    }

    private fun extractSummitEntry(entries: ArrayList<SummitEntry>, i: Int, files: ArrayList<File>, entry: SummitEntry?): SummitEntry {
        var entry1 = entry
        val tempFileName = getTempGpsFilePath(entries[i].date).toFile()
        if (tempFileName.exists()) {
            files.add(tempFileName)
        }
        if (entry1 == null) {
            entry1 = entries[i]
        } else {
            entry1.heightMeter += entries[i].heightMeter
            val timeInHouroldEntry: Double = entry1.kilometers / entry1.pace
            val timeInHourNewEntry: Double = entries[i].kilometers / entries[i].pace
            entry1.kilometers += entries[i].kilometers
            entry1.pace = entry1.kilometers / (timeInHourNewEntry + timeInHouroldEntry)
            if (entry1.topSpeed < entries[i].topSpeed) entry1.topSpeed = entries[i].topSpeed
            if (entry1.topElevation < entries[i].topElevation) entry1.topElevation = entries[i].topElevation
            if (entry1.activityData == null) {
                if (entries[i].activityData != null) entry1.activityData = entries[i].activityData
            } else {
                val activityDataOnI = entries[i].activityData
                val activityDataEntry1 = entry1.activityData
                if (activityDataEntry1 != null && activityDataOnI != null) {
                    activityDataEntry1.activityIds.addAll(activityDataOnI.activityIds)
                    activityDataEntry1.calories += activityDataOnI.calories
                    activityDataEntry1.averageHR = ((activityDataEntry1.averageHR * timeInHouroldEntry + activityDataOnI.averageHR * timeInHourNewEntry) / (timeInHourNewEntry + timeInHouroldEntry)).toFloat()
                    if (activityDataEntry1.maxHR < activityDataOnI.maxHR) activityDataEntry1.maxHR = activityDataOnI.maxHR
                    if (activityDataEntry1.ftp < activityDataOnI.ftp) activityDataEntry1.ftp = activityDataOnI.ftp
                    if (activityDataEntry1.vo2max < activityDataOnI.vo2max) activityDataEntry1.vo2max = activityDataOnI.vo2max
                    if (activityDataEntry1.grit < activityDataOnI.grit) activityDataEntry1.grit = activityDataOnI.grit
                    if (activityDataEntry1.flow < activityDataOnI.flow) activityDataEntry1.flow = activityDataOnI.flow
                    if (activityDataEntry1.power.oneSec > 0 && activityDataOnI.power.oneSec > 0) {
                        if (activityDataEntry1.power.maxPower < activityDataOnI.power.maxPower) activityDataEntry1.power.maxPower = activityDataOnI.power.maxPower
                        activityDataEntry1.power.avgPower = ((activityDataEntry1.power.avgPower * timeInHouroldEntry + activityDataOnI.power.avgPower * timeInHourNewEntry) / (timeInHourNewEntry + timeInHouroldEntry)).toFloat()
                        if (activityDataEntry1.power.normPower < activityDataOnI.power.normPower) activityDataEntry1.power.normPower = activityDataOnI.power.normPower
                        if (activityDataEntry1.power.oneSec < activityDataOnI.power.oneSec) activityDataEntry1.power.oneSec = activityDataOnI.power.oneSec
                        if (activityDataEntry1.power.twoSec < activityDataOnI.power.twoSec) activityDataEntry1.power.twoSec = activityDataOnI.power.twoSec
                        if (activityDataEntry1.power.fiveSec < activityDataOnI.power.fiveSec) activityDataEntry1.power.fiveSec = activityDataOnI.power.fiveSec
                        if (activityDataEntry1.power.tenSec < activityDataOnI.power.tenSec) activityDataEntry1.power.tenSec = activityDataOnI.power.tenSec
                        if (activityDataEntry1.power.twentySec < activityDataOnI.power.twentySec) activityDataEntry1.power.twentySec = activityDataOnI.power.twentySec
                        if (activityDataEntry1.power.thirtySec < activityDataOnI.power.thirtySec) activityDataEntry1.power.thirtySec = activityDataOnI.power.thirtySec
                        if (activityDataEntry1.power.oneMin < activityDataOnI.power.oneMin) activityDataEntry1.power.oneMin = activityDataOnI.power.oneMin
                        if (activityDataEntry1.power.twoMin < activityDataOnI.power.twoMin) activityDataEntry1.power.twoMin = activityDataOnI.power.twoMin
                        if (activityDataEntry1.power.fiveMin < activityDataOnI.power.fiveMin) activityDataEntry1.power.fiveMin = activityDataOnI.power.fiveMin
                        if (activityDataEntry1.power.tenMin < activityDataOnI.power.tenMin) activityDataEntry1.power.tenMin = activityDataOnI.power.tenMin
                        if (activityDataEntry1.power.twentyMin < activityDataOnI.power.twentyMin) activityDataEntry1.power.twentyMin = activityDataOnI.power.twentyMin
                        if (activityDataEntry1.power.thirtyMin < activityDataOnI.power.thirtyMin) activityDataEntry1.power.thirtyMin = activityDataOnI.power.thirtyMin
                        if (activityDataEntry1.power.oneHour < activityDataOnI.power.oneHour) activityDataEntry1.power.oneHour = activityDataOnI.power.oneHour
                        if (activityDataEntry1.power.twoHours < activityDataOnI.power.twoHours) activityDataEntry1.power.twoHours = activityDataOnI.power.twoHours
                        if (activityDataEntry1.power.fiveHours < activityDataOnI.power.fiveHours) activityDataEntry1.power.fiveHours = activityDataOnI.power.fiveHours
                    } else if (activityDataOnI.power.oneSec > 0) {
                        activityDataEntry1.power = activityDataOnI.power
                    }
                    if (activityDataEntry1.anaerobicTrainingEffect < activityDataOnI.anaerobicTrainingEffect) activityDataEntry1.anaerobicTrainingEffect = activityDataOnI.anaerobicTrainingEffect
                    if (activityDataEntry1.aerobicTrainingEffect < activityDataOnI.aerobicTrainingEffect) activityDataEntry1.aerobicTrainingEffect = activityDataOnI.aerobicTrainingEffect
                }
            }
        }
        return entry1
    }

    @Throws(Exception::class)
    private fun checkTicketInLogin(response: String) {
        val pattern = Pattern.compile(".*\\?ticket=([-\\w]+)\";.*")
        val matcher = pattern.matcher(response)
        if (!matcher.find()) {
            throw Exception("Did not get a ticket in the login response. Cannot log in. Did you enter the correct username and password?")
        }
    }

    private val url: String
        get() {
            val baseUrl = "https://connect.garmin.com/en-US/signin"
            val cssUrl = "https://static.garmincdn.com/com.garmin.connect/ui/css/gauth-custom-v1.2-min.css"
            val redirectUrl = "https://connect.garmin.com/modern/"
            val ssoUrl = "https://sso.garmin.com/sso"
            val webhostUrl = "https://connect.garmin.com"
            val data = HashMap<String, String>()
            data["service"] = redirectUrl
            data["webhost"] = webhostUrl
            data["source"] = baseUrl
            data["redirectAfterAccountLoginUrl"] = redirectUrl
            data["redirectAfterAccountCreationUrl"] = redirectUrl
            data["gauthHost"] = ssoUrl
            data["locale"] = "en_US"
            data["id"] = "gauth-widget"
            data["cssUrl"] = cssUrl
            data["clientId"] = "GarminConnect"
            data["rememberMeShown"] = "true"
            data["rememberMeChecked"] = "false"
            data["createAccountShown"] = "true"
            data["openCreateAccount"] = "false"
            data["displayNameShown"] = "false"
            data["consumeServiceTicket"] = "false"
            data["initialFocus"] = "true"
            data["embedWidget"] = "false"
            data["generateExtraServiceTicket"] = "true"
            data["generateTwoExtraServiceTickets"] = "false"
            data["generateNoServiceTicket"] = "false"
            data["globalOptInShown"] = "true"
            data["globalOptInChecked"] = "false"
            data["mobile"] = "false"
            data["connectLegalTerms"] = "true"
            data["locationPromptShown"] = "true"
            data["showPassword"] = "true"
            return gcLoginUrl + data.entries.stream()
                    .map { p: Map.Entry<String, String> ->
                        try {
                            return@map URLEncoder.encode(p.key, "UTF-8") + "=" + URLEncoder.encode(p.value, "UTF-8")
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                            return@map ""
                        }
                    }
                    .reduce { p1: String, p2: String -> "$p1&$p2" }
                    .orElse("")
        }

    fun setProgressBarLayout(progressBar: RelativeLayout) {
        this.progressBar = progressBar
    }

    companion object {

        private fun round(value: Double, precision: Int): Double {
            val scale = 10.0.pow(precision.toDouble()).toInt()
            return (value * scale).roundToLong().toDouble() / scale
        }

        fun getTempGpsFilePath(date: Date): Path {
            val tag = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.US).format(date)
            val fileName = String.format(Locale.ENGLISH, "track_from_%s.gpx", tag)
            return Paths.get(MainActivity.cache.toString(), fileName)
        }

        fun getTempGpsFilePath(activityId: String): Path {
            val fileName = String.format(Locale.ENGLISH, "id_%s.gpx", activityId)
            return Paths.get(MainActivity.cache.toString(), fileName)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun getDisplayName(response: String?): String {
            val pattern = Pattern.compile(".*\\\\\"displayName\\\\\":\\\\\"([-.\\w]+)\\\\\".*")
            val matcher = pattern.matcher(response as CharSequence)
            return if (matcher.find()) {
                matcher.group(1) ?: ""
            } else {
                throw Exception("Did not find the display name in the profile page.")
            }
        }

        @JvmStatic
        fun getJsonData(file: File): String? {
            val json: String
            json = try {
                val stream: InputStream = FileInputStream(file)
                val size = stream.available()
                val buffer = ByteArray(size)
                stream.read(buffer)
                stream.close()
                String(buffer, StandardCharsets.UTF_8)
            } catch (ex: IOException) {
                ex.printStackTrace()
                return null
            }
            return json
        }
    }
}