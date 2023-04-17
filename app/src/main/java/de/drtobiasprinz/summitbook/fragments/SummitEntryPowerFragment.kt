package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.Px
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryPowerBinding
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.SummitEntryResultReceiver
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
import java.util.*
import kotlin.math.*

class SummitEntryPowerFragment : Fragment() {

    private lateinit var binding: FragmentSummitEntryPowerBinding
    private var pageViewModel: PageViewModel? = null
    private lateinit var summitEntry: Summit
    private lateinit var metrics: DisplayMetrics
    private var selectedTimeRangeSpinner: Int = 0
    private var summitToCompare: Summit? = null
    private var summitsToCompare: List<Summit> = emptyList()
    private var extremaValuesAllSummits: ExtremaValuesSummits? = null
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
        binding = FragmentSummitEntryPowerBinding.inflate(layoutInflater, container, false)

        summitEntry = resultReceiver.getSummit()
        summitToCompare = resultReceiver.getSelectedSummitForComparison()
        resultReceiver.getAllSummits().observe(viewLifecycleOwner) {
            summitsToCompare = SummitEntryDetailsActivity.getSummitsToCompare(it, summitEntry, onlyWithPowerData = true)
            if (summitEntry.isBookmark) {
                binding.summitNameToCompare.visibility = View.GONE
            } else {
                prepareCompareAutoComplete()
            }
        }
        metrics = DisplayMetrics()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val display = activity?.display
            display?.getRealMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            val display = activity?.windowManager?.defaultDisplay
            @Suppress("DEPRECATION")
            display?.getMetrics(metrics)
        }
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



        binding.summitName.text = summitEntry.name
        binding.sportTypeImage.setImageResource(summitEntry.sportType.imageIdBlack)
        drawChart()

        binding.spinnerTimeRange.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    i: Int,
                    l: Long
                ) {
                    selectedTimeRangeSpinner = i
                    drawChart()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        return binding.root
    }

    private fun prepareCompareAutoComplete() {
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
                        drawChart()
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
                        drawChart()
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    drawChart()
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


    private fun drawChart() {
        binding.lineChart.invalidate()
        val power = summitEntry.garminData?.power
        if (power != null) {
            setXAxis(binding.lineChart)
            binding.lineChart.axisLeft.axisMinimum = 0f
            binding.lineChart.axisRight.axisMinimum = 0f

            val params = binding.lineChart.layoutParams
            params.height = (metrics.heightPixels * 0.65).toInt()
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
            val summits = resultReceiver.getAllSummits()
            summits.observe(viewLifecycleOwner) {
                val filteredSummits = it.data?.let { it1 -> getFilteredSummits(it1) } ?: emptyList()
                extremaValuesAllSummits =
                    ExtremaValuesSummits(filteredSummits, excludeZeroValueFromMin = true)
                val extremalChartEntries = getLineChartEntriesMax(extremaValuesAllSummits)
                val minimalChartEntries = getLineChartEntriesMin(extremaValuesAllSummits)
                val dataSetMaximalValues =
                    LineDataSet(extremalChartEntries, getString(R.string.power_profile_max_label))
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
            }
            dataSets.add(dataSet)

            binding.lineChart.setTouchEnabled(true)
            binding.lineChart.marker =
                PowerMarkerView(requireContext(), R.layout.marker_power_graph)
            binding.lineChart.data = LineData(dataSets)
        }
    }

    private fun getFilteredSummits(summits: List<Summit>): List<Summit> {
        var filtered = listOf<Summit>()
        if (selectedTimeRangeSpinner != 0) {
            filtered = summits.filter { summit ->
                val diff = Date().time - summit.date.time
                if (summit == summitEntry) {
                    false
                } else {
                    when (selectedTimeRangeSpinner) {
                        1 -> summit.date.year == Date().year
                        2 -> diff < 3 * 30 * 24 * 3600000L
                        3 -> diff < 12 * 30 * 24 * 3600000L
                        else -> true
                    }
                }
            }
        }
        return filtered.ifEmpty { summits }
    }

    private fun getLineChartEntriesMin(extremaValuesSummits: ExtremaValuesSummits?): MutableList<Entry?> {
        return TimeIntervalPower.values().map {
            Entry(
                scaleCbr(it.seconds.toDouble()),
                it.minPower(extremaValuesSummits),
                it.getMinSummit(extremaValuesSummits)
            )
        }.toMutableList()
    }

    private fun getLineChartEntriesMax(extremaValuesSummits: ExtremaValuesSummits?): MutableList<Entry?> {
        return TimeIntervalPower.values().map {
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry.id)
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    companion object {
        private const val TAG = "SummitEntryPowerFragment"

        fun newInstance(summitEntry: Summit): SummitEntryPowerFragment {
            val fragment = SummitEntryPowerFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }


    inner class PowerMarkerView(context: Context?, layoutResource: Int) :
        MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry, highlight: Highlight?) {
            val timeIntervalPower =
                TimeIntervalPower.values().minByOrNull { abs(10f.pow(e.x) - it.seconds) }
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
    val minPower: (ExtremaValuesSummits?) -> Float,
    val maxPower: (ExtremaValuesSummits?) -> Float,
    val getMinSummit: (ExtremaValuesSummits?) -> Summit?,
    val getMaxSummit: (ExtremaValuesSummits?) -> Summit?
) {
    OneSec(1, "1 sec",
        { e -> e?.power1sMinMax?.first?.garminData?.power?.oneSec?.toFloat() ?: 0f },
        { e -> e?.power1sMinMax?.second?.garminData?.power?.oneSec?.toFloat() ?: 0f },
        { e -> e?.power1sMinMax?.first }, { e -> e?.power1sMinMax?.second }),
    TwoSec(2, "2 sec",
        { e -> e?.power2sMinMax?.first?.garminData?.power?.twoSec?.toFloat() ?: 0f },
        { e -> e?.power2sMinMax?.second?.garminData?.power?.twoSec?.toFloat() ?: 0f },
        { e -> e?.power2sMinMax?.first }, { e -> e?.power2sMinMax?.second }),
    FiveSec(5, "5 sec",
        { e -> e?.power5sMinMax?.first?.garminData?.power?.fiveSec?.toFloat() ?: 0f },
        { e -> e?.power5sMinMax?.second?.garminData?.power?.fiveSec?.toFloat() ?: 0f },
        { e -> e?.power5sMinMax?.first }, { e -> e?.power5sMinMax?.second }),
    TenSec(10, "10 sec",
        { e -> e?.power10sMinMax?.first?.garminData?.power?.tenSec?.toFloat() ?: 0f },
        { e -> e?.power10sMinMax?.second?.garminData?.power?.tenSec?.toFloat() ?: 0f },
        { e -> e?.power10sMinMax?.first }, { e -> e?.power10sMinMax?.second }),
    TwentySec(20, "20 sec",
        { e -> e?.power20sMinMax?.first?.garminData?.power?.twentySec?.toFloat() ?: 0f },
        { e -> e?.power20sMinMax?.second?.garminData?.power?.twentySec?.toFloat() ?: 0f },
        { e -> e?.power20sMinMax?.first }, { e -> e?.power20sMinMax?.second }),
    ThirtySec(30, "30 sec",
        { e -> e?.power30sMinMax?.first?.garminData?.power?.thirtySec?.toFloat() ?: 0f },
        { e -> e?.power30sMinMax?.second?.garminData?.power?.thirtySec?.toFloat() ?: 0f },
        { e -> e?.power30sMinMax?.first }, { e -> e?.power30sMinMax?.second }),
    OneMin(60, "1 min",
        { e -> e?.power1minMinMax?.first?.garminData?.power?.oneMin?.toFloat() ?: 0f },
        { e -> e?.power1minMinMax?.second?.garminData?.power?.oneMin?.toFloat() ?: 0f },
        { e -> e?.power1minMinMax?.first }, { e -> e?.power1minMinMax?.second }),
    TwoMin(120, "2 min",
        { e -> e?.power2minMinMax?.first?.garminData?.power?.twoMin?.toFloat() ?: 0f },
        { e -> e?.power2minMinMax?.second?.garminData?.power?.twoMin?.toFloat() ?: 0f },
        { e -> e?.power2minMinMax?.first }, { e -> e?.power2minMinMax?.second }),
    FiveMin(300, "5 min",
        { e -> e?.power5minMinMax?.first?.garminData?.power?.fiveMin?.toFloat() ?: 0f },
        { e -> e?.power5minMinMax?.second?.garminData?.power?.fiveMin?.toFloat() ?: 0f },
        { e -> e?.power5minMinMax?.first }, { e -> e?.power5minMinMax?.second }),
    TenMin(600, "10 min",
        { e -> e?.power10minMinMax?.first?.garminData?.power?.tenMin?.toFloat() ?: 0f },
        { e -> e?.power10minMinMax?.second?.garminData?.power?.tenMin?.toFloat() ?: 0f },
        { e -> e?.power10minMinMax?.first }, { e -> e?.power10minMinMax?.second }),
    TwentyMin(1200, "20 min",
        { e -> e?.power20minMinMax?.first?.garminData?.power?.twentyMin?.toFloat() ?: 0f },
        { e -> e?.power20minMinMax?.second?.garminData?.power?.twentyMin?.toFloat() ?: 0f },
        { e -> e?.power20minMinMax?.first }, { e -> e?.power20minMinMax?.second }),
    ThirtyMin(1800, "30 min",
        { e -> e?.power30minMinMax?.first?.garminData?.power?.thirtyMin?.toFloat() ?: 0f },
        { e -> e?.power30minMinMax?.second?.garminData?.power?.thirtyMin?.toFloat() ?: 0f },
        { e -> e?.power30minMinMax?.first }, { e -> e?.power30minMinMax?.second }),
    OneHour(3600, "1 h",
        { e -> e?.power1hMinMax?.first?.garminData?.power?.oneHour?.toFloat() ?: 0f },
        { e -> e?.power1hMinMax?.second?.garminData?.power?.oneHour?.toFloat() ?: 0f },
        { e -> e?.power1hMinMax?.first }, { e -> e?.power1hMinMax?.second }),
    TwoHours(7200, "2 h",
        { e -> e?.power2hMinMax?.first?.garminData?.power?.twoHours?.toFloat() ?: 0f },
        { e -> e?.power2hMinMax?.second?.garminData?.power?.twoHours?.toFloat() ?: 0f },
        { e -> e?.power2hMinMax?.first }, { e -> e?.power2hMinMax?.second }),
    FiveHours(18000, "5 h",
        { e -> e?.power5hMinMax?.first?.garminData?.power?.fiveHours?.toFloat() ?: 0f },
        { e -> e?.power5hMinMax?.second?.garminData?.power?.fiveHours?.toFloat() ?: 0f },
        { e -> e?.power5hMinMax?.first }, { e -> e?.power5hMinMax?.second })
}
