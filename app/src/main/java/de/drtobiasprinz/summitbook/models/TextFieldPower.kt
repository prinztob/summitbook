package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class TextFieldPower(
    val unit: String,
    val nameId: Int,
    val getValue: (Summit) -> Number?,
    val getMinMaxSummit: (ExtremaValuesSummits?) -> Pair<Summit, Summit>? = { _ -> null },
    val reverse: Boolean = false,
    val toHHms: Boolean = false,
    val digits: Int = 1,
    val factor: Int = 1,
) {


    MaxPower(
        "W",
        R.string.max_power,
        { e -> e.garminData?.power?.maxPower },
        { e -> e?.maxPowerMinMax },
        digits = 0
    ),
    AveragePower(
        "W",
        R.string.average_power,
        { e -> e.garminData?.power?.avgPower },
        { e -> e?.averagePowerMinMax },
        digits = 0
    ),
    NormPower(
        "W",
        R.string.normalized_power,
        { e -> e.garminData?.power?.normPower },
        { e -> e?.normPowerMinMax },
        digits = 0
    ),
    IntensityFactor(
        "",
        R.string.intensity_factor,
        { e -> e.garminData?.power?.intensityFactor },
        { e -> e?.intensityFactorMinMax },
        digits = 2
    ),
    TrainingStressScore(
        "",
        R.string.training_stress_score,
        { e -> e.garminData?.power?.trainingStressScore },
        { e -> e?.trainingStressScoreMinMax },
        digits = 0
    ),
    FTP(
        "",
        R.string.ftp,
        { e -> e.garminData?.ftp },
        { e -> e?.ftpMinMax },
        digits = 0
    ),
    EfficiencyFactor(
        "",
        R.string.efficiency_factor,
        { e ->
            if ((e.garminData?.averageHR ?: 0f) > 0) {
                ((e.garminData?.power?.normPower ?: 0f) / e.garminData?.averageHR!!)
            } else 0f
        },
        { e -> e?.power1sMinMax },
        digits = 2
    ),
    Power1s(
        "W",
        R.string.power_1sec,
        { e -> e.garminData?.power?.oneSec },
        { e -> e?.power1sMinMax },
        digits = 0
    ),
    Power2s(
        "W",
        R.string.power_2sec,
        { e -> e.garminData?.power?.twoSec },
        { e -> e?.power2sMinMax },
        digits = 0
    ),
    Power5s(
        "W",
        R.string.power_5sec,
        { e -> e.garminData?.power?.fiveSec },
        { e -> e?.power5sMinMax },
        digits = 0
    ),
    Power10s(
        "W",
        R.string.power_10sec,
        { e -> e.garminData?.power?.tenSec },
        { e -> e?.power10sMinMax },
        digits = 0
    ),
    Power20s(
        "W",
        R.string.power_20sec,
        { e -> e.garminData?.power?.twentySec },
        { e -> e?.power20sMinMax },
        digits = 0
    ),
    Power30s(
        "W",
        R.string.power_30sec,
        { e -> e.garminData?.power?.thirtySec },
        { e -> e?.power30sMinMax },
        digits = 0
    ),
    Power1Min(
        "W",
        R.string.power_1min,
        { e -> e.garminData?.power?.oneMin },
        { e -> e?.power1minMinMax },
        digits = 0
    ),
    Power2Min(
        "W",
        R.string.power_2min,
        { e -> e.garminData?.power?.twoMin },
        { e -> e?.power2minMinMax },
        digits = 0
    ),
    Power5Min(
        "W",
        R.string.power_5min,
        { e -> e.garminData?.power?.fiveMin },
        { e -> e?.power5minMinMax },
        digits = 0
    ),
    Power10Min(
        "W",
        R.string.power_10min,
        { e -> e.garminData?.power?.tenMin },
        { e -> e?.power10minMinMax },
        digits = 0
    ),
    Power20Min(
        "W",
        R.string.power_20min,
        { e -> e.garminData?.power?.twentyMin },
        { e -> e?.power20minMinMax },
        digits = 0
    ),
    Power30Min(
        "W",
        R.string.power_30min,
        { e -> e.garminData?.power?.thirtyMin },
        { e -> e?.power30minMinMax },
        digits = 0
    ),
    Power1H(
        "W",
        R.string.power_1h,
        { e -> e.garminData?.power?.oneHour },
        { e -> e?.power1hMinMax },
        digits = 0
    ),
    Power2H(
        "W",
        R.string.power_2h,
        { e -> e.garminData?.power?.twoHours },
        { e -> e?.power2hMinMax },
        digits = 0
    ),
    Power3H(
        "W",
        R.string.power_3h,
        { e -> e.garminData?.power?.threeHours },
        { e -> e?.power3hMinMax },
        digits = 0
    ),
    Power4H(
        "W",
        R.string.power_4h,
        { e -> e.garminData?.power?.fourHours },
        { e -> e?.power4hMinMax },
        digits = 0
    ),
    Power5H(
        "W",
        R.string.power_5h,
        { e -> e.garminData?.power?.fiveHours },
        { e -> e?.power5hMinMax },
        digits = 0
    ), ;

}
