package de.drtobiasprinz.summitbook.ui.compose

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.text.TextWatcher
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.GpsTrack.Companion.interpolateColor
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.Surface
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling.Companion.getSportTypeForMapProviders
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling.Companion.selectedItem
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.sharedPreferences
import de.drtobiasprinz.summitbook.ui.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.SummitUtils
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import kotlin.math.roundToLong

@Composable
fun SummitEntryTrackScreen(
    summit: Summit?,
    allSummits: List<Summit>?,
    compareSummit: Summit?,
    onGetSummitToCompare: (Long) -> Unit,
    onSetSummitToCompareToNull: () -> Unit,
    modifier: Modifier = Modifier
) {

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var gpsTrack by remember { mutableStateOf<GpsTrack?>(null) }
    var selectedCustomizeTrackItem by remember { mutableStateOf(TrackColor.Elevation) }
    var usedItemsForColorCode by remember { mutableStateOf<List<TrackColor>>(emptyList()) }
    var alreadyZoomedOnTrack by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var trackInfoText by remember { mutableStateOf<String?>(null) }
    var mapViewRef by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var trackVersion by remember { mutableIntStateOf(0) }

    if (summit == null) {
        Box(
            modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val summitsToCompare = remember(allSummits, summit.id) {
        allSummits?.let { summits ->
            SummitUtils.getSummitsToCompare(
                summits, summit, onlyWithPowerData = true
            )
        } ?: emptyList()
    }

    // Initialize GPS track - use summit.id to avoid infinite recomposition
    LaunchedEffect(summit.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            setGpsTrack(summit, useSimplifiedTrack = true) { track, slopeGraph ->
                gpsTrack = track
            }
        }
        isLoading = false

        // Load full track in background
        coroutineScope.launch(Dispatchers.IO) {
            setGpsTrack(summit, forceUpdate = true) { track, slopeGraph ->
                gpsTrack = track
                trackVersion++ // Increment to trigger map update
            }
        }
    }

    // Update used items for color code
    LaunchedEffect(gpsTrack) {
        usedItemsForColorCode = TrackColor.entries.filter { trackColorEntry ->
            gpsTrack?.trackPoints?.any {
                val value = trackColorEntry.f(it)
                value != null && value != 0.0
            } == true
        }.mapIndexed { i, entry ->
            entry.spinnerId = i
            entry
        }

        if (TrackColor.Elevation !in usedItemsForColorCode) {
            selectedCustomizeTrackItem = TrackColor.None
        } else if (TrackColor.Elevation in usedItemsForColorCode) {
            selectedCustomizeTrackItem = TrackColor.Elevation
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Compare dropdown
        if (!summit.isBookmark && summitsToCompare.isNotEmpty()) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CompareDropdown(
                    summitsToCompare = summitsToCompare,
                    currentCompare = compareSummit,
                    onSummitSelected = { selectedSummit ->
                        if (selectedSummit == null) {
                            onSetSummitToCompareToNull()
                        } else {
                            onGetSummitToCompare(selectedSummit.id)
                        }
                    })
            }
        }

        // Map view
        if (gpsTrack?.hasOnlyZeroCoordinates() == false || summit.latLng != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((Resources.getSystem().displayMetrics.heightPixels * 0.6 / Resources.getSystem().displayMetrics.density).dp)
                    .clipToBounds()
            ) {
                MapView(
                    summit = summit,
                    summitToCompare = compareSummit,
                    allSummits = allSummits,
                    selectedTrackColor = selectedCustomizeTrackItem,
                    calculateBoundingBox = !alreadyZoomedOnTrack,
                    onMapReady = { alreadyZoomedOnTrack = true },
                    onTrackInfoUpdate = { text -> trackInfoText = text },
                    onMapViewCreated = { mapView -> mapViewRef = mapView },
                    onLocationOverlayCreated = { overlay -> locationOverlayRef = overlay }
                )

                // Track info display (shows track points count or clicked track point info)
                val displayText =
                    trackInfoText ?: if (gpsTrack != null && gpsTrack!!.trackPoints.isNotEmpty()) {
                        "${gpsTrack!!.trackPoints.size} ${stringResource(R.string.pts)}"
                    } else {
                        null
                    }

                if (displayText != null) {
                    Text(
                        text = displayText,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .background(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                // Map control buttons
                MapControlButtons(
                    summit = summit,
                    allSummits = allSummits,
                    compareSummit = compareSummit,
                    mapView = mapViewRef,
                    locationOverlay = locationOverlayRef,
                    onCustomizeTrack = { showColorDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Chart view
            if (selectedCustomizeTrackItem.discreteInput) {
                BarChartView(
                    summit = summit,
                    trackColor = selectedCustomizeTrackItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .height((Resources.getSystem().displayMetrics.heightPixels * 0.2 / Resources.getSystem().displayMetrics.density).dp)
                )
            } else {
                LineChartView(
                    summit = summit,
                    gpsTrack = gpsTrack,
                    trackColor = selectedCustomizeTrackItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .height((Resources.getSystem().displayMetrics.heightPixels * 0.2 / Resources.getSystem().displayMetrics.density).dp)
                )
            }
        }
    }

    // Color customization dialog
    if (showColorDialog) {
        TrackColorDialog(usedItems = usedItemsForColorCode, onItemSelected = { item ->
            selectedCustomizeTrackItem = item
            showColorDialog = false
        }, onDismiss = { showColorDialog = false })
    }
}

@Composable
fun MapView(
    summit: Summit,
    summitToCompare: Summit?,
    allSummits: List<Summit>?,
    selectedTrackColor: TrackColor,
    calculateBoundingBox: Boolean,
    onMapReady: () -> Unit,
    onTrackInfoUpdate: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onMapViewCreated: (CustomMapViewToAllowScrolling) -> Unit = {},
    onLocationOverlayCreated: (MyLocationNewOverlay) -> Unit = {}
) {
    var mLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var rootViewWrapper by remember { mutableStateOf<FrameLayout?>(null) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        onDispose {
            mLocationOverlay?.disableMyLocation()
        }
    }

    AndroidView(
        factory = { ctx ->
            // Create a wrapper view with a TextView for track info
            val wrapper = FrameLayout(ctx).apply {
                val textView = TextView(ctx).apply {
                    id = R.id.track_value
                    visibility = android.view.View.GONE

                    // Add a TextWatcher to monitor text changes
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(s: android.text.Editable?) {
                            val text = s?.toString()
                            if (isVisible && !text.isNullOrEmpty()) {
                                onTrackInfoUpdate(text)
                            } else {
                                onTrackInfoUpdate(null)
                            }
                        }
                    })
                }
                addView(textView)
            }
            rootViewWrapper = wrapper

            CustomMapViewToAllowScrolling(ctx).apply {

                // Initialize location overlay
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)
                mLocationOverlay = locationOverlay
                
                // Notify that location overlay is created
                onLocationOverlayCreated(locationOverlay)

                // Configure map
                addDefaultSettings()

                if (PreferencesHelper.loadOnDeviceMaps() && FileHelper.getOnDeviceMapFiles(ctx)
                        .isNotEmpty()
                ) {
                    selectedItem = getSportTypeForMapProviders(summit.sportType, ctx)
                } else if (FileHelper.getOnDeviceMbtilesFiles(ctx).isNotEmpty()) {
                    selectedItem = MapProvider.MBTILES
                }
                setTileProvider()
                
                // Notify that map view is created
                onMapViewCreated(this)
            }
        }, update = { view ->
            // Ensure the view fills its allocated space
            view.layoutParams = FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Force measure and layout
            view.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(
                    view.width, android.view.View.MeasureSpec.EXACTLY
                ), android.view.View.MeasureSpec.makeMeasureSpec(
                    view.height, android.view.View.MeasureSpec.EXACTLY
                )
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)

            // Update map with track
            view.overlays.clear()
            mLocationOverlay?.let { view.overlays.add(it) }
            view.addDefaultSettings()

            if (summitToCompare != null) {
                view.drawTrack(summitToCompare, true, TrackColor.None, color = Color.BLACK)
            } else {
                val connectedEntries = summit.getConnectedEntries(allSummits)
                for (entry in connectedEntries) {
                    view.drawTrack(entry, true, TrackColor.None, color = Color.BLACK)
                }
            }

            view.addTrackAndMarker(
                summit,
                true,
                selectedTrackColor,
                true,
                rootView = rootViewWrapper,
                calculateBondingBox = calculateBoundingBox
            )

            if (calculateBoundingBox) {
                onMapReady()
            }

            view.enableRoadInfoOnMapClick()
            view.invalidate()

            // Monitor TextView changes and update Compose state
            rootViewWrapper?.findViewById<TextView>(R.id.track_value)?.let { textView ->
                val text = textView.text?.toString()
                if (textView.isVisible && !text.isNullOrEmpty()) {
                    onTrackInfoUpdate(text)
                } else {
                    onTrackInfoUpdate(null)
                }
            }
        }, modifier = modifier
    )
}

@Composable
fun MapControlButtons(
    summit: Summit,
    allSummits: List<Summit>?,
    compareSummit: Summit?,
    mapView: CustomMapViewToAllowScrolling?,
    locationOverlay: MyLocationNewOverlay?,
    onCustomizeTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val buttonColors = IconButtonDefaults.iconButtonColors(
        containerColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.7f)
    )

    Column(
        modifier = modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show all tracks button
        IconButton(
            onClick = {
                if (mapView != null && allSummits != null) {
                    coroutineScope.launch {
                        showAllTracksOfSummitInBoundingBox(
                            context = context,
                            mapView = mapView,
                            summit = summit,
                            compareSummit = compareSummit,
                            allSummits = allSummits
                        )
                    }
                }
            },
            colors = buttonColors
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_select_all_24),
                contentDescription = stringResource(R.string.show_all_tracks)
            )
        }

        // Change map type button
        IconButton(
            onClick = {
                mapView?.showMapTypeSelectorDialog()
            },
            colors = buttonColors
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_more_vert_black_24dp),
                contentDescription = stringResource(R.string.map_type)
            )
        }

        // Share GPS button
        IconButton(
            onClick = {
                if (summit.hasGpsTrack()) {
                    shareGpsTrack(context, summit)
                }
            },
            colors = buttonColors
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_share_black_24dp),
                contentDescription = stringResource(R.string.action_share)
            )
        }

        // Open with button
        IconButton(
            onClick = {
                if (summit.hasGpsTrack()) {
                    openGpsTrack(context, summit)
                }
            },
            colors = buttonColors
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_open_in_new_black_24dp),
                contentDescription = stringResource(R.string.open_with)
            )
        }

        // Center on location button
        IconButton(
            onClick = {
                if (locationOverlay?.isMyLocationEnabled == true && mapView != null) {
                    val mapController = mapView.controller
                    mapController.setZoom(15.0)
                    mapController.setCenter(locationOverlay.myLocation)
                }
            },
            colors = buttonColors
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_center_focus_strong_24),
                contentDescription = stringResource(R.string.center_on_current_location)
            )
        }

        // Customize track color button
        IconButton(
            onClick = onCustomizeTrack,
            colors = buttonColors
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_color_lens_black_24dp),
                contentDescription = stringResource(R.string.customize_track_color)
            )
        }
    }
}

