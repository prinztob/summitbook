package de.drtobiasprinz.summitbook.fragments

import android.content.res.Configuration
import android.os.Bundle
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
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.TextField
import de.drtobiasprinz.summitbook.models.TextFieldGroup
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
        binding.constraintLayout.visibility = View.GONE
        binding.constraintLayoutMoreSpeedData.visibility = View.GONE
        binding.constraintLayoutChips.visibility = View.GONE
        binding.loadingPanel.visibility = View.VISIBLE
        numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        pageViewModel?.summitToView?.observe(viewLifecycleOwner) {
            it.data.let { summitToView ->
                if (summitToView != null) {
                    binding.constraintLayout.visibility = View.VISIBLE
                    binding.constraintLayoutMoreSpeedData.visibility = View.VISIBLE
                    binding.constraintLayoutChips.visibility = View.VISIBLE
                    setBaseData(summitToView)
                    pageViewModel?.summitsList?.observe(viewLifecycleOwner) { summitsListData ->
                        summitsToCompare = SummitEntryDetailsActivity.getSummitsToCompare(
                            summitsListData,
                            summitToView
                        )
                        summitsListData.data.let { summits ->
                            if (summits != null) {
                                pageViewModel?.summitToCompare?.observe(viewLifecycleOwner) { summitToCompare ->
                                    binding.loadingPanel.visibility = View.GONE
                                    val extrema = ExtremaValuesSummits(summits)
                                    if (summitToView.isBookmark) {
                                        binding.summitNameToCompare.visibility = View.GONE
                                    } else {
                                        prepareCompareAutoComplete(
                                            summitToView,
                                            summitToCompare.data
                                        )
                                    }
                                    if (summitToCompare.data != null) {
                                        setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
                                            summitToView,
                                            summitToCompare.data
                                        )
                                    }
                                    setCircleBeforeTextForAllTextFields(summitToView, extrema)
                                    setAdditionalSpeedData(
                                        summitToView,
                                        extrema,
                                        summitToCompare.data
                                    )
                                    setChipsText(
                                        R.id.places,
                                        summitToView.getPlacesWithConnectedEntryString(
                                            requireContext(),
                                            summits
                                        ),
                                        R.drawable.baseline_place_black_24dp
                                    )
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
    ) {
        binding.tourDate.text = summitToView.getDateAsString()
        binding.summitName.text = summitToView.name
        if (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdWhite)
        } else {
            binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdBlack)
        }
        setText(summitToView.comments, binding.comments, binding.comments)

        TextField.values().filter { it.group == TextFieldGroup.Base }.forEach {
            setTextOnlyForCurrentSummit(it, summitToView)
        }

        setChipsText(R.id.countries, summitToView.countries, R.drawable.ic_baseline_flag_24)
        setChipsText(R.id.participants, summitToView.participants, R.drawable.ic_baseline_people_24)
        setChipsText(R.id.equipments, summitToView.equipments, R.drawable.ic_baseline_handyman_24)
        setChipsTextForSegments(R.id.segments, R.drawable.ic_baseline_route_24, summitToView)
    }

    private fun setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
        summitToView: Summit,
        summitToCompare: Summit?,
        textFieldGroup: TextFieldGroup = TextFieldGroup.Base,
        visibility: Int = View.VISIBLE
    ) {
        TextField.values().filter { it.group == textFieldGroup }.forEach {
            setTextForCurrentSummitAndCompareWithSummit(
                it,
                summitToView,
                summitToCompare,
                visibility
            )
        }
    }

    private fun setCircleBeforeTextForAllTextFields(
        summitToView: Summit,
        extrema: ExtremaValuesSummits? = null,
        textFieldGroup: TextFieldGroup = TextFieldGroup.Base
    ) {
        TextField.values().filter { it.group == textFieldGroup }.forEach {
            setCircleBeforeText(it, summitToView, extrema)
        }
    }

    private fun setAdditionalSpeedData(
        summitToView: Summit,
        extrema: ExtremaValuesSummits?,
        summitToCompare: Summit?
    ) {
        TextField.values().filter { it.group == TextFieldGroup.AdditionalSpeedData }
            .forEach {
                setTextOnlyForCurrentSummit(it, summitToView, View.GONE)
            }
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
                    setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
                        summitToView,
                        summitToCompare,
                        textFieldGroup = TextFieldGroup.AdditionalSpeedData
                    )
                    setCircleBeforeTextForAllTextFields(
                        summitToView,
                        extrema,
                        textFieldGroup = TextFieldGroup.AdditionalSpeedData
                    )
                } else {
                    binding.expandMoreSpeedData.text = getString(R.string.more_speed)
                    binding.expandMoreSpeedData.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_baseline_expand_more_24,
                        0,
                        0,
                        0
                    )
                    setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
                        summitToView,
                        summitToCompare,
                        textFieldGroup = TextFieldGroup.AdditionalSpeedData,
                        visibility = View.GONE
                    )
                }
            }
        } else {
            binding.expandMoreSpeedData.visibility = View.GONE
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

    private fun setTextOnlyForCurrentSummit(
        textField: TextField,
        summit: Summit,
        visibility: Int = View.VISIBLE
    ) {
        val value =
            textField.getValue(summit) ?: (if (textField.getValue(summit) is Int) 0 else 0.0)
        if (abs(value.toDouble() * textField.factor) < 0.01) {
            textField.descriptionTextView(binding).visibility = View.GONE
            textField.valueTextView(binding).visibility = View.GONE
        } else {
            textField.descriptionTextView(binding).visibility = visibility
            textField.valueTextView(binding).visibility = visibility
            if (textField.toHHms) {
                val valueInMs = (value.toDouble() * 3600000.0).toLong()
                textField.valueTextView(binding).text = String.format(
                    "%02dh %02dm", TimeUnit.MILLISECONDS.toHours(valueInMs),
                    TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1)
                )
            } else {
                numberFormat.maximumFractionDigits = textField.digits
                textField.valueTextView(binding).text =
                    "${numberFormat.format(value.toDouble() * textField.factor)} ${textField.unit}"

            }
        }
    }

    private fun setTextForCurrentSummitAndCompareWithSummit(
        textField: TextField,
        summit: Summit,
        compareSummit: Summit? = null,
        visibility: Int = View.VISIBLE
    ) {
        val value =
            textField.getValue(summit) ?: (if (textField.getValue(summit) is Int) 0 else 0.0)
        val valueToCompare =
            if (compareSummit != null) textField.getValue(compareSummit) else (if (textField.getValue(
                    summit
                ) is Int
            ) 0 else 0.0)
        if (abs(value.toDouble() * textField.factor) < 0.01) {
            textField.descriptionTextView(binding).visibility = View.GONE
            textField.valueTextView(binding).visibility = View.GONE
        } else {
            textField.descriptionTextView(binding).visibility = visibility
            textField.valueTextView(binding).visibility = visibility
            if (textField.toHHms) {
                val valueInMs = (value.toDouble() * 3600000.0).toLong()
                val valueInMsCompareSummit = ((valueToCompare?.toDouble()
                    ?: 0.0) * 3600000.0).toLong()
                if (valueInMsCompareSummit > 0) {
                    textField.valueTextView(binding).text = String.format(
                        "%02dh %02dm (%02dh %02dm)", TimeUnit.MILLISECONDS.toHours(valueInMs),
                        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toHours(valueInMsCompareSummit),
                        TimeUnit.MILLISECONDS.toMinutes(valueInMsCompareSummit) % TimeUnit.HOURS.toMinutes(
                            1
                        )
                    )
                } else {
                    textField.valueTextView(binding).text = String.format(
                        "%02dh %02dm", TimeUnit.MILLISECONDS.toHours(valueInMs),
                        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1)
                    )
                }
            } else {
                numberFormat.maximumFractionDigits = textField.digits
                textField.valueTextView(binding).text =
                    if (valueToCompare != null && valueToCompare.toInt() != 0) {
                        "${numberFormat.format(value.toDouble() * textField.factor)} " +
                                "(${numberFormat.format(valueToCompare.toDouble() * textField.factor)}) " +
                                textField.unit
                    } else {
                        "${numberFormat.format(value.toDouble() * textField.factor)} ${textField.unit}"
                    }
            }
        }
    }

    private fun setCircleBeforeText(
        textField: TextField,
        summit: Summit,
        extrema: ExtremaValuesSummits?
    ) {
        val minSummit = textField.getMinMaxSummit(extrema)?.first
        val maxSummit = textField.getMinMaxSummit(extrema)?.second
        val value =
            textField.getValue(summit) ?: (if (textField.getValue(summit) is Int) 0 else 0.0)
        if (abs(value.toDouble() * textField.factor) > 0.01) {
            drawCircleWithIndication(
                textField.valueTextView(binding),
                minSummit?.let { textField.getValue(it)?.toDouble() }
                    ?: 0.0,
                maxSummit?.let { textField.getValue(it)?.toDouble() },
                value.toDouble(),
                textField.reverse
            )
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
                segments?.let { segmentList -> summitEntry.updateSegmentInfo(segmentList) }
                val chipGroup: ChipGroup = binding.root.findViewById(id)
                chipGroup.removeAllViews()
                if (summitEntry.segmentInfo.isEmpty()) {
                    chipGroup.visibility = View.GONE
                } else {
                    for (entry in summitEntry.segmentInfo) {
                        val chip = createChip(
                            String.format(
                                getString(R.string.rank_in_activity),
                                entry.third,
                                entry.second.getDisplayName()
                            ), imageId
                        )
                        chip.setOnClickListener {
                            binding.segmentDetails.visibility = View.VISIBLE
                            binding.segmentName.text = String.format(
                                getString(R.string.ranked),
                                entry.third,
                            )
                            binding.durationSegment.text = String.format(
                                resources.configuration.locales[0],
                                "%.1f %s",
                                entry.first.duration,
                                getString(R.string.min)
                            )
                            binding.kilometersSegment.text = String.format(
                                resources.configuration.locales[0],
                                "%.1f %s",
                                entry.first.kilometers,
                                getString(R.string.km)
                            )
                            binding.averageHrSegment.text = String.format(
                                resources.configuration.locales[0],
                                "%s %s",
                                entry.first.averageHeartRate,
                                getString(R.string.bpm)
                            )
                            binding.averagePowerSegment.text = String.format(
                                resources.configuration.locales[0],
                                "%s %s",
                                entry.first.averagePower,
                                getString(R.string.watt)
                            )
                            binding.heightMeterSegment.text = String.format(
                                resources.configuration.locales[0],
                                "%s/%s %s",
                                entry.first.heightMetersUp,
                                entry.first.heightMetersDown,
                                getString(R.string.hm)
                            )
                        }
                        chipGroup.addView(chip)
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
