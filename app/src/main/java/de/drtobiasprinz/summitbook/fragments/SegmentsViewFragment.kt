package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import de.drtobiasprinz.summitbook.adapter.SegmentsViewAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentRoutesViewBinding
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.di.DatabaseModule


class SegmentsViewFragment : Fragment() {
    private lateinit var segments: MutableList<Segment>
    private var adapter: SegmentsViewAdapter? = null
    private lateinit var binding: FragmentRoutesViewBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRoutesViewBinding.inflate(layoutInflater, container, false)
        val database = context?.let { DatabaseModule.provideDatabase(it) }

        segments = (database?.segmentsDao()?.getAllSegmentsDeprecated() ?: listOf()) as MutableList
        adapter = SegmentsViewAdapter(segments)
        binding.root.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        binding.root.layoutManager = layoutManager

        return binding.root
    }

}