@Composable
fun LineChartView(
    summit: Summit, gpsTrack: GpsTrack?, trackColor: TrackColor, modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current

    if (!summit.hasGpsTrack() || gpsTrack == null) return

    val actualTrackColor = if (trackColor == TrackColor.None || trackColor == TrackColor.Mileage) {
        TrackColor.Elevation
    } else {
        trackColor
    }
    val label = stringResource(actualTrackColor.labelId)
    val minLabel = stringResource(R.string.min)
    val maxLabel = stringResource(R.string.max)

    AndroidView(
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setDrawGridBackground(false)

                val xAxis = this.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format(
                            configuration.locales[0], "%.1f km", (value / 100f).roundToLong() / 10f
                        )
                    }
                }

                val leftAxis = this.axisLeft
                leftAxis.setDrawGridLines(true)
                leftAxis.isGranularityEnabled = true
            }
        }, update = { chart ->
            val lineChartEntries = gpsTrack.getTrackGraph(actualTrackColor.f)

            val dataSet = LineDataSet(lineChartEntries, label).apply {
                setDrawValues(false)
                setDrawFilled(true)
                setDrawCircles(false)
                axisDependency = YAxis.AxisDependency.LEFT
                color = Color.RED
                setCircleColor(Color.RED)
                lineWidth = 5f
                circleRadius = 3f
                fillAlpha = 50
                fillColor = Color.RED
                setDrawCircleHole(false)
                highLightColor = Color.rgb(244, 117, 117)
                setDrawHorizontalHighlightIndicator(true)

                // Set colors based on values
                val min = lineChartEntries.minByOrNull { it.y }?.y
                val max = lineChartEntries.maxByOrNull { it.y }?.y
                if (min != null && max != null) {
                    val colors = lineChartEntries.map {
                        val fraction = (it.y - min) / (max - min)
                        interpolateColor(
                            actualTrackColor.minColor, actualTrackColor.maxColor, fraction
                        )
                    }
                    this.colors = colors
                }
            }

            val dataSets: MutableList<ILineDataSet> = ArrayList()
            dataSets.add(dataSet)
            chart.data = LineData(dataSets)

            // Set colors based on theme
            val textColor = if (isDark) Color.WHITE else Color.BLACK
            chart.xAxis.textColor = textColor
            chart.axisRight.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.legend?.textColor = textColor

            // Set legend
            val legend = chart.legend
            legend.yEntrySpace = 10f
            legend.isWordWrapEnabled = true
            val l1 = LegendEntry(
                "$label $minLabel",
                Legend.LegendForm.CIRCLE,
                9f,
                5f,
                null,
                actualTrackColor.minColor
            )
            val l2 = LegendEntry(
                "$label $maxLabel",
                Legend.LegendForm.CIRCLE,
                9f,
                5f,
                null,
                actualTrackColor.maxColor
            )
            legend.setCustom(arrayOf(l1, l2))
            legend.isEnabled = true

            chart.invalidate()
        }, modifier = modifier
    )
}

