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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.CustomBarChart
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import java.text.DateFormatSymbols
import java.text.ParseException
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream


class BarChartFragment : Fragment(), SummationFragment {
    private var summitEntries: ArrayList<Summit>? = null
    private var filteredEntries: ArrayList<Summit>? = null
    private var yAxisSpinner: Spinner? = null
    private var selectedYAxisSpinnerEntry: YAxisSelector = YAxisSelector.Count
    private var indoorHeightMeterPercent = 0
    private var xAxisSpinner: Spinner? = null
    private var selectedXAxisSpinnerEntry: XAxisSelector = XAxisSelector.Date
    private var barChartView: View? = null
    private var barChartEntries: MutableList<BarEntry?> = mutableListOf()
    private var lineChartEntriesForecast: MutableList<Entry?> = mutableListOf()
    private var unit: String? = "hm"
    private var label: String? = "Height meters"
    private var barChart: CustomBarChart? = null
    private lateinit var intervalHelper: IntervalHelper
    private lateinit var resultReceiver: FragmentResultReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultReceiver = context as FragmentResultReceiver
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        intervalHelper = IntervalHelper(resultReceiver.getSortFilterHelper().filteredEntries)
        barChartView = inflater.inflate(R.layout.fragment_bar_chart, container, false)
        setHasOptionsMenu(true)
        resultReceiver.getSortFilterHelper().fragment = this
        indoorHeightMeterPercent = resultReceiver.getSharedPreference().getInt("indoor_height_meter_per_cent", 0)
        fillDateSpinner()
        summitEntries = resultReceiver.getSortFilterHelper().entries
        barChart = barChartView?.findViewById(R.id.barChart)
        val barChartCustomRenderer = BarChartCustomRenderer(barChart, barChart?.animator, barChart?.viewPortHandler)
        barChart?.renderer = barChartCustomRenderer
        barChart?.setDrawValueAboveBar(false)
        resizeChart()
        filteredEntries = resultReceiver.getSortFilterHelper().filteredEntries
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
        xAxis?.axisMaximum = if ((selectedXAxisSpinnerEntry == XAxisSelector.Participants || selectedXAxisSpinnerEntry == XAxisSelector.Equipments) && max < 10) 10.5f else max + 0.5f
        xAxis?.axisMinimum = min - 0.5f
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (selectedXAxisSpinnerEntry == XAxisSelector.Date) {
                    if (resultReceiver.getSortFilterHelper().selectedYear == "" || value > 12f || value == 0f) {
                        String.format("%s", value.toInt())
                    } else {
                        val month = if (value < 1 || value > 12) 0 else value.toInt() - 1
                        String.format("%s", DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month])
                    }
                } else if (selectedXAxisSpinnerEntry == XAxisSelector.Participants || selectedXAxisSpinnerEntry == XAxisSelector.Equipments) {
                    if (value.toInt() < selectedXAxisSpinnerEntry.getIntervals(intervalHelper).size) {
                        selectedXAxisSpinnerEntry.getIntervals(intervalHelper)[value.toInt()].toString()
                    } else {
                        ""
                    }
                } else {
                    String.format("%s", ((value + 0.5) * selectedXAxisSpinnerEntry.stepsSize).toInt())
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
            var annualTarget: Float = resultReceiver.getSharedPreference().getString(selectedYAxisSpinnerEntry.sharedPreferenceKey, selectedYAxisSpinnerEntry.defaultAnnualTarget.toString())?.toFloat()
                    ?: selectedYAxisSpinnerEntry.defaultAnnualTarget.toFloat()
            if (resultReceiver.getSortFilterHelper().selectedYear != "") {
                annualTarget /= 12f
            }
            val line1 = LimitLine(annualTarget)
            barChart?.axisLeft?.addLimitLine(line1)
            barChart?.setSafeZoneColor(ContextCompat.getColor(requireContext(), R.color.red_50), ContextCompat.getColor(requireContext(), R.color.green_50))
            updateBarChart()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun updateDataForChart(xValue: Float, streamSupplier: Supplier<Stream<Summit?>?>) {
        val now = Calendar.getInstance()
        val currentYear = now[Calendar.YEAR].toString()
        label = getString(selectedYAxisSpinnerEntry.nameId)
        unit = getString(selectedYAxisSpinnerEntry.unitId)
        barChartEntries.add(BarEntry(xValue, getValueForEntry(streamSupplier)))
        if (resultReceiver.getSortFilterHelper().selectedYear == currentYear && selectedXAxisSpinnerEntry == XAxisSelector.Date) {
            val forecasts = resultReceiver.getSortFilterHelper().database.forecastDao()?.allForecasts as ArrayList<Forecast>
            val forecast = forecasts.firstOrNull { it.month == xValue.toInt() && it.year.toString() == currentYear }
            if (forecast != null) {
                lineChartEntriesForecast.add(Entry(xValue - 0.5F, selectedYAxisSpinnerEntry.getForecastValue(forecast)))
            }
        }
    }


    @Throws(ParseException::class)
    private fun updateBarChart() {
        intervalHelper = IntervalHelper(resultReceiver.getSortFilterHelper().filteredEntries)
        intervalHelper.setSelectedYear(resultReceiver.getSortFilterHelper().selectedYear)
        intervalHelper.calculate()
        val interval = selectedXAxisSpinnerEntry.getIntervals(intervalHelper)
        val annotation = selectedXAxisSpinnerEntry.getAnnotation(intervalHelper)
        for (i in 0 until interval.size - 1) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier {
                selectedXAxisSpinnerEntry.getStream(filteredEntries, interval[i], interval[i + 1])
            }
            val xValue = annotation[i]
            updateDataForChart(xValue, streamSupplier)
        }
    }

    private fun getValueForEntry(entriesSupplier: Supplier<Stream<Summit?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        SportType.values().forEach { sportType -> list.add(selectedYAxisSpinnerEntry.f(entriesSupplier.get()?.filter { it?.sportType == sportType }, indoorHeightMeterPercent)) }
        return list.toFloatArray()
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
                val value: String
                if (e != null && highlight != null) {
                    value = if (selectedXAxisSpinnerEntry == XAxisSelector.Date) {
                        if (e.x > 12 || resultReceiver.getSortFilterHelper().selectedYear == "") {
                            String.format("%s", e.x.toInt())
                        } else {
                            val month = if (e.x < 1 || e.x > 12) 0 else e.x.toInt() - 1
                            String.format("%s %s", DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month], resultReceiver.getSortFilterHelper().selectedYear)
                        }
                    } else if (selectedXAxisSpinnerEntry == XAxisSelector.Participants || selectedXAxisSpinnerEntry == XAxisSelector.Equipments) {
                        if (e.x.toInt() < selectedXAxisSpinnerEntry.getIntervals(intervalHelper).size) {
                            selectedXAxisSpinnerEntry.getIntervals(intervalHelper)[e.x.toInt()].toString()
                        } else {
                            ""
                        }
                    } else {
                        String.format("%s - %s %s", (e.x * selectedXAxisSpinnerEntry.stepsSize).toInt(), ((e.x + 1) * selectedXAxisSpinnerEntry.stepsSize).toInt(), getString(selectedXAxisSpinnerEntry.unitId))

                    }
                    val currentYear = Calendar.getInstance()[Calendar.YEAR].toString()
                    val forecasts = resultReceiver.getSortFilterHelper().database.forecastDao()?.allForecasts as ArrayList<Forecast>
                    val forecast = forecasts.firstOrNull { it.month == e.x.toInt() && it.year.toString() == currentYear }

                    val selectedValue = (e as BarEntry).yVals[highlight.stackIndex].toInt()
                    val unitString = if (unit == "") "" else " $unit"
                    if (selectedValue == 0) {
                        val forecastValue = forecast?.let { selectedYAxisSpinnerEntry.getForecastValue(it) }
                                ?: 0f
                        tvContent?.text = if (forecastValue > 0 && selectedXAxisSpinnerEntry == XAxisSelector.Date) {
                            String.format("%s%s\n%s\n%s: %s%s", e.getY().toInt(), unitString, value, getString(R.string.forecast_abbr), forecastValue.toInt(), unitString)
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

    enum class XAxisSelector(val nameId: Int, val unitId: Int, val stepsSize: Double, val getStream: (entries: List<Summit>?, start: Any, end: Any) -> Stream<Summit?>?,
                             val getIntervals: (IntervalHelper) -> List<Any>, val getAnnotation: (IntervalHelper) -> List<Float>) {
        Date(R.string.date, R.string.empty, 0.0, { entries, start, end ->
            entries
                    ?.stream()
                    ?.filter { o: Summit? -> o?.date?.after(start as java.util.Date) ?: false && o?.date?.before(end as java.util.Date) ?: false }
        }, { e -> e.dates }, { e -> e.dateAnnotation }),
        Kilometers(R.string.kilometers_hint, R.string.km, IntervalHelper.kilometersStep, { entries, start, end ->
            entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.kilometers >= start as Float && o.kilometers < end as Float }
        }, { e -> e.kilometers }, { e -> e.kilometerAnnotation }),
        ElevationGain(R.string.height_meter_hint, R.string.hm, IntervalHelper.elevationGainStep, { entries, start, end ->
            entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.elevationData.elevationGain >= start as Float && o.elevationData.elevationGain < end as Float }
        }, { e -> e.elevationGains }, { e -> e.elevationGainAnnotation }),
        TopElevation(R.string.top_elevation_hint, R.string.masl, IntervalHelper.topElevationStep, { entries, start, end ->
            entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.elevationData.maxElevation >= start as Float && o.elevationData.maxElevation < end as Float }
        }, { e -> e.topElevations }, { e -> e.topElevationAnnotation }),
        Participants(R.string.participants, R.string.empty, 1.0, { entries, start, _ ->
            entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.participants.contains(start) }
        }, { e -> e.participants }, { e -> e.participantsAnnotation }),
        Equipments(R.string.equipments, R.string.empty, 1.0, { entries, start, _ ->
            entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.equipments.contains(start) }
        }, { e -> e.equipments }, { e -> e.equipmentsAnnotation })
    }

    enum class YAxisSelector(val nameId: Int, val unitId: Int, val sharedPreferenceKey: String, val defaultAnnualTarget: Int,
                             val f: (Stream<Summit?>?, Int) -> Float, val getForecastValue: (Forecast) -> Float) {
        Count(R.string.count, R.string.empty, "annual_target_activities", 52, { stream, _ ->
            stream?.count()?.toFloat() ?: 0f
        }, { forecast -> forecast.forecastNumberActivities.toFloat() }),
        Kilometers(R.string.kilometers_hint, R.string.km, "annual_target_km", 1200, { stream, _ ->
            stream
                    ?.mapToInt { o: Summit? -> o?.kilometers?.toInt() ?: 0 }
                    ?.sum()?.toFloat() ?: 0.0f
        }, { forecast -> forecast.forecastDistance.toFloat() }),
        ElevationGain(R.string.height_meter_hint, R.string.hm, "annual_target", 50000, { stream, indoorHeightMeterPercent ->
            stream
                    ?.mapToInt {
                        if (it?.sportType == SportType.IndoorTrainer) {
                            it.elevationData.elevationGain * indoorHeightMeterPercent / 100
                        } else {
                            it?.elevationData?.elevationGain ?: 0
                        }
                    }
                    ?.sum()?.toFloat() ?: 0.0f
        }, { forecast -> forecast.forecastHeightMeter.toFloat() })

    }

}