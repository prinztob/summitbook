package de.drtobiasprinz.summitbook.fragments

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
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
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentSummitEntryTrackBinding
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.GpsTrack.Companion.interpolateColor
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.getSportTypeForMapProviders
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import de.drtobiasprinz.summitbook.viewmodel.PageViewModel
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import kotlin.math.roundToLong

@AndroidEntryPoint
class SummitEntryTrackFragment : Fragment() {

    private lateinit var binding: FragmentSummitEntryTrackBinding

    private var pageViewModel: PageViewModel? = null
    private var marker: Marker? = null
    private var selectedCustomizeTrackItem = TrackColor.Elevation
    private var trackSlopeGraph: MutableList<Entry> = mutableListOf()
    private var gpsTrack: GpsTrack? = null
    private var usedItemsForColorCode: List<TrackColor> = emptyList()
    private var summitsToCompare: List<Summit> = emptyList()
    private lateinit var mLocationOverlay: MyLocationNewOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = (requireActivity() as SummitEntryDetailsActivity).pageViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSummitEntryTrackBinding.inflate(layoutInflater, container, false)
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        mLocationOverlay =
            MyLocationNewOverlay(GpsMyLocationProvider(context), binding.osmap)
        mLocationOverlay.enableMyLocation()
        binding.osmap.overlays.add(mLocationOverlay)
        binding.loadingPanel.visibility = View.VISIBLE
        binding.lineChart.visibility = View.GONE
        OpenStreetMapUtils.addDefaultSettings(
            binding.osmap,
            requireActivity()
        )
        return binding.root
    }

    private fun setContent() {
        val summitToView = (requireActivity() as SummitEntryDetailsActivity).summitEntry
        if (PreferencesHelper.loadOnDeviceMaps() &&
            FileHelper.getOnDeviceMapFiles(requireContext()).isNotEmpty()
        ) {
            selectedItem = getSportTypeForMapProviders(summitToView.sportType, requireContext())
        } else if (FileHelper.getOnDeviceMbtilesFiles(requireContext()).isNotEmpty()) {
            selectedItem = MapProvider.MBTILES
        }
        pageViewModel?.summitToCompare?.observe(viewLifecycleOwner) { itData ->
            itData.data.let { summitToCompare ->
                pageViewModel?.summitsList?.observe(viewLifecycleOwner) { summitsListData ->
                    summitsListData.data.let { allSummits ->
                        summitsToCompare =
                            SummitEntryDetailsActivity.getSummitsToCompare(
                                summitsListData,
                                summitToView,
                                onlyWithPowerData = true
                            )
                        if (summitToView.isBookmark) {
                            binding.summitNameToCompare.visibility = View.GONE
                        } else {
                            prepareCompareAutoComplete(summitToView, summitToCompare)
                        }
                        binding.summitName.text = summitToView.name
                        binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdBlack)
                        binding.osmap.overlays.clear()
                        OpenStreetMapUtils.setTileProvider(binding.osmap, requireContext())
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                setGpsTrack(summitToView, useSimplifiedTrack = true)
                            }
                            binding.loadingPanel.visibility = View.GONE
                            binding.lineChart.visibility = View.VISIBLE
                            drawChart(summitToView)
                            updateMap(summitToView, summitToCompare, allSummits)
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    setGpsTrack(summitToView, forceUpdate = true)
                                }
                                drawChart(summitToView)
                                updateMap(summitToView, summitToCompare, allSummits, false)
                                setButtons(summitToView)
                            }
                        }
                        val numberOfPointsToShow =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .getString(Keys.PREF_MAX_NUMBER_POINT, "10000")?.toInt()
                                ?: 10000
                        binding.showAllTracks.setOnClickListener { _: View? ->
                            summitsListData.data?.let { summits ->
                                showAllTracksOfSummitInBoundingBox(
                                    summitToView,
                                    summitToCompare,
                                    summits,
                                    numberOfPointsToShow
                                )
                            }
                        }

                        binding.centerOnLocation.setOnClickListener {
                            if (mLocationOverlay.isMyLocationEnabled) {
                                val mapController = binding.osmap.controller
                                mapController.setZoom(15.0)
                                mapController.setCenter(mLocationOverlay.myLocation)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setContent()
    }

    private fun updateMap(
        summitToView: Summit,
        summitToCompare: Summit?,
        summits: List<Summit>?,
        doCleanUp: Boolean = true
    ) {
        setUsedItemsForColorCode()
        setOpenStreetMap(summitToView, summitToCompare, summits, doCleanUp = doCleanUp)
        OpenStreetMapUtils.setOsmConfForTiles()
    }

    private fun setButtons(summitToView: Summit) {
        binding.gpsOpenWith.setOnClickListener { _: View? ->
            if (summitToView.hasGpsTrack()) {
                try {
                    val uri =
                        summitToView.copyGpsTrackToTempFile(requireActivity().externalCacheDir)
                            ?.let {
                                FileProvider.getUriForFile(
                                    requireContext(),
                                    BuildConfig.APPLICATION_ID + ".provider",
                                    it
                                )
                            }
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "application/gpx")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(intent)
                } catch (e: IOException) {
                    Toast.makeText(
                        requireContext(), getString(R.string.gpx_file_not_copied), Toast.LENGTH_LONG
                    ).show()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.gpx_viewer_not_installed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        binding.gpsShare.setOnClickListener { _: View? ->
            if (summitToView.hasGpsTrack()) {
                try {
                    val uri =
                        summitToView.copyGpsTrackToTempFile(requireActivity().externalCacheDir)
                            ?.let {
                                FileProvider.getUriForFile(
                                    requireContext(),
                                    BuildConfig.APPLICATION_ID + ".provider",
                                    it
                                )
                            }
                    val intentShareFile = Intent(Intent.ACTION_SEND)
                    intentShareFile.type = "application/pdf"
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, uri)
                    intentShareFile.putExtra(
                        Intent.EXTRA_SUBJECT, getString(R.string.shared_gpx_subject)
                    )
                    intentShareFile.putExtra(
                        Intent.EXTRA_TEXT, getString(
                            R.string.shared_summit_gpx_text,
                            summitToView.name,
                            summitToView.getDateAsString(),
                            summitToView.elevationData.toString(),
                            summitToView.kilometers.toString()
                        )
                    )
                    startActivity(intentShareFile)
                } catch (e: IOException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.no_email_program_installed),
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.no_email_program_installed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun prepareCompareAutoComplete(summitToView: Summit, summitToCompare: Summit?) {
        val items = getSummitsSuggestions(summitToView)
        binding.summitNameToCompare.item = items
        var selectedPosition = -1
        if (summitToCompare != null) {
            selectedPosition =
                items.indexOfFirst { "${summitToCompare.getDateAsString()} ${summitToCompare.name}" == it }
            if (selectedPosition > -1) {
                binding.summitNameToCompare.setSelection(selectedPosition)
            }
        }
        binding.summitNameToCompare.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (items[position] == getString(R.string.none)) {
                        pageViewModel?.setSummitToCompareToNull()
                    } else if (view != null && selectedPosition != position) {
                        selectedPosition = position
                        val text = items[position]
                        if (text != "") {
                            val newSummitToCompare = summitsToCompare.find {
                                "${it.getDateAsString()} ${it.name}" == text
                            }
                            newSummitToCompare?.id?.let { pageViewModel?.getSummitToCompare(it) }
                        }
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {
                    if (summitToCompare != null) {
                        pageViewModel?.setSummitToCompareToNull()
                    }
                }
            }
    }


    private fun getSummitsSuggestions(localSummit: Summit): List<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsWithoutSimilarName =
            summitsToCompare.filter { it.name != localSummit.name && it.hasGpsTrack() }
                .sortedByDescending { it.date }
        val summitsWithSimilarName =
            summitsToCompare.filter { it.name == localSummit.name && it != localSummit }
                .sortedByDescending { it.date }
        summitsToCompare = summitsWithSimilarName + summitsWithoutSimilarName
        summitsToCompare.forEach {
            suggestions.add("${it.getDateAsString()} ${it.name}")
        }
        return suggestions
    }

    private fun setGpsTrack(
        localSummit: Summit,
        useSimplifiedTrack: Boolean = false,
        forceUpdate: Boolean = false
    ) {
        if (localSummit.hasGpsTrack(useSimplifiedTrack)) {
            localSummit.setGpsTrack(useSimplifiedTrack = useSimplifiedTrack)
            gpsTrack = localSummit.gpsTrack
            if (gpsTrack?.hasNoTrackPoints() == true || forceUpdate) {
                gpsTrack?.parseTrack(useSimplifiedIfExists = useSimplifiedTrack)
            }
            val localGpsTrack = gpsTrack
            if (localGpsTrack?.trackPoints?.isNotEmpty() == true && trackSlopeGraph.isEmpty()) {
                trackSlopeGraph = localGpsTrack.getTrackSlopeGraph()
            }
            if (localGpsTrack?.trackPoints?.isEmpty() == true) {
                localSummit.getGpsTrackPath(useSimplifiedTrack)
            }
        }
    }

    private fun drawChart(summitToView: Summit) {
        if (summitToView.hasGpsTrack()) {
            val localGpsTrack = gpsTrack
            if (localGpsTrack != null) {
                val lineChart = binding.lineChart
                setXAxis(lineChart)
                val params = lineChart.layoutParams
                params.height = (Resources.getSystem().displayMetrics.heightPixels * 0.2).toInt()
                lineChart.layoutParams = params
                val dataSets: MutableList<ILineDataSet> = ArrayList()
                val trackColor =
                    if (selectedCustomizeTrackItem == TrackColor.None || selectedCustomizeTrackItem == TrackColor.Mileage) TrackColor.Elevation else selectedCustomizeTrackItem
                val lineChartEntries = localGpsTrack.getTrackGraph(trackColor.f)
                val label = getString(trackColor.labelId)

                val leftAxis: YAxis = lineChart.axisLeft
                leftAxis.setDrawGridLines(true)
                leftAxis.isGranularityEnabled = true

                val dataSet = LineDataSet(lineChartEntries, label)
                setGraphView(dataSet)
                setColors(lineChartEntries, dataSet, summitToView)
                dataSets.add(dataSet)
                lineChart.data = LineData(dataSets)
                lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry, h: Highlight?) {
                        if (e.data is Pair<*, *> && (e.data as Pair<*, *>).first is TrackPoint) {
                            val trackPoint = (e.data as Pair<*, *>).first as TrackPoint
                            binding.osmap.overlays.remove(marker)
                            marker = OpenStreetMapUtils.addMarker(
                                binding.osmap,
                                requireContext(),
                                GeoPoint(trackPoint.latitude, trackPoint.longitude),
                                summitToView
                            )
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
        val l1 = LegendEntry(
            "$label ${getString(R.string.min)}",
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            selectedCustomizeTrackItem.minColor
        )
        val l2 = LegendEntry(
            "$label ${getString(R.string.max)}",
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            selectedCustomizeTrackItem.maxColor
        )
        l.setCustom(arrayOf(l1, l2))
        l.isEnabled = true
    }

    private fun setColors(
        lineChartEntries: MutableList<Entry>,
        dataSet: LineDataSet,
        summitToView: Summit
    ) {

        when (requireContext().resources?.configuration?.uiMode?.and(android.content.res.Configuration.UI_MODE_NIGHT_MASK)) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                binding.lineChart.xAxis.textColor = Color.WHITE
                binding.lineChart.axisRight.textColor = Color.WHITE
                binding.lineChart.axisLeft.textColor = Color.WHITE
                binding.lineChart.legend?.textColor = Color.WHITE
                binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdWhite)
            }

            android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                binding.lineChart.xAxis.textColor = Color.BLACK
                binding.lineChart.axisRight.textColor = Color.BLACK
                binding.lineChart.axisLeft.textColor = Color.BLACK
                binding.lineChart.legend?.textColor = Color.BLACK
                binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdBlack)
            }

            android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                binding.lineChart.xAxis.textColor = Color.BLACK
                binding.lineChart.axisRight.textColor = Color.WHITE
                binding.lineChart.axisLeft.textColor = Color.WHITE
                binding.lineChart.legend?.textColor = Color.WHITE
                binding.sportTypeImage.setImageResource(summitToView.sportType.imageIdWhite)
            }
        }
        val min = lineChartEntries.minByOrNull { it.y }?.y
        val max = lineChartEntries.maxByOrNull { it.y }?.y
        if (min != null && max != null) {
            val colors = lineChartEntries.map {
                val fraction = (it.y - min) / (max - min)
                interpolateColor(
                    selectedCustomizeTrackItem.minColor,
                    selectedCustomizeTrackItem.maxColor,
                    fraction
                )
            }
            dataSet.colors = colors
        }
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(
                    requireContext().resources.configuration.locales[0],
                    "%.1f km",
                    (value / 100f).roundToLong() / 10f
                )
            }
        }
    }

    private fun setOpenStreetMap(
        summitToView: Summit,
        summitToCompare: Summit?,
        summits: List<Summit>?,
        calculateBondingBox: Boolean = true,
        doCleanUp: Boolean = true
    ) {
        val hasPoints = gpsTrack?.hasOnlyZeroCoordinates() == false || summitToView.latLng != null
        binding.changeMapType.setImageResource(R.drawable.baseline_more_vert_black_24dp)
        binding.changeMapType.setOnClickListener {
            OpenStreetMapUtils.showMapTypeSelectorDialog(
                requireContext(), binding.osmap
            )
        }
        val height = if (hasPoints) 0.55 else 0.0
        val params = binding.osmap.layoutParams
        params?.height = (Resources.getSystem().displayMetrics.heightPixels * height).toInt()
        binding.osmap.layoutParams = params
        if (hasPoints) {
            if (doCleanUp) {
                binding.osmap.overlays.clear()
                binding.osmap.overlays.add(mLocationOverlay)
                OpenStreetMapUtils.addDefaultSettings(
                    binding.osmap,
                    requireActivity()
                )
            }
            if (summitToCompare != null) {
                OpenStreetMapUtils.drawTrack(
                    summitToCompare,
                    true,
                    binding.osmap,
                    TrackColor.None,
                    color = Color.BLACK
                )
            } else {
                val connectedEntries = summitToView.getConnectedEntries(summits)
                for (entry in connectedEntries) {
                    OpenStreetMapUtils.drawTrack(
                        entry, true, binding.osmap, TrackColor.None, color = Color.BLACK
                    )
                }
            }
            marker = OpenStreetMapUtils.addTrackAndMarker(
                summitToView,
                binding.osmap,
                requireContext(),
                true,
                selectedCustomizeTrackItem,
                true,
                rootView = binding.root,
                calculateBondingBox = calculateBondingBox
            )
            binding.customizeTrack.setOnClickListener {
                customizeColorOfTrackDialog(summitToView, summitToCompare, summits)
            }
        } else {
            binding.osmap.visibility = View.GONE
            binding.customizeTrack.visibility = View.GONE
            binding.changeMapType.visibility = View.GONE
            binding.gpsShare.visibility = View.GONE
            binding.gpsOpenWith.visibility = View.GONE
            binding.customizeTrack.visibility = View.GONE
            binding.customizeTrack.visibility = View.GONE
        }

    }

    private fun customizeColorOfTrackDialog(
        summitToView: Summit,
        summitToCompare: Summit?,
        summits: List<Summit>?
    ) {
        val fDialogTitle = getString(R.string.color_code_selection)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(fDialogTitle)
        builder.setSingleChoiceItems(
            usedItemsForColorCode.map { resources.getString(it.nameId) }.toTypedArray(),
            selectedCustomizeTrackItem.spinnerId
        ) { dialog: DialogInterface, item: Int ->
            selectedCustomizeTrackItem = usedItemsForColorCode[item]
            setOpenStreetMap(summitToView, summitToCompare, summits)
            drawChart(summitToView)
            dialog.dismiss()
        }

        val fMapTypeDialog = builder.create()
        fMapTypeDialog.setCanceledOnTouchOutside(true)
        fMapTypeDialog.show()
    }

    private fun setUsedItemsForColorCode() {
        usedItemsForColorCode = TrackColor.entries.filter { trackColorEntry: TrackColor ->
            gpsTrack?.trackPoints?.any {
                val value = trackColorEntry.f(it)
                value != null && value != 0.0
            } == true
        }.mapIndexed { i, entry ->
            entry.spinnerId = i
            entry
        }
        if (TrackColor.Elevation !in usedItemsForColorCode) {
            selectedCustomizeTrackItem = TrackColor.None
        } else if (TrackColor.Elevation in usedItemsForColorCode) {
            selectedCustomizeTrackItem = TrackColor.Elevation
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

    private fun showAllTracksOfSummitInBoundingBox(
        summitToView: Summit,
        summitToCompare: Summit?,
        summits: List<Summit>,
        maxPointsToShow: Int
    ) {
        binding.osmap.overlays.clear()

        val summitsWithSameBoundingBox = summits.filter {
            it.activityId != summitToView.activityId && binding.osmap.boundingBox?.let { it1 ->
                it.trackBoundingBox?.intersects(
                    it1
                )
            } == true
        }
        var pointsShown = 0
        var summitsShown = 0
        summitsWithSameBoundingBox.forEach { entry ->
            if (entry.hasGpsTrack() && pointsShown < maxPointsToShow) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        if (entry.gpsTrack == null) {
                            entry.setGpsTrack()
                        }
                    }
                    entry.gpsTrack?.addGpsTrack(
                        binding.osmap,
                        TrackColor.None,
                        summit = entry,
                        color = ContextCompat.getColor(requireContext(), entry.sportType.color)
                    )
                    summitsShown += 1
                    pointsShown += entry.gpsTrack?.trackPoints?.size ?: 0
                    binding.osmap.zoomController.activate()
                    if (summitsWithSameBoundingBox.last() == entry) {
                        setOpenStreetMap(
                            summitToView,
                            summitToCompare,
                            summits,
                            calculateBondingBox = false,
                            doCleanUp = false
                        )
                    }
                }
            }
        }
        if (pointsShown > maxPointsToShow) {
            if (context != null) {
                Toast.makeText(
                    context,
                    String.format(
                        requireContext().resources.getString(
                            R.string.summits_shown
                        ),
                        summitsShown.toString(),
                        summitsWithSameBoundingBox.size.toString()
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    }

}