@Composable
fun BarChartView(
    summit: Summit, trackColor: TrackColor, modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val configuration = LocalConfiguration.current

    if (!summit.hasGpsTrack()) return

    val noDataText = stringResource(R.string.no_data_available)
    val dataSetLabel = stringResource(trackColor.labelId)

    // Pre-compute labels outside of remember block
    val roadTypeLabels = RoadType.entries.associateWith { stringResource(it.nameId) }
    val surfaceLabels = Surface.entries.associateWith { stringResource(it.nameId) }

    // Pre-compute data
    val chartData = remember(summit, trackColor) {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()
        var i = 0

        if (trackColor == TrackColor.RoadType) {
            RoadType.entries.forEach { enumEntry ->
                val distance = (summit.distancePerRoadType[enumEntry] ?: 0) / 1000.0
                if (distance > 0.0) {
                    entries.add(BarEntry(i.toFloat(), distance.toFloat()))
                    labels.add(roadTypeLabels[enumEntry] ?: "")
                    colors.add(enumEntry.color)
                    i += 1
                }
            }
        } else if (trackColor == TrackColor.RoadSurface) {
            Surface.entries.forEach { enumEntry ->
                val distance = (summit.distancePerSurface[enumEntry] ?: 0) / 1000.0
                if (distance > 0.0) {
                    entries.add(BarEntry(i.toFloat(), distance.toFloat()))
                    labels.add(surfaceLabels[enumEntry] ?: "")
                    colors.add(enumEntry.color)
                    i += 1
                }
            }
        }

        Triple(entries, labels, colors)
    }

    AndroidView(
        factory = { ctx ->
            HorizontalBarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisLeft.isEnabled = false
                axisRight.isEnabled = false
            }
        }, update = { chart ->
            val (entries, labels, colors) = chartData

            if (entries.isNotEmpty()) {
                val dataSet = BarDataSet(entries, dataSetLabel)
                dataSet.colors = colors
                dataSet.valueTextSize = 12f
                dataSet.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format(
                            configuration.locales[0], "%.1f km", value
                        )
                    }
                }

                val barData = BarData(dataSet)
                chart.data = barData

                val xAxis = chart.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setLabelCount(labels.size, false)
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < labels.size) labels[index] else ""
                    }
                }

                // Set colors based on theme
                val textColor = if (isDark) Color.WHITE else Color.BLACK
                chart.xAxis.textColor = textColor
                chart.axisRight.textColor = textColor
                chart.axisLeft.textColor = textColor
                chart.legend?.textColor = textColor
                chart.data.setValueTextColor(textColor)

                chart.setFitBars(true)
                chart.invalidate()
            } else {
                chart.clear()
                chart.setNoDataText(noDataText)
            }
        }, modifier = modifier
    )
}

