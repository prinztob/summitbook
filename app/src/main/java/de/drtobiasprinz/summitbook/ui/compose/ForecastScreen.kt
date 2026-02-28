package de.drtobiasprinz.summitbook.ui.compose

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.sharedPreferences
import de.drtobiasprinz.summitbook.utils.ForecastConstants
import kotlinx.coroutines.Job
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun ForecastScreen(
    summits: List<Summit>,
    forecasts: MutableList<Forecast>,
    onNavigateBack: () -> Unit = {},
    onSaveForecasts: (Boolean, List<Forecast>) -> Job
) {
    val context = LocalContext.current

    val indoorHeightMeterPercent = remember {
        sharedPreferences.getInt("pref_indoor_height_meter", 0)
    }
    val annualTargetActivity = remember {
        sharedPreferences.getString("pref_annual_target_activities", "52") ?: "52"
    }
    val annualTargetKm = remember {
        sharedPreferences.getString("pref_annual_target_km", "1200") ?: "1200"
    }
    val annualTargetHm = remember {
        sharedPreferences.getString("pref_annual_target", "50000") ?: "50000"
    }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    var forecastsUpdated by remember { mutableStateOf(false) }
    var forecastsChanged by remember { mutableIntStateOf(0) }
    var hmSum by remember { mutableIntStateOf(0) }
    var kmSum by remember { mutableIntStateOf(0) }
    var activitySum by remember { mutableIntStateOf(0) }


    // Update forecasts if needed
    LaunchedEffect(summits, forecasts) {
        if (!forecastsUpdated && summits.isNotEmpty()) {
            forecastsUpdated = true
            val yearsWithForecasts = listOf(currentYear, currentYear + 1)
            updateMissingForecasts(yearsWithForecasts, forecasts, summits, onSaveForecasts)
        }
        forecasts.forEach { forecast ->
            if (forecast.year == currentYear && forecast.month <= currentMonth) {
                forecast.setActual(summits, indoorHeightMeterPercent)
            }
        }
    }

    LaunchedEffect(forecasts, forecastsChanged) {
        hmSum =
            Forecast.getSumForYear(currentYear, forecasts, 0, currentYear, currentMonth)
        kmSum =
            Forecast.getSumForYear(currentYear, forecasts, 1, currentYear, currentMonth)
        activitySum =
            Forecast.getSumForYear(currentYear, forecasts, 2, currentYear, currentMonth)
    }

    when {
        forecasts.isEmpty() && summits.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        forecasts.isEmpty() || summits.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error loading data",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {


                val hmTargetReached = hmSum > annualTargetHm.toInt()
                val kmTargetReached = kmSum > annualTargetKm.toInt()
                val activityTargetReached = activitySum > annualTargetActivity.toInt()


                // Table approach for forecast info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Column headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.elevationGain),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.kilometers_accumulated),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.total_activities),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // Current year data row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$hmSum${stringResource(R.string.m)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hmTargetReached) Color.Green else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text(
                                text = "$kmSum${stringResource(R.string.km)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (kmTargetReached) Color.Green else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Text(
                                text = activitySum.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activityTargetReached) Color.Green else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        // Annual target data row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$annualTargetHm${stringResource(R.string.m)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$annualTargetKm${stringResource(R.string.km)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = annualTargetActivity,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Action buttons (Recalculate and Save)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = {
                            updateForecastsForYear(
                                forecasts,
                                currentYear + 1,
                                summits,
                                onSaveForecasts
                            )
                            forecastsUpdated = !forecastsUpdated
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_refresh_24),
                            contentDescription = stringResource(R.string.recalculate),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    val successfullySaved = stringResource(R.string.forecast_successfully_saved)
                    IconButton(
                        onClick = {
                            // Save all forecasts for both years
                            val job = onSaveForecasts(true, forecasts)
                            job.invokeOnCompletion {
                                Toast.makeText(
                                    context,
                                    successfullySaved,
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_save_black_24dp),
                            contentDescription = stringResource(R.string.saveButtonText),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Month list for both current and next year
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Items for current year, starting from current month
                    items(
                        count = 12 - currentMonth + 1,
                        key = { index -> (currentMonth + index) * 100 + currentYear }
                    ) { index ->
                        val month = currentMonth + index
                        ForecastMonthRow(
                            month = month,
                            year = currentYear,
                            forecasts = forecasts,
                            currentYear = currentYear,
                            currentMonth = currentMonth,
                            forecastsUpdated = forecastsUpdated,
                            onForecastChanged = { updatedForecast ->
                                forecastsChanged++
                                val forecastIndex =
                                    forecasts.indexOfFirst { it.month == month && it.year == currentYear }
                                if (forecastIndex != -1) {
                                    updatedForecast.id = forecasts[forecastIndex].id
                                    forecasts[forecastIndex] = updatedForecast
                                }
                            },
                            onRecalculate = {
                                updateForecastForMonthAndYear(
                                    month,
                                    currentYear,
                                    summits,
                                    forecasts,
                                    onSaveForecasts = onSaveForecasts
                                )
                                forecastsUpdated = !forecastsUpdated
                            }
                        )
                    }

                    // Items for next year, starting from January
                    items(
                        count = 12,
                        key = { index -> (index + 1) * 100 + (currentYear + 1) }
                    ) { index ->
                        val month = index + 1
                        ForecastMonthRow(
                            month = month,
                            year = currentYear + 1,
                            forecasts = forecasts,
                            currentYear = currentYear,
                            currentMonth = currentMonth,
                            forecastsUpdated = forecastsUpdated,
                            onForecastChanged = { updatedForecast ->
                                val forecastIndex =
                                    forecasts.indexOfFirst { it.month == month && it.year == (currentYear + 1) }
                                if (forecastIndex != -1) {
                                    updatedForecast.id = forecasts[forecastIndex].id
                                    forecasts[forecastIndex] = updatedForecast
                                }
                            },
                            onRecalculate = {
                                updateForecastForMonthAndYear(
                                    month,
                                    currentYear + 1,
                                    summits,
                                    forecasts,
                                    onSaveForecasts = onSaveForecasts
                                )
                                forecastsUpdated = !forecastsUpdated
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastMonthRow(
    month: Int,
    year: Int,
    forecasts: List<Forecast>,
    currentYear: Int,
    currentMonth: Int,
    forecastsUpdated: Boolean,
    onForecastChanged: (Forecast) -> Unit,
    onRecalculate: () -> Unit
) {
    val isCurrentYear = year == currentYear
    val isPastMonth = year == currentYear && month < currentMonth

    val forecast = forecasts.find { it.month == month && it.year == year } ?: Forecast(
        year = year,
        month = month,
        forecastHeightMeter = 0,
        forecastDistance = 0,
        forecastNumberActivities = 0
    )

    val actualDistance = forecast.actualDistance

    var heightMeterValue by remember { mutableIntStateOf(forecast.forecastHeightMeter) }
    var distanceValue by remember { mutableIntStateOf(forecast.forecastDistance) }
    var activitiesValue by remember { mutableIntStateOf(forecast.forecastNumberActivities) }

    var isSliderEnabled by remember { mutableStateOf(!isPastMonth) }

    // Update slider values when forecast values change (e.g., after recalculation)
    LaunchedEffect(
        forecast.forecastHeightMeter,
        forecast.forecastDistance,
        forecast.forecastNumberActivities,
        forecastsUpdated
    ) {
        heightMeterValue = forecast.forecastHeightMeter
        distanceValue = forecast.forecastDistance
        activitiesValue = forecast.forecastNumberActivities
    }

    // Height meter slider parameters
    val hmStepSize = ForecastConstants.STEP_SIZE_HM
    val hmValueTo = (ceil(forecast.forecastHeightMeter * 1.2 / hmStepSize) * hmStepSize).toInt()
    val hmMaxValue = if (hmValueTo < 15000) 15000f else hmValueTo.toFloat()
    val hmCalculatedSteps = ((hmMaxValue / hmStepSize).toInt() - 1).coerceAtLeast(0)

    // Distance slider parameters
    val kmStepSize = ForecastConstants.STEP_SIZE_KM
    val kmValueTo = (ceil(forecast.forecastDistance * 1.2 / kmStepSize) * kmStepSize).toInt()
    val kmMaxValue = if (kmValueTo < 750) 750f else kmValueTo.toFloat()
    val kmCalculatedSteps = ((kmMaxValue / kmStepSize).toInt() - 1).coerceAtLeast(0)

    // Activities slider parameters
    val activityStepSize = ForecastConstants.STEP_SIZE_ACTIVITY
    val activityValueTo =
        (ceil(forecast.forecastNumberActivities * 1.2 / activityStepSize) * activityStepSize).toInt()
    val activityMaxValue = if (activityValueTo < 25) 25f else activityValueTo.toFloat()
    val activityCalculatedSteps =
        ((activityMaxValue / activityStepSize).toInt() - 1).coerceAtLeast(0)

    val textColor = if (isCurrentYear && month <= currentMonth) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Top row: Month name and year, and recalculate button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month name and year
                Text(
                    text = "${stringResource(getMonthResource(month))} $year",
                    modifier = Modifier.width(120.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Recalculate button
                IconButton(
                    onClick = onRecalculate,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_refresh_12),
                        contentDescription = stringResource(R.string.recalculate),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Actual values for past months
                if (isPastMonth) {
                    val percentAchievement = if (forecast.forecastDistance > 0) {
                        (actualDistance.toDouble() / forecast.forecastDistance.toDouble() * 100.0).roundToInt()
                    } else 0
                    val achievementColor = if (percentAchievement > 0 && actualDistance > 0) {
                        if (percentAchievement >= 100) Color.Green else Color.Red
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = "${actualDistance}${stringResource(R.string.km)} ($percentAchievement%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = achievementColor
                    )
                }
            }

            // Height meter slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.hm),
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${heightMeterValue}${stringResource(R.string.hm)}",
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Slider(
                    value = heightMeterValue.toFloat(),
                    onValueChange = { newValue: Float ->
                        if (isSliderEnabled) {
                            val roundedValue = (newValue / hmStepSize).roundToInt() * hmStepSize
                            heightMeterValue = roundedValue
                            val updatedForecast = forecast.copy(forecastHeightMeter = roundedValue)
                            onForecastChanged(updatedForecast)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    valueRange = 0f..hmMaxValue,
                    steps = hmCalculatedSteps,
                    enabled = isSliderEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Distance slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.km),
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${distanceValue}${stringResource(R.string.km)}",
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Slider(
                    value = distanceValue.toFloat(),
                    onValueChange = { newValue: Float ->
                        if (isSliderEnabled) {
                            val roundedValue = (newValue / kmStepSize).roundToInt() * kmStepSize
                            distanceValue = roundedValue
                            val updatedForecast = forecast.copy(forecastDistance = roundedValue)
                            onForecastChanged(updatedForecast)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    valueRange = 0f..kmMaxValue,
                    steps = kmCalculatedSteps,
                    enabled = isSliderEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Activities slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.activity_hint),
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = activitiesValue.toString(),
                    modifier = Modifier.width(80.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Slider(
                    value = activitiesValue.toFloat(),
                    onValueChange = { newValue: Float ->
                        if (isSliderEnabled) {
                            val roundedValue =
                                (newValue / activityStepSize).roundToInt() * activityStepSize
                            activitiesValue = roundedValue
                            val updatedForecast =
                                forecast.copy(forecastNumberActivities = roundedValue)
                            onForecastChanged(updatedForecast)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    valueRange = 0f..activityMaxValue,
                    steps = activityCalculatedSteps,
                    enabled = isSliderEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

private fun getMonthResource(month: Int): Int {
    return when (month) {
        1 -> R.string.january
        2 -> R.string.february
        3 -> R.string.march
        4 -> R.string.april
        5 -> R.string.may
        6 -> R.string.june
        7 -> R.string.july
        8 -> R.string.august
        9 -> R.string.september
        10 -> R.string.october
        11 -> R.string.november
        12 -> R.string.december
        else -> R.string.january
    }
}

private fun updateMissingForecasts(
    years: List<Int>,
    forecasts: MutableList<Forecast>,
    summits: List<Summit>,
    onSaveForecasts: (Boolean, List<Forecast>) -> Job
) {
    for (year in years) {
        for (month in 1..12) {
            val updatedForecast = Forecast.getNewForecastFrom(
                month,
                year,
                summits,
                3,
                "52",
                "1200",
                "50000"
            )
            val existingForecast = forecasts.firstOrNull { it.month == month && it.year == year }
            if (existingForecast == null) {
                onSaveForecasts(false, listOf(updatedForecast))
            }
        }
    }
}

private fun updateForecastsForYear(
    forecasts: MutableList<Forecast>,
    year: Int,
    summits: List<Summit>?,
    onSaveForecasts: (Boolean, List<Forecast>) -> Job
) {
    for (month in 1..12) {
        updateForecastForMonthAndYear(
            month,
            year,
            summits,
            forecasts,
            onSaveForecasts = onSaveForecasts
        )
    }
}

private fun updateForecastForMonthAndYear(
    month: Int,
    year: Int,
    summits: List<Summit>?,
    forecasts: MutableList<Forecast>,
    onSaveForecasts: (Boolean, List<Forecast>) -> Job
) {
    val updatedForecast = Forecast.getNewForecastFrom(
        month,
        year,
        summits,
        3,
        "52",
        "1200",
        "50000"
    )
    val existingForecastIndex = forecasts.indexOfFirst { it.month == month && it.year == year }
    if (existingForecastIndex == -1) {
        onSaveForecasts(false, listOf(updatedForecast))
    } else {
        // Replace the forecast object in the list to trigger recomposition
        updatedForecast.id = forecasts[existingForecastIndex].id
        forecasts[existingForecastIndex] = updatedForecast
    }
}
