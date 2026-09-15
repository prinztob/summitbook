@file:Suppress("AssignedValueIsNeverRead")

package de.drtobiasprinz.summitbook.ui.activities

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.chaquo.python.Python
import com.google.gson.Gson
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.core.Keys
import de.drtobiasprinz.summitbook.ui.activities.PythonActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.data.db.entities.Forecast
import de.drtobiasprinz.summitbook.data.db.entities.Peak
import de.drtobiasprinz.summitbook.data.db.entities.Segment
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.model.Poster
import de.drtobiasprinz.summitbook.ui.filters.SortFilterValues
import de.drtobiasprinz.summitbook.data.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.ui.compose.AddSegmentEntryScreen
import de.drtobiasprinz.summitbook.ui.compose.AddSummitDialogCompose
import de.drtobiasprinz.summitbook.ui.compose.DatabaseErrorBanner
import de.drtobiasprinz.summitbook.ui.compose.BarChartScreen
import de.drtobiasprinz.summitbook.ui.compose.ForecastScreen
import de.drtobiasprinz.summitbook.ui.compose.LineChartScreen
import de.drtobiasprinz.summitbook.ui.compose.LoadingPanel
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
import de.drtobiasprinz.summitbook.data.analytics.DistanceIntervalVelocity
import de.drtobiasprinz.summitbook.sync.GarminDataUpdater
import de.drtobiasprinz.summitbook.sync.GarminSyncResult
import de.drtobiasprinz.summitbook.sync.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.data.analytics.TimeIntervalPower
import de.drtobiasprinz.summitbook.data.analytics.TimeIntervalVerticalVelocity
import de.drtobiasprinz.summitbook.data.backup.ZipFileReader
import de.drtobiasprinz.summitbook.data.backup.ZipFileWriter
import de.drtobiasprinz.summitbook.work.SummitUpdateWorker
import de.drtobiasprinz.summitbook.core.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import de.drtobiasprinz.summitbook.core.DataStatus
import de.drtobiasprinz.summitbook.ui.viewmodel.DatabaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
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
import kotlin.time.Duration.Companion.milliseconds
import de.drtobiasprinz.summitbook.ui.view.FullscreenImageViewer
import de.drtobiasprinz.summitbook.ui.view.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.sync.GarminPythonExecutor
import de.drtobiasprinz.summitbook.data.appstate.AppState

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

    // Segment entry states
    private var summitForSegmentEntry by mutableStateOf<Summit?>(null)
    private var showAddSegmentEntryScreen by mutableStateOf(false)

    // Loading state
    private val loadingState = mutableStateOf(false)
    private val loadingTooltip = mutableStateOf("")
    private val loadingProgressCurrent = mutableStateOf<Int?>(null)
    private val loadingProgressTotal = mutableStateOf<Int?>(null)
    private var loadingJob by mutableStateOf<Job?>(null)
    private var snackbarHostState by mutableStateOf<SnackbarHostState?>(null)

    private fun resetLoadingState() {
        loadingState.value = false
        loadingTooltip.value = ""
        loadingProgressCurrent.value = null
        loadingProgressTotal.value = null
        loadingJob = null
    }

    /**
     * Keeps the current screen, open dialogs and the summit used for segment
     * entry across rotation/process death, so no user input context is lost.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DESTINATION, currentDestination.name)
        outState.putBoolean(STATE_SHOW_BOOKMARKS_ONLY, showBookmarksOnly)
        outState.putBoolean(STATE_SHOW_SORT_AND_FILTER_DIALOG, showSortAndFilterDialog)
        outState.putBoolean(STATE_SHOW_ADD_SUMMIT_DIALOG, showAddSummitDialog)
        newSummitsSelectedDate?.let { outState.putLong(STATE_NEW_SUMMITS_DATE, it.time) }
        summitForSegmentEntry?.let {
            outState.putString(STATE_SUMMIT_FOR_SEGMENT_ENTRY, Gson().toJson(it))
        }
        outState.putBoolean(
            STATE_SHOW_ADD_SEGMENT_ENTRY_SCREEN,
            showAddSegmentEntryScreen && summitForSegmentEntry != null
        )
    }

    private fun restoreUiState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        savedInstanceState.getString(STATE_DESTINATION)?.let { name ->
            runCatching { Destination.valueOf(name) }.getOrNull()?.let {
                currentDestination = it
            }
        }
        showBookmarksOnly = savedInstanceState.getBoolean(STATE_SHOW_BOOKMARKS_ONLY, false)
        showSortAndFilterDialog =
            savedInstanceState.getBoolean(STATE_SHOW_SORT_AND_FILTER_DIALOG, false)
        showAddSummitDialog = savedInstanceState.getBoolean(STATE_SHOW_ADD_SUMMIT_DIALOG, false)
        if (savedInstanceState.containsKey(STATE_NEW_SUMMITS_DATE)) {
            newSummitsSelectedDate = Date(savedInstanceState.getLong(STATE_NEW_SUMMITS_DATE))
        }
        savedInstanceState.getString(STATE_SUMMIT_FOR_SEGMENT_ENTRY)?.let { json ->
            runCatching { Gson().fromJson(json, Summit::class.java) }.getOrNull()?.let {
                summitForSegmentEntry = it
            }
        }
        showAddSegmentEntryScreen =
            savedInstanceState.getBoolean(STATE_SHOW_ADD_SEGMENT_ENTRY_SCREEN, false) &&
                summitForSegmentEntry != null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        restoreUiState(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize disk I/O operations off main thread to avoid StrictMode violations
        lifecycleScope.launch(Dispatchers.IO) {
            // SharedPreferences access involves disk read
            val prefs =
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MainActivityCompose)
            prefs.registerOnSharedPreferenceChangeListener(this@MainActivityCompose)
            AppState.sharedPreferences = prefs

            // File system operations
            val filesDir = applicationContext.filesDir
            val cacheDir = applicationContext.cacheDir
            AppState.storage = filesDir
            AppState.cache = cacheDir
            AppState.activitiesDir = File(filesDir, "activities")

            val heatmapDirectory = File(filesDir, "heatmaps")
            heatmapDirectory.mkdirs()
            AppState.heatmapDir = heatmapDirectory

            val segmentScreenshotDirectory = File(filesDir, "segmentScreenshots")
            segmentScreenshotDirectory.mkdirs()
            AppState.segmentScreenshotDir = segmentScreenshotDirectory

            // Pre-initialize OSMDroid tile cache database to avoid StrictMode violation
            // SqlTileWriter accesses SQLite when MapView is created
            try {
                val osmConf = org.osmdroid.config.Configuration.getInstance()
                osmConf.userAgentValue = BuildConfig.APPLICATION_ID
                // Point osmdroid to a writable cache dir, otherwise SqlTileWriter
                // tries to open the unwritable default '/tiles/cache.db'
                CustomMapViewToAllowScrolling.setOsmConfForTiles(true)
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
            AppState.pythonInstance = Python.getInstance()
            MapsForgeTileSource.createInstance(application)
            withContext(Dispatchers.Main) {
                updatePythonExecutor()
            }
        }

        // Schedule WorkManager for bounding box updates (every minute)
        scheduleBoundingBoxUpdateWorker()
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

        // WorkManager clamps periodic work to at least 15 minutes anyway; a
        // frequent interval kept the CPU/Python runtime busy while the user was
        // interacting with the app, so run it every 12 hours instead.
        val boundingBoxUpdateRequest = PeriodicWorkRequestBuilder<SummitUpdateWorker>(
            12, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BoundingBoxUpdateWork",
            ExistingPeriodicWorkPolicy.UPDATE,
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
        this.snackbarHostState = snackbarHostState
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
                        AppState.latestFilteredSummits = filtered
                    }
                }
            }
        }

        LaunchedEffect(forecastList) {
            forecastList.data?.let {
                forecasts = it
            }
        }
        LaunchedEffect(AppState.peaks) {
            peakList.data?.let {
                AppState.peaks = it as MutableList<Peak>
            }
        }


        // Recompute records; debounced so a burst of database writes (e.g. an
        // import) only triggers one recomputation instead of one per emission
        var recordsJob by remember { mutableStateOf<Job?>(null) }
        LaunchedEffect(forecastList, filteredSummits, summitsFromDatabase) {
            recordsJob?.cancel()
            recordsJob = coroutineScope.launch(Dispatchers.IO) {
                delay(300.milliseconds)
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

        // Surface database read errors instead of rendering them as empty lists
        val databaseError = remember(summitsList, forecastList, segmentsList, peakList) {
            listOf(summitsList, forecastList, segmentsList, peakList)
                .firstOrNull { it.status == DataStatus.Status.ERROR }?.message
        }
        var errorBannerDismissed by rememberSaveable { mutableStateOf(false) }

        // Scaffold with top app bar and navigation drawer
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                // Keep the diashow lambda stable so the drawer does not recompose
                // whenever the filtered summit list changes
                val filteredSummitsForDiashow = rememberUpdatedState(filteredSummits)
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
                    onShowDiashow = { openViewer(filteredSummitsForDiashow.value) },
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
                        var searchJob by remember { mutableStateOf<Job?>(null) }

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
                                                    delay(300.milliseconds)
                                                    withContext(Dispatchers.Default) {
                                                        val filtered =
                                                            sortFilterValues.applyForSummits(
                                                                summitsFromDatabase
                                                            )
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
                                                            val filtered =
                                                                sortFilterValues.applyForSummits(
                                                                    summitsFromDatabase
                                                                )
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
                                                delay(100.milliseconds)
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

                    if (databaseError != null && !errorBannerDismissed) {
                        DatabaseErrorBanner(
                            modifier = Modifier.align(Alignment.TopCenter),
                            detail = databaseError,
                            onDismiss = { errorBannerDismissed = true }
                        )
                    }

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
                            AppState.peaks,
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
                                    val peakToRemove = AppState.peaks.find { it.name == placeName }
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
                                        val filtered =
                                            sortFilterValues.applyForSummits(summitsFromDatabase)
                                        withContext(Dispatchers.Main) {
                                            filteredSummits = filtered
                                        }
                                    }
                                }
                            },
                            summits = summitsFromDatabase
                        )
                    }

                    // Show Add Segment Entry Screen when requested (for adding mountain pass from map)
                    if (showAddSegmentEntryScreen && summitForSegmentEntry != null) {
                        val segmentsList by viewModel.segmentsList.asFlow()
                            .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                        val segments = segmentsList.data ?: emptyList()
                        val summit = summitForSegmentEntry!!
                        Dialog(
                            onDismissRequest = {
                                showAddSegmentEntryScreen = false
                                summitForSegmentEntry = null
                            },
                            properties = DialogProperties(
                                dismissOnBackPress = true,
                                dismissOnClickOutside = false,
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                AddSegmentEntryScreen(
                                    segmentId = -1,
                                    segmentEntryId = null,
                                    segments = segments,
                                    summits = summitsFromDatabase,
                                    onSaveSegmentEntry = { isUpdate, segmentEntry ->
                                        viewModel.saveSegmentEntry(isUpdate, segmentEntry)
                                    },
                                    onCancel = {
                                        showAddSegmentEntryScreen = false
                                        summitForSegmentEntry = null
                                    },
                                    preselectedSummit = summit,
                                    hideSummitDropdown = true,
                                    initialStartPointId = 0,
                                    initialEndPointId = -1,
                                    onSaveMountainPass = { mountainPass ->
                                        viewModel.saveMountainPass(mountainPass)
                                        showAddSegmentEntryScreen = false
                                        summitForSegmentEntry = null
                                    }
                                )
                            }
                        }
                    }

                    // Loading panel with status text, progress and cancel
                    LoadingPanel(
                        visible = loadingState.value,
                        statusText = loadingTooltip.value,
                        progressCurrent = loadingProgressCurrent.value,
                        progressTotal = loadingProgressTotal.value,
                        onCancel = if (loadingJob != null) {
                            { loadingJob?.cancel() }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun NavigationDrawerContent(
        onDestinationSelected: (Destination) -> Unit,
        onBookmarksSelected: () -> Unit,
        onShowDiashow: () -> Unit,
        topBarPadding: PaddingValues = PaddingValues(0.dp),
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
                    onShowDiashow()
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
                        peaks = AppState.peaks,
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
                                val peakToRemove = AppState.peaks.find { it.name == placeName }
                                peakToRemove?.let { viewModel.deletePeak(it) }
                            }
                        },
                        onAddSegmentEntry = { summit ->
                            summitForSegmentEntry = summit
                            showAddSegmentEntryScreen = true
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
                    filteredSummits = filteredSummits
                )
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
                        downloadSelectedSummits(selectedSummits, isMerge, coroutineScope)
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
                    onSharedPreferenceChanged(AppState.sharedPreferences, key)
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
            val executor = GarminPythonExecutor.instance
            if (executor != null) {
                loadingState.value = true
                loadingTooltip.value = getString(R.string.update_3rd_part)
                val updater = GarminDataUpdater(
                    AppState.sharedPreferences,
                    executor,
                    repository
                )
                val result = withContext(Dispatchers.IO) {
                    updater.update()
                }
                loadingState.value = false
                when (result) {
                    is GarminSyncResult.Success -> {
                        updater.persistSyncWindow()
                        val message = getString(
                            if (result.hasNewActivities) {
                                R.string.update_done_new_summits
                            } else {
                                R.string.update_done
                            }
                        )
                        snackbarHostState?.showSnackbar(
                            message,
                            duration = SnackbarDuration.Long
                        )
                        if (result.hasNewActivities) {
                            currentDestination = Destination.NewSummits
                        }
                    }
                    is GarminSyncResult.Failed -> {
                        val retry = snackbarHostState?.showSnackbar(
                            getString(R.string.garmin_connect_failed, result.message ?: ""),
                            actionLabel = getString(R.string.retry),
                            duration = SnackbarDuration.Long
                        )
                        if (retry == SnackbarResult.ActionPerformed) {
                            updateThirdPartyData(scope)
                        }
                    }
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

    private fun downloadSelectedSummits(
        selectedSummits: List<Summit>,
        isMerge: Boolean,
        scope: CoroutineScope
    ) {
        val job = scope.launch {
            loadingState.value = true
            val finalSummits = mutableListOf<Summit>()
            try {
                if (isMerge) {
                    loadingTooltip.value = getString(
                        R.string.tool_tip_progress_new_garmin_activities,
                        selectedSummits.joinToString(", ") { it.name }
                    )
                    executeDownload(selectedSummits)?.let { finalSummits.add(it) }
                } else {
                    loadingProgressTotal.value = selectedSummits.size
                    selectedSummits.forEachIndexed { index, summit ->
                        loadingProgressCurrent.value = index + 1
                        loadingTooltip.value = getString(
                            R.string.tool_tip_progress_new_garmin_activities,
                            summit.name
                        )
                        executeDownload(listOf(summit))?.let { finalSummits.add(it) }
                    }
                }
                if (finalSummits.isNotEmpty()) {
                    viewModel.saveSummits(finalSummits).join()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivityCompose,
                            getString(R.string.add_new_summit_successful),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    snackbarHostState?.showSnackbar(
                        getString(R.string.loading_canceled),
                        duration = SnackbarDuration.Short
                    )
                }
                throw e
            } finally {
                resetLoadingState()
            }
        }
        loadingJob = job
    }

    private suspend fun executeDownload(summits: List<Summit>): Summit? {
        val downloader = GarminTrackAndDataDownloader(
            summits, GarminPythonExecutor.instance, AppState.sharedPreferences.getBoolean(Keys.PREF_DOWNLOAD_TCX, false)
        )
        return try {
            withContext(Dispatchers.IO) {
                downloader.extractFinalSummit()
                if (downloader.finalEntry?.sportType != SportType.IndoorTrainer) {
                    downloader.downloadTracks()
                    downloader.composeFinalTrack()
                }
                downloader.finalEntry
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            Log.e(
                "MainActivity",
                "Connecting to third party provider failed. Please try again later. Error: ${e.message}",
                e
            )
            val retry = withContext(Dispatchers.Main) {
                snackbarHostState?.showSnackbar(
                    getString(R.string.garmin_connect_failed, e.message ?: ""),
                    actionLabel = getString(R.string.retry),
                    duration = SnackbarDuration.Long
                )
            }
            if (retry == SnackbarResult.ActionPerformed) {
                return executeDownload(summits)
            }
            null
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
                    AppState.sharedPreferences.getBoolean(Keys.PREF_EXPORT_THIRD_PARTY_DATA, true)
                val exportCalculatedData =
                    AppState.sharedPreferences.getBoolean(Keys.PREF_EXPORT_CALCULATED_DATA, true)
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
        val exportJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
            val summitsListDataStatus = viewModel.summitsList.asFlow().first()
            val forecastListDataStatus = viewModel.forecastList.asFlow().first()
            val segmentsListDataStatus = viewModel.segmentsList.asFlow().first()
            val entityEventsDataStatus = viewModel.entityEvents.asFlow().first()

            loadingState.value = true
            loadingTooltip.value = getString(R.string.exporting_backup)
            try {
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
                val targetUri = resultData?.data
                if (targetUri == null) {
                    throw IllegalStateException(getString(R.string.export_no_target))
                }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(targetUri)?.let {
                        writer.writeToZipFile(it)
                        it.close()
                    } ?: throw IllegalStateException(getString(R.string.export_no_target))
                }
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MainActivityCompose", "Export failed", e)
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivityCompose)
                    .setTitle(getString(R.string.export_failed_title))
                    .setMessage(getString(R.string.export_failed, e.message ?: ""))
                    .setPositiveButton(R.string.accept) { _: DialogInterface?, _: Int -> }
                    .setIcon(android.R.drawable.ic_dialog_alert).show()
            } finally {
                resetLoadingState()
            }
        }
        loadingJob = exportJob
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
        val importJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
            val summitsListDataStatus = viewModel.summitsList.asFlow().first()
            val forecastListDataStatus = viewModel.forecastList.asFlow().first()
            val segmentsListDataStatus = viewModel.segmentsList.asFlow().first()

            loadingState.value = true
            loadingTooltip.value = getString(R.string.importing_backup)
            val newSummitsToSave = mutableListOf<Summit>()
            val updatedSummits = mutableListOf<Summit>()
            val summits = summitsListDataStatus.data?.toMutableList() ?: mutableListOf()
            val forecasts =
                forecastListDataStatus.data?.toMutableList() ?: mutableListOf()
            val segments =
                segmentsListDataStatus.data?.toMutableList() ?: mutableListOf()

            var successfulImports = 0
            var unsuccessfulImports = 0
            var duplicateImports = 0

            try {
                withContext(Dispatchers.IO) {
                    val reader = ZipFileReader(
                        File(cacheDir, "ZipFileReader_${Date().time}"),
                        summits,
                        forecasts,
                        segments
                    )
                    // Collect summits during import so they can be saved in one
                    // batch afterwards instead of triggering N flow re-emissions
                    reader.saveSummit = { isEdit, summit ->
                        if (isEdit) {
                            synchronized(updatedSummits) { updatedSummits.add(summit) }
                        } else {
                            synchronized(newSummitsToSave) { newSummitsToSave.add(summit) }
                        }
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
                        reader.extractAndImport(inputStream) { current, total ->
                            loadingProgressCurrent.value = current
                            loadingProgressTotal.value = total
                        }
                        successfulImports = reader.successful
                        unsuccessfulImports = reader.unsuccessful
                        duplicateImports = reader.duplicate
                    }
                    reader.cleanUp()
                }
                // Batch save in one transaction, then wait until the rows are committed
                viewModel.saveSummits(newSummitsToSave).join()
                if (updatedSummits.isNotEmpty()) {
                    viewModel.updateSummits(updatedSummits).join()
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
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    snackbarHostState?.showSnackbar(
                        getString(R.string.loading_canceled),
                        duration = SnackbarDuration.Short
                    )
                }
                throw e
            } finally {
                resetLoadingState()
            }
        }
        loadingJob = importJob
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
            openViewer(AppState.latestFilteredSummits)
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
                AppState.sharedPreferences.getBoolean(Keys.PREF_CURRENT_YEAR_SWITCH, false)
            )
            viewModel.refresh()
        }
        if (key == Keys.PREF_GARMIN_USERNAME || key == Keys.PREF_GARMIN_PASSWORD || key == Keys.PREF_GARMIN_MFA) {
            updatePythonExecutor()
        }
    }

    private fun updatePythonExecutor() {
        val currentExecutor = GarminPythonExecutor.instance
        if (currentExecutor == null || currentExecutor.username == "" || currentExecutor.password == "") {
            val username = AppState.sharedPreferences.getString(Keys.PREF_GARMIN_USERNAME, "") ?: ""
            val password = AppState.sharedPreferences.getString(Keys.PREF_GARMIN_PASSWORD, "") ?: ""
            val garminMfaSwitch = AppState.sharedPreferences.getBoolean(Keys.PREF_GARMIN_MFA, false)
            val oauthPath = File(AppState.storage?.absolutePath, ".garminconnect")
            if (oauthPath.exists()) {
                GarminPythonExecutor.instance = GarminPythonExecutor(username, password)
            } else if (username != "" && password != "" && garminMfaSwitch) {
                val intent = Intent(this, PythonActivity::class.java)
                startActivity(intent)
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

        AppState.activitiesWithPowerRecordsFiltered =
            getSummitIdsWithPowerRecord(filteredSummits)
        val calendar = Calendar.getInstance()
        // Move calendar back 5 years from today
        calendar.add(Calendar.YEAR, -5)
        AppState.activitiesWithPowerRecordsLast5Years = getSummitIdsWithPowerRecord(
            allSummits.filter { it.date.after(calendar.time) })
        AppState.activitiesWithPowerRecordsAll = getSummitIdsWithPowerRecord(allSummits)

        // Calculate vertical velocity records
        AppState.activitiesWithVerticalVelocityRecordsFiltered =
            getSummitIdsWithVerticalVelocityRecord(filteredSummits)
        AppState.activitiesWithVerticalVelocityRecordsLast5Years = getSummitIdsWithVerticalVelocityRecord(
            allSummits.filter { it.date.after(calendar.time) })
        AppState.activitiesWithVerticalVelocityRecordsAll =
            getSummitIdsWithVerticalVelocityRecord(allSummits)

        // Calculate average velocity records
        AppState.activitiesWithAverageVelocityRecordsFiltered =
            getSummitIdsWithAverageVelocityRecord(filteredSummits)
        AppState.activitiesWithAverageVelocityRecordsLast5Years = getSummitIdsWithAverageVelocityRecord(
            allSummits.filter { it.date.after(calendar.time) })
        AppState.activitiesWithAverageVelocityRecordsAll = getSummitIdsWithAverageVelocityRecord(allSummits)

        if (!segments.isEmpty()) {
            allSummits.forEach { summit ->
                summit.updateSegmentInfo(segments)
            }


            allSummits.forEach { summit ->
                if (summit.segmentInfo.isNotEmpty()) {
                    val position = summit.segmentInfo.minOf { it.third }
                    if (position in 1..3) {
                        AppState.activitiesWithSegmentsRecord.add(
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

    private fun getSummitIdsWithVerticalVelocityRecord(
        summits: List<Summit>,
    ): List<Long> {
        return TimeIntervalVerticalVelocity.entries.filter { it.relevantForRecords }
            .mapNotNull { interval ->
                summits.filter { interval.value(it) > 0.0 }
                    .maxByOrNull { interval.value(it) }?.activityId
            }
    }

    private fun getSummitIdsWithAverageVelocityRecord(
        summits: List<Summit>,
    ): List<Long> {
        return DistanceIntervalVelocity.entries.mapNotNull { interval ->
            summits.filter { interval.value(it) > 0.0 }
                .maxByOrNull { interval.value(it) }?.activityId
        }
    }

    companion object {
        private const val KEY_IS_DIALOG_SHOWN = "IS_DIALOG_SHOWN"
        private const val KEY_CURRENT_POSITION = "CURRENT_POSITION"

        private const val STATE_DESTINATION = "STATE_DESTINATION"
        private const val STATE_SHOW_BOOKMARKS_ONLY = "STATE_SHOW_BOOKMARKS_ONLY"
        private const val STATE_SHOW_SORT_AND_FILTER_DIALOG = "STATE_SHOW_SORT_AND_FILTER_DIALOG"
        private const val STATE_SHOW_ADD_SUMMIT_DIALOG = "STATE_SHOW_ADD_SUMMIT_DIALOG"
        private const val STATE_NEW_SUMMITS_DATE = "STATE_NEW_SUMMITS_DATE"
        private const val STATE_SUMMIT_FOR_SEGMENT_ENTRY = "STATE_SUMMIT_FOR_SEGMENT_ENTRY"
        private const val STATE_SHOW_ADD_SEGMENT_ENTRY_SCREEN = "STATE_SHOW_ADD_SEGMENT_ENTRY_SCREEN"
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