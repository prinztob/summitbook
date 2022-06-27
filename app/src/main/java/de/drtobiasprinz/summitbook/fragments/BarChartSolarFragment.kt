package de.drtobiasprinz.summitbook.fragments

import android.content.Context
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
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.SolarIntensity
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.CustomBarChart
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import kotlin.math.ceil


class BarChartSolarFragment : Fragment() {
    private var solarIntensities: List<SolarIntensity>? = null
    private var yAxisSpinner: Spinner? = null
    private var selectedYAxisSpinnerEntry: YAxisSelector = YAxisSelector.BatteryGainBySolar
    private var xAxisSpinner: Spinner? = null
    private var selectedXAxisSpinnerEntry: XAxisSelector = XAxisSelector.Days
    private var barChartView: View? = null
    private var barChartEntries: MutableList<BarEntry?> = mutableListOf()
    private var lineChartEntries: MutableList<Entry?> = mutableListOf()
    private var unit: String? = "hm"
    private var label: String? = "Height meters"
    private var barChart: CustomBarChart? = null
    private var summits: ArrayList<Summit>? = null
    private var max: Float = 1F
    private lateinit var resultReceiver: FragmentResultReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultReceiver = context as FragmentResultReceiver
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        barChartView = inflater.inflate(R.layout.fragment_bar_chart, container, false)
        setHasOptionsMenu(true)
        fillDateSpinner()
        summits = resultReceiver.getSortFilterHelper().entries
        solarIntensities = resultReceiver.getSortFilterHelper().database.solarIntensityDao()?.getAll()
        val numberActivitiesPerDay = summits?.groupingBy { it.getDateAsString() }?.eachCount()
        solarIntensities?.forEach {
            it.activitiesOnDay = if (numberActivitiesPerDay?.keys?.contains(it.getDateAsString()) == true) numberActivitiesPerDay[it.getDateAsString()]
                    ?: 0 else 0
        }
        barChart = barChartView?.findViewById(R.id.barChart)
        resizeChart()
        listenOnDataSpinner()
        return barChartView
    }

    private fun resizeChart() {
        val metrics = DisplayMetrics()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val display = activity?.display
            display?.getRealMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            val display = activity?.windowManager?.defaultDisplay
            @Suppress("DEPRECATION")
            display?.getMetrics(metrics)
        }
        barChart?.minimumHeight = (metrics.heightPixels * 0.7).toInt()
    }

    private fun drawChart() {
        resetChart()
        val barChartCustomRenderer = BarChartCustomRenderer(barChart, barChart?.animator, barChart?.viewPortHandler)
        barChart?.renderer = barChartCustomRenderer
        barChart?.setDrawValueAboveBar(false)
        barChart?.drawOrder = arrayOf(
                DrawOrder.LINE, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.BAR, DrawOrder.SCATTER
        )
        val combinedData = CombinedData()

        setBarData(combinedData)
        if (lineChartEntries.isNotEmpty()) {
            val lastEntry = lineChartEntries.last()
            if (lastEntry != null) {
                lineChartEntries.add(Entry(lastEntry.x + 1f, lastEntry.y))
            }
            setLineData(combinedData)
        }
        setXAxis()
        setYAxisBarData(barChart?.axisLeft)
        setYAxisLineData(barChart?.axisRight)
        barChart?.data = combinedData
        barChart?.setTouchEnabled(true)
        barChart?.marker = CustomMarkerView(barChartView?.context, R.layout.marker_graph_bar_chart)

        barChart?.setVisibleXRangeMaximum(12f) // allow 12 values to be displayed at once on the x-axis, not more
        barChart?.moveViewToX(barChartEntries.size.toFloat() - 12f)
    }

    private fun setLineData(combinedData: CombinedData) {
        val dataSet = LineDataSet(lineChartEntries, "Summits per Date")
        setGraphViewLineChart(dataSet)
        val data = LineData(dataSet)
        combinedData.setData(data)
    }

    private fun setGraphViewLineChart(dataSet: LineDataSet) {
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        dataSet.isHighlightEnabled = false
        dataSet.color = Color.BLACK
        dataSet.lineWidth = 2f
        dataSet.mode = LineDataSet.Mode.STEPPED
    }

    private fun resetChart() {
        barChart?.fitScreen()
        barChart?.data?.clearValues()
        barChart?.xAxis?.valueFormatter = null
        barChart?.notifyDataSetChanged()
        barChart?.clear()
        barChart?.invalidate()
    }

    private fun setBarData(combinedData: CombinedData) {
        val dataSet = BarDataSet(barChartEntries, label)
        setGraphViewBarChart(dataSet)
        combinedData.setData(BarData(dataSet))
    }


    private fun setXAxis(): XAxis? {
        val xAxis = barChart?.xAxis
        val max = barChartEntries.maxByOrNull { it?.x ?: 0f }?.x ?: 0f
        val min = barChartEntries.minByOrNull { it?.x ?: 0f }?.x ?: 0f
        xAxis?.axisMaximum = max + 0.5f
        xAxis?.axisMinimum = min - 0.5f

        val startDate = (barChartEntries.first()?.data as SolarIntensity).date.time
        val endDate = (barChartEntries.last()?.data as SolarIntensity).date.time
        val divisor = (barChartEntries.last()?.x ?: 0f) - (barChartEntries.first()?.x ?: 0f)
        val factor = (endDate - startDate) / (if (divisor == 0f) 1f else divisor)
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val time = factor * value + startDate
                return SimpleDateFormat(Summit.DATE_FORMAT, requireContext().resources.configuration.locales[0])
                        .format(Date(time.toLong()))
            }
        }
        return xAxis
    }

    private fun setYAxisBarData(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(requireContext().resources.configuration.locales[0], "%.2f %s", value / max, unit)
            }
        }
    }

    private fun setYAxisLineData(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val decimalPlace = (value - ceil(value))
                return if (decimalPlace == 0f) String.format(requireContext().resources.configuration.locales[0], "# %s", value.toInt()) else ""
            }
        }
    }

    private fun setGraphViewBarChart(dataSet: BarDataSet) {
        dataSet.setDrawValues(false)
        dataSet.highLightColor = Color.GREEN
        dataSet.colors = barChartEntries.map {
            if ((it?.data as SolarIntensity).isForWholeDay) Color.BLUE else Color.RED
        }
        val legend = barChart?.legend
        if (legend != null) {
            legend.entries
            legend.yEntrySpace = 10f
            legend.isWordWrapEnabled = true
            val legends = mutableListOf(
                    LegendEntry(getString(R.string.part_day), Legend.LegendForm.CIRCLE, 9f, 5f, null, Color.YELLOW),
                    LegendEntry(getString(R.string.whole_day), Legend.LegendForm.CIRCLE, 9f, 5f, null, Color.BLUE),
                    LegendEntry(getString(R.string.activity_hint), Legend.LegendForm.CIRCLE, 9f, 5f, null, Color.BLACK),
            )
            legend.setCustom(legends)
            legend.isEnabled = true
        }
    }

    private fun selectedDataSpinner() {
        barChart?.axisLeft?.removeAllLimitLines()
        val sortedEntries = solarIntensities
        sortedEntries?.sortedBy { it.date }
        barChartEntries.clear()
        try {
            updateData()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    @Throws(ParseException::class)
    private fun updateData() {
        val dateFormat: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
        val data = when (selectedXAxisSpinnerEntry) {
            XAxisSelector.Days -> {
                solarIntensities?.sortedBy { it.date }
            }
            XAxisSelector.Weeks -> {
                solarIntensities
                        ?.sortedBy { it.date }
                        ?.chunked(7)
                        ?.filter { it.isNotEmpty() }
                        ?.map { entries ->
                            val sorted = entries.sortedBy { it.getDateAsFloat() }
                            val newEntry = SolarIntensity(0,
                                    sorted.first().date,
                                    sorted.sumByDouble { it.solarUtilizationInHours },
                                    sorted.sumByDouble { it.solarExposureInHours } / sorted.size,
                                    true)
                            newEntry.markerText = "${dateFormat.format(sorted.first().date)} - ${dateFormat.format(sorted.last().date)}"
                            newEntry.activitiesOnDay = sorted.sumBy { it.activitiesOnDay }
                            newEntry
                        }
            }
            else -> {
                val solarIntensitiesLocal: MutableList<SolarIntensity> = mutableListOf()
                //TODO: until max date
                for (i in 0L until 24L) {
                    val startDate = Date.from(YearMonth.now().minusMonths(i).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                    val endDate = Date.from(YearMonth.now().minusMonths(i).atEndOfMonth().atStartOfDay(ZoneId.systemDefault()).toInstant())

                    val entries = solarIntensities?.filter { it.date.before(endDate) && (it.date.after(startDate) || it.date == startDate) }
                    val newEntry = if (entries != null && entries.isNotEmpty()) {
                        SolarIntensity(0,
                                entries.first().date,
                                entries.sumByDouble { it.solarUtilizationInHours },
                                entries.sumByDouble { it.solarExposureInHours } / entries.size,
                                true)
                    } else {
                        SolarIntensity(0,
                                startDate, 0.0, 0.0, true)
                    }
                    newEntry.activitiesOnDay = entries?.sumBy { it.activitiesOnDay } ?: 0
                    newEntry.markerText = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
                    solarIntensitiesLocal.add(newEntry)
                }
                solarIntensitiesLocal.sortedBy { it.date }
            }
        }
        val maxBarChart = data?.map { selectedYAxisSpinnerEntry.getBinValue(it) }?.maxOrNull() ?: 1F
        val maxLineChart = data?.map { it.activitiesOnDay.toFloat() }?.maxOrNull() ?: 1F
        max = maxLineChart / (if (maxBarChart > 0f) maxBarChart else 1f)
        barChartEntries = data?.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), selectedYAxisSpinnerEntry.getBinValue(entry) * max, entry)
        }?.toMutableList() ?: mutableListOf()
        lineChartEntries = data?.mapIndexed { index, entry ->
            Entry(index.toFloat() - 0.5F, entry.activitiesOnDay.toFloat(), entry)
        }?.toMutableList() ?: mutableListOf()
        label = getString(selectedYAxisSpinnerEntry.nameId)
        unit = getString(selectedYAxisSpinnerEntry.unitId)
    }

    private fun listenOnDataSpinner() {
        yAxisSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                selectedYAxisSpinnerEntry = YAxisSelector.values()[i]
                selectedDataSpinner()
                drawChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        xAxisSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                selectedXAxisSpinnerEntry = XAxisSelector.values()[i]
                selectedDataSpinner()
                drawChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val dateAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item,
                YAxisSelector.values().map { resources.getString(it.nameId) }.toTypedArray())
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yAxisSpinner = barChartView?.findViewById(R.id.bar_chart_spinner_data)
        yAxisSpinner?.adapter = dateAdapter
        val xAxisAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item,
                XAxisSelector.values().map { resources.getString(it.nameId) }.toTypedArray())
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        xAxisSpinner = barChartView?.findViewById(R.id.bar_chart_spinner_x_axis)
        xAxisSpinner?.adapter = xAxisAdapter
    }

    inner class CustomMarkerView(context: Context?, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            try {
                val entry = e?.data as SolarIntensity?
                if (entry != null) {
                    tvContent?.text = String.format("%s\n%.2f %s\n%.2f %s", entry.markerText, entry.solarUtilizationInHours, getString(R.string.percent), entry.solarExposureInHours, getString(R.string.h))
                } else {
                    tvContent?.text = ""
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

    enum class XAxisSelector(val nameId: Int) {
        Days(R.string.days),
        Weeks(R.string.weeks),
        Months(R.string.months),
    }

    enum class YAxisSelector(val nameId: Int, val unitId: Int, val getBinValue: (SolarIntensity) -> Float) {
        BatteryGainBySolar(R.string.solar_50000_lux_condition, R.string.percent, { it.solarUtilizationInHours.toFloat() }),
        SolarExposure(R.string.solar_exposure, R.string.h, { it.solarExposureInHours.toFloat() }),
    }

}