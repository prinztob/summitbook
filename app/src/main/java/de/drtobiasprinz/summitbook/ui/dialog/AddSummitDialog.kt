package de.drtobiasprinz.summitbook.ui.dialog

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
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
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.*
import de.drtobiasprinz.summitbook.models.Summit.Companion.CONNECTED_ACTIVITY_PREFIX
import de.drtobiasprinz.summitbook.ui.CustomAutoCompleteChips
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.getAllDownloadedSummitsFromGarmin
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.utils.InputFilterMinMax
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.round


class AddSummitDialog() : DialogFragment(), BaseDialog {
    var isUpdate = false
    var temporaryGpxFile: File? = null
    var latlngHightestPoint: TrackPoint? = null
    private lateinit var currentContext: Context
    private lateinit var sportTypeAdapter: ArrayAdapter<String>
    private var currentSummit: Summit? = null
    private var connectedSummits: MutableList<Summit> = mutableListOf()
    private var mDialog: AlertDialog? = null
    private var listItemsGpsDownloadSuccessful: BooleanArray? = null
    private lateinit var date: EditText
    private lateinit var summitName: AutoCompleteTextView
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
    private lateinit var saveEntryButton: Button

    private lateinit var participantsChips: ChipGroup
    private lateinit var equipmentsChips: ChipGroup
    private lateinit var placesChips: ChipGroup
    private lateinit var countriesChips: ChipGroup

    private lateinit var resultreceiver: FragmentResultReceiver

