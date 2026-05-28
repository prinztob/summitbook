package de.drtobiasprinz.summitbook.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.room.Room
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.ui.GraphType
import de.drtobiasprinz.summitbook.ui.PerformanceGraphProvider
import de.drtobiasprinz.summitbook.ui.widget.theme.GlanceColorScheme
import de.drtobiasprinz.summitbook.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

class SummitBookGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.i("SummitBookGlanceWidget", "provideGlance started")
        val widgetData = loadWidgetData(context)
        Log.i("SummitBookGlanceWidget", "provideGlance data loaded, providing content")
        provideContent {
            val prefs = currentState<Preferences>()
            val now = prefs[longPreferencesKey("now")] ?: System.currentTimeMillis()
            Log.i("SummitBookGlanceWidget", "provideGlance at $now")
            val widgetData by produceState(widgetData, now) {
                value = loadWidgetData(context)
            }
            GlanceTheme(
                colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceTheme.colors
                } else {
                    GlanceColorScheme.colors
                },
            ) {
                SummitBookWidgetContent(widgetData)
            }
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData = withContext(Dispatchers.IO) {
        var db: AppDatabase? = null
        try {
            db = Room.databaseBuilder(
                context, AppDatabase::class.java, Constants.DATABASE
            ).build()
            val repository = DatabaseRepository(
                db.summitsDao(),
                db.segmentsDao(),
                db.forecastDao(),
                db.ignoredActivityDao(),
                db.entityEventDao(),
                db.peakDao(),
                db.dailyActivitySummaryDao(),
                db.mountainPassDao()
            )

            val summits = repository.getAllSummits().first().filter { !it.isBookmark }
            val forecasts = repository.getAllForecasts().first()

            val calendar = Calendar.getInstance()
            val currentYear: Int = calendar[Calendar.YEAR]

            Log.d("SummitBookGlanceWidget", "Loading widget data: ${summits.size} summits, ${forecasts.size} forecasts")

            val result = if (forecasts.isNotEmpty() && forecasts.any { it.year == currentYear }) {
                createWidgetDataFromForecasts(forecasts, summits, context)
            } else {
                createWidgetDataFromStatistics(summits, context)
            }
            Log.d("SummitBookGlanceWidget", "Widget data loaded successfully")
            result
        } catch (e: Exception) {
            Log.e("SummitBookGlanceWidget", "Error loading widget data", e)
            WidgetData()
        } finally {
            db?.close()
        }
    }

    /**
     * Generate a bitmap of the yearly overview chart centered on current month
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

            // Load chart data
            val chartData =
                loadChartData(performanceGraphProvider, currentYear, currentDate) ?: return null

            // Create bitmap and canvas
            val width = 400
            val height = 200
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)

            // Draw background
            canvas.drawColor("#1E1E1E".toColorInt())

            // Calculate chart bounds
            val bounds = calculateChartBounds(chartData, calendar)
            val padding = 20f
            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding - 30f

            // Draw chart elements
            drawGridLines(canvas, chartWidth, chartHeight, width.toFloat())
            drawMinMaxArea(
                canvas,
                chartData.minEntries,
                chartData.maxEntries,
                bounds,
                chartWidth,
                chartHeight
            )
            drawActualDataLine(
                canvas,
                chartData.actualEntries,
                chartData.maxEntries,
                chartData.forecastEntries,
                bounds,
                chartWidth,
                chartHeight
            )
            drawForecastLine(canvas, chartData.forecastEntries, bounds, chartWidth, chartHeight)
            drawAxisLabels(
                canvas,
                bounds,
                chartWidth,
                chartHeight,
                width.toFloat(),
                height.toFloat(),
                calendar
            )

            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Data class to hold chart data
     */
    private data class ChartData(
        val actualEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        val forecastEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        val minEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        val maxEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>
    )

    /**
     * Data class to hold chart bounds
     */
    private data class ChartBounds(
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float
    )

    /**
     * Load chart data from performance graph provider
     */
    private fun loadChartData(
        provider: PerformanceGraphProvider,
        year: String,
        currentDate: Date
    ): ChartData? {
        val actualEntries = provider.getActualGraphForSummits(
            GraphType.ElevationGain, year, month = null, currentDate = currentDate
        )
        val forecastEntries = provider.getForecastGraphForSummits(
            GraphType.ElevationGain, year, month = null, allDays = true
        )
        val minMaxData = provider.getActualGraphMinMaxForSummits(
            GraphType.ElevationGain, year, month = null
        )

        if (actualEntries.isEmpty() && minMaxData.first.isEmpty() && forecastEntries.isEmpty()) {
            return null
        }

        return ChartData(
            actualEntries = actualEntries,
            forecastEntries = forecastEntries,
            minEntries = minMaxData.first,
            maxEntries = minMaxData.second
        )
    }

    /**
     * Calculate chart bounds centered on current month
     */
    private fun calculateChartBounds(data: ChartData, calendar: Calendar): ChartBounds {
        val currentMonth = calendar[Calendar.MONTH]
        val currentYearInt = calendar[Calendar.YEAR]

        // Calculate X-axis bounds centered on current month
        val startOfMonthCalendar = Calendar.getInstance().apply {
            set(currentYearInt, currentMonth, 1)
        }
        val endOfMonthCalendar = Calendar.getInstance().apply {
            set(currentYearInt, currentMonth, getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        val startDayOfYear = startOfMonthCalendar.get(Calendar.DAY_OF_YEAR).toFloat()
        val endDayOfYear = endOfMonthCalendar.get(Calendar.DAY_OF_YEAR).toFloat()

        val paddingDays = 15f
        val minX = maxOf(1f, startDayOfYear - paddingDays)
        val maxX = minOf(366f, endDayOfYear + paddingDays)

        // Calculate Y-axis bounds from visible data only
        val allEntries = data.actualEntries + data.maxEntries
        val visibleEntries = allEntries.filter { it.x in minX..maxX }
        val visibleYValues = visibleEntries.map { it.y }
        val minY = visibleYValues.minOrNull() ?: 0f
        val maxY = visibleYValues.maxOrNull() ?: 1f

        // Add 10% padding to Y-axis
        val yRange = maxY - minY
        val yPadding = if (yRange > 0) yRange * 0.1f else 1f
        val adjustedMinY = maxOf(0f, minY - yPadding)
        val adjustedMaxY = maxY + yPadding

        return ChartBounds(minX, maxX, adjustedMinY, adjustedMaxY)
    }

    /**
     * Draw grid lines
     */
    private fun drawGridLines(
        canvas: Canvas,
        chartWidth: Float,
        chartHeight: Float,
        width: Float,
        padding: Float = 20f
    ) {
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
    }

    /**
     * Draw min/max filled area and lines
     */
    private fun drawMinMaxArea(
        canvas: Canvas,
        minEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        maxEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        bounds: ChartBounds,
        chartWidth: Float,
        chartHeight: Float,
        padding: Float = 20f
    ) {
        if (minEntries.isEmpty() || maxEntries.isEmpty()) return

        // Draw filled area
        val fillPaint = Paint().apply {
            color = "#0000FF".toColorInt()
            alpha = 50
            style = Paint.Style.FILL
        }

        val fillPath = Path()
        maxEntries.forEachIndexed { index, entry ->
            val (x, y) = mapToCanvas(entry.x, entry.y, bounds, padding, chartWidth, chartHeight)
            if (index == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
        }
        minEntries.reversed().forEach { entry ->
            val (x, y) = mapToCanvas(entry.x, entry.y, bounds, padding, chartWidth, chartHeight)
            fillPath.lineTo(x, y)
        }
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        // Draw max and min lines
        val linePaint = Paint().apply {
            color = "#0000FF".toColorInt()
            strokeWidth = 2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        listOf(maxEntries, minEntries).forEach { entries ->
            val path = Path()
            entries.forEachIndexed { index, entry ->
                val (x, y) = mapToCanvas(entry.x, entry.y, bounds, padding, chartWidth, chartHeight)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, linePaint)
        }
    }

    /**
     * Draw actual data line with colored segments
     */
    private fun drawActualDataLine(
        canvas: Canvas,
        actualEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        maxEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        forecastEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        bounds: ChartBounds,
        chartWidth: Float,
        chartHeight: Float,
        padding: Float = 20f
    ) {
        if (actualEntries.isEmpty()) return

        actualEntries.forEachIndexed { index, entry ->
            if (index < actualEntries.size - 1) {
                val nextEntry = actualEntries[index + 1]
                val (x1, y1) = mapToCanvas(
                    entry.x,
                    entry.y,
                    bounds,
                    padding,
                    chartWidth,
                    chartHeight
                )
                val (x2, y2) = mapToCanvas(
                    nextEntry.x,
                    nextEntry.y,
                    bounds,
                    padding,
                    chartWidth,
                    chartHeight
                )

                val color = determineLineColor(entry, maxEntries, forecastEntries)

                val paint = Paint().apply {
                    this.color = color
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }

                canvas.drawLine(x1, y1, x2, y2, paint)
            }
        }
    }

    /**
     * Draw forecast line (dashed)
     */
    private fun drawForecastLine(
        canvas: Canvas,
        forecastEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        bounds: ChartBounds,
        chartWidth: Float,
        chartHeight: Float,
        padding: Float = 20f
    ) {
        if (forecastEntries.isEmpty()) return

        val paint = Paint().apply {
            color = "#888888".toColorInt()
            strokeWidth = 2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }

        val path = Path()
        forecastEntries.forEachIndexed { index, entry ->
            val (x, y) = mapToCanvas(entry.x, entry.y, bounds, padding, chartWidth, chartHeight)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    /**
     * Draw axis labels
     */
    private fun drawAxisLabels(
        canvas: Canvas,
        bounds: ChartBounds,
        chartWidth: Float,
        chartHeight: Float,
        width: Float,
        height: Float,
        calendar: Calendar,
        padding: Float = 20f
    ) {
        val labelPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }

        // X-axis labels (months) - locale-aware
        val monthNames = DateFormatSymbols.getInstance().shortMonths
        val currentMonth = calendar[Calendar.MONTH]
        val currentYearInt = calendar[Calendar.YEAR]

        val monthsToShow = buildList {
            add(currentMonth)
            if (currentMonth > 0) add(currentMonth - 1)
            if (currentMonth < 11) add(currentMonth + 1)
            if (currentMonth > 1) add(currentMonth - 2)
            if (currentMonth < 10) add(currentMonth + 2)
        }.distinct().sorted().take(3)

        monthsToShow.forEach { monthIndex ->
            val dayOfYearForMonth = Calendar.getInstance().apply {
                set(currentYearInt, monthIndex, 15)
            }.get(Calendar.DAY_OF_YEAR).toFloat()

            val x =
                padding + ((dayOfYearForMonth - bounds.minX) / (bounds.maxX - bounds.minX)) * chartWidth
            if (x in padding..(width - padding)) {
                canvas.drawText(monthNames[monthIndex], x, height - 5f, labelPaint)
            }
        }

        // Y-axis labels
        labelPaint.textAlign = Paint.Align.LEFT
        for (i in 0..4) {
            val yValue = bounds.minY + (bounds.maxY - bounds.minY) * (4 - i) / 4f
            val y = padding + (chartHeight * i / 4f)
            val numberFormat = NumberFormat.getIntegerInstance()
            val label = if (yValue > 999) {
                "${numberFormat.format((yValue / 1000).roundToInt())}k"
            } else {
                numberFormat.format(yValue.roundToInt())
            }
            canvas.drawText(label, 5f, y + 5f, labelPaint)
        }
    }

    /**
     * Map data coordinates to canvas coordinates
     */
    private fun mapToCanvas(
        x: Float,
        y: Float,
        bounds: ChartBounds,
        padding: Float,
        chartWidth: Float,
        chartHeight: Float
    ): Pair<Float, Float> {
        val canvasX = padding + ((x - bounds.minX) / (bounds.maxX - bounds.minX)) * chartWidth
        val canvasY =
            padding + chartHeight - ((y - bounds.minY) / (bounds.maxY - bounds.minY)) * chartHeight
        return canvasX to canvasY
    }

    /**
     * Determine line color based on comparison with max and forecast
     */
    private fun determineLineColor(
        entry: de.drtobiasprinz.summitbook.models.ChartEntry,
        maxEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>,
        forecastEntries: List<de.drtobiasprinz.summitbook.models.ChartEntry>
    ): Int {
        val maxValue = maxEntries.firstOrNull { it.x == entry.x }?.y ?: 0f
        val forecastEntry = forecastEntries.firstOrNull { it.x == entry.x }

        return when {
            entry.y > maxValue -> "#FFD700".toColorInt() // Gold - new record
            forecastEntry != null && entry.y > forecastEntry.y -> "#00FF00".toColorInt() // Green - above forecast
            else -> "#FF0000".toColorInt() // Red - below forecast or no forecast
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
                it.elevationData.elevationGain.toDouble() * indoorHeightMeterPercent / 100.0
            } else {
                it.elevationData.elevationGain.toDouble()
            }
        }.roundToInt()

        val actualHeightMeterYearly = summitsForCurrentYear.sumOf {
            if (it.sportType == de.drtobiasprinz.summitbook.db.entities.SportType.IndoorTrainer) {
                it.elevationData.elevationGain.toDouble() * indoorHeightMeterPercent / 100.0
            } else {
                it.elevationData.elevationGain.toDouble()
            }
        }.roundToInt()

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
            yearlyChartBitmap = chartBitmap,
            recentSummits = summits.sortedByDescending { it.date }.take(3)
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
                it.elevationData.elevationGain.toDouble() * indoorHeightMeterPercent / 100.0
            } else {
                it.elevationData.elevationGain.toDouble()
            }
        }.roundToInt()
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
                    isAchieved = statisticEntry.getTotalActivities() >= statisticEntry.expectedAchievementActivityAbsolute.roundToInt()
                ),
                heightMeter = StatItem(
                    actualValue = statisticEntry.totalHm,
                    expectedValue = statisticEntry.expectedAchievementHmAbsolute.roundToInt(),
                    unit = "hm",
                    isAchieved = statisticEntry.totalHm >= statisticEntry.expectedAchievementHmAbsolute.roundToInt()
                ),
                kilometers = StatItem(
                    actualValue = statisticEntry.totalKm.roundToInt(),
                    expectedValue = statisticEntry.expectedAchievementKmAbsolute.roundToInt(),
                    unit = "km",
                    isAchieved = statisticEntry.totalKm.roundToInt() >= statisticEntry.expectedAchievementKmAbsolute.roundToInt()
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
            yearlyChartBitmap = chartBitmap,
            recentSummits = entries.sortedByDescending { it.date }.take(3)
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