package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryPowerBinding
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.SummitEntryThirdPartyFragment.Companion.drawCircleWithIndication
import de.drtobiasprinz.summitbook.models.TextFieldPower
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@AndroidEntryPoint
class SummitEntryPowerFragment : Fragment() {

    private lateinit var binding: FragmentSummitEntryPowerBinding
    private var pageViewModel: PageViewModel? = null
    private var selectedTimeRangeSpinner: Int = 0
    private var summitsToCompare: List<Summit> = emptyList()
    private var extremaValuesAllSummits: ExtremaValuesSummits? = null
    private lateinit var numberFormat: NumberFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = (requireActivity() as SummitEntryDetailsActivity).pageViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSummitEntryPowerBinding.inflate(layoutInflater, container, false)
        numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setContent()
    }

    private fun setContent() {
        pageViewModel?.summitToView?.observe(viewLifecycleOwner) {
            it.data.let { summitToView ->
                binding.loadingPanel.visibility = View.GONE
                if (summitToView != null) {
                    pageViewModel?.summitsList?.observe(viewLifecycleOwner) { summitsListData ->
                        summitsToCompare = SummitEntryDetailsActivity.getSummitsToCompare(
                            summitsListData,
                            summitToView,
                            onlyWithPowerData = true
                        )
                        if (summitToView.isBookmark) {
                            binding.summitNameToCompare.visibility = View.GONE
                        } else {
                            pageViewModel?.summitToCompare?.observe(viewLifecycleOwner) { itSummitData ->
                                prepareCompareAutoComplete(summitToView, itSummitData.data)
                            }
                        }
                        summitsListData.data.let { summits ->
                            if (summits != null) {
                                val extrema = ExtremaValuesSummits(summits)
                                pageViewModel?.summitToCompare?.observe(viewLifecycleOwner) { itSummitData ->
                                    itSummitData.data.let { summitToCompare ->

                                        drawChart(summitToView, summitToCompare, summits)
                                        setTimeRangeAdapter(summitToView, summitToCompare, summits)
                                        TextFieldPower.entries.forEach { entry ->
                                            setTextForCurrentSummitAndCompareWithSummit(
                                                entry,
                                                summitToView,
                                                summitToCompare,
                                            )
                                        }
                                    }
                                }
                                TextFieldPower.entries.forEach { entry ->
                                    setCircleBeforeText(entry, summitToView, extrema)
                                }
                            }
                        }

                    }
                    binding.summitName.text = summitToView.name
                    binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdBlack)
                    TextFieldPower.entries.forEach { entry ->
                        setTextOnlyForCurrentSummit(entry, summitToView)
                    }

                }
            }
        }
    }

    private fun setCircleBeforeText(
        textField: TextFieldPower,
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
                textField.reverse,
            )
        }
    }

    private fun setTimeRangeAdapter(
        summitToView: Summit,
        summitToCompare: Summit?,
        summits: List<Summit>
    ) {
        val timeRangeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf(
                getString(R.string.all),
                getString(R.string.current_year),
                getString(R.string.last_3_month),
                getString(R.string.last_12_month)
            )
        )
        timeRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTimeRange.adapter = timeRangeAdapter

        binding.spinnerTimeRange.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    i: Int,
                    l: Long
                ) {
                    selectedTimeRangeSpinner = i
                    drawChart(summitToView, summitToCompare, summits)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
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


    private fun getSummitsSuggestions(localSummit: Summit): List<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsWithoutSimilarName =
            summitsToCompare.filter { it.name != localSummit.name && it.garminData?.power?.hasPowerData() == true }
                .sortedByDescending { it.date }
        val summitsWithSimilarName =
            summitsToCompare.filter { it.name == localSummit.name && it != localSummit }
                .sortedByDescending { it.date }
        summitsToCompare = summitsWithSimilarName + summitsWithoutSimilarName
        summitsToCompare.forEach {
            suggestions.add("${it.getDateAsString()} ${it.name}")
        }
        return suggestions as ArrayList
    }


    private fun drawChart(summitToView: Summit, summitToCompare: Summit?, summits: List<Summit>) {
        binding.lineChart.invalidate()
        val power = summitToView.garminData?.power
        if (power != null) {
            setXAxis(binding.lineChart)
            binding.lineChart.axisLeft.axisMinimum = 0f
            binding.lineChart.axisRight.axisMinimum = 0f
            binding.lineChart.description.isEnabled = false
            when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    binding.lineChart.xAxis.textColor = Color.BLACK
                    binding.lineChart.axisRight.textColor = Color.WHITE
                    binding.lineChart.axisLeft.textColor = Color.WHITE
                    binding.lineChart.legend?.textColor = Color.WHITE
                }

                Configuration.UI_MODE_NIGHT_NO -> {
                    binding.lineChart.xAxis.textColor = Color.BLACK
                    binding.lineChart.axisRight.textColor = Color.BLACK
                    binding.lineChart.axisLeft.textColor = Color.BLACK
                    binding.lineChart.legend?.textColor = Color.BLACK
                }

                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    binding.lineChart.xAxis.textColor = Color.BLACK
                    binding.lineChart.axisRight.textColor = Color.WHITE
                    binding.lineChart.axisLeft.textColor = Color.WHITE
                    binding.lineChart.legend?.textColor = Color.WHITE
                }
            }

            val params = binding.lineChart.layoutParams
            params.height = (Resources.getSystem().displayMetrics.heightPixels * 0.65).toInt()
            binding.lineChart.layoutParams = params

            val dataSets: MutableList<ILineDataSet?> = ArrayList()
            val chartEntries = getLineChartEntriesMax(power)
            val dataSet = LineDataSet(chartEntries, getString(R.string.power_profile_label))
            setGraphView(dataSet, false)
            val powerToCompare = summitToCompare?.garminData?.power
            if (powerToCompare != null) {
                val chartEntriesComparator = getLineChartEntriesMax(powerToCompare)
                val dataSetComparator = LineDataSet(
                    chartEntriesComparator,
                    getString(R.string.power_profile_compare_label)
                )
                setGraphView(dataSetComparator, true, color = Color.GRAY)
                dataSets.add(dataSetComparator)
            }
            val filteredSummits = getFilteredSummits(summitToView, summits)
            extremaValuesAllSummits =
                ExtremaValuesSummits(filteredSummits, excludeZeroValueFromMin = true)
            val extremalChartEntries = getLineChartEntriesMax(extremaValuesAllSummits)
            val minimalChartEntries = getLineChartEntriesMin(extremaValuesAllSummits)
            val maxWatts = (extremalChartEntries + chartEntries).maxOf { it?.y ?: 0f }
            binding.lineChart.axisLeft.axisMaximum = maxWatts
            binding.lineChart.axisRight.axisMaximum = maxWatts

            val dataSetMaximalValues =
                LineDataSet(
                    extremalChartEntries,
                    getString(R.string.power_profile_max_label)
                )
            dataSetMaximalValues.fillFormatter = MyFillFormatter(
                LineDataSet(
                    minimalChartEntries,
                    getString(R.string.power_profile_min_label)
                )
            )
            binding.lineChart.renderer = MyLineLegendRenderer(
                binding.lineChart,
                binding.lineChart.animator,
                binding.lineChart.viewPortHandler
            )

            setGraphView(dataSetMaximalValues)
            dataSets.add(dataSetMaximalValues)
            dataSet.circleColors = chartEntries.mapIndexed { index, chartEntry ->
                val maxEntry = extremalChartEntries[index]
                val minEntry = minimalChartEntries[index]
                if (chartEntry != null && maxEntry != null && minEntry != null) {
                    if (chartEntry.y >= maxEntry.y) {
                        Color.rgb(255, 215, 0)
                    } else if (chartEntry.y < minEntry.y) {
                        Color.RED
                    } else {
                        ColorUtils.blendARGB(
                            Color.GREEN,
                            Color.RED,
                            1f - ((chartEntry.y - minEntry.y) / (maxEntry.y - minEntry.y))
                        )
                    }
                } else {
                    Color.BLUE
                }
            }
            dataSets.add(dataSet)

            binding.lineChart.setTouchEnabled(true)
            binding.lineChart.marker =
                PowerMarkerView(requireContext(), R.layout.marker_power_graph)
            binding.lineChart.data = LineData(dataSets)
        }
    }

    private fun getFilteredSummits(summitToView: Summit, summits: List<Summit>): List<Summit> {
        var filtered = listOf<Summit>()
        if (selectedTimeRangeSpinner != 0) {
            filtered = summits.filter { summit ->
                val diff = Date().time - summit.date.time

                when (selectedTimeRangeSpinner) {
                    1 -> getYear(summit.date) == getYear(Date())
                    2 -> diff < 3 * 30 * 24 * 3600000L
                    3 -> diff < 12 * 30 * 24 * 3600000L
                    else -> true
                }
            }
        }
        return filtered.ifEmpty { summits }.filter { !it.equalsInBaseProperties(summitToView) }
    }

    private fun setTextOnlyForCurrentSummit(
        textField: TextFieldPower,
        summit: Summit,
        visibility: Int = View.VISIBLE
    ) {
        val value =
            textField.getValue(summit) ?: (if (textField.getValue(summit) is Int) 0 else 0.0)
        if (abs(value.toDouble() * textField.factor) < 0.01) {
            textField.descriptionTextView(binding).visibility = View.GONE
            textField.valueTextView(binding).visibility = View.GONE
            textField.valueTextViewRange(binding)?.visibility = View.GONE
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
                textField.valueTextView(binding).text = String.format(
                    resources.configuration.locales[0],
                    "%s %s",
                    numberFormat.format(value.toDouble() * textField.factor),
                    textField.unit
                )
            }
            val rangeValue = textField.getValueRange(summit)
            if (rangeValue != null) {
                textField.valueTextViewRange(binding)?.text = String.format(
                    resources.configuration.locales[0],
                    "- %s %s",
                    numberFormat.format(rangeValue.toDouble() * textField.factor),
                    textField.unit
                )
            } else {
                textField.valueTextViewRange(binding)?.visibility = View.GONE
            }
        }
    }

    private fun setTextForCurrentSummitAndCompareWithSummit(
        textField: TextFieldPower,
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

    private fun getYear(date: Date): Int {
        val calendar: Calendar = GregorianCalendar()
        calendar.time = date
        return calendar[Calendar.YEAR]
    }

    private fun getLineChartEntriesMin(extremaValuesSummits: ExtremaValuesSummits?): MutableList<Entry?> {
        return TimeIntervalPower.entries.map {
            Entry(
                scaleCbr(it.seconds.toDouble()),
                it.minPower(extremaValuesSummits),
                it.getMinSummit(extremaValuesSummits)
            )
        }.toMutableList()
    }

    private fun getLineChartEntriesMax(extremaValuesSummits: ExtremaValuesSummits?): MutableList<Entry?> {
        return TimeIntervalPower.entries.map {
            Entry(
                scaleCbr(it.seconds.toDouble()),
                it.maxPower(extremaValuesSummits),
                it.getMaxSummit(extremaValuesSummits)
            )
        }.toMutableList()
    }

    private fun scaleCbr(cbr: Double): Float {
        return log10(cbr).toFloat()
    }

    private fun unScaleCbr(cbr: Double): Float {
        val calcVal = 10.0.pow(cbr)
        return calcVal.toFloat()
    }

    private fun getLineChartEntriesMax(power: PowerData): MutableList<Entry?> {
        val lineChartEntries: MutableList<Entry?> = ArrayList()
        if (power.oneSec > 0) lineChartEntries.add(Entry(scaleCbr(1.0), power.oneSec.toFloat()))
        if (power.twoSec > 0) lineChartEntries.add(Entry(scaleCbr(2.0), power.twoSec.toFloat()))
        if (power.fiveSec > 0) lineChartEntries.add(Entry(scaleCbr(5.0), power.fiveSec.toFloat()))
        if (power.tenSec > 0) lineChartEntries.add(Entry(scaleCbr(10.0), power.tenSec.toFloat()))
        if (power.twentySec > 0) lineChartEntries.add(
            Entry(
                scaleCbr(20.0),
                power.twentySec.toFloat()
            )
        )
        if (power.thirtySec > 0) lineChartEntries.add(
            Entry(
                scaleCbr(30.0),
                power.thirtySec.toFloat()
            )
        )
        if (power.oneMin > 0) lineChartEntries.add(Entry(scaleCbr(60.0), power.oneMin.toFloat()))
        if (power.twoMin > 0) lineChartEntries.add(Entry(scaleCbr(120.0), power.twoMin.toFloat()))
        if (power.fiveMin > 0) lineChartEntries.add(Entry(scaleCbr(300.0), power.fiveMin.toFloat()))
        if (power.tenMin > 0) lineChartEntries.add(Entry(scaleCbr(600.0), power.tenMin.toFloat()))
        if (power.twentyMin > 0) lineChartEntries.add(
            Entry(
                scaleCbr(1200.0),
                power.twentyMin.toFloat()
            )
        )
        if (power.thirtyMin > 0) lineChartEntries.add(
            Entry(
                scaleCbr(1800.0),
                power.thirtyMin.toFloat()
            )
        )
        if (power.oneHour > 0) lineChartEntries.add(
            Entry(
                scaleCbr(3600.0),
                power.oneHour.toFloat()
            )
        )
        if (power.twoHours > 0) lineChartEntries.add(
            Entry(
                scaleCbr(7200.0),
                power.twoHours.toFloat()
            )
        )
        if (power.threeHours > 0) lineChartEntries.add(
            Entry(
                scaleCbr(10400.0),
                power.threeHours.toFloat()
            )
        )
        if (power.fourHours > 0) lineChartEntries.add(
            Entry(
                scaleCbr(14400.0),
                power.fourHours.toFloat()
            )
        )
        if (power.fiveHours > 0) lineChartEntries.add(
            Entry(
                scaleCbr(18000.0),
                power.fiveHours.toFloat()
            )
        )
        return lineChartEntries
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(
                    requireContext().resources.configuration.locales[0],
                    "%s ${getString(R.string.sec)}",
                    unScaleCbr(round(value.toDouble())).toInt()
                )
            }
        }
        xAxis.axisMinimum = scaleCbr(1.0)
        xAxis.axisMaximum = scaleCbr(100000.0)
        xAxis.setLabelCount(6, true)
    }

    private fun setGraphView(set1: LineDataSet?, filled: Boolean = true, color: Int = Color.BLUE) {
        set1?.mode = LineDataSet.Mode.LINEAR
        set1?.circleRadius = 7.0f
        set1?.setDrawValues(false)
        if (filled) {
            set1?.cubicIntensity = 20f
            set1?.lineWidth = 2.5f
            set1?.setCircleColor(color)
            set1?.color = color
            set1?.highLightColor = Color.rgb(244, 117, 117)
            set1?.setDrawFilled(true)
            set1?.fillColor = color
            set1?.fillAlpha = 50
        } else {
            set1?.lineWidth = 4.8f
            set1?.setCircleColor(Color.BLACK)
            set1?.color = Color.BLACK
            set1?.setDrawFilled(false)
        }
        set1?.setDrawHorizontalHighlightIndicator(true)
    }

    inner class PowerMarkerView(context: Context?, layoutResource: Int) :
        MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry, highlight: Highlight?) {
            val timeIntervalPower =
                TimeIntervalPower.entries.toTypedArray()
                    .minByOrNull { abs(10f.pow(e.x) - it.seconds) }
            var text = "${e.y.roundToInt()} W "
            if (extremaValuesAllSummits != null && timeIntervalPower != null && (timeIntervalPower.maxPower(
                    extremaValuesAllSummits
                ) - e.y) > 1
            ) {
                text += "(${
                    timeIntervalPower.minPower(extremaValuesAllSummits).roundToInt()
                } - ${timeIntervalPower.maxPower(extremaValuesAllSummits).roundToInt()})"
            }
            text += "\n${timeIntervalPower?.asString}"
            tvContent?.text = text
            super.refreshContent(e, highlight)
        }

        private var mOffset: MPPointF? = null
        override fun getOffset(): MPPointF {
            if (mOffset == null) {
                mOffset = MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
            }
            return mOffset!!
        }
    }
}

