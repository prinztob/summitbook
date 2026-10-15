package de.drtobiasprinz.summitbook.ui.compose

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.appstate.AppState.pythonInstance
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.ElevationInterval
import de.drtobiasprinz.summitbook.data.model.TrackIntervalData
import de.drtobiasprinz.summitbook.data.model.bestMatchedValue
import de.drtobiasprinz.summitbook.data.model.bestSplitValues
import de.drtobiasprinz.summitbook.data.model.bestWindowValue
import de.drtobiasprinz.summitbook.data.model.fastestMatchedDuration
import de.drtobiasprinz.summitbook.data.model.fastestSplitTimes
import de.drtobiasprinz.summitbook.data.model.readTrackIntervalData
import de.drtobiasprinz.summitbook.data.model.splitTimesForRange
import de.drtobiasprinz.summitbook.data.model.splitValuesForRange
import de.drtobiasprinz.summitbook.sync.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.theme.ChartLime
import de.drtobiasprinz.summitbook.ui.theme.RecordGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Metric dimension of the times tab: time/speed, power or heart rate. */
private enum class TimesMetric { TIME, POWER, HEART_RATE }

private val RANGE_CHOICES_KM = listOf(1, 2, 5, 10)
private val WINDOW_CHOICES_MIN = listOf(1, 10, 60)

/** How many split rows are visible before the card must be expanded. */
private const val COLLAPSED_SPLITS_COUNT = 5

/**
 * Times tab: best efforts and splits over a selectable km or time window,
 * compared against the same-course activities, plus the ascent and descent
 * sections of the track.
 */
