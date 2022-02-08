import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
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
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.GpsTrack.Companion.interpolateColor
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowSrolling
import de.drtobiasprinz.summitbook.ui.PageViewModel
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToLong


class SummitEntryTrackFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var summitEntry: Summit? = null
    private lateinit var root: View
    private lateinit var osMap: CustomMapViewToAllowSrolling
    private lateinit var metrics: DisplayMetrics
    private var marker: Marker? = null
    private var database: AppDatabase? = null
    private var selectedCustomizeTrackItem = TrackColor.Elevation
    private var trackSlopeGraph: MutableList<Entry> = mutableListOf()
    private var trackSlopeGraphBinSize: Double = 100.0
    private var gpsTrack: GpsTrack? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_track, container, false)
        database = context?.let { AppDatabase.getDatabase(it) }
        metrics = DisplayMetrics()
        val mainActivity = MainActivity.mainActivity
        mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0L) {
                summitEntry = database?.summitDao()?.getSummit(summitEntryId)
            }
        }
        val localSummit = summitEntry
        if (localSummit != null) {
            setGpsTrack(localSummit, loadFullTrackAsynchronous = true)
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummit.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummit.sportType.imageIdBlack)
            setOpenStreetMap(localSummit)
            drawChart(localSummit)
            val openWithButton = root.findViewById<ImageButton>(R.id.gps_open_with)
            openWithButton.setOnClickListener { _: View? ->
                if (localSummit.hasGpsTrack()) {
                    try {
                        val uri = localSummit.copyGpsTrackToTempFile(requireActivity().externalCacheDir)?.let { FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", it) }
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
            val shareButton = root.findViewById<ImageButton>(R.id.gps_share)
            shareButton.setOnClickListener { _: View? ->
                if (localSummit.hasGpsTrack()) {
                    try {
                        val uri = localSummit.copyGpsTrackToTempFile(requireActivity().externalCacheDir)?.let { FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", it) }
                        val intentShareFile = Intent(Intent.ACTION_SEND)
                        intentShareFile.type = "application/pdf"
                        intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
                        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_gpx_subject))
                        intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_summit_gpx_text,
                                localSummit.name, localSummit.getDateAsString(), localSummit.elevationData.toString(), localSummit.kilometers.toString()))
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

        }
        return root
    }

    private fun setGpsTrack(localSummit: Summit, loadFullTrackAsynchronous: Boolean = false) {
        if (localSummit.hasGpsTrack()) {
            localSummit.setGpsTrack(loadFullTrackAsynchronous = loadFullTrackAsynchronous)
            gpsTrack = localSummit.gpsTrack
            if (gpsTrack?.hasNoTrackPoints() == true) {
                gpsTrack?.parseTrack(loadFullTrackAsynchronous = loadFullTrackAsynchronous)
            }
            val localGpsTrack = gpsTrack
            if (localGpsTrack?.trackPoints?.isNotEmpty() == true && trackSlopeGraph.isNullOrEmpty()) {
                trackSlopeGraph = localGpsTrack.getTrackSlopeGraph(binSizeMeter = trackSlopeGraphBinSize)
            }
        }
    }


    private fun drawChart(summitEntry: Summit) {
        if (summitEntry.hasGpsTrack()) {
            val localGpsTrack = gpsTrack
            if (localGpsTrack != null) {
                val lineChart = root.findViewById<LineChart>(R.id.lineChart)
                setXAxis(lineChart)
                val params = lineChart.layoutParams
                params.height = (metrics.heightPixels * 0.3).toInt()
                lineChart.layoutParams = params
                val dataSets: MutableList<ILineDataSet> = ArrayList()
                val trackColor = if (selectedCustomizeTrackItem == TrackColor.None || selectedCustomizeTrackItem == TrackColor.Mileage) TrackColor.Elevation else selectedCustomizeTrackItem
                val lineChartEntries = localGpsTrack.getTrackGraph(trackColor.f)
                val label = getString(trackColor.labelId)

                val leftAxis: YAxis = lineChart.axisLeft
                leftAxis.textColor = Color.BLACK
                leftAxis.setDrawGridLines(true)
                leftAxis.isGranularityEnabled = true

                val dataSet = LineDataSet(lineChartEntries, label)
                setGraphView(dataSet)
                setColors(lineChartEntries, dataSet)
                dataSets.add(dataSet)
                lineChart.data = LineData(dataSets)
                lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry, h: Highlight?) {
                        if (e.data is TrackPoint) {
                            val trackPoint = e.data as TrackPoint
                            osMap.overlays.remove(marker)
                            marker = OpenStreetMapUtils.addMarker(osMap, requireContext(), GeoPoint(trackPoint.lat, trackPoint.lon), summitEntry)
                        }
                    }

                    override fun onNothingSelected() {}
                })
                setLegend(lineChart, label)
            }
        }
    }

    private fun setLegend(lineChart: LineChart, label: String?) {
        val l: Legend = lineChart.legend
        l.entries
        l.yEntrySpace = 10f
        l.isWordWrapEnabled = true
        val l1 = LegendEntry("$label min", Legend.LegendForm.CIRCLE, 9f, 5f, null, gpsTrack?.startColor
                ?: Color.GREEN)
        val l2 = LegendEntry("$label max", Legend.LegendForm.CIRCLE, 9f, 5f, null, gpsTrack?.endColor
                ?: Color.RED)
        l.setCustom(arrayOf(l1, l2))
        l.isEnabled = true
    }

    private fun setColors(lineChartEntries: MutableList<Entry>, dataSet: LineDataSet) {
        val min = lineChartEntries.minByOrNull { it.y }?.y
        val max = lineChartEntries.maxByOrNull { it.y }?.y
        if (min != null && max != null) {
            val colors = lineChartEntries.map {
                val fraction = (it.y - min) / (max - min)
                interpolateColor(gpsTrack?.startColor ?: Color.GREEN, gpsTrack?.endColor
                        ?: Color.RED, fraction)
            }
            dataSet.colors = colors
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

    private fun setOpenStreetMap(localSummit: Summit) {
        val hasPoints = gpsTrack?.hasOnlyZeroCoordinates() == false || localSummit.latLng != null

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
        val customizeTrackButton = root.findViewById<ImageButton>(R.id.customize_track)
        val databaseLocal = database
        if (hasPoints && databaseLocal != null) {
            val connectedEntries = mutableListOf<Summit>()
            localSummit.setConnectedEntries(connectedEntries, databaseLocal)
            for (entry in connectedEntries) {
                OpenStreetMapUtils.drawTrack(entry, false, osMap, TrackColor.None, color = Color.BLACK)
            }
            marker = OpenStreetMapUtils.addTrackAndMarker(localSummit, osMap, requireContext(), true, selectedCustomizeTrackItem, true, rootView = root)
            customizeTrackButton.setOnClickListener {
                customizeTrackDialog()
            }
        } else {
            customizeTrackButton.visibility = View.GONE
            root.findViewById<RelativeLayout>(R.id.osmapLayout).visibility = View.GONE
        }

    }

    private fun customizeTrackDialog() {
        val localSummit = summitEntry
        if (localSummit != null) {
            // TODO: translate
            val fDialogTitle = "Select value used for color code"
            val useItems = TrackColor.values().filter { trackColorEntry: TrackColor ->
                gpsTrack?.trackPoints?.any {
                    val value = trackColorEntry.f(it)
                    value != null && value != 0.0
                } == true
            }.mapIndexed { i, entry ->
                entry.spinnerId = i
                entry
            }
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(fDialogTitle)
            builder.setSingleChoiceItems(useItems.map { resources.getString(it.nameId) }.toTypedArray(), selectedCustomizeTrackItem.spinnerId) { dialog: DialogInterface, item: Int ->
                selectedCustomizeTrackItem = useItems[item]
                osMap.overlays?.clear()
                localSummit.let { OpenStreetMapUtils.addTrackAndMarker(it, osMap, requireContext(), true, selectedCustomizeTrackItem, true, rootView = root) }
                drawChart(localSummit)
                dialog.dismiss()
            }

            val fMapTypeDialog = builder.create()
            fMapTypeDialog.setCanceledOnTouchOutside(true)
            fMapTypeDialog.show()
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
            set1?.axisDependency = YAxis.AxisDependency.LEFT
            set1?.color = Color.RED
            set1?.setCircleColor(Color.RED)
            set1?.lineWidth = 5f
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
        summitEntry?.id?.let { outState.putLong(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        database?.close()
    }


    companion object {
        private const val TAG = "SummitEntryTrackFragement"

        fun newInstance(summitEntry: Summit): SummitEntryTrackFragment {
            val fragment = SummitEntryTrackFragment()
            fragment.summitEntry = summitEntry
            return fragment
        }
    }
}