package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.dialog.AddBookmarkDialog.Companion.updateInstance


class BookmarkViewAdapter(var bookmarks: MutableList<Summit>) : RecyclerView.Adapter<BookmarkViewAdapter.ViewHolder?>() {
    private var cardView: CardView? = null
    private var context: Context? = null
    private var database: AppDatabase? = null
    override fun getItemCount(): Int {
        return bookmarks.size
    }

    override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int,
    ): ViewHolder {
        cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_bookmark, parent, false) as CardView
        context = parent.context
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cardView = holder.cardView
        database = context?.let { AppDatabase.getDatabase(it) }
        val entry = bookmarks[position]
        if (cardView != null) {
            val textViewName = cardView.findViewById<TextView?>(R.id.bookmark_name)
            textViewName?.text = entry.name
            val textViewHeight = cardView.findViewById<TextView?>(R.id.height_meter)
            textViewHeight?.text = String.format("%s %s", entry.elevationData.elevationGain, cardView.resources.getString(R.string.hm))
            val textViewKm = cardView.findViewById<TextView?>(R.id.kilometers)
            textViewKm?.text = String.format("%s km", entry.kilometers, cardView.resources.getString(R.string.km))
            val imageViewSportType = cardView.findViewById<ImageView?>(R.id.sport_type_image)
            entry.sportType.imageIdBlack.let { imageViewSportType?.setImageResource(it) }

            val removeButton = cardView.findViewById<ImageButton?>(R.id.entry_delete)
            //delete a summit entry
            removeButton?.setOnClickListener { v: View? ->
                v?.context?.let {
                    showAlertDialog(it, database, entry, v)
                }
            }
            val editButton = cardView.findViewById<ImageButton?>(R.id.entry_edit)
            editButton?.setOnClickListener { _: View? ->
                val updateDialog = updateInstance(entry)
                MainActivity.mainActivity?.supportFragmentManager?.let { updateDialog.show(it, "Update Bookmark") }
            }
            cardView.setOnClickListener { v: View? ->
                val context = v?.context
                val intent = Intent(context, SummitEntryDetailsActivity::class.java)
                intent.putExtra(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, entry.id)
                intent.putExtra(SUMMIT_IS_BOOKMARK_IDENTIFIERS, true)
                v?.context?.startActivity(intent)
            }
        }
    }

    private fun showAlertDialog(it: Context, database: AppDatabase?, entry: Summit, v: View): AlertDialog? {
        return AlertDialog.Builder(it)
                .setTitle(v.context.getString(R.string.delete_entry, entry.name))
                .setMessage(v.context.getString(R.string.delete_entry_text))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    database?.let { database.summitDao()?.delete(entry) }
                    bookmarks.remove(entry)
                    notifyDataSetChanged()
                    Toast.makeText(v.context, v.context.getString(R.string.delete_entry, entry.name), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel
                ) { _: DialogInterface?, _: Int ->
                    Toast.makeText(v.context, v.context.getString(R.string.delete_cancel),
                            Toast.LENGTH_SHORT).show()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    class ViewHolder internal constructor(val cardView: CardView?) : RecyclerView.ViewHolder(cardView!!) {
    }

    companion object {
        var SUMMIT_IS_BOOKMARK_IDENTIFIERS = "SUMMIT_IS_BOOKMARK_IDENTIFIERS"
    }
}