package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class StatisticGroup {
    VELOCITY,
    SPEED,
    POWER,
    NONE
}

enum class StatisticEntryDefinitions(
    val getSummit: (ExtremaValuesSummits) -> Summit?,
    val unit: Int,
    val nameId: Int,
    val getValue: (Summit) -> Double,
    val digits: Int = 0,
    val factor: Int = 1,
    val toHHms: Boolean = false,
    val group: StatisticGroup = StatisticGroup.NONE
) {
    AverageSpeed(
        { e -> e.averageSpeedMinMax?.second },
        R.string.value_with_kmh,
        R.string.highest_average_speed,
        { e -> e.getAverageVelocity() },
        1
    ),
    LongestDuration(
        { e -> e.durationMinMax?.second },
        R.string.value_with_h,
        R.string.longest_duration,
        { e -> e.duration / 3600.0 },
        1,
        toHHms = true
    ),
    MaxSlope(
        { e -> e.topSlopeMinMax?.second },
        R.string.value_with_per_cent,
        R.string.max_slope,
        { e -> e.elevationData.maxSlope },
        1
    ),
    MaxVerticalVelocity1Mi(
        { e -> e.topVerticalVelocity1MinMinMax?.second },
        R.string.value_with_m,
        R.string.max_verticalVelocity_1Min,
        { e -> e.elevationData.maxVerticalVelocity1Min },
        factor = 60,
        digits = 0,
        group = StatisticGroup.VELOCITY
    ),
    MaxVerticalVelocity10Min(
        { e -> e.topVerticalVelocity10MinMinMax?.second },
        R.string.value_with_m,
        R.string.max_verticalVelocity_10Min,
        { e -> e.elevationData.maxVerticalVelocity10Min },
        factor = 600,
        digits = 0,
        group = StatisticGroup.VELOCITY
    ),
    MaxVerticalVelocity1H(
        { e -> e.topVerticalVelocity1hMinMax?.second },
        R.string.value_with_m,
        R.string.max_verticalVelocity_1h,
        { e -> e.elevationData.maxVerticalVelocity1h },
        factor = 3600,
        digits = 0,
        group = StatisticGroup.VELOCITY
    ),
    TopSpeed(
        { e -> e.topSpeedMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed,
        { e -> e.velocityData.maxVelocity },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed1Km(
        { e -> e.oneKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_1km_hint,
        { e -> e.velocityData.oneKilometer },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed5Km(
        { e -> e.fiveKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_5km_hint,
        { e -> e.velocityData.fiveKilometer },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed10Km(
        { e -> e.tenKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_10km_hint,
        { e -> e.velocityData.tenKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed15Km(
        { e -> e.fifteenKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_15km_hint,
        { e -> e.velocityData.fifteenKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed20Km(
        { e -> e.twentyKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_20km_hint,
        { e -> e.velocityData.twentyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed30Km(
        { e -> e.thirtyKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_30km_hint,
        { e -> e.velocityData.thirtyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed40Km(
        { e -> e.fortyKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_40km_hint,
        { e -> e.velocityData.fortyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed50Km(
        { e -> e.fiftyKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_50km_hint,
        { e -> e.velocityData.fiftyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed75Km(
        { e -> e.seventyFiveKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_75km_hint,
        { e -> e.velocityData.seventyFiveKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed100Km(
        { e -> e.hundredKmMinMax?.second },
        R.string.value_with_kmh,
        R.string.top_speed_100km_hint,
        { e -> e.velocityData.hundredKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    ElevationGain(
        { e -> e.heightMetersMinMax?.second },
        R.string.value_with_m,
        R.string.elevationGain,
        { e -> e.elevationData.elevationGain.toDouble() }),
    TopElevation(
        { e -> e.topElevationMinMax?.second },
        R.string.value_with_masl,
        R.string.top_elevation_hint,
        { e -> e.elevationData.maxElevation.toDouble() }),
    NormPower(
        { e -> e.normPowerMinMax?.second },
        R.string.value_with_watt,
        R.string.normalized_power,
        { e -> e.garminData?.power?.normPower?.toDouble() ?: 0.0 }),
    Vo2Max(
        { e -> e.vo2maxMinMax?.second },
        R.string.value_only,
        R.string.highest_VO2MAX,
        { e -> e.garminData?.vo2max?.toDouble() ?: 0.0 },
        1
    ),
    Kilometer(
        { e -> e.kilometersMinMax?.second },
        R.string.value_with_km,
        R.string.longest_distance,
        { e -> e.kilometers },
        1
    ),
    Power1sec(
        { e -> e.power1sMinMax?.second },
        R.string.value_with_watt,
        R.string.power_1sec,
        { e -> e.garminData?.power?.oneSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power2sec(
        { e -> e.power2sMinMax?.second },
        R.string.value_with_watt,
        R.string.power_2sec,
        { e -> e.garminData?.power?.twoSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power5sec(
        { e -> e.power5sMinMax?.second },
        R.string.value_with_watt,
        R.string.power_5sec,
        { e -> e.garminData?.power?.fiveSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power10sec(
        { e -> e.power10sMinMax?.second },
        R.string.value_with_watt,
        R.string.power_10sec,
        { e -> e.garminData?.power?.tenSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power20sec(
        { e -> e.power20sMinMax?.second },
        R.string.value_with_watt,
        R.string.power_20sec,
        { e -> e.garminData?.power?.twentySec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power30sec(
        { e -> e.power30sMinMax?.second },
        R.string.value_with_watt,
        R.string.power_30sec,
        { e -> e.garminData?.power?.thirtySec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power1min(
        { e -> e.power1minMinMax?.second },
        R.string.value_with_watt,
        R.string.power_1min,
        { e -> e.garminData?.power?.oneMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power2min(
        { e -> e.power2minMinMax?.second },
        R.string.value_with_watt,
        R.string.power_2min,
        { e -> e.garminData?.power?.twoMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power5min(
        { e -> e.power5minMinMax?.second },
        R.string.value_with_watt,
        R.string.power_5min,
        { e -> e.garminData?.power?.fiveMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power10min(
        { e -> e.power10minMinMax?.second },
        R.string.value_with_watt,
        R.string.power_10min,
        { e -> e.garminData?.power?.tenMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power20min(
        { e -> e.power20minMinMax?.second },
        R.string.value_with_watt,
        R.string.power_20min,
        { e -> e.garminData?.power?.twentyMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power30min(
        { e -> e.power30minMinMax?.second },
        R.string.value_with_watt,
        R.string.power_30min,
        { e -> e.garminData?.power?.thirtyMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power1h(
        { e -> e.power1hMinMax?.second },
        R.string.value_with_watt,
        R.string.power_1h,
        { e -> e.garminData?.power?.oneHour?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power2h(
        { e -> e.power2hMinMax?.second },
        R.string.value_with_watt,
        R.string.power_2h,
        { e -> e.garminData?.power?.twoHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power3h(
        { e -> e.power3hMinMax?.second },
        R.string.value_with_watt,
        R.string.power_3h,
        { e -> e.garminData?.power?.threeHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power4h(
        { e -> e.power4hMinMax?.second },
        R.string.value_with_watt,
        R.string.power_4h,
        { e -> e.garminData?.power?.fourHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power5h(
        { e -> e.power5hMinMax?.second },
        R.string.value_with_watt,
        R.string.power_5h,
        { e -> e.garminData?.power?.fiveHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    AverageHR(
        { e -> e.averageHRMinMax?.second },
        R.string.value_with_bpm,
        R.string.average_hr,
        { e -> e.garminData?.averageHR?.toDouble() ?: 0.0 }),
    Flow(
        { e -> e.flowMinMax?.second },
        R.string.value_only,
        R.string.flow,
        { e -> e.garminData?.flow?.toDouble() ?: 0.0 },
        1
    ),
    Grit(
        { e -> e.gritMinMax?.second },
        R.string.value_only,
        R.string.grit,
        { e -> e.garminData?.grit?.toDouble() ?: 0.0 }),
    TrainingsLoad(
        { e -> e.trainingsLoadMinMax?.second },
        R.string.value_only,
        R.string.trainingLoad,
        { e -> e.garminData?.trainingLoad?.toDouble() ?: 0.0 }),
    FTP(
        { e -> e.ftpMinMax?.second },
        R.string.value_only,
        R.string.ftp,
        { e -> e.garminData?.ftp?.toDouble() ?: 0.0 }),
}