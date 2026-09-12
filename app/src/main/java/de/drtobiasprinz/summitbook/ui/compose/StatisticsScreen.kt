package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.models.StatisticEntryDefinitions
import de.drtobiasprinz.summitbook.models.StatisticGroup
import de.drtobiasprinz.summitbook.models.StatisticsData
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.sharedPreferences
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

@Composable
fun StatisticsScreen(
    filteredSummits: List<Summit>,
    forecasts: List<Forecast>,
    onNavigateToSummitDetails: (Long) -> Unit
) {
    val configuration = LocalConfiguration.current
    val numberFormat = NumberFormat.getInstance(configuration.locales[0])
    val annualTargetActivity =
        sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, "52") ?: "52"
    val annualTargetKm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200") ?: "1200"
    val annualTargetHm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET, "50000") ?: "50000"
    val indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
    var statisticsData by remember { mutableStateOf(StatisticsData()) }
    LaunchedEffect(filteredSummits, forecasts) {
        val statisticEntry = StatisticEntry(
            filteredSummits,
            annualTargetActivity.toIntOrNull() ?: 52,
            annualTargetKm.toIntOrNull() ?: 1200,
            annualTargetHm.toIntOrNull() ?: 50000,
            indoorHeightMeterPercent
        )
        statisticEntry.calculate()

        val extremaValuesSummits = ExtremaValuesSummits(
            filteredSummits, shouldIndoorActivityBeExcluded = true
        )

        statisticsData = StatisticsData(
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
            summits = filteredSummits
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCCCCCC))
            .padding(8.dp)
    ) {

        // Summary section
        item {
            SummarySection(statisticsData, numberFormat)
        }

        // Road surface section
        item {
            RoadSurfaceSection(statisticsData, numberFormat)
        }

        // Road type section
        item {
            RoadTypeSection(statisticsData, numberFormat)
        }

        // Height meters section
        item {
            HeightMetersSection(statisticsData, numberFormat)
        }

        // Achievement section
        item {
            AchievementSection(statisticsData, numberFormat)
        }

        // Extrema values sections
        item {
            ExtremaValuesSection(statisticsData, onNavigateToSummitDetails, numberFormat)
        }

        // Visited countries section
        item {
            VisitedCountriesSection(statisticsData, numberFormat)
        }
    }
}

@Composable
fun SummarySection(statisticsData: StatisticsData, numberFormat: NumberFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.total_activities),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = numberFormat.format(statisticsData.totalActivities),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.total_summits),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = numberFormat.format(statisticsData.totalSummits),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.total_kilometers),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "${numberFormat.format(statisticsData.totalKm)} km",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp
            )
        }
    }
}

@Composable
fun RoadSurfaceSection(statisticsData: StatisticsData, numberFormat: NumberFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.road_surface),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Horizontal scrollable row for road surfaces
            HorizontalScrollbarContainer {
                statisticsData.totalRoadSurfaceMeter.forEach { (surface, meters) ->
                    SurfaceCard(surface.name.replace("_", " "), meters / 1000.0, numberFormat)
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
                    .height(48.dp)
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
                fontSize = 20.sp,
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

@Composable
fun ExtremaValuesSection(
    statisticsData: StatisticsData,
    onNavigateToSummitDetails: (Long) -> Unit,
    numberFormat: NumberFormat
) {
    val extremaValues = statisticsData.extremaValuesSummits

    if (extremaValues != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterHorizontally),
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
                                    val valueInMs = (value * 3600000.0).toLong()
                                    String.format(
                                        LocalConfiguration.current.locales[0],
                                        "%02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(valueInMs),
                                        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(
                                            1
                                        ),
                                        TimeUnit.MILLISECONDS.toSeconds(valueInMs) % TimeUnit.MINUTES.toSeconds(
                                            1
                                        ),
                                    )
                                } else {
                                    numberFormat.format(value * entry.factor)
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
                                    val valueInMs = (value * 3600000.0).toLong()
                                    String.format(
                                        LocalConfiguration.current.locales[0],
                                        "%02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(valueInMs),
                                        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(
                                            1
                                        ),
                                        TimeUnit.MILLISECONDS.toSeconds(valueInMs) % TimeUnit.MINUTES.toSeconds(
                                            1
                                        ),
                                    )
                                } else {
                                    numberFormat.format(value * entry.factor)
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
                                    val valueInMs = (value * 3600000.0).toLong()
                                    String.format(
                                        LocalConfiguration.current.locales[0],
                                        "%02d:%02d:%02d",
                                        TimeUnit.MILLISECONDS.toHours(valueInMs),
                                        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(
                                            1
                                        ),
                                        TimeUnit.MILLISECONDS.toSeconds(valueInMs) % TimeUnit.MINUTES.toSeconds(
                                            1
                                        ),
                                    )
                                } else {
                                    numberFormat.format(value * entry.factor)
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
                                val valueInMs = (value * 3600000.0).toLong()
                                String.format(
                                    LocalConfiguration.current.locales[0],
                                    "%02d:%02d:%02d",
                                    TimeUnit.MILLISECONDS.toHours(valueInMs),
                                    TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(
                                        1
                                    ),
                                    TimeUnit.MILLISECONDS.toSeconds(valueInMs) % TimeUnit.MINUTES.toSeconds(
                                        1
                                    ),
                                )
                            } else {
                                numberFormat.format(value * entry.factor)
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
                                        .align(Alignment.CenterHorizontally),
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 25.sp
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
fun RoadTypeSection(statisticsData: StatisticsData, numberFormat: NumberFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.road_type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Horizontal scrollable row for road types
            HorizontalScrollbarContainer {
                statisticsData.totalRoadTypeMeter.forEach { (roadType, meters) ->
                    SurfaceCard(roadType.name.replace("_", " "), meters / 1000.0, numberFormat)
                }
            }
        }
    }
}

@Composable
fun HeightMetersSection(statisticsData: StatisticsData, numberFormat: NumberFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.total_height_meters),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "${numberFormat.format(statisticsData.totalHm)} hm",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp
            )
        }
    }
}

@Composable
fun AchievementSection(statisticsData: StatisticsData, numberFormat: NumberFormat) {
    if (statisticsData.totalActivities > 0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.achievement),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${numberFormat.format(statisticsData.achievement)}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp
                )
            }
        }
    }
}

@Composable
fun VisitedCountriesSection(statisticsData: StatisticsData, numberFormat: NumberFormat) {
    if (statisticsData.visitedCountries > 0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.visited_countries),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = numberFormat.format(statisticsData.visitedCountries),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp
                )
            }
        }
    }
}