package de.drtobiasprinz.summitbook.models

import android.widget.LinearLayout
import android.widget.TextView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentStatisticsBinding
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
    val layout: (FragmentStatisticsBinding) -> LinearLayout,
    val data: (FragmentStatisticsBinding) -> TextView,
    val info: (FragmentStatisticsBinding) -> TextView,
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
        { b -> b.layoutHighestAverageSpeed },
        { b -> b.textHeighestAverageSpeed },
        { b -> b.textHeighestAverageSpeedInfo },
        R.string.value_with_kmh,
        R.string.highest_average_speed,
        { e -> e.getAverageVelocity() },
        1
    ),
    LongestDuration(
        { e -> e.durationMinMax?.second },
        { b -> b.layoutLongestDuration },
        { b -> b.textLongestDuration },
        { b -> b.textLongestDurationInfo },
        R.string.value_with_h,
        R.string.longest_duration,
        { e -> e.duration / 3600.0 },
        1,
        toHHms = true
    ),
    MaxSlope(
        { e -> e.topSlopeMinMax?.second },
        { b -> b.layoutMaxSlope },
        { b -> b.textMaxSlope },
        { b -> b.textMaxSlopeInfo },
        R.string.value_with_per_cent,
        R.string.max_slope,
        { e -> e.elevationData.maxSlope },
        1
    ),
    MaxVerticalVelocity1Mi(
        { e -> e.topVerticalVelocity1MinMinMax?.second },
        { b -> b.layoutMaxVerticalVelocity1Min },
        { b -> b.textMaxVerticalVelocity1Min },
        { b -> b.textMaxVerticalVelocity1MinInfo },
        R.string.value_with_m,
        R.string.max_verticalVelocity_1Min,
        { e -> e.elevationData.maxVerticalVelocity1Min },
        factor = 60,
        digits = 0,
        group = StatisticGroup.VELOCITY
    ),
    MaxVerticalVelocity10Min(
        { e -> e.topVerticalVelocity10MinMinMax?.second },
        { b -> b.layoutMaxVerticalVelocity10Min },
        { b -> b.textMaxVerticalVelocity10Min },
        { b -> b.textMaxVerticalVelocity10MinInfo },
        R.string.value_with_m,
        R.string.max_verticalVelocity_10Min,
        { e -> e.elevationData.maxVerticalVelocity10Min },
        factor = 600,
        digits = 0,
        group = StatisticGroup.VELOCITY
    ),
    MaxVerticalVelocity1H(
        { e -> e.topVerticalVelocity1hMinMax?.second },
        { b -> b.layoutMaxVerticalVelocity1h },
        { b -> b.textMaxVerticalVelocity1h },
        { b -> b.textMaxVerticalVelocity1hInfo },
        R.string.value_with_m,
        R.string.max_verticalVelocity_1h,
        { e -> e.elevationData.maxVerticalVelocity1h },
        factor = 3600,
        digits = 0,
        group = StatisticGroup.VELOCITY
    ),
    TopSpeed(
        { e -> e.topSpeedMinMax?.second },
        { b -> b.layoutTopSpeed },
        { b -> b.textTopSpeed },
        { b -> b.textTopSpeedInfo },
        R.string.value_with_kmh,
        R.string.top_speed,
        { e -> e.velocityData.maxVelocity },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed1Km(
        { e -> e.oneKmMinMax?.second },
        { b -> b.layoutTopSpeed1Km },
        { b -> b.textTopSpeed1Km },
        { b -> b.textTopSpeed1KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_1km_hint,
        { e -> e.velocityData.oneKilometer },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed5Km(
        { e -> e.fiveKmMinMax?.second },
        { b -> b.layoutTopSpeed5Km },
        { b -> b.textTopSpeed5Km },
        { b -> b.textTopSpeed5KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_5km_hint,
        { e -> e.velocityData.fiveKilometer },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed10Km(
        { e -> e.tenKmMinMax?.second },
        { b -> b.layoutTopSpeed10Km },
        { b -> b.textTopSpeed10Km },
        { b -> b.textTopSpeed10KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_10km_hint,
        { e -> e.velocityData.tenKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed15Km(
        { e -> e.fifteenKmMinMax?.second },
        { b -> b.layoutTopSpeed15Km },
        { b -> b.textTopSpeed15Km },
        { b -> b.textTopSpeed15KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_15km_hint,
        { e -> e.velocityData.fifteenKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed20Km(
        { e -> e.twentyKmMinMax?.second },
        { b -> b.layoutTopSpeed20Km },
        { b -> b.textTopSpeed20Km },
        { b -> b.textTopSpeed20KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_20km_hint,
        { e -> e.velocityData.twentyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed30Km(
        { e -> e.thirtyKmMinMax?.second },
        { b -> b.layoutTopSpeed30Km },
        { b -> b.textTopSpeed30Km },
        { b -> b.textTopSpeed30KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_30km_hint,
        { e -> e.velocityData.thirtyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed40Km(
        { e -> e.fortyKmMinMax?.second },
        { b -> b.layoutTopSpeed40Km },
        { b -> b.textTopSpeed40Km },
        { b -> b.textTopSpeed40KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_40km_hint,
        { e -> e.velocityData.fortyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed50Km(
        { e -> e.fiftyKmMinMax?.second },
        { b -> b.layoutTopSpeed50Km },
        { b -> b.textTopSpeed50Km },
        { b -> b.textTopSpeed50KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_50km_hint,
        { e -> e.velocityData.fiftyKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed75Km(
        { e -> e.seventyFiveKmMinMax?.second },
        { b -> b.layoutTopSpeed75Km },
        { b -> b.textTopSpeed75Km },
        { b -> b.textTopSpeed75KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_75km_hint,
        { e -> e.velocityData.seventyFiveKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    TopSpeed100Km(
        { e -> e.hundredKmMinMax?.second },
        { b -> b.layoutTopSpeed100Km },
        { b -> b.textTopSpeed100Km },
        { b -> b.textTopSpeed100KmInfo },
        R.string.value_with_kmh,
        R.string.top_speed_100km_hint,
        { e -> e.velocityData.hundredKilometers },
        1,
        group = StatisticGroup.SPEED
    ),
    ElevationGain(
        { e -> e.heightMetersMinMax?.second },
        { b -> b.layoutMostHeightMeter },
        { b -> b.textMostHeightMeter },
        { b -> b.textMostHeightMeterInfo },
        R.string.value_with_m,
        R.string.elevationGain,
        { e -> e.elevationData.elevationGain.toDouble() }),
    TopElevation(
        { e -> e.topElevationMinMax?.second },
        { b -> b.layoutHighestPeak },
        { b -> b.textHighestPeak },
        { b -> b.textHighestPeakInfo },
        R.string.value_with_masl,
        R.string.top_elevation_hint,
        { e -> e.elevationData.maxElevation.toDouble() }),
    NormPower(
        { e -> e.normPowerMinMax?.second },
        { b -> b.layoutHighestPower },
        { b -> b.textHeighestPower },
        { b -> b.textHeighestPowerInfo },
        R.string.value_with_watt,
        R.string.normalized_power,
        { e -> e.garminData?.power?.normPower?.toDouble() ?: 0.0 }),
    Vo2Max(
        { e -> e.vo2maxMinMax?.second },
        { b -> b.layoutHighestVO2MAX },
        { b -> b.textHeighestVO2MAX },
        { b -> b.textHeighestVO2MAXInfo },
        R.string.value_only,
        R.string.highest_VO2MAX,
        { e -> e.garminData?.vo2max?.toDouble() ?: 0.0 },
        1
    ),
    Kilometer(
        { e -> e.kilometersMinMax?.second },
        { b -> b.layoutLongestDistance },
        { b -> b.textLongestDistance },
        { b -> b.textLongestDistanceInfo },
        R.string.value_with_km,
        R.string.longest_distance,
        { e -> e.kilometers },
        1
    ),
    Power1sec(
        { e -> e.power1sMinMax?.second },
        { b -> b.layoutHighestPower1sec },
        { b -> b.textHeighestPower1sec },
        { b -> b.textHeighestPower1secInfo },
        R.string.value_with_watt,
        R.string.power_1sec,
        { e -> e.garminData?.power?.oneSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power2sec(
        { e -> e.power2sMinMax?.second },
        { b -> b.layoutHighestPower2sec },
        { b -> b.textHeighestPower2sec },
        { b -> b.textHeighestPower2secInfo },
        R.string.value_with_watt,
        R.string.power_2sec,
        { e -> e.garminData?.power?.twoSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power5sec(
        { e -> e.power5sMinMax?.second },
        { b -> b.layoutHighestPower5sec },
        { b -> b.textHeighestPower5sec },
        { b -> b.textHeighestPower5secInfo },
        R.string.value_with_watt,
        R.string.power_5sec,
        { e -> e.garminData?.power?.fiveSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power10sec(
        { e -> e.power10sMinMax?.second },
        { b -> b.layoutHighestPower10sec },
        { b -> b.textHeighestPower10sec },
        { b -> b.textHeighestPower10secInfo },
        R.string.value_with_watt,
        R.string.power_10sec,
        { e -> e.garminData?.power?.tenSec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power20sec(
        { e -> e.power20sMinMax?.second },
        { b -> b.layoutHighestPower20sec },
        { b -> b.textHeighestPower20sec },
        { b -> b.textHeighestPower20secInfo },
        R.string.value_with_watt,
        R.string.power_20sec,
        { e -> e.garminData?.power?.twentySec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power30sec(
        { e -> e.power30sMinMax?.second },
        { b -> b.layoutHighestPower30sec },
        { b -> b.textHeighestPower30sec },
        { b -> b.textHeighestPower30secInfo },
        R.string.value_with_watt,
        R.string.power_30sec,
        { e -> e.garminData?.power?.thirtySec?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power1min(
        { e -> e.power1minMinMax?.second },
        { b -> b.layoutHighestPower1min },
        { b -> b.textHeighestPower1min },
        { b -> b.textHeighestPower1minInfo },
        R.string.value_with_watt,
        R.string.power_1min,
        { e -> e.garminData?.power?.oneMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power2min(
        { e -> e.power2minMinMax?.second },
        { b -> b.layoutHighestPower2min },
        { b -> b.textHeighestPower2min },
        { b -> b.textHeighestPower2minInfo },
        R.string.value_with_watt,
        R.string.power_2min,
        { e -> e.garminData?.power?.twoMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power5min(
        { e -> e.power5minMinMax?.second },
        { b -> b.layoutHighestPower5min },
        { b -> b.textHeighestPower5min },
        { b -> b.textHeighestPower5minInfo },
        R.string.value_with_watt,
        R.string.power_5min,
        { e -> e.garminData?.power?.fiveMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power10min(
        { e -> e.power10minMinMax?.second },
        { b -> b.layoutHighestPower10min },
        { b -> b.textHeighestPower10min },
        { b -> b.textHeighestPower10minInfo },
        R.string.value_with_watt,
        R.string.power_10min,
        { e -> e.garminData?.power?.tenMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power20min(
        { e -> e.power20minMinMax?.second },
        { b -> b.layoutHighestPower20min },
        { b -> b.textHeighestPower20min },
        { b -> b.textHeighestPower20minInfo },
        R.string.value_with_watt,
        R.string.power_20min,
        { e -> e.garminData?.power?.twentyMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power30min(
        { e -> e.power30minMinMax?.second },
        { b -> b.layoutHighestPower30min },
        { b -> b.textHeighestPower30min },
        { b -> b.textHeighestPower30minInfo },
        R.string.value_with_watt,
        R.string.power_30min,
        { e -> e.garminData?.power?.thirtyMin?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power1h(
        { e -> e.power1hMinMax?.second },
        { b -> b.layoutHighestPower1h },
        { b -> b.textHeighestPower1h },
        { b -> b.textHeighestPower1hInfo },
        R.string.value_with_watt,
        R.string.power_1h,
        { e -> e.garminData?.power?.oneHour?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power2h(
        { e -> e.power2hMinMax?.second },
        { b -> b.layoutHighestPower2h },
        { b -> b.textHeighestPower2h },
        { b -> b.textHeighestPower2hInfo },
        R.string.value_with_watt,
        R.string.power_2h,
        { e -> e.garminData?.power?.twoHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power3h(
        { e -> e.power3hMinMax?.second },
        { b -> b.layoutHighestPower3h },
        { b -> b.textHeighestPower3h },
        { b -> b.textHeighestPower3hInfo },
        R.string.value_with_watt,
        R.string.power_3h,
        { e -> e.garminData?.power?.threeHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power4h(
        { e -> e.power4hMinMax?.second },
        { b -> b.layoutHighestPower4h },
        { b -> b.textHeighestPower4h },
        { b -> b.textHeighestPower4hInfo },
        R.string.value_with_watt,
        R.string.power_4h,
        { e -> e.garminData?.power?.fourHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    Power5h(
        { e -> e.power5hMinMax?.second },
        { b -> b.layoutHighestPower5h },
        { b -> b.textHeighestPower5h },
        { b -> b.textHeighestPower5hInfo },
        R.string.value_with_watt,
        R.string.power_5h,
        { e -> e.garminData?.power?.fiveHours?.toDouble() ?: 0.0 },
        group = StatisticGroup.POWER
    ),
    AverageHR(
        { e -> e.averageHRMinMax?.second },
        { b -> b.layoutHighestAverageHR },
        { b -> b.textHeighestAverageHR },
        { b -> b.textHeighestAverageHRInfo },
        R.string.value_with_bpm,
        R.string.average_hr,
        { e -> e.garminData?.averageHR?.toDouble() ?: 0.0 }),
    Flow(
        { e -> e.flowMinMax?.second },
        { b -> b.layoutHighestFlow },
        { b -> b.textHeighestFlow },
        { b -> b.textHeighestFlowInfo },
        R.string.value_only,
        R.string.flow,
        { e -> e.garminData?.flow?.toDouble() ?: 0.0 },
        1
    ),
    Grit(
        { e -> e.gritMinMax?.second },
        { b -> b.layoutHighestGrit },
        { b -> b.textHeighestGrit },
        { b -> b.textHeighestGritInfo },
        R.string.value_only,
        R.string.grit,
        { e -> e.garminData?.grit?.toDouble() ?: 0.0 }),
    TrainingsLoad(
        { e -> e.trainingsLoadMinMax?.second },
        { b -> b.layoutHighestTrainingLoad },
        { b -> b.textHeighestTrainingLoad },
        { b -> b.textHeighestTrainingLoadInfo },
        R.string.value_only,
        R.string.trainingLoad,
        { e -> e.garminData?.trainingLoad?.toDouble() ?: 0.0 }),
    FTP(
        { e -> e.ftpMinMax?.second },
        { b -> b.layoutHighestFTP },
        { b -> b.textHeighestFTP },
        { b -> b.textHeighestFTPInfo },
        R.string.value_only,
        R.string.ftp,
        { e -> e.garminData?.ftp?.toDouble() ?: 0.0 }),
}