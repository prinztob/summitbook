package de.drtobiasprinz.summitbook.ui.compose

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.ExtensionFromYaml
import de.drtobiasprinz.summitbook.data.model.GpsTrack
import de.drtobiasprinz.summitbook.data.model.TrackColor
import de.drtobiasprinz.summitbook.ui.theme.ChartTextLightGray
import de.drtobiasprinz.summitbook.ui.theme.CompareTrackColor
import de.drtobiasprinz.summitbook.ui.theme.HighlightYellow
import de.drtobiasprinz.summitbook.ui.theme.Scrim
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling.Companion.getSportTypeForMapProviders
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling.Companion.selectedItem
import de.drtobiasprinz.summitbook.data.appstate.AppState.sharedPreferences
import de.drtobiasprinz.summitbook.data.maps.MapProvider
import de.drtobiasprinz.summitbook.data.maps.FileHelper
import de.drtobiasprinz.summitbook.core.preferences.PreferencesHelper
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException

@Composable
fun SummitEntryTrackScreen(
    summit: Summit?,
    allSummits: List<Summit>?,
    summitsToCompare: List<Summit>,
    compareSummit: Summit?,
    modifier: Modifier = Modifier,
    isAnalyzingTrack: Boolean = false,
    onShowSnackbar: (String) -> Unit = {},
    onGetSummitToCompare: (Long) -> Unit,
    onSetSummitToCompareToNull: () -> Unit
) {

    var isLoading by remember { mutableStateOf(true) }
    var trackPoints by remember {
        mutableStateOf<List<Pair<TrackPoint, ExtensionFromYaml>>>(
            emptyList()
        )
    }
    var compareTrackPoints by remember {
        mutableStateOf<List<Pair<TrackPoint, ExtensionFromYaml>>>(
            emptyList()
        )
    }
    var connectedTrackPoints by remember {
        mutableStateOf<List<List<Pair<TrackPoint, ExtensionFromYaml>>>>(
            emptyList()
        )
    }
    var selectedCustomizeTrackItem by rememberSaveable(stateSaver = enumSaver<TrackColor>()) {
        mutableStateOf(TrackColor.Elevation)
    }
    var usedItemsForColorCode by remember { mutableStateOf<List<TrackColor>>(emptyList()) }
    var alreadyZoomedOnTrack by rememberSaveable { mutableStateOf(false) }
    var userPickedColor by rememberSaveable { mutableStateOf(false) }
    var showColorDialog by rememberSaveable { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    var locationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var selectedTrackPointIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    if (summit == null) {
        Box(
            modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Initialize GPS track - use summit.id to avoid infinite recomposition.
    // Load the simplified track first (fast) so the map shows quickly, then
    // upgrade to the full track in the background: only the full track has
    // the YAML extension data (heart rate, power, ...) needed for track
    // coloring. The spinner is shown for the first load only, so it does not
    // reappear when the simplified points are replaced by the full ones.
    LaunchedEffect(summit.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val useSimplifiedTracks =
                sharedPreferences.getBoolean("pref_use_simplified_tracks", true)
            if (useSimplifiedTracks && summit.hasGpsTrack(simplified = true)) {
                setGpsTrack(summit, useSimplifiedTrack = true) { track ->
                    trackPoints = track?.trackPoints ?: emptyList()
                }
                isLoading = false
                setGpsTrack(summit, forceUpdate = true) { track ->
                    trackPoints = track?.trackPoints ?: emptyList()
                }
            } else {
                setGpsTrack(summit, forceUpdate = true) { track ->
                    trackPoints = track?.trackPoints ?: emptyList()
                }
            }
        }
        isLoading = false
    }

    // Load compare track asynchronously
    LaunchedEffect(compareSummit?.id) {
        if (compareSummit != null) {
            withContext(Dispatchers.IO) {
                setGpsTrackWithFullTrackFallback(compareSummit) { track ->
                    compareTrackPoints = track?.trackPoints ?: emptyList()
                }
            }
        } else {
            compareTrackPoints = emptyList()
        }
    }

    // Load connected tracks asynchronously
    LaunchedEffect(summit.id, allSummits?.size, summit.connectedActivityIds) {
        if (allSummits != null) {
            withContext(Dispatchers.IO) {
                val connectedEntries = summit.getConnectedEntries(allSummits)
                val trackPointList: MutableList<List<Pair<TrackPoint, ExtensionFromYaml>>> = mutableListOf()
                for (entry in connectedEntries) {
                    setGpsTrackWithFullTrackFallback(entry) { track ->
                        trackPointList.add(track?.trackPoints ?: emptyList())
                    }
                }
                connectedTrackPoints = trackPointList
            }
        } else {
            connectedTrackPoints = emptyList()
        }
    }

    // Update used items for color code
    LaunchedEffect(trackPoints) {
        usedItemsForColorCode = withContext(Dispatchers.Default) {
            TrackColor.entries.filter { trackColorEntry ->
                trackPoints.any {
                    val value = trackColorEntry.f(it)
                    value != null && value != 0.0
                }
            }
        }

        if (!userPickedColor) {
            selectedCustomizeTrackItem =
                if (TrackColor.Elevation in usedItemsForColorCode) {
                    TrackColor.Elevation
                } else {
                    TrackColor.None
                }
        }
    }

    // Track point bookkeeping: O(n) scan, so compute it once per track
    val trackHasOnlyZeroCoordinates = remember(trackPoints) {
        hasOnlyZeroCoordinates(trackPoints)
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

        // Loading panel
        if (isLoading) {
            LoadingPanel(
                visible = true,
                statusText = stringResource(
                    if (isAnalyzingTrack) R.string.analyzing_track
                    else R.string.loading_please_wait
                )
            )
        } else {
            if (isAnalyzingTrack && trackHasOnlyZeroCoordinates && summit.latLng == null) {
                // Track data is still being generated in the background
                Text(
                    text = stringResource(R.string.analyzing_track),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            if (!trackHasOnlyZeroCoordinates || summit.latLng != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((Resources.getSystem().displayMetrics.heightPixels * 0.6 / Resources.getSystem().displayMetrics.density).dp)
                        .clipToBounds()
                ) {
                    SummitEntryTrackMapView(
                        summit = summit,
                        trackPoints = trackPoints,
                        compareTrackPoints = compareTrackPoints,
                        connectedTrackPoints = connectedTrackPoints,
                        summitToCompare = compareSummit,
                        selectedTrackColor = selectedCustomizeTrackItem,
                        calculateBoundingBox = !alreadyZoomedOnTrack,
                        onMapReady = { alreadyZoomedOnTrack = true },
                        onMapViewCreated = { mapView -> mapViewRef = mapView },
                        onLocationOverlayCreated = { overlay -> locationOverlayRef = overlay },
                        selectedTrackPointIndex = selectedTrackPointIndex,
                        onTrackPointSelected = { index -> selectedTrackPointIndex = index }
                    )

                    // Track info display (shows track points count)
                    val displayText =
                        if (trackPoints.isNotEmpty()) {
                            "${trackPoints.size} ${stringResource(R.string.pts)}"
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
                                    Scrim,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    ChartTextLightGray.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChartTextLightGray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Map control buttons
                    TrackMapControlButtons(
                        summit = summit,
                        trackPoints = trackPoints,
                        allSummits = allSummits,
                        mapView = mapViewRef,
                        locationOverlay = locationOverlayRef,
                        compareTrackPoints = compareTrackPoints,
                        summitToCompare = compareSummit,
                        onShowSnackbar = onShowSnackbar,
                        onCustomizeTrack = { showColorDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            } else if (!isAnalyzingTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((Resources.getSystem().displayMetrics.heightPixels * 0.6 / Resources.getSystem().displayMetrics.density).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.no_gps_data))
                }
            }

            // Chart view
            if (selectedCustomizeTrackItem.discreteInput) {
                HorizontalBarChartView(
                    summit = summit,
                    trackColor = selectedCustomizeTrackItem,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .height((Resources.getSystem().displayMetrics.heightPixels * 0.2 / Resources.getSystem().displayMetrics.density).dp)
                )
            } else {
                LineChartView(
                    trackPoints = trackPoints,
                    trackColor = selectedCustomizeTrackItem,
                    selectedTrackPointIndex = selectedTrackPointIndex,
                    onTrackPointSelected = { index -> selectedTrackPointIndex = index },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .height((Resources.getSystem().displayMetrics.heightPixels * 0.3 / Resources.getSystem().displayMetrics.density).dp)
                )
            }
        }
    }

    // Color customization dialog
    if (showColorDialog) {
        TrackColorDialog(usedItems = usedItemsForColorCode, onItemSelected = { item ->
            userPickedColor = true
            selectedCustomizeTrackItem = item
            showColorDialog = false
        }, onDismiss = { showColorDialog = false })
    }
}

private fun hasOnlyZeroCoordinates(trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>): Boolean {
    return trackPoints.none { it.first.latitude != 0.0 && it.first.longitude != 0.0 }
}

@Composable
fun SummitEntryTrackMapView(
    summit: Summit,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    modifier: Modifier = Modifier,
    compareTrackPoints: List<Pair<TrackPoint, ExtensionFromYaml>> = emptyList(),
    connectedTrackPoints: List<List<Pair<TrackPoint, ExtensionFromYaml>>> = emptyList(),
    summitToCompare: Summit?,
    selectedTrackColor: TrackColor,
    calculateBoundingBox: Boolean,
    onMapReady: () -> Unit,
    onMapViewCreated: (CustomMapViewToAllowScrolling) -> Unit = {},
    onLocationOverlayCreated: (MyLocationNewOverlay) -> Unit = {},
    selectedTrackPointIndex: Int? = null,
    onTrackPointSelected: (Int) -> Unit = {}
) {
    var mLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var mapCreatedView by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    var lastOverlayKey by remember { mutableStateOf<Any?>(null) }
    var selectedPointMarker by remember { mutableStateOf<Marker?>(null) }
    var lastMarkerIndex by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mLocationOverlay?.disableMyLocation()
        }
    }

    if (calculateBoundingBox && trackPoints.isNotEmpty()) {
        SideEffect { onMapReady() }
    }

    // Discovering on-device maps reads preferences and SAF folder listings
    // (disk access); keep that off the main thread and apply the tile provider
    // once the results are in.
    LaunchedEffect(mapCreatedView) {
        val view = mapCreatedView ?: return@LaunchedEffect
        val ctx = view.context
        val provider = withContext(Dispatchers.IO) {
            if (PreferencesHelper.loadOnDeviceMaps() &&
                FileHelper.getOnDeviceMapFiles(ctx).isNotEmpty()
            ) {
                getSportTypeForMapProviders(summit.sportType, ctx)
            } else if (FileHelper.getOnDeviceMbtilesFiles(ctx).isNotEmpty()) {
                MapProvider.MBTILES
            } else {
                null
            }
        }
        provider?.let { selectedItem = it }
        view.setTileProvider()
    }

    SummitBookMapView(
        onMapCreated = { view ->
            val ctx = view.context
            // Initialize location overlay (only if the permission is granted;
            // this detail screen has no permission-request flow of its own)
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), view)
            if (ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationOverlay.enableMyLocation()
            }
            view.overlays.add(locationOverlay)
            mLocationOverlay = locationOverlay

            // Notify that location overlay is created
            onLocationOverlayCreated(locationOverlay)

            // Notify that map view is created; tile-provider setup happens in
            // the LaunchedEffect above once map files have been discovered
            onMapViewCreated(view)
            mapCreatedView = view
        },
        update = { view ->
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

            // Rebuild the heavy track overlays only when the track data
            // actually changed; a mere track point selection only refreshes
            // the marker below.
            val overlayKey = listOf(
                trackPoints,
                selectedTrackColor,
                compareTrackPoints,
                connectedTrackPoints,
                summitToCompare != null
            )
            var rebuiltOverlays = false
            if (lastOverlayKey != overlayKey) {
                lastOverlayKey = overlayKey
                rebuiltOverlays = true

                // Update map with track
                view.overlays.clear()
                mLocationOverlay?.let { view.overlays.add(it) }
                view.addDefaultSettings()

                if (summitToCompare != null) {
                    view.addAdditionalGpsTrack(
                        trackPoints = compareTrackPoints,
                        color = CompareTrackColor.toArgb()
                    )
                } else {
                    connectedTrackPoints.forEach {
                        view.addAdditionalGpsTrack(
                            trackPoints = it,
                            color = CompareTrackColor.toArgb()
                        )
                    }
                }

                view.addTrackAndMarker(
                    summit,
                    trackPoints,
                    true,
                    selectedTrackColor,
                    true,
                    addInfoWindow = false,
                    calculateBondingBox = calculateBoundingBox,
                    onTrackPointSelected = onTrackPointSelected
                )
            }

            // Handle selected track point marker
            if (rebuiltOverlays || selectedTrackPointIndex != lastMarkerIndex) {
                lastMarkerIndex = selectedTrackPointIndex
                selectedPointMarker?.let { view.overlays.remove(it) }
                selectedPointMarker = null
                selectedTrackPointIndex?.let { index ->
                    if (index in trackPoints.indices) {
                        val trackPoint = trackPoints[index].first
                        val geoPoint = GeoPoint(
                            trackPoint.latitude,
                            trackPoint.longitude,
                            trackPoint.elevation
                        )
                        val markerSizePx =
                            (20 * view.resources.displayMetrics.density).toInt()
                        val marker = Marker(view).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createBitmap(markerSizePx, markerSizePx).apply {
                                eraseColor(HighlightYellow.toArgb())
                            }.toDrawable(view.resources)
                            setOnMarkerClickListener { _, _ ->
                                true // Consume the click so the map doesn't treat it as a track-point tap
                            }
                        }
                        view.overlays.add(marker)
                        selectedPointMarker = marker
                    }
                }
            }

            view.enableRoadInfoOnMapClick()
            view.invalidate()
        },
        modifier = modifier
    )
}

