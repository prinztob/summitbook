import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowSrolling
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong


class SummitEntryTrackFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var dataSpinner: Spinner? = null
    private var binSpinner: Spinner? = null
    private var summitEntry: SummitEntry? = null
    private lateinit var root: View
    private lateinit var osMap: CustomMapViewToAllowSrolling
    private lateinit var metrics: DisplayMetrics
    private var marker: Marker? = null
    private var isMilageButtonShown: Boolean = false
    private var helper: SummitBookDatabaseHelper? = null
    private var database: SQLiteDatabase? = null
    private val activeSpinnerFields: MutableMap<String, Pair<Boolean, Int>> = HashMap()
    private var selectedDataSpinner = 0
    private var selectedBinSpinner = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_track, container, false)
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
            setOpenStreetMap(localSummitEntry)
            fillDateSpinner(localSummitEntry)
            drawChart(localSummitEntry)
            val openWithButton = root.findViewById<ImageButton>(R.id.gps_open_with)
            openWithButton.setOnClickListener { _: View? ->
                if (localSummitEntry.hasGpsTrack()) {
                    try {
                        val uri = localSummitEntry.copyGpsTrackToTempFile(requireActivity().externalCacheDir)?.let { FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", it) }
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(uri, "application/gpx")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        if (intent.resolveActivity(requireActivity().packageManager) != null) {
                            startActivity(intent)
                        } else {
                            Toast.makeText(requireContext(),
                                    "GPX viewer not installed", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: IOException) {
                        Toast.makeText(requireContext(),
                                "GPX file could not be copied", Toast.LENGTH_LONG).show()
                    }
                }
            }
            val showMileageButton = root.findViewById<ImageButton>(R.id.osm_with_milage)
            showMileageButton.setOnClickListener { _: View? ->
                if (isMilageButtonShown) {
                    isMilageButtonShown = false
                    showMileageButton.setImageResource(R.drawable.moreinfo_arrow)
                } else {
                    isMilageButtonShown = true
                    showMileageButton.setImageResource(R.drawable.moreinfo_arrow_pressed)
                }
                osMap.overlays?.clear()
                localSummitEntry.let { OpenStreetMapUtils.addTrackAndMarker(it, osMap, requireContext(), true, isMilageButtonShown, true) }
            }
            val shareButton = root.findViewById<ImageButton>(R.id.gps_share)
            shareButton.setOnClickListener { _: View? ->
                if (localSummitEntry.hasGpsTrack()) {
                    try {
                        val uri = localSummitEntry.copyGpsTrackToTempFile(requireActivity().externalCacheDir)?.let { FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", it) }
                        val intentShareFile = Intent(Intent.ACTION_SEND)
                        intentShareFile.type = "application/pdf"
                        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
                        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_gpx_subject))
                        intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_summit_gpx_text,
                                localSummitEntry.name, localSummitEntry.getDateAsString(), localSummitEntry.heightMeter.toString(), localSummitEntry.kilometers.toString()))
                        if (intentShareFile.resolveActivity(requireActivity().packageManager) != null) {
                            startActivity(intentShareFile)
                        } else {
                            Toast.makeText(requireContext(),
                                    "E-Mail program not installed", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: IOException) {
                        Toast.makeText(requireContext(),
                                "GPX file could not be shared", Toast.LENGTH_LONG).show()
                    }
                }
            }

            binSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                    selectedBinSpinner = i
                    drawChart(localSummitEntry)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
            dataSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                    selectedDataSpinner = i
                    drawChart(localSummitEntry)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        }
        return root
    }

    private fun fillDateSpinner(summitEntry: SummitEntry) {
        var moreThanOneActive = false
        activeSpinnerFields["height_meter"] = Pair(true, 0)

        val entries = mutableListOf(resources.getString(R.string.height_meter))
        if (summitEntry.gpsTrack?.trackPoints?.firstOrNull { it?.extension?.power != null } != null) {
            entries.add(resources.getString(R.string.power))
            activeSpinnerFields["power"] = Pair(true, 1)
            moreThanOneActive = true
        } else {
            activeSpinnerFields["power"] = Pair(false, 0)
        }
        if (summitEntry.gpsTrack?.trackPoints?.firstOrNull { it?.extension?.cadence != null } != null) {
            entries.add(resources.getString(R.string.cadence))
            activeSpinnerFields["cadence"] = Pair(true, (activeSpinnerFields["power"]?.second
                    ?: 0) + 1)
            moreThanOneActive = true
        } else {
            activeSpinnerFields["cadence"] = Pair(false, 0)
        }
        if (summitEntry.gpsTrack?.trackPoints?.firstOrNull { it?.extension?.heartRate != null } != null) {
            entries.add(resources.getString(R.string.heart_rate))
            activeSpinnerFields["heartRate"] = Pair(true, maxOf(activeSpinnerFields["cadence"]?.second
                    ?: 0, activeSpinnerFields["power"]?.second ?: 0) + 1)
            moreThanOneActive = true
        } else {
            activeSpinnerFields["heartRate"] = Pair(false, 0)
        }
        val dateAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, java.util.ArrayList(entries))
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataSpinner = root.findViewById(R.id.spinner_data)
        dataSpinner?.adapter = dateAdapter
        dataSpinner?.visibility = if (moreThanOneActive) View.VISIBLE else View.GONE


        val binAdapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, java.util.ArrayList(listOf("None", "3", "10", "30", "100")))
        binAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binSpinner = root.findViewById(R.id.spinner_bin)
        binSpinner?.adapter = binAdapter
        binSpinner?.visibility = if (moreThanOneActive) View.VISIBLE else View.GONE
    }

    private fun drawChart(localSummitEntry: SummitEntry) {
        if (localSummitEntry.hasGpsTrack()) {
            localSummitEntry.setGpsTrack()
            val gpsTrack: GpsTrack? = localSummitEntry.gpsTrack
            if (gpsTrack != null) {
                if (gpsTrack.hasNoTrackPoints()) {
                    gpsTrack.parseTrack()
                }
                val lineChart = root.findViewById<LineChart>(R.id.lineChart)
                setXAxis(lineChart)
                val params = lineChart.layoutParams
                params.height = (metrics.heightPixels * 0.3).toInt()
                lineChart.layoutParams = params
                val dataSets: MutableList<ILineDataSet> = ArrayList()
                val lineChartEntries: MutableList<Entry>?
                val secondLineChartEntries: MutableList<Entry>?
                val label: String?
                val secondLabel: String?
                when {
                    selectedDataSpinner == activeSpinnerFields["power"]?.second && activeSpinnerFields["power"]?.first == true -> {
                        lineChartEntries = gpsTrack.getTrackGraph{ e -> e.extension?.power?.toFloat() }
                        label = getString(R.string.power_profile_label)
                        secondLineChartEntries = gpsTrack.getTrackGraph{ e -> e.extension?.power?.toFloat() }
                        secondLabel = label
                    }
                    selectedDataSpinner == activeSpinnerFields["cadence"]?.second && activeSpinnerFields["cadence"]?.first == true -> {
                        lineChartEntries = gpsTrack.getTrackGraph{ e -> e.extension?.cadence?.toFloat() }
                        label = getString(R.string.cadence_profile_label)
                        secondLineChartEntries = gpsTrack.getTrackGraph{ e -> e.extension?.cadence?.toFloat() }
                        secondLabel = label
                    }
                    selectedDataSpinner == activeSpinnerFields["heartRate"]?.second && activeSpinnerFields["heartRate"]?.first == true -> {
                        lineChartEntries = gpsTrack.getTrackGraph{ e -> e.extension?.heartRate?.toFloat() }
                        label = getString(R.string.heart_rate_profile_label)
                        secondLineChartEntries = gpsTrack.getTrackGraph{ e -> e.extension?.cadence?.toFloat() }
                        secondLabel = label
                    }
                    else -> {
                        lineChartEntries = gpsTrack.getTrackGraph{ e -> e.ele?.toFloat() }
                        label = getString(R.string.height_profile_label)
                        secondLineChartEntries = gpsTrack.getTrackTiltGraph()
                        secondLabel = getString(R.string.tilt_profile_label)
                    }
                }
                val leftAxis: YAxis = lineChart.axisLeft
                leftAxis.textColor = Color.BLACK
                leftAxis.setDrawGridLines(true)
                leftAxis.isGranularityEnabled = true

                val dataSet = LineDataSet(lineChartEntries, label)
                setGraphView(dataSet)

                dataSets.add(dataSet)
                if (selectedBinSpinner != 0) {
                    val rightAxis: YAxis = lineChart.axisRight
                    rightAxis.textColor = Color.BLUE
                    rightAxis.setDrawGridLines(false)
                    rightAxis.setDrawZeroLine(false)
                    rightAxis.isGranularityEnabled = false
                    val binSize = when (selectedBinSpinner) {
                        2 -> 10
                        3 -> 30
                        4 -> 100
                        else -> 3
                    }
                    val secondLineChartEntriesBinned = secondLineChartEntries.chunked(binSize) { Entry((it.sumBy { it.x.toInt() }.toFloat())/it.size, (it.sumBy { it.y.toInt() }.toFloat())/it.size) } as MutableList<Entry>
                    val secondDataSet = LineDataSet(secondLineChartEntriesBinned, "${secondLabel} (bin: ${binSize})")
                    setGraphView(secondDataSet, true)
                    dataSets.add(secondDataSet)
                }
                lineChart.data = LineData(dataSets)
                lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry, h: Highlight?) {
                        val trackPoint = e.data as TrackPoint
                        osMap.overlays.remove(marker)
                        marker = OpenStreetMapUtils.addMarker(osMap, requireContext(), GeoPoint(trackPoint.lat, trackPoint.lon), localSummitEntry)
                    }

                    override fun onNothingSelected() {}
                })

            }
        }
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(Locale.ENGLISH, "%.1f km", (value / 100f).roundToLong() / 10f)
            }
        }
    }

    private fun setOpenStreetMap(localSummitEntry: SummitEntry) {
        if (localSummitEntry.gpsTrack == null) {
            localSummitEntry.setGpsTrack()
        }
        val hasPoints = localSummitEntry.gpsTrack?.hasOnlyZeroCoordinates() == false || localSummitEntry.latLng != null
        osMap = root.findViewById(R.id.osmap)
        OpenStreetMapUtils.setTileSource(OpenStreetMapUtils.selectedItem, osMap)
        val changeMapTypeFab: ImageButton = root.findViewById(R.id.change_map_type)
        changeMapTypeFab.setImageResource(R.drawable.ic_more_vert_black_24dp)
        changeMapTypeFab.setOnClickListener { OpenStreetMapUtils.showMapTypeSelectorDialog(requireContext(), osMap) }
        OpenStreetMapUtils.addDefaultSettings(requireContext(), osMap, requireActivity())
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        val height = if (hasPoints) 0.75 else 0.0
        val params = osMap.layoutParams
        params?.height = (metrics.heightPixels * height).toInt()
        osMap.layoutParams = params
        if (hasPoints) {
            val connectedEntries = mutableListOf<SummitEntry>()
            localSummitEntry.setConnectedEntries(connectedEntries, database!!, helper!!)
            for (entry in connectedEntries) {
                OpenStreetMapUtils.drawTrack(entry, false, osMap, false, color = Color.BLACK)
            }
            marker = OpenStreetMapUtils.addTrackAndMarker(localSummitEntry, osMap, requireContext(), false, isMilageButtonShown, true)
        } else {
            root.findViewById<RelativeLayout>(R.id.osmapLayout).visibility = View.GONE
        }
    }

    private fun setGraphView(set1: LineDataSet?, secondView: Boolean = false) {
        set1?.setDrawValues(false)
        set1?.setDrawFilled(!secondView)
        set1?.setDrawCircles(false)
        if (secondView) {
            set1?.axisDependency = YAxis.AxisDependency.RIGHT
            set1?.color = ColorTemplate.getHoloBlue()
            set1?.mode = LineDataSet.Mode.LINEAR
            set1?.setCircleColor(ColorTemplate.getHoloBlue())
            set1?.lineWidth = 1f
            set1?.circleRadius = 3f
            set1?.fillAlpha = 25
            set1?.fillColor = ColorTemplate.getHoloBlue()
            set1?.highLightColor = Color.rgb(244, 117, 117)
            set1?.setDrawCircleHole(false)
        } else {
            set1?.axisDependency = YAxis.AxisDependency.LEFT;
            set1?.color = Color.RED
            set1?.setCircleColor(Color.RED)
            set1?.lineWidth = 1f
            set1?.circleRadius = 3f
            set1?.fillAlpha = 50
            set1?.fillColor = Color.RED
            set1?.setDrawCircleHole(false)
            set1?.highLightColor = Color.rgb(244, 117, 117)
            set1?.setDrawHorizontalHighlightIndicator(true)

        }
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
        private const val TAG = "SummitEntryTrackFragement"

        fun newInstance(summitEntry: SummitEntry): SummitEntryTrackFragment {
            val fragment = SummitEntryTrackFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}