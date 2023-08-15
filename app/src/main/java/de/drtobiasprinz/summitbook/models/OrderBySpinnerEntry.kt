package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit

enum class OrderBySpinnerEntry(
    val nameId: Int, val unit: Int = R.string.empty, val accumulate: Boolean = false,
    var includeIndoorActivities: Boolean = false, var excludeFromLineChart: Boolean = false,
    var f: (Summit) -> Float?
) {
    Date(
        R.string.date,
        R.string.empty,
        excludeFromLineChart = true,
        f = { e -> e.getDateAsFloat() }),
    HeightMeter(
        R.string.elevationGain,
        R.string.hm,
        f = { e -> e.elevationData.elevationGain.toFloat() }),
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
    Elevation(
        R.string.highest_peak,
        R.string.masl,
        f = { e -> e.elevationData.maxElevation.toFloat() }),
    AverageSpeed(
        R.string.pace_hint,
        R.string.kmh,
        f = { e -> e.velocityData.avgVelocity.toFloat() }),
    TopSpeed(R.string.top_speed, R.string.kmh, f = { e -> e.velocityData.maxVelocity.toFloat() }),
    Vo2Max(R.string.vo2Max, includeIndoorActivities = true, f = { e -> e.garminData?.vo2max }),
    Grit(R.string.grit, includeIndoorActivities = true, f = { e -> e.garminData?.grit }),
    Flow(R.string.flow, includeIndoorActivities = true, f = { e -> e.garminData?.flow }),
    FTP(R.string.ftp, includeIndoorActivities = true, f = { e -> e.garminData?.ftp?.toFloat() }),
    Calories(
        R.string.calories,
        includeIndoorActivities = true,
        f = { e -> e.garminData?.calories }),
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
        f = { e -> e.garminData?.power?.oneHour?.toFloat() }),
    VerticalVelocity1Min(
        R.string.max_verticalVelocity_1Min,
        R.string.m,
        f = { e -> e.elevationData.maxVerticalVelocity1Min.toFloat() * 60f }),
    VerticalVelocity10Min(
        R.string.max_verticalVelocity_10Min,
        R.string.m,
        f = { e -> e.elevationData.maxVerticalVelocity10Min.toFloat() * 600f }),
    VerticalVelocity1Hour(
        R.string.max_verticalVelocity_1h,
        R.string.m,
        f = { e -> e.elevationData.maxVerticalVelocity1h.toFloat() * 3600f }),
    MaxSlope(
        R.string.max_slope,
        R.string.per_cent,
        f = { e -> e.elevationData.maxSlope.toFloat() });

    override fun toString(): String {
        return name
    }

    companion object {
        fun getSpinnerEntriesWithoutAccumulated(): List<OrderBySpinnerEntry> {
            return OrderBySpinnerEntry.values().filter { !it.accumulate }
        }
        fun getSpinnerEntriesWithoutExcludedFromLineChart(): List<OrderBySpinnerEntry> {
            return OrderBySpinnerEntry.values().filter { !it.excludeFromLineChart }
        }
    }

}