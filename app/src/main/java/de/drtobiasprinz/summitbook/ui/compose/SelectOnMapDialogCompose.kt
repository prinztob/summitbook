package de.drtobiasprinz.summitbook.ui.compose

import android.content.Intent
import android.location.Address
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.data.analytics.GpsUtils.Companion.copyGpxFileToCache
import de.drtobiasprinz.summitbook.data.analytics.GpsUtils.Companion.prepareGpxTrack
import de.drtobiasprinz.summitbook.data.appstate.AppState
import de.drtobiasprinz.summitbook.data.appstate.AppState.sharedPreferences
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.ExtensionFromYaml
import de.drtobiasprinz.summitbook.data.model.GpsTrack
import de.drtobiasprinz.summitbook.data.model.TrackColor
import de.drtobiasprinz.summitbook.sync.GarminPythonExecutor
import de.drtobiasprinz.summitbook.sync.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.activities.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val TAG = "SelectOnMapDialog"

/**
 * Jetpack Compose version of SelectOnOsMapActivity
 * Replaces the Activity-based implementation with a modern Compose Dialog
 */
@Composable
fun SelectOnMapDialogCompose(
    summit: Summit,
    onDismiss: () -> Unit,
    onSaveSummit: (Boolean, Summit) -> Job,
    onAddSegmentEntry: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var summitEntry by rememberSaveable(stateSaver = jsonSaver<Summit>()) { mutableStateOf(summit.clone()) }
    var selectedLat by rememberSaveable { mutableDoubleStateOf(Double.NaN) }
    var selectedLng by rememberSaveable { mutableDoubleStateOf(Double.NaN) }
    val selectedPosition: GeoPoint? =
        if (selectedLat.isNaN() || selectedLng.isNaN()) null else GeoPoint(selectedLat, selectedLng)
    var selectedGpsFile by rememberSaveable(stateSaver = fileSaver) { mutableStateOf<File?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchPanelVisible by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    var searchMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var importedMarker by remember { mutableStateOf<Marker?>(null) }
    var lastDrawnTrackPoints by remember {
        mutableStateOf<List<Pair<TrackPoint, ExtensionFromYaml>>?>(null)
    }

    val deleteCoordinatesText = stringResource(R.string.delete_coordinates)
    val deleteCoordinatesMessage = stringResource(R.string.delete_coordinates_message)
    val deleteGps = stringResource(R.string.delete_gps)
    val deleteCancel = stringResource(R.string.delete_cancel)
    val addPositionToSummitSuccessful = stringResource(R.string.add_position_to_summit_successful)
    val addPositionToSummitCancel = stringResource(R.string.add_position_to_summit_cancel)
    val foundAddress = stringResource(R.string.found_address)
    val notFound = stringResource(R.string.not_found)
    val searchText = stringResource(R.string.search)
    val searchHint = stringResource(R.string.search_address_hint)
    val closeText = stringResource(R.string.close)
    val closeSearchText = stringResource(R.string.cd_close_search)
    val fileInfoTitle = stringResource(R.string.file_info_title)
    val saveButtonText = stringResource(R.string.saveButtonText)
    val cancelButtonText = stringResource(R.string.cancelButtonText)
    val addGpsTrackText = stringResource(R.string.addGpsTrack)
    val addMountainPassText = stringResource(R.string.add_mountain_pass)
    val downloadFromGarminText = stringResource(R.string.download_from_garmin)
    val downloadSuccessText = stringResource(R.string.download_success)
    val downloadFailedText = stringResource(R.string.garmin_download_failed)
    val retryText = stringResource(R.string.retry)
    val errorLoadingGpxText = stringResource(R.string.error_loading_gpx)

    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        onActionPerformed: () -> Unit = {}
    ) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) onActionPerformed()
        }
    }

    fun performSearch() {
        val query = searchQuery
        val map = mapView
        if (query.isBlank() || map == null) return
        scope.launch {
            isLoading = true
            val address = searchForAddress(query, map)
            isLoading = false
            if (address == null) {
                showSnackbar(notFound.format(query))
            } else {
                showSnackbar(foundAddress.format(query))
                searchMarkers.forEach { map.overlays.remove(it) }
                val poiMarker = Marker(map).apply {
                    position = GeoPoint(address.latitude, address.longitude)
                    title = address.getAddressLine(0)
                    snippet = address.getAddressLine(1)
                    relatedObject = address
                    icon = ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.ic_filled_location_black_48,
                        null
                    )
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setInfoWindow(CustomInfoWindow(map, summitEntry))
                }
                map.overlays.add(poiMarker)
                searchMarkers = listOf(poiMarker)
                map.setExpectedCenter(GeoPoint(address.latitude, address.longitude))
            }
        }
    }

    fun downloadFromGarmin(activityIdsSize: Int) {
        isLoading = true
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val downloader = GarminTrackAndDataDownloader(
                        listOf(summitEntry),
                        GarminPythonExecutor.instance,
                        sharedPreferences.getBoolean(Keys.PREF_DOWNLOAD_TCX, false)
                    )
                    downloader.downloadTracks(forceDownload = true)
                    downloader.composeFinalTrack(summitEntry)
                    true
                } catch (ex: Exception) {
                    Log.e(TAG, "Garmin download failed: ${ex.message}")
                    false
                }
            }
            isLoading = false
            if (success) {
                showSnackbar(String.format(downloadSuccessText, activityIdsSize, summitEntry.name))
            } else {
                showSnackbar(downloadFailedText, retryText) { downloadFromGarmin(activityIdsSize) }
            }
        }
    }

    val gpxFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    val file = File(AppState.cache, "new_gpx_track.gpx")
                    val gpsTrack = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            copyGpxFileToCache(inputStream, file)
                        }
                        loadGpxTrack(file, summitEntry)
                    }
                    val highestTrackPoint = gpsTrack?.getHighestElevation()
                    if (gpsTrack != null && highestTrackPoint != null) {
                        mapView?.let { map ->
                            importedMarker?.let { map.overlays.remove(it) }
                            importedMarker = addSelectedPositionAndTrack(
                                highestTrackPoint,
                                gpsTrack,
                                map,
                                summitEntry
                            )
                        }
                        selectedLat = highestTrackPoint.latitude
                        selectedLng = highestTrackPoint.longitude
                        selectedGpsFile = file
                    } else {
                        showSnackbar(errorLoadingGpxText)
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Could not import GPX track: ${ex.message}")
                    showSnackbar(errorLoadingGpxText)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun saveSelection() {
        val position = selectedPosition
        if (position == null) return
        summitEntry.lat = position.latitude
        summitEntry.lng = position.longitude
        summitEntry.latLng = position
        scope.launch {
            selectedGpsFile?.let { file ->
                try {
                    withContext(Dispatchers.IO) {
                        Files.copy(
                            file.toPath(),
                            summitEntry.getGpsTrackPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                    summitEntry.hasTrack = true
                } catch (ex: IOException) {
                    Log.e(TAG, "Could not copy selected GPX track: ${ex.message}")
                }
            }
            summitEntry.setBoundingBoxFromTrack()
            onSaveSummit(true, summitEntry)
            showSnackbar(addPositionToSummitSuccessful.format(summitEntry.name))
            onDismiss()
        }
    }

    fun deleteCoordinates() {
        selectedLat = Double.NaN
        selectedLng = Double.NaN
        selectedGpsFile = null
        scope.launch {
            if (summitEntry.hasGpsTrack()) {
                withContext(Dispatchers.IO) {
                    summitEntry.getGpsTrackPath().toFile().delete()
                    summitEntry.getGpsTrackPath(simplified = true).toFile().delete()
                }
                summitEntry.hasTrack = false
            }
            summitEntry.latLng = GeoPoint(0.0, 0.0)
            onSaveSummit(true, summitEntry)
            showSnackbar(deleteGps.format(summitEntry.name))
            onDismiss()
            showDeleteDialog = false
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteCoordinatesText) },
            text = { Text(deleteCoordinatesMessage) },
            confirmButton = {
                TextButton(onClick = { deleteCoordinates() }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        showSnackbar(deleteCancel)
                    }
                ) {
                    Text(cancelButtonText)
                }
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_delete_black_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }

    // Main dialog
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Map view
                SummitBookMapView(
                    modifier = Modifier.fillMaxSize(),
                    onMapCreated = { map ->
                        mapView = map
                        map.setTileSource(TileSourceFactory.MAPNIK)
                        map.setTileProviderDependingOnSummitSportType(
                            summitEntry.sportType
                        )
                        if (summitEntry.hasGpsTrack() && summitEntry.gpsTrack == null) {
                            summitEntry.setGpsTrack()
                        }
                        val gpsTrack = summitEntry.gpsTrack
                        if (gpsTrack?.hasNoTrackPoints() == true) {
                            gpsTrack.parseTrack()
                        }
                        val trackPoints = gpsTrack?.trackPoints ?: emptyList()
                        lastDrawnTrackPoints = trackPoints
                        map.addTrackAndMarker(
                            summitEntry,
                            trackPoints,
                            false,
                            TrackColor.None,
                            alwaysShowTrackOnMap = false,
                            calculateBondingBox = true
                        )
                        summitEntry.trackBoundingBox?.let { boundingBox ->
                            map.drawBoundingBox(boundingBox)
                        }
                    },
                    update = { map ->
                        if (summitEntry.hasTrack && summitEntry.gpsTrack == null) {
                            summitEntry.setGpsTrack()
                        }
                        val gpsTrack = summitEntry.gpsTrack
                        if (gpsTrack?.hasNoTrackPoints() == true) {
                            gpsTrack.parseTrack()
                        }
                        val trackPoints = gpsTrack?.trackPoints ?: emptyList()
                        if (trackPoints !== lastDrawnTrackPoints) {
                            lastDrawnTrackPoints = trackPoints
                            map.addTrackAndMarker(
                                summitEntry,
                                trackPoints,
                                false,
                                TrackColor.None,
                                alwaysShowTrackOnMap = false,
                                calculateBondingBox = false
                            )
                            summitEntry.trackBoundingBox?.let { boundingBox ->
                                map.drawBoundingBox(boundingBox)
                            }
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search panel
                    if (searchPanelVisible) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(searchHint) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { performSearch() })
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { performSearch() }) {
                                    Text(searchText)
                                }
                            }
                        }
                    }

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!searchPanelVisible) {
                            MapFab(
                                iconRes = R.drawable.ic_baseline_clear_24,
                                contentDescription = closeText,
                                onClick = {
                                    showSnackbar(addPositionToSummitCancel.format(summitEntry.name))
                                    onDismiss()
                                }
                            )
                            MapFab(
                                iconRes = R.drawable.baseline_add_black_24dp,
                                contentDescription = addGpsTrackText,
                                onClick = { gpxFilePickerLauncher.launch("*/*") }
                            )
                            MapFab(
                                iconRes = R.drawable.baseline_save_black_24dp,
                                contentDescription = saveButtonText,
                                onClick = { saveSelection() },
                                enabled = selectedPosition != null,
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            MapFab(
                                iconRes = R.drawable.baseline_delete_black_24dp,
                                contentDescription = deleteCoordinatesText,
                                onClick = { showDeleteDialog = true },
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                            MapFab(
                                iconRes = R.drawable.baseline_refresh_24,
                                contentDescription = fileInfoTitle,
                                onClick = { showFileInfoDialog = true }
                            )
                            if (onAddSegmentEntry != null) {
                                MapFab(
                                    iconRes = R.drawable.baseline_add_mountain_pass_24,
                                    contentDescription = addMountainPassText,
                                    onClick = onAddSegmentEntry
                                )
                            }
                            val garminData = summitEntry.garminData
                            if (garminData != null && garminData.activityIds.isNotEmpty()) {
                                MapFab(
                                    iconRes = R.drawable.baseline_download_black_24dp,
                                    contentDescription = downloadFromGarminText,
                                    onClick = { downloadFromGarmin(garminData.activityIds.size) },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        MapFab(
                            iconRes = if (searchPanelVisible) {
                                R.drawable.baseline_keyboard_double_arrow_up_black_24dp
                            } else {
                                R.drawable.baseline_keyboard_double_arrow_down_black_24dp
                            },
                            contentDescription = if (searchPanelVisible) closeSearchText else searchText,
                            onClick = { searchPanelVisible = !searchPanelVisible }
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                )

                if (isLoading) {
                    LoadingPanel(visible = true)
                }
            }
        }
    }

    // FileInfoDialog
    if (showFileInfoDialog) {
        FileInfoDialogCompose(
            entry = summitEntry,
            onUpdateSummit = onSaveSummit,
            onDismiss = { showFileInfoDialog = false },
            onLoadingStateChanged = { loading -> isLoading = loading },
            onShowSnackbar = { message -> showSnackbar(message) }
        )
    }
}

@Composable
private fun MapFab(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .background(
                color = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = CircleShape
            )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    }
}

