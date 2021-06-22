import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.PowerData
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import kotlin.math.round


class SummitEntryPowerFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var summitEntry: SummitEntry? = null
    private lateinit var root: View
    private lateinit var metrics: DisplayMetrics
    private var helper: SummitBookDatabaseHelper? = null
    private var database: SQLiteDatabase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_power, container, false)
        helper = SummitBookDatabaseHelper(requireContext())
        database = helper?.writableDatabase
        metrics = DisplayMetrics()
        val mainActivity = MainActivity.mainActivity
        mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0) {
                summitEntry = helper?.getSummitsWithId(summitEntryId, database)
            }
        }
        val localSummitEntry = summitEntry
        if (localSummitEntry != null) {
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummitEntry.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummitEntry.sportType.imageId)
            drawChart(localSummitEntry)
        }
        return root
    }

    private fun drawChart(entry: SummitEntry) {
        val power = entry.activityData?.power
        if (power != null) {

            val lineChart = root.findViewById<LineChart>(R.id.lineChart)
            setXAxis(lineChart)
            lineChart.axisLeft.axisMinimum = 0f
            lineChart.axisRight.axisMinimum = 0f

            val params = lineChart.layoutParams
            params.height = (metrics.heightPixels * 0.75).toInt()
            lineChart.layoutParams = params

            val dataSets: MutableList<ILineDataSet?> = ArrayList() // for adding multiple plots
            val dataSet = LineDataSet(getLineChartEntries(power), getString(R.string.power_profile_label))
            setGraphView(dataSet, false)
            dataSets.add(dataSet)
            val extremaValuesAllSummits = MainActivity.extremaValuesAllSummits
            if (extremaValuesAllSummits != null) {
                val dataSetAllTimeExtrema = LineDataSet(getLineChartEntries(extremaValuesAllSummits), getString(R.string.power_profile_all_label))
                setGraphView(dataSetAllTimeExtrema)
                dataSets.add(dataSetAllTimeExtrema)
            }

            lineChart.data = LineData(dataSets)
        }
    }

    private fun getLineChartEntries(extremaValuesSummits: ExtremaValuesSummits): MutableList<Entry?> {
        val lineChartEntries: MutableList<Entry?> = ArrayList()
        lineChartEntries.add(Entry(scaleCbr(1.0), extremaValuesSummits.power1sMinMax?.second?.activityData?.power?.oneSec?.toFloat()
                ?: 0f, extremaValuesSummits.power1sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(2.0), extremaValuesSummits.power2sMinMax?.second?.activityData?.power?.twoSec?.toFloat()
                ?: 0f, extremaValuesSummits.power2sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(5.0), extremaValuesSummits.power5sMinMax?.second?.activityData?.power?.fiveSec?.toFloat()
                ?: 0f, extremaValuesSummits.power5sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(10.0), extremaValuesSummits.power10sMinMax?.second?.activityData?.power?.tenSec?.toFloat()
                ?: 0f, extremaValuesSummits.power10sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(20.0), extremaValuesSummits.power20sMinMax?.second?.activityData?.power?.twentySec?.toFloat()
                ?: 0f, extremaValuesSummits.power20sMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(30.0), extremaValuesSummits.power30sMinMax?.second?.activityData?.power?.thirtySec?.toFloat()
                ?: 0f, extremaValuesSummits.power30sMinMax?.second))

        lineChartEntries.add(Entry(scaleCbr(60.0), extremaValuesSummits.power1minMinMax?.second?.activityData?.power?.oneMin?.toFloat()
                ?: 0f, extremaValuesSummits.power1minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(120.0), extremaValuesSummits.power2minMinMax?.second?.activityData?.power?.twoMin?.toFloat()
                ?: 0f, extremaValuesSummits.power2minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(300.0), extremaValuesSummits.power5minMinMax?.second?.activityData?.power?.fiveMin?.toFloat()
                ?: 0f, extremaValuesSummits.power5minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(600.0), extremaValuesSummits.power10minMinMax?.second?.activityData?.power?.tenMin?.toFloat()
                ?: 0f, extremaValuesSummits.power10minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(1200.0), extremaValuesSummits.power20minMinMax?.second?.activityData?.power?.twentyMin?.toFloat()
                ?: 0f, extremaValuesSummits.power20minMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(1800.0), extremaValuesSummits.power30minMinMax?.second?.activityData?.power?.thirtyMin?.toFloat()
                ?: 0f, extremaValuesSummits.power30minMinMax?.second))

        lineChartEntries.add(Entry(scaleCbr(3600.0), extremaValuesSummits.power1hMinMax?.second?.activityData?.power?.oneHour?.toFloat()
                ?: 0f, extremaValuesSummits.power1hMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(7200.0), extremaValuesSummits.power2hMinMax?.second?.activityData?.power?.twoHours?.toFloat()
                ?: 0f, extremaValuesSummits.power2hMinMax?.second))
        lineChartEntries.add(Entry(scaleCbr(18000.0), extremaValuesSummits.power5hMinMax?.second?.activityData?.power?.fiveHours?.toFloat()
                ?: 0f, extremaValuesSummits.power5hMinMax?.second))
        return lineChartEntries
    }

    private fun scaleCbr(cbr: Double): Float {
        return Math.log10(cbr).toFloat()
    }

    private fun unScaleCbr(cbr: Double): Float {
        val calcVal = Math.pow(10.0, cbr)
        return calcVal.toFloat()
    }

    private fun getLineChartEntries(power: PowerData): MutableList<Entry?> {
        val lineChartEntries: MutableList<Entry?> = ArrayList()
        if (power.oneSec > 0) lineChartEntries.add(Entry(scaleCbr(1.0), power.oneSec.toFloat()))
        if (power.twoSec > 0) lineChartEntries.add(Entry(scaleCbr(2.0), power.twoSec.toFloat()))
        if (power.fiveSec > 0) lineChartEntries.add(Entry(scaleCbr(5.0), power.fiveSec.toFloat()))
        if (power.tenSec > 0) lineChartEntries.add(Entry(scaleCbr(10.0), power.tenSec.toFloat()))
        if (power.twentySec > 0) lineChartEntries.add(Entry(scaleCbr(20.0), power.twentySec.toFloat()))
        if (power.thirtySec > 0) lineChartEntries.add(Entry(scaleCbr(30.0), power.thirtySec.toFloat()))
        if (power.oneMin > 0) lineChartEntries.add(Entry(scaleCbr(60.0), power.oneMin.toFloat()))
        if (power.twoMin > 0) lineChartEntries.add(Entry(scaleCbr(120.0), power.twoMin.toFloat()))
        if (power.fiveMin > 0) lineChartEntries.add(Entry(scaleCbr(300.0), power.fiveMin.toFloat()))
        if (power.tenMin > 0) lineChartEntries.add(Entry(scaleCbr(600.0), power.tenMin.toFloat()))
        if (power.twentyMin > 0) lineChartEntries.add(Entry(scaleCbr(1200.0), power.twentyMin.toFloat()))
        if (power.thirtyMin > 0) lineChartEntries.add(Entry(scaleCbr(1800.0), power.thirtyMin.toFloat()))
        if (power.oneHour > 0) lineChartEntries.add(Entry(scaleCbr(3600.0), power.oneHour.toFloat()))
        if (power.twoHours > 0) lineChartEntries.add(Entry(scaleCbr(7200.0), power.twoHours.toFloat()))
        if (power.fiveHours > 0) lineChartEntries.add(Entry(scaleCbr(18000.0), power.fiveHours.toFloat()))
        return lineChartEntries
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.ENGLISH, "%s sec", unScaleCbr(round(value.toDouble())).toInt())
            }
        }
        xAxis.axisMinimum = scaleCbr(1.0)
        xAxis.axisMaximum = scaleCbr(100000.0)
        xAxis.setLabelCount(6, true)
    }

    private fun setGraphView(set1: LineDataSet?, filled: Boolean = true) {
        set1?.mode = LineDataSet.Mode.LINEAR
        set1?.valueTextSize = 8f
        set1?.circleRadius = 4f
        set1?.setDrawValues(false)
        if (filled) {
            set1?.cubicIntensity = 20f
            set1?.lineWidth = 1.8f
            set1?.setCircleColor(Color.BLUE)
            set1?.color = Color.BLUE
            set1?.highLightColor = Color.rgb(244, 117, 117)
            set1?.setDrawFilled(true)
            set1?.fillColor = Color.BLUE
            set1?.fillAlpha = 50
        } else {
            set1?.lineWidth = 4.8f
            set1?.setCircleColor(Color.BLACK)
            set1?.color = Color.BLACK
            set1?.setDrawFilled(false)
        }
        set1?.setDrawHorizontalHighlightIndicator(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        summitEntry?._id?.let { outState.putInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
        helper?.close()
    }


    companion object {
        private const val TAG = "SummitEntryPowerFragement"

        fun newInstance(summitEntry: SummitEntry): SummitEntryPowerFragment {
            val fragment = SummitEntryPowerFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}