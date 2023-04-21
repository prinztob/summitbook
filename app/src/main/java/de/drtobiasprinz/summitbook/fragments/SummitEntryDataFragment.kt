package de.drtobiasprinz.summitbook.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryDataBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.models.SummitEntryResultReceiver
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

class SummitEntryDataFragment : Fragment() {

    private lateinit var binding: FragmentSummitEntryDataBinding

    private var pageViewModel: PageViewModel? = null
    private lateinit var summitEntry: Summit
    private lateinit var database: AppDatabase
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
        binding = FragmentSummitEntryDataBinding.inflate(layoutInflater, container, false)

        database = DatabaseModule.provideDatabase(requireContext())
        summitEntry = resultReceiver.getSummit()
        summitToCompare = resultReceiver.getSelectedSummitForComparison()
        resultReceiver.getAllSummits().observe(viewLifecycleOwner) {
            summitsToCompare = SummitEntryDetailsActivity.getSummitsToCompare(it, summitEntry)
            it.data?.let { summits ->
                val extrema = ExtremaValuesSummits(summits)
                if (summitEntry.isBookmark) {
                    binding.summitNameToCompare.visibility = View.GONE
                } else {
                    prepareCompareAutoComplete(extrema)
                }
                setBaseData(extrema)
                setThirdPartyData(extrema)
            }
        }