    private lateinit var equipmentsAdapter: ArrayAdapter<String>
    private lateinit var participantsAdapter: ArrayAdapter<String>
    private lateinit var placesAdapter: ArrayAdapter<String>

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
    private var garminDataFromGarminConnect: GarminData? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        resultreceiver = context as FragmentResultReceiver
        return inflater.inflate(R.layout.dialog_add_summit, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()
        participantsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, resultreceiver.getSortFilterHelper().entries.flatMap { it.participants }.distinct().filter { it != "" })
        equipmentsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, resultreceiver.getSortFilterHelper().entries.flatMap { it.equipments }.distinct().filter { it != "" })
        placesAdapter = getPlacesSuggestions()
        addPlaces(view)
        addCountries(view)
        addParticipants(view)
        addEquipments(view)
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
                SportType.values().map { resources.getString(it.sportNameStringId) }.toTypedArray())
        sportTypeSpinner.adapter = sportTypeAdapter

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
        if (isUpdate) {
            saveEntryButton.setText(R.string.updateButtonText)
            updateDialogFields(currentSummit, true)
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

        saveEntryButton.setOnClickListener {
            val sportType = SportType.values()[sportTypeSpinner.selectedItemPosition]
            parseSummit(sportType)
            val entry = currentSummit
            if (entry != null) {
                val garminDataLocal = entry.garminData
                val gpsTrackPath = entry.getGpsTrackPath().toFile()
                val temporaryGpxFileLocal = temporaryGpxFile
                if (garminDataLocal != null && temporaryGpxFileLocal != null && temporaryGpxFileLocal.exists() && gpsTrackPath != null) {
                    temporaryGpxFileLocal.copyTo(gpsTrackPath, overwrite = true)
                }
                val latlngHighestPointLocal = latlngHightestPoint
                if (entry.latLng == null && latlngHighestPointLocal != null) {
                    entry.latLng = latlngHighestPointLocal
                }
                entry.setBoundingBoxFromTrack()
                val adapter = SummitViewFragment.adapter
                if (isUpdate) {
                    resultreceiver.getSortFilterHelper().database.summitDao()?.updateSummit(entry)
                } else {
                    entry.id = resultreceiver.getSortFilterHelper().database.summitDao()?.addSummit(entry) ?: 0L
                    resultreceiver.getSortFilterHelper().entries.add(entry)
                    resultreceiver.getSortFilterHelper().update(resultreceiver.getSortFilterHelper().entries)
                }
                adapter.notifyDataSetChanged()
                dialog?.cancel()
            }
        }
        closeDialogButton.setOnClickListener { v: View ->
            dialog?.cancel()
            val text = if (currentSummit != null) getString(R.string.update_summit_cancel) else getString(R.string.add_new_summit_cancel)
            Toast.makeText(v.context, text, Toast.LENGTH_SHORT).show()
        }

        val addGarminActivityButton = view.findViewById<ImageButton>(R.id.add_track)
        addGarminActivityButton.setOnClickListener {
            val dateAsString = date.text.toString()
            val contextLocal = context
            if (dateAsString != "" && contextLocal != null) {
                val pythonExecutor = resultreceiver.getPythonExecutor()
                if (pythonExecutor != null) {
                    view.findViewById<RelativeLayout>(R.id.loadingPanel).visibility = View.VISIBLE
                    AsyncDownloadJsonViaPython(pythonExecutor, dateAsString, this).execute()
                } else {
                    Toast.makeText(context,
                            getString(R.string.set_user_pwd), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context,
                        getString(R.string.date_garmin_connect), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun showSummitsDialog(pythonExecutor: GarminPythonExecutor, entries: List<Summit>, progressBar: RelativeLayout, powerData: JsonObject? = null) {
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
                        downloadGpxForSummit(pythonExecutor, entry, which, useTcx, powerData)
                    }
                }
                .setPositiveButton(R.string.saveButtonText) { _: DialogInterface?, _: Int ->
                    val selectedEntries: MutableList<Summit> = mutableListOf()
                    for (i in entries.indices) {
                        if (listItemsChecked[i]) {
                            selectedEntries.add(entries[i])
                        }
                    }
                    val downloader = GarminTrackAndDataDownloader(selectedEntries, pythonExecutor)
                    downloader.downloadTracks(true)
                    downloader.extractFinalSummit()
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

    private fun addParticipants(view: View) {
        val participantsView = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewParticipants)
        participantsChips = view.findViewById(R.id.chipGroupParticipants)
        val chips = CustomAutoCompleteChips(view)
        chips.addChips(participantsAdapter, currentSummit?.participants, participantsView, participantsChips)
    }

    private fun addEquipments(view: View) {
        val equipmentsView = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewEquipments)
        showDropDown(equipmentsView)
        equipmentsChips = view.findViewById(R.id.chipGroupEquipments)
        val chips = CustomAutoCompleteChips(view)
        chips.addChips(equipmentsAdapter, currentSummit?.equipments, equipmentsView, equipmentsChips)
    }

    private fun addCountries(view: View) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, getSuggestionCountries())
        val countriesView = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewCountries)
        countriesChips = view.findViewById(R.id.chipGroupCountries)
        val chips = CustomAutoCompleteChips(view)
        chips.addChips(adapter, currentSummit?.countries, countriesView, countriesChips)
    }

    private fun addPlaces(view: View) {
        val placesView = view.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextViewPlaces)
        placesChips = view.findViewById(R.id.chipGroupPlaces)
        val chips = CustomAutoCompleteChips(view)
        chips.addChips(placesAdapter, currentSummit?.getPlacesWithConnectedEntryString(requireContext(), resultreceiver.getSortFilterHelper().database),
                placesView, placesChips)
    }

    private fun showDropDown(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteTextView.showDropDown()
            }
        }
    }

    private fun getPlacesSuggestions(addConnectedEntryString: Boolean = true): ArrayAdapter<String> {
        val suggestions: MutableList<String> = (resultreceiver.getSortFilterHelper().entries.flatMap { it.places } +
                resultreceiver.getSortFilterHelper().entries.map { it.name }).filter { !it.startsWith(CONNECTED_ACTIVITY_PREFIX) } as MutableList<String>
        val localSummit = currentSummit
        if (addConnectedEntryString && localSummit != null) {
            for (entry in resultreceiver.getSortFilterHelper().entries.filter { it != currentSummit }) {
                val differenceInMilliSec: Long = localSummit.date.time - entry.date.time
                val differenceInDays: Double = round(TimeUnit.MILLISECONDS.toDays(differenceInMilliSec).toDouble())
                if (differenceInDays in 0.0..1.0) {
                    connectedSummits.add(entry)
                    suggestions.add(entry.getConnectedEntryString(requireContext()))
                }
            }
        }

        return ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions.filter { it != "" }.distinct())
    }

    private fun parseSummit(sportType: SportType) {
        val places = updatePlacesChipValuesWithId()
        parseGarminData()
        try {
            val entry = currentSummit
            if (entry != null) {
                entry.date = Summit.parseDate(date.text.toString())
                entry.name = summitName.text.toString()
                entry.sportType = sportType
                entry.places = places
                entry.countries = countriesChips.children.toList().map { (it as Chip).text.toString() }
                entry.comments = commentText.text.toString()
                entry.elevationData.elevationGain = heightMeterText.text.toString().toInt()
                entry.kilometers = getTextWithDefaultDouble(kmText)
                entry.velocityData.avgVelocity = getTextWithDefaultDouble(paceText)
                entry.velocityData.maxVelocity = getTextWithDefaultDouble(topSpeedText)
                entry.elevationData.maxElevation = getTextWithDefaultInt(topElevationText)
                entry.participants = participantsChips.children.toList().map { (it as Chip).text.toString() }
                entry.equipments = equipmentsChips.children.toList().map { (it as Chip).text.toString() }
                entry.garminData = garminDataFromGarminConnect
            } else {
                currentSummit = Summit(
                        Summit.parseDate(date.text.toString()),
                        summitName.text.toString(),
                        sportType,
                        places,
                        countriesChips.children.toList().map { (it as Chip).text.toString() },
                        commentText.text.toString(),
                        ElevationData.parse(getTextWithDefaultInt(topElevationText), heightMeterText.text.toString().toInt()),
                        getTextWithDefaultDouble(kmText),
                        VelocityData.parse(getTextWithDefaultDouble(paceText), getTextWithDefaultDouble(topSpeedText)),
                        latlngHightestPoint?.lat, latlngHightestPoint?.lon,
                        participantsChips.children.toList().map { (it as Chip).text.toString() },
                        equipmentsChips.children.toList().map { (it as Chip).text.toString() },
                        false,
                        false,
                        mutableListOf(),
                        garminDataFromGarminConnect,
                        null
                )
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun parseGarminData() {
        garminDataFromGarminConnect?.calories = getTextWithDefaultDouble(caloriesText).toFloat()
        garminDataFromGarminConnect?.averageHR = getTextWithDefaultDouble(averageHRText).toFloat()
        garminDataFromGarminConnect?.maxHR = getTextWithDefaultDouble(maxHRText).toFloat()
        garminDataFromGarminConnect?.ftp = getTextWithDefaultInt(ftpText)
        garminDataFromGarminConnect?.vo2max = getTextWithDefaultInt(vo2maxText)
        garminDataFromGarminConnect?.power?.avgPower = getTextWithDefaultDouble(powerAvgText).toFloat()
        garminDataFromGarminConnect?.power?.normPower = getTextWithDefaultDouble(powerNormText).toFloat()
        garminDataFromGarminConnect?.power?.oneSec = getTextWithDefaultInt(power1sText)
        garminDataFromGarminConnect?.power?.twoSec = getTextWithDefaultInt(power2sText)
        garminDataFromGarminConnect?.power?.fiveSec = getTextWithDefaultInt(power5sText)
        garminDataFromGarminConnect?.power?.tenSec = getTextWithDefaultInt(power10sText)
        garminDataFromGarminConnect?.power?.twentySec = getTextWithDefaultInt(power20sText)
        garminDataFromGarminConnect?.power?.thirtySec = getTextWithDefaultInt(power30sText)
        garminDataFromGarminConnect?.power?.oneMin = getTextWithDefaultInt(power1minText)
        garminDataFromGarminConnect?.power?.twoMin = getTextWithDefaultInt(power2minText)
        garminDataFromGarminConnect?.power?.fiveMin = getTextWithDefaultInt(power5minText)
        garminDataFromGarminConnect?.power?.tenMin = getTextWithDefaultInt(power10minText)
        garminDataFromGarminConnect?.power?.twentyMin = getTextWithDefaultInt(power20minText)
        garminDataFromGarminConnect?.power?.thirtyMin = getTextWithDefaultInt(power30minText)
        garminDataFromGarminConnect?.power?.oneHour = getTextWithDefaultInt(power1hText)
        garminDataFromGarminConnect?.power?.twoHours = getTextWithDefaultInt(power2hText)
        garminDataFromGarminConnect?.power?.fiveHours = getTextWithDefaultInt(power5hText)
    }

    private fun updatePlacesChipValuesWithId(): MutableList<String> {
        val places = placesChips.children.toList().map { (it as Chip).text.toString() }.toMutableList()
        for ((i, place) in places.withIndex()) {
            for (connectedSummit in connectedSummits)
                if (place == connectedSummit.getConnectedEntryString(requireContext())) {
                    places[i] = "${Summit.CONNECTED_ACTIVITY_PREFIX}${connectedSummit.activityId}"
                }
        }
        return places
    }

    private fun updateDialogFields(entry: Summit?, updateSpinner: Boolean) {
        if (updateSpinner) {
            sportTypeSpinner.setSelection(SportType.values().indexOf(entry?.sportType))
        }
        if (entry != null) {
            entry.getDateAsString()?.let { setTextIfNotAlreadySet(date, it) }
            setTextIfNotAlreadySet(summitName, entry.name)
            setTextIfNotAlreadySet(commentText, entry.comments)
            setTextIfNotAlreadySet(heightMeterText, entry.elevationData.elevationGain.toString())
            setTextIfNotAlreadySet(kmText, entry.kilometers.toString())
            setTextIfNotAlreadySet(paceText, entry.velocityData.avgVelocity.toString())
            setTextIfNotAlreadySet(topSpeedText, entry.velocityData.maxVelocity.toString())
            setTextIfNotAlreadySet(topElevationText, entry.elevationData.maxElevation.toString())
            setTextIfNotAlreadySet(caloriesText, entry.garminData?.calories?.toInt().toString())
            setTextIfNotAlreadySet(averageHRText, entry.garminData?.averageHR?.toInt().toString())
            setTextIfNotAlreadySet(maxHRText, entry.garminData?.maxHR?.toInt().toString())
            setTextIfNotAlreadySet(ftpText, entry.garminData?.ftp.toString())
            setTextIfNotAlreadySet(vo2maxText, entry.garminData?.vo2max.toString())
            setTextIfNotAlreadySet(powerNormText, entry.garminData?.power?.normPower?.toInt().toString())
            setTextIfNotAlreadySet(powerAvgText, entry.garminData?.power?.avgPower?.toInt().toString())
            setTextIfNotAlreadySet(power1sText, entry.garminData?.power?.oneSec.toString())
            setTextIfNotAlreadySet(power2sText, entry.garminData?.power?.twoSec.toString())
            setTextIfNotAlreadySet(power5sText, entry.garminData?.power?.fiveSec.toString())
            setTextIfNotAlreadySet(power10sText, entry.garminData?.power?.tenSec.toString())
            setTextIfNotAlreadySet(power20sText, entry.garminData?.power?.twentySec.toString())
            setTextIfNotAlreadySet(power30sText, entry.garminData?.power?.thirtySec.toString())
            setTextIfNotAlreadySet(power1minText, entry.garminData?.power?.oneMin.toString())
            setTextIfNotAlreadySet(power2minText, entry.garminData?.power?.twoMin.toString())
            setTextIfNotAlreadySet(power5minText, entry.garminData?.power?.fiveMin.toString())
            setTextIfNotAlreadySet(power10minText, entry.garminData?.power?.tenMin.toString())
            setTextIfNotAlreadySet(power20minText, entry.garminData?.power?.twentyMin.toString())
            setTextIfNotAlreadySet(power30minText, entry.garminData?.power?.thirtyMin.toString())
            setTextIfNotAlreadySet(power1hText, entry.garminData?.power?.oneHour.toString())
            setTextIfNotAlreadySet(power2hText, entry.garminData?.power?.twoHours.toString())
            setTextIfNotAlreadySet(power5hText, entry.garminData?.power?.fiveHours.toString())
            garminDataFromGarminConnect = entry.garminData
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
                { view: DatePicker, yearSelected: Int, monthSelected: Int, daySelected: Int -> eText.setText(view.context.getString(R.string.date_format, String.format(requireContext().resources.configuration.locales[0], "%02d", daySelected), String.format(requireContext().resources.configuration.locales[0], "%02d", monthSelected + 1), String.format(requireContext().resources.configuration.locales[0], "%02d", yearSelected))) }, year, month, day)
        picker.show()
    }

    private fun downloadGpxForSummit(pythonExecutor: GarminPythonExecutor, entry: Summit, index: Int, useTcx: Boolean, powerData: JsonObject? = null) {
        try {
            if (entry.sportType == SportType.BikeAndHike) {
                val power = entry.garminData?.power
                if (powerData != null && power != null) {
                    updatePower(powerData, power)
                }
                updateMultiSpotActivityIds(pythonExecutor, entry)
            }
            GarminPythonExecutor.Companion.AsyncDownloadGpxViaPython(pythonExecutor, listOf(entry), resultreceiver.getAllActivitiesFromThirdParty(), resultreceiver.getSortFilterHelper(), useTcx, this, index).execute()
        } catch (e: java.lang.RuntimeException) {
            Log.e("AsyncDownloadActivities", e.message ?: "")
        }
    }

    private fun updateMultiSpotActivityIds(pythonExecutor: GarminPythonExecutor, entry: Summit) {
        val activityId = entry.garminData?.activityId
        if (activityId != null) {
            val activityJsonFile = File(activitiesDir, "acivity_${activityId}.json")
            if (activityJsonFile.exists()) {
                val gson = JsonParser().parse(activityJsonFile.readText()) as JsonObject
                val parsedEntry = GarminPythonExecutor.parseJsonObject(gson)
                val ids = parsedEntry.garminData?.activityIds
                if (ids != null) {
                    entry.garminData?.activityIds = ids
                }
            } else {
                val gson = pythonExecutor.getMultiSportData(activityId)
                val ids = gson.get("metadataDTO").asJsonObject.get("childIds").asJsonArray
                entry.garminData?.activityIds?.addAll(ids.map { it.asString })
            }
        }
    }

    private fun getTempGpsFilePath(date: Date): Path {
        val tag = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.US).format(date)
        val fileName = String.format(requireContext().resources.configuration.locales[0], "track_from_%s.gpx", tag)
        return Paths.get(MainActivity.cache.toString(), fileName)
    }


    private fun getSuggestionCountries(): List<String> {
        val locales = Locale.getAvailableLocales()
        val countries = mutableListOf<String>()
        locales.forEach {
            val country = it.displayCountry
            if (country.trim().isNotEmpty() && !countries.contains(country)) {
                countries.add(country)
            }
        }
        return countries
    }

    companion object {

        @JvmStatic
        fun updateInstance(entry: Summit?): AddSummitDialog {
            val add = AddSummitDialog()
            add.isUpdate = true
            add.currentSummit = entry
            return add
        }

        private fun setTextIfNotAlreadySet(editText: EditText, setValue: String) {
            val textValue = editText.text.toString()
            if ((textValue == "" || textValue == "0" || textValue == "0.0" || textValue == "null") &&
                    !(setValue == "" || setValue == "0" || setValue == "0.0" || setValue == "null")) {
                editText.setText(setValue)
            }
        }

        private fun getJsonObjectEntryNotNone(jsonObject: JsonObject, key: String): Float {
            return if (jsonObject[key].toString().toLowerCase(Locale.ROOT).contains("none")) 0.0f else jsonObject[key].asFloat
        }

        class AsyncDownloadJsonViaPython(private val pythonExecutor: GarminPythonExecutor, private val dateAsString: String, private val dialog: AddSummitDialog) : AsyncTask<Void?, Void?, Void?>() {

            var entries: List<Summit>? = null
            var powerData: JsonObject? = null

            override fun doInBackground(vararg params: Void?): Void? {
                try {
                    val allKnownEntries = getAllDownloadedSummitsFromGarmin(activitiesDir)
                    val firstDate = allKnownEntries.minByOrNull { it.getDateAsFloat() }?.getDateAsString()
                    val lastDate = allKnownEntries.maxByOrNull { it.getDateAsFloat() }?.getDateAsString()
                    val knownEntriesOnDate = allKnownEntries.filter { it.getDateAsString() == dateAsString }
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

            private fun getPowerDataFromEntries(entries: List<Summit>?): JsonObject? {
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
        return view?.findViewById(R.id.progressBarDownload)
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
