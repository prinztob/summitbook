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
import de.drtobiasprinz.summitbook.models.LineChartSpinnerEntry
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LineChartFragment(private val sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment {
    private var summitEntries: List<Summit>? = null
    private var filteredEntries: List<Summit>? = null
    private var dataSpinner: Spinner? = null
    private var lineChartSpinnerEntry: LineChartSpinnerEntry = LineChartSpinnerEntry.HeightMeter
    private var lineChartView: View? = null
    private var lineChartEntries: MutableList<Entry?> = ArrayList()
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
        sortFilterHelper.fragment = this
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
        setLineChartEntries()
        val dataSets: MutableList<ILineDataSet?> = ArrayList()
        val dataSet = LineDataSet(lineChartEntries, resources.getString(lineChartSpinnerEntry.nameId))
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
                return SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
                        .format(Summit.getDateFromFloat(value))
            }
        }
    }

    private fun setYAxis(yAxis: YAxis?) {
        yAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.ENGLISH, "%.0f %s", value, lineChartSpinnerEntry.unit)
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

    private fun setGraphView(dataSet: LineDataSet?) {
        dataSet?.setDrawValues(false)
        dataSet?.color = Color.BLUE
        dataSet?.color = R.color.colorPrimaryDark
        dataSet?.circleHoleColor = R.color.colorPrimary
        dataSet?.setCircleColor(R.color.colorGreen)
        dataSet?.highLightColor = Color.RED
        dataSet?.lineWidth = 5f
        dataSet?.circleRadius = 10f
        dataSet?.valueTextSize = 15f
        dataSet?.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet?.cubicIntensity = 0.2f
        dataSet?.setDrawFilled(true)
        dataSet?.fillColor = Color.BLACK
        dataSet?.fillAlpha = 60
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


    inner class CustomMarkerView(context: Context?, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView? = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            try {
                val summitEntry = e?.data as Summit
                tvContent?.text = String.format("%s\n%s\n%s hm", summitEntry.name, summitEntry.getDateAsString(), summitEntry.elevationData.elevationGain)
            } catch (ex: Exception) {
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

}