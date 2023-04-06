package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
import de.drtobiasprinz.summitbook.databinding.ItemContactsBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.AddContactFragment
import de.drtobiasprinz.summitbook.ui.dialog.AddAdditionalDataFromExternalResourcesDialog
import de.drtobiasprinz.summitbook.utils.Constants
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import javax.inject.Singleton


@Singleton
class ContactsAdapter() : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    private lateinit var binding: ItemContactsBinding
    private lateinit var context: Context

    var viewModel: DatabaseViewModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = ItemContactsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return ViewHolder()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(differ.currentList[position])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class ViewHolder : RecyclerView.ViewHolder(binding.root) {
        fun setData(entity: Summit) {
            binding.apply {
                summitName.text = entity.name
                tourDate.text = entity.getDateAsString()
                heightMeter.text = String.format(
                    "%s %s",
                    entity.elevationData.elevationGain,
                    context.getString(R.string.hm)
                )
                if (entity.hasImagePath()) {
                    sportTypeImage.setImageResource(entity.sportType.imageIdWhite)
                } else {
                    sportTypeImage.setImageResource(entity.sportType.imageIdBlack)
                }
                addImage(entity)
                setFavoriteImage(entity, entryFavorite)
                setMountainImage(entity, entrySummit)

                entryAddImage.setOnClickListener { v: View? ->
                    val context = v?.context
                    val intent = Intent(context, AddImagesActivity::class.java)
                    intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, entity.id)
                    v?.context?.startActivity(intent)
                }

                if (entity.velocityData.hasAdditionalData() || entity.elevationData.hasAdditionalData()) {
                    entryAddVelocityData.setImageResource(R.drawable.ic_baseline_speed_24)
                } else {
                    entryAddVelocityData.setImageResource(R.drawable.ic_baseline_more_time_24)
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
                entryDelete.setOnClickListener { v: View? ->
                    v?.context?.let {
                        showDeleteEntryDialog(it, entity, v)
                    }
                }
                entryEdit.setOnClickListener { v: View? ->
                    val addContactFragment = AddContactFragment()
                    val bundle = Bundle()
                    bundle.putLong(Constants.BUNDLE_ID, entity.id)
                    addContactFragment.arguments = bundle
                    addContactFragment.show(
                        (FragmentComponentManager.findActivity(v?.context) as FragmentActivity).supportFragmentManager,
                        AddContactFragment().tag
                    )
                }
                setIconForPositionButton(entryAddCoordinate, entity)
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
            }

        }

        private fun ItemContactsBinding.addImage(item: Summit) {
            if (item.hasImagePath()) {
                cardViewText.setBackgroundResource(R.color.translucent)
                summitName.setTextColor(Color.WHITE)
                cardViewImage.visibility = View.VISIBLE
                Glide.with(context)
                    .load("file://" + item.getImagePath(item.imageIds.first()))
                    .centerInside()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(cardViewImage)
            } else {
                cardViewText.setBackgroundColor(Color.TRANSPARENT)
                summitName.setTextColor(Color.BLACK)
                cardViewImage.visibility = View.GONE
            }
        }

        private fun setFavoriteImage(summit: Summit, button: ImageButton) {
            if (summit.isFavorite) {
                button.setImageResource(R.drawable.ic_star_black_24dp)
            } else {
                button.setImageResource(R.drawable.ic_star_border_black_24dp)
            }
            button.setOnClickListener {
                if (summit.isFavorite) {
                    button.setImageResource(R.drawable.ic_star_border_black_24dp)
                } else {
                    button.setImageResource(R.drawable.ic_star_black_24dp)
                }
                summit.isFavorite = !summit.isFavorite
                viewModel?.saveContact(true, summit)
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
                summit.isPeak = !summit.isPeak

                viewModel?.saveContact(true, summit)
            }
        }

        private fun showDeleteEntryDialog(context: Context, entry: Summit, v: View): AlertDialog? {
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
            viewModel?.deleteContact(entry)
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

    private val differCallback = object : DiffUtil.ItemCallback<Summit>() {
        override fun areItemsTheSame(oldItem: Summit, newItem: Summit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Summit, newItem: Summit): Boolean {
            return oldItem == newItem
        }

    }
    val differ = AsyncListDiffer(this, differCallback)

    companion object {
        fun setIconForPositionButton(addPosition: ImageButton?, entry: Summit?) {
            if (entry?.latLng == null) {
                addPosition?.setImageResource(R.drawable.ic_add_location_black_24dp)
            } else {
                addPosition?.setImageResource(R.drawable.ic_edit_location_black_24dp)
            }
        }
    }
}