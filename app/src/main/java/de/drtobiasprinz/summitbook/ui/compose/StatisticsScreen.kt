package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.data.analytics.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.data.appstate.AppState.sharedPreferences
import de.drtobiasprinz.summitbook.data.db.entities.DailyActivityHelper.findDailyActivitySummariesWhichWasNotAddedToSummits
import de.drtobiasprinz.summitbook.data.db.entities.DailyActivityHelper.parseAsSummit
import de.drtobiasprinz.summitbook.data.db.entities.DailyActivitySummary
import de.drtobiasprinz.summitbook.data.db.entities.Forecast
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.StatisticEntry
import de.drtobiasprinz.summitbook.data.model.StatisticEntryDefinitions
import de.drtobiasprinz.summitbook.data.model.StatisticGroup
import de.drtobiasprinz.summitbook.data.model.StatisticsData
import de.drtobiasprinz.summitbook.ui.theme.ChartTextLightGray
import de.drtobiasprinz.summitbook.ui.theme.DarkCanvas
import de.drtobiasprinz.summitbook.ui.theme.DarkCanvasDeep
import de.drtobiasprinz.summitbook.ui.theme.SurfaceMidGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsScreen(
    filteredSummits: List<Summit>,
    forecasts: List<Forecast>,
    dailyActivitySummaryList: List<DailyActivitySummary>,
    onNavigateToSummitDetails: (Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val intFormat = NumberFormat.getInstance(locale).apply {
        maximumFractionDigits = 0
    }
    val distanceFormat = NumberFormat.getInstance(locale).apply {
        maximumFractionDigits = 1
    }
    val annualTargetActivity =
        sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, "52") ?: "52"
    val annualTargetKm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200") ?: "1200"
    val annualTargetHm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET, "50000") ?: "50000"
    val indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
    var statisticsData by remember { mutableStateOf(StatisticsData()) }
    var includeNotPersistedActivities by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(
        filteredSummits,
        forecasts,
        dailyActivitySummaryList,
        includeNotPersistedActivities
    ) {
        val statistics = withContext(Dispatchers.IO) {
            val summitsToUse = if (includeNotPersistedActivities) {
                val notPersistedSummits = findDailyActivitySummariesWhichWasNotAddedToSummits(
                    dailyActivitySummaryList,
                    filteredSummits
                )
                filteredSummits + parseAsSummit(notPersistedSummits)
            } else {
                filteredSummits
            }

            val statisticEntry = StatisticEntry(
                summitsToUse,
                annualTargetActivity.toIntOrNull() ?: 52,
                annualTargetKm.toIntOrNull() ?: 1200,
                annualTargetHm.toIntOrNull() ?: 50000,
                indoorHeightMeterPercent
            )
            statisticEntry.calculate()

            val extremaValuesSummits = ExtremaValuesSummits(
                summitsToUse, shouldIndoorActivityBeExcluded = true
            )

            StatisticsData(
                totalActivities = statisticEntry.getTotalActivities(),
                totalSummits = statisticEntry.getTotalSummits(),
                totalKm = statisticEntry.totalKm,
                totalHm = statisticEntry.totalHm,
                achievement = statisticEntry.getAchievement(),
                visitedCountries = statisticEntry.getVisitedCountries(),
                totalRoadSurfaceMeter = statisticEntry.totalRoadSurfaceMeter,
                totalRoadTypeMeter = statisticEntry.totalRoadTypeMeter,
                extremaValuesSummits = extremaValuesSummits,
                forecasts = forecasts,
                summits = summitsToUse
            )
        }
        statisticsData = statistics
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isSystemInDarkTheme()) DarkCanvasDeep else ChartTextLightGray
            )
            .padding(8.dp)
    ) {

        // Toggle for including not persisted activities
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IncludeNotPersistedActivitiesChip(
                    selected = includeNotPersistedActivities,
                    onCheckedChange = { includeNotPersistedActivities = it }
                )
            }
        }

        // Summary grid: activities, summits, km, height meters, achievement, countries
        item {
            SummaryGridSection(statisticsData, intFormat, distanceFormat)
        }

        // Road data section (surface + type), below the achievement tile
        item {
            RoadDataSection(statisticsData, distanceFormat)
        }

        // Extrema values sections
        item {
            ExtremaValuesSection(
                statisticsData,
                onNavigateToSummitDetails,
                distanceFormat,
                intFormat
            )
        }
    }
}

