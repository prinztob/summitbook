package de.drtobiasprinz.summitbook.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentOpenStreetMapBinding
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.setTileSource
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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
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
    private var fullscreenEnabled: Boolean = false
    private var summits: List<Summit> = emptyList()
    private var bookmarks: List<Summit> = emptyList()

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
        setMap()
        fullscreen(fullscreenEnabled)
        val osMapBoundingBox =
            sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")?.split(";")
                ?: emptyList()
        if (osMapBoundingBox.size == 4) {
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
            } catch (e: Exception) {
                Log.e(TAG, "Getting bounding box from shared preference failed. ${e.message}")
            }
        }
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

    override fun onDetach() {
        super.onDetach()
        val boundingBox = binding.osmap.boundingBox
        val editor = sharedPreferences.edit()
        editor.putString(
            Keys.PREF_OS_MAP_BOUNDING_BOX,
            "${boundingBox.latNorth};${boundingBox.lonEast};${boundingBox.latSouth};${boundingBox.lonWest}"
        )
        editor.apply()
        Log.d(TAG, "onDetach ${binding.osmap.boundingBox} and updated shared preference")
        fullscreen(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (PreferencesHelper.loadOnDeviceMaps() &&
            FileHelper.getOnDeviceMapFiles(requireContext()).isNotEmpty()
        ) {
            selectedItem = MapProvider.HIKING
        } else if (FileHelper.getOnDeviceMbtilesFiles(requireContext()).isNotEmpty()) {
            selectedItem = MapProvider.MBTILES
        }
        setTileSource(binding.osmap, requireContext())
        val context: Context? = this@OpenStreetMapFragment.activity
        mLocationOverlay =
            MyLocationNewOverlay(GpsMyLocationProvider(context), binding.osmap)
        mLocationOverlay.enableMyLocation()
        addDefaultSettings(requireContext(), binding.osmap, requireActivity())
        viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
            itData.data?.let { summits ->
                this.summits = summits
            }
        }
        viewModel.bookmarksList.observe(viewLifecycleOwner) { itData ->
            itData.data?.let { bookmarksList ->
                this.bookmarks = bookmarksList
            }
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDetach")
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
            zoomToLocation()
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
            binding.osmap.invalidate()
        }
    }

    private fun zoomToLocation() {
        if (mLocationOverlay.isMyLocationEnabled) {
            binding.osmap.overlays.add(mLocationOverlay)
            val mapController = binding.osmap.controller
            mapController.setZoom(10.0)
            mapController.setCenter(mLocationOverlay.myLocation)
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
        val markersInBoundingBox = mMarkers.filter {
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
        markersInBoundingBox.forEach {
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
                                        markersInBoundingBox.size.toString()
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                                boxAlreadyShown = true
                            }
                        }
                    }
                }
            }
        }
        binding.osmap.invalidate()
    }

    private fun addAllMarkers(summits: List<Pair<Summit, GeoPoint>>) {
        val context = requireContext()
        var mReceive: MapEventsReceiver
        val markers = RadiusMarkerClusterer(context)
        binding.osmap.overlays?.clear()
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
        }
    }

    companion object {
        const val TAG = "OpenStreetMapFragment"
    }

}