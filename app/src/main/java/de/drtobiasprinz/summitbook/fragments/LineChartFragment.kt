package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.MPPointF
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.text.SimpleDateFormat
import java.util.*

class LineChartFragment(private val sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment {
    private var summitEntries: ArrayList<SummitEntry>? = null
    private var filteredEntries: ArrayList<SummitEntry>? = null
    private var dataSpinner: Spinner? = null
    private var lineChartView: View? = null
    private var lineChartEntries: MutableList<Entry?> = ArrayList()
    private var unit: String = "hm"
    private var label: String = "Height meters"
    private var lineChart: LineChart? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        lineChartView = inflater.inflate(R.layout.fragment_line_chart, container, false)
        setHasOptionsMenu(true)
        sortFilterHelper.setFragment(this)
        fillDateSpinner()
        summitEntries = sortFilterHelper.entries
        lineChart = lineChartView?.findViewById(R.id.lineChart) // Fragment
        resizeChart()
        filteredEntries = sortFilterHelper.filteredEntries
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
        val dataSets: MutableList<ILineDataSet?> = ArrayList()
        val dataSet = LineDataSet(lineChartEntries, label)
        setGraphView(dataSet)
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

    private fun setXAxis() {
        val xAxis = lineChart?.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String? {
                return SimpleDateFormat(SummitEntry.DATE_FORMAT, Locale.ENGLISH)
                        .format(SummitEntry.getDateFromFloat(value))
            }
        }
    }

    private fun setYAxis(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.ENGLISH, "%.0f %s", value, unit)
            }
        }
    }

    override fun update(filteredSummitEntries: ArrayList<SummitEntry>?) {
        filteredEntries = filteredSummitEntries
        dataSpinner?.selectedItemId?.toInt()?.let { selectedDataSpinner(it) }
        drawLineChart()
    }

    private fun setGraphView(dataSet: LineDataSet?) {
        dataSet?.setDrawValues(false)
        dataSet?.color = Color.BLUE
        dataSet?.color = R.color.colorPrimaryDark
        dataSet?.circleHoleColor = R.color.colorPrimary
        dataSet?.setCircleColor(R.color.colorGreen)
        dataSet?.highLightColor = Color.RED
        dataSet?.lineWidth = 5f
        dataSet?.circleRadius = 15f
        dataSet?.valueTextSize = 15f
        dataSet?.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet?.cubicIntensity = 0.2f
        dataSet?.setDrawFilled(true)
        dataSet?.fillColor = Color.BLACK
        dataSet?.fillAlpha = 80
    }

    private fun selectedDataSpinner(position: Int) {
        val sortedEntries = filteredEntries
        sortedEntries?.sortWith(compareBy { it.date })
        lineChartEntries.clear()
        when (position) {
            1 -> {
                unit = "hm"
                label = "Height meters accumulated"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        var accumulatedHm = summitEntry.elevationData.elevationGain.toFloat()
                        if (lineChartEntries.size > 0) {
                            accumulatedHm += lineChartEntries[lineChartEntries.size - 1]?.y ?: 0.0f
                        }
                        lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), accumulatedHm, summitEntry))
                    }
                }
            }
            2 -> {
                unit = "km"
                label = "Kilometers"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), summitEntry.kilometers.toFloat(), summitEntry))
                    }
                }
            }
            3 -> {
                unit = "km"
                label = "Kilometers accumulated"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.kilometers > 0) {
                            var accumulatedKilometers = summitEntry.kilometers
                            if (lineChartEntries.size > 0) {
                                accumulatedKilometers += lineChartEntries[lineChartEntries.size - 1]?.y
                                        ?: 0.0f
                            }
                            lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), accumulatedKilometers.toFloat(), summitEntry))
                        }
                    }
                }
            }
            4 -> {
                unit = "hm"
                label = "Elevation"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.elevationData.maxElevation > 0) {
                            lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), summitEntry.elevationData.maxElevation.toFloat(), summitEntry))
                        }
                    }
                }
            }
            5 -> {
                unit = "km/h"
                label = "Average Speed"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.velocityData.avgVelocity > 0) {
                            lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), summitEntry.velocityData.avgVelocity.toFloat(), summitEntry))
                        }
                    }
                }
            }
            6 -> {
                unit = "km/h"
                label = "Top Speed"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.velocityData.maxVelocity > 0) {
                            lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), summitEntry.velocityData.maxVelocity.toFloat(), summitEntry))
                        }
                    }
                }
            }
            7 -> {
                unit = "bpm"
                label = "Average HR"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.activityData != null) {
                            val activityData = summitEntry.activityData
                            if (activityData != null && activityData.averageHR > 0) {
                                lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), activityData.averageHR, summitEntry))
                            }
                        }
                    }
                }
            }
            8 -> {
                unit = "W"
                label = "Norm Power"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.activityData != null) {
                            val activityData = summitEntry.activityData
                            if (activityData != null && activityData.power.normPower > 0) {
                                lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), activityData.power.normPower, summitEntry))
                            }
                        }
                    }
                }
            }
            9 -> {
                unit = "W"
                label = "Average Power 20 min"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.activityData != null) {
                            val activityData = summitEntry.activityData
                            if (activityData != null && activityData.power.twentyMin > 0) {
                                lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), activityData.power.twentyMin.toFloat(), summitEntry))
                            }
                        }
                    }
                }
            }
            10 -> {
                unit = "W"
                label = "Average Power 1 hour"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        if (summitEntry.activityData != null) {
                            val activityData = summitEntry.activityData
                            if (activityData != null && activityData.power.oneHour > 0) {
                                lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), activityData.power.oneHour.toFloat(), summitEntry))
                            }
                        }
                    }
                }
            }
            else -> {
                unit = "hm"
                label = "Height meters"
                if (sortedEntries != null) {
                    for (summitEntry in sortedEntries) {
                        lineChartEntries.add(Entry(summitEntry.getDateAsFloat(), summitEntry.elevationData.elevationGain.toFloat(), summitEntry))
                    }
                }
            }
        }
    }

    private fun listenOnDataSpinner() {
        dataSpinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                selectedDataSpinner(i)
                drawLineChart()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun fillDateSpinner() {
        val dateAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, ArrayList(listOf(*resources.getStringArray(R.array.data_array))))
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataSpinner = lineChartView?.findViewById(R.id.spinner_data)
        dataSpinner?.adapter = dateAdapter
    }


    inner class CustomMarkerView(context: Context?, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            try {
                val summitEntry = e?.data as SummitEntry
                tvContent?.text = String.format("%s\n%s\n%s hm", summitEntry.name, summitEntry.getDateAsString(), summitEntry.elevationData.elevationGain) // set the entry-value as the display text
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), (-height).toFloat())
        }
    }

}