enum class TimeIntervalPower(
    val seconds: Int,
    val asString: String,
    val value: (Summit) -> Int,
    val minPower: (ExtremaValuesSummits?) -> Float,
    val maxPower: (ExtremaValuesSummits?) -> Float,
    val getMinSummit: (ExtremaValuesSummits?) -> Summit?,
    val getMaxSummit: (ExtremaValuesSummits?) -> Summit?
) {
    OneSec(
        1, "1 sec",
        { s -> s.garminData?.power?.oneSec?:0 },
        { e -> e?.power1sMinMax?.first?.garminData?.power?.oneSec?.toFloat() ?: 0f },
        { e -> e?.power1sMinMax?.second?.garminData?.power?.oneSec?.toFloat() ?: 0f },
        { e -> e?.power1sMinMax?.first }, { e -> e?.power1sMinMax?.second }),
    TwoSec(
        2, "2 sec",
        { s -> s.garminData?.power?.twoSec?:0 },
        { e -> e?.power2sMinMax?.first?.garminData?.power?.twoSec?.toFloat() ?: 0f },
        { e -> e?.power2sMinMax?.second?.garminData?.power?.twoSec?.toFloat() ?: 0f },
        { e -> e?.power2sMinMax?.first }, { e -> e?.power2sMinMax?.second }),
    FiveSec(
        5, "5 sec",
        { s -> s.garminData?.power?.fiveSec?:0 },
        { e -> e?.power5sMinMax?.first?.garminData?.power?.fiveSec?.toFloat() ?: 0f },
        { e -> e?.power5sMinMax?.second?.garminData?.power?.fiveSec?.toFloat() ?: 0f },
        { e -> e?.power5sMinMax?.first }, { e -> e?.power5sMinMax?.second }),
    TenSec(
        10, "10 sec",
        { s -> s.garminData?.power?.tenSec?:0 },
        { e -> e?.power10sMinMax?.first?.garminData?.power?.tenSec?.toFloat() ?: 0f },
        { e -> e?.power10sMinMax?.second?.garminData?.power?.tenSec?.toFloat() ?: 0f },
        { e -> e?.power10sMinMax?.first }, { e -> e?.power10sMinMax?.second }),
    TwentySec(
        20, "20 sec",
        { s -> s.garminData?.power?.twentySec?:0 },
        { e -> e?.power20sMinMax?.first?.garminData?.power?.twentySec?.toFloat() ?: 0f },
        { e -> e?.power20sMinMax?.second?.garminData?.power?.twentySec?.toFloat() ?: 0f },
        { e -> e?.power20sMinMax?.first }, { e -> e?.power20sMinMax?.second }),
    ThirtySec(
        30, "30 sec",
        { s -> s.garminData?.power?.thirtySec?:0 },
        { e -> e?.power30sMinMax?.first?.garminData?.power?.thirtySec?.toFloat() ?: 0f },
        { e -> e?.power30sMinMax?.second?.garminData?.power?.thirtySec?.toFloat() ?: 0f },
        { e -> e?.power30sMinMax?.first }, { e -> e?.power30sMinMax?.second }),
    OneMin(
        60, "1 min",
        { s -> s.garminData?.power?.oneMin?:0 },
        { e -> e?.power1minMinMax?.first?.garminData?.power?.oneMin?.toFloat() ?: 0f },
        { e -> e?.power1minMinMax?.second?.garminData?.power?.oneMin?.toFloat() ?: 0f },
        { e -> e?.power1minMinMax?.first }, { e -> e?.power1minMinMax?.second }),
    TwoMin(
        120, "2 min",
        { s -> s.garminData?.power?.twoMin?:0 },
        { e -> e?.power2minMinMax?.first?.garminData?.power?.twoMin?.toFloat() ?: 0f },
        { e -> e?.power2minMinMax?.second?.garminData?.power?.twoMin?.toFloat() ?: 0f },
        { e -> e?.power2minMinMax?.first }, { e -> e?.power2minMinMax?.second }),
    FiveMin(
        300, "5 min",
        { s -> s.garminData?.power?.fiveMin?:0 },
        { e -> e?.power5minMinMax?.first?.garminData?.power?.fiveMin?.toFloat() ?: 0f },
        { e -> e?.power5minMinMax?.second?.garminData?.power?.fiveMin?.toFloat() ?: 0f },
        { e -> e?.power5minMinMax?.first }, { e -> e?.power5minMinMax?.second }),
    TenMin(
        600, "10 min",
        { s -> s.garminData?.power?.tenMin?:0 },
        { e -> e?.power10minMinMax?.first?.garminData?.power?.tenMin?.toFloat() ?: 0f },
        { e -> e?.power10minMinMax?.second?.garminData?.power?.tenMin?.toFloat() ?: 0f },
        { e -> e?.power10minMinMax?.first }, { e -> e?.power10minMinMax?.second }),
    TwentyMin(
        1200, "20 min",
        { s -> s.garminData?.power?.twentyMin?:0 },
        { e -> e?.power20minMinMax?.first?.garminData?.power?.twentyMin?.toFloat() ?: 0f },
        { e -> e?.power20minMinMax?.second?.garminData?.power?.twentyMin?.toFloat() ?: 0f },
        { e -> e?.power20minMinMax?.first }, { e -> e?.power20minMinMax?.second }),
    ThirtyMin(
        1800, "30 min",
        { s -> s.garminData?.power?.thirtyMin?:0 },
        { e -> e?.power30minMinMax?.first?.garminData?.power?.thirtyMin?.toFloat() ?: 0f },
        { e -> e?.power30minMinMax?.second?.garminData?.power?.thirtyMin?.toFloat() ?: 0f },
        { e -> e?.power30minMinMax?.first }, { e -> e?.power30minMinMax?.second }),
    OneHour(
        3600, "1 h",
        { s -> s.garminData?.power?.oneHour?:0 },
        { e -> e?.power1hMinMax?.first?.garminData?.power?.oneHour?.toFloat() ?: 0f },
        { e -> e?.power1hMinMax?.second?.garminData?.power?.oneHour?.toFloat() ?: 0f },
        { e -> e?.power1hMinMax?.first }, { e -> e?.power1hMinMax?.second }),
    TwoHours(
        7200, "2 h",
        { s -> s.garminData?.power?.twoHours?:0 },
        { e -> e?.power2hMinMax?.first?.garminData?.power?.twoHours?.toFloat() ?: 0f },
        { e -> e?.power2hMinMax?.second?.garminData?.power?.twoHours?.toFloat() ?: 0f },
        { e -> e?.power2hMinMax?.first }, { e -> e?.power2hMinMax?.second }),
    ThreeHours(
        10800, "3 h",
        { s -> s.garminData?.power?.threeHours?:0 },
        { e -> e?.power3hMinMax?.first?.garminData?.power?.threeHours?.toFloat() ?: 0f },
        { e -> e?.power3hMinMax?.second?.garminData?.power?.threeHours?.toFloat() ?: 0f },
        { e -> e?.power3hMinMax?.first }, { e -> e?.power3hMinMax?.second }),
    FourHours(
        14400, "4 h",
        { s -> s.garminData?.power?.fourHours?:0 },
        { e -> e?.power4hMinMax?.first?.garminData?.power?.fourHours?.toFloat() ?: 0f },
        { e -> e?.power4hMinMax?.second?.garminData?.power?.fourHours?.toFloat() ?: 0f },
        { e -> e?.power4hMinMax?.first }, { e -> e?.power4hMinMax?.second }),
    FiveHours(
        18000, "5 h",
        { s -> s.garminData?.power?.fiveHours?:0 },
        { e -> e?.power5hMinMax?.first?.garminData?.power?.fiveHours?.toFloat() ?: 0f },
        { e -> e?.power5hMinMax?.second?.garminData?.power?.fiveHours?.toFloat() ?: 0f },
        { e -> e?.power5hMinMax?.first }, { e -> e?.power5hMinMax?.second })
}
