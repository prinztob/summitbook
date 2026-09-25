package de.drtobiasprinz.summitbook.ui.compose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.data.analytics.GraphType
import de.drtobiasprinz.summitbook.data.analytics.PerformanceGraphProvider
import de.drtobiasprinz.summitbook.data.appstate.AppState
import de.drtobiasprinz.summitbook.data.db.entities.Forecast
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.ChartEntry
import de.drtobiasprinz.summitbook.data.model.StatisticEntry
import de.drtobiasprinz.summitbook.ui.theme.ChartBlue
import de.drtobiasprinz.summitbook.ui.theme.ChartGold
import de.drtobiasprinz.summitbook.ui.theme.ChartLime
import de.drtobiasprinz.summitbook.ui.theme.ChartRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date

@Composable
fun OverviewScreen(
    filteredSummits: List<Summit>,
    summitsFromDatabase: List<Summit>,
    forecasts: List<Forecast>,
    years: List<String>
) {
    // State variables
    var selectedGraphType by rememberSaveable(stateSaver = enumSaver<GraphType>()) {
        mutableStateOf(GraphType.ElevationGain)
    }
    var showMonths by rememberSaveable { mutableStateOf(false) }
    var showYears by rememberSaveable { mutableStateOf(false) }
    var currentMonth by rememberSaveable { mutableIntStateOf(Calendar.getInstance()[Calendar.MONTH] + 1) }
    var currentYear by rememberSaveable { mutableIntStateOf(Calendar.getInstance()[Calendar.YEAR]) }
    var selectedYear by rememberSaveable { mutableIntStateOf(currentYear) }

    val sharedPreferences = AppState.sharedPreferences
    val indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
    val numberFormat = NumberFormat.getInstance(LocalConfiguration.current.locales[0])
    val summitsWithoutBookmarks = remember(summitsFromDatabase) {
        summitsFromDatabase.filter { !it.isBookmark }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        // Overview header section
        OverviewHeader(
            filteredSummits = filteredSummits,
            forecastsList = forecasts,
            indoorHeightMeterPercent = indoorHeightMeterPercent,
            numberFormat = numberFormat,
            showMonths = showMonths,
            showYears = showYears,
            onToggleMonths = { showMonths = true; showYears = false },
            onToggleYears = { showYears = true; showMonths = false },
            onToggleNone = { showMonths = false; showYears = false }
        )

        // Chart section (conditionally visible)
        if ((showMonths || showYears) && filteredSummits.isNotEmpty()) {
            ChartSection(
                summits = summitsWithoutBookmarks,
                forecasts = forecasts,
                selectedGraphType = selectedGraphType,
                onGraphTypeSelected = { selectedGraphType = it },
                showMonths = showMonths,
                showYears = showYears,
                currentMonth = currentMonth,
                currentYear = currentYear,
                selectedYear = selectedYear,
                onYearChanged = { selectedYear = it },
                onMonthChanged = { currentMonth = it },
                indoorHeightMeterPercent = indoorHeightMeterPercent,
                numberFormat = numberFormat,
                years = years
            )
        }
    }
}

