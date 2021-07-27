package de.drtobiasprinz.summitbook.adapter

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.models.VelocityData
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog.Companion.updateInstance
import de.drtobiasprinz.summitbook.ui.utils.*
import java.io.File
import java.util.*


class SummitViewAdapter(private val sortFilterHelper: SortFilterHelper, private val pythonExecutor: GarminPythonExecutor?) : RecyclerView.Adapter<SummitViewAdapter.ViewHolder?>(), Filterable {
    var cardView: CardView? = null
    val summitEntries = sortFilterHelper.entries
    lateinit var context: Context
    var summitEntriesFiltered: ArrayList<SummitEntry>?
    override fun getItemCount(): Int {
        return summitEntriesFiltered?.size ?: 0
    }

    fun sort(filterHelper: SortFilterHelper) {
        filterHelper.setAllEntries(sortFilterHelper.entries)
        if (sortFilterHelper.entries.size > 0) {
            filterHelper.showDialog()
            filterHelper.apply()
        }
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
                fillCardView(cardView, entry, sortFilterHelper.database, position)
            }
        }
    }

    fun getItem(position: Int): SummitEntry? {
        return summitEntriesFiltered?.get(position)
    }

    private fun fillCardView(cardView: CardView, summitEntry: SummitEntry, db: SQLiteDatabase, position: Int) {
        val textViewDate = cardView.findViewById<TextView?>(R.id.tour_date)
        textViewDate?.text = summitEntry.getDateAsString()
        val textViewName = cardView.findViewById<TextView?>(R.id.summit_name)
        textViewName?.text = summitEntry.name
        val textViewHeight = cardView.findViewById<TextView?>(R.id.height_meter)
        textViewHeight?.text = String.format("%s hm", summitEntry.elevationData.elevationGain)
        val imageViewSportType = cardView.findViewById<ImageView?>(R.id.sport_type_image)
        summitEntry.sportType.imageId.let { imageViewSportType?.setImageResource(it) }
        val image = cardView.findViewById<ImageView?>(R.id.card_view_image)
        val imageText = cardView.findViewById<RelativeLayout?>(R.id.card_view_text)
        if (imageText != null) {
            addImage(summitEntry, imageText, textViewName, image, cardView)
        }
        val setFavoriteButton = cardView.findViewById<ImageButton?>(R.id.entry_favorite)
        if (summitEntry.isFavorite) {
            setFavoriteButton?.setImageResource(R.drawable.ic_star_black_24dp)
        } else {
            setFavoriteButton?.setImageResource(R.drawable.ic_star_border_black_24dp)
        }
        setFavoriteButton?.setOnClickListener { _: View? ->
            if (summitEntry.isFavorite) {
                setFavoriteButton.setImageResource(R.drawable.ic_star_border_black_24dp)
            } else {
                setFavoriteButton.setImageResource(R.drawable.ic_star_black_24dp)
            }
            summitEntry.isFavorite = !summitEntry.isFavorite
            sortFilterHelper.databaseHelper.updateIsFavoriteSummit(db, summitEntry._id, summitEntry.isFavorite)
        }
        val addImageButton = cardView.findViewById<ImageButton?>(R.id.entry_add_image)
        val listener = ImagePickerListner()
        addImageButton?.let { listener.setListener(it, summitEntry, this, db, sortFilterHelper.databaseHelper) }
        val addVelocityData = cardView.findViewById<ImageButton?>(R.id.entry_add_velocity_data)
        if (summitEntry.velocityData.oneKilometer > 0) {
            addVelocityData?.setImageResource(R.drawable.ic_baseline_speed_24)
        } else {
            addVelocityData?.setImageResource(R.drawable.ic_baseline_more_time_24)
        }
        if (summitEntry.activityData == null || summitEntry.activityData?.activityId == null) {
            addVelocityData?.visibility = View.GONE
        }
        addVelocityData?.setOnClickListener { v: View? ->
            if (pythonExecutor != null && summitEntry.activityData != null && summitEntry.activityData?.activityId != null) {
                if (summitEntry.hasGpsTrack()) {
                    if (summitEntry.gpsTrack == null) {
                        summitEntry.setGpsTrack()
                    }
                    val gpsTrack = summitEntry.gpsTrack
                    if (gpsTrack != null) {
                        if (gpsTrack.hasNoTrackPoints()) {
                            gpsTrack.parseTrack()
                        }
                        val slopeCalculator = SummitSlope()
                        slopeCalculator.calculateMaxSlope(summitEntry.gpsTrack?.gpxTrack)
                        summitEntry.elevationData.maxSlope = slopeCalculator.maxSlope
                        if (summitEntry.elevationData.maxSlope > 0) {
                            sortFilterHelper.databaseHelper.updateElevationDataSummit(sortFilterHelper.database, summitEntry._id, summitEntry.elevationData)
                        }
                    }
                }
                if (summitEntry.velocityData.oneKilometer > 0) {
                    summitEntry.velocityData = VelocityData(summitEntry.velocityData.avgVelocity, summitEntry.velocityData.maxVelocity, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                    sortFilterHelper.databaseHelper.updateVelocityDataSummit(sortFilterHelper.database, summitEntry._id, summitEntry.velocityData)
                    addVelocityData.setImageResource(R.drawable.ic_baseline_more_time_24)
                } else {
                    val splitsFile = File("${SummitViewFragment.activitiesDir.absolutePath}/activity_${summitEntry.activityData?.activityId}_splits.json")
                    if (splitsFile.exists()) {
                        val json = JsonParser().parse(GarminConnectAccess.getJsonData(splitsFile)) as JsonObject
                        setVelocityData(json, summitEntry, sortFilterHelper, addVelocityData)
                    } else {
                        AsyncDownloadSpeedDataForActivity(summitEntry, pythonExecutor, sortFilterHelper, addVelocityData).execute()
                    }
                }
            } else {
                Toast.makeText(v?.context, v?.context?.getString(R.string.speed_data_failed),
                        Toast.LENGTH_SHORT).show()
            }
        }
        val removeButton = cardView.findViewById<ImageButton?>(R.id.entry_delete)
        //delete a summit entry
        removeButton?.setOnClickListener { v: View? ->
            v?.context?.let {
                showDeleteEntryDialog(it, summitEntry, v)
            }
        }
        val editButton = cardView.findViewById<ImageButton?>(R.id.entry_edit)
        editButton?.setOnClickListener { _: View? ->
            val updateDialog = sortFilterHelper.let { updateInstance(summitEntry, it, pythonExecutor) }
            MainActivity.mainActivity?.supportFragmentManager?.let { updateDialog.show(it, "Summits") }
        }
        val addPosition = cardView.findViewById<ImageButton?>(R.id.entry_add_coordinate)
        setIconForPositionButton(addPosition, summitEntry)
        addPosition?.setOnClickListener { v: View? ->
            val context = v?.context
            val intent = Intent(context, SelectOnOsMapActivity::class.java)
            intent.putExtra(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry._id)
            intent.putExtra(SelectOnOsMapActivity.SUMMIT_POSITION, position)
            v?.context?.startActivity(intent)
        }
        cardView.setOnClickListener { v: View? ->
            val context = v?.context
            val intent = Intent(context, SummitEntryDetailsActivity::class.java)
            intent.putExtra(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry._id)
            v?.context?.startActivity(intent)
        }
    }

    fun showDeleteEntryDialog(it: Context, entry: SummitEntry, v: View): AlertDialog? {
        return AlertDialog.Builder(it)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    deleteEntry(sortFilterHelper.database, entry, v)
                }
                .setNegativeButton(android.R.string.no
                ) { _: DialogInterface?, _: Int ->
                    Toast.makeText(v.context, v.context.getString(R.string.delete_cancel),
                            Toast.LENGTH_SHORT).show()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    private fun deleteEntry(db: SQLiteDatabase?, entry: SummitEntry, v: View) {
        val taskState = db?.let { sortFilterHelper.databaseHelper.deleteSummit(it, entry) }
                ?: false
        if (taskState) {
            if (entry.hasGpsTrack()) {
                entry.getGpsTrackPath()?.toFile()?.delete()
            }
            if (entry.hasImagePath()) {
                entry.imageIds.forEach {
                    entry.getImagePath(it).toFile().delete()
                }
            }
            sortFilterHelper.entries.remove(entry)
            summitEntriesFiltered?.remove(entry)
            notifyDataSetChanged()
            Toast.makeText(v.context, v.context.getString(R.string.delete_entry, entry.name), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(v.context, "An error occurred, please try again.",
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun addImage(entry: SummitEntry, imageText: RelativeLayout, textViewName: TextView?, image: ImageView?, cardView: CardView?) {
        if (entry.hasImagePath()) {
            imageText.setBackgroundResource(R.color.translucent)
            textViewName?.setTextColor(Color.WHITE)
            image?.visibility = View.VISIBLE
            cardView?.context?.let {
                if (image != null) {
                    Glide.with(it)
                            .load("file://" + entry.getImagePath(entry.imageIds.first()))
                            .centerInside()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(image)
                }
            }
        } else {
            imageText.setBackgroundColor(Color.TRANSPARENT)
            textViewName?.setTextColor(Color.BLACK)
            image?.visibility = View.GONE
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val charString = charSequence.toString()
                summitEntriesFiltered = if (charString.isEmpty()) {
                    sortFilterHelper.entries
                } else {
                    val filteredList = ArrayList<SummitEntry>()
                    for (entry in sortFilterHelper.entries) {
                        val name: String = entry.name
                        val comments: String = entry.comments
                        if (name.contains(charString, ignoreCase = true) || comments.contains(charString, ignoreCase = true)) {
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
                summitEntriesFiltered = filterResults?.values as ArrayList<SummitEntry>
                notifyDataSetChanged()
            }
        }
    }

    fun setFilteredSummitEntries(entries: ArrayList<SummitEntry>?) {
        summitEntriesFiltered = entries
        notifyDataSetChanged()
    }

    class ViewHolder internal constructor(val cardView: CardView?) : RecyclerView.ViewHolder(cardView!!)

    companion object {
        fun setIconForPositionButton(addPosition: ImageButton?, entry: SummitEntry?) {
            if (entry?.latLng == null) {
                addPosition?.setImageResource(R.drawable.ic_add_location_black_24dp)
            } else {
                addPosition?.setImageResource(R.drawable.ic_edit_location_black_24dp)
            }
        }

        class AsyncDownloadSpeedDataForActivity(private val summit: SummitEntry, private val pythonExecutor: GarminPythonExecutor, var sortFilterHelper: SortFilterHelper, val addVelocityData: ImageButton) : AsyncTask<Void?, Void?, Void?>() {
            var json: JsonObject? = null
            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    summit.activityData?.activityId?.let { json = pythonExecutor.downloadSpeedDataForActivity(SummitViewFragment.activitiesDir, it) }
                } catch (e: java.lang.RuntimeException) {
                    Log.e("AsyncDownloadActivities", e.message ?: "")
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                val jsonLocal = json
                if (jsonLocal != null) {
                    setVelocityData(jsonLocal, summit, sortFilterHelper, addVelocityData)
                }
            }
        }

        private fun setVelocityData(jsonLocal: JsonObject, summit: SummitEntry, sortFilterHelper: SortFilterHelper, addVelocityData: ImageButton? = null) {
            val maxVelocitySummit = MaxVelocitySummit()
            val velocityEntries = maxVelocitySummit.parseFomGarmin(jsonLocal)
            summit.velocityData.oneKilometer = maxVelocitySummit.getAverageVelocityForKilometers(1.0, velocityEntries)
            if (summit.velocityData.oneKilometer > 0) summit.velocityData.fiveKilometer = maxVelocitySummit.getAverageVelocityForKilometers(5.0, velocityEntries)
            if (summit.velocityData.fiveKilometer > 0) summit.velocityData.tenKilometers = maxVelocitySummit.getAverageVelocityForKilometers(10.0, velocityEntries)
            if (summit.velocityData.tenKilometers > 0) summit.velocityData.fifteenKilometers = maxVelocitySummit.getAverageVelocityForKilometers(15.0, velocityEntries)
            if (summit.velocityData.fifteenKilometers > 0) summit.velocityData.twentyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(20.0, velocityEntries)
            if (summit.velocityData.twentyKilometers > 0) summit.velocityData.thirtyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(30.0, velocityEntries)
            if (summit.velocityData.thirtyKilometers > 0) summit.velocityData.fortyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(40.0, velocityEntries)
            if (summit.velocityData.fortyKilometers > 0) summit.velocityData.fiftyKilometers = maxVelocitySummit.getAverageVelocityForKilometers(50.0, velocityEntries)
            if (summit.velocityData.fiftyKilometers > 0) summit.velocityData.seventyFiveKilometers = maxVelocitySummit.getAverageVelocityForKilometers(75.0, velocityEntries)
            if (summit.velocityData.seventyFiveKilometers > 0) summit.velocityData.hundredKilometers = maxVelocitySummit.getAverageVelocityForKilometers(100.0, velocityEntries)
            sortFilterHelper.databaseHelper.updateVelocityDataSummit(sortFilterHelper.database, summit._id, summit.velocityData)
            addVelocityData?.setImageResource(R.drawable.ic_baseline_speed_24)
        }

    }

    init {
        summitEntriesFiltered = sortFilterHelper.entries
    }
}