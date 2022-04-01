package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import de.drtobiasprinz.summitbook.*
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.dialog.AddAdditionalDataFromExternalResourcesDialog
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog.Companion.updateInstance
import java.util.*


class SummitViewAdapter(private val resultReceiver: FragmentResultReceiver) : RecyclerView.Adapter<SummitViewAdapter.ViewHolder?>(), Filterable {
    private var cardView: CardView? = null
    val summitEntries = resultReceiver.getSortFilterHelper().entries
    lateinit var context: Context
    var summitEntriesFiltered: ArrayList<Summit>? = null

    override fun getItemCount(): Int {
        return summitEntriesFiltered?.size ?: 0
    }

    override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
    ): ViewHolder {
        cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_summit, parent, false) as CardView
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cardView = holder.cardView
        if (cardView != null) {
            context = cardView.context
            val entry = summitEntriesFiltered?.get(position)
            if (entry != null) {
                fillCardView(cardView, entry, position)
            }
        }
    }

    fun getItem(position: Int): Summit? {
        return summitEntriesFiltered?.get(position)
    }

    private fun fillCardView(cardView: CardView, summit: Summit, position: Int) {
        val textViewDate = cardView.findViewById<TextView?>(R.id.tour_date)
        textViewDate?.text = summit.getDateAsString()
        val textViewName = cardView.findViewById<TextView?>(R.id.summit_name)
        textViewName?.text = summit.name
        val textViewHeight = cardView.findViewById<TextView?>(R.id.height_meter)
        textViewHeight?.text = String.format("%s %s", summit.elevationData.elevationGain, cardView.resources.getString(R.string.hm))
        val imageViewSportType = cardView.findViewById<ImageView?>(R.id.sport_type_image)
        if (summit.hasImagePath()) {
            imageViewSportType.setImageResource(summit.sportType.imageIdWhite)
        } else {
            imageViewSportType.setImageResource(summit.sportType.imageIdBlack)
        }
        val image = cardView.findViewById<ImageView?>(R.id.card_view_image)
        val imageText = cardView.findViewById<RelativeLayout>(R.id.card_view_text)
        addImage(summit, imageText, textViewName, image, cardView)

        val favoriteButton = cardView.findViewById<ImageButton?>(R.id.entry_favorite)
        setFavoriteImage(summit, favoriteButton)

        val mountainButton = cardView.findViewById<ImageButton?>(R.id.entry_summit)
        setMountainImage(summit, mountainButton)

        val addImageButton = cardView.findViewById<ImageButton?>(R.id.entry_add_image)
        addImageButton.setOnClickListener { v: View? ->
            val context = v?.context
            val intent = Intent(context, AddImagesActivity::class.java)
            intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summit.id)
            intent.putExtra(SelectOnOsMapActivity.SUMMIT_POSITION, position)
            resultReceiver.getResultLauncher().launch(intent)
        }

        val addVelocityData = cardView.findViewById<ImageButton?>(R.id.entry_add_velocity_data)
        if (summit.velocityData.hasAdditionalData() || summit.elevationData.hasAdditionalData()) {
            addVelocityData?.setImageResource(R.drawable.ic_baseline_speed_24)
        } else {
            addVelocityData?.setImageResource(R.drawable.ic_baseline_more_time_24)
        }
        if ((summit.garminData == null || summit.garminData?.activityId == null) && !summit.hasGpsTrack()) {
            addVelocityData?.visibility = View.GONE
        } else {
            addVelocityData?.visibility = View.VISIBLE
        }
        addVelocityData?.setOnClickListener { _: View? ->
            AddAdditionalDataFromExternalResourcesDialog.getInstance(summit)
                    .show((context as FragmentActivity).supportFragmentManager, "Show addition data")
        }
        val removeButton = cardView.findViewById<ImageButton?>(R.id.entry_delete)
        //delete a summit entry
        removeButton?.setOnClickListener { v: View? ->
            v?.context?.let {
                showDeleteEntryDialog(it, summit, v)
            }
        }
        val editButton = cardView.findViewById<ImageButton?>(R.id.entry_edit)
        editButton?.setOnClickListener { _: View? ->
            val updateDialog = updateInstance(summit)
            MainActivity.mainActivity?.supportFragmentManager?.let { updateDialog.show(it, "Summits") }
        }
        val addPosition = cardView.findViewById<ImageButton?>(R.id.entry_add_coordinate)
        setIconForPositionButton(addPosition, summit)
        addPosition?.setOnClickListener { v: View? ->
            val context = v?.context
            val intent = Intent(context, SelectOnOsMapActivity::class.java)
            intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summit.id)
            intent.putExtra(SelectOnOsMapActivity.SUMMIT_POSITION, position)
            v?.context?.startActivity(intent)
        }
        cardView.setOnClickListener { v: View? ->
            val context = v?.context
            val intent = Intent(context, SummitEntryDetailsActivity::class.java)
            intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summit.id)
            v?.context?.startActivity(intent)
        }
    }

    private fun setFavoriteImage(summit: Summit, imageButton: ImageButton) {
        if (summit.isFavorite) {
            imageButton.setImageResource(R.drawable.ic_star_black_24dp)
        } else {
            imageButton.setImageResource(R.drawable.ic_star_border_black_24dp)
        }
        imageButton.setOnClickListener {
            if (summit.isFavorite) {
                imageButton.setImageResource(R.drawable.ic_star_border_black_24dp)
            } else {
                imageButton.setImageResource(R.drawable.ic_star_black_24dp)
            }
            summit.isFavorite = !summit.isFavorite
            resultReceiver.getSortFilterHelper().database.summitDao()?.updateIsFavorite(summit.id, summit.isFavorite)
        }
    }

    private fun setMountainImage(summit: Summit, mountainButton: ImageButton) {
        if (summit.isPeak) {
            mountainButton.setImageResource(R.drawable.icons8_mountain_24)
        } else {
            mountainButton.setImageResource(R.drawable.icons8_valley_24)
        }
        mountainButton.setOnClickListener {
            if (summit.isPeak) {
                mountainButton.setImageResource(R.drawable.icons8_valley_24)
            } else {
                mountainButton.setImageResource(R.drawable.icons8_mountain_24)
            }
            summit.isPeak = !summit.isPeak
            resultReceiver.getSortFilterHelper().database.summitDao()?.updateIsPeak(summit.id, summit.isPeak)
        }
    }

    private fun showDeleteEntryDialog(context: Context, entry: Summit, v: View): AlertDialog? {
        return AlertDialog.Builder(context)
                .setTitle(String.format(context.resources.getString(R.string.delete_entry), entry.name))
                .setMessage(context.resources.getString(R.string.delete_entry_text))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    deleteEntry(entry, v)
                }
                .setNegativeButton(android.R.string.cancel
                ) { _: DialogInterface?, _: Int ->
                    Toast.makeText(v.context, v.context.getString(R.string.delete_cancel),
                            Toast.LENGTH_SHORT).show()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    private fun deleteEntry(entry: Summit, v: View) {
        resultReceiver.getSortFilterHelper().database.summitDao()?.delete(entry)
        if (entry.hasGpsTrack()) {
            entry.getGpsTrackPath().toFile()?.delete()
        }
        if (entry.hasImagePath()) {
            entry.imageIds.forEach {
                entry.getImagePath(it).toFile().delete()
            }
        }
        resultReceiver.getSortFilterHelper().entries.remove(entry)
        summitEntriesFiltered?.remove(entry)
        notifyDataSetChanged()
        Toast.makeText(v.context, v.context.getString(R.string.delete_entry, entry.name), Toast.LENGTH_SHORT).show()
    }

    fun updateIsPeak(entry: Summit, position: Int) {
        entry.isPeak = !entry.isPeak
        resultReceiver.getSortFilterHelper().database.summitDao()?.updateIsPeak(entry.id, entry.isPeak)
        notifyItemChanged(position)
    }

    fun updateIsFavorite(entry: Summit, position: Int) {
        entry.isFavorite = !entry.isFavorite
        resultReceiver.getSortFilterHelper().database.summitDao()?.updateIsFavorite(entry.id, entry.isFavorite)
        notifyItemChanged(position)
    }

    private fun addImage(entry: Summit, imageText: RelativeLayout, textViewName: TextView, image: ImageView, cardView: CardView) {
        if (entry.hasImagePath()) {
            imageText.setBackgroundResource(R.color.translucent)
            textViewName.setTextColor(Color.WHITE)
            image.visibility = View.VISIBLE
            cardView.context?.let {
                Glide.with(it)
                        .load("file://" + entry.getImagePath(entry.imageIds.first()))
                        .centerInside()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(image)
            }
        } else {
            imageText.setBackgroundColor(Color.TRANSPARENT)
            textViewName.setTextColor(Color.BLACK)
            image.visibility = View.GONE
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val charString = charSequence.toString()
                summitEntriesFiltered = if (charString.isEmpty()) {
                    resultReceiver.getSortFilterHelper().entries
                } else {
                    val filteredList = ArrayList<Summit>()
                    for (entry in resultReceiver.getSortFilterHelper().entries) {
                        if (entry.name.contains(charString, ignoreCase = true) || entry.comments.contains(charString, ignoreCase = true) || entry.places.joinToString(";").contains(charString, ignoreCase = true)) {
                            filteredList.add(entry)
                        }
                    }
                    filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = summitEntriesFiltered
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults?) {
                summitEntriesFiltered = filterResults?.values as ArrayList<Summit>
                notifyDataSetChanged()
            }
        }
    }

    fun setFilteredSummitEntries(entries: List<Summit>?) {
        summitEntriesFiltered = entries as ArrayList<Summit>?
        notifyDataSetChanged()
    }

    class ViewHolder internal constructor(val cardView: CardView?) : RecyclerView.ViewHolder(cardView!!)

    companion object {
        fun setIconForPositionButton(addPosition: ImageButton?, entry: Summit?) {
            if (entry?.latLng == null) {
                addPosition?.setImageResource(R.drawable.ic_add_location_black_24dp)
            } else {
                addPosition?.setImageResource(R.drawable.ic_edit_location_black_24dp)
            }
        }
    }

    init {
        summitEntriesFiltered = resultReceiver.getSortFilterHelper().entries
    }
}