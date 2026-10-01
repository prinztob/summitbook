package de.drtobiasprinz.summitbook.ui.compose

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.db.entities.RoadType
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.SavedLocation
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.db.entities.Surface
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling.Companion.TAG
import de.drtobiasprinz.summitbook.data.appstate.AppState.sharedPreferences
import de.drtobiasprinz.summitbook.ui.theme.MapVoidBackground
import de.drtobiasprinz.summitbook.ui.theme.SavedLocationPinColorPicker
import de.drtobiasprinz.summitbook.ui.view.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.data.maps.MapProvider
import de.drtobiasprinz.summitbook.data.maps.MapTilesHelper
import de.drtobiasprinz.summitbook.data.maps.OfflineMapAnalyzer
import de.drtobiasprinz.summitbook.data.model.PositionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.utils.BonusPackHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
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
import de.drtobiasprinz.summitbook.data.maps.FileHelper
import de.drtobiasprinz.summitbook.data.maps.OverlayTileProvider
import kotlinx.coroutines.CancellationException
import java.io.File
import java.util.Locale

/** Alpha for freshly attached overlay layers; all of them are faded in by
 *  the user via the sliders (the mbtiles slider range tops out at 0.4). */
private const val DEFAULT_OVERLAY_ALPHA = 0f

/** Number of layer rows the overlay panel keeps visible before it scrolls. */
private const val VISIBLE_LAYER_ROWS = 4

/** Height budget of one layer row in the overlay panel (label + slider). */
private const val LAYER_ROW_HEIGHT_DP = 72

/** A map overlay layer listed in the overlay panel, with its alpha slider
 *  range (mbtiles overlays top out at 0.4, heatmaps go up to 1). */
