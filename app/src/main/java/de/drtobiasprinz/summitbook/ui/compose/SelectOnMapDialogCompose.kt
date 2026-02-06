package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.graphics.drawable.Drawable
import android.location.Address
import android.os.StrictMode
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.copyGpxFileToCache
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.prepareGpxTrack
import io.ticofab.androidgpxparser.parser.GPXParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Jetpack Compose version of SelectOnOsMapActivity
 * Replaces the Activity-based implementation with a modern Compose Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectOnMapDialogCompose(
    summit: Summit,
    onDismiss: () -> Unit,
    onSaveSummit: (Boolean, Summit) -> Job,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State management
    var summitEntry by remember { mutableStateOf(summit.clone()) }
    var latLngSelectedPosition by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedGpsPath by remember { mutableStateOf<Path?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchPanelVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf(false) }
    var wasBoundingBoxCalculated by remember { mutableStateOf(false) }

    // Map view reference
    var mapView by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }

    // File picker launcher for GPX tracks
    val gpxFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch() {
                val file = File(MainActivityCompose.cache, "new_gpx_track.gpx")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    copyGpxFileToCache(inputStream, file)
                }
                addGpxTrack(file, GPXParser(), mapView, summitEntry) { position, _ ->
                    latLngSelectedPosition = position
                    selectedGpsPath = file.toPath()
                    wasBoundingBoxCalculated = true
                }
            }
        }
    }

    // String resources
    val deleteCoordinates = stringResource(R.string.delete_coordinates)
    val deleteCoordinatesMessage = stringResource(R.string.delete_coordinates_message)
    val deleteGps = stringResource(R.string.delete_gps)
    val deleteCancel = stringResource(R.string.delete_cancel)
    val addPositionToSummitSuccessful = stringResource(R.string.add_position_to_summit_successful)
    val addPositionToSummitCancel = stringResource(R.string.add_position_to_summit_cancel)
    val foundAddress = stringResource(R.string.found_address)
    val notFound = stringResource(R.string.not_found)
    val myPosition = stringResource(R.string.my_position)
    val search = stringResource(R.string.search)
    val back = stringResource(R.string.back)
    val saveButtonText = stringResource(R.string.saveButtonText)
    val cancelButtonText = stringResource(R.string.cancelButtonText)
    val addGpsTrack = stringResource(R.string.addGpsTrack)

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteCoordinates) },
            text = { Text(deleteCoordinatesMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedGpsPath = null
                        if (summitEntry.hasGpsTrack()) {
                            val gpsTrackPath = summitEntry.getGpsTrackPath()
                            gpsTrackPath.toFile()?.delete()
                            summitEntry.hasTrack = false
                        }
                        summitEntry.latLng = GeoPoint(0.0, 0.0)
                        onSaveSummit(true, summitEntry)
                        onDismiss()
                        Toast.makeText(
                            context,
                            deleteGps.format(summitEntry.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        Toast.makeText(context, deleteCancel, Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            icon = {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_alert),
                    contentDescription = null
                )
            }
        )
    }

    // Main dialog
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Map view
                AndroidView(
                    factory = { ctx ->
                        CustomMapViewToAllowScrolling(ctx).apply {
                            mapView = this
                            setTileSource(TileSourceFactory.MAPNIK)
                            Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
                            addDefaultSettings()
                        }
                    },
                    update = { map ->
                        map.setTileProviderDependingOnSummitSportType(
                            summitEntry.sportType
                        )

                        // Map tap handler - add before track and marker
                        val mapEventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                latLngSelectedPosition = GeoPoint(p.latitude, p.longitude)
                                map.addMarker(p, summitEntry)
                                map.zoomController.activate()
                                return false
                            }

                            override fun longPressHelper(p: GeoPoint): Boolean {
                                return false
                            }
                        }
                        // Remove existing MapEventsOverlay if present, then add new one
                        map.overlays.removeIf { it is MapEventsOverlay }
                        map.overlays.add(MapEventsOverlay(mapEventsReceiver))

                        map.addTrackAndMarker(
                            summitEntry,
                            false,
                            TrackColor.None,
                            alwaysShowTrackOnMap = false
                        )
                        summitEntry.trackBoundingBox?.let { boundingBox ->
                            map.drawBoundingBox(boundingBox)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Loading panel
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .align(Alignment.BottomCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // Search panel
                if (searchPanelVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(myPosition) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            searchForAddress(
                                context = context,
                                searchQuery = searchQuery,
                                mapView = mapView,
                                foundAddress = foundAddress,
                                notFound = notFound
                            )
                        }) {
                            Text(search)
                        }
                    }
                }

                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = if (searchPanelVisible) 70.dp else 8.dp,
                            bottom = 8.dp
                        )
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    if (!searchPanelVisible) {
                        IconButton(
                            onClick = {
                                onDismiss()
                                Toast.makeText(
                                    context,
                                    addPositionToSummitCancel.format(summitEntry.name),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_clear_24),
                                contentDescription = back
                            )
                        }
                    }

                    // Add GPS track button
                    if (!searchPanelVisible) {
                        IconButton(
                            onClick = {
                                gpxFilePickerLauncher.launch("*/*")
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_add_black_24dp),
                                contentDescription = addGpsTrack
                            )
                        }
                    }

                    // Save button
                    if (!searchPanelVisible) {
                        IconButton(
                            onClick = {
                                val position = latLngSelectedPosition
                                if (position != null) {
                                    summitEntry.lat = position.latitude
                                    summitEntry.lng = position.longitude
                                    summitEntry.latLng = latLngSelectedPosition
                                    onSaveSummit(true, summitEntry)
                                    onDismiss()
                                    Toast.makeText(
                                        context,
                                        addPositionToSummitSuccessful.format(summitEntry.name),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                val localSelectedPath = selectedGpsPath
                                if (localSelectedPath != null) {
                                    try {
                                        Files.copy(
                                            localSelectedPath,
                                            summitEntry.getGpsTrackPath(),
                                            StandardCopyOption.REPLACE_EXISTING
                                        )
                                        summitEntry.hasTrack = true
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                                summitEntry.setBoundingBoxFromTrack()
                                onSaveSummit(true, summitEntry)
                            },
                            enabled = latLngSelectedPosition != null,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (latLngSelectedPosition != null) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                    } else {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    },
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (latLngSelectedPosition != null) {
                                        R.drawable.baseline_save_black_24dp
                                    } else {
                                        R.drawable.baseline_save_grey_500_24dp
                                    }
                                ),
                                contentDescription = saveButtonText
                            )
                        }
                    }

                    // Delete button
                    if (!searchPanelVisible) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_delete_black_24dp),
                                contentDescription = cancelButtonText
                            )
                        }
                    }

                    // Refresh road info button
                    if (!searchPanelVisible) {
                        IconButton(
                            onClick = {
                                showFileInfoDialog = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_refresh_24),
                                contentDescription = null
                            )
                        }
                    }

                    // Search expander
                    IconButton(
                        onClick = { searchPanelVisible = !searchPanelVisible },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(
                                if (searchPanelVisible) {
                                    R.drawable.baseline_keyboard_double_arrow_up_black_24dp
                                } else {
                                    R.drawable.baseline_keyboard_double_arrow_down_black_24dp
                                }
                            ),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }

    // FileInfoDialog
    if (showFileInfoDialog) {
        FileInfoDialogCompose(
            entry = summitEntry,
            viewModel = null,
            onDismiss = { showFileInfoDialog = false },
            onLoadingStateChanged = { loading -> isLoading = loading }
        )
    }
}

