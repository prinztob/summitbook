package de.drtobiasprinz.summitbook.ui

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.PythonActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Peak
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
import de.drtobiasprinz.summitbook.ui.utils.ZipFileReader
import de.drtobiasprinz.summitbook.ui.utils.ZipFileWriter
import de.drtobiasprinz.summitbook.utils.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.mapsforge.MapsForgeTileSource
import java.io.File
import java.time.LocalDate
import java.util.Date
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
    private var boundingBoxUpdaterTriggered: Boolean = false

    // Navigation state
    private var currentDestination by mutableStateOf(Destination.Summits)
    private var showBookmarksOnly by mutableStateOf(false)

    // Dialog states
    private var showSortAndFilterDialog by mutableStateOf(false)
    private var showNewSummitsDialog by mutableStateOf(false)
    private var showAddSummitDialog by mutableStateOf(false)
    private var newSummitsSelectedDate by mutableStateOf<Date?>(null)

    // Drawer state
    private val drawerState = mutableStateOf(false)

    // Loading state
    private val loadingState = mutableStateOf(false)
    private val loadingTooltip = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Python if needed
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Initialize shared preferences
        sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updatePythonExecutor()
        pythonInstance = Python.getInstance()
        storage = applicationContext.filesDir
        cache = applicationContext.cacheDir
        activitiesDir = File(storage, "activities")
        segmentScreenshotDir = File(storage, "segmentScreenshots")
        segmentScreenshotDir?.mkdirs()

        // Configure MapsForge settings
        MapsForgeTileSource.createInstance(application)

        // Set the Compose content
        setContent {
            SummitBookTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
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

        // Update forecasts if needed
        LaunchedEffect(summitsList) {
            summitsList.data?.let {
                summitsFromDatabase = it
                filteredSummits = sortFilterValues.applyForSummits(it)
            }
        }
        updateBoundingBoxInternal(coroutineScope, summitsFromDatabase)

        LaunchedEffect(forecastList) {
            forecastList.data?.let {
                forecasts = it
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
                        coroutineScope.launch {
                            drawerState.close()
                        }
                    },
                    onBookmarksSelected = {
                        currentDestination = Destination.Summits
                        showBookmarksOnly = true
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
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
                            var searchText by remember { mutableStateOf("") }
                            var isSearching by remember { mutableStateOf(false) }

                            if (isSearching) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = {
                                        searchText = it
                                        sortFilterValues.searchString = it
                                        filteredSummits = sortFilterValues.applyForSummits(
                                            summitsFromDatabase
                                        )
                                    },
                                    placeholder = { Text(stringResource(R.string.search)) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            isSearching = false
                                            searchText = ""
                                            sortFilterValues.searchString = ""
                                            filteredSummits = sortFilterValues.applyForSummits(
                                                summitsFromDatabase
                                            )
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.baseline_cancel_24),
                                                contentDescription = "Close search"
                                            )
                                        }
                                    }
                                )
                            } else {
                                IconButton(onClick = { isSearching = true }) {
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
                    MainContent(filteredSummits, summitsFromDatabase, forecasts)

                    // Floating Action Button for adding a summit (only visible on Summits screen)
                    if (currentDestination == Destination.Summits && !showBookmarksOnly) {
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
                            onDismiss = { showAddSummitDialog = false },
                            onSaveSummit = { isEdit, summit ->
                                viewModel.saveSummit(
                                    isEdit,
                                    summit
                                )
                            }
                        )
                    }

                    // Show Sort and Filter Dialog when requested
                    if (showSortAndFilterDialog) {
                        SortAndFilterDialogCompose(
                            sortFilterValues = sortFilterValues,
                            onDismiss = { showSortAndFilterDialog = false },
                            onApply = {
                                filteredSummits = sortFilterValues.applyForSummits(
                                    summitsFromDatabase
                                )
                            },
                            summits = summitsFromDatabase
                        )
                    }

                    if (showNewSummitsDialog) {
                        ShowNewSummitsFromGarminScreen(
                            viewModel = viewModel,
                            summits = summitsList.data ?: emptyList(),
                            selectedDate = newSummitsSelectedDate,
                            onBack = { selectedSummits, isMerge ->
                                showNewSummitsDialog = false
                                // Execute download for selected summits
                                if (isMerge) {
                                    executeDownload(selectedSummits, coroutineScope)
                                } else {
                                    selectedSummits.forEach {
                                        executeDownload(listOf(it), coroutineScope)
                                    }
                                }
                            },
                            onRefresh = {
                                updateThirdPartyData(coroutineScope)
                            }
                        )
                    }

                    // Loading indicator
                    if (loadingState.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
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
        onBookmarksSelected: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                onClick = { openViewer() },
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
                        painter = painterResource(R.drawable.baseline_add_location_alt_black_24dp),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(R.string.add_new_summit)) },
                selected = false,
                onClick = { showAddSummitDialog() },
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
                selected = false,
                onClick = { showNewSummitsDialog() },
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
                onClick = { showExportCsvDialog() },
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

    @Composable
    fun MainContent(
        filteredSummits: List<Summit>,
        summitFromDatabase: List<Summit>,
        forecasts: List<Forecast>,
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
                            forecasts = forecasts,
                            sortFilterValues = sortFilterValues
                        )
                    }

                    SummitsListScreen(
                        summits = if (showBookmarksOnly) summitFromDatabase.filter { it.isBookmark } else filteredSummits,
                        isBookmark = showBookmarksOnly,
                        onSaveSummit = { isEdit, summit -> viewModel.saveSummit(isEdit, summit) },
                        onUpdateIsFavorite = { summit ->
                            val updatedSummit = summit.clone()
                            updatedSummit.isFavorite = !summit.isFavorite
                            viewModel.saveSummit(true, updatedSummit)
                        },
                        onUpdateIsPeak = { summit ->
                            val updatedSummit = summit.clone()
                            updatedSummit.isPeak = !summit.isPeak
                            viewModel.saveSummit(true, updatedSummit)
                        },
                        onDelete = { summit ->
                            viewModel.deleteSummit(summit)
                        }
                    )
                }
            }

            Destination.Overview -> {
                OverviewScreen(
                    filteredSummits,
                    forecasts,
                    sortFilterValues
                )
            }

            Destination.Routes -> {
                val segmentsList by viewModel.segmentsList.asFlow()
                    .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                SegmentsListScreen(
                    segments = segmentsList.data ?: emptyList(),
                    summits = summitFromDatabase,
                    onDeleteSegment = { segment ->
                        viewModel.deleteSegment(segment)
                    },
                    onDeleteSegmentEntry = { entry ->
                        viewModel.deleteSegmentEntry(entry)
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
                BarChartScreen(filteredSummits, forecasts, dailyActivitySummary.data ?: emptyList())
            }

            Destination.Map -> {
                OpenStreetMapScreen(filteredSummits, summitFromDatabase.filter { it.isBookmark })
            }

            Destination.Forecast -> {
                ForecastScreen(
                    summitFromDatabase,
                    forecasts as MutableList<Forecast>,
                    { currentDestination = Destination.Summits },
                    { isEdit, forecasts -> viewModel.saveForecasts(isEdit, forecasts) })
            }

            Destination.AdditionalData -> {
                val entityEvents by viewModel.entityEvents.asFlow()
                    .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
                SummitEntitiesScreen(
                    filteredSummits,
                    entityEvents.data ?: emptyList(),
                    { isEdit, summit -> viewModel.saveSummit(isEdit, summit) },
                    { viewModel.deleteEntityEvent(it) },
                    { isEdit, event -> viewModel.saveEntityEvent(isEdit, event) }
                )
            }

            Destination.Settings -> {
                SettingsScreen(summitFromDatabase, { key ->
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
        scope.launch() {
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
                    showNewSummitsDialog()
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

    private fun updateBoundingBoxInternal(scope: CoroutineScope, summits: List<Summit>) {
        if (!boundingBoxUpdaterTriggered && summits.isNotEmpty()) {
            boundingBoxUpdaterTriggered = true
            scope.launch {
                loadingState.value = true
                withContext(Dispatchers.IO) {
                    updateTracksAndBoundingBox(summits)
                }
            }
        }
    }

    private fun updateTracksAndBoundingBox(summits: List<Summit>) {
        if (summits.isNotEmpty()) {
            val entriesWithoutBoundingBox = summits.filter {
                it.hasGpsTrack() && it.trackBoundingBox == null && it !in entriesToExcludeForBoundingBoxCalculation
            }
            if (entriesWithoutBoundingBox.isNotEmpty()) {
                updateBoundingBox(entriesWithoutBoundingBox)
            }
            Log.i(
                "Scheduler", "No more bounding boxes to update."
            )
            loadingState.value = false
        }
    }

    private fun updateBoundingBox(entriesWithoutBoundingBox: List<Summit>) {
        val entriesToCheck = entriesWithoutBoundingBox.take(250)
        entriesToCheck.forEachIndexed { index, entryToCheck ->
            entryToCheck.setBoundingBoxFromTrack()
            if (entryToCheck.trackBoundingBox != null) {
                viewModel.saveSummit(true, entryToCheck)
                Log.i(
                    "Scheduler",
                    "Updated bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name}, " + "${entriesWithoutBoundingBox.size - index} remaining."
                )
            } else {
                Log.i(
                    "Scheduler",
                    "Updated bounding box for ${entryToCheck.getDateAsString()}_${entryToCheck.name} failed, remove it from update list."
                )
                entriesToExcludeForBoundingBoxCalculation.add(entryToCheck)
            }
        }
    }

    private fun executeDownload(summits: List<Summit>, scope: CoroutineScope) {
        val downloader = GarminTrackAndDataDownloader(
            summits, pythonExecutor, sharedPreferences.getBoolean(Keys.PREF_DOWNLOAD_TCX, false)
        )
        loadingState.value = true
        loadingTooltip.value = getString(
            R.string.tool_tip_progress_new_garmin_activities,
            summits.joinToString(", ") { it.name })
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    downloader.extractFinalSummit()
                    if (downloader.finalEntry?.sportType != SportType.IndoorTrainer) {
                        downloader.downloadTracks()
                        downloader.composeFinalTrack()
                    }
                }
            } catch (e: RuntimeException) {
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
            downloader.updateFinalEntry(viewModel)
            loadingState.value = false
            loadingTooltip.value = ""
            Toast.makeText(
                this@MainActivityCompose,
                getString(R.string.add_new_summit_successful),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun showNewSummitsDialog(selectedDate: Date? = null) {
        newSummitsSelectedDate = selectedDate
        showNewSummitsDialog = true
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

    private fun getAllImages(summits: List<Summit>?): MutableList<Poster> {
        return summits?.map { entry ->
            entry.imageIds.mapIndexed { i, imageId ->
                Poster(
                    entry.getImageUrl(imageId), entry.getImageDescription(resources, i)
                )
            }
        }?.flatten() as MutableList<Poster>
    }

    private fun openViewer() {
        // Collect data from ViewModels using .asFlow().collectAsStateWithLifecycle pattern
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            val summitsListDataStatus = viewModel.summitsList.asFlow().first()
            val summits = summitsListDataStatus.data ?: emptyList()
            val sortFilterSummits = summits.let { sortFilterValues.applyForSummits(it) }
            val allImages = getAllImages(sortFilterSummits)

            if (fullscreenImageViewer == null) {
                fullscreenImageViewer = FullscreenImageViewer(this@MainActivityCompose, resources)
            }

            val currentPosition = fullscreenImageViewer?.currentPosition ?: 0
            val adjustedPosition = if (allImages.size <= currentPosition) 0 else currentPosition

            if (allImages.isNotEmpty()) {
                Log.i("MainActivity", "showFullscreenImageViewer")
                fullscreenImageViewer?.show(allImages, adjustedPosition, sortFilterSummits)
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
            openViewer()
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
            if (oauthPath.exists()) {
                pythonExecutor = GarminPythonExecutor(username, password)
            } else if (username != "" && password != "" && garminMfaSwitch) {
                val intent = Intent(this, PythonActivity::class.java)
                startActivity(intent)
            }
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

        var entriesToExcludeForBoundingBoxCalculation: MutableList<Summit> = mutableListOf()
        var storage: File? = null
        var cache: File? = null
        var activitiesDir: File? = null
        var segmentScreenshotDir: File? = null
        var pythonInstance: Python? = null
        var pythonExecutor: GarminPythonExecutor? = null
        var activitiesWithPowerRecordsFiltered: List<Long> = emptyList()
        var activitiesWithPowerRecordsLast5Years: List<Long> = emptyList()
        var activitiesWithPowerRecordsAll: List<Long> = emptyList()
        var activitiesWithSegmentsRecord: MutableList<Pair<Long, Int>> = mutableListOf()
        lateinit var sharedPreferences: SharedPreferences
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
    AdditionalData,
    Settings
}