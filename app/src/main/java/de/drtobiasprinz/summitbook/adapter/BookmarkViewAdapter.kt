package de.drtobiasprinz.summitbook.adapter

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import de.drtobiasprinz.summitbook.BookmarkDetailsActivity
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity.Companion.PICK_GPX_FILE
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.BookmarkEntry
import de.drtobiasprinz.summitbook.ui.dialog.AddBookmarkDialog.Companion.updateInstance
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*


class BookmarkViewAdapter(var bookmarks: ArrayList<BookmarkEntry>) : RecyclerView.Adapter<BookmarkViewAdapter.ViewHolder?>() {
    private var cardView: CardView? = null
    private var context: Context? = null
    private var selectedBookmarkEntry: BookmarkEntry? = null
    private var helper: SummitBookDatabaseHelper? = null
    override fun getItemCount(): Int {
        return bookmarks.size
    }

    override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
    ): ViewHolder {
        cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_bookmark, parent, false) as CardView
        context = parent.context
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cardView = holder.cardView
        helper = SummitBookDatabaseHelper(cardView?.context)
        val db = helper?.writableDatabase
        val entry = bookmarks[position]
        if (cardView != null) {
            val textViewName = cardView.findViewById<TextView?>(R.id.bookmark_name)
            textViewName?.text = entry.name
            val textViewHeight = cardView.findViewById<TextView?>(R.id.height_meter)
            textViewHeight?.text = String.format("%s hm", entry.heightMeter)
            val textViewKm = cardView.findViewById<TextView?>(R.id.kilometers)
            textViewKm?.text = String.format("%s km", entry.kilometers)
            val imageViewSportType = cardView.findViewById<ImageView?>(R.id.sport_type_image)
            entry.sportType.imageId.let { imageViewSportType?.setImageResource(it) }

            val removeButton = cardView.findViewById<ImageButton?>(R.id.entry_delete)
            //delete a summit entry
            removeButton?.setOnClickListener { v: View? ->
                v?.context?.let {
                    showAlertDialog(it, db, entry, v)
                }
            }
            val editButton = cardView.findViewById<ImageButton?>(R.id.entry_edit)
            editButton?.setOnClickListener { _: View? ->
                val updateDialog = updateInstance(entry)
                MainActivity.mainActivity?.supportFragmentManager?.let { updateDialog.show(it, "Update Bookmark") }
            }
        }
        val addPosition = cardView?.findViewById<ImageButton?>(R.id.entry_add_coordinate)
        addPosition?.setOnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            val origin = v?.context as Activity
            selectedBookmarkEntry = entry
            origin.startActivityForResult(intent, PICK_GPX_FILE)
        }
        cardView?.setOnClickListener { v: View? ->
            val context = v?.context
            val intent = Intent(context, BookmarkDetailsActivity::class.java)
            intent.putExtra(BookmarkDetailsActivity.BOOKMARK_ID_EXTRA_IDENTIFIER, entry._id)
            v?.context?.startActivity(intent)
        }
    }

    private fun showAlertDialog(it: Context, db: SQLiteDatabase?, entry: BookmarkEntry, v: View): AlertDialog? {
        return AlertDialog.Builder(it)
                .setTitle(v.context.getString(R.string.delete_entry, entry.name))
                .setMessage(v.context.getString(R.string.delete_entry_text))
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    val taskState = db?.let { helper?.deleteBookmark(it, entry) }
                            ?: false
                    if (taskState) {
                        bookmarks.remove(entry)
                        notifyDataSetChanged()
                        Toast.makeText(v.context, v.context.getString(R.string.delete_entry, entry.name), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(v.context, v.context.getString(R.string.try_again),
                                Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(android.R.string.no
                ) { _: DialogInterface?, _: Int ->
                    Toast.makeText(v.context, v.context.getString(R.string.delete_cancel),
                            Toast.LENGTH_SHORT).show()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == PICK_GPX_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                val bookmarkLocal = selectedBookmarkEntry
                if (bookmarkLocal != null) {
                    context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                        uploadGpxFile(inputStream, bookmarkLocal, cardView)
                    }
                }
            }
        }
    }

    private fun uploadGpxFile(inputStream: InputStream, entry: BookmarkEntry, v: View?) {
        try {
            val fileDest = entry.getGpsTrackPath()
            try {
                Files.copy(inputStream, fileDest, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Toast.makeText(v?.context, v?.context?.getString(R.string.add_gpx_successful, entry.name), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(v?.context, v?.context?.getString(R.string.add_gpx_failed, entry.name), Toast.LENGTH_SHORT).show()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            Toast.makeText(v?.context, v?.context?.getString(R.string.add_gpx_failed, entry.name), Toast.LENGTH_SHORT).show()
        }
    }


    class ViewHolder internal constructor(val cardView: CardView?) : RecyclerView.ViewHolder(cardView!!) {
    }
}