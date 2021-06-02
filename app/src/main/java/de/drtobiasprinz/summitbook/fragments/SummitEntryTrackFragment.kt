import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
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
import de.drtobiasprinz.gpx.TrackPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import java.util.*
import kotlin.math.roundToLong

class SummitEntryTrackFragment : Fragment() {
    private var pageViewModel: PageViewModel? = null
    private var summitEntry: SummitEntry? = null
    private lateinit var root: View
    private lateinit var osMap: CustomMapViewToAllowSrolling
    private lateinit var metrics: DisplayMetrics
    private var marker: Marker? = null
    private var isMilageButtonShown: Boolean = false
    private lateinit var helper: SummitBookDatabaseHelper
    private lateinit var database: SQLiteDatabase
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
        database = helper.writableDatabase
        metrics = DisplayMetrics()
        val mainActivity = MainActivity.mainActivity
        mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        if (summitEntry == null && savedInstanceState != null) {
            val summitEntryId = savedInstanceState.getInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER)
            if (summitEntryId != 0) {
                summitEntry = helper.getSummitsWithId(summitEntryId, database)
            }
        }
        val localSummitEntry = summitEntry
        if (localSummitEntry != null) {
            val textViewName = root.findViewById<TextView>(R.id.summit_name)
            textViewName.text = localSummitEntry.name
            val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
            imageViewSportType.setImageResource(localSummitEntry.sportType.imageId)
            setOpenStreetMap(localSummitEntry)
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
        }
        return root
    }

    private fun drawChart(localSummitEntry: SummitEntry) {
        if (localSummitEntry.hasGpsTrack()) {
            localSummitEntry.setGpsTrack()
            val gpsTrack: GpsTrack? = localSummitEntry.gpsTrack
            if (gpsTrack != null) {
                if (gpsTrack.hasNoTrackPoints()) {
                    gpsTrack.parseTrack()
                }
                val lineChartEntries: MutableList<Entry?> = ArrayList()
                val entries = gpsTrack.getTrackGraph()
                for (entry in entries) {
                    lineChartEntries.add(entry)
                }
                val lineChart = root.findViewById<LineChart>(R.id.lineChart)
                setXAxis(lineChart)
                val params = lineChart.layoutParams
                params.height = (metrics.heightPixels * 0.3).toInt()
                lineChart.layoutParams = params

                val dataSets: MutableList<ILineDataSet?> = ArrayList() // for adding multiple plots
                val dataSet = LineDataSet(lineChartEntries, getString(R.string.height_profile_label))
                setGraphView(dataSet)
                dataSets.add(dataSet)
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
        osMap = root.findViewById(R.id.osmap)
        OpenStreetMapUtils.setTileSource(OpenStreetMapUtils.selectedItem, osMap)
        val changeMapTypeFab: ImageButton = root.findViewById(R.id.change_map_type)
        changeMapTypeFab.setImageResource(R.drawable.ic_more_vert_black_24dp)
        changeMapTypeFab.setOnClickListener { OpenStreetMapUtils.showMapTypeSelectorDialog(requireContext(), osMap) }
        OpenStreetMapUtils.addDefaultSettings(requireContext(), osMap, requireActivity())
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        val params = osMap.layoutParams
        params?.height = (metrics.heightPixels * 0.75).toInt()
        osMap.layoutParams = params
        val connectedEntries = mutableListOf<SummitEntry>()
        localSummitEntry.setConnectedEntries(connectedEntries, database, helper)
        for (entry in connectedEntries) {
            OpenStreetMapUtils.drawTrack(entry, false, osMap, false, color = Color.BLACK)
        }
        marker = OpenStreetMapUtils.addTrackAndMarker(localSummitEntry, osMap, requireContext(), false, isMilageButtonShown, true)
    }

    private fun setGraphView(set1: LineDataSet?) {
        set1?.mode = LineDataSet.Mode.LINEAR
        set1?.cubicIntensity = 20f
        set1?.setDrawValues(false)
        set1?.setDrawFilled(true)
        set1?.setDrawCircles(false)
        set1?.lineWidth = 1.8f
        set1?.circleRadius = 4f
        set1?.setCircleColor(Color.GREEN)
        set1?.highLightColor = Color.rgb(244, 117, 117)
        set1?.color = Color.GREEN
        set1?.fillColor = Color.GREEN
        set1?.fillAlpha = 50
        set1?.setDrawHorizontalHighlightIndicator(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        summitEntry?._id?.let { outState.putInt(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
        helper.close()
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