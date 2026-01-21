package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.chaquo.python.Python
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Peak
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.allSummits
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.peaks
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.updateOfTracksStarted
import de.drtobiasprinz.summitbook.ui.compose.AddSummitDialogCompose
import de.drtobiasprinz.summitbook.ui.compose.SummitsListScreen
import de.drtobiasprinz.summitbook.ui.observeOnce
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import de.drtobiasprinz.summitbook.ui.utils.TimeIntervalPower
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SummitViewFragment : Fragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    val viewModel: DatabaseViewModel? by activityViewModels()
    private lateinit var sharedPreferences: SharedPreferences

    var showBookmarksOnly = false

    // State flows for Compose UI
    private val _summitsState = MutableStateFlow<List<Summit>>(emptyList())
    private val summitsState: StateFlow<List<Summit>> = _summitsState.asStateFlow()

    private val _isLoadingState = MutableStateFlow(false)
    private val isLoadingState: StateFlow<Boolean> = _isLoadingState.asStateFlow()

    private val _isEmptyState = MutableStateFlow(false)
    private val isEmptyState: StateFlow<Boolean> = _isEmptyState.asStateFlow()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                SummitBookTheme {
                    SummitViewScreen()
                }
            }
        }
    }

    @Composable
    private fun SummitViewScreen() {
        val summits by summitsState.collectAsState()
        val isLoading by isLoadingState.collectAsState()
        val isEmpty by isEmptyState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        var showAddDialog by remember { mutableStateOf(false) }

        @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (showBookmarksOnly) {
                                R.drawable.baseline_bookmark_add_black_24dp
                            } else {
                                R.drawable.baseline_add_photo_alternate_black_24dp
                            }
                        ),
                        contentDescription = stringResource(R.string.add_new_summit),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(150.dp)
                                .align(Alignment.Center)
                        )
                    }

                    isEmpty -> {
                        EmptyListView(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        SummitsListScreen(
                            summits = summits,
                            isBookmark = showBookmarksOnly,
                            onUpdateIsFavorite = { summit ->
                                adapterOnClickUpdateIsFavorite(summit)
                            },
                            onUpdateIsPeak = { summit ->
                                adapterOnClickUpdateIsPeak(summit)
                            },
                            onDelete = { summit ->
                                adapterOnClickDelete(summit, snackbarHostState, coroutineScope)
                            }
                        )
                    }
                }
            }
        }
        
        // Add Summit Dialog
        if (showAddDialog) {
            AddSummitDialogCompose(
                summitId = 0L,
                isBookmark = showBookmarksOnly,
                onDismiss = { showAddDialog = false }
            )
        }
    }

    @Composable
    private fun EmptyListView(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.empty),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    private fun adapterOnClickUpdateIsFavorite(summit: Summit) {
        summit.isFavorite = !summit.isFavorite
        viewModel?.saveSummit(true, summit)
    }

    private fun adapterOnClickUpdateIsPeak(summit: Summit) {
        summit.isPeak = !summit.isPeak
        viewModel?.saveSummit(true, summit)
        if (summit.isPeak && summit.name !in peaks.map { it.name }) {
            viewModel?.savePeak(Peak(summit.name, summit.elevationData.maxElevation))
        }
        if (!summit.isPeak && summit.name in peaks.map { it.name }) {
            viewModel?.deletePeak(Peak(summit.name, summit.elevationData.maxElevation))
        }
    }

    private fun adapterOnClickDelete(
        summit: Summit,
        snackbarHostState: SnackbarHostState,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        viewModel?.deleteSummit(summit)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = String.format(getString(R.string.delete_entry_done), summit.name),
                actionLabel = getString(R.string.delete_undo)
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel?.saveSummit(false, summit)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSummitsData()
    }

    private fun observeSummitsData() {
        if (showBookmarksOnly) {
            viewModel?.getAllBookmarks()
            viewModel?.bookmarksList?.observe(viewLifecycleOwner) {
                when (it.status) {
                    DataStatus.Status.LOADING -> {
                        _isLoadingState.value = true
                        _isEmptyState.value = false
                    }

                    DataStatus.Status.SUCCESS -> {
                        _isLoadingState.value = false
                        _isEmptyState.value = it.isEmpty ?: false
                        val data = sortFilterValues.applyForBookmarks(it.data ?: emptyList())
                        _summitsState.value = data
                    }

                    DataStatus.Status.ERROR -> {
                        _isLoadingState.value = false
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            viewModel?.summitsList?.observe(viewLifecycleOwner) { summitsStatus ->
                when (summitsStatus.status) {
                    DataStatus.Status.LOADING -> {
                        _isLoadingState.value = true
                        _isEmptyState.value = false
                    }

                    DataStatus.Status.SUCCESS -> {
                        _isLoadingState.value = false
                        _isEmptyState.value = summitsStatus.isEmpty ?: false
                        allSummits = summitsStatus.data ?: emptyList()
                        val data = sortFilterValues.apply(
                            summitsStatus.data ?: emptyList(), sharedPreferences
                        )
                        _summitsState.value = data
                        if (!sharedPreferences.getBoolean(Keys.PREF_DEBUG, false)) {
                            setRecordsOnce(summitsStatus.data ?: emptyList(), data)
                            if (!updateOfTracksStarted && summitsStatus.data != null) {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        updateTracks(summitsStatus.data)
                                    }
                                }
                            }
                        }
                        convertPeaks(summitsStatus.data)
                    }

                    DataStatus.Status.ERROR -> {
                        _isLoadingState.value = false
                        Toast.makeText(context, summitsStatus.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private fun convertPeaks(data: List<Summit>?) {
        data?.forEach {
            if (it.isPeak && it.name !in peaks.map { peak -> peak.name }) {
                Log.i(TAG, "convertPeaks - added ${it.name}")
                peaks.add(Peak(it.name))
                viewModel?.savePeak(Peak(it.name, it.elevationData.maxElevation))
            }
        }
    }

    private fun setRecordsOnce(allSummits: List<Summit>, filteredSummits: List<Summit>) {
        Log.i(
            TAG, "setRecordsOnce - records will be added for ${filteredSummits.size} summits."
        )

        MainActivity.activitiesWithPowerRecordsFiltered =
            getSummitIdsWithPowerRecord(filteredSummits)
        val calendar = Calendar.getInstance()
        // Move calendar back 5 years from today
        calendar.add(Calendar.YEAR, -5)
        MainActivity.activitiesWithPowerRecordsLast5Years = getSummitIdsWithPowerRecord(
            allSummits.filter { it.date.after(calendar.time) })
        MainActivity.activitiesWithPowerRecordsAll = getSummitIdsWithPowerRecord(allSummits)

        viewModel?.segmentsList?.observeOnce(viewLifecycleOwner) { itDataSegments ->
            itDataSegments.data.let { segments ->
                if (!segments.isNullOrEmpty()) {
                    allSummits.forEach { summit ->
                        summit.updateSegmentInfo(segments)
                    }
                }
            }
            allSummits.forEach { summit ->
                if (summit.segmentInfo.isNotEmpty()) {
                    val position = summit.segmentInfo.minOf { it.third }
                    if (position in 1..3) {
                        MainActivity.activitiesWithSegmentsRecord.add(
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
            summits.maxByOrNull { interval.value(it) }?.activityId
        }
    }


    private fun updateTracks(summits: List<Summit>) {
        updateOfTracksStarted = true
        val useSimplifiedTracks =
            MainActivity.sharedPreferences.getBoolean(Keys.PREF_USE_SIMPLIFIED_TRACKS, true)
        if (useSimplifiedTracks) {
            simplifyTracks(summits)
        } else {
            summits.filter {
                it.hasGpsTrack(simplified = true)
            }.forEach {
                val trackFile = it.getGpsTrackPath(simplified = true).toFile()
                if (trackFile.exists()) {
                    trackFile.delete()
                }
                val gpxPyFile = it.getGpxPyPath().toFile()
                if (gpxPyFile.exists()) {
                    gpxPyFile.delete()
                }
                Log.e(
                    TAG,
                    "updateTracks - deleted ${it.getDateAsString()}_${it.name} because useSimplifiedTracks was set to false."
                )
            }
        }
        val analyzer = OfflineMapAnalyzer.from(requireContext())
        val summitsForDistanceCalc = summits.filter {
            OfflineMapAnalyzer.isDistancePerSurfacesAndRoadTypePossible(analyzer, it)
        }.sortedByDescending { it.date }.take(25)
        Log.i(
            TAG,
            "updateTracks - setDistancePerSurfacesAndRoadType for ${summitsForDistanceCalc.size} summits."
        )
        val summitsToUpdate = summitsForDistanceCalc.filter {
            Log.i(
                TAG,
                "updateTracks - setDistancePerSurfacesAndRoadType for summit ${it.getDateAsString()}_${it.name}."
            )
            try {
                OfflineMapAnalyzer.setDistancePerSurfacesAndRoadType(requireContext(), it)
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "updateTracks - setDistancePerSurfacesAndRoadType for summit ${it.getDateAsString()}_${it.name} failed with ${e.message}."
                )
                false
            }
        }
        viewModel?.updateSummitDistanceDataBatch(summitsToUpdate)
        Log.i(
            TAG, "updateTracks - setDistancePerSurfacesAndRoadType done."
        )
    }

    private fun simplifyTracks(summits: List<Summit>) {
        summits.forEach {
            if (it.ignoreSimplifyingTrack) {
                Log.w(
                    TAG,
                    "simplifyTracks - Track ${it.getDateAsString()} ${it.name} (${it.getGpsTrackPath()}) " + "will not be simplified, because it failed before"
                )
            }
        }
        val entriesWithoutSimplifiedGpxTrack = summits.filter {
            it.hasGpsTrack() && !it.ignoreSimplifyingTrack && !it.hasGpsTrack(simplified = true) && it.sportType != SportType.IndoorTrainer
        }.sortedByDescending { it.date }

        val entriesWithoutAdditionalData = if (entriesWithoutSimplifiedGpxTrack.size < 50) {
            summits.filter {
                it.hasGpsTrack() && !it.ignoreSimplifyingTrack && (!it.getYamlExtensionsFile()
                    .exists() || !it.getGpxPyPath().toFile()
                    .exists()) && it.sportType != SportType.IndoorTrainer
            }.sortedByDescending { it.date }.take(51 - entriesWithoutSimplifiedGpxTrack.size)
        } else {
            emptyList()
        }
        pythonInstance?.let {
            asyncSimplifyGpsTracks(
                entriesWithoutSimplifiedGpxTrack.take(100), entriesWithoutAdditionalData, it
            )
        }
    }

    private fun asyncSimplifyGpsTracks(
        summitsWithoutSimplifiedTracks: List<Summit>,
        summitsWithoutAdditionalData: List<Summit>,
        pythonInstance: Python
    ) {
        var numberSimplifiedGpxTracks = 0
        if (summitsWithoutSimplifiedTracks.isNotEmpty()) {
            summitsWithoutSimplifiedTracks.forEachIndexed { i, e ->
                try {
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Simplifying track ${i + 1} of ${summitsWithoutSimplifiedTracks.size} for ${e.getDateAsString()}_${e.name}."
                    )
                    GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(
                        e.getGpsTrackPath(),
                    )
                    numberSimplifiedGpxTracks += 1
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Simplified track for ${e.getDateAsString()}_${e.name}."
                    )
                } catch (ex: RuntimeException) {
                    Log.e(
                        TAG,
                        "asyncSimplifyGpsTracks - Error in simplify track for ${e.getDateAsString()}_${e.name}: ${ex.message}"
                    )
                    e.ignoreSimplifyingTrack = true
                    viewModel?.updateIgnoreSimplifyingTrack(e.id, true)
                }
            }
        } else if (summitsWithoutAdditionalData.isNotEmpty()) {
            summitsWithoutAdditionalData.forEachIndexed { i, e ->
                try {
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Calculate additional data for  $i of ${summitsWithoutAdditionalData.size} for ${e.getDateAsString()}_${e.name}."
                    )
                    GpxPyExecutor(pythonInstance).analyzeGpxTrackAndCreateGpxPyDataFile(e)
                    Log.i(
                        TAG,
                        "asyncSimplifyGpsTracks - Calculated additional data for ${e.getDateAsString()}_${e.name}."
                    )
                } catch (ex: RuntimeException) {
                    Log.e(
                        TAG,
                        "asyncSimplifyGpsTracks - Error in simplify track for ${e.getDateAsString()}_${e.name}: ${ex.message}"
                    )
                    e.ignoreSimplifyingTrack = true
                    viewModel?.updateIgnoreSimplifyingTrack(e.id, true)
                }
            }
        } else {
            Log.i(TAG, "asyncSimplifyGpsTracks - No more gpx tracks to simplify.")
        }
    }

    companion object {
        const val TAG = "SummitViewFragment"
    }

}
