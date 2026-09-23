package de.drtobiasprinz.summitbook.data.model

import android.graphics.Color
import de.drtobiasprinz.summitbook.R
import io.ticofab.androidgpxparser.parser.domain.TrackPoint

enum class TrackColor(
    val nameId: Int,
    val unit: String,
    val labelId: Int,
    val minColor: Int = Color.BLUE,
    val maxColor: Int = Color.RED,
    val discreteInput: Boolean = false,
    var f: (Pair<TrackPoint, ExtensionFromYaml>) -> Double?
) {
    None(
        R.string.none, "", R.string.none, f = { e -> e.first.latitude + e.first.longitude }),
    Mileage(R.string.mileage, "km", R.string.mileage, f = { e ->
        e.second.distance
    }),
    Elevation(
        R.string.height_meter_hint,
        "hm",
        R.string.height_meter_hint,
        f = { e -> e.first.elevation }),
    Cadence(R.string.cadence, "", R.string.cadence_profile_label, f = { e ->
        e.second.cadence?.toDouble()
    }),
    HeartRate(R.string.heart_rate, "bpm", R.string.heart_rate_profile_label, f = { e ->
        e.second.hr?.toDouble()
    }),
    Power(R.string.power, "W", R.string.power_profile_label, f = { e ->
        e.second.power?.toDouble()
    }),
    Power1Min(R.string.power_1min, "W", R.string.power_profile_label, f = { e ->
        e.second.power60s?.toDouble()
    }),
    Speed(R.string.speed, "km/h", R.string.speed_profile_label, f = { e ->
        (e.second.speed ?: 0.0) * 3.6
    }),
    Slope(R.string.slope, "%", R.string.slope_profile_label, f = { e ->
        e.second.slope
    }),
    VerticalSpeedUp(
        R.string.vertical_speed_up, "m/min", R.string.vertical_speed_profile_label, f = { e ->
            val v = e.second.verticalVelocity ?: 0.0
            if (v > 0) v.times(60) else 0.0
        }),
    VerticalSpeeddDown(
        R.string.vertical_speed_down, "m/min", R.string.vertical_speed_profile_label, f = { e ->
            val v = e.second.verticalVelocity ?: 0.0
            if (v < 0) v.times(60) else 0.0
        }),

    RoadSurface(
        R.string.road_surface, "", R.string.road_surface, discreteInput = true, f = { e ->
            e.second.surface.number
        }),
    RoadType(R.string.road_type, "", R.string.road_type, discreteInput = true, f = { e ->
        e.second.roadType.number
    });


    override fun toString(): String {
        return name
    }

}
