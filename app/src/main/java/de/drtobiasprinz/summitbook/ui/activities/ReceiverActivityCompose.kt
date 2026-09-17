package de.drtobiasprinz.summitbook.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.data.model.GpsTrack
import de.drtobiasprinz.summitbook.data.model.TrackColor
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.compose.AddSummitDialogCompose
import de.drtobiasprinz.summitbook.ui.compose.SummitBookMapView
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.data.analytics.GpsUtils.Companion.copyGpxFileToCache
import de.drtobiasprinz.summitbook.data.analytics.GpsUtils.Companion.prepareGpxTrack
import de.drtobiasprinz.summitbook.core.utils.Utils
import de.drtobiasprinz.summitbook.ui.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.data.appstate.AppState

@AndroidEntryPoint
class ReceiverActivityCompose : ComponentActivity() {
    private var gpxTrackUri: Uri? = null
    private val viewModel: DatabaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        Log.i("ReceiverActivityCompose", "onCreate")

        setContent {
            SummitBookTheme {
                ReceiverScreen(
                    onBackClicked = { finish() }
                )
            }
        }
    }

    private enum class ImportState { Loading, Ready, Failed }

    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReceiverScreen(
        onBackClicked: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        var showDialog by rememberSaveable { mutableStateOf(false) }
        var isBookmark by rememberSaveable { mutableStateOf(false) }
        var gpsTrack by remember { mutableStateOf<GpsTrack?>(null) }
        var mapViewReference by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
        var gpxTrackUriState by remember { mutableStateOf<Uri?>(null) }
        var importState by remember { mutableStateOf(ImportState.Loading) }

        LaunchedEffect(Unit) {
            // Initialize cache and storage directories off the main thread
            // (getCacheDir()/getFilesDir() hit the file system)
            withContext(Dispatchers.IO) {
                AppState.cache = applicationContext.cacheDir
                AppState.storage = applicationContext.filesDir
                AppState.activitiesDir =
                    File(AppState.storage, "activities")
            }

            // Process intent when activity starts
            when (intent.action) {
                Intent.ACTION_VIEW, Intent.ACTION_SEND -> {
                    importState = ImportState.Loading
                    scope.launch {
                        processIntent(intent) { track ->
                            if (track != null) {
                                importState = ImportState.Ready
                                gpsTrack = track
                                gpxTrackUriState = gpxTrackUri
                                mapViewReference?.let { map ->
                                    drawGpxTrackOnMap(track, map)
                                }
                            } else {
                                importState = ImportState.Failed
                            }
                        }
                    }
                }
                else -> {
                    Log.i("ReceiverActivityCompose", "intent was something else: ${intent.action}")
                    importState = ImportState.Failed
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (importState == ImportState.Ready && gpxTrackUriState != null) {
                    val addAsBookmarkLabel = stringResource(R.string.add_to_bookmarks)
                    val addAsSummitLabel = stringResource(R.string.add_to_summits)
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                isBookmark = true
                                showDialog = true
                            },
                            modifier = Modifier.semantics {
                                contentDescription = addAsBookmarkLabel
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_baseline_bookmarks_24),
                                    contentDescription = null
                                )
                            },
                            text = { Text(stringResource(R.string.add_to_bookmarks)) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ExtendedFloatingActionButton(
                            onClick = {
                                isBookmark = false
                                showDialog = true
                            },
                            modifier = Modifier.semantics {
                                contentDescription = addAsSummitLabel
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_add_location_alt_black_24dp),
                                    contentDescription = null
                                )
                            },
                            text = { Text(stringResource(R.string.add_to_summits)) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Map view
                SummitBookMapView(
                    modifier = Modifier.fillMaxSize(),
                    onMapCreated = { map ->
                        map.setTileSource(TileSourceFactory.OpenTopo)
                        mapViewReference = map
                    },
                    update = { map ->
                        Utils.fixEdgeToEdge(map)
                        // Redraw track if we have one and it hasn't been drawn yet
                        gpsTrack?.let { track ->
                            if (!track.isShownOnMap) {
                                drawGpxTrackOnMap(track, map)
                            }
                        }
                    }
                )
                when (importState) {
                    ImportState.Loading -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text(text = stringResource(R.string.gpx_import_loading))
                    }
                    ImportState.Failed -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.gpx_import_failed),
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onBackClicked) {
                            Text(text = stringResource(R.string.close))
                        }
                    }
                    ImportState.Ready -> Unit
                }
            }
        }

        // Show add summit dialog when requested
        if (showDialog && importState == ImportState.Ready && gpxTrackUriState != null) {
            AddSummitDialogCompose(
                summitsFromDatabase = emptyList(),
                peaks = emptyList(),
                uri = gpxTrackUri,
                isBookmark = isBookmark,
                onDismiss = {
                    showDialog = false
                    // Reset state after dialog is dismissed
                    isBookmark = false
                },
                onSaveSummit = { isEdit, summit ->
                    // Handle saving summit
                    viewModel.saveSummit(isEdit, summit).apply {
                        invokeOnCompletion {
                            showDialog = false
                            isBookmark = false
                        }
                    }
                }
            )
        }
    }

    private suspend fun processIntent(
        intent: Intent,
        onTrackProcessed: (GpsTrack?) -> Unit
    ) {
        val uri = when (intent.action) {
            Intent.ACTION_SEND ->
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> intent.data
        }
        gpxTrackUri = uri
        Log.i(
            "ReceiverActivityCompose",
            "intent was: ${intent.action} , received url ${gpxTrackUri.toString()}"
        )

        if (uri != null) {
            // Copying the shared file and parsing the GPX both hit the file
            // system; keep that off the main thread
            val gpsTrack = withContext(Dispatchers.IO) {
                try {
                    val file = File(AppState.cache, "input_filter_file.gpx")
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        copyGpxFileToCache(inputStream, file)
                    }
                    if (file.exists()) {
                        val track = prepareGpxTrack(file.toPath(), null)
                        if (track != null && !track.hasNoTrackPoints()) track else null
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("ReceiverActivityCompose", "Failed to read shared file: ${e.message}", e)
                    null
                }
            }
            onTrackProcessed(gpsTrack)
        } else {
            onTrackProcessed(null)
        }
    }

    private fun drawGpxTrackOnMap(gpsTrack: GpsTrack?, mapView: CustomMapViewToAllowScrolling) {
        gpsTrack?.addGpsTrack(mapView, TrackColor.None)
        val highestTrackPoint = gpsTrack?.getHighestElevation()

        if (highestTrackPoint != null) {
            val highestGeoPoint = GeoPoint(highestTrackPoint.latitude, highestTrackPoint.longitude)
            val marker = Marker(mapView)
            marker.position = highestGeoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ResourcesCompat.getDrawable(
                resources,
                R.drawable.ic_outline_location_green_48,
                null
            )
            marker.title = getString(R.string.new_summit_marker)
            mapView.overlays.add(marker)
        }

        if (gpsTrack != null) {
            mapView.post {
                mapView.calculateBoundingBox(gpsTrack.trackGeoPoints)
            }
        }
    }
}
