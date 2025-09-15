package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivity
import java.util.stream.Stream

enum class BarChartYAxisSelector(
    val nameId: Int,
    val unitId: Int,
    val sharedPreferenceKey: String?,
    val defaultAnnualTarget: Int,
    val f: (Stream<Summit?>?, Int) -> Float,
    val getForecastValue: (Forecast) -> Float?
) {
    TotalActivities(R.string.total_activities, R.string.empty, Keys.PREF_ANNUAL_TARGET_ACTIVITIES, 52, { stream, _ ->
        stream?.count()?.toFloat() ?: 0f
    }, { forecast -> forecast.forecastNumberActivities.toFloat() }),
    TotalSummits(R.string.total_summits, R.string.empty, Keys.PREF_ANNUAL_TARGET_SUMMITS, 10, { stream, _ ->
        val peaks = mutableListOf<String>()
        stream?.forEach {
            peaks.addAll(it?.places?.filter { place -> place in MainActivity.peaks.map { peak -> peak.name } } ?: emptyList())
            if (it?.isPeak == true) {
                peaks.add(it.name)
            }
        }
        peaks.count().toFloat()
    },{ _ -> null }),
    Kilometers(R.string.kilometers_hint, R.string.km, Keys.PREF_ANNUAL_TARGET_KM, 1200, { stream, _ ->
        stream
            ?.mapToDouble { o: Summit? -> o?.kilometers ?: 0.0 }
            ?.sum()?.toFloat() ?: 0.0f
    }, { forecast -> forecast.forecastDistance.toFloat() }),
    ElevationGain(
        R.string.height_meter_hint,
        R.string.hm,
        Keys.PREF_ANNUAL_TARGET,
        50000,
        { stream, indoorHeightMeterPercent ->
            stream
                ?.mapToInt {
                    if (it?.sportType == SportType.IndoorTrainer) {
                        it.elevationData.elevationGain * indoorHeightMeterPercent / 100
                    } else {
                        it?.elevationData?.elevationGain ?: 0
                    }
                }
                ?.sum()?.toFloat() ?: 0.0f
        },
        { forecast -> forecast.forecastHeightMeter.toFloat() }),
    Duration(R.string.duration, R.string.h, null, 0, { stream, _ ->
        (stream
            ?.mapToInt { o: Summit? -> (o?.duration ?: 0) }
            ?.sum()?.toFloat() ?: 0.0f) / 3600f
    }, { _ -> null }),
}