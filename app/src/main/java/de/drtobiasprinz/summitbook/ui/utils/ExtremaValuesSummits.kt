package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import java.util.*
import kotlin.math.ceil

class ExtremaValuesSummits(val entries: List<Summit>, private val indoorHeightMeterPercent: Int = 0) {
    var averageSpeedMinMax = getMinMax(true) { e -> e.velocityData.avgVelocity }
    var minAverageSpeed = averageSpeedMinMax?.first?.velocityData?.avgVelocity ?: 0.0
    var maxAverageSpeed = averageSpeedMinMax?.second?.velocityData?.avgVelocity ?: 0.0
    val maxAverageSpeedCeil = ceil(averageSpeedMinMax?.second?.velocityData?.avgVelocity
            ?: 0.0).toInt()

    var durationMinMax = getMinMax { e -> if (e.duration < 24.0) e.duration else 0.0 }
    var topSpeedMinMax = getMinMax(true) { e -> e.velocityData.maxVelocity }
    var minTopSpeed = topSpeedMinMax?.first?.velocityData?.maxVelocity ?: 0.0
    var maxTopSpeed = topSpeedMinMax?.second?.velocityData?.maxVelocity ?: 0.0
    val maxTopSpeedCeil = ceil(topSpeedMinMax?.second?.velocityData?.maxVelocity ?: 0.0).toInt()
    var oneKmMinMax = getMinMax(true) { e -> e.velocityData.oneKilometer }
    var fiveKmMinMax = getMinMax(true) { e -> e.velocityData.fiveKilometer }
    var tenKmMinMax = getMinMax(true) { e -> e.velocityData.tenKilometers }
    var fifteenKmMinMax = getMinMax(true) { e -> e.velocityData.fifteenKilometers }
    var twentyKmMinMax = getMinMax(true) { e -> e.velocityData.twentyKilometers }
    var thirtyKmMinMax = getMinMax(true) { e -> e.velocityData.thirtyKilometers }
    var fortyKmMinMax = getMinMax(true) { e -> e.velocityData.fortyKilometers }
    var fiftyKmMinMax = getMinMax(true) { e -> e.velocityData.fiftyKilometers }
    var seventyFiveKmMinMax = getMinMax(true) { e -> e.velocityData.seventyFiveKilometers }
    var hundredKmMinMax = getMinMax(true) { e -> e.velocityData.hundredKilometers }

    var kilometersMinMax = getMinMax(true) { e -> e.kilometers }
    var minKilometers = kilometersMinMax?.first?.kilometers ?: 0.0
    var maxKilometers = kilometersMinMax?.second?.kilometers ?: 0.0
    val maxKilometersCeil = ceil(kilometersMinMax?.second?.kilometers ?: 0.0).toInt()

    var heightMetersMinMax = getMinMax(true) { e -> e.elevationData.elevationGain }
    var minHeightMeters = heightMetersMinMax?.first?.elevationData?.elevationGain ?: 0
    var maxHeightMeters = heightMetersMinMax?.second?.elevationData?.elevationGain ?: 0

    var topElevationMinMax = getMinMax(true) { e -> e.elevationData.maxElevation }
    var minTopElevation = topElevationMinMax?.first?.elevationData?.maxElevation ?: 0
    var maxTopElevation = topElevationMinMax?.second?.elevationData?.maxElevation ?: 0
    var topSlopeMinMax = getMinMax(true) { e -> e.elevationData.maxSlope }
    var topVerticalVelocity1MinMinMax = getMinMax(true) { e -> e.elevationData.maxVerticalVelocity1Min }
    var topVerticalVelocity10MinMinMax = getMinMax(true) { e -> e.elevationData.maxVerticalVelocity10Min }
    var topVerticalVelocity1hMinMax = getMinMax(true) { e -> e.elevationData.maxVerticalVelocity1h }

    var averageHRMinMax = getMinMax { e -> e.garminData?.averageHR ?: 0 }

    var maxHRMinMax = getMinMax { e -> e.garminData?.maxHR ?: 0 }
    var normPowerMinMax = getMinMax { e -> e.garminData?.power?.normPower ?: 0 }
    var maxPowerMinMax = getMinMax { e -> e.garminData?.power?.maxPower ?: 0 }
    var averagePowerMinMax = getMinMax { e -> e.garminData?.power?.avgPower ?: 0 }
    var power1sMinMax = getMinMax { e -> e.garminData?.power?.oneSec ?: 0 }
    var power2sMinMax = getMinMax { e -> e.garminData?.power?.twoSec ?: 0 }
    var power5sMinMax = getMinMax { e -> e.garminData?.power?.fiveSec ?: 0 }
    var power10sMinMax = getMinMax { e -> e.garminData?.power?.tenSec ?: 0 }
    var power20sMinMax = getMinMax { e -> e.garminData?.power?.twentySec ?: 0 }
    var power30sMinMax = getMinMax { e -> e.garminData?.power?.thirtySec ?: 0 }
    var power1minMinMax = getMinMax { e -> e.garminData?.power?.oneMin ?: 0 }
    var power2minMinMax = getMinMax { e -> e.garminData?.power?.twoMin ?: 0 }
    var power5minMinMax = getMinMax { e -> e.garminData?.power?.fiveMin ?: 0 }
    var power10minMinMax = getMinMax { e -> e.garminData?.power?.tenMin ?: 0 }
    var power20minMinMax = getMinMax { e -> e.garminData?.power?.twentyMin ?: 0 }
    var power30minMinMax = getMinMax { e -> e.garminData?.power?.thirtyMin ?: 0 }
    var power1hMinMax = getMinMax { e -> e.garminData?.power?.oneHour ?: 0 }
    var power2hMinMax = getMinMax { e -> e.garminData?.power?.twoHours ?: 0 }
    var power5hMinMax = getMinMax { e -> e.garminData?.power?.fiveHours ?: 0 }

    var caloriesMinMax = getMinMax { e -> e.garminData?.calories ?: 0 }
    var ftpMinMax = getMinMax { e -> e.garminData?.ftp ?: 0 }
    var vo2maxMinMax = getMinMax { e -> e.garminData?.vo2max ?: 0 }
    var flowMinMax = getMinMax { e -> e.garminData?.flow ?: 0 }
    var gritMinMax = getMinMax { e -> e.garminData?.grit ?: 0 }
    var trainingsLoadMinMax = getMinMax { e -> e.garminData?.trainingLoad ?: 0 }

    private fun getMinMax(shouldIndoorAvitivityBeExcluded: Boolean = false, f: (Summit) -> Number): Pair<Summit, Summit>? {
        var min: Summit? = entries.firstOrNull { f(it).toDouble() != 0.0 }
        if (min != null) {
            var minValue = f(min)
            var max: Summit = min
            var maxValue = f(max)
            val filteredEntries = if (shouldIndoorAvitivityBeExcluded) entries.filter { it.sportType != SportType.IndoorTrainer } else entries
            for (entry in filteredEntries) {
                val value = f(entry)
                if (value.toDouble() > maxValue.toDouble()) {
                    maxValue = value
                    max = entry
                }
                if (value.toDouble() in 0.0..minValue.toDouble() && value.toDouble() != 0.0) {
                    minValue = value
                    min = entry
                }
            }
            return Pair(min!!, max)
        } else {
            return null
        }
    }

}