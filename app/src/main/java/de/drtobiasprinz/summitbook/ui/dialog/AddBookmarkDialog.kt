package de.drtobiasprinz.summitbook.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.chaquo.python.Python
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.fragments.BookmarkViewFragment
import de.drtobiasprinz.summitbook.models.ElevationData
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.VelocityData
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader.Companion.getTempGpsFilePath
import de.drtobiasprinz.summitbook.ui.utils.InputFilterMinMax
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import de.drtobiasprinz.summitbook.ui.utils.SummitSlope
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
class AddBookmarkDialog(val gpxTrackUrl: Uri? = null) : DialogFragment() {
    var isUpdate = false
    private var temporaryGpxFile: File? = null
    private var database: AppDatabase? = null
    private var sportTypeAdapter: ArrayAdapter<SportType>? = null
    private var currentBookmark: Summit? = null
    private lateinit var usedView: View
    private lateinit var bookmarkName: EditText
    private lateinit var commentText: EditText
    private lateinit var heightMeterText: EditText
    private lateinit var kmText: EditText
    private lateinit var saveEntryButton: Button
    private lateinit var addTrack: ImageButton

    private var sportTypeSpinner: Spinner? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_add_bookmark, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = context?.let { AppDatabase.getDatabase(it) }
        usedView = view
        saveEntryButton = view.findViewById(R.id.add_bookmark_save)
        saveEntryButton.isEnabled = false
        val closeDialogButton = view.findViewById<Button>(R.id.add_bookmark_cancel)
        bookmarkName = view.findViewById(R.id.bookmark_name)
        bookmarkName.addTextChangedListener(watcher)
        sportTypeSpinner = view.findViewById(R.id.activities)
        sportTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
                SportType.values())
        sportTypeSpinner?.adapter = sportTypeAdapter
        commentText = view.findViewById(R.id.comments)
        heightMeterText = view.findViewById(R.id.height_meter)
        heightMeterText.addTextChangedListener(watcher)
        heightMeterText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 9999))
        kmText = view.findViewById(R.id.kilometers)
        kmText.addTextChangedListener(watcher)
        kmText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 999))
        if (currentBookmark == null) {
            createNewBookmark()
        }
        if (isUpdate) {
            saveEntryButton.setText(R.string.updateButtonText)
            updateDialogFields(currentBookmark, true)
        }
        if (gpxTrackUrl != null) {
            addAndAnalyzeTrack(gpxTrackUrl)
        }

        saveEntryButton.setOnClickListener {
            val sportType = SportType.valueOf(sportTypeSpinner?.selectedItem.toString())
            parseSummitEntry(sportType)
            val adapter = BookmarkViewFragment.adapter
            val bookmark = currentBookmark
            if (bookmark != null) {
                if (isUpdate) {
                    database?.summitDao()?.updateSummit(bookmark)
                } else {
                    bookmark.id = database?.summitDao()?.addSummit(bookmark) ?: 0L
                    adapter?.bookmarks?.add(bookmark)
                }
                if (temporaryGpxFile?.exists() == true) {
                    temporaryGpxFile?.copyTo(bookmark.getGpsTrackPath().toFile())
                }
                adapter?.notifyDataSetChanged()
                dialog?.cancel()
            }
        }

        addTrack = view.findViewById(R.id.add_gps_track)
        addTrack.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            resultLauncher.launch(intent)
        }
        closeDialogButton.setOnClickListener { v: View ->
            if (!isUpdate && currentBookmark?.getGpsTrackPath()?.toFile()?.exists() == true) {
                currentBookmark?.getGpsTrackPath()?.toFile()?.delete()
            }
            if (!isUpdate && currentBookmark?.getGpsTrackPath(simplified = true)?.toFile()?.exists() == true) {
                currentBookmark?.getGpsTrackPath(simplified = true)?.toFile()?.delete()
            }
            if (!isUpdate && currentBookmark?.getGpxPyPath()?.toFile()?.exists() == true) {
                currentBookmark?.getGpxPyPath()?.toFile()?.delete()
            }
            dialog?.cancel()
            val text = if (currentBookmark != null) getString(R.string.update_summit_cancel) else getString(R.string.add_new_summit_cancel)
            Toast.makeText(v.context, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadGpxFile(inputStream: InputStream, entry: Summit, v: View?) {
        try {
            Files.copy(inputStream, entry.getGpsTrackPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(v?.context, v?.context?.getString(R.string.add_gpx_failed, entry.name), Toast.LENGTH_SHORT).show()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            Toast.makeText(v?.context, v?.context?.getString(R.string.add_gpx_failed, entry.name), Toast.LENGTH_SHORT).show()
        }
    }


    private fun parseSummitEntry(sportType: SportType) {
        try {
            val bookmark = currentBookmark
            if (bookmark != null) {
                bookmark.name = bookmarkName.text.toString()
                bookmark.sportType = sportType
                bookmark.comments = commentText.text.toString()
                bookmark.elevationData.elevationGain = heightMeterText.text.toString().toInt()
                bookmark.kilometers = getTextWithDefault(kmText, 0.0)
                bookmark.isBookmark = true
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun createNewBookmark() {
        currentBookmark = Summit(Date(),
                if (bookmarkName.text.toString() != "") bookmarkName.text.toString() else "New Bookmark",
                SportType.Bicycle,
                emptyList(),
                emptyList(),
                commentText.text.toString(),
                ElevationData.parse(0, getTextWithDefault(heightMeterText, 0)),
                getTextWithDefault(kmText, 0.0),
                VelocityData.parse(0.0, 0.0),
                0.0, 0.0, emptyList(), emptyList(), isFavorite = false, isPeak = false, imageIds = mutableListOf(), garminData = null, trackBoundingBox = null,
                isBookmark = true
        )
    }

    private fun updateDialogFields(entry: Summit?, updateSpinner: Boolean) {
        if (updateSpinner) {
            val sportTypeString = SportType.valueOf(entry?.sportType.toString())
            val spinnerPosition = sportTypeAdapter?.getPosition(sportTypeString)
            spinnerPosition?.let { sportTypeSpinner?.setSelection(it) }
        }
        if (entry != null) {
            setTextIfNotAlreadySet(bookmarkName, entry.name)
            setTextIfNotAlreadySet(commentText, entry.comments)
            setTextIfNotAlreadySet(heightMeterText, entry.elevationData.elevationGain.toString())
            setTextIfNotAlreadySet(kmText, entry.kilometers.toString())
        }
    }

    private fun getTextWithDefault(editText: EditText, defaultValue: Double): Double {
        return try {
            editText.text.toString().toDouble()
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun getTextWithDefault(editText: EditText, defaultValue: Int): Int {
        return try {
            editText.text.toString().toInt()
        } catch (e: Exception) {
            defaultValue
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database?.close()
    }


    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultData ->
        if (resultData.resultCode == Activity.RESULT_OK) {
            resultData?.data?.data.also { uri ->
                addAndAnalyzeTrack(uri)
            }
        }
    }

    private fun addAndAnalyzeTrack(uri: Uri?) {
        val bookmark = currentBookmark
        if (temporaryGpxFile == null) {
            temporaryGpxFile = getTempGpsFilePath(SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.US).format(Date())).toFile()
        }
        if (uri != null && bookmark != null) {
            context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                uploadGpxFile(inputStream, bookmark, view)
                usedView.findViewById<RelativeLayout>(R.id.loadingPanel).visibility = View.VISIBLE
                database?.let { AsyncAnalyzeGpsTracks(currentBookmark, MainActivity.pythonInstance, it, usedView).execute() }
            }
        }
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            saveEntryButton.isEnabled = !(isEmpty(bookmarkName) || isEmpty(heightMeterText) || isEmpty(kmText))
        }

        private fun isEmpty(editText: EditText): Boolean {
            return TextUtils.isEmpty(editText.text.toString().trim { it <= ' ' })
        }
    }


    companion object {
        @JvmStatic
        fun updateInstance(entry: Summit?): AddBookmarkDialog {
            val add = AddBookmarkDialog()
            add.isUpdate = true
            add.currentBookmark = entry
            return add
        }

        @SuppressLint("StaticFieldLeak")
        class AsyncAnalyzeGpsTracks(private val entry: Summit?, private val pythonInstance: Python,
                                    private val database: AppDatabase, private val view: View) : AsyncTask<Uri, Int?, Void?>() {
            private var highestElevation: TrackPoint? = null
            override fun doInBackground(vararg uri: Uri): Void? {
                try {
                    if (entry?.hasGpsTrack() == true) {
                        GpxPyExecutor(pythonInstance).createSimplifiedGpxTrack(entry.getGpsTrackPath())
                        entry.setGpsTrack()
                        highestElevation = entry.gpsTrack?.getHighestElevation()
                        entry.gpsTrack?.setDistance()
                    }
                } catch (ex: RuntimeException) {
                    Log.e("AsyncAnalyzeGpaTracks", "Error in simplify track: ${ex.message}")
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                Log.i("AsyncAnalyzeGpaTracks", "Gpx tracks simplified.")
                view.findViewById<RelativeLayout>(R.id.loadingPanel).visibility = View.GONE
                if (entry != null) {
                    entry.lat = highestElevation?.lat
                    entry.lng = highestElevation?.lon
                    entry.latLng = highestElevation
                    highestElevation?.lat?.let { database.summitDao()?.updateLat(entry.id, it) }
                    highestElevation?.lon?.let { database.summitDao()?.updateLng(entry.id, it) }
                    val gpsTrack = entry.gpsTrack
                    if (gpsTrack != null) {
                        val nameFromTrack = gpsTrack.gpxTrack?.metadata?.name
                                ?: gpsTrack.gpxTrack?.tracks?.toList()?.blockingGet()?.first()?.name
                        view.findViewById<EditText>(R.id.bookmark_name).setText(nameFromTrack)
                        if (gpsTrack.hasNoTrackPoints()) {
                            gpsTrack.parseTrack(useSimplifiedIfExists = false)
                        }
                        val slopeCalculator = SummitSlope(gpsTrack.trackPoints)
                        entry.elevationData.maxSlope = slopeCalculator.calculateMaxSlope()
                    }
                    val gpxPyJsonFile = entry.getGpxPyPath().toFile()
                    if (gpxPyJsonFile.exists()) {
                        val gpxPyJson = JsonParser.parseString(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

                        val elevationGain = try {
                            gpxPyJson.getAsJsonPrimitive("elevation_gain").asDouble.roundToInt()
                        } catch (_: ClassCastException) {
                            0
                        }
                        entry.elevationData.elevationGain = elevationGain
                        val hmText = view.findViewById<EditText>(R.id.height_meter)
                        hmText.filters = arrayOf()
                        hmText.setText(elevationGain.toString())
                        hmText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 9999))

                        val maxElevation = try {
                            gpxPyJson.getAsJsonPrimitive("max_elevation").asDouble.roundToInt()
                        } catch (_: ClassCastException) {
                            0
                        }
                        if (maxElevation > 0) {
                            entry.elevationData.maxElevation = maxElevation
                        }
                        var distance = try {
                            gpxPyJson.getAsJsonPrimitive("moving_distance").asDouble / 1000
                        } catch (_: ClassCastException) {
                            0.0
                        }
                        if (distance == 0.0 && gpsTrack != null) {
                            distance = (gpsTrack.trackPoints.last().extension?.distance
                                    ?: 0.0) / 1000
                        }
                        entry.kilometers = distance
                        val kmText = view.findViewById<EditText>(R.id.kilometers)
                        kmText.filters = arrayOf()
                        kmText.setText(String.format(view.context.resources.configuration.locales[0], "%.1f", distance))
                        kmText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 999))
                        val movingDuration = try {
                            gpxPyJson.getAsJsonPrimitive("moving_time").asDouble
                        }  catch (_: ClassCastException) {
                            0.0
                        }
                        if (movingDuration > 0) {
                            val pace = distance / movingDuration * 3600
                            entry.velocityData.avgVelocity = pace
                        }
                    }
                }
            }
        }

        private fun setTextIfNotAlreadySet(editText: EditText, setValue: String) {
            if (editText.text.toString() == "") {
                editText.setText(setValue)
            }
        }
    }

}
