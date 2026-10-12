package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.Forecast
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.appstate.AppState.sharedPreferences
import de.drtobiasprinz.summitbook.core.ForecastConstants
import de.drtobiasprinz.summitbook.core.Keys
import kotlinx.coroutines.Job
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.roundToInt
import de.drtobiasprinz.summitbook.ui.theme.RecordGreen

@Composable
fun ForecastScreen(
    summits: List<Summit>,
    forecasts: List<Forecast>,
    onNavigateBack: () -> Unit = {},
    onSaveForecasts: (Boolean, List<Forecast>) -> Job,
    onShowSnackbar: (String) -> Unit = {}
) {
    val indoorHeightMeterPercent = remember {
        sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
    }
    val annualTargetActivity = remember {
        sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, "52") ?: "52"
    }
    val annualTargetKm = remember {
        sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200") ?: "1200"
    }
    val annualTargetHm = remember {
        sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET, "50000") ?: "50000"
    }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val locale = LocalConfiguration.current.locales[0]

    // Screen-local editable copy of the forecasts. Rebuilt whenever the
    // database emits a new list; slider edits update this list and are
    // persisted via the Save button.
    val forecastState = remember { mutableStateListOf<Forecast>() }
    var missingForecastsChecked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(forecasts, summits, indoorHeightMeterPercent) {
        forecastState.clear()
        forecastState.addAll(
            forecasts.map { forecast ->
                forecast.copy().also {
                    it.id = forecast.id
                    if (it.year == currentYear && it.month <= currentMonth) {
                        it.setActual(summits, indoorHeightMeterPercent)
                    }
                }
            }
        )
        if (!missingForecastsChecked) {
            missingForecastsChecked = true
            updateMissingForecasts(
                listOf(currentYear, currentYear + 1),
                forecasts,
                summits,
                onSaveForecasts,
                annualTargetActivity,
                annualTargetKm,
                annualTargetHm
            )
        }
    }

    // Year shown in the summary card (0 = current year, 1 = next year)
    var summaryYearOffset by rememberSaveable { mutableIntStateOf(0) }
    val summaryYear = currentYear + summaryYearOffset

    val hmSum by remember(currentYear, currentMonth, summaryYear) {
        derivedStateOf {
            Forecast.getSumForYear(summaryYear, forecastState, 0, currentYear, currentMonth)
        }
    }
    val kmSum by remember(currentYear, currentMonth, summaryYear) {
        derivedStateOf {
            Forecast.getSumForYear(summaryYear, forecastState, 1, currentYear, currentMonth)
        }
    }
    val activitySum by remember(currentYear, currentMonth, summaryYear) {
        derivedStateOf {
            Forecast.getSumForYear(summaryYear, forecastState, 2, currentYear, currentMonth)
        }
    }

    when {
        forecasts.isEmpty() && summits.isEmpty() -> {
            LoadingPanel(visible = true)
        }

        forecasts.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.error_loading_data),
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
                val hmTarget = annualTargetHm.toIntOrNull() ?: 50000
                val kmTarget = annualTargetKm.toIntOrNull() ?: 1200
                val activityTarget = annualTargetActivity.toIntOrNull() ?: 52
                val hmTargetReached = hmSum >= hmTarget
                val kmTargetReached = kmSum >= kmTarget
                val activityTargetReached = activitySum >= activityTarget

                // Yearly summary: accumulated values vs. annual targets,
                // switchable between the current and the next year
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Year switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentYear.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (summaryYearOffset == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (summaryYearOffset == 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = summaryYearOffset == 1,
                                onCheckedChange = {
                                    summaryYearOffset = if (it) 1 else 0
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (currentYear + 1).toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (summaryYearOffset == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (summaryYearOffset == 1) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ForecastSummaryColumn(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.elevationGain),
                                valueText = String.format(
                                    locale, "%,d %s", hmSum, stringResource(R.string.m)
                                ),
                                targetText = String.format(
                                    locale, "/ %,d %s", hmTarget, stringResource(R.string.m)
                                ),
                                progress = if (hmTarget > 0) hmSum.toFloat() / hmTarget else 0f,
                                reached = hmTargetReached
                            )
                            ForecastSummaryColumn(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.kilometers_accumulated),
                                valueText = String.format(
                                    locale, "%,d %s", kmSum, stringResource(R.string.km)
                                ),
                                targetText = String.format(
                                    locale, "/ %,d %s", kmTarget, stringResource(R.string.km)
                                ),
                                progress = if (kmTarget > 0) kmSum.toFloat() / kmTarget else 0f,
                                reached = kmTargetReached
                            )
                            ForecastSummaryColumn(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.total_activities),
                                valueText = String.format(locale, "%,d", activitySum),
                                targetText = String.format(locale, "/ %,d", activityTarget),
                                progress = if (activityTarget > 0) {
                                    activitySum.toFloat() / activityTarget
                                } else 0f,
                                reached = activityTargetReached
                            )
                        }
                    }
                }

                // Action buttons (Recalculate and Save)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            updateForecastsForYear(
                                forecastState,
                                currentYear + 1,
                                summits,
                                onSaveForecasts,
                                annualTargetActivity,
                                annualTargetKm,
                                annualTargetHm
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_refresh_24),
                            contentDescription = stringResource(R.string.recalculate),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.recalculate))
                    }

                    val successfullySaved = stringResource(R.string.forecast_successfully_saved)
                    val saveFailed = stringResource(R.string.forecast_save_failed)
                    Button(
                        onClick = {
                            // Save all forecasts for both years
                            val job = onSaveForecasts(true, forecastState.toList())
                            job.invokeOnCompletion { cause ->
                                if (cause == null) {
                                    onShowSnackbar(successfullySaved)
                                    onNavigateBack()
                                } else if (cause !is kotlinx.coroutines.CancellationException) {
                                    onShowSnackbar(saveFailed)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RecordGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_save_black_24dp),
                            contentDescription = stringResource(R.string.saveButtonText),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.saveButtonText))
                    }
                }

                // Month list for both current and next year. All months of the
                // current year are shown (past months below the initial scroll
                // position, read-only until unlocked via their edit button);
                // the list starts at the current month.
                val monthListState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (currentMonth - 1).coerceIn(0, 11)
                )
                LazyColumn(
                    state = monthListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Items for the current year, January to December
                    items(
                        count = 12,
                        key = { index -> (index + 1) * 100 + currentYear }
                    ) { index ->
                        val month = index + 1
                        ForecastMonthRow(
                            month = month,
                            year = currentYear,
                            forecast = forecastState.firstOrNull {
                                it.month == month && it.year == currentYear
                            },
                            currentYear = currentYear,
                            currentMonth = currentMonth,
                            onForecastChanged = { updated ->
                                updateForecastInState(forecastState, month, currentYear, updated)
                            },
                            onRecalculate = {
                                updateForecastForMonthAndYear(
                                    month,
                                    currentYear,
                                    summits,
                                    forecastState,
                                    onSaveForecasts,
                                    annualTargetActivity,
                                    annualTargetKm,
                                    annualTargetHm
                                )
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
                            forecast = forecastState.firstOrNull {
                                it.month == month && it.year == (currentYear + 1)
                            },
                            currentYear = currentYear,
                            currentMonth = currentMonth,
                            onForecastChanged = { updated ->
                                updateForecastInState(
                                    forecastState,
                                    month,
                                    currentYear + 1,
                                    updated
                                )
                            },
                            onRecalculate = {
                                updateForecastForMonthAndYear(
                                    month,
                                    currentYear + 1,
                                    summits,
                                    forecastState,
                                    onSaveForecasts,
                                    annualTargetActivity,
                                    annualTargetKm,
                                    annualTargetHm
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastSummaryColumn(
    label: String,
    valueText: String,
    targetText: String,
    progress: Float,
    reached: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (reached) RecordGreen else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = targetText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            color = if (reached) RecordGreen else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun ForecastMonthRow(
    month: Int,
    year: Int,
    forecast: Forecast?,
    currentYear: Int,
    currentMonth: Int,
    onForecastChanged: (Forecast) -> Unit,
    onRecalculate: () -> Unit
) {
    val isPastMonth = year == currentYear && month < currentMonth
    // Past months are read-only until the user unlocks them via the
    // edit button on the row
    var pastMonthEditUnlocked by rememberSaveable { mutableStateOf(false) }
    val slidersEnabled = !isPastMonth || pastMonthEditUnlocked
    val resolvedForecast = forecast ?: Forecast(year, month, 0, 0, 0)

    val locale = LocalConfiguration.current.locales[0]

    var heightMeterValue by rememberSaveable { mutableIntStateOf(resolvedForecast.forecastHeightMeter) }
    var distanceValue by rememberSaveable { mutableIntStateOf(resolvedForecast.forecastDistance) }
    var activitiesValue by rememberSaveable { mutableIntStateOf(resolvedForecast.forecastNumberActivities) }

    // Sync local slider values when the forecast object is replaced
    // (e.g. after a recalculation)
    LaunchedEffect(forecast) {
        forecast?.let {
            heightMeterValue = it.forecastHeightMeter
            distanceValue = it.forecastDistance
            activitiesValue = it.forecastNumberActivities
        }
    }

    // Height meter slider parameters
    val hmStepSize = ForecastConstants.STEP_SIZE_HM
    val hmValueTo = (ceil(resolvedForecast.forecastHeightMeter * 1.2 / hmStepSize) * hmStepSize).toInt()
    val hmMaxValue = if (hmValueTo < 15000) 15000f else hmValueTo.toFloat()
    val hmCalculatedSteps = ((hmMaxValue / hmStepSize).toInt() - 1).coerceAtLeast(0)

    // Distance slider parameters
    val kmStepSize = ForecastConstants.STEP_SIZE_KM
    val kmValueTo = (ceil(resolvedForecast.forecastDistance * 1.2 / kmStepSize) * kmStepSize).toInt()
    val kmMaxValue = if (kmValueTo < 750) 750f else kmValueTo.toFloat()
    val kmCalculatedSteps = ((kmMaxValue / kmStepSize).toInt() - 1).coerceAtLeast(0)

    // Activities slider parameters
    val activityStepSize = ForecastConstants.STEP_SIZE_ACTIVITY
    val activityValueTo =
        (ceil(resolvedForecast.forecastNumberActivities * 1.2 / activityStepSize) * activityStepSize).toInt()
    val activityMaxValue = if (activityValueTo < 25) 25f else activityValueTo.toFloat()
    val activityCalculatedSteps =
        ((activityMaxValue / activityStepSize).toInt() - 1).coerceAtLeast(0)

    val textColor = if (year == currentYear && month <= currentMonth) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val hmDescription = stringResource(R.string.elevationGain)
    val kmDescription = stringResource(R.string.kilometers_accumulated)
    val activitiesDescription = stringResource(R.string.activity_hint)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top row: Month name and year, recalculate button and
            // actual values for past months
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(getMonthResource(month))} $year",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                if (isPastMonth) {
                    val actualDistance = resolvedForecast.actualDistance
                    val percentAchievement = if (resolvedForecast.forecastDistance > 0) {
                        (actualDistance.toDouble() / resolvedForecast.forecastDistance.toDouble() * 100.0).roundToInt()
                    } else 0
                    val achievementColor = if (percentAchievement > 0 && actualDistance > 0) {
                        if (percentAchievement >= 100) RecordGreen else MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = String.format(
                            locale,
                            "%,d %s (%d%%)",
                            actualDistance,
                            stringResource(R.string.km),
                            percentAchievement
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = achievementColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Edit button to unlock editing of a past month
                    IconButton(
                        onClick = { pastMonthEditUnlocked = !pastMonthEditUnlocked },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_mode_edit_black_24dp),
                            contentDescription = stringResource(R.string.edit_icon),
                            tint = if (pastMonthEditUnlocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onRecalculate,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_refresh_12),
                        contentDescription = stringResource(R.string.recalculate),
                    )
                }
            }

            // Height meter slider
            ForecastSliderRow(
                valueText = String.format(
                    locale, "%,d %s", heightMeterValue, stringResource(R.string.hm)
                ),
                value = heightMeterValue.toFloat(),
                onValueChange = { newValue ->
                    heightMeterValue = (newValue / hmStepSize).roundToInt() * hmStepSize
                },
                onValueChangeFinished = {
                    onForecastChanged(
                        resolvedForecast.copy(forecastHeightMeter = heightMeterValue)
                    )
                },
                onDecrement = {
                    heightMeterValue = (heightMeterValue - hmStepSize).coerceAtLeast(0)
                    onForecastChanged(
                        resolvedForecast.copy(forecastHeightMeter = heightMeterValue)
                    )
                },
                onIncrement = {
                    heightMeterValue =
                        (heightMeterValue + hmStepSize).coerceAtMost(hmMaxValue.toInt())
                    onForecastChanged(
                        resolvedForecast.copy(forecastHeightMeter = heightMeterValue)
                    )
                },
                valueRange = 0f..hmMaxValue,
                steps = hmCalculatedSteps,
                enabled = slidersEnabled,
                valueColor = textColor,
                sliderDescription = hmDescription,
                sliderStateDescription = String.format(
                    locale, "%,d %s", heightMeterValue, stringResource(R.string.hm)
                )
            )

            // Distance slider
            ForecastSliderRow(
                valueText = String.format(
                    locale, "%,d %s", distanceValue, stringResource(R.string.km)
                ),
                value = distanceValue.toFloat(),
                onValueChange = { newValue ->
                    distanceValue = (newValue / kmStepSize).roundToInt() * kmStepSize
                },
                onValueChangeFinished = {
                    onForecastChanged(
                        resolvedForecast.copy(forecastDistance = distanceValue)
                    )
                },
                onDecrement = {
                    distanceValue = (distanceValue - kmStepSize).coerceAtLeast(0)
                    onForecastChanged(
                        resolvedForecast.copy(forecastDistance = distanceValue)
                    )
                },
                onIncrement = {
                    distanceValue =
                        (distanceValue + kmStepSize).coerceAtMost(kmMaxValue.toInt())
                    onForecastChanged(
                        resolvedForecast.copy(forecastDistance = distanceValue)
                    )
                },
                valueRange = 0f..kmMaxValue,
                steps = kmCalculatedSteps,
                enabled = slidersEnabled,
                valueColor = textColor,
                sliderDescription = kmDescription,
                sliderStateDescription = String.format(
                    locale, "%,d %s", distanceValue, stringResource(R.string.km)
                )
            )

            // Activities slider
            ForecastSliderRow(
                valueText = String.format(locale, "%,d", activitiesValue),
                value = activitiesValue.toFloat(),
                onValueChange = { newValue ->
                    activitiesValue = (newValue / activityStepSize).roundToInt() * activityStepSize
                },
                onValueChangeFinished = {
                    onForecastChanged(
                        resolvedForecast.copy(forecastNumberActivities = activitiesValue)
                    )
                },
                onDecrement = {
                    activitiesValue = (activitiesValue - activityStepSize).coerceAtLeast(0)
                    onForecastChanged(
                        resolvedForecast.copy(forecastNumberActivities = activitiesValue)
                    )
                },
                onIncrement = {
                    activitiesValue =
                        (activitiesValue + activityStepSize).coerceAtMost(activityMaxValue.toInt())
                    onForecastChanged(
                        resolvedForecast.copy(forecastNumberActivities = activitiesValue)
                    )
                },
                valueRange = 0f..activityMaxValue,
                steps = activityCalculatedSteps,
                enabled = slidersEnabled,
                valueColor = textColor,
                sliderDescription = activitiesDescription,
                sliderStateDescription = String.format(locale, "%,d", activitiesValue)
            )
        }
    }
}

@Composable
private fun ForecastSliderRow(
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    valueColor: Color,
    sliderDescription: String,
    sliderStateDescription: String
) {
    val increaseDescription = stringResource(R.string.increase_value)
    val decreaseDescription = stringResource(R.string.decrease_value)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp)
        )
        IconButton(
            onClick = onDecrement,
            enabled = enabled && value > valueRange.start,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_remove_black_24dp),
                contentDescription = decreaseDescription
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = sliderDescription
                    stateDescription = sliderStateDescription
                },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        IconButton(
            onClick = onIncrement,
            enabled = enabled && value < valueRange.endInclusive,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_add_black_24dp),
                contentDescription = increaseDescription
            )
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

