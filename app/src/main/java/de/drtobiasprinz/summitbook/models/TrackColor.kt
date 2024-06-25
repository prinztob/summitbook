package de.drtobiasprinz.summitbook.models

import android.graphics.Color
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.R

enum class TrackColor(val nameId: Int, var spinnerId: Int, val unit: String, val labelId: Int,
                      val digits: Int = 0, val minColor: Int = Color.BLUE, val maxColor: Int = Color.RED,
                      var f: (TrackPoint) -> Double?) {
    None(R.string.none, 0, "", R.string.none, f = { e -> e.latitude + e.longitude }),
    Mileage(R.string.mileage, 1, "km", R.string.mileage, f = { e -> e.pointExtension?.distance }),
    Elevation(R.string.height_meter_hint, 2, "hm", R.string.height_meter_hint, f = { e -> e.elevation }),
    Cadence(R.string.cadence, 3, "", R.string.cadence_profile_label, f = { e -> e.pointExtension?.cadence?.toDouble() }),
    HeartRate(R.string.heart_rate, 4, "bpm", R.string.heart_rate_profile_label, f = { e -> e.pointExtension?.heartRate?.toDouble() }),
    Power(R.string.power, 5, "W", R.string.power_profile_label, f = { e -> e.pointExtension?.power?.toDouble() }),
    Speed(R.string.speed, 6, "km/h", R.string.speed_profile_label, digits = 1, f = { e -> (e.pointExtension?.speed?: 0.0) * 3.6 }),
    Slope(R.string.slope, 7, "%", R.string.slope_profile_label, digits = 1, f = { e -> e.pointExtension?.slope }),
    VerticalSpeed(R.string.vertical_speed, 8, "m/min", R.string.vertical_speed_profile_label, f = { e -> e.pointExtension?.verticalVelocity?.times(60) });

    override fun toString(): String {
        return name
    }

}