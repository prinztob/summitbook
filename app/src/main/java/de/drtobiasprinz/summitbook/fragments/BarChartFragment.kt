package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.MPPointF
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.CustomBarChart
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.text.ParseException
import java.time.Month
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream


class BarChartFragment(private val sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment {
    private var summitEntries: ArrayList<SummitEntry>? = null
    private var filteredEntries: ArrayList<SummitEntry>? = null
    private var dataSpinner: Spinner? = null
    private var xAxisSpinner: Spinner? = null
    private var barChartView: View? = null
    private var barChartEntries: MutableList<BarEntry?>? = null
    private var unit: String? = "hm"
    private var label: String? = "Height meters"
    private var barChart: CustomBarChart? = null
    private lateinit var intervallHelper: IntervalHelper
    private var selectedDataSpinner = 0
    private var selectedXAxisSpinner = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        barChartView = inflater.inflate(R.layout.fragment_bar_chart, container, false)
        setHasOptionsMenu(true)
        sortFilterHelper.fragment = this
        fillDateSpinner()
        summitEntries = sortFilterHelper.entries
        intervallHelper = IntervalHelper(summitEntries)
        barChart = barChartView?.findViewById(R.id.barChart) // Fragment
        val barChartCustomRenderer = BarChartCustomRenderer(barChart, barChart?.animator, barChart?.viewPortHandler)
        barChart?.renderer = barChartCustomRenderer
        barChart?.setDrawValueAboveBar(false)
        resizeChart()
        barChartEntries = ArrayList()
        filteredEntries = sortFilterHelper.filteredEntries
        listenOnDataSpinner()
        update(filteredEntries)
        return barChartView
    }

    private fun resizeChart() {
        val metrics = DisplayMetrics()
        MainActivity.mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        barChart?.minimumHeight = (metrics.heightPixels * 0.7).toInt()
    }

    private fun drawLineChart() {
        val dataSets: MutableList<IBarDataSet?> = ArrayList()
        val dataSet = BarDataSet(barChartEntries, label)
        setGraphView(dataSet)
        dataSets.add(dataSet)
        barChart?.data = BarData(dataSets)
        setXAxis()
        val yAxisLeft = barChart?.axisLeft
        setYAxis(yAxisLeft)
        val yAxisRight = barChart?.axisRight
        setYAxis(yAxisRight)
        barChart?.setTouchEnabled(true)
        barChart?.marker = CustomMarkerView(barChartView?.context, R.layout.marker_graph_bar_chart)
        barChart?.invalidate()
    }

    private fun setXAxis() {
        val xAxis = barChart?.xAxis
        xAxis?.position = XAxis.XAxisPosition.TOP
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (selectedXAxisSpinner) {
                    1 -> String.format("%s", ((value + 0.5) * IntervalHelper.kilometersStep).toInt())
                    2 -> String.format("%s", ((value + 0.5) * IntervalHelper.elevationGainStep).toInt())
                    3 -> String.format("%s", ((value + 0.5) * IntervalHelper.topElevationStep).toInt())
                    else -> if (sortFilterHelper.selectedYear == "" || value > 12) String.format("%s", value.toInt()) else String.format("%s", Month.of(value.toInt()))
                }
            }
        }
    }

    private fun setYAxis(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.ENGLISH, "%.0f %s", value, unit)
            }
        }
    }

    override fun update(filteredSummitEntries: ArrayList<SummitEntry>?) {
        filteredEntries = filteredSummitEntries
        selectedDataSpinner()
        drawLineChart()
    }

    private fun setGraphView(dataSet: BarDataSet) {
        dataSet.setDrawValues(false)
        dataSet.highLightColor = Color.RED
        dataSet.colors = SportType.values().map { ContextCompat.getColor(requireContext(), it.color) }
        dataSet.stackLabels = SportType.values().map { getString(it.abbreviationStringId) }.toTypedArray()
    }

    private fun selectedDataSpinner() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        barChart?.axisLeft?.removeAllLimitLines()
        val sortedEntries = filteredEntries
        sortedEntries?.sortWith(compareBy { it.date })
        barChartEntries?.clear()
        try {
            var annualTarget: Float = when (selectedDataSpinner) {
                1 -> {
                    sharedPreferences.getString("annual_target_km", "1200")?.toFloat() ?: 1200f
                }
                2 -> {
                    sharedPreferences.getString("annual_target", "50000")?.toFloat() ?: 50000f
                }
                else -> {
                    sharedPreferences.getString("annual_target_activities", "52")?.toFloat() ?: 52f
                }
            }
            if (sortFilterHelper.selectedYear != "") {
                annualTarget /= 12f
            }
            val line1 = LimitLine(annualTarget)
            barChart?.axisLeft?.addLimitLine(line1)
            barChart?.setSafeZoneColor(ContextCompat.getColor(requireContext(), R.color.red_50), ContextCompat.getColor(requireContext(), R.color.green_50))

            when (selectedXAxisSpinner) {
                1 -> updateBarChartWithKilometersAsXAxis()
                2 -> updateBarChartWithElevationGainAsXAxis()
                3 -> updateBarChartWithTopElevationAsXAxis()
                else -> updateBarChartWithDateAsXAxis()
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChartWithDateAsXAxis() {
        intervallHelper.setSelectedYear(sortFilterHelper.selectedYear)
        intervallHelper.calculate()
        for (i in 0 until intervallHelper.dates.size - 1) {
            val streamSupplier: Supplier<Stream<SummitEntry?>?> = Supplier { getEntriesBetweenDates(filteredEntries, intervallHelper.dates[i], intervallHelper.dates[i + 1]) }
            val xValue = intervallHelper.dateAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries?.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries?.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries?.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChartWithElevationGainAsXAxis() {
        intervallHelper.calculate()
        for (i in 0 until intervallHelper.elevationGains.size - 1) {
            val streamSupplier: Supplier<Stream<SummitEntry?>?> = Supplier { getEntriesBetweenElevationGains(filteredEntries, intervallHelper.elevationGains[i], intervallHelper.elevationGains[i + 1]) }
            val xValue = intervallHelper.elevationGainAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries?.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries?.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries?.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChartWithKilometersAsXAxis() {
        intervallHelper.calculate()
        for (i in 0 until intervallHelper.kilometers.size - 1) {
            val streamSupplier: Supplier<Stream<SummitEntry?>?> = Supplier { getEntriesBetweenKilometers(filteredEntries, intervallHelper.kilometers[i], intervallHelper.kilometers[i + 1]) }
            val xValue = intervallHelper.kilometerAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries?.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries?.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries?.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChartWithTopElevationAsXAxis() {
        intervallHelper.calculate()
        for (i in 0 until intervallHelper.topElevations.size - 1) {
            val streamSupplier: Supplier<Stream<SummitEntry?>?> = Supplier { getEntriesBetweenTopElevation(filteredEntries, intervallHelper.topElevations[i], intervallHelper.topElevations[i + 1]) }
            val xValue = intervallHelper.topElevationAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries?.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries?.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries?.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
        }
    }

    private fun getCountsPerSportType(entriesSupplier: Supplier<Stream<SummitEntry?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        SportType.values().forEach { sportType -> list.add(getCountsFromStream(entriesSupplier.get()?.filter { it?.sportType == sportType })) }
        return list.toFloatArray()
    }

    private fun getKilometerPerSportType(entriesSupplier: Supplier<Stream<SummitEntry?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        SportType.values().forEach { sportType -> list.add(getKilometersFromStream(entriesSupplier.get()?.filter { it?.sportType == sportType })) }
        return list.toFloatArray()
    }

    private fun getElevationGainsPerSportType(entriesSupplier: Supplier<Stream<SummitEntry?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        SportType.values().forEach { sportType -> list.add(getElevationGainsFromStream(entriesSupplier.get()?.filter { it?.sportType == sportType })) }
        return list.toFloatArray()
    }

    private fun getEntriesBetweenDates(entries: ArrayList<SummitEntry>?, start: Date?, end: Date?): Stream<SummitEntry?>? {
        return entries
                ?.stream()
                ?.filter { o: SummitEntry? -> o?.date?.after(start) ?: false && o?.date?.before(end) ?: false }
    }

    private fun getEntriesBetweenTopElevation(entries: ArrayList<SummitEntry>?, start: Float, end: Float): Stream<SummitEntry?>? {
        return entries
                ?.stream()
                ?.filter { o: SummitEntry? -> o != null && o.elevationData.maxElevation >= start && o.elevationData.maxElevation < end }
    }

    private fun getEntriesBetweenElevationGains(entries: ArrayList<SummitEntry>?, start: Float, end: Float): Stream<SummitEntry?>? {
        return entries
                ?.stream()
                ?.filter { o: SummitEntry? -> o != null && o.elevationData.elevationGain >= start && o.elevationData.elevationGain < end }
    }

    private fun getEntriesBetweenKilometers(entries: ArrayList<SummitEntry>?, start: Float, end: Float): Stream<SummitEntry?>? {
        return entries
                ?.stream()
                ?.filter { o: SummitEntry? -> o != null && o.kilometers >= start && o.kilometers < end }
    }

    private fun getCountsFromStream(stream: Stream<SummitEntry?>?): Float {
        return stream?.count()?.toFloat() ?: 0.0f
    }

    private fun getKilometersFromStream(stream: Stream<SummitEntry?>?): Float {
        return stream
                ?.mapToInt { o: SummitEntry? -> o?.kilometers?.toInt() ?: 0 }
                ?.sum()?.toFloat() ?: 0.0f
    }

    private fun getElevationGainsFromStream(stream: Stream<SummitEntry?>?): Float {
        return stream
                ?.mapToInt { obj: SummitEntry? -> obj?.elevationData?.elevationGain ?: 0 }
                ?.sum()?.toFloat() ?: 0.0f
    }

    private fun listenOnDataSpinner() {
        dataSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                selectedDataSpinner = i
                selectedDataSpinner()
                drawLineChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        xAxisSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                selectedXAxisSpinner = i
                selectedDataSpinner()
                drawLineChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val dateAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, ArrayList(listOf(*resources.getStringArray(R.array.bar_chart_spinner))))
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataSpinner = barChartView?.findViewById(R.id.bar_chart_spinner_data)
        dataSpinner?.adapter = dateAdapter
        val xAxisAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, ArrayList(listOf(*resources.getStringArray(R.array.bar_chart_spinner_x_axis))))
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        xAxisSpinner = barChartView?.findViewById(R.id.bar_chart_spinner_x_axis)
        xAxisSpinner?.adapter = xAxisAdapter
    }

    inner class CustomMarkerView(context: Context?, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)
        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            try {
                val value: String
                if (e != null && highlight != null) {
                    value = when (selectedXAxisSpinner) {
                        1 -> String.format("%s - %s km", (e.x * IntervalHelper.kilometersStep).toInt(), ((e.x + 1) * IntervalHelper.kilometersStep).toInt())
                        2 -> String.format("%s - %s m", (e.x * IntervalHelper.elevationGainStep).toInt(), ((e.x + 1) * IntervalHelper.elevationGainStep).toInt())
                        3 -> String.format("%s - %s m", (e.x * IntervalHelper.topElevationStep).toInt(), ((e.x + 1) * IntervalHelper.topElevationStep).toInt())
                        else -> if (e.x > 12 || sortFilterHelper.selectedYear == "") String.format("%s", e.x.toInt()) else String.format("%s %s", Month.of(e.x.toInt()), sortFilterHelper.selectedYear)
                    }
                    val selectedValue = (e as BarEntry).yVals[highlight.stackIndex].toInt()
                    val unitString = if (unit == "") "" else " $unit"
                    if (selectedValue == 0) {
                        tvContent?.text = String.format("%s%s\n%s", e.getY().toInt(), unitString, value)
                    } else {
                        tvContent?.text = String.format("%s/%s%s\n%s\n%s", selectedValue, e.getY().toInt(), unitString, value, SportType.values()[highlight.stackIndex].name)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), (-height).toFloat())
        }

    }

}