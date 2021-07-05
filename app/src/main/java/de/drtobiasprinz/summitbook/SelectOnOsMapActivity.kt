package de.drtobiasprinz.summitbook

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.Drawable
import android.location.Address
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.model.LatLng
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter.Companion.setIconForPositionButton
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment.Companion.adapter
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment.Companion.summitRecycler
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addTrackAndMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
import de.drtobiasprinz.gpx.GPXParser
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.drawBoundingBox
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class SelectOnOsMapActivity : FragmentActivity() {
    private var latLngSelectedPosition: LatLng? = null
    private var summitEntry: SummitEntry? = null
    private lateinit var helper: SummitBookDatabaseHelper
    private lateinit var database: SQLiteDatabase
    private var selectedGpsPath: Path? = null
    private var osMap: MapView? = null
    private lateinit var savePositionButton: ImageButton
    private lateinit var closeDialogButton: ImageButton
    private lateinit var addGpsTrackButton: ImageButton
    private lateinit var deletButton: ImageButton
    private lateinit var searchForLocation: EditText
    private var summitEntryId = 0
    private var summitEntryPosition = 0
    private var wasBoundingBoxCalculated: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_on_osmap)
        helper = SummitBookDatabaseHelper(this)
        database = helper.writableDatabase
        val searchPanel = findViewById<View>(R.id.search_panel)
        val expander = findViewById<View>(R.id.expander)
        expander.setOnClickListener {
            if (searchPanel.visibility == View.VISIBLE) {
                searchPanel.visibility = View.GONE
            } else {
                searchPanel.visibility = View.VISIBLE
            }
        }

        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val bundle = intent.extras
        if (bundle != null) {
            summitEntryId = bundle.getInt(SUMMIT_ID_EXTRA_IDENTIFIER)
            summitEntryPosition = bundle.getInt(SUMMIT_POSITION)
            summitEntry = helper.getSummitsWithId(summitEntryId, database)
            searchForLocation = findViewById(R.id.editLocation)
            searchForLocation.setText(summitEntry?.name)

        }
        osMap = findViewById(R.id.osmap)
        val localOsMap = osMap
        if (localOsMap != null) {
            localOsMap.setTileSource(TileSourceFactory.MAPNIK)
            Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
            val entry = summitEntry
            if (entry != null) {
                addTrackAndMarker(entry, localOsMap, this, false, isMilageButtonShown = false, alwaysShowTrackOnMap = false)
                entry.trackBoundingBox?.let { drawBoundingBox(osMap, it) }
            }
            addDefaultSettings(this, localOsMap, this)
            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    if (entry != null) {
                        updateSavePositionButton(true)
                        latLngSelectedPosition = LatLng(p.latitude, p.longitude)
                        addMarker(localOsMap, applicationContext, p, entry)
                        prepareGpxTrack()?.let { addSelectedPositionAndTrack(LatLng(p.latitude, p.longitude), it, localOsMap) }
                    }
                    return false
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    return false
                }
            }
            localOsMap.overlays.add(MapEventsOverlay(mReceive))
        }

        val searchButton: Button = findViewById(R.id.buttonSearchLocation)
        searchButton.setOnClickListener {
            if (localOsMap != null) {
                searchForAddress(localOsMap, searchForLocation.text.toString())
            }
        }

        savePositionButton = findViewById(R.id.add_position_save)
        updateSavePositionButton(false)
        savePositionButton.setOnClickListener { v: View ->
            val entry = summitEntry
            val position = latLngSelectedPosition
            if (entry != null) {
                if (position != null) {
                    helper.updatePositionOfSummit(database, entry._id, position)
                    entry.latLng = latLngSelectedPosition
                    finish()
                    Toast.makeText(v.context, "Adding a new position to summit " +
                            entry.name + " was successful.", Toast.LENGTH_SHORT).show()
                }
                val localSelectedPath = selectedGpsPath
                if (localSelectedPath != null) {
                    val fileDest = entry.getGpsTrackPath()
                    try {
                        if (fileDest != null) {
                            Files.copy(localSelectedPath, fileDest, StandardCopyOption.REPLACE_EXISTING)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                entry.setBoundingBoxFromTrack()
                val trackBoundingBox = entry.trackBoundingBox
                if (trackBoundingBox != null) {
                    helper.updateTrackBoundingBox(database, entry._id, trackBoundingBox)
                }
                val entries = adapter.summitEntries
                for (i in entries.indices) {
                    if (entries[i]._id == summitEntryId) {
                        entries[0] = entry
                    }
                }
                val holder = summitRecycler.findViewHolderForAdapterPosition(summitEntryPosition) as SummitViewAdapter.ViewHolder
                val addButton = holder.cardView?.findViewById<ImageButton>(R.id.entry_add_coordinate)
                setIconForPositionButton(addButton, entry)
            }
        }
        closeDialogButton = findViewById(R.id.add_position_cancel)
        closeDialogButton.setOnClickListener { v: View ->
            finish()
            Toast.makeText(v.context, "Adding a new position was canceled.", Toast.LENGTH_SHORT).show()
        }
        deletButton = findViewById(R.id.add_position_delete)
        deletButton.setOnClickListener { v: View ->
            val entry = summitEntry
            if (entry != null) {
                AlertDialog.Builder(v.context)
                        .setTitle("Delete coordinates and GPS track")
                        .setMessage("Are you sure you want to delete the selected coordinates and the added GPS track?")
                        .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                            selectedGpsPath = null
                            if (entry.hasGpsTrack()) {
                                val gpsTrackPath = entry.getGpsTrackPath()
                                gpsTrackPath?.toFile()?.delete()
                            }
                            entry.latLng = LatLng(0.0, 0.0)
                            helper.updatePositionOfSummit(database, summitEntryId, LatLng(0.0, 0.0))
                            finish()
                            Toast.makeText(v.context, v.context.getString(R.string.delete_gps, entry.name), Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton(android.R.string.no
                        ) { _: DialogInterface?, _: Int ->
                            Toast.makeText(v.context, getString(R.string.delete_cancel), Toast.LENGTH_SHORT).show()
                        }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
            }
        }
        addGpsTrackButton = findViewById(R.id.add_gps_track)
        addGpsTrackButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, PICK_GPX_FILE)
        }
    }

    private fun searchForAddress(localOsMap: MapView, locationAddress: String) {
        val geoCoder = GeocoderNominatim(BuildConfig.APPLICATION_ID)
        val viewBox: BoundingBox = localOsMap.boundingBox
        val foundAddresses: List<Address> = geoCoder.getFromLocationName(locationAddress, 1,
                viewBox.latSouth, viewBox.lonEast, viewBox.latNorth, viewBox.lonWest, false)
        if (foundAddresses.isNotEmpty()) {
            Toast.makeText(applicationContext, getString(R.string.found_adress, locationAddress), Toast.LENGTH_SHORT).show()
            val address = foundAddresses[0]
            val geoPoint = GeoPoint(address.latitude, address.longitude)
            val poiIcon: Drawable? = ResourcesCompat.getDrawable(resources, R.drawable.ic_filled_location_black_48, null)
            localOsMap.setExpectedCenter(geoPoint)
            val poiMarker = Marker(osMap)
            poiMarker.title = address.getAddressLine(0)
            poiMarker.snippet = address.getAddressLine(1)
            poiMarker.position = geoPoint
            poiMarker.relatedObject = address
            poiMarker.icon = poiIcon
            poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            poiMarker.setInfoWindow(CustomInfoWindow(localOsMap))
            localOsMap.overlays.add(poiMarker)
            localOsMap.setExpectedCenter(geoPoint)
        } else {
            Toast.makeText(applicationContext, getString(R.string.not_found, locationAddress), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_GPX_FILE && resultCode == Activity.RESULT_OK) {
            val file = File(MainActivity.cache, "new_gpx_track.gpx")
            resultData?.data?.also { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    copyGpxFileToCache(inputStream, file)
                }
            }
            osMap?.let { addGpxTrack(file, GPXParser(), it) }
        }
    }

    private fun copyGpxFileToCache(inputStream: InputStream, file: File) {
        var out: OutputStream? = null
        try {
            out = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun addGpxTrack(file: File, mParser: GPXParser, osMap: MapView) {
        val inputStream: InputStream = FileInputStream(file)
        mParser.parse(inputStream)
        selectedGpsPath = file.toPath()
        val gpsTrack = prepareGpxTrack()
        val highestTrackPoint = gpsTrack?.getHighestElevation()
        if (highestTrackPoint != null) {
            latLngSelectedPosition = LatLng(highestTrackPoint.lat, highestTrackPoint.lon)
            addSelectedPositionAndTrack(LatLng(highestTrackPoint.lat, highestTrackPoint.lon), gpsTrack, osMap)
            updateSavePositionButton(true)
        }
    }

    private fun prepareGpxTrack(): GpsTrack? {
        val path = selectedGpsPath
        val entry = summitEntry
        val gpsTrack = if (path != null && path.toFile().exists()) {
            GpsTrack(path)
        } else if (entry != null && entry.hasGpsTrack()) {
            entry.gpsTrack
        } else {
            null
        }
        if (gpsTrack != null && gpsTrack.hasNoTrackPoints()) {
            gpsTrack.parseTrack()
        }
        return gpsTrack
    }

    private fun addSelectedPositionAndTrack(point: LatLng, gpsTrack: GpsTrack, osMap: MapView) {
        val entry = summitEntry
        if (entry != null) {
            val geoPointSelectedPosition = GeoPoint(point.latitude, point.longitude)
            addMarker(osMap, this, geoPointSelectedPosition, entry)
            gpsTrack.addGpsTrack(osMap, false)
            if (!wasBoundingBoxCalculated) {
                calculateBoundingBox(osMap, gpsTrack, geoPointSelectedPosition)
                wasBoundingBoxCalculated = true
            }
        }
    }

    private fun updateSavePositionButton(enable: Boolean) {
        savePositionButton.isEnabled = enable
        if (enable) {
            savePositionButton.setImageResource(R.drawable.ic_save_black_24dp)
        } else {
            savePositionButton.setImageResource(R.drawable.ic_save_grey_400_24dp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
        helper.close()
    }

    internal class CustomInfoWindow(mapView: MapView?) : MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView) {
        private var mSelectedPoi: Address? = null
        override fun onOpen(item: Any) {
            super.onOpen(item)
            val button: Button = mView.findViewById(org.osmdroid.bonuspack.R.id.bubble_moreinfo)
            button.visibility = View.VISIBLE
            val marker: Marker = item as Marker
            mSelectedPoi = marker.relatedObject as Address
        }
    }

    companion object {
        const val SUMMIT_POSITION = "SUMMIT_POSITION"
        const val PICK_GPX_FILE = 103
        var SUMMIT_ID_EXTRA_IDENTIFIER = "SUMMIT_ID"
    }
}