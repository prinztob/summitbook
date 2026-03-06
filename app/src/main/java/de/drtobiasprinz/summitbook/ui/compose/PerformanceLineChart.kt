package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import com.patrykandpatrick.vico.compose.common.shader.toShaderProvider
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.LegendItem
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.component.TextComponent
import de.drtobiasprinz.summitbook.models.ChartEntry
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
    val pointColors: List<ComposeColor>? = null,
    val fillBetweenSeries: List<ChartDataPoint>? = null
)

/**
 * A Compose-based line chart for displaying performance statistics using Vico
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
    val activeSeries = series.filter { it.data.isNotEmpty() }
    if (activeSeries.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }

    // Create a zoom state that shows the entire x-axis range
    val zoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = { _, _, _ -> 0f }
    )

    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineSeries {
                // For series with point colors, we need to create separate series for each segment
                // to achieve per-segment coloring
                activeSeries.forEach { chartSeries ->
                    if (chartSeries.pointColors != null && chartSeries.pointColors.isNotEmpty()) {
                        // Create a series for each segment between consecutive points
                        for (i in 0 until chartSeries.data.size - 1) {
                            series(
                                x = listOf(
                                    chartSeries.data[i].x.toDouble(),
                                    chartSeries.data[i + 1].x.toDouble()
                                ),
                                y = listOf(
                                    chartSeries.data[i].y.toDouble(),
                                    chartSeries.data[i + 1].y.toDouble()
                                )
                            )
                        }
                    } else {
                        series(
                            x = chartSeries.data.map { it.x.toDouble() },
                            y = chartSeries.data.map { it.y.toDouble() }
                        )
                    }
                }
            }
        }
    }

// Build line specs for each active series.
// LineCartesianLayer.Line is constructed directly (non-composable) inside remember.
// LineStroke.Continuous takes thicknessDp as Float; lineWidth is already in dp units.
val lineSpecs = remember(activeSeries) {
    val specs = mutableListOf<LineCartesianLayer.Line>()
    activeSeries.forEach { chartSeries ->
        if (chartSeries.pointColors != null && chartSeries.pointColors.isNotEmpty()) {
            // Create a line spec for each segment with its own color
            for (i in 0 until chartSeries.data.size - 1) {
                val segmentColor = chartSeries.pointColors.getOrElse(i) { chartSeries.color }
                specs.add(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(segmentColor)),
                        stroke = LineCartesianLayer.LineStroke.Continuous(thicknessDp = chartSeries.lineWidth),
                        areaFill = null
                    )
                )
            }
        } else {
            specs.add(
                LineCartesianLayer.Line(
                    fill = LineCartesianLayer.LineFill.single(fill(chartSeries.color)),
                    stroke = LineCartesianLayer.LineStroke.Continuous(thicknessDp = chartSeries.lineWidth),
                    areaFill = if (chartSeries.filled && chartSeries.fillColor != null) {
                        LineCartesianLayer.AreaFill.single(
                            fill(chartSeries.fillColor.copy(alpha = chartSeries.fillAlpha))
                        )
                    } else {
                        null
                    }
                )
            )
        }
    }
    specs
}

    val yValueFormatter = remember(graphType, numberFormat) {
        CartesianValueFormatter { _, value, _ ->
            val formattedValue = when {
                value >= 1000 -> {
                    // Show values >= 1000 with 2 significant digits (e.g., 1.5k)
                    val kValue = value / 1000
                    numberFormat.maximumFractionDigits = if (kValue >= 10) 0 else 1
                    "${numberFormat.format(kValue)}k"
                }

                value > 99 -> {
                    // Show values > 99 with no fraction digits
                    numberFormat.maximumFractionDigits = 0
                    numberFormat.format(value)
                }

                else -> {
                    // Show values <= 99 with 1 fraction digit
                    numberFormat.maximumFractionDigits = 1
                    numberFormat.format(value)
                }
            }
            "$formattedValue\n${graphType.unit}"
        }
    }

    val xValueFormatter = remember(year, month) {
        CartesianValueFormatter { _, value, _ ->
            val cal = Calendar.getInstance()
            cal.time = PerformanceGraphProvider.parseDate(
                String.format("${year}-${month ?: "01"}-01 00:00:00")
            )
            if (month == null) {
                cal.set(Calendar.DAY_OF_YEAR, value.toInt().coerceAtLeast(1))
            } else {
                cal.set(Calendar.DAY_OF_MONTH, value.toInt().coerceAtLeast(1))
            }
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
        }
    }

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    Column(modifier = modifier.fillMaxWidth()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(lineSpecs)
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = yValueFormatter
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = xValueFormatter,
                    itemPlacer = remember {
                        HorizontalAxis.ItemPlacer.aligned(
                            spacing = { if (month != null) 7 else 30 }
                        )
                    }
                ),
                legend = rememberHorizontalLegend(
                    items = {
                        activeSeries.forEach { chartSeries ->
                            val legendFill = if (chartSeries.pointColors != null && chartSeries.pointColors.isNotEmpty()) {
                                // Create a gradient fill showing gold, green, red for per-segment colored series
                                fill(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            ComposeColor(0xFFFFD700), // Gold
                                            ComposeColor(0xFF00FF00), // Green
                                            ComposeColor(0xFFFF0000)  // Red
                                        )
                                    ).toShaderProvider()
                                )
                            } else {
                                fill(chartSeries.color)
                            }
                            add(
                                LegendItem(
                                    ShapeComponent(legendFill),
                                    TextComponent(color = textColor),
                                    chartSeries.name,
                                )
                            )
                        }
                    }
                ),
            ),
            zoomState = zoomState,
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

/**
 * Convert ChartEntry list to ChartDataPoint list
 */
fun convertEntriesToChartDataPoints(entries: List<ChartEntry>): List<ChartDataPoint> {
    return entries.map { ChartDataPoint(it.x, it.y) }
}
