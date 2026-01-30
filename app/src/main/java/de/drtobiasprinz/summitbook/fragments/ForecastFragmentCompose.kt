package de.drtobiasprinz.summitbook.fragments

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    var selectedYear by remember { mutableIntStateOf(0) } // 0 = current year, 1 = next year
    var selectedProperty by remember { mutableIntStateOf(0) } // 0 = height meter, 1 = km, 2 = activities
    var forecastsUpdated by remember { mutableStateOf(false) }


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
            val year = if (selectedYear == 0) currentYear else currentYear + 1

            val sum = Forecast.getSumForYear(
                year, forecasts, selectedProperty, currentYear, currentMonth
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Overview text
                val overviewText = when (selectedProperty) {
                    1 -> stringResource(
                        R.string.forecast_info_km,
                        year.toString(),
                        sum.toString(),
                        annualTargetKm
                    )

                    2 -> stringResource(
                        R.string.forecast_info_activities,
                        year.toString(),
                        sum.toString(),
                        annualTargetActivity
                    )

                    else -> stringResource(
                        R.string.forecast_info_hm,
                        year.toString(),
                        sum.toString(),
                        annualTargetHm
                    )
                }
                val annualTarget = when (selectedProperty) {
                    1 -> annualTargetKm.toInt()
                    2 -> annualTargetActivity.toInt()
                    else -> annualTargetHm.toInt()
                }
                val overviewColor = if (sum > annualTarget) Color.Green else Color.Red

                // Action buttons (Recalculate and Save)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier
                            .padding(2.dp)
                    ) {
                        val textForToast = stringResource(R.string.not_recalculate_current_year)
                        IconButton(
                            onClick = {
                                if (selectedYear == 1) {
                                    updateForecastsForYear(
                                        forecasts,
                                        year,
                                        summits,
                                        onSaveForecasts
                                    )
                                } else {
                                    Toast.makeText(context, textForToast, Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_refresh_24),
                                contentDescription = stringResource(R.string.recalculate),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val txtForToast = stringResource(R.string.forecast_successfully_saved)
                        // Save button
                        IconButton(
                            onClick = {
                                // Save all forecasts for the current selected year
                                val forecastsToSave = forecasts.filter { it.year == year }
                                val job = onSaveForecasts(true, forecastsToSave)
                                job.invokeOnCompletion {
                                    Toast.makeText(
                                        context,
                                        txtForToast,
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

                    Spacer(modifier = Modifier.width(1.dp))
                    Text(
                        text = overviewText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        textAlign = TextAlign.Center,
                        color = overviewColor
                    )

                }
                // Year selection buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { selectedYear = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedYear == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (selectedYear == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.current_year))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedYear = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedYear == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (selectedYear == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.next_year))
                    }
                }

                // Property selection buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { selectedProperty = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedProperty == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (selectedProperty == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.hm))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedProperty = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedProperty == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (selectedProperty == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.km))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { selectedProperty = 2 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedProperty == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (selectedProperty == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.activity_hint))
                    }
                }

                // Month list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = 12,
                        key = { month -> month + 1 }
                    ) { index ->
                        val month = index + 1
                        ForecastMonthRow(
                            month = month,
                            year = year,
                            forecasts = forecasts,
                            currentYear = currentYear,
                            currentMonth = currentMonth,
                            selectedProperty = selectedProperty,
                            onForecastChanged = { updatedForecast ->
                                val forecastIndex =
                                    forecasts.indexOfFirst { it.month == month && it.year == year }
                                if (forecastIndex != -1) {
                                    updatedForecast.id = forecasts[forecastIndex].id
                                    forecasts[forecastIndex] = updatedForecast
                                }
                            },
                            onRecalculate = {
                                updateForecastForMonthAndYear(
                                    month,
                                    year,
                                    summits,
                                    forecasts,
                                    onSaveForecasts = onSaveForecasts
                                )
                            },
                            onSaveForecasts = onSaveForecasts
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
    selectedProperty: Int,
    onForecastChanged: (Forecast) -> Unit,
    onRecalculate: () -> Unit,
    onSaveForecasts: (Boolean, List<Forecast>) -> Job
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

    var sliderValue by remember { mutableIntStateOf(getForecastValue(forecast, selectedProperty)) }
    var isSliderEnabled by remember { mutableStateOf(!isPastMonth) }
    var showEditDialog by remember { mutableStateOf(false) }

    val stepSize = when (selectedProperty) {
        1 -> ForecastConstants.STEP_SIZE_KM
        2 -> ForecastConstants.STEP_SIZE_ACTIVITY
        else -> ForecastConstants.STEP_SIZE_HM
    }

    val valueTo = when (selectedProperty) {
        1 -> (ceil(forecast.forecastDistance * 1.2 / stepSize) * stepSize).toInt()
        2 -> (ceil(forecast.forecastNumberActivities * 1.2 / stepSize) * stepSize).toInt()
        else -> (ceil(forecast.forecastHeightMeter * 1.2 / stepSize) * stepSize).toInt()
    }

    val maxValue =
        if (valueTo < (if (selectedProperty == 1) 750 else if (selectedProperty == 2) 25 else 15000)) {
            if (selectedProperty == 1) 750f else if (selectedProperty == 2) 25f else 15000f
        } else {
            valueTo.toFloat()
        }

    val unit = when (selectedProperty) {
        1 -> stringResource(R.string.km)
        2 -> ""
        else -> stringResource(R.string.hm)
    }

    val textColor = if (isCurrentYear && month <= currentMonth) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val achievementText = if (isPastMonth) {
        val percentAchievement = if (forecast.forecastDistance > 0) {
            (actualDistance.toDouble() / forecast.forecastDistance.toDouble() * 100.0).roundToInt()
        } else forecast.forecastDistance
        val text = if (forecast.forecastDistance > 0) {
            "$actualDistance $unit (${percentAchievement} %)"
        } else "$actualDistance $unit"
        val achievementColor = if (percentAchievement > 0 && actualDistance > 0) {
            if (percentAchievement >= 100) Color.Green else Color.Red
        } else {
            MaterialTheme.colorScheme.onSurface
        }
        text to achievementColor
    } else {
        // Show current slider value in real-time
        val displayValue = sliderValue
        val text = when (selectedProperty) {
            1 -> String.format(
                stringResource(R.string.value_with_km),
                displayValue.toString()
            )

            2 -> displayValue.toString()
            else -> String.format(
                stringResource(R.string.value_with_hm),
                displayValue.toString()
            )
        }
        text to textColor
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
            // Top row: Month name, recalculate button, value, and edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month name
                Text(
                    text = stringResource(getMonthResource(month)),
                    modifier = Modifier.width(80.dp),
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

                Spacer(modifier = Modifier.width(8.dp))

                // Forecast value text
                Text(
                    text = achievementText.first,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = achievementText.second
                )

                // Edit button
                if (isCurrentYear && month <= currentMonth) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_edit_12),
                            contentDescription = stringResource(R.string.edit_icon),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Bottom row: Slider
            val calculatedSteps = ((maxValue / stepSize).toInt() - 1).coerceAtLeast(0)
            Slider(
                value = sliderValue.toFloat(),
                onValueChange = { newValue: Float ->
                    if (isSliderEnabled) {
                        // Round to nearest step
                        val roundedValue = (newValue / stepSize).roundToInt() * stepSize
                        sliderValue = roundedValue
                        val updatedForecast = when (selectedProperty) {
                            1 -> forecast.copy(forecastDistance = roundedValue)
                            2 -> forecast.copy(forecastNumberActivities = roundedValue)
                            else -> forecast.copy(forecastHeightMeter = roundedValue)
                        }
                        onForecastChanged(updatedForecast)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                valueRange = 0f..maxValue,
                steps = calculatedSteps,
                onValueChangeFinished = {
                    onSaveForecasts(false, listOf(forecast))
                },
                enabled = isSliderEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }

    // Edit dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_icon)) },
            text = { Text("Edit forecast value") },
            confirmButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    isSliderEnabled = !isSliderEnabled
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

private fun getForecastValue(forecast: Forecast, selectedProperty: Int): Int {
    return when (selectedProperty) {
        1 -> forecast.forecastDistance
        2 -> forecast.forecastNumberActivities
        else -> forecast.forecastHeightMeter
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
    forecasts: List<Forecast>,
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
    forecasts: List<Forecast>,
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
    forecasts: List<Forecast>,
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
    val existingForecast = forecasts.firstOrNull { it.month == month && it.year == year }
    if (existingForecast == null) {
        onSaveForecasts(false, listOf(updatedForecast))
    } else {
        existingForecast.forecastDistance = updatedForecast.forecastDistance
        existingForecast.forecastHeightMeter = updatedForecast.forecastHeightMeter
        existingForecast.forecastNumberActivities = updatedForecast.forecastNumberActivities
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp
    ),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        colors = colors
    ) {
        content()
    }
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
