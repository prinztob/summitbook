package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.BookmarkViewAdapter
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.BookmarkEntry
import de.drtobiasprinz.summitbook.ui.dialog.AddBookmarkDialog
import java.util.*


class BookmarkViewFragment : Fragment() {
    private lateinit var bookmarks: ArrayList<BookmarkEntry>
    private var addBookmarkFab: FloatingActionButton? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        summitRecycler = inflater.inflate(
                R.layout.fragment_summit_view, container, false) as RecyclerView
        setHasOptionsMenu(true)
        val helper = SummitBookDatabaseHelper(activity)
        val db = helper.readableDatabase
        bookmarks = helper.getAllBookmarks(db)
        adapter = BookmarkViewAdapter(bookmarks)
        summitRecycler?.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        summitRecycler?.layoutManager = layoutManager


        requireActivity().findViewById<View>(R.id.add_new_summit).visibility = View.INVISIBLE
        addBookmarkFab = requireActivity().findViewById(R.id.add_new_bookmark)
        addBookmarkFab?.visibility = View.VISIBLE
        addBookmarkFab?.setOnClickListener { _: View? ->
            val addSummit = AddBookmarkDialog()
            MainActivity.mainActivity?.supportFragmentManager?.let { addSummit.show(it, "Add new bookmark") }
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        adapter?.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var summitRecycler: RecyclerView? = null
        var adapter: BookmarkViewAdapter? = null
    }

}