package de.drtobiasprinz.summitbook.ui.compose

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
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
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun BarChartScreen(
    filteredSummits: List<Summit>,
    forecasts: List<Forecast>,
    dailyActivitySummaryList: List<DailyActivitySummary>,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sharedPreferences =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    var selectedXAxisSpinnerEntry by remember { mutableStateOf(BarChartXAxisSelector.DateByMonth) }
    var selectedYAxisSpinnerEntry by remember { mutableStateOf(BarChartYAxisSelector.TotalActivities) }
    var selectedZAxisSpinnerEntry by remember { mutableStateOf(BarChartZAxisSelector.PerSportGroup) }
    var selectedXAxisSpinnerMonth by remember { mutableStateOf(0) }
    var includeFilteredDailyActivitySummaries by remember { mutableStateOf(false) }

    var barChartEntries by remember { mutableStateOf<List<BarChartDataPoint>>(emptyList()) }
    var lineChartEntriesForecast by remember { mutableStateOf<List<LineChartDataPoint>>(emptyList()) }
    var selectedDataPoint by remember { mutableStateOf<BarChartDataPoint?>(null) }
    var minDate by remember { mutableStateOf(Date()) }
    var intervalHelper by remember { mutableStateOf<IntervalHelper?>(null) }
    var filteredDailyActivitySummaries by remember {
        mutableStateOf<List<DailyActivitySummary>>(
            emptyList()
        )
    }
    var unit by remember { mutableStateOf("hm") }
    var label by remember { mutableStateOf("Height meters") }

    var showXAxisDropdown by remember { mutableStateOf(false) }
    var showYAxisDropdown by remember { mutableStateOf(false) }
    var showZAxisDropdown by remember { mutableStateOf(false) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val xAxisEntries = remember { BarChartXAxisSelector.entries.toList() }
    val yAxisEntries = remember { BarChartYAxisSelector.entries.toList() }
    val zAxisEntries = remember { BarChartZAxisSelector.entries.toList() }
    val nameLabel = stringResource(selectedYAxisSpinnerEntry.nameId)
    val unitLabel = stringResource(selectedYAxisSpinnerEntry.unitId)
    val weekLabel = stringResource(R.string.calender_wek_abrv)
    val allLabel = stringResource(R.string.all)
    val symbols = DateFormatSymbols()
    val monthNames = remember {
        mutableListOf(allLabel).apply {
            addAll(symbols.shortMonths.toList())
        }
    }

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
            val summitsToDisplay = if (includeFilteredDailyActivitySummaries) {
                val filteredDailyActivities =
                    findDailyActivitySummariesWhichWasNotAddedToSummits(
                        dailyActivitySummaryList,
                        filteredSummits
                    )
                filteredDailyActivitySummaries = filteredDailyActivities
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
                    minDate,
                    sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0),
                    weekLabel
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
        }

        // Update labels after background work
        label = nameLabel
        unit = unitLabel
    }

    val isDarkTheme = when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        else -> false
    }

    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFCCCCCC)
    val chartBackgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val gridColor = if (isDarkTheme) Color(0xFF444444) else Color.LightGray

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(2.dp)
    ) {
        // Spinner section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        ) {
            // X, Y, Z Axis Spinners in one line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 1.dp),
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
                        modifier = Modifier.padding(end = 2.dp)
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
                            properties = PopupProperties(focusable = true),
                            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                        ) {
                            xAxisEntries.forEach { entry ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(id = entry.nameId),
                                            color = if (isDarkTheme) Color.White else Color.Black,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = {
                                        selectedXAxisSpinnerEntry = entry
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
                        modifier = Modifier.padding(end = 2.dp)
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
                            properties = PopupProperties(focusable = true),
                            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                        ) {
                            yAxisEntries.forEach { entry ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(id = entry.nameId),
                                            color = if (isDarkTheme) Color.White else Color.Black,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = {
                                        selectedYAxisSpinnerEntry = entry
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
                        modifier = Modifier.padding(end = 2.dp)
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
                            properties = PopupProperties(focusable = true),
                            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                        ) {
                            zAxisEntries.forEach { entry ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(id = entry.nameId),
                                            color = if (isDarkTheme) Color.White else Color.Black,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = {
                                        selectedZAxisSpinnerEntry = entry
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
                        .padding(bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_today_black_24dp),
                        contentDescription = stringResource(id = R.string.choose_data_source),
                        tint = textColor,
                        modifier = Modifier
                            .padding(end = 2.dp)
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
                            expanded = showMonthDropdown && selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYear,
                            onDismissRequest = { showMonthDropdown = false },
                            properties = PopupProperties(focusable = true),
                            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                        ) {
                            monthNames.forEachIndexed { index, monthName ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            monthName,
                                            color = if (isDarkTheme) Color.White else Color.Black,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = {
                                        selectedXAxisSpinnerMonth = index
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includeFilteredDailyActivitySummaries,
                    onCheckedChange = { includeFilteredDailyActivitySummaries = it },
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = stringResource(id = R.string.include_not_persisted_activities),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    modifier = Modifier.padding(start = 2.dp),
                    fontSize = 10.sp
                )
            }
        }

        // Chart section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(chartBackgroundColor)
        ) {
            if (barChartEntries.isNotEmpty()) {
                BarChart(
                    barDataPoints = barChartEntries,
                    lineDataPoints = lineChartEntriesForecast,
                    onDataPointSelected = { selectedDataPoint = it },
                    selectedXAxisSpinnerEntry = selectedXAxisSpinnerEntry,
                    selectedYAxisSpinnerEntry = selectedYAxisSpinnerEntry,
                    selectedZAxisSpinnerEntry = selectedZAxisSpinnerEntry,
                    selectedXAxisSpinnerMonth = selectedXAxisSpinnerMonth,
                    context = context,
                    isDarkTheme = isDarkTheme,
                    gridColor = gridColor,
                    textColor = textColor,
                    unit = unit,
                    minDate = minDate
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }

        // Legend section
        Legend(
            selectedZAxisSpinnerEntry = selectedZAxisSpinnerEntry,
            isDarkTheme = isDarkTheme,
            context = context
        )
    }
}

// Data classes for chart data points
data class BarChartDataPoint(
    val x: Float,
    val yValues: FloatArray, // Stacked values
    val label: String
)

data class LineChartDataPoint(
    val x: Float,
    val y: Float
)

@Composable
fun BarChart(
    barDataPoints: List<BarChartDataPoint>,
    lineDataPoints: List<LineChartDataPoint>,
    onDataPointSelected: (BarChartDataPoint?) -> Unit,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    selectedYAxisSpinnerEntry: BarChartYAxisSelector,
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    selectedXAxisSpinnerMonth: Int,
    context: android.content.Context,
    isDarkTheme: Boolean,
    gridColor: Color,
    textColor: Color,
    unit: String,
    minDate: Date
) {
    val configuration = LocalConfiguration.current

    // Calculate chart bounds
    val minX = barDataPoints.minOfOrNull { it.x } ?: 0f
    val maxX = barDataPoints.maxOfOrNull { it.x } ?: 1f
    val minY = 0f // Bar charts typically start at 0
    val maxY = barDataPoints.maxOfOrNull { it.yValues.sum() } ?: 1f

    // Add some padding to the bounds
    val xRange = if (maxX - minX > 0) maxX - minX else 1f
    val yRange = if (maxY - minY > 0) maxY - minY else 1f
    val paddedMinX = minX - xRange * 0.05f
    val paddedMaxX = maxX + xRange * 0.05f
    val paddedMinY = minY - yRange * 0.1f
    val paddedMaxY = maxY + yRange * 0.1f

    var selectedDataPoint by remember { mutableStateOf<BarChartDataPoint?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(barDataPoints) {
                    detectTapGestures { offset ->
                        val chartWidth = size.width

                        val tappedX = offset.x

                        // Find the closest data point
                        val closestPoint = barDataPoints.minByOrNull { point ->
                            val screenX =
                                (point.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                            kotlin.math.abs(screenX - tappedX)
                        }

                        if (closestPoint != null) {
                            val screenX =
                                (closestPoint.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth

                            // Check if tap is close enough to the point (within 50 pixels)
                            if (kotlin.math.abs(screenX - tappedX) < 50) {
                                selectedDataPoint = closestPoint
                                onDataPointSelected(closestPoint)
                            } else {
                                selectedDataPoint = null
                                onDataPointSelected(null)
                            }
                        } else {
                            selectedDataPoint = null
                            onDataPointSelected(null)
                        }
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

            // Draw grid lines and labels
            drawGridAndLabels(
                minX = paddedMinX,
                maxX = paddedMaxX,
                minY = minY,
                maxY = maxY,
                paddedMinY = paddedMinY,
                paddedMaxY = paddedMaxY,
                selectedXAxisSpinnerEntry = selectedXAxisSpinnerEntry,
                configuration = configuration,
                unit = unit,
                gridColor = gridColor,
                textColor = textColor,
                minDate = minDate
            )

            // Draw limit line for annual target
            val sharedPreferences =
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            var annualTarget: Float = sharedPreferences.getString(
                selectedYAxisSpinnerEntry.sharedPreferenceKey,
                selectedYAxisSpinnerEntry.defaultAnnualTarget.toString()
            )?.toFloat() ?: selectedYAxisSpinnerEntry.defaultAnnualTarget.toFloat()

            when (selectedXAxisSpinnerEntry) {
                BarChartXAxisSelector.DateByWeek -> {
                    annualTarget /= 52f
                }

                BarChartXAxisSelector.DateByYear -> {
                    if (selectedXAxisSpinnerMonth != 0) {
                        annualTarget /= 12f
                    }
                }

                BarChartXAxisSelector.DateByMonth -> {
                    annualTarget /= 12f
                }

                BarChartXAxisSelector.DateByQuarter -> {
                    annualTarget /= 4f
                }

                else -> {
                    // DO NOTHING
                }
            }

            // Convert annual target to screen coordinates
            val targetY =
                chartHeight - (annualTarget - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight

            // Draw limit line for annual target
            drawLine(
                color = Color.Red,
                start = Offset(0f, targetY),
                end = Offset(chartWidth, targetY),
                strokeWidth = 2f
            )

            // Draw bars
            barDataPoints.forEach { point ->
                val x = (point.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth

                // Draw stacked bars
                var currentY = chartHeight
                val colors = selectedZAxisSpinnerEntry.getColors(context)
                val barWidth = chartWidth / barDataPoints.size * 0.8f

                point.yValues.forEachIndexed { index, value ->
                    val barHeight = (value / (paddedMaxY - paddedMinY)) * chartHeight
                    val color = if (index < colors.size) {
                        val colorRes = colors[index]
                        if (colorRes and 0xFF000000.toInt() == 0xFF000000.toInt()) {
                            // This is already a color value
                            Color(colorRes)
                        } else {
                            // This is a resource ID
                            Color(ContextCompat.getColor(context, colorRes))
                        }
                    } else {
                        Color.Gray
                    }

                    drawRect(
                        color = color,
                        topLeft = Offset(x - barWidth / 2, currentY - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )

                    currentY -= barHeight
                }
            }

            // Draw line chart for forecasts
            if (lineDataPoints.size > 1) {
                val linePath = Path().apply {
                    val firstPoint = lineDataPoints.first()
                    val firstX =
                        (firstPoint.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                    val firstY =
                        chartHeight - (firstPoint.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight
                    moveTo(firstX, firstY)

                    for (i in 1 until lineDataPoints.size) {
                        val prevPoint = lineDataPoints[i - 1]
                        val point = lineDataPoints[i]
                        val prevX =
                            (prevPoint.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                        val prevY =
                            chartHeight - (prevPoint.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight
                        val x = (point.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                        val y =
                            chartHeight - (point.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight

                        // Draw stepped line: horizontal line to current x, then vertical line to current y
                        lineTo(x, prevY)  // Horizontal step
                        lineTo(x, y)      // Vertical step
                    }
                }

                drawPath(
                    path = linePath,
                    color = Color.Red,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            // Draw selected point highlight
            selectedDataPoint?.let { dataPoint ->
                val x = (dataPoint.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                val totalY = dataPoint.yValues.sum()
                val y =
                    chartHeight - (totalY - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight

                drawCircle(
                    color = Color.Red,
                    radius = 15f,
                    center = Offset(x, y)
                )
            }
        }

        // Show marker if a data point is selected
        selectedDataPoint?.let { dataPoint ->
            ChartMarker(
                dataPoint = dataPoint,
                selectedYAxisSpinnerEntry = selectedYAxisSpinnerEntry,
                selectedZAxisSpinnerEntry = selectedZAxisSpinnerEntry,
                lineDataPoints = lineDataPoints,
                modifier = Modifier.align(Alignment.TopStart),
                isDarkTheme = isDarkTheme,
                context = context
            )
        }
    }
}

fun DrawScope.drawGridAndLabels(
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    paddedMinY: Float,
    paddedMaxY: Float,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    configuration: Configuration,
    unit: String,
    gridColor: Color,
    textColor: Color,
    minDate: Date
) {
    // Draw Y axis labels
    for (i in 0..4) {
        val yValue = paddedMinY + (paddedMaxY - paddedMinY) * i / 4f
        val format = "%.0f %s"
        val label = String.format(
            configuration.locales[0],
            format,
            yValue,
            unit
        )

        // Draw grid line
        drawLine(
            color = gridColor,
            start = Offset(0f, this.size.height - i * this.size.height / 4f),
            end = Offset(this.size.width, this.size.height - i * this.size.height / 4f),
            strokeWidth = 1f
        )

        // Draw label
        drawContext.canvas.nativeCanvas.drawText(
            label,
            10f,
            this.size.height - i * this.size.height / 4f,
            Paint().apply {
                color = textColor.toArgb()
                textSize = 30f
                textAlign = Paint.Align.LEFT
            }
        )
    }

    // Draw a few X axis labels
    for (i in 0..4) {
        val xValue = minX + (maxX - minX) * i / 4f
        val label = when (selectedXAxisSpinnerEntry) {
            BarChartXAxisSelector.DateByYear, BarChartXAxisSelector.DateByYearUntilToday -> {
                xValue.toInt().toString()
            }

            BarChartXAxisSelector.DateByWeek -> {
                "${xValue.toInt() % 52}"
            }

            BarChartXAxisSelector.DateByMonth -> {
                val month = (xValue % 12f).toInt()
                val year = floor((xValue + 1f) / 12f).toInt() + getYear(minDate)
                "${DateFormatSymbols(configuration.locales[0]).months[month]} $year"
            }

            BarChartXAxisSelector.DateByQuarter -> {
                val quarter = ((xValue + 1f) % 4f).toInt()
                val year = floor((xValue + 1f) / 4f).toInt() + getYear(minDate)
                "${toRomanNumerics(quarter)} $year"
            }

            else -> {
                if (selectedXAxisSpinnerEntry.isAQuality) {
                    // For quality-based selectors, we would need the actual range values
                    xValue.toInt().toString()
                } else {
                    "${((xValue + 0.5) * selectedXAxisSpinnerEntry.stepsSize).toInt()}"
                }
            }
        }

        // Draw grid line
        drawLine(
            color = gridColor,
            start = Offset(i * this.size.width / 4f, 0f),
            end = Offset(i * this.size.width / 4f, this.size.height),
            strokeWidth = 1f
        )

        // Draw label
        drawContext.canvas.nativeCanvas.drawText(
            label,
            i * this.size.width / 4f,
            this.size.height - 10,
            Paint().apply {
                color = textColor.toArgb()
                textSize = 30f
                textAlign = Paint.Align.CENTER
            }
        )
    }
}

@Composable
fun ChartMarker(
    dataPoint: BarChartDataPoint,
    selectedYAxisSpinnerEntry: BarChartYAxisSelector,
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    lineDataPoints: List<LineChartDataPoint>,
    isDarkTheme: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    // context is passed as parameter
    val unit = context.getString(selectedYAxisSpinnerEntry.unitId)
    val stackLabels = selectedZAxisSpinnerEntry.getStackLabels(context)

    // Find corresponding forecast value if available
    val forecastValue = lineDataPoints.find { it.x == dataPoint.x }?.y ?: 0f

    val text = buildString {
        append("${dataPoint.label}\n")

        // Display individual sport type values
        dataPoint.yValues.forEachIndexed { index, value ->
            if (index < stackLabels.size) {
                append("${stackLabels[index]}: ${value.roundToInt()} $unit\n")
            } else {
                append("Item $index: ${value.roundToInt()} $unit\n")
            }
        }

        // Display total
        val total = dataPoint.yValues.sum()
        append("${context.getString(R.string.total)}: ${total.roundToInt()} $unit")

        if (forecastValue > 0) {
            append("\n${context.getString(R.string.forecast_abbr)}: ${forecastValue.roundToInt()} $unit")
        }
    }

    val backgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.Black.copy(alpha = 0.8f)
    val textColor = if (isDarkTheme) Color.White else Color.White

    Surface(
        modifier = modifier
            .padding(8.dp),
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.padding(4.dp),
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        )
    }
}

@Composable
fun Legend(
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    isDarkTheme: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val colors = selectedZAxisSpinnerEntry.getColors(context)
    val labels = selectedZAxisSpinnerEntry.getStackLabels(context)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(backgroundColor, RoundedCornerShape(4.dp))
    ) {
        LazyRow(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(labels.toList().zip(colors.toList())) { (label, colorRes) ->
                Row(
                    modifier = Modifier.padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = if (colorRes and 0xFF000000.toInt() == 0xFF000000.toInt()) {
                        // This is already a color value
                        Color(colorRes)
                    } else {
                        // This is a resource ID
                        Color(ContextCompat.getColor(context, colorRes))
                    }
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx()
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = label,
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getYear(date: Date): Int {
    val calendar: Calendar = Calendar.getInstance()
    calendar.time = date
    return calendar[Calendar.YEAR]
}

private fun toRomanNumerics(quarter: Int) = when (quarter) {
    1 -> "I"
    2 -> "II"
    3 -> "III"
    else -> "IV"
}

// Data generation functions
private fun generateBarChartEntries(
    summits: List<Summit>,
    selectedXAxisSpinnerEntry: BarChartXAxisSelector,
    selectedYAxisSpinnerEntry: BarChartYAxisSelector,
    selectedZAxisSpinnerEntry: BarChartZAxisSelector,
    selectedXAxisSpinnerMonth: Int,
    intervalHelper: IntervalHelper,
    minDate: Date,
    indoorHeightMeterPercent: Int,
    weekLabel: String
): List<BarChartDataPoint> {
    val entries = mutableListOf<BarChartDataPoint>()

    try {
        val rangeAndAnnotation = selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper)
        val interval = rangeAndAnnotation.second
        val annotation = rangeAndAnnotation.first

        for (i in interval.indices) {
            val calender = Calendar.getInstance(TimeZone.getDefault())
            val streamSupplier = java.util.function.Supplier<java.util.stream.Stream<Summit?>?> {
                selectedXAxisSpinnerEntry.getStream(
                    summits,
                    interval[i],
                    calender,
                    selectedXAxisSpinnerMonth
                )
            }

            val xValue = annotation[i]
            val yValues = selectedZAxisSpinnerEntry.getValueForEntry(
                streamSupplier,
                selectedYAxisSpinnerEntry,
                indoorHeightMeterPercent
            )

            val label = when (selectedXAxisSpinnerEntry) {
                BarChartXAxisSelector.DateByYear, BarChartXAxisSelector.DateByYearUntilToday -> {
                    xValue.toInt().toString()
                }

                BarChartXAxisSelector.DateByWeek -> {
                    val week = (xValue % 52f).toInt()
                    val year = floor(xValue / 52f).toInt() + getYear(minDate)
                    "$weekLabel $week/$year"
                }

                BarChartXAxisSelector.DateByMonth -> {
                    val month = (xValue % 12f).toInt()
                    val year = floor((xValue + 1f) / 12f).toInt() + getYear(minDate)
                    "${DateFormatSymbols().months[month]} $year"
                }

                BarChartXAxisSelector.DateByQuarter -> {
                    val quarter = ((xValue + 1f) % 4f).toInt()
                    val year = floor((xValue + 1f) / 4f).toInt() + getYear(minDate)
                    "${toRomanNumerics(quarter)} $year"
                }

                else -> {
                    if (selectedXAxisSpinnerEntry.isAQuality) {
                        // For quality-based selectors
                        xValue.toInt().toString()
                    } else {
                        "${((xValue + 0.5) * selectedXAxisSpinnerEntry.stepsSize).toInt()}"
                    }
                }
            }

            entries.add(BarChartDataPoint(xValue, yValues, label))
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
): List<LineChartDataPoint> {
    val entries = mutableListOf<LineChartDataPoint>()

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
                entries.add(
                    LineChartDataPoint(xValuesForecast[index] - 0.5f, yValue.toFloat())
                )
            }
            if (entries.isNotEmpty()) {
                val lastEntry = entries.last()
                entries.add(LineChartDataPoint(lastEntry.x + 1f, lastEntry.y))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return entries
}