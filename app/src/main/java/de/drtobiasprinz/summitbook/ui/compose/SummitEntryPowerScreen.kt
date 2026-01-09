package de.drtobiasprinz.summitbook.ui.compose

import android.content.res.Resources
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.TextFieldPower
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
import de.drtobiasprinz.summitbook.ui.utils.SummitUtils
import de.drtobiasprinz.summitbook.ui.utils.TimeIntervalPower
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

@Composable
fun SummitEntryPowerScreen(
    summit: Summit?,
    allSummits: List<Summit>?,
    compareSummit: Summit?,
    extrema: ExtremaValuesSummits?,
    onGetSummitToCompare: (Long) -> Unit,
    onSetSummitToCompareToNull: () -> Unit,
    modifier: Modifier = Modifier
) {

    val configuration = LocalConfiguration.current
    val numberFormat = remember { NumberFormat.getInstance(configuration.locales[0]) }

    var selectedTimeRange by remember { mutableIntStateOf(0) }

    if (summit == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val summitsToCompare = remember(allSummits, summit.id) {
        allSummits?.let { summits ->
            SummitUtils.getSummitsToCompare(
                summits,
                summit,
                onlyWithPowerData = true
            )
        } ?: emptyList()
    }

    val filteredSummits = remember(summit.id, allSummits, selectedTimeRange) {
        getFilteredSummits(summit, allSummits ?: emptyList(), selectedTimeRange)
    }

    val extremaValuesAllSummits = remember(filteredSummits) {
        ExtremaValuesSummits(filteredSummits, excludeZeroValueFromMin = true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            SummitHeader(summit = summit)
        }

        // Compare dropdown (if not bookmark)
        if (!summit.isBookmark && summitsToCompare.isNotEmpty()) {
            item {
                CompareDropdown(
                    summitsToCompare = summitsToCompare,
                    currentCompare = compareSummit,
                    onSummitSelected = { selectedSummit ->
                        if (selectedSummit == null) {
                            onSetSummitToCompareToNull()
                        } else {
                            onGetSummitToCompare(selectedSummit.id)
                        }
                    }
                )
            }
        }

        // Time range selector
        item {
            TimeRangeSelector(
                selectedTimeRange = selectedTimeRange,
                onTimeRangeSelected = { selectedTimeRange = it }
            )
        }

        // Power chart
        if (summit.garminData?.power != null) {
            item {
                PowerChart(
                    summit = summit,
                    summitToCompare = compareSummit,
                    extremaValuesAllSummits = extremaValuesAllSummits,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((Resources.getSystem().displayMetrics.heightPixels * 0.65 / Resources.getSystem().displayMetrics.density).dp)
                )
            }
        }

        // Power data fields
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextFieldPower.entries.forEach { field ->
                        PowerDataFieldRow(
                            field = field,
                            summit = summit,
                            compareSummit = compareSummit,
                            extrema = extrema,
                            numberFormat = numberFormat
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(
    selectedTimeRange: Int,
    onTimeRangeSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val timeRangeOptions = listOf(
        stringResource(R.string.all),
        stringResource(R.string.current_year),
        stringResource(R.string.last_3_month),
        stringResource(R.string.last_12_month)
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = timeRangeOptions[selectedTimeRange],
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.time_interval)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            timeRangeOptions.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onTimeRangeSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PowerChart(
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

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setDrawGridBackground(false)
                setTouchEnabled(true)

                // Setup X axis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val unscaled = unScaleCbr(round(value.toDouble())).toInt()
                        return String.format(
                            configuration.locales[0],
                            "%s $secLabel",
                            unscaled
                        )
                    }
                }
                xAxis.axisMinimum = scaleCbr(1.0)
                xAxis.axisMaximum = scaleCbr(100000.0)
                xAxis.setLabelCount(6, true)

                axisLeft.axisMinimum = 0f
                axisRight.axisMinimum = 0f
                axisLeft.setDrawGridLines(true)
                axisLeft.isGranularityEnabled = true
            }
        },
        update = { chart ->
            val dataSets: MutableList<ILineDataSet> = ArrayList()

            // Main power data
            val chartEntries = getLineChartEntriesMax(power)
            val dataSet = LineDataSet(chartEntries, powerProfileLabel)
            setGraphView(dataSet, false)

            // Compare power data
            val powerToCompare = summitToCompare?.garminData?.power
            if (powerToCompare != null) {
                val chartEntriesComparator = getLineChartEntriesMax(powerToCompare)
                val dataSetComparator = LineDataSet(
                    chartEntriesComparator,
                    powerProfileCompareLabel
                )
                setGraphView(dataSetComparator, true, color = Color.GRAY)
                dataSets.add(dataSetComparator)
            }

            // Extremal values
            val extremalChartEntries = getLineChartEntriesMax(extremaValuesAllSummits)
            val minimalChartEntries = getLineChartEntriesMin(extremaValuesAllSummits)
            val maxWatts = (extremalChartEntries + chartEntries).maxOf { it?.y ?: 0f }
            chart.axisLeft.axisMaximum = maxWatts
            chart.axisRight.axisMaximum = maxWatts

            val dataSetMaximalValues = LineDataSet(
                extremalChartEntries,
                powerProfileMaxLabel
            )
            dataSetMaximalValues.fillFormatter = MyFillFormatter(
                LineDataSet(
                    minimalChartEntries,
                    powerProfileMinLabel
                )
            )
            chart.renderer = MyLineLegendRenderer(
                chart,
                chart.animator,
                chart.viewPortHandler
            )

            setGraphView(dataSetMaximalValues)
            dataSets.add(dataSetMaximalValues)

            // Color circles based on performance
            dataSet.circleColors = chartEntries.mapIndexed { index, chartEntry ->
                val maxEntry = extremalChartEntries[index]
                val minEntry = minimalChartEntries[index]
                if (chartEntry != null && maxEntry != null && minEntry != null) {
                    when {
                        chartEntry.y >= maxEntry.y -> Color.rgb(255, 215, 0) // Gold
                        chartEntry.y < minEntry.y -> Color.RED
                        else -> ColorUtils.blendARGB(
                            Color.GREEN,
                            Color.RED,
                            1f - ((chartEntry.y - minEntry.y) / (maxEntry.y - minEntry.y))
                        )
                    }
                } else {
                    Color.BLUE
                }
            }
            dataSets.add(dataSet)

            chart.data = LineData(dataSets)

            // Set colors based on theme
            val textColor = if (isDark) Color.WHITE else Color.BLACK
            chart.xAxis.textColor = textColor
            chart.axisRight.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.legend?.textColor = textColor

            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
fun PowerDataFieldRow(
    field: TextFieldPower,
    summit: Summit,
    compareSummit: Summit?,
    extrema: ExtremaValuesSummits?,
    numberFormat: NumberFormat
) {
    val value = field.getValue(summit) ?: return
    val valueDouble = value.toDouble()

    if (abs(valueDouble * field.factor) < 0.01) return

    val compareValue = compareSummit?.let { field.getValue(it) }
    val compareDouble = compareValue?.toDouble()

    // Calculate indicator color - use valueDouble instead of summit to avoid recomposition
    val indicatorColor = remember(field, valueDouble, extrema) {
        val minSummit = field.getMinMaxSummit(extrema)?.first
        val maxSummit = field.getMinMaxSummit(extrema)?.second

        if (minSummit != null && maxSummit != null) {
            val min = field.getValue(minSummit)?.toDouble() ?: 0.0
            val max = field.getValue(maxSummit)?.toDouble() ?: valueDouble
            val percent = if (field.reverse) {
                (max - valueDouble) / (max - min)
            } else {
                (valueDouble - min) / (max - min)
            }

            when {
                percent <= 0.2 -> androidx.compose.ui.graphics.Color.Red
                percent <= 0.4 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
                percent <= 0.6 -> androidx.compose.ui.graphics.Color.Yellow
                percent <= 0.8 -> androidx.compose.ui.graphics.Color.Blue
                else -> androidx.compose.ui.graphics.Color.Green
            }
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(field.nameId),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (indicatorColor != androidx.compose.ui.graphics.Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(indicatorColor, shape = CircleShape)
                )
            }

            Text(
                text = formatPowerValue(valueDouble, compareDouble, field, numberFormat),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun formatPowerValue(
    value: Double,
    compareValue: Double?,
    field: TextFieldPower,
    numberFormat: NumberFormat
): String {
    numberFormat.maximumFractionDigits = field.digits

    return if (field.toHHms) {
        val valueInMs = (value * 3600000.0).toLong()
        val hours = TimeUnit.MILLISECONDS.toHours(valueInMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(valueInMs) % 60

        if (compareValue != null && compareValue > 0) {
            val compareInMs = (compareValue * 3600000.0).toLong()
            val compareHours = TimeUnit.MILLISECONDS.toHours(compareInMs)
            val compareMinutes = TimeUnit.MILLISECONDS.toMinutes(compareInMs) % 60
            String.format(
                Locale.getDefault(),
                "%02d:%02d (%02d:%02d)",
                hours,
                minutes,
                compareHours,
                compareMinutes
            )
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        }
    } else {
        val formattedValue = numberFormat.format(value * field.factor)
        if (compareValue != null && compareValue > 0) {
            val formattedCompare = numberFormat.format(compareValue * field.factor)
            "$formattedValue ($formattedCompare) ${field.unit}"
        } else {
            "$formattedValue ${field.unit}"
        }
    }
}

// Helper functions
private fun getFilteredSummits(
    summitToView: Summit,
    summits: List<Summit>,
    selectedTimeRangeSpinner: Int
): List<Summit> {
    var filtered = listOf<Summit>()
    if (selectedTimeRangeSpinner != 0) {
        filtered = summits.filter { summit ->
            val diff = Date().time - summit.date.time

            when (selectedTimeRangeSpinner) {
                1 -> getYear(summit.date) == getYear(Date())
                2 -> diff < 3 * 30 * 24 * 3600000L
                3 -> diff < 12 * 30 * 24 * 3600000L
                else -> true
            }
        }
    }
    return filtered.ifEmpty { summits }.filter { !it.equalsInBaseProperties(summitToView) }
}

private fun getYear(date: Date): Int {
    val calendar: Calendar = GregorianCalendar()
    calendar.time = date
    return calendar[Calendar.YEAR]
}

private fun getLineChartEntriesMin(extremaValuesSummits: ExtremaValuesSummits?): MutableList<Entry?> {
    return TimeIntervalPower.entries.map {
        Entry(
            scaleCbr(it.seconds.toDouble()),
            it.minPower(extremaValuesSummits),
            it.getMinSummit(extremaValuesSummits)
        )
    }.toMutableList()
}

private fun getLineChartEntriesMax(extremaValuesSummits: ExtremaValuesSummits?): MutableList<Entry?> {
    return TimeIntervalPower.entries.map {
        Entry(
            scaleCbr(it.seconds.toDouble()),
            it.maxPower(extremaValuesSummits),
            it.getMaxSummit(extremaValuesSummits)
        )
    }.toMutableList()
}

private fun scaleCbr(cbr: Double): Float {
    return log10(cbr).toFloat()
}

private fun unScaleCbr(cbr: Double): Float {
    val calcVal = 10.0.pow(cbr)
    return calcVal.toFloat()
}

private fun getLineChartEntriesMax(power: PowerData): MutableList<Entry?> {
    val lineChartEntries: MutableList<Entry?> = ArrayList()
    if (power.oneSec > 0) lineChartEntries.add(Entry(scaleCbr(1.0), power.oneSec.toFloat()))
    if (power.twoSec > 0) lineChartEntries.add(Entry(scaleCbr(2.0), power.twoSec.toFloat()))
    if (power.fiveSec > 0) lineChartEntries.add(Entry(scaleCbr(5.0), power.fiveSec.toFloat()))
    if (power.tenSec > 0) lineChartEntries.add(Entry(scaleCbr(10.0), power.tenSec.toFloat()))
    if (power.twentySec > 0) lineChartEntries.add(Entry(scaleCbr(20.0), power.twentySec.toFloat()))
    if (power.thirtySec > 0) lineChartEntries.add(Entry(scaleCbr(30.0), power.thirtySec.toFloat()))
    if (power.oneMin > 0) lineChartEntries.add(Entry(scaleCbr(60.0), power.oneMin.toFloat()))
    if (power.twoMin > 0) lineChartEntries.add(Entry(scaleCbr(120.0), power.twoMin.toFloat()))
    if (power.fiveMin > 0) lineChartEntries.add(Entry(scaleCbr(300.0), power.fiveMin.toFloat()))
    if (power.tenMin > 0) lineChartEntries.add(Entry(scaleCbr(600.0), power.tenMin.toFloat()))
    if (power.twentyMin > 0) lineChartEntries.add(
        Entry(
            scaleCbr(1200.0),
            power.twentyMin.toFloat()
        )
    )
    if (power.thirtyMin > 0) lineChartEntries.add(
        Entry(
            scaleCbr(1800.0),
            power.thirtyMin.toFloat()
        )
    )
    if (power.oneHour > 0) lineChartEntries.add(Entry(scaleCbr(3600.0), power.oneHour.toFloat()))
    if (power.twoHours > 0) lineChartEntries.add(Entry(scaleCbr(7200.0), power.twoHours.toFloat()))
    if (power.threeHours > 0) lineChartEntries.add(
        Entry(
            scaleCbr(10400.0),
            power.threeHours.toFloat()
        )
    )
    if (power.fourHours > 0) lineChartEntries.add(
        Entry(
            scaleCbr(14400.0),
            power.fourHours.toFloat()
        )
    )
    if (power.fiveHours > 0) lineChartEntries.add(
        Entry(
            scaleCbr(18000.0),
            power.fiveHours.toFloat()
        )
    )
    return lineChartEntries
}

private fun setGraphView(set1: LineDataSet?, filled: Boolean = true, color: Int = Color.BLUE) {
    set1?.mode = LineDataSet.Mode.LINEAR
    set1?.circleRadius = 7.0f
    set1?.setDrawValues(false)
    if (filled) {
        set1?.cubicIntensity = 20f
        set1?.lineWidth = 2.5f
        set1?.setCircleColor(color)
        set1?.color = color
        set1?.highLightColor = Color.rgb(244, 117, 117)
        set1?.setDrawFilled(true)
        set1?.fillColor = color
        set1?.fillAlpha = 50
    } else {
        set1?.lineWidth = 4.8f
        set1?.setCircleColor(Color.BLACK)
        set1?.color = Color.BLACK
        set1?.setDrawFilled(false)
    }
    set1?.setDrawHorizontalHighlightIndicator(true)
}
