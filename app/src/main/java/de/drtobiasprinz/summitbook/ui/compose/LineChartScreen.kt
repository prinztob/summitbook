package de.drtobiasprinz.summitbook.ui.compose

import android.content.res.Configuration
import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.OrderBySpinnerEntry
import de.drtobiasprinz.summitbook.ui.utils.CustomLineChartWithMarker
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerView
import de.drtobiasprinz.summitbook.utils.Constants.DATE_FORMAT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Generic chart data point used by [PerformanceLineChart] and [OverviewScreen].
 * Kept here because it was originally defined in this file and both consumers
 * are in the same package.
 */
data class ChartDataPoint(
    val x: Float,
    val y: Float,
    val summit: Summit? = null,
    val color: ComposeColor = ComposeColor.Black
)

@Composable
fun LineChartScreen(
    filteredSummits: List<Summit>
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var lineChartSpinnerEntry by remember { mutableStateOf(OrderBySpinnerEntry.HeightMeter) }
    var lineChartEntries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var lineChartColors by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }

    val spinnerEntries = remember {
        OrderBySpinnerEntry.getSpinnerEntriesWithoutExcludedFromLineChart()
    }

    // Process data when summits or selected entry change
    LaunchedEffect(filteredSummits, lineChartSpinnerEntry) {
        withContext(Dispatchers.IO) {
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
            val colors = useEntries.map { ContextCompat.getColor(context, it.sportType.color) }
            val entries = useEntries.map {
                val value = if (!lineChartSpinnerEntry.accumulate) {
                    lineChartSpinnerEntry.f(it)
                } else {
                    accumulator += lineChartSpinnerEntry.f(it) ?: 0f
                    accumulator
                }
                Entry(it.getDateAsFloat(), value ?: 0f, it)
            }

            lineChartColors = colors
            lineChartEntries = entries
        }
    }

    val isDarkTheme = when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }

    val backgroundColor =
        if (isDarkTheme) ComposeColor(0xFF121212) else ComposeColor(0xFFCCCCCC)
    val textColor = if (isDarkTheme) ComposeColor.White else ComposeColor.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp)
    ) {
        // Spinner / dropdown section
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
                TextButton(onClick = { showDropdown = true }) {
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
                    containerColor = if (isDarkTheme) ComposeColor(0xFF1E1E1E) else ComposeColor.White
                ) {
                    spinnerEntries.forEach { entry ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(id = entry.nameId),
                                    color = if (isDarkTheme) ComposeColor.White else ComposeColor.Black
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

        // MPAndroidChart section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            MPLineChart(
                entries = lineChartEntries,
                colors = lineChartColors,
                spinnerEntry = lineChartSpinnerEntry,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Legend section
        LineChartLegendSection(
            lineChartSpinnerEntry = lineChartSpinnerEntry,
            sportTypes = SportType.entries.toList(),
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * Wraps [CustomLineChartWithMarker] (MPAndroidChart) inside an [AndroidView] composable.
  */
@Composable
fun MPLineChart(
    entries: List<Entry>,
    colors: List<Int>,
    spinnerEntry: OrderBySpinnerEntry,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val unitString = stringResource(spinnerEntry.unit)
    val labelString = stringResource(spinnerEntry.nameId)

    // Resolve theme-dependent colors once
    val axisTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK
    val lineColor =
        if (isDarkTheme) Color.WHITE else ContextCompat.getColor(context, R.color.colorPrimaryDark)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CustomLineChartWithMarker(ctx).apply {
                description.isEnabled = false
                setNoDataText(ctx.getString(R.string.no_data_available))
                setTouchEnabled(true)
                isDragEnabled = true
                isScaleXEnabled = true
                isScaleYEnabled = true
                setPinchZoom(false)
            }
        },
        update = { chart ->
            // ── Axis text colours (mirrors resizeChart()) ──────────────────────
            chart.xAxis.textColor = axisTextColor
            chart.axisLeft.textColor = axisTextColor
            chart.axisRight.textColor = axisTextColor
            chart.legend?.textColor = axisTextColor

            // ── X-Axis (mirrors setXAxis()) ────────────────────────────────────
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.valueFormatter = object : ValueFormatter() {
                private val sdf = SimpleDateFormat(DATE_FORMAT, configuration.locales[0])
                override fun getFormattedValue(value: Float): String =
                    sdf.format(Summit.getDateFromFloat(value))
            }

            // ── Y-Axes (mirrors setYAxis()) ────────────────────────────────────
            val yFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val fmt =
                        if (spinnerEntry == OrderBySpinnerEntry.Vo2Max) "%.1f %s" else "%.0f %s"
                    return String.format(
                        configuration.locales[0], fmt, value, unitString
                    )
                }
            }
            chart.axisLeft.valueFormatter = yFormatter
            chart.axisRight.isEnabled = false

            // ── Dataset (mirrors setGraphView() + drawLineChart()) ─────────────
            val dataSet = LineDataSet(entries, labelString).apply {
                setDrawValues(false)
                circleColors = colors
                highLightColor = Color.RED
                lineWidth = 5f
                circleRadius = 10f
                valueTextSize = 15f
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                cubicIntensity = 0.2f
                setDrawFilled(true)
                fillAlpha = 60
                color = lineColor
                fillColor = Color.WHITE
            }

            val dataSets: MutableList<ILineDataSet> = mutableListOf(dataSet)
            chart.data = LineData(dataSets)

            chart.legend.isEnabled = false

            // ── Marker ─────────────────────────────────────────────────────────
            chart.marker = CustomMarkerView(context, R.layout.marker_graph, spinnerEntry)

            chart.invalidate()
        }
    )
}

// ── Legend composable (kept for visual consistency with the rest of the screen) ──

@Composable
fun LineChartLegendSection(
    lineChartSpinnerEntry: OrderBySpinnerEntry,
    sportTypes: List<SportType>,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = if (isDarkTheme) ComposeColor(0xFF1E1E1E) else ComposeColor.White
    val textColor = if (isDarkTheme) ComposeColor.White else ComposeColor.Black
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main legend entry (selected metric)
            item {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = primaryColor, radius = 6.dp.toPx())
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = lineChartSpinnerEntry.nameId),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Sport-type legend entries
            items(sportTypes) { sportType ->
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color =
                        ComposeColor(ContextCompat.getColor(context, sportType.color))
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = color, radius = 6.dp.toPx())
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
