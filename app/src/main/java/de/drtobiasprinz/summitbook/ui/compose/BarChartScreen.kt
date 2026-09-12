package de.drtobiasprinz.summitbook.ui.compose

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.DailyActivityHelper.findDailyActivitySummariesWhichWasNotAddedToSummits
import de.drtobiasprinz.summitbook.db.entities.DailyActivityHelper.parseAsSummit
import de.drtobiasprinz.summitbook.db.entities.DailyActivitySummary
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.BarChartXAxisSelector
import de.drtobiasprinz.summitbook.models.BarChartYAxisSelector
import de.drtobiasprinz.summitbook.models.BarChartZAxisSelector
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.floor

@Composable
fun BarChartScreen(
    filteredSummits: List<Summit>,
    forecasts: List<Forecast>,
    dailyActivitySummaryList: List<DailyActivitySummary>,
    areMoreThanOneYearSelected: Boolean = false
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val sharedPreferences = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    var selectedXAxisSpinnerEntry by remember {
        mutableStateOf(
            if (areMoreThanOneYearSelected) BarChartXAxisSelector.DateByYear else BarChartXAxisSelector.DateByMonth
        )
    }
    var selectedYAxisSpinnerEntry by remember { mutableStateOf(BarChartYAxisSelector.TotalActivities) }
    var selectedZAxisSpinnerEntry by remember { mutableStateOf(BarChartZAxisSelector.PerSportGroup) }
    var selectedXAxisSpinnerMonth by remember { mutableIntStateOf(0) }
    var includeFilteredDailyActivitySummaries by remember { mutableStateOf(false) }

    var barChartEntries by remember { mutableStateOf<List<BarEntry>>(emptyList()) }
    var lineChartEntriesForecast by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var minDate by remember { mutableStateOf(Date()) }
    var intervalHelper by remember { mutableStateOf<IntervalHelper?>(null) }
    var unit by remember { mutableStateOf("hm") }
    var label by remember { mutableStateOf("Height meters") }
    var isLoading by remember { mutableStateOf(true) }
    var combinedChart by remember { mutableStateOf<CombinedChart?>(null) }

    val indoorHeightMeterPercent = remember {
        sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
    }

    val xAxisEntries = remember { BarChartXAxisSelector.entries.toList() }
    val yAxisEntries = remember { BarChartYAxisSelector.entries.toList() }
    val zAxisEntries = remember { BarChartZAxisSelector.entries.toList() }
    val unitLabel = stringResource(selectedYAxisSpinnerEntry.unitId)
    val labelLabel = stringResource(selectedYAxisSpinnerEntry.nameId)
    val allLabel = stringResource(R.string.all)
    val symbols = DateFormatSymbols()
    val monthNames = remember {
        mutableListOf(allLabel).apply {
            addAll(symbols.shortMonths.toList())
        }
    }

    val textColor = if (isDark) Color.WHITE else Color.BLACK
    val gridColor = if (isDark) "#444444".toColorInt() else Color.LTGRAY
    val chartBackgroundColor = if (isDark) "#1E1E1E".toColorInt() else Color.WHITE

    // Process data when summits or filters change
    LaunchedEffect(
        filteredSummits,
        dailyActivitySummaryList,
        forecasts,
        selectedXAxisSpinnerEntry,
        selectedYAxisSpinnerEntry,
        selectedZAxisSpinnerEntry,
        selectedXAxisSpinnerMonth,
        includeFilteredDailyActivitySummaries,
    ) {
        if (filteredSummits.isNotEmpty()) {
            isLoading = true
            val summitsToDisplay = if (includeFilteredDailyActivitySummaries) {
                val filteredDailyActivities =
                    findDailyActivitySummariesWhichWasNotAddedToSummits(
                        dailyActivitySummaryList,
                        filteredSummits
                    )
                filteredSummits + parseAsSummit(filteredDailyActivities)
            } else {
                filteredSummits
            }

            minDate = summitsToDisplay.minByOrNull { it.date }?.date ?: Date()
            intervalHelper = IntervalHelper(summitsToDisplay)

            // Move heavy computation to IO thread
            val (barEntries, lineEntries) = withContext(Dispatchers.IO) {
                // Generate bar chart entries
                val barEntries = generateBarChartEntries(
                    summitsToDisplay,
                    selectedXAxisSpinnerEntry,
                    selectedYAxisSpinnerEntry,
                    selectedZAxisSpinnerEntry,
                    selectedXAxisSpinnerMonth,
                    intervalHelper!!,
                    indoorHeightMeterPercent
                )

                // Generate forecast line entries
                val lineEntries = generateForecastLineEntries(
                    forecasts,
                    selectedXAxisSpinnerEntry,
                    selectedYAxisSpinnerEntry,
                    selectedXAxisSpinnerMonth,
                    intervalHelper!!,
                )

                Pair(barEntries, lineEntries)
            }

            barChartEntries = barEntries
            lineChartEntriesForecast = lineEntries
            unit = unitLabel
            label = labelLabel
            isLoading = false
        } else {
            isLoading = false
        }
    }

    val backgroundColor = if (isDark) {
        androidx.compose.ui.graphics.Color(0xFF121212)
    } else {
        androidx.compose.ui.graphics.Color(0xFFCCCCCC)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Spinner section
        SpinnerSection(
            selectedXAxisSpinnerEntry = selectedXAxisSpinnerEntry,
            selectedYAxisSpinnerEntry = selectedYAxisSpinnerEntry,
            selectedZAxisSpinnerEntry = selectedZAxisSpinnerEntry,
            selectedXAxisSpinnerMonth = selectedXAxisSpinnerMonth,
            includeFilteredDailyActivitySummaries = includeFilteredDailyActivitySummaries,
            onXAxisEntrySelected = { selectedXAxisSpinnerEntry = it },
            onYAxisEntrySelected = { selectedYAxisSpinnerEntry = it },
            onZAxisEntrySelected = { selectedZAxisSpinnerEntry = it },
            onMonthSelected = { selectedXAxisSpinnerMonth = it },
            onIncludeFilteredChanged = { includeFilteredDailyActivitySummaries = it },
            xAxisEntries = xAxisEntries,
            yAxisEntries = yAxisEntries,
            zAxisEntries = zAxisEntries,
            monthNames = monthNames,
            isDarkTheme = isDark
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Chart section
        var selectedEntry by remember { mutableStateOf<BarEntry?>(null) }
        var selectedHighlight by remember { mutableStateOf<Highlight?>(null) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.ui.graphics.Color(chartBackgroundColor))
                .padding(8.dp, top = 0.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (barChartEntries.isNotEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        CombinedChart(ctx).apply {
                            setDrawOrder(
                                arrayOf(
                                    DrawOrder.BUBBLE,
                                    DrawOrder.CANDLE,
                                    DrawOrder.BAR,
                                    DrawOrder.SCATTER,
                                    DrawOrder.LINE
                                )
                            )
                            legend.isWordWrapEnabled = true
                            xAxis.textColor = textColor
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            axisLeft.textColor = textColor
                            axisRight.isEnabled = false
                            legend.textColor = textColor
                            setBackgroundColor(chartBackgroundColor)
                            description.isEnabled = false
                            setTouchEnabled(true)
                            setDrawValueAboveBar(false)

                            combinedChart = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { chart ->
                        updateCombinedChart(
                            combinedChart = chart,
                            barChartEntries = barChartEntries,
                            lineChartEntriesForecast = lineChartEntriesForecast,
                            selectedXAxisSpinnerEntry = selectedXAxisSpinnerEntry,
                            selectedYAxisSpinnerEntry = selectedYAxisSpinnerEntry,
                            selectedZAxisSpinnerEntry = selectedZAxisSpinnerEntry,
                            sharedPreferences = sharedPreferences,
                            intervalHelper = intervalHelper,
                            unit = unit,
                            label = label,
                            textColor = textColor,
                            gridColor = gridColor,
                            chartBackgroundColor = chartBackgroundColor,
                            context = context,
                            onEntrySelected = { entry, highlight ->
                                selectedEntry = entry
                                selectedHighlight = highlight
                            },
                            onNothingSelected = {
                                selectedEntry = null
                                selectedHighlight = null
                            }
                        )
                    }
                )

                // Compose-based marker overlay
                intervalHelper?.let { helper ->
                    selectedEntry?.let { entry ->
                        selectedHighlight?.let { highlight ->
                            ChartMarkerCompose(
                                entry = entry,
                                highlight = highlight,
                                selectedXAxisSpinnerEntry = selectedXAxisSpinnerEntry,
                                selectedZAxisSpinnerEntry = selectedZAxisSpinnerEntry,
                                minDate = minDate,
                                unit = unit,
                                intervalHelper = helper,
                                lineChartEntriesForecast = lineChartEntriesForecast,
                                isDark = isDark,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun SpinnerSection(
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    selectedYAxisSpinnerEntry: BarChartYAxisSelector,
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    selectedXAxisSpinnerMonth: Int,
    includeFilteredDailyActivitySummaries: Boolean,
    onXAxisEntrySelected: (BarChartXAxisSelector) -> Unit,
    onYAxisEntrySelected: (BarChartYAxisSelector) -> Unit,
    onZAxisEntrySelected: (BarChartZAxisSelector) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onIncludeFilteredChanged: (Boolean) -> Unit,
    xAxisEntries: List<BarChartXAxisSelector>,
    yAxisEntries: List<BarChartYAxisSelector>,
    zAxisEntries: List<BarChartZAxisSelector>,
    monthNames: List<String>,
    isDarkTheme: Boolean
) {
    var showXAxisDropdown by remember { mutableStateOf(false) }
    var showYAxisDropdown by remember { mutableStateOf(false) }
    var showZAxisDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val textColor =
        if (isDarkTheme) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
    val backgroundColor =
        if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // X, Y, Z Axis Spinners in one line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // X Axis Spinner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.padding(end = 4.dp)
                )

                Box {
                    TextButton(
                        onClick = { showXAxisDropdown = true }
                    ) {
                        Text(
                            text = stringResource(id = selectedXAxisSpinnerEntry.nameId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_drop_down_24),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showXAxisDropdown,
                        onDismissRequest = { showXAxisDropdown = false },
                        containerColor = backgroundColor
                    ) {
                        xAxisEntries.forEach { entry ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(id = entry.nameId),
                                        color = textColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = {
                                    onXAxisEntrySelected(entry)
                                    showXAxisDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Y Axis Spinner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Y",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.padding(end = 4.dp)
                )

                Box {
                    TextButton(
                        onClick = { showYAxisDropdown = true }
                    ) {
                        Text(
                            text = stringResource(id = selectedYAxisSpinnerEntry.nameId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_drop_down_24),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showYAxisDropdown,
                        onDismissRequest = { showYAxisDropdown = false },
                        containerColor = backgroundColor
                    ) {
                        yAxisEntries.forEach { entry ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(id = entry.nameId),
                                        color = textColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = {
                                    onYAxisEntrySelected(entry)
                                    showYAxisDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Z Axis Spinner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Z",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.padding(end = 4.dp)
                )

                Box {
                    TextButton(
                        onClick = { showZAxisDropdown = true }
                    ) {
                        Text(
                            text = stringResource(id = selectedZAxisSpinnerEntry.nameId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_drop_down_24),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showZAxisDropdown,
                        onDismissRequest = { showZAxisDropdown = false },
                        containerColor = backgroundColor
                    ) {
                        zAxisEntries.forEach { entry ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(id = entry.nameId),
                                        color = textColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = {
                                    onZAxisEntrySelected(entry)
                                    showZAxisDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Month spinner (only visible for DateByYear)
        if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYear) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_today_black_24dp),
                    contentDescription = stringResource(id = R.string.choose_data_source),
                    tint = textColor,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(12.dp)
                )

                Box {
                    TextButton(
                        onClick = { showMonthDropdown = true }
                    ) {
                        Text(
                            text = monthNames[selectedXAxisSpinnerMonth],
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_arrow_drop_down_24),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { showMonthDropdown = false },
                        containerColor = backgroundColor
                    ) {
                        monthNames.forEachIndexed { index, monthName ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        monthName,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = {
                                    onMonthSelected(index)
                                    showMonthDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Include filtered daily activity summaries checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Checkbox(
                checked = includeFilteredDailyActivitySummaries,
                onCheckedChange = onIncludeFilteredChanged,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = stringResource(id = R.string.include_not_persisted_activities),
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                modifier = Modifier.padding(start = 4.dp),
                fontSize = 10.sp
            )
        }
    }
}

@Suppress("LongMethod", "ComplexMethod")
private fun updateCombinedChart(
    combinedChart: CombinedChart,
    barChartEntries: List<BarEntry>,
    lineChartEntriesForecast: List<Entry>,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    selectedYAxisSpinnerEntry: BarChartYAxisSelector,
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    sharedPreferences: android.content.SharedPreferences,
    intervalHelper: IntervalHelper?,
    unit: String,
    label: String,
    textColor: Int,
    gridColor: Int,
    chartBackgroundColor: Int,
    context: android.content.Context,
    onEntrySelected: (BarEntry, Highlight) -> Unit,
    onNothingSelected: () -> Unit
) {
    if (intervalHelper == null) return

    combinedChart.setBackgroundColor(chartBackgroundColor)
    combinedChart.xAxis.textColor = textColor
    combinedChart.xAxis.gridColor = gridColor
    combinedChart.axisLeft.textColor = textColor
    combinedChart.axisLeft.gridColor = gridColor
    combinedChart.legend.textColor = textColor

    val combinedData = CombinedData()

    // Set bar data
    val barDataSet = BarDataSet(barChartEntries, label).apply {
        setDrawValues(false)
        highLightColor = Color.RED
        colors = selectedZAxisSpinnerEntry.getColors(context)
        stackLabels = selectedZAxisSpinnerEntry.getStackLabels(context)
    }
    combinedData.setData(BarData(barDataSet))

    // Set line data for forecast
    if (lineChartEntriesForecast.isNotEmpty()) {
        val lineDataSet = LineDataSet(lineChartEntriesForecast, "Forecast").apply {
            setDrawValues(false)
            setDrawCircles(false)
            isHighlightEnabled = false
            color = Color.DKGRAY
            circleHoleColor = Color.RED
            highLightColor = Color.RED
            lineWidth = 2f
            mode = LineDataSet.Mode.STEPPED
        }
        combinedData.setData(LineData(lineDataSet))
    }

    // Configure X axis
    val max = barChartEntries.maxByOrNull { it.x }?.x ?: 0f
    val min = barChartEntries.minByOrNull { it.x }?.x ?: 0f
    combinedChart.xAxis.axisMaximum =
        if (selectedXAxisSpinnerEntry.isAQuality && max < 10) 10.5f else max + 0.5f
    combinedChart.xAxis.axisMinimum = min - 0.5f
    combinedChart.xAxis.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return when (selectedXAxisSpinnerEntry) {
                BarChartXAxisSelector.DateByYear,
                BarChartXAxisSelector.DateByYearUntilToday -> {
                    String.format("%s", value.toInt())
                }

                BarChartXAxisSelector.DateByWeek -> {
                    String.format("%s", value.toInt() % 52)
                }

                BarChartXAxisSelector.DateByMonth -> {
                    String.format(
                        "%s",
                        DateFormatSymbols(context.resources.configuration.locales[0]).months[(value % 12f).toInt()]
                    )
                }

                BarChartXAxisSelector.DateByQuarter -> {
                    String.format("%s", toRomanNumerics((value.toInt() + 1) % 4))
                }

                else -> {
                    if (selectedXAxisSpinnerEntry.isAQuality) {
                        getFormattedValueForQuantity(
                            value,
                            selectedXAxisSpinnerEntry,
                            intervalHelper
                        )
                    } else {
                        String.format(
                            "%s",
                            ((value + 0.5) * selectedXAxisSpinnerEntry.stepsSize).toInt()
                        )
                    }
                }
            }
        }
    }

    // Configure Y axis
    combinedChart.axisLeft.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format(
                context.resources.configuration.locales[0],
                "%.0f %s",
                value,
                unit
            )
        }
    }

    // Add limit line for annual target
    combinedChart.axisLeft.removeAllLimitLines()
    var annualTarget: Float = sharedPreferences.getString(
        selectedYAxisSpinnerEntry.sharedPreferenceKey,
        selectedYAxisSpinnerEntry.defaultAnnualTarget.toString()
    )?.toFloat() ?: selectedYAxisSpinnerEntry.defaultAnnualTarget.toFloat()

    when (selectedXAxisSpinnerEntry) {
        BarChartXAxisSelector.DateByWeek -> annualTarget /= 52f
        BarChartXAxisSelector.DateByYear -> {
            // annual target stays the same for full year view
        }

        BarChartXAxisSelector.DateByMonth -> annualTarget /= 12f
        BarChartXAxisSelector.DateByQuarter -> annualTarget /= 4f
        else -> { /* Do nothing */
        }
    }

    val limitLine = LimitLine(annualTarget)
    combinedChart.axisLeft.addLimitLine(limitLine)

    // Set custom renderer
    val barChartCustomRenderer = BarChartCustomRenderer(
        combinedChart,
        combinedChart.animator,
        combinedChart.viewPortHandler
    )
    combinedChart.renderer = barChartCustomRenderer

    // Set up selection listener to trigger Compose marker
    combinedChart.setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
        override fun onValueSelected(e: Entry?, h: Highlight?) {
            if (e != null && h != null && e is BarEntry) {
                onEntrySelected(e, h)
            } else {
                onNothingSelected()
            }
        }

        override fun onNothingSelected() {
            onNothingSelected()
        }
    })

    combinedChart.data = combinedData
    combinedChart.setVisibleXRangeMinimum(if (barChartEntries.size < 12) (barChartEntries.size + 1).toFloat() else 12f)

    if (selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart != -1f) {
        combinedChart.setVisibleXRangeMaximum(selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart)
        if (!selectedXAxisSpinnerEntry.isAQuality) {
            combinedChart.moveViewToX(
                barChartEntries.maxOf { it.x } - selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart + 1
            )
        }
    }

    combinedChart.invalidate()
}

// Compose-based Chart Marker
@Composable
fun ChartMarkerCompose(
    entry: BarEntry,
    highlight: Highlight,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    minDate: Date,
    unit: String,
    intervalHelper: IntervalHelper,
    lineChartEntriesForecast: List<Entry>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    
    // Get string resources outside of remember blocks
    val weekAbbreviation = stringResource(R.string.calender_wek_abrv)
    val unitLabel = if (selectedXAxisSpinnerEntry.unitId != 0) {
        stringResource(selectedXAxisSpinnerEntry.unitId)
    } else {
        ""
    }
    val forecastAbbreviation = stringResource(R.string.forecast_abbr)
    val zAxisLabel = stringResource(
        selectedZAxisSpinnerEntry.getStringIdForSelectedItem(highlight.stackIndex)
    )

    val value = remember(entry.x, selectedXAxisSpinnerEntry, weekAbbreviation, unitLabel, configuration) {
        when (selectedXAxisSpinnerEntry) {
            BarChartXAxisSelector.DateByYear,
            BarChartXAxisSelector.DateByYearUntilToday -> {
                String.format("%s", entry.x.toInt())
            }

            BarChartXAxisSelector.DateByWeek -> {
                val week = (entry.x % 52f).toInt()
                val year = floor(entry.x / 52f).toInt() + getYear(minDate)
                String.format(
                    "%s %s/%s",
                    weekAbbreviation,
                    week,
                    year
                )
            }

            BarChartXAxisSelector.DateByMonth -> {
                val month = (entry.x % 12f).toInt()
                val year = floor((entry.x + 1f) / 12f).toInt() + getYear(minDate)
                String.format(
                    "%s %s",
                    DateFormatSymbols(configuration.locales[0]).months[month],
                    year
                )
            }

            BarChartXAxisSelector.DateByQuarter -> {
                val quarter = ((entry.x + 1f) % 4f).toInt()
                val year = floor((entry.x + 1f) / 4f).toInt() + getYear(minDate)
                "${toRomanNumerics(quarter)} $year"
            }

            else -> {
                if (selectedXAxisSpinnerEntry.isAQuality) {
                    getFormattedValueForQuantity(
                        entry.x,
                        selectedXAxisSpinnerEntry,
                        intervalHelper
                    )
                } else {
                    String.format(
                        "%s - %s %s",
                        (entry.x * selectedXAxisSpinnerEntry.stepsSize).toInt(),
                        ((entry.x + 1) * selectedXAxisSpinnerEntry.stepsSize).toInt(),
                        unitLabel
                    )
                }
            }
        }
    }

    val selectedValue = entry.yVals?.getOrNull(highlight.stackIndex)?.toInt() ?: 0
    val unitString = if (unit == "") "" else " $unit"
    val forecastValue = lineChartEntriesForecast.find { it.x == entry.x }?.y?.toInt() ?: 0

    val text = remember(selectedValue, forecastValue, value, unitString, forecastAbbreviation, zAxisLabel) {
        if (selectedValue == 0 && forecastValue > 0) {
            String.format(
                "%s%s\n%s\n%s: %s%s",
                entry.y.toInt(),
                unitString,
                value,
                forecastAbbreviation,
                forecastValue,
                unitString
            )
        } else {
            String.format(
                "%s/%s%s\n%s\n%s",
                selectedValue,
                entry.y.toInt(),
                unitString,
                value,
                zAxisLabel
            )
        }
    }

    val backgroundColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
    val textColor = androidx.compose.ui.graphics.Color.White

    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.padding(4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
    }
}

// Helper functions
private fun getYear(date: Date): Int {
    val calendar: Calendar = GregorianCalendar()
    calendar.time = date
    return calendar[Calendar.YEAR]
}

private fun toRomanNumerics(quarter: Int) = when (quarter) {
    1 -> "I"
    2 -> "II"
    3 -> "III"
    else -> "IV"
}

private fun getFormattedValueForQuantity(
    value: Float,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    intervalHelper: IntervalHelper
): String {
    return if (value.toInt() < selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper).second.size) {
        selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper).second[value.toInt()].toString()
    } else {
        ""
    }
}

// Data generation functions
private fun generateBarChartEntries(
    summits: List<Summit>,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    selectedYAxisSpinnerEntry: BarChartYAxisSelector,
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    selectedXAxisSpinnerMonth: Int,
    intervalHelper: IntervalHelper,
    indoorHeightMeterPercent: Int
): List<BarEntry> {
    val entries = mutableListOf<BarEntry>()

    try {
        val rangeAndAnnotation = selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper)
        val interval = rangeAndAnnotation.second
        val annotation = rangeAndAnnotation.first

        for (i in interval.indices) {
            val calendar = Calendar.getInstance(TimeZone.getDefault())
            val streamSupplier = java.util.function.Supplier<java.util.stream.Stream<Summit?>?> {
                selectedXAxisSpinnerEntry.getStream(
                    summits,
                    interval[i],
                    calendar,
                    selectedXAxisSpinnerMonth
                )
            }

            val xValue = annotation[i]
            val yValues = selectedZAxisSpinnerEntry.getValueForEntry(
                streamSupplier,
                selectedYAxisSpinnerEntry,
                indoorHeightMeterPercent
            )

            entries.add(BarEntry(xValue, yValues))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return entries
}

private fun generateForecastLineEntries(
    forecasts: List<Forecast>,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    selectedYAxisSpinnerEntry: BarChartYAxisSelector,
    selectedXAxisSpinnerMonth: Int,
    intervalHelper: IntervalHelper
): List<Entry> {
    val entries = mutableListOf<Entry>()

    if (selectedXAxisSpinnerEntry in listOf(
            BarChartXAxisSelector.DateByMonth,
            BarChartXAxisSelector.DateByYear,
            BarChartXAxisSelector.DateByYearUntilToday,
            BarChartXAxisSelector.DateByQuarter
        )
    ) {
        try {
            val ranges = selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper).second
            val xValuesForecast =
                selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper).first
            val yValuesForecast = ranges.filterIsInstance<ClosedRange<Date>>().map { range ->
                forecasts.sumOf { forecast ->
                    val date = forecast.getDate()
                    val shouldAddThisMonth =
                        selectedXAxisSpinnerEntry != BarChartXAxisSelector.DateByYear ||
                                selectedXAxisSpinnerMonth == 0 || selectedXAxisSpinnerMonth == forecast.month
                    if (date != null && date in range && shouldAddThisMonth) {
                        selectedYAxisSpinnerEntry.getForecastValue(forecast)?.toInt() ?: 0
                    } else {
                        0
                    }
                }
            }

            yValuesForecast.forEachIndexed { index, yValue ->
                entries.add(Entry(xValuesForecast[index] - 0.5f, yValue.toFloat()))
            }

            if (entries.isNotEmpty()) {
                val lastEntry = entries.last()
                entries.add(Entry(lastEntry.x + 1f, lastEntry.y))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return entries
}
