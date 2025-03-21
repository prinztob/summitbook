package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentOpenStreetMapBinding
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.allSummits
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.getOsmdroidTilesFolder
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.setTileProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.showMapTypeSelectorDialog
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.utils.BonusPackHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class OpenStreetMapFragment : Fragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: FragmentOpenStreetMapBinding
    private val viewModel: DatabaseViewModel by activityViewModels()

    private var mGeoPoints: MutableList<GeoPoint?> = ArrayList()
    private var mMarkers: MutableList<Marker?> = ArrayList()
    private var mMarkersShown: MutableList<Marker?> = ArrayList()
    private var gotoLocationDialog: AlertDialog? = null
    private var maxPointsToShow: Int = 10000
    private var showSummits: Boolean = false
    private var showBookmarks: Boolean = false
    private var followLocationEnabled: Boolean = false
    private var fullscreenEnabled: Boolean = false
    private var summits: List<Summit> = emptyList()
    private var bookmarks: List<Summit> = emptyList()
    private var layers: MutableList<Pair<String, TilesOverlay>> = mutableListOf()

    private lateinit var mLocationOverlay: MyLocationNewOverlay


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val context = requireContext()
        maxPointsToShow =
            (sharedPreferences.getString(Keys.PREF_MAX_NUMBER_POINT, maxPointsToShow.toString())
                ?: maxPointsToShow.toString()).toInt()
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        OpenStreetMapUtils.setOsmConfForTiles()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOpenStreetMapBinding.inflate(layoutInflater, container, false)
        val osMapBoundingBox =
            sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")?.split(";")
                ?: emptyList()
        if (osMapBoundingBox.size == 6) {
            try {
                val boundingBox = BoundingBox()
                boundingBox.set(
                    osMapBoundingBox[0].toDouble(),
                    osMapBoundingBox[1].toDouble(),
                    osMapBoundingBox[2].toDouble(),
                    osMapBoundingBox[3].toDouble()
                )
                binding.osmap.post {
                    binding.osmap.zoomToBoundingBox(boundingBox, false, 30)
                }
                if (osMapBoundingBox[4].toInt() == 1) {
                    showSummits = true
                    binding.showSummits.alpha = 1f
                }
                if (osMapBoundingBox[5].toInt() == 1) {
                    showBookmarks = true
                    binding.showBookmarks.alpha = 1f
                }
                Log.i(
                    TAG,
                    "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Getting bounding box from shared preference failed. ${e.message}")
            }
        }
        setMap()
        fullscreen(fullscreenEnabled)
        return binding.root
    }


    override fun onPause() {
        super.onPause()
        binding.osmap.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.osmap.onResume()
    }

    @SuppressLint("ApplySharedPref")
    private fun cleanupAndSaveCurrentStatus() {
        Log.i(TAG, "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}")
        val boundingBox = binding.osmap.boundingBox
        val editor = sharedPreferences.edit()
        editor.putString(
            Keys.PREF_OS_MAP_BOUNDING_BOX,
            "${boundingBox.latNorth};${boundingBox.lonEast};${boundingBox.latSouth};${boundingBox.lonWest};${if (showSummits) 1 else 0};${if (showBookmarks) 1 else 0}"
        )
        editor.commit()
        Log.i(TAG, "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}")
        fullscreen(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (PreferencesHelper.loadOnDeviceMaps() &&
            FileHelper.getOnDeviceMbtilesFiles(requireContext()).isNotEmpty()
        ) {
            selectedItem = MapProvider.MBTILES
        } else if (PreferencesHelper.loadOnDeviceMaps() &&
            FileHelper.getOnDeviceMapFiles(requireContext()).isNotEmpty()
        ) {
            selectedItem = MapProvider.HIKING
        }
        setTileProvider(binding.osmap, requireContext())
        showOverlayIfExist()

        setSlidersForOverlayMaps()
        val context: Context? = this@OpenStreetMapFragment.activity
        addFollowTrack(context)
        mLocationOverlay.enableMyLocation()
        showMyLocation()
        addDefaultSettings(requireContext(), binding.osmap, requireActivity())
        viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
            itData.data?.let { summits ->
                allSummits = summits
                this.summits = summits
                showSummitsAndBookmarksIfEnabled()
            }
        }
        viewModel.bookmarksList.observe(viewLifecycleOwner) { itData ->
            itData.data?.let { bookmarksList ->
                this.bookmarks = bookmarksList
            }
        }
    }

    private fun addFollowTrack(context: Context?) {
        var currentTrack = mutableListOf<GeoPoint>()
        val polyline = Polyline(binding.osmap)

        binding.followLocation.setOnClickListener {
            followLocationEnabled = !followLocationEnabled
            if (followLocationEnabled) {
                binding.followLocation.setImageResource(R.drawable.baseline_stop_circle_24)
            } else {
                currentTrack = mutableListOf()
                binding.osmap.overlayManager?.remove(polyline)
                binding.followLocation.setImageResource(R.drawable.baseline_play_circle_filled_24)
            }
        }

        mLocationOverlay =
            object : MyLocationNewOverlay(GpsMyLocationProvider(context), binding.osmap) {
                override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
                    super.onLocationChanged(location, source)
                    if (location != null && followLocationEnabled && location.speed > 0f) {
                        currentTrack.add(GeoPoint(location.latitude, location.longitude))
                        polyline.outlinePaint?.color = Color.MAGENTA
                        polyline.outlinePaint?.strokeWidth = 16f
                        polyline.setPoints(currentTrack)
                        binding.osmap.overlayManager?.add(polyline)
                    }
                }
            }
    }

    private fun setSlidersForOverlayMaps() {
        var lastId = -1
        val factor = requireContext().resources.displayMetrics.density.toInt()

        for (layer in layers) {
            val slider = Slider(requireContext())
            slider.id = View.generateViewId()

            val layoutParams = ConstraintLayout.LayoutParams(
                factor * 200,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.endToEnd = ConstraintSet.PARENT_ID

            if (lastId == -1) {
                layoutParams.topToBottom = binding.showAllTracks.id
            } else {
                layoutParams.topToBottom = lastId
            }
            layoutParams.topMargin = 10 * factor
            layoutParams.marginEnd = 40 * factor
            slider.layoutParams = layoutParams
            slider.stepSize = 0.1f
            slider.valueFrom = 0f
            slider.valueTo = 0.4f


            slider.addOnSliderTouchListener(
                object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                    }

                    override fun onStopTrackingTouch(slider: Slider) {
                        binding.osmap.overlays.remove(layer.second)
                        setAlphaForLayer(layer.second, slider.value)
                        binding.osmap.overlays.add(layer.second)
                        binding.osmap.invalidate()
                    }
                }
            )
            binding.constraintLayout.addView(slider)
            val tv = TextView(requireContext())
            tv.text = layer.first
            val layoutParamsTv = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParamsTv.startToStart = slider.id
            layoutParamsTv.topToTop = slider.id
            layoutParamsTv.topMargin = -10 * factor
            tv.layoutParams = layoutParamsTv
            binding.constraintLayout.addView(tv)
            lastId = slider.id
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        cleanupAndSaveCurrentStatus()
        binding.osmap.onDetach()
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        gotoLocationDialog?.dismiss()
    }


    private fun setMap() {
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        binding.fullscreen.setOnClickListener {
            fullscreen(fullscreenEnabled)
        }
        binding.showSummits.setOnClickListener {
            showSummits = !showSummits
            if (showSummits) {
                binding.showSummits.alpha = 1f
            } else {
                binding.showSummits.alpha = 0.5f
            }
            showSummitsAndBookmarksIfEnabled()
        }
        binding.showBookmarks.setOnClickListener {
            showBookmarks = !showBookmarks
            if (showBookmarks) {
                binding.showBookmarks.alpha = 1f
            } else {
                binding.showBookmarks.alpha = 0.5f
            }
            showSummitsAndBookmarksIfEnabled()
        }
        binding.changeMap.setOnClickListener {
            showMapTypeSelectorDialog(
                requireContext(),
                binding.osmap
            )
        }

        binding.showAllTracks.setOnClickListener {
            showAllTracksOfSummitInBoundingBox()
        }
        binding.centerOnSummits.setOnClickListener {
            binding.osmap.post { calculateBoundingBox(binding.osmap, mGeoPoints) }
        }
        binding.centerOnLocation.setOnClickListener {
            showMyLocation(true)
        }

        binding.osmap.setOnGenericMotionListener { _: View?, event: MotionEvent ->
            if (0 != event.source and InputDevice.SOURCE_CLASS_POINTER) {
                if (event.action == MotionEvent.ACTION_SCROLL) {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        binding.osmap.controller?.zoomOut()
                    } else {
                        binding.osmap.controller?.zoomIn()
                    }
                    return@setOnGenericMotionListener true
                }
            }
            false
        }
    }

    private fun showSummitsAndBookmarksIfEnabled() {
        if (showSummits || showBookmarks) {
            binding.loadingPanel.visibility = View.VISIBLE
            lifecycleScope.launch {
                var filteredSummits: List<Pair<Summit, GeoPoint>> = listOf()
                withContext(Dispatchers.IO) {
                    val relevantSummits = if (showSummits) sortFilterValues.apply(
                        summits,
                        sharedPreferences
                    ) else emptyList()
                    val relevantBookmarks =
                        if (showBookmarks) sortFilterValues.applyForBookmarks(bookmarks) else emptyList()

                    filteredSummits =
                        (relevantSummits + relevantBookmarks).filter {
                            it.sportType != SportType.IndoorTrainer
                                    && it.lat != null
                                    && it.lat != 0.0
                                    && it.lng != null
                                    && it.lng != 0.0
                        }.map { Pair(it, GeoPoint(it.lat!!, it.lng!!)) }
                }
                addAllMarkers(filteredSummits)
            }
        } else {
            binding.osmap.overlays?.clear()
            showMyLocation()
            binding.osmap.invalidate()
        }
    }

    private fun showMyLocation(zoom: Boolean = false) {
        if (mLocationOverlay.isMyLocationEnabled) {
            val arrow = ResourcesCompat.getDrawable(
                resources, R.drawable.baseline_my_location_24,
                null
            )?.toBitmap()
            mLocationOverlay.setPersonIcon(arrow)
            mLocationOverlay.setPersonAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            mLocationOverlay.setDirectionIcon(arrow)
            mLocationOverlay.setDirectionAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            binding.osmap.overlays.add(mLocationOverlay)
            val mapController = binding.osmap.controller
            if (zoom) {
                mapController.setCenter(mLocationOverlay.myLocation)
            }
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_not_enabled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showAllTracksOfSummitInBoundingBox() {
        var pointsShown = mMarkersShown.sumOf {
            (it?.infoWindow as MapCustomInfoBubble).entry.gpsTrack?.trackPoints?.size ?: 0
        }
        val summitsInBoundingBox = mMarkers.filter {
            val mapCustomInfoBubble: MapCustomInfoBubble = it?.infoWindow as MapCustomInfoBubble
            val shouldBeShown = binding.osmap.boundingBox?.let { it1 ->
                mapCustomInfoBubble.entry.isInBoundingBox(it1)
            }
            if (shouldBeShown == false && it in mMarkersShown) {
                Log.i(
                    "trackPoints",
                    "trackPoints --: ${mapCustomInfoBubble.entry.gpsTrack?.trackPoints?.size ?: 0}"
                )
                pointsShown -= mapCustomInfoBubble.entry.gpsTrack?.trackPoints?.size ?: 0
                mapCustomInfoBubble.updateGpxTrack(forceRemove = true)
                mMarkersShown.remove(it)
            }
            shouldBeShown == true
        }
        var boxAlreadyShown = false
        summitsInBoundingBox.forEach {
            if (it != null) {
                val infoWindow: MapCustomInfoBubble = it.infoWindow as MapCustomInfoBubble
                if (it !in mMarkersShown || infoWindow.entry.gpsTrack?.isShownOnMap == false) {
                    if (infoWindow.entry.hasGpsTrack()) {
                        lifecycleScope.launch {
                            var show = false
                            withContext(Dispatchers.Default) {
                                if (pointsShown < maxPointsToShow) {
                                    show = true
                                    infoWindow.entry.setGpsTrack()
                                    pointsShown += infoWindow.entry.gpsTrack?.trackPoints?.size ?: 0
                                }
                            }
                            if (show) {
                                infoWindow.updateGpxTrack(forceShow = true)
                                Log.e(
                                    "trackPoints",
                                    "trackPoints ${pointsShown}++: ${infoWindow.entry.gpsTrack?.trackPoints?.size ?: 0}"
                                )
                                mMarkersShown.add(it)
                            } else if (!boxAlreadyShown) {
                                Toast.makeText(
                                    context,
                                    String.format(
                                        requireContext().resources.getString(
                                            R.string.summits_shown
                                        ),
                                        mMarkersShown.size.toString(),
                                        summitsInBoundingBox.size.toString()
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                                boxAlreadyShown = true
                            }
                            binding.osmap.invalidate()
                        }
                    }
                }
            }
        }
    }

    private fun addAllMarkers(summits: List<Pair<Summit, GeoPoint>>) {
        val context = requireContext()
        var mReceive: MapEventsReceiver
        val markers = RadiusMarkerClusterer(context)
        binding.osmap.overlays?.clear()
        showMyLocation()
        showOverlayIfExist()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val clusterIcon = BonusPackHelper.getBitmapFromVectorDrawable(
                    context,
                    org.osmdroid.bonuspack.R.drawable.marker_cluster
                )
                markers.setIcon(clusterIcon)
                markers.setMaxClusteringZoomLevel(10)
                mGeoPoints = ArrayList()
                mMarkers = ArrayList()
                summits.forEach { pair ->
                    mGeoPoints.add(pair.second)
                    val marker = getMarker(binding.osmap, pair.first, pair.second, context)
                    markers.add(marker)
                    mMarkers.add(marker)
                }
                mReceive = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        return false
                    }

                    override fun longPressHelper(arg0: GeoPoint): Boolean {
                        mMarkers.forEach {
                            if (it?.isInfoWindowShown == true) {
                                it.infoWindow.close()
                            }
                        }
                        return false
                    }
                }

            }
            val eventsOverlay = MapEventsOverlay(mReceive)
            binding.osmap.overlays.add(markers)
            binding.osmap.overlays?.add(eventsOverlay)
            binding.loadingPanel.visibility = View.GONE
            binding.osmap.invalidate()
        }
    }


    private fun getMarker(
        localMapView: MapView?,
        entry: Summit,
        point: GeoPoint,
        context: Context
    ): Marker {
        val marker = Marker(localMapView)
        marker.title = entry.id.toString()
        marker.position = point
        if (entry.hasGpsTrack()) {
            marker.icon = ResourcesCompat.getDrawable(
                context.resources,
                entry.sportType.markerIdWithGpx,
                null
            )
        } else {
            marker.icon = ResourcesCompat.getDrawable(
                context.resources,
                entry.sportType.markerIdWithoutGpx,
                null
            )
        }
        marker.infoWindow = MapCustomInfoBubble(binding.osmap, entry, context, false)
        marker.setOnMarkerClickListener { marker1, _ ->
            if (!marker1.isInfoWindowShown) {
                marker1.showInfoWindow()
            } else {
                marker1.closeInfoWindow()
            }
            false
        }
        return marker
    }

    private fun fullscreen(exit: Boolean) {
        fullscreenEnabled = !fullscreenEnabled
        if (exit) {
            binding.fullscreen.setImageResource(R.drawable.baseline_fullscreen_24)
            (requireActivity() as MainActivity).binding.toolbarInclude.toolbar.visibility =
                View.VISIBLE
            (requireActivity() as MainActivity).binding.overviewLayout.visibility = View.VISIBLE
        } else {
            binding.fullscreen.setImageResource(R.drawable.baseline_fullscreen_exit_24)
            (requireActivity() as MainActivity).binding.toolbarInclude.toolbar.visibility =
                View.GONE
            (requireActivity() as MainActivity).binding.overviewLayout.visibility = View.GONE
            (requireActivity() as MainActivity).binding.chartLayout.visibility = View.GONE
        }
    }


    private fun showOverlayIfExist() {
        val fileEnding = "mbtiles"
        val overlayFolder = File(getOsmdroidTilesFolder(), "overlays")
        val files = overlayFolder.listFiles()?.filter { it.name.endsWith(".${fileEnding}") }
        if (ArchiveFileFactory.isFileExtensionRegistered(fileEnding) && files?.isNotEmpty() == true) {
            try {
                files.forEach {
                    val tileProvider =
                        OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(it))
                    val layer = TilesOverlay(tileProvider, context)
                    layer.loadingBackgroundColor = Color.TRANSPARENT
                    layer.loadingLineColor = Color.TRANSPARENT
                    binding.osmap.overlays.add(layer)
                    layers.add(Pair(it.name.replace(".$fileEnding", ""), layer))
                    setAlphaForLayer(layer)
                    binding.osmap.invalidate()
                }
                return
            } catch (ex: Exception) {
                Log.e(TAG, Log.getStackTraceString(ex))
            }
        }
    }

    private fun setAlphaForLayer(layer: TilesOverlay, alpha: Float = 0f) {
        Log.i(TAG, "Set alpha $alpha")
        layer.setColorFilter(
            ColorMatrixColorFilter(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,  //red
                    0f, 1f, 0f, 0f, 0f,  //green
                    0f, 0f, 1f, 0f, 0f,  //blue
                    alpha, alpha, alpha, alpha, alpha
                )
            )
        )
    }


    companion object {
        const val TAG = "OpenStreetMapFragment"
    }

}