package de.drtobiasprinz.summitbook.ui.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.lifecycle.asFlow
import androidx.room.Room
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

class SummitBookGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val widgetData by produceState(WidgetData()) {
                value = loadWidgetData(context)
            }

            SummitBookWidgetContent(widgetData)
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData = withContext(Dispatchers.IO) {
        try {
            val dao = Room.databaseBuilder(
                context, AppDatabase::class.java, Constants.DATABASE
            ).build()
            val repository = DatabaseRepository(
                dao.summitsDao(),
                dao.segmentsDao(),
                dao.forecastDao(),
                dao.ignoredActivityDao(),
                dao.entityEventDao(),
                dao.peakDao(),
                dao.dailyActivitySummaryDao()
            )
            val summits = repository.getAllSummitsLiveData().asFlow().first()
            val forecasts = repository.getAllForecasts().first()


            // Check if we have forecasts for current year
            val calendar = Calendar.getInstance()
            val currentYear: Int = calendar[Calendar.YEAR]

            if (forecasts.isNotEmpty() && forecasts.any { it.year == currentYear }) {
                createWidgetDataFromForecasts(forecasts, summits, context)
            } else {
                createWidgetDataFromStatistics(summits, context)
            }
        } catch (_: Exception) {
            // Return default data in case of error
            WidgetData()
        }
    }

    private fun createWidgetDataFromForecasts(
        forecasts: List<de.drtobiasprinz.summitbook.db.entities.Forecast>,
        summits: List<de.drtobiasprinz.summitbook.db.entities.Summit>,
        context: Context
    ): WidgetData {
        val calendar = Calendar.getInstance()
        val currentYear: Int = calendar[Calendar.YEAR]
        val currentMonth: Int = calendar[Calendar.MONTH] + 1
        val currentDay: Int = calendar[Calendar.DAY_OF_MONTH]
        val dayOfMonthPercentage =
            currentDay.toDouble() / calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val forecastForCurrentMonth = forecasts.firstOrNull {
            it.month == currentMonth && it.year == currentYear
        }

        val indoorHeightMeterPercent =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("pref_indoor_height_meter", 0)

        val summitsForCurrentMonth = summits.filter {
            val calForSummit = Calendar.getInstance()
            calForSummit.time = it.date
            calendar[Calendar.MONTH] == calForSummit[Calendar.MONTH] &&
                    calendar[Calendar.YEAR] == calForSummit[Calendar.YEAR]
        }

        val forecastForCurrentYear = forecasts.filter {
            it.month < currentMonth && it.year == currentYear
        }

        val summitsForCurrentYear = summits.filter {
            val calForSummit = Calendar.getInstance()
            calForSummit.time = it.date
            calendar[Calendar.YEAR] == calForSummit[Calendar.YEAR]
        }

        val distanceForecastForCurrentMonth = (forecastForCurrentMonth?.forecastNumberActivities
            ?: 0) * dayOfMonthPercentage
        val heightMeterForecastForCurrentMonth = (forecastForCurrentMonth?.forecastHeightMeter
            ?: 0) * dayOfMonthPercentage
        val kilometerForecastForCurrentMonth = (forecastForCurrentMonth?.forecastDistance
            ?: 0) * dayOfMonthPercentage

        // Calculate actual values with indoor height meter adjustment
        val actualHeightMeterMonthly = summitsForCurrentMonth.sumOf {
            if (it.sportType == de.drtobiasprinz.summitbook.db.entities.SportType.IndoorTrainer) {
                it.elevationData.elevationGain * indoorHeightMeterPercent / 100
            } else {
                it.elevationData.elevationGain
            }
        }

        val actualHeightMeterYearly = summitsForCurrentYear.sumOf {
            if (it.sportType == de.drtobiasprinz.summitbook.db.entities.SportType.IndoorTrainer) {
                it.elevationData.elevationGain * indoorHeightMeterPercent / 100
            } else {
                it.elevationData.elevationGain
            }
        }

        val actualKilometersMonthly = summitsForCurrentMonth.sumOf { it.kilometers }.roundToInt()
        val actualKilometersYearly = summitsForCurrentYear.sumOf { it.kilometers }.roundToInt()

        return WidgetData(
            yearlyStats = YearlyStats(
                activities = StatItem(
                    actualValue = summitsForCurrentYear.count(),
                    expectedValue = (forecastForCurrentYear.sumOf { it.forecastNumberActivities } +
                            distanceForecastForCurrentMonth).roundToInt(),
                    isAchieved = summitsForCurrentYear.count() >= (
                            forecastForCurrentYear.sumOf { it.forecastNumberActivities } +
                                    distanceForecastForCurrentMonth).roundToInt()
                ),
                heightMeter = StatItem(
                    actualValue = actualHeightMeterYearly,
                    expectedValue = (forecastForCurrentYear.sumOf { it.forecastHeightMeter } +
                            heightMeterForecastForCurrentMonth).roundToInt(),
                    unit = "hm",
                    isAchieved = actualHeightMeterYearly >= (
                            forecastForCurrentYear.sumOf { it.forecastHeightMeter } +
                                    heightMeterForecastForCurrentMonth).roundToInt()
                ),
                kilometers = StatItem(
                    actualValue = actualKilometersYearly,
                    expectedValue = (forecastForCurrentYear.sumOf { it.forecastDistance } +
                            kilometerForecastForCurrentMonth).roundToInt(),
                    unit = "km",
                    isAchieved = actualKilometersYearly >= (
                            forecastForCurrentYear.sumOf { it.forecastDistance } +
                                    kilometerForecastForCurrentMonth).roundToInt()
                )
            ),
            monthlyStats = MonthlyStats(
                activities = StatItem(
                    actualValue = summitsForCurrentMonth.count(),
                    expectedValue = distanceForecastForCurrentMonth.roundToInt(),
                    isAchieved = summitsForCurrentMonth.count() >= distanceForecastForCurrentMonth.roundToInt()
                ),
                heightMeter = StatItem(
                    actualValue = actualHeightMeterMonthly,
                    expectedValue = heightMeterForecastForCurrentMonth.roundToInt(),
                    unit = "hm",
                    isAchieved = actualHeightMeterMonthly >= heightMeterForecastForCurrentMonth.roundToInt()
                ),
                kilometers = StatItem(
                    actualValue = actualKilometersMonthly,
                    expectedValue = kilometerForecastForCurrentMonth.roundToInt(),
                    unit = "km",
                    isAchieved = actualKilometersMonthly >= kilometerForecastForCurrentMonth.roundToInt()
                )
            )
        )
    }

    private fun createWidgetDataFromStatistics(
        entries: List<de.drtobiasprinz.summitbook.db.entities.Summit>,
        context: Context
    ): WidgetData {
        val sharedPreferences =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val annualTargetActivity =
            sharedPreferences.getString("pref_annual_target_activities", "52")?.toInt() ?: 52
        val annualTargetKm =
            sharedPreferences.getString("pref_annual_target_km", "1200")?.toInt() ?: 1200
        val annualTargetHm =
            sharedPreferences.getString("pref_annual_target", "50000")?.toInt() ?: 50000
        val indoorHeightMeterPercent =
            sharedPreferences.getInt("pref_indoor_height_meter", 0)

        val statisticEntry = de.drtobiasprinz.summitbook.models.StatisticEntry(
            filterByDate(entries),
            annualTargetActivity,
            annualTargetKm,
            annualTargetHm,
            indoorHeightMeterPercent
        )
        statisticEntry.calculate()
        statisticEntry.setExpectedAchievement()

        return WidgetData(
            yearlyStats = YearlyStats(
                activities = StatItem(
                    actualValue = statisticEntry.getTotalActivities(),
                    expectedValue = statisticEntry.expectedAchievementActivityAbsolute.roundToInt(),
                    isAchieved = statisticEntry.getTotalActivities() >= statisticEntry.expectedAchievementActivityAbsolute
                ),
                heightMeter = StatItem(
                    actualValue = statisticEntry.totalHm,
                    expectedValue = statisticEntry.expectedAchievementHmAbsolute.roundToInt(),
                    unit = "hm",
                    isAchieved = statisticEntry.totalHm >= statisticEntry.expectedAchievementHmAbsolute
                ),
                kilometers = StatItem(
                    actualValue = statisticEntry.totalKm.roundToInt(),
                    expectedValue = statisticEntry.expectedAchievementKmAbsolute.roundToInt(),
                    unit = "km",
                    isAchieved = statisticEntry.totalKm >= statisticEntry.expectedAchievementKmAbsolute
                )
            )
        )
    }

    private fun filterByDate(allEntries: List<de.drtobiasprinz.summitbook.db.entities.Summit>): List<de.drtobiasprinz.summitbook.db.entities.Summit> {
        val now = Calendar.getInstance()
        val year = now[Calendar.YEAR]
        val startDate = de.drtobiasprinz.summitbook.db.entities.Summit.parseDate(
            String.format(
                "%s-12-31",
                (year - 1).toString()
            )
        )
        val endDate = de.drtobiasprinz.summitbook.db.entities.Summit.parseDate(
            String.format(
                "%s-12-31",
                year.toString()
            )
        )
        val entries = ArrayList<de.drtobiasprinz.summitbook.db.entities.Summit>()
        for (entry in allEntries) {
            if (entry.date.after(startDate) && entry.date.before(endDate)) {
                entries.add(entry)
            }
        }
        return entries
    }
}