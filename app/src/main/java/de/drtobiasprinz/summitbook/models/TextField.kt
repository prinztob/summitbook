package de.drtobiasprinz.summitbook.models

import android.widget.TextView
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryDataBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class TextFieldGroup {
    Base(),
    AdditionalSpeedData(),
    ThirdParty(),
    ThirdPartyAdditionalData(),
}

enum class TextField(
    val group: TextFieldGroup,
    val descriptionTextView: (FragmentSummitEntryDataBinding) -> TextView,
    val valueTextView: (FragmentSummitEntryDataBinding) -> TextView,
    val unit: String,
    val getValue: (Summit) -> Number?,
    val getMinMaxSummit: (ExtremaValuesSummits?) -> Pair<Summit, Summit>?,
    val reverse: Boolean = false,
    val toHHms: Boolean = false,
    val digits: Int = 1,
    val factor: Int = 1
) {
    HeightMeter(
        TextFieldGroup.Base,
        { b -> b.heightMeterText },
        { b -> b.heightMeter },
        "hm",
        { e -> e.elevationData.elevationGain },
        { e -> e?.heightMetersMinMax }, digits = 0
    ),
    Kilometer(TextFieldGroup.Base,
        { b -> b.kilometersText },
        { b -> b.kilometers },
        "km",
        { e -> e.kilometers },
        { e -> e?.kilometersMinMax }
    ),
    TopElevation(
        TextFieldGroup.Base,
        { b -> b.topElevationText },
        { b -> b.topElevation },
        "hm",
        { e -> e.elevationData.maxElevation },
        { e -> e?.topElevationMinMax }, digits = 0
    ),
    TopVerticalVelocity1Min(
        TextFieldGroup.Base,
        { b -> b.topVerticalVelocity1MinText },
        { b -> b.topVerticalVelocity1Min },
        "m",
        { e -> e.elevationData.maxVerticalVelocity1Min },
        { e -> e?.topVerticalVelocity1MinMinMax },
        factor = 60,
        digits = 0
    ),
    TopVerticalVelocity10Min(
        TextFieldGroup.Base,
        { b -> b.topVerticalVelocity10MinText },
        { b -> b.topVerticalVelocity10Min },
        "m",
        { e -> e.elevationData.maxVerticalVelocity10Min },
        { e -> e?.topVerticalVelocity10MinMinMax },
        factor = 600,
        digits = 0
    ),
    TopVerticalVelocity1H(
        TextFieldGroup.Base,
        { b -> b.topVerticalVelocity1hText },
        { b -> b.topVerticalVelocity1h },
        "m",
        { e -> e.elevationData.maxVerticalVelocity1h },
        { e -> e?.topVerticalVelocity1hMinMax },
        factor = 3600,
        digits = 0
    ),
    TopSlope(TextFieldGroup.Base,
        { b -> b.topSlopeText },
        { b -> b.topSlope },
        "%",
        { e -> e.elevationData.maxSlope },
        { e -> e?.topSlopeMinMax }
    ),
    Pace(TextFieldGroup.Base,
        { b -> b.paceText },
        { b -> b.pace },
        "km/h",
        { e -> e.velocityData.avgVelocity },
        { e -> e?.averageSpeedMinMax }
    ),
    TopSpeed(TextFieldGroup.Base,
        { b -> b.topSpeedText },
        { b -> b.topSpeed },
        "km/h",
        { e -> e.velocityData.maxVelocity },
        { e -> e?.topSpeedMinMax }
    ),
    Durations(TextFieldGroup.Base,
        { b -> b.durationText },
        { b -> b.duration },
        "h",
        { e -> e.duration },
        { e -> e?.durationMinMax }
    ),

    TopSpeedOneKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.oneKMTopSpeedText },
        { b -> b.oneKMTopSpeed },
        "km/h",
        { e -> e.velocityData.oneKilometer },
        { e -> e?.oneKmMinMax }
    ),
    TopSpeedFiveKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.fiveKMTopSpeedText },
        { b -> b.fiveKMTopSpeed },
        "km/h",
        { e -> e.velocityData.fiveKilometer },
        { e -> e?.fiveKmMinMax }
    ),
    TopSpeedTenKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.tenKMTopSpeedText },
        { b -> b.tenKMTopSpeed },
        "km/h",
        { e -> e.velocityData.tenKilometers },
        { e -> e?.tenKmMinMax }
    ),
    TopSpeedFifteenKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.fifteenKMTopSpeedText },
        { b -> b.fifteenKMTopSpeed },
        "km/h",
        { e -> e.velocityData.fifteenKilometers },
        { e -> e?.fifteenKmMinMax }
    ),
    TopSpeedTwentyKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.twentyKMTopSpeedText },
        { b -> b.twentyKMTopSpeed },
        "km/h",
        { e -> e.velocityData.twentyKilometers },
        { e -> e?.twentyKmMinMax }
    ),
    TopSpeedThirtyKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.thirtyKMTopSpeedText },
        { b -> b.thirtyKMTopSpeed },
        "km/h",
        { e -> e.velocityData.thirtyKilometers },
        { e -> e?.thirtyKmMinMax }
    ),
    TopSpeedFortyKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.fourtyKMTopSpeedText },
        { b -> b.fourtyKMTopSpeed },
        "km/h",
        { e -> e.velocityData.fortyKilometers },
        { e -> e?.fortyKmMinMax }
    ),
    TopSpeedFiftyKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.fiftyKMTopSpeedText },
        { b -> b.fiftyKMTopSpeed },
        "km/h",
        { e -> e.velocityData.fiftyKilometers },
        { e -> e?.fiftyKmMinMax }
    ),
    TopSpeedSeventyFiveKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.seventyfiveKMTopSpeedText },
        { b -> b.seventyfiveKMTopSpeed },
        "km/h",
        { e -> e.velocityData.seventyFiveKilometers },
        { e -> e?.seventyFiveKmMinMax }
    ),
    TopSpeedHundredKm(TextFieldGroup.AdditionalSpeedData,
        { b -> b.hundretKMTopSpeedText },
        { b -> b.hundretKMTopSpeed },
        "km/h",
        { e -> e.velocityData.hundredKilometers },
        { e -> e?.hundredKmMinMax }
    ),

    AverageHr(
        TextFieldGroup.ThirdParty,
        { b -> b.averageHrText },
        { b -> b.averageHr },
        "bpm",
        { e -> e.garminData?.averageHR },
        { e -> e?.averageHRMinMax },
        digits = 0,
        reverse = true
    ),
    MaxHr(
        TextFieldGroup.ThirdParty,
        { b -> b.maxHrText },
        { b -> b.maxHr },
        "bpm",
        { e -> e.garminData?.maxHR },
        { e -> e?.maxHRMinMax },
        digits = 0,
        reverse = true
    ),
    Calories(
        TextFieldGroup.ThirdParty,
        { b -> b.caloriesText },
        { b -> b.calories },
        "kcal",
        { e -> e.garminData?.calories },
        { e -> e?.caloriesMinMax },
        digits = 0
    ),
    MaxPower(
        TextFieldGroup.ThirdParty,
        { b -> b.maxPowerText },
        { b -> b.maxPower },
        "W",
        { e -> e.garminData?.power?.maxPower },
        { e -> e?.maxPowerMinMax },
        digits = 0
    ),
    AveragePower(
        TextFieldGroup.ThirdParty,
        { b -> b.averagePowerText },
        { b -> b.averagePower },
        "W",
        { e -> e.garminData?.power?.avgPower },
        { e -> e?.averagePowerMinMax },
        digits = 0
    ),
    NormPower(
        TextFieldGroup.ThirdParty,
        { b -> b.normPowerText },
        { b -> b.normPower },
        "W",
        { e -> e.garminData?.power?.normPower },
        { e -> e?.normPowerMinMax },
        digits = 0
    ),
    AerobicTrainingEffect(
        TextFieldGroup.ThirdParty,
        { b -> b.aerobicTrainingEffectText },
        { b -> b.aerobicTrainingEffect },
        "",
        { e -> e.garminData?.aerobicTrainingEffect },
        { e -> e?.aerobicTrainingEffectMinMax },
    ),
    AnaerobicTrainingEffect(
        TextFieldGroup.ThirdParty,
        { b -> b.anaerobicTrainingEffectText },
        { b -> b.anaerobicTrainingEffect },
        "",
        { e -> e.garminData?.anaerobicTrainingEffect },
        { e -> e?.anaerobicTrainingEffectMinMax },
    ),
    Grit(
        TextFieldGroup.ThirdParty,
        { b -> b.gritText },
        { b -> b.grit },
        "",
        { e -> e.garminData?.grit },
        { e -> e?.gritMinMax },
    ),
    Flow(
        TextFieldGroup.ThirdParty,
        { b -> b.flowText },
        { b -> b.flow },
        "",
        { e -> e.garminData?.flow },
        { e -> e?.flowMinMax },
    ),
    TrainingsLoad(
        TextFieldGroup.ThirdParty,
        { b -> b.trainingLoadText },
        { b -> b.trainingLoad },
        "",
        { e -> e.garminData?.trainingLoad },
        { e -> e?.trainingsLoadMinMax },
    ),
    Vo2Max(
        TextFieldGroup.ThirdParty,
        { b -> b.vo2MaxText },
        { b -> b.vo2Max },
        "",
        { e -> e.garminData?.vo2max },
        { e -> e?.vo2maxMinMax },
        digits = 1
    ),
    FTP(
        TextFieldGroup.ThirdParty,
        { b -> b.FTPText },
        { b -> b.FTP },
        "",
        { e -> e.garminData?.ftp },
        { e -> e?.ftpMinMax },
        digits = 0
    ),

    Power1s(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power1secText },
        { b -> b.power1sec },
        "W",
        { e -> e.garminData?.power?.oneSec },
        { e -> e?.power1sMinMax },
        digits = 0
    ),
    Power2s(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power2secText },
        { b -> b.power2sec },
        "W",
        { e -> e.garminData?.power?.twoSec },
        { e -> e?.power2sMinMax },
        digits = 0
    ),
    Power5s(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power5secText },
        { b -> b.power5sec },
        "W",
        { e -> e.garminData?.power?.fiveSec },
        { e -> e?.power5sMinMax },
        digits = 0
    ),
    Power10s(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power10secText },
        { b -> b.power10sec },
        "W",
        { e -> e.garminData?.power?.tenSec },
        { e -> e?.power10sMinMax },
        digits = 0
    ),
    Power20s(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power20secText },
        { b -> b.power20sec },
        "W",
        { e -> e.garminData?.power?.twentySec },
        { e -> e?.power20sMinMax },
        digits = 0
    ),
    Power30s(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power30secText },
        { b -> b.power30sec },
        "W",
        { e -> e.garminData?.power?.thirtySec },
        { e -> e?.power30sMinMax },
        digits = 0
    ),
    Power1Min(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power1minText },
        { b -> b.power1min },
        "W",
        { e -> e.garminData?.power?.oneMin },
        { e -> e?.power1minMinMax },
        digits = 0
    ),
    Power2Min(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power2minText },
        { b -> b.power2min },
        "W",
        { e -> e.garminData?.power?.twoMin },
        { e -> e?.power2minMinMax },
        digits = 0
    ),
    Power5Min(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power5minText },
        { b -> b.power5min },
        "W",
        { e -> e.garminData?.power?.fiveMin },
        { e -> e?.power5minMinMax },
        digits = 0
    ),
    Power10Min(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power10minText },
        { b -> b.power10min },
        "W",
        { e -> e.garminData?.power?.tenMin },
        { e -> e?.power10minMinMax },
        digits = 0
    ),
    Power20Min(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power20minText },
        { b -> b.power20min },
        "W",
        { e -> e.garminData?.power?.twentyMin },
        { e -> e?.power20minMinMax },
        digits = 0
    ),
    Power30Min(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power30minText },
        { b -> b.power30min },
        "W",
        { e -> e.garminData?.power?.thirtyMin },
        { e -> e?.power30minMinMax },
        digits = 0
    ),
    Power1H(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power1hText },
        { b -> b.power1h },
        "W",
        { e -> e.garminData?.power?.oneHour },
        { e -> e?.power1hMinMax },
        digits = 0
    ),
    Power2H(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power2hText },
        { b -> b.power2h },
        "W",
        { e -> e.garminData?.power?.twoHours },
        { e -> e?.power2hMinMax },
        digits = 0
    ),
    Power5H(
        TextFieldGroup.ThirdPartyAdditionalData,
        { b -> b.power5hText },
        { b -> b.power5h },
        "W",
        { e -> e.garminData?.power?.fiveHours },
        { e -> e?.power5hMinMax },
        digits = 0
    ),

}
