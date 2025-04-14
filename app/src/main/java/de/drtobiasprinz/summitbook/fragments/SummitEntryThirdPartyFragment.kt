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
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryThirdPartyBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.TextFieldGroupThirdPArty
import de.drtobiasprinz.summitbook.models.TextFieldThirdParty
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@AndroidEntryPoint
class SummitEntryThirdPartyFragment : Fragment() {

    private lateinit var binding: FragmentSummitEntryThirdPartyBinding

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
        binding = FragmentSummitEntryThirdPartyBinding.inflate(layoutInflater, container, false)
        binding.constraintLayout.visibility = View.GONE
        binding.constraintLayoutMorePowerData.visibility = View.GONE
        binding.loadingPanel.visibility = View.VISIBLE

        numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        pageViewModel?.summitToView?.observe(viewLifecycleOwner) {
            it.data.let { summitToView ->
                if (summitToView != null) {
                    binding.summitName.text = summitToView.name
                    if (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                        binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdWhite)
                    } else {
                        binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdBlack)
                    }
                    setThirdPartyData(summitToView)
                    pageViewModel?.summitsList?.observe(viewLifecycleOwner) { summitsListData ->
                        summitsToCompare = SummitEntryDetailsActivity.getSummitsToCompare(
                            summitsListData,
                            summitToView
                        )
                        summitsListData.data.let { summits ->
                            if (summits != null) {
                                pageViewModel?.summitToCompare?.observe(viewLifecycleOwner) { summitToCompare ->
                                    binding.constraintLayout.visibility = View.VISIBLE
                                    binding.constraintLayoutMorePowerData.visibility = View.VISIBLE
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


    private fun setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
        summitToView: Summit,
        summitToCompare: Summit?,
        textFieldGroup: TextFieldGroupThirdPArty = TextFieldGroupThirdPArty.ThirdParty,
        visibility: Int = View.VISIBLE
    ) {
        TextFieldThirdParty.entries.filter { it.group == textFieldGroup }.forEach {
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
        textFieldGroup: TextFieldGroupThirdPArty = TextFieldGroupThirdPArty.ThirdParty
    ) {
        TextFieldThirdParty.entries.filter { it.group == textFieldGroup }.forEach {
            setCircleBeforeText(it, summitToView, extrema)
        }
    }


    private fun setThirdPartyData(
        summitToView: Summit
    ) {
        val garminData = summitToView.garminData
        if (garminData != null) {
            binding.link.isClickable = true
            binding.link.movementMethod = LinkMovementMethod.getInstance()
            val text =
                "<a href=\"${garminData.url}\">${requireContext().getString(R.string.sensor_data)}</a>"
            binding.link.text = Html.fromHtml(text, 0)
            binding.constraintLayout.visibility = View.VISIBLE

            TextFieldThirdParty.entries.filter { it.group == TextFieldGroupThirdPArty.ThirdParty }
                .forEach {
                    setTextOnlyForCurrentSummit(it, summitToView)
                }

            TextFieldThirdParty.entries
                .filter { it.group == TextFieldGroupThirdPArty.ThirdPartyAdditionalData }
                .forEach {
                    setTextOnlyForCurrentSummit(it, summitToView, View.GONE)
                }
            if (summitToView.garminData?.power?.oneSec != null &&
                (summitToView.garminData?.power?.oneSec ?: 0) > 0
            ) {
                binding.expandMorePowerData.visibility = View.VISIBLE
            } else {
                binding.expandMorePowerData.visibility = View.GONE
            }
        } else {
            binding.constraintLayout.visibility = View.GONE
            binding.expandMorePowerData.visibility = View.GONE
        }
    }

    private fun setThirdPartyData(
        summitToView: Summit,
        summitToCompare: Summit? = null,
        extrema: ExtremaValuesSummits? = null
    ) {
        val garminData = summitToView.garminData
        if (garminData != null) {
            if (summitToView.garminData?.power?.oneSec != null &&
                (summitToView.garminData?.power?.oneSec ?: 0) > 0
            ) {
                setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
                    summitToView,
                    summitToCompare,
                    textFieldGroup = TextFieldGroupThirdPArty.ThirdParty
                )
                setCircleBeforeTextForAllTextFields(
                    summitToView,
                    extrema,
                    TextFieldGroupThirdPArty.ThirdParty
                )
                binding.expandMorePowerData.setOnClickListener {
                    if (binding.expandMorePowerData.text == getString(R.string.more_power)) {
                        binding.expandMorePowerData.text = getString(R.string.less_power)
                        binding.expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_expand_less_24,
                            0,
                            0,
                            0
                        )
                        setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
                            summitToView,
                            summitToCompare,
                            textFieldGroup = TextFieldGroupThirdPArty.ThirdPartyAdditionalData
                        )
                        setCircleBeforeTextForAllTextFields(
                            summitToView,
                            extrema,
                            TextFieldGroupThirdPArty.ThirdPartyAdditionalData
                        )
                    } else {
                        binding.expandMorePowerData.text = getString(R.string.more_power)
                        binding.expandMorePowerData.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_baseline_expand_more_24,
                            0,
                            0,
                            0
                        )
                        binding.power1sec.visibility = View.GONE
                        setAllTextFieldsWithCurrentSummitAndCompareWithSummitData(
                            summitToView,
                            summitToCompare,
                            textFieldGroup = TextFieldGroupThirdPArty.ThirdPartyAdditionalData,
                            visibility = View.GONE
                        )
                    }
                }
            } else {
                binding.expandMorePowerData.visibility = View.GONE
            }
        } else {
            binding.constraintLayout.visibility = View.GONE
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
                    if (items[position] == getString(R.string.none)) {
                        pageViewModel?.setSummitToCompareToNull()
                    } else if (view != null && selectedPosition != position) {
                        selectedPosition = position
                        val text = items[position]
                        if (text != "") {
                            val newSummitToCompare = summitsToCompare.find {
                                "${it.getDateAsString()} ${it.name}" == text
                            }
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


    private fun setTextOnlyForCurrentSummit(
        textField: TextFieldThirdParty,
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
                    Locale.getDefault(),
                    "%02d:%02d", TimeUnit.MILLISECONDS.toHours(valueInMs),
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
        textField: TextFieldThirdParty,
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
                        Locale.getDefault(),
                        "%02d:%02d (%02d:%02d)", TimeUnit.MILLISECONDS.toHours(valueInMs),
                        TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toHours(valueInMsCompareSummit),
                        TimeUnit.MILLISECONDS.toMinutes(valueInMsCompareSummit) % TimeUnit.HOURS.toMinutes(
                            1
                        )
                    )
                } else {
                    textField.valueTextView(binding).text = String.format(
                        Locale.getDefault(),
                        "%02d:%02d", TimeUnit.MILLISECONDS.toHours(valueInMs),
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
        textField: TextFieldThirdParty,
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
}
