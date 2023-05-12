package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.content.SharedPreferences
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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentBarChartBinding
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.DateFormatSymbols
import java.text.ParseException
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream
import javax.inject.Inject
import kotlin.math.ceil

@AndroidEntryPoint
class BarChartFragment : Fragment() {

    private lateinit var binding: FragmentBarChartBinding
    private val viewModel: DatabaseViewModel by activityViewModels()

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private var selectedYAxisSpinnerEntry: YAxisSelector = YAxisSelector.Count
    private var indoorHeightMeterPercent = 0
    private var selectedXAxisSpinnerEntry: XAxisSelector = XAxisSelector.DateByMonth
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
        indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        fillDateSpinner()
        binding.apply {
            viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
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
        val yAxisLeft = binding.barChart.axisLeft
        setYAxis(yAxisLeft)
        val yAxisRight = binding.barChart.axisRight
        setYAxis(yAxisRight)
        binding.barChart.data = combinedData
        binding.barChart.setTouchEnabled(true)
        binding.barChart.marker =
            CustomMarkerView(requireContext(), R.layout.marker_graph_bar_chart)
        binding.barChart.setVisibleXRangeMinimum(12f)
        if (selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart != -1f) {
            binding.barChart.setVisibleXRangeMaximum(selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart)
            if (!selectedXAxisSpinnerEntry.isAQuality) {
                binding.barChart.moveViewToX(barChartEntries.size.toFloat() - selectedXAxisSpinnerEntry.maxVisibilityRangeForBarChart)
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
                return if (selectedXAxisSpinnerEntry == XAxisSelector.DateByYear || selectedXAxisSpinnerEntry == XAxisSelector.DateByYearUntilToday) {
                    String.format("%s", value.toInt())
                } else if (selectedXAxisSpinnerEntry == XAxisSelector.DateByWeek) {
                    String.format("%s", value.toInt() % 52)
                } else if (selectedXAxisSpinnerEntry == XAxisSelector.DateByMonth) {
                    String.format(
                        "%s",
                        DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[(value % 12f).toInt()]
                    )
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
        val filteredSummits = sortFilterValues.apply(summits)
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

    private fun setGraphViewBarChart(dataSet: BarDataSet) {
        dataSet.setDrawValues(false)
        dataSet.highLightColor = Color.RED
        dataSet.colors =
            SportType.values().map { ContextCompat.getColor(requireContext(), it.color) }
        dataSet.stackLabels =
            SportType.values().map { getString(it.sportNameStringId) }.toTypedArray()
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
            if (selectedXAxisSpinnerEntry == XAxisSelector.DateByWeek) {
                annualTarget /= 52f
            } else if (selectedXAxisSpinnerEntry == XAxisSelector.DateByMonth) {
                annualTarget /= 12f
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
        val rangeAndAnnotation = selectedXAxisSpinnerEntry.getRangeAndAnnotation(intervalHelper)
        val interval = rangeAndAnnotation.second
        val annotation = rangeAndAnnotation.first
        for (i in interval.indices) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier {
                selectedXAxisSpinnerEntry.getStream(summits, interval[i])
            }
            val xValue = annotation[i]

            label = getString(selectedYAxisSpinnerEntry.nameId)
            unit = getString(selectedYAxisSpinnerEntry.unitId)
            val yValues = getValueForEntry(streamSupplier)
            barChartEntries.add(BarEntry(xValue, yValues))
        }
    }

    private fun setForecastsChart() {
        if (selectedXAxisSpinnerEntry in listOf(
                XAxisSelector.DateByMonth,
                XAxisSelector.DateByYear,
                XAxisSelector.DateByYearUntilToday
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
                            if (date != null && date in range) {
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
        SportType.values().forEach { sportType ->
            list.add(
                selectedYAxisSpinnerEntry.f(
                    entriesSupplier.get()?.filter { it?.sportType == sportType },
                    indoorHeightMeterPercent
                )
            )
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
                selectedYAxisSpinnerEntry = YAxisSelector.values()[i]
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
                selectedXAxisSpinnerEntry = XAxisSelector.values()[i]
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
            YAxisSelector.values().map { resources.getString(it.nameId) }.toTypedArray()
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.barChartSpinnerData.adapter = dateAdapter
        val xAxisAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            XAxisSelector.values().map { resources.getString(it.nameId) }.toTypedArray()
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.barChartSpinnerXAxis.adapter = xAxisAdapter
    }

    inner class CustomMarkerView(context: Context?, layoutResource: Int) :
        MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            try {
                val value: String
                if (e != null && highlight != null) {
                    value =
                        if (selectedXAxisSpinnerEntry == XAxisSelector.DateByYear || selectedXAxisSpinnerEntry == XAxisSelector.DateByYearUntilToday) {
                            String.format("%s", e.x.toInt())
                        } else if (selectedXAxisSpinnerEntry == XAxisSelector.DateByWeek) {
                            val week = (e.x % 52f).toInt()
                            val year = ceil((e.x + 1f) / 52f).toInt() + getYear(minDate)
                            String.format(
                                "%s %s/%s",
                                getString(R.string.calender_wek_abrv),
                                week,
                                year
                            )
                        } else if (selectedXAxisSpinnerEntry == XAxisSelector.DateByMonth) {
                            val month = (e.x % 12f).toInt()
                            val year = ceil((e.x + 1f) / 12f).toInt() + getYear(minDate)
                            String.format(
                                "%s %s",
                                DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month],
                                year
                            )
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
                                        selectedXAxisSpinnerEntry == XAxisSelector.DateByMonth ||
                                                selectedXAxisSpinnerEntry == XAxisSelector.DateByYear) ||
                                selectedXAxisSpinnerEntry == XAxisSelector.DateByYearUntilToday
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
                            SportType.values()[highlight.stackIndex].name
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

    @Suppress("UNCHECKED_CAST")
    enum class XAxisSelector(
        val nameId: Int,
        val unitId: Int,
        val stepsSize: Double,
        val isAQuality: Boolean,
        val maxVisibilityRangeForBarChart: Float,
        val getStream: (entries: List<Summit>?, start: Any) -> Stream<Summit?>?,
        val getRangeAndAnnotation: (IntervalHelper) -> Pair<List<Float>, List<Any>>,
    ) {
        DateByMonth(R.string.monthly, R.string.empty, 0.0, false, 25f, { entries, range ->
            entries
                ?.stream()
                ?.filter { o: Summit ->
                    o.date in (range as ClosedRange<Date>)
                }
        }, { e -> e.dateByMonthRangeAndAnnotation }),
        DateByYear(R.string.yearly, R.string.empty, 0.0, false, 12f, { entries, range ->
            entries
                ?.stream()
                ?.filter { o: Summit ->
                    o.date in (range as ClosedRange<Date>)
                }
        }, { e -> e.dateByYearRangeAndAnnotation }),
        DateByYearUntilToday(
            R.string.yearly_until_today,
            R.string.empty,
            0.0,
            false,
            12f,
            { entries, range ->
                entries
                    ?.stream()
                    ?.filter { o: Summit ->
                        o.date in (range as ClosedRange<Date>)
                    }
            },
            { e -> e.dateByYearUntilTodayRangeAndAnnotation }),
        DateByWeek(R.string.wekly, R.string.empty, 0.0, false, 26f, { entries, range ->
            entries
                ?.stream()
                ?.filter { o: Summit ->
                    o.date in (range as ClosedRange<Date>)
                }
        }, { e -> e.dateByWeekRangeAndAnnotation }),
        Kilometers(
            R.string.kilometers_hint,
            R.string.km,
            IntervalHelper.kilometersStep,
            false,
            -1f,
            { entries, range ->
                entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.kilometers.toFloat() in (range as ClosedRange<Float>) }
            },
            { e -> e.kilometersRangeAndAnnotation }),
        ElevationGain(
            R.string.height_meter_hint,
            R.string.hm,
            IntervalHelper.elevationGainStep,
            false,
            -1f,
            { entries, range ->
                entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.elevationData.elevationGain.toFloat() in range as ClosedRange<Float> }
            },
            { e -> e.elevationGainRangeAndAnnotation }),
        TopElevation(
            R.string.top_elevation_hint,
            R.string.masl,
            IntervalHelper.topElevationStep,
            false,
            -1f,
            { entries, range ->
                entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.elevationData.maxElevation.toFloat() in (range as ClosedRange<Float>) }
            },
            { e -> e.topElevationRangeAndAnnotation }),
        Participants(R.string.participants, R.string.empty, 1.0, true, 12f, { entries, range ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.participants.contains(range) }
        }, { e -> e.participantsRangeAndAnnotationForSummitChipValues }),
        Equipments(R.string.equipments, R.string.empty, 1.0, true, 12f, { entries, range ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.equipments.contains(range) }
        }, { e -> e.equipmentsRangeAndAnnotationForSummitChipValues }),

        Countries(R.string.country_hint, R.string.empty, 1.0, true, 12f, { entries, range ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.countries.contains(range) }
        }, { e -> e.countriesRangeAndAnnotationForSummitChipValues })
    }

    enum class YAxisSelector(
        val nameId: Int,
        val unitId: Int,
        val sharedPreferenceKey: String,
        val defaultAnnualTarget: Int,
        val f: (Stream<Summit?>?, Int) -> Float,
        val getForecastValue: (Forecast) -> Float
    ) {
        Count(R.string.count, R.string.empty, "annual_target_activities", 52, { stream, _ ->
            stream?.count()?.toFloat() ?: 0f
        }, { forecast -> forecast.forecastNumberActivities.toFloat() }),
        Kilometers(R.string.kilometers_hint, R.string.km, "annual_target_km", 1200, { stream, _ ->
            stream
                ?.mapToInt { o: Summit? -> o?.kilometers?.toInt() ?: 0 }
                ?.sum()?.toFloat() ?: 0.0f
        }, { forecast -> forecast.forecastDistance.toFloat() }),
        ElevationGain(
            R.string.height_meter_hint,
            R.string.hm,
            "annual_target",
            50000,
            { stream, indoorHeightMeterPercent ->
                stream
                    ?.mapToInt {
                        if (it?.sportType == SportType.IndoorTrainer) {
                            it.elevationData.elevationGain * indoorHeightMeterPercent / 100
                        } else {
                            it?.elevationData?.elevationGain ?: 0
                        }
                    }
                    ?.sum()?.toFloat() ?: 0.0f
            },
            { forecast -> forecast.forecastHeightMeter.toFloat() })

    }

}