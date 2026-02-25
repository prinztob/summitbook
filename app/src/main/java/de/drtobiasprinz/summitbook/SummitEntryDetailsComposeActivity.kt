package de.drtobiasprinz.summitbook

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.chaquo.python.Python
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.sharedPreferences
import de.drtobiasprinz.summitbook.ui.compose.SummitEntryDataScreen
import de.drtobiasprinz.summitbook.ui.compose.SummitEntryImagesScreen
import de.drtobiasprinz.summitbook.ui.compose.SummitEntryPowerScreen
import de.drtobiasprinz.summitbook.ui.compose.SummitEntryThirdPartyScreen
import de.drtobiasprinz.summitbook.ui.compose.SummitEntryTrackScreen
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.utils.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Compose-based activity for displaying summit entry details with tabs.
 *
 * NOTE: This is a TEMPLATE/SKELETON implementation. The fragments are currently shown as
 * placeholder screens. To complete the migration, each fragment needs to be converted to
 * a proper Compose screen that reads data from the PageViewModel.
 *
 * For now, use the original SummitEntryDetailsActivity for full functionality.
 * This Compose version demonstrates the structure and can be gradually completed.
 */
@AndroidEntryPoint
class SummitEntryDetailsComposeActivity : ComponentActivity() {

    lateinit var pageViewModel: PageViewModel
    private var summitEntry: Summit? = null
    private var hasLoadedSummit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pageViewModel = ViewModelProvider(this)[PageViewModel::class.java]
        CustomMapViewToAllowScrolling.setOsmConfForTiles()

        val summitEntryId = intent.extras?.getLong(SUMMIT_ID_EXTRA_IDENTIFIER)
        Log.i("SummitEntryDetails", "Summit entry ID from intent: $summitEntryId")
        if (summitEntryId != null) {
            pageViewModel.getSummitToView(summitEntryId)
        }
        setContent {
            val coroutineScope = rememberCoroutineScope()
            SummitBookTheme {
                SummitEntryDetailsScreen(
                    pageViewModel = pageViewModel,
                    onBackPressed = { finish() },
                    onSummitLoaded = { summit ->
                        if (!hasLoadedSummit) {
                            Log.i("SummitEntryDetails", "Loading summit: ${summit.activityId}")
                            hasLoadedSummit = true
                            summitEntry = summit
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    pythonInstance?.let { analyzeAndSimplifyTrack(it, summit) }
                                }
                            }
                        }
                    })
            }
        }
    }

    private fun analyzeAndSimplifyTrack(pythonInstance: Python, summit: Summit) {
        val useSimplifiedTracks = sharedPreferences.getBoolean("pref_use_simplified_tracks", true)

        if (summit.sportType == SportType.IndoorTrainer) {
            return
        }

        Log.i(
            "SummitEntryDetails", "Summit has GPS track: ${summit.hasGpsTrack()}, simplified: ${
                summit.hasGpsTrack(simplified = true)
            }"
        )

        if (useSimplifiedTracks && summit.hasGpsTrack() && !summit.hasGpsTrack(simplified = true) && summit.sportType != SportType.IndoorTrainer) {
            try {
                GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(
                    summit.getGpsTrackPath(simplified = false)
                )
                Log.i(
                    "SummitEntryDetails",
                    "Successfully simplified track for ${summit.getDateAsString()}_${summit.name}."
                )
            } catch (ex: RuntimeException) {
                Log.e(
                    "SummitEntryDetails",
                    "Error in simplify track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}",
                    ex
                )
            }
        }

        if (useSimplifiedTracks && !summit.hasTrackData() && summit.sportType != SportType.IndoorTrainer) {
            try {
                GpxPyExecutor(pythonInstance).analyzeGpxTrackAndCreateGpxPyDataFile(summit)
                Log.i(
                    "SummitEntryDetails",
                    "Analyzed track for ${summit.getDateAsString()}_${summit.name}."
                )
            } catch (ex: RuntimeException) {
                Log.e(
                    "SummitEntryDetails",
                    "Error in analyze track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}",
                    ex
                )
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        summitEntry?.let {
            outState.putLong(SUMMIT_ID_EXTRA_IDENTIFIER, it.id)
        }
    }
}

