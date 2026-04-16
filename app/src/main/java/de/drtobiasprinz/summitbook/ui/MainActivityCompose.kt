@file:Suppress("AssignedValueIsNeverRead")

package de.drtobiasprinz.summitbook.ui

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.PythonActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Peak
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.Poster
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.ui.compose.AddSummitDialogCompose
import de.drtobiasprinz.summitbook.ui.compose.BarChartScreen
import de.drtobiasprinz.summitbook.ui.compose.ForecastScreen
import de.drtobiasprinz.summitbook.ui.compose.LineChartScreen
import de.drtobiasprinz.summitbook.ui.compose.OpenStreetMapScreen
import de.drtobiasprinz.summitbook.ui.compose.OverviewScreen
import de.drtobiasprinz.summitbook.ui.compose.SegmentsListScreen
import de.drtobiasprinz.summitbook.ui.compose.SettingsScreen
import de.drtobiasprinz.summitbook.ui.compose.ShowNewSummitsFromGarminScreen
import de.drtobiasprinz.summitbook.ui.compose.SortAndFilterDialogCompose
import de.drtobiasprinz.summitbook.ui.compose.StatisticsScreen
import de.drtobiasprinz.summitbook.ui.compose.SummitEntitiesScreen
import de.drtobiasprinz.summitbook.ui.compose.SummitsListScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.ui.utils.GarminDataUpdater
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.utils.TimeIntervalPower
import de.drtobiasprinz.summitbook.ui.utils.ZipFileReader
import de.drtobiasprinz.summitbook.ui.utils.ZipFileWriter
import de.drtobiasprinz.summitbook.ui.work.SummitUpdateWorker
import de.drtobiasprinz.summitbook.utils.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.mapsforge.MapsForgeTileSource
import java.io.File
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivityCompose : ComponentActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val viewModel: DatabaseViewModel by viewModels()

    @Inject
    lateinit var repository: DatabaseRepository

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private var fullscreenImageViewer: FullscreenImageViewer? = null
    private var useFilteredSummits: Boolean = false

    // Navigation state
    private var currentDestination by mutableStateOf(Destination.Summits)
    private var showBookmarksOnly by mutableStateOf(false)
    private var isMapFullscreen by mutableStateOf(false)

    // Dialog states
    private var showSortAndFilterDialog by mutableStateOf(false)
    private var showAddSummitDialog by mutableStateOf(false)
    private var newSummitsSelectedDate by mutableStateOf<Date?>(null)

    // Loading state
    private val loadingState = mutableStateOf(false)
    private val loadingTooltip = mutableStateOf("")

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize disk I/O operations off main thread to avoid StrictMode violations
        lifecycleScope.launch(Dispatchers.IO) {
            // SharedPreferences access involves disk read
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MainActivityCompose)
            prefs.registerOnSharedPreferenceChangeListener(this@MainActivityCompose)
            sharedPreferences = prefs
            
            // File system operations
            val filesDir = applicationContext.filesDir
            val cacheDir = applicationContext.cacheDir
            storage = filesDir
            cache = cacheDir
            activitiesDir = File(filesDir, "activities")
            
            val heatmapDirectory = File(filesDir, "heatmaps")
            heatmapDirectory.mkdirs()
            heatmapDir = heatmapDirectory
            
            val segmentScreenshotDirectory = File(filesDir, "segmentScreenshots")
            segmentScreenshotDirectory.mkdirs()
            segmentScreenshotDir = segmentScreenshotDirectory
            
            // Pre-initialize OSMDroid tile cache database to avoid StrictMode violation
            // SqlTileWriter accesses SQLite when MapView is created
            try {
                val osmConf = org.osmdroid.config.Configuration.getInstance()
                osmConf.userAgentValue = BuildConfig.APPLICATION_ID
                // Trigger tile cache database initialization on background thread
                val tileWriter = org.osmdroid.tileprovider.modules.SqlTileWriter()
                tileWriter.onDetach()
            } catch (e: Exception) {
                Log.w("MainActivityCompose", "Failed to pre-initialize OSMDroid tile cache", e)
            }
            
            // Initialize Python and MapsForge off main thread to prevent frame skips
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this@MainActivityCompose))
            }
            pythonInstance = Python.getInstance()
            MapsForgeTileSource.createInstance(application)
            withContext(Dispatchers.Main) {
                updatePythonExecutor()
            }
        }

        // Schedule WorkManager for bounding box updates (every minute)
        scheduleBoundingBoxUpdateWorker()
        MainActivityCompose.applicationContext = applicationContext
        // Set the Compose content
        setContent {
            SummitBookTheme {
                MainScreen()
            }
        }
    }

    private fun scheduleBoundingBoxUpdateWorker() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val boundingBoxUpdateRequest = PeriodicWorkRequestBuilder<SummitUpdateWorker>(
            1, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BoundingBoxUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            boundingBoxUpdateRequest
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val darkTheme = isSystemInDarkTheme()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        var summitsFromDatabase by remember { mutableStateOf<List<Summit>>(emptyList()) }
        var filteredSummits by remember { mutableStateOf<List<Summit>>(emptyList()) }
        var forecasts by remember { mutableStateOf<List<Forecast>>(emptyList()) }

        val summitsList by viewModel.summitsList.asFlow()
            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
        val forecastList by viewModel.forecastList.asFlow()
            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
        val segmentsList by viewModel.segmentsList.asFlow()
            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
        val peakList by viewModel.peaks.asFlow()
            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())

        // Update forecasts if needed - run filtering on background thread
        LaunchedEffect(summitsList.data) {
            summitsList.data?.let { data ->
                summitsFromDatabase = data.toList()
                withContext(Dispatchers.Default) {
                    val filtered = sortFilterValues.applyForSummits(data).toList()
                    withContext(Dispatchers.Main) {
                        filteredSummits = filtered
                        latestFilteredSummits = filtered
                    }
                }
            }
        }

        LaunchedEffect(forecastList) {
            forecastList.data?.let {
                forecasts = it
            }
        }
        LaunchedEffect(peaks) {
            peakList.data?.let {
                peaks = it as MutableList<Peak>
            }
        }


        LaunchedEffect(forecastList, filteredSummits) {
            coroutineScope.launch(Dispatchers.IO) {
                segmentsList.data?.let {
                    setRecordsOnce(summitsFromDatabase, filteredSummits, it)
                }
            }
        }

        // Reset fullscreen state when navigating away from Map
        LaunchedEffect(currentDestination) {
            if (currentDestination != Destination.Map) {
                isMapFullscreen = false
            }
        }

        // Scaffold with top app bar and navigation drawer
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                NavigationDrawerContent(
                    onDestinationSelected = { destination ->
                        currentDestination = destination
                        showBookmarksOnly = false
                        isMapFullscreen = false
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    },
                    onBookmarksSelected = {
                        currentDestination = Destination.Summits
                        showBookmarksOnly = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    filteredSummits = filteredSummits,
                    topBarPadding = if (!isMapFullscreen) {
                        WindowInsets.statusBars.asPaddingValues()
                    } else {
                        PaddingValues(0.dp)
                    }
                )
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    if (!isMapFullscreen) {
                        var searchText by remember { mutableStateOf("") }
                        var isSearching by remember { mutableStateOf(false) }
                        val focusRequester = remember { FocusRequester() }
                        
                        // Debounced search job to prevent excessive filtering on main thread
                        var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (darkTheme) {
                                        Color.Black.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                    }
                                )
                        ) {
                            TopAppBar(
                                title = {
                                    if (isSearching) {
                                        OutlinedTextField(
                                            value = searchText,
                                            onValueChange = {
                                                searchText = it
                                                sortFilterValues.searchString = it
                                                // Cancel previous search job and start new one with debounce
                                                searchJob?.cancel()
                                                searchJob = coroutineScope.launch {
                                                    delay(300) // 300ms debounce
                                                    withContext(Dispatchers.Default) {
                                                        val filtered = sortFilterValues.applyForSummits(summitsFromDatabase)
                                                        withContext(Dispatchers.Main) {
                                                            filteredSummits = filtered
                                                        }
                                                    }
                                                }
                                            },
                                            placeholder = { Text(stringResource(R.string.search)) },
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    isSearching = false
                                                    searchText = ""
                                                    sortFilterValues.searchString = ""
                                                    searchJob?.cancel()
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.Default) {
                                                            val filtered = sortFilterValues.applyForSummits(summitsFromDatabase)
                                                            withContext(Dispatchers.Main) {
                                                                filteredSummits = filtered
                                                            }
                                                        }
                                                    }
                                                }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.baseline_cancel_24),
                                                        contentDescription = "Close search"
                                                    )
                                                }
                                            },
                                            singleLine = true,
                                            modifier = Modifier.focusRequester(focusRequester)
                                        )
                                    } else {
                                        Text(stringResource(R.string.app_name))
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_menu_24),
                                            contentDescription = "Menu"
                                        )
                                    }
                                },
                                actions = {
                                    // Search action
                                    if (!isSearching) {
                                        IconButton(onClick = {
                                            isSearching = true
                                            // Request focus after a short delay to ensure the text field is rendered
                                            coroutineScope.launch {
                                                delay(100)
                                                focusRequester.requestFocus()
                                            }
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_baseline_search_24),
                                                contentDescription = stringResource(R.string.action_search)
                                            )
                                        }
                                    }

                                    // Sort action
                                    IconButton(onClick = { showSortAndFilterDialog() }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_baseline_sort_24),
                                            contentDescription = stringResource(R.string.sort_entries)
                                        )
                                    }

                                    // Update action
                                    IconButton(onClick = {
                                        updateThirdPartyData(
                                            coroutineScope
                                        )
                                    }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_baseline_sync_24),
                                            contentDescription = stringResource(R.string.update_3rd_part)
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Main content based on current destination
                    MainContent(filteredSummits, summitsFromDatabase, forecasts, coroutineScope)

                    // Floating Action Button for adding a summit (only visible on Summits screen)
                    if (currentDestination == Destination.Summits) {
                        FloatingActionButton(
                            onClick = {
                                showAddSummitDialog()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_add_black_24dp),
                                contentDescription = stringResource(R.string.add_new_summit)
                            )
                        }
                    }

                    // Show Add Summit Dialog when requested
                    if (showAddSummitDialog) {
                        AddSummitDialogCompose(
                            summitsFromDatabase,
                            peaks,
                            isBookmark = showBookmarksOnly,
                            onDismiss = { showAddSummitDialog = false },
                            onSaveSummit = { isEdit, summit ->
                                viewModel.saveSummit(
                                    isEdit,
                                    summit
                                )
                            },
                            onPeakToggle = { placeName, isPeak ->
                                if (isPeak) {
                                    val elevation = summitsFromDatabase.firstOrNull {
                                        it.name == placeName || it.places.contains(placeName)
                                    }?.elevationData?.maxElevation ?: 0
                                    viewModel.savePeak(Peak(placeName, elevation))
                                } else {
                                    val peakToRemove = peaks.find { it.name == placeName }
                                    peakToRemove?.let { viewModel.deletePeak(it) }
                                }
                            }
                        )
                    }

                    // Show Sort and Filter Dialog when requested
                    if (showSortAndFilterDialog) {
                        SortAndFilterDialogCompose(
                            sortFilterValues = sortFilterValues,
                            onDismiss = { showSortAndFilterDialog = false },
                            onApply = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.Default) {
                                        val filtered = sortFilterValues.applyForSummits(summitsFromDatabase)
                                        withContext(Dispatchers.Main) {
                                            filteredSummits = filtered
                                        }
                                    }
                                }
                            },
                            summits = summitsFromDatabase
                        )
                    }

                    // Loading indicator
                    if (loadingState.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .zIndex(1f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(150.dp),
                                strokeWidth = 8.dp
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun NavigationDrawerContent(
        onDestinationSelected: (Destination) -> Unit,
        onBookmarksSelected: () -> Unit,
        topBarPadding: PaddingValues = PaddingValues(0.dp),
        filteredSummits: List<Summit>,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                    top = 16.dp + topBarPadding.calculateTopPadding()
                )
                .background(MaterialTheme.colorScheme.surface)
        ) {

            // Navigation items
            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.outline_landscape_2_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.nav_summits)) },
                selected = currentDestination == Destination.Summits && !showBookmarksOnly,
                onClick = { onDestinationSelected(Destination.Summits) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_map_black_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.nav_osmap)) },
                selected = currentDestination == Destination.Map,
                onClick = { onDestinationSelected(Destination.Map) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_bookmarks_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.bookmarks)) },
                selected = showBookmarksOnly,
                onClick = { onBookmarksSelected() },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_route_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.segments)) },
                selected = currentDestination == Destination.Routes,
                onClick = { onDestinationSelected(Destination.Routes) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_pie_chart_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.nav_statistics)) },
                selected = currentDestination == Destination.Statistics,
                onClick = { onDestinationSelected(Destination.Statistics) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_multiline_chart_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.nav_diagrams)) },
                selected = currentDestination == Destination.Diagrams,
                onClick = { onDestinationSelected(Destination.Diagrams) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_bar_chart_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.bar_charts)) },
                selected = currentDestination == Destination.BarCharts,
                onClick = { onDestinationSelected(Destination.BarCharts) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_photo_album_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.nav_diashow)) },
                selected = false,
                onClick = {
                    openViewer(filteredSummits)
                    onDestinationSelected(Destination.Summits)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_timer_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.forecast)) },
                selected = currentDestination == Destination.Forecast,
                onClick = { onDestinationSelected(Destination.Forecast) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_landscape_black_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.new_summits)) },
                selected = currentDestination == Destination.NewSummits,
                onClick = { onDestinationSelected(Destination.NewSummits) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_handyman_24),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.additional_summit_data)) },
                selected = currentDestination == Destination.AdditionalData,
                onClick = { onDestinationSelected(Destination.AdditionalData) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider()

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_upload_black_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.nav_export)) },
                selected = false,
                onClick = {
                    showExportCsvDialog()
                    onDestinationSelected(Destination.Summits)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_download_black_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.nav_import)) },
                selected = false,
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip"
                    }
                    resultLauncherForImportZip.launch(intent)
                    onDestinationSelected(Destination.Summits)
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider()

            NavigationDrawerItem(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_settings_white_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.action_settings)) },
                selected = currentDestination == Destination.Settings,
                onClick = { onDestinationSelected(Destination.Settings) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    fun MainContent(
        filteredSummits: List<Summit>,
        summitsFromDatabase: List<Summit>,
        forecasts: List<Forecast>,
        coroutineScope: CoroutineScope
    ) {
        when (currentDestination) {
            Destination.Summits -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Show OverviewScreen above SummitsListScreen (like in original MainActivity)
                    if (!showBookmarksOnly) {
                        OverviewScreen(
                            filteredSummits = filteredSummits,
                            summitsFromDatabase = summitsFromDatabase,
                            forecasts = forecasts,
                            years = sortFilterValues.years
                        )
                    }

                    SummitsListScreen(
                        filteredSummits = if (showBookmarksOnly) summitsFromDatabase.filter { it.isBookmark } else filteredSummits,
                        summitsFromDatabase = summitsFromDatabase,
                        peaks = peaks,
                        isBookmark = showBookmarksOnly,
                        onSaveSummit = { isEdit, summit -> viewModel.saveSummit(isEdit, summit) },
                        onDelete = { summit ->
                            viewModel.deleteSummit(summit)
                        },
                        onPeakToggle = { placeName, isPeak ->
                            // Update peak in database when toggled
                            if (isPeak) {
                                // Add as peak - try to get elevation from existing summit
                                val elevation = summitsFromDatabase.firstOrNull {
                                    it.name == placeName || it.places.contains(placeName)
                                }?.elevationData?.maxElevation ?: 0
                                viewModel.savePeak(Peak(placeName, elevation))
                            } else {
                                // Remove from peaks
                                val peakToRemove = peaks.find { it.name == placeName }
                                peakToRemove?.let { viewModel.deletePeak(it) }
                            }
                        }
                    )
                }
            }

            Destination.Overview -> {
                OverviewScreen(
                    filteredSummits,
                    summitsFromDatabase,
                    forecasts,
                    sortFilterValues.years
                )
            }

            Destination.Routes -> {
                val segmentsList by viewModel.segmentsList.asFlow()
                    .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                SegmentsListScreen(
                    segments = sortFilterValues.applyForSegments(segmentsList.data ?: emptyList()),
                    summits = summitsFromDatabase,
                    onDeleteSegment = { segment ->
                        viewModel.deleteSegment(segment)
                    }
                )
            }

            Destination.Statistics -> {
                StatisticsScreen(
                    filteredSummits = filteredSummits,
                    forecasts = forecasts,
                    onNavigateToSummitDetails = { startSummitEntryDetailsComposeActivity(it) })
            }

            Destination.Diagrams -> {
                LineChartScreen(
                    filteredSummits = filteredSummits,
                    onNavigateToSummitDetails = { startSummitEntryDetailsComposeActivity(it) })
            }

            Destination.BarCharts -> {
                val dailyActivitySummary by viewModel.dailyActivitySummary.asFlow()
                    .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                BarChartScreen(
                    filteredSummits,
                    forecasts,
                    dailyActivitySummary.data ?: emptyList(),
                    !sortFilterValues.wasFullYearSelected()
                )
            }

            Destination.Map -> {
                OpenStreetMapScreen(
                    filteredSummits,
                    summitsFromDatabase.filter { it.isBookmark },
                    onFullscreenChanged = { isFullscreen ->
                        isMapFullscreen = isFullscreen
                    }
                )
            }

            Destination.Forecast -> {
                ForecastScreen(
                    summitsFromDatabase,
                    forecasts as MutableList<Forecast>,
                    { currentDestination = Destination.Summits },
                    { isEdit, forecasts -> viewModel.saveForecasts(isEdit, forecasts) })
            }

            Destination.NewSummits -> {
                ShowNewSummitsFromGarminScreen(
                    viewModel = viewModel,
                    summits = summitsFromDatabase,
                    selectedDate = newSummitsSelectedDate,
                    onBack = { selectedSummits, isMerge ->
                        currentDestination = Destination.Summits
                        // Execute download for selected summits
                        coroutineScope.launch {
                            val finalSummits = if (isMerge) {
                                listOf(executeDownload(selectedSummits))
                            } else {
                                selectedSummits.map {
                                    executeDownload(listOf(it))
                                }
                            }.filterNotNull()
                            viewModel.saveSummits(finalSummits).invokeOnCompletion {
                                loadingState.value = false
                                loadingTooltip.value = ""
                            }
                        }
                    },
                    onRefresh = {
                        updateThirdPartyData(coroutineScope)
                    }
                )
            }

            Destination.AdditionalData -> {
                val entityEvents by viewModel.entityEvents.asFlow()
                    .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                SummitEntitiesScreen(
                    filteredSummits,
                    entityEvents.data ?: emptyList(),
                    sortFilterValues,
                    { isEdit, summit -> viewModel.saveSummit(isEdit, summit) },
                    { viewModel.deleteEntityEvent(it) },
                    { isEdit, event -> viewModel.saveEntityEvent(isEdit, event) },
                    { oldName, newName -> viewModel.updatePeakName(oldName, newName) }
                )
            }

            Destination.Settings -> {
                SettingsScreen(summitsFromDatabase, { key ->
                    onSharedPreferenceChanged(sharedPreferences, key)
                }, onSaveSummit = { isEdit, summit -> viewModel.saveSummit(isEdit, summit) })
            }
        }
    }

    private fun startSummitEntryDetailsComposeActivity(summitId: Long) {
        val intent = Intent(this@MainActivityCompose, SummitEntryDetailsComposeActivity::class.java)
        intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, summitId)
        this@MainActivityCompose.startActivity(intent)
    }

    private fun showSortAndFilterDialog() {
        showSortAndFilterDialog = true
    }

    private fun showAddSummitDialog() {
        showAddSummitDialog = true
    }

    fun updateThirdPartyData(scope: CoroutineScope) {
        scope.launch {
            val executor = pythonExecutor
            if (executor != null) {
                loadingState.value = true
                loadingTooltip.value = getString(R.string.update_3rd_part)
                val updater = GarminDataUpdater(
                    sharedPreferences,
                    executor,
                    repository,
                    viewModel
                )
                withContext(Dispatchers.IO) {
                    updater.update()
                }
                updater.onFinish(
                    object : ProgressBar(this@MainActivityCompose) {
                        override fun getVisibility(): Int {
                            return if (loadingState.value) VISIBLE else GONE
                        }

                        override fun setVisibility(visibility: Int) {
                            loadingState.value = visibility == VISIBLE
                        }
                    },
                    this@MainActivityCompose
                ) {
                    currentDestination = Destination.NewSummits
                }
            } else {
                Toast.makeText(
                    this@MainActivityCompose,
                    getString(R.string.set_user_pwd),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun executeDownload(summits: List<Summit>): Summit? {
        val downloader = GarminTrackAndDataDownloader(
            summits, pythonExecutor, sharedPreferences.getBoolean(Keys.PREF_DOWNLOAD_TCX, false)
        )
        loadingState.value = true
        loadingTooltip.value = getString(
            R.string.tool_tip_progress_new_garmin_activities,
            summits.joinToString(", ") { it.name })
        return try {
            withContext(Dispatchers.IO) {
                downloader.extractFinalSummit()
                if (downloader.finalEntry?.sportType != SportType.IndoorTrainer) {
                    downloader.downloadTracks()
                    downloader.composeFinalTrack()
                }
                downloader.finalEntry
            }
        } catch (e: RuntimeException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivityCompose,
                    "Connecting to third party provider failed. Please try again later. Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(
                    "MainActivity",
                    "Connecting to third party provider failed. Please try again later. Error: ${e.message}",
                    e
                )
            }
            null
        } finally {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivityCompose,
                    getString(R.string.add_new_summit_successful),
                    Toast.LENGTH_LONG
                ).show()
                loadingState.value = false
            }
        }
    }

    private fun showExportCsvDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_csv_dialog))
            .setMessage(getString(R.string.export_csv_dialog_text))
            .setPositiveButton(R.string.export_csv_dialog_positive) { _: DialogInterface?, _: Int ->
                useFilteredSummits = false
                startFileSelectorAndExportSummits(
                    String.format(
                        "%s_summit-book_backup_ALL.zip", LocalDate.now()
                    )
                )
            }.setNeutralButton(
                R.string.export_csv_dialog_neutral
            ) { _: DialogInterface?, _: Int ->
                useFilteredSummits = true
                startFileSelectorAndExportSummits(
                    String.format(
                        "%s_summit-book_backup_FILTERED.zip", LocalDate.now()
                    )
                )
            }.setNegativeButton(
                android.R.string.cancel
            ) { _: DialogInterface?, _: Int ->
                Toast.makeText(
                    this, getString(R.string.export_csv_dialog_negative_text), Toast.LENGTH_SHORT
                ).show()
            }.setIcon(android.R.drawable.ic_dialog_alert).show()
    }

    private fun startFileSelectorAndExportSummits(filename: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        resultLauncherForExportZipSummits.launch(intent)
    }

    private val resultLauncherForExportZipSummits =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val exportThirdPartyData =
                    sharedPreferences.getBoolean(Keys.PREF_EXPORT_THIRD_PARTY_DATA, true)
                val exportCalculatedData =
                    sharedPreferences.getBoolean(Keys.PREF_EXPORT_CALCULATED_DATA, true)
                // In a real implementation, we would collect the flows
                // For now, we'll just simulate the callback behavior
                exportZipFile(exportThirdPartyData, exportCalculatedData, result.data)
            }
        }

    private fun exportZipFile(
        exportThirdPartyData: Boolean,
        exportCalculatedData: Boolean,
        resultData: Intent?
    ) {
        // Collect data from ViewModels using .asFlow().collectAsStateWithLifecycle pattern
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            val summitsListDataStatus = viewModel.summitsList.asFlow().first()
            val forecastListDataStatus = viewModel.forecastList.asFlow().first()
            val segmentsListDataStatus = viewModel.segmentsList.asFlow().first()
            val entityEventsDataStatus = viewModel.entityEvents.asFlow().first()

            loadingState.value = true
            val allSummits = summitsListDataStatus.data ?: emptyList()
            val filteredSummits = if (useFilteredSummits) {
                allSummits.let { sortFilterValues.applyForSummits(it) }
            } else {
                allSummits
            }
            val forecasts = forecastListDataStatus.data ?: emptyList()
            val segments = segmentsListDataStatus.data ?: emptyList()
            val entityEvents = entityEventsDataStatus.data ?: emptyList()

            val writer = ZipFileWriter(
                filteredSummits,
                segments,
                forecasts,
                entityEvents,
                this@MainActivityCompose,
                exportThirdPartyData,
                exportCalculatedData
            )
            withContext(Dispatchers.IO) {
                resultData?.data?.also { resultDataUri ->
                    contentResolver.openOutputStream(resultDataUri)?.let {
                        writer.writeToZipFile(it)
                        it.close()
                    }
                }
            }
            loadingState.value = false
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivityCompose)
                .setTitle(getString(R.string.export_csv_summary_title)).setMessage(
                    getString(
                        R.string.export_csv_summary_text,
                        filteredSummits.size.toString(),
                        writer.withGpsFile.toString(),
                        writer.withImages.toString()
                    )
                )
                .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                .setIcon(android.R.drawable.ic_dialog_info).show()
        }
    }

    private val resultLauncherForImportZip =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data.also { uri ->
                    if (uri != null) {
                        asyncImportZipFile(uri)
                    }
                }
            }
        }

    private fun asyncImportZipFile(uri: Uri) {
        // Collect data from ViewModels using .asFlow().collectAsStateWithLifecycle pattern
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            val summitsListDataStatus = viewModel.summitsList.asFlow().first()
            val forecastListDataStatus = viewModel.forecastList.asFlow().first()
            val segmentsListDataStatus = viewModel.segmentsList.asFlow().first()

            loadingState.value = true
            val summits = summitsListDataStatus.data?.toMutableList() ?: mutableListOf()
            val forecasts =
                forecastListDataStatus.data?.toMutableList() ?: mutableListOf()
            val segments =
                segmentsListDataStatus.data?.toMutableList() ?: mutableListOf()

            var successfulImports = 0
            var unsuccessfulImports = 0
            var duplicateImports = 0

            withContext(Dispatchers.IO) {
                val reader = ZipFileReader(
                    File(cacheDir, "ZipFileReader_${Date().time}"),
                    summits,
                    forecasts,
                    segments
                )
                reader.saveSummit = { isEdit, summit ->
                    viewModel.saveSummit(isEdit, summit)
                }
                reader.saveForecast = { forecast ->
                    viewModel.saveForecast(false, forecast)
                }
                reader.saveSegmentDetails = { details ->
                    viewModel.saveSegmentDetails(false, details)
                }
                reader.saveSegmentEntry = { entry ->
                    viewModel.saveSegmentEntry(false, entry)
                }
                reader.saveEntityEvent = { entry ->
                    viewModel.saveEntityEvent(false, entry)
                }
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    reader.extractAndImport(inputStream)
                    successfulImports = reader.successful
                    unsuccessfulImports = reader.unsuccessful
                    duplicateImports = reader.duplicate
                }
                reader.cleanUp()
            }
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivityCompose)
                .setTitle(getString(R.string.import_string_title))
                .setMessage(
                    getString(
                        R.string.import_string,
                        (successfulImports + unsuccessfulImports + duplicateImports).toString(),
                        successfulImports.toString(),
                        unsuccessfulImports.toString(),
                        duplicateImports.toString()
                    )
                )
                .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                .setIcon(android.R.drawable.ic_dialog_info).show()
            loadingState.value = false
        }
    }

    private suspend fun getAllImages(summits: List<Summit>?): MutableList<Poster> {
        return withContext(Dispatchers.Default) {
            summits?.flatMap { entry ->
                entry.imageIds.mapIndexed { i, imageId ->
                    Poster(
                        entry.getImageUrl(imageId), entry.getImageDescription(resources, i)
                    )
                }
            } as MutableList<Poster>
        }
    }

    private fun openViewer(filteredSummits: List<Summit>) {
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            val allImages = getAllImages(filteredSummits)

            if (fullscreenImageViewer == null) {
                fullscreenImageViewer = FullscreenImageViewer(this@MainActivityCompose, resources)
            }

            val currentPosition = fullscreenImageViewer?.currentPosition ?: 0
            val adjustedPosition = if (allImages.size <= currentPosition) 0 else currentPosition

            if (allImages.isNotEmpty()) {
                Log.i("MainActivity", "showFullscreenImageViewer")
                fullscreenImageViewer?.show(allImages, adjustedPosition, filteredSummits)
            } else {
                Toast.makeText(
                    this@MainActivityCompose,
                    getString(R.string.no_image_selected),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_DIALOG_SHOWN, fullscreenImageViewer?.isShowing() ?: false)
        outState.putInt(KEY_CURRENT_POSITION, fullscreenImageViewer?.currentPosition ?: 0)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val isDialogShown = savedInstanceState.getBoolean(KEY_IS_DIALOG_SHOWN)
        if (isDialogShown) {
            openViewer(latestFilteredSummits)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fullscreenImageViewer?.dismiss()
        fullscreenImageViewer = null
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (key == Keys.PREF_CURRENT_YEAR_SWITCH) {
            sortFilterValues.updateCurrentYearSwitch(
                sharedPreferences.getBoolean(Keys.PREF_CURRENT_YEAR_SWITCH, false)
            )
            viewModel.refresh()
        }
        if (key == Keys.PREF_GARMIN_USERNAME || key == Keys.PREF_GARMIN_PASSWORD || key == Keys.PREF_GARMIN_MFA) {
            updatePythonExecutor()
        }
    }

    private fun updatePythonExecutor() {
        if (pythonExecutor == null || pythonExecutor?.username == "" || pythonExecutor?.password == "") {
            val username = sharedPreferences.getString(Keys.PREF_GARMIN_USERNAME, "") ?: ""
            val password = sharedPreferences.getString(Keys.PREF_GARMIN_PASSWORD, "") ?: ""
            val garminMfaSwitch = sharedPreferences.getBoolean(Keys.PREF_GARMIN_MFA, false)
            val oauthPath = File(storage?.absolutePath, ".garminconnect")
            // Use cached hasTrack property or allow disk read temporarily for this check
            val oldPolicy = StrictMode.allowThreadDiskReads()
            try {
                if (oauthPath.exists()) {
                    pythonExecutor = GarminPythonExecutor(username, password)
                } else if (username != "" && password != "" && garminMfaSwitch) {
                    val intent = Intent(this, PythonActivity::class.java)
                    startActivity(intent)
                }
            } finally {
                StrictMode.setThreadPolicy(oldPolicy)
            }
        }
    }

    private fun setRecordsOnce(
        allSummits: List<Summit>,
        filteredSummits: List<Summit>,
        segments: List<Segment>
    ) {
        Log.i(
            "SummitListScreen",
            "setRecordsOnce - records will be added for ${filteredSummits.size} summits."
        )

        activitiesWithPowerRecordsFiltered =
            getSummitIdsWithPowerRecord(filteredSummits)
        val calendar = Calendar.getInstance()
        // Move calendar back 5 years from today
        calendar.add(Calendar.YEAR, -5)
        activitiesWithPowerRecordsLast5Years = getSummitIdsWithPowerRecord(
            allSummits.filter { it.date.after(calendar.time) })
        activitiesWithPowerRecordsAll = getSummitIdsWithPowerRecord(allSummits)

        if (!segments.isEmpty()) {
            allSummits.forEach { summit ->
                summit.updateSegmentInfo(segments)
            }


            allSummits.forEach { summit ->
                if (summit.segmentInfo.isNotEmpty()) {
                    val position = summit.segmentInfo.minOf { it.third }
                    if (position in 1..3) {
                        activitiesWithSegmentsRecord.add(
                            Pair(
                                summit.activityId, position
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getSummitIdsWithPowerRecord(
        summits: List<Summit>,
    ): List<Long> {
        return TimeIntervalPower.entries.mapNotNull { interval ->
            summits.filter { interval.value(it) > 0 }.maxByOrNull { interval.value(it) }?.activityId
        }
    }

    companion object {
        var peaks: MutableList<Peak> = mutableListOf()
        private const val KEY_IS_DIALOG_SHOWN = "IS_DIALOG_SHOWN"
        private const val KEY_CURRENT_POSITION = "CURRENT_POSITION"
        var CSV_FILE_NAME_VERSION: String = "de-prinz-summitbook-export.version"
        var CSV_FILE_VERSION: String = "v1"
        var CSV_FILE_NAME_SUMMITS: String = "de-prinz-summitbook-export.csv"
        var CSV_FILE_NAME_THIRD_PARTY_DATA: String =
            "de-prinz-summitbook-export-third-party-data.csv"
        var CSV_FILE_NAME_CALCULATED_DATA: String = "de-prinz-summitbook-export-calculated-data.csv"
        var CSV_FILE_NAME_SEGMENTS: String = "de-prinz-summitbook-export-segments.csv"
        var CSV_FILE_NAME_FORECASTS: String = "de-prinz-summitbook-export-forecasts.csv"
        var CSV_FILE_NAME_ENTITY_EVENTS: String = "de-prinz-summitbook-export-entity-events.csv"

        var storage: File? = null
        var cache: File? = null
        var activitiesDir: File? = null
        var heatmapDir: File? = null
        var segmentScreenshotDir: File? = null
        var pythonInstance: Python? = null
        var pythonExecutor: GarminPythonExecutor? = null
        var activitiesWithPowerRecordsFiltered: List<Long> = emptyList()
        var activitiesWithPowerRecordsLast5Years: List<Long> = emptyList()
        var activitiesWithPowerRecordsAll: List<Long> = emptyList()
        var activitiesWithSegmentsRecord: MutableList<Pair<Long, Int>> = mutableListOf()
        lateinit var sharedPreferences: SharedPreferences
        var latestFilteredSummits: List<Summit> = emptyList()

        var applicationContext: Context? = null
    }
}

// Enum for destinations
enum class Destination {
    Summits,
    Overview,
    Routes,
    Statistics,
    Diagrams,
    BarCharts,
    Map,
    Forecast,
    NewSummits,
    AdditionalData,
    Settings
}