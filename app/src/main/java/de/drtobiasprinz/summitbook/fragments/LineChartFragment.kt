package de.drtobiasprinz.summitbook.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentLineChartBinding
import de.drtobiasprinz.summitbook.db.entities.LineChartSpinnerEntry
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerLineChart
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerView
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class LineChartFragment : Fragment() {

    @Inject
    lateinit var contactsAdapter: ContactsAdapter

    private var summitEntries: List<Summit>? = null
    private var filteredEntries: List<Summit>? = null
    private var lineChartSpinnerEntry: LineChartSpinnerEntry = LineChartSpinnerEntry.HeightMeter
    private var lineChartEntries: MutableList<Entry?> = ArrayList()
    private var lineChartColors: List<Int>? = mutableListOf()
    private lateinit var binding: FragmentLineChartBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLineChartBinding.inflate(layoutInflater, container, false)
//        setHasOptionsMenu(true)
        fillDateSpinner()
        summitEntries = contactsAdapter.differ.currentList
        resizeChart()
        filteredEntries = contactsAdapter.differ.currentList
        listenOnDataSpinner()
        drawLineChart()
        return binding.root
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
        binding.lineChart.minimumHeight = (metrics.heightPixels * 0.7).toInt()
    }

    private fun drawLineChart() {
        setLineChartEntries()
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

    private fun setLineChartEntries() {
        val useEntries = filteredEntries?.filter {
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
        }?.sortedBy { it.date }
        var accumulator = 0f
        lineChartColors =
            useEntries?.map { ContextCompat.getColor(requireContext(), it.sportType.color) }
        lineChartEntries = useEntries?.map {
            val value = if (!lineChartSpinnerEntry.accumulate) {
                lineChartSpinnerEntry.f(it)
            } else {
                accumulator += lineChartSpinnerEntry.f(it) ?: 0f
                accumulator
            }
            Entry(it.getDateAsFloat(), value ?: 0f, it)
        }?.toMutableList() ?: mutableListOf()
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

    private fun listenOnDataSpinner() {
        binding.spinnerData.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                lineChartSpinnerEntry = LineChartSpinnerEntry.values()[i]
                drawLineChart()
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