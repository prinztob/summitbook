package de.drtobiasprinz.summitbook.ui.compose

import android.content.res.Resources
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.TextFieldPower
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun SummitEntryPowerScreen(
    summit: Summit?,
    allSummits: List<Summit>?,
    summitsToCompare: List<Summit>,
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

    val filteredSummits = remember(summit.id, allSummits?.size, selectedTimeRange) {
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
                PowerLineChart(
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
        onExpandedChange = {
            @Suppress("AssignedValueIsNeverRead")
            expanded = it
        }
    ) {
        OutlinedTextField(
            value = timeRangeOptions[selectedTimeRange],
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.time_interval)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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

