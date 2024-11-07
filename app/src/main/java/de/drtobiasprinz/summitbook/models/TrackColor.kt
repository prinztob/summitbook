package de.drtobiasprinz.summitbook.models

import android.graphics.Color
import de.drtobiasprinz.summitbook.R
import io.ticofab.androidgpxparser.parser.domain.TrackPoint

enum class TrackColor(
    val nameId: Int,
    var spinnerId: Int,
    val unit: String,
    val labelId: Int,
    val digits: Int = 0,
    val minColor: Int = Color.BLUE,
    val maxColor: Int = Color.RED,
    var f: (Pair<TrackPoint, ExtensionFromYaml>) -> Double?
) {
    None(R.string.none,
        0,
        "",
        R.string.none,
        f = { e -> e.first.latitude + e.first.longitude }),
    Mileage(R.string.mileage, 1, "km", R.string.mileage, f = { e ->
        e.second.distance
    }),
    Elevation(R.string.height_meter_hint,
        2,
        "hm",
        R.string.height_meter_hint,
        f = { e -> e.first.elevation }),
    Cadence(R.string.cadence, 3, "", R.string.cadence_profile_label, f = { e ->
        e.second.cadence?.toDouble()
    }),
    HeartRate(R.string.heart_rate, 4, "bpm", R.string.heart_rate_profile_label, f = { e ->
        e.second.hr?.toDouble()
    }),
    Power(R.string.power, 5, "W", R.string.power_profile_label, f = { e ->
        e.second.power?.toDouble()
    }),
    Power1Min(R.string.power_1min, 5, "W", R.string.power_profile_label, f = { e ->
        e.second.power60s?.toDouble()
    }),
    Speed(R.string.speed, 6, "km/h", R.string.speed_profile_label, digits = 1, f = { e ->
        (e.second.speed ?: 0.0) * 3.6
    }),
    Slope(R.string.slope, 7, "%", R.string.slope_profile_label, digits = 1, f = { e ->
        e.second.slope
    }),
    VerticalSpeed(
        R.string.vertical_speed,
        8,
        "m/min",
        R.string.vertical_speed_profile_label,
        f = { e ->
            e.second.verticalVelocity?.times(60)
        });

    override fun toString(): String {
        return name
    }

}