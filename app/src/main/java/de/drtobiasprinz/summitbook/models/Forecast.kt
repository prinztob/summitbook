package de.drtobiasprinz.summitbook.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*
import kotlin.math.roundToInt

@Entity
class Forecast(
        var year: Int, var month: Int, var forecastHeightMeter: Int, var forecastDistance: Int, var forecastNumberActivities: Int,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @Ignore
    var actualHeightMeter = 0

    @Ignore
    var actualDistance = 0

    @Ignore
    var actualNumberActivities = 0

    fun setActual(summits: List<Summit>, indoorHeightMeterPercent: Int = 0) {
        val cal: Calendar = Calendar.getInstance()
        val summitsInMonth = summits.filter {
            cal.time = it.date
            cal.get(Calendar.MONTH) + 1 == month && cal.get(Calendar.YEAR) == year
        }
        actualHeightMeter = summitsInMonth.sumBy {
            if (it.sportType == SportType.IndoorTrainer) {
                it.elevationData.elevationGain
            } else {
                it.elevationData.elevationGain * indoorHeightMeterPercent / 100
            }
        }
        actualDistance = summitsInMonth.sumBy { it.kilometers.roundToInt() }
        actualNumberActivities = summitsInMonth.size
    }

    companion object {
        fun getSumForYear(year: Int, forecasts: List<Forecast>, selectedSegmentedForecastProperty: Int, currentYear: Int, currentMonth: Int): Int {
            var sum = 0
            forecasts.forEach {
                if (it.year == year) {
                    when (selectedSegmentedForecastProperty) {
                        1 -> sum += getSumPerYear(it, { e -> e.forecastDistance }, { e -> e.actualDistance }, currentYear, currentMonth)
                        2 -> sum += getSumPerYear(it, { e -> e.forecastNumberActivities }, { e -> e.actualNumberActivities }, currentYear, currentMonth)
                        else -> sum += getSumPerYear(it, { e -> e.forecastHeightMeter }, { e -> e.actualHeightMeter }, currentYear, currentMonth)
                    }
                }
            }
            return sum
        }

        private fun getSumPerYear(forecast: Forecast, getForecast: (Forecast) -> Int, getActual: (Forecast) -> Int, currentYear: Int, currentMonth: Int): Int {
            if (forecast.year == currentYear) {
                if (forecast.month < currentMonth) {
                    return getActual(forecast)
                } else if (forecast.month == currentMonth) {
                    if (getForecast(forecast) < getActual(forecast)) return getActual(forecast) else return getForecast(forecast)
                } else {
                    return getForecast(forecast)
                }
            } else {
                return getForecast(forecast)
            }
        }

    }
}