@Composable
fun TrackMapControlButtons(
    summit: Summit,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    allSummits: List<Summit>?,
    mapView: CustomMapViewToAllowScrolling?,
    locationOverlay: MyLocationNewOverlay?,
    compareTrackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    summitToCompare: Summit?,
    onShowSnackbar: (String) -> Unit,
    onCustomizeTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val buttonColors = IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    )

    Column(
        modifier = modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show all tracks button
        IconButton(
            onClick = {
                if (mapView != null && allSummits != null) {
                    coroutineScope.launch {
                        // Reading the default shared preferences hits the disk
                        val sharedPreferences =
                            withContext(Dispatchers.IO) {
                                androidx.preference.PreferenceManager.getDefaultSharedPreferences(
                                    context
                                )
                            }
                        showAllTracksOfSummitInBoundingBox(
                            context = context,
                            mapView = mapView,
                            summit = summit,
                            trackPoints = trackPoints,
                            allSummits = allSummits,
                            compareTrackPoints = compareTrackPoints,
                            summitToCompare = summitToCompare,
                            locationOverlay = locationOverlay,
                            sharedPreferences = sharedPreferences,
                            onShowSnackbar = onShowSnackbar
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
                coroutineScope.launch {
                    // hasGpsTrack() and copying the file hit the disk
                    if (withContext(Dispatchers.IO) { summit.hasGpsTrack() }) {
                        shareGpsTrack(context, summit, onShowSnackbar)
                    }
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
                coroutineScope.launch {
                    // hasGpsTrack() and copying the file hit the disk
                    if (withContext(Dispatchers.IO) { summit.hasGpsTrack() }) {
                        openGpsTrack(context, summit, onShowSnackbar)
                    }
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
                Text(stringResource(R.string.cancel))
            }
        })
}

// Helper functions
private fun setGpsTrack(
    summit: Summit,
    useSimplifiedTrack: Boolean = false,
    forceUpdate: Boolean = false,
    onTrackLoaded: (GpsTrack?) -> Unit
) {
    if (summit.hasGpsTrack(useSimplifiedTrack)) {
        summit.setGpsTrack(useSimplifiedTrack)
        val gpsTrack = summit.gpsTrack

        if (gpsTrack?.hasNoTrackPoints() == true || forceUpdate) {
            gpsTrack?.parseTrack(useSimplifiedIfExists = useSimplifiedTrack)
        }

        onTrackLoaded(gpsTrack)
    } else {
        onTrackLoaded(null)
    }
}

/**
 * Loads a secondary track (compare/connected): prefers the simplified track
 * (fast), but falls back to the full one — manually imported activities have
 * no simplified file, and without the fallback their track would be missing.
 */
private fun setGpsTrackWithFullTrackFallback(
    summit: Summit,
    onTrackLoaded: (GpsTrack?) -> Unit
) {
    val hasSimplifiedTrack = summit.hasGpsTrack(simplified = true)
    if (hasSimplifiedTrack || summit.hasGpsTrack()) {
        setGpsTrack(
            summit,
            useSimplifiedTrack = hasSimplifiedTrack,
            forceUpdate = !hasSimplifiedTrack
        ) { track ->
            onTrackLoaded(track)
        }
    } else {
        onTrackLoaded(null)
    }
}

private suspend fun shareGpsTrack(
    context: android.content.Context,
    summit: Summit,
    onShowSnackbar: (String) -> Unit
) {
    try {
        // Copying the GPX file to the external cache dir hits the disk
        val uri = withContext(Dispatchers.IO) {
            summit.copyGpsTrackToTempFile(context.externalCacheDir)?.let {
                androidx.core.content.FileProvider.getUriForFile(
                    context, BuildConfig.APPLICATION_ID + ".provider", it
                )
            }
        }
        val intentShareFile = Intent(Intent.ACTION_SEND)
        intentShareFile.type = "application/gpx+xml"
        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
        intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
    } catch (e: IOException) {
        Log.e("SummitEntryTrackScreen", "Failed to share GPX track", e)
        onShowSnackbar(context.getString(R.string.gpx_file_not_copied))
    } catch (e: ActivityNotFoundException) {
        Log.e("SummitEntryTrackScreen", "No app to share GPX track", e)
        onShowSnackbar(context.getString(R.string.no_email_program_installed))
    }
}

private suspend fun openGpsTrack(
    context: android.content.Context,
    summit: Summit,
    onShowSnackbar: (String) -> Unit
) {
    try {
        // Copying the GPX file to the external cache dir hits the disk
        val uri = withContext(Dispatchers.IO) {
            summit.copyGpsTrackToTempFile(context.externalCacheDir)?.let {
                androidx.core.content.FileProvider.getUriForFile(
                    context, BuildConfig.APPLICATION_ID + ".provider", it
                )
            }
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/gpx")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        context.startActivity(intent)
    } catch (e: IOException) {
        Log.e("SummitEntryTrackScreen", "Failed to open GPX track", e)
        onShowSnackbar(context.getString(R.string.gpx_file_not_copied))
    } catch (e: ActivityNotFoundException) {
        Log.e("SummitEntryTrackScreen", "No GPX viewer installed", e)
        onShowSnackbar(context.getString(R.string.gpx_viewer_not_installed))
    }
}

private suspend fun showAllTracksOfSummitInBoundingBox(
    context: android.content.Context,
    mapView: CustomMapViewToAllowScrolling,
    summit: Summit,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    allSummits: List<Summit>,
    compareTrackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    summitToCompare: Summit?,
    locationOverlay: MyLocationNewOverlay?,
    sharedPreferences: android.content.SharedPreferences,
    onShowSnackbar: (String) -> Unit
) {
    val maxPointsToShow = sharedPreferences
        .getString(Keys.PREF_MAX_NUMBER_POINT, "10000")?.toInt() ?: 10000

    withContext(Dispatchers.Main) {
        mapView.overlays.clear()
        // Keep the location dot and default gesture/settings overlays alive;
        // the clear above removed them along with the previous tracks
        locationOverlay?.let { mapView.overlays.add(it) }
        mapView.addDefaultSettings()

        if (summitToCompare != null) {
            mapView.addAdditionalGpsTrack(
                trackPoints = compareTrackPoints,
                color = CompareTrackColor.toArgb()
            )
        }

        val summitsWithSameBoundingBox = allSummits.filter {
            it.activityId != summit.activityId && mapView.boundingBox?.let { boundingBox ->
                it.trackBoundingBox?.intersects(boundingBox)
            } == true
        }

        var pointsShown = 0
        var summitsShown = 0

        summitsWithSameBoundingBox.forEach { entry ->
            // hasGpsTrack() checks the track file on disk; keep that off the main
            // thread (StrictMode DiskReadViolation)
            val hasTrack = withContext(Dispatchers.IO) { entry.hasGpsTrack() }
            if (hasTrack && pointsShown < maxPointsToShow) {
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

        mapView.addTrackAndMarker(
            summit,
            trackPoints,
            true,
            TrackColor.Elevation,
            true,
            addInfoWindow = false,
            calculateBondingBox = false
        )

        if (pointsShown > maxPointsToShow) {
            onShowSnackbar(
                context.getString(
                    R.string.summits_shown,
                    summitsShown.toString(),
                    summitsWithSameBoundingBox.size.toString()
                )
            )
        }
    }
}