private fun updateForecastInState(
    forecastState: MutableList<Forecast>,
    month: Int,
    year: Int,
    updated: Forecast
) {
    val index = forecastState.indexOfFirst { it.month == month && it.year == year }
    if (index != -1) {
        val existing = forecastState[index]
        // copy() drops the fields declared outside the primary constructor
        // (id and the transient actuals); restore them from the object
        // being replaced so updates keep their database identity.
        updated.id = existing.id
        updated.actualHeightMeter = existing.actualHeightMeter
        updated.actualDistance = existing.actualDistance
        updated.actualNumberActivities = existing.actualNumberActivities
        forecastState[index] = updated
    }
}

private fun updateMissingForecasts(
    years: List<Int>,
    forecasts: List<Forecast>,
    summits: List<Summit>,
    onSaveForecasts: (Boolean, List<Forecast>) -> Job,
    annualTargetActivity: String,
    annualTargetKm: String,
    annualTargetHm: String
) {
    val missingForecasts = mutableListOf<Forecast>()
    for (year in years) {
        for (month in 1..12) {
            if (forecasts.none { it.month == month && it.year == year }) {
                missingForecasts.add(
                    Forecast.getNewForecastFrom(
                        month,
                        year,
                        summits,
                        ForecastConstants.AVERAGE_OF_LAST_X_YEARS,
                        annualTargetActivity,
                        annualTargetKm,
                        annualTargetHm
                    )
                )
            }
        }
    }
    if (missingForecasts.isNotEmpty()) {
        onSaveForecasts(false, missingForecasts)
    }
}

