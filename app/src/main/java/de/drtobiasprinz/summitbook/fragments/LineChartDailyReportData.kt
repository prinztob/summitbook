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
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentLineChartBinding
import de.drtobiasprinz.summitbook.db.entities.DailyReportData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.CustomLineChartWithMarker
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerViewDailyReportData
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.SimpleDateFormat
import java.util.Date

@AndroidEntryPoint
class LineChartDailyReportData : Fragment() {
    private val viewModel: DatabaseViewModel by activityViewModels()

    private lateinit var binding: FragmentLineChartBinding
    private var intervalHelper: IntervalHelper? = null

    private var lineChartEntriesBatteryGain: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesSolarExposure: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesActivities: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesActiveDuration: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesModerateIntensityMinutes: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesVigorousIntensityMinutes: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesSteps: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesFloorsClimbed: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesRestingHeartRate: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesMinHeartRate: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesMaxHeartRate: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesSleepData: MutableList<Entry?> = mutableListOf()
    private var lineChartXAxisSpinner: XAxisSelector = XAxisSelector.Days7
    private var lineChartYAxisSpinner: YAxisSelector = YAxisSelector.SolarIntensity
    private var lineChartColors: List<Int>? = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLineChartBinding.inflate(layoutInflater, container, false)

        fillDateSpinner()
        viewModel.dailyReportDataList.observe(viewLifecycleOwner) { dailyReportDataStatus ->
            dailyReportDataStatus.data.let { dailyReportData ->
                viewModel.summitsList.observe(viewLifecycleOwner) { summitListDataStatus ->
                    summitListDataStatus.data.let { summits ->
                        if (summits != null) {
                            intervalHelper = IntervalHelper(
                                summits,
                                dailyReportData?.minBy { it.date }?.date,
                                dailyReportData?.maxBy { it.date }?.date
                            )
                            val numberActivitiesPerDay =
                                summits.groupingBy { it.getDateAsString() }.eachCount()
                            dailyReportData?.forEach {
                                it.activitiesOnDay =
                                    if (numberActivitiesPerDay.keys.contains(it.getDateAsString())) numberActivitiesPerDay[it.getDateAsString()]
                                        ?: 0 else 0
                            }
                        }
                        resizeChart()
                        if (dailyReportData != null) {
                            listenOnDataSpinner(dailyReportData)
                            drawLineChart(dailyReportData)
                        }
                    }
                }
            }
        }
        return binding.root
    }

    private fun resizeChart() {
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
        binding.lineChart.minimumHeight =
            (Resources.getSystem().displayMetrics.heightPixels * 0.7).toInt()
    }

    private fun drawLineChart(dailyReportData: List<DailyReportData>) {
        setLineChartEntries(dailyReportData)
        val dataSets: MutableList<ILineDataSet?> = ArrayList()
        val legends = mutableListOf<LegendEntry>()
        if (lineChartYAxisSpinner == YAxisSelector.SolarIntensity) {
            legends.add(
                addData(
                    lineChartEntriesBatteryGain,
                    dataSets,
                    Color.RED,
                    getString(R.string.solar_50000_lux_condition)
                )
            )
            legends.add(
                addData(
                    lineChartEntriesSolarExposure,
                    dataSets,
                    Color.YELLOW,
                    getString(R.string.solar_exposure)
                )
            )

            val dataSetActivities = LineDataSet(
                lineChartEntriesActivities,
                resources.getString(lineChartXAxisSpinner.nameId)
            )
            setGraphViewActivities(dataSetActivities)
            dataSets.add(dataSetActivities)
            legends.add(
                LegendEntry(
                    getString(R.string.activity_hint),
                    Legend.LegendForm.CIRCLE,
                    9f,
                    5f,
                    null,
                    Color.DKGRAY
                )
            )
        }
        if (lineChartYAxisSpinner == YAxisSelector.ActivityMinutes) {
            legends.add(
                addData(
                    lineChartEntriesActiveDuration,
                    dataSets,
                    Color.GREEN,
                    getString(R.string.active_duration)
                )
            )
            legends.add(
                addData(
                    lineChartEntriesModerateIntensityMinutes,
                    dataSets,
                    Color.BLUE,
                    getString(R.string.moderate_intensity_minutes)
                )
            )
            legends.add(
                addData(
                    lineChartEntriesVigorousIntensityMinutes,
                    dataSets,
                    Color.RED,
                    getString(R.string.vigorous_intensity_minutes)
                )
            )
        }
        if (lineChartYAxisSpinner == YAxisSelector.Steps) {
            legends.add(
                addData(
                    lineChartEntriesSteps,
                    dataSets,
                    Color.GREEN,
                    getString(R.string.steps)
                )
            )
        }
        if (lineChartYAxisSpinner == YAxisSelector.FloorsClimbed) {
            legends.add(
                addData(
                    lineChartEntriesFloorsClimbed,
                    dataSets,
                    Color.GREEN,
                    getString(R.string.floors_climbed)
                )
            )
        }
        if (lineChartYAxisSpinner == YAxisSelector.SleepHours) {
            legends.add(
                addData(
                    lineChartEntriesSleepData,
                    dataSets,
                    Color.GREEN,
                    getString(R.string.sleep_hours)
                )
            )
        }

        if (lineChartYAxisSpinner == YAxisSelector.HeartRate) {
            legends.add(
                addData(
                    lineChartEntriesRestingHeartRate,
                    dataSets,
                    Color.BLUE,
                    getString(R.string.resting_heart_rate)
                )
            )
            legends.add(
                addData(
                    lineChartEntriesMinHeartRate,
                    dataSets,
                    Color.RED,
                    getString(R.string.min_heart_rate)
                )
            )
            legends.add(
                addData(
                    lineChartEntriesMaxHeartRate,
                    dataSets,
                    Color.GREEN,
                    getString(R.string.max_heart_rate)
                )
            )
        }
        setLegend(binding.lineChart, legends)
        binding.lineChart.data = LineData(dataSets)

        binding.lineChart.setVisibleXRangeMaximum(12f) // allow 12 values to be displayed at once on the x-axis, not more
        binding.lineChart.moveViewToX(lineChartEntriesBatteryGain.size.toFloat() - 12f)
        setXAxis()
        setYAxis(binding.lineChart.axisLeft)
        setYAxis(binding.lineChart.axisRight)
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.marker =
            CustomMarkerViewDailyReportData(
                requireContext(),
                R.layout.marker_graph_daily_report_data,
                lineChartYAxisSpinner
            )
        binding.lineChart.invalidate()
    }

    private fun addData(
        data: MutableList<Entry?>,
        dataSets: MutableList<ILineDataSet?>,
        color: Int,
        name: String
    ): LegendEntry {
        val dataSetActiveDuration = LineDataSet(
            data,
            getString(R.string.active_duration)
        )
        setGraphView(dataSetActiveDuration, color)
        dataSets.add(dataSetActiveDuration)
        return LegendEntry(
            name,
            Legend.LegendForm.LINE,
            9f,
            5f,
            null,
            color
        )
    }

    private fun resetChart() {
        binding.lineChart.fitScreen()
        binding.lineChart.data?.clearValues()
        binding.lineChart.xAxis?.valueFormatter = null
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.clear()
        binding.lineChart.invalidate()
    }

    private fun setLineChartEntries(dailyReportData: List<DailyReportData>) {
        val data = intervalHelper?.let { lineChartXAxisSpinner.filterData(it, dailyReportData) }
        if (data != null) {
            lineChartEntriesBatteryGain = data.mapIndexed { index, entry ->
                Entry(index.toFloat(), (entry.solarUtilizationInHours).toFloat(), entry)
            }.toMutableList()
            lineChartEntriesSolarExposure = data.mapIndexed { index, entry ->
                Entry(index.toFloat(), (entry.solarExposureInHours).toFloat(), entry)
            }.toMutableList()
            lineChartEntriesActivities = data.mapIndexed { index, entry ->
                Entry(index.toFloat() - 0.5F, entry.activitiesOnDay.toFloat(), entry)
            }.toMutableList()
            lineChartEntriesActiveDuration = data.mapIndexed { index, entry ->
                Entry(index.toFloat(), entry.events.sumOf { it.duration / 60.0 }.toFloat(), entry)
            }.toMutableList()
            lineChartEntriesModerateIntensityMinutes = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.events.sumOf { it.moderateIntensityMinutes / 60.0 }.toFloat(),
                    entry
                )
            }.toMutableList()
            lineChartEntriesVigorousIntensityMinutes = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.events.sumOf { it.vigorousIntensityMinutes / 60.0 }.toFloat(),
                    entry
                )
            }.toMutableList()
            lineChartEntriesSteps = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.steps.toFloat(),
                    entry
                )
            }.toMutableList()
            lineChartEntriesFloorsClimbed = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.floorsClimbed.toFloat(),
                    entry
                )
            }.toMutableList()
            lineChartEntriesRestingHeartRate = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.restingHeartRate.toFloat(),
                    entry
                )
            }.toMutableList()
            lineChartEntriesMinHeartRate = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.minHeartRate.toFloat(),
                    entry
                )
            }.toMutableList()
            lineChartEntriesMaxHeartRate = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.maxHeartRate.toFloat(),
                    entry
                )
            }.toMutableList()
            lineChartEntriesSleepData = data.mapIndexed { index, entry ->
                Entry(
                    index.toFloat(),
                    entry.sleepHours.toFloat(),
                    entry
                )
            }.toMutableList()
            val lastEntry = lineChartEntriesActivities.lastOrNull()
            if (lastEntry != null) {
                lineChartEntriesActivities.add(Entry(lastEntry.x + 1f, lastEntry.y))
            }
            lineChartColors = data.map { if (it.isForWholeDay) Color.BLUE else Color.RED }
        }
    }

    private fun setXAxis() {
        val xAxis = binding.lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return SimpleDateFormat(
                    Summit.DATE_FORMAT,
                    requireContext().resources.configuration.locales[0]
                )
                    .format(Summit.getDateFromFloat(value))
            }
        }
    }

    private fun setYAxis(yAxis: YAxis?) {
        yAxis?.textSize = 12f
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(
                    requireContext().resources.configuration.locales[0],
                    "%s %s",
                    value.toInt().toString(),
                    lineChartYAxisSpinner.unit
                )
            }
        }
    }

    private fun setGraphViewActivities(dataSet: LineDataSet) {
        dataSet.setDrawCircles(false)
        dataSet.valueTextSize = 18f
        dataSet.setDrawValues(false)
        dataSet.isHighlightEnabled = false
        dataSet.color = Color.DKGRAY
        dataSet.lineWidth = 2f
        dataSet.mode = LineDataSet.Mode.STEPPED
    }

    private fun setGraphView(dataSet: LineDataSet, color: Int) {
        dataSet.setDrawValues(false)
        dataSet.color = color
        dataSet.circleColors = lineChartColors
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 7f
        dataSet.circleHoleRadius = 4f
        dataSet.circleHoleColor = color
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet.cubicIntensity = 0.1f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = color
        dataSet.fillAlpha = 20
    }

    private fun setLegend(
        lineChart: CustomLineChartWithMarker,
        additionalLegendEntries: List<LegendEntry>
    ) {
        val legend = lineChart.legend
        legend.entries
        legend.yEntrySpace = 10f
        legend.isWordWrapEnabled = true
        val legends = mutableListOf(
            LegendEntry(
                getString(R.string.part_day),
                Legend.LegendForm.CIRCLE,
                9f,
                5f,
                null,
                Color.RED
            ),
            LegendEntry(
                getString(R.string.whole_day),
                Legend.LegendForm.CIRCLE,
                9f,
                5f,
                null,
                Color.BLUE
            )
        )
        legends.addAll(additionalLegendEntries)
        legend.setCustom(legends)
        legend.isEnabled = true
    }

    private fun listenOnDataSpinner(dailyReportData: List<DailyReportData>) {
        binding.spinnerXAxis.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                lineChartXAxisSpinner = XAxisSelector.values()[i]
                resetChart()
                drawLineChart(dailyReportData)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        binding.spinnerYAxis.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                lineChartYAxisSpinner = YAxisSelector.values()[i]
                resetChart()
                drawLineChart(dailyReportData)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val xAxisAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            XAxisSelector.values().map { resources.getString(it.nameId) }.toTypedArray()
        )
        xAxisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerXAxis.adapter = xAxisAdapter

        val yAxisAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            YAxisSelector.values().map { resources.getString(it.nameId) }.toTypedArray()
        )
        yAxisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYAxis.adapter = yAxisAdapter
    }

    companion object {

        fun filterDailyReportDataPerMonth(
            dailyReportData: List<DailyReportData>,
            intervalHelper: IntervalHelper
        ): List<DailyReportData> {
            val minDate = dailyReportData.minBy { it.date }.date
            return intervalHelper.dateByMonthRangeAndAnnotationUntilToday.second.filter { it.start >= minDate }
                .map { range ->
                    filter(dailyReportData, range)
                }
        }

        fun filterDailyReportDataPerQuarter(
            dailyReportData: List<DailyReportData>,
            intervalHelper: IntervalHelper
        ): List<DailyReportData> {
            return intervalHelper.dateByQuarterRangeAndAnnotation.second.map { range ->
                filter(dailyReportData, range)
            }
        }

        fun filterDailyReportDataPerWeek(
            dailyReportData: List<DailyReportData>,
            intervalHelper: IntervalHelper
        ): List<DailyReportData> {
            val minDate = dailyReportData.minBy { it.date }.date
            return intervalHelper.dateByWeekRangeAndAnnotation.second.filter { it.start >= minDate }
                .map { range ->
                    filter(dailyReportData, range)
                }
        }

        private fun filter(
            dailyReportData: List<DailyReportData>,
            range: ClosedRange<Date>
        ): DailyReportData {
            val relevantData = dailyReportData.filter { data -> data.date in range }
            return DailyReportData(
                0,
                range.start,
                relevantData.sumOf { it.solarUtilizationInHours },
                relevantData.sumOf { it.solarExposureInHours },
                relevantData.sumOf { it.steps },
                relevantData.sumOf { it.floorsClimbed },
                relevantData.flatMap { it.events },
                true,
                relevantData.sumOf { it.totalIntensityMinutes },
                relevantData.sumOf { it.restingHeartRate } / relevantData.size,
                relevantData.minOf { it.minHeartRate },
                relevantData.maxOf { it.maxHeartRate },
                relevantData.sumOf { it.heartRateVariability } / relevantData.size,
                relevantData.sumOf { it.sleepHours } / relevantData.size,
            )
        }
    }

}

