package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentOverviewBinding
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.ui.GraphType
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.sharedPreferences
import de.drtobiasprinz.summitbook.ui.PerformanceGraphProvider
import de.drtobiasprinz.summitbook.ui.utils.CustomLineChartWithMarker
import de.drtobiasprinz.summitbook.ui.utils.MyFillFormatter
import de.drtobiasprinz.summitbook.ui.utils.MyLineLegendRenderer
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class OverviewFragment : Fragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private lateinit var binding: FragmentOverviewBinding
    private val viewModel: DatabaseViewModel by activityViewModels()
    private lateinit var performanceGraphProvider: PerformanceGraphProvider
    private var indoorHeightMeterPercent: Int = 0

    private var selectedGraphType = GraphType.ElevationGain
    private lateinit var numberFormat: NumberFormat

    private var graphIsVisible: Boolean = false
    private var currentMonth: Int = 0
    private var currentYear: Int = 0
    private var selectedYear: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOverviewBinding.inflate(layoutInflater, container, false)
        setDropDown()
        indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
        numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])

        viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
            itData.data?.let { summits ->
                setOverviewText(
                    sortFilterValues.apply(summits, sharedPreferences)
                )
                viewModel.forecastList.observe(viewLifecycleOwner) { itDataForeCasts ->
                    itDataForeCasts.data?.let { forecasts ->
                        setOverviewChart(summits, forecasts)
                    }
                }
            }
        }
        return binding.root
    }


    private fun setOverviewChart(
        summits: List<Summit>,
        forecasts: List<Forecast>
    ) {
        performanceGraphProvider =
            PerformanceGraphProvider(summits, forecasts, indoorHeightMeterPercent)
        drawPerformanceGraph(selectedGraphType)
        var showMonths = true
        var showYears = true
        binding.showMonthButton.setOnClickListener {
            showMonths = !showMonths
            updateLayoutOfCharts(showMonths, showYears)
        }
        binding.showYearButton.setOnClickListener {
            showYears = !showYears
            updateLayoutOfCharts(showMonths, showYears)
        }
        binding.refreshMonth.setOnClickListener {
            binding.lineChartMonth.fitScreen()
        }
        binding.refreshYear.setOnClickListener {
            binding.lineChartYear.fitScreen()
        }
        binding.overviewLayout.setOnClickListener {
            if (!graphIsVisible) {
                when (resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> binding.dopDown.setImageResource(
                        R.drawable.baseline_arrow_drop_up_white_24dp
                    )

                    Configuration.UI_MODE_NIGHT_NO -> binding.dopDown.setImageResource(
                        R.drawable.baseline_arrow_drop_up_black_24dp
                    )

                    else -> binding.dopDown.setImageResource(R.drawable.baseline_arrow_drop_up_white_24dp)
                }
                binding.chartLayout.visibility = View.VISIBLE
                binding.groupProperty.addOnButtonCheckedListener { _, checkedId, isChecked ->
                    binding.lineChartMonth.clear()
                    binding.lineChartYear.clear()
                    if (isChecked) {
                        selectedGraphType = when (checkedId) {
                            binding.buttonKilometers.id -> {
                                GraphType.Kilometer
                            }

                            binding.buttonActivity.id -> {
                                GraphType.Count
                            }

                            binding.buttonVo2max.id -> {
                                GraphType.Vo2Max
                            }

                            binding.buttonPower.id -> {
                                GraphType.Power
                            }

                            else -> {
                                GraphType.ElevationGain
                            }
                        }
                        drawPerformanceGraph(
                            selectedGraphType
                        )
                    }
                }
            } else {
                binding.chartLayout.visibility = View.GONE
                setDropDown()
            }
            graphIsVisible = !graphIsVisible
        }
    }

    private fun updateLayoutOfCharts(showMonths: Boolean, showYears: Boolean) {
        if (showMonths && showYears) {
            binding.lineChartMonth.visibility = View.VISIBLE
            binding.lineChartYear.visibility = View.VISIBLE
            setHeight(0.22, binding.lineChartMonth)
            setHeight(0.22, binding.lineChartYear)
        } else if (!showMonths && showYears) {
            binding.lineChartMonth.visibility = View.GONE
            binding.lineChartYear.visibility = View.VISIBLE
            setHeight(0.44, binding.lineChartYear)
        } else if (showMonths) {
            binding.lineChartMonth.visibility = View.VISIBLE
            binding.lineChartYear.visibility = View.GONE
            setHeight(0.44, binding.lineChartMonth)
        } else {
            binding.lineChartMonth.visibility = View.GONE
            binding.lineChartYear.visibility = View.GONE
        }
    }

    private fun setHeight(height: Double, chart: LineChart) {
        val params = chart.layoutParams
        params.height = (Resources.getSystem().displayMetrics.heightPixels * height).toInt()
        params.width = Resources.getSystem().displayMetrics.widthPixels
        chart.layoutParams = params
    }

    private fun setDropDown() {
        when (resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> binding.dopDown.setImageResource(
                R.drawable.baseline_arrow_drop_down_white_24dp
            )

            Configuration.UI_MODE_NIGHT_NO -> binding.dopDown.setImageResource(
                R.drawable.baseline_arrow_drop_down_24
            )

            else -> binding.dopDown.setImageResource(R.drawable.baseline_arrow_drop_down_white_24dp)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawPerformanceGraph(
        graphType: GraphType
    ) {
        currentYear = Calendar.getInstance()[Calendar.YEAR]
        currentMonth = Calendar.getInstance()[Calendar.MONTH] + 1
        selectedYear =
            if (sortFilterValues.getSelectedYear() != "") sortFilterValues.getSelectedYear()
                .toInt() else currentYear
        binding.textYear.text =
            String.format(resources.configuration.locales[0], "%s", selectedYear)
        drawChart(binding.lineChartYear, graphType, selectedYear.toString())
        binding.textYear.setOnTouchListener(View.OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val compounds = binding.textYear.compoundDrawables
                if (event.rawX >= binding.textYear.right - compounds[2].bounds.width() && selectedYear < (sortFilterValues.years.maxOfOrNull { it.toInt() }
                        ?: 0)) {
                    selectedYear += 1
                    binding.textYear.text =
                        String.format(resources.configuration.locales[0], "%s", selectedYear)
                    drawChart(binding.lineChartYear, graphType, selectedYear.toString())
                    return@OnTouchListener true
                }
                if (event.rawX <= binding.textYear.left + compounds[0].bounds.width() && selectedYear > (sortFilterValues.years.minOfOrNull { it.toInt() }
                        ?: 0)) {
                    selectedYear -= 1
                    binding.textYear.text =
                        String.format(resources.configuration.locales[0], "%s", selectedYear)
                    drawChart(binding.lineChartYear, graphType, selectedYear.toString())
                    return@OnTouchListener true
                }
            }
            true
        })
        if (currentYear == selectedYear) {
            drawMonthChart(currentMonth, graphType, selectedYear)
            binding.textMonth.setOnTouchListener(View.OnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val compounds = binding.textMonth.compoundDrawables
                    if (event.rawX >= binding.textMonth.right - compounds[2].bounds.width() && currentMonth < 12) {
                        currentMonth += 1
                        drawMonthChart(currentMonth, graphType, selectedYear)
                        return@OnTouchListener true
                    }
                    if (event.rawX <= binding.textMonth.left + compounds[0].bounds.width() && currentMonth > 1) {
                        currentMonth -= 1
                        drawMonthChart(currentMonth, graphType, selectedYear)
                        return@OnTouchListener true
                    }
                }
                true
            })
        } else {
            binding.lineChartMonth.visibility = View.GONE
            binding.textMonth.visibility = View.GONE
        }
    }

    private fun drawMonthChart(
        currentMonth: Int, graphType: GraphType, selectedYear: Int
    ) {
        binding.lineChartMonth.visibility = View.VISIBLE
        binding.textMonth.visibility = View.VISIBLE
        val textMonth = "${DateFormatSymbols().months[currentMonth - 1]} $selectedYear"
        binding.textMonth.text = textMonth
        drawChart(
            binding.lineChartMonth,
            graphType,
            selectedYear.toString(),
            if (currentMonth < 10) "0${currentMonth}" else currentMonth.toString()
        )
    }

    private fun drawChart(
        lineChart: CustomLineChartWithMarker,
        graphType: GraphType,
        year: String,
        month: String? = null
    ) {
        var chartEntries: List<Entry>
        var chartEntriesForecast: List<Entry>
        var minMax: Pair<List<Entry>, List<Entry>>
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                chartEntries = performanceGraphProvider.getActualGraphForSummits(
                    graphType,
                    year,
                    month,
                    if (
                        year == (Calendar.getInstance())[Calendar.YEAR].toString()
                        && (month == null || (month.toInt() == (Calendar.getInstance())[Calendar.MONTH] + 1))
                    ) {
                        Date()
                    } else {
                        null
                    }
                )
                chartEntriesForecast =
                    performanceGraphProvider.getForecastGraphForSummits(
                        graphType,
                        year,
                        month,
                        allDays = true
                    )
                minMax =
                    performanceGraphProvider.getActualGraphMinMaxForSummits(graphType, year, month)
            }
            lineChart.invalidate()
            lineChart.axisRight.setDrawLabels(false)
            setYAxis(lineChart.axisLeft, graphType)
            setXAxis(lineChart.xAxis, year, month)
            if (graphType.cumulative) {
                lineChart.axisLeft.axisMinimum = 0f
                lineChart.axisRight.axisMinimum = 0f
            } else {
                lineChart.axisLeft.axisMinimum =
                    (minMax.second + chartEntries).filter { it.y > 0 }.minOf { it.y }
                lineChart.axisRight.axisMinimum =
                    (minMax.second + chartEntries).filter { it.y > 0 }.minOf { it.y }
            }

            setHeight(0.22, lineChart)

            val dataSets: MutableList<ILineDataSet?> = ArrayList()
            if (chartEntries.isNotEmpty() && chartEntries[0].x != 1f) {
                chartEntries = listOf(Entry(1f, 0f)) + chartEntries
            }
            val entries =
                if (graphType.filterZeroValues) chartEntries.filter { it.y > 0f } else chartEntries
            val dataSet = LineDataSet(entries, getString(R.string.actually))
            setGraphView(
                dataSet,
                false,
                lineWidth = 5f,
                colors = entries.map { e ->
                    if (e.y > (minMax.second.firstOrNull { it.x == e.x }?.y ?: 0f)) {
                        Color.rgb(255, 215, 0)
                    } else if (
                        graphType.hasForecast &&
                        e.y > (chartEntriesForecast.firstOrNull { it.x == e.x }?.y ?: 0f)
                    ) {
                        Color.GREEN
                    } else {
                        Color.RED
                    }
                })

            if (minMax.first.isNotEmpty() && minMax.second.isNotEmpty()) {
                lineChart.axisLeft.axisMaximum =
                    (minMax.second + chartEntries).maxOf { it.y } + graphType.delta
                lineChart.axisRight.axisMaximum =
                    (minMax.second + chartEntries).maxOf { it.y } + graphType.delta
                val dataSetMaximalValues =
                    LineDataSet(if (graphType.filterZeroValues) minMax.second.filter { it.y > 0f } else minMax.second,
                        getString(R.string.max_5_yrs))
                if (graphType.cumulative) {
                    val dataSetMinimalValues = LineDataSet(
                        minMax.first, getString(R.string.min_5_yrs)
                    )
                    setGraphView(dataSetMinimalValues)
                    dataSets.add(dataSetMinimalValues)
                    dataSetMaximalValues.fillFormatter = MyFillFormatter(dataSetMinimalValues)
                    lineChart.renderer = MyLineLegendRenderer(
                        lineChart, lineChart.animator, lineChart.viewPortHandler
                    )
                }
                setGraphView(dataSetMaximalValues)
                dataSets.add(dataSetMaximalValues)
            }
            if (graphType.hasForecast) {
                val dataSetForecast =
                    LineDataSet(chartEntriesForecast, getString(R.string.forecast))
                setGraphView(dataSetForecast, false, color = Color.rgb(255, 0, 0))
                dataSets.add(dataSetForecast)
            }
            dataSets.add(dataSet)

            when (resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    lineChart.xAxis.textColor = Color.WHITE
                    lineChart.axisRight.textColor = Color.WHITE
                    lineChart.axisLeft.textColor = Color.WHITE
                    lineChart.legend?.textColor = Color.WHITE
                }

                Configuration.UI_MODE_NIGHT_NO -> {
                    lineChart.xAxis.textColor = Color.BLACK
                    lineChart.axisRight.textColor = Color.BLACK
                    lineChart.axisLeft.textColor = Color.BLACK
                    lineChart.legend?.textColor = Color.BLACK
                }

                Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    lineChart.xAxis.textColor = Color.WHITE
                    lineChart.axisRight.textColor = Color.WHITE
                    lineChart.axisLeft.textColor = Color.WHITE
                    lineChart.legend?.textColor = Color.WHITE
                }
            }

            lineChart.setTouchEnabled(true)
            lineChart.data = LineData(dataSets)
            setLegend(lineChart)
        }
    }


    private fun setLegend(lineChart: LineChart) {
        val l: Legend = lineChart.legend
        l.entries
        l.yEntrySpace = 10f
        l.isWordWrapEnabled = true
        val l1 = LegendEntry(
            getString(R.string.new_record),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.rgb(255, 215, 0)
        )
        val l2 = LegendEntry(
            getString(R.string.better_then),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.GREEN
        )
        val l3 = LegendEntry(
            getString(R.string.forecast),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.RED
        )
        val l4 = LegendEntry(
            getString(R.string.min_max_5_yrs),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            Color.BLUE
        )
        l.setCustom(arrayOf(l1, l2, l3, l4))
        l.isEnabled = true
    }


    private fun setYAxis(yAxis: YAxis?, graphType: GraphType) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val format = "${numberFormat.format(value.toDouble())} ${graphType.unit}"
                return String.format(
                    resources.configuration.locales[0], format, value, graphType.unit
                )
            }
        }
    }

    private fun setXAxis(xAxis: XAxis, year: String? = null, month: String? = null) {
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                val cal = Calendar.getInstance()
                cal.time =
                    PerformanceGraphProvider.parseDate(String.format("${year}-${month ?: "01"}-01 00:00:00"))
                if (month == null) {
                    cal.set(Calendar.DAY_OF_YEAR, value.toInt())
                } else {
                    cal.set(Calendar.DAY_OF_MONTH, value.toInt())
                }
                return SimpleDateFormat(
                    "dd MMM", resources.configuration.locales[0]
                ).format(cal.time)
            }
        }
    }


    private fun setOverviewText(summits: List<Summit>) {
        val numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        numberFormat.maximumFractionDigits = 0
        val statisticEntry = StatisticEntry(summits, indoorHeightMeterPercent)
        statisticEntry.calculate()
        val peaks = summits.filter { it.isPeak }
        binding.overview.text = getString(
            R.string.base_info_activities,
            numberFormat.format(summits.size),
            numberFormat.format(statisticEntry.totalKm),
            numberFormat.format(statisticEntry.totalHm)
        )
        binding.overviewSummits.text = getString(
            R.string.base_info_summits,
            numberFormat.format(peaks.size),
            numberFormat.format(peaks.sumOf { it.kilometers }),
            numberFormat.format(peaks.sumOf { it.elevationData.elevationGain })
        )
    }

    private fun setGraphView(
        set1: LineDataSet?,
        filled: Boolean = true,
        color: Int = Color.BLUE,
        lineWidth: Float = 2f,
        colors: List<Int>? = null
    ) {
        set1?.mode = LineDataSet.Mode.LINEAR
        set1?.setDrawValues(false)
        set1?.setDrawCircles(false)
        if (filled) {
            set1?.cubicIntensity = 20f
            if (colors != null && colors.size == set1?.entryCount) {
                set1.colors = colors
            } else {
                set1?.color = color
            }
            set1?.highLightColor = Color.rgb(244, 117, 117)
            set1?.setDrawFilled(true)
            set1?.fillColor = color
            set1?.fillAlpha = 100
        } else {
            set1?.lineWidth = lineWidth
            set1?.setCircleColor(Color.BLACK)
            if (colors != null && colors.size == set1?.entryCount) {
                set1.colors = colors
            } else {
                set1?.color = color
            }
            set1?.setDrawFilled(false)
        }
        set1?.setDrawHorizontalHighlightIndicator(true)
    }
}