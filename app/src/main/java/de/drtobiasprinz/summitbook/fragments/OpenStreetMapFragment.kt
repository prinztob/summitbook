package de.drtobiasprinz.summitbook.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.MapCustomInfoBubble
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addMarker
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.calculateBoundingBox
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.setTileSource
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.showMapTypeSelectorDialog
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class OpenStreetMapFragment(var sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment {
    private var mGeoPoints: MutableList<GeoPoint?> = ArrayList()
    private var mMarkers: MutableList<Marker?> = ArrayList()
    private var mMarkersShown: MutableList<Marker?> = ArrayList()
    private var gotoLocationDialog: AlertDialog? = null
    lateinit var summitEntries: ArrayList<SummitEntry>
    private var filteredEntries: ArrayList<SummitEntry>? = null
    private lateinit var mMapView: MapView
    private lateinit var root: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(true)
        setHasOptionsMenu(true)
        val ctx = requireContext()
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Log.d(TAG, "onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        root = inflater.inflate(R.layout.fragment_open_street_map, container, false)
        setHasOptionsMenu(true)
        sortFilterHelper.setFragment(this)
        summitEntries = sortFilterHelper.entries
        filteredEntries = sortFilterHelper.filteredEntries
        mMapView = root.findViewById(R.id.osmap)
        val changeMapTypeButton: ImageButton = root.findViewById(R.id.change_map)
        changeMapTypeButton.setOnClickListener { showMapTypeSelectorDialog(requireContext(), mMapView) }

        val showAllTracksButton: ImageButton = root.findViewById(R.id.show_all_tracks)
        showAllTracksButton.setOnClickListener { _: View? ->
            val markersInBoundingBox = mMarkers.filter {
                val mapCustomInfoBubble: MapCustomInfoBubble = it?.infoWindow as MapCustomInfoBubble
                val shouldBeShown = mapCustomInfoBubble.entry.isInBoundingBox(mMapView.boundingBox)
                if (!shouldBeShown && it in mMarkersShown) {
                    mapCustomInfoBubble.updateGpxTrack(forceRemove = true)
                    mMarkersShown.remove(it)
                }
                shouldBeShown
            }
            if (markersInBoundingBox.size <= 10) {
                markersInBoundingBox.forEach {
                    if (it != null) {
                        val infoWindow: MapCustomInfoBubble = it.infoWindow as MapCustomInfoBubble
                        if (it !in mMarkersShown) {
                            infoWindow.updateGpxTrack(forceShow = true)
                            mMarkersShown.add(it)
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Show all tracks is only active when less than 10 tracks are selected. Currently ${markersInBoundingBox.size} would be shown", Toast.LENGTH_SHORT).show()
            }
        }

        requireActivity().findViewById<View>(R.id.add_new_summit).visibility = View.INVISIBLE

        mMapView.setOnGenericMotionListener { _: View?, event: MotionEvent ->
            if (0 != event.source and InputDevice.SOURCE_CLASS_POINTER) {
                if (event.action == MotionEvent.ACTION_SCROLL) {
                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                        mMapView.controller.zoomOut()
                    } else {
                        mMapView.controller.zoomIn()
                    }
                    return@setOnGenericMotionListener true
                }
            }
            false
        }
        Log.d(TAG, "onCreateView")
        return root
    }

    override fun onPause() {
        mMapView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView.onResume()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setTileSource(selectedItem, mMapView)
        addOverlays()
        val context: Context? = this.activity
        val mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mMapView)
        mLocationOverlay.enableMyLocation()
        mMapView.overlays.add(mLocationOverlay)
        addDefaultSettings(requireContext(), mMapView, requireActivity())
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDetach")
        mMapView.onDetach()
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        gotoLocationDialog?.dismiss()
        requireActivity().findViewById<View>(R.id.add_new_summit).visibility = View.VISIBLE
    }

    private fun addOverlays() {
        addAllMarkers()
    }

    private fun addAllMarkers() {
        mMapView.overlays.clear()
        mGeoPoints = ArrayList()
        mMarkers = ArrayList()
        val filteredEntriesLocal = filteredEntries
        if (filteredEntriesLocal != null) {
            for (entry in filteredEntriesLocal) {
                val latLng = entry.latLng
                if (latLng != null) {
                    val point = GeoPoint(latLng.latitude, latLng.longitude)
                    mGeoPoints.add(point)
                    mMarkers.add(addMarker(mMapView, requireContext(), point, entry))
                }
            }
        }
        mMapView.post { calculateBoundingBox(mMapView, mGeoPoints) }
    }

    override fun update(filteredSummitEntries: ArrayList<SummitEntry>?) {
        filteredEntries = filteredSummitEntries
        addAllMarkers()
    }

    companion object {
        const val TAG = "osmBaseFrag"
    }

}