/**
 * Search for an address on the map; runs the geocoding request off the main thread.
 * Returns null if nothing was found or the request failed.
 */
private suspend fun searchForAddress(
    searchQuery: String,
    mapView: CustomMapViewToAllowScrolling
): Address? {
    val viewBox: BoundingBox = mapView.boundingBox
    return withContext(Dispatchers.IO) {
        try {
            GeocoderNominatim(BuildConfig.APPLICATION_ID).getFromLocationName(
                searchQuery,
                1,
                viewBox.latSouth,
                viewBox.lonWest,
                viewBox.latNorth,
                viewBox.lonEast,
                false
            ).firstOrNull()
        } catch (ex: Exception) {
            Log.e(TAG, "Address search failed: ${ex.message}")
            null
        }
    }
}

/**
 * Parse and prepare a GPX file for display; returns null if the file cannot be parsed.
 */
private fun loadGpxTrack(file: File, summitEntry: Summit?): GpsTrack? = try {
    FileInputStream(file).use { inputStream ->
        GPXParser().parse(inputStream)
    }
    prepareGpxTrack(file.toPath(), summitEntry)
} catch (ex: Exception) {
    Log.e(TAG, "Could not parse GPX file ${file.absolutePath}: ${ex.message}")
    null
}

/**
 * Add selected position and track to the map
 */
