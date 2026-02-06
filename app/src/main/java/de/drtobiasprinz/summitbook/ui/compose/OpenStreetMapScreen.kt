package de.drtobiasprinz.summitbook.ui.compose

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling.Companion.TAG
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.sharedPreferences
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.ui.MapProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.utils.BonusPackHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

@Suppress("AssignedValueIsNeverRead")
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenStreetMapScreen(
    filteredSummits: List<Summit>,
    bookmarks: List<Summit>,
    onFullscreenChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSummitDisabledMessage = stringResource(R.string.show_summit_disabled)

    // State variables
    var sharedPreferences by remember { mutableStateOf<SharedPreferences?>(null) }
    var maxPointsToShow by remember { mutableIntStateOf(10000) }
    var showSummits by rememberSaveable { mutableStateOf(false) }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var followLocationEnabled by rememberSaveable { mutableStateOf(false) }
    var fullscreenEnabled by rememberSaveable { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showMapTypeDialog by remember { mutableStateOf(false) }

    // Lists
    val mGeoPoints = remember { mutableStateListOf<GeoPoint?>() }
    val mMarkers = remember { mutableStateListOf<Marker?>() }
    val mMarkersShown = remember { mutableStateListOf<Marker?>() }
    val layers = remember { mutableStateListOf<Pair<String, TilesOverlay>>() }

    // Overlays
    var mLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var mapView by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    var polyline by remember { mutableStateOf<Polyline?>(null) }
    var osMapBoundingBox by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        sharedPreferences =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        maxPointsToShow =
            (sharedPreferences?.getString(Keys.PREF_MAX_NUMBER_POINT, maxPointsToShow.toString())
                ?: maxPointsToShow.toString()).toInt()
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        CustomMapViewToAllowScrolling.setOsmConfForTiles()

        // Set default map type to offline map if available
        val availableProviders = MapProvider.entries.filter { it.exists(context) }
        val offlineProvider = availableProviders.firstOrNull { it.isOffline }
        if (offlineProvider != null && CustomMapViewToAllowScrolling.selectedItem == MapProvider.OPENTOPO) {
            CustomMapViewToAllowScrolling.selectedItem = offlineProvider
        }

        // Load saved bounding box
        osMapBoundingBox =
            sharedPreferences?.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")?.split(";")
                ?: emptyList()
        if (osMapBoundingBox.size == 6) {
            try {
                if (osMapBoundingBox[4].toInt() == 1) {
                    showSummits = true
                }
                if (osMapBoundingBox[5].toInt() == 1) {
                    showBookmarks = true
                }
                Log.i(
                    "OpenStreetMapScreen",
                    "Loaded bounding box: ${
                        sharedPreferences?.getString(
                            Keys.PREF_OS_MAP_BOUNDING_BOX,
                            ""
                        )
                    }"
                )
            } catch (e: Exception) {
                Log.e(
                    "OpenStreetMapScreen",
                    "Getting bounding box from shared preference failed. ${e.message}"
                )
            }
        }
    }

    // Handle system bar visibility when fullscreen state changes
    LaunchedEffect(fullscreenEnabled) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            if (fullscreenEnabled) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                window.insetsController?.show(WindowInsets.Type.systemBars())
            }
        }
        onFullscreenChanged(fullscreenEnabled)
    }

    // Show summits and bookmarks when they are enabled and map is ready
    LaunchedEffect(showSummits, showBookmarks, mapView, filteredSummits, bookmarks) {
        if (mapView != null && (showSummits || showBookmarks)) {
            showSummitsAndBookmarksIfEnabled(
                mapView,
                showSummits,
                showBookmarks,
                filteredSummits,
                bookmarks,
                mGeoPoints,
                mMarkers,
                context,
                coroutineScope
            )
        }
    }

    // Handle map type selection
    if (showMapTypeDialog) {
        MapTypeSelectionDialog(
            onDismiss = { showMapTypeDialog = false },
            onMapTypeSelected = { mapProvider ->
                CustomMapViewToAllowScrolling.selectedItem = mapProvider
                mapView?.setTileProvider()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Column(modifier = Modifier.fillMaxSize()) {

                // Loading indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }

                // Map view
                Box(modifier = Modifier.weight(1f)) {
                    MapViewComposable(
                        context = context,
                        followLocationEnabled = followLocationEnabled,
                        onMapReady = { map ->
                            mapView = map
                        },
                        onPolylineCreated = { newPolyline ->
                            polyline = newPolyline
                        },
                        onLocationOverlayCreated = { overlay ->
                            mLocationOverlay = overlay
                        },
                        layers = layers,
                        osMapBoundingBox = osMapBoundingBox
                    )
                }
            }

            // Map control buttons
            MapControlButtons(
                onBackClick = null,
                fullscreenEnabled = fullscreenEnabled,
                onFullscreenToggle = {
                    fullscreenEnabled = !fullscreenEnabled
                },
                onShowAllTracks = {
                    showAllTracksOfSummitInBoundingBox(
                        mapView,
                        mMarkers,
                        mMarkersShown,
                        maxPointsToShow,
                        context,
                        coroutineScope
                    )
                },
                onChangeMapType = {
                    showMapTypeDialog = true
                },
                onCenterOnLocation = {
                    showMyLocation(mapView, mLocationOverlay, true)
                },
                onCenterOnSummits = {
                    mapView?.post {
                        mapView?.calculateBoundingBox(mGeoPoints.toList())
                    }
                },
                showBookmarks = showBookmarks,
                onShowBookmarksToggle = {
                    if (!followLocationEnabled) {
                        showBookmarks = !showBookmarks
                        updateSelectedParameters(
                            showSummits,
                            showBookmarks
                        )
                        showSummitsAndBookmarksIfEnabled(
                            mapView,
                            showSummits,
                            showBookmarks,
                            filteredSummits,
                            bookmarks,
                            mGeoPoints,
                            mMarkers,
                            context,
                            coroutineScope
                        )
                    }
                },
                showSummits = showSummits,
                onShowSummitsToggle = {
                    if (!followLocationEnabled) {
                        showSummits = !showSummits
                        updateSelectedParameters(
                            showSummits,
                            showBookmarks
                        )
                        showSummitsAndBookmarksIfEnabled(
                            mapView,
                            showSummits,
                            showBookmarks,
                            filteredSummits,
                            bookmarks,
                            mGeoPoints,
                            mMarkers,
                            context,
                            coroutineScope
                        )
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(showSummitDisabledMessage)
                        }
                    }
                },
                followLocationEnabled = followLocationEnabled,
                onFollowLocationToggle = {
                    followLocationEnabled = !followLocationEnabled
                    if (!followLocationEnabled) {
                        polyline?.setPoints(mutableListOf())
                        mapView?.overlayManager?.remove(polyline)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            )

            // Overlay map sliders
            if (layers.isNotEmpty()) {
                OverlaySliders(
                    layers = layers,
                    mapView = mapView,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun MapViewComposable(
    context: Context,
    followLocationEnabled: Boolean,
    onMapReady: (CustomMapViewToAllowScrolling) -> Unit,
    onPolylineCreated: (Polyline) -> Unit,
    onLocationOverlayCreated: (MyLocationNewOverlay) -> Unit,
    layers: SnapshotStateList<Pair<String, TilesOverlay>>,
    osMapBoundingBox: List<String>
) {
    AndroidView(
        factory = { ctx ->
            val mapView = CustomMapViewToAllowScrolling(ctx).apply {
                updateBoundingBox = true
                setTileProvider()
                addDefaultSettings()

                // Setup polyline for follow location
                val polyline = Polyline(this).apply {
                    outlinePaint?.color = Color.MAGENTA
                    outlinePaint?.strokeWidth = 16f
                }
                onPolylineCreated(polyline)

                // Setup location overlay
                val locationOverlay =
                    object : MyLocationNewOverlay(GpsMyLocationProvider(ctx), this) {
                        override fun onLocationChanged(
                            location: android.location.Location?,
                            source: IMyLocationProvider?
                        ) {
                            super.onLocationChanged(location, source)
                            if (location != null && followLocationEnabled && location.speed > 0f) {
                                polyline.addPoint(GeoPoint(location.latitude, location.longitude))
                                this@apply.overlayManager?.remove(polyline)
                                this@apply.overlayManager?.add(polyline)
                                this@apply.controller.setCenter(myLocation)
                            }
                        }
                    }
                onLocationOverlayCreated(locationOverlay)
                overlays.add(locationOverlay)
                locationOverlay.enableMyLocation()

                // Enable road info on map click
                enableRoadInfoOnMapClick()
            }

            onMapReady(mapView)
            mapView
        },
        update = { mapView ->
            // Update map when state changes
            mapView.updateBoundingBox = true

            Log.e(TAG, "Updated $osMapBoundingBox")
            if (osMapBoundingBox.size == 6) {
                try {
                    val boundingBox = BoundingBox()
                    boundingBox.set(
                        osMapBoundingBox[0].toDouble(),
                        osMapBoundingBox[1].toDouble(),
                        osMapBoundingBox[2].toDouble(),
                        osMapBoundingBox[3].toDouble()
                    )
                    mapView.post {
                        Log.d(
                            "OpenStreetMapScreen", "Attempting to zoom to bounding box: " +
                                    "north=${boundingBox.latNorth}, east=${boundingBox.lonEast}, " +
                                    "south=${boundingBox.latSouth}, west=${boundingBox.lonWest}"
                        )
                        mapView.zoomToBoundingBox(boundingBox, false, 30)
                        Log.d("OpenStreetMapScreen", "Zoom operation completed")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "MapViewComposable",
                        "Getting bounding box from shared preference failed. ${e.message}"
                    )
                }
            }
            // Show overlay maps if exist
            showOverlayIfExist(mapView, context, layers)

            // Show my location
            showMyLocation(
                mapView,
                mapView.overlays.find { it is MyLocationNewOverlay } as? MyLocationNewOverlay)

            mapView.onResume()
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun OverlaySliders(
    layers: List<Pair<String, TilesOverlay>>,
    mapView: CustomMapViewToAllowScrolling?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .verticalScroll(rememberScrollState()),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp)
        ) {
            items(layers) { layer ->
                var alpha by remember { mutableFloatStateOf(0f) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = layer.first,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Slider(
                        value = alpha,
                        onValueChange = { newValue ->
                            alpha = newValue
                            setAlphaForLayer(layer.second, newValue)
                            mapView?.invalidate()
                        },
                        valueRange = 0f..0.4f,
                        steps = 4
                    )
                }
            }
        }
    }
}

@Composable
fun MapTypeSelectionDialog(
    onDismiss: () -> Unit,
    onMapTypeSelected: (MapProvider) -> Unit
) {
    val context = LocalContext.current
    val mapProviders = MapProvider.entries.filter { it.exists(context) }
    val currentProvider = CustomMapViewToAllowScrolling.selectedItem

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_map_type)) },
        text = {
            LazyColumn {
                items(mapProviders) { provider ->
                    val isSelected = provider == currentProvider
                    Text(
                        text = stringResource(provider.textId) + if (isSelected) " ✓" else "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMapTypeSelected(provider)
                                onDismiss()
                            }
                            .padding(16.dp),
                        style = if (isSelected) {
                            MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        } else {
                            MaterialTheme.typography.bodyLarge
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun showOverlayIfExist(
    mapView: CustomMapViewToAllowScrolling,
    context: Context,
    layers: SnapshotStateList<Pair<String, TilesOverlay>>
) {
    val fileEnding = "mbtiles"
    val overlayFolder = File(CustomMapViewToAllowScrolling.getOsmdroidTilesFolder(), "overlays")
    val files = overlayFolder.listFiles()?.filter { it.name.endsWith(".${fileEnding}") }
    if (ArchiveFileFactory.isFileExtensionRegistered(fileEnding) && files?.isNotEmpty() == true) {
        try {
            files.forEach {
                val tileProvider = OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(it))
                val layer = TilesOverlay(tileProvider, context)
                layer.loadingBackgroundColor = Color.TRANSPARENT
                layer.loadingLineColor = Color.TRANSPARENT
                mapView.overlays.add(layer)
                layers.add(Pair(it.name.replace(".$fileEnding", ""), layer))
                setAlphaForLayer(layer)
                mapView.invalidate()
            }
        } catch (ex: Exception) {
            Log.e("OpenStreetMapScreen", Log.getStackTraceString(ex))
        }
    }
}

private fun setAlphaForLayer(layer: TilesOverlay, alpha: Float = 0f) {
    Log.i("OpenStreetMapScreen", "Set alpha $alpha")
    layer.setColorFilter(
        android.graphics.ColorMatrixColorFilter(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,  //red
                0f, 1f, 0f, 0f, 0f,  //green
                0f, 0f, 1f, 0f, 0f,  //blue
                alpha, alpha, alpha, alpha, alpha
            )
        )
    )
}

private fun updateSelectedParameters(
    showSummits: Boolean,
    showBookmarks: Boolean
) {
    val osMapBoundingBox =
        sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")?.split(";")
            ?: emptyList()
    if (osMapBoundingBox.size == 6) {
        sharedPreferences.edit {
            putString(
                Keys.PREF_OS_MAP_BOUNDING_BOX,
                "${osMapBoundingBox[0]};${osMapBoundingBox[1]};${osMapBoundingBox[2]};${osMapBoundingBox[3]};${if (showSummits) 1 else 0};${if (showBookmarks) 1 else 0}"
            )
        }
    }
    Log.d(TAG, "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}")
}

private fun showSummitsAndBookmarksIfEnabled(
    mapView: CustomMapViewToAllowScrolling?,
    showSummits: Boolean,
    showBookmarks: Boolean,
    summits: List<Summit>,
    bookmarks: List<Summit>,
    mGeoPoints: SnapshotStateList<GeoPoint?>,
    mMarkers: SnapshotStateList<Marker?>,
    context: Context,
    coroutineScope: CoroutineScope
) {
    if (showSummits || showBookmarks) {
        // In a real implementation, we would set isLoading = true here
        mapView?.enableRoadInfoOnMapClick(coroutineScope)
        coroutineScope.launch {
            var filteredSummits: List<Pair<Summit, GeoPoint>> = listOf()
            withContext(Dispatchers.IO) {
                val relevantSummits = if (showSummits) summits else emptyList()
                val relevantBookmarks = if (showBookmarks) bookmarks else emptyList()

                filteredSummits =
                    (relevantSummits + relevantBookmarks).filter {
                        it.sportType != SportType.IndoorTrainer
                                && it.lat != null
                                && it.lat != 0.0
                                && it.lng != null
                                && it.lng != 0.0
                    }.map { Pair(it, GeoPoint(it.lat!!, it.lng!!)) }
            }
            addAllMarkers(mapView, filteredSummits, context, mGeoPoints, mMarkers, coroutineScope)
            // In a real implementation, we would set isLoading = false here
        }
    } else {
        mapView?.overlays?.clear()
        showMyLocation(
            mapView,
            mapView?.overlays?.find { it is MyLocationNewOverlay } as? MyLocationNewOverlay)
        mapView?.invalidate()
    }
}

private fun showMyLocation(
    mapView: CustomMapViewToAllowScrolling?,
    mLocationOverlay: MyLocationNewOverlay?,
    zoom: Boolean = false
) {
    mLocationOverlay?.let { overlay ->
        if (overlay.isMyLocationEnabled) {
            val arrow = ResourcesCompat.getDrawable(
                mapView!!.context.resources, R.drawable.baseline_my_location_24,
                null
            )?.toBitmap()
            overlay.setPersonIcon(arrow)
            overlay.setPersonAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            overlay.setDirectionIcon(arrow)
            overlay.setDirectionAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mapView.overlays?.add(overlay)
            if (zoom) {
                mapView.controller?.setCenter(overlay.myLocation)
            }
        }
    }
}

private fun addAllMarkers(
    mapView: CustomMapViewToAllowScrolling?,
    summits: List<Pair<Summit, GeoPoint>>,
    context: Context,
    mGeoPoints: SnapshotStateList<GeoPoint?>,
    mMarkers: SnapshotStateList<Marker?>,
    coroutineScope: CoroutineScope
) {
    mapView?.let { map ->
        var mReceive: MapEventsReceiver
        val markers = RadiusMarkerClusterer(context)
        map.overlays?.clear()
        showMyLocation(
            map,
            map.overlays?.find { it is MyLocationNewOverlay } as? MyLocationNewOverlay)
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val clusterIcon = BonusPackHelper.getBitmapFromVectorDrawable(
                    context,
                    org.osmdroid.bonuspack.R.drawable.marker_cluster
                )
                markers.setIcon(clusterIcon)
                markers.setMaxClusteringZoomLevel(10)
                mGeoPoints.clear()
                mMarkers.clear()
                summits.forEach { pair ->
                    mGeoPoints.add(pair.second)
                    val marker = getMarker(map, pair.first, pair.second, context)
                    markers.add(marker)
                    mMarkers.add(marker)
                }
                mReceive = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        return false
                    }

                    override fun longPressHelper(arg0: GeoPoint): Boolean {
                        mMarkers.forEach {
                            if (it?.isInfoWindowShown == true) {
                                it.infoWindow.close()
                            }
                        }
                        return false
                    }
                }
            }
            val eventsOverlay = MapEventsOverlay(mReceive)
            map.overlays?.add(markers)
            map.overlays?.add(eventsOverlay)
            map.invalidate()
        }
    }
}

private fun getMarker(
    localMapView: MapView?,
    entry: Summit,
    point: GeoPoint,
    context: Context
): Marker {
    val marker = Marker(localMapView)
    marker.title = entry.id.toString()
    marker.position = point
    if (entry.hasGpsTrack()) {
        marker.icon = ResourcesCompat.getDrawable(
            context.resources,
            entry.sportType.markerIdWithGpx,
            null
        )
    } else {
        marker.icon = ResourcesCompat.getDrawable(
            context.resources,
            entry.sportType.markerIdWithoutGpx,
            null
        )
    }
    marker.infoWindow =
        MapCustomInfoBubble(localMapView as? CustomMapViewToAllowScrolling, entry, context, false)
    marker.setOnMarkerClickListener { marker1, _ ->
        if (!marker1.isInfoWindowShown) {
            marker1.showInfoWindow()
        } else {
            marker1.closeInfoWindow()
        }
        false
    }
    return marker
}

private fun showAllTracksOfSummitInBoundingBox(
    mapView: CustomMapViewToAllowScrolling?,
    mMarkers: SnapshotStateList<Marker?>,
    mMarkersShown: SnapshotStateList<Marker?>,
    maxPointsToShow: Int,
    context: Context,
    coroutineScope: CoroutineScope
) {
    var pointsShown = mMarkersShown.sumOf {
        (it?.infoWindow as MapCustomInfoBubble).entry.gpsTrack?.trackPoints?.size ?: 0
    }
    val summitsInBoundingBox = mMarkers.filter {
        val mapCustomInfoBubble: MapCustomInfoBubble = it?.infoWindow as MapCustomInfoBubble
        val shouldBeShown = mapView?.boundingBox?.let { it1 ->
            mapCustomInfoBubble.entry.isInBoundingBox(it1)
        }
        if (shouldBeShown == false && it in mMarkersShown) {
            Log.i(
                "trackPoints",
                "trackPoints --: ${mapCustomInfoBubble.entry.gpsTrack?.trackPoints?.size ?: 0}"
            )
            pointsShown -= mapCustomInfoBubble.entry.gpsTrack?.trackPoints?.size ?: 0
            mapCustomInfoBubble.updateGpxTrack(forceRemove = true)
            mMarkersShown.remove(it)
        }
        shouldBeShown == true
    }
    var boxAlreadyShown = false
    summitsInBoundingBox.forEach {
        if (it != null) {
            val infoWindow: MapCustomInfoBubble = it.infoWindow as MapCustomInfoBubble
            if (it !in mMarkersShown || infoWindow.entry.gpsTrack?.isShownOnMap == false) {
                if (infoWindow.entry.hasGpsTrack()) {
                    coroutineScope.launch {
                        var show = false
                        withContext(Dispatchers.Default) {
                            if (pointsShown < maxPointsToShow) {
                                show = true
                                infoWindow.entry.setGpsTrack()
                                pointsShown += infoWindow.entry.gpsTrack?.trackPoints?.size ?: 0
                            }
                        }
                        if (show) {
                            infoWindow.updateGpxTrack(forceShow = true)
                            Log.e(
                                "trackPoints",
                                "trackPoints ${pointsShown}++: ${infoWindow.entry.gpsTrack?.trackPoints?.size ?: 0}"
                            )
                            mMarkersShown.add(it)
                        } else if (!boxAlreadyShown) {
                            Toast.makeText(
                                context,
                                String.format(
                                    context.resources.getString(
                                        R.string.summits_shown
                                    ),
                                    mMarkersShown.size.toString(),
                                    summitsInBoundingBox.size.toString()
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                            boxAlreadyShown = true
                        }
                        mapView?.invalidate()
                    }
                }
            }
        }
    }
}

@Composable
fun MapControlButtons(
    onBackClick: (() -> Unit)?,
    fullscreenEnabled: Boolean,
    onFullscreenToggle: () -> Unit,
    onShowAllTracks: () -> Unit,
    onChangeMapType: () -> Unit,
    onCenterOnLocation: () -> Unit,
    onCenterOnSummits: () -> Unit,
    showBookmarks: Boolean,
    onShowBookmarksToggle: () -> Unit,
    showSummits: Boolean,
    onShowSummitsToggle: () -> Unit,
    followLocationEnabled: Boolean,
    onFollowLocationToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Back button (only shown if callback is provided)
        onBackClick?.let {
            FloatingActionButton(
                onClick = it,
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = "Back"
                )
            }
        }

        FloatingActionButton(
            onClick = onFullscreenToggle,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (fullscreenEnabled) R.drawable.baseline_fullscreen_exit_24
                    else R.drawable.baseline_fullscreen_24
                ),
                contentDescription = "Fullscreen"
            )
        }

        FloatingActionButton(
            onClick = onShowAllTracks,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_baseline_route_24),
                contentDescription = "Show all tracks"
            )
        }

        FloatingActionButton(
            onClick = onChangeMapType,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_black_24dp),
                contentDescription = "Change map"
            )
        }

        FloatingActionButton(
            onClick = onCenterOnLocation,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_my_location_24),
                contentDescription = "Center on location"
            )
        }

        FloatingActionButton(
            onClick = onCenterOnSummits,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_center_focus_strong_24),
                contentDescription = "Center on summits"
            )
        }

        FloatingActionButton(
            onClick = onShowBookmarksToggle,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp),
            containerColor = if (showBookmarks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_bookmarks_24),
                contentDescription = "Show bookmarks"
            )
        }

        FloatingActionButton(
            onClick = onShowSummitsToggle,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp),
            containerColor = if (showSummits) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                painterResource(R.drawable.baseline_directions_run_24),
                contentDescription = "Show summits"
            )
        }

        FloatingActionButton(
            onClick = onFollowLocationToggle,
            modifier = Modifier
                .size(40.dp),
            containerColor = if (followLocationEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                painter = if (followLocationEnabled) painterResource(id = R.drawable.baseline_stop_circle_24) else painterResource(
                    id = R.drawable.baseline_play_circle_filled_24
                ),
                contentDescription = "Follow location"
            )
        }
    }
}