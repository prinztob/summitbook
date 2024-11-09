package de.drtobiasprinz.summitbook.models

import com.google.gson.JsonObject
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import kotlin.math.roundToInt

enum class AdditionalDataTableEntry(
    val nameId: Int,
    val unitId: Int,
    var getValue: (Summit) -> Double,
    var setValue: (Summit, Double) -> Unit,
    var jsonKey: String = "",
    var isInt: Boolean = false,
    var scaleFactorView: Int = 1,
    var scaleFactorForJson: (JsonObject) -> Double = { _ -> 1.0 },
) {
    ElevationGain(
        R.string.height_meter_hint,
        R.string.hm,
        { e -> e.elevationData.elevationGain.toDouble() },
        { e, d -> e.elevationData.elevationGain = d.roundToInt() },
        jsonKey = "elevation_gain",
        isInt = true
    ),
    MaxElevation(
        R.string.top_elevation_hint,
        R.string.hm,
        { e -> e.elevationData.maxElevation.toDouble() },
        { e, d -> e.elevationData.maxElevation = d.roundToInt() },
        jsonKey = "max_elevation",
        isInt = true
    ),
    MaxVelocity(
        R.string.top_speed,
        R.string.kmh,
        { e -> e.velocityData.maxVelocity },
        { e, d -> e.velocityData.maxVelocity = d },
        scaleFactorForJson = { _ -> 3.6 },
        jsonKey = "max_speed"
    ),

    OneKilometer(
        R.string.top_speed_1km_hint,
        R.string.kmh,
        { e -> e.velocityData.oneKilometer },
        { e, d -> e.velocityData.oneKilometer = d },
        jsonKey = "avg_velocity_1km"
    ),
    FiveKilometer(
        R.string.top_speed_5km_hint,
        R.string.kmh,
        { e -> e.velocityData.fiveKilometer },
        { e, d -> e.velocityData.fiveKilometer = d },
        jsonKey = "avg_velocity_5km"
    ),
    TenKilometers(
        R.string.top_speed_10km_hint,
        R.string.kmh,
        { e -> e.velocityData.tenKilometers },
        { e, d -> e.velocityData.tenKilometers = d },
        jsonKey = "avg_velocity_10km"
    ),
    FifteenKilometers(
        R.string.top_speed_15km_hint,
        R.string.kmh,
        { e -> e.velocityData.fifteenKilometers },
        { e, d -> e.velocityData.fifteenKilometers = d },
        jsonKey = "avg_velocity_15km"
    ),
    TwentyKilometers(
        R.string.top_speed_20km_hint,
        R.string.kmh,
        { e -> e.velocityData.twentyKilometers },
        { e, d -> e.velocityData.twentyKilometers = d },
        jsonKey = "avg_velocity_20km"
    ),
    ThirtyKilometers(
        R.string.top_speed_30km_hint,
        R.string.kmh,
        { e -> e.velocityData.thirtyKilometers },
        { e, d -> e.velocityData.thirtyKilometers = d },
        jsonKey = "avg_velocity_30km"
    ),
    FortyKilometers(
        R.string.top_speed_40km_hint,
        R.string.kmh,
        { e -> e.velocityData.fortyKilometers },
        { e, d -> e.velocityData.fortyKilometers = d },
        jsonKey = "avg_velocity_40km"
    ),
    FiftyKilometers(
        R.string.top_speed_50km_hint,
        R.string.kmh,
        { e -> e.velocityData.fiftyKilometers },
        { e, d -> e.velocityData.fiftyKilometers = d },
        jsonKey = "avg_velocity_50km"
    ),
    SeventyFiveKilometers(
        R.string.top_speed_75km_hint,
        R.string.kmh,
        { e -> e.velocityData.seventyFiveKilometers },
        { e, d -> e.velocityData.seventyFiveKilometers = d },
        jsonKey = "avg_velocity_75km"
    ),
    HundredKilometers(
        R.string.top_speed_100km_hint,
        R.string.kmh,
        { e -> e.velocityData.hundredKilometers },
        { e, d -> e.velocityData.hundredKilometers = d },
        jsonKey = "avg_velocity_100km"
    ),
    Slope100Meters(
        R.string.max_slope,
        R.string.per_cent,
        { e -> e.elevationData.maxSlope },
        { e, d -> e.elevationData.maxSlope = d },
        jsonKey = "slope_100",
    ),
    VerticalVelocity60Seconds(
        R.string.max_verticalVelocity_1Min,
        R.string.m,
        { e -> e.elevationData.maxVerticalVelocity1Min },
        { e, d -> e.elevationData.maxVerticalVelocity1Min = d },
        jsonKey = "vertical_velocity_60s_+",
        scaleFactorView = 60
    ),
    VerticalVelocity600Seconds(
        R.string.max_verticalVelocity_10Min,
        R.string.m,
        { e -> e.elevationData.maxVerticalVelocity10Min },
        { e, d -> e.elevationData.maxVerticalVelocity10Min = d },
        jsonKey = "vertical_velocity_600s_+",
        scaleFactorView = 600
    ),
    VerticalVelocity360Seconds(
        R.string.max_verticalVelocity_1h,
        R.string.m,
        { e -> e.elevationData.maxVerticalVelocity1h },
        { e, d -> e.elevationData.maxVerticalVelocity1h = d },
        jsonKey = "vertical_velocity_3600s_+",
        scaleFactorView = 3600
    ),

    VerticalVelocityDown60Seconds(
        R.string.max_verticalVelocity_down_1Min,
        R.string.m,
        { e -> e.elevationData.maxVerticalVelocityDown1Min },
        { e, d -> e.elevationData.maxVerticalVelocityDown1Min = d },
        jsonKey = "vertical_velocity_60s_-",
        scaleFactorView = 60
    ),
    VerticalVelocityDown600Seconds(
        R.string.max_verticalVelocity_down_10Min,
        R.string.m,
        { e -> e.elevationData.maxVerticalVelocityDown10Min },
        { e, d -> e.elevationData.maxVerticalVelocityDown10Min = d },
        jsonKey = "vertical_velocity_600s_-",
        scaleFactorView = 600
    ),
    VerticalVelocityDown360Seconds(
        R.string.max_verticalVelocity_down_1h,
        R.string.m,
        { e -> e.elevationData.maxVerticalVelocityDown1h },
        { e, d -> e.elevationData.maxVerticalVelocityDown1h = d },
        jsonKey = "vertical_velocity_3600s_-",
        scaleFactorView = 3600
    ),
    PowerAverage(
        R.string.average_power,
        R.string.watt,
        { e -> e.garminData?.power?.avgPower?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.avgPower = d.toFloat() },
        jsonKey = "power_avg",
        isInt = true
    ),
    Power10Seconds(
        R.string.power_10sec,
        R.string.watt,
        { e -> e.garminData?.power?.tenSec?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.tenSec = d.roundToInt() },
        jsonKey = "power_10s",
        isInt = true
    ),
    Power30Seconds(
        R.string.power_30sec,
        R.string.watt,
        { e -> e.garminData?.power?.thirtySec?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.thirtySec = d.roundToInt() },
        jsonKey = "power_30s",
        isInt = true
    ),
    Power1Minute(
        R.string.power_1min,
        R.string.watt,
        { e -> e.garminData?.power?.oneMin?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.oneMin = d.roundToInt() },
        jsonKey = "power_1min",
        isInt = true
    ),
    Power5Minutes(
        R.string.power_5min,
        R.string.watt,
        { e -> e.garminData?.power?.fiveMin?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.fiveMin = d.roundToInt() },
        jsonKey = "power_5min",
        isInt = true
    ),
    Power10Minutes(
        R.string.power_10min,
        R.string.watt,
        { e -> e.garminData?.power?.tenMin?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.tenMin = d.roundToInt() },
        jsonKey = "power_10min",
        isInt = true
    ),
    Power20Minutes(
        R.string.power_20min,
        R.string.watt,
        { e -> e.garminData?.power?.twentyMin?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.twentyMin = d.roundToInt() },
        jsonKey = "power_20min",
        isInt = true
    ),
    Power30Minutes(
        R.string.power_30min,
        R.string.watt,
        { e -> e.garminData?.power?.thirtyMin?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.thirtyMin = d.roundToInt() },
        jsonKey = "power_30min",
        isInt = true
    ),
    Power1Hour(
        R.string.power_1h,
        R.string.watt,
        { e -> e.garminData?.power?.oneHour?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.oneHour = d.roundToInt() },
        jsonKey = "power_1h",
        isInt = true
    ),
    Power2Hours(
        R.string.power_2h,
        R.string.watt,
        { e -> e.garminData?.power?.twoHours?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.twoHours = d.roundToInt() },
        jsonKey = "power_2h",
        isInt = true
    ),
    Power5Hours(
        R.string.power_5h,
        R.string.watt,
        { e -> e.garminData?.power?.fiveHours?.toDouble() ?: 0.0 },
        { e, d -> e.garminData?.power?.fiveHours = d.roundToInt() },
        jsonKey = "power_5h",
        isInt = true
    ),
}
