package de.drtobiasprinz.summitbook.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.internal.managers.FragmentComponentManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.CardSegmentEntryBinding
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.ui.dialog.AddSegmentDetailsDialog


class SegmentsEntryAdapter(
    var segmentDetails: SegmentDetails,
    private var segmentEntryToShow: SegmentEntry
) :
    RecyclerView.Adapter<SegmentsEntryAdapter.ViewHolder?>() {

    private lateinit var context: Context
    var onClickDelete: (SegmentEntry) -> Unit = { }
    var onClickSegment: (SegmentEntry) -> Unit = { }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): ViewHolder {
        context = parent.context
        return ViewHolder(
            CardSegmentEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = differ.currentList[position]
        addSegmentCard((holder.binding as CardSegmentEntryBinding), entry)
    }

    @SuppressLint("SetTextI18n")
    private fun addSegmentCard(binding: CardSegmentEntryBinding, segmentEntry: SegmentEntry) {
        binding.root.setOnClickListener {
            segmentEntryToShow = segmentEntry
            onClickSegment(segmentEntry)
        }
        setView(segmentEntry, binding)
    }

    private fun setView(
        segmentEntry: SegmentEntry,
        binding: CardSegmentEntryBinding
    ) {
        if (segmentEntry == segmentEntryToShow) {
            binding.root.setBackgroundColor(Color.GRAY)
        } else {
            when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.root.setBackgroundColor(Color.DKGRAY)
                }

                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.root.setBackgroundColor(Color.WHITE)
                }

                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.root.setBackgroundColor(Color.WHITE)
                }
            }
        }
        binding.date.text = (segmentEntry.getDateAsString() ?: "")
        binding.minutes.text = String.format(
            context.resources.configuration.locales[0],
            "%.1f %s",
            segmentEntry.duration,
            context.getString(R.string.min)
        )
        binding.velocity.text = String.format(
            context.resources.configuration.locales[0],
            "%.1f\n%s",
            segmentEntry.kilometers / segmentEntry.duration * 60,
            context.getString(R.string.km)
        )
        binding.bpm.text = String.format(
            context.resources.configuration.locales[0],
            "%s\n%s",
            segmentEntry.averageHeartRate,
            context.getString(R.string.bpm)
        )
        binding.watt.text = String.format(
            context.resources.configuration.locales[0],
            "%s\n%s",
            segmentEntry.averagePower,
            context.getString(R.string.watt)
        )

        binding.entryDelete.setOnClickListener { v: View? ->
            v?.context?.let {
                showAlertDialog(it, segmentEntry, v)
            }
        }
        binding.entryEdit.setOnClickListener { view: View? ->
            AddSegmentDetailsDialog.getInstance(
                segmentDetails,
            ).show(
                (FragmentComponentManager.findActivity(view?.context) as FragmentActivity).supportFragmentManager.beginTransaction(),
                "Add Segment Details"
            )
        }
    }


    private fun showAlertDialog(
        it: Context,
        entry: SegmentEntry,
        v: View
    ): AlertDialog? {
        return AlertDialog.Builder(it)
            .setTitle(
                v.context.getString(
                    R.string.delete_entry,
                    "${segmentDetails.getDisplayName()} on ${entry.getDateAsString()}"
                )
            )
            .setMessage(v.context.getString(R.string.delete_entry_text))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                onClickDelete(entry)
                Toast.makeText(
                    v.context,
                    v.context.getString(
                        R.string.delete_entry,
                        "${segmentDetails.getDisplayName()} on ${entry.getDateAsString()}"
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

    private val differCallback = object : DiffUtil.ItemCallback<SegmentEntry>() {
        override fun areItemsTheSame(oldItem: SegmentEntry, newItem: SegmentEntry): Boolean {
            return false
        }

        override fun areContentsTheSame(oldItem: SegmentEntry, newItem: SegmentEntry): Boolean {
            return false
        }

    }
    val differ = AsyncListDiffer(this, differCallback)
}