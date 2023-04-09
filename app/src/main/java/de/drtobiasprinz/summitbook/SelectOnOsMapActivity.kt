package de.drtobiasprinz.summitbook

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.Address
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.gpx.GPXParser
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.databinding.ActivitySelectOnOsmapBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.GpsTrack
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.TrackColor
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addTrackAndMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.drawBoundingBox
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
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
import javax.inject.Inject

@AndroidEntryPoint
class SelectOnOsMapActivity : FragmentActivity() {

    private lateinit var binding: ActivitySelectOnOsmapBinding

    @Inject
    lateinit var contactsAdapter: ContactsAdapter
    private val viewModel: DatabaseViewModel by viewModels()
    private var latLngSelectedPosition: TrackPoint? = null
    private var summitEntry: Summit? = null
    private var database: AppDatabase? = null
    private var selectedGpsPath: Path? = null
    private var summitEntryId = 0L
    private var wasBoundingBoxCalculated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectOnOsmapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = DatabaseModule.provideDatabase(applicationContext)
        binding.expander.setOnClickListener {
            if (binding.searchPanel.visibility == View.VISIBLE) {
                binding.searchPanel.visibility = View.GONE
            } else {
                binding.searchPanel.visibility = View.VISIBLE
            }
        }

        OpenStreetMapUtils.setOsmConfForTiles()
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        val bundle = intent.extras
        if (bundle != null) {
            summitEntryId = bundle.getLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER)
            summitEntry = database?.summitsDao()?.getSummit(summitEntryId)
            binding.editLocation.setText(summitEntry?.name)

        }
        binding.osmap.setTileSource(TileSourceFactory.MAPNIK)
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        val entry = summitEntry
        if (entry != null) {
            addTrackAndMarker(
                entry,
                binding.osmap,
                this,
                false,
                TrackColor.None,
                alwaysShowTrackOnMap = false
            )
            entry.trackBoundingBox?.let { drawBoundingBox(binding.osmap, it) }
        }
        addDefaultSettings(this, binding.osmap, this)
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (entry != null) {
                    updateSavePositionButton(true)
                    latLngSelectedPosition = TrackPoint(p.latitude, p.longitude)
                    addMarker(binding.osmap, applicationContext, p, entry)
                    prepareGpxTrack(
                        selectedGpsPath,
                        summitEntry
                    )?.let {
                        addSelectedPositionAndTrack(
                            TrackPoint(p.latitude, p.longitude),
                            it,
                            binding.osmap
                        )
                    }
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        binding.osmap.overlays.add(MapEventsOverlay(mReceive))

        binding.buttonSearchLocation.setOnClickListener {
            searchForAddress(binding.editLocation.text.toString())
        }

        updateSavePositionButton(false)
        binding.addPositionSave.setOnClickListener { v: View ->
            val summit = summitEntry
            val position = latLngSelectedPosition
            if (summit != null) {
                if (position != null) {
                    summit.lat = position.lat
                    summit.lng = position.lon
                    summit.latLng = latLngSelectedPosition
                    viewModel.saveContact(true, summit)
                    finish()
                    Toast.makeText(
                        v.context,
                        String.format(
                            getString(R.string.add_position_to_summit_successful),
                            summit.name
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val localSelectedPath = selectedGpsPath
                if (localSelectedPath != null) {
                    try {
                        Files.copy(
                            localSelectedPath,
                            summit.getGpsTrackPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                        summit.hasTrack = true
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                summit.setBoundingBoxFromTrack()
                viewModel.saveContact(true, summit)
            }
        }
        binding.addPositionCancel.setOnClickListener { v: View ->
            finish()
            Toast.makeText(
                v.context,
                getString(R.string.add_position_to_summit_cancel, summitEntry?.name),
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.addPositionDelete.setOnClickListener { v: View ->
            val summitEntryLocal = summitEntry
            if (summitEntryLocal != null) {
                AlertDialog.Builder(v.context)
                    .setTitle(getString(R.string.delete_coordinates))
                    .setMessage(getString(R.string.delete_coordinates_message))
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        selectedGpsPath = null
                        if (summitEntryLocal.hasGpsTrack()) {
                            val gpsTrackPath = summitEntryLocal.getGpsTrackPath()
                            gpsTrackPath.toFile()?.delete()
                            summitEntryLocal.hasTrack = false
                        }
                        summitEntryLocal.latLng = TrackPoint(0.0, 0.0)

                        viewModel.saveContact(true, summitEntryLocal)
                        finish()
                        Toast.makeText(
                            v.context,
                            v.context.getString(R.string.delete_gps, summitEntryLocal.name),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setNegativeButton(
                        android.R.string.cancel
                    ) { _: DialogInterface?, _: Int ->
                        Toast.makeText(
                            v.context,
                            getString(R.string.delete_cancel),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }
        }
        binding.addGpsTrack.setOnClickListener {
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

    private fun searchForAddress(locationAddress: String) {
        val geoCoder = GeocoderNominatim(BuildConfig.APPLICATION_ID)
        val viewBox: BoundingBox = binding.osmap.boundingBox
        val foundAddresses: List<Address> = geoCoder.getFromLocationName(
            locationAddress, 1,
            viewBox.latSouth, viewBox.lonEast, viewBox.latNorth, viewBox.lonWest, false
        )
        if (foundAddresses.isNotEmpty()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.found_address, locationAddress),
                Toast.LENGTH_SHORT
            ).show()
            val address = foundAddresses[0]
            val geoPoint = GeoPoint(address.latitude, address.longitude)
            val poiIcon: Drawable? =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_filled_location_black_48, null)
            binding.osmap.setExpectedCenter(geoPoint)
            val poiMarker = Marker(binding.osmap)
            poiMarker.title = address.getAddressLine(0)
            poiMarker.snippet = address.getAddressLine(1)
            poiMarker.position = geoPoint
            poiMarker.relatedObject = address
            poiMarker.icon = poiIcon
            poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            poiMarker.setInfoWindow(CustomInfoWindow(binding.osmap))
            binding.osmap.overlays.add(poiMarker)
            binding.osmap.setExpectedCenter(geoPoint)
        } else {
            Toast.makeText(
                applicationContext,
                getString(R.string.not_found, locationAddress),
                Toast.LENGTH_SHORT
            ).show()
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
        binding.addPositionSave.isEnabled = enable
        if (enable) {
            binding.addPositionSave.setImageResource(R.drawable.ic_save_black_24dp)
        } else {
            binding.addPositionSave.setImageResource(R.drawable.ic_save_grey_400_24dp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
    }

    private val resultLauncherForAddingGpxTrack =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val file = File(MainActivity.cache, "new_gpx_track.gpx")
                result?.data?.data?.also { uri ->
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        copyGpxFileToCache(inputStream, file)
                    }
                }
                addGpxTrack(file, GPXParser(), binding.osmap)
            }
        }

    internal class CustomInfoWindow(mapView: MapView?) :
        MarkerInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView) {
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