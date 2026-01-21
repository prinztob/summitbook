package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class TextFieldGroupThirdParty {
    ThirdParty,
    ThirdPartyAdditionalData,
}

enum class TextFieldThirdParty(
    val group: TextFieldGroupThirdParty,
    val unitWithPlaceHolder: Int,
    val getValue: (Summit) -> Number?,
    val getMinMaxSummit: (ExtremaValuesSummits?) -> Pair<Summit, Summit>? = { _ -> null },
    val reverse: Boolean = false,
    val toMinSec: Boolean = false,
    val digits: Int = 1,
    val factor: Int = 1,
    val getValueRange: (Summit) -> Number? = { _ -> null },
) {
    AverageHr(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_with_bpm,
        { e -> e.garminData?.averageHR },
        { e -> e?.averageHRMinMax },
        digits = 0,
        reverse = true
    ),
    MaxHr(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_with_bpm,
        { e -> e.garminData?.maxHR },
        { e -> e?.maxHRMinMax },
        digits = 0,
        reverse = true
    ),
    Calories(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_with_kcal,
        { e -> e.garminData?.calories },
        { e -> e?.caloriesMinMax },
        digits = 0
    ),
    PartPaved(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_with_per_cent,
        { e -> e.garminData?.surfaceTypeUnpavedPercentage },
        { e -> e?.surfaceTypeUnpavedPercentageMinMax },
        digits = 1,
    ),
    AerobicTrainingEffect(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.aerobicTrainingEffect },
        { e -> e?.aerobicTrainingEffectMinMax },
    ),
    AnaerobicTrainingEffect(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.anaerobicTrainingEffect },
        { e -> e?.anaerobicTrainingEffectMinMax },
    ),
    Grit(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.grit },
        { e -> e?.gritMinMax },
    ),
    Flow(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.flow },
        { e -> e?.flowMinMax },
    ),
    TrainingsLoad(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.trainingLoad },
        { e -> e?.trainingsLoadMinMax },
    ),
    Vo2Max(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.vo2max },
        { e -> e?.vo2maxMinMax },
        digits = 1
    ),
    Strokes(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.cyclingDynamics?.totalNumberOfStrokes },
        { e -> e?.strokesMinMax },
        digits = 0
    ),
    StandingTime(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.cyclingDynamics?.standingTime },
        { e -> e?.standingTimeMinMax },
        digits = 0,
        toMinSec = true
    ),
    StandingAvgPower(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.cyclingDynamics?.averageStandingPower },
        { e -> e?.standingAvgPowerMinMax },
        digits = 0
    ),
    StandingMaxPower(
        TextFieldGroupThirdParty.ThirdParty,
        R.string.value_only,
        { e -> e.garminData?.cyclingDynamics?.maxStandingPower },
        { e -> e?.standingMaxPowerMinMax },
        digits = 0
    ),
    PedalSmoothness(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_per_cent,
        { e -> e.garminData?.cyclingDynamics?.leftPedalSmoothness },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.rightPedalSmoothness },
    ),
    Balance(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_per_cent,
        { e -> e.garminData?.cyclingDynamics?.leftBalance },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.rightBalance },
    ),
    TorqueEffectiveness(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_per_cent,
        { e -> e.garminData?.cyclingDynamics?.leftTorqueEffectiveness },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.rightTorqueEffectiveness },
    ),
    LeftPowerPhase(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_degree,
        { e -> e.garminData?.cyclingDynamics?.leftPowerPhaseStart },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.leftPowerPhaseEnd },
    ),
    RightPowerPhase(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_degree,
        { e -> e.garminData?.cyclingDynamics?.rightPowerPhaseStart },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.rightPowerPhaseEnd },
    ),
    LeftPeakPowerPhase(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_degree,
        { e -> e.garminData?.cyclingDynamics?.leftPowerPhasePeakStart },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.leftPowerPhasePeakEnd },
    ),
    RightPeakPowerPhase(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_degree,
        { e -> e.garminData?.cyclingDynamics?.rightPowerPhasePeakStart },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.rightPowerPhasePeakEnd },
    ),
    PlatformCenterOffset(
        TextFieldGroupThirdParty.ThirdPartyAdditionalData,
        R.string.value_with_millimeter,
        { e -> e.garminData?.cyclingDynamics?.leftPlatformCenterOffset },
        digits = 1,
        getValueRange =  { e -> e.garminData?.cyclingDynamics?.rightPlatformCenterOffset },
    ),
}
