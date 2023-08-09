package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import java.util.stream.Stream

enum class BarChartYAxisSelector(
    val nameId: Int,
    val unitId: Int,
    val sharedPreferenceKey: String,
    val defaultAnnualTarget: Int,
    val f: (Stream<Summit?>?, Int) -> Float,
    val getForecastValue: (Forecast) -> Float
) {
    Count(R.string.count, R.string.empty, "annual_target_activities", 52, { stream, _ ->
        stream?.count()?.toFloat() ?: 0f
    }, { forecast -> forecast.forecastNumberActivities.toFloat() }),
    Kilometers(R.string.kilometers_hint, R.string.km, "annual_target_km", 1200, { stream, _ ->
        stream
            ?.mapToInt { o: Summit? -> o?.kilometers?.toInt() ?: 0 }
            ?.sum()?.toFloat() ?: 0.0f
    }, { forecast -> forecast.forecastDistance.toFloat() }),
    ElevationGain(
        R.string.height_meter_hint,
        R.string.hm,
        "annual_target",
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
        { forecast -> forecast.forecastHeightMeter.toFloat() })

}