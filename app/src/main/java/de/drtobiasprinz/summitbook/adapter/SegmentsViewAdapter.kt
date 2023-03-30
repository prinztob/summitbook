package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.fragments.AddSegmentEntryFragment
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.dialog.AddSegmentDetailsDialog
import kotlin.math.floor
import kotlin.math.roundToInt


class SegmentsViewAdapter(var segments: MutableList<Segment>) : RecyclerView.Adapter<SegmentsViewAdapter.ViewHolder?>() {

    private lateinit var cardView: CardView
    private lateinit var context: Context
    private var database: AppDatabase? = null
    private var setSortLabelFor: Int = 21
    private var sorter: (List<SegmentEntry>) -> List<SegmentEntry> = { e -> e.sortedBy { it.duration } }

    override fun getItemCount(): Int {
        return segments.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < segments.size) {
            CARD
        } else {
            BUTTON
        }
    }

    override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int,
    ): ViewHolder {
        cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_segment, parent, false) as CardView
        context = parent.context
        if (viewType == CARD) {
            return ViewHolder(cardView)
        } else if (viewType == BUTTON) {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.card_add_segment_details, parent, false) as CardView)
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cardView = holder.cardView
        database = context.let { DatabaseModule.provideDatabase(it) }
        if (position >= segments.size) {
            cardView?.findViewById<Button?>(R.id.add_segment_detail)?.setOnClickListener {
                (context as AppCompatActivity).supportFragmentManager.let { AddSegmentDetailsDialog.getInstance(null, this).show(it, "Add Segment Details") }
            }
        } else {
            val entry = segments[position]
            addSegmentCard(cardView, entry)
        }
    }

    private fun addSegmentCard(cardView: CardView?, segment: Segment) {
        if (cardView != null) {
            cardView.findViewById<TextView?>(R.id.route_name).text = segment.segmentDetails.getDisplayName()
            "# ${segment.segmentEntries.size}".also { cardView.findViewById<TextView?>(R.id.route_number).text = it }

            val averageDistance = if (segment.segmentEntries.isNotEmpty()) segment.segmentEntries.sumByDouble { it.kilometers } / segment.segmentEntries.size else 0.0
            val averageElevationGainUp = if (segment.segmentEntries.isNotEmpty()) segment.segmentEntries.sumBy { it.heightMetersUp } / segment.segmentEntries.size else 0
            val averageElevationGainDown = if (segment.segmentEntries.isNotEmpty()) segment.segmentEntries.sumBy { it.heightMetersDown } / segment.segmentEntries.size else 0
            cardView.findViewById<TextView?>(R.id.distance).text = if (averageDistance > 0.0) String.format("%.1f %s", averageDistance, context.getString(R.string.km)) else ""
            cardView.findViewById<TextView?>(R.id.elevationGain).text = if (averageElevationGainUp > 0 || averageElevationGainDown > 0) String.format("%s/%s %s", averageElevationGainUp, averageElevationGainDown, context.getString(R.string.hm)) else ""

            val removeButton = cardView.findViewById<ImageButton?>(R.id.entry_delete)
            removeButton?.setOnClickListener { v: View? ->
                v?.context?.let {
                    showAlertDialog(it, database, segment, v)
                }
            }
            val editButton = cardView.findViewById<ImageButton?>(R.id.entry_edit)
            editButton?.setOnClickListener { _: View? ->
                (context as AppCompatActivity).supportFragmentManager.let { AddSegmentDetailsDialog.getInstance(segment.segmentDetails, this).show(it, "Add Segment Details") }
            }
            cardView.setOnClickListener { v: View? ->
                val tableLayout = cardView.findViewById(R.id.tableSegments) as TableLayout
                val visibility = tableLayout.visibility
                if (visibility == View.VISIBLE) {
                    tableLayout.visibility = View.GONE
                } else {
                    tableLayout.visibility = View.VISIBLE
                    drawTable(tableLayout, segment)
                }
            }

            val addSegmentEntryButton = cardView.findViewById<ImageButton?>(R.id.add_segment_entry)
            addSegmentEntryButton.setOnClickListener { view: View? ->
                val fragment: Fragment = AddSegmentEntryFragment.getInstance(segment.segmentDetails.segmentDetailsId, this, null)
                ((view?.context) as MainActivity).supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit()
            }
        }
    }

    private fun drawTable(tableLayout: TableLayout, segment: Segment) {
        tableLayout.removeAllViews()
        addHeader(cardView, tableLayout, segment)
        sorter(segment.segmentEntries).forEachIndexed { index, entry ->
            val summit = database?.summitsDao()?.getSummitFromActivityId(entry.activityId)
            if (summit != null) {
                addSegmentToTable(summit, entry, cardView, index, tableLayout, segment)
            }
        }
    }

    private fun addHeader(view: View, tableLayout: TableLayout, segment: Segment) {
        val tableRowHead = TableRow(view.context)
        10.also { tableRowHead.id = it }
        tableRowHead.setBackgroundColor(Color.WHITE)
        tableRowHead.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        addLabel(view, tableRowHead, 20, context.getString(R.string.date), tableLayout, segment, sorter = { e -> e.sortedBy { it.date } })
        addLabel(view, tableRowHead, 21, context.getString(R.string.minutes), tableLayout, segment)
        addLabel(view, tableRowHead, 22, context.getString(R.string.kmh), tableLayout, segment, sorter = { e -> e.sortedBy { it.kilometers * 60 / it.duration }.reversed() })
        addLabel(view, tableRowHead, 23, context.getString(R.string.bpm), tableLayout, segment, sorter = { e -> e.sortedBy { it.averageHeartRate }.reversed() })
        addLabel(view, tableRowHead, 24, context.getString(R.string.watt), tableLayout, segment, sorter = { e -> e.sortedBy { it.averagePower }.reversed() })
        addLabel(view, tableRowHead, 25, "", tableLayout, segment)
        addLabel(view, tableRowHead, 26, "", tableLayout, segment)
        tableLayout.addView(tableRowHead, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addLabel(view: View, tr: TableRow, id: Int, text: String, color: Int = Color.WHITE,
                         padding: Int = 5, alignment: Int = View.TEXT_ALIGNMENT_CENTER) {
        val label = TextView(view.context)
        label.id = id
        label.text = text
        label.gravity = alignment
        label.setTextColor(color)
        label.setPadding(padding, padding, padding, padding)
        tr.addView(label)
    }

    private fun addLabel(view: View, tr: TableRow, id: Int, text: String, tableLayout: TableLayout, segment: Segment,
                         sorter: (List<SegmentEntry>) -> List<SegmentEntry> = { e -> e.sortedBy { it.duration } }) {
        val label = TextView(view.context)
        label.id = id
        label.text = text
        if (id == setSortLabelFor) {
            label.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_keyboard_double_arrow_down_24, 0, 0, 0)
        }
        label.setOnClickListener {
            this.sorter = sorter
            setSortLabelFor = id
            drawTable(tableLayout, segment)
        }
        tr.addView(label)
    }

    private fun addSegmentToTable(summit: Summit, entry: SegmentEntry, view: View, i: Int, tableLayout: TableLayout, segment: Segment) {
        val dateParts = (summit.getDateAsString() ?: "").split("-")
        val date = "${dateParts[0]}\n${dateParts[1]}-${dateParts[2]}"
        val tr = TableRow(view.context)
        tr.setBackgroundColor(Color.GRAY)
        tr.id = 100 + i
        tr.layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        addLabel(view, tr, 200 + i, date, padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
        addLabel(view, tr, 201 + i, String.format("%s:%s", floor(entry.duration).toInt(), ((entry.duration - floor(entry.duration)) * 60).roundToInt().toString().padStart(2, '0')), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
        addLabel(view, tr, 202 + i, String.format("%.1f", entry.kilometers * 60 / entry.duration), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
        addLabel(view, tr, 203 + i, String.format("%s", entry.averageHeartRate), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
        addLabel(view, tr, 204 + i, String.format("%s", entry.averagePower), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END)
        val updateButton = ImageButton(view.context)
        updateButton.setImageResource(R.drawable.ic_baseline_edit_24)
        updateButton.setOnClickListener {
            val fragment: Fragment = AddSegmentEntryFragment.getInstance(segment.segmentDetails.segmentDetailsId, this, entry.entryId)
            ((view.context) as MainActivity).supportFragmentManager.beginTransaction().replace(R.id.content_frame, fragment).addToBackStack(null).commit()
        }
        tr.addView(updateButton)
        val removeButton = ImageButton(view.context)
        removeButton.setImageResource(R.drawable.ic_baseline_delete_24)
        removeButton.setOnClickListener {
            database?.segmentsDao()?.deleteSegmentEntry(entry)
            segments = database?.segmentsDao()?.getAllSegments() ?: mutableListOf()
            segment.segmentEntries.remove(entry)
            notifyDataSetChanged()
            drawTable(tableLayout, segment)
        }
        tr.addView(removeButton)

        tableLayout.addView(tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun showAlertDialog(it: Context, database: AppDatabase?, entry: Segment, v: View): AlertDialog? {
        return AlertDialog.Builder(it)
                .setTitle(v.context.getString(R.string.delete_entry, entry.segmentDetails.getDisplayName()))
                .setMessage(v.context.getString(R.string.delete_entry_text))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    database?.let { database.segmentsDao()?.delete(entry) }
                    segments.remove(entry)
                    notifyDataSetChanged()
                    Toast.makeText(v.context, v.context.getString(R.string.delete_entry, entry.segmentDetails.getDisplayName()), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel
                ) { _: DialogInterface?, _: Int ->
                    Toast.makeText(v.context, v.context.getString(R.string.delete_cancel),
                            Toast.LENGTH_SHORT).show()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    class ViewHolder internal constructor(val cardView: CardView?) : RecyclerView.ViewHolder(cardView!!)

    companion object {
        const val CARD = 0
        const val BUTTON = 1
    }
}