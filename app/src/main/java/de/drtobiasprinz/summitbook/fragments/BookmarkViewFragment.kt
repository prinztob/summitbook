package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import de.drtobiasprinz.summitbook.adapter.BookmarkViewAdapter
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.dialog.AddBookmarkDialog


class BookmarkViewFragment : Fragment() {
    private lateinit var bookmarks: MutableList<Summit>
    private var gpxTrackUrl: Uri? = null
    private var addBookmarkFab: FloatingActionButton? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        summitRecycler = inflater.inflate(
                R.layout.fragment_summit_view, container, false) as RecyclerView
        setHasOptionsMenu(true)
        val database = context?.let { AppDatabase.getDatabase(it) }
        bookmarks = (database?.summitDao()?.allBookmark ?: listOf()) as MutableList
        adapter = BookmarkViewAdapter(bookmarks)
        summitRecycler?.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        summitRecycler?.layoutManager = layoutManager


        requireActivity().findViewById<View>(R.id.add_new_summit).visibility = View.INVISIBLE
        addBookmarkFab = requireActivity().findViewById(R.id.add_new_bookmark)
        addBookmarkFab?.visibility = View.VISIBLE
        addBookmarkFab?.setOnClickListener { _: View? ->
            val addSummit = AddBookmarkDialog()
            (context as AppCompatActivity).supportFragmentManager.let { addSummit.show(it, getString(R.string.add_new_bookmark)) }
        }
        if (gpxTrackUrl != null) {
            val addSummit = AddBookmarkDialog(gpxTrackUrl)
            (context as AppCompatActivity).supportFragmentManager.let { addSummit.show(it, getString(R.string.add_new_bookmark)) }
        }
        return summitRecycler
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_search) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(OpenStreetMapFragment.TAG, "onDestroy")
        val addBookmarkFabLocal = addBookmarkFab
        if (addBookmarkFabLocal != null) {
            addBookmarkFabLocal.visibility = View.INVISIBLE
            addBookmarkFabLocal.setOnClickListener(null)
            requireActivity().findViewById<View>(R.id.add_new_summit).visibility = View.VISIBLE
        }
    }

    companion object {

        fun getInstance(uri: Uri? = null): BookmarkViewFragment {
            val fragment = BookmarkViewFragment()
            fragment.gpxTrackUrl = uri
            return fragment
        }

        @SuppressLint("StaticFieldLeak")
        var summitRecycler: RecyclerView? = null

        @SuppressLint("StaticFieldLeak")
        var adapter: BookmarkViewAdapter? = null
    }

}