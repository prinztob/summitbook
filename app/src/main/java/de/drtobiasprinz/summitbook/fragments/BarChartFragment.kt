package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
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
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.Forecast
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.CustomBarChart
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.text.DateFormatSymbols
import java.text.ParseException
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.collections.ArrayList


class BarChartFragment(private val sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment {
    private var summitEntries: ArrayList<Summit>? = null
    private var filteredEntries: ArrayList<Summit>? = null
    private var dataSpinner: Spinner? = null
    private var selectedDataSpinner = 0
    private var indoorHeightMeterPercent = 0
    private var xAxisSpinner: Spinner? = null
    private var selectedXAxisSpinner = 0
    private var barChartView: View? = null
    private var barChartEntries: MutableList<BarEntry?> = mutableListOf()
    private var lineChartEntriesForecast: MutableList<Entry?> = mutableListOf()
    private var unit: String? = "hm"
    private var label: String? = "Height meters"
    private var barChart: CustomBarChart? = null
    private lateinit var intervalHelper: IntervalHelper
    private lateinit var forecasts: ArrayList<Forecast>
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        barChartView = inflater.inflate(R.layout.fragment_bar_chart, container, false)
        setHasOptionsMenu(true)
        sortFilterHelper.fragment = this
        forecasts = sortFilterHelper.database.forecastDao()?.allForecasts as java.util.ArrayList<Forecast>
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        fillDateSpinner()
        summitEntries = sortFilterHelper.entries
        intervalHelper = IntervalHelper(summitEntries)
        barChart = barChartView?.findViewById(R.id.barChart)
        val barChartCustomRenderer = BarChartCustomRenderer(barChart, barChart?.animator, barChart?.viewPortHandler)
        barChart?.renderer = barChartCustomRenderer
        barChart?.setDrawValueAboveBar(false)
        resizeChart()
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

    private fun drawChart() {
        barChart?.drawOrder = arrayOf(
                DrawOrder.LINE, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.BAR, DrawOrder.SCATTER
        )
        barChart?.legend?.isWordWrapEnabled = true
        val combinedData = CombinedData()

        setBarData(combinedData)

        if (lineChartEntriesForecast.isNotEmpty()) {
            val lastEntry = lineChartEntriesForecast.last()
            if (lastEntry != null) {
                lineChartEntriesForecast.add(Entry(lastEntry.x + 1f, lastEntry.y))
            }
            setLineData(combinedData)
        }

        setXAxis()
        val yAxisLeft = barChart?.axisLeft
        setYAxis(yAxisLeft)
        val yAxisRight = barChart?.axisRight
        setYAxis(yAxisRight)
        barChart?.data = combinedData
        barChart?.setTouchEnabled(true)
        barChart?.marker = CustomMarkerView(barChartView?.context, R.layout.marker_graph_bar_chart)

        barChart?.invalidate()
    }

    private fun setBarData(combinedData: CombinedData) {
        val dataSet = BarDataSet(barChartEntries, label)
        setGraphViewBarChart(dataSet)
        combinedData.setData(BarData(dataSet))
    }

    private fun setLineData(combinedData: CombinedData) {
        val dataSet = LineDataSet(lineChartEntriesForecast, "Forecast")
        setGraphViewLineChart(dataSet)
        val data = LineData(dataSet)
        combinedData.setData(data)
    }

    private fun setXAxis(): XAxis? {
        val xAxis = barChart?.xAxis
        val max = barChartEntries.maxByOrNull { it?.x ?: 0f }?.x ?: 0f
        val min = barChartEntries.minByOrNull { it?.x ?: 0f }?.x ?: 0f
        xAxis?.axisMaximum = max + 0.5f
        xAxis?.axisMinimum = min - 0.5f
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return when (selectedXAxisSpinner) {
                    1 -> String.format("%s", ((value + 0.5) * IntervalHelper.kilometersStep).toInt())
                    2 -> String.format("%s", ((value + 0.5) * IntervalHelper.elevationGainStep).toInt())
                    3 -> String.format("%s", ((value + 0.5) * IntervalHelper.topElevationStep).toInt())
                    else -> {
                        if (sortFilterHelper.selectedYear == "" || value > 12f || value == 0f) {
                            String.format("%s", value.toInt())
                        } else {
                            val month = if (value < 1 || value > 12) 0 else value.toInt() - 1
                            String.format("%s", DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month])
                        }
                    }
                }
            }
        }
        return xAxis
    }

    private fun setYAxis(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(requireContext().resources.configuration.locales[0], "%.0f %s", value, unit)
            }
        }
    }

    override fun update(filteredSummitEntries: List<Summit>?) {
        filteredEntries = filteredSummitEntries as ArrayList<Summit>
        selectedDataSpinner()
        drawChart()
    }

    private fun setGraphViewBarChart(dataSet: BarDataSet) {
        dataSet.setDrawValues(false)
        dataSet.highLightColor = Color.RED
        dataSet.colors = SportType.values().map { ContextCompat.getColor(requireContext(), it.color) }
        dataSet.stackLabels = SportType.values().map { getString(it.sportNameStringId) }.toTypedArray()
    }

    private fun setGraphViewLineChart(dataSet: LineDataSet) {
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.isHighlightEnabled = false
        dataSet.color = Color.DKGRAY
        dataSet.circleHoleColor = Color.RED
        dataSet.highLightColor = Color.RED
        dataSet.lineWidth = 2f
        dataSet.mode = LineDataSet.Mode.STEPPED
    }

    private fun selectedDataSpinner() {
        barChart?.axisLeft?.removeAllLimitLines()
        val sortedEntries = filteredEntries
        sortedEntries?.sortWith(compareBy { it.date })
        barChartEntries.clear()
        lineChartEntriesForecast.clear()
        try {
            var annualTarget: Float = when (selectedDataSpinner) {
                1 -> sharedPreferences.getString("annual_target_km", "1200")?.toFloat() ?: 1200f
                2 -> sharedPreferences.getString("annual_target", "50000")?.toFloat() ?: 50000f
                else -> sharedPreferences.getString("annual_target_activities", "52")?.toFloat() ?: 52f
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
        intervalHelper.setSelectedYear(sortFilterHelper.selectedYear)
        intervalHelper.calculate()
        val now = Calendar.getInstance()
        val currentYear = now[Calendar.YEAR].toString()

        for (i in 0 until intervalHelper.dates.size - 1) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier { getEntriesBetweenDates(filteredEntries, intervalHelper.dates[i], intervalHelper.dates[i + 1]) }
            val xValue = intervalHelper.dateAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
            if (sortFilterHelper.selectedYear == currentYear) {
                val forecast = forecasts.firstOrNull { it.month == xValue.toInt() && it.year.toString() == currentYear }
                if (forecast != null) {
                    when (selectedDataSpinner) {
                        1 -> lineChartEntriesForecast.add(Entry(xValue - 0.5F, forecast.forecastDistance.toFloat()))
                        2 -> lineChartEntriesForecast.add(Entry(xValue - 0.5F, forecast.forecastHeightMeter.toFloat()))
                        else -> lineChartEntriesForecast.add(Entry(xValue - 0.5F, forecast.forecastNumberActivities.toFloat()))
                    }
                }
            }
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChartWithElevationGainAsXAxis() {
        intervalHelper.calculate()
        for (i in 0 until intervalHelper.elevationGains.size - 1) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier { getEntriesBetweenElevationGains(filteredEntries, intervalHelper.elevationGains[i], intervalHelper.elevationGains[i + 1]) }
            val xValue = intervalHelper.elevationGainAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChartWithKilometersAsXAxis() {
        intervalHelper.calculate()
        for (i in 0 until intervalHelper.kilometers.size - 1) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier { getEntriesBetweenKilometers(filteredEntries, intervalHelper.kilometers[i], intervalHelper.kilometers[i + 1]) }
            val xValue = intervalHelper.kilometerAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChartWithTopElevationAsXAxis() {
        intervalHelper.calculate()
        for (i in 0 until intervalHelper.topElevations.size - 1) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier { getEntriesBetweenTopElevation(filteredEntries, intervalHelper.topElevations[i], intervalHelper.topElevations[i + 1]) }
            val xValue = intervalHelper.topElevationAnnotation[i]
            when (selectedDataSpinner) {
                1 -> {
                    label = "Kilometers"
                    unit = "km"
                    barChartEntries.add(BarEntry(xValue, getKilometerPerSportType(streamSupplier)))
                }
                2 -> {
                    label = "Elevation Gain"
                    unit = "m"
                    barChartEntries.add(BarEntry(xValue, getElevationGainsPerSportType(streamSupplier)))
                }
                else -> {
                    label = "Count"
                    unit = ""
                    barChartEntries.add(BarEntry(xValue, getCountsPerSportType(streamSupplier)))
                }
            }
        }
    }

    private fun getCountsPerSportType(entriesSupplier: Supplier<Stream<Summit?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        SportType.values().forEach { sportType -> list.add(getCountsFromStream(entriesSupplier.get()?.filter { it?.sportType == sportType })) }
        return list.toFloatArray()
    }

    private fun getKilometerPerSportType(entriesSupplier: Supplier<Stream<Summit?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        SportType.values().forEach { sportType -> list.add(getKilometersFromStream(entriesSupplier.get()?.filter { it?.sportType == sportType })) }
        return list.toFloatArray()
    }

    private fun getElevationGainsPerSportType(entriesSupplier: Supplier<Stream<Summit?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        SportType.values().forEach { sportType -> list.add(getElevationGainsFromStream(entriesSupplier.get()?.filter { it?.sportType == sportType })) }
        return list.toFloatArray()
    }

    private fun getEntriesBetweenDates(entries: List<Summit>?, start: Date?, end: Date?): Stream<Summit?>? {
        return entries
                ?.stream()
                ?.filter { o: Summit? -> o?.date?.after(start) ?: false && o?.date?.before(end) ?: false }
    }

    private fun getEntriesBetweenTopElevation(entries: List<Summit>?, start: Float, end: Float): Stream<Summit?>? {
        return entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.elevationData.maxElevation >= start && o.elevationData.maxElevation < end }
    }

    private fun getEntriesBetweenElevationGains(entries: List<Summit>?, start: Float, end: Float): Stream<Summit?>? {
        return entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.elevationData.elevationGain >= start && o.elevationData.elevationGain < end }
    }

    private fun getEntriesBetweenKilometers(entries: List<Summit>?, start: Float, end: Float): Stream<Summit?>? {
        return entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.kilometers >= start && o.kilometers < end }
    }

    private fun getCountsFromStream(stream: Stream<Summit?>?): Float {
        return stream?.count()?.toFloat() ?: 0.0f
    }

    private fun getKilometersFromStream(stream: Stream<Summit?>?): Float {
        return stream
                ?.mapToInt { o: Summit? -> o?.kilometers?.toInt() ?: 0 }
                ?.sum()?.toFloat() ?: 0.0f
    }

    private fun getElevationGainsFromStream(stream: Stream<Summit?>?): Float {
        return stream
                ?.mapToInt {
                    if (it?.sportType == SportType.IndoorTrainer) {
                        it.elevationData.elevationGain * indoorHeightMeterPercent / 100
                    } else {
                        it?.elevationData?.elevationGain ?: 0
                    }
                }
                ?.sum()?.toFloat() ?: 0.0f
    }

    private fun listenOnDataSpinner() {
        dataSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                selectedDataSpinner = i
                selectedDataSpinner()
                drawChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        xAxisSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                selectedXAxisSpinner = i
                selectedDataSpinner()
                drawChart()
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
                        else -> {
                            if (e.x > 12 || sortFilterHelper.selectedYear == "") {
                                String.format("%s", e.x.toInt())
                            } else {
                                val month = if (e.x < 1 || e.x > 12) 0 else e.x.toInt() - 1
                                String.format("%s %s", DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month], sortFilterHelper.selectedYear)
                            }
                        }
                    }
                    val currentYear = Calendar.getInstance()[Calendar.YEAR].toString()
                    val forecast = forecasts.firstOrNull { it.month == e.x.toInt() && it.year.toString() == currentYear }

                    val selectedValue = (e as BarEntry).yVals[highlight.stackIndex].toInt()
                    val unitString = if (unit == "") "" else " $unit"
                    if (selectedValue == 0) {
                        val forecastValue = when (selectedDataSpinner) {
                            1 -> forecast?.forecastDistance?: 0
                            2 -> forecast?.forecastHeightMeter?: 0
                            else -> forecast?.forecastNumberActivities?: 0
                        }
                        tvContent?.text = if (forecastValue > 0) {
                            String.format("%s%s\n%s\n%s: %s%s", e.getY().toInt(), unitString, value, getString(R.string.forecast_abbr), forecastValue, unitString)
                        } else {
                            String.format("%s%s\n%s", e.getY().toInt(), unitString, value)
                        }
                    } else {
                        tvContent?.text = String.format("%s/%s%s\n%s\n%s", selectedValue, e.getY().toInt(), unitString, value, SportType.values()[highlight.stackIndex].name)
                    }
                }

            } catch (ex: Exception) {
                tvContent?.text = ""
                ex.printStackTrace()
            }
        }

        private val uiScreenWidth = resources.displayMetrics.widthPixels
        override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
            var newPosX = posX
            if ((uiScreenWidth - posX) < width / 2f) {
                newPosX = uiScreenWidth - width / 2f - 25f
            }
            if (tvContent?.text != "") {
                super.draw(canvas, newPosX, posY)
            }
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), (-height).toFloat())
        }

    }

}