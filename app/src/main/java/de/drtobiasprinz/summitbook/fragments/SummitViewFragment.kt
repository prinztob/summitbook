package de.drtobiasprinz.summitbook.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaquo.python.Python
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentSummitViewBinding
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.allSummits
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.updateOfTracksStarted
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.observeOnce
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.utils.Constants
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.utils.isVisible
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SummitViewFragment : Fragment() {

    @Inject
    lateinit var summitsAdapter: SummitsAdapter
    private lateinit var binding: FragmentSummitViewBinding

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    val viewModel: DatabaseViewModel? by activityViewModels()
    private lateinit var sharedPreferences: SharedPreferences

    var showBookmarksOnly = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummitViewBinding.inflate(layoutInflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return binding.root
    }

    private fun adapterOnClickUpdateIsFavorite(summit: Summit) {
        summit.isFavorite = !summit.isFavorite
        viewModel?.saveSummit(true, summit)
    }

    private fun adapterOnClickUpdateIsPeak(summit: Summit) {
        summit.isPeak = !summit.isPeak
        viewModel?.saveSummit(true, summit)
    }

    private fun adapterOnClickDelete(summit: Summit) {
        viewModel?.deleteSummit(summit)
        Snackbar.make(
            binding.root,
            String.format(getString(R.string.delete_entry_done), summit.name),
            Snackbar.LENGTH_LONG
        )
            .apply {
                setAction(getString(R.string.delete_undo)) {
                    viewModel?.saveSummit(false, summit)
                }
            }.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            if (showBookmarksOnly) {
                btnShowDialog.setImageResource(R.drawable.baseline_bookmark_add_black_24dp)
            } else {
                btnShowDialog.setImageResource(R.drawable.baseline_add_photo_alternate_black_24dp)
            }
            btnShowDialog.setOnClickListener {
                startAddSummitDialog(null)
            }
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = summitsAdapter
                summitsAdapter.onClickDelete = { e -> adapterOnClickDelete(e) }
                summitsAdapter.onClickUpdateIsFavorite = { e -> adapterOnClickUpdateIsFavorite(e) }
                summitsAdapter.onClickUpdateIsPeak = { e -> adapterOnClickUpdateIsPeak(e) }
                summitsAdapter.isBookmark = showBookmarksOnly
            }
            if (showBookmarksOnly) {
                viewModel?.getAllBookmarks()
                viewModel?.bookmarksList?.observe(viewLifecycleOwner) {
                    when (it.status) {
                        DataStatus.Status.LOADING -> {
                            loading.isVisible(true, recyclerView)
                            emptyBody.isVisible(false, recyclerView)
                        }

                        DataStatus.Status.SUCCESS -> {
                            it.isEmpty?.let { isEmpty -> showEmpty(isEmpty) }
                            loading.isVisible(false, recyclerView)
                            val data = sortFilterValues.applyForBookmarks(it.data ?: emptyList())
                            summitsAdapter.differ.submitList(data)
                        }

                        DataStatus.Status.ERROR -> {
                            loading.isVisible(false, recyclerView)
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                viewModel?.summitsList?.observe(viewLifecycleOwner) { summitsStatus ->
                    when (summitsStatus.status) {
                        DataStatus.Status.LOADING -> {
                            loading.isVisible(true, recyclerView)
                            emptyBody.isVisible(false, recyclerView)
                        }

                        DataStatus.Status.SUCCESS -> {
                            setRecordsOnce(summitsStatus.data ?: emptyList())
                            summitsStatus.isEmpty?.let { isEmpty -> showEmpty(isEmpty) }
                            loading.isVisible(false, recyclerView)
                            allSummits = summitsStatus.data ?: emptyList()
                            val data = sortFilterValues.apply(
                                summitsStatus.data ?: emptyList(),
                                sharedPreferences
                            )
                            summitsAdapter.differ.submitList(data)
                            if (!updateOfTracksStarted) {
                                summitsStatus.data?.let { updateTracks(it) }
                            }
                        }

                        DataStatus.Status.ERROR -> {
                            loading.isVisible(false, recyclerView)
                            Toast.makeText(context, summitsStatus.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }


            val swipeCallback = object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.absoluteAdapterPosition
                    val summit = summitsAdapter.differ.currentList[position]
                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            startAddSummitDialog(summit)
                        }

                        ItemTouchHelper.RIGHT -> {
                            startAddSummitDialog(summit)
                        }
                    }
                }

            }

            val itemTouchHelper = ItemTouchHelper(swipeCallback)
            itemTouchHelper.attachToRecyclerView(recyclerView)

        }
    }

    private fun setRecordsOnce(summits: List<Summit>) {
        MainActivity.activitiesWithPowerRecords = mutableListOf()
        MainActivity.activitiesWithSegmentsRecord = mutableListOf()
        val filteredSummits = summits.filter {
            !it.isBookmark && sortFilterValues.filterDate(it)
        }
        Log.i(
            "SummitViewFragment",
            "records will be added for ${filteredSummits.size} summits."
        )
        val maxSummits = TimeIntervalPower.entries.map {
            it.getMaxSummit(
                ExtremaValuesSummits(
                    filteredSummits,
                    excludeZeroValueFromMin = true
                )
            )
        }

        summits.forEach { summit ->
            if (summit in maxSummits) {
                MainActivity.activitiesWithPowerRecords.add(summit.activityId)
            }
        }

        viewModel?.segmentsList?.observeOnce(viewLifecycleOwner) { itDataSegments ->
            itDataSegments.data.let { segments ->
                if (!segments.isNullOrEmpty()) {
                    summits.forEach { summit ->
                        summit.updateSegmentInfo(segments)
                    }
                }
            }
            summits.forEach { summit ->
                if (summit.segmentInfo.isNotEmpty()) {
                    val position = summit.segmentInfo.minOf { it.third }
                    if (position in 1..3) {
                        MainActivity.activitiesWithSegmentsRecord.add(
                            Pair(
                                summit.activityId,
                                position
                            )
                        )
                    }
                }
            }
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
                    "useSimplifiedTracks",
                    "Deleted ${it.getDateAsString()}_${it.name} because useSimplifiedTracks was set to false."
                )
            }
        }
    }

    private fun simplifyTracks(summits: List<Summit>) {
        summits.forEach {
            if (it.ignoreSimplifyingTrack) {
                Log.w(
                    "updateSimplifiedTracks",
                    "Track ${it.getDateAsString()} ${it.name} (${it.getGpsTrackPath()}) " +
                            "will not be simplified, because it failed before"
                )
            }
        }
        val entriesWithoutSimplifiedGpxTrack = summits.filter {
            it.hasGpsTrack() &&
                    !it.ignoreSimplifyingTrack &&
                    !it.hasGpsTrack(simplified = true) &&
                    it.sportType != SportType.IndoorTrainer
        }.sortedByDescending { it.date }

        val entriesWithoutAdditionalData = if (entriesWithoutSimplifiedGpxTrack.size < 50) {
            summits.filter {
                it.hasGpsTrack() &&
                        !it.ignoreSimplifyingTrack &&
                        (!it.getYamlExtensionsFile().exists() ||
                                !it.getGpxPyPath().toFile().exists()) &&
                        it.sportType != SportType.IndoorTrainer
            }.sortedByDescending { it.date }.take(51 - entriesWithoutSimplifiedGpxTrack.size)
        } else {
            emptyList()
        }
        pythonInstance?.let {
            asyncSimplifyGpsTracks(
                entriesWithoutSimplifiedGpxTrack.take(250),
                entriesWithoutAdditionalData, it
            )
        }
    }

    private fun asyncSimplifyGpsTracks(
        summitsWithoutSimplifiedTracks: List<Summit>,
        summitsWithoutAdditionalData: List<Summit>,
        pythonInstance: Python
    ) {
        var numberSimplifiedGpxTracks = 0
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (summitsWithoutSimplifiedTracks.isNotEmpty()) {
                    summitsWithoutSimplifiedTracks.forEachIndexed { i, e ->
                        try {
                            Log.i(
                                "AsyncSimplifyGpsTracks",
                                "Simplifying track ${i + 1} of ${summitsWithoutSimplifiedTracks.size} for ${e.getDateAsString()}_${e.name}."
                            )
                            GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(
                                e.getGpsTrackPath(),
                            )
                            numberSimplifiedGpxTracks += 1
                            Log.i(
                                "AsyncSimplifyGpsTracks",
                                "Simplified track for ${e.getDateAsString()}_${e.name}."
                            )
                        } catch (ex: RuntimeException) {
                            Log.e(
                                "AsyncSimplifyGpsTracks",
                                "Error in simplify track for ${e.getDateAsString()}_${e.name}: ${ex.message}"
                            )
                            e.ignoreSimplifyingTrack = true
                            viewModel?.saveSummit(true, e)
                        }
                    }
                } else if (summitsWithoutAdditionalData.isNotEmpty()) {
                    summitsWithoutAdditionalData.forEachIndexed { i, e ->
                        try {
                            Log.i(
                                "AsyncSimplifyGpsTracks",
                                "Calculate additional data for  $i of ${summitsWithoutAdditionalData.size} for ${e.getDateAsString()}_${e.name}."
                            )
                            GpxPyExecutor(pythonInstance).analyzeGpxTrackAndCreateGpxPyDataFile(e)
                            Log.i(
                                "AsyncSimplifyGpsTracks",
                                "Calculated additional data for ${e.getDateAsString()}_${e.name}."
                            )
                        } catch (ex: RuntimeException) {
                            Log.e(
                                "AsyncSimplifyGpsTracks",
                                "Error in simplify track for ${e.getDateAsString()}_${e.name}: ${ex.message}"
                            )
                            e.ignoreSimplifyingTrack = true
                            viewModel?.saveSummit(true, e)
                        }
                    }
                } else {
                    Log.i("AsyncSimplifyGpsTracks", "No more gpx tracks to simplify.")
                }
            }
        }
    }

    private fun startAddSummitDialog(summit: Summit?) {
        val addSummitDialog = AddSummitDialog()
        if (summit != null) {
            val bundle = Bundle()
            bundle.putLong(Constants.BUNDLE_ID, summit.id)
            addSummitDialog.arguments = bundle
        }
        addSummitDialog.isBookmark = showBookmarksOnly
        addSummitDialog.show(
            requireActivity().supportFragmentManager, AddSummitDialog().tag
        )
    }


    private fun showEmpty(isShown: Boolean) {
        binding.apply {
            if (isShown) {
                emptyBody.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyBody.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

}
