package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.adapter.SegmentsViewAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentRoutesViewBinding
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import javax.inject.Inject

@AndroidEntryPoint
class SegmentsViewFragment : Fragment() {
    @Inject
    lateinit var sortFilterValues: SortFilterValues
    private lateinit var binding: FragmentRoutesViewBinding

    private val viewModel: DatabaseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRoutesViewBinding.inflate(layoutInflater, container, false)
        viewModel.segmentsList.observe(viewLifecycleOwner) {
            it.data.let { segments ->
                if (segments != null) {
                    val data = sortFilterValues.apply(segments)
                    val adapter = SegmentsViewAdapter(data)
                    adapter.onClickDelete = { segment -> viewModel.deleteSegment(segment) }
                    binding.root.adapter = adapter
                }
            }
        }
        val layoutManager = LinearLayoutManager(activity)
        binding.root.layoutManager = layoutManager

        return binding.root
    }

}