package de.drtobiasprinz.summitbook.fragments

import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SummitsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentSummitViewBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.utils.Constants
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.utils.isVisible
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
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
        Snackbar.make(binding.root, getString(R.string.delete_entry_done), Snackbar.LENGTH_LONG)
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
                    viewModel?.segmentsList?.observe(viewLifecycleOwner) { segmentsStatus ->
                        when (summitsStatus.status) {
                            DataStatus.Status.LOADING -> {
                                loading.isVisible(true, recyclerView)
                                emptyBody.isVisible(false, recyclerView)
                            }

                            DataStatus.Status.SUCCESS -> {
                                summitsStatus.isEmpty?.let { isEmpty -> showEmpty(isEmpty) }
                                loading.isVisible(false, recyclerView)
                                val data = sortFilterValues.apply(
                                    summitsStatus.data ?: emptyList(),
                                    sharedPreferences
                                )
                                segmentsStatus.data.let { segments ->
                                    if (segments != null) {
                                        data.forEach { summit ->
                                            summit.updateSegmentInfo(segments)
                                        }
                                    }
                                }
                                val maxSummits = TimeIntervalPower.values().map {
                                    it.getMaxSummit(
                                        ExtremaValuesSummits(
                                            data,
                                            excludeZeroValueFromMin = true
                                        )
                                    )
                                }
                                data.forEach { summits ->
                                    summits.hasPowerRecord = summits in maxSummits
                                    if (summits.segmentInfo.isNotEmpty()) {
                                        summits.bestPositionInSegment = summits.segmentInfo.minOf { it.third }
                                    }
                                }
                                summitsAdapter.differ.submitList(data)
                            }

                            DataStatus.Status.ERROR -> {
                                loading.isVisible(false, recyclerView)
                                Toast.makeText(context, summitsStatus.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
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
                    ).addSwipeLeftLabel(getString(R.string.update))
                        .addSwipeLeftBackgroundColor(Color.GREEN)
                        .setSwipeLeftLabelColor(Color.WHITE)
                        .setSwipeLeftActionIconTint(Color.WHITE)
                        .addSwipeLeftActionIcon(R.drawable.ic_baseline_edit_24)
                        .addSwipeRightLabel(getString(R.string.update))
                        .addSwipeRightBackgroundColor(Color.GREEN)
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

}
