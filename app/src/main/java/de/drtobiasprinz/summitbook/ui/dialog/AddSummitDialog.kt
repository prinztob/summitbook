package de.drtobiasprinz.summitbook.ui.dialog

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.DialogAddSummitBinding
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.CONNECTED_ACTIVITY_PREFIX
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.ui.CustomAutoCompleteChips
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor.Companion.getAllDownloadedSummitsFromGarmin
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.activitiesDir
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.utils.InputFilterMinMax
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import de.drtobiasprinz.summitbook.utils.Constants.BUNDLE_ID
import de.drtobiasprinz.summitbook.utils.Constants.EDIT
import de.drtobiasprinz.summitbook.utils.Constants.NEW
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.round
import kotlin.math.roundToInt

@AndroidEntryPoint
class AddSummitDialog : DialogFragment(), BaseDialog {

    @Inject
    lateinit var entity: Summit

    private val viewModel: DatabaseViewModel by activityViewModels()

    private lateinit var binding: DialogAddSummitBinding

    private var listItemsGpsDownloadSuccessful: BooleanArray? = null
    private var mDialog: AlertDialog? = null
    private var temporaryGpxFile: File? = null
    var gpxTrackUri: Uri? = null
    private var latlngHighestPoint: TrackPoint? = null
    private lateinit var currentContext: Context
    private var connectedSummits: MutableList<Summit> = mutableListOf()
    private var garminDataFromGarminConnect: GarminData? = null

    private lateinit var equipmentsAdapter: ArrayAdapter<String>
    private lateinit var participantsAdapter: ArrayAdapter<String>
    private lateinit var placesAdapter: ArrayAdapter<String>

    private var summitId = 0L

