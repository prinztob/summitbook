import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.Px
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner
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
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.GpsTrack.Companion.interpolateColor
import de.drtobiasprinz.summitbook.models.MyResultReceiver
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
    private lateinit var summitEntry: Summit
    private lateinit var root: View
    private lateinit var metrics: DisplayMetrics
    private var database: AppDatabase? = null
    private lateinit var osMap: CustomMapViewToAllowSrolling
    private var marker: Marker? = null
    private var selectedCustomizeTrackItem = TrackColor.Elevation
    private var trackSlopeGraph: MutableList<Entry> = mutableListOf()
    private var trackSlopeGraphBinSize: Double = 100.0
    private var gpsTrack: GpsTrack? = null
    private var usedItemsForColorCode: List<TrackColor> = emptyList()
    private var summitToCompare: Summit? = null
    private var summitsToCompare: List<Summit> = emptyList()
    private lateinit var resultreceiver: MyResultReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultreceiver = context as MyResultReceiver
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java)
        pageViewModel?.setIndex(TAG)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        root = inflater.inflate(R.layout.fragment_summit_entry_track, container, false)
        database = context?.let { AppDatabase.getDatabase(it) }
        summitEntry = resultreceiver.getSummit()
        summitToCompare = resultreceiver.getSelectedSummitForComparison()
        summitsToCompare = resultreceiver.getSummitsForComparison()
        metrics = DisplayMetrics()
        val mainActivity = MainActivity.mainActivity
        mainActivity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        if (summitEntry.isBookmark) {
            root.findViewById<Spinner>(R.id.summit_name_to_compare).visibility = View.GONE
        } else {
            prepareCompareAutoComplete()
        }
        setGpsTrack(summitEntry)
        setUsedItemsForColorCode(true)
        val textViewName = root.findViewById<TextView>(R.id.summit_name)
        textViewName.text = summitEntry.name
        val imageViewSportType = root.findViewById<ImageView>(R.id.sport_type_image)
        imageViewSportType.setImageResource(summitEntry.sportType.imageIdBlack)
        setOpenStreetMap()
        drawChart()
        val openWithButton = root.findViewById<ImageButton>(R.id.gps_open_with)
        openWithButton.setOnClickListener { _: View? ->
            if (summitEntry.hasGpsTrack()) {
                try {
                    val uri = summitEntry.copyGpsTrackToTempFile(requireActivity().externalCacheDir)?.let { FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", it) }
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "application/gpx")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(intent)
                } catch (e: IOException) {
                    Toast.makeText(requireContext(), getString(R.string.gpx_file_not_copied),
                            Toast.LENGTH_LONG).show()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), getString(R.string.gpx_viewer_not_installed),
                            Toast.LENGTH_LONG).show()
                }
            }
        }
        val shareButton = root.findViewById<ImageButton>(R.id.gps_share)
        shareButton.setOnClickListener { _: View? ->
            if (summitEntry.hasGpsTrack()) {
                try {
                    val uri = summitEntry.copyGpsTrackToTempFile(requireActivity().externalCacheDir)?.let { FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID + ".provider", it) }
                    val intentShareFile = Intent(Intent.ACTION_SEND)
                    intentShareFile.type = "application/pdf"
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shared_gpx_subject))
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_summit_gpx_text,
                            summitEntry.name, summitEntry.getDateAsString(), summitEntry.elevationData.toString(), summitEntry.kilometers.toString()))
                    startActivity(intentShareFile)
                } catch (e: IOException) {
                    Toast.makeText(requireContext(),
                            getString(R.string.no_email_prgram_installed), Toast.LENGTH_LONG).show()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(),
                            getString(R.string.no_email_prgram_installed), Toast.LENGTH_LONG).show()
                }
            }
        }

        return root
    }

    private fun prepareCompareAutoComplete() {
        val summitToCompareSpinner: SmartMaterialSpinner<String> = root.findViewById(R.id.summit_name_to_compare)
        val items = getSummitsSuggestions(summitEntry)
        summitToCompareSpinner.item = items
        resultreceiver.getViewPager().addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, @Px positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                val summitToCompareLocal = resultreceiver.getSelectedSummitForComparison()
                if (summitToCompareLocal != null) {
                    val name = "${summitToCompareLocal.getDateAsString()} ${summitToCompareLocal.name}"
                    val index = items.indexOf(name)
                    summitToCompareSpinner.setSelection(index)
                    setOpenStreetMap()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        summitToCompareSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (view != null) {
                    val text = items[position]
                    if (text != "") {
                        summitToCompare = summitsToCompare.find { "${it.getDateAsString()} ${it.name}" == text }
                        resultreceiver.setSelectedSummitForComparison(summitToCompare)
                    }
                    setOpenStreetMap()
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
                setOpenStreetMap()
            }
        }
    }

    private fun getSummitsSuggestions(localSummit: Summit): ArrayList<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsToCompareFromActivity = resultreceiver.getSummitsForComparison()
        val summitsWithoutSimilarName = summitsToCompareFromActivity.filter { it.name != localSummit.name && it.hasGpsTrack() }.sortedByDescending { it.date }
        val summitsWithSimilarName = summitsToCompareFromActivity.filter { it.name == localSummit.name && it != localSummit }.sortedByDescending { it.date }
        summitsToCompare = summitsWithSimilarName + summitsWithoutSimilarName
        summitsToCompare.forEach {
            suggestions.add("${it.getDateAsString()} ${it.name}")
        }
        return suggestions as ArrayList
    }

    private fun setGpsTrack(localSummit: Summit) {
        if (localSummit.hasGpsTrack()) {
            localSummit.setGpsTrack(loadFullTrackAsynchronous = true)
            gpsTrack = localSummit.gpsTrack
            if (gpsTrack?.hasNoTrackPoints() == true) {
                gpsTrack?.parseTrack(loadFullTrackAsynchronous = true)
            }
            val localGpsTrack = gpsTrack
            if (localGpsTrack?.trackPoints?.isNotEmpty() == true && trackSlopeGraph.isNullOrEmpty()) {
                trackSlopeGraph = localGpsTrack.getTrackSlopeGraph(binSizeMeter = trackSlopeGraphBinSize)
            }
        }
    }


    private fun drawChart() {
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
        val l1 = LegendEntry("$label ${getString(R.string.min)}", Legend.LegendForm.CIRCLE, 9f, 5f, null, selectedCustomizeTrackItem.minColor)
        val l2 = LegendEntry("$label ${getString(R.string.max)}", Legend.LegendForm.CIRCLE, 9f, 5f, null, selectedCustomizeTrackItem.maxColor)
        l.setCustom(arrayOf(l1, l2))
        l.isEnabled = true
    }

    private fun setColors(lineChartEntries: MutableList<Entry>, dataSet: LineDataSet) {
        val min = lineChartEntries.minByOrNull { it.y }?.y
        val max = lineChartEntries.maxByOrNull { it.y }?.y
        if (min != null && max != null) {
            val colors = lineChartEntries.map {
                val fraction = (it.y - min) / (max - min)
                interpolateColor(selectedCustomizeTrackItem.minColor,
                        selectedCustomizeTrackItem.maxColor, fraction)
            }
            dataSet.colors = colors
        }
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(requireContext().resources.configuration.locales[0], "%.1f km", (value / 100f).roundToLong() / 10f)
            }
        }
    }

    private fun setOpenStreetMap() {
        val hasPoints = gpsTrack?.hasOnlyZeroCoordinates() == false || summitEntry.latLng != null

        osMap = root.findViewById(R.id.osmap)
        osMap.overlays?.clear()
        osMap.overlayManager?.clear()
        OpenStreetMapUtils.setTileSource(OpenStreetMapUtils.selectedItem, osMap)
        val changeMapTypeFab: ImageButton = root.findViewById(R.id.change_map_type)
        changeMapTypeFab.setImageResource(R.drawable.ic_more_vert_black_24dp)
        changeMapTypeFab.setOnClickListener { OpenStreetMapUtils.showMapTypeSelectorDialog(requireContext(), osMap) }
        OpenStreetMapUtils.addDefaultSettings(requireContext(), osMap, requireActivity())
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        val height = if (hasPoints) 0.7 else 0.0
        val params = osMap.layoutParams
        params?.height = (metrics.heightPixels * height).toInt()
        osMap.layoutParams = params
        val customizeTrackButton = root.findViewById<ImageButton>(R.id.customize_track)
        val databaseLocal = database
        if (hasPoints && databaseLocal != null) {
            val connectedEntries = mutableListOf<Summit>()
            summitEntry.setConnectedEntries(connectedEntries, databaseLocal)
            val localSummitToCompare = summitToCompare
            if (localSummitToCompare != null) {
                OpenStreetMapUtils.drawTrack(localSummitToCompare, true, osMap, TrackColor.None, color = Color.BLACK)
            } else {
                for (entry in connectedEntries) {
                    OpenStreetMapUtils.drawTrack(entry, true, osMap, TrackColor.None, color = Color.BLACK)
                }
            }

            marker = OpenStreetMapUtils.addTrackAndMarker(summitEntry, osMap, requireContext(), true, selectedCustomizeTrackItem, true, rootView = root)
            customizeTrackButton.setOnClickListener {
                customizeTrackDialog()
            }
        } else {
            customizeTrackButton.visibility = View.GONE
            root.findViewById<RelativeLayout>(R.id.osmapLayout).visibility = View.GONE
        }

    }

    private fun customizeTrackDialog() {
        val fDialogTitle = getString(R.string.color_code_selection)
        setUsedItemsForColorCode()
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(fDialogTitle)
        builder.setSingleChoiceItems(usedItemsForColorCode.map { resources.getString(it.nameId) }.toTypedArray(), selectedCustomizeTrackItem.spinnerId) { dialog: DialogInterface, item: Int ->
            selectedCustomizeTrackItem = usedItemsForColorCode[item]
            setOpenStreetMap()
            drawChart()
            dialog.dismiss()
        }

        val fMapTypeDialog = builder.create()
        fMapTypeDialog.setCanceledOnTouchOutside(true)
        fMapTypeDialog.show()
    }

    private fun setUsedItemsForColorCode(setDefault: Boolean = false) {
        usedItemsForColorCode = TrackColor.values().filter { trackColorEntry: TrackColor ->
            gpsTrack?.trackPoints?.any {
                val value = trackColorEntry.f(it)
                value != null && value != 0.0
            } == true
        }.mapIndexed { i, entry ->
            entry.spinnerId = i
            entry
        }
        if (setDefault && TrackColor.Elevation !in usedItemsForColorCode) {
            selectedCustomizeTrackItem = TrackColor.None
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
        outState.putLong(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summitEntry.id)
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