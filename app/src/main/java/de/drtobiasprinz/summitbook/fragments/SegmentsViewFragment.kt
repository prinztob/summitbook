package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SegmentsViewAdapter
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.dialog.AddBookmarkDialog


class SegmentsViewFragment : Fragment() {
    private lateinit var segments: MutableList<Segment>
    private var addRoutesFab: FloatingActionButton? = null
    private var adapter: SegmentsViewAdapter? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val recycler = inflater.inflate(
                R.layout.fragment_routes_view, container, false) as RecyclerView
        setHasOptionsMenu(true)
        val database = context?.let { DatabaseModule.provideDatabase(it) }

        segments = (database?.segmentsDao()?.getAllSegments() ?: listOf()) as MutableList
        adapter = SegmentsViewAdapter(segments)
        recycler.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        recycler.layoutManager = layoutManager

        addRoutesFab?.visibility = View.VISIBLE
        addRoutesFab?.setOnClickListener { _: View? ->
            val addSummit = AddBookmarkDialog()
            (context as AppCompatActivity).supportFragmentManager.let { addSummit.show(it, getString(R.string.add_new_bookmark)) }
        }
        return recycler
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_search) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}