@Composable
fun SummitEntryTimesScreen(
    summit: Summit?,
    summitsToCompare: List<Summit>,
    similarityScores: Map<Long, Double> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    var selectedMetric by rememberSaveable { mutableStateOf(TimesMetric.TIME) }
    var selectedRangeKm by rememberSaveable { mutableIntStateOf(1) }
    var selectedWindowMin by rememberSaveable { mutableIntStateOf(0) }
    var splitsExpanded by rememberSaveable { mutableStateOf(false) }
    var showComparisonInfo by rememberSaveable { mutableStateOf(false) }
    val windowIsTime = selectedWindowMin > 0

    if (summit == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val similarSummits = remember(summitsToCompare, similarityScores) {
        summitsToCompare.filter { similarityScores[it.activityId] != null }
    }

    var isLoading by remember { mutableStateOf(true) }
    var computeTotal by remember { mutableIntStateOf(0) }
    var computeCurrent by remember { mutableIntStateOf(0) }
    var currentData by remember { mutableStateOf<TrackIntervalData?>(null) }
    var similarData by remember { mutableStateOf<Map<Long, TrackIntervalData>>(emptyMap()) }
    LaunchedEffect(summit.id, similarSummits) {
        withContext(Dispatchers.IO) {
            isLoading = true
            computeTotal = 0
            computeCurrent = 0
            // Fast pass: read whatever is already persisted, collecting the
            // tracks that still have to be computed via the Python backfill
            val missing = mutableListOf<Summit>()
            currentData = readTrackIntervalData(summit).also {
                if (it == null && canComputeIntervalData(summit)) {
                    missing.add(summit)
                }
            }
            val loadedSimilarData = mutableMapOf<Long, TrackIntervalData>()
            similarSummits.forEach { similar ->
                val similarIntervalData = readTrackIntervalData(similar)
                if (similarIntervalData != null) {
                    loadedSimilarData[similar.activityId] = similarIntervalData
                } else if (canComputeIntervalData(similar)) {
                    missing.add(similar)
                }
            }
            similarData = loadedSimilarData.toMap()
            // Slow pass: compute the missing ones with progress; results are
            // persisted, so each track is only ever computed once
            computeTotal = missing.size
            missing.forEach { summitToCompute ->
                val computed = computeIntervalData(summitToCompute)
                computeCurrent++
                if (computed != null) {
                    if (summitToCompute.id == summit.id) {
                        currentData = computed
                    } else {
                        loadedSimilarData[summitToCompute.activityId] = computed
                        similarData = loadedSimilarData.toMap()
                    }
                }
            }
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Metric chips with the comparison info button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimesMetric.entries.forEach { metric ->
                FilterChip(
                    selected = selectedMetric == metric,
                    onClick = { selectedMetric = metric },
                    label = {
                        Text(
                            text = stringResource(
                                when (metric) {
                                    TimesMetric.TIME -> R.string.metric_time
                                    TimesMetric.POWER -> R.string.tab_text_4
                                    TimesMetric.HEART_RATE -> R.string.heart_rate
                                }
                            )
                        )
                    }
                )
            }
            if (similarSummits.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { showComparisonInfo = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_info_24),
                        contentDescription = stringResource(
                            R.string.comparison_activities_title,
                            similarSummits.size
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Window chips: km windows and time windows, one selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RANGE_CHOICES_KM.forEach { range ->
                FilterChip(
                    selected = !windowIsTime && selectedRangeKm == range,
                    onClick = {
                        selectedRangeKm = range
                        selectedWindowMin = 0
                    },
                    label = { Text(stringResource(R.string.km_range_label, range)) }
                )
            }
            WINDOW_CHOICES_MIN.forEach { minutes ->
                FilterChip(
                    selected = windowIsTime && selectedWindowMin == minutes,
                    onClick = { selectedWindowMin = minutes },
                    label = { Text(stringResource(R.string.min_range_label, minutes)) }
                )
            }
        }
        // Inline progress while the Python backfill computes interval
        // data of tracks that lack it (first time only); the already
        // computed values stay visible and update as results arrive
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = if (computeTotal > 0) {
                        stringResource(R.string.computing_interval_data) + " · " +
                                stringResource(
                                    R.string.loading_progress_counter,
                                    computeCurrent,
                                    computeTotal
                                )
                    } else {
                        stringResource(R.string.computing_interval_data)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val data = currentData
            if (!isLoading && data == null) {
                item {
                    Card {
                        Text(
                            text = stringResource(R.string.no_interval_data),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (data != null) {
                // Course best over the selected window across the same-course rides
                val otherSimilarData = similarData.values.toList()

                // Best effort over the selected window
                item {
                    BestEffortCard(
                        data = data,
                        otherSimilarData = otherSimilarData,
                        metric = selectedMetric,
                        rangeKm = selectedRangeKm,
                        windowMin = selectedWindowMin,
                        locale = locale
                    )
                }

                // Splits over the selected km range, with the best value
                // any same-course ride did in that same interval alongside
                if (!windowIsTime) {
                    when (selectedMetric) {
                        TimesMetric.TIME -> {
                            val splits = splitTimesForRange(data.kmTimes, selectedRangeKm)
                            val fastestSplits = fastestSplitTimes(
                                data.kmTimes, selectedRangeKm, otherSimilarData
                            )
                            if (splits.isNotEmpty()) {
                                item {
                                    Card {
                                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                            Text(
                                                text = stringResource(
                                                    R.string.splits_title,
                                                    selectedRangeKm
                                                ),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            val visibleSplits = if (splitsExpanded) {
                                                splits
                                            } else {
                                                splits.take(COLLAPSED_SPLITS_COUNT)
                                            }
                                            visibleSplits.forEachIndexed { index, seconds ->
                                                val fastest = fastestSplits.getOrNull(index)
                                                val isRecord = fastest != null && fastest >= seconds
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (index % 2 == 1) {
                                                                MaterialTheme.colorScheme.surfaceContainer
                                                            } else {
                                                                Color.Transparent
                                                            }
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = stringResource(
                                                            R.string.km_range_from_to,
                                                            index * selectedRangeKm + 1,
                                                            (index + 1) * selectedRangeKm
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = formatDuration(locale, seconds),
                                                        style = tabularNumbers(
                                                            MaterialTheme.typography.bodyMedium
                                                        )
                                                    )
                                                    if (fastest != null && fastest < seconds) {
                                                        Text(
                                                            text = stringResource(
                                                                R.string.interval_best_time,
                                                                formatDuration(locale, fastest)
                                                            ),
                                                            color = ChartLime,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        )
                                                    }
                                                    if (isRecord) {
                                                        RecordFlagIcon(
                                                            Modifier.padding(start = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (splits.size > COLLAPSED_SPLITS_COUNT) {
                                                TextButton(
                                                    onClick = {
                                                        splitsExpanded = !splitsExpanded
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = if (splitsExpanded) {
                                                            stringResource(R.string.show_less_splits)
                                                        } else {
                                                            stringResource(
                                                                R.string.show_all_splits,
                                                                splits.size
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        TimesMetric.POWER -> {
                            val powerSplits = splitValuesForRange(
                                data.powerPerKm, data.kmTimes, selectedRangeKm
                            )
                            if (powerSplits.isNotEmpty()) {
                                item {
                                    MetricSplitsCard(
                                        title = stringResource(
                                            R.string.power_splits_title,
                                            selectedRangeKm
                                        ),
                                        rangeKm = selectedRangeKm,
                                        values = powerSplits,
                                        bestValues = bestSplitValues(otherSimilarData) {
                                            splitValuesForRange(
                                                it.powerPerKm, it.kmTimes, selectedRangeKm
                                            )
                                        },
                                        expanded = splitsExpanded,
                                        onToggleExpanded = {
                                            splitsExpanded = !splitsExpanded
                                        },
                                        valueTemplate = R.string.value_with_watt,
                                        locale = locale
                                    )
                                }
                            }
                        }

                        TimesMetric.HEART_RATE -> {
                            val hrSplits = splitValuesForRange(
                                data.hrPerKm, data.kmTimes, selectedRangeKm
                            )
                            if (hrSplits.isNotEmpty()) {
                                item {
                                    MetricSplitsCard(
                                        title = stringResource(
                                            R.string.hr_splits_title,
                                            selectedRangeKm
                                        ),
                                        rangeKm = selectedRangeKm,
                                        values = hrSplits,
                                        bestValues = bestSplitValues(otherSimilarData) {
                                            splitValuesForRange(
                                                it.hrPerKm, it.kmTimes, selectedRangeKm
                                            )
                                        },
                                        expanded = splitsExpanded,
                                        onToggleExpanded = {
                                            splitsExpanded = !splitsExpanded
                                        },
                                        valueTemplate = R.string.value_with_bpm,
                                        locale = locale
                                    )
                                }
                            }
                        }
                    }
                }

                // Ascents and descents with the best same-course value alongside
                item {
                    ElevationIntervalsCard(
                        title = stringResource(R.string.ascents_title),
                        iconRes = R.drawable.ic_baseline_arrow_upward_24,
                        metric = selectedMetric,
                        intervals = data.ascents,
                        otherIntervals = otherSimilarData.flatMap { it.ascents },
                        locale = locale
                    )
                }
                item {
                    ElevationIntervalsCard(
                        title = stringResource(R.string.descents_title),
                        iconRes = R.drawable.ic_baseline_arrow_downward_24,
                        metric = selectedMetric,
                        intervals = data.descents,
                        otherIntervals = otherSimilarData.flatMap { it.descents },
                        locale = locale
                    )
                }
            }
        }
    }

    if (showComparisonInfo && similarSummits.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showComparisonInfo = false },
            title = {
                Text(
                    text = stringResource(
                        R.string.comparison_activities_title,
                        similarSummits.size
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.comparison_activities_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(max = 400.dp)
                    ) {
                        items(similarSummits) { similar ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${similar.getDateAsString()} ${similar.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                similarityScores[similar.activityId]?.let { score ->
                                    Text(
                                        text = stringResource(
                                            R.string.similarity_score_suffix,
                                            String.format(locale, "%.0f", score)
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showComparisonInfo = false }) {
                    Text(text = stringResource(R.string.close))
                }
            }
        )
    }
}

/**
 * Best effort over the selected window: fastest time (km windows) or
 * farthest distance (time windows) for the time metric, best average power
 * or heart rate for the other metrics - always against the same-course
 * rides.
 */
@Composable
private fun BestEffortCard(
    data: TrackIntervalData,
    otherSimilarData: List<TrackIntervalData>,
    metric: TimesMetric,
    rangeKm: Int,
    windowMin: Int,
    locale: Locale
) {
    val windowIsTime = windowMin > 0
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            when (metric) {
                TimesMetric.TIME -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                if (windowIsTime) {
                                    R.drawable.ic_baseline_av_timer_24
                                } else {
                                    R.drawable.ic_baseline_timer_24
                                }
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(
                                if (windowIsTime) {
                                    R.string.best_distance_min_label
                                } else {
                                    R.string.best_effort_label
                                },
                                if (windowIsTime) windowMin else rangeKm
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (!windowIsTime) {
                        val effort = data.bestEfforts[rangeKm]
                        if (effort != null) {
                            val courseBest = otherSimilarData
                                .mapNotNull { it.bestEfforts[rangeKm]?.timeSeconds }
                                .minOrNull()
                            val isRecord =
                                courseBest != null && courseBest >= effort.timeSeconds
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (isRecord) {
                                    RecordFlagIcon()
                                }
                                Text(
                                    text = formatDuration(locale, effort.timeSeconds),
                                    style = tabularNumbers(
                                        MaterialTheme.typography.headlineMedium
                                    )
                                )
                                Text(
                                    text = stringResource(
                                        R.string.value_with_kmh,
                                        String.format(
                                            locale,
                                            "%.1f",
                                            (effort.endDistanceMeters - effort.startDistanceMeters) /
                                                effort.timeSeconds * 3.6
                                        )
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.best_effort_at,
                                    formatKm(locale, effort.startDistanceMeters),
                                    formatKm(locale, effort.endDistanceMeters)
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (courseBest != null && courseBest < effort.timeSeconds) {
                                Text(
                                    text = stringResource(
                                        R.string.best_effort_course_best,
                                        formatDuration(locale, courseBest)
                                    ),
                                    color = ChartLime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.no_best_effort, rangeKm),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        val covered = data.bestDistanceMin[windowMin]
                        if (covered != null) {
                            val othersBest = bestWindowValue(otherSimilarData, windowMin) {
                                it.bestDistanceMin
                            }
                            val isRecord = othersBest != null && othersBest <= covered
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (isRecord) {
                                    RecordFlagIcon()
                                }
                                Text(
                                    text = stringResource(
                                        R.string.value_with_km,
                                        formatKm(locale, covered)
                                    ),
                                    style = tabularNumbers(
                                        MaterialTheme.typography.headlineMedium
                                    )
                                )
                                Text(
                                    text = stringResource(
                                        R.string.value_with_kmh,
                                        String.format(
                                            locale,
                                            "%.1f",
                                            covered / (windowMin * 60.0) * 3.6
                                        )
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }
                            if (othersBest != null && othersBest > covered) {
                                Text(
                                    text = stringResource(
                                        R.string.interval_best_value,
                                        formatKm(locale, othersBest)
                                    ),
                                    color = ChartLime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.no_best_effort_min, windowMin),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                TimesMetric.POWER -> {
                    MetricBestEffortContent(
                        value = if (windowIsTime) {
                            data.bestPowerMin[windowMin]
                        } else {
                            data.bestPowerKm[rangeKm]
                        },
                        hasAnyData = data.powerPerKm.isNotEmpty(),
                        otherSimilarData = otherSimilarData,
                        window = if (windowIsTime) windowMin else rangeKm,
                        selector = if (windowIsTime) {
                            { it: TrackIntervalData -> it.bestPowerMin }
                        } else {
                            { it: TrackIntervalData -> it.bestPowerKm }
                        },
                        titleRes = if (windowIsTime) {
                            R.string.best_power_min_label
                        } else {
                            R.string.best_power_km_label
                        },
                        titleArg = if (windowIsTime) windowMin else rangeKm,
                        iconRes = R.drawable.baseline_electric_bolt_24,
                        noDataRes = R.string.no_power_data,
                        missingWindowRes = if (windowIsTime) {
                            R.string.no_best_effort_min
                        } else {
                            R.string.no_best_effort
                        },
                        valueTemplate = R.string.value_with_watt,
                        locale = locale
                    )
                }

                TimesMetric.HEART_RATE -> {
                    MetricBestEffortContent(
                        value = if (windowIsTime) {
                            data.bestHrMin[windowMin]
                        } else {
                            data.bestHrKm[rangeKm]
                        },
                        hasAnyData = data.hrPerKm.isNotEmpty(),
                        otherSimilarData = otherSimilarData,
                        window = if (windowIsTime) windowMin else rangeKm,
                        selector = if (windowIsTime) {
                            { it: TrackIntervalData -> it.bestHrMin }
                        } else {
                            { it: TrackIntervalData -> it.bestHrKm }
                        },
                        titleRes = if (windowIsTime) {
                            R.string.best_heart_rate_min_label
                        } else {
                            R.string.best_heart_rate_km_label
                        },
                        titleArg = if (windowIsTime) windowMin else rangeKm,
                        iconRes = R.drawable.ic_baseline_monitor_heart_24,
                        noDataRes = R.string.no_heart_rate_data,
                        missingWindowRes = if (windowIsTime) {
                            R.string.no_best_effort_min
                        } else {
                            R.string.no_best_effort
                        },
                        valueTemplate = R.string.value_with_bpm,
                        locale = locale
                    )
                }
            }
        }
    }
}

/**
 * The power/heart rate variant of the best-effort card: the best average
 * value over the selected window with the course best alongside.
 */
@Composable
private fun MetricBestEffortContent(
    value: Double?,
    hasAnyData: Boolean,
    otherSimilarData: List<TrackIntervalData>,
    window: Int,
    selector: (TrackIntervalData) -> Map<Int, Double>,
    @StringRes titleRes: Int,
    titleArg: Int,
    @DrawableRes iconRes: Int,
    @StringRes noDataRes: Int,
    @StringRes missingWindowRes: Int,
    @StringRes valueTemplate: Int,
    locale: Locale
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(titleRes, titleArg),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    if (value == null) {
        Text(
            text = if (hasAnyData) {
                stringResource(missingWindowRes, titleArg)
            } else {
                stringResource(noDataRes)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        return
    }
    val othersBest = bestWindowValue(otherSimilarData, window, selector)
    val isRecord = othersBest != null && othersBest <= value
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        if (isRecord) {
            RecordFlagIcon()
        }
        Text(
            text = stringResource(
                valueTemplate,
                String.format(locale, "%.0f", value)
            ),
            style = tabularNumbers(MaterialTheme.typography.headlineMedium)
        )
    }
    if (othersBest != null && othersBest > value) {
        Text(
            text = stringResource(
                R.string.interval_best_value,
                String.format(locale, "%.0f", othersBest)
            ),
            color = ChartLime,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Splits card for the power or heart rate metric: the time-weighted average
 * value per selected km interval, with the best value any same-course ride
 * achieved in that same interval alongside.
 */
@Composable
private fun MetricSplitsCard(
    title: String,
    rangeKm: Int,
    values: List<Double>,
    bestValues: List<Double?>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    @StringRes valueTemplate: Int,
    locale: Locale
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            val visibleValues = if (expanded) {
                values
            } else {
                values.take(COLLAPSED_SPLITS_COUNT)
            }
            visibleValues.forEachIndexed { index, value ->
                val best = bestValues.getOrNull(index)
                val isRecord = best != null && best <= value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (index % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.km_range_from_to,
                            index * rangeKm + 1,
                            (index + 1) * rangeKm
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(
                            valueTemplate,
                            String.format(locale, "%.0f", value)
                        ),
                        style = tabularNumbers(MaterialTheme.typography.bodyMedium)
                    )
                    if (best != null && best > value) {
                        Text(
                            text = stringResource(
                                R.string.interval_best_value,
                                String.format(locale, "%.0f", best)
                            ),
                            color = ChartLime,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (isRecord) {
                        RecordFlagIcon(Modifier.padding(start = 4.dp))
                    }
                }
            }
            if (values.size > COLLAPSED_SPLITS_COUNT) {
                TextButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (expanded) {
                            stringResource(R.string.show_less_splits)
                        } else {
                            stringResource(R.string.show_all_splits, values.size)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ElevationIntervalsCard(
    title: String,
    @DrawableRes iconRes: Int,
    metric: TimesMetric,
    intervals: List<ElevationInterval>,
    otherIntervals: List<ElevationInterval>,
    locale: Locale
) {
    Card {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (intervals.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_intervals),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val metricMissing = when (metric) {
                TimesMetric.TIME -> false
                TimesMetric.POWER -> intervals.none { it.power != null }
                TimesMetric.HEART_RATE -> intervals.none { it.hr != null }
            }
            if (metricMissing) {
                Text(
                    text = stringResource(
                        if (metric == TimesMetric.POWER) {
                            R.string.no_power_data
                        } else {
                            R.string.no_heart_rate_data
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            intervals.forEach { interval ->
                val fastest = fastestMatchedDuration(interval, otherIntervals)
                val isTimeRecord = fastest != null && fastest >= interval.durationSeconds
                // the metric value of this section: duration, power or heart rate
                val metricValue = when (metric) {
                    TimesMetric.TIME -> interval.durationSeconds
                    TimesMetric.POWER -> interval.power
                    TimesMetric.HEART_RATE -> interval.hr
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                R.string.km_range_from_to_decimal,
                                formatKm(locale, interval.startDistanceMeters),
                                formatKm(locale, interval.endDistanceMeters)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.elevation_interval_details,
                                String.format(
                                    locale,
                                    "%+d",
                                    interval.elevationDelta.roundToInt()
                                ),
                                String.format(
                                    locale,
                                    "%.1f",
                                    if (interval.lengthMeters > 0) {
                                        abs(interval.elevationDelta) /
                                            interval.lengthMeters * 100.0
                                    } else {
                                        0.0
                                    }
                                ),
                                interval.lengthMeters.roundToInt()
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        if (metric == TimesMetric.TIME || metricValue == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDuration(locale, interval.durationSeconds),
                                    style = tabularNumbers(
                                        MaterialTheme.typography.titleMedium
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                if (isTimeRecord) {
                                    RecordFlagIcon(Modifier.padding(start = 4.dp))
                                }
                            }
                            if (fastest != null && fastest < interval.durationSeconds) {
                                Text(
                                    text = stringResource(
                                        R.string.interval_best_time,
                                        formatDuration(locale, fastest)
                                    ),
                                    color = ChartLime,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        } else {
                            val valueTemplate = if (metric == TimesMetric.POWER) {
                                R.string.value_with_watt
                            } else {
                                R.string.value_with_bpm
                            }
                            val othersBest = bestMatchedValue(interval, otherIntervals) {
                                if (metric == TimesMetric.POWER) it.power else it.hr
                            }
                            val isRecord = othersBest != null && othersBest <= metricValue
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(
                                        valueTemplate,
                                        String.format(locale, "%.0f", metricValue)
                                    ),
                                    style = tabularNumbers(
                                        MaterialTheme.typography.titleMedium
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                                if (isRecord) {
                                    RecordFlagIcon(Modifier.padding(start = 4.dp))
                                }
                            }
                            if (othersBest != null && othersBest > metricValue) {
                                Text(
                                    text = stringResource(
                                        R.string.interval_best_value,
                                        String.format(locale, "%.0f", othersBest)
                                    ),
                                    color = ChartLime,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                text = formatDuration(locale, interval.durationSeconds),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordFlagIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_baseline_flag_24),
        contentDescription = stringResource(R.string.course_record),
        tint = RecordGold,
        modifier = modifier.size(16.dp)
    )
}

private fun tabularNumbers(style: TextStyle): TextStyle =
    style.copy(fontFeatureSettings = "tnum")

/**
 * Whether [summit]'s interval data can be computed via the Python backfill.
 */
private fun canComputeIntervalData(summit: Summit): Boolean =
    summit.hasTrackData() && !summit.ignoreSimplifyingTrack

/**
 * Computes the interval data of [summit] via the Python backfill and reads
 * the persisted result. Returns null when the computation fails.
 */
private fun computeIntervalData(summit: Summit): TrackIntervalData? {
    return try {
        pythonInstance?.let { python ->
            GpxPyExecutor(python).computeIntervalData(summit)
            readTrackIntervalData(summit)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error computing interval data for ${summit.getDateAsString()}_${summit.name}: ${e.message}")
        null
    }
}

private fun formatDuration(locale: Locale, seconds: Double): String {
    val total = seconds.toLong()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return if (hours > 0) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(locale, "%d:%02d", minutes, secs)
    }
}

private fun formatKm(locale: Locale, meters: Double): String =
    String.format(locale, "%.1f", meters / 1000.0)

private const val TAG = "SummitEntryTimesScreen"