/**
 * Immutable data class to hold stable summit information for UI.
 * This prevents unnecessary recompositions by ensuring the data is stable.
 */
@Immutable
data class SummitUiState(
    val summitId: Long, val summitName: String, val tabs: List<SummitTab>
)

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummitEntryDetailsScreen(
    pageViewModel: PageViewModel, onBackPressed: () -> Unit, onSummitLoaded: (Summit) -> Unit
) {
    Log.i("SummitEntryDetails", "SummitEntryDetailsScreen recomposing")
    val summitToView by pageViewModel.summitToView.observeAsState()

    // Hoist state: Use stable UI state to prevent recomposition loops
    var summitUiState by remember { mutableStateOf<SummitUiState?>(null) }
    var lastProcessedId by remember { mutableStateOf<Long?>(null) }

    // Process summit only when we get a new one with different ID
    LaunchedEffect(summitToView) {
        Log.i(
            "SummitEntryDetails",
            "LaunchedEffect triggered, summitToView: ${summitToView?.data?.activityId}"
        )
        val newSummit = summitToView?.data
        if (newSummit != null && newSummit.id != lastProcessedId) {
            Log.i("SummitEntryDetails", "Processing new summit: ${newSummit.activityId}")
            lastProcessedId = newSummit.id
            summitUiState = SummitUiState(
                summitId = newSummit.id,
                summitName = newSummit.name,
                tabs = getTabsForSummit(newSummit)
            )
            onSummitLoaded(newSummit)
        }
    }

    Log.i("SummitEntryDetails", "Rendering Scaffold with summit: ${summitUiState?.summitId}")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(summitUiState?.summitName ?: "") }, navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = "Back"
                        )
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }) { paddingValues ->
        if (summitUiState != null) {
            Log.i("SummitEntryDetails", "Showing tabs for summit: ${summitUiState!!.summitId}")
            SummitEntryDetailsTabs(
                summitId = summitUiState!!.summitId,
                tabs = summitUiState!!.tabs,
                pageViewModel = pageViewModel,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Log.i("SummitEntryDetails", "Showing loading indicator")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun SummitEntryDetailsTabs(
    summitId: Long,
    tabs: List<SummitTab>,
    pageViewModel: PageViewModel,
    modifier: Modifier = Modifier
) {
    Log.i(
        "SummitEntryDetails",
        "SummitEntryDetailsTabs recomposing for summit: $summitId, tabs: ${tabs.size}"
    )

    val pagerState = rememberPagerState(
        initialPage = 0, pageCount = { tabs.size })
    Log.i(
        "SummitEntryDetails",
        "Pager state with ${tabs.size} pages, current: ${pagerState.currentPage}"
    )
    val coroutineScope = rememberCoroutineScope()

    // Observe state once at this level and extract data to prevent unnecessary recompositions
    val summitToViewData by pageViewModel.summitToView.observeAsState()
    val summitsListData by pageViewModel.summitsList.observeAsState()
    val summitToCompareData by pageViewModel.summitToCompare.observeAsState()
    val segmentsListData by pageViewModel.segmentsList.observeAsState()
    val extremaValuesSummits by pageViewModel.extremaValuesSummits.observeAsState()

    // Extract actual data from DataStatus wrappers to create stable state
    val summit = remember(summitToViewData) { summitToViewData?.data }
    val allSummits = remember(summitsListData) { summitsListData?.data }
    val compareSummit = remember(summitToCompareData) { summitToCompareData?.data }
    val segments = remember(segmentsListData) { segmentsListData?.data }

    // Create stable callbacks
    val onGetSummitToCompare: (Long) -> Unit = remember {
        { id -> pageViewModel.getSummitToCompare(id) }
    }
    val onSetSummitToCompareToNull: () -> Unit = remember {
        { pageViewModel.setSummitToCompareToNull() }
    }

    Log.i("SummitEntryDetails", "Rendering Column for tabs with ${tabs.size} tabs")
    Column(modifier = modifier.fillMaxSize()) {
        Log.i(
            "SummitEntryDetails",
            "Rendering PrimaryTabRow with ${tabs.size} tabs, current page: ${pagerState.currentPage}"
        )
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            tabs.forEachIndexed { index, tab ->
                Log.i("SummitEntryDetails", "Rendering tab $index: ${tab.name}")
                Tab(selected = pagerState.currentPage == index, onClick = {
                    Log.i("SummitEntryDetails", "Clicking tab $index: ${tab.name}")
                    coroutineScope.launch(Dispatchers.Main.immediate) {
                        pagerState.animateScrollToPage(index)
                    }
                }, text = {
                    Text(
                        text = stringResource(id = tab.titleResId),
                        style = MaterialTheme.typography.labelSmall
                    )
                })
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = tabs.size, // Keep all pages loaded
            key = { page -> "${summitId}_${tabs[page].name}_$page" }) { page ->
            Log.i("SummitEntryDetails", "Rendering page $page for summit: $summitId")
            when (tabs[page]) {
                SummitTab.DATA -> {
                    Log.i("SummitEntryDetails", "Rendering DATA tab for page $page")
                    SummitEntryDataScreen(
                        summit = summit,
                        allSummits = allSummits,
                        compareSummit = compareSummit,
                        segments = segments,
                        extrema = extremaValuesSummits,
                        onGetSummitToCompare = onGetSummitToCompare,
                        onSetSummitToCompareToNull = onSetSummitToCompareToNull
                    )
                }

                SummitTab.THIRD_PARTY -> {
                    Log.i("SummitEntryDetails", "Rendering THIRD_PARTY tab for page $page")
                    SummitEntryThirdPartyScreen(
                        summit = summit,
                        allSummits = allSummits,
                        compareSummit = compareSummit,
                        extrema = extremaValuesSummits,
                        onGetSummitToCompare = onGetSummitToCompare,
                        onSetSummitToCompareToNull = onSetSummitToCompareToNull
                    )
                }

                SummitTab.IMAGES -> {
                    Log.i("SummitEntryDetails", "Rendering IMAGES tab for page $page")
                    SummitEntryImagesScreen(
                        summit = summit
                    )
                }

                SummitTab.TRACK -> {
                    Log.i("SummitEntryDetails", "Rendering TRACK tab for page $page")
                    SummitEntryTrackScreen(
                        summit = summit,
                        allSummits = allSummits,
                        compareSummit = compareSummit,
                        onGetSummitToCompare = onGetSummitToCompare,
                        onSetSummitToCompareToNull = onSetSummitToCompareToNull
                    )
                }

                SummitTab.POWER -> {
                    Log.i("SummitEntryDetails", "Rendering POWER tab for page $page")
                    SummitEntryPowerScreen(
                        summit = summit,
                        allSummits = allSummits,
                        compareSummit = compareSummit,
                        extrema = extremaValuesSummits,
                        onGetSummitToCompare = onGetSummitToCompare,
                        onSetSummitToCompareToNull = onSetSummitToCompareToNull
                    )
                }
            }
        }
    }
}

enum class SummitTab(val titleResId: Int) {
    DATA(R.string.tab_text_0), THIRD_PARTY(R.string.tab_text_1), IMAGES(R.string.tab_text_2), TRACK(
        R.string.tab_text_3
    ),
    POWER(R.string.tab_text_4)
}

fun getTabsForSummit(summit: Summit): List<SummitTab> {
    Log.i("SummitEntryDetails", "Getting tabs for summit: ${summit.activityId}")
    val tabs = mutableListOf(SummitTab.DATA)

    if (summit.garminData != null) {
        tabs.add(SummitTab.THIRD_PARTY)
    }
    if (summit.hasImagePath()) {
        tabs.add(SummitTab.IMAGES)
    }
    if (summit.hasGpsTrack()) {
        tabs.add(SummitTab.TRACK)
    }
    if (summit.garminData?.power != null && summit.garminData?.power?.hasPowerData() == true) {
        tabs.add(SummitTab.POWER)
    }

    return tabs
}