enum class YAxisSelector(
    val nameId: Int,
    val unit: String,
    val getStringForCustomMarker: (DailyReportData, Context) -> String
) {
    SolarIntensity(R.string.solar_intensity, "h", { entry, context ->
        String.format(
            "%s\n%s: %.2f %s\n%s: %.2f %s",
            entry.markerText,
            context.getString(R.string.solar_50000_lux_condition),
            entry.solarUtilizationInHours,
            "h",
            context.getString(R.string.solar_exposure),
            entry.solarExposureInHours,
            "h"
        )
    }),
    ActivityMinutes(R.string.active_duration, "h", { entry, context ->
        String.format(
            "%s\n%s: %s %s\n%s: %s %s\n%s: %s %s",
            entry.markerText,
            context.getString(R.string.active_duration),
            entry.totalIntensityMinutes,
            context.getString(R.string.min),
            context.getString(R.string.moderate_intensity_minutes),
            entry.events.sumOf { it.moderateIntensityMinutes },
            context.getString(R.string.min),
            context.getString(R.string.vigorous_intensity_minutes),
            entry.events.sumOf { it.vigorousIntensityMinutes },
            context.getString(R.string.min),
        )
    }),
    Steps(R.string.steps, "", { entry, context ->
        String.format(
            "%s\n%s: %s",
            entry.markerText,
            context.getString(R.string.steps),
            entry.steps
        )
    }),
    FloorsClimbed(R.string.floors_climbed, "", { entry, context ->
        String.format(
            "%s\n%s: %s",
            entry.markerText,
            context.getString(R.string.floors_climbed),
            entry.floorsClimbed
        )
    }),
    HeartRate(R.string.heart_rate, "bpm", { entry, context ->
        String.format(
            "%s\n%s: %s %s\n%s: %s %s\n%s: %s %s",
            entry.markerText,
            context.getString(R.string.min_heart_rate),
            entry.minHeartRate,
            context.getString(R.string.bpm),
            context.getString(R.string.resting_heart_rate),
            entry.restingHeartRate,
            context.getString(R.string.bpm),
            context.getString(R.string.max_heart_rate),
            entry.maxHeartRate,
            context.getString(R.string.bpm),
        )
    }),
    SleepHours(R.string.sleep_hours, "h", { entry, context ->
        String.format(
            "%s\n%s: %s",
            entry.markerText,
            context.getString(R.string.sleep_hours),
            entry.sleepHours
        )
    }),
}

enum class XAxisSelector(
    val nameId: Int,
    val filterData: (IntervalHelper, List<DailyReportData>) -> List<DailyReportData>?
) {
    Days7(R.string.seven_days, { _, dailyReportData ->
        dailyReportData.sortedBy { it.date }.takeLast(7)
    }),
    Days28(R.string.twenty_eight_days, { _, dailyReportData ->
        dailyReportData.sortedBy { it.date }.takeLast(28)
    }),
    Days(R.string.days, { _, dailyReportData ->
        dailyReportData.sortedBy { it.date }
    }),
    Weeks12(R.string.twelf_weeks, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerWeek(
            dailyReportData,
            intervalHelper
        ).takeLast(12)
    }),
    Weeks(R.string.weeks, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerWeek(
            dailyReportData,
            intervalHelper
        )
    }),
    Months(R.string.months, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerMonth(
            dailyReportData,
            intervalHelper
        )
    }),
    Quarterly(R.string.quarterly, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerQuarter(
            dailyReportData,
            intervalHelper
        )
    });
}
