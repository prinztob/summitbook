package de.drtobiasprinz.summitbook.fragments

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentSummitViewBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonExecutor
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.observeOnce
import de.drtobiasprinz.summitbook.ui.utils.GarminDataUpdater
import de.drtobiasprinz.summitbook.utils.Constants
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.utils.isVisible
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
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

    private var startedScheduler: Boolean = false
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
                            summitsAdapter.differ.submitList(it.data)
                        }
                        DataStatus.Status.ERROR -> {
                            loading.isVisible(false, recyclerView)
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                viewModel?.summitsList?.observe(viewLifecycleOwner) { dataStatus ->
                    when (dataStatus.status) {
                        DataStatus.Status.LOADING -> {
                            loading.isVisible(true, recyclerView)
                            emptyBody.isVisible(false, recyclerView)
                        }
                        DataStatus.Status.SUCCESS -> {
                            dataStatus.isEmpty?.let { isEmpty -> showEmpty(isEmpty) }
                            loading.isVisible(false, recyclerView)
                            val data = sortFilterValues.apply(dataStatus.data ?: emptyList())
                            summitsAdapter.differ.submitList(data)
                            if (!startedScheduler) {
                                dataStatus.data?.let { addBackgroundTasks(it) }
                                startedScheduler = true
                            }
                        }
                        DataStatus.Status.ERROR -> {
                            loading.isVisible(false, recyclerView)
                            Toast.makeText(context, dataStatus.message, Toast.LENGTH_SHORT).show()
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
                            viewModel?.deleteSummit(summit)
                            Snackbar.make(binding.root, "Item Deleted!", Snackbar.LENGTH_LONG)
                                .apply {
                                    setAction("UNDO") {
                                        viewModel?.saveSummit(false, summit)
                                    }
                                }.show()
                        }
                        ItemTouchHelper.RIGHT -> {
                            startAddSummitDialog(summit)
                        }
                    }
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    RecyclerViewSwipeDecorator.Builder(
                        c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                    ).addSwipeLeftLabel("Delete").addSwipeLeftBackgroundColor(Color.RED)
                        .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24)
                        .setSwipeLeftLabelColor(Color.WHITE).setSwipeLeftActionIconTint(Color.WHITE)
                        .addSwipeRightLabel("Edit").addSwipeRightBackgroundColor(Color.GREEN)
                        .setSwipeRightLabelColor(Color.WHITE)
                        .setSwipeRightActionIconTint(Color.WHITE)
                        .addSwipeRightActionIcon(R.drawable.ic_baseline_edit_24).create().decorate()
                    super.onChildDraw(
                        c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                    )
                }

            }

            val itemTouchHelper = ItemTouchHelper(swipeCallback)
            itemTouchHelper.attachToRecyclerView(recyclerView)

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
                listBody.visibility = View.GONE
            } else {
                emptyBody.visibility = View.GONE
                listBody.visibility = View.VISIBLE
            }
        }
    }

    private fun addBackgroundTasks(summits: List<Summit>) {
        val useSimplifiedTracks = sharedPreferences.getBoolean("use_simplified_tracks", true)
        if (useSimplifiedTracks) {
            val entriesWithoutSimplifiedGpxTrack = summits.filter {
                it !in MainActivity.entriesToExcludeForSimplifyGpxTrack && it.hasGpsTrack() && !it.hasGpsTrack(
                    simplified = true
                )
            }.take(50)
            MainActivity.pythonInstance.let {
                if (it != null) {
                    asyncSimplifyGpsTracks(
                        entriesWithoutSimplifiedGpxTrack,
                        it
                    )
                }
            }
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
        val executor = pythonExecutor
        if (sharedPreferences.getBoolean("startup_auto_update_switch", false) && executor != null) {
            binding.loading.visibility = View.VISIBLE
            viewModel?.solarIntensitiesList?.observeOnce(viewLifecycleOwner) { solarListDataStatus ->
                solarListDataStatus?.data.let { solarIntensities ->
                    val updater = GarminDataUpdater(
                        sharedPreferences,
                        executor,
                        summits,
                        solarIntensities,
                        viewModel
                    )
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            updater.update()
                        }
                        updater.onFinish(binding.loading, requireContext())
                    }
                }
            }
        }
    }


    private fun asyncSimplifyGpsTracks(
        summitsWithoutSimplifiedTracks: List<Summit>,
        pythonInstance: Python
    ) {
        var numberSimplifiedGpxTracks = 0
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (summitsWithoutSimplifiedTracks.isNotEmpty()) {
                    summitsWithoutSimplifiedTracks.forEach {
                        try {
                            GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(
                                it.getGpsTrackPath(
                                    simplified = false
                                )
                            )
                            numberSimplifiedGpxTracks += 1
                            Log.i(
                                "AsyncSimplifyGpsTracks",
                                "Simplify track for ${it.getDateAsString()}_${it.name}."
                            )
                        } catch (ex: RuntimeException) {
                            Log.e(
                                "AsyncSimplifyGpsTracks",
                                "Error in simplify track for ${it.getDateAsString()}_${it.name}: ${ex.message}"
                            )
                            MainActivity.entriesToExcludeForSimplifyGpxTrack.add(it)
                        }
                    }
                } else {
                    Log.i("AsyncSimplifyGpsTracks", "No more gpx tracks to simplify.")
                }
            }
        }
    }

}
