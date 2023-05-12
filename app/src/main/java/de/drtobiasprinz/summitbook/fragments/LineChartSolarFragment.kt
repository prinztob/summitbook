package de.drtobiasprinz.summitbook.fragments

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
import de.drtobiasprinz.summitbook.adapter.SummitsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentLineChartBinding
import de.drtobiasprinz.summitbook.db.entities.SolarIntensity
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerLineChart
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerViewSolarIntensity
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LineChartSolarFragment : Fragment() {
    private val viewModel: DatabaseViewModel by activityViewModels()

    @Inject
    lateinit var summitsAdapter: SummitsAdapter
    private lateinit var binding: FragmentLineChartBinding

    private var lineChartEntriesBatteryGain: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesSolarExposure: MutableList<Entry?> = mutableListOf()
    private var lineChartEntriesActivities: MutableList<Entry?> = mutableListOf()
    private lateinit var summits: List<Summit>
    private var lineChartSpinnerEntry: XAxisSelector = XAxisSelector.Days
    private var lineChartColors: List<Int>? = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLineChartBinding.inflate(layoutInflater, container, false)

        fillDateSpinner()
        summits = summitsAdapter.differ.currentList
        viewModel.solarIntensitiesList.observe(viewLifecycleOwner) { solarListDataStatus ->
            solarListDataStatus.data.let { solarIntensities ->
                val numberActivitiesPerDay = summits.groupingBy { it.getDateAsString() }.eachCount()
                solarIntensities?.forEach {
                    it.activitiesOnDay =
                        if (numberActivitiesPerDay.keys.contains(it.getDateAsString())) numberActivitiesPerDay[it.getDateAsString()]
                            ?: 0 else 0
                }
                resizeChart()
                if (solarIntensities != null) {
                    listenOnDataSpinner(solarIntensities)
                    drawLineChart(solarIntensities)
                }
            }
        }
        return binding.root
    }

    private fun resizeChart() {
        binding.lineChart.minimumHeight =
            (Resources.getSystem().displayMetrics.heightPixels * 0.7).toInt()
    }

    private fun drawLineChart(solarIntensities: List<SolarIntensity>) {
        setLineChartEntries(solarIntensities)
        val dataSets: MutableList<ILineDataSet?> = ArrayList()
        setLegend(binding.lineChart)

        val dataSetBatteryGain = LineDataSet(
            lineChartEntriesBatteryGain,
            resources.getString(lineChartSpinnerEntry.nameId)
        )
        setGraphView(dataSetBatteryGain, Color.GREEN)
        dataSets.add(dataSetBatteryGain)
        val dataSetSolarExposure = LineDataSet(
            lineChartEntriesSolarExposure,
            resources.getString(lineChartSpinnerEntry.nameId)
        )
        setGraphView(dataSetSolarExposure, Color.RED)
        dataSets.add(dataSetSolarExposure)
        val dataSetActivities = LineDataSet(
            lineChartEntriesActivities,
            resources.getString(lineChartSpinnerEntry.nameId)
        )
        setGraphViewActivities(dataSetActivities)
        dataSets.add(dataSetActivities)

        binding.lineChart.data = LineData(dataSets)

        binding.lineChart.setVisibleXRangeMaximum(12f) // allow 12 values to be displayed at once on the x-axis, not more
        binding.lineChart.moveViewToX(lineChartEntriesBatteryGain.size.toFloat() - 12f)
        setXAxis()
        setYAxis(binding.lineChart.axisLeft)
        setYAxis(binding.lineChart.axisRight)
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.marker =
            CustomMarkerViewSolarIntensity(requireContext(), R.layout.marker_graph)
        binding.lineChart.invalidate()
    }

    private fun resetChart() {
        binding.lineChart.fitScreen()
        binding.lineChart.data?.clearValues()
        binding.lineChart.xAxis?.valueFormatter = null
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.clear()
        binding.lineChart.invalidate()
    }

    private fun setLineChartEntries(solarIntensities: List<SolarIntensity>) {
        val dateFormat: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
        val data = when (lineChartSpinnerEntry) {
            XAxisSelector.Days -> {
                solarIntensities.sortedBy { it.date }
            }
            XAxisSelector.Weeks -> {
                solarIntensities
                    .sortedBy { it.date }
                    .chunked(7)
                    .filter { it.isNotEmpty() }
                    .map { entries ->
                        val sorted = entries.sortedBy { it.getDateAsFloat() }
                        val newEntry = SolarIntensity(0,
                            sorted.first().date,
                            sorted.sumOf { it.solarUtilizationInHours },
                            sorted.sumOf { it.solarExposureInHours } / sorted.size,
                            true)
                        newEntry.markerText =
                            "${dateFormat.format(sorted.first().date)} - ${dateFormat.format(sorted.last().date)}"
                        newEntry.activitiesOnDay = sorted.sumOf { it.activitiesOnDay }
                        newEntry
                    }
            }
            else -> {
                val solarIntensitiesLocal: MutableList<SolarIntensity> = mutableListOf()
                //TODO: until max date
                for (i in 0L until 24L) {
                    val startDate = Date.from(
                        YearMonth.now().minusMonths(i).atDay(1).atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                    )
                    val endDate = Date.from(
                        YearMonth.now().minusMonths(i).atEndOfMonth()
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()
                    )

                    val entries = solarIntensities.filter {
                        it.date.before(endDate) && (it.date.after(startDate) || it.date == startDate)
                    }
                    val newEntry = if (entries.isNotEmpty()) {
                        SolarIntensity(0,
                            entries.first().date,
                            entries.sumOf { it.solarUtilizationInHours },
                            entries.sumOf { it.solarExposureInHours } / entries.size,
                            true)
                    } else {
                        SolarIntensity(
                            0,
                            startDate, 0.0, 0.0, true
                        )
                    }
                    newEntry.activitiesOnDay = entries.sumOf { it.activitiesOnDay }
                    newEntry.markerText =
                        "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
                    solarIntensitiesLocal.add(newEntry)
                }
                solarIntensitiesLocal.sortedBy { it.date }
            }
        }
        lineChartEntriesBatteryGain = data.mapIndexed { index, entry ->
            Entry(index.toFloat(), (entry.solarUtilizationInHours).toFloat(), entry)
        }.toMutableList()
        lineChartEntriesSolarExposure = data.mapIndexed { index, entry ->
            Entry(index.toFloat(), (entry.solarExposureInHours).toFloat(), entry)
        }.toMutableList()
        lineChartEntriesActivities = data.mapIndexed { index, entry ->
            Entry(index.toFloat() - 0.5F, entry.activitiesOnDay.toFloat(), entry)
        }.toMutableList()
        val lastEntry = lineChartEntriesActivities.last()
        if (lastEntry != null) {
            lineChartEntriesActivities.add(Entry(lastEntry.x + 1f, lastEntry.y))
        }
        lineChartColors = data.map { if (it.isForWholeDay) Color.BLUE else Color.RED }

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
                    "%.1f h",
                    value
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
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet.cubicIntensity = 0.1f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = color
        dataSet.fillAlpha = 20
    }

    private fun setLegend(lineChart: CustomMarkerLineChart) {
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
            ),
            LegendEntry(
                getString(R.string.activity_hint),
                Legend.LegendForm.CIRCLE,
                9f,
                5f,
                null,
                Color.DKGRAY
            ),
            LegendEntry(
                getString(R.string.solar_50000_lux_condition),
                Legend.LegendForm.LINE,
                9f,
                5f,
                null,
                Color.GREEN
            ),
            LegendEntry(
                getString(R.string.solar_exposure),
                Legend.LegendForm.LINE,
                9f,
                5f,
                null,
                Color.RED
            ),
        )
        legend.setCustom(legends)
        legend.isEnabled = true
    }

    private fun listenOnDataSpinner(solarIntensities: List<SolarIntensity>) {
        binding.spinnerData.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                lineChartSpinnerEntry = XAxisSelector.values()[i]
                resetChart()
                drawLineChart(solarIntensities)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val dateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            XAxisSelector.values().map { resources.getString(it.nameId) }.toTypedArray()
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerData.adapter = dateAdapter
    }

}


enum class XAxisSelector(val nameId: Int) {
    Days(R.string.days),
    Weeks(R.string.weeks),
    Months(R.string.months),
}
