package de.drtobiasprinz.summitbook.ui.compose

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.Fill
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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.OrderBySpinnerEntry
import de.drtobiasprinz.summitbook.utils.Constants.DATE_FORMAT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import kotlin.math.abs

@Composable
fun LineChartScreen(
    filteredSummits: List<Summit>,
    onNavigateToSummitDetails: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val primaryColor = MaterialTheme.colorScheme.primary

    var lineChartSpinnerEntry by remember { mutableStateOf(OrderBySpinnerEntry.HeightMeter) }
    var lineChartEntries by remember { mutableStateOf<List<ChartDataPoint>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }

    val spinnerEntries = remember {
        OrderBySpinnerEntry.getSpinnerEntriesWithoutExcludedFromLineChart()
    }

    // Process data when summits change
    LaunchedEffect(filteredSummits, lineChartSpinnerEntry) {
        withContext(Dispatchers.IO) {
            // Set line chart entries
            val useEntries = filteredSummits.filter {
                val value = lineChartSpinnerEntry.f(it)
                if (value != null) {
                    if (!lineChartSpinnerEntry.includeIndoorActivities) {
                        if (it.sportType == SportType.IndoorTrainer) false else value > 0
                    } else {
                        value > 0
                    }
                } else {
                    false
                }
            }.sortedBy { it.date }

            var accumulator = 0f
            val colors = useEntries.map {
                val colorRes = ContextCompat.getColor(context, it.sportType.color)
                Color(colorRes)
            }

            val entries = useEntries.map {
                val value = if (!lineChartSpinnerEntry.accumulate) {
                    lineChartSpinnerEntry.f(it)
                } else {
                    accumulator += lineChartSpinnerEntry.f(it) ?: 0f
                    accumulator
                }
                ChartDataPoint(
                    x = it.getDateAsFloat(),
                    y = value ?: 0f,
                    summit = it,
                    color = colors.getOrNull(useEntries.indexOf(it)) ?: Color.Black
                )
            }

            lineChartEntries = entries
        }
    }

    // Determine if dark theme is enabled
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
            .padding(8.dp)
    ) {
        // Spinner section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_trending_up_black_24dp),
                contentDescription = stringResource(id = R.string.choose_data_source),
                modifier = Modifier.padding(end = 8.dp),
                tint = textColor
            )

            Box {
                TextButton(
                    onClick = { showDropdown = true }
                ) {
                    Text(
                        text = stringResource(id = lineChartSpinnerEntry.nameId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_arrow_drop_down_24),
                        contentDescription = null,
                        tint = textColor
                    )
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    properties = PopupProperties(focusable = true),
                    containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                ) {
                    spinnerEntries.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(id = entry.nameId),
                                    color = if (isDarkTheme) Color.White else Color.Black
                                )
                            },
                            onClick = {
                                lineChartSpinnerEntry = entry
                                showDropdown = false
                            }
                        )
                    }
                }
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
            if (lineChartEntries.isNotEmpty()) {
                LineChart(
                    dataPoints = lineChartEntries,
                    onDataPointSelected = { _ -> },
                    lineChartSpinnerEntry = lineChartSpinnerEntry,
                    onNavigateToSummitDetails = onNavigateToSummitDetails,
                    isDarkTheme = isDarkTheme,
                    gridColor = gridColor,
                    textColor = textColor,
                    unit = stringResource(lineChartSpinnerEntry.unit)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }
            }
        }

        // Legend section
        Legend(
            lineChartSpinnerEntry = lineChartSpinnerEntry,
            sportTypes = SportType.entries.toList(),
            isDarkTheme = isDarkTheme,
            primaryColor = primaryColor
        )
    }
}

data class ChartDataPoint(
    val x: Float,
    val y: Float,
    val summit: Summit? = null,
    val color: Color = Color.Black
)

