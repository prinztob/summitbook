package de.drtobiasprinz.summitbook.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.JsonObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Entity
data class SolarIntensity(
        @PrimaryKey(autoGenerate = true) var entryId: Long = 0,
        var date: Date,
        var solarUtilizationInHours: Double,
        var solarExposureInHours: Double,
        var isForWholeDay: Boolean,
) {
    @Ignore
    var activitiesOnDay = 0
    @Ignore
    var markerText = getDateAsString()
    fun getDateAsFloat(): Float {
        return ((date.time - Summit.REFERENCE_VALUE_DATE) / 1e8).toFloat()
    }

    fun getDateAsString(): String? {
        val dateFormat: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
        return dateFormat.format(date)
    }

    companion object {
        fun parseFromJson(jsonObject: JsonObject): SolarIntensity? {
            if (jsonObject.has("deviceSolarInput") && jsonObject["deviceSolarInput"].asJsonObject.has("solarDailyDataDTOs")) {
                val deviceSolarInputs = jsonObject["deviceSolarInput"].asJsonObject.get("solarDailyDataDTOs").asJsonArray
                for (deviceSolarInput in deviceSolarInputs) {
                    if (deviceSolarInput.asJsonObject.has("solarInputReadings")) {
                        val array = deviceSolarInput.asJsonObject.get("solarInputReadings").asJsonArray
                        val solarUtilization = array.map {
                            it.asJsonObject.get("solarUtilization").asDouble
                        }
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S")
                        val startDate = LocalDateTime.parse(array.first().asJsonObject.get("readingTimestampLocal").asString, formatter)
                        val endDate = LocalDateTime.parse(array.last().asJsonObject.get("readingTimestampLocal").asString, formatter)
                        val duration = Duration.between(startDate, endDate)
                        val solarExposureInHours = solarUtilization.filter { it > 5 }.size / 60.0
                        val multiplicand = 1.0 / (60 * 100)
                        val solarUtilizationInHours = solarUtilization.sum() * multiplicand
                        return SolarIntensity(
                                0,
                                Date.from(startDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
                                solarUtilizationInHours,
                                solarExposureInHours,
                                duration.seconds > 85400
                        )
                    }
                }
            }
            return null
        }
    }
}
