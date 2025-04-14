package de.drtobiasprinz.summitbook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity.Companion.copyGpxFileToCache
import de.drtobiasprinz.summitbook.databinding.ActivityReceiverBinding
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.utils.Utils
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File

@AndroidEntryPoint
class ReceiverActivity : AppCompatActivity() {
    private var gpxTrackUri: Uri? = null
    lateinit var binding: ActivityReceiverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Utils.fixEdgeToEdge(binding.root)

        Log.i("ReceiverActivity", "onCreate")
        MainActivity.cache = applicationContext.cacheDir
        MainActivity.storage = applicationContext.filesDir
        MainActivity.activitiesDir = File(MainActivity.storage, "activities")
        binding.osmap.setTileSource(TileSourceFactory.OpenTopo)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        OpenStreetMapUtils.addDefaultSettings(binding.osmap, this)

        binding.addToSummits.setOnClickListener {
            if (gpxTrackUri != null) {
                val addSummit = AddSummitDialog()
                addSummit.gpxTrackUri = gpxTrackUri
                addSummit.fromReceiverActivity = true
                addSummit.show(
                    supportFragmentManager,
                    getString(R.string.add_new_summit)
                )
            }
        }
        binding.addToBookmarks.setOnClickListener {
            if (gpxTrackUri != null) {
                val addSummit = AddSummitDialog()
                addSummit.gpxTrackUri = gpxTrackUri
                addSummit.isBookmark = true
                addSummit.fromReceiverActivity = true
                addSummit.show(
                    supportFragmentManager,
                    getString(R.string.add_new_bookmark)
                )
            }
        }

        binding.back.setOnClickListener {
            finish()
        }
    }

    private fun drawGpxFile(intent: Intent) {
        val uri = intent.data
        gpxTrackUri = uri
        Log.i(
            "ReceiverActivity",
            "intent was: ${intent.action} , received url ${gpxTrackUri.toString()}"
        )
        if (uri != null) {
            val file = File(MainActivity.cache, "input_filter_file.gpx")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                copyGpxFileToCache(inputStream, file)
            }
            if (file.exists()) {
                val gpsTrack = SelectOnOsMapActivity.prepareGpxTrack(file.toPath(), null)
                gpsTrack?.addGpsTrack(binding.osmap, TrackColor.None)
                val highestTrackPoint = gpsTrack?.getHighestElevation()
                if (highestTrackPoint != null) {
                    val highestGeoPoint = GeoPoint(highestTrackPoint.latitude, highestTrackPoint.longitude)
                    val marker = Marker(binding.osmap)
                    marker.position = highestGeoPoint
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.icon = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_outline_location_green_48,
                        null
                    )
                    marker.title = "New summit"
                }
                if (gpsTrack != null) {
                    binding.osmap.post {
                        OpenStreetMapUtils.calculateBoundingBox(
                            binding.osmap,
                            gpsTrack.trackGeoPoints
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("ReceiverActivity", "onResume")

        if (Intent.ACTION_VIEW == intent.action) {
            drawGpxFile(intent)
        } else {
            Log.i("ReceiverActivity", "intent was something else: ${intent.action}")
        }

    }


}
