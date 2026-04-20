package de.drtobiasprinz.summitbook.ui.compose

import android.content.res.Configuration
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.ui.utils.TimeIntervalPower
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.ui.graphics.Color as ComposeColor

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
    val configuration = LocalConfiguration.current

    val power = summit.garminData?.power ?: return

    val secLabel = stringResource(R.string.sec)
    val powerProfileLabel = stringResource(R.string.power_profile_label)
    val powerProfileCompareLabel = stringResource(R.string.power_profile_compare_label)
    val powerProfileMaxLabel = stringResource(R.string.power_profile_max_label)
    val powerProfileMinLabel = stringResource(R.string.power_profile_min_label)

    // Get chart data for all series
    val mainPowerData = remember(power) { getPowerChartDataPoints(power) }
    val comparePowerData = remember(summitToCompare?.garminData?.power) {
        summitToCompare?.garminData?.power?.let { getPowerChartDataPoints(it) } ?: emptyList()
    }
    val maxPowerData = remember(extremaValuesAllSummits) {
        getExtremaChartDataPointsMax(extremaValuesAllSummits)
    }
    val minPowerData = remember(extremaValuesAllSummits) {
        getExtremaChartDataPointsMin(extremaValuesAllSummits)
    }

    // Calculate chart bounds
    val allDataPoints = mainPowerData + comparePowerData + maxPowerData + minPowerData
    val minX = scaleCbr(1.0)
    val maxX = scaleCbr(100000.0)
    val maxY = (allDataPoints.maxOfOrNull { it.y } ?: 0f) * 1.1f
    val minY = 0f

    // Calculate colors for each point based on performance
    val pointColors = remember(mainPowerData, maxPowerData, minPowerData) {
        mainPowerData.mapIndexed { index, chartEntry ->
            val maxEntry = maxPowerData.getOrNull(index)
            val minEntry = minPowerData.getOrNull(index)
            if (maxEntry != null && minEntry != null) {
                when {
                    chartEntry.y >= maxEntry.y -> ComposeColor(0xFFFFD700) // Gold
                    chartEntry.y < minEntry.y -> ComposeColor.Red
                    else -> {
                        val fraction =
                            1f - ((chartEntry.y - minEntry.y) / (maxEntry.y - minEntry.y))
                        interpolateColor(ComposeColor.Green, ComposeColor.Red, fraction)
                    }
                }
            } else {
                ComposeColor.Blue
            }
        }
    }

    val textColor = if (isDark) ComposeColor.White else ComposeColor.Black
    val gridColor = if (isDark) ComposeColor(0xFF444444) else ComposeColor.LightGray
    val chartBackgroundColor = if (isDark) ComposeColor(0xFF1E1E1E) else ComposeColor.White

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

                            var closestIndex = -1
                            var closestDistance = Float.MAX_VALUE

                            mainPowerData.forEachIndexed { index, point ->
                                val screenX = (point.x - minX) / (maxX - minX) * chartWidth
                                val screenY = effectiveChartHeight - (point.y - minY) / (maxY - minY) * effectiveChartHeight
                                val distance = kotlin.math.sqrt(
                                    (offset.x - screenX).pow(2) + (offset.y - screenY).pow(2)
                                )
                                if (distance < closestDistance && distance < 50f) { // 50f is tap tolerance
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
                    configuration = configuration,
                    secLabel = secLabel,
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
                    color = ComposeColor.Black,
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
                        backgroundColor = if (isDark) ComposeColor(0xFF333333) else ComposeColor(0xFFF5F5F5),
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        // Legend
        PowerChartLegend(
            series = listOfNotNull(
                LegendItemData(powerProfileLabel, ComposeColor.Black),
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
    configuration: Configuration,
    secLabel: String,
    gridColor: ComposeColor,
    textColor: ComposeColor,
    bottomPadding: Float,
    effectiveChartHeight: Float
) {
    val chartWidth = size.width

    // Draw X axis labels (logarithmic time scale)
    val xSteps = 6
    for (i in 0..xSteps) {
        val xValue = minX + (maxX - minX) * i / xSteps
        val xPos = i * chartWidth / xSteps

        // Draw grid line
        drawLine(
            color = gridColor,
            start = Offset(xPos, 0f),
            end = Offset(xPos, effectiveChartHeight),
            strokeWidth = 1f
        )

        // Draw label
        val unscaled = unScaleCbr(xValue.toDouble()).toInt()
        val label = String.format(configuration.locales[0], "%s $secLabel", unscaled)

        drawContext.canvas.nativeCanvas.drawText(
            label,
            xPos,
            effectiveChartHeight + bottomPadding - 5,
            Paint().apply {
                color = textColor.toArgb()
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
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
            Paint().apply {
                color = textColor.toArgb()
                textSize = 24f
                textAlign = Paint.Align.LEFT
            }
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
                    color = ComposeColor(0xFF4CAF50),
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
    val backgroundColor = if (isDark) ComposeColor(0xFF1E1E1E) else ComposeColor.White

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
 * Get chart data points from PowerData
 */
private fun getPowerChartDataPoints(power: PowerData): List<PowerChartDataPoint> {
    val dataPoints = mutableListOf<PowerChartDataPoint>()
    if (power.oneSec > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(1.0),
            power.oneSec.toFloat(),
            1.0
        )
    )
    if (power.twoSec > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(2.0),
            power.twoSec.toFloat(),
            2.0
        )
    )
    if (power.fiveSec > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(5.0),
            power.fiveSec.toFloat(),
            5.0
        )
    )
    if (power.tenSec > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(10.0),
            power.tenSec.toFloat(),
            10.0
        )
    )
    if (power.twentySec > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(20.0),
            power.twentySec.toFloat(),
            20.0
        )
    )
    if (power.thirtySec > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(30.0),
            power.thirtySec.toFloat(),
            30.0
        )
    )
    if (power.oneMin > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(60.0),
            power.oneMin.toFloat(),
            60.0
        )
    )
    if (power.twoMin > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(120.0),
            power.twoMin.toFloat(),
            120.0
        )
    )
    if (power.fiveMin > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(300.0),
            power.fiveMin.toFloat(),
            300.0
        )
    )
    if (power.tenMin > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(600.0),
            power.tenMin.toFloat(),
            600.0
        )
    )
    if (power.twentyMin > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(1200.0),
            power.twentyMin.toFloat(),
            1200.0
        )
    )
    if (power.thirtyMin > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(1800.0),
            power.thirtyMin.toFloat(),
            1800.0
        )
    )
    if (power.oneHour > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(3600.0),
            power.oneHour.toFloat(),
            3600.0
        )
    )
    if (power.twoHours > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(7200.0),
            power.twoHours.toFloat(),
            7200.0
        )
    )
    if (power.threeHours > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(10800.0),
            power.threeHours.toFloat(),
            10800.0
        )
    )
    if (power.fourHours > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(14400.0),
            power.fourHours.toFloat(),
            14400.0
        )
    )
    if (power.fiveHours > 0) dataPoints.add(
        PowerChartDataPoint(
            scaleCbr(18000.0),
            power.fiveHours.toFloat(),
            18000.0
        )
    )
    return dataPoints
}