private fun addSelectedPositionAndTrack(
    geoPointSelectedPosition: GeoPoint,
    gpsTrack: GpsTrack,
    osMap: CustomMapViewToAllowScrolling,
    summitEntry: Summit?
): Marker? {
    return if (summitEntry != null) {
        val marker = osMap.addMarker(geoPointSelectedPosition, summitEntry)
        gpsTrack.addGpsTrack(osMap, TrackColor.None)
        osMap.calculateBoundingBox(gpsTrack.trackPoints, geoPointSelectedPosition)
        marker
    } else {
        null
    }
}

/**
 * Custom info window for map markers
 */
private class CustomInfoWindow(
    private val mapView: CustomMapViewToAllowScrolling?,
    private val summitEntry: Summit
) : MarkerInfoWindow(R.layout.bonuspack_bubble, mapView) {
    private var mSelectedPoi: Address? = null
    override fun onOpen(item: Any) {
        super.onOpen(item)
        val button = mView?.findViewById<android.widget.Button>(R.id.bubble_moreinfo)
        button?.visibility = android.view.View.VISIBLE
        button?.setOnClickListener {
            val context = mapView?.context
            if (context != null) {
                try {
                    val intent = Intent(context, SummitEntryDetailsComposeActivity::class.java)
                    intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry.id)
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // DO NOTHING
                }
            }
        }
        val marker: Marker = item as Marker
        mSelectedPoi = marker.relatedObject as Address
    }
}