private fun updateForecastsForYear(
    forecasts: MutableList<Forecast>,
    year: Int,
    summits: List<Summit>?,
    onSaveForecasts: (Boolean, List<Forecast>) -> Job,
    annualTargetActivity: String,
    annualTargetKm: String,
    annualTargetHm: String
) {
    for (month in 1..12) {
        updateForecastForMonthAndYear(
            month,
            year,
            summits,
            forecasts,
            onSaveForecasts = onSaveForecasts,
            annualTargetActivity = annualTargetActivity,
            annualTargetKm = annualTargetKm,
            annualTargetHm = annualTargetHm
        )
    }
}

private fun updateForecastForMonthAndYear(
    month: Int,
    year: Int,
    summits: List<Summit>?,
    forecasts: MutableList<Forecast>,
    onSaveForecasts: (Boolean, List<Forecast>) -> Job,
    annualTargetActivity: String,
    annualTargetKm: String,
    annualTargetHm: String
) {
    val updatedForecast = Forecast.getNewForecastFrom(
        month,
        year,
        summits,
        ForecastConstants.AVERAGE_OF_LAST_X_YEARS,
        annualTargetActivity,
        annualTargetKm,
        annualTargetHm
    )
    val existingForecastIndex = forecasts.indexOfFirst { it.month == month && it.year == year }
    if (existingForecastIndex == -1) {
        onSaveForecasts(false, listOf(updatedForecast))
    } else {
        // Replace the forecast object in the list to trigger recomposition
        val existing = forecasts[existingForecastIndex]
        updatedForecast.id = existing.id
        updatedForecast.actualHeightMeter = existing.actualHeightMeter
        updatedForecast.actualDistance = existing.actualDistance
        updatedForecast.actualNumberActivities = existing.actualNumberActivities
        forecasts[existingForecastIndex] = updatedForecast
    }
}