    private var type = ""
    private var isEdit = false
    var isBookmark = false
    var fromReceiverActivity = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DialogAddSummitBinding.inflate(layoutInflater, container, false)
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        summitId = arguments?.getLong(BUNDLE_ID) ?: 0
        if (summitId > 0) {
            type = EDIT
            isEdit = true
        } else {
            isEdit = false
            type = NEW
        }
        val list = if (isBookmark) {
            if (fromReceiverActivity) {
                viewModel.getAllBookmarks()
            }
            viewModel.bookmarksList
        } else {
            if (fromReceiverActivity) {
                viewModel.getAllSummits()
            }
            viewModel.summitsList
        }
        list.observe(viewLifecycleOwner) { summitData ->
            summitData.data.let { summits ->
                if (summits != null) {
                    setData(summits, view)
                }
            }
        }
    }

    private fun setData(
        summits: List<Summit>,
        view: View
    ) {

        binding.apply {
            if (gpxTrackUri != null) {
                addAndAnalyzeTrack(gpxTrackUri)
            }
            btnCancel.setOnClickListener {
                dismiss()
                val text =
                    if (isEdit) getString(R.string.update_summit_cancel) else getString(
                        R.string.add_new_summit_cancel
                    )
                Snackbar.make(it, text, Snackbar.LENGTH_SHORT).show()
            }
            if (type == EDIT) {
                val summitToEdit = summits.firstOrNull { it.id == summitId }
                if (summitToEdit != null) {
                    entity = summitToEdit
                    updateBaseBindings(view, summits)
                    updateDialogFields(true)
                    btnSave.text = getString(R.string.update)
                    isBookmark = entity.isBookmark
                }
            } else {
                updateBaseBindings(view, summits)
            }

            btnSave.setOnClickListener {
                val sportType = SportType.values()[activities.selectedItemPosition]
                parseSummit(sportType)

                val latlngHighestPointLocal = latlngHighestPoint
                if (entity.latLng == null && latlngHighestPointLocal != null &&
                    latlngHighestPointLocal.lat != 0.0 && latlngHighestPointLocal.lon != 0.0) {
                    entity.latLng = latlngHighestPointLocal
                }
                entity.hasTrack = true
                if (isBookmark) {
                    entity.isBookmark = true
                }
                viewModel.saveSummit(isEdit, entity).invokeOnCompletion {
                    val garminDataLocal = entity.garminData
                    val gpsTrackPath = entity.getGpsTrackPath().toFile()
                    val temporaryGpxFileLocal = temporaryGpxFile
                    if (garminDataLocal != null && temporaryGpxFileLocal != null && temporaryGpxFileLocal.exists() && gpsTrackPath != null) {
                        temporaryGpxFileLocal.copyTo(gpsTrackPath, overwrite = true)
                    }
                }
                dismiss()
            }
            addGpsTrack.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                resultLauncher.launch(intent)
            }
            if (isBookmark) {
                tourDate.visibility = View.GONE
                addTrackFromGarmin.visibility = View.GONE
                summitName.hint = getString(R.string.add_new_bookmark)
                expandMore.visibility = View.GONE
                expandMorePerformance.visibility = View.GONE
            } else {
                addTrackFromGarmin.setOnClickListener {
                    val dateAsString = binding.tourDate.text.toString()
                    val contextLocal = context
                    if (dateAsString != "" && contextLocal != null) {
                        val executor = pythonExecutor
                        if (executor != null) {
                            binding.loadingPanel.visibility = View.VISIBLE
                            pythonExecutor?.let { it1 ->
                                downloadJsonViaPython(
                                    dateAsString,
                                    it1
                                )
                            }
                        } else {
                            Toast.makeText(
                                context,
                                getString(R.string.set_user_pwd), Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.date_garmin_connect), Toast.LENGTH_LONG
                        ).show()
                    }
                }
                setExpandMoreButtons()
            }
        }
    }

    private fun downloadJsonViaPython(dateAsString: String, pythonExecutor: GarminPythonExecutor) {
        var entries: List<Summit> = emptyList()
        var powerData: JsonObject? = null
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val allKnownEntries = getAllDownloadedSummitsFromGarmin(activitiesDir)
                    val firstDate =
                        allKnownEntries.minByOrNull { it.getDateAsFloat() }?.getDateAsString()
                    val lastDate =
                        allKnownEntries.maxByOrNull { it.getDateAsFloat() }?.getDateAsString()
                    val knownEntriesOnDate =
                        allKnownEntries.filter { it.getDateAsString() == dateAsString }
                    entries =
                        if (dateAsString != firstDate && dateAsString != lastDate && knownEntriesOnDate.isNotEmpty()) {
                            knownEntriesOnDate
                        } else {
                            pythonExecutor.getActivityJsonAtDate(dateAsString)
                        }
                    powerData = getPowerDataFromEntries(entries, pythonExecutor, dateAsString)
                } catch (e: RuntimeException) {
                    Log.e("AsyncDownloadJsonViaPython", e.message ?: "")
                }
            }
            if (entries.isNotEmpty()) {
                showSummitsDialog(
                    pythonExecutor, entries, binding.loadingPanel, powerData
                )
            } else {
                binding.loadingPanel.visibility = View.GONE
            }
        }
    }

    private fun getPowerDataFromEntries(
        entries: List<Summit>,
        pythonExecutor: GarminPythonExecutor,
        dateAsString: String
    ): JsonObject? {
        for (entry in entries) {
            if (entry.sportType == SportType.BikeAndHike) {
                return pythonExecutor.getMultiSportPowerData(dateAsString)
            }
        }
        return null
    }

    private fun DialogAddSummitBinding.setExpandMoreButtons() {
        expandMore.setOnClickListener {
            if (expandMore.text == getString(R.string.more)) {
                expandMore.text = getString(R.string.less)
                expandMore.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_baseline_expand_less_24, 0, 0, 0
                )
                additionalDataFieldsView.visibility = View.VISIBLE
            } else {
                expandMore.text = getString(R.string.more)
                expandMore.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_baseline_expand_more_24, 0, 0, 0
                )
                additionalDataFieldsView.visibility = View.GONE
            }
        }

        expandMorePerformance.setOnClickListener {
            if (expandMorePerformance.text == getString(R.string.more_performance)) {
                expandMorePerformance.text = getString(R.string.less_performance)
                expandMorePerformance.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_baseline_expand_less_24, 0, 0, 0
                )
                performanceDataFieldsView.visibility = View.VISIBLE
            } else {
                expandMorePerformance.text = getString(R.string.more_performance)
                expandMorePerformance.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_baseline_expand_more_24, 0, 0, 0
                )
                performanceDataFieldsView.visibility = View.GONE
            }
        }
    }

    private fun DialogAddSummitBinding.updateBaseBindings(
        view: View,
        summits: List<Summit>
    ) {
        heightMeter.addTextChangedListener(watcher)
        kilometers.addTextChangedListener(watcher)
        kilometers.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 999))
        tourDate.addTextChangedListener(watcher)
        tourDate.inputType = InputType.TYPE_NULL
        tourDate.onFocusChangeListener =
            View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    showDatePicker(tourDate, view.context)
                }
            }
        activities.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            SportType.values().map { resources.getString(it.sportNameStringId) }
                .toTypedArray()
        )
        setImageColor()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                participantsAdapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    summits.flatMap { it.participants }.distinct().filter { it != "" })
                equipmentsAdapter = ArrayAdapter(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    summits.flatMap { it.equipments }.distinct().filter { it != "" })
                placesAdapter = getPlacesSuggestions(summits)
            }
            summitName.setAdapter(getPlacesSuggestions(summits, false))
            addPlaces(view, summits)
            addCountries(view)
            addParticipants(view)
            addEquipments(view)
        }
    }

    private fun setImageColor() {
        binding.imageParticipants.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.black)
        binding.imageCountries.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.black)
        binding.imageEquipments.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.black)
        binding.imagePlaces.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.black)

    }


    private fun showSummitsDialog(
        pythonExecutor: GarminPythonExecutor,
        entries: List<Summit>,
        progressBar: RelativeLayout,
        powerData: JsonObject? = null
    ) {
        val sharedPreferences = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val useTcx = sharedPreferences?.getBoolean("download_tcx", false) ?: false

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
        mBuilder.setMultiChoiceItems(
            listItems, listItemsChecked
        ) { _: DialogInterface, which: Int, isChecked: Boolean ->
            val entry = entries[which]
            listItemsGpsDownloadSuccessful?.set(which, false)
            listItemsChecked[which] = isChecked
            mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                listItemsGpsDownloadSuccessful?.contains(false) == false
            if (isChecked) {
                downloadGpxForSummit(pythonExecutor, entry, which, useTcx, powerData)
            }
        }.setPositiveButton(R.string.saveButtonText) { _: DialogInterface?, _: Int ->
            val selectedEntries: MutableList<Summit> = mutableListOf()
            for (i in entries.indices) {
                if (listItemsChecked[i]) {
                    selectedEntries.add(entries[i])
                }
            }

            val downloader = GarminTrackAndDataDownloader(selectedEntries, pythonExecutor, useTcx)
            lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    downloader.downloadTracks(true)
                    downloader.extractFinalSummit()
                    val entry = downloader.finalEntry
                    if (entry != null) {
                        entry.activityId = entity.activityId
                        entry.id = entity.id
                        entry.imageIds = entity.imageIds
                        entry.velocityData = entity.velocityData
                        entity = entry
                        temporaryGpxFile = getTempGpsFilePath(entry.date).toFile()
                        downloader.composeFinalTrack(temporaryGpxFile)
                    }
                }
                val entry = downloader.finalEntry
                if (entry != null) {
                    latlngHighestPoint = entry.latLng
                    updateDialogFields(!isEdit)
                    Toast.makeText(
                        context,
                        context?.getString(R.string.garmin_add_successful, entry.name),
                        Toast.LENGTH_LONG
                    ).show()
                }
                progressBar.visibility = View.GONE
            }
        }.setNegativeButton(R.string.cancelButtonText) { _: DialogInterface?, _: Int ->
            Toast.makeText(
                context, context?.getString(R.string.garmin_add_cancel), Toast.LENGTH_SHORT
            ).show()
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
                "1" -> power.oneSec = getJsonObjectEntryNotNone(element).toInt()
                "2" -> power.twoSec = getJsonObjectEntryNotNone(element).toInt()
                "5" -> power.fiveSec = getJsonObjectEntryNotNone(element).toInt()
                "10" -> power.tenSec = getJsonObjectEntryNotNone(element).toInt()
                "20" -> power.twentySec = getJsonObjectEntryNotNone(element).toInt()
                "30" -> power.thirtySec = getJsonObjectEntryNotNone(element).toInt()
                "60" -> power.oneMin = getJsonObjectEntryNotNone(element).toInt()
                "120" -> power.twoMin = getJsonObjectEntryNotNone(element).toInt()
                "300" -> power.fiveMin = getJsonObjectEntryNotNone(element).toInt()
                "600" -> power.tenMin = getJsonObjectEntryNotNone(element).toInt()
                "1200" -> power.twentyMin = getJsonObjectEntryNotNone(element).toInt()
                "1800" -> power.thirtyMin = getJsonObjectEntryNotNone(element).toInt()
                "3600" -> power.oneHour = getJsonObjectEntryNotNone(element).toInt()
                "7200" -> power.twoHours = getJsonObjectEntryNotNone(element).toInt()
                "18000" -> power.fiveHours = getJsonObjectEntryNotNone(element).toInt()
            }
        }
    }

    private fun addParticipants(view: View) {
        CustomAutoCompleteChips(view).addChips(
            participantsAdapter,
            entity.participants,
            binding.autoCompleteTextViewParticipants,
            binding.chipGroupParticipants
        )
    }

    private fun addEquipments(view: View) {
        showDropDown(binding.autoCompleteTextViewEquipments)
        CustomAutoCompleteChips(view).addChips(
            equipmentsAdapter,
            entity.equipments,
            binding.autoCompleteTextViewEquipments,
            binding.chipGroupEquipments
        )
    }

    private fun addCountries(view: View) {
        val adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_dropdown_item_1line, getSuggestionCountries()
        )
        CustomAutoCompleteChips(view).addChips(
            adapter,
            entity.countries,
            binding.autoCompleteTextViewCountries,
            binding.chipGroupCountries
        )
    }

    private fun addPlaces(view: View, summits: List<Summit>) {
        CustomAutoCompleteChips(view).addChips(
            placesAdapter, entity.getPlacesWithConnectedEntryString(
                requireContext(), summits
            ), binding.autoCompleteTextViewPlaces, binding.chipGroupPlaces
        )
    }

    private fun showDropDown(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteTextView.showDropDown()
            }
        }
    }

    private fun getPlacesSuggestions(
        summits: List<Summit>,
        addConnectedEntryString: Boolean = true
    ): ArrayAdapter<String> {
        val suggestions: MutableList<String> =
            (summits.flatMap { it.places } + summits.map { it.name }).filter {
                it != "" && !it.startsWith(
                    CONNECTED_ACTIVITY_PREFIX
                )
            } as MutableList<String>
        val localSummit = entity
        if (addConnectedEntryString) {
            for (entry in summits.filter { it != entity }) {
                val differenceInMilliSec: Long = localSummit.date.time - entry.date.time
                val differenceInDays: Double =
                    round(TimeUnit.MILLISECONDS.toDays(differenceInMilliSec).toDouble())
                if (differenceInDays in 0.0..1.0) {
                    connectedSummits.add(entry)
                    suggestions.add(entry.getConnectedEntryString(requireContext()))
                }
            }
        }

        return ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            suggestions.filter { it != "" }.distinct()
        )
    }

    private fun parseSummit(sportType: SportType) {
        val places = updatePlacesChipValuesWithId()
        parseGarminData()
        try {
            entity.date =
                if (isBookmark) Date() else Summit.parseDate(binding.tourDate.text.toString())
            entity.name = binding.summitName.text.toString()
            entity.sportType = sportType
            entity.places = places
            entity.countries =
                binding.chipGroupCountries.children.toList().map { (it as Chip).text.toString() }
            entity.comments = binding.comments.text.toString()
            entity.elevationData.elevationGain = binding.heightMeter.text.toString().toInt()
            entity.kilometers = getTextWithDefaultDouble(binding.kilometers)
            entity.velocityData.avgVelocity = getTextWithDefaultDouble(binding.pace)
            entity.velocityData.maxVelocity = getTextWithDefaultDouble(binding.topSpeed)
            entity.elevationData.maxElevation = getTextWithDefaultInt(binding.topElevation)
            entity.participants =
                binding.chipGroupParticipants.children.toList().map { (it as Chip).text.toString() }
            entity.equipments =
                binding.chipGroupEquipments.children.toList().map { (it as Chip).text.toString() }
            entity.garminData = garminDataFromGarminConnect
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun parseGarminData() {
        garminDataFromGarminConnect?.calories = getTextWithDefaultDouble(binding.calories).toFloat()
        garminDataFromGarminConnect?.averageHR =
            getTextWithDefaultDouble(binding.averageHr).toFloat()
        garminDataFromGarminConnect?.maxHR = getTextWithDefaultDouble(binding.maxHr).toFloat()
        garminDataFromGarminConnect?.ftp = getTextWithDefaultInt(binding.ftp)
        garminDataFromGarminConnect?.vo2max = getTextWithDefaultDouble(binding.vo2Max).toFloat()
        garminDataFromGarminConnect?.power?.avgPower =
            getTextWithDefaultDouble(binding.avgPower).toFloat()
        garminDataFromGarminConnect?.power?.normPower =
            getTextWithDefaultDouble(binding.normPower).toFloat()
        garminDataFromGarminConnect?.power?.oneSec = getTextWithDefaultInt(binding.power1s)
        garminDataFromGarminConnect?.power?.twoSec = getTextWithDefaultInt(binding.power2s)
        garminDataFromGarminConnect?.power?.fiveSec = getTextWithDefaultInt(binding.power5s)
        garminDataFromGarminConnect?.power?.tenSec = getTextWithDefaultInt(binding.power10s)
        garminDataFromGarminConnect?.power?.twentySec = getTextWithDefaultInt(binding.power20s)
        garminDataFromGarminConnect?.power?.thirtySec = getTextWithDefaultInt(binding.power30s)
        garminDataFromGarminConnect?.power?.oneMin = getTextWithDefaultInt(binding.power1min)
        garminDataFromGarminConnect?.power?.twoMin = getTextWithDefaultInt(binding.power2min)
        garminDataFromGarminConnect?.power?.fiveMin = getTextWithDefaultInt(binding.power5min)
        garminDataFromGarminConnect?.power?.tenMin = getTextWithDefaultInt(binding.power10min)
        garminDataFromGarminConnect?.power?.twentyMin = getTextWithDefaultInt(binding.power20min)
        garminDataFromGarminConnect?.power?.thirtyMin = getTextWithDefaultInt(binding.power30min)
        garminDataFromGarminConnect?.power?.oneHour = getTextWithDefaultInt(binding.power1h)
        garminDataFromGarminConnect?.power?.twoHours = getTextWithDefaultInt(binding.power2h)
        garminDataFromGarminConnect?.power?.fiveHours = getTextWithDefaultInt(binding.power5h)
    }

    private fun updatePlacesChipValuesWithId(): MutableList<String> {
        val places = binding.chipGroupPlaces.children.toList().map { (it as Chip).text.toString() }
            .toMutableList()
        for ((i, place) in places.withIndex()) {
            for (connectedSummit in connectedSummits) if (place == connectedSummit.getConnectedEntryString(
                    requireContext()
                )
            ) {
                places[i] = "$CONNECTED_ACTIVITY_PREFIX${connectedSummit.activityId}"
            }
        }
        return places
    }

    private fun updateDialogFields(updateSpinner: Boolean) {
        if (updateSpinner) {
            binding.activities.setSelection(SportType.values().indexOf(entity.sportType))
        }
        entity.getDateAsString()?.let { setTextIfNotAlreadySet(binding.tourDate, it) }
        setTextIfNotAlreadySet(binding.summitName, entity.name)
        setTextIfNotAlreadySet(binding.comments, entity.comments)
        setTextIfNotAlreadySet(
            binding.heightMeter, entity.elevationData.elevationGain.toString()
        )
        setTextIfNotAlreadySet(binding.kilometers, entity.kilometers.toString())
        setTextIfNotAlreadySet(binding.pace, entity.velocityData.avgVelocity.toString())
        setTextIfNotAlreadySet(binding.topSpeed, entity.velocityData.maxVelocity.toString())
        setTextIfNotAlreadySet(
            binding.topElevation, entity.elevationData.maxElevation.toString()
        )
        setTextIfNotAlreadySet(binding.calories, entity.garminData?.calories?.toInt().toString())
        setTextIfNotAlreadySet(
            binding.averageHr, entity.garminData?.averageHR?.toInt().toString()
        )
        setTextIfNotAlreadySet(binding.maxHr, entity.garminData?.maxHR?.toInt().toString())
        setTextIfNotAlreadySet(binding.ftp, entity.garminData?.ftp.toString())
        setTextIfNotAlreadySet(binding.vo2Max, entity.garminData?.vo2max.toString())
        setTextIfNotAlreadySet(
            binding.normPower, entity.garminData?.power?.normPower?.toInt().toString()
        )
        setTextIfNotAlreadySet(
            binding.avgPower, entity.garminData?.power?.avgPower?.toInt().toString()
        )
        setTextIfNotAlreadySet(binding.power1s, entity.garminData?.power?.oneSec.toString())
        setTextIfNotAlreadySet(binding.power2s, entity.garminData?.power?.twoSec.toString())
        setTextIfNotAlreadySet(binding.power5s, entity.garminData?.power?.fiveSec.toString())
        setTextIfNotAlreadySet(binding.power10s, entity.garminData?.power?.tenSec.toString())
        setTextIfNotAlreadySet(binding.power20s, entity.garminData?.power?.twentySec.toString())
        setTextIfNotAlreadySet(binding.power30s, entity.garminData?.power?.thirtySec.toString())
        setTextIfNotAlreadySet(binding.power1min, entity.garminData?.power?.oneMin.toString())
        setTextIfNotAlreadySet(binding.power2min, entity.garminData?.power?.twoMin.toString())
        setTextIfNotAlreadySet(binding.power5min, entity.garminData?.power?.fiveMin.toString())
        setTextIfNotAlreadySet(binding.power10min, entity.garminData?.power?.tenMin.toString())
        setTextIfNotAlreadySet(
            binding.power20min, entity.garminData?.power?.twentyMin.toString()
        )
        setTextIfNotAlreadySet(
            binding.power30min, entity.garminData?.power?.thirtyMin.toString()
        )
        setTextIfNotAlreadySet(binding.power1h, entity.garminData?.power?.oneHour.toString())
        setTextIfNotAlreadySet(binding.power2h, entity.garminData?.power?.twoHours.toString())
        setTextIfNotAlreadySet(binding.power5h, entity.garminData?.power?.fiveHours.toString())
        garminDataFromGarminConnect = entity.garminData
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            binding.btnSave.isEnabled =
                !(isEmpty(binding.summitName) || isEmpty(binding.heightMeter) ||
                        isEmpty(binding.kilometers) || (if (isBookmark) false else isEmpty(binding.tourDate)))
        }

        private fun isEmpty(editText: EditText): Boolean {
            return TextUtils.isEmpty(editText.text.toString().trim { it <= ' ' })
        }
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultData ->
            if (resultData.resultCode == Activity.RESULT_OK) {
                resultData?.data?.data.also { uri ->
                    addAndAnalyzeTrack(uri)
                }
            }
        }

    private fun addAndAnalyzeTrack(uri: Uri?) {
        if (entity.name == "") {
            entity = createNewSummit()
        }
        if (temporaryGpxFile == null) {
            temporaryGpxFile = GarminTrackAndDataDownloader.getTempGpsFilePath(
                SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.US).format(Date())
            ).toFile()
        }
        if (uri != null) {
            requireContext().contentResolver?.openInputStream(uri)?.use { inputStream ->
                uploadGpxFile(inputStream, entity, view)
                binding.loadingPanel.visibility = View.VISIBLE
                var python = pythonInstance
                if (python == null) {
                    if (!Python.isStarted()) {
                        Python.start(AndroidPlatform(requireContext()))
                    }
                    python = Python.getInstance()
                }
                asyncAnalyzeGpsTracks(entity, python)
            }
        }
    }

    private fun createNewSummit(): Summit {
        return Summit(
            Date(),
            if (binding.summitName.text.toString() != "") binding.summitName.text.toString() else "New Bookmark",
            SportType.Bicycle,
            emptyList(),
            emptyList(),
            binding.comments.text.toString(),
            ElevationData.parse(0, getTextWithDefaultInt(binding.heightMeter)),
            getTextWithDefaultDouble(binding.kilometers),
            VelocityData.parse(0.0, 0.0),
            0.0,
            0.0,
            emptyList(),
            emptyList(),
            isFavorite = false,
            isPeak = false,
            imageIds = mutableListOf(),
            garminData = null,
            trackBoundingBox = null,
            isBookmark = isBookmark
        )
    }

    private fun uploadGpxFile(inputStream: InputStream, entry: Summit, v: View?) {
        try {
            Files.copy(inputStream, entry.getGpsTrackPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                v?.context,
                v?.context?.getString(R.string.add_gpx_failed, entry.name),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            Toast.makeText(
                v?.context,
                v?.context?.getString(R.string.add_gpx_failed, entry.name),
                Toast.LENGTH_SHORT
            ).show()
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
        val calendar = Calendar.getInstance()
        var day = calendar[Calendar.DAY_OF_MONTH]
        var month = calendar[Calendar.MONTH]
        var year = calendar[Calendar.YEAR]
        if (eText.text.toString().trim() != "") {
            val dateSplit = eText.text.toString().trim().split("-".toRegex()).toTypedArray()
            if (dateSplit.size == 3) {
                day = dateSplit[2].toInt()
                month = dateSplit[1].toInt() - 1
                year = dateSplit[0].toInt()
            }
        }
        val picker = DatePickerDialog(
            context,
            R.style.CustomDatePickerDialogTheme,
            { view: DatePicker, yearSelected: Int, monthSelected: Int, daySelected: Int ->
                eText.setText(
                    view.context.getString(
                        R.string.date_format, String.format(
                            requireContext().resources.configuration.locales[0], "%02d", daySelected
                        ), String.format(
                            requireContext().resources.configuration.locales[0],
                            "%02d",
                            monthSelected + 1
                        ), String.format(
                            requireContext().resources.configuration.locales[0],
                            "%02d",
                            yearSelected
                        )
                    )
                )
            },
            year,
            month,
            day
        )
        picker.show()
    }

    private fun downloadGpxForSummit(
        pythonExecutor: GarminPythonExecutor,
        entry: Summit,
        index: Int,
        useTcx: Boolean,
        powerData: JsonObject? = null
    ) {
        try {
            if (entry.sportType == SportType.BikeAndHike) {
                val power = entry.garminData?.power
                if (powerData != null && power != null) {
                    updatePower(powerData, power)
                }
                updateMultiSpotActivityIds(pythonExecutor, entry)
            }
            val downloader = GarminTrackAndDataDownloader(
                listOf(entry),
                MainActivity.pythonExecutor, useTcx
            )
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    downloader.downloadTracks()
                    val activityId = downloader.finalEntry?.garminData?.activityId
                    if (activityId != null) {
                        downloader.composeFinalTrack(
                            GarminTrackAndDataDownloader.getTempGpsFilePath(
                                activityId
                            ).toFile()
                        )
                    }
                }
                if (index != -1) {
                    listItemsGpsDownloadSuccessful?.set(
                        index,
                        downloader.downloadedTracks.none { !it.exists() })
                    mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        listItemsGpsDownloadSuccessful?.contains(false) == false
                }
                binding.loadingPanel.visibility = View.GONE
                binding.loadingPanel.tooltipText = ""
            }
        } catch (e: java.lang.RuntimeException) {
            Log.e("AsyncDownloadActivities", e.message ?: "")
        }
    }

    private fun updateMultiSpotActivityIds(pythonExecutor: GarminPythonExecutor, entry: Summit) {
        val activityId = entry.garminData?.activityId
        if (activityId != null) {
            val activityJsonFile = File(activitiesDir, "activity_${activityId}.json")
            if (activityJsonFile.exists()) {
                val gson = JsonParser.parseString(activityJsonFile.readText()) as JsonObject
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
        val fileName = String.format(
            requireContext().resources.configuration.locales[0], "track_from_%s.gpx", tag
        )
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

    private fun asyncAnalyzeGpsTracks(entry: Summit, python: Python) {
        var highestElevation: TrackPoint? = null
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (entry.hasGpsTrack()) {
                        Log.i(
                            "AddSummitDialog.asyncAnalyzeGpsTracks",
                            "Entry ${entry.name} will be simplified"
                        )
                        GpxPyExecutor(python).createSimplifiedGpxTrack(entry.getGpsTrackPath())
                        entry.setGpsTrack()
                        highestElevation = entry.gpsTrack?.getHighestElevation()
                        entry.gpsTrack?.setDistance()

                    } else {
                        Log.i(
                            "asyncAnalyzeGpsTracks",
                            "Entry ${entry.name} has no gpx track -> Nothing to simplify"
                        )
                    }
                } catch (ex: RuntimeException) {
                    Log.e("asyncAnalyzeGpsTracks", "Error in simplify track: ${ex.message}")
                }

            }
            Log.i("asyncAnalyzeGpaTracks", "Gpx tracks simplified.")
            binding.loadingPanel.visibility = View.GONE
            entry.lat = highestElevation?.lat
            entry.lng = highestElevation?.lon
            entry.latLng = highestElevation
            viewModel.saveSummit(true, entry)
            val gpsTrack = entry.gpsTrack
            if (gpsTrack != null) {
                val nameFromTrack = gpsTrack.gpxTrack?.metadata?.name
                    ?: gpsTrack.gpxTrack?.tracks?.toList()?.blockingGet()?.first()?.name
                binding.summitName.setText(nameFromTrack)
                if (gpsTrack.hasNoTrackPoints()) {
                    gpsTrack.parseTrack(useSimplifiedIfExists = false)
                }
            }
            val gpxPyJsonFile = entry.getGpxPyPath().toFile()
            if (gpxPyJsonFile.exists()) {
                val gpxPyJson =
                    JsonParser.parseString(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

                val elevationGain = try {
                    gpxPyJson.getAsJsonPrimitive("elevation_gain").asDouble.roundToInt()
                } catch (_: ClassCastException) {
                    0
                }
                entry.elevationData.elevationGain = elevationGain
                binding.heightMeter.filters = arrayOf()
                binding.heightMeter.setText(elevationGain.toString())
                binding.heightMeter.filters =
                    arrayOf<InputFilter>(InputFilterMinMax(0, 9999))

                val maxElevation = try {
                    gpxPyJson.getAsJsonPrimitive("max_elevation").asDouble.roundToInt()
                } catch (_: ClassCastException) {
                    0
                }
                if (maxElevation > 0) {
                    entry.elevationData.maxElevation = maxElevation
                    binding.topElevation.setText(maxElevation.toString())
                }
                var distance = try {
                    gpxPyJson.getAsJsonPrimitive("moving_distance").asDouble / 1000
                } catch (_: ClassCastException) {
                    0.0
                }
                if (distance == 0.0 && gpsTrack != null) {
                    distance = (gpsTrack.trackPoints.lastOrNull()?.extension?.distance
                        ?: 0.0) / 1000
                }
                entry.kilometers = distance
                binding.kilometers.filters = arrayOf()
                binding.kilometers.setText(
                    String.format(
                        Locale.ENGLISH,
                        "%.1f",
                        distance
                    )
                )
                binding.kilometers.filters = arrayOf<InputFilter>(InputFilterMinMax(0, 999))
                val movingDuration = try {
                    gpxPyJson.getAsJsonPrimitive("moving_time").asDouble
                } catch (_: ClassCastException) {
                    0.0
                }
                if (movingDuration > 0) {
                    val pace = distance / movingDuration * 3600
                    entry.velocityData.avgVelocity = pace
                    if (!isBookmark) {
                        binding.pace.setText(pace.toString())
                    }
                }
            }
        }
    }

    companion object {

        private fun setTextIfNotAlreadySet(editText: EditText, setValue: String) {
            val textValue = editText.text.toString()
            if ((textValue == "" || textValue == "0" || textValue == "0.0" || textValue == "null") && !(setValue == "" || setValue == "0" || setValue == "0.0" || setValue == "null")) {
                editText.setText(setValue)
            }
        }

        private fun getJsonObjectEntryNotNone(jsonObject: JsonObject): Float {
            return if (jsonObject["power"].toString().lowercase(Locale.ROOT)
                    .contains("none")
            ) 0.0f else jsonObject["power"].asFloat
        }


    }

    override fun getProgressBarForAsyncTask(): ProgressBar? {
        return null
    }

    override fun isStepByStepDownload(): Boolean {
        return true
    }

    override fun doInPostExecute(index: Int, successfulDownloaded: Boolean) {
        listItemsGpsDownloadSuccessful?.set(index, successfulDownloaded)
        mDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
            listItemsGpsDownloadSuccessful?.contains(false) == false
    }

    override fun getDialogContext(): Context {
        return currentContext
    }
}