@Composable
fun SummaryGridSection(
    statisticsData: StatisticsData,
    intFormat: NumberFormat,
    distanceFormat: NumberFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = stringResource(R.string.total_activities),
                value = intFormat.format(statisticsData.totalActivities),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.total_summits),
                value = intFormat.format(statisticsData.totalSummits),
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = stringResource(R.string.total_kilometers),
                value = "${distanceFormat.format(statisticsData.totalKm)} km",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.total_height_meters),
                value = "${intFormat.format(statisticsData.totalHm)} hm",
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatTile(
                label = stringResource(R.string.achievement),
                value = if (statisticsData.totalActivities > 0) {
                    "${intFormat.format(statisticsData.achievement)} %"
                } else {
                    "-"
                },
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.visited_countries),
                value = if (statisticsData.visitedCountries > 0) {
                    intFormat.format(statisticsData.visitedCountries)
                } else {
                    "-"
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun RoadDataSection(statisticsData: StatisticsData, distanceFormat: NumberFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.road_surface),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalScrollbarContainer {
                statisticsData.totalRoadSurfaceMeter.forEach { (surface, meters) ->
                    SurfaceCard(
                        stringResource(surface.nameId),
                        meters / 1000.0,
                        distanceFormat
                    )
                }
            }

            Text(
                text = stringResource(R.string.road_type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            HorizontalScrollbarContainer {
                statisticsData.totalRoadTypeMeter.forEach { (roadType, meters) ->
                    SurfaceCard(
                        stringResource(roadType.nameId),
                        meters / 1000.0,
                        distanceFormat
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalScrollbarContainer(content: @Composable () -> Unit) {
    ScrollableRow(
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
fun ScrollableRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        content()
    }
}

@Composable
fun SurfaceCard(title: String, value: Double, numberFormat: NumberFormat) {
    Card(
        modifier = Modifier.width(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
            )
            Text(
                text = "${numberFormat.format(value)} km",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatItemCard(
    title: String,
    value: String,
    info: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatDurationHHms(value: Double, locale: java.util.Locale): String {
    val valueInMs = (value * 3600000.0).toLong()
    return String.format(
        locale,
        "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(valueInMs),
        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(valueInMs) % TimeUnit.MINUTES.toSeconds(1),
    )
}

@Composable
fun ExtremaValuesSection(
    statisticsData: StatisticsData,
    onNavigateToSummitDetails: (Long) -> Unit,
    distanceFormat: NumberFormat,
    intFormat: NumberFormat
) {
    val extremaValues = statisticsData.extremaValuesSummits
    val locale = LocalConfiguration.current.locales[0]

    if (extremaValues != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.statistics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Velocity section (horizontal scroll)
                val velocityEntries =
                    StatisticEntryDefinitions.entries.filter { it.group == StatisticGroup.VELOCITY }
                if (velocityEntries.any { entry ->
                        extremaValues.getSummitForEntry(entry) != null
                    }) {
                    Text(
                        text = stringResource(R.string.vertical_speed_up),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    HorizontalScrollbarContainer {
                        velocityEntries.forEach { entry ->
                            val summit = extremaValues.getSummitForEntry(entry)
                            if (summit != null) {
                                val value = entry.getValue(summit)
                                val formattedValue = if (entry.toHHms) {
                                    formatDurationHHms(value, locale)
                                } else {
                                    distanceFormat.format(value * entry.factor)
                                }

                                StatItemCard(
                                    title = stringResource(entry.nameId),
                                    value = formattedValue,
                                    info = "${summit.name}\n${summit.getDateAsString()}"
                                ) {
                                    onNavigateToSummitDetails(summit.id)
                                }
                            }
                        }
                    }
                }

                // Speed section (horizontal scroll)
                val speedEntries =
                    StatisticEntryDefinitions.entries.filter { it.group == StatisticGroup.SPEED }
                if (speedEntries.any { entry ->
                        extremaValues.getSummitForEntry(entry) != null
                    }) {
                    Text(
                        text = stringResource(R.string.speed),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    HorizontalScrollbarContainer {
                        speedEntries.forEach { entry ->
                            val summit = extremaValues.getSummitForEntry(entry)
                            if (summit != null) {
                                val value = entry.getValue(summit)
                                val formattedValue = if (entry.toHHms) {
                                    formatDurationHHms(value, locale)
                                } else {
                                    distanceFormat.format(value * entry.factor)
                                }

                                StatItemCard(
                                    title = stringResource(entry.nameId),
                                    value = formattedValue,
                                    info = "${summit.name}\n${summit.getDateAsString()}"
                                ) {
                                    onNavigateToSummitDetails(summit.id)
                                }
                            }
                        }
                    }
                }

                // Power section (horizontal scroll)
                val powerEntries =
                    StatisticEntryDefinitions.entries.filter { it.group == StatisticGroup.POWER }
                if (powerEntries.any { entry ->
                        extremaValues.getSummitForEntry(entry) != null
                    }) {
                    Text(
                        text = stringResource(R.string.power),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    HorizontalScrollbarContainer {
                        powerEntries.forEach { entry ->
                            val summit = extremaValues.getSummitForEntry(entry)
                            if (summit != null) {
                                val value = entry.getValue(summit)
                                val formattedValue = if (entry.toHHms) {
                                    formatDurationHHms(value, locale)
                                } else {
                                    intFormat.format(value * entry.factor)
                                }

                                StatItemCard(
                                    title = stringResource(entry.nameId),
                                    value = formattedValue,
                                    info = "${summit.name}\n${summit.getDateAsString()}"
                                ) {
                                    onNavigateToSummitDetails(summit.id)
                                }
                            }
                        }
                    }
                }

                // Other statistics (vertical list)
                StatisticEntryDefinitions.entries.forEach { entry ->
                    // Skip entries that are already shown in horizontal sections
                    if (entry.group == StatisticGroup.NONE) {

                        val summit = extremaValues.getSummitForEntry(entry)
                        if (summit != null) {
                            val value = entry.getValue(summit)
                            val formattedValue = if (entry.toHHms) {
                                formatDurationHHms(value, locale)
                            } else {
                                distanceFormat.format(value * entry.factor)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onNavigateToSummitDetails(summit.id)
                                    },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(entry.nameId),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = formattedValue,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${summit.name}\n${summit.getDateAsString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
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

// Extension function to get summit for an entry
fun ExtremaValuesSummits.getSummitForEntry(entry: StatisticEntryDefinitions): Summit? {
    return entry.getSummit(this)
}

@Composable
fun IncludeNotPersistedActivitiesChip(
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val contentColor = if (isDark) Color.White else Color.Black
    FilterChip(
        selected = selected,
        onClick = { onCheckedChange(!selected) },
        label = { Text(stringResource(R.string.include_not_persisted_activities)) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isDark) DarkCanvas else SurfaceMidGray,
            labelColor = contentColor,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}
