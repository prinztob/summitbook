package de.drtobiasprinz.summitbook

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.Address
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import de.drtobiasprinz.gpx.GPXParser
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter
import de.drtobiasprinz.summitbook.adapter.SummitViewAdapter.Companion.setIconForPositionButton
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment.Companion.summitRecycler
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addTrackAndMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
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
    private var latLngSelectedPosition: TrackPoint? = null
    private var summitEntry: Summit? = null
    private var database: AppDatabase? = null
    private var selectedGpsPath: Path? = null
    private var osMap: MapView? = null
    private lateinit var savePositionButton: ImageButton
    private lateinit var closeDialogButton: ImageButton
    private lateinit var addGpsTrackButton: ImageButton
    private lateinit var deletButton: ImageButton
    private lateinit var searchForLocation: EditText
    private var summitEntryId = 0L
    private var summitEntryPosition = 0
    private var wasBoundingBoxCalculated: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_on_osmap)
        database = AppDatabase.getDatabase(applicationContext)
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
            summitEntryId = bundle.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            summitEntryPosition = bundle.getInt(SUMMIT_POSITION)
            summitEntry = database?.summitDao()?.getSummit(summitEntryId)

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
                addTrackAndMarker(entry, localOsMap, this, false, TrackColor.None, alwaysShowTrackOnMap = false)
                entry.trackBoundingBox?.let { drawBoundingBox(osMap, it) }
            }
            addDefaultSettings(this, localOsMap, this)
            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    if (entry != null) {
                        updateSavePositionButton(true)
                        latLngSelectedPosition = TrackPoint(p.latitude, p.longitude)
                        addMarker(localOsMap, applicationContext, p, entry)
                        prepareGpxTrack(selectedGpsPath, summitEntry)?.let { addSelectedPositionAndTrack(TrackPoint(p.latitude, p.longitude), it, localOsMap) }
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
                    entry.lat = position.lat
                    entry.lng = position.lon
                    database?.summitDao()?.updateLat(entry.id, position.lat)
                    database?.summitDao()?.updateLng(entry.id, position.lon)
                    entry.latLng = latLngSelectedPosition
                    finish()
                    Toast.makeText(v.context, String.format(getString(R.string.no_email_program_installed), entry.name), Toast.LENGTH_SHORT).show()
                }
                val localSelectedPath = selectedGpsPath
                if (localSelectedPath != null) {
                    val fileDest = entry.getGpsTrackPath()
                    try {
                        Files.copy(localSelectedPath, fileDest, StandardCopyOption.REPLACE_EXISTING)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                entry.setBoundingBoxFromTrack()
                if (entry.trackBoundingBox != null) {
                    database?.summitDao()?.updateSummit(entry)
                }
                val holder = summitRecycler.findViewHolderForAdapterPosition(summitEntryPosition) as SummitViewAdapter.ViewHolder
                val addButton = holder.cardView?.findViewById<ImageButton>(R.id.entry_add_coordinate)
                setIconForPositionButton(addButton, entry)
            }
        }
        closeDialogButton = findViewById(R.id.add_position_cancel)
        closeDialogButton.setOnClickListener { v: View ->
            finish()
            Toast.makeText(v.context, getString(R.string.add_position_to_summit_cancel, summitEntry?.name), Toast.LENGTH_SHORT).show()
        }
        deletButton = findViewById(R.id.add_position_delete)
        deletButton.setOnClickListener { v: View ->
            val entry = summitEntry
            if (entry != null) {
                AlertDialog.Builder(v.context)
                        .setTitle(getString(R.string.delete_coordinates))
                        .setMessage(getString(R.string.delete_coordinates_message))
                        .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                            selectedGpsPath = null
                            if (entry.hasGpsTrack()) {
                                val gpsTrackPath = entry.getGpsTrackPath()
                                gpsTrackPath.toFile()?.delete()
                            }
                            entry.latLng = TrackPoint(0.0, 0.0)

                            database?.summitDao()?.updateLat(entry.id, 0.0)
                            database?.summitDao()?.updateLng(entry.id, 0.0)
                            finish()
                            Toast.makeText(v.context, v.context.getString(R.string.delete_gps, entry.name), Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton(android.R.string.cancel
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
            resultLauncherForAddingGpxTrack.launch(intent)
        }

        val data = Intent()
        data.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry?.id)
        setResult(Activity.RESULT_OK, data)
    }

    private fun searchForAddress(localOsMap: MapView, locationAddress: String) {
        val geoCoder = GeocoderNominatim(BuildConfig.APPLICATION_ID)
        val viewBox: BoundingBox = localOsMap.boundingBox
        val foundAddresses: List<Address> = geoCoder.getFromLocationName(locationAddress, 1,
                viewBox.latSouth, viewBox.lonEast, viewBox.latNorth, viewBox.lonWest, false)
        if (foundAddresses.isNotEmpty()) {
            Toast.makeText(applicationContext, getString(R.string.found_address, locationAddress), Toast.LENGTH_SHORT).show()
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


    private fun addGpxTrack(file: File, mParser: GPXParser, osMap: MapView) {
        val inputStream: InputStream = FileInputStream(file)
        mParser.parse(inputStream)
        selectedGpsPath = file.toPath()
        val gpsTrack = prepareGpxTrack(selectedGpsPath, summitEntry)
        val highestTrackPoint = gpsTrack?.getHighestElevation()
        if (highestTrackPoint != null) {
            latLngSelectedPosition = highestTrackPoint
            addSelectedPositionAndTrack(highestTrackPoint, gpsTrack, osMap)
            updateSavePositionButton(true)
        }
    }

    private fun addSelectedPositionAndTrack(point: TrackPoint, gpsTrack: GpsTrack, osMap: MapView) {
        val entry = summitEntry
        if (entry != null) {
            val geoPointSelectedPosition = GeoPoint(point.lat, point.lon)
            addMarker(osMap, this, geoPointSelectedPosition, entry)
            gpsTrack.addGpsTrack(osMap, TrackColor.None)
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
        database?.close()
    }

    private val resultLauncherForAddingGpxTrack = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val file = File(MainActivity.cache, "new_gpx_track.gpx")
            result?.data?.data?.also { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    copyGpxFileToCache(inputStream, file)
                }
            }
            osMap?.let { addGpxTrack(file, GPXParser(), it) }
        }
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

        fun prepareGpxTrack(path: Path?, entry: Summit?): GpsTrack? {
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

        fun copyGpxFileToCache(inputStream: InputStream, file: File) {
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
    }
}