import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
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
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.PowerData
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
import java.util.*
import kotlin.math.pow
import kotlin.math.round


class SummitEntryPowerFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var summitEntry: Summit? = null
    private lateinit var root: View
    private lateinit var metrics: DisplayMetrics
    private var database: AppDatabase? = null
    private var timeRangeSpinner: Spinner? = null
    private var selectedTimeRangeSpinner: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_power, container, false)
        database = context?.let { AppDatabase.getDatabase(it) }
        metrics = DisplayMetrics()
        val mainActivity = MainActivity.mainActivity
        mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0L) {
                summitEntry = database?.summitDao()?.getSummit(summitEntryId)
            }
        }
        timeRangeSpinner = root.findViewById(R.id.spinner_time_range)
        //TODO: translate
        val timeRangeAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, listOf("All", "current year", "last 3 month", "last 12 month"))
        timeRangeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeRangeSpinner?.adapter = timeRangeAdapter


        val localSummit = summitEntry
        if (localSummit != null) {
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummit.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummit.sportType.imageIdBlack)
            drawChart(localSummit)

            timeRangeSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                    selectedTimeRangeSpinner = i
                    drawChart(localSummit)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        }
        return root
    }

    private fun drawChart(entry: Summit) {
        val power = entry.garminData?.power
        if (power != null) {

            val lineChart = root.findViewById<LineChart>(R.id.lineChart)
            setXAxis(lineChart)
            lineChart.axisLeft.axisMinimum = 0f
            lineChart.axisRight.axisMinimum = 0f

            val params = lineChart.layoutParams
            params.height = (metrics.heightPixels * 0.7).toInt()
            lineChart.layoutParams = params

            val dataSets: MutableList<ILineDataSet?> = ArrayList()
            val chartEntries = getLineChartEntriesMax(power)
            val dataSet = LineDataSet(chartEntries, getString(R.string.power_profile_label))
            setGraphView(dataSet, false)

            val summits = MainActivity.extremaValuesAllSummits?.entries
            if (summits != null) {
                val filteredSummits = getFilteredSummits(summits)
                val extremaValuesAllSummits = ExtremaValuesSummits(filteredSummits, excludeZeroValueFromMin = true)
                val extremalChartEntries = getLineChartEntriesMax(extremaValuesAllSummits)
                val minimalChartEntries = getLineChartEntriesMin(extremaValuesAllSummits)
                val dataSetMaximalValues = LineDataSet(extremalChartEntries, getString(R.string.power_profile_max_label))
                dataSetMaximalValues.fillFormatter = MyFillFormatter(LineDataSet(minimalChartEntries, getString(R.string.power_profile_min_label)))

                lineChart.renderer = MyLineLegendRenderer(lineChart, lineChart.animator, lineChart.viewPortHandler)

                setGraphView(dataSetMaximalValues)
                dataSets.add(dataSetMaximalValues)
                dataSet.circleColors = chartEntries.mapIndexed { index, chartEntry ->
                    val maxEntry = extremalChartEntries[index]
                    val minEntry = minimalChartEntries[index]
                    if (chartEntry != null && maxEntry != null && minEntry != null) {
                        ColorUtils.blendARGB(Color.GREEN, Color.RED, 1f - ((chartEntry.y - minEntry.y) / (maxEntry.y - minEntry.y)))
                    } else {
                        Color.BLUE
                    }
                }
            }
            dataSets.add(dataSet)

            lineChart?.setTouchEnabled(true)
            lineChart?.marker = PowerMarkerView(root.context, R.layout.marker_power_graph)
            lineChart.data = LineData(dataSets)
        }
    }

    private fun getFilteredSummits(summits: List<Summit>): List<Summit> {
        var filtered = listOf<Summit>()
        if (selectedTimeRangeSpinner != 0) {
            filtered = summits.filter { summit ->
                val diff = Date().time - summit.date.time
                when (selectedTimeRangeSpinner) {
                    1 -> summit.date.year == Date().year
                    2 -> diff < 3 * 30 * 24 * 3600000L
                    3 -> diff < 12 * 30 * 24 * 3600000L
                    else -> true
                }
            }
        }
        return if (filtered.isNotEmpty()) filtered else summits
    }

    private fun getLineChartEntriesMax(extremaValuesSummits: ExtremaValuesSummits): MutableList<Entry?> {
        val lineChartEntries: MutableList<Entry?> = ArrayList()
        lineChartEntries.add(Entry(scaleCbr(1.0), extremaValuesSummits.power1sMinMax?.second?.garminData?.power?.oneSec?.toFloat()
                ?: 0f, extremaValuesSummits.power1sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(2.0), extremaValuesSummits.power2sMinMax?.second?.garminData?.power?.twoSec?.toFloat()
                ?: 0f, extremaValuesSummits.power2sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(5.0), extremaValuesSummits.power5sMinMax?.second?.garminData?.power?.fiveSec?.toFloat()
                ?: 0f, extremaValuesSummits.power5sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(10.0), extremaValuesSummits.power10sMinMax?.second?.garminData?.power?.tenSec?.toFloat()
                ?: 0f, extremaValuesSummits.power10sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(20.0), extremaValuesSummits.power20sMinMax?.second?.garminData?.power?.twentySec?.toFloat()
                ?: 0f, extremaValuesSummits.power20sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(30.0), extremaValuesSummits.power30sMinMax?.second?.garminData?.power?.thirtySec?.toFloat()
                ?: 0f, extremaValuesSummits.power30sMinMax?.second))

        lineChartEntries.add(Entry(scaleCbr(60.0), extremaValuesSummits.power1minMinMax?.second?.garminData?.power?.oneMin?.toFloat()
                ?: 0f, extremaValuesSummits.power1minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(120.0), extremaValuesSummits.power2minMinMax?.second?.garminData?.power?.twoMin?.toFloat()
                ?: 0f, extremaValuesSummits.power2minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(300.0), extremaValuesSummits.power5minMinMax?.second?.garminData?.power?.fiveMin?.toFloat()
                ?: 0f, extremaValuesSummits.power5minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(600.0), extremaValuesSummits.power10minMinMax?.second?.garminData?.power?.tenMin?.toFloat()
                ?: 0f, extremaValuesSummits.power10minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(1200.0), extremaValuesSummits.power20minMinMax?.second?.garminData?.power?.twentyMin?.toFloat()
                ?: 0f, extremaValuesSummits.power20minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(1800.0), extremaValuesSummits.power30minMinMax?.second?.garminData?.power?.thirtyMin?.toFloat()
                ?: 0f, extremaValuesSummits.power30minMinMax?.second))

        lineChartEntries.add(Entry(scaleCbr(3600.0), extremaValuesSummits.power1hMinMax?.second?.garminData?.power?.oneHour?.toFloat()
                ?: 0f, extremaValuesSummits.power1hMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(7200.0), extremaValuesSummits.power2hMinMax?.second?.garminData?.power?.twoHours?.toFloat()
                ?: 0f, extremaValuesSummits.power2hMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(18000.0), extremaValuesSummits.power5hMinMax?.second?.garminData?.power?.fiveHours?.toFloat()
                ?: 0f, extremaValuesSummits.power5hMinMax?.second))
        return lineChartEntries
    }

    private fun getLineChartEntriesMin(extremaValuesSummits: ExtremaValuesSummits): MutableList<Entry?> {
        val lineChartEntries: MutableList<Entry?> = ArrayList()
        lineChartEntries.add(Entry(scaleCbr(1.0), extremaValuesSummits.power1sMinMax?.first?.garminData?.power?.oneSec?.toFloat()
                ?: 0f, extremaValuesSummits.power1sMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(2.0), extremaValuesSummits.power2sMinMax?.first?.garminData?.power?.twoSec?.toFloat()
                ?: 0f, extremaValuesSummits.power2sMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(5.0), extremaValuesSummits.power5sMinMax?.first?.garminData?.power?.fiveSec?.toFloat()
                ?: 0f, extremaValuesSummits.power5sMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(10.0), extremaValuesSummits.power10sMinMax?.first?.garminData?.power?.tenSec?.toFloat()
                ?: 0f, extremaValuesSummits.power10sMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(20.0), extremaValuesSummits.power20sMinMax?.first?.garminData?.power?.twentySec?.toFloat()
                ?: 0f, extremaValuesSummits.power20sMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(30.0), extremaValuesSummits.power30sMinMax?.first?.garminData?.power?.thirtySec?.toFloat()
                ?: 0f, extremaValuesSummits.power30sMinMax?.first))

        lineChartEntries.add(Entry(scaleCbr(60.0), extremaValuesSummits.power1minMinMax?.first?.garminData?.power?.oneMin?.toFloat()
                ?: 0f, extremaValuesSummits.power1minMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(120.0), extremaValuesSummits.power2minMinMax?.first?.garminData?.power?.twoMin?.toFloat()
                ?: 0f, extremaValuesSummits.power2minMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(300.0), extremaValuesSummits.power5minMinMax?.first?.garminData?.power?.fiveMin?.toFloat()
                ?: 0f, extremaValuesSummits.power5minMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(600.0), extremaValuesSummits.power10minMinMax?.first?.garminData?.power?.tenMin?.toFloat()
                ?: 0f, extremaValuesSummits.power10minMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(1200.0), extremaValuesSummits.power20minMinMax?.first?.garminData?.power?.twentyMin?.toFloat()
                ?: 0f, extremaValuesSummits.power20minMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(1800.0), extremaValuesSummits.power30minMinMax?.first?.garminData?.power?.thirtyMin?.toFloat()
                ?: 0f, extremaValuesSummits.power30minMinMax?.first))

        lineChartEntries.add(Entry(scaleCbr(3600.0), extremaValuesSummits.power1hMinMax?.first?.garminData?.power?.oneHour?.toFloat()
                ?: 0f, extremaValuesSummits.power1hMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(7200.0), extremaValuesSummits.power2hMinMax?.first?.garminData?.power?.twoHours?.toFloat()
                ?: 0f, extremaValuesSummits.power2hMinMax?.first))
        lineChartEntries.add(Entry(scaleCbr(18000.0), extremaValuesSummits.power5hMinMax?.first?.garminData?.power?.fiveHours?.toFloat()
                ?: 0f, extremaValuesSummits.power5hMinMax?.first))
        return lineChartEntries
    }

    private fun scaleCbr(cbr: Double): Float {
        return Math.log10(cbr).toFloat()
    }

    private fun unScaleCbr(cbr: Double): Float {
        val calcVal = Math.pow(10.0, cbr)
        return calcVal.toFloat()
    }

    private fun getLineChartEntriesMax(power: PowerData): MutableList<Entry?> {
        val lineChartEntries: MutableList<Entry?> = ArrayList()
        if (power.oneSec > 0) lineChartEntries.add(Entry(scaleCbr(1.0), power.oneSec.toFloat()))
        if (power.twoSec > 0) lineChartEntries.add(Entry(scaleCbr(2.0), power.twoSec.toFloat()))
        if (power.fiveSec > 0) lineChartEntries.add(Entry(scaleCbr(5.0), power.fiveSec.toFloat()))
        if (power.tenSec > 0) lineChartEntries.add(Entry(scaleCbr(10.0), power.tenSec.toFloat()))
        if (power.twentySec > 0) lineChartEntries.add(Entry(scaleCbr(20.0), power.twentySec.toFloat()))
        if (power.thirtySec > 0) lineChartEntries.add(Entry(scaleCbr(30.0), power.thirtySec.toFloat()))
        if (power.oneMin > 0) lineChartEntries.add(Entry(scaleCbr(60.0), power.oneMin.toFloat()))
        if (power.twoMin > 0) lineChartEntries.add(Entry(scaleCbr(120.0), power.twoMin.toFloat()))
        if (power.fiveMin > 0) lineChartEntries.add(Entry(scaleCbr(300.0), power.fiveMin.toFloat()))
        if (power.tenMin > 0) lineChartEntries.add(Entry(scaleCbr(600.0), power.tenMin.toFloat()))
        if (power.twentyMin > 0) lineChartEntries.add(Entry(scaleCbr(1200.0), power.twentyMin.toFloat()))
        if (power.thirtyMin > 0) lineChartEntries.add(Entry(scaleCbr(1800.0), power.thirtyMin.toFloat()))
        if (power.oneHour > 0) lineChartEntries.add(Entry(scaleCbr(3600.0), power.oneHour.toFloat()))
        if (power.twoHours > 0) lineChartEntries.add(Entry(scaleCbr(7200.0), power.twoHours.toFloat()))
        if (power.fiveHours > 0) lineChartEntries.add(Entry(scaleCbr(18000.0), power.fiveHours.toFloat()))
        return lineChartEntries
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.ENGLISH, "%s sec", unScaleCbr(round(value.toDouble())).toInt())
            }
        }
        xAxis.axisMinimum = scaleCbr(1.0)
        xAxis.axisMaximum = scaleCbr(100000.0)
        xAxis.setLabelCount(6, true)
    }

    private fun setGraphView(set1: LineDataSet?, filled: Boolean = true) {
        set1?.mode = LineDataSet.Mode.LINEAR
        set1?.circleRadius = 7.0f
        set1?.setDrawValues(false)
        if (filled) {
            set1?.cubicIntensity = 20f
            set1?.lineWidth = 2.5f
            set1?.setCircleColor(Color.BLUE)
            set1?.color = Color.BLUE
            set1?.highLightColor = Color.rgb(244, 117, 117)
            set1?.setDrawFilled(true)
            set1?.fillColor = Color.BLUE
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
        summitEntry?.id?.let { outState.putLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
    }


    companion object {
        private const val TAG = "SummitEntryPowerFragement"

        fun newInstance(summitEntry: Summit): SummitEntryPowerFragment {
            val fragment = SummitEntryPowerFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }


    inner class PowerMarkerView(context: Context?, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry, highlight: Highlight?) {
            val value = 10f.pow(e.x)
            val xText = when (value) {
                in 0f..59f -> "${round(value).toInt()} sec"
                in 60f..3599f -> "${round(value / 60f).toInt()} min"
                else -> "${round(value / 3660f).toInt()} h"
            }
            tvContent?.text = "${e.y.toInt()} W\n($xText)"
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