/**
 * Search for an address on the map
 */
private fun searchForAddress(
    context: Context,
    searchQuery: String,
    mapView: CustomMapViewToAllowScrolling?,
    foundAddress: String,
    notFound: String
) {
    if (searchQuery.isBlank() || mapView == null) return

    val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
    StrictMode.setThreadPolicy(policy)

    val geoCoder = GeocoderNominatim(BuildConfig.APPLICATION_ID)
    val viewBox: BoundingBox = mapView.boundingBox
    val foundAddresses: List<Address> = geoCoder.getFromLocationName(
        searchQuery,
        1,
        viewBox.latSouth,
        viewBox.lonEast,
        viewBox.latNorth,
        viewBox.lonWest,
        false
    )

    if (foundAddresses.isNotEmpty()) {
        Toast.makeText(
            context,
            foundAddress.format(searchQuery),
            Toast.LENGTH_SHORT
        ).show()
        val address = foundAddresses[0]
        val geoPoint = GeoPoint(address.latitude, address.longitude)
        val poiIcon: Drawable? = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.ic_filled_location_black_48,
            null
        )
        mapView.setExpectedCenter(geoPoint)
        val poiMarker = Marker(mapView)
        poiMarker.title = address.getAddressLine(0)
        poiMarker.snippet = address.getAddressLine(1)
        poiMarker.position = geoPoint
        poiMarker.relatedObject = address
        poiMarker.icon = poiIcon
        poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        poiMarker.setInfoWindow(CustomInfoWindow(mapView))
        mapView.overlays.add(poiMarker)
        mapView.setExpectedCenter(geoPoint)
    } else {
        Toast.makeText(
            context,
            notFound.format(searchQuery),
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Add a GPX track to the map
 */
private fun addGpxTrack(
    file: File,
    mParser: GPXParser,
    osMap: CustomMapViewToAllowScrolling?,
    summitEntry: Summit?,
    onTrackAdded: (GeoPoint, GpsTrack) -> Unit
) {
    if (osMap == null) return

    val inputStream: InputStream = FileInputStream(file)
    mParser.parse(inputStream)
    val selectedGpsPath = file.toPath()
    val gpsTrack = prepareGpxTrack(selectedGpsPath, summitEntry)
    val highestTrackPoint = gpsTrack?.getHighestElevation()
    if (highestTrackPoint != null) {
        addSelectedPositionAndTrack(highestTrackPoint, gpsTrack, osMap, summitEntry)
        onTrackAdded(highestTrackPoint, gpsTrack)
    }
}

/**
 * Add selected position and track to the map
 */
private fun addSelectedPositionAndTrack(
    geoPointSelectedPosition: GeoPoint,
    gpsTrack: GpsTrack,
    osMap: CustomMapViewToAllowScrolling,
    summitEntry: Summit?
) {
    if (summitEntry != null) {
        osMap.addMarker(geoPointSelectedPosition, summitEntry)
        gpsTrack.addGpsTrack(osMap, TrackColor.None)
        osMap.calculateBoundingBox(gpsTrack, geoPointSelectedPosition)
    }
}

/**
 * Custom info window for map markers
 */
private class CustomInfoWindow(mapView: CustomMapViewToAllowScrolling?) :
    MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView) {
    private var mSelectedPoi: Address? = null
    override fun onOpen(item: Any) {
        super.onOpen(item)
        val button =
            mView.findViewById<android.widget.Button>(org.osmdroid.bonuspack.R.id.bubble_moreinfo)
        button.visibility = android.view.View.VISIBLE
        val marker: Marker = item as Marker
        mSelectedPoi = marker.relatedObject as Address
    }
}
