package de.drtobiasprinz.summitbook.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Peak
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.TextField
import de.drtobiasprinz.summitbook.models.TextFieldGroup
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun SummitEntryDataScreen(
    summit: Summit?,
    allSummits: List<Summit>?,
    summitsToCompare: List<Summit>,
    compareSummit: Summit?,
    segments: List<*>?,
    extrema: ExtremaValuesSummits?,
    onGetSummitToCompare: (Long) -> Unit,
    onSetSummitToCompareToNull: () -> Unit,
    modifier: Modifier = Modifier,
    peaks: List<Peak> = emptyList(),
    mountainPasses: List<SegmentEntry> = emptyList(),
    onEditMountainPass: ((SegmentEntry) -> Unit)? = null,
    onDeleteMountainPass: ((SegmentEntry) -> Unit)? = null
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

    var showMoreSpeedData by remember { mutableStateOf(false) }
    var showSurfaceData by remember { mutableStateOf(false) }
    var showRoadTypeData by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with date, name, and sport type
        item {
            SummitHeader(summit = summit)
        }

        // Comments
        if (summit.comments.isNotEmpty()) {
            item {
                Text(
                    text = summit.comments,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
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

        // Base data fields
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField.entries.filter { it.group == TextFieldGroup.Base }.forEach { field ->
                        DataFieldRow(
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

        // Additional speed data (expandable)
        if (summit.velocityData.hasAdditionalData()) {
            item {
                Card {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { showMoreSpeedData = !showMoreSpeedData },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = if (showMoreSpeedData) painterResource(R.drawable.ic_baseline_expand_less_24) else painterResource(
                                    R.drawable.ic_baseline_expand_more_24
                                ),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showMoreSpeedData)
                                    stringResource(R.string.less_speed)
                                else
                                    stringResource(R.string.more_speed)
                            )
                        }

                        if (showMoreSpeedData) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextField.entries.filter { it.group == TextFieldGroup.AdditionalSpeedData }
                                    .forEach { field ->
                                        DataFieldRow(
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

        // Surface data (expandable)
        if (summit.distancePerSurface.isNotEmpty() &&
            summit.distancePerSurface.values.any { it != 0 }
        ) {
            item {
                Card {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { showSurfaceData = !showSurfaceData },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = if (showSurfaceData) painterResource(R.drawable.ic_baseline_expand_less_24) else painterResource(
                                    R.drawable.ic_baseline_expand_more_24
                                ),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showSurfaceData)
                                    stringResource(R.string.road_surface_close)
                                else
                                    stringResource(R.string.road_surface)
                            )
                        }

                        if (showSurfaceData) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextField.entries.filter { it.group == TextFieldGroup.DistancePerSurface }
                                    .forEach { field ->
                                        DataFieldRow(
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

        // Road type data (expandable)
        if (summit.distancePerRoadType.isNotEmpty() &&
            summit.distancePerRoadType.values.any { it != 0 }
        ) {
            item {
                Card {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { showRoadTypeData = !showRoadTypeData },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = if (showRoadTypeData) painterResource(R.drawable.ic_baseline_expand_less_24) else painterResource(
                                    R.drawable.ic_baseline_expand_more_24
                                ),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showRoadTypeData)
                                    stringResource(R.string.road_type_close)
                                else
                                    stringResource(R.string.road_type)
                            )
                        }

                        if (showRoadTypeData) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextField.entries.filter { it.group == TextFieldGroup.DistancePerRoadType }
                                    .forEach { field ->
                                        DataFieldRow(
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

        // Countries chips
        val countries = summit.countries.filter { it != "" }
        if (countries.isNotEmpty() && countries.first().isNotEmpty()) {
            item {
                ChipSection(
                    title = stringResource(R.string.country_hint),
                    items = countries,
                    icon = R.drawable.ic_baseline_flag_24
                )
            }
        }

        // Participants chips
        val participants = summit.participants.filter { it != "" }
        if (participants.isNotEmpty() && participants.first().isNotEmpty()) {
            item {
                ChipSection(
                    title = stringResource(R.string.participants),
                    items = participants,
                    icon = R.drawable.ic_baseline_people_24
                )
            }
        }

        // Equipment chips
        val equipments = summit.equipments.filter { it != "" }
        if (equipments.isNotEmpty() && equipments.first().isNotEmpty()) {
            item {
                ChipSection(
                    title = stringResource(R.string.equipments),
                    items = equipments,
                    icon = R.drawable.ic_baseline_handyman_24
                )
            }
        }

        // Places chips
        item {
            val context = LocalContext.current
            val places = remember(summit.id, allSummits) {
                summit.getPlacesWithConnectedEntryString(context, allSummits ?: emptyList())
            }
            if (places.isNotEmpty() && places.first().isNotEmpty()) {
                PlacesChipSection(
                    title = stringResource(R.string.place_hint),
                    items = places,
                    peaks = peaks
                )
            }
        }

        // Segments
        item {
            SegmentsSection(
                summit = summit,
                segments = segments
            )
        }

        // Mountain Passes
        item {
            MountainPassesSection(
                summit = summit,
                mountainPasses = mountainPasses,
                onEditMountainPass = onEditMountainPass,
                onDeleteMountainPass = onDeleteMountainPass
            )
        }
    }
}

@Composable
fun SummitHeader(summit: Summit) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summit.getDateAsString() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summit.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Image(
            painter = painterResource(
                id = if (isDark) summit.sportType.imageIdWhite else summit.sportType.imageIdBlack
            ),
            contentDescription = summit.sportType.name,
            modifier = Modifier.size(48.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareDropdown(
    summitsToCompare: List<Summit>,
    currentCompare: Summit?,
    onSummitSelected: (Summit?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val items = remember(summitsToCompare) {
        summitsToCompare
    }

    // Pagination state
    val pageSize = 50
    val totalPages = remember(items.size) {
        (items.size + pageSize - 1) / pageSize
    }
    var currentPage by remember { mutableIntStateOf(0) }

    // Get paginated items
    val paginatedItems = remember(items, currentPage, pageSize) {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, items.size)
        if (start >= items.size) emptyList() else items.subList(start, end)
    }

    // Reset page when dropdown closes
    LaunchedEffect(expanded) {
        if (!expanded) {
            currentPage = 0
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentCompare?.let { "${it.getDateAsString()} ${it.name}" }
                ?: stringResource(R.string.none),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.summit_name_compare_hint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Pagination controls at top
            if (totalPages > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                        enabled = currentPage > 0
                    ) {
                        Text(text = stringResource(R.string.previous))
                    }
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            stringResource(R.string.page_of),
                            currentPage + 1,
                            totalPages
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages - 1) },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Text(text = stringResource(R.string.next))
                    }
                }
            }

            // Paginated items
            paginatedItems.forEach { summit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${summit.getDateAsString()} ${summit.name}"
                        )
                    },
                    onClick = {
                        onSummitSelected(summit)
                        expanded = false
                    }
                )
            }

            // Pagination controls at bottom
            if (totalPages > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                        enabled = currentPage > 0
                    ) {
                        Text(text = stringResource(R.string.previous))
                    }
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            stringResource(R.string.page_of),
                            currentPage + 1,
                            totalPages
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        onClick = { currentPage = (currentPage + 1).coerceAtMost(totalPages - 1) },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Text(text = stringResource(R.string.next))
                    }
                }
            }
        }
    }
}

@Composable
fun DataFieldRow(
    field: TextField,
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
                text = stringResource(field.nameId),
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
                text = formatValue(valueDouble, compareDouble, field, numberFormat),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun formatValue(
    value: Double,
    compareValue: Double?,
    field: TextField,
    numberFormat: NumberFormat
): String {
    numberFormat.maximumFractionDigits = field.digits

    return if (field.toHHms) {
        val valueInMs = (value * 1000.0).toLong()
        val hours = TimeUnit.MILLISECONDS.toHours(valueInMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(valueInMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(valueInMs) % 60

        if (compareValue != null && compareValue > 0) {
            val compareInMs = (compareValue * 1000.0).toLong()
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
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
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

@Composable
fun ChipSection(
    title: String,
    items: List<String>,
    icon: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                AssistChip(
                    onClick = { },
                    label = { Text(item) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PlacesChipSection(
    title: String,
    items: List<String>,
    peaks: List<Peak>
) {
    val peakNames = remember(peaks) { peaks.map { it.name }.toSet() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                val isPeak = peakNames.contains(item)
                AssistChip(
                    onClick = { },
                    label = { Text(item) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(
                                id = if (isPeak) R.drawable.baseline_terrain_24 else R.drawable.outline_landscape_2_off_24
                            ),
                            contentDescription = if (isPeak) "Peak in database" else "Place",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun SegmentsSection(
    summit: Summit,
    segments: List<*>?
) {
    // Use remember to ensure updateSegmentInfo is only called once per summit/segments combination
    val segmentInfo = remember(summit.id, segments) {
        @Suppress("UNCHECKED_CAST")
        segments?.let { summit.updateSegmentInfo(it as List<Segment>) }
        summit.segmentInfo
    }

    if (segmentInfo.isEmpty()) return

    var selectedSegment by remember { mutableStateOf<Triple<*, *, *>?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.segments),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(segmentInfo) { entry ->
                AssistChip(
                    onClick = { selectedSegment = entry },
                    label = {
                        Text(
                            stringResource(
                                R.string.rank_in_activity,
                                entry.third.toString(),
                                entry.second.getDisplayName()
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_route_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Show segment details if selected
        selectedSegment?.let { entry ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ranked, entry.third.toString()),
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Add segment details here based on entry.first properties
                }
            }
        }
    }
}

@Composable
fun MountainPassesSection(
    summit: Summit,
    mountainPasses: List<SegmentEntry>,
    onEditMountainPass: ((SegmentEntry) -> Unit)? = null,
    onDeleteMountainPass: ((SegmentEntry) -> Unit)? = null
) {
    // Filter mountain passes for this summit's activity
    val summitPasses = remember(summit.activityId, mountainPasses) {
        mountainPasses.filter { it.activityId == summit.activityId }
    }

    if (summitPasses.isEmpty()) return

    var selectedPass by remember { mutableStateOf<SegmentEntry?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.mountain_passes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summitPasses) { pass ->
                AssistChip(
                    onClick = {
                        selectedPass = if (selectedPass == pass) null else pass
                    },
                    label = {
                        Text(pass.getDisplayName())
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_add_mountain_pass_24),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Show mountain pass details if selected (with expand/collapse animation)
        AnimatedVisibility(
            visible = selectedPass != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            selectedPass?.let { pass ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pass.getDisplayName(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (onEditMountainPass != null) {
                                IconButton(
                                    onClick = { onEditMountainPass.invoke(pass) }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_edit_24),
                                        contentDescription = stringResource(R.string.edit),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (onDeleteMountainPass != null) {
                                IconButton(
                                    onClick = { onDeleteMountainPass.invoke(pass) }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_delete_24),
                                        contentDescription = stringResource(R.string.delete_mountain_pass),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Distance
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.outline_distance_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f km", pass.kilometers),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Elevation gain
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_keyboard_double_arrow_up_black_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.0f hm", pass.heightMetersUp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Elevation loss
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_keyboard_double_arrow_down_black_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.0f hm", pass.heightMetersDown),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Average gradient
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_trending_flat_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", pass.avgGradient),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Max grade in window
                            if (pass.windowDistanceMeters > 0) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_trending_up_black_24dp),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "%.0fm: %.1f%%",
                                            pass.windowDistanceMeters,
                                            pass.maxGradeInWindow
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Duration, average speed
                        val activeDuration = if (pass.durationInMotion > 0) pass.durationInMotion else pass.duration
                        val avgSpeed = if (activeDuration > 0) {
                            pass.kilometers / (activeDuration / 60.0)
                        } else 0.0
                        val avgSpeedTotal = if (pass.duration > 0) {
                            pass.kilometers / (pass.duration / 60.0)
                        } else 0.0

                        val durationText = if (pass.durationInMotion > 0 && pass.durationInMotion != pass.duration) {
                            String.format(
                                Locale.getDefault(),
                                "%.1f (%.1f) %s",
                                pass.durationInMotion,
                                pass.duration,
                                stringResource(R.string.min)
                            )
                        } else {
                            String.format(Locale.getDefault(), "%.1f %s", pass.duration, stringResource(R.string.min))
                        }

                        val speedText = if (pass.durationInMotion > 0 && pass.durationInMotion != pass.duration) {
                            String.format(
                                Locale.getDefault(),
                                "%.1f (%.1f) %s",
                                avgSpeed,
                                avgSpeedTotal,
                                stringResource(R.string.kmh)
                            )
                        } else {
                            String.format(Locale.getDefault(), "%.1f %s", avgSpeed, stringResource(R.string.kmh))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Duration
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_baseline_timer_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Average speed
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_speed_black_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = speedText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
