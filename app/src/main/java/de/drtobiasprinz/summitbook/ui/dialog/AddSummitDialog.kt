package de.drtobiasprinz.summitbook.ui.dialog

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Bundle
import android.text.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.terminator.ChipTerminatorHandler
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.GarminActivityData
import de.drtobiasprinz.summitbook.models.PowerData
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.getAllDownloadedSummitsFromGarmin
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.utils.InputFilterMinMax
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class AddSummitDialog(private val sortFilterHelper: SortFilterHelper, private val pythonExecutor: GarminPythonExecutor?) : DialogFragment(), BaseDialog {
    var isUpdate = false
    var temporaryGpxFile: File? = null
    var latlngHightestPoint: LatLng? = null
    private lateinit var currentContext: Context
    private lateinit var helper: SummitBookDatabaseHelper
    private lateinit var database: SQLiteDatabase
    private lateinit var sportTypeAdapter: ArrayAdapter<SportType>
    private var currentSummitEntry: SummitEntry? = null
    private var connectedSummits: MutableList<SummitEntry> = mutableListOf()
    private var mDialog: AlertDialog? = null
    private var listItemsGpsDownloadSuccessful: BooleanArray? = null
    private lateinit var date: EditText
    private lateinit var summitName: AutoCompleteTextView
    private lateinit var placesView: NachoTextView
    private lateinit var countriesView: NachoTextView
    private lateinit var commentText: EditText
    private lateinit var heightMeterText: EditText
    private lateinit var kmText: EditText
    private lateinit var paceText: EditText
    private lateinit var topSpeedText: EditText
    private lateinit var topElevationText: EditText
    private lateinit var caloriesText: EditText
    private lateinit var averageHRText: EditText
    private lateinit var maxHRText: EditText
    private lateinit var ftpText: EditText
    private lateinit var vo2maxText: EditText
    private lateinit var powerNormText: EditText
    private lateinit var powerAvgText: EditText
    private lateinit var power1sText: EditText
    private lateinit var power2sText: EditText
    private lateinit var power5sText: EditText
    private lateinit var power10sText: EditText
    private lateinit var power20sText: EditText
    private lateinit var power30sText: EditText
    private lateinit var power1minText: EditText
    private lateinit var power2minText: EditText
    private lateinit var power5minText: EditText
    private lateinit var power10minText: EditText
    private lateinit var power20minText: EditText
    private lateinit var power30minText: EditText
    private lateinit var power1hText: EditText
    private lateinit var power2hText: EditText
    private lateinit var power5hText: EditText
    private lateinit var participantsView: NachoTextView
    private lateinit var saveEntryButton: Button
    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            saveEntryButton.isEnabled = !(isEmpty(summitName) || isEmpty(heightMeterText) || isEmpty(kmText) || isEmpty(date))
        }

        private fun isEmpty(editText: EditText): Boolean {
            return TextUtils.isEmpty(editText.text.toString().trim { it <= ' ' })
        }
    }
    private lateinit var sportTypeSpinner: Spinner
    private var activityDataFromGarminConnect: GarminActivityData? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_add_summit, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        helper = SummitBookDatabaseHelper(view.context)
        database = helper.writableDatabase
        saveEntryButton = view.findViewById(R.id.add_summit_save)
        saveEntryButton.isEnabled = false
        val closeDialogButton = view.findViewById<Button>(R.id.add_summit_cancel)
        date = view.findViewById(R.id.tour_date)
        date.addTextChangedListener(watcher)
        date.inputType = InputType.TYPE_NULL
        date.onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {
                showDatePicker(date, view.context)
            }
        }
        summitName = view.findViewById(R.id.summit_name)
        summitName.addTextChangedListener(watcher)
        summitName.setAdapter(getPlacesSuggestions(false))
        sportTypeSpinner = view.findViewById(R.id.activities)
        sportTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
                SportType.values())
        sportTypeSpinner.adapter = sportTypeAdapter
        addPlaces(view)
        addCountries(view)
        commentText = view.findViewById(R.id.comments)
        heightMeterText = view.findViewById(R.id.height_meter)
        heightMeterText.addTextChangedListener(watcher)
        heightMeterText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 9999))
        kmText = view.findViewById(R.id.kilometers)
        kmText.addTextChangedListener(watcher)
        kmText.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 999))
        paceText = view.findViewById(R.id.pace)
        topSpeedText = view.findViewById(R.id.top_speed)
        topElevationText = view.findViewById(R.id.top_elevation)
        averageHRText = view.findViewById(R.id.averageHr)
        maxHRText = view.findViewById(R.id.maxHr)
        ftpText = view.findViewById(R.id.ftp)
        vo2maxText = view.findViewById(R.id.vo2Max)
        caloriesText = view.findViewById(R.id.calories)
        powerNormText = view.findViewById(R.id.normPower)
        powerAvgText = view.findViewById(R.id.avgPower)
        power1sText = view.findViewById(R.id.power1s)
        power2sText = view.findViewById(R.id.power2s)
        power5sText = view.findViewById(R.id.power5s)
        power10sText = view.findViewById(R.id.power10s)
        power20sText = view.findViewById(R.id.power20s)
        power30sText = view.findViewById(R.id.power30s)
        power1minText = view.findViewById(R.id.power1min)
        power2minText = view.findViewById(R.id.power2min)
        power5minText = view.findViewById(R.id.power5min)
        power10minText = view.findViewById(R.id.power10min)
        power20minText = view.findViewById(R.id.power20min)
        power30minText = view.findViewById(R.id.power30min)
        power1hText = view.findViewById(R.id.power1h)
        power2hText = view.findViewById(R.id.power2h)
        power5hText = view.findViewById(R.id.power5h)
        addParticipants(view)
        if (isUpdate) {
            saveEntryButton.setText(R.string.updateButtonText)
            updateDialogFields(currentSummitEntry, true)
        }

        val expandMore: TextView = view.findViewById(R.id.expand_more)
        expandMore.setOnClickListener {
            if (expandMore.text == getString(R.string.more)) {
                expandMore.text = getString(R.string.less)
                expandMore.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                view.findViewById<LinearLayout>(R.id.additional_data_fields_view).visibility = View.VISIBLE
            } else {
                expandMore.text = getString(R.string.more)
                expandMore.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                view.findViewById<LinearLayout>(R.id.additional_data_fields_view).visibility = View.GONE
            }
        }

        val expandMorePerformance: TextView = view.findViewById(R.id.expand_more_performance)
        expandMorePerformance.setOnClickListener {
            if (expandMorePerformance.text == getString(R.string.more_performance)) {
                expandMorePerformance.text = getString(R.string.less_performance)
                expandMorePerformance.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                view.findViewById<LinearLayout>(R.id.performance_data_fields_view).visibility = View.VISIBLE
            } else {
                expandMorePerformance.text = getString(R.string.more_performance)
                expandMorePerformance.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                view.findViewById<LinearLayout>(R.id.performance_data_fields_view).visibility = View.GONE
            }
        }

        //save the summit
        saveEntryButton.setOnClickListener {
            val sportType = SportType.valueOf(sportTypeSpinner.selectedItem.toString())
            parseSummitEntry(sportType)
            val entry = currentSummitEntry
            if (entry != null) {
                val activityDataLocal = entry.activityData
                val gpsTrackPath = entry.getGpsTrackPath()?.toFile()
                val temporaryGpxFileLocal = temporaryGpxFile
                if (activityDataLocal != null && temporaryGpxFileLocal != null && temporaryGpxFileLocal.exists() && gpsTrackPath != null) {
                    temporaryGpxFileLocal.copyTo(gpsTrackPath, overwrite = true)
                }
                val latlngHighestPointLocal = latlngHightestPoint
                if (entry.latLng == null && latlngHighestPointLocal != null) {
                    entry.latLng = latlngHighestPointLocal
                }
                entry.setBoundingBoxFromTrack()
                val adapter = SummitViewFragment.adapter
                if (isUpdate) {
                    helper.updateSummit(database, entry)
                } else {
                    entry._id = helper.insertSummit(database, entry).toInt()
                    adapter.summitEntries.add(entry)
                    sortFilterHelper.entries.add(entry)
                    sortFilterHelper.update(sortFilterHelper.entries)
                }
                adapter.notifyDataSetChanged()
                dialog?.cancel()
            }
        }
        closeDialogButton.setOnClickListener { v: View ->
            dialog?.cancel()
            val text = if (currentSummitEntry != null) getString(R.string.update_summit_cancel) else getString(R.string.add_new_summit_cancel)
            Toast.makeText(v.context, text, Toast.LENGTH_SHORT).show()
        }

        val addGarminActivityButton = view.findViewById<ImageButton>(R.id.add_garmin_activity)
        addGarminActivityButton.setOnClickListener {
            val dateAsString = date.text.toString()
            val contextLocal = context
            if (dateAsString != "" && contextLocal != null) {
                if (pythonExecutor != null) {
                    view.findViewById<RelativeLayout>(R.id.loadingPanel).visibility = View.VISIBLE
                    AsyncDownloadJsonViaPython(pythonExecutor, dateAsString, this).execute()
                } else {
                    Toast.makeText(context,
                            "Please set Username and Password for Garmin connect in settings", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context,
                        "Please set a date to be able to retrieve data from Garmin connect", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun showSummitsDialog(pythonExecutor: GarminPythonExecutor, entries: List<SummitEntry>, progressBar: RelativeLayout, powerData: JsonObject? = null) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val useTcx = sharedPreferences.getBoolean("download_tcx", false)

        val mBuilder = AlertDialog.Builder(requireContext())
        mBuilder.setTitle(requireContext().getString(R.string.choose_item))
        val listItems = arrayOfNulls<String>(entries.size)
        val listItemsChecked = BooleanArray(entries.size)
        listItemsGpsDownloadSuccessful = BooleanArray(entries.size)
        for (i in entries.indices) {
            listItems[i] = entries[i].toReadableString(requireContext())
            listItemsChecked[i] = false
            listItemsGpsDownloadSuccessful?.set(i, true)
        }
        mBuilder
                .setMultiChoiceItems(listItems, listItemsChecked) { _: DialogInterface, which: Int, isChecked: Boolean ->
                    val entry = entries[which]
                    listItemsGpsDownloadSuccessful?.set(which, false)
                    listItemsChecked[which] = isChecked
                    mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = listItemsGpsDownloadSuccessful?.contains(false) == false
                    if (isChecked) {
                        downloadGpxForSummitEntry(pythonExecutor, entry, which, useTcx, powerData)
                    }
                }
                .setPositiveButton(R.string.saveButtonText) { _: DialogInterface?, _: Int ->
                    val selectedEntries: MutableList<SummitEntry> = mutableListOf()
                    for (i in entries.indices) {
                        if (listItemsChecked[i]) {
                            selectedEntries.add(entries[i])
                        }
                    }
                    val downloader = GarminTrackAndDataDownloader(selectedEntries, pythonExecutor)
                    downloader.downloadTracks(true)
                    downloader.extractFinalSummitEntry()
                    val entry = downloader.finalEntry
                    if (entry != null) {
                        temporaryGpxFile = getTempGpsFilePath(entry.date).toFile()
                        downloader.composeFinalTrack(temporaryGpxFile)
                        latlngHightestPoint = entry.latLng
                        updateDialogFields(entry, !isUpdate)
                        Toast.makeText(context, context?.getString(R.string.garmin_add_successful, entry.name), Toast.LENGTH_LONG).show()
                    }
                    progressBar.visibility = View.GONE
                }
                .setNegativeButton(R.string.cancelButtonText) { _: DialogInterface?, _: Int ->
                    Toast.makeText(context, context?.getString(R.string.garmin_add_cancel), Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }

        mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        mDialog = mBuilder.create()
        mDialog?.show()
    }

    private fun addParticipants(view: View) {
        val suggestions: MutableList<String> = mutableListOf()
        sortFilterHelper.entries.forEach {
            suggestions.addAll(it.participants)
        }
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions.distinct())
        participantsView = view.findViewById(R.id.participants)
        addChipWithSuggestions(participantsView, adapter)
    }

    private fun updatePower(powerData: JsonObject, power: PowerData) {
        powerData["entries"].asJsonArray.forEach {
            val element = it.asJsonObject
            when (element["duration"].asString) {
                "1" -> power.oneSec = getJsonObjectEntryNotNone(element, "power").toInt()
                "2" -> power.twoSec = getJsonObjectEntryNotNone(element, "power").toInt()
                "5" -> power.fiveSec = getJsonObjectEntryNotNone(element, "power").toInt()
                "10" -> power.tenSec = getJsonObjectEntryNotNone(element, "power").toInt()
                "20" -> power.twentySec = getJsonObjectEntryNotNone(element, "power").toInt()
                "30" -> power.thirtySec = getJsonObjectEntryNotNone(element, "power").toInt()
                "60" -> power.oneMin = getJsonObjectEntryNotNone(element, "power").toInt()
                "120" -> power.twoMin = getJsonObjectEntryNotNone(element, "power").toInt()
                "300" -> power.fiveMin = getJsonObjectEntryNotNone(element, "power").toInt()
                "600" -> power.tenMin = getJsonObjectEntryNotNone(element, "power").toInt()
                "1200" -> power.twentyMin = getJsonObjectEntryNotNone(element, "power").toInt()
                "1800" -> power.thirtyMin = getJsonObjectEntryNotNone(element, "power").toInt()
                "3600" -> power.oneHour = getJsonObjectEntryNotNone(element, "power").toInt()
                "7200" -> power.twoHours = getJsonObjectEntryNotNone(element, "power").toInt()
                "18000" -> power.fiveHours = getJsonObjectEntryNotNone(element, "power").toInt()
            }
        }
    }

    private fun addCountries(view: View) {
        val suggestions: MutableList<String> = mutableListOf()
        sortFilterHelper.entries.forEach {
            suggestions.addAll(it.countries)
        }
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions.distinct())
        countriesView = view.findViewById(R.id.countries)
        addChipWithSuggestions(countriesView, adapter)
    }

    private fun addPlaces(view: View) {
        placesView = view.findViewById(R.id.places)
        addChipWithSuggestions(placesView, getPlacesSuggestions())
    }

    private fun getPlacesSuggestions(addConnectedEntryString: Boolean = true): ArrayAdapter<String> {
        val suggestions: MutableList<String> = mutableListOf()
        sortFilterHelper.entries.forEach {
            suggestions.addAll(it.places)
            suggestions.add(it.name)
        }
        val localSummitEntry = currentSummitEntry
        if (addConnectedEntryString && localSummitEntry != null) {
            for (entry in sortFilterHelper.entries) {
                val diffInMilliSec: Long = localSummitEntry.date.time - entry.date.time
                val diffInDays: Long = TimeUnit.MILLISECONDS.toDays(diffInMilliSec)
                if (diffInDays <= 1 && diffInDays > 0) {
                    connectedSummits.add(entry)
                    suggestions.add(entry.getConnectedEntryString(requireContext()))
                }
            }
        }
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions.distinct())
        return adapter
    }

    private fun addChipWithSuggestions(view: NachoTextView, adapter: ArrayAdapter<String>) {
        view.setAdapter(adapter)
        view.addChipTerminator(',', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR)
        view.enableEditChipOnTouch(false, true)
    }

    private fun parseSummitEntry(sportType: SportType) {
        val places = updatePlacesChipValuesWithId()
        try {
            val entry = currentSummitEntry
            if (entry != null) {
                entry.date = SummitEntry.parseDate(date.text.toString())
                entry.name = summitName.text.toString()
                entry.sportType = sportType
                entry.places = places
                entry.countries = countriesView.chipValues
                entry.comments = commentText.text.toString()
                entry.heightMeter = heightMeterText.text.toString().toInt()
                entry.kilometers = getTextWithDefaultDouble(kmText)
                entry.pace = getTextWithDefaultDouble(paceText)
                entry.topSpeed = getTextWithDefaultDouble(topSpeedText)
                entry.topElevation = getTextWithDefaultInt(topElevationText)
                entry.participants = participantsView.chipValues
            } else {
                currentSummitEntry = SummitEntry(
                        SummitEntry.parseDate(date.text.toString()),
                        summitName.text.toString(),
                        sportType,
                        places,
                        countriesView.chipValues,
                        commentText.text.toString(), heightMeterText.text.toString().toInt(),
                        getTextWithDefaultDouble(kmText),
                        getTextWithDefaultDouble(paceText),
                        getTextWithDefaultDouble(topSpeedText),
                        getTextWithDefaultInt(topElevationText),
                        participantsView.chipValues,
                        mutableListOf()
                )
            }
            activityDataFromGarminConnect?.calories = getTextWithDefaultDouble(caloriesText).toFloat()
            activityDataFromGarminConnect?.averageHR = getTextWithDefaultDouble(averageHRText).toFloat()
            activityDataFromGarminConnect?.maxHR = getTextWithDefaultDouble(maxHRText).toFloat()
            activityDataFromGarminConnect?.ftp = getTextWithDefaultInt(ftpText)
            activityDataFromGarminConnect?.vo2max = getTextWithDefaultInt(vo2maxText)
            activityDataFromGarminConnect?.power?.avgPower = getTextWithDefaultDouble(powerAvgText).toFloat()
            activityDataFromGarminConnect?.power?.normPower = getTextWithDefaultDouble(powerNormText).toFloat()
            activityDataFromGarminConnect?.power?.oneSec = getTextWithDefaultInt(power1sText)
            activityDataFromGarminConnect?.power?.twoSec = getTextWithDefaultInt(power2sText)
            activityDataFromGarminConnect?.power?.fiveSec = getTextWithDefaultInt(power5sText)
            activityDataFromGarminConnect?.power?.tenSec = getTextWithDefaultInt(power10sText)
            activityDataFromGarminConnect?.power?.twentySec = getTextWithDefaultInt(power20sText)
            activityDataFromGarminConnect?.power?.thirtySec = getTextWithDefaultInt(power30sText)
            activityDataFromGarminConnect?.power?.oneMin = getTextWithDefaultInt(power1minText)
            activityDataFromGarminConnect?.power?.twoMin = getTextWithDefaultInt(power2minText)
            activityDataFromGarminConnect?.power?.fiveMin = getTextWithDefaultInt(power5minText)
            activityDataFromGarminConnect?.power?.tenMin = getTextWithDefaultInt(power10minText)
            activityDataFromGarminConnect?.power?.twentyMin = getTextWithDefaultInt(power20minText)
            activityDataFromGarminConnect?.power?.thirtyMin = getTextWithDefaultInt(power30minText)
            activityDataFromGarminConnect?.power?.oneHour = getTextWithDefaultInt(power1hText)
            activityDataFromGarminConnect?.power?.twoHours = getTextWithDefaultInt(power2hText)
            activityDataFromGarminConnect?.power?.fiveHours = getTextWithDefaultInt(power5hText)
            currentSummitEntry?.activityData = activityDataFromGarminConnect
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun updatePlacesChipValuesWithId(): MutableList<String> {
        val places = placesView.chipValues
        for ((i, place) in places.withIndex()) {
            for (connectedSummit in connectedSummits)
                if (place == connectedSummit.getConnectedEntryString(requireContext())) {
                    places[i] = "${SummitEntry.CONNECTED_ACTIVITY_PREFIX}${connectedSummit.activityId}"
                }
        }
        return places
    }

    fun updateDialogFields(entry: SummitEntry?, updateSpinner: Boolean) {
        if (updateSpinner) {
            val sportTypeString = SportType.valueOf(entry?.sportType.toString())
            val spinnerPosition = sportTypeAdapter.getPosition(sportTypeString)
            spinnerPosition.let { sportTypeSpinner.setSelection(it) }
        }
        if (entry != null) {
            entry.getDateAsString()?.let { setTextIfNotAlreadySet(date, it) }
            setTextIfNotAlreadySet(summitName, entry.name)
            setTextIfNotAlreadySet(placesView, entry.getPlacesWithConnectedEntryString(requireContext(), sortFilterHelper.database, sortFilterHelper.databaseHelper).joinToString(",") + ",")
            setTextIfNotAlreadySet(countriesView, entry.countries.joinToString(",") + ",")
            setTextIfNotAlreadySet(commentText, entry.comments)
            setTextIfNotAlreadySet(participantsView, entry.participants.joinToString(",") + ",")
            setTextIfNotAlreadySet(heightMeterText, entry.heightMeter.toString())
            setTextIfNotAlreadySet(kmText, entry.kilometers.toString())
            setTextIfNotAlreadySet(paceText, entry.pace.toString())
            setTextIfNotAlreadySet(topSpeedText, entry.topSpeed.toString())
            setTextIfNotAlreadySet(topElevationText, entry.topElevation.toString())
            setTextIfNotAlreadySet(caloriesText, entry.activityData?.calories.toString())
            setTextIfNotAlreadySet(averageHRText, entry.activityData?.averageHR.toString())
            setTextIfNotAlreadySet(maxHRText, entry.activityData?.maxHR.toString())
            setTextIfNotAlreadySet(ftpText, entry.activityData?.ftp.toString())
            setTextIfNotAlreadySet(vo2maxText, entry.activityData?.vo2max.toString())
            setTextIfNotAlreadySet(powerNormText, entry.activityData?.power?.normPower.toString())
            setTextIfNotAlreadySet(powerAvgText, entry.activityData?.power?.avgPower.toString())
            setTextIfNotAlreadySet(power1sText, entry.activityData?.power?.oneSec.toString())
            setTextIfNotAlreadySet(power2sText, entry.activityData?.power?.twoSec.toString())
            setTextIfNotAlreadySet(power5sText, entry.activityData?.power?.fiveSec.toString())
            setTextIfNotAlreadySet(power10sText, entry.activityData?.power?.tenSec.toString())
            setTextIfNotAlreadySet(power20sText, entry.activityData?.power?.twentySec.toString())
            setTextIfNotAlreadySet(power30sText, entry.activityData?.power?.thirtySec.toString())
            setTextIfNotAlreadySet(power1minText, entry.activityData?.power?.oneMin.toString())
            setTextIfNotAlreadySet(power2minText, entry.activityData?.power?.twoMin.toString())
            setTextIfNotAlreadySet(power5minText, entry.activityData?.power?.fiveMin.toString())
            setTextIfNotAlreadySet(power10minText, entry.activityData?.power?.tenMin.toString())
            setTextIfNotAlreadySet(power20minText, entry.activityData?.power?.twentyMin.toString())
            setTextIfNotAlreadySet(power30minText, entry.activityData?.power?.thirtyMin.toString())
            setTextIfNotAlreadySet(power1hText, entry.activityData?.power?.oneHour.toString())
            setTextIfNotAlreadySet(power2hText, entry.activityData?.power?.twoHours.toString())
            setTextIfNotAlreadySet(power5hText, entry.activityData?.power?.fiveHours.toString())
            activityDataFromGarminConnect = entry.activityData
        }
    }


    private fun getTextWithDefaultDouble(editText: EditText): Double {
        return try {
            editText.text.toString().toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getTextWithDefaultInt(editText: EditText): Int {
        return try {
            editText.text.toString().toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun showDatePicker(eText: EditText, context: Context) {
        val cldr = Calendar.getInstance()
        var day = cldr[Calendar.DAY_OF_MONTH]
        var month = cldr[Calendar.MONTH]
        var year = cldr[Calendar.YEAR]
        if (eText.text.toString().trim() != "") {
            val dateSplitted = eText.text.toString().trim().split("-".toRegex()).toTypedArray()
            if (dateSplitted.size == 3) {
                day = dateSplitted[2].toInt()
                month = dateSplitted[1].toInt() - 1
                year = dateSplitted[0].toInt()
            }
        }
        val picker = DatePickerDialog(context, R.style.CustomDatePickerDialogTheme,
                { view: DatePicker, yearSelected: Int, monthSelected: Int, daySelected: Int -> eText.setText(view.context.getString(R.string.date_format, String.format(Locale.ENGLISH, "%02d", daySelected), String.format(Locale.ENGLISH, "%02d", monthSelected + 1), String.format(Locale.ENGLISH, "%02d", yearSelected))) }, year, month, day)
        picker.show()
    }

    private fun downloadGpxForSummitEntry(pythonExecutor: GarminPythonExecutor, entry: SummitEntry, index: Int, useTcx: Boolean, powerData: JsonObject? = null) {
        try {
            if (entry.sportType == SportType.BikeAndHike) {
                val power = entry.activityData?.power
                if (powerData != null && power != null) {
                    updatePower(powerData, power)
                }
                updateMultiSpotActivityIds(pythonExecutor, entry)
            }
            GarminPythonExecutor.Companion.AsyncDownloadGpxViaPython(pythonExecutor, listOf(entry), sortFilterHelper, useTcx, this, index).execute()
        } catch (e: java.lang.RuntimeException) {
            Log.e("AsyncDownloadActivities", e.message ?: "")
        }
    }

    private fun updateMultiSpotActivityIds(pythonExecutor: GarminPythonExecutor, entry: SummitEntry) {
        val activityId = entry.activityData?.activityId
        if (activityId != null) {
            val activityJsonFile = File(SummitViewFragment.activitiesDir, "acivity_${activityId}.json")
            if (activityJsonFile.exists()) {
                val gson = JsonParser().parse(activityJsonFile.readText()) as JsonObject
                val parsedEntry = GarminPythonExecutor.parseJsonObject(gson)
                val ids = parsedEntry.activityData?.activityIds
                if (ids != null) {
                    entry.activityData?.activityIds = ids
                }
            } else {
                val gson = pythonExecutor.getMultiSportData(activityId)
                val ids = gson.get("metadataDTO").asJsonObject.get("childIds").asJsonArray
                entry.activityData?.activityIds?.addAll(ids.map { it.asString })
            }
        }
    }

    fun getTempGpsFilePath(date: Date): Path {
        val tag = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.US).format(date)
        val fileName = String.format(Locale.ENGLISH, "track_from_%s.gpx", tag)
        return Paths.get(MainActivity.cache.toString(), fileName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        database.close()
        helper.close()
    }

    companion object {

        @JvmStatic
        fun updateInstance(entry: SummitEntry?, sortFilterHelper: SortFilterHelper, pythonExecutor: GarminPythonExecutor?): AddSummitDialog {
            val add = AddSummitDialog(sortFilterHelper, pythonExecutor)
            add.isUpdate = true
            add.currentSummitEntry = entry
            return add
        }

        private fun setTextIfNotAlreadySet(editText: EditText, setValue: String) {
            val textValue = editText.text.toString()
            if (textValue == "" || textValue == "0" || textValue == "0.0" || textValue == "null") {
                editText.setText(setValue)
            }
        }

        private fun getJsonObjectEntryNotNone(jsonObject: JsonObject, key: String): Float {
            return if (jsonObject[key].toString().toLowerCase(Locale.ROOT).contains("none")) 0.0f else jsonObject[key].asFloat
        }

        class AsyncDownloadJsonViaPython(private val pythonExecutor: GarminPythonExecutor, private val dateAsString: String, private val dialog: AddSummitDialog) : AsyncTask<Void?, Void?, Void?>() {

            var entries: List<SummitEntry>? = null
            var powerData: JsonObject? = null

            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    val allKnownEntries = getAllDownloadedSummitsFromGarmin(SummitViewFragment.activitiesDir)
                    val firstDate = allKnownEntries.minBy { it.getDateAsFloat() }?.getDateAsString()
                    val lastDate = allKnownEntries.maxBy { it.getDateAsFloat() }?.getDateAsString()
                    val knownEntriesOnDate = allKnownEntries.filter { it.getDateAsString() == dateAsString}
                    if (dateAsString != firstDate && dateAsString != lastDate && knownEntriesOnDate.isNotEmpty()) {
                        entries = knownEntriesOnDate
                    } else {
                        entries = pythonExecutor.getActivityJsonAtDate(dateAsString)
                    }
                    powerData = getPowerDataFromEntries(entries)
                } catch (e: RuntimeException) {
                    Log.e("AsyncDownloadJsonViaPython", e.message ?: "")
                }
                return null
            }

            private fun getPowerDataFromEntries(entries: List<SummitEntry>?): JsonObject? {
                if (entries != null) {
                    for (entry in entries) {
                        if (entry.sportType == SportType.BikeAndHike) {
                            return pythonExecutor.getMultiSportPowerData(dateAsString)
                        }
                    }
                }
                return null
            }

            override fun onPostExecute(param: Void?) {
                val entriesLocal = entries
                val progressBar = dialog.view?.findViewById<RelativeLayout>(R.id.loadingPanel)
                if (entriesLocal != null) {
                    if (progressBar != null) {
                        dialog.showSummitsDialog(pythonExecutor, entriesLocal, progressBar, powerData)
                    }
                } else {
                    progressBar?.visibility = View.GONE
                }
            }
        }

    }

    override fun getProgressBarForAsyncTask(): ProgressBar? {
        return progressBarDownload
    }

    override fun isStepByStepDownload(): Boolean {
        return true
    }

    override fun doInPostExecute(index: Int, successfulDownloaded: Boolean) {
        listItemsGpsDownloadSuccessful?.set(index, successfulDownloaded)
        mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = listItemsGpsDownloadSuccessful?.contains(false) == false
    }

    override fun getDialogContext(): Context {
        return currentContext
    }
}
