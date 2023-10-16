package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.hilt.android.internal.managers.FragmentComponentManager
import de.drtobiasprinz.summitbook.AddImagesActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.CardSummitBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.dialog.AddAdditionalDataFromExternalResourcesDialog
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.utils.Constants
import javax.inject.Singleton


@Singleton
class SummitsAdapter :
    RecyclerView.Adapter<SummitsAdapter.ViewHolder>() {

    lateinit var context: Context
    var onClickUpdateIsFavorite: (Summit) -> Unit = { }
    var onClickUpdateIsPeak: (Summit) -> Unit = { }
    var onClickDelete: (Summit) -> Unit = { }
    var isBookmark: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding =
            CardSummitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class ViewHolder(var binding: CardSummitBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(entity: Summit) {
            binding.apply {
                summitName.text = entity.name
                heightMeter.text = String.format(
                    "%s %s", entity.elevationData.elevationGain, "hm"
                )
                addImage(entity)
                setAddVelocityData(entity)
                if (entity.isBookmark) {
                    entryAddImage.setImageResource(R.drawable.ic_baseline_bookmarks_24)
                    tourDate.text = String.format(
                        "%s %s", entity.kilometers, "km"
                    )
                    entryFavorite.visibility = View.INVISIBLE
                    entrySummit.visibility = View.INVISIBLE
                } else {
                    entryAddImage.setImageResource(R.drawable.baseline_add_a_photo_black_24dp)
                    entryFavorite.visibility = View.VISIBLE
                    entrySummit.visibility = View.VISIBLE
                    setViewForSummitsOnly(entity)
                }
                entryDelete.setOnClickListener { v: View? ->
                    v?.context?.let {
                        showDeleteEntryDialog(it, entity, v)
                    }
                }
                entryEdit.setOnClickListener { v: View? ->
                    val addSummitDialog = AddSummitDialog()
                    val bundle = Bundle()
                    bundle.putLong(Constants.BUNDLE_ID, entity.id)
                    addSummitDialog.arguments = bundle
                    addSummitDialog.isBookmark = isBookmark
                    addSummitDialog.show(
                        (FragmentComponentManager.findActivity(v?.context) as FragmentActivity).supportFragmentManager,
                        AddSummitDialog().tag
                    )
                }
                if (entity.latLng == null) {
                    entryAddCoordinate.setImageResource(R.drawable.baseline_add_location_black_24dp)
                } else if (entity.hasGpsTrack(true)) {
                    entryAddCoordinate.setImageResource(R.drawable.baseline_edit_location_alt_24)
                } else {
                    entryAddCoordinate.setImageResource(R.drawable.baseline_edit_location_black_24dp)
                }
                entryAddCoordinate.setOnClickListener { v: View? ->
                    val context = v?.context
                    val intent = Intent(context, SelectOnOsMapActivity::class.java)
                    intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, entity.id)
                    v?.context?.startActivity(intent)
                }
                root.setOnClickListener { v: View? ->
                    val context = v?.context
                    val intent = Intent(context, SummitEntryDetailsActivity::class.java)
                    intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, entity.id)
                    v?.context?.startActivity(intent)
                }
                setRecords(entity, segmentRecord, powerRecord)
            }
        }

        private fun CardSummitBinding.setAddVelocityData(entity: Summit) {
            if (entity.velocityData.hasAdditionalData() || entity.elevationData.hasAdditionalData()) {
                entryAddVelocityData.setImageResource(R.drawable.baseline_speed_black_24dp)
            } else {
                entryAddVelocityData.setImageResource(R.drawable.baseline_more_time_black_24dp)
            }
            if ((entity.garminData == null || entity.garminData?.activityId == null) && !entity.hasGpsTrack()) {
                entryAddVelocityData.visibility = View.GONE
            } else {
                entryAddVelocityData.visibility = View.VISIBLE
            }
            entryAddVelocityData.setOnClickListener { v: View? ->
                AddAdditionalDataFromExternalResourcesDialog.getInstance(entity).show(
                    (FragmentComponentManager.findActivity(v?.context) as FragmentActivity).supportFragmentManager,
                    "Show addition data"
                )
            }
        }

        private fun CardSummitBinding.setViewForSummitsOnly(entity: Summit) {
            tourDate.text = entity.getDateAsString()
            setFavoriteImage(entity, entryFavorite)
            setMountainImage(entity, entrySummit)

            entryAddImage.setOnClickListener { v: View? ->
                val context = v?.context
                val intent = Intent(context, AddImagesActivity::class.java)
                intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, entity.id)
                v?.context?.startActivity(intent)
            }

        }

        private fun CardSummitBinding.addImage(item: Summit) {
            if (item.hasImagePath()) {
                sportTypeImage.setImageResource(item.sportType.imageIdWhite)
                cardViewText.setBackgroundResource(R.color.translucent)
                summitName.setTextColor(Color.WHITE)
                cardViewImage.visibility = View.VISIBLE
                Glide.with(root)
                    .load("file://" + item.getImagePath(item.imageIds.first()))
                    .centerInside()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(cardViewImage)
            } else {
                cardViewText.setBackgroundColor(Color.TRANSPARENT)
                when (context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        sportTypeImage.setImageResource(item.sportType.imageIdWhite)
                        summitName.setTextColor(Color.WHITE)
                    }

                    Configuration.UI_MODE_NIGHT_NO -> {
                        sportTypeImage.setImageResource(item.sportType.imageIdBlack)
                        summitName.setTextColor(Color.BLACK)
                    }

                    Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                        sportTypeImage.setImageResource(item.sportType.imageIdWhite)
                        summitName.setTextColor(Color.GRAY)
                    }
                }
                cardViewImage.visibility = View.GONE
            }
        }

        private fun setFavoriteImage(summit: Summit, button: ImageButton) {
            if (summit.isFavorite) {
                button.setImageResource(R.drawable.baseline_star_black_24dp)
            } else {
                button.setImageResource(R.drawable.baseline_star_border_black_24dp)
            }
            button.setOnClickListener {
                if (summit.isFavorite) {
                    button.setImageResource(R.drawable.baseline_star_border_black_24dp)
                } else {
                    button.setImageResource(R.drawable.baseline_star_black_24dp)
                }
                onClickUpdateIsFavorite(summit)
            }
        }

        private fun setMountainImage(summit: Summit, button: ImageButton) {
            if (summit.isPeak) {
                button.setImageResource(R.drawable.icons8_mountain_24)
            } else {
                button.setImageResource(R.drawable.icons8_valley_24)
            }
            button.setOnClickListener {
                if (summit.isPeak) {
                    button.setImageResource(R.drawable.icons8_valley_24)
                } else {
                    button.setImageResource(R.drawable.icons8_mountain_24)
                }
                onClickUpdateIsPeak(summit)
            }
        }

        private fun showDeleteEntryDialog(
            context: Context,
            entry: Summit,
            v: View
        ): AlertDialog? {
            return AlertDialog.Builder(context)
                .setTitle(
                    String.format(
                        context.resources.getString(R.string.delete_entry),
                        entry.name
                    )
                )
                .setMessage(context.resources.getString(R.string.delete_entry_text))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    deleteEntry(entry, v)
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

        private fun deleteEntry(entry: Summit, v: View) {
            onClickDelete(entry)
            if (entry.hasGpsTrack()) {
                entry.getGpsTrackPath().toFile()?.delete()
            }
            if (entry.hasImagePath()) {
                entry.imageIds.forEach {
                    entry.getImagePath(it).toFile().delete()
                }
            }
            Toast.makeText(
                v.context,
                v.context.getString(R.string.delete_entry, entry.name),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun setRecords(entity: Summit, segmentRecord: ImageView, powerRecord: ImageView) {
        if (entity.bestPositionInSegment in 1..3) {
            segmentRecord.visibility = View.VISIBLE
            when (entity.bestPositionInSegment) {
                1 -> segmentRecord.setColorFilter(Color.rgb(255, 215, 0))
                2 -> segmentRecord.setColorFilter(Color.rgb(192, 192, 192))
                3 -> segmentRecord.setColorFilter(Color.rgb(168, 112, 0))
                else -> segmentRecord.visibility = View.GONE
            }
        } else {
            segmentRecord.visibility = View.GONE
        }
        if (entity.hasPowerRecord) {
            powerRecord.visibility = View.VISIBLE
            powerRecord.setColorFilter(Color.rgb(255, 215, 0))
        } else {
            powerRecord.visibility = View.GONE
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<Summit>() {
        override fun areItemsTheSame(oldItem: Summit, newItem: Summit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Summit, newItem: Summit): Boolean {
            return oldItem == newItem
        }

    }
    val differ = AsyncListDiffer(this, differCallback)

}