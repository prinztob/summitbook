package de.drtobiasprinz.summitbook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity.Companion.copyGpxFileToCache
import de.drtobiasprinz.summitbook.db.entities.TrackColor
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

class ReceiverActivity : AppCompatActivity() {
    private lateinit var osMap: MapView
    private var gpxTrackUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receiver)
        Log.i("ReceiverActivity", "onCreate")
        MainActivity.cache = applicationContext.cacheDir
        MainActivity.storage = applicationContext.filesDir
        MainActivity.activitiesDir = File(MainActivity.storage, "activities")
        osMap = findViewById(R.id.osmap)
        osMap.setTileSource(TileSourceFactory.OpenTopo)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        OpenStreetMapUtils.addDefaultSettings(this, osMap, this)

        val addSummitButton = findViewById<Button>(R.id.add_to_summits)
        addSummitButton.setOnClickListener {
            if (gpxTrackUri != null) {
                val addSummit = AddSummitDialog()
                supportFragmentManager.let {
                    addSummit.show(
                        it,
                        getString(R.string.add_new_bookmark)
                    )
                }
            }
        }
        val addBookmarkButton = findViewById<Button>(R.id.add_to_bookmarks)
        addBookmarkButton.setOnClickListener {
            if (gpxTrackUri != null) {
                val addSummit = AddSummitDialog()
                addSummit.gpxTrackUri = gpxTrackUri
                addSummit.isBookmark = true
                supportFragmentManager.let {
                    addSummit.show(
                        it,
                        getString(R.string.add_new_bookmark)
                    )
                }
            }
        }

        findViewById<ImageButton>(R.id.back).setOnClickListener {
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
                gpsTrack?.addGpsTrack(osMap, TrackColor.None)
                val highestTrackPoint = gpsTrack?.getHighestElevation()
                if (highestTrackPoint != null) {
                    val highestGeoPoint = GeoPoint(highestTrackPoint.lat, highestTrackPoint.lon)
                    val marker = Marker(osMap)
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
                    osMap.post {
                        OpenStreetMapUtils.calculateBoundingBox(
                            osMap,
                            OpenStreetMapUtils.getTrackPointsFrom(gpsTrack)
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
