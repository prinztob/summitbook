package de.drtobiasprinz.summitbook.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.adapter.SummitEntitiesAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntitiesViewBinding
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.CONNECTED_ACTIVITY_PREFIX
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.models.SummitEntities
import de.drtobiasprinz.summitbook.models.SummitEntityType
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.allSummits
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.utils.isVisible
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import javax.inject.Inject

@AndroidEntryPoint
class SummitEntitiesViewFragment : Fragment() {

    private lateinit var summitEntitiesAdapter: SummitEntitiesAdapter
    private lateinit var binding: FragmentSummitEntitiesViewBinding

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    val viewModel: DatabaseViewModel? by activityViewModels()
    private lateinit var sharedPreferences: SharedPreferences

    var usedSummitEntityType = SummitEntityType.COUNTRIES

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        summitEntitiesAdapter = SummitEntitiesAdapter()
        binding = FragmentSummitEntitiesViewBinding.inflate(layoutInflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = summitEntitiesAdapter
            }

            viewModel?.summitsList?.observe(viewLifecycleOwner) { summitsStatus ->
                when (summitsStatus.status) {
                    DataStatus.Status.LOADING -> {
                        loading.isVisible(true, recyclerView)
                        emptyBody.isVisible(false, recyclerView)
                    }

                    DataStatus.Status.SUCCESS -> {
                        summitsStatus.isEmpty?.let { isEmpty -> showEmpty(isEmpty) }
                        loading.isVisible(false, recyclerView)
                        allSummits = summitsStatus.data ?: emptyList()
                        val entityNames = sortFilterValues.apply(
                            summitsStatus.data ?: emptyList(),
                            sharedPreferences
                        ).flatMap { usedSummitEntityType.getRelevantValueFromSummit(it) }
                            .filter { it != "" && !it.startsWith(CONNECTED_ACTIVITY_PREFIX) }
                            .toSet().toList()
                        val data = entityNames.map { name ->
                            val relevantSummits = allSummits.filter {
                                name in usedSummitEntityType.getRelevantValueFromSummit(it)
                            }
                            SummitEntities(
                                usedSummitEntityType,
                                name,
                                relevantSummits.size,
                                relevantSummits.sumOf { it.kilometers },
                                relevantSummits.sumOf { it.elevationData.elevationGain }
                            )
                        }
                        summitEntitiesAdapter.differ.submitList(sortFilterValues.applyOnSummitEntities(data))
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
