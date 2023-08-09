package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit

enum class LineChartSpinnerEntry(
    val nameId: Int, val unit: Int = R.string.empty, val accumulate: Boolean = false,
    var includeIndoorActivities: Boolean = false,
    var f: (Summit) -> Float?
) {
    HeightMeter(R.string.height_meter, R.string.hm, f = { e -> e.elevationData.elevationGain.toFloat() }),
    HeightMeterAccumulated(
        R.string.height_meter_accumulated,
        R.string.hm,
        true,
        f = { e -> e.elevationData.elevationGain.toFloat() }),
    Kilometers(R.string.kilometers_hint, R.string.km, f = { e -> e.kilometers.toFloat() }),
    KilometersAccumulated(
        R.string.kilometers_accumulated,
        R.string.km,
        true,
        f = { e -> e.kilometers.toFloat() }),
    Elevation(R.string.highest_peak, R.string.masl, f = { e -> e.elevationData.maxElevation.toFloat() }),
    AverageSpeed(R.string.pace_hint, R.string.kmh, f = { e -> e.velocityData.avgVelocity.toFloat() }),
    TopSpeed(R.string.top_speed, R.string.kmh, f = { e -> e.velocityData.maxVelocity.toFloat() }),
    Vo2Max(R.string.vo2Max, includeIndoorActivities = true, f = { e -> e.garminData?.vo2max }),
    AverageHeartRate(
        R.string.average_hr,
        R.string.bpm,
        includeIndoorActivities = true,
        f = { e -> e.garminData?.averageHR }),
    NormalizedPower(
        R.string.normalized_power,
        R.string.watt,
        includeIndoorActivities = true,
        f = { e -> e.garminData?.power?.normPower }),
    Power20Min(
        R.string.power_20min,
        R.string.watt,
        includeIndoorActivities = true,
        f = { e -> e.garminData?.power?.twentyMin?.toFloat() }),
    Power1h(
        R.string.power_1h,
        R.string.watt,
        includeIndoorActivities = true,
        f = { e -> e.garminData?.power?.oneHour?.toFloat() });

    override fun toString(): String {
        return name
    }

}