package de.drtobiasprinz.summitbook.ui.compose

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlin.math.roundToLong
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * A Compose-based line chart view for displaying track data
 *
 * @param trackPoints List of track points with extension data
 * @param trackColor The track color configuration for the chart
 * @param modifier Modifier for the chart container
 * @param showLegend Whether to show the legend below the chart (default: true)
 * @param verticalLines Optional list of vertical lines to draw (x positions and colors)
 */
@Composable
fun LineChartView(
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    trackColor: TrackColor,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
    verticalLines: List<Pair<Float, ComposeColor>>? = null
) {
    val isDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current
    val primaryColor = MaterialTheme.colorScheme.primary

    if (trackPoints.isEmpty()) return

    val actualTrackColor = if (trackColor == TrackColor.None || trackColor == TrackColor.Mileage) {
        TrackColor.Elevation
    } else {
        trackColor
    }
    val label = stringResource(actualTrackColor.labelId)
    val minLabel = stringResource(R.string.min)
    val maxLabel = stringResource(R.string.max)

    // Get track graph data
    val lineChartEntries = remember(trackPoints, actualTrackColor) {
        GpsTrack.getTrackGraph(trackPoints, actualTrackColor.f)
    }

    if (lineChartEntries.isEmpty()) return

    // Calculate chart bounds
    val minX = lineChartEntries.minOfOrNull { it.x } ?: 0f
    val maxX = lineChartEntries.maxOfOrNull { it.x } ?: 1f
    val minY = lineChartEntries.minOfOrNull { it.y } ?: 0f
    val maxY = lineChartEntries.maxOfOrNull { it.y } ?: 1f

    // Add some padding to the bounds
    val xRange = maxX - minX
    val yRange = maxY - minY
    val paddedMinX = minX - xRange * 0.05f
    val paddedMaxX = maxX + xRange * 0.05f
    val paddedMinY = minY - yRange * 0.1f
    val paddedMaxY = maxY + yRange * 0.1f

    // Calculate colors for each point
    val pointColors = remember(lineChartEntries, actualTrackColor) {
        val min = lineChartEntries.minByOrNull { it.y }?.y ?: 0f
        val max = lineChartEntries.maxByOrNull { it.y }?.y ?: 0f
        lineChartEntries.map {
            val fraction = if (max - min > 0) (it.y - min) / (max - min) else 0f
            ComposeColor(
                interpolateColor(
                    actualTrackColor.minColor, actualTrackColor.maxColor, fraction
                )
            )
        }
    }

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
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val chartWidth = size.width
                val chartHeight = size.height

                // Draw grid lines and labels
                this.drawGridAndLabels(
                    minX = paddedMinX,
                    maxX = paddedMaxX,
                    minY = paddedMinY,
                    maxY = paddedMaxY,
                    configuration = configuration,
                    gridColor = gridColor,
                    textColor = textColor
                )

                // Convert data points to screen coordinates
                val screenPoints = lineChartEntries.mapIndexed { index, entry ->
                    val x = (entry.x - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                    val y =
                        chartHeight - (entry.y - paddedMinY) / (paddedMaxY - paddedMinY) * chartHeight
                    Offset(x, y) to pointColors[index]
                }

                // Draw filled area under curve
                if (screenPoints.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(screenPoints.first().first.x, chartHeight)
                        screenPoints.forEach { (point, _) ->
                            lineTo(point.x, point.y)
                        }
                        lineTo(screenPoints.last().first.x, chartHeight)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        color = primaryColor.copy(alpha = 0.3f),
                        style = Fill
                    )
                }

                // Draw line with gradient colors
                if (screenPoints.size > 1) {
                    for (i in 0 until screenPoints.size - 1) {
                        val (startPoint, startColor) = screenPoints[i]
                        val (endPoint, _) = screenPoints[i + 1]

                        drawLine(
                            color = startColor,
                            start = startPoint,
                            end = endPoint,
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Draw vertical lines if provided
                verticalLines?.forEach { (xValue, color) ->
                    val xPos = (xValue - paddedMinX) / (paddedMaxX - paddedMinX) * chartWidth
                    drawLine(
                        color = color,
                        start = Offset(xPos, 0f),
                        end = Offset(xPos, chartHeight),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Legend below the graph
        if (showLegend) {
            LineChartLegend(
                label = label,
                minLabel = minLabel,
                maxLabel = maxLabel,
                minColor = ComposeColor(actualTrackColor.minColor),
                maxColor = ComposeColor(actualTrackColor.maxColor),
                textColor = textColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Legend component for the line chart
 */
@Composable
fun LineChartLegend(
    label: String,
    minLabel: String,
    maxLabel: String,
    minColor: ComposeColor,
    maxColor: ComposeColor,
    textColor: ComposeColor,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSystemInDarkTheme()) ComposeColor(0xFF1E1E1E) else ComposeColor.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Min legend entry
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = minColor,
                        radius = 6.dp.toPx()
                    )
                }
                Text(
                    text = "$label $minLabel",
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Max legend entry
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(
                        color = maxColor,
                        radius = 6.dp.toPx()
                    )
                }
                Text(
                    text = "$label $maxLabel",
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Draw grid lines and axis labels on the chart
 */
fun DrawScope.drawGridAndLabels(
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    configuration: Configuration,
    gridColor: ComposeColor,
    textColor: ComposeColor
) {
    // Draw X axis labels (distance in km) - skip first and last
    for (i in 0..4) {
        val xValue = minX + (maxX - minX) * i / 4f
        val kmValue = (xValue / 100f).roundToLong() / 10f
        val xPos = i * this.size.width / 4f

        // Draw grid line
        drawLine(
            color = gridColor,
            start = Offset(xPos, 0f),
            end = Offset(xPos, this.size.height),
            strokeWidth = 1f
        )

        // Draw label (skip first and last)
        if (i in 1..<4) {
            val label = String.format(configuration.locales[0], "%.1f km", kmValue)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                xPos,
                this.size.height - 10,
                Paint().apply {
                    color = textColor.toArgb()
                    textSize = 30f
                    textAlign = Paint.Align.CENTER
                }
            )
        }
    }

    // Draw Y axis labels
    for (i in 0..4) {
        val yValue = minY + (maxY - minY) * i / 4f
        val label = String.format(configuration.locales[0], "%.0f", yValue)

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

/**
 * Interpolate between two colors using HSV color space for smooth spectrum transitions
 */
private fun interpolateColor(colorA: Int, colorB: Int, fraction: Float): Int {
    val hsva = FloatArray(3)
    val hsvb = FloatArray(3)
    android.graphics.Color.colorToHSV(colorA, hsva)
    android.graphics.Color.colorToHSV(colorB, hsvb)
    
    // Interpolate each HSV component
    val h = hsva[0] + (hsvb[0] - hsva[0]) * fraction
    val s = hsva[1] + (hsvb[1] - hsva[1]) * fraction
    val v = hsva[2] + (hsvb[2] - hsva[2]) * fraction
    
    // Interpolate alpha
    val a = (android.graphics.Color.alpha(colorA) +
            (android.graphics.Color.alpha(colorB) - android.graphics.Color.alpha(colorA)) * fraction).toInt()
    
    return android.graphics.Color.HSVToColor(a, floatArrayOf(h, s, v))
}