        return binding.root
    }

    private fun setBaseData(extrema: ExtremaValuesSummits?) {
        binding.tourDate.text = summitEntry.getDateAsString()
        binding.summitName.text = summitEntry.name
        binding.sportTypeImage.setImageResource(summitEntry.sportType.imageIdBlack)

        setText(
            binding.heightMeterText, binding.heightMeter, getString(R.string.hm), summitEntry,
            extrema?.heightMetersMinMax?.first, extrema?.heightMetersMinMax?.second, summitToCompare
        ) { entry -> entry.elevationData.elevationGain }
        setText(
            binding.kilometersText, binding.kilometers, getString(R.string.km), summitEntry,
            extrema?.kilometersMinMax?.first, extrema?.kilometersMinMax?.second, summitToCompare
        ) { entry -> entry.kilometers }
        setText(
            binding.topElevationText, binding.topElevation, getString(R.string.hm), summitEntry,
            extrema?.topElevationMinMax?.first, extrema?.topElevationMinMax?.second, summitToCompare
        ) { entry -> entry.elevationData.maxElevation }
        setText(
            binding.topVerticalVelocity1MinText,
            binding.topVerticalVelocity1Min,
            getString(R.string.m),
            summitEntry,
            extrema?.topVerticalVelocity1MinMinMax?.first,
            extrema?.topVerticalVelocity1MinMinMax?.second,
            summitToCompare,
            factor = 60,
            digits = 0
        ) { entry -> entry.elevationData.maxVerticalVelocity1Min }
        setText(
            binding.topVerticalVelocity10MinText,
            binding.topVerticalVelocity10Min,
            getString(R.string.m),
            summitEntry,
            extrema?.topVerticalVelocity10MinMinMax?.first,
            extrema?.topVerticalVelocity10MinMinMax?.second,
            summitToCompare,
            factor = 600,
            digits = 0
        ) { entry -> entry.elevationData.maxVerticalVelocity10Min }
        setText(
            binding.topVerticalVelocity1hText,
            binding.topVerticalVelocity1h,
            getString(R.string.m),
            summitEntry,
            extrema?.topVerticalVelocity1hMinMax?.first,
            extrema?.topVerticalVelocity1hMinMax?.second,
            summitToCompare,
            factor = 3600,
            digits = 0
        ) { entry -> entry.elevationData.maxVerticalVelocity1h }
        setText(
            binding.topSlopeText, binding.topSlope, "%", summitEntry,
            extrema?.topSlopeMinMax?.first, extrema?.topSlopeMinMax?.second, summitToCompare
        ) { entry -> entry.elevationData.maxSlope }
        setText(
            binding.paceText, binding.pace, getString(R.string.kmh), summitEntry,
            extrema?.averageSpeedMinMax?.first, extrema?.averageSpeedMinMax?.second, summitToCompare
        ) { entry -> entry.velocityData.avgVelocity }
        setText(
            binding.topSpeedText, binding.topSpeed, getString(R.string.kmh), summitEntry,
            extrema?.topSpeedMinMax?.first, extrema?.topSpeedMinMax?.second, summitToCompare
        ) { entry -> entry.velocityData.maxVelocity }

        setAdditionalSpeedData(summitEntry, extrema)
        if (summitEntry.velocityData.hasAdditionalData()) {
            binding.expandMoreSpeedData.visibility = View.VISIBLE
            binding.expandMoreSpeedData.setOnClickListener {
                if (binding.expandMoreSpeedData.text == getString(R.string.more_speed)) {
                    binding.expandMoreSpeedData.text = getString(R.string.less_speed)
                    binding.expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_less_24,
                        0,
                        0,
                        0
                    )
                    setAdditionalSpeedData(summitEntry, extrema, View.VISIBLE)
                } else {
                    binding.expandMoreSpeedData.text = getString(R.string.more_speed)
                    binding.expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_more_24,
                        0,
                        0,
                        0
                    )
                    binding.power1sec.visibility = View.GONE
                    setAdditionalSpeedData(summitEntry, extrema)
                }
            }
        } else {
            binding.expandMoreSpeedData.visibility = View.GONE
        }

        setText(
            binding.durationText,
            binding.duration,
            "h",
            summitEntry,
            extrema?.durationMinMax?.first,
            extrema?.durationMinMax?.second,
            summitToCompare,
            toHHms = true
        ) { entry -> entry.duration }
        setText(summitEntry.comments, binding.comments, binding.comments)
        setChipsText(
            R.id.places,
            summitEntry.getPlacesWithConnectedEntryString(requireContext(), database),
            R.drawable.baseline_place_black_24dp
        )
        setChipsText(R.id.countries, summitEntry.countries, R.drawable.ic_baseline_flag_24)
        setChipsText(R.id.participants, summitEntry.participants, R.drawable.ic_baseline_people_24)
        setChipsText(R.id.equipments, summitEntry.equipments, R.drawable.ic_baseline_handyman_24)
        setChipsTextForSegments(R.id.segments, R.drawable.ic_baseline_route_24)
    }

    private fun setThirdPartyData(extrema: ExtremaValuesSummits?) {
        val garminData = summitEntry.garminData
        if (garminData != null) {
            binding.link.isClickable = true
            binding.link.movementMethod = LinkMovementMethod.getInstance()
            val text =
                "<a href=\"${garminData.url}\">${requireContext().getString(R.string.sensor_data)}</a>"
            binding.link.text = Html.fromHtml(text, 0)
            binding.garminData.visibility = View.VISIBLE
            setText(
                binding.averageHrText,
                binding.averageHr,
                getString(R.string.bpm),
                summitEntry,
                extrema?.averageHRMinMax?.first,
                extrema?.averageHRMinMax?.second,
                summitToCompare,
                digits = 0,
                reverse = true
            ) { entry -> entry.garminData?.averageHR }
            setText(
                binding.maxHrText,
                binding.maxHr,
                getString(R.string.bpm),
                summitEntry,
                extrema?.maxHRMinMax?.first,
                extrema?.maxHRMinMax?.second,
                summitToCompare,
                digits = 0,
                reverse = true
            ) { entry -> entry.garminData?.maxHR }
            setText(
                binding.caloriesText,
                binding.calories,
                getString(R.string.kcal),
                summitEntry,
                extrema?.caloriesMinMax?.first,
                extrema?.caloriesMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.calories }
            setText(
                binding.maxPowerText,
                binding.maxPower,
                getString(R.string.watt),
                summitEntry,
                extrema?.maxPowerMinMax?.first,
                extrema?.maxPowerMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.power?.maxPower }
            setText(
                binding.averagePowerText,
                binding.averagePower,
                getString(R.string.watt),
                summitEntry,
                extrema?.averagePowerMinMax?.first,
                extrema?.averagePowerMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.power?.avgPower }
            setText(
                binding.normPowerText,
                binding.normPower,
                getString(R.string.watt),
                summitEntry,
                extrema?.normPowerMinMax?.first,
                extrema?.normPowerMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.power?.normPower }

            setAdditionalPowerData(summitEntry, extrema)
            if (summitEntry.garminData?.power?.oneSec != null && (summitEntry.garminData?.power?.oneSec
                    ?: 0) > 0
            ) {
                binding.expandMorePowerData.visibility = View.VISIBLE
                binding.expandMorePowerData.setOnClickListener {
                    if (binding.expandMorePowerData.text == getString(R.string.more_power)) {
                        binding.expandMorePowerData.text = getString(R.string.less_power)
                        binding.expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_expand_less_24,
                            0,
                            0,
                            0
                        )
                        setAdditionalPowerData(summitEntry, extrema, View.VISIBLE)
                    } else {
                        binding.expandMorePowerData.text = getString(R.string.more_power)
                        binding.expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_expand_more_24,
                            0,
                            0,
                            0
                        )
                        binding.power1sec.visibility = View.GONE
                        setAdditionalPowerData(summitEntry, extrema)
                    }
                }
            } else {
                binding.expandMorePowerData.visibility = View.GONE
            }
            setText(
                binding.aerobicTrainingEffectText,
                binding.aerobicTrainingEffect,
                "",
                summitEntry,
                extrema?.aerobicTrainingEffectMinMax?.first,
                extrema?.aerobicTrainingEffectMinMax?.second,
                summitToCompare
            ) { entry -> entry.garminData?.aerobicTrainingEffect }
            setText(
                binding.anaerobicTrainingEffectText,
                binding.anaerobicTrainingEffect,
                "",
                summitEntry,
                extrema?.anaerobicTrainingEffectMinMax?.first,
                extrema?.anaerobicTrainingEffectMinMax?.second,
                summitToCompare
            ) { entry -> entry.garminData?.anaerobicTrainingEffect }
            setText(
                binding.gritText, binding.grit, "", summitEntry,
                extrema?.gritMinMax?.first, extrema?.gritMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.grit }
            setText(
                binding.flowText, binding.flow, "", summitEntry,
                extrema?.flowMinMax?.first, extrema?.flowMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.flow }
            setText(
                binding.trainingLoadText,
                binding.trainingLoad,
                "",
                summitEntry,
                extrema?.trainingsLoadMinMax?.first,
                extrema?.trainingsLoadMinMax?.second,
                summitToCompare
            ) { entry -> entry.garminData?.trainingLoad }
            setText(
                binding.vo2MaxText, binding.vo2Max, "", summitEntry,
                extrema?.vo2maxMinMax?.first, extrema?.vo2maxMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.vo2max }
            setText(
                binding.FTPText, binding.FTP, "", summitEntry,
                extrema?.ftpMinMax?.first, extrema?.ftpMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.ftp }
        } else {
            binding.garminData.visibility = View.GONE
            binding.expandMorePowerData.visibility = View.GONE
        }
    }

    private fun prepareCompareAutoComplete(extrema: ExtremaValuesSummits?) {
        binding.summitNameToCompare.visibility = View.VISIBLE
        val items = getSummitsSuggestions(summitEntry)
        binding.summitNameToCompare.item = items
        resultReceiver.getViewPager()
            .registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    @Px positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    val summitToCompareLocal = resultReceiver.getSelectedSummitForComparison()
                    if (summitToCompareLocal != null) {
                        val name =
                            "${summitToCompareLocal.getDateAsString()} ${summitToCompareLocal.name}"
                        val index = items.indexOf(name)
                        binding.summitNameToCompare.setSelection(index)
                        setBaseData(extrema)
                        setThirdPartyData(extrema)
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })

        binding.summitNameToCompare.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (view != null) {
                        val text = items[position]
                        if (text != "") {
                            summitToCompare =
                                summitsToCompare.find { "${it.getDateAsString()} ${it.name}" == text }
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

    private fun getSummitsSuggestions(summit: Summit): List<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsWithoutSimilarName =
            summitsToCompare.filter { it.name != summit.name }
                .sortedByDescending { it.date }
        val summitsWithSimilarName =
            summitsToCompare.filter { it.name == summit.name && it != summit }
                .sortedByDescending { it.date }
        summitsToCompare = summitsWithSimilarName + summitsWithoutSimilarName
        summitsToCompare.forEach {
            suggestions.add("${it.getDateAsString()} ${it.name}")
        }
        return suggestions as ArrayList
    }

    private fun setAdditionalSpeedData(
        localSummit: Summit,
        extrema: ExtremaValuesSummits?,
        visibility: Int = View.GONE
    ) {
        setText(
            binding.oneKMTopSpeedText,
            binding.oneKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.oneKmMinMax?.first,
            extrema?.oneKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.oneKilometer }
        setText(
            binding.fiveKMTopSpeedText,
            binding.fiveKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.fiveKmMinMax?.first,
            extrema?.fiveKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.fiveKilometer }
        setText(
            binding.tenKMTopSpeedText,
            binding.tenKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.tenKmMinMax?.first,
            extrema?.tenKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.tenKilometers }
        setText(
            binding.fifteenKMTopSpeedText,
            binding.fifteenKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.fifteenKmMinMax?.first,
            extrema?.fifteenKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.fifteenKilometers }
        setText(
            binding.twentyKMTopSpeedText,
            binding.twentyKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.twentyKmMinMax?.first,
            extrema?.twentyKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.twentyKilometers }
        setText(
            binding.thirtyKMTopSpeedText,
            binding.thirtyKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.thirtyKmMinMax?.first,
            extrema?.thirtyKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.thirtyKilometers }
        setText(
            binding.fourtyKMTopSpeedText,
            binding.fourtyKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.fortyKmMinMax?.first,
            extrema?.fortyKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.fortyKilometers }
        setText(
            binding.fiftyKMTopSpeedText,
            binding.fiftyKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.fiftyKmMinMax?.first,
            extrema?.fiftyKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.fiftyKilometers }
        setText(
            binding.seventyfiveKMTopSpeedText,
            binding.seventyfiveKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.seventyFiveKmMinMax?.first,
            extrema?.seventyFiveKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.seventyFiveKilometers }
        setText(
            binding.hundretKMTopSpeedText,
            binding.hundretKMTopSpeed,
            getString(R.string.kmh),
            localSummit,
            extrema?.hundredKmMinMax?.first,
            extrema?.hundredKmMinMax?.second,
            summitToCompare,
            visibility = visibility
        ) { entry -> entry.velocityData.hundredKilometers }
    }

    private fun setAdditionalPowerData(
        summit: Summit,
        extrema: ExtremaValuesSummits?,
        visibility: Int = View.GONE
    ) {
        setText(
            binding.power1secText,
            binding.power1sec,
            getString(R.string.watt),
            summit,
            extrema?.power1sMinMax?.first,
            extrema?.power1sMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.oneSec }
        setText(
            binding.power2secText,
            binding.power2sec,
            getString(R.string.watt),
            summit,
            extrema?.power2sMinMax?.first,
            extrema?.power2sMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.twoSec }
        setText(
            binding.power5secText,
            binding.power5sec,
            getString(R.string.watt),
            summit,
            extrema?.power5sMinMax?.first,
            extrema?.power5sMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.fiveSec }
        setText(
            binding.power10secText,
            binding.power10sec,
            getString(R.string.watt),
            summit,
            extrema?.power10sMinMax?.first,
            extrema?.power10sMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.tenSec }
        setText(
            binding.power20secText,
            binding.power20sec,
            getString(R.string.watt),
            summit,
            extrema?.power20sMinMax?.first,
            extrema?.power20sMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.twentySec }
        setText(
            binding.power30secText,
            binding.power30sec,
            getString(R.string.watt),
            summit,
            extrema?.power30sMinMax?.first,
            extrema?.power30sMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.thirtySec }

        setText(
            binding.power1minText,
            binding.power1min,
            getString(R.string.watt),
            summit,
            extrema?.power1minMinMax?.first,
            extrema?.power1minMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.oneMin }
        setText(
            binding.power2minText,
            binding.power2min,
            getString(R.string.watt),
            summit,
            extrema?.power2minMinMax?.first,
            extrema?.power2minMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.twoMin }
        setText(
            binding.power5minText,
            binding.power5min,
            getString(R.string.watt),
            summit,
            extrema?.power5minMinMax?.first,
            extrema?.power5minMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.fiveMin }
        setText(
            binding.power10minText,
            binding.power10min,
            getString(R.string.watt),
            summit,
            extrema?.power10minMinMax?.first,
            extrema?.power10minMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.tenMin }
        setText(
            binding.power20minText,
            binding.power20min,
            getString(R.string.watt),
            summit,
            extrema?.power20minMinMax?.first,
            extrema?.power20minMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.twentyMin }
        setText(
            binding.power30minText,
            binding.power30min,
            getString(R.string.watt),
            summit,
            extrema?.power30minMinMax?.first,
            extrema?.power30minMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.thirtyMin }

        setText(
            binding.power1hText,
            binding.power1h,
            getString(R.string.watt),
            summit,
            extrema?.power1hMinMax?.first,
            extrema?.power1hMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.oneHour }
        setText(
            binding.power2hText,
            binding.power2h,
            getString(R.string.watt),
            summit,
            extrema?.power2hMinMax?.first,
            extrema?.power2hMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.twoHours }
        setText(
            binding.power5hText,
            binding.power5h,
            getString(R.string.watt),
            summit,
            extrema?.power5hMinMax?.first,
            extrema?.power5hMinMax?.second,
            summitToCompare,
            visibility = visibility,
            digits = 0
        ) { entry -> entry.garminData?.power?.fiveHours }
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

    private fun setText(
        descriptionTextView: TextView, valueTextView: TextView, unit: String, summit: Summit,
        minSummit: Summit? = null, maxSummit: Summit? = null, compareSummit: Summit? = null,
        reverse: Boolean = false, visibility: Int = View.VISIBLE, toHHms: Boolean = false,
        digits: Int = 1, factor: Int = 1, f: (Summit) -> Number?
    ) {
        val value = f(summit) ?: (if (f(summit) is Int) 0 else 0.0)
        val valueToCompare =
            if (compareSummit != null) f(compareSummit) else (if (f(summit) is Int) 0 else 0.0)
        if (abs(value.toDouble() * factor) < 0.01) {
            descriptionTextView.visibility = View.GONE
            valueTextView.visibility = View.GONE
        } else {
            descriptionTextView.visibility = visibility
            valueTextView.visibility = visibility
            if (value is Int || digits == 0) {
                valueTextView.text = if (valueToCompare != null && valueToCompare.toInt() != 0) {
                    String.format(
                        "%s (%s) %s",
                        (value.toDouble() * factor).roundToInt(),
                        (valueToCompare.toDouble() * factor).roundToInt(),
                        unit
                    )
                } else {
                    String.format("%s %s", (value.toDouble() * factor).roundToInt(), unit)
                }
            } else {
                if (toHHms) {
                    val valueInMs = (value.toDouble() * 3600000.0).toLong()
                    val valueInMsCompareSummit = ((valueToCompare?.toDouble()
                        ?: 0.0) * 3600000.0).toLong()
                    if (valueInMsCompareSummit > 0) {
                        valueTextView.text = String.format(
                            "%02dh %02dm (%02dh %02dm)", TimeUnit.MILLISECONDS.toHours(valueInMs),
                            TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1),
                            TimeUnit.MILLISECONDS.toHours(valueInMsCompareSummit),
                            TimeUnit.MILLISECONDS.toMinutes(valueInMsCompareSummit) % TimeUnit.HOURS.toMinutes(
                                1
                            )
                        )
                    } else {
                        valueTextView.text = String.format(
                            "%02dh %02dm", TimeUnit.MILLISECONDS.toHours(valueInMs),
                            TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1)
                        )
                    }
                } else {
                    valueTextView.text =
                        if (valueToCompare != null && valueToCompare.toInt() != 0) {
                            String.format(
                                Locale.US,
                                "%.${digits}f (%.${digits}f) %s",
                                value.toDouble() * factor,
                                valueToCompare.toDouble() * factor,
                                unit
                            )
                        } else {
                            String.format(
                                Locale.US,
                                "%.${digits}f %s",
                                value.toDouble() * factor,
                                unit
                            )
                        }
                }
            }
            drawCircleWithIndication(valueTextView, minSummit?.let { f(it)?.toDouble() }
                ?: 0.0, maxSummit?.let { f(it)?.toDouble() }, value.toDouble(), reverse)
        }
    }

    private fun drawCircleWithIndication(
        textView: TextView,
        min: Double?,
        max: Double?,
        value: Double,
        reverse: Boolean
    ) {
        textView.compoundDrawablePadding = 20
        var drawable = R.drawable.filled_circle_white
        if (min != null && max != null) {
            val percent =
                if (reverse) (max.toDouble() - value) / (max.toDouble() - min.toDouble()) else
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

    private fun setChipsTextForSegments(id: Int, imageId: Int) {
        resultReceiver.getAllSegments().observe(viewLifecycleOwner) {
            it.data.let { segments ->
                val segmentsForSummit =
                    segments?.filter { it.segmentEntries.any { it.activityId == summitEntry.activityId } }
                if (segmentsForSummit != null && segmentsForSummit.isNotEmpty()) {
                    val list = mutableListOf<Triple<SegmentEntry, SegmentDetails, String>>()
                    segmentsForSummit.forEach { segment ->
                        segment.segmentEntries.sortBy { it.duration }
                        val relevantEntries =
                            segment.segmentEntries.filter { it.activityId == summitEntry.activityId }
                        relevantEntries.forEach { segmentEntry ->
                            list.add(
                                Triple(
                                    segmentEntry, segment.segmentDetails,
                                    String.format(
                                        getString(R.string.rank_in_activity),
                                        segment.segmentEntries.indexOf(segmentEntry),
                                        segment.segmentDetails.getDisplayName()
                                    )
                                )
                            )
                        }
                    }
                    val chipGroup: ChipGroup = binding.root.findViewById(id)
                    chipGroup.removeAllViews()
                    if (list.isEmpty() || list.first().third == "") {
                        chipGroup.visibility = View.GONE
                    } else {
                        for (entry in list) {
                            val chip = Chip(requireContext())
                            chip.text = entry.third
                            chip.isClickable = false
                            chip.chipIcon = ResourcesCompat.getDrawable(resources, imageId, null)
                            when (requireContext().resources?.configuration?.uiMode?.and(
                                Configuration.UI_MODE_NIGHT_MASK
                            )) {
                                Configuration.UI_MODE_NIGHT_YES -> {
                                    chip.chipIconTint =
                                        ContextCompat.getColorStateList(
                                            requireContext(),
                                            R.color.white
                                        )
                                }
                                Configuration.UI_MODE_NIGHT_NO -> {
                                    chip.chipIconTint =
                                        ContextCompat.getColorStateList(
                                            requireContext(),
                                            R.color.black
                                        )
                                }
                                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                                    chip.chipIconTint =
                                        ContextCompat.getColorStateList(
                                            requireContext(),
                                            R.color.black
                                        )
                                }
                            }

                            chipGroup.addView(chip)
                        }
                    }
                }
            }
        }
    }

    private fun setChipsText(id: Int, list: List<String>, imageId: Int) {
        val chipGroup: ChipGroup = binding.root.findViewById(id)
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
                        chip.chipIconTint =
                            ContextCompat.getColorStateList(requireContext(), R.color.white)
                    }
                    Configuration.UI_MODE_NIGHT_NO -> {
                        chip.chipIconTint =
                            ContextCompat.getColorStateList(requireContext(), R.color.black)
                    }
                    Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                        chip.chipIconTint =
                            ContextCompat.getColorStateList(requireContext(), R.color.black)
                    }
                }

                chipGroup.addView(chip)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
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
