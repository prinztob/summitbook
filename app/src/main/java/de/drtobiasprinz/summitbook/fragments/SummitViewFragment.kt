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
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentSummitViewBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.SortFilterValues
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.entriesToExcludeForBoundingBoxCalculation
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.utils.AsyncUpdateGarminData
import de.drtobiasprinz.summitbook.utils.Constants
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.utils.isVisible
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SummitViewFragment : Fragment() {
    @Inject
    lateinit var contactsAdapter: ContactsAdapter
    private lateinit var binding: FragmentSummitViewBinding

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private var startedScheduler: Boolean = false
    private lateinit var database: AppDatabase
    val viewModel: DatabaseViewModel? by activityViewModels()
    private lateinit var sharedPreferences: SharedPreferences

    var showBookmarksOnly = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSummitViewBinding.inflate(layoutInflater, container, false)
        database = DatabaseModule.provideDatabase(requireContext())
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return binding.root
    }

    private fun adapterOnClickUpdateIsFavorite(summit: Summit) {
        summit.isFavorite = !summit.isFavorite
        viewModel?.saveContact(true, summit)
    }

    private fun adapterOnClickUpdateIsPeak(summit: Summit) {
        summit.isPeak = !summit.isPeak
        viewModel?.saveContact(true, summit)
    }

    private fun adapterOnClickDelete(summit: Summit) {
        viewModel?.deleteContact(summit)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnShowDialog.setOnClickListener {
                val addFragment = AddSummitDialog()
                addFragment.isBookmark = showBookmarksOnly
                addFragment.show(
                    requireActivity().supportFragmentManager,
                    AddSummitDialog().tag
                )
            }
            rvContacts.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = contactsAdapter
                contactsAdapter.onClickDelete = { e -> adapterOnClickDelete(e) }
                contactsAdapter.onClickUpdateIsFavorite = { e -> adapterOnClickUpdateIsFavorite(e) }
                contactsAdapter.onClickUpdateIsPeak = { e -> adapterOnClickUpdateIsPeak(e) }
            }
            if (showBookmarksOnly) {
                viewModel?.getAllBookmarks()
                viewModel?.bookmarksList?.observe(viewLifecycleOwner) {
                    when (it.status) {
                        DataStatus.Status.LOADING -> {
                            loading.isVisible(true, rvContacts)
                            emptyBody.isVisible(false, rvContacts)
                        }
                        DataStatus.Status.SUCCESS -> {
                            it.isEmpty?.let { isEmpty -> showEmpty(isEmpty) }
                            loading.isVisible(false, rvContacts)
                            contactsAdapter.differ.submitList(it.data)
                        }
                        DataStatus.Status.ERROR -> {
                            loading.isVisible(false, rvContacts)
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                viewModel?.summitsList?.observe(viewLifecycleOwner) {
                    when (it.status) {
                        DataStatus.Status.LOADING -> {
                            loading.isVisible(true, rvContacts)
                            emptyBody.isVisible(false, rvContacts)
                        }
                        DataStatus.Status.SUCCESS -> {
                            it.isEmpty?.let { isEmpty -> showEmpty(isEmpty) }
                            loading.isVisible(false, rvContacts)
                            val data = sortFilterValues.apply(it.data ?: emptyList())
                            contactsAdapter.differ.submitList(data)
                            if (!startedScheduler) {
                                addScheduler()
                                startedScheduler = true
                            }
                        }
                        DataStatus.Status.ERROR -> {
                            loading.isVisible(false, rvContacts)
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
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
                    val contact = contactsAdapter.differ.currentList[position]
                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            viewModel?.deleteContact(contact)
                            Snackbar.make(binding.root, "Item Deleted!", Snackbar.LENGTH_LONG)
                                .apply {
                                    setAction("UNDO") {
                                        viewModel?.saveContact(false, contact)
                                    }
                                }.show()
                        }
                        ItemTouchHelper.RIGHT -> {
                            val addSummitDialog = AddSummitDialog()
                            val bundle = Bundle()
                            bundle.putLong(Constants.BUNDLE_ID, contact.id)
                            addSummitDialog.arguments = bundle
                            addSummitDialog.show(
                                requireActivity().supportFragmentManager, AddSummitDialog().tag
                            )
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
            itemTouchHelper.attachToRecyclerView(rvContacts)

        }
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


    private fun addScheduler() {
        val schedulerBoundingBox = Executors.newSingleThreadScheduledExecutor()
        schedulerBoundingBox.schedule({
            val entriesWithoutBoundingBox = contactsAdapter.differ.currentList.filter {
                it.hasGpsTrack() && it.trackBoundingBox == null && it !in entriesToExcludeForBoundingBoxCalculation
            }
            if (entriesWithoutBoundingBox.isNotEmpty()) {
                val entryToCheck = entriesWithoutBoundingBox.first()
                entryToCheck.setBoundingBoxFromTrack()
                if (entryToCheck.trackBoundingBox != null) {
                    database.summitsDao().updateSummit(entryToCheck)
                    Log.i(
                        "Scheduler",
                        "Updated bounding box for ${entryToCheck.name}, ${entriesWithoutBoundingBox.size} remaining."
                    )
                } else {
                    Log.i(
                        "Scheduler",
                        "Updated bounding box for ${entryToCheck.name} failed, remove it from update list."
                    )
                    entriesToExcludeForBoundingBoxCalculation.add(entryToCheck)
                }
            } else {
                Log.i("Scheduler", "No more bounding boxes to calculate.")
            }

        }, 10, TimeUnit.MINUTES)

        val useSimplifiedTracks = sharedPreferences.getBoolean("use_simplified_tracks", true)
        if (useSimplifiedTracks) {
            val entriesWithoutSimplifiedGpxTrack = contactsAdapter.differ.currentList.filter {
                it.hasGpsTrack() && !it.hasGpsTrack(simplified = true)
            }.take(50)
            MainActivity.pythonInstance.let {
                if (it != null) {
                    @Suppress("DEPRECATION")
                    MainActivity.AsyncSimplifyGpsTracks(entriesWithoutSimplifiedGpxTrack, it)
                        .execute()
                }
            }
        } else {
            contactsAdapter.differ.currentList.filter {
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

        if (sharedPreferences.getBoolean("startup_auto_update_switch", false)) {
            binding.loading.visibility = View.VISIBLE
            @Suppress("DEPRECATION")
            ((MainActivity.pythonExecutor)?.let {
                AsyncUpdateGarminData(
                    sharedPreferences,
                    it,
                    database,
                    contactsAdapter.differ.currentList,
                    requireContext(),
                    binding.loading
                ).execute()
            })
        }
    }

}