@Composable
fun LineChart(
    dataPoints: List<ChartDataPoint>,
    onDataPointSelected: (ChartDataPoint?) -> Unit,
    lineChartSpinnerEntry: OrderBySpinnerEntry,
    onNavigateToSummitDetails: (Long) -> Unit,
    isDarkTheme: Boolean,
    gridColor: Color,
    textColor: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val primaryColor = MaterialTheme.colorScheme.primary

    // Calculate chart bounds
    val minX = dataPoints.minOfOrNull { it.x } ?: 0f
    val maxX = dataPoints.maxOfOrNull { it.x } ?: 1f
    val minY = dataPoints.minOfOrNull { it.y } ?: 0f
    val maxY = dataPoints.maxOfOrNull { it.y } ?: 1f

    // Add some padding to the bounds
    val xRange = maxX - minX
    val yRange = maxY - minY
    val paddedMinX = minX - xRange * 0.05f
    val paddedMaxX = maxX + xRange * 0.05f
    val paddedMinY = minY - yRange * 0.1f
    val paddedMaxY = maxY + yRange * 0.1f

    var selectedDataPoint by remember { mutableStateOf<ChartDataPoint?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        // Find the closest data point to the tap
                        val chartWidth = size.width
                        val chartHeight = size.height

                        val tappedX = offset.x
                        val tappedY = offset.y

                        // Find the closest data point
                        val closestPoint = dataPoints.minByOrNull { point ->
                            val screenX =
                                (point.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                            abs(screenX - tappedX)
                        }

                        if (closestPoint != null) {
                            val screenX =
                                (closestPoint.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                            val screenY =
                                chartHeight - (closestPoint.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight

                            // Check if tap is close enough to the point (within 50 pixels)
                            if (abs(screenX - tappedX) < 50 && abs(screenY - tappedY) < 50) {
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
            this.drawGridAndLabels(
                minX = paddedMinX,
                maxX = paddedMaxX,
                minY = paddedMinY,
                maxY = paddedMaxY,
                lineChartSpinnerEntry = lineChartSpinnerEntry,
                configuration = configuration,
                unit = unit,
                gridColor = gridColor,
                textColor = textColor
            )

            // Convert data points to screen coordinates
            val screenPoints = dataPoints.map { point ->
                val x = (point.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                val y =
                    chartHeight - (point.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight
                Offset(x, y)
            }

            // Draw filled area under curve
            if (screenPoints.size > 1) {
                val fillPath = Path().apply {
                    moveTo(screenPoints.first().x, chartHeight)
                    screenPoints.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                    lineTo(screenPoints.last().x, chartHeight)
                    close()
                }

                drawPath(
                    path = fillPath,
                    color = primaryColor.copy(alpha = 0.3f),
                    style = Fill
                )
            }

            // Draw line
            if (screenPoints.size > 1) {
                val linePath = Path().apply {
                    moveTo(screenPoints.first().x, screenPoints.first().y)
                    for (i in 1 until screenPoints.size) {
                        val prev = screenPoints[i - 1]
                        val current = screenPoints[i]
                        cubicTo(
                            x1 = (prev.x + current.x) / 2,
                            y1 = prev.y,
                            x2 = (prev.x + current.x) / 2,
                            y2 = current.y,
                            x3 = current.x,
                            y3 = current.y
                        )
                    }
                }

                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            // Draw data points
            screenPoints.forEachIndexed { index, point ->
                val color = dataPoints[index].color
                drawCircle(
                    color = color,
                    radius = 10f,
                    center = point
                )
            }

            // Draw selected point highlight
            selectedDataPoint?.let { dataPoint ->
                val selectedIndex = dataPoints.indexOfFirst { it === dataPoint }
                if (selectedIndex >= 0 && selectedIndex < screenPoints.size) {
                    val point = screenPoints[selectedIndex]
                    drawCircle(
                        color = Color.Red,
                        radius = 15f,
                        center = point
                    )
                }
            }
        }

        // Show marker if a data point is selected
        selectedDataPoint?.let { dataPoint ->
            ChartMarker(
                dataPoint = dataPoint,
                lineChartSpinnerEntry = lineChartSpinnerEntry,
                onNavigateToSummitDetails = onNavigateToSummitDetails,
                modifier = Modifier.align(Alignment.TopStart),
                isDarkTheme = isDarkTheme
            )
        }
    }
}

fun DrawScope.drawGridAndLabels(
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    lineChartSpinnerEntry: OrderBySpinnerEntry,
    configuration: Configuration,
    unit: String,
    gridColor: Color,
    textColor: Color
) {
    // Draw X axis labels (dates)
    val dateFormat = SimpleDateFormat(DATE_FORMAT, configuration.locales[0])

    // Draw a few X axis labels
    for (i in 0..4) {
        val xValue = minX + (maxX - minX) * i / 4f
        val date = Summit.getDateFromFloat(xValue)
        val dateString = dateFormat.format(date)

        // Draw grid line
        drawLine(
            color = gridColor,
            start = Offset(i * this.size.width / 4f, 0f),
            end = Offset(i * this.size.width / 4f, this.size.height),
            strokeWidth = 1f
        )

        // Draw label
        drawContext.canvas.nativeCanvas.drawText(
            dateString,
            i * this.size.width / 4f,
            this.size.height - 10,
            Paint().apply {
                color = textColor.toArgb()
                textSize = 30f
                textAlign = Paint.Align.CENTER
            }
        )
    }

    // Draw Y axis labels
    for (i in 0..4) {
        val yValue = minY + (maxY - minY) * i / 4f
        val format =
            if (lineChartSpinnerEntry == OrderBySpinnerEntry.Vo2Max || yValue < 10) "%.1f %s" else "%.0f %s"
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
}

@Composable
fun ChartMarker(
    dataPoint: ChartDataPoint,
    lineChartSpinnerEntry: OrderBySpinnerEntry,
    onNavigateToSummitDetails: (Long) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val value = dataPoint.summit?.let { lineChartSpinnerEntry.f(it) } ?: 0f
    val format =
        if (lineChartSpinnerEntry == OrderBySpinnerEntry.Vo2Max || value < 10) "%s\n%s\n%.1f %s" else "%s\n%s\n%.0f %s"
    val unit = stringResource(lineChartSpinnerEntry.unit)
    val text = String.format(
        format,
        dataPoint.summit?.name,
        dataPoint.summit?.getDateAsString(),
        value,
        unit
    )

    val backgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.Black.copy(alpha = 0.8f)
    val textColor = if (isDarkTheme) Color.White else Color.White

    Surface(
        modifier = modifier
            .padding(16.dp)
            .clickable { dataPoint.summit?.id?.let { onNavigateToSummitDetails(it) } },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = text,
            color = textColor,
            modifier = Modifier.padding(8.dp),
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
        )
    }
}

@Composable
fun Legend(
    lineChartSpinnerEntry: OrderBySpinnerEntry,
    sportTypes: List<SportType>,
    isDarkTheme: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp))
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main legend entry
            item {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(
                            color = primaryColor,
                            radius = 6.dp.toPx()
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = lineChartSpinnerEntry.nameId),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Sport type legend entries
            items(sportTypes) { sportType ->
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = Color(ContextCompat.getColor(context, sportType.color))
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(
                            color = color,
                            radius = 6.dp.toPx()
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = sportType.sportNameStringId),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}