package de.drtobiasprinz.summitbook.ui.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.summitbook.ui.GraphType
import de.drtobiasprinz.summitbook.ui.PerformanceGraphProvider
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Data class representing a data series in the chart
 */
data class ChartSeries(
    val name: String,
    val data: List<ChartDataPoint>,
    val color: ComposeColor,
    val lineWidth: Float = 2f,
    val filled: Boolean = false,
    val fillColor: ComposeColor? = null,
    val fillAlpha: Float = 0.3f,
    val drawCircles: Boolean = false,
    val pointColors: List<ComposeColor>? = null,  // Optional per-point colors
    val fillBetweenSeries: List<ChartDataPoint>? = null  // Optional series to fill between
)

/**
 * A Compose-based line chart for displaying performance statistics
 *
 * @param series List of data series to display
 * @param graphType The type of graph being displayed
 * @param year The year for the chart data
 * @param month Optional month for monthly charts
 * @param numberFormat Number formatter for axis labels
 * @param modifier Modifier for the chart container
 */
@Composable
fun PerformanceLineChart(
    series: List<ChartSeries>,
    graphType: GraphType,
    year: String,
    modifier: Modifier = Modifier,
    month: String? = null,
    numberFormat: NumberFormat
) {
    val isDark = isSystemInDarkTheme()

    if (series.all { it.data.isEmpty() }) return

    // Calculate overall chart bounds
    val allDataPoints = series.flatMap { it.data }
    val minX = allDataPoints.minOfOrNull { it.x } ?: 0f
    val maxX = allDataPoints.maxOfOrNull { it.x } ?: 1f
    val minY = allDataPoints.minOfOrNull { it.y } ?: 0f
    val maxY = allDataPoints.maxOfOrNull { it.y } ?: 1f

    // Add padding to bounds
    val xRange = maxX - minX
    val yRange = maxY - minY
    val paddedMinX = if (xRange > 0) minX - xRange * 0.05f else minX
    val paddedMaxX = if (xRange > 0) maxX + xRange * 0.05f else maxX + 1f
    val paddedMinY =
        if (graphType.cumulative) 0f else if (yRange > 0) minY - yRange * 0.1f else minY
    val paddedMaxY = if (yRange > 0) maxY + yRange * 0.1f else maxY + 1f

    val textColor = if (isDark) ComposeColor.White else ComposeColor.Black
    val gridColor = if (isDark) ComposeColor(0xFF444444) else ComposeColor.LightGray
    val chartBackgroundColor = if (isDark) ComposeColor(0xFF1E1E1E) else ComposeColor.White

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(chartBackgroundColor)
    ) {
        // Chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                // Reserve space for x-axis labels at the bottom
                val bottomPadding = 40f
                val effectiveChartHeight = chartHeight - bottomPadding

                // Draw grid lines and labels
                drawGridAndLabels(
                    minX = paddedMinX,
                    maxX = paddedMaxX,
                    minY = paddedMinY,
                    maxY = paddedMaxY,
                    year = year,
                    month = month,
                    graphType = graphType,
                    numberFormat = numberFormat,
                    gridColor = gridColor,
                    textColor = textColor,
                    bottomPadding = bottomPadding,
                    effectiveChartHeight = effectiveChartHeight
                )

                // Draw each series
                series.forEach { chartSeries ->
                    if (chartSeries.data.isNotEmpty()) {
                        drawSeries(
                            series = chartSeries,
                            paddedMinX = paddedMinX,
                            paddedMaxX = paddedMaxX,
                            paddedMinY = paddedMinY,
                            paddedMaxY = paddedMaxY,
                            chartWidth = chartWidth,
                            chartHeight = effectiveChartHeight
                        )
                    }
                }
            }
        }

        // Legend
        PerformanceChartLegend(
            series = series,
            textColor = textColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Draw a single data series on the chart
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeries(
    series: ChartSeries,
    paddedMinX: Float,
    paddedMaxX: Float,
    paddedMinY: Float,
    paddedMaxY: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    val screenPoints = series.data.map { point ->
        val x = if (paddedMaxX > paddedMinX) {
            (point.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
        } else {
            chartWidth / 2f
        }
        val y = if (paddedMaxY > paddedMinY) {
            chartHeight - (point.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight
        } else {
            chartHeight / 2f
        }
        Offset(x, y)
    }

    // Draw filled area if specified
    if (series.filled && series.fillColor != null && screenPoints.size > 1) {
        if (series.fillBetweenSeries != null) {
            // Fill between this series and another series (e.g., between min and max)
            val otherScreenPoints = series.fillBetweenSeries.map { point ->
                val x = if (paddedMaxX > paddedMinX) {
                    (point.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                } else {
                    chartWidth / 2f
                }
                val y = if (paddedMaxY > paddedMinY) {
                    chartHeight - (point.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight
                } else {
                    chartHeight / 2f
                }
                Offset(x, y)
            }

            val fillPath = Path().apply {
                // Start with the first point of this series
                moveTo(screenPoints.first().x, screenPoints.first().y)
                // Draw all points of this series
                screenPoints.forEach { point ->
                    lineTo(point.x, point.y)
                }
                // Draw the other series in reverse order
                otherScreenPoints.reversed().forEach { point ->
                    lineTo(point.x, point.y)
                }
                close()
            }

            drawPath(
                path = fillPath,
                color = series.fillColor.copy(alpha = series.fillAlpha),
                style = Fill
            )
        } else {
            // Fill to bottom of chart (original behavior)
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
                color = series.fillColor.copy(alpha = series.fillAlpha),
                style = Fill
            )
        }
    }

    // Draw line with per-point colors if available, otherwise use single color
    if (screenPoints.size > 1) {
        for (i in 0 until screenPoints.size - 1) {
            val lineColor = if (series.pointColors != null && i < series.pointColors.size) {
                series.pointColors[i]
            } else {
                series.color
            }
            drawLine(
                color = lineColor,
                start = screenPoints[i],
                end = screenPoints[i + 1],
                strokeWidth = series.lineWidth,
                cap = StrokeCap.Round
            )
        }
    }

    // Draw circles if specified
    if (series.drawCircles) {
        screenPoints.forEachIndexed { index, point ->
            val circleColor = if (series.pointColors != null && index < series.pointColors.size) {
                series.pointColors[index]
            } else {
                series.color
            }
            drawCircle(
                color = circleColor,
                radius = 4f,
                center = point
            )
        }
    }
}

/**
 * Draw grid lines and axis labels
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridAndLabels(
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    year: String,
    month: String?,
    graphType: GraphType,
    numberFormat: NumberFormat,
    gridColor: ComposeColor,
    textColor: ComposeColor,
    bottomPadding: Float,
    effectiveChartHeight: Float
) {
    val chartWidth = size.width
    val chartHeight = effectiveChartHeight

    // Draw X axis labels (dates) - reduce number of labels to prevent overlap
    val xSteps = if (month != null) 4 else 6
    for (i in 0..xSteps) {
        val xValue = minX + (maxX - minX) * i / xSteps
        val xPos = i * chartWidth / xSteps

        // Draw grid line (only within chart area)
        drawLine(
            color = gridColor,
            start = Offset(xPos, 0f),
            end = Offset(xPos, chartHeight),
            strokeWidth = 1f
        )

        // Draw label (skip first and last to avoid edge overlap)
        if (i in 1..<xSteps) {
            val cal = Calendar.getInstance()
            cal.time = PerformanceGraphProvider.parseDate(
                String.format("${year}-${month ?: "01"}-01 00:00:00")
            )
            if (month == null) {
                cal.set(Calendar.DAY_OF_YEAR, xValue.toInt())
            } else {
                cal.set(Calendar.DAY_OF_MONTH, xValue.toInt())
            }
            val label = SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)

            drawContext.canvas.nativeCanvas.drawText(
                label,
                xPos,
                chartHeight + bottomPadding - 5,
                Paint().apply {
                    color = textColor.toArgb()
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                }
            )
        }
    }

    // Draw Y axis labels
    val ySteps = 5
    for (i in 0..ySteps) {
        val yValue = minY + (maxY - minY) * i / ySteps
        val yPos = chartHeight - i * chartHeight / ySteps

        // Draw grid line (only within chart area)
        drawLine(
            color = gridColor,
            start = Offset(0f, yPos),
            end = Offset(chartWidth, yPos),
            strokeWidth = 1f
        )

        // Draw label
        numberFormat.maximumFractionDigits = if (yValue > 99) 0 else 1
        val label = "${numberFormat.format(yValue.toDouble())} ${graphType.unit}"

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
 * Legend component for the performance chart
 */
@Composable
fun PerformanceChartLegend(
    series: List<ChartSeries>,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            series.forEach { chartSeries ->
                LegendItem(
                    name = chartSeries.name,
                    color = chartSeries.color,
                    textColor = textColor
                )
            }
        }
    }
}

/**
 * Individual legend item
 */
@Composable
private fun LegendItem(
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
 * Convert MPAndroidChart Entry list to ChartDataPoint list
 */
fun convertEntriesToChartDataPoints(entries: List<Entry>): List<ChartDataPoint> {
    return entries.map { ChartDataPoint(it.x, it.y) }
}