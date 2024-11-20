package de.drtobiasprinz.summitbook.models

import android.widget.TextView
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryDataBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class TextFieldGroup {
    Base,
    AdditionalSpeedData,
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
    TopVerticalVelocityDown1Min(
        TextFieldGroup.Base,
        { b -> b.topVerticalVelocityDown1MinText },
        { b -> b.topVerticalVelocityDown1Min },
        "m",
        { e -> e.elevationData.maxVerticalVelocityDown1Min },
        { e -> e?.topVerticalVelocityDown1MinMinMax },
        factor = 60,
        digits = 0
    ),
    TopVerticalVelocityDown10Min(
        TextFieldGroup.Base,
        { b -> b.topVerticalVelocityDown10MinText },
        { b -> b.topVerticalVelocityDown10Min },
        "m",
        { e -> e.elevationData.maxVerticalVelocityDown10Min },
        { e -> e?.topVerticalVelocityDown10MinMinMax },
        factor = 600,
        digits = 0
    ),
    TopVerticalVelocityDown1H(
        TextFieldGroup.Base,
        { b -> b.topVerticalVelocityDown1hText },
        { b -> b.topVerticalVelocityDown1h },
        "m",
        { e -> e.elevationData.maxVerticalVelocityDown1h },
        { e -> e?.topVerticalVelocityDown1hMinMax },
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
        { e -> e.getAverageVelocity() },
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
        { e -> e?.durationMinMax },
        toHHms=true
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

}
