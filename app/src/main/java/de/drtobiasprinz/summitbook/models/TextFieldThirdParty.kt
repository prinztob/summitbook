package de.drtobiasprinz.summitbook.models

import android.widget.TextView
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryThirdPartyBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class TextFieldGroupThirdPArty {
    ThirdParty,
    ThirdPartyAdditionalData,
}

enum class TextFieldThirdParty(
    val group: TextFieldGroupThirdPArty,
    val descriptionTextView: (FragmentSummitEntryThirdPartyBinding) -> TextView,
    val valueTextView: (FragmentSummitEntryThirdPartyBinding) -> TextView,
    val unit: String,
    val getValue: (Summit) -> Number?,
    val getMinMaxSummit: (ExtremaValuesSummits?) -> Pair<Summit, Summit>?,
    val reverse: Boolean = false,
    val toHHms: Boolean = false,
    val digits: Int = 1,
    val factor: Int = 1
) {


    AverageHr(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.averageHrText },
        { b -> b.averageHr },
        "bpm",
        { e -> e.garminData?.averageHR },
        { e -> e?.averageHRMinMax },
        digits = 0,
        reverse = true
    ),
    MaxHr(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.maxHrText },
        { b -> b.maxHr },
        "bpm",
        { e -> e.garminData?.maxHR },
        { e -> e?.maxHRMinMax },
        digits = 0,
        reverse = true
    ),
    Calories(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.caloriesText },
        { b -> b.calories },
        "kcal",
        { e -> e.garminData?.calories },
        { e -> e?.caloriesMinMax },
        digits = 0
    ),
    MaxPower(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.maxPowerText },
        { b -> b.maxPower },
        "W",
        { e -> e.garminData?.power?.maxPower },
        { e -> e?.maxPowerMinMax },
        digits = 0
    ),
    AveragePower(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.averagePowerText },
        { b -> b.averagePower },
        "W",
        { e -> e.garminData?.power?.avgPower },
        { e -> e?.averagePowerMinMax },
        digits = 0
    ),
    NormPower(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.normPowerText },
        { b -> b.normPower },
        "W",
        { e -> e.garminData?.power?.normPower },
        { e -> e?.normPowerMinMax },
        digits = 0
    ),
    AerobicTrainingEffect(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.aerobicTrainingEffectText },
        { b -> b.aerobicTrainingEffect },
        "",
        { e -> e.garminData?.aerobicTrainingEffect },
        { e -> e?.aerobicTrainingEffectMinMax },
    ),
    AnaerobicTrainingEffect(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.anaerobicTrainingEffectText },
        { b -> b.anaerobicTrainingEffect },
        "",
        { e -> e.garminData?.anaerobicTrainingEffect },
        { e -> e?.anaerobicTrainingEffectMinMax },
    ),
    Grit(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.gritText },
        { b -> b.grit },
        "",
        { e -> e.garminData?.grit },
        { e -> e?.gritMinMax },
    ),
    Flow(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.flowText },
        { b -> b.flow },
        "",
        { e -> e.garminData?.flow },
        { e -> e?.flowMinMax },
    ),
    TrainingsLoad(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.trainingLoadText },
        { b -> b.trainingLoad },
        "",
        { e -> e.garminData?.trainingLoad },
        { e -> e?.trainingsLoadMinMax },
    ),
    Vo2Max(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.vo2MaxText },
        { b -> b.vo2Max },
        "",
        { e -> e.garminData?.vo2max },
        { e -> e?.vo2maxMinMax },
        digits = 1
    ),
    FTP(
        TextFieldGroupThirdPArty.ThirdParty,
        { b -> b.FTPText },
        { b -> b.FTP },
        "",
        { e -> e.garminData?.ftp },
        { e -> e?.ftpMinMax },
        digits = 0
    ),

    Power1s(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power1secText },
        { b -> b.power1sec },
        "W",
        { e -> e.garminData?.power?.oneSec },
        { e -> e?.power1sMinMax },
        digits = 0
    ),
    Power2s(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power2secText },
        { b -> b.power2sec },
        "W",
        { e -> e.garminData?.power?.twoSec },
        { e -> e?.power2sMinMax },
        digits = 0
    ),
    Power5s(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power5secText },
        { b -> b.power5sec },
        "W",
        { e -> e.garminData?.power?.fiveSec },
        { e -> e?.power5sMinMax },
        digits = 0
    ),
    Power10s(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power10secText },
        { b -> b.power10sec },
        "W",
        { e -> e.garminData?.power?.tenSec },
        { e -> e?.power10sMinMax },
        digits = 0
    ),
    Power20s(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power20secText },
        { b -> b.power20sec },
        "W",
        { e -> e.garminData?.power?.twentySec },
        { e -> e?.power20sMinMax },
        digits = 0
    ),
    Power30s(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power30secText },
        { b -> b.power30sec },
        "W",
        { e -> e.garminData?.power?.thirtySec },
        { e -> e?.power30sMinMax },
        digits = 0
    ),
    Power1Min(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power1minText },
        { b -> b.power1min },
        "W",
        { e -> e.garminData?.power?.oneMin },
        { e -> e?.power1minMinMax },
        digits = 0
    ),
    Power2Min(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power2minText },
        { b -> b.power2min },
        "W",
        { e -> e.garminData?.power?.twoMin },
        { e -> e?.power2minMinMax },
        digits = 0
    ),
    Power5Min(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power5minText },
        { b -> b.power5min },
        "W",
        { e -> e.garminData?.power?.fiveMin },
        { e -> e?.power5minMinMax },
        digits = 0
    ),
    Power10Min(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power10minText },
        { b -> b.power10min },
        "W",
        { e -> e.garminData?.power?.tenMin },
        { e -> e?.power10minMinMax },
        digits = 0
    ),
    Power20Min(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power20minText },
        { b -> b.power20min },
        "W",
        { e -> e.garminData?.power?.twentyMin },
        { e -> e?.power20minMinMax },
        digits = 0
    ),
    Power30Min(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power30minText },
        { b -> b.power30min },
        "W",
        { e -> e.garminData?.power?.thirtyMin },
        { e -> e?.power30minMinMax },
        digits = 0
    ),
    Power1H(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power1hText },
        { b -> b.power1h },
        "W",
        { e -> e.garminData?.power?.oneHour },
        { e -> e?.power1hMinMax },
        digits = 0
    ),
    Power2H(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power2hText },
        { b -> b.power2h },
        "W",
        { e -> e.garminData?.power?.twoHours },
        { e -> e?.power2hMinMax },
        digits = 0
    ),
    Power5H(
        TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
        { b -> b.power5hText },
        { b -> b.power5h },
        "W",
        { e -> e.garminData?.power?.fiveHours },
        { e -> e?.power5hMinMax },
        digits = 0
    ),

}
