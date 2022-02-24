import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import java.util.concurrent.TimeUnit


class SummitEntryDataFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var database: AppDatabase? = null
    private lateinit var root: View
    private var summitEntry: Summit? = null
    private var summitsToCompare: List<Summit> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_data, container, false)
        database = context?.let { AppDatabase.getDatabase(it) }
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0L) {
                summitEntry = database?.summitDao()?.getSummit(summitEntryId)
            }
        }
        val localSummit = summitEntry
        if (localSummit != null) {
            val extrema = MainActivity.extremaValuesAllSummits
            if (localSummit.isBookmark) {
                root.findViewById<SmartMaterialSpinner<String>>(R.id.summit_name_to_compare).visibility = View.GONE
            } else {
                summitsToCompare = database?.summitDao()?.getAllSummitWithSameSportType(localSummit.sportType)
                        ?: emptyList()
                prepareCompareAutoComplete(localSummit, extrema)
            }
            setBaseData(localSummit, extrema)
            setThirdPartyData(localSummit, extrema)
        }
        return root
    }

    private fun setBaseData(localSummit: Summit, extrema: ExtremaValuesSummits?, compareEntry: Summit? = null) {
        val textViewDate = root.findViewById<TextView>(R.id.tour_date)
        textViewDate.text = localSummit.getDateAsString()
        val textViewName = root.findViewById<TextView>(R.id.summit_name)
        textViewName.text = localSummit.name
        val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
        imageViewSportType.setImageResource(localSummit.sportType.imageIdBlack)

        setText(root.findViewById(R.id.height_meterText), root.findViewById(R.id.height_meter), getString(R.string.hm), localSummit,
                extrema?.heightMetersMinMax?.first, extrema?.heightMetersMinMax?.second, compareEntry) { entry -> entry.elevationData.elevationGain }
        setText(root.findViewById(R.id.kilometersText), root.findViewById(R.id.kilometers), getString(R.string.km), localSummit,
                extrema?.kilometersMinMax?.first, extrema?.kilometersMinMax?.second, compareEntry) { entry -> entry.kilometers }
        setText(root.findViewById(R.id.top_elevationText), root.findViewById(R.id.top_elevation), getString(R.string.hm), localSummit,
                extrema?.topElevationMinMax?.first, extrema?.topElevationMinMax?.second, compareEntry) { entry -> entry.elevationData.maxElevation }
        setText(root.findViewById(R.id.top_verticalVelocity1MinText), root.findViewById(R.id.top_verticalVelocity1Min), getString(R.string.m), localSummit,
                extrema?.topVerticalVelocity1MinMinMax?.first, extrema?.topVerticalVelocity1MinMinMax?.second, compareEntry) { entry -> entry.elevationData.maxVerticalVelocity1Min }
        setText(root.findViewById(R.id.top_verticalVelocity10MinText), root.findViewById(R.id.top_verticalVelocity10Min), getString(R.string.m), localSummit,
                extrema?.topVerticalVelocity10MinMinMax?.first, extrema?.topVerticalVelocity10MinMinMax?.second, compareEntry) { entry -> entry.elevationData.maxVerticalVelocity10Min }
        setText(root.findViewById(R.id.top_verticalVelocity1hText), root.findViewById(R.id.top_verticalVelocity1h), getString(R.string.m), localSummit,
                extrema?.topVerticalVelocity1hMinMax?.first, extrema?.topVerticalVelocity1hMinMax?.second, compareEntry) { entry -> entry.elevationData.maxVerticalVelocity1h }
        setText(root.findViewById(R.id.top_slopeText), root.findViewById(R.id.top_slope), "%", localSummit,
                extrema?.topSlopeMinMax?.first, extrema?.topSlopeMinMax?.second, compareEntry) { entry -> entry.elevationData.maxSlope }
        setText(root.findViewById(R.id.paceText), root.findViewById(R.id.pace), getString(R.string.kmh), localSummit,
                extrema?.averageSpeedMinMax?.first, extrema?.averageSpeedMinMax?.second, compareEntry) { entry -> entry.velocityData.avgVelocity }
        setText(root.findViewById(R.id.top_speedText), root.findViewById(R.id.top_speed), getString(R.string.kmh), localSummit,
                extrema?.topSpeedMinMax?.first, extrema?.topSpeedMinMax?.second, compareEntry) { entry -> entry.velocityData.maxVelocity }

        setAdditionalSpeedData(localSummit, extrema, compareEntry = compareEntry)
        val expandMoreSpeedData: TextView = root.findViewById(R.id.expand_more_speed_data)
        if (localSummit.velocityData.hasAdditionalData()) {
            expandMoreSpeedData.visibility = View.VISIBLE
            expandMoreSpeedData.setOnClickListener {
                if (expandMoreSpeedData.text == getString(R.string.more_speed)) {
                    expandMoreSpeedData.text = getString(R.string.less_speed)
                    expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                    setAdditionalSpeedData(localSummit, extrema, View.VISIBLE, compareEntry = compareEntry)
                } else {
                    expandMoreSpeedData.text = getString(R.string.more_speed)
                    expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                    root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                    setAdditionalSpeedData(localSummit, extrema, compareEntry = compareEntry)
                }
            }
        } else {
            expandMoreSpeedData.visibility = View.GONE
        }

        setText(root.findViewById(R.id.durationText), root.findViewById(R.id.duration), "h", localSummit,
                extrema?.durationMinMax?.first, extrema?.durationMinMax?.second, compareEntry, toHHms = true) { entry -> entry.duration }
        setText(localSummit.comments, root.findViewById(R.id.comments), root.findViewById(R.id.comments))
        database?.let { setChipsText(R.id.places, localSummit.getPlacesWithConnectedEntryString(requireContext(), it), R.drawable.ic_place_black_24dp) }
        setChipsText(R.id.countries, localSummit.countries, R.drawable.ic_baseline_flag_24)
        setChipsText(R.id.participants, localSummit.participants, R.drawable.ic_baseline_people_24)
    }

    private fun setThirdPartyData(localSummit: Summit, extrema: ExtremaValuesSummits?, compareEntry: Summit? = null) {
        val garminData = localSummit.garminData
        val expandMorePowerData: TextView = root.findViewById(R.id.expand_more_power_data)
        if (garminData != null) {
            val textView = root.findViewById(R.id.link) as TextView
            textView.isClickable = true
            textView.movementMethod = LinkMovementMethod.getInstance()
            val text = "<a href=\"${garminData.url}\">${requireContext().getString(R.string.sensor_data)}</a>"
            textView.text = Html.fromHtml(text, 0)
            root.findViewById<View>(R.id.garminData).visibility = View.VISIBLE
            setText(root.findViewById(R.id.averageHrText), root.findViewById(R.id.averageHr), getString(R.string.bpm), localSummit,
                    extrema?.averageHRMinMax?.first, extrema?.averageHRMinMax?.second, compareEntry, digits = 0, reverse = true) { entry -> entry.garminData?.averageHR }
            setText(root.findViewById(R.id.maxHrText), root.findViewById(R.id.maxHr), getString(R.string.bpm), localSummit,
                    extrema?.maxHRMinMax?.first, extrema?.maxHRMinMax?.second, compareEntry, digits = 0, reverse = true) { entry -> entry.garminData?.maxHR }
            setText(root.findViewById(R.id.caloriesText), root.findViewById(R.id.calories), getString(R.string.kcal), localSummit,
                    extrema?.caloriesMinMax?.first, extrema?.caloriesMinMax?.second, compareEntry, digits = 0) { entry -> entry.garminData?.calories }
            setText(root.findViewById(R.id.maxPowerText), root.findViewById(R.id.maxPower), getString(R.string.watt), localSummit,
                    extrema?.maxPowerMinMax?.first, extrema?.maxPowerMinMax?.second, compareEntry, digits = 0) { entry -> entry.garminData?.power?.maxPower }
            setText(root.findViewById(R.id.averagePowerText), root.findViewById(R.id.averagePower), getString(R.string.watt), localSummit,
                    extrema?.averagePowerMinMax?.first, extrema?.averagePowerMinMax?.second, compareEntry, digits = 0) { entry -> entry.garminData?.power?.avgPower }
            setText(root.findViewById(R.id.normPowerText), root.findViewById(R.id.normPower), getString(R.string.watt), localSummit,
                    extrema?.normPowerMinMax?.first, extrema?.normPowerMinMax?.second, compareEntry, digits = 0) { entry -> entry.garminData?.power?.normPower }

            setAdditionalPowerData(localSummit, extrema, compareEntry = compareEntry)
            if (localSummit.garminData?.power?.oneSec != null && (localSummit.garminData?.power?.oneSec
                            ?: 0) > 0) {
                expandMorePowerData.visibility = View.VISIBLE
                expandMorePowerData.setOnClickListener {
                    if (expandMorePowerData.text == getString(R.string.more_power)) {
                        expandMorePowerData.text = getString(R.string.less_power)
                        expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                        setAdditionalPowerData(localSummit, extrema, View.VISIBLE, compareEntry = compareEntry)
                    } else {
                        expandMorePowerData.text = getString(R.string.more_power)
                        expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                        root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                        setAdditionalPowerData(localSummit, extrema, compareEntry = compareEntry)
                    }
                }
            } else {
                expandMorePowerData.visibility = View.GONE
            }
            setText(root.findViewById(R.id.aerobicTrainingEffectText), root.findViewById(R.id.aerobicTrainingEffect), "", localSummit,
                    extrema?.aerobicTrainingEffectMinMax?.first, extrema?.aerobicTrainingEffectMinMax?.second,
                    compareEntry) { entry -> entry.garminData?.aerobicTrainingEffect }
            setText(root.findViewById(R.id.anaerobicTrainingEffectText), root.findViewById(R.id.anaerobicTrainingEffect), "", localSummit,
                    extrema?.anaerobicTrainingEffectMinMax?.first, extrema?.anaerobicTrainingEffectMinMax?.second,
                    compareEntry) { entry -> entry.garminData?.anaerobicTrainingEffect }
            setText(root.findViewById(R.id.gritText), root.findViewById(R.id.grit), "", localSummit,
                    extrema?.gritMinMax?.first, extrema?.gritMinMax?.second, compareEntry) { entry -> entry.garminData?.grit }
            setText(root.findViewById(R.id.flowText), root.findViewById(R.id.flow), "", localSummit,
                    extrema?.flowMinMax?.first, extrema?.flowMinMax?.second, compareEntry) { entry -> entry.garminData?.flow }
            setText(root.findViewById(R.id.trainingLoadText), root.findViewById(R.id.trainingLoad), "", localSummit,
                    extrema?.trainingsLoadMinMax?.first, extrema?.trainingsLoadMinMax?.second, compareEntry) { entry -> entry.garminData?.trainingLoad }
            setText(root.findViewById(R.id.vo2MaxText), root.findViewById(R.id.vo2Max), "", localSummit,
                    extrema?.vo2maxMinMax?.first, extrema?.vo2maxMinMax?.second, compareEntry) { entry -> entry.garminData?.vo2max }
            setText(root.findViewById(R.id.FTPText), root.findViewById(R.id.FTP), "", localSummit,
                    extrema?.ftpMinMax?.first, extrema?.ftpMinMax?.second, compareEntry) { entry -> entry.garminData?.ftp }
        } else {
            root.findViewById<View>(R.id.garminData).visibility = View.GONE
            expandMorePowerData.visibility = View.GONE
        }
    }

    private fun prepareCompareAutoComplete(localSummit: Summit, extrema: ExtremaValuesSummits?) {
        val summitToCompareAutoComplete: SmartMaterialSpinner<String> = root.findViewById(R.id.summit_name_to_compare)
        val items = getSummitsSuggestions(localSummit)
        summitToCompareAutoComplete.item = items

        summitToCompareAutoComplete.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
                val text = items[position]
                if (text != "") {
                    val compareEntry = summitsToCompare.find { "${it.getDateAsString()} ${it.name}" == text }
                    if (compareEntry != null) {
                        setBaseData(localSummit, extrema, compareEntry)
                        setThirdPartyData(localSummit, extrema, compareEntry)
                    } else {
                        setBaseData(localSummit, extrema)
                        setThirdPartyData(localSummit, extrema)
                    }
                } else {
                    setBaseData(localSummit, extrema)
                    setThirdPartyData(localSummit, extrema)
                }
            }
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                setBaseData(localSummit, extrema)
                setThirdPartyData(localSummit, extrema)
            }
        }
    }

    private fun getSummitsSuggestions(localSummit: Summit): ArrayList<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsWithoutSimilarName = summitsToCompare.filter { it.name != localSummit.name && it.hasGpsTrack() }.sortedByDescending { it.date }
        val summitsWithSimilarName = summitsToCompare.filter { it.name == localSummit.name && it != localSummit }.sortedByDescending { it.date }
        summitsToCompare = summitsWithSimilarName + summitsWithoutSimilarName
        summitsToCompare.forEach {
            suggestions.add("${it.getDateAsString()} ${it.name}")
        }
        return suggestions as ArrayList
    }

    private fun setAdditionalSpeedData(localSummit: Summit, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE, compareEntry: Summit? = null) {
        setText(root.findViewById(R.id.oneKM_top_speedText), root.findViewById(R.id.oneKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.oneKmMinMax?.first, extrema?.oneKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.oneKilometer }
        setText(root.findViewById(R.id.fiveKM_top_speedText), root.findViewById(R.id.fiveKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fiveKmMinMax?.first, extrema?.fiveKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.fiveKilometer }
        setText(root.findViewById(R.id.tenKM_top_speedText), root.findViewById(R.id.tenKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.tenKmMinMax?.first, extrema?.tenKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.tenKilometers }
        setText(root.findViewById(R.id.fifteenKM_top_speedText), root.findViewById(R.id.fifteenKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fifteenKmMinMax?.first, extrema?.fifteenKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.fifteenKilometers }
        setText(root.findViewById(R.id.twentyKM_top_speedText), root.findViewById(R.id.twentyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.twentyKmMinMax?.first, extrema?.twentyKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.twentyKilometers }
        setText(root.findViewById(R.id.thirtyKM_top_speedText), root.findViewById(R.id.thirtyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.thirtyKmMinMax?.first, extrema?.thirtyKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.thirtyKilometers }
        setText(root.findViewById(R.id.fourtyKM_top_speedText), root.findViewById(R.id.fourtyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fortyKmMinMax?.first, extrema?.fortyKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.fortyKilometers }
        setText(root.findViewById(R.id.fiftyKM_top_speedText), root.findViewById(R.id.fiftyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fiftyKmMinMax?.first, extrema?.fiftyKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.fiftyKilometers }
        setText(root.findViewById(R.id.seventyfiveKM_top_speedText), root.findViewById(R.id.seventyfiveKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.seventyFiveKmMinMax?.first, extrema?.seventyFiveKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.seventyFiveKilometers }
        setText(root.findViewById(R.id.hundretKM_top_speedText), root.findViewById(R.id.hundretKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.hundredKmMinMax?.first, extrema?.hundredKmMinMax?.second, compareEntry, visibility = visibility) { entry -> entry.velocityData.hundredKilometers }
    }

    private fun setAdditionalPowerData(summit: Summit, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE, compareEntry: Summit? = null) {
        setText(root.findViewById(R.id.power1secText), root.findViewById(R.id.power1sec), getString(R.string.watt), summit,
                extrema?.power1sMinMax?.first, extrema?.power1sMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneSec }
        setText(root.findViewById(R.id.power2secText), root.findViewById(R.id.power2sec), getString(R.string.watt), summit,
                extrema?.power2sMinMax?.first, extrema?.power2sMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twoSec }
        setText(root.findViewById(R.id.power5secText), root.findViewById(R.id.power5sec), getString(R.string.watt), summit,
                extrema?.power5sMinMax?.first, extrema?.power5sMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.fiveSec }
        setText(root.findViewById(R.id.power10secText), root.findViewById(R.id.power10sec), getString(R.string.watt), summit,
                extrema?.power10sMinMax?.first, extrema?.power10sMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.tenSec }
        setText(root.findViewById(R.id.power20secText), root.findViewById(R.id.power20sec), getString(R.string.watt), summit,
                extrema?.power20sMinMax?.first, extrema?.power20sMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twentySec }
        setText(root.findViewById(R.id.power30secText), root.findViewById(R.id.power30sec), getString(R.string.watt), summit,
                extrema?.power30sMinMax?.first, extrema?.power30sMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.thirtySec }

        setText(root.findViewById(R.id.power1minText), root.findViewById(R.id.power1min), getString(R.string.watt), summit,
                extrema?.power1minMinMax?.first, extrema?.power1minMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneMin }
        setText(root.findViewById(R.id.power2minText), root.findViewById(R.id.power2min), getString(R.string.watt), summit,
                extrema?.power2minMinMax?.first, extrema?.power2minMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twoMin }
        setText(root.findViewById(R.id.power5minText), root.findViewById(R.id.power5min), getString(R.string.watt), summit,
                extrema?.power5minMinMax?.first, extrema?.power5minMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.fiveMin }
        setText(root.findViewById(R.id.power10minText), root.findViewById(R.id.power10min), getString(R.string.watt), summit,
                extrema?.power10minMinMax?.first, extrema?.power10minMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.tenMin }
        setText(root.findViewById(R.id.power20minText), root.findViewById(R.id.power20min), getString(R.string.watt), summit,
                extrema?.power20minMinMax?.first, extrema?.power20minMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twentyMin }
        setText(root.findViewById(R.id.power30minText), root.findViewById(R.id.power30min), getString(R.string.watt), summit,
                extrema?.power30minMinMax?.first, extrema?.power30minMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.thirtyMin }

        setText(root.findViewById(R.id.power1hText), root.findViewById(R.id.power1h), getString(R.string.watt), summit,
                extrema?.power1hMinMax?.first, extrema?.power1hMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneHour }
        setText(root.findViewById(R.id.power2hText), root.findViewById(R.id.power2h), getString(R.string.watt), summit,
                extrema?.power2hMinMax?.first, extrema?.power2hMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneHour }
        setText(root.findViewById(R.id.power5hText), root.findViewById(R.id.power5h), getString(R.string.watt), summit,
                extrema?.power5hMinMax?.first, extrema?.power5hMinMax?.second, compareEntry, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneHour }
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

    private fun setText(descriptionTextView: TextView, valueTextView: TextView, unit: String, summit: Summit, minSummit: Summit? = null, maxSummit: Summit? = null,
                        compareSummit: Summit? = null, reverse: Boolean = false, visibility: Int = View.VISIBLE, toHHms: Boolean = false, digits: Int = 1, f: (Summit) -> Number?) {
        val value = f(summit) ?: (if (f(summit) is Int) 0 else 0.0)
        val valueToCompare = if (compareSummit != null) f(compareSummit) else (if (f(summit) is Int) 0 else 0.0)
        if (value.toInt() == 0) {
            descriptionTextView.visibility = View.GONE
            valueTextView.visibility = View.GONE
        } else {
            descriptionTextView.visibility = visibility
            valueTextView.visibility = visibility
            if (value is Int || digits == 0) {
                valueTextView.text = if (valueToCompare?.toInt() != 0) String.format("%s (%s) %s", value.toInt(), valueToCompare?.toInt(), unit) else String.format("%s %s", value.toInt(), unit)
            } else {
                if (toHHms) {
                    val valueInMs = (value.toLong() * 3600000L)
                    val valueInMsCompareSummit = ((valueToCompare?:0).toLong() * 3600000L)
                    if (valueInMsCompareSummit > 0) {

                    } else {
                        valueTextView.text = String.format("%02dh %02dm", TimeUnit.MILLISECONDS.toHours(valueInMs),
                                TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1))
                    }
                } else {
                    valueTextView.text = if (valueToCompare?.toInt() != 0) String.format(Locale.US, "%.${digits}f (%.${digits}f) %s", value, valueToCompare, unit) else String.format(Locale.US, "%.${digits}f %s", value, unit)
                }
            }
            drawCircleWithIndication(valueTextView, minSummit?.let { f(it)?.toDouble() }
                    ?: 0.0, maxSummit?.let { f(it)?.toDouble() }, value.toDouble(), reverse)
        }
    }

    private fun drawCircleWithIndication(textView: TextView, min: Double?, max: Double?, value: Double, reverse: Boolean) {
        textView.compoundDrawablePadding = 20
        var drawable = R.drawable.filled_circle_white
        if (min != null && max != null) {
            val percent = if (reverse) (max.toDouble() - value) / (max.toDouble() - min.toDouble()) else
                (value - min.toDouble()) / (max.toDouble() - min.toDouble())
            drawable = when (percent) {
                in 0.0..0.2 -> R.drawable.filled_circle_red
                in 0.2..0.4 -> R.drawable.filled_circle_orange
                in 0.4..0.6 -> R.drawable.filled_circle_yellow
                in 0.6..0.8 -> R.drawable.filled_circle_blue
                in 0.8..1.0 -> R.drawable.filled_circle_green
                else -> R.drawable.filled_circle_white
            }
        }
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0)
    }

    private fun setChipsText(id: Int, list: List<String>, imageId: Int) {
        val chipGroup: ChipGroup = root.findViewById(id)
        chipGroup.removeAllViews()
        if (list.isEmpty() || list.first() == "") {
            chipGroup.visibility = View.GONE
        } else {
            for (entry in list) {
                val chip = Chip(requireContext())
                chip.text = entry
                chip.isClickable = false
                chip.chipIcon = ResourcesCompat.getDrawable(resources, imageId, null)
                chipGroup.addView(chip)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        summitEntry?.id?.let { savedInstanceState.putLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    companion object {
        private const val TAG = "SummitEntryDataFragment"

        fun newInstance(summitEntry: Summit): SummitEntryDataFragment {
            val fragment = SummitEntryDataFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}
