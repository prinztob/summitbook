package de.drtobiasprinz.summitbook.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.SummitEntryResultReceiver
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt


class SummitEntryDataFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private lateinit var summitEntry: Summit
    private lateinit var root: View
    private var database: AppDatabase? = null
    private var summitToCompare: Summit? = null
    private var summitsToCompare: List<Summit> = emptyList()
    private lateinit var resultReceiver: SummitEntryResultReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultReceiver = context as SummitEntryResultReceiver
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_data, container, false)

        database = context?.let { DatabaseModule.provideDatabase(it) }
        val extrema = MainActivity.extremaValuesAllSummits
        summitEntry = resultReceiver.getSummit()
        summitToCompare = resultReceiver.getSelectedSummitForComparison()
        summitsToCompare = resultReceiver.getSummitsForComparison()
        if (summitEntry.isBookmark) {
            root.findViewById<Spinner>(R.id.summit_name_to_compare).visibility = View.GONE
        } else {
            prepareCompareAutoComplete(extrema)
        }
        setBaseData(extrema)
        setThirdPartyData(extrema)
        return root
    }

    private fun setBaseData(extrema: ExtremaValuesSummits?) {
        val textViewDate = root.findViewById<TextView>(R.id.tour_date)
        textViewDate.text = summitEntry.getDateAsString()
        val textViewName = root.findViewById<TextView>(R.id.summit_name)
        textViewName.text = summitEntry.name
        val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
        imageViewSportType.setImageResource(summitEntry.sportType.imageIdBlack)

        setText(root.findViewById(R.id.height_meterText), root.findViewById(R.id.height_meter), getString(R.string.hm), summitEntry,
                extrema?.heightMetersMinMax?.first, extrema?.heightMetersMinMax?.second, summitToCompare) { entry -> entry.elevationData.elevationGain }
        setText(root.findViewById(R.id.kilometersText), root.findViewById(R.id.kilometers), getString(R.string.km), summitEntry,
                extrema?.kilometersMinMax?.first, extrema?.kilometersMinMax?.second, summitToCompare) { entry -> entry.kilometers }
        setText(root.findViewById(R.id.top_elevationText), root.findViewById(R.id.top_elevation), getString(R.string.hm), summitEntry,
                extrema?.topElevationMinMax?.first, extrema?.topElevationMinMax?.second, summitToCompare) { entry -> entry.elevationData.maxElevation }
        setText(root.findViewById(R.id.top_verticalVelocity1MinText), root.findViewById(R.id.top_verticalVelocity1Min), getString(R.string.m), summitEntry,
                extrema?.topVerticalVelocity1MinMinMax?.first, extrema?.topVerticalVelocity1MinMinMax?.second, summitToCompare, factor = 60, digits = 0) { entry -> entry.elevationData.maxVerticalVelocity1Min }
        setText(root.findViewById(R.id.top_verticalVelocity10MinText), root.findViewById(R.id.top_verticalVelocity10Min), getString(R.string.m), summitEntry,
                extrema?.topVerticalVelocity10MinMinMax?.first, extrema?.topVerticalVelocity10MinMinMax?.second, summitToCompare, factor = 600, digits = 0) { entry -> entry.elevationData.maxVerticalVelocity10Min }
        setText(root.findViewById(R.id.top_verticalVelocity1hText), root.findViewById(R.id.top_verticalVelocity1h), getString(R.string.m), summitEntry,
                extrema?.topVerticalVelocity1hMinMax?.first, extrema?.topVerticalVelocity1hMinMax?.second, summitToCompare, factor = 3600, digits = 0) { entry -> entry.elevationData.maxVerticalVelocity1h }
        setText(root.findViewById(R.id.top_slopeText), root.findViewById(R.id.top_slope), "%", summitEntry,
                extrema?.topSlopeMinMax?.first, extrema?.topSlopeMinMax?.second, summitToCompare) { entry -> entry.elevationData.maxSlope }
        setText(root.findViewById(R.id.paceText), root.findViewById(R.id.pace), getString(R.string.kmh), summitEntry,
                extrema?.averageSpeedMinMax?.first, extrema?.averageSpeedMinMax?.second, summitToCompare) { entry -> entry.velocityData.avgVelocity }
        setText(root.findViewById(R.id.top_speedText), root.findViewById(R.id.top_speed), getString(R.string.kmh), summitEntry,
                extrema?.topSpeedMinMax?.first, extrema?.topSpeedMinMax?.second, summitToCompare) { entry -> entry.velocityData.maxVelocity }

        setAdditionalSpeedData(summitEntry, extrema)
        val expandMoreSpeedData: TextView = root.findViewById(R.id.expand_more_speed_data)
        if (summitEntry.velocityData.hasAdditionalData()) {
            expandMoreSpeedData.visibility = View.VISIBLE
            expandMoreSpeedData.setOnClickListener {
                if (expandMoreSpeedData.text == getString(R.string.more_speed)) {
                    expandMoreSpeedData.text = getString(R.string.less_speed)
                    expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                    setAdditionalSpeedData(summitEntry, extrema, View.VISIBLE)
                } else {
                    expandMoreSpeedData.text = getString(R.string.more_speed)
                    expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                    root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                    setAdditionalSpeedData(summitEntry, extrema)
                }
            }
        } else {
            expandMoreSpeedData.visibility = View.GONE
        }

        setText(root.findViewById(R.id.durationText), root.findViewById(R.id.duration), "h", summitEntry,
                extrema?.durationMinMax?.first, extrema?.durationMinMax?.second, summitToCompare, toHHms = true) { entry -> entry.duration }
        setText(summitEntry.comments, root.findViewById(R.id.comments), root.findViewById(R.id.comments))
        database?.let { setChipsText(R.id.places, summitEntry.getPlacesWithConnectedEntryString(requireContext(), it), R.drawable.baseline_place_black_24dp) }
        setChipsText(R.id.countries, summitEntry.countries, R.drawable.ic_baseline_flag_24)
        setChipsText(R.id.participants, summitEntry.participants, R.drawable.ic_baseline_people_24)
        setChipsText(R.id.equipments, summitEntry.equipments, R.drawable.ic_baseline_handyman_24)
    }

    private fun setThirdPartyData(extrema: ExtremaValuesSummits?) {
        val garminData = summitEntry.garminData
        val expandMorePowerData: TextView = root.findViewById(R.id.expand_more_power_data)
        if (garminData != null) {
            val textView = root.findViewById(R.id.link) as TextView
            textView.isClickable = true
            textView.movementMethod = LinkMovementMethod.getInstance()
            val text = "<a href=\"${garminData.url}\">${requireContext().getString(R.string.sensor_data)}</a>"
            textView.text = Html.fromHtml(text, 0)
            root.findViewById<View>(R.id.garminData).visibility = View.VISIBLE
            setText(root.findViewById(R.id.averageHrText), root.findViewById(R.id.averageHr), getString(R.string.bpm), summitEntry,
                    extrema?.averageHRMinMax?.first, extrema?.averageHRMinMax?.second, summitToCompare, digits = 0, reverse = true) { entry -> entry.garminData?.averageHR }
            setText(root.findViewById(R.id.maxHrText), root.findViewById(R.id.maxHr), getString(R.string.bpm), summitEntry,
                    extrema?.maxHRMinMax?.first, extrema?.maxHRMinMax?.second, summitToCompare, digits = 0, reverse = true) { entry -> entry.garminData?.maxHR }
            setText(root.findViewById(R.id.caloriesText), root.findViewById(R.id.calories), getString(R.string.kcal), summitEntry,
                    extrema?.caloriesMinMax?.first, extrema?.caloriesMinMax?.second, summitToCompare, digits = 0) { entry -> entry.garminData?.calories }
            setText(root.findViewById(R.id.maxPowerText), root.findViewById(R.id.maxPower), getString(R.string.watt), summitEntry,
                    extrema?.maxPowerMinMax?.first, extrema?.maxPowerMinMax?.second, summitToCompare, digits = 0) { entry -> entry.garminData?.power?.maxPower }
            setText(root.findViewById(R.id.averagePowerText), root.findViewById(R.id.averagePower), getString(R.string.watt), summitEntry,
                    extrema?.averagePowerMinMax?.first, extrema?.averagePowerMinMax?.second, summitToCompare, digits = 0) { entry -> entry.garminData?.power?.avgPower }
            setText(root.findViewById(R.id.normPowerText), root.findViewById(R.id.normPower), getString(R.string.watt), summitEntry,
                    extrema?.normPowerMinMax?.first, extrema?.normPowerMinMax?.second, summitToCompare, digits = 0) { entry -> entry.garminData?.power?.normPower }

            setAdditionalPowerData(summitEntry, extrema)
            if (summitEntry.garminData?.power?.oneSec != null && (summitEntry.garminData?.power?.oneSec
                            ?: 0) > 0) {
                expandMorePowerData.visibility = View.VISIBLE
                expandMorePowerData.setOnClickListener {
                    if (expandMorePowerData.text == getString(R.string.more_power)) {
                        expandMorePowerData.text = getString(R.string.less_power)
                        expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_less_24, 0, 0, 0)
                        setAdditionalPowerData(summitEntry, extrema, View.VISIBLE)
                    } else {
                        expandMorePowerData.text = getString(R.string.more_power)
                        expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_expand_more_24, 0, 0, 0)
                        root.findViewById<TextView>(R.id.power1sec).visibility = View.GONE
                        setAdditionalPowerData(summitEntry, extrema)
                    }
                }
            } else {
                expandMorePowerData.visibility = View.GONE
            }
            setText(root.findViewById(R.id.aerobicTrainingEffectText), root.findViewById(R.id.aerobicTrainingEffect), "", summitEntry,
                    extrema?.aerobicTrainingEffectMinMax?.first, extrema?.aerobicTrainingEffectMinMax?.second,
                    summitToCompare) { entry -> entry.garminData?.aerobicTrainingEffect }
            setText(root.findViewById(R.id.anaerobicTrainingEffectText), root.findViewById(R.id.anaerobicTrainingEffect), "", summitEntry,
                    extrema?.anaerobicTrainingEffectMinMax?.first, extrema?.anaerobicTrainingEffectMinMax?.second,
                    summitToCompare) { entry -> entry.garminData?.anaerobicTrainingEffect }
            setText(root.findViewById(R.id.gritText), root.findViewById(R.id.grit), "", summitEntry,
                    extrema?.gritMinMax?.first, extrema?.gritMinMax?.second, summitToCompare) { entry -> entry.garminData?.grit }
            setText(root.findViewById(R.id.flowText), root.findViewById(R.id.flow), "", summitEntry,
                    extrema?.flowMinMax?.first, extrema?.flowMinMax?.second, summitToCompare) { entry -> entry.garminData?.flow }
            setText(root.findViewById(R.id.trainingLoadText), root.findViewById(R.id.trainingLoad), "", summitEntry,
                    extrema?.trainingsLoadMinMax?.first, extrema?.trainingsLoadMinMax?.second, summitToCompare) { entry -> entry.garminData?.trainingLoad }
            setText(root.findViewById(R.id.vo2MaxText), root.findViewById(R.id.vo2Max), "", summitEntry,
                    extrema?.vo2maxMinMax?.first, extrema?.vo2maxMinMax?.second, summitToCompare) { entry -> entry.garminData?.vo2max }
            setText(root.findViewById(R.id.FTPText), root.findViewById(R.id.FTP), "", summitEntry,
                    extrema?.ftpMinMax?.first, extrema?.ftpMinMax?.second, summitToCompare) { entry -> entry.garminData?.ftp }
        } else {
            root.findViewById<View>(R.id.garminData).visibility = View.GONE
            expandMorePowerData.visibility = View.GONE
        }
    }

    private fun prepareCompareAutoComplete(extrema: ExtremaValuesSummits?) {
        val summitToCompareSpinner: SmartMaterialSpinner<String> = root.findViewById(R.id.summit_name_to_compare)
        summitToCompareSpinner.visibility = View.VISIBLE
        val items = getSummitsSuggestions(summitEntry)
        summitToCompareSpinner.item = items
        resultReceiver.getViewPager().registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, @Px positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                val summitToCompareLocal = resultReceiver.getSelectedSummitForComparison()
                if (summitToCompareLocal != null) {
                    val name = "${summitToCompareLocal.getDateAsString()} ${summitToCompareLocal.name}"
                    val index = items.indexOf(name)
                    summitToCompareSpinner.setSelection(index)
                    setBaseData(extrema)
                    setThirdPartyData(extrema)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        summitToCompareSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (view != null) {
                    val text = items[position]
                    if (text != "") {
                        summitToCompare = summitsToCompare.find { "${it.getDateAsString()} ${it.name}" == text }
                        resultReceiver.setSelectedSummitForComparison(summitToCompare)
                    }
                    setBaseData(extrema)
                    setThirdPartyData(extrema)
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                setBaseData(extrema)
                setThirdPartyData(extrema)
            }
        }
    }

    private fun getSummitsSuggestions(localSummit: Summit): ArrayList<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsToCompareFromActivity = resultReceiver.getSummitsForComparison()
        val summitsWithoutSimilarName = summitsToCompareFromActivity.filter { it.name != localSummit.name }.sortedByDescending { it.date }
        val summitsWithSimilarName = summitsToCompareFromActivity.filter { it.name == localSummit.name && it != localSummit }.sortedByDescending { it.date }
        summitsToCompare = summitsWithSimilarName + summitsWithoutSimilarName
        summitsToCompare.forEach {
            suggestions.add("${it.getDateAsString()} ${it.name}")
        }
        return suggestions as ArrayList
    }

    private fun setAdditionalSpeedData(localSummit: Summit, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE) {
        setText(root.findViewById(R.id.oneKM_top_speedText), root.findViewById(R.id.oneKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.oneKmMinMax?.first, extrema?.oneKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.oneKilometer }
        setText(root.findViewById(R.id.fiveKM_top_speedText), root.findViewById(R.id.fiveKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fiveKmMinMax?.first, extrema?.fiveKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.fiveKilometer }
        setText(root.findViewById(R.id.tenKM_top_speedText), root.findViewById(R.id.tenKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.tenKmMinMax?.first, extrema?.tenKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.tenKilometers }
        setText(root.findViewById(R.id.fifteenKM_top_speedText), root.findViewById(R.id.fifteenKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fifteenKmMinMax?.first, extrema?.fifteenKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.fifteenKilometers }
        setText(root.findViewById(R.id.twentyKM_top_speedText), root.findViewById(R.id.twentyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.twentyKmMinMax?.first, extrema?.twentyKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.twentyKilometers }
        setText(root.findViewById(R.id.thirtyKM_top_speedText), root.findViewById(R.id.thirtyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.thirtyKmMinMax?.first, extrema?.thirtyKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.thirtyKilometers }
        setText(root.findViewById(R.id.fourtyKM_top_speedText), root.findViewById(R.id.fourtyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fortyKmMinMax?.first, extrema?.fortyKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.fortyKilometers }
        setText(root.findViewById(R.id.fiftyKM_top_speedText), root.findViewById(R.id.fiftyKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.fiftyKmMinMax?.first, extrema?.fiftyKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.fiftyKilometers }
        setText(root.findViewById(R.id.seventyfiveKM_top_speedText), root.findViewById(R.id.seventyfiveKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.seventyFiveKmMinMax?.first, extrema?.seventyFiveKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.seventyFiveKilometers }
        setText(root.findViewById(R.id.hundretKM_top_speedText), root.findViewById(R.id.hundretKM_top_speed), getString(R.string.kmh), localSummit,
                extrema?.hundredKmMinMax?.first, extrema?.hundredKmMinMax?.second, summitToCompare, visibility = visibility) { entry -> entry.velocityData.hundredKilometers }
    }

    private fun setAdditionalPowerData(summit: Summit, extrema: ExtremaValuesSummits?, visibility: Int = View.GONE) {
        setText(root.findViewById(R.id.power1secText), root.findViewById(R.id.power1sec), getString(R.string.watt), summit,
                extrema?.power1sMinMax?.first, extrema?.power1sMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneSec }
        setText(root.findViewById(R.id.power2secText), root.findViewById(R.id.power2sec), getString(R.string.watt), summit,
                extrema?.power2sMinMax?.first, extrema?.power2sMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twoSec }
        setText(root.findViewById(R.id.power5secText), root.findViewById(R.id.power5sec), getString(R.string.watt), summit,
                extrema?.power5sMinMax?.first, extrema?.power5sMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.fiveSec }
        setText(root.findViewById(R.id.power10secText), root.findViewById(R.id.power10sec), getString(R.string.watt), summit,
                extrema?.power10sMinMax?.first, extrema?.power10sMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.tenSec }
        setText(root.findViewById(R.id.power20secText), root.findViewById(R.id.power20sec), getString(R.string.watt), summit,
                extrema?.power20sMinMax?.first, extrema?.power20sMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twentySec }
        setText(root.findViewById(R.id.power30secText), root.findViewById(R.id.power30sec), getString(R.string.watt), summit,
                extrema?.power30sMinMax?.first, extrema?.power30sMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.thirtySec }

        setText(root.findViewById(R.id.power1minText), root.findViewById(R.id.power1min), getString(R.string.watt), summit,
                extrema?.power1minMinMax?.first, extrema?.power1minMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneMin }
        setText(root.findViewById(R.id.power2minText), root.findViewById(R.id.power2min), getString(R.string.watt), summit,
                extrema?.power2minMinMax?.first, extrema?.power2minMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twoMin }
        setText(root.findViewById(R.id.power5minText), root.findViewById(R.id.power5min), getString(R.string.watt), summit,
                extrema?.power5minMinMax?.first, extrema?.power5minMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.fiveMin }
        setText(root.findViewById(R.id.power10minText), root.findViewById(R.id.power10min), getString(R.string.watt), summit,
                extrema?.power10minMinMax?.first, extrema?.power10minMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.tenMin }
        setText(root.findViewById(R.id.power20minText), root.findViewById(R.id.power20min), getString(R.string.watt), summit,
                extrema?.power20minMinMax?.first, extrema?.power20minMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twentyMin }
        setText(root.findViewById(R.id.power30minText), root.findViewById(R.id.power30min), getString(R.string.watt), summit,
                extrema?.power30minMinMax?.first, extrema?.power30minMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.thirtyMin }

        setText(root.findViewById(R.id.power1hText), root.findViewById(R.id.power1h), getString(R.string.watt), summit,
                extrema?.power1hMinMax?.first, extrema?.power1hMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.oneHour }
        setText(root.findViewById(R.id.power2hText), root.findViewById(R.id.power2h), getString(R.string.watt), summit,
                extrema?.power2hMinMax?.first, extrema?.power2hMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.twoHours }
        setText(root.findViewById(R.id.power5hText), root.findViewById(R.id.power5h), getString(R.string.watt), summit,
                extrema?.power5hMinMax?.first, extrema?.power5hMinMax?.second, summitToCompare, visibility = visibility, digits = 0) { entry -> entry.garminData?.power?.fiveHours }
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

    private fun setText(descriptionTextView: TextView, valueTextView: TextView, unit: String, summit: Summit,
                        minSummit: Summit? = null, maxSummit: Summit? = null, compareSummit: Summit? = null,
                        reverse: Boolean = false, visibility: Int = View.VISIBLE, toHHms: Boolean = false,
                        digits: Int = 1, factor: Int = 1, f: (Summit) -> Number?) {
        val value = f(summit) ?: (if (f(summit) is Int) 0 else 0.0)
        val valueToCompare = if (compareSummit != null) f(compareSummit) else (if (f(summit) is Int) 0 else 0.0)
        if (abs(value.toDouble() * factor) < 0.01) {
            descriptionTextView.visibility = View.GONE
            valueTextView.visibility = View.GONE
        } else {
            descriptionTextView.visibility = visibility
            valueTextView.visibility = visibility
            if (value is Int || digits == 0) {
                valueTextView.text = if (valueToCompare != null && valueToCompare.toInt() != 0) {
                    String.format("%s (%s) %s", (value.toDouble() * factor).roundToInt(), (valueToCompare.toDouble() * factor).roundToInt(), unit)
                } else {
                    String.format("%s %s", (value.toDouble() * factor).roundToInt(), unit)
                }
            } else {
                if (toHHms) {
                    val valueInMs = (value.toDouble() * 3600000.0).toLong()
                    val valueInMsCompareSummit = ((valueToCompare?.toDouble()
                            ?: 0.0) * 3600000.0).toLong()
                    if (valueInMsCompareSummit > 0) {
                        valueTextView.text = String.format("%02dh %02dm (%02dh %02dm)", TimeUnit.MILLISECONDS.toHours(valueInMs),
                                TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1),
                                TimeUnit.MILLISECONDS.toHours(valueInMsCompareSummit),
                                TimeUnit.MILLISECONDS.toMinutes(valueInMsCompareSummit) % TimeUnit.HOURS.toMinutes(1))
                    } else {
                        valueTextView.text = String.format("%02dh %02dm", TimeUnit.MILLISECONDS.toHours(valueInMs),
                                TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1))
                    }
                } else {
                    valueTextView.text = if (valueToCompare != null && valueToCompare.toInt() != 0) {
                        String.format(Locale.US,"%.${digits}f (%.${digits}f) %s", value.toDouble() * factor, valueToCompare.toDouble() * factor, unit)
                    } else {
                        String.format(Locale.US, "%.${digits}f %s", value.toDouble() * factor, unit)
                    }
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
                when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> {
                        chip.chipIconTint = ContextCompat.getColorStateList(requireContext(), R.color.white)
                    }
                    Configuration.UI_MODE_NIGHT_NO -> {
                        chip.chipIconTint = ContextCompat.getColorStateList(requireContext(), R.color.black)
                    }
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                        chip.chipIconTint = ContextCompat.getColorStateList(requireContext(), R.color.black)
                    }
                }

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
        savedInstanceState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry.id)
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
