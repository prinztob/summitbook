package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.TextFieldGroupThirdParty
import de.drtobiasprinz.summitbook.models.TextFieldThirdParty
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun SummitEntryThirdPartyScreen(
    summit: Summit?,
    summitsToCompare: List<Summit>,
    compareSummit: Summit?,
    extrema: ExtremaValuesSummits?,
    onGetSummitToCompare: (Long) -> Unit,
    onSetSummitToCompareToNull: () -> Unit,
    modifier: Modifier = Modifier
) {

    val configuration = LocalConfiguration.current
    val numberFormat = remember { NumberFormat.getInstance(configuration.locales[0]) }

    if (summit == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Check if summit has Garmin data
    if (summit.garminData == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.delete),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    var showMoreCyclingDynamics by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Garmin link
        item {
            summit.garminData?.url?.let { url ->
                GarminLink(url = url)
            }
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

        // Base third party data fields
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.sensor_data),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextFieldThirdParty.entries.filter { it.group == TextFieldGroupThirdParty.ThirdParty }
                        .forEach { field ->
                            ThirdPartyDataFieldRow(
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

        // Additional cycling dynamics data (expandable)
        if (summit.garminData?.power?.oneSec != null && (summit.garminData?.power?.oneSec
                ?: 0) > 0
        ) {
            item {
                Card {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { showMoreCyclingDynamics = !showMoreCyclingDynamics },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = if (showMoreCyclingDynamics)
                                    painterResource(R.drawable.ic_baseline_expand_less_24)
                                else
                                    painterResource(R.drawable.ic_baseline_expand_more_24),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showMoreCyclingDynamics)
                                    stringResource(R.string.less_cycling_dynamics)
                                else
                                    stringResource(R.string.more_cycling_dynamics)
                            )
                        }

                        if (showMoreCyclingDynamics) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextFieldThirdParty.entries
                                    .filter { it.group == TextFieldGroupThirdParty.ThirdPartyAdditionalData }
                                    .forEach { field ->
                                        ThirdPartyDataFieldRow(
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
        }
    }
}

@Composable
fun GarminLink(url: String) {
    val uriHandler = LocalUriHandler.current

    Text(
        text = stringResource(R.string.sensor_data),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { uriHandler.openUri(url) }
    )
}

@Composable
fun ThirdPartyDataFieldRow(
    field: TextFieldThirdParty,
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

    val rangeValue = field.getValueRange(summit)
    val rangeDouble = rangeValue?.toDouble()

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
                percent <= 0.2 -> Color.Red
                percent <= 0.4 -> Color(0xFFFF9800) // Orange
                percent <= 0.6 -> Color.Yellow
                percent <= 0.8 -> Color.Blue
                else -> Color.Green
            }
        } else {
            Color.Transparent
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = field.iconId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = stringResource(field.labelId),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (indicatorColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(indicatorColor, shape = CircleShape)
                )
            }

            Text(
                text = formatThirdPartyValue(
                    valueDouble,
                    compareDouble,
                    rangeDouble,
                    field,
                    numberFormat
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun formatThirdPartyValue(
    value: Double,
    compareValue: Double?,
    rangeValue: Double?,
    field: TextFieldThirdParty,
    numberFormat: NumberFormat
): String {
    numberFormat.maximumFractionDigits = field.digits

    return if (field.toMinSec) {
        val valueInSec = value.toLong()
        val compareInSec = (compareValue ?: 0.0).toLong()

        if (compareInSec > 0) {
            String.format(
                Locale.getDefault(),
                "%02d:%02d (%02d:%02d)",
                TimeUnit.SECONDS.toMinutes(valueInSec),
                valueInSec % TimeUnit.MINUTES.toSeconds(1),
                TimeUnit.SECONDS.toMinutes(compareInSec),
                compareInSec % TimeUnit.MINUTES.toSeconds(1)
            )
        } else {
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                TimeUnit.SECONDS.toMinutes(valueInSec),
                valueInSec % TimeUnit.MINUTES.toSeconds(1)
            )
        }
    } else {
        val formattedValue = numberFormat.format(value * field.factor)

        val result = StringBuilder(formattedValue)

        if (rangeValue != null && rangeValue > 0) {
            val formattedRange = numberFormat.format(rangeValue * field.factor)
            result.append(" - ").append(formattedRange)
        }

        if (compareValue != null && compareValue > 0) {
            val formattedCompare = numberFormat.format(compareValue * field.factor)
            result.append(" (").append(formattedCompare).append(")")
        }

        val unitString = stringResource(field.unitWithPlaceHolder)
        val unit = String.format(Locale.getDefault(), unitString, "")
        if (unit.isNotBlank()) {
            result.append(" ").append(unit.trim())
        }
        result.toString()
    }
}

