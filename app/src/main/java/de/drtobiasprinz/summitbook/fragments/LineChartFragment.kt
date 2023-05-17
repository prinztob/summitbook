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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
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
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.LineChartSpinnerEntry
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerLineChart
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerView
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class LineChartFragment : Fragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private lateinit var binding: FragmentLineChartBinding
    private val viewModel: DatabaseViewModel by activityViewModels()

    private var lineChartSpinnerEntry: LineChartSpinnerEntry = LineChartSpinnerEntry.HeightMeter
    private var lineChartEntries: MutableList<Entry?> = ArrayList()
    private var lineChartColors: List<Int>? = mutableListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLineChartBinding.inflate(layoutInflater, container, false)
        fillDateSpinner()
        binding.apply {
            viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
                itData.data?.let { summits ->
                    val filteredSummits = sortFilterValues.apply(
                        summits,
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                    )
                    resizeChart()
                    listenOnDataSpinner(filteredSummits)
                    drawLineChart(filteredSummits)
                }
            }
        }
        return binding.root
    }

    private fun resizeChart() {
        binding.lineChart.minimumHeight =
            (Resources.getSystem().displayMetrics.heightPixels * 0.7).toInt()
    }

    private fun drawLineChart(summits: List<Summit>) {
        setLineChartEntries(summits)
        val dataSets: MutableList<ILineDataSet?> = ArrayList()
        val dataSet =
            LineDataSet(lineChartEntries, resources.getString(lineChartSpinnerEntry.nameId))
        setGraphView(dataSet)
        setLegend(binding.lineChart, resources.getString(lineChartSpinnerEntry.nameId))
        dataSets.add(dataSet)
        binding.lineChart.data = LineData(dataSets)
        setXAxis()
        val yAxisLeft = binding.lineChart.axisLeft
        setYAxis(yAxisLeft)
        val yAxisRight = binding.lineChart.axisRight
        setYAxis(yAxisRight)
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.marker = CustomMarkerView(requireContext(), R.layout.marker_graph)
        binding.lineChart.invalidate()
    }

    private fun setLineChartEntries(summits: List<Summit>) {
        val useEntries = summits.filter {
            val value = lineChartSpinnerEntry.f(it)
            if (value != null) {
                if (!lineChartSpinnerEntry.includeIndoorActivities) {
                    if (it.sportType == SportType.IndoorTrainer) false else value > 0
                } else {
                    value > 0
                }
            } else {
                false
            }
        }.sortedBy { it.date }
        var accumulator = 0f
        lineChartColors =
            useEntries.map { ContextCompat.getColor(requireContext(), it.sportType.color) }
        lineChartEntries = useEntries.map {
            val value = if (!lineChartSpinnerEntry.accumulate) {
                lineChartSpinnerEntry.f(it)
            } else {
                accumulator += lineChartSpinnerEntry.f(it) ?: 0f
                accumulator
            }
            Entry(it.getDateAsFloat(), value ?: 0f, it)
        }.toMutableList()
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
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(
                    requireContext().resources.configuration.locales[0],
                    "%.0f %s",
                    value,
                    lineChartSpinnerEntry.unit
                )
            }
        }
    }

    private fun setGraphView(dataSet: LineDataSet) {
        dataSet.setDrawValues(false)
        dataSet.color = R.color.colorPrimaryDark
        dataSet.circleColors = lineChartColors
        dataSet.highLightColor = Color.RED
        dataSet.lineWidth = 5f
        dataSet.circleRadius = 10f
        dataSet.valueTextSize = 15f
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet.cubicIntensity = 0.2f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.BLACK
        dataSet.fillAlpha = 60
    }

    private fun setLegend(lineChart: CustomMarkerLineChart, label: String) {
        val l: Legend = lineChart.legend
        l.entries
        l.yEntrySpace = 10f
        l.isWordWrapEnabled = true
        val legends = mutableListOf(
            LegendEntry(
                label,
                Legend.LegendForm.CIRCLE,
                9f,
                5f,
                null,
                R.color.colorPrimaryDark
            )
        )
        legends.addAll(
            SportType.values().map {
                LegendEntry(
                    resources.getString(it.sportNameStringId),
                    Legend.LegendForm.CIRCLE,
                    9f,
                    5f,
                    null,
                    ContextCompat.getColor(requireContext(), it.color)
                )
            })
        l.setCustom(legends)
        l.isEnabled = true
    }

    private fun listenOnDataSpinner(summits: List<Summit>) {
        binding.spinnerData.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                lineChartSpinnerEntry = LineChartSpinnerEntry.values()[i]
                drawLineChart(summits)
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val dateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            LineChartSpinnerEntry.values().map { resources.getString(it.nameId) }.toTypedArray()
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerData.adapter = dateAdapter
    }


}