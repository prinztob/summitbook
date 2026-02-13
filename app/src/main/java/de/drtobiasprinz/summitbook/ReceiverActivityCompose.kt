package de.drtobiasprinz.summitbook

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.compose.AddSummitDialogCompose
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.copyGpxFileToCache
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.prepareGpxTrack
import de.drtobiasprinz.summitbook.utils.Utils
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File

@Suppress("AssignedValueIsNeverRead")
@AndroidEntryPoint
class ReceiverActivityCompose : ComponentActivity() {
    private var gpxTrackUri: Uri? = null
    private lateinit var mapView: CustomMapViewToAllowScrolling
    private val viewModel: DatabaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        Log.i("ReceiverActivityCompose", "onCreate")

        // Initialize cache and storage directories
        MainActivityCompose.cache = applicationContext.cacheDir
        MainActivityCompose.storage = applicationContext.filesDir
        MainActivityCompose.activitiesDir = File(MainActivityCompose.storage, "activities")

        setContent {
            SummitBookTheme {
                ReceiverScreen(
                    onBackClicked = { finish() }
                )
            }
        }
    }

    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ReceiverScreen(
        onBackClicked: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        var showDialog by remember { mutableStateOf(false) }
        var isBookmark by remember { mutableStateOf(false) }
        var gpsTrack by remember { mutableStateOf<GpsTrack?>(null) }
        var mapViewReference by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
        var gpxTrackUriState by remember { mutableStateOf<Uri?>(null) }

        LaunchedEffect(Unit) {
            // Initialize map configuration
            Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

            // Process intent when activity starts
            if (Intent.ACTION_VIEW == intent.action) {
                scope.launch {
                    processIntent(intent) { track ->
                        gpsTrack = track
                        gpxTrackUriState = gpxTrackUri
                        mapViewReference?.let { map ->
                            drawGpxTrackOnMap(track, map)
                        }
                    }
                }
            } else {
                Log.i("ReceiverActivityCompose", "intent was something else: ${intent.action}")
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
                if (gpxTrackUriState != null) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                isBookmark = true
                                showDialog = true
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
                AndroidView(
                    factory = { ctx ->
                        mapView = CustomMapViewToAllowScrolling(ctx).apply {
                            setTileSource(TileSourceFactory.OpenTopo)
                            addDefaultSettings()
                        }
                        mapViewReference = mapView
                        mapView
                    },
                    update = { map ->
                        Utils.fixEdgeToEdge(map)
                        // Redraw track if we have one and it hasn't been drawn yet
                        gpsTrack?.let { track ->
                            if (!track.isShownOnMap) {
                                drawGpxTrackOnMap(track, map)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Show add summit dialog when requested
        if (showDialog && gpxTrackUriState != null) {
            AddSummitDialogCompose(
                summitsFromDatabase = emptyList(),
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

    private fun processIntent(
        intent: Intent,
        onTrackProcessed: (GpsTrack?) -> Unit
    ) {
        val uri = intent.data
        gpxTrackUri = uri
        Log.i(
            "ReceiverActivityCompose",
            "intent was: ${intent.action} , received url ${gpxTrackUri.toString()}"
        )

        if (uri != null) {
            val file = File(MainActivityCompose.cache, "input_filter_file.gpx")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                copyGpxFileToCache(inputStream, file)
            }

            if (file.exists()) {
                val gpsTrack = prepareGpxTrack(file.toPath(), null)
                onTrackProcessed(gpsTrack)
            } else {
                onTrackProcessed(null)
            }
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
            marker.title = "New summit"
            mapView.overlays.add(marker)
        }

        if (gpsTrack != null) {
            mapView.post {
                mapView.calculateBoundingBox(gpsTrack.trackGeoPoints)
            }
        }
    }
}