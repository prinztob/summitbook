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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryDataBinding
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel
import java.text.NumberFormat
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@AndroidEntryPoint
class SummitEntryDataFragment : Fragment() {

    private lateinit var binding: FragmentSummitEntryDataBinding

    private var pageViewModel: PageViewModel? = null
    private var summitsToCompare: List<Summit> = emptyList()
    private lateinit var numberFormat: NumberFormat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = (requireActivity() as SummitEntryDetailsActivity).pageViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSummitEntryDataBinding.inflate(layoutInflater, container, false)
        numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        pageViewModel?.summitToView?.observe(viewLifecycleOwner) {
            it.data.let { summitToView ->
                if (summitToView != null) {
                    pageViewModel?.summitsList?.observe(viewLifecycleOwner) { summitsListData ->
                        summitsToCompare = SummitEntryDetailsActivity.getSummitsToCompare(
                            summitsListData,
                            summitToView
                        )
                        summitsListData.data.let { summits ->
                            if (summits != null) {
                                pageViewModel?.summitToCompare?.observe(viewLifecycleOwner) { summitToCompare ->
                                    val extrema = ExtremaValuesSummits(summits)
                                    if (summitToView.isBookmark) {
                                        binding.summitNameToCompare.visibility = View.GONE
                                    } else {
                                        prepareCompareAutoComplete(
                                            summitToView,
                                            summitToCompare.data
                                        )
                                    }
                                    setBaseData(
                                        summitToView,
                                        summitToCompare.data,
                                        summits,
                                        extrema
                                    )
                                    setThirdPartyData(summitToView, summitToCompare.data, extrema)
                                }
                            }
                        }
                    }
                }
            }
        }
        return binding.root
    }

    private fun setBaseData(
        summitToView: Summit,
        summitToCompare: Summit?,
        summits: List<Summit>,
        extrema: ExtremaValuesSummits?
    ) {
        binding.tourDate.text = summitToView.getDateAsString()
        binding.summitName.text = summitToView.name
        if (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdWhite)
        } else {
            binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdBlack)
        }

        setText(
            binding.heightMeterText, binding.heightMeter, getString(R.string.hm), summitToView,
            extrema?.heightMetersMinMax?.first, extrema?.heightMetersMinMax?.second, summitToCompare
        ) { entry -> entry.elevationData.elevationGain }
        setText(
            binding.kilometersText, binding.kilometers, getString(R.string.km), summitToView,
            extrema?.kilometersMinMax?.first, extrema?.kilometersMinMax?.second, summitToCompare
        ) { entry -> entry.kilometers }
        setText(
            binding.topElevationText, binding.topElevation, getString(R.string.hm), summitToView,
            extrema?.topElevationMinMax?.first, extrema?.topElevationMinMax?.second, summitToCompare
        ) { entry -> entry.elevationData.maxElevation }
        setText(
            binding.topVerticalVelocity1MinText,
            binding.topVerticalVelocity1Min,
            getString(R.string.m),
            summitToView,
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
            summitToView,
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
            summitToView,
            extrema?.topVerticalVelocity1hMinMax?.first,
            extrema?.topVerticalVelocity1hMinMax?.second,
            summitToCompare,
            factor = 3600,
            digits = 0
        ) { entry -> entry.elevationData.maxVerticalVelocity1h }
        setText(
            binding.topSlopeText, binding.topSlope, "%", summitToView,
            extrema?.topSlopeMinMax?.first, extrema?.topSlopeMinMax?.second, summitToCompare
        ) { entry -> entry.elevationData.maxSlope }
        setText(
            binding.paceText, binding.pace, getString(R.string.kmh), summitToView,
            extrema?.averageSpeedMinMax?.first, extrema?.averageSpeedMinMax?.second, summitToCompare
        ) { entry -> entry.velocityData.avgVelocity }
        setText(
            binding.topSpeedText, binding.topSpeed, getString(R.string.kmh), summitToView,
            extrema?.topSpeedMinMax?.first, extrema?.topSpeedMinMax?.second, summitToCompare
        ) { entry -> entry.velocityData.maxVelocity }

        setAdditionalSpeedData(summitToView, summitToCompare, extrema)
        if (summitToView.velocityData.hasAdditionalData()) {
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
                    setAdditionalSpeedData(summitToView, summitToCompare, extrema, View.VISIBLE)
                } else {
                    binding.expandMoreSpeedData.text = getString(R.string.more_speed)
                    binding.expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_more_24,
                        0,
                        0,
                        0
                    )
                    binding.power1sec.visibility = View.GONE
                    setAdditionalSpeedData(summitToView, summitToCompare, extrema)
                }
            }
        } else {
            binding.expandMoreSpeedData.visibility = View.GONE
        }

        setText(
            binding.durationText,
            binding.duration,
            "h",
            summitToView,
            extrema?.durationMinMax?.first,
            extrema?.durationMinMax?.second,
            summitToCompare,
            toHHms = true
        ) { entry -> entry.duration }
        setText(summitToView.comments, binding.comments, binding.comments)
        setChipsText(
            R.id.places,
            summitToView.getPlacesWithConnectedEntryString(requireContext(), summits),
            R.drawable.baseline_place_black_24dp
        )
        setChipsText(R.id.countries, summitToView.countries, R.drawable.ic_baseline_flag_24)
        setChipsText(R.id.participants, summitToView.participants, R.drawable.ic_baseline_people_24)
        setChipsText(R.id.equipments, summitToView.equipments, R.drawable.ic_baseline_handyman_24)
        setChipsTextForSegments(R.id.segments, R.drawable.ic_baseline_route_24, summitToView)
    }

    private fun setThirdPartyData(
        summitToView: Summit,
        summitToCompare: Summit?,
        extrema: ExtremaValuesSummits?
    ) {
        val garminData = summitToView.garminData
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
                summitToView,
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
                summitToView,
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
                summitToView,
                extrema?.caloriesMinMax?.first,
                extrema?.caloriesMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.calories }
            setText(
                binding.maxPowerText,
                binding.maxPower,
                getString(R.string.watt),
                summitToView,
                extrema?.maxPowerMinMax?.first,
                extrema?.maxPowerMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.power?.maxPower }
            setText(
                binding.averagePowerText,
                binding.averagePower,
                getString(R.string.watt),
                summitToView,
                extrema?.averagePowerMinMax?.first,
                extrema?.averagePowerMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.power?.avgPower }
            setText(
                binding.normPowerText,
                binding.normPower,
                getString(R.string.watt),
                summitToView,
                extrema?.normPowerMinMax?.first,
                extrema?.normPowerMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.power?.normPower }

            setAdditionalPowerData(summitToView, summitToCompare, extrema)
            if (summitToView.garminData?.power?.oneSec != null && (summitToView.garminData?.power?.oneSec
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
                        setAdditionalPowerData(summitToView, summitToCompare, extrema, View.VISIBLE)
                    } else {
                        binding.expandMorePowerData.text = getString(R.string.more_power)
                        binding.expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_expand_more_24,
                            0,
                            0,
                            0
                        )
                        binding.power1sec.visibility = View.GONE
                        setAdditionalPowerData(summitToView, summitToCompare, extrema)
                    }
                }
            } else {
                binding.expandMorePowerData.visibility = View.GONE
            }
            setText(
                binding.aerobicTrainingEffectText,
                binding.aerobicTrainingEffect,
                "",
                summitToView,
                extrema?.aerobicTrainingEffectMinMax?.first,
                extrema?.aerobicTrainingEffectMinMax?.second,
                summitToCompare
            ) { entry -> entry.garminData?.aerobicTrainingEffect }
            setText(
                binding.anaerobicTrainingEffectText,
                binding.anaerobicTrainingEffect,
                "",
                summitToView,
                extrema?.anaerobicTrainingEffectMinMax?.first,
                extrema?.anaerobicTrainingEffectMinMax?.second,
                summitToCompare
            ) { entry -> entry.garminData?.anaerobicTrainingEffect }
            setText(
                binding.gritText, binding.grit, "", summitToView,
                extrema?.gritMinMax?.first, extrema?.gritMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.grit }
            setText(
                binding.flowText, binding.flow, "", summitToView,
                extrema?.flowMinMax?.first, extrema?.flowMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.flow }
            setText(
                binding.trainingLoadText,
                binding.trainingLoad,
                "",
                summitToView,
                extrema?.trainingsLoadMinMax?.first,
                extrema?.trainingsLoadMinMax?.second,
                summitToCompare,
                digits = 0
            ) { entry -> entry.garminData?.trainingLoad }
            setText(
                binding.vo2MaxText, binding.vo2Max, "", summitToView,
                extrema?.vo2maxMinMax?.first, extrema?.vo2maxMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.vo2max }
            setText(
                binding.FTPText, binding.FTP, "", summitToView,
                extrema?.ftpMinMax?.first, extrema?.ftpMinMax?.second, summitToCompare
            ) { entry -> entry.garminData?.ftp }
        } else {
            binding.garminData.visibility = View.GONE
            binding.expandMorePowerData.visibility = View.GONE
        }


    }

    private fun prepareCompareAutoComplete(summitToView: Summit, summitToCompare: Summit?) {
        val items = getSummitsSuggestions(summitToView)
        binding.summitNameToCompare.item = items
        var selectedPosition = -1
        if (summitToCompare != null) {
            selectedPosition =
                items.indexOfFirst { "${summitToCompare.getDateAsString()} ${summitToCompare.name}" == it }
            if (selectedPosition > -1) {
                binding.summitNameToCompare.setSelection(selectedPosition)
            }
        }
        binding.summitNameToCompare.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (view != null && selectedPosition != position) {
                        selectedPosition = position
                        val text = items[position]
                        if (text != "") {
                            val newSummitToCompare =
                                summitsToCompare.find { "${it.getDateAsString()} ${it.name}" == text }
                            newSummitToCompare?.id?.let { pageViewModel?.getSummitToCompare(it) }
                        }
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    pageViewModel?.setSummitToCompareToNull()
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
        summitToCompare: Summit?,
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
        summitToCompare: Summit?,
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
                numberFormat.maximumFractionDigits = digits
                valueTextView.text =
                    if (valueToCompare != null && valueToCompare.toInt() != 0) {
                        "${numberFormat.format(value.toDouble() * factor)} " +
                                "(${numberFormat.format(valueToCompare.toDouble() * factor)}) " +
                                unit
                    } else {
                        "${numberFormat.format(value.toDouble() * factor)} $unit"
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

    private fun setChipsTextForSegments(id: Int, imageId: Int, summitEntry: Summit) {
        pageViewModel?.segmentsList?.observe(viewLifecycleOwner) {
            it.data.let { segments ->
                val segmentsForSummit =
                    segments?.filter { summit -> summit.segmentEntries.any { entry -> entry.activityId == summitEntry.activityId } }
                if (segmentsForSummit != null && segmentsForSummit.isNotEmpty()) {
                    val list = mutableListOf<Triple<SegmentEntry, SegmentDetails, String>>()
                    segmentsForSummit.forEach { segment ->
                        segment.segmentEntries.sortBy { entry -> entry.duration }
                        val relevantEntries =
                            segment.segmentEntries.filter { entry -> entry.activityId == summitEntry.activityId }
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
                            val chip = createChip(entry.third, imageId)
//                            chip.setOnClickListener {
//                                val fragment = SegmentEntryDetailsFragment()
//                                fragment.segmentDetailsId = entry.second.segmentDetailsId
//                                fragment.segmentEntryId = entry.first.entryId
//                                childFragmentManager.beginTransaction()
//                                    .replace(R.id.content_frame_pager, fragment, "SegmentEntryDetailsFragment")
//                                    .addToBackStack(null).commit()
//                            }
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
                chipGroup.addView(createChip(entry, imageId))
            }
        }
    }

    private fun createChip(
        entry: String,
        imageId: Int
    ): Chip {
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
        return chip
    }
}
