package de.drtobiasprinz.summitbook.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.internal.managers.FragmentComponentManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SegmentEntryDetailsFragment
import de.drtobiasprinz.summitbook.databinding.CardAddSegmentDetailsBinding
import de.drtobiasprinz.summitbook.databinding.CardSegmentBinding
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.fragments.AddSegmentEntryFragment
import de.drtobiasprinz.summitbook.ui.dialog.AddSegmentDetailsDialog


class SegmentsViewAdapter(var segments: List<Segment>) :
    RecyclerView.Adapter<SegmentsViewAdapter.ViewHolder?>() {

    private lateinit var context: Context
    var onClickDelete: (Segment) -> Unit = { }

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
        context = parent.context
        if (viewType == CARD) {
            return ViewHolder(
                CardSegmentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else if (viewType == BUTTON) {
            return ViewHolder(
                CardAddSegmentDetailsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
        throw java.lang.RuntimeException("Something went wrong")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.binding is CardAddSegmentDetailsBinding) {
            holder.binding.addSegmentDetail.setOnClickListener {
                AddSegmentDetailsDialog.getInstance(
                    null,
                    this
                ).show(
                    (FragmentComponentManager.findActivity(context) as FragmentActivity).supportFragmentManager.beginTransaction(),
                    "Add Segment Details"
                )
            }
        } else {
            val entry = segments[position]
            addSegmentCard((holder.binding as CardSegmentBinding), entry)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addSegmentCard(binding: CardSegmentBinding, segment: Segment) {
        binding.routeName.text = segment.segmentDetails.getDisplayName()
        binding.routeNumber.text = "# ${segment.segmentEntries.size}"
        val averageDistance =
            if (segment.segmentEntries.isNotEmpty()) segment.segmentEntries.sumOf { it.kilometers } / segment.segmentEntries.size else 0.0
        val averageElevationGainUp =
            if (segment.segmentEntries.isNotEmpty()) segment.segmentEntries.sumOf { it.heightMetersUp } / segment.segmentEntries.size else 0
        val averageElevationGainDown =
            if (segment.segmentEntries.isNotEmpty()) segment.segmentEntries.sumOf { it.heightMetersDown } / segment.segmentEntries.size else 0
        binding.distance.text =
            if (averageDistance > 0.0) String.format(
                "%.1f %s",
                averageDistance,
                context.getString(R.string.km)
            ) else ""
        binding.elevationGain.text =
            if (averageElevationGainUp > 0 || averageElevationGainDown > 0) String.format(
                "%s/%s %s",
                averageElevationGainUp,
                averageElevationGainDown,
                context.getString(R.string.hm)
            ) else ""

        binding.entryDelete.setOnClickListener { v: View? ->
            v?.context?.let {
                showAlertDialog(it, segment, v)
            }
        }
        binding.entryEdit.setOnClickListener { view: View? ->
            AddSegmentDetailsDialog.getInstance(
                segment.segmentDetails,
                this
            ).show(
                (FragmentComponentManager.findActivity(view?.context) as FragmentActivity).supportFragmentManager.beginTransaction(),
                "Add Segment Details"
            )

        }
        binding.root.setOnClickListener {
            val fragment = SegmentEntryDetailsFragment()
            fragment.segmentDetailsId = segment.segmentDetails.segmentDetailsId
            (FragmentComponentManager.findActivity(it?.context) as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment, "SegmentEntryDetailsFragment")
                .addToBackStack(null).commit()
        }

        binding.addSegmentEntry.setOnClickListener { view: View? ->
            val fragment: Fragment = AddSegmentEntryFragment.getInstance(
                segment.segmentDetails.segmentDetailsId,
                this,
                null
            )
            (FragmentComponentManager.findActivity(view?.context) as FragmentActivity).supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment).addToBackStack(null).commit()
        }
    }


    private fun showAlertDialog(
        it: Context,
        entry: Segment,
        v: View
    ): AlertDialog? {
        return AlertDialog.Builder(it)
            .setTitle(
                v.context.getString(
                    R.string.delete_entry,
                    entry.segmentDetails.getDisplayName()
                )
            )
            .setMessage(v.context.getString(R.string.delete_entry_text))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                onClickDelete(entry)
                Toast.makeText(
                    v.context,
                    v.context.getString(
                        R.string.delete_entry,
                        entry.segmentDetails.getDisplayName()
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(
                android.R.string.cancel
            ) { _: DialogInterface?, _: Int ->
                Toast.makeText(
                    v.context, v.context.getString(R.string.delete_cancel),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    class ViewHolder internal constructor(val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        const val CARD = 0
        const val BUTTON = 1
    }
}