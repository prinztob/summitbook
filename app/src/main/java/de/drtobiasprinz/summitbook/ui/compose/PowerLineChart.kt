package de.drtobiasprinz.summitbook.ui.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.analytics.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.data.analytics.TimeIntervalPower
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.ui.graphics.Color as ComposeColor
import de.drtobiasprinz.summitbook.ui.theme.ChartGold
import de.drtobiasprinz.summitbook.ui.theme.ChartTextDarkGray
import de.drtobiasprinz.summitbook.ui.theme.DarkCanvas
import de.drtobiasprinz.summitbook.ui.theme.DarkGrid
import de.drtobiasprinz.summitbook.ui.theme.RecordGreen
import de.drtobiasprinz.summitbook.ui.theme.SurfaceLightGray

/**
 * Data class representing a data point in the power chart
 */
data class PowerChartDataPoint(
    val x: Float,  // Scaled X value (logarithmic)
    val y: Float,  // Power value in watts
    val originalSeconds: Double  // Original time in seconds for label
)

/**
 * A Compose-based line chart for displaying power profile data
 *
 * @param summit The main summit with power data
 * @param summitToCompare Optional summit to compare against
 * @param extremaValuesAllSummits Extremal values from all summits
 * @param modifier Modifier for the chart container
 */
@Composable
fun PowerLineChart(
    summit: Summit,
    summitToCompare: Summit?,
    extremaValuesAllSummits: ExtremaValuesSummits,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    if (summit.garminData?.power == null) return

    val secLabel = stringResource(R.string.sec)
    val minLabel = stringResource(R.string.min)
    val hLabel = stringResource(R.string.h)
    val powerProfileLabel = stringResource(R.string.power_profile_label)
    val powerProfileCompareLabel = stringResource(R.string.power_profile_compare_label)
    val powerProfileMaxLabel = stringResource(R.string.power_profile_max_label)
    val powerProfileMinLabel = stringResource(R.string.power_profile_min_label)

    // Get chart data for all series
    val mainPowerData = remember(summit) { getPowerChartDataPoints(summit) }
    val comparePowerData = remember(summitToCompare) {
        summitToCompare?.let { getPowerChartDataPoints(it) } ?: emptyList()
    }
    val maxPowerData = remember(extremaValuesAllSummits) {
        getExtremaChartDataPointsMax(extremaValuesAllSummits)
    }
    val minPowerData = remember(extremaValuesAllSummits) {
        getExtremaChartDataPointsMin(extremaValuesAllSummits)
    }

    // Calculate chart bounds
    val allDataPoints = mainPowerData + comparePowerData + maxPowerData + minPowerData
    val minX = scaleLog(1.0)
    val maxX = scaleLog(100000.0)
    val maxY = (allDataPoints.maxOfOrNull { it.y } ?: 0f) * 1.1f
    val minY = 0f

    // Clean, human-readable x-axis ticks (seconds -> label)
    val xTicks = remember(secLabel, minLabel, hLabel) {
        listOf(1, 10, 60, 600, 3600, 18000).map { seconds ->
            val label = when {
                seconds < 60 -> "$seconds $secLabel"
                seconds < 3600 -> "${seconds / 60} $minLabel"
                else -> "${seconds / 3600} $hLabel"
            }
            scaleLog(seconds.toDouble()) to label
        }
    }

    // Calculate colors for each point based on performance
    val pointColors = remember(mainPowerData, maxPowerData, minPowerData) {
        mainPowerData.mapIndexed { index, chartEntry ->
            val maxEntry = maxPowerData.getOrNull(index)
            val minEntry = minPowerData.getOrNull(index)
            if (maxEntry != null && minEntry != null) {
                when {
                    chartEntry.y >= maxEntry.y -> ChartGold // Gold
                    chartEntry.y < minEntry.y -> ComposeColor.Red
                    else -> {
                        val fraction =
                            1f - ((chartEntry.y - minEntry.y) / (maxEntry.y - minEntry.y))
                        lerp(ComposeColor.Green, ComposeColor.Red, fraction)
                    }
                }
            } else {
                ComposeColor.Blue
            }
        }
    }

    val textColor = if (isDark) ComposeColor.White else ComposeColor.Black
    val gridColor = if (isDark) ChartTextDarkGray else ComposeColor.LightGray
    val chartBackgroundColor = if (isDark) DarkCanvas else ComposeColor.White

    // State for selected data point - reset when chart data or bounds change
    var selectedPointIndex by remember(mainPowerData, extremaValuesAllSummits) { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(chartBackgroundColor)
    ) {
        // Chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(mainPowerData, extremaValuesAllSummits, maxY, minY, maxX, minX) {
                        detectTapGestures { offset ->
                            // Find the closest data point to the tap
                            val chartWidth = size.width
                            val chartHeight = size.height
                            val bottomPadding = 40f
                            val effectiveChartHeight = chartHeight - bottomPadding
                            val tapTolerancePx = 50.dp.toPx()

                            var closestIndex = -1
                            var closestDistance = Float.MAX_VALUE

                            mainPowerData.forEachIndexed { index, point ->
                                val screenX = (point.x - minX) / (maxX - minX) * chartWidth
                                val screenY = effectiveChartHeight - (point.y - minY) / (maxY - minY) * effectiveChartHeight
                                val distance = kotlin.math.sqrt(
                                    (offset.x - screenX).pow(2) + (offset.y - screenY).pow(2)
                                )
                                if (distance < closestDistance && distance < tapTolerancePx) {
                                    closestDistance = distance
                                    closestIndex = index
                                }
                            }

                            selectedPointIndex = if (closestIndex >= 0) closestIndex else null
                        }
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                // Reserve space for x-axis labels at the bottom
                val bottomPadding = 40f
                val effectiveChartHeight = chartHeight - bottomPadding

                // Draw grid lines and labels
                drawPowerGridAndLabels(
                    minX = minX,
                    maxX = maxX,
                    minY = minY,
                    maxY = maxY,
                    xTicks = xTicks,
                    gridColor = gridColor,
                    textColor = textColor,
                    bottomPadding = bottomPadding,
                    effectiveChartHeight = effectiveChartHeight
                )

                // Draw filled area between max and min
                if (maxPowerData.isNotEmpty() && minPowerData.isNotEmpty()) {
                    drawFilledAreaBetweenSeries(
                        maxSeries = maxPowerData,
                        minSeries = minPowerData,
                        minX = minX,
                        maxX = maxX,
                        minY = minY,
                        maxY = maxY,
                        chartWidth = chartWidth,
                        chartHeight = effectiveChartHeight,
                        fillColor = ComposeColor.Blue.copy(alpha = 0.2f)
                    )
                }

                // Draw compare power data (if available)
                if (comparePowerData.isNotEmpty()) {
                    drawPowerSeries(
                        data = comparePowerData,
                        minX = minX,
                        maxX = maxX,
                        minY = minY,
                        maxY = maxY,
                        chartWidth = chartWidth,
                        chartHeight = effectiveChartHeight,
                        color = ComposeColor.Gray,
                        lineWidth = 2.5f,
                        filled = true,
                        fillColor = ComposeColor.Gray.copy(alpha = 0.2f)
                    )
                }

                // Draw main power data with colored circles
                drawPowerSeries(
                    data = mainPowerData,
                    minX = minX,
                    maxX = maxX,
                    minY = minY,
                    maxY = maxY,
                    chartWidth = chartWidth,
                    chartHeight = effectiveChartHeight,
                    color = textColor,
                    lineWidth = 4.8f,
                    filled = false,
                    drawCircles = true,
                    pointColors = pointColors,
                    selectedIndex = selectedPointIndex
                )
            }

            // Show tooltip when a point is selected
            selectedPointIndex?.let { index ->
                if (index < mainPowerData.size) {
                    val point = mainPowerData[index]
                    val maxPoint = maxPowerData.getOrNull(index)
                    val minPoint = minPowerData.getOrNull(index)
                    val comparePoint = comparePowerData.getOrNull(index)

                    PowerChartTooltip(
                        point = point,
                        maxPoint = maxPoint,
                        minPoint = minPoint,
                        comparePoint = comparePoint,
                        secLabel = secLabel,
                        textColor = textColor,
                        backgroundColor = if (isDark) DarkGrid else SurfaceLightGray,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        // Legend
        PowerChartLegend(
            series = listOfNotNull(
                LegendItemData(powerProfileLabel, textColor),
                if (comparePowerData.isNotEmpty()) LegendItemData(
                    powerProfileCompareLabel,
                    ComposeColor.Gray
                ) else null,
                LegendItemData(powerProfileMaxLabel, ComposeColor.Blue),
                LegendItemData(powerProfileMinLabel, ComposeColor.Blue)
            ),
            textColor = textColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Draw a single power series on the chart
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPowerSeries(
    data: List<PowerChartDataPoint>,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    chartWidth: Float,
    chartHeight: Float,
    color: ComposeColor,
    lineWidth: Float,
    filled: Boolean = false,
    fillColor: ComposeColor? = null,
    drawCircles: Boolean = false,
    pointColors: List<ComposeColor>? = null,
    selectedIndex: Int? = null
) {
    val screenPoints = data.map { point ->
        val x = if (maxX > minX) {
            (point.x - minX) / (maxX - minX) * chartWidth
        } else {
            chartWidth / 2f
        }
        val y = if (maxY > minY) {
            chartHeight - (point.y - minY) / (maxY - minY) * chartHeight
        } else {
            chartHeight / 2f
        }
        Offset(x, y)
    }

    // Draw filled area if specified
    if (filled && fillColor != null && screenPoints.size > 1) {
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
            color = fillColor,
            style = Fill
        )
    }

    // Draw line
    if (screenPoints.size > 1) {
        for (i in 0 until screenPoints.size - 1) {
            drawLine(
                color = color,
                start = screenPoints[i],
                end = screenPoints[i + 1],
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )
        }
    }

    // Draw circles if specified
    if (drawCircles) {
        screenPoints.forEachIndexed { index, point ->
            val circleColor = if (pointColors != null && index < pointColors.size) {
                pointColors[index]
            } else {
                color
            }
            val isSelected = index == selectedIndex
            val radius = if (isSelected) 10.dp.toPx() else 7.dp.toPx()
            drawCircle(
                color = circleColor,
                radius = radius,
                center = point
            )
            // Draw selection ring
            if (isSelected) {
                drawCircle(
                    color = ComposeColor.White,
                    radius = radius + 2.dp.toPx(),
                    center = point,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

/**
 * Draw filled area between two series (e.g., between max and min)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFilledAreaBetweenSeries(
    maxSeries: List<PowerChartDataPoint>,
    minSeries: List<PowerChartDataPoint>,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    chartWidth: Float,
    chartHeight: Float,
    fillColor: ComposeColor
) {
    val maxScreenPoints = maxSeries.map { point ->
        val x = if (maxX > minX) {
            (point.x - minX) / (maxX - minX) * chartWidth
        } else {
            chartWidth / 2f
        }
        val y = if (maxY > minY) {
            chartHeight - (point.y - minY) / (maxY - minY) * chartHeight
        } else {
            chartHeight / 2f
        }
        Offset(x, y)
    }

    val minScreenPoints = minSeries.map { point ->
        val x = if (maxX > minX) {
            (point.x - minX) / (maxX - minX) * chartWidth
        } else {
            chartWidth / 2f
        }
        val y = if (maxY > minY) {
            chartHeight - (point.y - minY) / (maxY - minY) * chartHeight
        } else {
            chartHeight / 2f
        }
        Offset(x, y)
    }

    if (maxScreenPoints.isNotEmpty() && minScreenPoints.isNotEmpty()) {
        val fillPath = Path().apply {
            // Start with the first point of max series
            moveTo(maxScreenPoints.first().x, maxScreenPoints.first().y)
            // Draw all points of max series
            maxScreenPoints.forEach { point ->
                lineTo(point.x, point.y)
            }
            // Draw the min series in reverse order
            minScreenPoints.reversed().forEach { point ->
                lineTo(point.x, point.y)
            }
            close()
        }

        drawPath(
            path = fillPath,
            color = fillColor,
            style = Fill
        )
    }
}

/**
 * Draw grid lines and axis labels for power chart
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPowerGridAndLabels(
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    xTicks: List<Pair<Float, String>>,
    gridColor: ComposeColor,
    textColor: ComposeColor,
    bottomPadding: Float,
    effectiveChartHeight: Float
) {
    val chartWidth = size.width

    val xLabelPaint = Paint().apply {
        color = textColor.toArgb()
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    val yLabelPaint = Paint().apply {
        color = textColor.toArgb()
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }

    // Draw X axis grid lines and labels (logarithmic time scale)
    xTicks.forEach { (xLog, label) ->
        val xPos = if (maxX > minX) {
            (xLog - minX) / (maxX - minX) * chartWidth
        } else {
            chartWidth / 2f
        }

        // Draw grid line
        drawLine(
            color = gridColor,
            start = Offset(xPos, 0f),
            end = Offset(xPos, effectiveChartHeight),
            strokeWidth = 1f
        )

        // Draw label
        drawContext.canvas.nativeCanvas.drawText(
            label,
            xPos,
            effectiveChartHeight + bottomPadding - 5,
            xLabelPaint
        )
    }

    // Draw Y axis labels (power in watts)
    val ySteps = 5
    for (i in 0..ySteps) {
        val yValue = minY + (maxY - minY) * i / ySteps
        val yPos = effectiveChartHeight - i * effectiveChartHeight / ySteps

        // Draw grid line
        drawLine(
            color = gridColor,
            start = Offset(0f, yPos),
            end = Offset(chartWidth, yPos),
            strokeWidth = 1f
        )

        // Draw label
        val label = "${yValue.toInt()} W"

        drawContext.canvas.nativeCanvas.drawText(
            label,
            10f,
            yPos - 5,
            yLabelPaint
        )
    }
}

/**
 * Tooltip component for showing power values when a point is selected
 */
@Composable
fun PowerChartTooltip(
    point: PowerChartDataPoint,
    maxPoint: PowerChartDataPoint?,
    minPoint: PowerChartDataPoint?,
    comparePoint: PowerChartDataPoint?,
    secLabel: String,
    textColor: ComposeColor,
    backgroundColor: ComposeColor,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val wattLabel = stringResource(R.string.watt)
    val currentLabel = stringResource(R.string.current)
    val maxLabel = stringResource(R.string.max)
    val minLabel = stringResource(R.string.minimum)
    val compareLabel = stringResource(R.string.compare)
    
    // Find matching TimeIntervalPower and use its asLocalizedString method
    val timeLabel = remember(point.originalSeconds) {
        TimeIntervalPower.entries.find { it.seconds == point.originalSeconds.toInt() }
            ?.asLocalizedString(context)
            ?: "$secLabel ${point.originalSeconds.toInt()}"
    }
    
    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Time label
            Text(
                text = timeLabel,
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            // Current value
            Text(
                text = "$currentLabel: ${point.y.toInt()} $wattLabel",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Max value
            maxPoint?.let {
                Text(
                    text = "$maxLabel: ${it.y.toInt()} $wattLabel",
                    color = RecordGreen,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Min value
            minPoint?.let {
                Text(
                    text = "$minLabel: ${it.y.toInt()} $wattLabel",
                    color = ComposeColor.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Compare value
            comparePoint?.let {
                Text(
                    text = "$compareLabel: ${it.y.toInt()} $wattLabel",
                    color = ComposeColor.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Legend component for the power chart
 */
@Composable
fun PowerChartLegend(
    series: List<LegendItemData>,
    textColor: ComposeColor,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkCanvas else ComposeColor.White

    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(series) { item ->
                PowerLegendItem(
                    name = item.name,
                    color = item.color,
                    textColor = textColor
                )
            }
        }
    }
}

/**
 * Data class for legend items
 */
data class LegendItemData(
    val name: String,
    val color: ComposeColor
)

/**
 * Individual legend item
 */
@Composable
private fun PowerLegendItem(
    name: String,
    color: ComposeColor,
    textColor: ComposeColor
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(
                color = color,
                radius = 6.dp.toPx()
            )
        }
        Text(
            text = name,
            color = textColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Get chart data points from a summit's power profile
 */
private fun getPowerChartDataPoints(summit: Summit): List<PowerChartDataPoint> {
    return TimeIntervalPower.entries.mapNotNull { interval ->
        val watts = interval.value(summit)
        if (watts > 0) {
            PowerChartDataPoint(
                scaleLog(interval.seconds.toDouble()),
                watts.toFloat(),
                interval.seconds.toDouble()
            )
        } else {
            null
        }
    }
}

/**
 * Get chart data points for max values from ExtremaValuesSummits
 */
private fun getExtremaChartDataPointsMax(extremaValuesSummits: ExtremaValuesSummits): List<PowerChartDataPoint> {
    return TimeIntervalPower.entries.map {
        PowerChartDataPoint(
            scaleLog(it.seconds.toDouble()),
            it.maxPower(extremaValuesSummits),
            it.seconds.toDouble()
        )
    }
}

/**
 * Get chart data points for min values from ExtremaValuesSummits
 */
private fun getExtremaChartDataPointsMin(extremaValuesSummits: ExtremaValuesSummits): List<PowerChartDataPoint> {
    return TimeIntervalPower.entries.map {
        PowerChartDataPoint(
            scaleLog(it.seconds.toDouble()),
            it.minPower(extremaValuesSummits),
            it.seconds.toDouble()
        )
    }
}

/**
 * Scale value using logarithm (for logarithmic X-axis)
 */
private fun scaleLog(value: Double): Float {
    return log10(value).toFloat()
}