package de.drtobiasprinz.summitbook.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.room.Room
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.ui.GraphType
import de.drtobiasprinz.summitbook.ui.PerformanceGraphProvider
import de.drtobiasprinz.summitbook.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
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
            val summits = repository.getAllSummits().first()
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

    /**
     * Generate a bitmap of the yearly overview chart
     */
    private fun generateYearlyChartBitmap(
        summits: List<Summit>,
        forecasts: List<Forecast>,
        indoorHeightMeterPercent: Int
    ): Bitmap? {
        return try {
            val calendar = Calendar.getInstance()
            val currentYear = calendar[Calendar.YEAR].toString()
            val currentDate = Date()

            val performanceGraphProvider = PerformanceGraphProvider(
                summits,
                forecasts,
                indoorHeightMeterPercent
            )

            // Get actual data for the current year (only until today)
            val actualEntries = performanceGraphProvider.getActualGraphForSummits(
                GraphType.ElevationGain,
                currentYear,
                month = null,
                currentDate = currentDate
            )

            // Get forecast data
            val forecastEntries = performanceGraphProvider.getForecastGraphForSummits(
                GraphType.ElevationGain,
                currentYear,
                month = null
            )

            // Get min/max data from previous 5 years
            val minMaxData = performanceGraphProvider.getActualGraphMinMaxForSummits(
                GraphType.ElevationGain,
                currentYear,
                month = null
            )
            val minEntries = minMaxData.first
            val maxEntries = minMaxData.second

            if (actualEntries.isEmpty() && minEntries.isEmpty() && forecastEntries.isEmpty()) {
                return null
            }

            // Chart dimensions
            val width = 400
            val height = 200
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            // Background
            val backgroundColor = "#1E1E1E".toColorInt()
            canvas.drawColor(backgroundColor)

            // Calculate bounds - include all data for proper scaling
            val allEntries = actualEntries + maxEntries
            val minX = 1f
            val maxX = 366f // Days in a year
            val minY = 0f
            val maxY = allEntries.maxOfOrNull { it.y } ?: 1f

            val padding = 20f
            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding - 30f // Extra space for labels

            // Draw grid lines
            val gridPaint = Paint().apply {
                color = "#444444".toColorInt()
                strokeWidth = 1f
                alpha = 128
            }

            // Horizontal grid lines
            for (i in 0..4) {
                val y = padding + (chartHeight * i / 4f)
                canvas.drawLine(padding, y, width - padding, y, gridPaint)
            }

            // Vertical grid lines
            for (i in 0..6) {
                val x = padding + (chartWidth * i / 6f)
                canvas.drawLine(x, padding, x, padding + chartHeight, gridPaint)
            }

            // Draw min/max filled area (blue)
            if (minEntries.isNotEmpty() && maxEntries.isNotEmpty()) {
                val fillPaint = Paint().apply {
                    color = "#0000FF".toColorInt()
                    alpha = 50 // Semi-transparent
                    style = Paint.Style.FILL
                }

                val fillPath = Path()
                // Start with max line
                maxEntries.forEachIndexed { index, entry ->
                    val x = padding + ((entry.x - minX) / (maxX - minX)) * chartWidth
                    val y = padding + chartHeight - ((entry.y - minY) / (maxY - minY)) * chartHeight
                    if (index == 0) {
                        fillPath.moveTo(x, y)
                    } else {
                        fillPath.lineTo(x, y)
                    }
                }
                // Go back with min line
                minEntries.reversed().forEach { entry ->
                    val x = padding + ((entry.x - minX) / (maxX - minX)) * chartWidth
                    val y = padding + chartHeight - ((entry.y - minY) / (maxY - minY)) * chartHeight
                    fillPath.lineTo(x, y)
                }
                fillPath.close()
                canvas.drawPath(fillPath, fillPaint)

                // Draw max line (blue)
                val maxPaint = Paint().apply {
                    color = "#0000FF".toColorInt()
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                val maxPath = Path()
                maxEntries.forEachIndexed { index, entry ->
                    val x = padding + ((entry.x - minX) / (maxX - minX)) * chartWidth
                    val y = padding + chartHeight - ((entry.y - minY) / (maxY - minY)) * chartHeight
                    if (index == 0) {
                        maxPath.moveTo(x, y)
                    } else {
                        maxPath.lineTo(x, y)
                    }
                }
                canvas.drawPath(maxPath, maxPaint)

                // Draw min line (blue)
                val minPath = Path()
                minEntries.forEachIndexed { index, entry ->
                    val x = padding + ((entry.x - minX) / (maxX - minX)) * chartWidth
                    val y = padding + chartHeight - ((entry.y - minY) / (maxY - minY)) * chartHeight
                    if (index == 0) {
                        minPath.moveTo(x, y)
                    } else {
                        minPath.lineTo(x, y)
                    }
                }
                canvas.drawPath(minPath, maxPaint)
            }

            // Draw actual data line with colored segments
            if (actualEntries.isNotEmpty()) {
                val lineWidth = 4f

                actualEntries.forEachIndexed { index, entry ->
                    if (index < actualEntries.size - 1) {
                        val nextEntry = actualEntries[index + 1]

                        val x1 = padding + ((entry.x - minX) / (maxX - minX)) * chartWidth
                        val y1 =
                            padding + chartHeight - ((entry.y - minY) / (maxY - minY)) * chartHeight
                        val x2 = padding + ((nextEntry.x - minX) / (maxX - minX)) * chartWidth
                        val y2 =
                            padding + chartHeight - ((nextEntry.y - minY) / (maxY - minY)) * chartHeight

                        // Determine color based on comparison with max
                        val maxValue = maxEntries.firstOrNull { it.x == entry.x }?.y ?: 0f
                        val color = if (entry.y > maxValue) {
                            "#FFD700".toColorInt() // Gold - new record
                        } else {
                            "#FF0000".toColorInt() // Red
                        }

                        val actualPaint = Paint().apply {
                            this.color = color
                            strokeWidth = lineWidth
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                        }

                        canvas.drawLine(x1, y1, x2, y2, actualPaint)
                    }
                }
            }

            // Draw forecast line (dashed, gray)
            if (forecastEntries.isNotEmpty()) {
                val forecastPaint = Paint().apply {
                    color = "#888888".toColorInt()
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
                }
                val forecastPath = Path()
                forecastEntries.forEachIndexed { index, entry ->
                    val x = padding + ((entry.x - minX) / (maxX - minX)) * chartWidth
                    val y = padding + chartHeight - ((entry.y - minY) / (maxY - minY)) * chartHeight
                    if (index == 0) {
                        forecastPath.moveTo(x, y)
                    } else {
                        forecastPath.lineTo(x, y)
                    }
                }
                canvas.drawPath(forecastPath, forecastPaint)
            }

            // Draw axis labels
            val labelPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }

            // X-axis labels (months)
            val months = listOf("Jan", "Mar", "May", "Jul", "Sep", "Nov")
            months.forEachIndexed { index, month ->
                val x = padding + (chartWidth * (index + 1) / 7f)
                canvas.drawText(month, x, height - 5f, labelPaint)
            }

            // Y-axis labels
            labelPaint.textAlign = Paint.Align.LEFT
            for (i in 0..4) {
                val yValue = minY + (maxY - minY) * (4 - i) / 4f
                val y = padding + (chartHeight * i / 4f)
                val label = if (yValue > 999) {
                    "${(yValue / 1000).toInt()}k"
                } else {
                    "${yValue.toInt()}"
                }
                canvas.drawText(label, 5f, y + 5f, labelPaint)
            }

            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun createWidgetDataFromForecasts(
        forecasts: List<Forecast>,
        summits: List<Summit>,
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

        val chartBitmap = generateYearlyChartBitmap(
            summits = summits,
            forecasts = forecasts,
            indoorHeightMeterPercent = indoorHeightMeterPercent
        )

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
            ),
            yearlyChartBitmap = chartBitmap
        )
    }

    private fun createWidgetDataFromStatistics(
        entries: List<Summit>,
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

        // Calculate monthly stats
        val calendar = Calendar.getInstance()
        val currentMonth: Int = calendar[Calendar.MONTH]
        val currentYear: Int = calendar[Calendar.YEAR]
        val currentDay: Int = calendar[Calendar.DAY_OF_MONTH]
        val dayOfMonthPercentage =
            currentDay.toDouble() / calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthlyTargetActivity =
            (annualTargetActivity / 12.0 * dayOfMonthPercentage).roundToInt()
        val monthlyTargetKm = (annualTargetKm / 12.0 * dayOfMonthPercentage).roundToInt()
        val monthlyTargetHm = (annualTargetHm / 12.0 * dayOfMonthPercentage).roundToInt()

        val entriesForCurrentMonth = entries.filter {
            val calForEntry = Calendar.getInstance()
            calForEntry.time = it.date
            calForEntry[Calendar.MONTH] == currentMonth &&
                    calForEntry[Calendar.YEAR] == currentYear
        }

        val monthlyActivities = entriesForCurrentMonth.count()
        val monthlyHm = entriesForCurrentMonth.sumOf {
            if (it.sportType == de.drtobiasprinz.summitbook.db.entities.SportType.IndoorTrainer) {
                it.elevationData.elevationGain * indoorHeightMeterPercent / 100
            } else {
                it.elevationData.elevationGain
            }
        }
        val monthlyKm = entriesForCurrentMonth.sumOf { it.kilometers }.roundToInt()

        val chartBitmap = generateYearlyChartBitmap(
            summits = entries,
            forecasts = emptyList(),
            indoorHeightMeterPercent = indoorHeightMeterPercent
        )

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
            ),
            monthlyStats = MonthlyStats(
                activities = StatItem(
                    actualValue = monthlyActivities,
                    expectedValue = monthlyTargetActivity,
                    isAchieved = monthlyActivities >= monthlyTargetActivity
                ),
                heightMeter = StatItem(
                    actualValue = monthlyHm,
                    expectedValue = monthlyTargetHm,
                    unit = "hm",
                    isAchieved = monthlyHm >= monthlyTargetHm
                ),
                kilometers = StatItem(
                    actualValue = monthlyKm,
                    expectedValue = monthlyTargetKm,
                    unit = "km",
                    isAchieved = monthlyKm >= monthlyTargetKm
                )
            ),
            yearlyChartBitmap = chartBitmap
        )
    }

    private fun filterByDate(allEntries: List<Summit>): List<Summit> {
        val now = Calendar.getInstance()
        val year = now[Calendar.YEAR]
        val startDate = Summit.parseDate(
            String.format(
                "%s-12-31",
                (year - 1).toString()
            )
        )
        val endDate = Summit.parseDate(
            String.format(
                "%s-12-31",
                year.toString()
            )
        )
        val entries = ArrayList<Summit>()
        for (entry in allEntries) {
            if (entry.date.after(startDate) && entry.date.before(endDate)) {
                entries.add(entry)
            }
        }
        return entries
    }
}