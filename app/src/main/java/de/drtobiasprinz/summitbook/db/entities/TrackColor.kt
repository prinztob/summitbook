package de.drtobiasprinz.summitbook.db.entities

import android.graphics.Color
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.R

enum class TrackColor(val nameId: Int, var spinnerId: Int, val unit: String, val labelId: Int,
                      val digits: Int = 0, val minColor: Int = Color.BLUE, val maxColor: Int = Color.RED,
                      var f: (TrackPoint) -> Double?) {
    None(R.string.none, 0, "", R.string.none, f = { e -> e.lat + e.lon }),
    Mileage(R.string.mileage, 1, "km", R.string.mileage, f = { e -> e.extension?.distance }),
    Elevation(R.string.height_meter_hint, 2, "hm", R.string.height_meter_hint, f = { e -> e.ele }),
    Cadence(R.string.cadence, 3, "", R.string.cadence_profile_label, f = { e -> e.extension?.cadence?.toDouble() }),
    HeartRate(R.string.heart_rate, 4, "bpm", R.string.heart_rate_profile_label, f = { e -> e.extension?.heartRate?.toDouble() }),
    Power(R.string.power, 5, "W", R.string.power_profile_label, f = { e -> e.extension?.power?.toDouble() }),
    Speed(R.string.speed, 6, "km/h", R.string.speed_profile_label, digits = 1, f = { e -> (e.extension?.speed?: 0.0) * 3.6 }),
    Slope(R.string.slope, 7, "%", R.string.slope_profile_label, digits = 1, f = { e -> e.extension?.slope }),
    VerticalSpeed(R.string.vertical_speed, 8, "m/min", R.string.vertical_speed_profile_label, f = { e -> e.extension?.verticalVelocity?.times(60) });

    override fun toString(): String {
        return name
    }

}