@Composable
fun TrackColorDialog(
    usedItems: List<TrackColor>, onItemSelected: (TrackColor) -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_code_selection)) },
        text = {
            Column {
                usedItems.forEach { item ->
                    TextButton(
                        onClick = { onItemSelected(item) }, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(item.nameId), modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        })
}

// Helper functions
private fun setGpsTrack(
    summit: Summit,
    useSimplifiedTrack: Boolean = false,
    forceUpdate: Boolean = false,
    onTrackLoaded: (GpsTrack?, MutableList<Entry>) -> Unit
) {
    if (summit.hasGpsTrack(useSimplifiedTrack)) {
        summit.setGpsTrack(useSimplifiedTrack = useSimplifiedTrack)
        val gpsTrack = summit.gpsTrack

        if (gpsTrack?.hasNoTrackPoints() == true || forceUpdate) {
            gpsTrack?.parseTrack(useSimplifiedIfExists = useSimplifiedTrack)
        }

        val trackSlopeGraph = if (gpsTrack?.trackPoints?.isNotEmpty() == true) {
            gpsTrack.getTrackSlopeGraph()
        } else {
            mutableListOf()
        }

        onTrackLoaded(gpsTrack, trackSlopeGraph)
    } else {
        onTrackLoaded(null, mutableListOf())
    }
}

private fun shareGpsTrack(context: android.content.Context, summit: Summit) {
    try {
        val uri = summit.copyGpsTrackToTempFile(context.externalCacheDir)?.let {
            FileProvider.getUriForFile(
                context, BuildConfig.APPLICATION_ID + ".provider", it
            )
        }
        val intentShareFile = Intent(Intent.ACTION_SEND)
        intentShareFile.type = "application/pdf"
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
        intentShareFile.putExtra(
            Intent.EXTRA_SUBJECT, context.getString(R.string.shared_gpx_subject)
        )
        intentShareFile.putExtra(
            Intent.EXTRA_TEXT, context.getString(
                R.string.shared_summit_gpx_text,
                summit.name,
                summit.getDateAsString(),
                summit.elevationData.toString(),
                summit.kilometers.toString()
            )
        )
        context.startActivity(intentShareFile)
    } catch (_: IOException) {
        Toast.makeText(
            context, context.getString(R.string.no_email_program_installed), Toast.LENGTH_LONG
        ).show()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context, context.getString(R.string.no_email_program_installed), Toast.LENGTH_LONG
        ).show()
    }
}

