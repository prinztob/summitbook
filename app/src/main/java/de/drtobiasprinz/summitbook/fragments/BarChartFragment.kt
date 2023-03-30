package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.content.SharedPreferences
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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentBarChartBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.utils.BarChartCustomRenderer
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import java.text.DateFormatSymbols
import java.text.ParseException
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream
import javax.inject.Inject

@AndroidEntryPoint
class BarChartFragment : Fragment() {

    @Inject
    lateinit var contactsAdapter: ContactsAdapter

    private lateinit var summitEntries: List<Summit>
    private lateinit var filteredEntries: List<Summit>
    private var selectedYAxisSpinnerEntry: YAxisSelector = YAxisSelector.Count
    private var indoorHeightMeterPercent = 0
    private var selectedXAxisSpinnerEntry: XAxisSelector = XAxisSelector.Date
    private var barChartEntries: MutableList<BarEntry?> = mutableListOf()
    private var lineChartEntriesForecast: MutableList<Entry?> = mutableListOf()
    //TODO: fix
    private var selectedYear: String = ""
    private var unit: String = "hm"
    private var label: String = "Height meters"
    private lateinit var intervalHelper: IntervalHelper
    private lateinit var binding: FragmentBarChartBinding
    lateinit var database: AppDatabase

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBarChartBinding.inflate(layoutInflater, container, false)
        database = DatabaseModule.provideDatabase(requireContext())
        summitEntries = contactsAdapter.differ.currentList
        filteredEntries = contactsAdapter.differ.currentList
        intervalHelper = IntervalHelper(filteredEntries)
//        setHasOptionsMenu(true)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        fillDateSpinner()
        val barChartCustomRenderer = BarChartCustomRenderer(
            binding.barChart,
            binding.barChart.animator,
            binding.barChart.viewPortHandler
        )
        binding.barChart.renderer = barChartCustomRenderer
        binding.barChart.setDrawValueAboveBar(false)
        resizeChart()
        listenOnDataSpinner()
        update(filteredEntries)
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
        binding.barChart.minimumHeight = (metrics.heightPixels * 0.7).toInt()
    }

    private fun drawChart() {
        binding.barChart.drawOrder = arrayOf(
            DrawOrder.LINE, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.BAR, DrawOrder.SCATTER
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
        if (selectedXAxisSpinnerEntry.isAQuality) {
            binding.barChart.setVisibleXRangeMaximum(12f)
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
                return if (selectedXAxisSpinnerEntry == XAxisSelector.Date) {
                    if (selectedYear == "" || value > 12f || value == 0f) {
                        String.format("%s", value.toInt())
                    } else {
                        val month = if (value < 1 || value > 12) 0 else value.toInt() - 1
                        String.format(
                            "%s",
                            DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month]
                        )
                    }
                } else if (selectedXAxisSpinnerEntry.isAQuality) {
                    if (value.toInt() < selectedXAxisSpinnerEntry.getIntervals(intervalHelper).size) {
                        selectedXAxisSpinnerEntry.getIntervals(intervalHelper)[value.toInt()].toString()
                    } else {
                        ""
                    }
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

    fun update(filteredSummitEntries: List<Summit>) {
        filteredEntries = filteredSummitEntries
        selectedDataSpinner()
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

    private fun selectedDataSpinner() {
        binding.barChart.axisLeft?.removeAllLimitLines()
        val sortedEntries = filteredEntries
        sortedEntries.sortedBy { it.date }
        barChartEntries.clear()
        lineChartEntriesForecast.clear()
        try {
            var annualTarget: Float = sharedPreferences.getString(
                selectedYAxisSpinnerEntry.sharedPreferenceKey,
                selectedYAxisSpinnerEntry.defaultAnnualTarget.toString()
            )?.toFloat()
                ?: selectedYAxisSpinnerEntry.defaultAnnualTarget.toFloat()
            if (selectedYear != "") {
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
            updateBarChart()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun updateDataForChart(xValue: Float, streamSupplier: Supplier<Stream<Summit?>?>) {
        val now = Calendar.getInstance()
        val currentYear = now[Calendar.YEAR].toString()
        label = getString(selectedYAxisSpinnerEntry.nameId)
        unit = getString(selectedYAxisSpinnerEntry.unitId)
        barChartEntries.add(BarEntry(xValue, getValueForEntry(streamSupplier)))
        if (selectedYear == currentYear && selectedXAxisSpinnerEntry == XAxisSelector.Date) {
            val forecasts = database.forecastDao()?.allForecasts as ArrayList<Forecast>
            val forecast =
                forecasts.firstOrNull { it.month == xValue.toInt() && it.year.toString() == currentYear }
            if (forecast != null) {
                lineChartEntriesForecast.add(
                    Entry(
                        xValue - 0.5F,
                        selectedYAxisSpinnerEntry.getForecastValue(forecast)
                    )
                )
            }
        }
    }


    @Throws(ParseException::class)
    private fun updateBarChart() {
        intervalHelper = IntervalHelper(filteredEntries)
        intervalHelper.setSelectedYear(selectedYear)
        intervalHelper.calculate()
        val interval = selectedXAxisSpinnerEntry.getIntervals(intervalHelper)
        val annotation = selectedXAxisSpinnerEntry.getAnnotation(intervalHelper)
        for (i in 0 until interval.size - 1) {
            val streamSupplier: Supplier<Stream<Summit?>?> = Supplier {
                selectedXAxisSpinnerEntry.getStream(filteredEntries, interval[i], interval[i + 1])
            }
            val xValue = annotation[i]
            updateDataForChart(xValue, streamSupplier)
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


    private fun listenOnDataSpinner() {
        binding.barChartSpinnerData.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                selectedYAxisSpinnerEntry = YAxisSelector.values()[i]
                selectedDataSpinner()
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
                selectedDataSpinner()
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
                    value = if (selectedXAxisSpinnerEntry == XAxisSelector.Date) {
                        if (e.x > 12 || selectedYear == "") {
                            String.format("%s", e.x.toInt())
                        } else {
                            val month = if (e.x < 1 || e.x > 12) 0 else e.x.toInt() - 1
                            String.format(
                                "%s %s",
                                DateFormatSymbols(requireContext().resources.configuration.locales[0]).months[month],
                                selectedYear
                            )
                        }
                    } else if (selectedXAxisSpinnerEntry.isAQuality) {
                        if (e.x.toInt() < selectedXAxisSpinnerEntry.getIntervals(intervalHelper).size) {
                            selectedXAxisSpinnerEntry.getIntervals(intervalHelper)[e.x.toInt()].toString()
                        } else {
                            ""
                        }
                    } else {
                        String.format(
                            "%s - %s %s",
                            (e.x * selectedXAxisSpinnerEntry.stepsSize).toInt(),
                            ((e.x + 1) * selectedXAxisSpinnerEntry.stepsSize).toInt(),
                            getString(selectedXAxisSpinnerEntry.unitId)
                        )

                    }
                    val currentYear = Calendar.getInstance()[Calendar.YEAR].toString()
                    val forecasts = database.forecastDao()?.allForecasts as ArrayList<Forecast>
                    val forecast =
                        forecasts.firstOrNull { it.month == e.x.toInt() && it.year.toString() == currentYear }

                    val selectedValue = (e as BarEntry).yVals[highlight.stackIndex].toInt()
                    val unitString = if (unit == "") "" else " $unit"
                    if (selectedValue == 0) {
                        val forecastValue =
                            forecast?.let { selectedYAxisSpinnerEntry.getForecastValue(it) }
                                ?: 0f
                        tvContent?.text =
                            if (forecastValue > 0 && selectedXAxisSpinnerEntry == XAxisSelector.Date) {
                                String.format(
                                    "%s%s\n%s\n%s: %s%s",
                                    e.getY().toInt(),
                                    unitString,
                                    value,
                                    getString(R.string.forecast_abbr),
                                    forecastValue.toInt(),
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

    enum class XAxisSelector(
        val nameId: Int,
        val unitId: Int,
        val stepsSize: Double,
        val isAQuality: Boolean,
        val getStream: (entries: List<Summit>?, start: Any, end: Any) -> Stream<Summit?>?,
        val getIntervals: (IntervalHelper) -> List<Any>,
        val getAnnotation: (IntervalHelper) -> List<Float>
    ) {
        Date(R.string.date, R.string.empty, 0.0, false, { entries, start, end ->
            entries
                ?.stream()
                ?.filter { o: Summit? ->
                    o?.date?.after(start as java.util.Date) ?: false && o?.date?.before(
                        end as java.util.Date
                    ) ?: false
                }
        }, { e -> e.dates }, { e -> e.dateAnnotation }),
        Kilometers(
            R.string.kilometers_hint,
            R.string.km,
            IntervalHelper.kilometersStep,
            false,
            { entries, start, end ->
                entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.kilometers >= start as Float && o.kilometers < end as Float }
            },
            { e -> e.kilometers },
            { e -> e.kilometerAnnotation }),
        ElevationGain(
            R.string.height_meter_hint,
            R.string.hm,
            IntervalHelper.elevationGainStep,
            false,
            { entries, start, end ->
                entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.elevationData.elevationGain >= start as Float && o.elevationData.elevationGain < end as Float }
            },
            { e -> e.elevationGains },
            { e -> e.elevationGainAnnotation }),
        TopElevation(
            R.string.top_elevation_hint,
            R.string.masl,
            IntervalHelper.topElevationStep,
            false,
            { entries, start, end ->
                entries
                    ?.stream()
                    ?.filter { o: Summit? -> o != null && o.elevationData.maxElevation >= start as Float && o.elevationData.maxElevation < end as Float }
            },
            { e -> e.topElevations },
            { e -> e.topElevationAnnotation }),
        Participants(R.string.participants, R.string.empty, 1.0, true, { entries, start, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.participants.contains(start) }
        }, { e -> e.participants }, { e -> e.participantsAnnotation }),
        Equipments(R.string.equipments, R.string.empty, 1.0, true, { entries, start, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.equipments.contains(start) }
        }, { e -> e.equipments }, { e -> e.equipmentsAnnotation }),

        Countries(R.string.country_hint, R.string.empty, 1.0, true, { entries, start, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.countries.contains(start) }
        }, { e -> e.countries }, { e -> e.countriesAnnotation })
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