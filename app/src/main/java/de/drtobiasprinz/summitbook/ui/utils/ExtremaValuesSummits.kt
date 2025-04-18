package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit

class ExtremaValuesSummits(val entries: List<Summit>, val shouldIndoorActivityBeExcluded: Boolean = false, private val excludeZeroValueFromMin: Boolean = false) {
    var averageSpeedMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.getAverageVelocity() }
    var durationMinMax = getMinMax { e -> if (e.duration < 24 * 3600.0) e.duration else 0.0 }
    var topSpeedMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.maxVelocity }
    var oneKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.oneKilometer }
    var fiveKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.fiveKilometer }
    var tenKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.tenKilometers }
    var fifteenKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.fifteenKilometers }
    var twentyKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.twentyKilometers }
    var thirtyKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.thirtyKilometers }
    var fortyKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.fortyKilometers }
    var fiftyKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.fiftyKilometers }
    var seventyFiveKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.seventyFiveKilometers }
    var hundredKmMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.velocityData.hundredKilometers }

    var kilometersMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.kilometers }

    var heightMetersMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.elevationGain }

    var topElevationMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxElevation }
    var topSlopeMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxSlope }
    var topVerticalVelocity1MinMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxVerticalVelocity1Min }
    var topVerticalVelocity10MinMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxVerticalVelocity10Min }
    var topVerticalVelocity1hMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxVerticalVelocity1h }
    var topVerticalVelocityDown1MinMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxVerticalVelocityDown1Min }
    var topVerticalVelocityDown10MinMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxVerticalVelocityDown10Min }
    var topVerticalVelocityDown1hMinMax = getMinMax(shouldIndoorActivityBeExcluded) { e -> e.elevationData.maxVerticalVelocityDown1h }

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
    var aerobicTrainingEffectMinMax = getMinMax { e -> e.garminData?.aerobicTrainingEffect ?: 0 }
    var anaerobicTrainingEffectMinMax = getMinMax { e -> e.garminData?.anaerobicTrainingEffect ?: 0 }

    private fun getMinMax(shouldIndoorActivityBeExcluded: Boolean = false, f: (Summit) -> Number): Pair<Summit, Summit>? {
        val filteredEntries = if (shouldIndoorActivityBeExcluded) entries.filter { it.sportType != SportType.IndoorTrainer } else entries
        var min: Summit? = filteredEntries.firstOrNull { f(it).toDouble() != 0.0 }
        if (min != null) {
            var minValue = f(min)
            var max: Summit = min
            var maxValue = f(max)
            for (entry in filteredEntries) {
                val value = f(entry)
                if (value.toDouble() > maxValue.toDouble()) {
                    maxValue = value
                    max = entry
                }
                if (value.toDouble() in 0.0..minValue.toDouble() && ((value.toDouble() != 0.0 && excludeZeroValueFromMin) || !excludeZeroValueFromMin)) {
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