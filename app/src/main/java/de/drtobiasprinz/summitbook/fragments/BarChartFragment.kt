package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.Keys.PREF_USE_SPORT_GROUP_INSTEAD_OF_TYPE
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentBarChartBinding
import de.drtobiasprinz.summitbook.db.entities.SportGroup
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.BarChartXAxisSelector
import de.drtobiasprinz.summitbook.models.BarChartYAxisSelector
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.ParseException
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream
import javax.inject.Inject
import kotlin.math.floor

@AndroidEntryPoint
class BarChartFragment : Fragment() {

    private lateinit var binding: FragmentBarChartBinding
    private val viewModel: DatabaseViewModel by activityViewModels()

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private var selectedYAxisSpinnerEntry: BarChartYAxisSelector = BarChartYAxisSelector.Count
    private var indoorHeightMeterPercent = 0
    private var selectedXAxisSpinnerMonth: Int = 0
    private var selectedXAxisSpinnerEntry: BarChartXAxisSelector = BarChartXAxisSelector.DateByMonth
    private var barChartEntries: MutableList<BarEntry?> = mutableListOf()
    private var lineChartEntriesForecast: MutableList<Entry?> = mutableListOf()
    private var unit: String = "hm"
    private var label: String = "Height meters"
    private lateinit var intervalHelper: IntervalHelper
    private var minDate: Date = Date()
    private var xValuesForecast: List<Float> = mutableListOf()
    private var yValuesForecast: List<Int> = mutableListOf()

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBarChartBinding.inflate(layoutInflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
        fillDateSpinner()
        binding.apply {
            viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
                binding.loading.visibility = View.VISIBLE
                binding.barChart.visibility = View.GONE
                itData.data?.let { summits ->
                    if (summits.isNotEmpty()) {
                        update(summits)
                    }
                }
            }
        }
        return binding.root
    }

    private fun resizeChart() {
        binding.barChart.minimumHeight =
            (Resources.getSystem().displayMetrics.heightPixels * 0.7).toInt()
    }

    private fun drawChart() {
        binding.barChart.drawOrder = arrayOf(
            DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.BAR, DrawOrder.SCATTER, DrawOrder.LINE
        )
        binding.barChart.legend?.isWordWrapEnabled = true
        when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.barChart.xAxis.textColor = Color.BLACK
                binding.barChart.axisLeft.textColor = Color.WHITE
                binding.barChart.legend?.textColor = Color.WHITE
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                binding.barChart.xAxis.textColor = Color.BLACK
                binding.barChart.axisLeft.textColor = Color.BLACK
                binding.barChart.legend?.textColor = Color.BLACK
            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                binding.barChart.xAxis.textColor = Color.BLACK
                binding.barChart.axisLeft.textColor = Color.WHITE
                binding.barChart.legend?.textColor = Color.WHITE
            }
        }
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
        setYAxis(binding.barChart.axisLeft)
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.data = combinedData
        binding.barChart.setTouchEnabled(true)
        binding.barChart.marker =
            CustomMarkerView(requireContext(), R.layout.marker_graph_bar_chart)
        binding.barChart.setVisibleXRangeMinimum(if (barChartEntries.size < 12) (barChartEntries.size + 1).toFloat() else 12f)
        if (selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart != -1f) {
            binding.barChart.setVisibleXRangeMaximum(selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart)
            if (!selectedXAxisSpinnerEntry.isAQuality) {
                binding.barChart.moveViewToX(
                    barChartEntries.maxOf { it?.x ?: 0f } -
                            selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart + 1
                )
            }
        }
        binding.barChart.invalidate()
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
        val xAxis = binding.barChart.xAxis
        val max = barChartEntries.maxByOrNull { it?.x ?: 0f }?.x ?: 0f
        val min = barChartEntries.minByOrNull { it?.x ?: 0f }?.x ?: 0f
        xAxis?.axisMaximum =
            if ((selectedXAxisSpinnerEntry.isAQuality) && max < 10) 10.5f else max + 0.5f
        xAxis?.axisMinimum = min - 0.5f
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYear || selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYearUntilToday) {
                    String.format("%s", value.toInt())
                } else if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByWeek) {
                    String.format("%s", value.toInt() % 52)
                } else if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByMonth) {
                    String.format(
                        "%s",
                        DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[(value % 12f).toInt()]
                    )
                } else if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByQuarter) {
                    String.format("%s", toRomanNumerics((value.toInt() + 1) % 4))
                } else if (selectedXAxisSpinnerEntry.isAQuality) {
                    getFormattedValueForQuantity(value)
                } else {
                    String.format(
                        "%s",
                        ((value + 0.5) * selectedXAxisSpinnerEntry.stepsSize).toInt()
                    )
                }
            }
        }
        return xAxis
    }

    private fun setYAxis(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(
                    requireContext().resources.configuration.locales[0],
                    "%.0f %s",
                    value,
                    unit
                )
            }
        }
    }

    fun update(summits: List<Summit>) {
        lifecycleScope.launch {
            val filteredSummits = withContext(Dispatchers.IO) {
                sortFilterValues.apply(summits, sharedPreferences)
            }
            binding.loading.visibility = View.GONE
            binding.barChart.visibility = View.VISIBLE
            minDate = filteredSummits.minBy { it.date }.date
            intervalHelper = IntervalHelper(filteredSummits)
            val barChartCustomRenderer = BarChartCustomRenderer(
                binding.barChart,
                binding.barChart.animator,
                binding.barChart.viewPortHandler
            )
            binding.barChart.renderer = barChartCustomRenderer
            binding.barChart.setDrawValueAboveBar(false)
            resizeChart()
            listenOnDataSpinner(filteredSummits)
            selectedDataSpinner(filteredSummits)
            drawChart()
        }
    }

    private fun setGraphViewBarChart(dataSet: BarDataSet) {
        dataSet.setDrawValues(false)
        dataSet.highLightColor = Color.RED
        dataSet.colors =
            if (sharedPreferences.getBoolean(PREF_USE_SPORT_GROUP_INSTEAD_OF_TYPE, false)) {
                SportGroup.entries.map { ContextCompat.getColor(requireContext(), it.color) }
            } else {
                SportType.entries.map { ContextCompat.getColor(requireContext(), it.color) }
            }
        dataSet.stackLabels =
            if (sharedPreferences.getBoolean(PREF_USE_SPORT_GROUP_INSTEAD_OF_TYPE, false)) {
                SportGroup.entries.map { getString(it.sportNameStringId) }.toTypedArray()
            } else {
                SportType.entries.map { getString(it.sportNameStringId) }.toTypedArray()
            }

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

    private fun selectedDataSpinner(summits: List<Summit>) {
        binding.barChart.axisLeft?.removeAllLimitLines()
        summits.sortedBy { it.date }
        barChartEntries.clear()
        lineChartEntriesForecast.clear()
        try {
            var annualTarget: Float = sharedPreferences.getString(
                selectedYAxisSpinnerEntry.sharedPreferenceKey,
                selectedYAxisSpinnerEntry.defaultAnnualTarget.toString()
            )?.toFloat()
                ?: selectedYAxisSpinnerEntry.defaultAnnualTarget.toFloat()
            when (selectedXAxisSpinnerEntry) {
                BarChartXAxisSelector.DateByWeek -> {
                    annualTarget /= 52f
                }

                BarChartXAxisSelector.DateByYear -> {
                    if (selectedXAxisSpinnerMonth != 0) {
                        annualTarget /= 12f
                    }
                }

                BarChartXAxisSelector.DateByMonth -> {
                    annualTarget /= 12f
                }

                BarChartXAxisSelector.DateByQuarter -> {
                    annualTarget /= 4f
                }

                else -> {
                    // DO NOTHING
                }
            }
            val line1 = LimitLine(annualTarget)
            binding.barChart.axisLeft?.addLimitLine(line1)
            binding.barChart.setSafeZoneColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.red_50
                ), ContextCompat.getColor(requireContext(), R.color.green_50)
            )
            updateBarChart(summits)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    @Throws(ParseException::class)
    private fun updateBarChart(summits: List<Summit>) {
        setForecastsChart()
        val calender = Calendar.getInstance(TimeZone.getDefault())
        val rangeAndAnnotation = selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper)
        val interval = rangeAndAnnotation.second
        val annotation = rangeAndAnnotation.first
        label = getString(selectedYAxisSpinnerEntry.nameId)
        unit = getString(selectedYAxisSpinnerEntry.unitId)
        for (i in interval.indices) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier {
                selectedXAxisSpinnerEntry.getStream(
                    summits,
                    interval[i],
                    calender,
                    selectedXAxisSpinnerMonth
                )
            }
            val xValue = annotation[i]
            val yValues = getValueForEntry(streamSupplier)
            barChartEntries.add(BarEntry(xValue, yValues))
        }
    }

    private fun setForecastsChart() {
        if (selectedXAxisSpinnerEntry in listOf(
                BarChartXAxisSelector.DateByMonth,
                BarChartXAxisSelector.DateByYear,
                BarChartXAxisSelector.DateByYearUntilToday,
                BarChartXAxisSelector.DateByQuarter
            )
        ) {
            viewModel.forecastList.observe(viewLifecycleOwner) {
                it.data?.let { forecasts ->
                    val ranges =
                        selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper).second
                    xValuesForecast =
                        selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper).first
                    yValuesForecast = ranges.filterIsInstance<ClosedRange<Date>>().map { range ->
                        forecasts.sumOf { forecast ->
                            val date = forecast.getDate()
                            val shouldAddThisMonth =
                                selectedXAxisSpinnerEntry != BarChartXAxisSelector.DateByYear || selectedXAxisSpinnerMonth == 0 || selectedXAxisSpinnerMonth == forecast.month
                            if (date != null && date in range && shouldAddThisMonth) {
                                selectedYAxisSpinnerEntry.getForecastValue(forecast).toInt()
                            } else {
                                0
                            }
                        }
                    }
                    yValuesForecast.forEachIndexed { index, yValue ->
                        lineChartEntriesForecast.add(
                            Entry(xValuesForecast[index] - 0.5f, yValue.toFloat())
                        )
                    }
                }
            }
        }
    }

    private fun getValueForEntry(entriesSupplier: Supplier<Stream<Summit?>?>): FloatArray {
        val list: MutableList<Float> = mutableListOf()
        if (sharedPreferences.getBoolean(PREF_USE_SPORT_GROUP_INSTEAD_OF_TYPE, false)) {
            SportGroup.entries.forEach { sportGroup ->
                list.add(
                    selectedYAxisSpinnerEntry.f(
                        entriesSupplier.get()?.filter { it?.sportType in sportGroup.sportTypes },
                        indoorHeightMeterPercent
                    )
                )
            }
        } else {
            SportType.entries.forEach { sportType ->
                list.add(
                    selectedYAxisSpinnerEntry.f(
                        entriesSupplier.get()?.filter { it?.sportType == sportType },
                        indoorHeightMeterPercent
                    )
                )
            }
        }
        return list.toFloatArray()
    }


    private fun listenOnDataSpinner(summits: List<Summit>) {
        binding.barChartSpinnerData.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                selectedYAxisSpinnerEntry = BarChartYAxisSelector.entries.toTypedArray()[i]
                selectedDataSpinner(summits)
                drawChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        binding.barChartSpinnerXAxis.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                selectedXAxisSpinnerEntry = BarChartXAxisSelector.entries.toTypedArray()[i]
                if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYear) {
                    binding.barChartSpinnerMonth.visibility = View.VISIBLE
                    binding.calender.visibility = View.VISIBLE
                } else {
                    binding.barChartSpinnerMonth.visibility = View.GONE
                    binding.calender.visibility = View.GONE
                }
                selectedDataSpinner(summits)
                drawChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        binding.barChartSpinnerMonth.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                selectedXAxisSpinnerMonth = i
                selectedDataSpinner(summits)
                drawChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val dateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            BarChartYAxisSelector.entries.map { resources.getString(it.nameId) }.toTypedArray()
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.barChartSpinnerData.adapter = dateAdapter
        val xAxisAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            BarChartXAxisSelector.entries.map { resources.getString(it.nameId) }.toTypedArray()
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.barChartSpinnerXAxis.adapter = xAxisAdapter
        val symbols = DateFormatSymbols()
        val monthNames = mutableListOf(getString(R.string.all))
        symbols.shortMonths.forEach { monthNames.add(it) }
        binding.barChartSpinnerMonth.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            monthNames
        )
    }

    inner class CustomMarkerView(context: Context?, layoutResource: Int) :
        MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            try {
                val value: String
                if (e != null && highlight != null) {
                    value =
                        if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYear || selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYearUntilToday) {
                            String.format("%s", e.x.toInt())
                        } else if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByWeek) {
                            val week = (e.x % 52f).toInt()
                            val year = floor(e.x / 52f).toInt() + getYear(minDate)
                            String.format(
                                "%s %s/%s",
                                getString(R.string.calender_wek_abrv),
                                week,
                                year
                            )
                        } else if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByMonth) {
                            val month = (e.x % 12f).toInt()
                            val year = floor((e.x + 1f) / 12f).toInt() + getYear(minDate)
                            String.format(
                                "%s %s",
                                DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month],
                                year
                            )
                        } else if (selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByQuarter) {
                            val quarter = ((e.x + 1f) % 4f).toInt()
                            val year = floor((e.x + 1f) / 4f).toInt() + getYear(minDate)
                            "${toRomanNumerics(quarter)} $year"
                        } else if (selectedXAxisSpinnerEntry.isAQuality) {
                            getFormattedValueForQuantity(e.x)
                        } else {
                            getFormattedValueNormalMarker(e)
                        }
                    val selectedValue = (e as BarEntry).yVals[highlight.stackIndex].toInt()
                    val unitString = if (unit == "") "" else " $unit"
                    if (selectedValue == 0 && xValuesForecast.indexOf(e.x) > -1) {
                        val forecastValue = yValuesForecast[xValuesForecast.indexOf(e.x)]
                        tvContent?.text =
                            if (forecastValue > 0 && (
                                        selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByMonth ||
                                                selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYear) ||
                                selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByYearUntilToday ||
                                selectedXAxisSpinnerEntry == BarChartXAxisSelector.DateByQuarter
                            ) {
                                String.format(
                                    "%s%s\n%s\n%s: %s%s",
                                    e.getY().toInt(),
                                    unitString,
                                    value,
                                    getString(R.string.forecast_abbr),
                                    forecastValue,
                                    unitString
                                )
                            } else {
                                String.format("%s%s\n%s", e.getY().toInt(), unitString, value)
                            }
                    } else {
                        tvContent?.text = String.format(
                            "%s/%s%s\n%s\n%s",
                            selectedValue,
                            e.getY().toInt(),
                            unitString,
                            value,
                            getString(
                                if (sharedPreferences.getBoolean(
                                        PREF_USE_SPORT_GROUP_INSTEAD_OF_TYPE,
                                        false
                                    )
                                ) {
                                    SportGroup.entries[highlight.stackIndex].sportNameStringId
                                } else {
                                    SportType.entries[highlight.stackIndex].sportNameStringId
                                }
                            )
                        )
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

    private fun toRomanNumerics(quarter: Int) = when (quarter) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        else -> "IV"
    }

    private fun getYear(date: Date): Int {
        val calendar: Calendar = GregorianCalendar()
        calendar.time = date
        return calendar[Calendar.YEAR]
    }

    private fun getFormattedValueNormalMarker(e: Entry) = String.format(
        "%s - %s %s",
        (e.x * selectedXAxisSpinnerEntry.stepsSize).toInt(),
        ((e.x + 1) * selectedXAxisSpinnerEntry.stepsSize).toInt(),
        getString(selectedXAxisSpinnerEntry.unitId)
    )

    private fun getFormattedValueForQuantity(value: Float) =
        if (value.toInt() < selectedXAxisSpinnerEntry.getRangeAndAnnotation(
                intervalHelper
            ).second.size
        ) {
            selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper).second[value.toInt()].toString()
        } else {
            ""
        }

}