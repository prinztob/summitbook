package de.drtobiasprinz.summitbook.ui.compose

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.ui.GraphType
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.PerformanceGraphProvider
import de.drtobiasprinz.summitbook.ui.utils.CustomLineChartWithMarker
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun OverviewScreen(
    filteredSummits: List<Summit>,
    forecasts: List<Forecast>,
    sortFilterValues: SortFilterValues
) {
    // State variables
    var selectedGraphType by remember { mutableStateOf(GraphType.ElevationGain) }
    var graphIsVisible by remember { mutableStateOf(false) }
    var showMonths by remember { mutableStateOf(false) }
    var showYears by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableIntStateOf(Calendar.getInstance()[Calendar.MONTH] + 1) }
    var currentYear by remember { mutableIntStateOf(Calendar.getInstance()[Calendar.YEAR]) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }

    val sharedPreferences = MainActivityCompose.sharedPreferences
    val indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
    val numberFormat = NumberFormat.getInstance(java.util.Locale.getDefault())

    // Calculate statistics when data changes
    LaunchedEffect(filteredSummits, forecasts) {
            // Calculate statistics
            numberFormat.maximumFractionDigits = 0
            val statisticEntry = StatisticEntry(filteredSummits, indoorHeightMeterPercent)
            statisticEntry.calculate()
    }
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
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
            onToggleMonths = { showMonths = !showMonths },
            onToggleYears = { showYears = !showYears },
            onToggleGraphVisibility = { graphIsVisible = !graphIsVisible }
        )

        // Chart section (conditionally visible)
        if ((showMonths || showYears) && filteredSummits.isNotEmpty() && forecasts.isNotEmpty()) {
            ChartSection(
                summits = filteredSummits,
                forecasts = forecasts,
                selectedGraphType = selectedGraphType,
                onGraphTypeSelected = { selectedGraphType = it },
                showMonths = showMonths,
                showYears = showYears,
                onToggleMonths = { showMonths = !showMonths },
                onToggleYears = { showYears = !showYears },
                currentMonth = currentMonth,
                currentYear = currentYear,
                selectedYear = selectedYear,
                onYearChanged = { selectedYear = it },
                onMonthChanged = { currentMonth = it },
                indoorHeightMeterPercent = indoorHeightMeterPercent,
                numberFormat = numberFormat,
                sortFilterValues = sortFilterValues
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
    onToggleGraphVisibility: () -> Unit
) {
    // Calculate statistics text
    var activitiesText by remember { mutableStateOf("") }
    var summitsText by remember { mutableStateOf("") }

    // Update text when data changes
    LaunchedEffect(filteredSummits, forecastsList) {


            // Calculate statistics
            numberFormat.maximumFractionDigits = 0
            val statisticEntry = StatisticEntry(filteredSummits, indoorHeightMeterPercent)
            statisticEntry.calculate()
            val peaks = filteredSummits.filter { it.isPeak }
            val numberOfPeaks = peaks.size + filteredSummits.flatMap { it.places }
                .filter { it in MainActivityCompose.peaks.map { peak -> peak.name } }.size

            // Format the text with string resources
            activitiesText = "${
                filteredSummits.size
            } activities, ${
                numberFormat.format(statisticEntry.totalKm)
            } km, ${
                numberFormat.format(statisticEntry.totalHm)
            } hm"

            summitsText = "$numberOfPeaks summits, ${
                numberFormat.format(peaks.sumOf { it.kilometers })
            } km, ${
                numberFormat.format(peaks.sumOf { it.elevationData.elevationGain })
            } hm"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggleGraphVisibility() },
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

            Spacer(modifier = Modifier.height(2.dp))

            // Chart controls
            ChartControls(
                showMonths = showMonths,
                showYears = showYears,
                onToggleMonths = onToggleMonths,
                onToggleYears = onToggleYears
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
    onToggleMonths: () -> Unit,
    onToggleYears: () -> Unit,
    currentMonth: Int,
    currentYear: Int,
    selectedYear: Int,
    onYearChanged: (Int) -> Unit,
    onMonthChanged: (Int) -> Unit,
    indoorHeightMeterPercent: Int,
    numberFormat: NumberFormat,
    sortFilterValues: SortFilterValues
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
                onToggleMonths = onToggleMonths,
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
                sortFilterValues = sortFilterValues,
                onToggleYears = onToggleYears,
                expandedHeight = !showMonths || currentYear != selectedYear
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
    onToggleMonths: () -> Unit,
) {
    if (performanceGraphProvider == null) return

    var monthChart by remember { mutableStateOf<CustomLineChartWithMarker?>(null) }

    Column {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleMonths,
                modifier = Modifier.wrapContentSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.monthly), fontSize = 11.sp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth > 1) {
                            onMonthChanged(currentMonth - 1)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = null
                    )
                }

                Text(
                    text = "${DateFormatSymbols().months[currentMonth - 1]} $selectedYear",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (currentMonth < 12) {
                            onMonthChanged(currentMonth + 1)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null
                    )
                }
            }

            IconButton(
                onClick = { monthChart?.fitScreen() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_refresh_24),
                    contentDescription = stringResource(R.string.back)
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Chart
        ChartView(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            performanceGraphProvider = performanceGraphProvider,
            graphType = graphType,
            year = selectedYear.toString(),
            month = if (currentMonth < 10) "0${currentMonth}" else currentMonth.toString(),
            numberFormat = numberFormat,
            onChartReady = { monthChart = it }
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
    sortFilterValues: SortFilterValues,
    onToggleYears: () -> Unit,
    expandedHeight: Boolean = false
) {
    if (performanceGraphProvider == null) return

    var yearChart by remember { mutableStateOf<CustomLineChartWithMarker?>(null) }
    var lastChartEntry by remember { mutableStateOf(Entry(0f, 0f)) }

    Column {
        // Year navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onToggleYears,
                modifier = Modifier.wrapContentSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.yearly), fontSize = 11.sp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        // Get min year from sortFilterValues
                        val minYear = sortFilterValues.years.minOfOrNull { year -> year.toInt() }
                            ?: selectedYear

                        if (selectedYear > minYear) {
                            onYearChanged(selectedYear - 1)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = null
                    )
                }

                Text(
                    text = selectedYear.toString(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        // Get max year from sortFilterValues
                        val maxYear = sortFilterValues.years.maxOfOrNull { year -> year.toInt() }
                            ?: selectedYear

                        if (selectedYear < maxYear) {
                            onYearChanged(selectedYear + 1)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null
                    )
                }
            }

            Row {
                IconButton(
                    onClick = { yearChart?.fitScreen() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_refresh_24),
                        contentDescription = stringResource(R.string.back)
                    )
                }

                IconButton(
                    onClick = {
                        if (lastChartEntry.x != 0f || lastChartEntry.y != 0f) {
                            yearChart?.zoom(
                                4f,
                                4f,
                                lastChartEntry.x,
                                lastChartEntry.y,
                                YAxis.AxisDependency.LEFT
                            )
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_search_24),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Chart
        ChartView(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (expandedHeight) 250.dp else 150.dp),
            performanceGraphProvider = performanceGraphProvider,
            graphType = graphType,
            year = selectedYear.toString(),
            numberFormat = numberFormat,
            onChartReady = { chart ->
                yearChart = chart
                // Update lastChartEntry when chart data is available
                chart.data?.let { data ->
                    data.getDataSetByIndex(0)?.let { dataSet ->
                        if (dataSet.entryCount > 0) {
                            lastChartEntry = dataSet.getEntryForIndex(dataSet.entryCount - 1)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ChartControls(
    showMonths: Boolean,
    showYears: Boolean,
    onToggleMonths: () -> Unit,
    onToggleYears: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onToggleMonths,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (showMonths) "${stringResource(R.string.monthly)} ✓" else stringResource(R.string.monthly),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        OutlinedButton(
            onClick = onToggleYears,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (showYears) "${stringResource(R.string.yearly)} ✓" else stringResource(R.string.yearly),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ChartView(
    modifier: Modifier = Modifier,
    performanceGraphProvider: PerformanceGraphProvider,
    graphType: GraphType,
    year: String,
    month: String? = null,
    numberFormat: NumberFormat,
    onChartReady: (CustomLineChartWithMarker) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var chartEntries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var chartEntriesForecast by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var minMax by remember {
        mutableStateOf<Pair<List<Entry>, List<Entry>>>(
            Pair(
                emptyList(),
                emptyList()
            )
        )
    }

    // Load chart data when parameters change
    LaunchedEffect(performanceGraphProvider, graphType, year, month) {
        // Reset data to trigger loading state
        chartEntries = emptyList()
        chartEntriesForecast = emptyList()

        scope.launch(Dispatchers.IO) {
            try {
                val actualEntries = performanceGraphProvider.getActualGraphForSummits(
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
                )
                val forecastEntries = performanceGraphProvider.getForecastGraphForSummits(
                    graphType, year, month, allDays = true
                )
                val minMaxData =
                    performanceGraphProvider.getActualGraphMinMaxForSummits(graphType, year, month)

                // Update state on main thread
                withContext(Dispatchers.Main) {
                    chartEntries = actualEntries
                    chartEntriesForecast = forecastEntries
                    minMax = minMaxData
                }
            } catch (e: Exception) {
                // Handle error silently or log it
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            CustomLineChartWithMarker(ctx).apply {
                // Configure chart properties
                invalidate()
                axisRight.setDrawLabels(false)

                // Set Y axis formatter
                axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        numberFormat.maximumFractionDigits = if (value > 99) 0 else 1
                        val format = "${numberFormat.format(value.toDouble())} ${graphType.unit}"
                        return format
                    }
                }

                // Set X axis formatter
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String? {
                        val cal = Calendar.getInstance()
                        cal.time = PerformanceGraphProvider.parseDate(
                            String.format("${year}-${month ?: "01"}-01 00:00:00")
                        )
                        if (month == null) {
                            cal.set(Calendar.DAY_OF_YEAR, value.toInt())
                        } else {
                            cal.set(Calendar.DAY_OF_MONTH, value.toInt())
                        }
                        return SimpleDateFormat(
                            "dd MMM", java.util.Locale.getDefault()
                        ).format(cal.time)
                    }
                }

                // Configure appearance based on theme
                when (ctx.resources.configuration?.uiMode?.and(android.content.res.Configuration.UI_MODE_NIGHT_MASK)) {
                    android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                        xAxis.textColor = Color.WHITE
                        axisRight.textColor = Color.WHITE
                        axisLeft.textColor = Color.WHITE
                        legend?.textColor = Color.WHITE
                    }

                    android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                        xAxis.textColor = Color.BLACK
                        axisRight.textColor = Color.BLACK
                        axisLeft.textColor = Color.BLACK
                        legend?.textColor = Color.BLACK
                    }

                    android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                        xAxis.textColor = Color.WHITE
                        axisRight.textColor = Color.WHITE
                        axisLeft.textColor = Color.WHITE
                        legend?.textColor = Color.WHITE
                    }
                }

                setTouchEnabled(true)
            }
        },
        update = { chart ->
            // Update chart data when entries change
            if (chartEntries.isNotEmpty() || chartEntriesForecast.isNotEmpty()) {
                updateLineChart(
                    chart = chart,
                    chartEntries = chartEntries,
                    graphType = graphType,
                    minMax = minMax,
                    chartEntriesForecast = chartEntriesForecast
                )
                onChartReady(chart)
                chart.visibility = android.view.View.VISIBLE
            } else {
                chart.visibility = android.view.View.GONE
            }
        },
        modifier = modifier
    )
}

fun updateLineChart(
    chart: CustomLineChartWithMarker,
    chartEntries: List<Entry>,
    graphType: GraphType,
    minMax: Pair<List<Entry>, List<Entry>>,
    chartEntriesForecast: List<Entry>
) {
    var chartEntries1 = chartEntries
    chart.invalidate()
    chart.axisRight.setDrawLabels(false)

    if (graphType.cumulative) {
        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.axisMinimum = 0f
    } else {
        val minLeft = (minMax.second + chartEntries1).filter { it.y > 0 }.minOfOrNull { it.y }
        if (minLeft != null) {
            chart.axisLeft.axisMinimum = minLeft
        }
        val minRight = (minMax.second + chartEntries1).filter { it.y > 0 }.minOfOrNull { it.y }
        if (minRight != null) {
            chart.axisRight.axisMinimum = minRight
        }
    }

    val dataSets: MutableList<ILineDataSet?> = ArrayList()
    if (chartEntries1.isNotEmpty() && chartEntries1[0].x != 1f) {
        chartEntries1 = listOf(Entry(1f, 0f)) + chartEntries1
    }
    val entries =
        if (graphType.filterZeroValues) chartEntries1.filter { it.y > 0f } else chartEntries1
    val dataSet = LineDataSet(entries, chart.context.getString(R.string.actually))

    // Set graph view properties
    setGraphView(
        dataSet, false, lineWidth = 5f, colors = entries.map { e ->
            if (e.y > (minMax.second.firstOrNull { it.x == e.x }?.y ?: 0f)) {
                Color.rgb(255, 215, 0)
            } else if (graphType.hasForecast && e.y > (chartEntriesForecast.firstOrNull { it.x == e.x }?.y
                    ?: 0f)
            ) {
                Color.GREEN
            } else {
                Color.RED
            }
        }
    )

    if (minMax.first.isNotEmpty() && minMax.second.isNotEmpty()) {
        chart.axisLeft.axisMaximum =
            (minMax.second + chartEntries1).maxOf { it.y } + graphType.delta
        chart.axisRight.axisMaximum =
            (minMax.second + chartEntries1).maxOf { it.y } + graphType.delta
        val dataSetMaximalValues = LineDataSet(
            if (graphType.filterZeroValues) minMax.second.filter { it.y > 0f } else minMax.second,
            chart.context.getString(R.string.max_5_yrs)
        )
        if (graphType.cumulative) {
            val dataSetMinimalValues = LineDataSet(
                minMax.first, chart.context.getString(R.string.min_5_yrs)
            )
            setGraphView(dataSetMinimalValues)
            dataSets.add(dataSetMinimalValues)
            dataSetMaximalValues.fillFormatter = MyFillFormatter(dataSetMinimalValues)
            chart.renderer = MyLineLegendRenderer(
                chart, chart.animator, chart.viewPortHandler
            )
        }
        setGraphView(dataSetMaximalValues)
        dataSets.add(dataSetMaximalValues)
    }
    if (graphType.hasForecast) {
        val dataSetForecast =
            LineDataSet(chartEntriesForecast, chart.context.getString(R.string.forecast))
        setGraphView(dataSetForecast, false, color = Color.rgb(255, 0, 0))
        dataSets.add(dataSetForecast)
    }
    dataSets.add(dataSet)

    chart.data = LineData(dataSets)
    setLegend(chart)
}

fun setLegend(lineChart: LineChart) {
    val l: Legend = lineChart.legend
    l.yEntrySpace = 10f
    l.isWordWrapEnabled = true
    val l1 = LegendEntry(
        lineChart.context.getString(R.string.new_record),
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        Color.rgb(255, 215, 0)
    )
    val l2 = LegendEntry(
        lineChart.context.getString(R.string.better_then),
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        Color.GREEN
    )
    val l3 = LegendEntry(
        lineChart.context.getString(R.string.forecast),
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        Color.RED
    )
    val l4 = LegendEntry(
        lineChart.context.getString(R.string.min_max_5_yrs),
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        Color.BLUE
    )
    l.setCustom(arrayOf(l1, l2, l3, l4))
    l.isEnabled = true
}

fun setGraphView(
    set1: LineDataSet?,
    filled: Boolean = true,
    color: Int = Color.BLUE,
    lineWidth: Float = 2f,
    colors: List<Int>? = null
) {
    set1?.mode = LineDataSet.Mode.LINEAR
    set1?.setDrawValues(false)
    set1?.setDrawCircles(false)
    if (filled) {
        set1?.cubicIntensity = 20f
        if (colors != null && colors.size == set1?.entryCount) {
            set1.colors = colors
        } else {
            set1?.color = color
        }
        set1?.highLightColor = Color.rgb(244, 117, 117)
        set1?.setDrawFilled(true)
        set1?.fillColor = color
        set1?.fillAlpha = 100
    } else {
        set1?.lineWidth = lineWidth
        set1?.setCircleColor(Color.BLACK)
        if (colors != null && colors.size == set1?.entryCount) {
            set1.colors = colors
        } else {
            set1?.color = color
        }
        set1?.setDrawFilled(false)
    }
    set1?.setDrawHorizontalHighlightIndicator(true)
}