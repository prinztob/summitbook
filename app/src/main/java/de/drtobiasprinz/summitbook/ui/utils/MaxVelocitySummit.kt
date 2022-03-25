package de.drtobiasprinz.summitbook.ui.utils

import com.google.gson.JsonObject

class MaxVelocitySummit() {

    fun parseFomGarmin(inputJson: JsonObject): List<VelocityEntry> {
        val velocityEntries = mutableListOf<VelocityEntry>()
        val laps = inputJson.getAsJsonArray("lapDTOs")
        for (i in 0 until laps.size()) {
            val row = laps[i] as JsonObject
            velocityEntries.add(VelocityEntry(row.getAsJsonPrimitive("distance").asDouble, row.getAsJsonPrimitive("movingDuration").asDouble))
        }
        return velocityEntries
    }

    fun getAverageVelocityForKilometers(kilometer: Double, velocityEntries: List<VelocityEntry>): Double {
        val velocitiesInKilometerInterval = mutableListOf<Double>()
        for (i in velocityEntries.indices) {
            var sumKilometers = 0.0
            var sumDurationHours = 0.0
            var j = i
            while (sumKilometers < kilometer && j < velocityEntries.size) {
                sumKilometers += velocityEntries.get(j).meter / 1000
                sumDurationHours += velocityEntries.get(j).seconds / 3600
                j += 1
            }
            if (sumKilometers >= kilometer) {
                velocitiesInKilometerInterval.add(sumKilometers / sumDurationHours)
            }
        }
        return velocitiesInKilometerInterval.maxOrNull() ?: 0.0
    }
}

class VelocityEntry(val meter: Double, val seconds: Double) {
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