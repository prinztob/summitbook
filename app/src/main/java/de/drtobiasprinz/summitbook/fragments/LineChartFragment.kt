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
import android.widget.Spinner
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
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.LineChartSpinnerEntry
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerLineChart
import de.drtobiasprinz.summitbook.ui.utils.CustomMarkerView
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LineChartFragment : Fragment(), SummationFragment {
    private var summitEntries: List<Summit>? = null
    private var filteredEntries: List<Summit>? = null
    private var dataSpinner: Spinner? = null
    private var lineChartSpinnerEntry: LineChartSpinnerEntry = LineChartSpinnerEntry.HeightMeter
    private var lineChartView: View? = null
    private var lineChartEntries: MutableList<Entry?> = ArrayList()
    private var lineChartColors: List<Int>? = mutableListOf()
    private var lineChart: CustomMarkerLineChart? = null
    private lateinit var resultreceiver: FragmentResultReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultreceiver = context as FragmentResultReceiver

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        lineChartView = inflater.inflate(R.layout.fragment_line_chart, container, false)
        setHasOptionsMenu(true)
        resultreceiver.getSortFilterHelper().fragment = this
        fillDateSpinner()
        summitEntries = resultreceiver.getSortFilterHelper().entries
        lineChart = lineChartView?.findViewById(R.id.lineChart) // Fragment
        resizeChart()
        filteredEntries = resultreceiver.getSortFilterHelper().filteredEntries
        update(filteredEntries)
        listenOnDataSpinner()
        drawLineChart()
        return lineChartView
    }

    private fun resizeChart() {
        val metrics = DisplayMetrics()
        MainActivity.mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        lineChart?.minimumHeight = (metrics.heightPixels * 0.7).toInt()
    }

    private fun drawLineChart() {
        setLineChartEntries()
        val dataSets: MutableList<ILineDataSet?> = ArrayList()
        val dataSet = LineDataSet(lineChartEntries, resources.getString(lineChartSpinnerEntry.nameId))
        setGraphView(dataSet)
        lineChart?.let { setLegend(it, resources.getString(lineChartSpinnerEntry.nameId)) }
        dataSets.add(dataSet)
        lineChart?.data = LineData(dataSets)
        setXAxis()
        val yAxisLeft = lineChart?.axisLeft
        setYAxis(yAxisLeft)
        val yAxisRight = lineChart?.axisRight
        setYAxis(yAxisRight)
        lineChart?.setTouchEnabled(true)
        lineChart?.marker = CustomMarkerView(lineChartView?.context, R.layout.marker_graph)
        lineChart?.invalidate()
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
        lineChartColors = useEntries?.map { ContextCompat.getColor(requireContext(), it.sportType.color) }
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
        val xAxis = lineChart?.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return SimpleDateFormat(Summit.DATE_FORMAT, requireContext().resources.configuration.locales[0])
                        .format(Summit.getDateFromFloat(value))
            }
        }
    }

    private fun setYAxis(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(requireContext().resources.configuration.locales[0], "%.0f %s", value, lineChartSpinnerEntry.unit)
            }
        }
    }

    override fun update(filteredSummitEntries: List<Summit>?) {
        filteredEntries = filteredSummitEntries
        val selected = dataSpinner?.selectedItemId?.toInt()
        if (selected != null) {
            lineChartSpinnerEntry = LineChartSpinnerEntry.values()[selected]
        }
        drawLineChart()
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
        val legends = mutableListOf(LegendEntry(label, Legend.LegendForm.CIRCLE, 9f, 5f, null, R.color.colorPrimaryDark))
        legends.addAll(SportType.values().map { LegendEntry(resources.getString(it.sportNameStringId), Legend.LegendForm.CIRCLE, 9f, 5f, null, ContextCompat.getColor(requireContext(), it.color)) })
        l.setCustom(legends)
        l.isEnabled = true
    }

    private fun listenOnDataSpinner() {
        dataSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                lineChartSpinnerEntry = LineChartSpinnerEntry.values()[i]
                drawLineChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val dateAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item,
                LineChartSpinnerEntry.values().map { resources.getString(it.nameId) }.toTypedArray())
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataSpinner = lineChartView?.findViewById(R.id.spinner_data)
        dataSpinner?.adapter = dateAdapter
    }


}