private fun openGpsTrack(context: android.content.Context, summit: Summit) {
    try {
        val uri = summit.copyGpsTrackToTempFile(context.externalCacheDir)?.let {
            FileProvider.getUriForFile(
                context, BuildConfig.APPLICATION_ID + ".provider", it
            )
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/gpx")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        context.startActivity(intent)
    } catch (_: IOException) {
        Toast.makeText(
            context, context.getString(R.string.gpx_file_not_copied), Toast.LENGTH_LONG
        ).show()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context, context.getString(R.string.gpx_viewer_not_installed), Toast.LENGTH_LONG
        ).show()
    }
}

private suspend fun showAllTracksOfSummitInBoundingBox(
    context: android.content.Context,
    mapView: CustomMapViewToAllowScrolling,
    summit: Summit,
    compareSummit: Summit?,
    allSummits: List<Summit>
) {
    val maxPointsToShow = sharedPreferences
        .getString(Keys.PREF_MAX_NUMBER_POINT, "10000")?.toInt() ?: 10000

    withContext(Dispatchers.Main) {
        mapView.overlays.clear()

        val summitsWithSameBoundingBox = allSummits.filter {
            it.activityId != summit.activityId && mapView.boundingBox?.let { bbox ->
                it.trackBoundingBox?.intersects(bbox)
            } == true
        }

        var pointsShown = 0
        var summitsShown = 0

        summitsWithSameBoundingBox.forEach { entry ->
            if (entry.hasGpsTrack() && pointsShown < maxPointsToShow) {
                withContext(Dispatchers.IO) {
                    if (entry.gpsTrack == null) {
                        entry.setGpsTrack()
                    }
                }

                withContext(Dispatchers.Main) {
                    entry.gpsTrack?.addGpsTrack(
                        mapView,
                        TrackColor.None,
                        summit = entry,
                        color = ContextCompat.getColor(context, entry.sportType.color)
                    )
                    summitsShown += 1
                    pointsShown += entry.gpsTrack?.trackPoints?.size ?: 0
                    mapView.zoomController.activate()
                }
            }
        }

        // Redraw the main summit track on top
        if (compareSummit != null) {
            mapView.drawTrack(compareSummit, true, TrackColor.None, color = Color.BLACK)
        } else {
            val connectedEntries = summit.getConnectedEntries(allSummits)
            for (entry in connectedEntries) {
                mapView.drawTrack(entry, true, TrackColor.None, color = Color.BLACK)
            }
        }

        mapView.addTrackAndMarker(
            summit,
            true,
            TrackColor.Elevation,
            true,
            calculateBondingBox = false
        )

        if (pointsShown > maxPointsToShow) {
            Toast.makeText(
                context,
                context.getString(
                    R.string.summits_shown,
                    summitsShown.toString(),
                    summitsWithSameBoundingBox.size.toString()
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
