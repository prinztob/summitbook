package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.models.SummitEntry
import java.util.*
import kotlin.math.ceil

class ExtremaValuesSummits(private val entries: ArrayList<SummitEntry>) {
    var averageSpeedMinMax = getMinMax { e -> e.pace }
    var minAverageSpeed = averageSpeedMinMax?.first?.pace ?: 0.0
    var maxAverageSpeed = averageSpeedMinMax?.second?.pace ?: 0.0
    val maxAverageSpeedCeil = ceil(averageSpeedMinMax?.second?.pace ?: 0.0).toInt()

    var durationMinMax = getMinMax { e -> if (e.duration < 24.0) e.duration else 0.0 }
    var topSpeedMinMax = getMinMax { e -> e.topSpeed }
    var minTopSpeed = topSpeedMinMax?.first?.topSpeed ?: 0.0
    var maxTopSpeed = topSpeedMinMax?.second?.topSpeed ?: 0.0
    val maxTopSpeedCeil = ceil(topSpeedMinMax?.second?.topSpeed ?: 0.0).toInt()

    var kilometersMinMax = getMinMax { e -> e.kilometers }
    var minKilometers = kilometersMinMax?.first?.kilometers ?: 0.0
    var maxKilometers = kilometersMinMax?.second?.kilometers ?: 0.0
    val maxKilometersCeil = ceil(kilometersMinMax?.second?.kilometers ?: 0.0).toInt()

    var heightMetersMinMax = getMinMax { e -> e.heightMeter }
    var minHeightMeters = heightMetersMinMax?.first?.heightMeter ?: 0
    var maxHeightMeters = heightMetersMinMax?.second?.heightMeter ?: 0

    var topElevationMinMax = getMinMax { e -> e.topElevation }
    var minTopElevation = topElevationMinMax?.first?.topElevation ?: 0
    var maxTopElevation = topElevationMinMax?.second?.topElevation ?: 0

    var averageHRMinMax = getMinMax { e -> e.activityData?.averageHR ?: 0 }

    var maxHRMinMax = getMinMax { e -> e.activityData?.maxHR ?: 0 }
    var normPowerMinMax = getMinMax { e -> e.activityData?.power?.normPower ?: 0 }
    var maxPowerMinMax = getMinMax { e -> e.activityData?.power?.maxPower ?: 0 }
    var averagePowerMinMax = getMinMax { e -> e.activityData?.power?.avgPower ?: 0 }
    var power1sMinMax = getMinMax { e -> e.activityData?.power?.oneSec ?: 0 }
    var power2sMinMax = getMinMax { e -> e.activityData?.power?.twoSec ?: 0 }
    var power5sMinMax = getMinMax { e -> e.activityData?.power?.fiveSec ?: 0 }
    var power10sMinMax = getMinMax { e -> e.activityData?.power?.tenSec ?: 0 }
    var power20sMinMax = getMinMax { e -> e.activityData?.power?.twentySec ?: 0 }
    var power30sMinMax = getMinMax { e -> e.activityData?.power?.thirtySec ?: 0 }
    var power1minMinMax = getMinMax { e -> e.activityData?.power?.oneMin ?: 0 }
    var power2minMinMax = getMinMax { e -> e.activityData?.power?.twoMin ?: 0 }
    var power5minMinMax = getMinMax { e -> e.activityData?.power?.fiveMin ?: 0 }
    var power10minMinMax = getMinMax { e -> e.activityData?.power?.tenMin ?: 0 }
    var power20minMinMax = getMinMax { e -> e.activityData?.power?.twentyMin ?: 0 }
    var power30minMinMax = getMinMax { e -> e.activityData?.power?.thirtyMin ?: 0 }
    var power1hMinMax = getMinMax { e -> e.activityData?.power?.oneHour ?: 0 }
    var power2hMinMax = getMinMax { e -> e.activityData?.power?.twoHours ?: 0 }
    var power5hMinMax = getMinMax { e -> e.activityData?.power?.fiveHours ?: 0 }

    var caloriesMinMax = getMinMax { e -> e.activityData?.calories ?: 0 }
    var ftpMinMax = getMinMax { e -> e.activityData?.ftp ?: 0 }
    var vo2maxMinMax = getMinMax { e -> e.activityData?.vo2max ?: 0 }
    var flowMinMax = getMinMax { e -> e.activityData?.flow ?: 0 }
    var gritMinMax = getMinMax { e -> e.activityData?.grit ?: 0 }
    var trainingsLoadMinMax = getMinMax { e -> e.activityData?.trainingLoad ?: 0 }

    private fun getMinMax(f: (SummitEntry) -> Number): Pair<SummitEntry, SummitEntry>? {
        var min: SummitEntry? = entries.firstOrNull { f(it).toDouble() != 0.0 }
        if (min != null) {
            var minValue = f(min)
            var max: SummitEntry = min
            var maxValue = f(max)
            for (entry in entries) {
                val value = f(entry)
                if (value.toDouble() > maxValue.toDouble()) {
                    maxValue = value
                    max = entry
                }
                if (value.toDouble() in 0.0..minValue.toDouble()) {
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