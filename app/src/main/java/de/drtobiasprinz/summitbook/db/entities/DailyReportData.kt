package de.drtobiasprinz.summitbook.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class DailyEvent(
    var sportType: SportType,
    var duration: Int,
    var moderateIntensityMinutes: Int,
    var vigorousIntensityMinutes: Int
) {
    override fun toString(): String {
        return "$sportType,$duration,$moderateIntensityMinutes,$vigorousIntensityMinutes"
    }
}

@Entity
data class DailyReportData(
    @PrimaryKey(autoGenerate = true) var entryId: Long = 0,
    var date: Date = Date(),
    var solarUtilizationInHours: Double = 0.0,
    var solarExposureInHours: Double = 0.0,
    @ColumnInfo(defaultValue = "0") var steps: Int = 0,
    @ColumnInfo(defaultValue = "0") var floorsClimbed: Int = 0,
    var events: List<DailyEvent> = emptyList(),
    var isForWholeDay: Boolean = false,
    @ColumnInfo(defaultValue = "0") var totalIntensityMinutes: Int = 0,
    @ColumnInfo(defaultValue = "0") var restingHeartRate: Int = 0,
    @ColumnInfo(defaultValue = "0") var minHeartRate: Int = 0,
    @ColumnInfo(defaultValue = "0") var maxHeartRate: Int = 0,
    @ColumnInfo(defaultValue = "0") var heartRateVariability: Int = 0,
    @ColumnInfo(defaultValue = "0") var sleepHours: Double = 0.0,
) {
    @Ignore
    var activitiesOnDay = 0

    @Ignore
    var markerText = getDateAsString()

    fun getDateAsString(): String? {
        val dateFormat: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
        return dateFormat.format(date)
    }

    companion object {
        fun parseFromJson(
            date: Date,
            jsonObjectSolarIntensity: JsonObject,
            dailyEvents: JsonArray,
            summary: JsonObject,
            heartRateVariability: JsonObject
        ): DailyReportData {
            val dailyReportData = getDailyReportDataFromJsonObject(date, summary)
            dailyReportData.events = getDailyEvents(
                dailyEvents
            )
            dailyReportData.heartRateVariability = getHrv(heartRateVariability)
            if (jsonObjectSolarIntensity.has("deviceSolarInput") && jsonObjectSolarIntensity["deviceSolarInput"].asJsonObject.has(
                    "solarDailyDataDTOs"
                )
            ) {
                parseSolarIntensity(dailyReportData, jsonObjectSolarIntensity)
            }
            return dailyReportData
        }

        fun getDailyReportDataFromJsonObject(
            date: Date,
            summary: JsonObject
        ) = DailyReportData(
            date = date,
            steps = if (check(summary, "totalSteps")) summary.get("totalSteps").asInt else 0,
            floorsClimbed = if (check(summary, "floorsAscendedInMeters")) summary.get("floorsAscendedInMeters").asDouble.roundToInt() else 0,
            totalIntensityMinutes = if (check(summary, "moderateIntensityMinutes") && check(summary, "vigorousIntensityMinutes")) {
                summary.get("moderateIntensityMinutes").asInt + summary.get("vigorousIntensityMinutes").asInt * 2
            } else {
                0
            },
            restingHeartRate = if (check(summary, "restingHeartRate")) summary.get("restingHeartRate").asInt else 0,
            minHeartRate = if (check(summary, "minHeartRate")) summary.get("minHeartRate").asInt else 0,
            maxHeartRate = if (check(summary, "maxHeartRate")) summary.get("maxHeartRate").asInt else 0,
            sleepHours = if (check(summary, "sleepingSeconds")) (summary.get("sleepingSeconds").asInt / 3600.0) else 0.0,
            isForWholeDay =
                if (check(summary, "durationInMilliseconds")) (summary.get("durationInMilliseconds").asInt / 1000.0) > 85400.0 else false

        )

        private fun check(summary: JsonObject, memberName: String) =
            summary.has(memberName) && !summary.get(memberName).isJsonNull && summary.get(memberName).asString != "None"

        fun getDailyEvents(jsonArray: JsonArray) =
            jsonArray.map {
                val entry = it.asJsonObject
                val sportType = when (entry["activityType"].asString) {
                    "cycling" -> SportType.Bicycle
                    "walking" -> SportType.Running
                    else -> SportType.Other
                }
                DailyEvent(
                    sportType,
                    entry["duration"].asInt,
                    if (entry.has("moderateIntensityMinutes")) entry["moderateIntensityMinutes"].asInt else 0,
                    if (entry.has("vigorousIntensityMinutes")) entry["vigorousIntensityMinutes"].asInt else 0
                )
            }
        fun getHrv(jsonObject: JsonObject): Int {
            return if (jsonObject.has("hrvSummary")) {
                jsonObject.get("hrvSummary").asJsonObject.get("lastNightAvg").asInt
            } else {
                0
            }
        }
        fun parseSolarIntensity(
            dailyReportData: DailyReportData,
            jsonObjectSolarIntensity: JsonObject
        ) {
            val deviceSolarInputs =
                jsonObjectSolarIntensity["deviceSolarInput"].asJsonObject.get("solarDailyDataDTOs").asJsonArray
            for (deviceSolarInput in deviceSolarInputs) {
                if (deviceSolarInput.asJsonObject.has("solarInputReadings")) {
                    val array =
                        deviceSolarInput.asJsonObject.get("solarInputReadings").asJsonArray
                    val solarUtilization = array.map {
                        it.asJsonObject.get("solarUtilization").asDouble
                    }
                    dailyReportData.solarExposureInHours =
                        solarUtilization.filter { it > 5 }.size / 60.0
                    val multiplicand = 1.0 / (60 * 100)
                    dailyReportData.solarUtilizationInHours = solarUtilization.sum() * multiplicand
                }
            }
        }
    }
}
