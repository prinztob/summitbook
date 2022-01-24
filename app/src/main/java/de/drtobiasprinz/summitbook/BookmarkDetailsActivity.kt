package de.drtobiasprinz.summitbook

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.Bookmark
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowSrolling
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.showMapTypeSelectorDialog
import org.osmdroid.config.Configuration
import java.io.IOException
import java.util.*

class BookmarkDetailsActivity : AppCompatActivity() {
    private var bookmark: Bookmark? = null
    private lateinit var database: AppDatabase
    private lateinit var osMap: CustomMapViewToAllowSrolling
    private var metrics = DisplayMetrics()
    private var isMilageButtonShown: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_entry_details)
        osMap = findViewById(R.id.osmap)
        database = AppDatabase.getDatabase(applicationContext)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val supportActionBarLocal = supportActionBar
        if (supportActionBarLocal != null) {
            supportActionBarLocal.setDisplayHomeAsUpEnabled(true)
            supportActionBarLocal.setDisplayShowHomeEnabled(true)
        }
        val mainActivity = MainActivity.mainActivity
        mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        val bundle = intent.extras
        if (bundle != null) {
            val entryId = bundle.getLong(BOOKMARK_ID_EXTRA_IDENTIFIER)
            val bookmarkLocal = database.bookmarkDao()?.getBookmark(entryId)
            if (bookmarkLocal != null) {
                bookmark = bookmarkLocal
                if (bookmarkLocal.hasGpsTrack()) {
                    osMap.visibility = View.VISIBLE
                } else {
                    osMap.visibility = View.GONE
                }
            }

            setOpenStreetMap()

            val textViewName = findViewById<TextView>(R.id.bookmark_name)
            textViewName.text = bookmark?.name
            val imageViewSportType = findViewById<ImageView>(R.id.sport_type_image)
            bookmark?.sportType?.imageIdBlack?.let { imageViewSportType.setImageResource(it) }
            bookmark?.heightMeter?.let { setText(it, "hm", findViewById(R.id.height_meterText), findViewById(R.id.height_meter)) }
            bookmark?.kilometers?.let { setText(it, "km", findViewById(R.id.kilometersText), findViewById(R.id.kilometers)) }
            bookmark?.comments?.let { setText(it, findViewById(R.id.comments), findViewById(R.id.comments)) }
            val openWithButton = findViewById<ImageButton>(R.id.gps_open_with)
            openWithButton.setOnClickListener { _: View? ->
                if (bookmark?.hasGpsTrack() == true) {
                    try {
                        val uri = bookmark?.copyGpsTrackToTempFile(this.externalCacheDir)?.let { FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", it) }
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, "application/gpx")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        } else {
                            Toast.makeText(applicationContext,
                                    "GPX viewer not installed", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: IOException) {
                        Toast.makeText(applicationContext,
                                "GPX file could not be copied", Toast.LENGTH_LONG).show()
                    }
                }
            }
            val showMilageButton = findViewById<ImageButton>(R.id.osm_with_milage)
            showMilageButton.setOnClickListener { _: View? ->
                if (isMilageButtonShown) {
                    isMilageButtonShown = false
                    showMilageButton.setImageResource(R.drawable.moreinfo_arrow)
                } else {
                    isMilageButtonShown = true
                    showMilageButton.setImageResource(R.drawable.moreinfo_arrow_pressed)
                }
                osMap.overlays?.clear()
                bookmark?.let { OpenStreetMapUtils.addTrackAndMarker(it, osMap, true, if (isMilageButtonShown) 1 else 0) }
            }
            val shareButton = findViewById<ImageButton>(R.id.gps_share)
            shareButton.setOnClickListener { _: View? ->
                if (bookmark?.hasGpsTrack() == true) {
                    try {
                        val uri = bookmark?.copyGpsTrackToTempFile(this.externalCacheDir)?.let { FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", it) }
                        val intentShareFile = Intent(Intent.ACTION_SEND)
                        intentShareFile.type = "application/pdf"
                        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
                        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_gpx_subject))
                        intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_gpx_text, bookmark?.name, bookmark?.heightMeter.toString(), bookmark?.kilometers.toString()))
                        if (intentShareFile.resolveActivity(packageManager) != null) {
                            startActivity(intentShareFile)
                        } else {
                            Toast.makeText(applicationContext,
                                    "E-Mail program not installed", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: IOException) {
                        Toast.makeText(applicationContext,
                                "GPX file could not be shared", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setOpenStreetMap() {
        OpenStreetMapUtils.setTileSource(OpenStreetMapUtils.selectedItem, osMap)
        val changeMapTypeFab: ImageButton = findViewById(R.id.change_map_type)
        changeMapTypeFab.setImageResource(R.drawable.ic_more_vert_black_24dp)
        changeMapTypeFab.setOnClickListener { showMapTypeSelectorDialog(this, osMap) }
        addDefaultSettings(this, osMap, this)
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        val params = osMap.layoutParams
        params?.height = (metrics.heightPixels * 0.5).toInt()
        osMap.layoutParams = params
        bookmark?.let { OpenStreetMapUtils.addTrackAndMarker(it, osMap, false, if (isMilageButtonShown) 1 else 0) }
    }

    private fun setText(text: String, info: TextView, textView: TextView) {
        if (text == "") {
            info.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            info.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = text
        }
    }

    private fun setText(value: Double, unit: String, info: TextView, textView: TextView) {
        if (value == 0.0) {
            info.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            info.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = String.format(Locale.US, "%.1f %s", value, unit)
        }
    }

    private fun setText(value: Int, unit: String, info: TextView, textView: TextView) {
        if (value == 0) {
            info.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            info.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = String.format("%s %s", value, unit)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }

    companion object {
        var BOOKMARK_ID_EXTRA_IDENTIFIER = "BOOKMARK_ID"
    }
}