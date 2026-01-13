package de.drtobiasprinz.summitbook.models

import android.widget.TextView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryPowerBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class TextFieldPower(
    val descriptionTextView: (FragmentSummitEntryPowerBinding) -> TextView,
    val valueTextView: (FragmentSummitEntryPowerBinding) -> TextView,
    val unit: String,
    val nameId: Int,
    val getValue: (Summit) -> Number?,
    val getMinMaxSummit: (ExtremaValuesSummits?) -> Pair<Summit, Summit>? = { _ -> null },
    val reverse: Boolean = false,
    val toHHms: Boolean = false,
    val digits: Int = 1,
    val factor: Int = 1,
    val valueTextViewRange: (FragmentSummitEntryPowerBinding) -> TextView? = { _ -> null },
    val getValueRange: (Summit) -> Number? = { _ -> null },
) {


    MaxPower(
        { b -> b.maxPowerText },
        { b -> b.maxPower },
        "W",
        R.string.max_power,
        { e -> e.garminData?.power?.maxPower },
        { e -> e?.maxPowerMinMax },
        digits = 0
    ),
    AveragePower(
        { b -> b.averagePowerText },
        { b -> b.averagePower },
        "W",
        R.string.average_power,
        { e -> e.garminData?.power?.avgPower },
        { e -> e?.averagePowerMinMax },
        digits = 0
    ),
    NormPower(
        { b -> b.normPowerText },
        { b -> b.normPower },
        "W",
        R.string.normalized_power,
        { e -> e.garminData?.power?.normPower },
        { e -> e?.normPowerMinMax },
        digits = 0
    ),
    IntensityFactor(
        { b -> b.intensityFactorText },
        { b -> b.intensityFactor },
        "",
        R.string.intensity_factor,
        { e -> e.garminData?.power?.intensityFactor },
        { e -> e?.intensityFactorMinMax },
        digits = 2
    ),
    TrainingStressScore(
        { b -> b.trainingStressScoreText },
        { b -> b.trainingStressScore },
        "",
        R.string.training_stress_score,
        { e -> e.garminData?.power?.trainingStressScore },
        { e -> e?.trainingStressScoreMinMax },
        digits = 0
    ),
    FTP(
        { b -> b.FTPText },
        { b -> b.FTP },
        "",
        R.string.ftp,
        { e -> e.garminData?.ftp },
        { e -> e?.ftpMinMax },
        digits = 0
    ),
    Power1s(
        { b -> b.power1secText },
        { b -> b.power1sec },
        "W",
        R.string.power_1sec,
        { e -> e.garminData?.power?.oneSec },
        { e -> e?.power1sMinMax },
        digits = 0
    ),
    Power2s(
        { b -> b.power2secText },
        { b -> b.power2sec },
        "W",
        R.string.power_2sec,
        { e -> e.garminData?.power?.twoSec },
        { e -> e?.power2sMinMax },
        digits = 0
    ),
    Power5s(
        { b -> b.power5secText },
        { b -> b.power5sec },
        "W",
        R.string.power_5sec,
        { e -> e.garminData?.power?.fiveSec },
        { e -> e?.power5sMinMax },
        digits = 0
    ),
    Power10s(
        { b -> b.power10secText },
        { b -> b.power10sec },
        "W",
        R.string.power_10sec,
        { e -> e.garminData?.power?.tenSec },
        { e -> e?.power10sMinMax },
        digits = 0
    ),
    Power20s(
        { b -> b.power20secText },
        { b -> b.power20sec },
        "W",
        R.string.power_20sec,
        { e -> e.garminData?.power?.twentySec },
        { e -> e?.power20sMinMax },
        digits = 0
    ),
    Power30s(
        { b -> b.power30secText },
        { b -> b.power30sec },
        "W",
        R.string.power_30sec,
        { e -> e.garminData?.power?.thirtySec },
        { e -> e?.power30sMinMax },
        digits = 0
    ),
    Power1Min(
        { b -> b.power1minText },
        { b -> b.power1min },
        "W",
        R.string.power_1min,
        { e -> e.garminData?.power?.oneMin },
        { e -> e?.power1minMinMax },
        digits = 0
    ),
    Power2Min(
        { b -> b.power2minText },
        { b -> b.power2min },
        "W",
        R.string.power_2min,
        { e -> e.garminData?.power?.twoMin },
        { e -> e?.power2minMinMax },
        digits = 0
    ),
    Power5Min(
        { b -> b.power5minText },
        { b -> b.power5min },
        "W",
        R.string.power_5min,
        { e -> e.garminData?.power?.fiveMin },
        { e -> e?.power5minMinMax },
        digits = 0
    ),
    Power10Min(
        { b -> b.power10minText },
        { b -> b.power10min },
        "W",
        R.string.power_10min,
        { e -> e.garminData?.power?.tenMin },
        { e -> e?.power10minMinMax },
        digits = 0
    ),
    Power20Min(
        { b -> b.power20minText },
        { b -> b.power20min },
        "W",
        R.string.power_20min,
        { e -> e.garminData?.power?.twentyMin },
        { e -> e?.power20minMinMax },
        digits = 0
    ),
    Power30Min(
        { b -> b.power30minText },
        { b -> b.power30min },
        "W",
        R.string.power_30min,
        { e -> e.garminData?.power?.thirtyMin },
        { e -> e?.power30minMinMax },
        digits = 0
    ),
    Power1H(
        { b -> b.power1hText },
        { b -> b.power1h },
        "W",
        R.string.power_1h,
        { e -> e.garminData?.power?.oneHour },
        { e -> e?.power1hMinMax },
        digits = 0
    ),
    Power2H(
        { b -> b.power2hText },
        { b -> b.power2h },
        "W",
        R.string.power_2h,
        { e -> e.garminData?.power?.twoHours },
        { e -> e?.power2hMinMax },
        digits = 0
    ),
    Power3H(
        { b -> b.power3hText },
        { b -> b.power3h },
        "W",
        R.string.power_3h,
        { e -> e.garminData?.power?.threeHours },
        { e -> e?.power3hMinMax },
        digits = 0
    ),
    Power4H(
        { b -> b.power4hText },
        { b -> b.power4h },
        "W",
        R.string.power_4h,
        { e -> e.garminData?.power?.fourHours },
        { e -> e?.power4hMinMax },
        digits = 0
    ),
    Power5H(
        { b -> b.power5hText },
        { b -> b.power5h },
        "W",
        R.string.power_5h,
        { e -> e.garminData?.power?.fiveHours },
        { e -> e?.power5hMinMax },
        digits = 0
    ),

}