/**
 * Get chart data points for max values from ExtremaValuesSummits
 */
private fun getExtremaChartDataPointsMax(extremaValuesSummits: ExtremaValuesSummits): List<PowerChartDataPoint> {
    return TimeIntervalPower.entries.map {
        PowerChartDataPoint(
            scaleCbr(it.seconds.toDouble()),
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
            scaleCbr(it.seconds.toDouble()),
            it.minPower(extremaValuesSummits),
            it.seconds.toDouble()
        )
    }
}

/**
 * Scale value using cube root logarithm (for logarithmic X-axis)
 */
private fun scaleCbr(cbr: Double): Float {
    return log10(cbr).toFloat()
}

/**
 * Unscale value from cube root logarithm
 */
private fun unScaleCbr(cbr: Double): Float {
    val calcVal = 10.0.pow(cbr)
    return calcVal.toFloat()
}

/**
 * Interpolate between two colors
 */
private fun interpolateColor(
    color1: ComposeColor,
    color2: ComposeColor,
    fraction: Float
): ComposeColor {
    val r1 = android.graphics.Color.red(color1.toArgb())
    val g1 = android.graphics.Color.green(color1.toArgb())
    val b1 = android.graphics.Color.blue(color1.toArgb())

    val r2 = android.graphics.Color.red(color2.toArgb())
    val g2 = android.graphics.Color.green(color2.toArgb())
    val b2 = android.graphics.Color.blue(color2.toArgb())

    val r = (r1 + (r2 - r1) * fraction).toInt()
    val g = (g1 + (g2 - g1) * fraction).toInt()
    val b = (b1 + (b2 - b1) * fraction).toInt()

    return ComposeColor(android.graphics.Color.rgb(r, g, b))
}