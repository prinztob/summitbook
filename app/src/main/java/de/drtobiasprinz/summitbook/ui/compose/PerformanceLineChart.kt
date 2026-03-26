package de.drtobiasprinz.summitbook.ui.compose

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.ChartEntry
import de.drtobiasprinz.summitbook.ui.GraphType
import de.drtobiasprinz.summitbook.ui.PerformanceGraphProvider
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
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
    val fillAlpha: Float = 0.2f,
    val drawCircles: Boolean = false,
    val pointColors: List<ComposeColor>? = null,
    val fillBetweenSeries: List<ChartDataPoint>? = null
)

/**
 * A Compose-based line chart for displaying performance statistics using MPAndroidChart
 *
 * @param series List of data series to display
 * @param graphType The type of graph being displayed
 * @param year The year for the chart data
 * @param month Optional month for monthly charts
 * @param numberFormat Number formatter for axis labels
 * @param modifier Modifier for the chart container
 */
@Suppress("RemoveRedundantQualifierName")
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

    var lineChart by remember { mutableStateOf<LineChart?>(null) }

    val textColor = if (isDark) Color.WHITE else Color.BLACK
    val gridColor = if (isDark) "#444444".toColorInt() else Color.LTGRAY
    val chartBackgroundColor = if (isDark) "#1E1E1E".toColorInt() else Color.WHITE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isDark) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color.White)
    ) {
        // Refresh button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { lineChart?.fitScreen() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_refresh_24),
                    contentDescription = "Reset zoom",
                    tint = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    LineChart(ctx).apply {
                        setDrawGridBackground(false)
                        description.isEnabled = false
                        setTouchEnabled(true)
                        setPinchZoom(true)
                        setScaleEnabled(true)
                        isDragEnabled = true

                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(true)
                        xAxis.gridColor = gridColor
                        xAxis.textColor = textColor
                        xAxis.textSize = 10f

                        axisRight.setDrawLabels(false)
                        axisLeft.setDrawGridLines(true)
                        axisLeft.gridColor = gridColor
                        axisLeft.textColor = textColor
                        axisLeft.textSize = 10f

                        legend.textColor = textColor
                        legend.textSize = 10f
                        legend.isWordWrapEnabled = true
                        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                        legend.orientation = Legend.LegendOrientation.HORIZONTAL
                        legend.setDrawInside(false)

                        setBackgroundColor(chartBackgroundColor)

                        lineChart = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                update = { chart ->
                    chart.setBackgroundColor(chartBackgroundColor)
                    chart.xAxis.textColor = textColor
                    chart.xAxis.gridColor = gridColor
                    chart.axisLeft.textColor = textColor
                    chart.axisLeft.gridColor = gridColor
                    chart.axisRight.textColor = textColor
                    chart.legend.textColor = textColor

                    updateLineChart(
                        lineChart = chart,
                        series = series,
                        graphType = graphType,
                        year = year,
                        month = month,
                        numberFormat = numberFormat
                    )
                }
            )
        }
    }
}

/**
 * Update the line chart with data
 */
private fun updateLineChart(
    lineChart: LineChart,
    series: List<ChartSeries>,
    graphType: GraphType,
    year: String,
    month: String?,
    numberFormat: NumberFormat
) {
    val dataSets: MutableList<ILineDataSet> = ArrayList()

    // Calculate overall bounds for Y-axis
    val allDataPoints = series.flatMap { it.data }
    val maxY = allDataPoints.maxOfOrNull { it.y } ?: 1f

    // Set Y-axis properties
    if (graphType.cumulative) {
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisRight.axisMinimum = 0f
    } else {
        val minLeft = allDataPoints.filter { it.y > 0 }.minOfOrNull { it.y }
        if (minLeft != null) {
            lineChart.axisLeft.axisMinimum = minLeft
            lineChart.axisRight.axisMinimum = minLeft
        }
    }

    lineChart.axisLeft.axisMaximum = maxY + 1
    lineChart.axisRight.axisMaximum = maxY + 1

    // Set Y-axis formatter
    lineChart.axisLeft.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            numberFormat.maximumFractionDigits = if (value > 99) 0 else 1
            return "${numberFormat.format(value.toDouble())} ${graphType.unit}"
        }
    }

    // Set X-axis formatter
    lineChart.xAxis.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val cal = Calendar.getInstance()
            cal.time =
                PerformanceGraphProvider.parseDate(String.format("${year}-${month ?: "01"}-01 00:00:00"))
            if (month == null) {
                cal.set(Calendar.DAY_OF_YEAR, value.toInt())
            } else {
                cal.set(Calendar.DAY_OF_MONTH, value.toInt())
            }
            return SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
        }
    }

    // Process each series
    series.forEach { chartSeries ->
        if (chartSeries.data.isNotEmpty()) {
            val entries = chartSeries.data.map { point ->
                Entry(point.x, point.y)
            }

            val dataSet = LineDataSet(entries, chartSeries.name).apply {
                mode = LineDataSet.Mode.LINEAR
                setDrawValues(false)
                setDrawCircles(chartSeries.drawCircles)
                lineWidth = chartSeries.lineWidth

                if (chartSeries.pointColors != null && chartSeries.pointColors.size == entries.size) {
                    colors = chartSeries.pointColors.map { it.toArgb() }
                } else {
                    color = chartSeries.color.toArgb()
                }

                if (chartSeries.filled && chartSeries.fillColor != null) {
                    highLightColor = Color.rgb(244, 117, 117)
                    setDrawFilled(true)
                    fillColor = chartSeries.fillColor.toArgb()
                    fillAlpha = (chartSeries.fillAlpha * 255).toInt()

                    if (chartSeries.fillBetweenSeries != null) {
                        // Fill between series
                        val boundaryEntries = chartSeries.fillBetweenSeries.map { point ->
                            Entry(point.x, point.y)
                        }
                        val boundaryDataSet = LineDataSet(boundaryEntries, "")
                        fillFormatter = MyFillFormatter(boundaryDataSet)
                        lineChart.renderer = MyLineLegendRenderer(
                            lineChart,
                            lineChart.animator,
                            lineChart.viewPortHandler
                        )
                    }
                } else {
                    setDrawFilled(false)
                }

                setDrawHorizontalHighlightIndicator(true)
            }

            dataSets.add(dataSet)
        }
    }

    lineChart.data = LineData(dataSets)

    // Set custom legend matching the original implementation
    val legend = lineChart.legend
    legend.yEntrySpace = 10f
    legend.isWordWrapEnabled = true
    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    legend.orientation = Legend.LegendOrientation.HORIZONTAL
    legend.setDrawInside(false)

    // Custom legend entries matching the original OverviewFragment
    val context = lineChart.context
    val legendEntries = arrayOf(
        LegendEntry(
            context.getString(R.string.new_record),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.rgb(255, 215, 0) // Gold
        ),
        LegendEntry(
            context.getString(R.string.better_then),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.GREEN
        ),
        LegendEntry(
            context.getString(R.string.forecast),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.RED
        ),
        LegendEntry(
            context.getString(R.string.min_max_5_yrs),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.BLUE
        )
    )
    legend.setCustom(legendEntries)
    legend.isEnabled = true

    lineChart.invalidate()
}

/**
 * Convert ChartEntry list to ChartDataPoint list
 */
fun convertEntriesToChartDataPoints(entries: List<ChartEntry>): List<ChartDataPoint> {
    return entries.map { ChartDataPoint(it.x, it.y) }
}
