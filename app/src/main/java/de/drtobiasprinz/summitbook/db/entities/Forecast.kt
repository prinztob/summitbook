package de.drtobiasprinz.summitbook.db.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import de.drtobiasprinz.summitbook.ui.dialog.ForecastDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

@Entity
data class Forecast(
    var year: Int,
    var month: Int,
    var forecastHeightMeter: Int,
    var forecastDistance: Int,
    var forecastNumberActivities: Int,
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
        actualHeightMeter = summitsInMonth.sumOf {
            if (it.sportType != SportType.IndoorTrainer) {
                it.elevationData.elevationGain
            } else {
                it.elevationData.elevationGain * indoorHeightMeterPercent / 100
            }
        }
        actualDistance = summitsInMonth.sumOf { it.kilometers.roundToInt() }
        actualNumberActivities = summitsInMonth.size
    }

    fun getDate(): Date? {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        return df.parse("$year-${String.format(Locale.getDefault(), "%02d", month)}-15")
    }

    fun getStringRepresentation(): String {
        return "$year;$month;$forecastHeightMeter;$forecastDistance;$forecastNumberActivities\n"
    }

    fun alreadyExists(forecasts: List<Forecast>): Boolean {
        return forecasts.any { it.year == this.year && it.month == this.month }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Forecast

        if (year != other.year) return false
        if (month != other.month) return false
        if (forecastHeightMeter != other.forecastHeightMeter) return false
        if (forecastDistance != other.forecastDistance) return false
        if (forecastNumberActivities != other.forecastNumberActivities) return false
        if (id != other.id) return false
        if (actualHeightMeter != other.actualHeightMeter) return false
        if (actualDistance != other.actualDistance) return false
        if (actualNumberActivities != other.actualNumberActivities) return false

        return true
    }

    fun equalsWithOutId(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Forecast

        if (year != other.year) return false
        if (month != other.month) return false
        if (forecastHeightMeter != other.forecastHeightMeter) return false
        if (forecastDistance != other.forecastDistance) return false
        if (forecastNumberActivities != other.forecastNumberActivities) return false
        if (actualHeightMeter != other.actualHeightMeter) return false
        if (actualDistance != other.actualDistance) return false
        if (actualNumberActivities != other.actualNumberActivities) return false

        return true
    }

    override fun hashCode(): Int {
        var result = year
        result = 31 * result + month
        result = 31 * result + forecastHeightMeter
        result = 31 * result + forecastDistance
        result = 31 * result + forecastNumberActivities
        result = 31 * result + id.hashCode()
        result = 31 * result + actualHeightMeter
        result = 31 * result + actualDistance
        result = 31 * result + actualNumberActivities
        return result
    }

    companion object {
        private const val NUMBER_OF_ELEMENTS = 5

        fun getNewForecastFrom(
            month: Int,
            year: Int,
            summits: List<Summit>?,
            averageOfLastXYears: Int,
            annualTargetActivity: String,
            annualTargetKm: String,
            annualTargetHm: String,
            today: Date = Date()
        ): Forecast {

            val summitsInTimeRange = if (averageOfLastXYears > 0 && summits?.isNotEmpty() == true) {
                summits.filter {
                    val cal = Calendar.getInstance()
                    cal.time = it.date
                    cal.get(Calendar.MONTH) == month - 1 && (today.time - it.date.time) / 31536000000.0 in 0.0..averageOfLastXYears.toDouble()
                }
            } else emptyList()
            val hmForecast = if (summitsInTimeRange.isNotEmpty()) {
                summitsInTimeRange.sumOf { it.elevationData.elevationGain }
                    .toDouble() / averageOfLastXYears
            } else annualTargetHm.toInt() / 12.0
            val kmForecast = if (summitsInTimeRange.isNotEmpty()) {
                summitsInTimeRange.sumOf { it.kilometers } / averageOfLastXYears
            } else annualTargetKm.toInt() / 12.0
            val numberActivitiesForecast = if (summitsInTimeRange.isNotEmpty()) {
                summitsInTimeRange.size.toDouble() / averageOfLastXYears
            } else annualTargetActivity.toInt() / 12.0
            val forecast = Forecast(
                year, month,
                (floor((hmForecast) / ForecastDialog.STEP_SIZE_HM) * ForecastDialog.STEP_SIZE_HM).toInt(),
                (floor((kmForecast) / ForecastDialog.STEP_SIZE_KM) * ForecastDialog.STEP_SIZE_KM).toInt(),
                (floor((numberActivitiesForecast) / ForecastDialog.STEP_SIZE_ACTIVITY) * ForecastDialog.STEP_SIZE_ACTIVITY).toInt()
            )
            return forecast
        }

        fun getSumForYear(
            year: Int,
            forecasts: List<Forecast>,
            selectedSegmentedForecastProperty: Int,
            currentYear: Int,
            currentMonth: Int
        ): Int {
            var sum = 0
            forecasts.forEach {
                if (it.year == year) {
                    sum += when (selectedSegmentedForecastProperty) {
                        1 -> getSumPerYear(
                            it,
                            { e -> e.forecastDistance },
                            { e -> e.actualDistance },
                            currentYear,
                            currentMonth
                        )

                        2 -> getSumPerYear(
                            it,
                            { e -> e.forecastNumberActivities },
                            { e -> e.actualNumberActivities },
                            currentYear,
                            currentMonth
                        )

                        else -> getSumPerYear(
                            it,
                            { e -> e.forecastHeightMeter },
                            { e -> e.actualHeightMeter },
                            currentYear,
                            currentMonth
                        )
                    }
                }
            }
            return sum
        }

        private fun getSumPerYear(
            forecast: Forecast,
            getForecast: (Forecast) -> Int,
            getActual: (Forecast) -> Int,
            currentYear: Int,
            currentMonth: Int
        ): Int {
            return if (forecast.year == currentYear) {
                if (forecast.month < currentMonth) {
                    getActual(forecast)
                } else if (forecast.month == currentMonth) {
                    if (getForecast(forecast) < getActual(forecast)) getActual(forecast) else getForecast(
                        forecast
                    )
                } else {
                    getForecast(forecast)
                }
            } else {
                getForecast(forecast)
            }
        }

        private fun checkValidNumberOfElements(splitLine: List<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS) {
                throw Exception(
                    "Line ${splitLine.joinToString { ";" }} has ${splitLine.size} number " +
                            "of elements. Expected are ${Segment.NUMBER_OF_ELEMENTS}"
                )
            }
        }

        fun parseFromCsvFileLine(
            line: String,
            forecasts: MutableList<Forecast>,
            saveForecast: (Forecast) -> Unit
        ): Boolean {
            val splitLine = line.split(";")
            checkValidNumberOfElements(splitLine)
            val forecast = Forecast(
                splitLine[0].toInt(),
                splitLine[1].toInt(),
                splitLine[2].toInt(),
                splitLine[3].toInt(),
                splitLine[4].toInt()
            )
            return if (forecast.alreadyExists(forecasts)) {
                false
            } else {
                saveForecast(forecast)
                forecasts.add(forecast)
                true
            }
        }

        fun getCsvHeadline(): String {
            return "Year;Month;Height Meter;Distance;Activities\n"
        }
    }
}