private data class MapLayer(
    val name: String,
    val overlay: TilesOverlay,
    val maxAlpha: Float = 0.4f,
    val isHeatmap: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenStreetMapScreen(
    filteredSummits: List<Summit>,
    bookmarks: List<Summit>,
    savedLocations: List<SavedLocation>,
    onSaveLocation: (name: String, lat: Double, lng: Double, color: Int) -> Unit,
    onRenameLocation: (location: SavedLocation, newName: String) -> Unit,
    onUpdateLocationColor: (location: SavedLocation, color: Int) -> Unit = { _, _ -> },
    onDeleteLocation: (location: SavedLocation) -> Unit,
    onFullscreenChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSummitDisabledMessage = stringResource(R.string.show_summit_disabled)
    val locationSavedMessage = stringResource(R.string.location_saved)
    val locationDeletedMessage = stringResource(R.string.location_deleted)
    val locationRenamedMessage = stringResource(R.string.location_renamed)

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackBarHostState.showSnackbar(message)
        }
    }

    // State variables
    var sharedPreferences by remember { mutableStateOf<SharedPreferences?>(null) }
    var maxPointsToShow by remember { mutableIntStateOf(10000) }
    var showSummits by rememberSaveable { mutableStateOf(false) }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var followLocationEnabled by rememberSaveable { mutableStateOf(false) }
    var fullscreenEnabled by rememberSaveable { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showMapTypeDialog by remember { mutableStateOf(false) }
    var showOverlaySliders by remember { mutableStateOf(false) }
    var overlayAlphas by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var hasOverlayLayers by remember { mutableStateOf(false) }
    // Whether the mbtiles layers were already attached for this map instance
    var baseLayersLoaded by remember { mutableStateOf(false) }

    // Saved locations (long-press to add, details on marker tap)
    var showSavedLocations by rememberSaveable { mutableStateOf(false) }
    var pendingLocation by rememberSaveable { mutableStateOf<Pair<Double, Double>?>(null) }
    var newLocationName by rememberSaveable { mutableStateOf("") }
    var pendingLocationColor by rememberSaveable { mutableIntStateOf(Color.BLACK) }
    var pendingMarker by remember { mutableStateOf<Marker?>(null) }
    var detailsLocation by remember { mutableStateOf<SavedLocation?>(null) }
    var locationDetails by remember { mutableStateOf<PositionInfo?>(null) }
    var renameMode by rememberSaveable { mutableStateOf(false) }
    var renameText by rememberSaveable { mutableStateOf("") }
    val savedLocationMarkers = remember { mutableStateListOf<Marker>() }

    // Lists
    val mGeoPoints = remember { mutableStateListOf<GeoPoint?>() }
    val mMarkers = remember { mutableStateListOf<Marker?>() }
    val mMarkersShown = remember { mutableStateListOf<Marker?>() }
    val layers = remember { mutableStateListOf<MapLayer>() }
    val mClusterer = remember { mutableStateListOf<RadiusMarkerClusterer>() }
    var boundingBoxRestored by remember { mutableStateOf(false) }

    // Overlays
    var mLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var mapView by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    var polyline by remember { mutableStateOf<Polyline?>(null) }
    var osMapBoundingBox by remember { mutableStateOf<List<String>>(emptyList()) }
    var mapProviderInitialized by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    fun removePendingLocationMarker() {
        pendingMarker?.let { mapView?.overlays?.remove(it) }
        pendingMarker = null
        mapView?.invalidate()
    }
    val locationPermissionDeniedMessage = stringResource(R.string.location_permission_denied)

    // Check and update location permission state
    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fineLocation || coarseLocation
    }

    // Permission launcher for location
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineLocationGranted || coarseLocationGranted
        
        if (hasLocationPermission) {
            Log.i("OpenStreetMapScreen", "Location permission granted")
            // Enable location on the overlay
            mLocationOverlay?.enableMyLocation()
        } else {
            Log.w("OpenStreetMapScreen", "Location permission denied")
            showSnackbar(locationPermissionDeniedMessage)
        }
    }

    // Function to request location permission
    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(Unit) {
        // Disk I/O (SharedPreferences, osmdroid config, map folder checks) must stay
        // off the main thread to avoid StrictMode DiskRead/WriteViolations
        withContext(Dispatchers.IO) {
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

            // Mark that map provider initialization is complete
            mapProviderInitialized = true

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

            // Check for overlay layers
            hasOverlayLayers = hasOverlayLayers(context)
        }
    }

    // Update map tile provider when map provider is initialized and map is ready
    LaunchedEffect(mapProviderInitialized, mapView) {
        if (mapProviderInitialized && mapView != null) {
            Log.i("OpenStreetMapScreen", "Updating tile provider after initialization")
            mapView?.setTileProvider()
        }
    }

    // Reset the base map style when leaving this screen so other map views
    // (e.g. the track detail screen) use the full render theme again.
    DisposableEffect(Unit) {
        onDispose {
            CustomMapViewToAllowScrolling.usePlainMapTheme = false
            mapView?.onMapLongPress = null
        }
    }

    // Handle system bar visibility when fullscreen state changes
    DisposableEffect(fullscreenEnabled) {
        val activity = context as? Activity
        val window = activity?.window
        if (fullscreenEnabled) {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView).apply {
                    // Transient bars revealed by an edge swipe overlay the
                    // content and auto-hide again; no legacy
                    // OnSystemUiVisibilityChangeListener needed.
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        } else {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onFullscreenChanged(fullscreenEnabled)
        onDispose {
            window?.let {
                WindowCompat.getInsetsController(it, it.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Show summits and bookmarks when they are enabled and map is ready
    LaunchedEffect(showSummits, showBookmarks, mapView, filteredSummits, bookmarks) {
        if (mapView != null) {
            showSummitsAndBookmarksIfEnabled(
                mapView,
                showSummits,
                showBookmarks,
                filteredSummits,
                bookmarks,
                mGeoPoints,
                mMarkers,
                mClusterer,
                mMarkersShown,
                context,
                coroutineScope,
                hasLocationPermission = hasLocationPermission,
                onRequestLocationPermission = { requestLocationPermission() },
                onShowSnackbar = { showSnackbar(it) },
                onLoadingChange = { isLoading = it }
            )
        }
    }

    // Show saved locations as plain (non-clustered) markers when enabled
    LaunchedEffect(savedLocations, showSavedLocations, mapView) {
        val map = mapView ?: return@LaunchedEffect
        savedLocationMarkers.forEach { map.overlays.remove(it) }
        savedLocationMarkers.clear()
        if (showSavedLocations) {
            savedLocations.forEach { location ->
                val marker = Marker(map).apply {
                    position = GeoPoint(location.lat, location.lng)
                    icon = getSavedLocationPinDrawable(map.context, location.color)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        detailsLocation = location
                        renameMode = false
                        true
                    }
                }
                map.overlays.add(marker)
                savedLocationMarkers.add(marker)
            }
            map.invalidate()
        }
    }

    // Live-preview the picked pin color on the pending long-press marker
    LaunchedEffect(pendingLocationColor) {
        pendingMarker?.let { marker ->
            marker.icon = getSavedLocationPinDrawable(context, pendingLocationColor)
            mapView?.invalidate()
        }
    }

    // Deduce all available infos for the selected or pending location from
    // the offline map (display only, not persisted)
    LaunchedEffect(detailsLocation, pendingLocation) {
        val target = detailsLocation?.let { GeoPoint(it.lat, it.lng) }
            ?: pendingLocation?.let { GeoPoint(it.first, it.second) }
        if (target == null) {
            locationDetails = null
            return@LaunchedEffect
        }
        locationDetails = null
        try {
            locationDetails = withContext(Dispatchers.IO) {
                OfflineMapAnalyzer.from(context).use { analyzer ->
                    analyzer.getPositionInfos(target)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get infos for location", e)
            locationDetails = PositionInfo()
        }
    }

    // Restore the persisted bounding box exactly once per map instance
    LaunchedEffect(mapView, osMapBoundingBox) {
        val map = mapView ?: return@LaunchedEffect
        if (!boundingBoxRestored && osMapBoundingBox.size == 6) {
            boundingBoxRestored = true
            try {
                val boundingBox = BoundingBox()
                boundingBox.set(
                    osMapBoundingBox[0].toDouble(),
                    osMapBoundingBox[1].toDouble(),
                    osMapBoundingBox[2].toDouble(),
                    osMapBoundingBox[3].toDouble()
                )
                map.post {
                    Log.d(
                        "OpenStreetMapScreen", "Attempting to zoom to bounding box: " +
                                "north=${boundingBox.latNorth}, east=${boundingBox.lonEast}, " +
                                "south=${boundingBox.latSouth}, west=${boundingBox.lonWest}"
                    )
                    map.zoomToBoundingBox(boundingBox, false, 30)
                }
            } catch (e: Exception) {
                Log.e(
                    "OpenStreetMapScreen",
                    "Getting bounding box from shared preference failed. ${e.message}"
                )
            }
        }
    }

    // Build the mbtiles overlay layers once per map instance, off the main
    // thread: listing the folder and opening the SQLite archives would
    // otherwise block composition (StrictMode DiskReadViolation) and stall
    // the first frames after the panel is opened.
    LaunchedEffect(showOverlaySliders, mapView) {
        val map = mapView ?: return@LaunchedEffect
        if (!showOverlaySliders || baseLayersLoaded) return@LaunchedEffect
        baseLayersLoaded = true
        isLoading = true
        try {
            // The overlays subfolder of the maps folder is reachable through
            // the storage access framework; getOverlayMbtilesFiles resolves
            // those to readable files, copying into the internal cache when
            // the folder has no real filesystem path (e.g. on emulators).
            // Merge with tiles-folder overlays and deduplicate by name -
            // when both resolve to the same physical file, the layer must
            // only be attached once.
            val layerFiles = withContext(Dispatchers.IO) {
                (FileHelper.getOverlayMbtilesFiles(context)
                    .map { it to it.nameWithoutExtension } +
                    getLayerFiles()).distinctBy { (_, name) -> name }
            }
            layerFiles.forEach { (file, name) ->
                if (!currentCoroutineContext().isActive) return@forEach
                val layer = try {
                    withContext(Dispatchers.IO) {
                        OverlayTileProvider.createTilesOverlay(context, file, map)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add overlay layer $name", e)
                    null
                } ?: return@forEach
                setAlphaForLayer(layer, DEFAULT_OVERLAY_ALPHA)
                map.overlays.add(layer)
                layers.add(MapLayer(name, layer))
                map.invalidate()
            }
            // Attach every heatmap with the other layers; each one is faded
            // in individually with its own alpha slider.
            withContext(Dispatchers.IO) { FileHelper.getHeatmapMbtilesFiles(context) }
                .forEach { heatmapFile ->
                    if (!currentCoroutineContext().isActive) return@forEach
                    val name = heatmapFile.nameWithoutExtension
                    val layer = try {
                        withContext(Dispatchers.IO) {
                            OverlayTileProvider.createTilesOverlay(context, heatmapFile, map)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add heatmap layer $name", e)
                        null
                    } ?: return@forEach
                    setAlphaForLayer(layer, DEFAULT_OVERLAY_ALPHA, isHeatmap = true)
                    map.overlays.add(layer)
                    layers.add(MapLayer(name, layer, maxAlpha = 1f, isHeatmap = true))
                    map.invalidate()
                }
        } finally {
            isLoading = false
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

    fun closeSaveLocationDialog() {
        pendingLocation = null
        newLocationName = ""
        pendingLocationColor = Color.BLACK
        removePendingLocationMarker()
    }

    fun closeDetailsDialog() {
        detailsLocation = null
        renameMode = false
    }

    // Dialog for naming and saving the location of the last long press
    if (pendingLocation != null) {
        AlertDialog(
            onDismissRequest = { closeSaveLocationDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.name_location),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { closeSaveLocationDialog() }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_close_24),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = newLocationName,
                        onValueChange = { newLocationName = it },
                        label = { Text(stringResource(R.string.location_name_hint)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.saved_location_pin_color),
                        style = MaterialTheme.typography.titleSmall
                    )
                    SavedLocationPinColorPicker(
                        selectedColor = pendingLocationColor,
                        onColorSelected = { pendingLocationColor = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    pendingLocation?.let { (lat, lng) ->
                        PositionInfoSection(
                            positionInfo = locationDetails,
                            lat = lat,
                            lng = lng
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingLocation?.let { (lat, lng) ->
                            if (newLocationName.isNotBlank()) {
                                onSaveLocation(newLocationName, lat, lng, pendingLocationColor)
                                showSavedLocations = true
                                showSnackbar(locationSavedMessage)
                            }
                        }
                        pendingLocation = null
                        newLocationName = ""
                        pendingLocationColor = Color.BLACK
                        removePendingLocationMarker()
                    },
                    enabled = newLocationName.isNotBlank()
                ) {
                    Text(stringResource(R.string.saveButtonText))
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        pendingLocation = null
                        newLocationName = ""
                        pendingLocationColor = Color.BLACK
                        removePendingLocationMarker()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Details dialog for a tapped saved location (rename/delete)
    detailsLocation?.let { location ->
        AlertDialog(
            onDismissRequest = { closeDetailsDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.saved_location_details),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { closeDetailsDialog() }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_close_24),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            },
            text = {
                if (renameMode) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.location_name_hint)) },
                        singleLine = true
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.saved_location_pin_color),
                            style = MaterialTheme.typography.titleSmall
                        )
                        SavedLocationPinColorPicker(
                            selectedColor = location.color,
                            onColorSelected = { onUpdateLocationColor(location, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PositionInfoSection(
                            positionInfo = locationDetails,
                            lat = location.lat,
                            lng = location.lng
                        )
                    }
                }
            },
            confirmButton = {
                if (renameMode) {
                    Button(
                        onClick = {
                            if (renameText.isNotBlank()) {
                                onRenameLocation(location, renameText)
                                showSnackbar(locationRenamedMessage)
                            }
                            detailsLocation = null
                            renameMode = false
                        },
                        enabled = renameText.isNotBlank()
                    ) {
                        Text(stringResource(R.string.saveButtonText))
                    }
                } else {
                    Button(
                        onClick = {
                            renameMode = true
                            renameText = location.name
                        }
                    ) {
                        Text(stringResource(R.string.rename_location))
                    }
                }
            },
            dismissButton = {
                if (renameMode) {
                    Button(
                        onClick = { renameMode = false }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                } else {
                    Button(
                        onClick = {
                            onDeleteLocation(location)
                            showSnackbar(locationDeletedMessage)
                            detailsLocation = null
                        }
                    ) {
                        Text(stringResource(R.string.delete_location))
                    }
                }
            }
        )
    }

    // contentWindowInsets is zeroed so the map draws behind the system bars;
    // the padding parameter is intentionally unused.
    @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                snackBarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
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
                    SummitBookMapView(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MapVoidBackground),
                        onMapCreated = { map ->
                            mapView = map
                            map.updateBoundingBox = true
                            map.setTileProvider()
                            
                            // Setup polyline for follow location
                            val newPolyline = Polyline(map).apply {
                                setInfoWindow(null) // Prevent default BasicInfoWindow crash
                                outlinePaint?.color = Color.MAGENTA
                                outlinePaint?.strokeWidth = 16f
                            }
                            polyline = newPolyline

                            // Setup location overlay
                            val locationOverlay =
                                object : MyLocationNewOverlay(GpsMyLocationProvider(context), map) {
                                    override fun onLocationChanged(
                                        location: android.location.Location?,
                                        source: IMyLocationProvider?
                                    ) {
                                        super.onLocationChanged(location, source)
                                        if (location != null && followLocationEnabled && location.speed > 0f) {
                                            newPolyline.addPoint(GeoPoint(location.latitude, location.longitude))
                                            map.overlayManager?.remove(newPolyline)
                                            map.overlayManager?.add(newPolyline)
                                            map.controller.setCenter(myLocation)
                                        }
                                    }
                                }
                            mLocationOverlay = locationOverlay
                            map.overlays.add(locationOverlay)
                            if (hasLocationPermission) {
                                locationOverlay.enableMyLocation()
                            }

                            // Enable road info on map click
                            map.enableRoadInfoOnMapClick()
                            // The road-and-surface info dialog on tap is disabled
                            // on this screen; deduced infos for a position are
                            // shown in the saved-location details instead.
                            map.roadInfoOnTapEnabled = false

                            // Long press on the map drops a pin and opens the
                            // save-location dialog for that position
                            map.onMapLongPress = { p ->
                                newLocationName = ""
                                pendingLocationColor = Color.BLACK
                                pendingLocation = p.latitude to p.longitude
                                removePendingLocationMarker()
                                val marker = Marker(map).apply {
                                    position = p
                                    icon = getSavedLocationPinDrawable(map.context, Color.BLACK)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                pendingMarker = marker
                                map.overlays.add(marker)
                                map.invalidate()
                            }
                        },
                        onShowMessage = { showSnackbar(it) },
                        update = { map ->
                            // Update map when state changes
                            map.updateBoundingBox = true

                            // Show my location
                            showMyLocation(
                                map,
                                map.overlays.find { it is MyLocationNewOverlay } as? MyLocationNewOverlay,
                                hasPermission = hasLocationPermission,
                                onRequestPermission = { requestLocationPermission() },
                                onShowSnackbar = { showSnackbar(it) }
                            )
                        }
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
                        coroutineScope,
                        (filteredSummits + bookmarks).associateBy { it.id.toString() },
                        onShowSnackbar = { showSnackbar(it) },
                        onLoadingChange = { isLoading = it }
                    )
                },
                onChangeMapType = {
                    showMapTypeDialog = true
                },
                onCenterOnLocation = {
                    showMyLocation(
                        mapView,
                        mLocationOverlay,
                        zoom = true,
                        hasPermission = hasLocationPermission,
                        onRequestPermission = { requestLocationPermission() }
                    )
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
                    } else {
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(showSummitDisabledMessage)
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
                showOverlaySliders = showOverlaySliders,
                hasOverlayLayers = hasOverlayLayers,
                onToggleOverlaySliders = {
                    showOverlaySliders = !showOverlaySliders
                },
                showSavedLocations = showSavedLocations,
                onShowSavedLocationsToggle = {
                    showSavedLocations = !showSavedLocations
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .safeDrawingPadding()
                    .padding(16.dp)
            )

            // Overlay map sliders; shown whenever toggled so the button always
            // has visible feedback, even while layers load or if none exist
            if (showOverlaySliders) {
                OverlaySliders(
                    layers = layers,
                    mapView = mapView,
                    overlayAlphas = overlayAlphas,
                    onAlphaChanged = { name, alpha ->
                        overlayAlphas = overlayAlphas.toMutableMap().apply { put(name, alpha) }
                    },
                    onAlphaSettled = { layer, _ ->
                        if (layer.isHeatmap) {
                            // Follow the heatmap alpha with the base-map theme:
                            // plain while any heatmap is visible, full render
                            // theme once all of them are faded out. Rebuilding
                            // the tile provider is expensive, so only do it
                            // when the theme actually flips.
                            val plain = layers.any {
                                it.isHeatmap &&
                                    (overlayAlphas[it.name] ?: DEFAULT_OVERLAY_ALPHA) > 0f
                            }
                            if (plain != CustomMapViewToAllowScrolling.usePlainMapTheme) {
                                CustomMapViewToAllowScrolling.usePlainMapTheme = plain
                                mapView?.setTileProvider()
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlaySliders(
    layers: List<MapLayer>,
    mapView: CustomMapViewToAllowScrolling?,
    overlayAlphas: Map<String, Float>,
    onAlphaChanged: (String, Float) -> Unit,
    onAlphaSettled: (MapLayer, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        if (layers.isEmpty()) {
            Text(
                text = stringResource(R.string.no_map_overlay_layers),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn(
                // Keep the panel at VISIBLE_LAYER_ROWS rows; longer lists scroll
                // inside the card instead of growing over the whole map.
                modifier = Modifier
                    .padding(2.dp)
                    .heightIn(max = (VISIBLE_LAYER_ROWS * LAYER_ROW_HEIGHT_DP).dp)
            ) {
                items(layers) { layer ->
                    val alpha =
                        (overlayAlphas[layer.name] ?: DEFAULT_OVERLAY_ALPHA).coerceAtMost(layer.maxAlpha)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = layer.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Slider(
                            value = alpha,
                            onValueChange = { newValue ->
                                onAlphaChanged(layer.name, newValue)
                                setAlphaForLayer(layer.overlay, newValue, layer.isHeatmap)
                                mapView?.invalidate()
                            },
                            onValueChangeFinished = {
                                onAlphaSettled(layer, alpha)
                            },
                            valueRange = 0f..layer.maxAlpha,
                            steps = if (layer.maxAlpha > 0.4f) 9 else 4
                        )
                    }
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
    var mapProviders by remember { mutableStateOf<List<MapProvider>>(emptyList()) }
    // MapProvider.exists() queries the maps folder via DocumentFile; keep that
    // storage access off the main thread (StrictMode DiskReadViolation)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            mapProviders = MapProvider.entries.filter { it.exists(context) }
        }
    }
    val currentProvider = CustomMapViewToAllowScrolling.selectedItem

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_map_type)) },
        text = {
            LazyColumn {
                items(mapProviders) { provider ->
                    val isSelected = provider == currentProvider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMapTypeSelected(provider)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onMapTypeSelected(provider)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(provider.textId),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Renders the coordinates and all infos deduced from the offline map for a
 * position. Shown in the save-location dialog and the saved-location
 * details dialog.
 */
@Composable
private fun PositionInfoSection(
    positionInfo: PositionInfo?,
    lat: Double,
    lng: Double
) {
    Text(
        text = stringResource(R.string.location_coordinates) + ": " +
                String.format(Locale.US, "%.5f, %.5f", lat, lng),
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    when (positionInfo) {
        null -> {
            Text(
                text = stringResource(R.string.querying_road_info),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        else -> {
            positionInfo.elevation?.let { elevation ->
                Text(
                    text = stringResource(
                        R.string.position_info_elevation,
                        elevation,
                        positionInfo.elevationDistance ?: 0
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            positionInfo.locationInfo?.let { locationInfo ->
                Text(
                    text = stringResource(R.string.road_info_location_heading),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${locationInfo.name} (${locationInfo.placeType}), " +
                            stringResource(
                                R.string.location_info_distance,
                                locationInfo.minDistance
                            ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (positionInfo.nearbyPois.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.position_info_nearby_pois),
                    style = MaterialTheme.typography.titleSmall
                )
                positionInfo.nearbyPois.forEach { poi ->
                    val poiDescription = buildString {
                        append(poi.name)
                        if (poi.type.isNotEmpty()) {
                            append(" (")
                            append(poi.type)
                            append(")")
                        }
                        poi.elevation?.let {
                            append(", ")
                            append(it)
                            append(" m")
                        }
                        append(", ")
                        append(
                            stringResource(
                                R.string.location_info_distance,
                                poi.minDistance
                            )
                        )
                    }
                    Text(
                        text = poiDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            positionInfo.roadInfo?.let { roadInfo ->
                Text(
                    text = stringResource(R.string.road_info_heading),
                    style = MaterialTheme.typography.titleSmall
                )
                roadInfo.name?.let { name ->
                    Text(
                        text = stringResource(R.string.road_info_name, name),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(
                        R.string.road_info_surface,
                        Surface.mapFromRoadInfo(roadInfo)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(
                        R.string.road_info_road_type,
                        RoadType.mapFromRoadInfo(roadInfo)
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                roadInfo.trackType?.takeIf { it.isNotEmpty() }?.let { trackType ->
                    Text(
                        text = stringResource(R.string.road_info_track_type, trackType),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (positionInfo.areas.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.position_info_areas),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = positionInfo.areas.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Helper functions

/**
 * Creates the push-pin marker icon tinted with the pin color of a saved
 * location (or the color picked for a pending one).
 */
private fun getSavedLocationPinDrawable(context: Context, color: Int) =
    ResourcesCompat.getDrawable(context.resources, R.drawable.baseline_push_pin_48, null)
        ?.mutate()?.apply { setTint(color) }

private fun getLayerFiles(): List<Pair<File, String>> {
    val fileEnding = "mbtiles"
    val overlayFolder = File(MapTilesHelper.getOsmdroidTilesFolder(), "overlays")
    return overlayFolder.listFiles()?.filter { it.name.endsWith(".${fileEnding}") }
        ?.mapNotNull { Pair(it, it.name.replace(".$fileEnding", "")) } ?: emptyList()
}

private fun hasOverlayLayers(context: Context): Boolean {
    return getLayerFiles().isNotEmpty() ||
        FileHelper.getOnDeviceOverlayMbtilesFiles(context).isNotEmpty() ||
        FileHelper.getHeatmapMbtilesFiles(context).isNotEmpty()
}

private fun setAlphaForLayer(layer: TilesOverlay, alpha: Float = 0f, isHeatmap: Boolean = false) {
    Log.i("OpenStreetMapScreen", "Set alpha $alpha")
    // Heatmap tiles carry their own transparency; scaling only the source
    // alpha preserves their soft gradient. mbtiles overlays are mostly
    // opaque (hillshades, imagery) - with a plain alpha scale they would
    // stay washed out at best 40% opacity, so the alpha channel is
    // premultiplied with the RGB channels and a constant there, which
    // renders them fully visible.
    layer.setColorFilter(
        android.graphics.ColorMatrixColorFilter(
            if (isHeatmap) {
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,  //red
                    0f, 1f, 0f, 0f, 0f,  //green
                    0f, 0f, 1f, 0f, 0f,  //blue
                    0f, 0f, 0f, alpha, 0f
                )
            } else {
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,  //red
                    0f, 1f, 0f, 0f, 0f,  //green
                    0f, 0f, 1f, 0f, 0f,  //blue
                    alpha, alpha, alpha, alpha, alpha
                )
            }
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

private fun removeSummitOverlays(
    map: CustomMapViewToAllowScrolling,
    mClusterer: SnapshotStateList<RadiusMarkerClusterer>,
    mMarkers: SnapshotStateList<Marker?>,
    mMarkersShown: SnapshotStateList<Marker?>,
    mGeoPoints: SnapshotStateList<GeoPoint?>,
    summitsById: Map<String, Summit>
) {
    // Remove only the overlays this screen owns (clusterer, events overlay,
    // info windows, tracks); base overlays added by addDefaultSettings(),
    // the heatmap and the mbtiles overlay layers must survive so their
    // tracked references stay valid.
    mClusterer.forEach { map.overlays.remove(it) }
    mClusterer.clear()
    map.overlays.removeAll { it is MapEventsOverlay }
    mMarkers.forEach { marker ->
        if (marker?.isInfoWindowShown == true) {
            marker.closeInfoWindow()
        }
        val gpsTrack = marker?.title?.let { summitsById[it] }?.gpsTrack
        if (gpsTrack?.isShownOnMap == true) {
            gpsTrack.osMapRoute?.let { map.overlayManager?.remove(it) }
            gpsTrack.isShownOnMap = false
        }
    }
    mMarkers.clear()
    mMarkersShown.clear()
    mGeoPoints.clear()
}

private fun showSummitsAndBookmarksIfEnabled(
    mapView: CustomMapViewToAllowScrolling?,
    showSummits: Boolean,
    showBookmarks: Boolean,
    summits: List<Summit>,
    bookmarks: List<Summit>,
    mGeoPoints: SnapshotStateList<GeoPoint?>,
    mMarkers: SnapshotStateList<Marker?>,
    mClusterer: SnapshotStateList<RadiusMarkerClusterer>,
    mMarkersShown: SnapshotStateList<Marker?>,
    context: Context,
    coroutineScope: CoroutineScope,
    hasLocationPermission: Boolean = false,
    onRequestLocationPermission: (() -> Unit)? = null,
    onShowSnackbar: ((String) -> Unit)? = null,
    onLoadingChange: ((Boolean) -> Unit)? = null
) {
    val summitsById = (summits + bookmarks).associateBy { it.id.toString() }
    mapView?.let { map ->
        removeSummitOverlays(map, mClusterer, mMarkers, mMarkersShown, mGeoPoints, summitsById)
    }
    if (showSummits || showBookmarks) {
        mapView?.enableRoadInfoOnMapClick(coroutineScope)
        onLoadingChange?.invoke(true)
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
            addAllMarkers(
                mapView,
                filteredSummits,
                context,
                mGeoPoints,
                mMarkers,
                mClusterer,
                coroutineScope,
                hasLocationPermission,
                onRequestLocationPermission,
                onShowSnackbar,
                onLoadingChange
            )
        }
    } else {
        mapView?.enableRoadInfoOnMapClick(coroutineScope)
        showMyLocation(
            mapView,
            mapView?.overlays?.find { it is MyLocationNewOverlay } as? MyLocationNewOverlay,
            hasPermission = hasLocationPermission,
            onRequestPermission = onRequestLocationPermission,
            onShowSnackbar = onShowSnackbar)
        mapView?.invalidate()
    }
}

private fun showMyLocation(
    mapView: CustomMapViewToAllowScrolling?,
    mLocationOverlay: MyLocationNewOverlay?,
    zoom: Boolean = false,
    hasPermission: Boolean = false,
    onRequestPermission: (() -> Unit)? = null,
    onShowSnackbar: ((String) -> Unit)? = null
) {
    if (mapView == null || mLocationOverlay == null) {
        Log.w("OpenStreetMapScreen", "showMyLocation: mapView or locationOverlay is null")
        return
    }

    // Check if we have location permission
    if (!hasPermission) {
        Log.w("OpenStreetMapScreen", "No location permission, requesting...")
        onRequestPermission?.invoke()
        return
    }

    mLocationOverlay.apply {
        // Enable location if not already enabled
        if (!isMyLocationEnabled) {
            try {
                enableMyLocation()
            } catch (e: SecurityException) {
                Log.e("OpenStreetMapScreen", "Failed to enable location: ${e.message}")
                onShowSnackbar?.invoke(mapView.context.getString(R.string.location_permission_denied))
                return
            }
        }

        // Set the person icon
        val arrow = ResourcesCompat.getDrawable(
            mapView.context.resources, R.drawable.baseline_my_location_24,
            null
        )?.toBitmap()
        setPersonIcon(arrow)
        setPersonAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        setDirectionIcon(arrow)
        setDirectionAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

        // Only add overlay if not already in the list
        if (!mapView.overlays.contains(this)) {
            mapView.overlays.add(this)
        }

        if (zoom) {
            // Try to get the current location
            val myLocationGeoPoint = myLocation
            if (myLocationGeoPoint != null && myLocationGeoPoint.latitude != 0.0 && myLocationGeoPoint.longitude != 0.0) {
                Log.d("OpenStreetMapScreen", "Centering on location: ${myLocationGeoPoint.latitude}, ${myLocationGeoPoint.longitude}")
                mapView.controller.setCenter(myLocationGeoPoint)
            } else {
                Log.w("OpenStreetMapScreen", "Location not yet available, waiting for fix...")
                // Location not available yet - we need to wait for the first fix
                // The overlay will automatically update when location becomes available
                onShowSnackbar?.invoke(mapView.context.getString(R.string.waiting_for_location))
            }
        }
        mapView.invalidate()
    }
}

private fun addAllMarkers(
    mapView: CustomMapViewToAllowScrolling?,
    summits: List<Pair<Summit, GeoPoint>>,
    context: Context,
    mGeoPoints: SnapshotStateList<GeoPoint?>,
    mMarkers: SnapshotStateList<Marker?>,
    mClusterer: SnapshotStateList<RadiusMarkerClusterer>,
    coroutineScope: CoroutineScope,
    hasLocationPermission: Boolean = false,
    onRequestLocationPermission: (() -> Unit)? = null,
    onShowSnackbar: ((String) -> Unit)? = null,
    onLoadingChange: ((Boolean) -> Unit)? = null
) {
    mapView?.let { map ->
        val mReceive = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                // Road info on tap is disabled on this screen; this overlay
                // exists for the long-press handling only.
                return false
            }

            override fun longPressHelper(arg0: GeoPoint): Boolean {
                mMarkers.forEach {
                    if (it?.isInfoWindowShown == true) {
                        it.infoWindow.close()
                    }
                }
                map.onMapLongPress?.invoke(arg0)
                return false
            }
        }
        val markers = RadiusMarkerClusterer(context)
        // Remove only the overlays this screen owns; base overlays (scale bar,
        // rotation gesture, copyright, heatmap, mbtiles layers, location) and
        // the tracked heatmap/layers references must stay valid.
        mClusterer.forEach { map.overlays.remove(it) }
        mClusterer.clear()
        map.overlays.removeAll { it is MapEventsOverlay }
        showMyLocation(
            map,
            map.overlays.find { it is MyLocationNewOverlay } as? MyLocationNewOverlay,
            hasPermission = hasLocationPermission,
            onRequestPermission = onRequestLocationPermission,
            onShowSnackbar = onShowSnackbar)
        coroutineScope.launch {
            val clusterIcon = withContext(Dispatchers.IO) {
                BonusPackHelper.getBitmapFromVectorDrawable(
                    context,
                    org.osmdroid.bonuspack.R.drawable.marker_cluster
                )
            }
            // Mutate the clusterer and the marker state lists on the main
            // thread only; onDraw may iterate them concurrently.
            markers.setIcon(clusterIcon)
            markers.setMaxClusteringZoomLevel(10)
            mGeoPoints.clear()
            mMarkers.clear()
            val newMarkers = mutableListOf<Marker>()
            withContext(Dispatchers.IO) {
                summits.forEach { pair ->
                    if (!currentCoroutineContext().isActive || !map.isAttachedToWindow) {
                        onLoadingChange?.invoke(false)
                        return@withContext
                    }
                    newMarkers.add(getMarker(map, pair.first, pair.second, context))
                }
            }
            newMarkers.forEach { marker ->
                mGeoPoints.add(marker.position)
                markers.add(marker)
                mMarkers.add(marker)
            }
            val eventsOverlay = MapEventsOverlay(mReceive)
            map.overlays.add(markers)
            map.overlays.add(eventsOverlay)
            mClusterer.add(markers)
            map.invalidate()
            onLoadingChange?.invoke(false)
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
    // Inflate the InfoWindow lazily on first tap: one inflated bubble layout
    // per marker would be far too heavy for large summit collections.
    marker.setOnMarkerClickListener { marker1, _ ->
        if (marker1.infoWindow == null) {
            marker1.infoWindow = MapCustomInfoBubble(
                localMapView as? CustomMapViewToAllowScrolling,
                entry,
                context,
                false
            )
        }
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
    coroutineScope: CoroutineScope,
    summitsById: Map<String, Summit>,
    onShowSnackbar: ((String) -> Unit)? = null,
    onLoadingChange: ((Boolean) -> Unit)? = null
) {
    val boundingBox = mapView?.boundingBox ?: return
    onLoadingChange?.invoke(true)
    coroutineScope.launch {
        try {
            // Summit.isInBoundingBox() checks the track file's existence; keep that
            // file system access off the main thread (StrictMode DiskReadViolation)
            val markersInBoundingBox: Set<Marker?> = withContext(Dispatchers.IO) {
                mMarkers.filterTo(HashSet()) { marker ->
                    marker?.title?.let { summitsById[it] }?.isInBoundingBox(boundingBox) == true
                }
            }
            var pointsShown = mMarkersShown.sumOf {
                (it?.title?.let { t -> summitsById[t] })?.gpsTrack?.trackPoints?.size ?: 0
            }
            val anyEntry = mMarkers.firstNotNullOfOrNull { marker ->
                marker?.title?.let { summitsById[it] }
            }
            if (anyEntry == null) {
                onLoadingChange?.invoke(false)
                return@launch
            }
            // Markers no longer carry an eagerly created InfoWindow; reuse a
            // single bubble as a handle for updateGpxTrack and swap its entry.
            val sharedBubble = MapCustomInfoBubble(mapView, anyEntry, context, false)
            val summitsInBoundingBox = mMarkers.filter {
                val entry = it?.title?.let { t -> summitsById[t] }
                val shouldBeShown = it in markersInBoundingBox
                if (!shouldBeShown && it in mMarkersShown && entry != null) {
                    sharedBubble.entry = entry
                    pointsShown -= entry.gpsTrack?.trackPoints?.size ?: 0
                    sharedBubble.updateGpxTrack(forceRemove = true)
                    mMarkersShown.remove(it)
                }
                shouldBeShown
            }
            var boxAlreadyShown = false
            summitsInBoundingBox.forEach {
                if (it != null) {
                    val entry = it.title?.let { t -> summitsById[t] }
                    if (entry != null && (it !in mMarkersShown || entry.gpsTrack?.isShownOnMap == false)) {
                        if (entry.hasGpsTrack()) {
                            launch {
                                var show = false
                                withContext(Dispatchers.Default) {
                                    if (pointsShown < maxPointsToShow) {
                                        show = true
                                        entry.setGpsTrack()
                                        pointsShown += entry.gpsTrack?.trackPoints?.size ?: 0
                                    }
                                }
                                if (show) {
                                    sharedBubble.entry = entry
                                    sharedBubble.updateGpxTrack(forceShow = true)
                                    Log.i(
                                        "trackPoints",
                                        "trackPoints ${pointsShown}++: ${entry.gpsTrack?.trackPoints?.size ?: 0}"
                                    )
                                    mMarkersShown.add(it)
                                } else if (!boxAlreadyShown) {
                                    onShowSnackbar?.invoke(
                                        String.format(
                                            context.resources.getString(
                                                R.string.summits_shown
                                            ),
                                            mMarkersShown.size.toString(),
                                            summitsInBoundingBox.size.toString()
                                        )
                                    )
                                    boxAlreadyShown = true
                                }
                                mapView.invalidate()
                            }
                        }
                    }
                }
            }
        } finally {
            onLoadingChange?.invoke(false)
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
    showOverlaySliders: Boolean,
    hasOverlayLayers: Boolean,
    onToggleOverlaySliders: () -> Unit,
    modifier: Modifier = Modifier,
    showSavedLocations: Boolean = false,
    onShowSavedLocationsToggle: () -> Unit = {}
) {
    var controlsExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Back button (only shown if callback is provided)
        onBackClick?.let {
            FloatingActionButton(
                onClick = it,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = stringResource(R.string.back)
                )
            }
        }

        // Fullscreen toggle is always visible
        FloatingActionButton(
            onClick = onFullscreenToggle,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (fullscreenEnabled) R.drawable.baseline_fullscreen_exit_24
                    else R.drawable.baseline_fullscreen_24
                ),
                contentDescription = stringResource(R.string.cd_fullscreen)
            )
        }

        // Remaining controls are only visible when the menu is expanded
        AnimatedVisibility(visible = controlsExpanded) {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onShowAllTracks,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_baseline_route_24),
                        contentDescription = stringResource(R.string.cd_show_all_tracks)
                    )
                }

                FloatingActionButton(
                    onClick = onChangeMapType,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_more_vert_black_24dp),
                        contentDescription = stringResource(R.string.cd_change_map)
                    )
                }

                FloatingActionButton(
                    onClick = onCenterOnLocation,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_my_location_24),
                        contentDescription = stringResource(R.string.cd_center_on_location)
                    )
                }

                FloatingActionButton(
                    onClick = onCenterOnSummits,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_center_focus_strong_24),
                        contentDescription = stringResource(R.string.cd_center_on_summits)
                    )
                }

                FloatingActionButton(
                    onClick = onShowBookmarksToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp),
                    containerColor = if (showBookmarks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_bookmarks_24),
                        contentDescription = stringResource(R.string.cd_show_bookmarks)
                    )
                }

                FloatingActionButton(
                    onClick = onShowSavedLocationsToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp),
                    containerColor = if (showSavedLocations) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_push_pin_24),
                        contentDescription = stringResource(R.string.cd_show_saved_locations)
                    )
                }

                FloatingActionButton(
                    onClick = onShowSummitsToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp),
                    containerColor = if (showSummits) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painterResource(R.drawable.baseline_directions_run_24),
                        contentDescription = stringResource(R.string.cd_show_summits)
                    )
                }

                FloatingActionButton(
                    onClick = onFollowLocationToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp),
                    containerColor = if (followLocationEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painter = if (followLocationEnabled) painterResource(id = R.drawable.baseline_stop_circle_24) else painterResource(
                            id = R.drawable.baseline_play_circle_filled_24
                        ),
                        contentDescription = stringResource(R.string.cd_follow_location)
                    )
                }

                // Toggle overlay sliders button (only shown if overlay layers exist)
                if (hasOverlayLayers) {
                    FloatingActionButton(
                        onClick = onToggleOverlaySliders,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 8.dp),
                        containerColor = if (showOverlaySliders) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_map_black_24dp),
                            contentDescription = stringResource(R.string.cd_toggle_overlay_sliders)
                        )
                    }
                }
            }
        }

        // Expand/collapse the map controls
        FloatingActionButton(
            onClick = { controlsExpanded = !controlsExpanded },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (controlsExpanded) R.drawable.baseline_keyboard_double_arrow_down_black_24dp
                    else R.drawable.baseline_keyboard_double_arrow_up_black_24dp
                ),
                contentDescription = stringResource(R.string.cd_toggle_map_controls)
            )
        }
    }
}