@Composable
fun OverviewHeader(
    filteredSummits: List<Summit>,
    forecastsList: List<Forecast>,
    indoorHeightMeterPercent: Int,
    numberFormat: NumberFormat,
    showMonths: Boolean,
    showYears: Boolean,
    onToggleMonths: () -> Unit,
    onToggleYears: () -> Unit,
    onToggleNone: () -> Unit
) {
    val context = LocalContext.current

    // Calculate statistics text
    var activitiesText by remember { mutableStateOf("") }
    var summitsText by remember { mutableStateOf("") }

    // Update text when data changes
    LaunchedEffect(filteredSummits, forecastsList) {
        numberFormat.maximumFractionDigits = 0
        // Heavy statistics computation runs off the main thread
        val (activities, summits) = withContext(Dispatchers.Default) {
            val statisticEntry = StatisticEntry(filteredSummits, indoorHeightMeterPercent)
            statisticEntry.calculate()
            val peaks = filteredSummits.filter { it.isPeak }
            val peakNames = AppState.peaks.map { peak -> peak.name }.toHashSet()
            val numberOfPeaks = peaks.size + filteredSummits.flatMap { it.places }
                .filter { it in peakNames }.size

            val resources = context.resources
            resources.getQuantityString(
                R.plurals.overview_activities,
                filteredSummits.size,
                filteredSummits.size,
                numberFormat.format(statisticEntry.totalKm),
                numberFormat.format(statisticEntry.totalHm)
            ) to resources.getQuantityString(
                R.plurals.overview_summits,
                numberOfPeaks,
                numberOfPeaks,
                numberFormat.format(peaks.sumOf { it.kilometers }),
                numberFormat.format(peaks.sumOf { it.elevationData.elevationGain })
            )
        }
        activitiesText = activities
        summitsText = summits
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Activities overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_directions_run_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = activitiesText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Summits overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_landscape_2_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = summitsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Chart controls
            ChartControls(
                showMonths = showMonths,
                showYears = showYears,
                onToggleMonths = onToggleMonths,
                onToggleYears = onToggleYears,
                onToggleNone = onToggleNone
            )
        }
    }
}

