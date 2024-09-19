package de.drtobiasprinz.summitbook.ui.utils

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class MaxVelocitySummit {

    val velocityEntries = mutableListOf<VelocityEntry>()

    fun parseVelocityEntriesFomGarmin(inputJson: JsonObject) {
        val laps = inputJson.getAsJsonArray("lapDTOs")
        for (i in 0 until laps.size()) {
            val row = laps[i] as JsonObject
            velocityEntries.add(
                VelocityEntry(
                    row.getAsJsonPrimitive("distance").asDouble,
                    row.getAsJsonPrimitive("movingDuration").asDouble
                )
            )
        }
    }

    fun getAverageVelocityForKilometers(kilometer: Double): Double {
        val velocitiesInKilometerInterval = mutableListOf<Double>()
        for (i in velocityEntries.indices) {
            var sumKilometers = 0.0
            var sumDurationHours = 0.0
            var j = i
            while (sumKilometers < kilometer && j < velocityEntries.size) {
                sumKilometers += velocityEntries[j].meter / 1000
                sumDurationHours += velocityEntries[j].seconds / 3600
                j += 1
            }
            if (sumKilometers >= kilometer) {
                velocitiesInKilometerInterval.add(sumKilometers / sumDurationHours)
            }
        }
        return velocitiesInKilometerInterval.maxOrNull() ?: 0.0
    }

    companion object {
        fun getMaxVelocitySummitFromSplitFiles(splitsFile: List<File>): MaxVelocitySummit {
            val maxVelocitySummit = MaxVelocitySummit()
            Log.i("getMaxVelocitySummitFromSplitFiles", "Analysing following files: $splitsFile")
            splitsFile.filter { it.exists() }.forEach { file ->
                val json = JsonParser.parseString(JsonUtils.getJsonData(file)) as JsonObject
                maxVelocitySummit.parseVelocityEntriesFomGarmin(json)
            }
            return maxVelocitySummit
        }
    }
}

class VelocityEntry(val meter: Double, val seconds: Double) {
    val velocity = meter/seconds * 3.6
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VelocityEntry

        if (meter != other.meter) return false
        if (seconds != other.seconds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = meter.hashCode()
        result = 31 * result + seconds.hashCode()
        return result
    }
}