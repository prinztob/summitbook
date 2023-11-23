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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentOpenStreetMapBinding
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.setTileSource
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.showMapTypeSelectorDialog
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.utils.BonusPackHelper
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
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

    private lateinit var mLocationOverlay: MyLocationNewOverlay


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val context = requireContext()
        maxPointsToShow =
            (sharedPreferences.getString("max_number_points", maxPointsToShow.toString())
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loadingPanel.visibility = View.VISIBLE
        binding.osmap.visibility = View.GONE
        setTileSource(selectedItem, binding.osmap)
        val context: Context? = this@OpenStreetMapFragment.activity
        mLocationOverlay =
            MyLocationNewOverlay(GpsMyLocationProvider(context), binding.osmap)
        mLocationOverlay.enableMyLocation()
        binding.osmap.overlays.add(mLocationOverlay)
        addDefaultSettings(requireContext(), binding.osmap, requireActivity())
        viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
            itData.data?.let { summits ->
                lifecycleScope.launch {
                    val filteredSummits = withContext(Dispatchers.IO) {
                        sortFilterValues.apply(summits, sharedPreferences)
                            .filter { it.sportType != SportType.IndoorTrainer && it.lat != null && it.lat != 0.0 && it.lng != null && it.lng != 0.0 }
                    }
                    addAllMarkers(filteredSummits)
                    binding.osmap.visibility = View.VISIBLE
                    binding.loadingPanel.visibility = View.GONE
                }
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
        binding.changeMap.setOnClickListener {
            showMapTypeSelectorDialog(
                requireContext(),
                binding.osmap
            )
        }

        binding.showAllTracks.setOnClickListener {
            showAllTracksOfSummitInBoundingBox()
        }

        binding.centerOnLocation.setOnClickListener {
            if (mLocationOverlay.isMyLocationEnabled) {
                val mapController = binding.osmap.controller
                mapController.setZoom(10.0)
                mapController.setCenter(mLocationOverlay.myLocation)
            }
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
                pointsShown -= mapCustomInfoBubble.entry.gpsTrack?.trackPoints?.size ?: 0
                mapCustomInfoBubble.updateGpxTrack(forceRemove = true)
                mMarkersShown.remove(it)
            }
            shouldBeShown == true
        }
        markersInBoundingBox.forEach {
            if (it != null) {
                val infoWindow: MapCustomInfoBubble = it.infoWindow as MapCustomInfoBubble
                if (it !in mMarkersShown || infoWindow.entry.gpsTrack?.isShownOnMap == false) {
                    if (pointsShown < maxPointsToShow) {
                        if (infoWindow.entry.hasGpsTrack()) {
                            lifecycleScope.launch {
                                withContext(Dispatchers.Default) {
                                    infoWindow.entry.setGpsTrack()
                                }
                                infoWindow.updateGpxTrack(forceShow = true)
                            }
                            pointsShown += infoWindow.entry.gpsTrack?.trackPoints?.size ?: 0
                            mMarkersShown.add(it)
                        }
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
                            R.string.summits_shown,
                            mMarkersShown.size.toString(),
                            markersInBoundingBox.size.toString()
                        )
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        binding.osmap.zoomController.activate()
    }

    private fun addAllMarkers(summits: List<Summit>) {
        val context = requireContext()
        binding.osmap.overlays?.clear()
        val markers = RadiusMarkerClusterer(context)
        val clusterIcon = BonusPackHelper.getBitmapFromVectorDrawable(
            context,
            org.osmdroid.bonuspack.R.drawable.marker_cluster
        )
        markers.setIcon(clusterIcon)
        markers.setMaxClusteringZoomLevel(10)
        binding.osmap.overlays.add(markers)
        mGeoPoints = ArrayList()
        mMarkers = ArrayList()
        for (entry in summits) {
            val latLng = entry.latLng
            if (latLng != null) {
                val point = GeoPoint(latLng.lat, latLng.lon)
                mGeoPoints.add(point)
                val marker = getMarker(binding.osmap, entry, point, context)
                markers.add(marker)
                mMarkers.add(marker)
            }
        }
        val mReceive: MapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(arg0: GeoPoint): Boolean {
                Log.d("debug", "LongPressHelper")
                mMarkers.forEach {
                    if (it?.isInfoWindowShown == true) {
                        it.infoWindow.close()
                    }
                }
                return false
            }
        }

        val eventsOverlay = MapEventsOverlay(mReceive)
        binding.osmap.overlays?.add(eventsOverlay)
        binding.osmap.post { calculateBoundingBox(binding.osmap, mGeoPoints) }
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

    companion object {
        const val TAG = "osmBaseFrag"
    }

}