@Composable
fun ChartSection(
    summits: List<Summit>,
    forecasts: List<Forecast>,
    selectedGraphType: GraphType,
    onGraphTypeSelected: (GraphType) -> Unit,
    showMonths: Boolean,
    showYears: Boolean,
    currentMonth: Int,
    currentYear: Int,
    selectedYear: Int,
    onYearChanged: (Int) -> Unit,
    onMonthChanged: (Int) -> Unit,
    indoorHeightMeterPercent: Int,
    numberFormat: NumberFormat,
    years: List<String>
) {
    var performanceGraphProvider by remember(summits, forecasts) {
        mutableStateOf<PerformanceGraphProvider?>(
            PerformanceGraphProvider(summits, forecasts, indoorHeightMeterPercent)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // Graph type selection buttons
        GraphTypeSelectionButtons(
            selectedGraphType = selectedGraphType,
            onGraphTypeSelected = onGraphTypeSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Month chart (only if current year is selected and showMonths is true)
        if (showMonths && currentYear == selectedYear) {
            MonthChart(
                performanceGraphProvider = performanceGraphProvider,
                graphType = selectedGraphType,
                currentMonth = currentMonth,
                selectedYear = selectedYear,
                onMonthChanged = onMonthChanged,
                numberFormat = numberFormat,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Year chart (only if showYears is true)
        if (showYears) {
            YearChart(
                performanceGraphProvider = performanceGraphProvider,
                graphType = selectedGraphType,
                selectedYear = selectedYear,
                onYearChanged = onYearChanged,
                numberFormat = numberFormat,
                years = years
            )
        }

    }
}

@Composable
fun GraphTypeSelectionButtons(
    selectedGraphType: GraphType,
    onGraphTypeSelected: (GraphType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FilterChip(
            selected = selectedGraphType == GraphType.ElevationGain,
            onClick = { onGraphTypeSelected(GraphType.ElevationGain) },
            label = { Text(stringResource(R.string.hm), fontSize = 11.sp) }
        )

        FilterChip(
            selected = selectedGraphType == GraphType.Kilometer,
            onClick = { onGraphTypeSelected(GraphType.Kilometer) },
            label = { Text(stringResource(R.string.km), fontSize = 11.sp) }
        )

        FilterChip(
            selected = selectedGraphType == GraphType.Count,
            onClick = { onGraphTypeSelected(GraphType.Count) },
            label = { Text(stringResource(R.string.count), fontSize = 11.sp) }
        )

        FilterChip(
            selected = selectedGraphType == GraphType.Power,
            onClick = { onGraphTypeSelected(GraphType.Power) },
            label = { Text(stringResource(R.string.watt), fontSize = 11.sp) }
        )

        FilterChip(
            selected = selectedGraphType == GraphType.Vo2Max,
            onClick = { onGraphTypeSelected(GraphType.Vo2Max) },
            label = { Text(stringResource(R.string.vo2Max), fontSize = 11.sp) }
        )
    }
}

@Composable
fun MonthChart(
    performanceGraphProvider: PerformanceGraphProvider?,
    graphType: GraphType,
    currentMonth: Int,
    selectedYear: Int,
    onMonthChanged: (Int) -> Unit,
    numberFormat: NumberFormat,
) {
    if (performanceGraphProvider == null) return
    val monthNames = remember { DateFormatSymbols().months }

    Column {
        // Month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentMonth > 1) {
                        onMonthChanged(currentMonth - 1)
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.previous_month),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "${monthNames[currentMonth - 1]} $selectedYear",
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (currentMonth < 12) {
                        onMonthChanged(currentMonth + 1)
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.next_month),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Chart
        PerformanceChartView(
            performanceGraphProvider = performanceGraphProvider,
            graphType = graphType,
            year = selectedYear.toString(),
            month = if (currentMonth < 10) "0${currentMonth}" else currentMonth.toString(),
            numberFormat = numberFormat
        )
    }
}

@Composable
fun YearChart(
    performanceGraphProvider: PerformanceGraphProvider?,
    graphType: GraphType,
    selectedYear: Int,
    onYearChanged: (Int) -> Unit,
    numberFormat: NumberFormat,
    years: List<String>
) {
    if (performanceGraphProvider == null) return

    Column {
        // Year navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val minYear = years.minOfOrNull { year -> year.toInt() }
                        ?: selectedYear

                    if (selectedYear > minYear) {
                        onYearChanged(selectedYear - 1)
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.previous_year),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = selectedYear.toString(),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    val maxYear = years.maxOfOrNull { year -> year.toInt() }
                        ?: selectedYear

                    if (selectedYear < maxYear) {
                        onYearChanged(selectedYear + 1)
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.next_year),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Chart
        PerformanceChartView(
            performanceGraphProvider = performanceGraphProvider,
            graphType = graphType,
            year = selectedYear.toString(),
            numberFormat = numberFormat
        )
    }
}

@Composable
fun ChartControls(
    showMonths: Boolean,
    showYears: Boolean,
    onToggleMonths: () -> Unit,
    onToggleYears: () -> Unit,
    onToggleNone: () -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = !showMonths && !showYears,
            onClick = onToggleNone,
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            label = { Text(stringResource(R.string.none)) }
        )
        SegmentedButton(
            selected = showMonths,
            onClick = onToggleMonths,
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            label = { Text(stringResource(R.string.monthly)) }
        )
        SegmentedButton(
            selected = showYears,
            onClick = onToggleYears,
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            label = { Text(stringResource(R.string.yearly)) }
        )
    }
}

@Composable
fun PerformanceChartView(
    performanceGraphProvider: PerformanceGraphProvider,
    graphType: GraphType,
    year: String,
    month: String? = null,
    numberFormat: NumberFormat
) {
    var chartEntries by remember { mutableStateOf<List<ChartEntry>>(emptyList()) }
    var chartEntriesForecast by remember { mutableStateOf<List<ChartEntry>>(emptyList()) }
    var minMax by remember {
        mutableStateOf<Pair<List<ChartEntry>, List<ChartEntry>>>(
            Pair(
                emptyList(),
                emptyList()
            )
        )
    }
    var isLoading by remember { mutableStateOf(true) }

    // Load chart data when parameters change; runs in the effect's coroutine
    // so a restart cancels the previous load instead of racing it
    LaunchedEffect(performanceGraphProvider, graphType, year, month) {
        isLoading = true
        // Reset data to trigger loading state
        chartEntries = emptyList()
        chartEntriesForecast = emptyList()
        minMax = Pair(emptyList(), emptyList())

        try {
            val (actualEntries, forecastEntries, minMaxData) = withContext(Dispatchers.IO) {
                Triple(
                    performanceGraphProvider.getActualGraphForSummits(
                        graphType,
                        year,
                        month,
                        if (year == Calendar.getInstance()[Calendar.YEAR].toString() &&
                            (month == null || (month.toInt() == Calendar.getInstance()[Calendar.MONTH] + 1))
                        ) {
                            Date()
                        } else {
                            null
                        }
                    ),
                    performanceGraphProvider.getForecastGraphForSummits(
                        graphType, year, month, allDays = true
                    ),
                    performanceGraphProvider.getActualGraphMinMaxForSummits(graphType, year, month)
                )
            }

            chartEntries = actualEntries
            chartEntriesForecast = forecastEntries
            minMax = minMaxData
        } catch (e: Exception) {
            Log.e("OverviewScreen", "Failed to load chart data", e)
        } finally {
            isLoading = false
        }
    }

    // Build chart series
    val series = remember(chartEntries, chartEntriesForecast, minMax, graphType) {
        buildChartSeries(
            chartEntries = chartEntries,
            chartEntriesForecast = chartEntriesForecast,
            minMax = minMax,
            graphType = graphType
        )
    }

    if (series.isNotEmpty()) {
        PerformanceLineChart(
            series = series,
            graphType = graphType,
            year = year,
            month = month,
            numberFormat = numberFormat
        )
    } else if (isLoading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
    }
}

/**
 * Build chart series from raw data
 */
private fun buildChartSeries(
    chartEntries: List<ChartEntry>,
    chartEntriesForecast: List<ChartEntry>,
    minMax: Pair<List<ChartEntry>, List<ChartEntry>>,
    graphType: GraphType
): List<ChartSeries> {
    val series = mutableListOf<ChartSeries>()

    // Add min/max 5 years series if available
    if (minMax.first.isNotEmpty() && minMax.second.isNotEmpty()) {
        val maxData = if (graphType.filterZeroValues) {
            minMax.second.filter { it.y > 0f }
        } else {
            minMax.second
        }
        val minData = minMax.first

        if (graphType.cumulative) {
            // For cumulative graphs, fill between min and max
            series.add(
                ChartSeries(
                    name = "Max 5 yrs",
                    data = convertEntriesToChartDataPoints(maxData),
                    color = ChartBlue, // Blue
                    lineWidth = 2f,
                    filled = true,
                    fillColor = ChartBlue,
                    fillAlpha = 0.2f,
                    fillBetweenSeries = convertEntriesToChartDataPoints(minData)
                )
            )
            series.add(
                ChartSeries(
                    name = "Min 5 yrs",
                    data = convertEntriesToChartDataPoints(minData),
                    color = ChartBlue, // Blue
                    lineWidth = 2f,
                    filled = false
                )
            )
        } else {
            // For non-cumulative graphs, just show max line
            series.add(
                ChartSeries(
                    name = "Max 5 yrs",
                    data = convertEntriesToChartDataPoints(maxData),
                    color = ChartBlue, // Blue
                    lineWidth = 2f,
                    filled = false
                )
            )
        }
    }

    // Add forecast series if available
    if (graphType.hasForecast && chartEntriesForecast.isNotEmpty()) {
        series.add(
            ChartSeries(
                name = "Forecast",
                data = convertEntriesToChartDataPoints(chartEntriesForecast),
                color = ChartRed, // Red
                lineWidth = 2f,
                filled = false
            )
        )
    }

    // Add actual data series
    if (chartEntries.isNotEmpty()) {
        var actualEntries = chartEntries
        if (actualEntries[0].x != 1f) {
            actualEntries = listOf(ChartEntry(1f, 0f)) + actualEntries
        }
        val filteredEntries = if (graphType.filterZeroValues) {
            actualEntries.filter { it.y > 0f }
        } else {
            actualEntries
        }

        // Calculate colors for each point
        val pointColors = filteredEntries.map { entry ->
            val minMaxValue = minMax.second.firstOrNull { it.x == entry.x }?.y ?: 0f
            val forecastValue = chartEntriesForecast.firstOrNull { it.x == entry.x }?.y ?: 0f
            when {
                entry.y > minMaxValue -> ChartGold // Gold - new record
                graphType.hasForecast && entry.y > forecastValue -> ChartLime // Green
                else -> ChartRed // Red
            }
        }

        series.add(
            ChartSeries(
                name = "Actual",
                data = convertEntriesToChartDataPoints(filteredEntries),
                color = ChartRed, // Default red
                lineWidth = 5f,
                filled = false,
                drawCircles = false,
                pointColors = pointColors
            )
        )
    }

    return series
}